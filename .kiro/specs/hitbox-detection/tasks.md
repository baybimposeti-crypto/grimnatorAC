# Implementation Plan: Hitbox Detection

## Overview

This implementation plan breaks down the HitboxCheck feature into discrete coding tasks following the deferred evaluation pattern established in the Reach check. The check will queue interaction packets, evaluate them after look confirmation, and integrate seamlessly with GrimnatorAC's existing combat check suite. Tasks are structured to build incrementally, with early integration of core functionality and validation through property-based and unit tests.

## Tasks

- [x] 1. Create HitboxCheck class structure and configuration system
  - [x] 1.1 Implement HitboxCheck class skeleton with Check extension and PacketCheck interface
    - Create `com.grimnatorac.checks.impl.combat.HitboxCheck.java`
    - Add `@CheckData` annotation with name="HitboxCheck", stableKey="grimnatorac.combat.hitbox_check", decay=0.05, setback=10
    - Define configuration fields: `threshold` (double, default 0.0005), `setbackVl` (double, default 10), `cancelOnFlag` (boolean, default false)
    - Define state field: `queuedInteractions` (Int2ObjectMap<InteractionRecord>)
    - Define private record classes: `InteractionRecord(double eyeX, double eyeY, double eyeZ, float yaw, float pitch)` and `CheckResult(boolean isViolation, double minDistance, String reason)`
    - Add constructor: `public HitboxCheck(GrimPlayer player)`
    - _Requirements: 3.1, 3.2_
  
  - [x] 1.2 Implement configuration loading and validation in onReload method
    - Override `onReload(ConfigManager config)` method
    - Read `threshold` from "HitboxCheck.threshold", clamp to [0.0, 5.0], default 0.0005
    - Read `setbackVl` from "HitboxCheck.setbackvl", clamp to [1, 1000], default 10
    - Read `cancelOnFlag` from "HitboxCheck.cancelOnFlag", default false
    - Log warnings for invalid or out-of-range configuration values
    - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 2. Implement packet reception and interaction queueing
  - [x] 2.1 Implement onPacketReceive for ATTACK and INTERACT_ENTITY packets
    - Override `onPacketReceive(PacketReceiveEvent event)` method
    - Handle `PacketType.Play.Client.ATTACK` packets: extract entity ID using WrapperPlayClientAttack
    - Handle `PacketType.Play.Client.INTERACT_ENTITY` packets: extract entity ID using WrapperPlayClientInteractEntity
    - Call `queueInteraction(entityId)` for both packet types after exemption checks
    - _Requirements: 1.1, 4.1_
  
  - [x] 2.2 Implement early exemption checks in onPacketReceive
    - Check permission node `grimnatorac.exempt.hitboxcheck`: if present, return early
    - Check setback teleport blocking: if `player.getSetbackTeleportUtil().shouldBlockMovement()`, cancel packet and return
    - Fetch entity from `player.compensatedEntities.entityMap`: if null or PacketEntityEnderDragonPart, handle entity tracking logic
    - Check entity.isDead: if true, return early without queueing
    - Check player gamemode: if CREATIVE or SPECTATOR, return early
    - Check player vehicle: if `player.inVehicle()`, return early
    - Check entity type: if BOAT, CHEST_BOAT, or SHULKER, return early
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 3.5, 5.4_
  
  - [x] 2.3 Implement queueInteraction method with queue-flooding protection
    - Check queue size: if `queuedInteractions.size() >= 10`, cancel packet and return without queueing
    - Create InteractionRecord with current player eye position (player.x, player.y, player.z) and look angles (player.yaw, player.pitch)
    - Store record in `queuedInteractions` map with entityId as key (overwrites previous entry for same entity)
    - _Requirements: 4.1, 4.4_

