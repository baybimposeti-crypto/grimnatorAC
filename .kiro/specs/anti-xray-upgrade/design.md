# Design Document: OreObfuscation (Exposure-Based Anti-Xray Upgrade)

## Overview

This document describes the design for the `OreObfuscation` check in GrimnatorAC. The check intercepts outbound `BLOCK_CHANGE` and `MULTI_BLOCK_CHANGE` packets and replaces ore blocks that are fully surrounded by solid blocks with contextually-appropriate fake blocks, so that xray clients cannot see ore positions. Ore blocks that are naturally exposed to air or non-solid neighbors are sent as-is, preventing visual glitches for legitimate players.

The upgrade replaces the old proximity-based reveal model (radius around the player) with an **exposure-based reveal model**: reveal a block if and only if at least one of its six face-adjacent neighbors is non-solid. This is more resistant to xray exploitation because the reveal threshold is not a predictable distance but the actual geometry of the world.

---

## Architecture

The check lives entirely in a single class:

```
common/src/main/java/com/grimnatorac/checks/impl/misc/OreObfuscation.java
```

It follows the same structure as `HealthObfuscation`: it extends `Check`, implements `PacketCheck`, uses `@CheckData` for metadata, and reads configuration in `onReload`.

Registration occurs in `CheckManager.java` inside the `packetChecks` map, alongside `HealthObfuscation` and other packet-intercepting checks.

### Data Flow

```
PacketSendEvent
      │
      ▼
OreObfuscation.onPacketSend(event)
      │
      ├─ shouldModifyPackets() == false → return (pass through)
      │
      ├─ PacketType == BLOCK_CHANGE → handleSingleBlock(event)
      │       │
      │       └─ getBlock() → isOre? → isExposed? → replace or pass
      │
      └─ PacketType == MULTI_BLOCK_CHANGE → handleMultiBlock(event)
              │
              └─ for each block entry → isOre? → isExposed? → replace or pass
                      │
                      └─ if any replaced → setBlocks() + markForReEncode(true)
```

---

## Components and Interfaces

### OreObfuscation (main class)

| Member | Kind | Purpose |
|--------|------|---------|
| `enabled` | `boolean` field | Config toggle — skip all logic when false |
| `onPacketSend(PacketSendEvent)` | Override | Entry point; dispatches to single/multi handlers |
| `handleSingleBlock(PacketSendEvent)` | Private method | Processes `WrapperPlayServerBlockChange` |
| `handleMultiBlock(PacketSendEvent)` | Private method | Processes `WrapperPlayServerMultiBlockChange` |
| `isExposed(int x, int y, int z)` | Private method | Returns true if any of the 6 face neighbors is non-solid |
| `isNonSolid(WrappedBlockState state)` | Private static method | Returns true if the state is air or a non-full-cube block |
| `getFakeBlock(StateType ore)` | Private static method | Returns DEEPSLATE, STONE, or NETHERRACK based on ore category |
| `isOre(StateType type)` | Private static method | Returns true if the StateType is in the Ore Set |
| `ORE_SET` | `static final Set<StateType>` | Immutable set of all 19 ore StateTypes |
| `DEEPSLATE_ORES` | `static final Set<StateType>` | Subset for deepslate fake block selection |
| `NETHER_ORES` | `static final Set<StateType>` | Subset for netherrack fake block selection |
| `onReload(ConfigManager)` | Override | Reads `OreObfuscation.enabled` from config |

### External Dependencies (existing GrimnatorAC APIs used)

| Dependency | Usage |
|------------|-------|
| `player.compensatedWorld.getBlock(x, y, z)` | Neighbor block lookup; returns air when chunk not loaded |
| `WrappedBlockState.getType().isAir()` | Air check |
| `WrappedBlockState.getGlobalId() == 0` | Global ID 0 = air (fallback) |
| `StateType.isBlocking()` | Full-cube solid check for non-solid determination |
| `WrapperPlayServerBlockChange` | Single-block packet wrapper |
| `WrapperPlayServerMultiBlockChange` | Multi-block packet wrapper |
| `PacketType.Play.Server.BLOCK_CHANGE` | Packet type constant |
| `PacketType.Play.Server.MULTI_BLOCK_CHANGE` | Packet type constant |
| `event.markForReEncode(true)` | Marks packet for re-serialization after modification |
| `shouldModifyPackets()` | Inherited from `Check`; guards the entire obfuscation path |

