package com.grimnatorac.checks.impl.aim;

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
 * AimSnapRetract — KillAura detection via head-snap-and-retract pattern.
 *
 * <p>KillAura clients often:
 * <ol>
 *   <li>Snap the camera to the target at an inhuman speed (large angle delta in 1 tick)</li>
 *   <li>Send the attack packet</li>
 *   <li>Snap the camera back toward the original look direction within a few ticks</li>
 * </ol>
 *
 * <p>This check flags when ALL of the following are true:
 * <ul>
 *   <li>A rotation packet shows a combined yaw+pitch delta exceeding {@code minSnapAngle} deg/tick</li>
 *   <li>An attack packet arrives within {@code attackWindowTicks} ticks of the snap</li>
 *   <li>Within {@code retractWindowTicks} ticks of the attack, the camera moves back in the
 *       opposite direction at inhuman speed (≥ {@code retractFraction} × snap magnitude)</li>
 * </ul>
 */
@CheckData(
        name = "AimSnapRetract",
        stableKey = "grimnatorac.aim.snap_retract",
        description = "Detects KillAura via inhuman head-snap-to-target and retract pattern",
        decay = 0.05,
        setback = 20,
        experimental = false
)
public class AimSnapRetract extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Minimum combined (|yaw| + |pitch|) delta in degrees/tick to be "inhuman". */
    private double minSnapAngle;

    /** Ticks after a snap in which an attack must arrive to be suspicious. */
    private int attackWindowTicks;

    /** Ticks after an attack in which a retract must occur to be suspicious. */
    private int retractWindowTicks;

    /** Retract must be at least this fraction of the snap magnitude. */
    private double retractFraction;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /** Tick counter — incremented on every flying packet. */
    private int tick = 0;

    /** Yaw at the start of the current tick (from the previous flying packet). */
    private float prevYaw = Float.NaN;

    /** Pitch at the start of the current tick. */
    private float prevPitch = Float.NaN;

    /** Tick at which the last inhuman snap was detected. -1 = none pending. */
    private int snapTick = -1;

    /** Signed yaw delta of the snap (used to determine retract direction). */
    private float snapYawDir = 0f;

    /** Signed pitch delta of the snap. */
    private float snapPitchDir = 0f;

    /** Combined magnitude of the snap. */
    private float snapMagnitude = 0f;

    /** Tick at which the last attack arrived after a snap. -1 = none pending. */
    private int attackTick = -1;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public AimSnapRetract(GrimPlayer player) {
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
            prevYaw = Float.NaN;
            prevPitch = Float.NaN;
            return;
        }

        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

        // Only process packets that include a rotation change
        if (!wrapper.hasRotationChanged()) return;

        float currentYaw   = wrapper.getLocation().getYaw();
        float currentPitch = wrapper.getLocation().getPitch();

        tick++;

        if (Float.isNaN(prevYaw)) {
            prevYaw   = currentYaw;
            prevPitch = currentPitch;
            return;
        }

        // Compute signed deltas
        float deltaYaw   = currentYaw - prevYaw;
        float deltaPitch = currentPitch - prevPitch;

        // Normalise yaw to [-180, 180]
        while (deltaYaw >  180f) deltaYaw -= 360f;
        while (deltaYaw < -180f) deltaYaw += 360f;

        float absDeltaYaw   = Math.abs(deltaYaw);
        float absDeltaPitch = Math.abs(deltaPitch);
        float combined      = absDeltaYaw + absDeltaPitch;

        prevYaw   = currentYaw;
        prevPitch = currentPitch;

        // --- Phase 1: detect inhuman snap ---
        if (combined >= minSnapAngle) {
            // New snap — reset any previous state
            snapTick      = tick;
            snapYawDir    = deltaYaw;
            snapPitchDir  = deltaPitch;
            snapMagnitude = combined;
            attackTick    = -1;
            return;
        }

        // --- Phase 3: detect retract after attack ---
        if (snapTick != -1 && attackTick != -1) {
            int ticksSinceAttack = tick - attackTick;

            if (ticksSinceAttack <= retractWindowTicks) {
                // Retract = moving back in the opposite direction to the snap
                boolean yawRetract   = snapYawDir == 0f
                        || (snapYawDir > 0 && deltaYaw < 0)
                        || (snapYawDir < 0 && deltaYaw > 0)
                        || absDeltaYaw < 1f;
                boolean pitchRetract = snapPitchDir == 0f
                        || (snapPitchDir > 0 && deltaPitch < 0)
                        || (snapPitchDir < 0 && deltaPitch > 0)
                        || absDeltaPitch < 1f;

                // Retract must also be inhuman speed
                boolean retractIsInhuman = combined >= snapMagnitude * retractFraction;

                if (yawRetract && pitchRetract && retractIsInhuman) {
                    String verbose = String.format(
                            "snap=%.1f°/tick attack+%dtick retract=%.1f°/tick",
                            snapMagnitude, attackTick - snapTick, combined
                    );
                    flagAndAlert(verbose);
                    resetState();
                    return;
                }
            } else {
                // Retract window expired — not suspicious
                resetState();
            }
        }

        // Expire stale snap if no attack arrived in time
        if (snapTick != -1 && attackTick == -1 && (tick - snapTick) > attackWindowTicks) {
            resetState();
        }
    }

    private void handleAttack() {
        // Only record the attack if we have a recent snap
        if (snapTick != -1 && (tick - snapTick) <= attackWindowTicks) {
            attackTick = tick;
        }
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
        // Minimum combined (|yaw| + |pitch|) delta in degrees/tick to be "inhuman".
        // Humans can flick ~30-40°/tick at most. KillAura snaps are typically 60-180°+.
        // Default: 45.0 — conservative to avoid false positives on fast mouse flicks.
        this.minSnapAngle      = config.getDoubleElse("AimSnapRetract.min-snap-angle", 45.0);

        // How many ticks after the snap the attack must arrive.
        this.attackWindowTicks = config.getIntElse("AimSnapRetract.attack-window-ticks", 3);

        // How many ticks after the attack the retract must occur.
        this.retractWindowTicks = config.getIntElse("AimSnapRetract.retract-window-ticks", 5);

        // Retract must be at least this fraction of the snap magnitude.
        this.retractFraction   = config.getDoubleElse("AimSnapRetract.retract-fraction", 0.5);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void resetState() {
        snapTick      = -1;
        snapYawDir    = 0f;
        snapPitchDir  = 0f;
        snapMagnitude = 0f;
        attackTick    = -1;
    }
}
