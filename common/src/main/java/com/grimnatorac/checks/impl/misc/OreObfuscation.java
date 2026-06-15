package com.grimnatorac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * OreObfuscation — exposure-based anti-xray packet filter.
 *
 * <p>Intercepts outbound {@code BLOCK_CHANGE} and {@code MULTI_BLOCK_CHANGE} packets and
 * replaces ore blocks that are fully surrounded by solid blocks with a depth-appropriate
 * fake block (STONE, DEEPSLATE, or NETHERRACK), preventing xray clients from seeing
 * ore positions.</p>
 *
 * <p>An ore is <em>revealed</em> (sent as-is) when at least one of its six face-adjacent
 * neighbors is non-solid (air, unloaded chunk, glass, slab, etc.).  An ore is
 * <em>obfuscated</em> only when all six neighbors are fully solid opaque blocks.</p>
 */
@CheckData(
        name = "OreObfuscation",
        stableKey = "grimnatorac.misc.ore_obfuscation",
        description = "Prevents xray clients from seeing ore positions by replacing occluded ores in block packets",
        decay = 0,
        setback = Integer.MAX_VALUE,
        experimental = false
)
public class OreObfuscation extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Ore classification sets
    // -----------------------------------------------------------------------

    /** Deepslate-layer ores → fake block: DEEPSLATE */
    private static final Set<StateType> DEEPSLATE_ORES;

    /** Nether ores → fake block: NETHERRACK */
    private static final Set<StateType> NETHER_ORES;

    /** Union of all tracked ore types (19 total). */
    static final Set<StateType> ORE_SET;

    static {
        Set<StateType> deepslate = Collections.newSetFromMap(new IdentityHashMap<>());
        deepslate.add(StateTypes.DEEPSLATE_DIAMOND_ORE);
        deepslate.add(StateTypes.DEEPSLATE_EMERALD_ORE);
        deepslate.add(StateTypes.DEEPSLATE_GOLD_ORE);
        deepslate.add(StateTypes.DEEPSLATE_IRON_ORE);
        deepslate.add(StateTypes.DEEPSLATE_COPPER_ORE);
        deepslate.add(StateTypes.DEEPSLATE_LAPIS_ORE);
        deepslate.add(StateTypes.DEEPSLATE_REDSTONE_ORE);
        deepslate.add(StateTypes.DEEPSLATE_COAL_ORE);
        deepslate.add(StateTypes.ANCIENT_DEBRIS);
        DEEPSLATE_ORES = Collections.unmodifiableSet(deepslate);

        Set<StateType> nether = Collections.newSetFromMap(new IdentityHashMap<>());
        nether.add(StateTypes.NETHER_GOLD_ORE);
        nether.add(StateTypes.NETHER_QUARTZ_ORE);
        NETHER_ORES = Collections.unmodifiableSet(nether);

        Set<StateType> all = Collections.newSetFromMap(new IdentityHashMap<>());
        all.addAll(DEEPSLATE_ORES);
        all.addAll(NETHER_ORES);
        // Normal ores
        all.add(StateTypes.DIAMOND_ORE);
        all.add(StateTypes.EMERALD_ORE);
        all.add(StateTypes.GOLD_ORE);
        all.add(StateTypes.IRON_ORE);
        all.add(StateTypes.COPPER_ORE);
        all.add(StateTypes.LAPIS_ORE);
        all.add(StateTypes.REDSTONE_ORE);
        all.add(StateTypes.COAL_ORE);
        ORE_SET = Collections.unmodifiableSet(all);
    }

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    private boolean enabled;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public OreObfuscation(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck — outbound
    // -----------------------------------------------------------------------

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled || !shouldModifyPackets()) return;

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            handleSingleBlock(event);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            handleMultiBlock(event);
        }
    }

    // -----------------------------------------------------------------------
    // Single-block handler
    // -----------------------------------------------------------------------

    private void handleSingleBlock(PacketSendEvent event) {
        WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);

        WrappedBlockState blockState = packet.getBlockState();
        StateType type = blockState.getType();

        // Air = block was broken — always pass through
        if (type.isAir()) return;

        // Non-ore — pass through unchanged
        if (!isOre(type)) return;

        Vector3i pos = packet.getBlockPosition();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // Exposed ore — reveal it
        if (isExposed(x, y, z)) return;

        // Fully occluded — replace with fake block
        packet.setBlockState(getFakeBlockState(type));
        event.markForReEncode(true);
    }

    // -----------------------------------------------------------------------
    // Multi-block handler
    // -----------------------------------------------------------------------

    private void handleMultiBlock(PacketSendEvent event) {
        WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
        WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = packet.getBlocks();

        if (blocks == null || blocks.length == 0) return;

        boolean modified = false;

        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            WrappedBlockState blockState = block.getBlockState(CompensatedWorld.blockVersion);
            StateType type = blockState.getType();

            // Air or non-ore — pass through
            if (type.isAir() || !isOre(type)) continue;

            int x = block.getX(), y = block.getY(), z = block.getZ();

            // Exposed — pass through
            if (isExposed(x, y, z)) continue;

            // Fully occluded — replace
            block.setBlockState(getFakeBlockState(type));
            modified = true;
        }

        if (modified) {
            packet.setBlocks(blocks);
            event.markForReEncode(true);
        }
    }

    // -----------------------------------------------------------------------
    // Exposure check
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if at least one of the six face-adjacent neighbors
     * of the block at (x, y, z) is non-solid (air, unloaded, or non-full-cube).
     */
    private boolean isExposed(int x, int y, int z) {
        // UP, DOWN, NORTH, SOUTH, WEST, EAST
        return isNonSolid(player.compensatedWorld.getBlock(x,     y + 1, z    ))
            || isNonSolid(player.compensatedWorld.getBlock(x,     y - 1, z    ))
            || isNonSolid(player.compensatedWorld.getBlock(x,     y,     z - 1))
            || isNonSolid(player.compensatedWorld.getBlock(x,     y,     z + 1))
            || isNonSolid(player.compensatedWorld.getBlock(x - 1, y,     z    ))
            || isNonSolid(player.compensatedWorld.getBlock(x + 1, y,     z    ));
    }

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the given state is non-solid: air, unloaded chunk
     * (global ID 0), or any non-full-cube block (glass, slab, fence, etc.).
     */
    static boolean isNonSolid(WrappedBlockState state) {
        return state.getGlobalId() == 0
            || state.getType().isAir()
            || !state.getType().isBlocking();
    }

    /**
     * Returns {@code true} if the given {@code StateType} is in the ore list.
     */
    static boolean isOre(StateType type) {
        return ORE_SET.contains(type);
    }

    /**
     * Returns the fake {@code WrappedBlockState} to substitute for the given ore type.
     * <ul>
     *   <li>Deepslate ores → DEEPSLATE</li>
     *   <li>Nether ores    → NETHERRACK</li>
     *   <li>Normal ores    → STONE</li>
     * </ul>
     */
    static WrappedBlockState getFakeBlockState(StateType ore) {
        if (DEEPSLATE_ORES.contains(ore)) {
            return StateTypes.DEEPSLATE.createBlockState();
        }
        if (NETHER_ORES.contains(ore)) {
            return StateTypes.NETHERRACK.createBlockState();
        }
        return StateTypes.STONE.createBlockState();
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        this.enabled = config.getBooleanElse("OreObfuscation.enabled", true);
    }
}
