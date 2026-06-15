package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * SmoothAim — Detects smooth killaura that snaps camera to entity head positions
 *
 * <p>This check detects "smooth" killaura modes that try to appear legitimate by:
 * <ul>
 *   <li>Moving camera smoothly toward targets (not instant snaps)</li>
 *   <li>Aiming precisely at entity head hitbox positions</li>
 *   <li>Using small, consistent rotation adjustments</li>
 *   <li>Attacking immediately after rotation completes</li>
 * </ul>
 *
 * <p>Detection patterns:
 * <ol>
 *   <li><b>Precision Snapping</b>: Rotations end within 0.5° of entity head center multiple times</li>
 *   <li><b>Consistent Magnitude</b>: Rotation deltas have low variance (bot-like consistency)</li>
 *   <li><b>Attack Timing</b>: Attacks happen within 100ms after rotation stops</li>
 *   <li><b>Repetition</b>: Pattern repeats 3+ times in short window</li>
 * </ol>
 *
 * <p>Human players show:
 * <ul>
 *   <li>Variable rotation deltas (not perfectly consistent)</li>
 *   <li>Imperfect aim (rarely land exactly on hitbox center)</li>
 *   <li>Variable attack timing (reaction time varies)</li>
 * </ul>
 */
@CheckData(
    name = "SmoothAim",
    stableKey = "grimnatorac.combat.smoothaim",
    description = "Detects smooth killaura with precision head snapping",
    decay = 0.1,
    setback = 15,
    experimental = false
)
public class SmoothAim extends Check implements PacketCheck {

    // Configuration
    private double precisionThreshold = 0.5;  // degrees - how close to entity head center
    private double consistencyThreshold = 0.15; // rotation variance threshold
    private long attackTimingWindow = 100; // ms after rotation stops
    private int minSuspiciousPatterns = 3; // how many times pattern must repeat

    // State tracking
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private long lastRotationTime = 0;
    private long lastAttackTime = 0;
    private int lastAttackedEntityId = -1;

    // Pattern tracking
    private final Deque<RotationSnapshot> recentRotations = new ArrayDeque<>();
    private int suspiciousPatternCount = 0;
    private static final int MAX_ROTATION_HISTORY = 20;

    // Rotation tracking for consistency analysis
    private final Deque<Double> rotationMagnitudes = new ArrayDeque<>();
    private static final int MAX_MAGNITUDE_HISTORY = 10;

    private static class RotationSnapshot {
        final float yaw;
        final float pitch;
        final long timestamp;
        final double deltaYaw;
        final double deltaPitch;

        RotationSnapshot(float yaw, float pitch, long timestamp, double deltaYaw, double deltaPitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
        }
    }

    public SmoothAim(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.precisionThreshold = config.getDoubleElse("SmoothAim.precision-threshold", 0.5);
        this.consistencyThreshold = config.getDoubleElse("SmoothAim.consistency-threshold", 0.15);
        this.attackTimingWindow = config.getIntElse("SmoothAim.attack-timing-window", 100);
        this.minSuspiciousPatterns = config.getIntElse("SmoothAim.min-suspicious-patterns", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(event);
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleRotation(event);
        }
    }

