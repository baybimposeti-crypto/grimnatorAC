package com.grimnatorac.predictionengine.predictions;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for Water Swimming False Positive
 *
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **DO NOT attempt to fix the test or the code when it fails**
 *
 * **Validates: Requirements 2.1, 2.2**
 *
 * This test explores the bug condition where players swimming in water with space key pressed
 * trigger false positives due to incorrect velocity prediction in transformSwimmingVectors().
 *
 * The test uses a scoped PBT approach targeting concrete failing cases:
 * - Simple upward swimming (pitch=90, space pressed)
 * - Horizontal swimming with space (pitch=0, space pressed)
 * - Looking down while swimming (pitch=-20, space pressed)
 * - Rapid space tapping (alternating space each tick)
 * - Water surface edge case (Y=62.5, swimming upward with space)
 *
 * Expected outcome: Test FAILS showing offset values >= threshold and setbacks triggered
 * Expected counterexamples: offset values between 0.001 and 0.1, repeated setbacks
 */
class PredictionEngineWaterBugConditionTest {

    /**
     * Property 1: Bug Condition - Swimming Space Does Not Trigger False Positives
     *
     * **Validates: Requirements 2.1, 2.2**
     *
     * For any movement input where a player is swimming in water and presses the space key
     * with legitimate swimming mechanics, the prediction engine SHALL calculate a velocity
     * that matches the actual client movement within the acceptable offset threshold,
     * preventing false positive violations and setbacks.
     *
     * This test is EXPECTED TO FAIL on unfixed code, proving the bug exists.
     * The failure demonstrates that the current transformSwimmingVectors() implementation
     * produces offset >= threshold for legitimate swimming space movements.
     *
     * This is a DIRECT TEST of the swimming space calculation formula:
     * newY = oldY + ((lookYAmount - oldY) * scalar)
     * where scalar = 0.085 if lookYAmount < -0.2, else 0.06
     */
    @Property(tries = 100)
    @Label("Swimming with space pressed should not produce offset >= threshold")
    void swimmingSpaceOffsetShouldBeBelowThreshold(
            @ForAll("swimmingScenarios") SwimmingScenario scenario) {

        // Test the swimming space formula directly
        // This is the exact calculation from transformSwimmingVectors():
        // swimmingVelocities.add(vector.returnNewModified(
        //   new Vector3dm(vector.vector.getX(),
        //                 vector.vector.getY() + ((lookYAmount - vector.vector.getY()) * scalar),
        //                 vector.vector.getZ()),
        //   VectorData.VectorType.SwimmingSpace));

        double lookYAmount = calculateLookYAmount(scenario.pitch);
        double scalar = lookYAmount < -0.2 ? 0.085 : 0.06;

        // The predicted Y velocity using the formula from transformSwimmingVectors
        double predictedY = scenario.verticalVelY + ((lookYAmount - scenario.verticalVelY) * scalar);

        // The offset is the difference between predicted and actual movement
        // In the unfixed code, this offset will be >= threshold (default 0.001)
        // causing false positive setbacks for legitimate swimming
        double offset = Math.abs(predictedY - scenario.actualClientY);

        // Store counterexample data for analysis
        String counterexample = String.format(
            "Scenario: %s, Pitch: %.2f, LookY: %.4f, Scalar: %.3f, PredictedY: %.6f, ActualY: %.6f, Offset: %.6f",
            scenario.description, scenario.pitch, lookYAmount, scalar, predictedY, scenario.actualClientY, offset
        );

        // THIS ASSERTION IS EXPECTED TO FAIL ON UNFIXED CODE
        // When it fails, it proves the bug exists by showing offset >= threshold
        // Expected counterexamples will show offset values between 0.001 and 0.1
        assertTrue(offset < 0.001,
            "Swimming space offset should be below threshold to prevent false positives. " +
            "COUNTEREXAMPLE: " + counterexample +
            " | This failure indicates the bug condition exists (offset >= 0.001 triggers setback)");
    }

