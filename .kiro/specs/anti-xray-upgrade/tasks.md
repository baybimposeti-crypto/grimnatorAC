# Implementation Plan: OreObfuscation (Exposure-Based Anti-Xray)

## Overview

Implement the `OreObfuscation` check in `common/src/main/java/com/grimnatorac/checks/impl/misc/OreObfuscation.java`, register it in `CheckManager.java`, and add unit + property-based tests. The implementation follows the same structural pattern as `HealthObfuscation`.

---

## Tasks

- [x] 1. Create the `OreObfuscation` class skeleton
  - Create `common/src/main/java/com/grimnatorac/checks/impl/misc/OreObfuscation.java`
  - Extend `Check`, implement `PacketCheck`
  - Add `@CheckData` annotation with `name = "OreObfuscation"`, `stableKey = "grimnatorac.misc.ore_obfuscation"`, `description`, `decay = 0`, `setback = Integer.MAX_VALUE`, `experimental = false`
  - Declare `private boolean enabled;` field
  - Add constructor `public OreObfuscation(GrimPlayer player) { super(player); }`
  - Add stub `onReload(ConfigManager config)` that reads `config.getBooleanElse("OreObfuscation.enabled", true)`
  - Add stub `onPacketSend(PacketSendEvent event)` with early return if `!shouldModifyPackets()`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 2. Implement ore classification data structures and helper predicates
  - [x] 2.1 Define ore sets as `private static final Set<StateType>` constants
    - `DEEPSLATE_ORES`: 9 entries — DEEPSLATE_DIAMOND_ORE, DEEPSLATE_EMERALD_ORE, DEEPSLATE_GOLD_ORE, DEEPSLATE_IRON_ORE, DEEPSLATE_COPPER_ORE, DEEPSLATE_LAPIS_ORE, DEEPSLATE_REDSTONE_ORE, DEEPSLATE_COAL_ORE, ANCIENT_DEBRIS
    - `NETHER_ORES`: 2 entries — NETHER_GOLD_ORE, NETHER_QUARTZ_ORE
    - `ORE_SET`: union of DEEPSLATE_ORES, NETHER_ORES, and the 8 normal ores (DIAMOND_ORE, EMERALD_ORE, GOLD_ORE, IRON_ORE, COPPER_ORE, LAPIS_ORE, REDSTONE_ORE, COAL_ORE). Use `Collections.unmodifiableSet` or `Set.of`
    - _Requirements: 2.4, 2.5, 2.6, 3.3_
  - [x] 2.2 Implement `private static boolean isOre(StateType type)`
    - Returns `ORE_SET.contains(type)`
    - _Requirements: 3.2, 3.3_
  - [x] 2.3 Implement `private static WrappedBlockState getFakeBlockState(StateType ore)`
    - Returns DEEPSLATE state if ore is in DEEPSLATE_ORES, NETHERRACK state if in NETHER_ORES, STONE state otherwise
    - Use `StateTypes.DEEPSLATE.createBlockState()` / `StateTypes.STONE.createBlockState()` / `StateTypes.NETHERRACK.createBlockState()` (or the equivalent zero-argument factory used elsewhere in the codebase)
    - _Requirements: 2.1, 2.2, 2.3_
  - [ ]* 2.4 Write unit tests for ore classification
    - Assert `isOre` returns true for all 19 ores and false for STONE, AIR, GRASS_BLOCK
    - Assert `getFakeBlockState` returns the correct type for one representative from each of the three ore categories
    - Assert `ORE_SET` has exactly 19 entries
    - _Requirements: 2.4, 2.5, 2.6, 3.3_
  - [ ]* 2.5 Write property test for fake block selection (Property 3)
    - **Property 3: Fake block selection is correct for all ore categories**
    - **Validates: Requirements 2.1, 2.2, 2.3**
    - Generator: random element from `ORE_SET`
    - Assert `getFakeBlockState(ore).getType()` is DEEPSLATE if ore in DEEPSLATE_ORES, NETHERRACK if in NETHER_ORES, STONE otherwise

- [ ] 3. Implement non-solid neighbor detection
  - [ ] 3.1 Implement `private static boolean isNonSolid(WrappedBlockState state)`
    - Returns true if `state.getGlobalId() == 0` OR `state.getType().isAir()` OR `!state.getType().isBlocking()`
    - _Requirements: 1.4, 1.5_
  - [~] 3.2 Implement `private boolean isExposed(int x, int y, int z)`
    - Checks all 6 face offsets: (0,+1,0), (0,-1,0), (0,0,-1), (0,0,+1), (-1,0,0), (+1,0,0)
    - For each offset, calls `player.compensatedWorld.getBlock(x+dx, y+dy, z+dz)`
    - Short-circuits and returns true as soon as any neighbor `isNonSolid`
    - Returns false only if all 6 neighbors are solid
    - _Requirements: 1.1, 1.4, 1.5_
  - [ ]* 3.3 Write unit tests for `isNonSolid`
    - Test returns true for an air state, a global-ID-0 state, and a glass (non-blocking) state
    - Test returns false for STONE, DEEPSLATE, DIRT
    - _Requirements: 1.4_
  - [ ]* 3.4 Write property test for non-solid determination (Property 2)
    - **Property 2: Non-solid determination is consistent with the three-condition rule**
    - **Validates: Requirements 1.4, 1.5**
    - Generator: sample from the registered StateType values (cover air, full solids, non-full-cube blocks)
    - Assert `isNonSolid(state) == (state.getGlobalId()==0 || state.getType().isAir() || !state.getType().isBlocking())`
  - [ ]* 3.5 Write property test for exposure decision (Property 1)
    - **Property 1: Exposure decision is determined entirely by neighbor solidity**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**
    - Generator: array of 6 random booleans (solid/non-solid), stub `compensatedWorld` to return matching states
    - Assert `isExposed` returns true iff any of the 6 booleans is "non-solid"

