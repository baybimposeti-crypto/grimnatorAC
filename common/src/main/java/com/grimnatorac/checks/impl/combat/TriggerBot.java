package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.GrimAPI;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.collisions.datatypes.SimpleCollisionBox;
import com.grimnatorac.utils.data.packetentity.PacketEntity;
import com.grimnatorac.utils.math.Vector3dm;
import com.grimnatorac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TriggerBot — detects trigger-bot and kill-aura clients using two methods:
 *
 * <p><b>Method A — TriggerBot timing:</b> Measures the gap between the
 * server-side crosshair first intersecting a target's AABB and the attack
 * packet arriving.  Legitimate players have a reaction time of ≥1 tick
 * (≥50 ms).  A trigger-bot fires within 0–1 ticks of the LOS entering
 * the hitbox, consistently.
 *
 * <p><b>Method B — Ray-miss (hitbox):</b> If an attack packet arrives but
 * the attacker's ray does not intersect the target's AABB at all, the
 * client is attacking a position the server never confirmed as valid.
 *
 * <p><b>Silent flag / decay system:</b> Neither method alerts staff
 * immediately.  Each method accumulates its own violation counter.  An
 * admin alert is only sent when {@code silentThreshold} (default 5) flags
 * have accumulated.  Violations decay by 1 flag every
 * {@code decayIntervalMs} (default 5 minutes) of clean gameplay.
 *
 * <p>Lag compensation: the look-vector is sampled over the last
 * {@code HISTORY_TICKS} ticks and matched against the ping-adjusted
 * entity box, tolerating up to one round-trip of latency.
 */