    private void handleRotation(PacketReceiveEvent event) {
        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

        if (!wrapper.hasRotationChanged()) {
            return;
        }

        float yaw = wrapper.getLocation().getYaw();
        float pitch = wrapper.getLocation().getPitch();

        if (Float.isNaN(lastYaw) || Float.isNaN(lastPitch)) {
            lastYaw = yaw;
            lastPitch = pitch;
            lastRotationTime = System.currentTimeMillis();
            return;
        }

        // Calculate rotation delta
        double deltaYaw = Math.abs(yaw - lastYaw);
        double deltaPitch = Math.abs(pitch - lastPitch);

        // Normalize yaw delta for 360° wrap-around
        if (deltaYaw > 180) {
            deltaYaw = 360 - deltaYaw;
        }

        double rotationMagnitude = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        // Track significant rotations (> 0.1°)
        if (rotationMagnitude > 0.1) {
            long now = System.currentTimeMillis();

            // Add to history
            recentRotations.addLast(new RotationSnapshot(yaw, pitch, now, deltaYaw, deltaPitch));
            if (recentRotations.size() > MAX_ROTATION_HISTORY) {
                recentRotations.removeFirst();
            }

            // Track rotation magnitudes for consistency analysis
            rotationMagnitudes.addLast(rotationMagnitude);
            if (rotationMagnitudes.size() > MAX_MAGNITUDE_HISTORY) {
                rotationMagnitudes.removeFirst();
            }

            lastRotationTime = now;
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void handleAttack(PacketReceiveEvent event) {
        if (isExemptPermission()) {
            return;
        }

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        // Only check attack interactions
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        int entityId = wrapper.getEntityId();
        PacketEntity target = player.compensatedEntities.entityMap.get(entityId);

        if (target == null || !target.isLivingEntity) {
            return;
        }

        long now = System.currentTimeMillis();
        lastAttackTime = now;

        // Check if attack happened shortly after rotation stopped
        long timeSinceRotation = now - lastRotationTime;

        if (timeSinceRotation > attackTimingWindow) {
            // Attack too long after rotation, reset
            suspiciousPatternCount = 0;
            return;
        }

        // Check if aim is precisely on entity head
        boolean isPreciseAim = checkPrecisionAim(target);

        if (!isPreciseAim) {
            // Not precise aim, reset counter but don't flag yet
            if (suspiciousPatternCount > 0) {
                suspiciousPatternCount--;
            }
            return;
        }

        // Check rotation consistency (low variance = bot-like)
        boolean isConsistentRotation = checkRotationConsistency();

        if (!isConsistentRotation) {
            // Rotations are too variable (human-like), reset
            if (suspiciousPatternCount > 0) {
                suspiciousPatternCount--;
            }
            return;
        }

        // Both precision and consistency checks passed - suspicious!
        suspiciousPatternCount++;

        // Flag if pattern repeats enough times
        if (suspiciousPatternCount >= minSuspiciousPatterns) {
            double avgMagnitude = rotationMagnitudes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

            double variance = calculateVariance();

            String verbose = String.format("precision=%.2f° consistency=%.3f patterns=%d avgRot=%.2f° timing=%dms",
                precisionThreshold, variance, suspiciousPatternCount, avgMagnitude, timeSinceRotation);

            flagAndAlert(verbose);

            // Decay pattern count slowly to avoid spam
            suspiciousPatternCount = Math.max(0, suspiciousPatternCount - 2);
        }

        lastAttackedEntityId = entityId;
    }

    /**
     * Checks if player's current aim is precisely on the entity's head hitbox center.
     * Returns true if aim is within precisionThreshold degrees of head center.
     */
    private boolean checkPrecisionAim(PacketEntity target) {
        if (Float.isNaN(lastYaw) || Float.isNaN(lastPitch)) {
            return false;
        }

        // Get entity head position using bounding box
        var boundingBox = target.getPossibleCollisionBoxes();
        double entityX = (boundingBox.minX + boundingBox.maxX) / 2.0;
        double entityY = boundingBox.maxY; // Top of hitbox = head position
        double entityZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0;

        // Calculate player eye position
        double[] eyeHeights = player.getPossibleEyeHeights();
        double playerEyeY = player.y + (eyeHeights.length > 0 ? eyeHeights[0] : 1.62);

        // Calculate vector from player to entity head
        double deltaX = entityX - player.x;
        double deltaY = entityY - playerEyeY;
        double deltaZ = entityZ - player.z;

        // Calculate expected yaw and pitch to look at entity head
        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double expectedYaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        double expectedPitch = Math.toDegrees(Math.atan2(-deltaY, distanceXZ));

        // Normalize yaw to [0, 360)
        if (expectedYaw < 0) {
            expectedYaw += 360;
        }

        // Calculate angle difference
        double yawDiff = Math.abs(lastYaw - expectedYaw);
        if (yawDiff > 180) {
            yawDiff = 360 - yawDiff;
        }

        double pitchDiff = Math.abs(lastPitch - expectedPitch);

        // Check if within precision threshold
        return yawDiff <= precisionThreshold && pitchDiff <= precisionThreshold;
    }

    /**
     * Checks if recent rotations show bot-like consistency (low variance).
     * Returns true if rotation magnitudes have low variance (consistent = suspicious).
     */
    private boolean checkRotationConsistency() {
        if (rotationMagnitudes.size() < 3) {
            return false; // Not enough data
        }

        double variance = calculateVariance();

        // Low variance = consistent = bot-like = suspicious
        return variance < consistencyThreshold;
    }

    /**
     * Calculates variance of recent rotation magnitudes.
     * Lower variance indicates more consistent (bot-like) rotations.
     */
    private double calculateVariance() {
        if (rotationMagnitudes.size() < 2) {
            return Double.MAX_VALUE; // Not enough data, return high variance (not suspicious)
        }

        // Calculate mean
        double mean = rotationMagnitudes.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        // Calculate variance
        double variance = rotationMagnitudes.stream()
            .mapToDouble(mag -> Math.pow(mag - mean, 2))
            .average()
            .orElse(Double.MAX_VALUE);

        return variance;
    }
}
