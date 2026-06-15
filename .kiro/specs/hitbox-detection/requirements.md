# Requirements Document

## Introduction

This feature adds a hitbox expansion detection check to the GrimnatorAC anti-cheat plugin. Certain cheats (commonly called "hitbox", "hitbox expander", or "criticals hitbox") expand the server-side bounding box of target entities on the client, making it trivially easy to hit targets that are outside the normal interaction range or that the player is not actually looking at. The check must detect when a player successfully interacts with an entity using a hitbox that is larger than the server-authoritative hitbox, flag the violation, and optionally cancel or set back the player in accordance with the existing punishment framework. The check integrates with the established `Check` / `CheckData` / `GrimProcessor` framework and sits alongside the existing `Reach` and `Hitboxes` stubs in `com.grimnatorac.checks.impl.combat`.

---

## Glossary

- **HitboxCheck**: The new check class implementing hitbox expansion detection, located at `com.grimnatorac.checks.impl.combat.HitboxCheck`.
- **Canonical_Hitbox**: The server-authoritative bounding box for an entity, derived from `BoundingBoxSize.getWidth` / `BoundingBoxSize.getHeight` with applicable scale attributes applied via `GetBoundingBox`.
- **Interaction_Ray**: The directed line segment from the player's eye position along the player's look vector, used to determine whether the look direction actually intersects the Canonical_Hitbox.
- **Threshold**: A configurable tolerance added to the Canonical_Hitbox before performing intersection tests, to absorb network jitter and latency uncertainty.
- **VL**: Violation level — a floating-point counter on each check instance incremented on each flag and decayed over time.
- **Setback**: A server-side teleport issued to a player when VL exceeds the configurable setback threshold, resetting their position to a safe location.
- **PacketEntity**: The plugin's tracked server-side representation of a Minecraft entity, including its interpolated bounding box.
- **CheckManager**: The component that instantiates and holds all check instances per player.
- **GrimPlayer**: The plugin's per-player data object, providing access to position, look angles, entity tracking, and the check manager.

---

## Requirements

### Requirement 1: Miss Detection

**User Story:** As a server administrator, I want the anti-cheat to detect when a player hits an entity without the player's look vector intersecting the entity's server-authoritative hitbox, so that hitbox expansion hacks that widen the target's effective bounding box are caught.

#### Acceptance Criteria

1. WHEN a player sends an attack or interact packet targeting an entity, THE HitboxCheck SHALL compute all candidate Interaction_Rays from all applicable eye-height positions and all look-vector combinations (current yaw+pitch, last yaw+pitch, last yaw+current pitch, and current yaw+last pitch), selecting the candidate set appropriate to the client's reported protocol version.
2. WHEN ALL candidate Interaction_Rays fail to intersect the Canonical_Hitbox expanded by the configured Threshold, AND the player's eye position is not already inside the Canonical_Hitbox, THE HitboxCheck SHALL flag the violation by calling `flagAndAlert` and setting the cancel buffer — regardless of whether the target entity is tracked in the compensated entity map.
3. WHEN ANY candidate Interaction_Ray intersects the Canonical_Hitbox expanded by the configured Threshold, THE HitboxCheck SHALL reward the player by calling `reward()` to decay the violation level.
4. WHILE a player is in CREATIVE or SPECTATOR game mode, THE HitboxCheck SHALL not flag any violation.
5. WHILE a player is riding a vehicle, THE HitboxCheck SHALL not flag any violation.
6. IF the target entity is dead (isDead == true), THEN THE HitboxCheck SHALL not flag any violation, independently of any other exemption conditions.
7. THE HitboxCheck SHALL support all entity types tracked by PacketEntity, including living entities and end crystals, using the same entity type exemption list applied by the existing Reach check; PacketEntityEnderDragonPart SHALL be explicitly excluded from checks.

---

### Requirement 2: Configurable Threshold and Setback

**User Story:** As a server administrator, I want to configure the hitbox tolerance and the VL setback threshold, so that I can tune the check sensitivity to my server's network conditions and player base.

#### Acceptance Criteria

1. THE HitboxCheck SHALL read the threshold value from the configuration key `HitboxCheck.threshold` on load and on config reload, with a default of `0.0005`; valid values are in the range `[0.0, 5.0]` — IF the configured value is outside this range, THEN THE HitboxCheck SHALL clamp to the default and log a warning.
2. THE HitboxCheck SHALL read the setback VL value from the configuration key `HitboxCheck.setbackvl` on load and on config reload, with a default of `10`; valid values are integers in the range `[1, 1000]` — IF the configured value is outside this range, THEN THE HitboxCheck SHALL clamp to the default and log a warning.
3. WHEN the configuration is reloaded, THE HitboxCheck SHALL apply updated threshold and setback values within the same reload operation, before the next check evaluation, without requiring a server restart.
4. WHERE the `HitboxCheck.cancelOnFlag` option is enabled AND packet cancellation is supported by the platform, WHEN a violation is detected, THE HitboxCheck SHALL cancel the interaction packet, including when the configured threshold is `0.0`.
5. IF a configuration value for `HitboxCheck.threshold` or `HitboxCheck.setbackvl` is not a valid number or is absent, THEN THE HitboxCheck SHALL use the default value and log a warning identifying the invalid key.

