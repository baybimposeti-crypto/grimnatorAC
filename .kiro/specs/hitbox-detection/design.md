# Design Document: Hitbox Detection

## Overview

The HitboxCheck feature adds hitbox expansion detection to GrimnatorAC, targeting cheats that artificially expand entity hitboxes client-side to make hits trivially easy. The check detects when a player successfully attacks or interacts with an entity without their look ray intersecting the server-authoritative bounding box, accounting for network uncertainty, interpolation, and client version differences.

The implementation follows the established Check/CheckData/PacketCheck pattern, integrating seamlessly with the existing combat check suite (Reach, SelfInteract). It employs a deferred evaluation model — interactions are queued on packet receipt and evaluated after look direction is confirmed on the next movement update, minimizing false positives from look-direction uncertainty.

Unlike the existing Reach check which measures distance violations, HitboxCheck focuses on ray-intersection failures: cases where **no** candidate look direction produces a ray that intersects the target's expanded bounding box. This distinction catches hitbox expansion hacks while the Reach check catches distance-based reach hacks.

## Architecture

### Component Structure

```
HitboxCheck (extends Check, implements PacketCheck)
├── Configuration (threshold, setbackvl, cancelOnFlag)
├── Deferred Evaluation Queue (entityId → InteractionRecord)
├── Ray-Box Intersection Engine (ReachUtils integration)
└── Violation Handling (flagAndAlert, setback, packet cancellation)
```

### Data Flow

1. **Packet Receipt Phase** (INTERACT_ENTITY / ATTACK packets):
   - Extract target entity ID
   - Apply early exemptions (creative mode, spectator, riding, dead entity, blacklisted types)
   - Queue interaction record containing: entityId, player eye position, current yaw/pitch
   - Apply queue-flooding protection (max 10 entries)
   - Optionally cancel packet immediately if queue is full

2. **Movement Update Phase** (next flying packet / CLIENT_TICK_END):
   - Retrieve queued interaction records
   - For each record:
     - Fetch target entity from compensated entity map
     - If entity despawned, discard silently
     - Build canonical hitbox from `getPossibleCollisionBoxes()`
     - Apply threshold expansion and uncertainty modifiers
     - Test all 4 look-direction combinations (current/last yaw × current/last pitch)
     - If **ALL** rays miss **AND** eye not inside box: flag violation
     - If **ANY** ray hits: reward player (decay VL)
   - Clear queue

3. **Violation Handling**:
   - Call `flagAndAlert(verbose)` with entity type and miss distance
   - Check `getVl() >= setbackVl`: if true, call `setbackIfAboveSetbackVL()`
   - If `cancelOnFlag` enabled and platform supports it: mark next interaction for cancellation

### Integration Points

- **CheckManager**: Instantiates HitboxCheck alongside other combat checks
- **PacketCheck dispatch**: Receives USE_ENTITY (INTERACT_ENTITY, ATTACK) and movement packets
- **ReachUtils**: Provides ray-box intersection (`calculateIntercept`), inside-box test (`isVecInside`), look vector calculation (`getLook`)
- **PacketEntity**: Provides `getPossibleCollisionBoxes()` for interpolation-aware bounding boxes
- **GrimPlayer**: Provides position, yaw/pitch/last snapshots, possible eye heights, movement threshold
- **ConfigManager**: Loads threshold, setbackvl, cancelOnFlag from `config.yml`

## Components and Interfaces

### HitboxCheck Class

```java
package com.grimnatorac.checks.impl.combat;

@CheckData(
    name = "HitboxCheck",
    stableKey = "grimnatorac.combat.hitbox_check",
    description = "Detects hitbox expansion cheats by ray-box intersection failures",
    decay = 0.05,
    setback = 10
)
public class HitboxCheck extends Check implements PacketCheck {
    // Configuration
    private double threshold = 0.0005;
    private double setbackVl = 10;
    private boolean cancelOnFlag = false;
    
    // State
    private final Int2ObjectMap<InteractionRecord> queuedInteractions;
    
    // Methods
    public HitboxCheck(GrimPlayer player);
    public void onPacketReceive(PacketReceiveEvent event);
    public void onReload(ConfigManager config);
    private void queueInteraction(int entityId);
    private void evaluateQueuedInteractions();
    private boolean shouldExempt(PacketEntity entity);
    private CheckResult evaluateInteraction(InteractionRecord record, PacketEntity entity);
}
```

### InteractionRecord

```java
private record InteractionRecord(
    double eyeX,
    double eyeY,
    double eyeZ,
    float yaw,
    float pitch
) {}
```

### CheckResult

```java
private record CheckResult(
    boolean isViolation,
    double minDistance,  // closest approach distance (for verbose)
    String reason        // "miss" or "ok"
) {}
```