- [x] 3. Implement deferred evaluation and movement update handling
  - [x] 3.1 Add movement update detection to onPacketReceive
    - Check if packet is movement update using `isUpdate(event.getPacketType())` (flying packets, CLIENT_TICK_END, transactions)
    - When movement update detected, call `evaluateQueuedInteractions()`
    - Clear `queuedInteractions` map after evaluation
    - _Requirements: 4.2_
  
  - [x] 3.2 Implement evaluateQueuedInteractions method
    - Iterate over all entries in `queuedInteractions` map
    - For each (entityId, record) entry:
      - Fetch entity from `player.compensatedEntities.entityMap`
      - If entity is null, skip silently (despawned entity)
      - Call `evaluateInteraction(record, entity)` and store CheckResult
      - If CheckResult.isViolation is true: call `flagAndAlert(verbose)` with formatted message including entity type and miss distance
      - If CheckResult.isViolation is true and `getVl() >= setbackVl`: call `setbackIfAboveSetbackVL()`
      - If CheckResult.isViolation is false: call `reward()` to decay VL
    - _Requirements: 1.2, 1.3, 3.3, 3.4, 4.2_

- [x] 4. Implement core ray-box intersection logic
  - [x] 4.1 Implement evaluateInteraction method with hitbox construction
    - Get canonical hitbox: call `entity.getPossibleCollisionBoxes()` and store result
    - Apply threshold expansion: call `box.expand(threshold)`
    - Check movement threshold condition: if `!player.packetStateData.didLastLastMovementIncludePosition`, call `box.expand(player.getMovementThreshold())`
    - Check ViaVersion 1.8 margin: if ViaVersion available and `Via.getConfig().use1_8HitboxMargin()`, call `box.expand(0.1)`
    - _Requirements: 1.1, 5.1, 5.2, 5.3_
  
  - [x] 4.2 Implement look-direction candidate generation
    - Build 4 candidate look vectors based on client version:
      - Vector 1: `ReachUtils.getLook(player, record.yaw, record.pitch)` (all versions)
      - Vector 2: `ReachUtils.getLook(player, player.lastYaw, record.pitch)` (1.8+ only)
      - Vector 3: `ReachUtils.getLook(player, record.yaw, player.lastPitch)` (1.9+ only)
      - Vector 4: `ReachUtils.getLook(player, player.lastYaw, player.lastPitch)` (1.9+ only)
    - For 1.7 clients: use only Vector 1
    - For 1.8 clients: use Vectors 1 and 2
    - For 1.9+ clients: use all 4 vectors
    - _Requirements: 1.1, 4.3_
  
  - [x] 4.3 Implement ray-box intersection testing for all candidates
    - Get possible eye heights: call `player.getPossibleEyeHeights()`
    - Initialize minDistance to Double.MAX_VALUE and hitFound to false
    - For each (lookVector, eyeHeight) combination:
      - Build eye position: `Vector3d(record.eyeX, record.eyeY + eyeHeight, record.eyeZ)`
      - Check if eye inside box: call `ReachUtils.isVecInside(box, eyePos)` — if true, return CheckResult(false, 0, "ok") immediately
      - Build end position: `eyePos + lookVector * 6.5`
      - Calculate intercept: call `ReachUtils.calculateIntercept(box, eyePos, endPos)`
      - If intercept found: set hitFound to true, update minDistance to min of current and distance to intercept
    - After all combinations tested:
      - If hitFound is true: return CheckResult(false, minDistance, "ok")
      - If hitFound is false: compute min distance to box, return CheckResult(true, minDistance, "miss")
    - _Requirements: 1.1, 1.2, 1.3, 4.3, 5.5_

- [x] 5. Implement violation handling and packet cancellation
  - [x] 5.1 Implement verbose message formatting in evaluateQueuedInteractions
    - Format verbose string: `"type=" + entity.getType().getName().getKey() + " miss=" + String.format("%.4f", checkResult.minDistance)`
    - Pass formatted verbose to `flagAndAlert(verbose)`
    - _Requirements: 3.3_
  
  - [x] 5.2 Implement packet cancellation when cancelOnFlag enabled
    - In `queueInteraction` method: if queue-flooding detected (size >= 10), call `event.setCancelled(true)` and `player.onPacketCancel()`
    - Add packet cancellation buffer logic similar to Reach check's `cancelBuffer` pattern
    - When violation flagged and `cancelOnFlag` is true and platform supports cancellation: mark interaction for cancellation
    - _Requirements: 2.4_