---

## Data Models

### Ore Classification

The three ore categories are represented as static immutable `Set<StateType>` fields. A block is classified by checking membership in each set in priority order.

**Deepslate Ores** (fake block → `StateTypes.DEEPSLATE`):
- `DEEPSLATE_DIAMOND_ORE`, `DEEPSLATE_EMERALD_ORE`, `DEEPSLATE_GOLD_ORE`
- `DEEPSLATE_IRON_ORE`, `DEEPSLATE_COPPER_ORE`, `DEEPSLATE_LAPIS_ORE`
- `DEEPSLATE_REDSTONE_ORE`, `DEEPSLATE_COAL_ORE`, `ANCIENT_DEBRIS`

**Normal Ores** (fake block → `StateTypes.STONE`):
- `DIAMOND_ORE`, `EMERALD_ORE`, `GOLD_ORE`, `IRON_ORE`
- `COPPER_ORE`, `LAPIS_ORE`, `REDSTONE_ORE`, `COAL_ORE`

**Nether Ores** (fake block → `StateTypes.NETHERRACK`):
- `NETHER_GOLD_ORE`, `NETHER_QUARTZ_ORE`

`ORE_SET` is the union of all three sets (19 entries total).

### Neighbor Face Offsets

The six cardinal face directions used for neighbor inspection:

| Direction | dx | dy | dz |
|-----------|----|----|-----|
| UP        |  0 | +1 |  0 |
| DOWN      |  0 | -1 |  0 |
| NORTH     |  0 |  0 | -1 |
| SOUTH     |  0 |  0 | +1 |
| WEST      | -1 |  0 |  0 |
| EAST      | +1 |  0 |  0 |

These are applied inline inside `isExposed` as a short-circuit loop; as soon as one non-solid neighbor is found the loop terminates and returns `true`.

### Non-Solid Determination

A neighbor block state is non-solid if any of the following hold:
1. `state.getGlobalId() == 0` (air / unloaded chunk — compensatedWorld returns global ID 0 for unloaded positions)
2. `state.getType().isAir()` (air variants)
3. `!state.getType().isBlocking()` (non-full-cube: glass, slabs, stairs, fences, etc.)

Because `compensatedWorld.getBlock` returns an air-equivalent state for unloaded chunks, condition 1 also covers the "out-of-range neighbor" case from Requirement 1.5 without any special-case code.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Exposure decision is determined entirely by neighbor solidity

*For any* ore block position and any assignment of solid/non-solid states to its six face-adjacent neighbors, `isExposed` SHALL return `true` if and only if at least one neighbor is non-solid (air, global ID 0, or non-blocking `StateType`).

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

### Property 2: Non-solid determination is consistent with the three-condition rule

*For any* `WrappedBlockState`, `isNonSolid` SHALL return `true` if and only if `getGlobalId() == 0` OR `getType().isAir()` OR `!getType().isBlocking()`.

**Validates: Requirements 1.4, 1.5**

### Property 3: Fake block selection is correct for all ore categories

*For any* ore `StateType` in `ORE_SET`, `getFakeBlock` SHALL return `StateTypes.DEEPSLATE` if the ore is in `DEEPSLATE_ORES`, `StateTypes.NETHERRACK` if the ore is in `NETHER_ORES`, and `StateTypes.STONE` otherwise.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 4: Non-ore blocks always pass through unchanged

*For any* `StateType` not in `ORE_SET` (including air), processing a single-block or multi-block packet containing that block type SHALL result in the client receiving the same `StateType` that was in the original packet, with no re-encoding triggered.

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 5: Exposed ore blocks are not replaced

*For any* ore block position where `isExposed` returns `true`, processing that block in a packet SHALL result in the client receiving the real ore `StateType` — i.e., the output block type equals the input ore `StateType`.

