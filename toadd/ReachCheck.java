package com.kyssta.redoxguard.checks.combat;

import com.kyssta.redoxguard.RedoxGuard;
import com.kyssta.redoxguard.checks.Check;
import com.kyssta.redoxguard.combat.evidence.AttackEvidence;
import com.kyssta.redoxguard.combat.evidence.EvidenceLevel;
import com.kyssta.redoxguard.combat.geometry.AABB;
import com.kyssta.redoxguard.combat.geometry.RayAABB;
import com.kyssta.redoxguard.combat.geometry.RayTraceResult;
import com.kyssta.redoxguard.combat.geometry.Vec3;
import com.kyssta.redoxguard.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReachCheck extends Check {

    private final Map<UUID, Integer> reachViolations = new HashMap<>();

    public ReachCheck(RedoxGuard plugin) {
        super(plugin, "Reach", "combat");
    }
    
    /**
     * Check if a player is attacking an entity from too far away
     * @param player The player
     * @param target The entity being attacked
     */
    public void checkReach(Player player, Entity target) {
        if (!isEnabled() || player.hasPermission("redoxguard.bypass")) {
            return;
        }
        
        // Skip if target is not a player (reach hacks are mainly for PvP)
        if (!(target instanceof Player)) {
            return;
        }
        
        PlayerData data = getPlayerData(player);
        UUID uuid = player.getUniqueId();
        
        Location eye = player.getEyeLocation();
        Vec3 origin = Vec3.from(eye);
        Vec3 direction = Vec3.from(eye.getDirection()).normalize();
        AABB targetBox = AABB.from(target.getBoundingBox());

        int ping = data.getPing();
        double baseReach = plugin.getConfigManager().getCheckConfig("combat")
                .getDouble("reach.max-distance", 3.1);
        double uncertainty = uncertainty(player, target, ping);
        double allowedReach = baseReach + uncertainty;
        RayTraceResult trace = RayAABB.trace(origin, direction, targetBox.expand(uncertainty), allowedReach + 1.0);
        double computedReach = trace.intersects() ? trace.distance() : origin.subtract(targetBox.center()).length();

        boolean exempt = isStrictExempt(player, target, ping);
        EvidenceLevel evidenceLevel = computedReach > allowedReach ? EvidenceLevel.MEDIUM : EvidenceLevel.INFO;
        AttackEvidence evidence = new AttackEvidence(
                player.getUniqueId(), player.getName(), target.getUniqueId(), target.getEntityId(),
                System.nanoTime(), plugin.getServerTick(), ping, ping, 20.0, "unknown",
                false, ping > plugin.getConfigManager().getConfig().getInt("combat.network-exemptions.max-ping-for-strict-checks", 180),
                false, player.isInsideVehicle() || target.isInsideVehicle(), false, false,
                trace.closestDistanceToAabb(), trace.intersects(), computedReach, allowedReach, uncertainty,
                0.0, 0.0, 0L, 0L, -1L, false, 0, 0, false,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0,
                plugin.getEvidenceEngine().profile(player).attacks().size(),
                0.0, 0.0, computedReach > allowedReach ? Math.min(15.0, (computedReach - allowedReach) * 10.0) : 0.0,
                0.0, 0.0, evidenceLevel,
                "ray-aabb reach computed=" + String.format("%.3f", computedReach) + " allowed=" + String.format("%.3f", allowedReach)
        );
        plugin.getEvidenceEngine().record(player, evidence);

        if (!exempt && trace.intersects() && computedReach > allowedReach) {
            int currentViolations = reachViolations.getOrDefault(uuid, 0);
            reachViolations.put(uuid, currentViolations + 1);
            
            // Only flag after multiple violations
            if (currentViolations >= 3) {
                flag(player, "ray reach too far (" + String.format("%.2f", computedReach) + 
                        " > " + String.format("%.2f", allowedReach) + ")");
                
                debug(player.getName() + " reached too far: " + String.format("%.2f", computedReach) + 
                        " > " + String.format("%.2f", allowedReach) + " (violations: " + currentViolations + ")");
            }
        } else {
            // Reset violations if player is not reaching too far
            reachViolations.put(uuid, Math.max(0, reachViolations.getOrDefault(uuid, 0) - 1));
        }
    }

    private double uncertainty(Player player, Entity target, int ping) {
        double configured = plugin.getConfigManager().getCheckConfig("combat")
                .getDouble("reach.uncertainty-buffer", 0.05);
        double pingBuffer = Math.min(ping / 1000.0 * 0.6, 0.25);
        double velocityBuffer = player.getVelocity().length() * 0.15 + target.getVelocity().length() * 0.15;
        return Math.min(0.45, configured + pingBuffer + velocityBuffer);
    }

    private boolean isStrictExempt(Player player, Entity target, int ping) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        if (player.isInsideVehicle() || target.isInsideVehicle() || player.isDead() || target.isDead()) {
            return true;
        }
        int maxPing = plugin.getConfigManager().getConfig()
                .getInt("combat.network-exemptions.max-ping-for-strict-checks", 180);
        return ping > maxPing;
    }
}