- [x] 6. Register HitboxCheck in CheckManager
  - [x] 6.1 Add HitboxCheck instantiation to CheckManager
    - Locate CheckManager class in `com.grimnatorac.manager`
    - Find combat check instantiation section (near Reach and SelfInteract)
    - Add HitboxCheck instantiation: `new HitboxCheck(player)`
    - Ensure check receives packet events through PacketCheck dispatch
    - _Requirements: 3.6_

- [ ] 7. Add HitboxCheck configuration to config.yml
  - [x] 7.1 Create default HitboxCheck configuration block
    - Add configuration block to `config.yml` (or relevant config file):
      ```yaml
      HitboxCheck:
        threshold: 0.0005
        setbackvl: 10
        cancelOnFlag: false
        decay: 0.05
        displayname: "HitboxCheck"
        description: "Detects hitbox expansion cheats"
      ```
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [~] 8. Checkpoint - Verify basic integration and manual testing
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Write unit tests for configuration and exemption logic
  - [~] 9.1 Write unit tests for configuration loading and validation
    - Test threshold clamping: values < 0.0 clamp to 0.0005, values > 5.0 clamp to 0.0005
    - Test setbackvl clamping: values < 1 clamp to 10, values > 1000 clamp to 10
    - Test missing config keys use defaults: threshold=0.0005, setbackvl=10, cancelOnFlag=false
    - Test config reload updates check state without restart
    - Test invalid number values log warnings and use defaults
    - _Requirements: 2.1, 2.2, 2.5_
  
  - [~] 9.2 Write unit tests for exemption checks
    - Test CREATIVE mode bypasses check
    - Test SPECTATOR mode bypasses check
    - Test player riding vehicle bypasses check
    - Test dead entity bypasses check
    - Test BOAT entity type bypasses check
    - Test CHEST_BOAT entity type bypasses check
    - Test SHULKER entity type bypasses check
    - Test PacketEntityEnderDragonPart bypasses check
    - Test END_CRYSTAL is NOT exempt
    - Test permission node `grimnatorac.exempt.hitboxcheck` bypasses check
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 3.5, 5.4_

- [ ] 10. Write unit tests for queue management and deferred evaluation
  - [~] 10.1 Write unit tests for queue management
    - Test queue accepts up to 10 entries without cancellation
    - Test 11th entry cancels packet and is discarded
    - Test queue clears after evaluation phase
    - Test multiple packets for same entity overwrites queue entry (only last is kept)
    - Test despawned entity in queue skips evaluation without flagging
    - _Requirements: 4.4, 4.2_
  
  - [~] 10.2 Write unit tests for movement update triggers
    - Test flying packet triggers evaluation
    - Test CLIENT_TICK_END triggers evaluation on 1.21.2+ when no flying packet sent
    - Test transaction packet triggers evaluation
    - Test queue is cleared after evaluation completes
    - _Requirements: 4.2_

- [ ] 11. Write unit tests for ray-box intersection logic
  - [~] 11.1 Write unit tests for ray intersection detection
    - Test eye position inside box returns clean (CheckResult.isViolation = false)
    - Test ray intersecting expanded box returns clean
    - Test ray missing all box faces returns violation (CheckResult.isViolation = true)
    - Test all 4 look-direction combinations tested for 1.9+ clients
    - Test 2 look-direction combinations tested for 1.8 clients
    - Test 1 look-direction combination tested for 1.7 clients
    - Test multiple eye heights are all tested
    - _Requirements: 1.1, 1.3, 4.3, 5.5_
  
  - [~] 11.2 Write unit tests for hitbox expansion logic
    - Test threshold expansion applied to canonical box
    - Test movement threshold expansion applied when `didLastLastMovementIncludePosition` is false
    - Test ViaVersion 1.8 margin (0.1) applied when config enabled
    - Test ViaVersion margin not applied when config disabled
    - Test getPossibleCollisionBoxes union used for canonical hitbox
    - _Requirements: 5.1, 5.2, 5.3_