### Key Methods

#### `onPacketReceive(PacketReceiveEvent event)`

Handles incoming INTERACT_ENTITY and ATTACK packets:
- Extract entity ID from packet wrapper
- Check exemptions: creative/spectator mode, riding, dead entity, blacklisted types, ender dragon parts
- If exempt: return early
- If queue size >= 10: cancel packet, return (queue-flooding protection)
- Queue interaction record with current eye position and look angles
- Return (deferred evaluation)

Also handles movement update packets (flying packets, CLIENT_TICK_END):
- Call `evaluateQueuedInteractions()`
- Clear queue

#### `evaluateQueuedInteractions()`

Processes all queued interactions after look confirmation:
- For each queued (entityId, record):
  - Fetch entity from `player.compensatedEntities.entityMap`
  - If entity is null: skip silently (despawned)
  - Call `evaluateInteraction(record, entity)`
  - If violation: call `flagAndAlert(verbose)`, check setback threshold
  - If clean: call `reward()`

#### `evaluateInteraction(InteractionRecord record, PacketEntity entity)`

Core hitbox intersection logic:
- Get canonical box: `entity.getPossibleCollisionBoxes()`
- Apply threshold expansion: `box.expand(threshold)`
- Apply movement threshold if `!player.packetStateData.didLastLastMovementIncludePosition`: `box.expand(player.getMovementThreshold())`
- Apply ViaVersion 1.8 margin if active: `box.expand(0.1)`
- Build 4 candidate look directions:
  - `getLook(record.yaw, record.pitch)`
  - `getLook(player.lastYaw, record.pitch)` (1.8+)
  - `getLook(record.yaw, player.lastPitch)` (1.9+)
  - `getLook(player.lastYaw, player.lastPitch)` (1.9+)
- Get possible eye heights: `player.getPossibleEyeHeights()`
- For each (look, eyeHeight) combination:
  - Build eye position: `Vector3d(record.eyeX, record.eyeY + eyeHeight, record.eyeZ)`
  - Check if eye inside box: `ReachUtils.isVecInside(box, eyePos)` — if true, return `CheckResult(false, 0, "ok")`
  - Build end position: `eyePos + look * 6.5`
  - Calculate intercept: `ReachUtils.calculateIntercept(box, eyePos, endPos)`
  - If intercept found: track min distance to intercept
- If **ANY** intercept found: return `CheckResult(false, minDistance, "ok")`
- If **NO** intercept found: compute min distance to box for verbose, return `CheckResult(true, minDistance, "miss")`

#### `shouldExempt(PacketEntity entity)`

Returns true if:
- Entity type is BOAT, CHEST_BOAT, or SHULKER
- Entity is PacketEntityEnderDragonPart
- Entity.isDead == true
- Player gamemode is CREATIVE or SPECTATOR
- Player is riding a vehicle

#### `onReload(ConfigManager config)`

Reloads configuration:
- `threshold = config.getDoubleElse("HitboxCheck.threshold", 0.0005)` — clamp to [0.0, 5.0], default on invalid
- `setbackVl = config.getDoubleElse("HitboxCheck.setbackvl", 10)` — clamp to [1, 1000], default on invalid
- `cancelOnFlag = config.getBooleanElse("HitboxCheck.cancelOnFlag", false)`
- Log warnings for invalid values

## Data Models

### Configuration Schema

```yaml
HitboxCheck:
  threshold: 0.0005        # double [0.0, 5.0] - tolerance added to canonical hitbox
  setbackvl: 10            # double [1, 1000] - VL threshold for setback
  cancelOnFlag: false      # boolean - cancel interaction packets on violation
  decay: 0.05              # double - VL decay per clean hit (from @CheckData)
  displayname: "HitboxCheck"
  description: "Detects hitbox expansion cheats"
```

### Runtime State

- **queuedInteractions**: `Int2ObjectMap<InteractionRecord>` — maps entity ID to deferred interaction record
  - Max size: 10 (hard limit for queue-flooding protection)
  - Cleared every tick after evaluation
  - Persists across multiple packets within same tick

- **violations**: inherited from Check superclass
  - Incremented on `flag()` call
  - Decayed by `decay` on `reward()` call
  - Compared against `setbackvl` for setback trigger

### Entity Exemption List

Mirroring Reach check's blacklist:
- EntityTypes.BOAT
- EntityTypes.CHEST_BOAT
- EntityTypes.SHULKER
- PacketEntityEnderDragonPart (explicit exclusion in code)

End crystals are **not** exempt (same as Reach).

## Error Handling

### Configuration Errors

| Error Condition | Handling |
|----------------|----------|
| `threshold` out of range [0.0, 5.0] | Clamp to 0.0005, log warning |
| `setbackvl` out of range [1, 1000] | Clamp to 10, log warning |
| `threshold` not a number | Use 0.0005, log warning |
| `setbackvl` not a number | Use 10, log warning |
| Config key missing | Use default value, no warning |