---

### Requirement 3: Integration with Check Framework

**User Story:** As a plugin developer, I want the hitbox detection check to integrate cleanly with the existing GrimnatorAC check framework, so that it uses established flagging, alerting, punishment, and permission mechanisms without duplicating logic.

#### Acceptance Criteria

1. THE HitboxCheck SHALL extend `Check` and implement `PacketCheck`, following the same pattern as `SelfInteract` and `Reach`, with no duplication of flagging, alerting, or permission logic.
2. THE HitboxCheck SHALL be annotated with `@CheckData`, providing a `name` ("HitboxCheck"), `stableKey` ("grimnatorac.combat.hitbox_check"), `description`, configurable `decay`, and configurable `setback` fields.
3. WHEN a violation is detected, THE HitboxCheck SHALL call `flagAndAlert(verbose)` where `verbose` is a string containing the target entity's type name and the computed minimum miss distance formatted to 4 decimal places (e.g., `"type=ZOMBIE miss=0.0342"`).
4. WHEN a violation is detected and `getVl() >= setbackVl`, THE HitboxCheck SHALL call `setbackIfAboveSetbackVL()`; IF that call returns false or throws, THEN THE HitboxCheck SHALL fall back to the existing staff-alert mechanism.
5. WHILE a player holds the `grimnatorac.exempt.hitboxcheck` permission node, THE HitboxCheck SHALL skip all violation checks and return immediately without calling `flagAndAlert`.
6. THE HitboxCheck SHALL be instantiated in CheckManager in the same location as other combat checks, ensuring it receives `USE_ENTITY` and movement packet events through the standard `PacketCheck` dispatch path.

---

### Requirement 4: Multi-Tick Deferred Evaluation

**User Story:** As a plugin developer, I want hitbox checks to be evaluated after the player's look direction is confirmed for the tick, so that false positives from look-direction uncertainty are minimised.

#### Acceptance Criteria

1. WHEN an attack or interact packet is received, THE HitboxCheck SHALL enqueue a record containing the target entity ID, the player's eye position (x, y, z), and the player's last confirmed yaw and pitch for deferred evaluation on the next movement update; no flagging SHALL occur at packet receipt time.
2. WHEN the movement update packet is processed for the tick, THE HitboxCheck SHALL evaluate all queued records using the now-confirmed look direction, then clear the queue; IF a queued entity has been despawned, THEN that record SHALL be silently discarded without flagging.
3. THE HitboxCheck SHALL test all four candidate look-direction combinations — (currentYaw, currentPitch), (lastYaw, lastPitch), (lastYaw, currentPitch), (currentYaw, lastPitch) — when evaluating a queued interaction; a violation SHALL be flagged only IF none of the four combinations produces a ray that intersects the expanded entity bounding box, AND the stored eye position is not strictly inside the bounding box.
4. IF the deferred queue already contains 10 or more entries when a new attack or interact packet arrives, THEN THE HitboxCheck SHALL discard the new entry and cancel the corresponding packet to prevent queue-flooding exploits.

---

### Requirement 5: False Positive Mitigation

**User Story:** As a server administrator, I want the hitbox check to avoid flagging legitimate players under lag or latency conditions, so that the check does not disrupt normal gameplay.

#### Acceptance Criteria

1. THE HitboxCheck SHALL call `getPossibleCollisionBoxes()` on the target PacketEntity and take the union of all returned bounding boxes as the Canonical_Hitbox, so that entity movement interpolation uncertainty is accounted for.
2. IF `packetStateData.didLastLastMovementIncludePosition` is false for the current tick, THEN THE HitboxCheck SHALL expand the Canonical_Hitbox by the player's `getMovementThreshold()` value on each axis before performing intersection tests.
3. WHERE the ViaVersion 1.8 hitbox margin configuration is active, THE HitboxCheck SHALL add 0.1 to the Canonical_Hitbox on each axis, matching the margin applied by the Reach check.
4. IF the target entity type is Boat, ChestBoat, or Shulker, THEN THE HitboxCheck SHALL return immediately without performing any hitbox computation or flagging, and this exemption SHALL be evaluated before all other checks.
5. THE HitboxCheck SHALL not flag a violation if the player's stored eye position is strictly inside the Canonical_Hitbox (exclusive boundary, i.e., `isVecInside` returns true), as this constitutes a legitimate close-range hit.