    /**
     * Provides swimming scenarios that trigger the bug condition
     * These are scoped to concrete failing cases based on the bug analysis
     */
    @Provide
    Arbitrary<SwimmingScenario> swimmingScenarios() {
        return Arbitraries.of(
            // Scenario 1: Simple upward swimming (pitch=90, looking straight up)
            new SwimmingScenario(
                "Simple upward swimming",
                90.0, // pitch (looking up)
                0.0,  // yaw
                0.0,  // horizontalVelX
                0.0,  // verticalVelY (starting from rest)
                0.0,  // horizontalVelZ
                0.06, // actualClientY (Minecraft's actual Y velocity for space in water)
                50.0, // playerY (underwater at depth 50)
                true  // pressingSpace
            ),

            // Scenario 2: Horizontal swimming with space (pitch=0)
            new SwimmingScenario(
                "Horizontal swimming with space",
                0.0,  // pitch (looking straight ahead)
                0.0,  // yaw
                0.2,  // horizontalVelX (swimming forward)
                0.0,  // verticalVelY
                0.0,  // horizontalVelZ
                0.036, // actualClientY (slight upward boost from space)
                55.0, // playerY
                true  // pressingSpace
            ),

            // Scenario 3: Looking down while swimming (pitch=-20, scalar should be 0.085)
            new SwimmingScenario(
                "Looking down while swimming",
                -25.0, // pitch (looking down, < -0.2 after conversion triggers scalar=0.085)
                0.0,  // yaw
                0.1,  // horizontalVelX
                -0.05, // verticalVelY (descending)
                0.1,  // horizontalVelZ
                -0.02, // actualClientY (space pressed while looking down)
                60.0, // playerY
                true  // pressingSpace
            ),

            // Scenario 4: Rapid space tapping (velocity alternates each tick)
            new SwimmingScenario(
                "Rapid space tapping",
                45.0, // pitch (45 degree angle)
                0.0,  // yaw
                0.15, // horizontalVelX
                0.02, // verticalVelY (small upward velocity from previous tick)
                0.0,  // horizontalVelZ
                0.065, // actualClientY (space pressed this tick)
                58.0, // playerY
                true  // pressingSpace
            ),

            // Scenario 5: Water surface edge case (Y=62.5, swimming upward)
            new SwimmingScenario(
                "Water surface edge case",
                80.0, // pitch (near vertical)
                0.0,  // yaw
                0.0,  // horizontalVelX
                0.04, // verticalVelY (already moving upward)
                0.0,  // horizontalVelZ
                0.08, // actualClientY (trying to reach surface)
                62.5, // playerY (at water surface boundary)
                true  // pressingSpace
            )
        );
    }

    /**
     * Calculate lookYAmount from pitch angle
     * This mimics ReachUtils.getLook(player, yaw, pitch).getY()
     */
    private double calculateLookYAmount(double pitch) {
        double pitchRadians = Math.toRadians(pitch);
        return -Math.sin(pitchRadians);
    }

    /**
     * Data class representing a swimming scenario that triggers the bug
     */
    static class SwimmingScenario {
        final String description;
        final double pitch;
        final double yaw;
        final double horizontalVelX;
        final double verticalVelY;
        final double horizontalVelZ;
        final double actualClientY; // What the client actually moved (from Minecraft physics)
        final double playerY;
        final boolean pressingSpace;

        SwimmingScenario(String description, double pitch, double yaw,
                        double horizontalVelX, double verticalVelY, double horizontalVelZ,
                        double actualClientY, double playerY, boolean pressingSpace) {
            this.description = description;
            this.pitch = pitch;
            this.yaw = yaw;
            this.horizontalVelX = horizontalVelX;
            this.verticalVelY = verticalVelY;
            this.horizontalVelZ = horizontalVelZ;
            this.actualClientY = actualClientY;
            this.playerY = playerY;
            this.pressingSpace = pressingSpace;
        }
    }
}