- [~] 4. Checkpoint — Ensure all tests pass so far
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement single-block packet handler
  - [~] 5.1 Implement `private void handleSingleBlock(PacketSendEvent event)`
    - Wrap with `WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event)`
    - Read block position via `packet.getBlockPosition()` (x, y, z)
    - Read `WrappedBlockState blockState = packet.getBlockState()`
    - If `blockState.getType().isAir()` → return immediately (air passthrough, Requirement 3.1)
    - If `!isOre(blockState.getType())` → return immediately (non-ore passthrough, Requirement 3.2)
    - If `isExposed(x, y, z)` → return immediately (reveal exposed ore, Requirement 1.2)
    - Otherwise: `packet.setBlockState(getFakeBlockState(blockState.getType()))` then `event.markForReEncode(true)` (Requirement 5.1)
    - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 5.1_
  - [ ]* 5.2 Write unit tests for `handleSingleBlock`
    - Test air block → no modification, no re-encode
    - Test non-ore (STONE) → no modification, no re-encode
    - Test exposed ore (one non-solid neighbor) → no modification, no re-encode
    - Test fully occluded ore → block replaced with correct fake, re-encode called
    - Test `enabled=false` → packet passes through for all ore types
    - _Requirements: 1.2, 1.3, 3.1, 3.2, 5.1, 4.2_
  - [ ]* 5.3 Write property test for non-ore passthrough (Property 4)
    - **Property 4: Non-ore blocks always pass through unchanged**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Generator: random StateType not in ORE_SET (including air types)
    - Build a mock BLOCK_CHANGE event with that type; run through `onPacketSend`
    - Assert resulting block type equals input type and `markForReEncode` was not called
  - [ ]* 5.4 Write property test for exposed ore passthrough (Property 5)
    - **Property 5: Exposed ore blocks are not replaced**
    - **Validates: Requirements 1.2**
    - Generator: random ore from ORE_SET, neighbor configuration with at least one non-solid neighbor
    - Assert resulting block type equals original ore type

- [ ] 6. Implement multi-block packet handler
  - [~] 6.1 Implement `private void handleMultiBlock(PacketSendEvent event)`
    - Wrap with `WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event)`
    - Read block records array via `packet.getBlocks()`
    - Iterate each block record; extract position (x, y, z) and `WrappedBlockState`
    - Apply the same logic as `handleSingleBlock` for each record: skip air, skip non-ore, skip if exposed, else replace state in the record
    - Track a `boolean modified` flag across the loop
    - After the loop: if `modified` → call `packet.setBlocks(blocks)` then `event.markForReEncode(true)` (Requirement 5.2)
    - If not modified → do not call `markForReEncode` (Requirement 5.3)
    - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 5.2, 5.3_
  - [ ]* 6.2 Write unit tests for `handleMultiBlock`
    - Test packet with no ore blocks → no modification, no re-encode
    - Test packet with all exposed ores → no modification, no re-encode
    - Test packet with one fully occluded ore → that entry replaced, re-encode called
    - Test packet with mixed ore/non-ore blocks → only fully occluded ores replaced
    - _Requirements: 1.2, 1.3, 5.2, 5.3_

- [ ] 7. Wire `onPacketSend` dispatch and register the check
  - [~] 7.1 Complete `onPacketSend(PacketSendEvent event)` dispatch logic
    - Early return if `!shouldModifyPackets()`
    - Dispatch to `handleSingleBlock(event)` when `event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE`
    - Dispatch to `handleMultiBlock(event)` when `event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE`
    - _Requirements: 1.1, 4.2, 4.3_
  - [~] 7.2 Register `OreObfuscation` in `CheckManager.java`
    - Add import: `import com.grimnatorac.checks.impl.misc.OreObfuscation;`
    - Add `.put(OreObfuscation.class, new OreObfuscation(player))` to the `packetChecks` map, alongside `HealthObfuscation`
    - _Requirements: 4.1_

- [~] 8. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- `shouldModifyPackets()` (inherited from `Check`) handles the exempt permission, `noModifyPacket` permission, and `disableGrim` flag automatically — no extra guards needed
- `compensatedWorld.getBlock` returns a global-ID-0 state for unloaded chunks; `isNonSolid` handles this with the `getGlobalId() == 0` condition, so no special unloaded-chunk path is needed
- `OreObfuscation` does not flag or alert players — it is a pure packet-modification check with no VL tracking; the `decay = 0` and `setback = Integer.MAX_VALUE` annotation values are consistent with `HealthObfuscation`
- The check does NOT use player coordinates for reveal decisions — no `revealRadius` field, no `isPlayerNear` method, no `reveal-radius` config key (Requirement 6)
- Property tests should use jqwik as the PBT library (`net.jqwik:jqwik` dependency)

## Task Dependency Graph

```json
{
  "waves": [
    { "tasks": ["1"] },
    { "tasks": ["2"] },
    { "tasks": ["3"] },
    { "tasks": ["4"] },
    { "tasks": ["5"] },
    { "tasks": ["6"] },
    { "tasks": ["7"] },
    { "tasks": ["8"] }
  ]
}
```