**Validates: Requirements 1.2**

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| `compensatedWorld.getBlock` returns air for unloaded chunk | Treated as non-solid neighbor → ore is revealed (conservative, safe) |
| `StateType` not in any ore category inside `getFakeBlock` | Should not occur if guarded by `isOre` first; can fall through to `STONE` as a safe default |
| Null or empty block list in MULTI_BLOCK_CHANGE | Loop body never executes; packet passes through unmodified; no re-encode call |
| `shouldModifyPackets()` returns false (exempt permission, disabled grim, etc.) | Early return in `onPacketSend`; packet passes through entirely |
| Config key absent | `getBooleanElse("OreObfuscation.enabled", true)` defaults to `true` |

---

## Testing Strategy

### Unit Tests

Unit tests should cover specific examples and edge cases that complement the property tests:

- `isNonSolid` returns true for air, global-ID-0 states, and non-blocking types (glass, slab, fence)
- `isNonSolid` returns false for STONE, DEEPSLATE, DIRT (full-cube solids)
- `getFakeBlock` returns DEEPSLATE for each of the 9 deepslate ores
- `getFakeBlock` returns STONE for each of the 8 normal ores
- `getFakeBlock` returns NETHERRACK for each of the 2 nether ores
- `isOre` returns true for all 19 ores and false for STONE, AIR, GRASS_BLOCK
- `isExposed` returns false when all 6 neighbors are STONE
- `isExposed` returns true when exactly one neighbor is AIR
- When `enabled = false`, a packet containing a fully occluded diamond ore is passed through without modification and `markForReEncode` is not called
- When a BLOCK_CHANGE packet contains a non-ore block (e.g. DIRT), the state is unchanged and re-encode is not triggered
- When a BLOCK_CHANGE packet contains air (broken block), state is unchanged and re-encode is not triggered
- When a MULTI_BLOCK_CHANGE has no ore blocks, `setBlocks` and `markForReEncode` are not called

### Property-Based Tests

Property-based testing library: **[jqwik](https://jqwik.net/)** (JUnit 5 compatible, natural fit for Java-based Gradle projects).

Each property test runs a **minimum of 100 iterations** with randomly generated inputs.

Tag format: `Feature: anti-xray-upgrade, Property {N}: {property_text}`

#### Property 1 — Exposure decision correctness
- Generator: random array of 6 booleans representing neighbor solidity
- Simulate `isExposed` by applying the "any non-solid → exposed" rule
- Assert `isExposed` output matches `anyOf(neighborIsNonSolid)`
- Tag: `Feature: anti-xray-upgrade, Property 1: exposure decision is determined entirely by neighbor solidity`

#### Property 2 — Non-solid determination
- Generator: sample from the full set of registered `StateType` values (air, solids, non-full-cube)
- Assert `isNonSolid(state) == (state.getGlobalId()==0 || state.getType().isAir() || !state.getType().isBlocking())`
- Tag: `Feature: anti-xray-upgrade, Property 2: non-solid determination is consistent with the three-condition rule`

#### Property 3 — Fake block selection
- Generator: random element from `ORE_SET`
- Assert `getFakeBlock(ore)` is `DEEPSLATE`, `STONE`, or `NETHERRACK` according to the ore's category
- Tag: `Feature: anti-xray-upgrade, Property 3: fake block selection is correct for all ore categories`

#### Property 4 — Non-ore passthrough
- Generator: random `StateType` sampled from non-ore types
- Build a mock BLOCK_CHANGE event with that type
- Assert the resulting block type equals the input and `markForReEncode` was not called
- Tag: `Feature: anti-xray-upgrade, Property 4: non-ore blocks always pass through unchanged`

#### Property 5 — Exposed ore passthrough
- Generator: random ore from `ORE_SET`, random neighbor configuration where at least one is non-solid
- Build a mock BLOCK_CHANGE event
- Assert the resulting block type equals the original ore type (not the fake block)
- Tag: `Feature: anti-xray-upgrade, Property 5: exposed ore blocks are not replaced`
