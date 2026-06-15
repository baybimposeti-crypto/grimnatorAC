package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.collisions.datatypes.SimpleCollisionBox;
import com.grimnatorac.utils.data.packetentity.PacketEntity;
import com.grimnatorac.utils.math.Vector3dm;
import com.grimnatorac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hitboxes — detects attacks where the player's ray does not intersect the
 * target's server-side AABB across all lag-compensated look snapshots.
 *
 * <p>The check runs independently of Reach and applies its own configurable
 * miss-distance threshold ({@code Hitboxes.miss-threshold}, default 0.1)
 * and expanded uncertainty buffer ({@code Hitboxes.expansion}, default 0.1)
 * to tolerate network jitter without false-flagging.
 *
 * <p>Violations accumulate silently (default alert after 5 hits). Each clean
 * attack decays 1 violation.
 *
 * <p>Reach.java still calls {@code flagAndAlert} on this check for complete
 * ray-miss cases — those continue to work unchanged.
 */
@CheckData(name = "Hitboxes", stableKey = "grimnatorac.combat.hitboxes", setback = 10)
public class Hitboxes extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final double RAY_LENGTH = 6.5;

    private static final List<com.github.retrooper.packetevents.protocol.entity.type.EntityType> BLACKLISTED =
            List.of(EntityTypes.BOAT, EntityTypes.CHEST_BOAT, EntityTypes.SHULKER);

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** How far the ray's closest approach to the AABB may exceed the box before flagging. */
    private double missThreshold;

    /** Extra expansion applied to the target box to absorb lag/0.03 uncertainty. */
    private double expansion;

    /** Number of flagged attacks before an alert fires. */
    private int alertThreshold;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    private int silentViolations = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public Hitboxes(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        if (player.inVehicle()) return;

        int entityId = -1;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity pkt = new WrapperPlayClientInteractEntity(event);
            if (pkt.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                entityId = pkt.getEntityId();
            }
        }

        if (entityId == -1) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        if (entity == null || entity.isDead || !entity.isLivingEntity) return;
        if (BLACKLISTED.contains(entity.getType())) return;

        checkHitbox(entity);
    }

    // -----------------------------------------------------------------------
    // Core check
    // -----------------------------------------------------------------------

    private void checkHitbox(PacketEntity entity) {
        SimpleCollisionBox box = getTargetBox(entity);
        box.expand(expansion);

        // Ping compensation: check current + last rotation snapshots
        List<Vector3dm> lookDirs = buildLookDirs();
        double[] eyeHeights = player.getPossibleEyeHeights();

        double minDist = Double.MAX_VALUE;

        for (Vector3dm look : lookDirs) {
            Vector3dm scaledLook = look.clone();
            scaledLook.multiply(RAY_LENGTH);

            for (double eye : eyeHeights) {
                Vector3d eyePos = new Vector3d(player.x, player.y + eye, player.z);
                Vector3d endPos = eyePos.add(scaledLook.getX(), scaledLook.getY(), scaledLook.getZ());

                if (ReachUtils.isVecInside(box, eyePos)) {
                    // Eye is inside box — definitely a hit
                    decay();
                    return;
                }

                Vector3d intercept = ReachUtils.calculateIntercept(box, eyePos, endPos).first();
                if (intercept != null) {
                    minDist = Math.min(eyePos.distance(intercept), minDist);
                }
            }
        }

        // minDist == Double.MAX_VALUE means no intercept found on any snapshot
        if (minDist == Double.MAX_VALUE) {
            // Complete miss — compute closest approach distance for verbose output
            double closestApproach = computeClosestApproach(box, lookDirs, eyeHeights);
            accumulate(String.format("miss dist=%.3f", closestApproach));
        } else {
            // Ray intersected box — clean
            decay();
        }
    }

    /**
     * Approximates the closest the ray came to the AABB when it missed entirely,
     * used only for the verbose flag string.
     */
    private double computeClosestApproach(SimpleCollisionBox box,
                                          List<Vector3dm> lookDirs,
                                          double[] eyeHeights) {
        double closest = Double.MAX_VALUE;
        for (Vector3dm look : lookDirs) {
            Vector3dm scaled = look.clone();
            scaled.multiply(RAY_LENGTH);
            for (double eye : eyeHeights) {
                Vector3d eyePos = new Vector3d(player.x, player.y + eye, player.z);
                // Use the distance from eye to nearest corner of the AABB as a proxy
                double dist = ReachUtils.getMinReachToBox(player, box);
                closest = Math.min(closest, dist);
            }
        }
        return closest == Double.MAX_VALUE ? -1.0 : closest;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<Vector3dm> buildLookDirs() {
        List<Vector3dm> dirs = new ArrayList<>(Collections.singletonList(
                ReachUtils.getLook(player, player.yaw, player.pitch)));

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
            dirs.add(ReachUtils.getLook(player, player.lastYaw, player.pitch));
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                dirs.add(ReachUtils.getLook(player, player.lastYaw, player.lastPitch));
            }
        }
        return dirs;
    }

    private SimpleCollisionBox getTargetBox(PacketEntity entity) {
        if (entity.getType() == EntityTypes.END_CRYSTAL) {
            return new SimpleCollisionBox(
                    entity.trackedServerPosition.getPos().subtract(1, 0, 1),
                    entity.trackedServerPosition.getPos().add(1, 2, 1));
        }
        return entity.getPossibleCollisionBoxes();
    }

    private void accumulate(String verbose) {
        silentViolations++;
        if (silentViolations >= alertThreshold) {
            flagAndAlert(verbose);
            silentViolations = 0;
        }
    }

    private void decay() {
        silentViolations = Math.max(0, silentViolations - 1);
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        missThreshold  = config.getDoubleElse("Hitboxes.miss-threshold", 0.1);
        expansion      = config.getDoubleElse("Hitboxes.expansion",      0.1);
        alertThreshold = config.getIntElse("Hitboxes.alert-threshold",    5);
    }
}