### Runtime Errors

| Error Condition | Handling |
|----------------|----------|
| Entity despawned before evaluation | Skip silently (no flag) |
| Queue size >= 10 on new packet | Cancel packet, discard new entry, no flag |
| `setbackIfAboveSetbackVL()` returns false | Fall back to staff alert (already handled by Check superclass) |
| Permission node exempts player | Skip all checks, return early |
| Packet cancellation unsupported by platform | Log once at startup, ignore `cancelOnFlag` |

### Edge Cases

1. **Teleport during queue window**: If player teleports between packet receipt and evaluation, use stored eye position from queue (accurate for the attack moment)

2. **Entity riding**: If target entity is riding another entity when queued, but dismounts before evaluation — still evaluate (entity position interpolation handles this)

3. **ViaVersion protocol translation**: ViaVersion 1.8 margin (0.1) is applied if `Via.getConfig().use1_8HitboxMargin()` returns true — aligns with Reach check behavior

4. **CLIENT_TICK_END fallback**: On 1.21.2+, if player doesn't send movement packet before tick end, use CLIENT_TICK_END as movement update trigger (matches Check superclass logic)

5. **Multiple attacks same entity per tick**: Only last queued interaction is evaluated (map overwrites previous entry) — acceptable tradeoff for simplicity

## Testing Strategy

### Unit Tests

Unit tests will verify specific behaviors and edge cases:

**Configuration Tests**:
- Test threshold clamping (values < 0, > 5.0, default on invalid)
- Test setbackvl clamping (values < 1, > 1000, default on invalid)
- Test config reload updates check state without restart
- Test missing config keys use defaults

**Exemption Tests**:
- Test creative/spectator modes bypass check
- Test riding player bypasses check
- Test dead entity bypasses check
- Test blacklisted entity types (boat, chest boat, shulker) bypass check
- Test ender dragon part bypass check
- Test permission node `grimnatorac.exempt.hitboxcheck` bypasses check
- Test end crystals are **not** exempt

**Queue Management Tests**:
- Test queue accepts up to 10 entries
- Test 11th entry cancels packet and is discarded
- Test queue clears after evaluation phase
- Test multiple packets same entity overwrites queue entry
- Test despawned entity in queue skips evaluation without flagging

**Ray Intersection Tests**:
- Test eye position inside box returns clean (no flag)
- Test ray intersecting expanded box returns clean
- Test ray missing all expanded box faces flags violation
- Test all 4 look-direction combinations are tested (1.9+)
- Test 2 look-direction combinations for 1.8 clients
- Test 1 look-direction for 1.7 clients
- Test multiple eye heights are tested

**Violation Handling Tests**:
- Test violation increments VL and calls alert
- Test VL >= setbackvl triggers setback
- Test reward() decrements VL on clean hit
- Test verbose includes entity type and miss distance

**Movement Threshold Tests**:
- Test `didLastLastMovementIncludePosition == false` expands box by movement threshold
- Test 1.21.2+ uses CLIENT_TICK_END when no movement packet sent

**ViaVersion Tests**:
- Test 1.8 hitbox margin (0.1) applied when ViaVersion config enabled
- Test margin not applied when ViaVersion disabled or on 1.9+ clients

### Integration Tests

Integration tests will validate the check within the full plugin context:

**End-to-End Interaction Tests**:
- Test ATTACK packet queues interaction and evaluates on next flying packet
- Test INTERACT_ENTITY packet queues and evaluates correctly
- Test packet cancellation when `cancelOnFlag` enabled
- Test setback teleport when VL exceeds threshold
- Test check cooperates with Reach check (both can flag same interaction)

**Entity Tracking Integration**:
- Test check uses `getPossibleCollisionBoxes()` for interpolation-aware boxes
- Test check handles entity movement between queue and evaluation
- Test check handles entity despawn between queue and evaluation

**Multi-Player Scenarios**:
- Test check state isolated per player (different queues, VL counters)
- Test check performance with multiple concurrent players attacking

**Protocol Version Tests**:
- Test 1.7 clients use single look snapshot
- Test 1.8 clients use 2 look snapshots
- Test 1.9+ clients use 4 look snapshots
- Test 1.21.2+ CLIENT_TICK_END fallback

### Performance Tests

- Test check overhead with 100 concurrent attacks per tick
- Test queue memory usage stays bounded (max 10 entries)
- Test ray intersection computation completes within tick budget

### Regression Tests

After deployment:
- Monitor false positive rate on production servers
- Collect samples of flagged interactions for manual review
- Validate threshold tuning reduces false positives without missing real cheats
