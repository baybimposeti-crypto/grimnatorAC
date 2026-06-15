# Water Swimming False Positive Fix - Bugfix Design

## Overview

The anticheat's movement prediction system incorrectly calculates the expected velocity when players hold the space key to swim upward in water. This creates a discrepancy between the predicted movement (calculated in `PredictionEngineWater.transformSwimmingVectors()`) and the actual client movement, resulting in an offset that exceeds the violation threshold. The `OffsetHandler` then flags this as suspicious movement and applies setbacks, causing rubber-banding. The fix will adjust the swimming space velocity calculation to accurately match Minecraft's client-side physics or add appropriate uncertainty tolerance for swimming movements.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when a player presses space while swimming in water, causing offset > threshold
- **Property (P)**: The desired behavior when swimming with space key - no setbacks should occur for legitimate upward swimming
- **Preservation**: Existing anticheat detection for speed/fly hacks in water that must remain unchanged by the fix
- **transformSwimmingVectors()**: The method in `PredictionEngineWater.java` that calculates predicted swimming velocities including space key (jump) input
- **SwimmingSpace**: A `VectorData.VectorType` that represents the predicted movement when space is pressed while swimming
- **Offset**: The distance between predicted velocity and actual movement - when offset >= threshold, violations are flagged
- **lookYAmount**: The Y component of the player's look direction vector, used to calculate swimming space velocity
- **scalar**: The multiplier (0.085 if looking down more than -0.2, otherwise 0.06) applied to swimming space calculations

## Bug Details

### Bug Condition

The bug manifests when a player is swimming in water and presses the space key to swim upward. The `transformSwimmingVectors()` method calculates the expected Y velocity using the formula: `newY = oldY + ((lookYAmount - oldY) * scalar)`, but this prediction does not accurately match the client's actual movement calculation. This results in an offset between predicted and actual movement that exceeds the `threshold` configuration value (default 0.001), triggering the `OffsetHandler` to flag the movement as suspicious and apply setbacks.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type MovementInput
  OUTPUT: boolean
  
  RETURN (input.player.wasEyeInWater OR input.player.isEyeInFluid(WATER) 
          OR input.player.isSwimming OR input.player.wasSwimming)
         AND input.pressingSpace == true
         AND offset(predictedVelocity, actualMovement) >= threshold
         AND NOT isActualCheat(input)
