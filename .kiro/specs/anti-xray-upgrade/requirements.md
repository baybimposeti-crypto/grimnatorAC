# Requirements Document

## Introduction

This document defines requirements for upgrading the anti-xray system in GrimnatorAC (the `OreObfuscation` check). The current implementation replaces all non-nearby ore blocks with stone in outbound block-change packets. The upgrade replaces the proximity-based reveal model with an **exposure-based reveal model**: a block is sent as its real type only when at least one of its six neighboring faces is exposed to a non-solid (air or transparent) block, otherwise it is replaced with a contextually-correct fake block. This produces a more accurate, cheat-resistant obfuscation layer for both `BLOCK_CHANGE` and `MULTI_BLOCK_CHANGE` packets.

---

## Glossary

- **OreObfuscation**: The GrimnatorAC check class (`OreObfuscation.java`) responsible for intercepting outbound block packets and hiding ore positions from xray clients.
- **CompensatedWorld**: The server-side per-player block-state cache used by GrimnatorAC to look up neighboring block types without a Bukkit API call.
- **Exposed Block**: A block that has at least one of its six face-adjacent neighbors (UP, DOWN, NORTH, SOUTH, EAST, WEST) that is air or a non-solid/transparent block.
- **Fully Occluded Block**: A block whose all six face-adjacent neighbors are solid (non-air, non-transparent) blocks.
- **Deepslate Ore**: Any ore whose `StateType` name contains `DEEPSLATE_`, as well as `ANCIENT_DEBRIS`. These ores generate at deepslate-level depths (y ≤ 0 in 1.18+).
- **Normal Ore**: Any ore that is not a Deepslate Ore and not a Nether ore. These generate in stone-layer depths.
- **Nether Ore**: `NETHER_GOLD_ORE` and `NETHER_QUARTZ_ORE`. These generate in the Nether.
- **Fake Block**: A block type used to replace an ore when sending data to the client. Its selection is based on the ore's depth category (see Deepslate/Normal/Nether Ore definitions above).
- **Reveal**: Sending the client the correct, real `WrappedBlockState` for a block position.
- **Obfuscate**: Sending the client a Fake Block in place of the real ore block state.
- **Block-Change Packet**: A `BLOCK_CHANGE` (single block) or `MULTI_BLOCK_CHANGE` (multiple blocks in a chunk section) outbound packet from the server to the client.
- **Ore List**: The complete set of `StateType` values classified as Deepslate Ores, Normal Ores, or Nether Ores (see Requirement 2).

---

## Requirements

### Requirement 1: Exposure-Based Ore Reveal

**User Story:** As a server administrator, I want ore blocks that are naturally exposed to air (e.g. blocks along a tunnel wall) to be sent as their real type to the client, so that players do not see visual glitches when mining next to revealed ores.

#### Acceptance Criteria

1. WHEN a `BLOCK_CHANGE` or `MULTI_BLOCK_CHANGE` packet containing an ore block is about to be sent to the client, THE `OreObfuscation` check SHALL evaluate whether the ore block at that position is an Exposed Block by inspecting each of its six face-adjacent neighbor positions (UP, DOWN, NORTH, SOUTH, EAST, WEST) using the server-side block-state cache.
2. WHEN an ore block is an Exposed Block (at least one neighbor is air or non-solid), THE `OreObfuscation` check SHALL send the client the real `WrappedBlockState` of that block without modification (the `StateType` received by the client SHALL equal the actual ore `StateType` at that position).
3. WHEN an ore block is a Fully Occluded Block (all six neighbors are solid and non-air), THE `OreObfuscation` check SHALL replace the block in the outbound packet with the appropriate Fake Block (as defined in Requirement 2) before sending.
4. THE `OreObfuscation` check SHALL treat a neighbor as non-solid IF the server-side block-state cache returns an air state (global ID 0) for that neighbor position OR IF the neighbor's `StateType` is not a full, opaque solid block (e.g. glass, slabs, stairs, or any other non-full-cube block).
5. WHEN any of the six face-adjacent neighbor positions is outside the loaded chunk range and the server-side block-state cache cannot return a state for it, THE `OreObfuscation` check SHALL treat that neighbor as non-solid, causing the ore to be revealed rather than obfuscated.

---

### Requirement 2: Depth-Aware Fake Block Selection

**User Story:** As a server administrator, I want replaced (obfuscated) ore blocks to blend in with their surroundings based on their depth, so that xray clients see a believable world and cannot infer ore locations from visually mismatched replacement blocks.

#### Acceptance Criteria

