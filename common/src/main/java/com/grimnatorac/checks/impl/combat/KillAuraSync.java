package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * KillAuraSync — Attack-synchronized rotation pattern detection.
 *
 * <p>Many KillAura modes (especially snap-mode) only rotate when attacking:
 * <ol>
 *   <li>Player sends rotation packets ONLY immediately before/during attack packets</li>
 *   <li>Rotation remains static between attacks (no natural drift/micro-corrections)</li>
 *   <li>Attack packets arrive within very tight timing windows after rotation changes</li>
 * </ol>
 *
 * <p>Human players naturally show:
 * <ul>
 *   <li>Continuous rotation adjustments even when not attacking</li>
 *   <li>Rotation packets sent independently of attack timing</li>
 *   <li>Variable delay between rotation change and attack (reaction time varies)</li>
 * </ul>
 *
 * <p>This check flags when:
 * <ul>
 *   <li>Player shows {@code minSyncedAttacks} attacks each preceded by rotation within {@code syncWindowMs}ms</li>
 *   <li>No rotations occur during {@code minStaticMs}ms between attacks (static camera)</li>
 *   <li>Pattern repeats consistently (attack → static → rotate → attack)</li>
 * </ul>
 */
@CheckData(
        name = "KillAuraSync",
        stableKey = "grimnatorac.combat.killaura_sync",
        description = "Detects KillAura via attack-synchronized rotation pattern",
        decay = 0.05,
        setback = 20,
        experimental = false
)
public class KillAuraSync extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Maximum ms between rotation and attack to be considered synchronized. */
    private long syncWindowMs;

    /** Minimum ms of no rotation between attacks to be considered static. */
    private long minStaticMs;

    /** Minimum number of synchronized attacks to flag. */
    private int minSyncedAttacks;

    /** Minimum rotation magnitude (deg) to count as significant rotation. */
    private double minRotationMagnitude;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /** Timestamp (ms) of last significant rotation packet. */
    private long lastRotationTime = 0;

    /** Timestamp (ms) of last attack packet. */
    private long lastAttackTime = 0;

    /** Yaw from previous tick. */
    private float prevYaw = Float.NaN;

    /** Pitch from previous tick. */
    private float prevPitch = Float.NaN;

    /** Count of consecutive attacks that were synchronized with rotation. */
    private int syncedAttackCount = 0;

    /** Whether current rotation was significant enough to count. */
    private boolean hadSignificantRotation = false;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public KillAuraSync(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // --- Flying packets carry rotation ---
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleFlyingPacket(event);
            return;
        }

        // --- Attack packets ---
        if (isAttackPacket(event)) {
            handleAttack();
        }
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    private void handleFlyingPacket(PacketReceiveEvent event) {
        // Skip teleports and vehicle riding
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            resetState();
            return;
        }

        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

        // Only process packets with rotation changes
        if (!wrapper.hasRotationChanged()) return;

        float currentYaw   = wrapper.getLocation().getYaw();
        float currentPitch = wrapper.getLocation().getPitch();

        if (Float.isNaN(prevYaw)) {
            prevYaw   = currentYaw;
            prevPitch = currentPitch;
            return;
        }

        // Compute rotation magnitude
        float deltaYaw   = currentYaw - prevYaw;
        float deltaPitch = currentPitch - prevPitch;

        // Normalise yaw to [-180, 180]
        while (deltaYaw >  180f) deltaYaw -= 360f;
        while (deltaYaw < -180f) deltaYaw += 360f;

        float magnitude = Math.abs(deltaYaw) + Math.abs(deltaPitch);

        prevYaw   = currentYaw;
        prevPitch = currentPitch;

        // Check if rotation is significant
        if (magnitude >= minRotationMagnitude) {
            long now = System.currentTimeMillis();

            // Check if we had a static period between last attack and this rotation
            if (lastAttackTime > 0 && lastRotationTime > 0) {
                long timeSinceAttack = now - lastAttackTime;
                long timeSinceLastRotation = now - lastRotationTime;

                // If we had a long period with no significant rotations after the last attack,
                // this is suspicious (static camera between attacks)
                if (timeSinceAttack >= minStaticMs && timeSinceLastRotation >= minStaticMs) {
                    // Pattern confirmed: attack → static → rotate
                    // Now waiting for attack to complete the cycle
                    hadSignificantRotation = true;
                }
            } else {
                // First rotation in sequence
                hadSignificantRotation = true;
            }

            lastRotationTime = now;
        }
    }

    private void handleAttack() {
        long now = System.currentTimeMillis();

        // Check if this attack is synchronized with recent rotation
        if (hadSignificantRotation && lastRotationTime > 0) {
            long timeSinceRotation = now - lastRotationTime;

            // Attack arrived shortly after rotation
            if (timeSinceRotation <= syncWindowMs) {
                syncedAttackCount++;

                // Check if we have enough synchronized attacks
                if (syncedAttackCount >= minSyncedAttacks) {
                    String verbose = String.format(
                            "synced_pattern: %d attacks synced within %dms of rotation",
                            syncedAttackCount, syncWindowMs
                    );
                    flagAndAlert(verbose);
                    resetState();
                    return;
                }
            } else {
                // Attack too late, pattern broken
                syncedAttackCount = 0;
            }
        } else {
            // Attack without prior rotation in sync window
            syncedAttackCount = 0;
        }

        lastAttackTime = now;
        hadSignificantRotation = false;
    }

    private boolean isAttackPacket(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            return true;
        }
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            return wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        // Maximum ms between rotation and attack to be considered synchronized.
        // Human reaction time is typically 150-300ms.
        // KillAura snap-mode rotates 0-50ms before attack.
        // Default: 100ms — catches automated patterns.
        this.syncWindowMs = config.getLongElse("KillAuraSync.sync-window-ms", 100L);

        // Minimum ms of no significant rotation between attacks.
        // Humans continuously adjust aim even when not attacking.
        // KillAura keeps camera static between attacks.
        // Default: 200ms — enough to distinguish from combat micro-adjustments.
        this.minStaticMs = config.getLongElse("KillAuraSync.min-static-ms", 200L);

        // Minimum number of synchronized attacks to flag.
        // One synchronized attack could be coincidence.
        // Default: 4 — enough to confirm pattern.
        this.minSyncedAttacks = config.getIntElse("KillAuraSync.min-synced-attacks", 4);

        // Minimum rotation magnitude to count as significant.
        // Prevents noise from sub-degree floating point drift.
        // Default: 1.0° — clearly intentional rotation.
        this.minRotationMagnitude = config.getDoubleElse("KillAuraSync.min-rotation-magnitude", 1.0);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void resetState() {
        syncedAttackCount = 0;
        hadSignificantRotation = false;
        lastRotationTime = 0;
        lastAttackTime = 0;
        prevYaw = Float.NaN;
        prevPitch = Float.NaN;
    }
}