- [ ] 12. Write unit tests for violation handling
  - [~] 12.1 Write unit tests for violation flagging and setback
    - Test violation increments VL
    - Test violation calls flagAndAlert with formatted verbose
    - Test VL >= setbackvl triggers setbackIfAboveSetbackVL call
    - Test reward() decrements VL on clean hit
    - Test verbose format: "type=ENTITY_TYPE miss=0.1234"
    - Test setback falls back to alert if setbackIfAboveSetbackVL returns false
    - _Requirements: 1.2, 1.3, 3.3, 3.4_
  
  - [~] 12.2 Write unit tests for packet cancellation
    - Test packet cancelled when queue size >= 10
    - Test packet cancelled when cancelOnFlag enabled and violation flagged
    - Test onPacketCancel called after packet cancellation
    - _Requirements: 2.4_

- [ ] 13. Write integration tests for end-to-end interaction flows
  - [~] 13.1 Write integration tests for packet flow
    - Test ATTACK packet queues interaction and evaluates on next flying packet
    - Test INTERACT_ENTITY packet queues and evaluates on next flying packet
    - Test evaluation uses confirmed look direction from movement packet
    - Test check cooperates with Reach check (both can flag same interaction independently)
    - _Requirements: 4.1, 4.2_
  
  - [~] 13.2 Write integration tests for entity tracking
    - Test check uses getPossibleCollisionBoxes for interpolation-aware boxes
    - Test check handles entity movement between queue and evaluation
    - Test check handles entity despawn between queue and evaluation (silent skip)
    - _Requirements: 4.2, 5.1_

- [ ] 14. Write integration tests for multi-player scenarios
  - [~] 14.1 Write integration tests for player isolation
    - Test check state isolated per player (separate queues)
    - Test VL counters isolated per player
    - Test multiple concurrent players attacking different entities
    - Test multiple concurrent players attacking same entity
    - _Requirements: 3.6_

- [ ] 15. Write integration tests for protocol version handling
  - [~] 15.1 Write integration tests for protocol versions
    - Test 1.7 clients use single look snapshot
    - Test 1.8 clients use 2 look snapshots
    - Test 1.9+ clients use 4 look snapshots
    - Test 1.21.2+ CLIENT_TICK_END fallback when no flying packet sent
    - _Requirements: 4.2, 4.3_

- [~] 16. Final checkpoint - Verify all tests pass and feature is complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP delivery
- Each task references specific requirements (e.g., _Requirements: 1.1, 2.3_) for traceability
- The implementation follows the established Reach check pattern for consistency
- Testing strategy includes unit tests, integration tests, and protocol version tests
- Queue-flooding protection (max 10 entries) prevents memory exhaustion exploits
- Deferred evaluation minimizes false positives from look-direction uncertainty
- The check integrates with existing Check framework (flagging, alerting, permissions, setback)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "6.1", "7.1"] },
    { "id": 2, "tasks": ["2.1", "2.2"] },
    { "id": 3, "tasks": ["2.3", "3.1"] },
    { "id": 4, "tasks": ["3.2", "4.1"] },
    { "id": 5, "tasks": ["4.2"] },
    { "id": 6, "tasks": ["4.3"] },
    { "id": 7, "tasks": ["5.1", "5.2"] },
    { "id": 8, "tasks": ["9.1", "9.2", "10.1"] },
    { "id": 9, "tasks": ["10.2", "11.1", "11.2"] },
    { "id": 10, "tasks": ["12.1", "12.2"] },
    { "id": 11, "tasks": ["13.1", "13.2"] },
    { "id": 12, "tasks": ["14.1", "15.1"] }
  ]
}
```