1. IF a Fully Occluded ore block is a Deepslate Ore, THEN THE `OreObfuscation` check SHALL replace it with `StateTypes.DEEPSLATE` as the Fake Block in the outbound packet.
2. IF a Fully Occluded ore block is a Normal Ore, THEN THE `OreObfuscation` check SHALL replace it with `StateTypes.STONE` as the Fake Block in the outbound packet.
3. IF a Fully Occluded ore block is a Nether Ore, THEN THE `OreObfuscation` check SHALL replace it with `StateTypes.NETHERRACK` as the Fake Block in the outbound packet.
4. THE `OreObfuscation` check SHALL classify `DEEPSLATE_DIAMOND_ORE`, `DEEPSLATE_EMERALD_ORE`, `DEEPSLATE_GOLD_ORE`, `DEEPSLATE_IRON_ORE`, `DEEPSLATE_COPPER_ORE`, `DEEPSLATE_LAPIS_ORE`, `DEEPSLATE_REDSTONE_ORE`, `DEEPSLATE_COAL_ORE`, and `ANCIENT_DEBRIS` as Deepslate Ores, with each ore type belonging to exactly one category.
5. THE `OreObfuscation` check SHALL classify `DIAMOND_ORE`, `EMERALD_ORE`, `GOLD_ORE`, `IRON_ORE`, `COPPER_ORE`, `LAPIS_ORE`, `REDSTONE_ORE`, and `COAL_ORE` as Normal Ores, with each ore type belonging to exactly one category.
6. THE `OreObfuscation` check SHALL classify `NETHER_GOLD_ORE` and `NETHER_QUARTZ_ORE` as Nether Ores, with each ore type belonging to exactly one category.
7. A block SHALL be considered Fully Occluded for the purposes of fake block selection ONLY when all six of its face-adjacent neighbors (UP, DOWN, NORTH, SOUTH, EAST, WEST) are solid, non-air blocks as determined by the server-side block-state cache.

---

### Requirement 3: Mining Safety — Air Replacement Passthrough

**User Story:** As a player, I want breaking an ore block to visually update correctly on my client, so that I do not see a stone block where I just mined out an ore.

#### Acceptance Criteria

1. WHEN a `BLOCK_CHANGE` or `MULTI_BLOCK_CHANGE` packet is sent for a block position where the new `StateType` is air (the block was broken), THE `OreObfuscation` check SHALL pass that block entry through without modification (the `StateType` received by the client SHALL be air).
2. IF a block entry's new `StateType` is not in the Ore List, THEN THE `OreObfuscation` check SHALL pass that block entry through to the client without modification (the `StateType` received by the client SHALL equal the original `StateType` from the packet).
3. THE Ore List SHALL consist of exactly the `StateType` values enumerated in Requirement 2 criteria 4, 5, and 6; a `StateType` not present in that enumeration SHALL NOT be treated as an ore by the `OreObfuscation` check.

---

### Requirement 4: Configurability

**User Story:** As a server administrator, I want to enable or disable the ore obfuscation feature and control its behavior via the GrimnatorAC configuration file, so that I can tune it without redeploying the plugin.

#### Acceptance Criteria

1. THE `OreObfuscation` check SHALL read an `OreObfuscation.enabled` boolean configuration key from the GrimnatorAC configuration file.
2. WHEN `OreObfuscation.enabled` is `false`, THE `OreObfuscation` check SHALL skip all packet interception logic and pass every `BLOCK_CHANGE` and `MULTI_BLOCK_CHANGE` packet through unmodified.
3. WHEN `OreObfuscation.enabled` is `true`, THE `OreObfuscation` check SHALL apply the exposure-based ore reveal logic defined in Requirements 1–3 to every applicable outbound packet.
4. THE `OreObfuscation` check SHALL apply all configuration values on each call to `onReload(ConfigManager config)` such that no server restart is required for configuration changes to take effect.
5. WHEN `OreObfuscation.enabled` is absent from the configuration file, THE `OreObfuscation` check SHALL default to `true` (obfuscation enabled).

---

### Requirement 5: Packet Re-Encoding

**User Story:** As a server developer, I want the modified packets to be correctly re-encoded and sent to the client, so that no data corruption or client-side errors occur due to partial or malformed packets.

#### Acceptance Criteria

1. WHEN any block entry in a `BLOCK_CHANGE` packet is replaced with a Fake Block, THE `OreObfuscation` check SHALL call `event.markForReEncode(true)` on the corresponding `PacketSendEvent` so that the modified state is serialized before transmission.
2. WHEN any block entry in a `MULTI_BLOCK_CHANGE` packet is replaced with a Fake Block, THE `OreObfuscation` check SHALL call `packet.setBlocks(blocks)` with the modified block array before calling `event.markForReEncode(true)` on the corresponding `PacketSendEvent`.
3. WHEN no block entries in a packet are modified, THE `OreObfuscation` check SHALL NOT call `event.markForReEncode(true)`.

---

### Requirement 6: Removal of Proximity-Based Reveal Logic

**User Story:** As a server administrator, I want the old radius-based reveal logic removed, so that xray clients cannot use the predictable proximity threshold to infer ore locations.

#### Acceptance Criteria

1. THE `OreObfuscation` check SHALL NOT use player coordinates to determine whether to reveal an ore block in `onPacketSend`, `handleSingleBlock`, `handleMultiBlock`, or any helper method in the obfuscation path.
2. THE `OreObfuscation` check SHALL NOT read or reference the `OreObfuscation.reveal-radius` configuration key in `onReload` or any other method; IF the key is present in the configuration file, THE check SHALL ignore it entirely.
3. THE `OreObfuscation` check SHALL NOT contain an `isPlayerNear` method or any equivalent distance-computation method that computes the Euclidean or Chebyshev distance between the player position and a block position for the purpose of reveal decisions.
4. THE `OreObfuscation` check SHALL NOT contain a `revealRadius` instance field or any other field that stores a distance threshold for reveal decisions.
