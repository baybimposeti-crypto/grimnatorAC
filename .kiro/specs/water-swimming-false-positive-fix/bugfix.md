# Bugfix Requirements Document

## Introduction

The anticheat incorrectly flags players as suspicious when they hold the space key to swim upward in water, causing the server to repeatedly teleport them back to their previous position (rubber-banding effect). This false positive prevents legitimate water navigation and degrades player experience. The fix must allow normal water swimming while preserving the anticheat's ability to detect actual movement violations in water.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a player is in water AND holds the space key to swim upward THEN the anticheat incorrectly flags the movement as suspicious and applies setbacks (teleports the player back)

1.2 WHEN the player continues holding space in water THEN the setbacks repeat continuously, creating a rubber-banding effect that prevents upward water movement

### Expected Behavior (Correct)

2.1 WHEN a player is in water AND holds the space key to swim upward THEN the anticheat SHALL recognize this as legitimate movement and NOT apply setbacks

2.2 WHEN the player continues holding space in water THEN the anticheat SHALL allow continuous upward swimming without any teleportation or interference

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a player performs actual suspicious movement in water (speed hacking, fly hacking) THEN the anticheat SHALL CONTINUE TO detect and flag these violations with setbacks

3.2 WHEN a player performs normal horizontal swimming or diving in water THEN the anticheat SHALL CONTINUE TO allow these movements without false positives

3.3 WHEN a player transitions between water and air (entering/exiting water) THEN the anticheat SHALL CONTINUE TO properly track and validate these state changes

3.4 WHEN a player uses legitimate movement mechanics outside of water THEN the anticheat SHALL CONTINUE TO validate movements according to existing rules