END FUNCTION
```

### Examples

- **Example 1**: Player is swimming horizontally at the surface of water and presses space to swim upward. Expected: smooth upward movement. Actual: player is teleported back to their previous position repeatedly.

- **Example 2**: Player is underwater and holds space continuously to swim to the surface. Expected: continuous upward swimming. Actual: player experiences rubber-banding effect, preventing them from reaching the surface efficiently.

- **Example 3**: Player is swimming upward at a 45-degree angle while pressing space and moving forward. Expected: diagonal upward swimming. Actual: setbacks occur, disrupting the movement flow.

- **Edge Case**: Player rapidly taps space while swimming (alternating between space pressed and not pressed). Expected: intermittent upward boosts. Actual: may trigger false positives on the ticks where space is pressed.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Speed hack detection in water (players moving faster than allowed swimming speed) must continue to be detected and flagged
- Fly hack detection in water (players hovering or ascending without proper swimming mechanics) must continue to be detected and flagged
- Normal horizontal swimming detection must continue to work exactly as before
- Surface swimming without space key must continue to be validated correctly
- Diving and descending in water must continue to be validated correctly
- Transitions between water and air (entering/exiting water) must continue to be properly tracked
- Other movement validation outside of water must remain completely unchanged

**Scope:**
All inputs that do NOT involve pressing space while swimming in water should be completely unaffected by this fix. This includes:
- Swimming without pressing space (horizontal, surface, diving)
- Walking/running on land
- Flying with elytra
- Riding entities in or out of water
- Actual cheating behaviors (speed hacks, fly hacks) should still be detected

## Hypothesized Root Cause

Based on the bug description and code analysis, the most likely issues are:

1. **Incorrect Swimming Space Formula**: The calculation `newY = oldY + ((lookYAmount - oldY) * scalar)` in `transformSwimmingVectors()` may not precisely match Minecraft's client-side swimming physics
   - The scalar values (0.085 when lookYAmount < -0.2, otherwise 0.06) may be incorrect or outdated
   - There may be additional factors (velocity damping, friction) that aren't accounted for in the prediction

2. **Missing Uncertainty Tolerance**: The `UncertaintyHandler.reduceOffset()` method may not provide enough tolerance for swimming space movements
   - Water movement is inherently more complex than ground movement due to fluid dynamics
   - The default threshold (0.001) may be too strict for swimming space calculations
   - No specific offset reduction exists for swimming movements like there is for boats or glitchy blocks

3. **Timing Issues with 0.03 Movement**: Minecraft's 0.03 movement system causes slight desync between client and server
   - Swimming space calculations may be more sensitive to 0.03 timing issues
   - The `couldSkipTick` handling in `addJumpsToPossibilities()` may not properly account for swimming space

4. **Friction/Gravity Application Order**: The `staticVectorEndOfTick()` method applies friction and gravity adjustments, but the order of operations may differ from the client
   - Swimming friction is applied as `multiply(swimmingFriction, 0.8F, swimmingFriction)`
   - The fluid falling adjusted movement may interact incorrectly with swimming space calculations

## Correctness Properties

Property 1: Bug Condition - Swimming Space Does Not Trigger False Positives

_For any_ movement input where a player is swimming in water and presses the space key with legitimate swimming mechanics, the prediction engine SHALL calculate a velocity that matches the actual client movement within the acceptable offset threshold, preventing false positive violations and setbacks.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Cheat Detection in Water Remains Effective

_For any_ movement input where a player is in water but NOT performing legitimate swimming space mechanics (speed hacking, fly hacking, or other violations), the prediction engine SHALL produce the same detection results as the original code, preserving the anticheat's ability to detect and flag actual violations with setbacks.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct, we will implement a multi-pronged fix:

**File**: `common/src/main/java/com/grimnatorac/predictionengine/predictions/PredictionEngineWater.java`

**Method**: `transformSwimmingVectors()`

**Specific Changes**:

1. **Verify and Correct Swimming Space Formula**: Review and test the swimming space calculation formula against vanilla Minecraft client behavior
   - Test with multiple Minecraft client versions (1.13+, 1.14+, 1.21+) to ensure compatibility
   - Potentially adjust the scalar values (0.085 and 0.06) based on empirical testing
   - Consider adding version-specific adjustments if the formula differs across versions

2. **Add Uncertainty Tolerance for Swimming Space**: If formula correction alone is insufficient, add offset tolerance in `UncertaintyHandler.reduceOffset()`
   - Detect when the predicted velocity type is `SwimmingSpace`
   - Apply a small offset reduction (e.g., 0.01-0.03) for swimming space movements
   - This provides tolerance for minor calculation differences while still detecting major violations

3. **Enhance 0.03 Handling for Swimming**: Improve the handling of swimming movements with 0.03 tick skipping
   - Review `addJumpsToPossibilities()` to ensure swimming space works correctly with `couldSkipTick`
   - May need to add special cases for swimming space + 0.03 combinations

4. **Verify End-of-Tick Calculations**: Ensure `staticVectorEndOfTick()` friction and gravity application matches client order
   - Test that `multiply(swimmingFriction, 0.8F, swimmingFriction)` is applied in the correct order
   - Verify `FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement()` works correctly with swimming space

5. **Add Debug Logging**: Temporarily add logging to compare predicted vs actual swimming space vectors
   - Log lookYAmount, scalar, predicted Y, actual Y, and offset for swimming space movements
   - This will help identify the exact source of the discrepancy during testing

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior. Since this is a movement prediction issue, we'll use a combination of manual testing, unit tests, and property-based tests.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Create test scenarios that simulate players swimming in water with space key pressed. Run these tests on the UNFIXED code to observe the offset values and setback triggers. Log the predicted velocity, actual movement, and offset for analysis.

**Test Cases**:
1. **Simple Upward Swimming**: Player at depth Y=50 in water, looking straight up (pitch=90), presses space (will fail on unfixed code - should show offset > 0.001)
2. **Horizontal Swimming with Space**: Player swimming horizontally (pitch=0), presses space (will fail on unfixed code - should show offset for Y component)
3. **Looking Down While Swimming**: Player looking down (pitch=-20), presses space while swimming (will fail on unfixed code - scalar should be 0.085, may show different offset)
4. **Rapid Space Tapping**: Player alternates between pressing and releasing space each tick (may fail intermittently on unfixed code)
5. **Edge of Water Surface**: Player at water surface (Y=62.5) swimming upward with space (may fail on unfixed code due to boundary conditions)

**Expected Counterexamples**:
- Offset values between 0.001 and 0.1 when space is pressed while swimming
- Setbacks triggered repeatedly (multiple times per second) during continuous space holding
- Possible causes: incorrect scalar values, missing uncertainty tolerance, 0.03 timing issues, or friction application order mismatch

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  offset := calculateOffset_fixed(input.predictedVelocity, input.actualMovement)
  offset_reduced := uncertaintyHandler.reduceOffset(offset)
  ASSERT offset_reduced < threshold OR offset_reduced < immediateSetbackThreshold
  ASSERT NOT setbackTriggered(input)
END FOR
```

