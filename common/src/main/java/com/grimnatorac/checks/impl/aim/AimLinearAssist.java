package com.grimnatorac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.checks.type.RotationCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

/**
 * AimLinearAssist — Aim assist detection via constant-velocity rotation pattern.
 *
 * <p>Aim assist clients produce unnaturally smooth rotation toward targets:
 * <ol>
 *   <li>Rotation deltas remain constant or very similar across multiple ticks (low acceleration)</li>
 *   <li>Rotation velocity is moderate (not human-like micro-corrections or full snaps)</li>
 *   <li>Pattern persists through combat (attack packets during constant rotation)</li>
 *   <li>Rotation direction doesn't change (no oscillation)</li>
 * </ol>
 *
 * <p>This check flags when:
 * <ul>
 *   <li>Player shows {@code minSuspiciousTicks} consecutive ticks of constant rotation velocity</li>
 *   <li>Standard deviation of rotation deltas is below {@code maxStdDev}</li>
 *   <li>Average rotation speed is between {@code minAvgSpeed} and {@code maxAvgSpeed} deg/tick</li>
 *   <li>At least one attack packet occurs during the pattern</li>
 * </ul>
 *
 * <p>Human players naturally show:
 * <ul>
 *   <li>High variation in rotation speed (stopping, accelerating, micro-corrections)</li>
 *   <li>Direction changes (oscillation around target)</li>
 *   <li>Either very fast flicks or very slow tracking, rarely constant moderate speeds</li>
 * </ul>
 */
@CheckData(
        name = "AimLinearAssist",
        stableKey = "grimnatorac.aim.linear_assist",
        description = "Detects aim assist via unnaturally constant rotation velocity",
        decay = 0.05,
        setback = 15,
        experimental = false
)
public class AimLinearAssist extends Check implements RotationCheck, PacketCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Minimum consecutive ticks of constant rotation to be suspicious. */
    private int minSuspiciousTicks;

    /** Maximum standard deviation of rotation deltas (lower = more constant). */
    private double maxStdDev;

    /** Minimum average rotation speed (deg/tick) to consider. */
    private double minAvgSpeed;

    /** Maximum average rotation speed (deg/tick) to consider. */
    private double maxAvgSpeed;

    /** Whether to require an attack during the pattern. */
    private boolean requireAttack;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /** Yaw from previous tick. */
    private float prevYaw = Float.NaN;

    /** Pitch from previous tick. */
    private float prevPitch = Float.NaN;

    /** Ring buffer of recent rotation deltas (combined |yaw| + |pitch|). */
    private final float[] deltaHistory;

    /** Current position in ring buffer. */
    private int historyIndex = 0;

    /** Number of valid entries in ring buffer. */
    private int historyCount = 0;

    /** Whether an attack occurred during current pattern. */
    private boolean attackDuringPattern = false;

    /** Direction of yaw movement in current pattern (1 = positive, -1 = negative, 0 = none). */
    private int yawDirection = 0;

    /** Direction of pitch movement in current pattern. */
    private int pitchDirection = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public AimLinearAssist(GrimPlayer player) {
        super(player);
        this.deltaHistory = new float[20]; // Max tracking window
    }

    // -----------------------------------------------------------------------
    // RotationCheck
    // -----------------------------------------------------------------------

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.disableGrim) return;
        
        // Skip teleports and vehicle riding
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            resetState();
            return;
        }

        float currentYaw   = rotationUpdate.getTo().getYaw();
        float currentPitch = rotationUpdate.getTo().getPitch();

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

        // Check for direction consistency
        int newYawDir = deltaYaw > 1f ? 1 : (deltaYaw < -1f ? -1 : 0);
        int newPitchDir = deltaPitch > 1f ? 1 : (deltaPitch < -1f ? -1 : 0);

        // If direction changes significantly, reset pattern
        if (historyCount > 0) {
            if (yawDirection != 0 && newYawDir != 0 && yawDirection != newYawDir) {
                resetState();
                return;
            }
            if (pitchDirection != 0 && newPitchDir != 0 && pitchDirection != newPitchDir) {
                resetState();
                return;
            }
        }

        // Update directions
        if (newYawDir != 0) yawDirection = newYawDir;
        if (newPitchDir != 0) pitchDirection = newPitchDir;

        // Add to history
        deltaHistory[historyIndex] = combined;
        historyIndex = (historyIndex + 1) % deltaHistory.length;
        if (historyCount < deltaHistory.length) {
            historyCount++;
        }

        // Need enough samples to analyze
        if (historyCount < minSuspiciousTicks) {
            return;
        }

        // Calculate statistics
        float sum = 0f;
        for (int i = 0; i < historyCount; i++) {
            sum += deltaHistory[i];
        }
        float mean = sum / historyCount;

        // Check if mean is in suspicious range
        if (mean < minAvgSpeed || mean > maxAvgSpeed) {
            return;
        }

        // Calculate standard deviation
        float variance = 0f;
        for (int i = 0; i < historyCount; i++) {
            float diff = deltaHistory[i] - mean;
            variance += diff * diff;
        }
        float stdDev = (float) Math.sqrt(variance / historyCount);

        // Check if pattern is too constant
        if (stdDev <= maxStdDev) {
            // Check attack requirement
            if (requireAttack && !attackDuringPattern) {
                return;
            }

            String verbose = String.format(
                    "const_rot: avg=%.2f°/tick std=%.3f over %d ticks%s",
                    mean, stdDev, historyCount,
                    attackDuringPattern ? " +attack" : ""
            );
            flagAndAlert(verbose);
            resetState();
        }
    }

    // -----------------------------------------------------------------------
    // PacketCheck (for attack detection)
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // Track attacks
        if (isAttackPacket(event)) {
            attackDuringPattern = true;
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
        // Minimum consecutive ticks showing constant rotation.
        // Humans can maintain constant speed for 2-3 ticks max before correcting.
        // Aim assist maintains it much longer.
        // Default: 8 ticks (~400ms) — enough to distinguish from human micro-adjustments.
        this.minSuspiciousTicks = config.getIntElse("AimLinearAssist.min-suspicious-ticks", 8);

        // Maximum standard deviation of rotation deltas.
        // Lower = more constant/robotic. Human tracking has stddev > 1.5°.
        // Default: 1.0° — aim assist typically < 0.5°.
        this.maxStdDev = config.getDoubleElse("AimLinearAssist.max-std-dev", 1.0);

        // Minimum average rotation speed (deg/tick).
        // Too slow = player standing still. Too fast = legitimate flick.
        // Default: 3.0° — enough to be tracking but not instant snap.
        this.minAvgSpeed = config.getDoubleElse("AimLinearAssist.min-avg-speed", 3.0);

        // Maximum average rotation speed (deg/tick).
        // Above this is likely legitimate fast mouse movement.
        // Default: 25.0° — below typical human flick speeds.
        this.maxAvgSpeed = config.getDoubleElse("AimLinearAssist.max-avg-speed", 25.0);

        // Whether to require an attack during the pattern.
        // Reduces false positives from players looking around smoothly.
        // Default: true — only flag if in combat.
        this.requireAttack = config.getBooleanElse("AimLinearAssist.require-attack", true);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void resetState() {
        historyIndex = 0;
        historyCount = 0;
        attackDuringPattern = false;
        yawDirection = 0;
        pitchDirection = 0;
        prevYaw = Float.NaN;
        prevPitch = Float.NaN;
    }
}
