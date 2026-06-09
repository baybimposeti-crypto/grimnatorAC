package com.kyssta.redoxguard.checks.combat;

import com.kyssta.redoxguard.RedoxGuard;
import com.kyssta.redoxguard.checks.Check;
import com.kyssta.redoxguard.combat.evidence.AttackEvidence;
import com.kyssta.redoxguard.combat.evidence.EvidenceLevel;
import com.kyssta.redoxguard.combat.geometry.AABB;
import com.kyssta.redoxguard.combat.geometry.RayAABB;
import com.kyssta.redoxguard.combat.geometry.RayTraceResult;
import com.kyssta.redoxguard.combat.geometry.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Checks if a player is hitting entities outside their hitbox
 */
public class HitboxCheck extends Check {

    private final Map<UUID, Integer> hitboxViolations = new HashMap<>();

    public HitboxCheck(RedoxGuard plugin) {
        super(plugin, "Hitbox", "combat");
    }
    
    /**
     * Check if a player is hitting entities outside their hitbox
     * @param player The player
     * @param target The entity being attacked
     */
    public void checkHitbox(Player player, Entity target) {
        if (!isEnabled() || player.hasPermission("redoxguard.bypass")) {
            return;
        }
        
        int ping = getPlayerData(player).getPing();
        double expansionAmount = uncertainty(player, target, ping);
        double maxDistance = plugin.getConfigManager().getCheckConfig("combat")
                .getDouble("hitbox.max-distance", 4.5);

        Location eye = player.getEyeLocation();
        AABB targetBox = AABB.from(target.getBoundingBox()).expand(expansionAmount);
        RayTraceResult trace = RayAABB.trace(Vec3.from(eye), Vec3.from(eye.getDirection()), targetBox, maxDistance);

        boolean exempt = isStrictExempt(player, target, ping);
        AttackEvidence evidence = new AttackEvidence(
                player.getUniqueId(), player.getName(), target.getUniqueId(), target.getEntityId(),
                System.nanoTime(), plugin.getServerTick(), ping, ping, 20.0, "unknown",
                false, ping > plugin.getConfigManager().getConfig().getInt("combat.network-exemptions.max-ping-for-strict-checks", 180),
                false, player.isInsideVehicle() || target.isInsideVehicle(), false, false,
                trace.closestDistanceToAabb(), trace.intersects(), trace.distance(), maxDistance, expansionAmount,
                0.0, 0.0, 0L, 0L, -1L, false, 0, 0, false,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0,
                plugin.getEvidenceEngine().profile(player).attacks().size(),
                0.0, 0.0, 0.0, trace.intersects() ? 0.0 : Math.min(15.0, trace.closestDistanceToAabb() * 20.0),
                0.0, trace.intersects() ? EvidenceLevel.INFO : EvidenceLevel.MEDIUM,
                "ray-aabb hitbox hit=" + trace.intersects() + " missDistance=" + String.format("%.3f", trace.closestDistanceToAabb())
        );
        plugin.getEvidenceEngine().record(player, evidence);

        UUID uuid = player.getUniqueId();
        if (!exempt && !trace.intersects()) {
            int currentViolations = hitboxViolations.getOrDefault(uuid, 0);
            hitboxViolations.put(uuid, currentViolations + 1);
            if (currentViolations >= 3) {
                flag(player, "ray missed target hitbox (miss distance " + String.format("%.3f", trace.closestDistanceToAabb()) + ")");
                debug(player.getName() + " ray missed hitbox of " + target.getType().name());
            }
        } else {
            hitboxViolations.put(uuid, Math.max(0, hitboxViolations.getOrDefault(uuid, 0) - 1));
        }
    }

    private double uncertainty(Player player, Entity target, int ping) {
        double configured = plugin.getConfigManager().getCheckConfig("combat")
                .getDouble("hitbox.expansion-amount", 0.05);
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
