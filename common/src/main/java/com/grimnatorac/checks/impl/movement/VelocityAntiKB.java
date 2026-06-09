package com.grimnatorac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.util.Vector3d;

/**
 * VelocityAntiKB — Anti-knockback detection via velocity modification.
 *
 * <p>Anti-knockback clients reduce or cancel knockback velocity:
 * <ol>
 *   <li>Horizontal velocity reduced below expected amount (X/Z axes)</li>
 *   <li>Vertical velocity reduced or cancelled (Y axis)</li>
 *   <li>Pattern occurs consistently after taking damage/knockback</li>
 *   <li>Velocity reduction exceeds tolerance for network/prediction errors</li>
 * </ol>
 *
 * <p>This check flags when:
 * <ul>
 *   <li>Player receives knockback velocity from server</li>
 *   <li>Actual movement shows velocity reduction exceeding {@code maxReductionPercent}%</li>
 *   <li>Occurs {@code minSuspiciousCount} times in {@code violationWindowTicks} ticks</li>
 * </ul>
 *
 * <p>Note: Uses GrimAC's existing velocity prediction system for comparison.
 */
@CheckData(
        name = "VelocityAntiKB",
        stableKey = "grimnatorac.movement.velocity_antikb",
        description = "Detects anti-knockback via velocity modification",
        decay = 0.1,
        setback = 25,
        experimental = false
)
public class VelocityAntiKB extends Check implements PostPredictionCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Maximum allowed velocity reduction percentage (0-100). */
    private double maxReductionPercent;

    /** Minimum number of suspicious velocity violations to flag. */
    private int minSuspiciousCount;

    /** Tick window to track violations in. */
    private int violationWindowTicks;

    /** Minimum velocity magnitude to check (ignore tiny velocities). */
    private double minVelocityMagnitude;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /** Ring buffer of recent violation flags (1 = violation, 0 = clean). */
    private final int[] violationHistory;

    /** Current position in ring buffer. */
    private int historyIndex = 0;

    /** Number of valid entries in ring buffer. */
    private int historyCount = 0;

    /** Current tick counter. */
    private int tick = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public VelocityAntiKB(GrimPlayer player) {
        super(player);
        this.violationHistory = new int[100]; // Track last 100 ticks (5 seconds)
    }

    // -----------------------------------------------------------------------
    // PostPredictionCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPredictionComplete(final PredictionComplete complete) {
        if (player.disableGrim) return;
        handleMovement();
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    private void handleMovement() {
        // Skip teleports and vehicle riding
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            return;
        }

        tick++;

        // Get predicted velocity (what the player SHOULD move)
        Vector3d predicted = new Vector3d(
                player.predictedVelocity.vector.getX(),
                player.predictedVelocity.vector.getY(),
                player.predictedVelocity.vector.getZ()
        );

        // Get actual movement (what the player DID move)
        Vector3d actual = new Vector3d(
                player.actualMovement.getX(),
                player.actualMovement.getY(),
                player.actualMovement.getZ()
        );

        // Calculate magnitudes
        double predictedMag = Math.sqrt(
                predicted.getX() * predicted.getX() +
                predicted.getY() * predicted.getY() +
                predicted.getZ() * predicted.getZ()
        );

        double actualMag = Math.sqrt(
                actual.getX() * actual.getX() +
                actual.getY() * actual.getY() +
                actual.getZ() * actual.getZ()
        );

        // Skip if predicted velocity is too small (noise)
        if (predictedMag < minVelocityMagnitude) {
            addToHistory(0); // Clean tick
            return;
        }

        // Check if player is affected by knockback
        // GrimAC sets clientVelocity when knockback is applied
        boolean hasKnockback = player.clientVelocity.lengthSquared() > 0.01;

        if (!hasKnockback) {
            addToHistory(0); // Not in knockback state
            return;
        }

        // Calculate velocity reduction percentage
        double reductionPercent = 0.0;
        if (predictedMag > 0) {
            reductionPercent = ((predictedMag - actualMag) / predictedMag) * 100.0;
        }

        // Check if reduction exceeds threshold
        if (reductionPercent > maxReductionPercent) {
            addToHistory(1); // Violation
        } else {
            addToHistory(0); // Clean
        }

        // Check if we have enough violations in window
        if (historyCount >= violationWindowTicks) {
            int violationCount = 0;
            for (int i = 0; i < Math.min(violationWindowTicks, historyCount); i++) {
                int idx = (historyIndex - 1 - i + violationHistory.length) % violationHistory.length;
                violationCount += violationHistory[idx];
            }

            if (violationCount >= minSuspiciousCount) {
                String verbose = String.format(
                        "velocity_mod: %d/%d ticks reduced velocity by %.1f%% (expected=%.2f actual=%.2f)",
                        violationCount, violationWindowTicks, reductionPercent, predictedMag, actualMag
                );
                flagAndAlert(verbose);
                
                // Reset history to avoid spam
                historyCount = 0;
                historyIndex = 0;
            }
        }
    }

    private void addToHistory(int value) {
        violationHistory[historyIndex] = value;
        historyIndex = (historyIndex + 1) % violationHistory.length;
        if (historyCount < violationHistory.length) {
            historyCount++;
        }
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        // Maximum allowed velocity reduction percentage.
        // Network lag, prediction errors, and legit movement can cause 10-20% deviation.
        // Anti-knockback typically reduces by 50-100%.
        // Default: 35% — catches most anti-KB while tolerating lag.
        this.maxReductionPercent = config.getDoubleElse("VelocityAntiKB.max-reduction-percent", 35.0);

        // Minimum number of suspicious velocity reductions.
        // One violation could be lag, multiple violations is suspicious.
        // Default: 3 violations — enough to confirm pattern.
        this.minSuspiciousCount = config.getIntElse("VelocityAntiKB.min-suspicious-count", 3);

        // Tick window to count violations in.
        // Knockback effects last 1-2 seconds typically.
        // Default: 40 ticks (2 seconds) — covers typical knockback duration.
        this.violationWindowTicks = config.getIntElse("VelocityAntiKB.violation-window-ticks", 40);

        // Minimum velocity magnitude to check.
        // Prevents false positives from tiny movements.
        // Default: 0.1 — filters out noise.
        this.minVelocityMagnitude = config.getDoubleElse("VelocityAntiKB.min-velocity-magnitude", 0.1);
    }
}