**Testing Approach**: After implementing the fix, re-run all exploratory test cases and verify that:
- Offset values are reduced below the threshold (0.001) for legitimate swimming space movements
- No setbacks are triggered during normal upward swimming with space key
- Players can swim from underwater to surface smoothly without rubber-banding

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  result_original := predictionEngine_original.checkMovement(input)
  result_fixed := predictionEngine_fixed.checkMovement(input)
  ASSERT result_original.flagged == result_fixed.flagged
  ASSERT result_original.offset == result_fixed.offset
  ASSERT result_original.setbackApplied == result_fixed.setbackApplied
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Capture the baseline behavior of the UNFIXED code for non-swimming-space scenarios, then write property-based tests to verify this behavior is preserved after the fix.

**Test Cases**:
1. **Speed Hack Detection Preservation**: Simulate players moving faster than allowed swimming speed in water - verify that BOTH original and fixed code flag this as suspicious with similar offset values
2. **Fly Hack Detection Preservation**: Simulate players hovering in water without proper swimming mechanics - verify that BOTH original and fixed code detect this violation
3. **Normal Swimming Preservation**: Simulate horizontal swimming, diving, surface swimming without space key - verify that offset calculations are identical between original and fixed code
4. **Ground Movement Preservation**: Simulate walking, running, jumping on land - verify completely unchanged behavior
5. **Other Water Transitions Preservation**: Simulate entering/exiting water, swimming to shore - verify unchanged validation results

### Unit Tests

- Test `transformSwimmingVectors()` with various player states (eye in water, swimming, pitch angles) and verify correct `SwimmingSpace` vector generation
- Test offset calculation for swimming space movements with known predicted and actual velocities
- Test `UncertaintyHandler.reduceOffset()` with swimming space vectors to verify appropriate tolerance is applied
- Test edge cases: player at water surface boundary, rapid pitch changes while swimming, transitioning from ground to water while pressing space

### Property-Based Tests

- Generate random player positions in water with random pitch/yaw angles and verify swimming space calculations produce offset < threshold
- Generate random swimming states (horizontal, vertical, diagonal) with space key pressed and verify no false positives across many scenarios
- Generate random non-swimming-space movements (running, jumping, flying) and verify the fix doesn't affect their detection results
- Generate random cheating behaviors (impossible speeds, impossible Y velocities) and verify they are still detected with similar accuracy

### Integration Tests

- Test full game flow: player jumps into water from shore, swims underwater while holding space, reaches surface, exits water - verify no setbacks occur during swimming
- Test combat scenario: player swimming in water during PvP, rapidly changing pitch while holding space - verify smooth movement without rubber-banding
- Test multiplayer scenario: multiple players swimming in same water body, all holding space - verify no false positives for any player
- Test version compatibility: run tests on 1.13, 1.14, 1.16, 1.20, 1.21+ clients to verify the fix works across versions