@CheckData(
        name = "TriggerBot",
        stableKey = "grimnatorac.combat.triggerbot",
        description = "Detects trigger-bot via crosshair-to-attack timing and ray-trace accuracy",
        decay = 0,        // we manage our own silent decay
        setback = Integer.MAX_VALUE,
        experimental = false
)
public class TriggerBot extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Number of past rotation snapshots kept for lag-compensated LOS checks. */
    private static final int HISTORY_TICKS = 10;

    /** Max ray length for AABB intersection tests (slightly beyond max vanilla reach). */
    private static final double RAY_LENGTH = 6.5;

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /**
     * Number of silent flags that must accumulate before an admin alert fires.
     * Default: 5.
     */
    private int silentThreshold;

    /**
     * Milliseconds of clean gameplay before one flag decays.
     * Default: 300 000 (5 minutes).
     */
    private long decayIntervalMs;

    /**
     * Maximum ticks between crosshair entering the target's AABB and the
     * attack packet arriving for it to be considered a triggerbot flag.
     * Default: 1.
     */
    private int triggerWindowTicks;

    // -----------------------------------------------------------------------
    // Per-player state — rotation history
    // -----------------------------------------------------------------------

    /** Circular buffer of rotation snapshots (yaw, pitch, tick). */
    private final Deque<RotationSnapshot> rotationHistory = new ArrayDeque<>(HISTORY_TICKS + 1);

    /** Current server-side tick counter (incremented on each flying packet). */
    private int tick = 0;

    // -----------------------------------------------------------------------
    // Per-player state — silent flag system
    // -----------------------------------------------------------------------

    /** Silent violation counter for the triggerbot sub-check. */
    private int silentViolationsTrigger = 0;

    /** Silent violation counter for the ray-miss sub-check. */
    private int silentViolationsRayMiss = 0;

    /** Timestamp of the last flag (any kind) — used for decay scheduling. */
    private long lastFlagMs = 0L;

    /** Timestamp of the last clean (no-flag) attack — used for decay. */
    private long lastCleanAttackMs = 0L;

    // -----------------------------------------------------------------------
    // Per-player state — triggerbot timing
    // -----------------------------------------------------------------------

    /**
     * Maps entity ID → tick at which the player's crosshair first entered
     * that entity's AABB.  Cleared when the LOS exits the box or after
     * {@code HISTORY_TICKS} ticks.
     */
    private final ConcurrentHashMap<Integer, Integer> losEnterTick = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public TriggerBot(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // --- Track rotation every tick ---
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleFlyingPacket(event);
            return;
        }

        // --- Attack / interact-attack packets ---
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack pkt = new WrapperPlayClientAttack(event);
            handleAttack(pkt.getEntityId());
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity pkt = new WrapperPlayClientInteractEntity(event);
            if (pkt.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(pkt.getEntityId());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Flying packet — update tick, rotation history, and LOS tracking
    // -----------------------------------------------------------------------

    private void handleFlyingPacket(PacketReceiveEvent event) {
        // Skip exempted states
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            rotationHistory.clear();
            losEnterTick.clear();
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        tick++;

        // Record current rotation snapshot
        float yaw   = flying.hasRotationChanged() ? flying.getLocation().getYaw()   : player.yaw;
        float pitch = flying.hasRotationChanged() ? flying.getLocation().getPitch() : player.pitch;

        rotationHistory.addLast(new RotationSnapshot(tick, yaw, pitch,
                player.x, player.y, player.z));
        while (rotationHistory.size() > HISTORY_TICKS) {
            rotationHistory.pollFirst();
        }

        // Decay flag counters if the player has been clean long enough
        applyFlagDecay();

        // Update LOS-tracking for all nearby entities
        updateLosTracking(yaw, pitch);
    }

    /**
     * For each entity currently in range, check if the player's crosshair
     * ray intersects its AABB.  Record the first tick of intersection in
     * {@code losEnterTick}.  Remove entries whose box is no longer hit.
     */
    private void updateLosTracking(float yaw, float pitch) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

        List<double[]> eyeHeights = collectEyeHeights();
        Vector3dm lookVec = ReachUtils.getLook(player, yaw, pitch);
        lookVec.multiply(RAY_LENGTH);

        for (Int2ObjectMap.Entry<PacketEntity> entry : player.compensatedEntities.entityMap.int2ObjectEntrySet()) {
            int entityId = entry.getIntKey();
            PacketEntity entity = entry.getValue();

            if (entity == null || entity.isDead || !entity.isLivingEntity) continue;

            SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
            boolean hit = false;

            for (double[] eyeH : eyeHeights) {
                Vector3d eye = new Vector3d(player.x, player.y + eyeH[0], player.z);
                Vector3d end = eye.add(lookVec.getX(), lookVec.getY(), lookVec.getZ());

                if (ReachUtils.isVecInside(box, eye)
                        || ReachUtils.calculateIntercept(box, eye, end).first() != null) {
                    hit = true;
                    break;
                }
            }

            if (hit) {
                losEnterTick.putIfAbsent(entityId, tick);
            } else {
                // LOS left this entity — remove so next entry starts fresh
                losEnterTick.remove(entityId);
            }
        }

        // Purge entries for entities that no longer exist
        losEnterTick.entrySet().removeIf(e ->
                !player.compensatedEntities.entityMap.containsKey(e.getKey()));

        // Purge stale entries (LOS was on entity long ago with no attack)
        losEnterTick.entrySet().removeIf(e -> (tick - e.getValue()) > HISTORY_TICKS * 3);
    }

    // -----------------------------------------------------------------------
    // Attack handler — triggerbot timing + ray-miss check
    // -----------------------------------------------------------------------

    private void handleAttack(int entityId) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        if (player.inVehicle()) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        if (entity == null || entity.isDead || !entity.isLivingEntity) return;

        boolean flagged = false;

        // ---- Method B: ray-miss check ----------------------------------------
        // Do any of the lag-compensated look vectors actually intersect the box?
        boolean hasIntercept = checkRayHit(entity);
        if (!hasIntercept) {
            // The player attacked but their crosshair (in all possible recent
            // snapshots) never touched the target's AABB.
            flagged = true;
            accumulateSilentFlag(silentViolationsRayMiss++, "ray-miss eid=" + entityId);
        }

        // ---- Method A: triggerbot timing check -------------------------------
        Integer enterTick = losEnterTick.get(entityId);
        if (enterTick != null) {
            int gap = tick - enterTick;
            // gap == 0 means the same tick the crosshair entered the box the
            // attack fired; gap == 1 is still within our trigger window.
            // IMPORTANT: only flag if the crosshair JUST arrived on the target
            // (gap is small). If the player has been looking at the entity for
            // many ticks already, gap will be large and is NOT suspicious.
            if (gap <= triggerWindowTicks) {
                accumulateSilentFlag(++silentViolationsTrigger,
                        "trigger gap=" + gap + "t eid=" + entityId);
                flagged = true;
            }
        }

        if (!flagged) {
            // Clean attack — record timestamp for decay, and reset the enter-tick
            // to NOW so that subsequent attacks against the same entity (while
            // still looking at it) don't appear as gap=0.
            lastCleanAttackMs = System.currentTimeMillis();
            losEnterTick.put(entityId, tick); // reset, not remove
        }
    }

    /**
     * Returns {@code true} if any lag-compensated look snapshot (within ping
     * window) produces a ray that intersects the entity's AABB.
     */
    private boolean checkRayHit(PacketEntity entity) {
        SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
        // Add a small margin to be generous to high-ping players
        box.expand(0.1);

        int pingTicks = Math.max(1, player.getTransactionPing() / 50 + 2);
        List<double[]> eyeHeights = collectEyeHeights();

        // Check all rotation snapshots within ping compensation window
        Iterator<RotationSnapshot> it = ((ArrayDeque<RotationSnapshot>) rotationHistory).descendingIterator();
        int checked = 0;
        while (it.hasNext() && checked < pingTicks) {
            RotationSnapshot snap = it.next();
            Vector3dm look = ReachUtils.getLook(player, snap.yaw, snap.pitch);
            look.multiply(RAY_LENGTH);

            for (double[] eyeH : eyeHeights) {
                Vector3d eye = new Vector3d(snap.x, snap.y + eyeH[0], snap.z);
                Vector3d end = eye.add(look.getX(), look.getY(), look.getZ());

                if (ReachUtils.isVecInside(box, eye)
                        || ReachUtils.calculateIntercept(box, eye, end).first() != null) {
                    return true;
                }
            }
            checked++;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Silent flag accumulation and alerting
    // -----------------------------------------------------------------------

    /**
     * Increments the appropriate counter and sends a staff alert only when
     * the counter reaches {@code silentThreshold}.  Resets the counter
     * after alerting so the cycle repeats.
     */
    private void accumulateSilentFlag(int currentCount, String verbose) {
        lastFlagMs = System.currentTimeMillis();
        if (currentCount >= silentThreshold) {
            // Fire the real Grim flag+alert to notify staff
            flagAndAlert(verbose);
            // Reset both counters after alerting
            silentViolationsTrigger = 0;
            silentViolationsRayMiss = 0;
        }
        // Do NOT call reward() or flag() directly — we control the VL ourselves.
    }

    /**
     * Reduces both silent counters by 1 once per {@code decayIntervalMs}
     * of clean gameplay (no flags recorded).
     */
    private void applyFlagDecay() {
        if (silentViolationsTrigger == 0 && silentViolationsRayMiss == 0) return;

        long now = System.currentTimeMillis();
        // Use the later of lastFlag and lastCleanAttack as the activity baseline
        long baseline = Math.max(lastFlagMs, lastCleanAttackMs);
        if (baseline > 0 && (now - baseline) >= decayIntervalMs) {
            silentViolationsTrigger  = Math.max(0, silentViolationsTrigger  - 1);
            silentViolationsRayMiss  = Math.max(0, silentViolationsRayMiss  - 1);
            // Advance baseline so decay fires at most once per interval
            lastCleanAttackMs = now;
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /**
     * Returns the set of possible eye heights for the current player state
     * as a list of single-element arrays for uniform iteration.
     */
    private List<double[]> collectEyeHeights() {
        double[] raw = player.getPossibleEyeHeights();
        List<double[]> result = new ArrayList<>(raw.length);
        for (double h : raw) {
            result.add(new double[]{h});
        }
        if (result.isEmpty()) result.add(new double[]{1.62}); // fallback
        return result;
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        this.silentThreshold    = config.getIntElse("TriggerBot.silent-threshold", 5);
        this.decayIntervalMs    = config.getIntElse("TriggerBot.decay-interval-seconds", 300) * 1000L;
        this.triggerWindowTicks = config.getIntElse("TriggerBot.trigger-window-ticks", 1);
    }

    // -----------------------------------------------------------------------
    // Internal record types
    // -----------------------------------------------------------------------

    /** Immutable snapshot of yaw, pitch, and player position at a given tick. */
    private static final class RotationSnapshot {
        final int  tick;
        final float yaw;
        final float pitch;
        final double x, y, z;

        RotationSnapshot(int tick, float yaw, float pitch, double x, double y, double z) {
            this.tick  = tick;
            this.yaw   = yaw;
            this.pitch = pitch;
            this.x     = x;
            this.y     = y;
            this.z     = z;
        }
    }
}
