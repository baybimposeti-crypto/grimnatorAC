package com.grimnatorac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockBreakCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * XRayAlert — statistical xray detection via ore-find rate analysis.
 *
 * <p>Tracks how many high-value ores (diamond, ancient debris, emerald, etc.) a
 * player mines within a rolling time window. If the rate exceeds the configured
 * threshold, a staff alert is dispatched.  No setback or punishment — this is
 * an alert-only check intended for manual staff review.</p>
 *
 * <p>Default thresholds (all configurable):</p>
 * <ul>
 *   <li>Ancient debris: 2 within 60 seconds → alert</li>
 *   <li>Diamond ore / deepslate diamond ore: 4 within 60 seconds → alert</li>
 *   <li>Emerald ore / deepslate emerald ore: 6 within 60 seconds → alert</li>
 *   <li>Any tracked ore combined: 8 within 60 seconds → alert</li>
 * </ul>
 */
@CheckData(
        name = "XRayAlert",
        stableKey = "grimnatorac.misc.xray_alert",
        description = "Alerts staff when a player mines an abnormal amount of valuable ores",
        decay = 0.01,
        setback = Integer.MAX_VALUE, // alert only — no setback
        experimental = false
)
public class XRayAlert extends Check implements BlockBreakCheck {

    // -----------------------------------------------------------------------
    // Tracked ore categories (configurable thresholds)
    // -----------------------------------------------------------------------

    /** Window duration in milliseconds for rate calculations. */
    private long windowMs;

    /** Max ancient debris in window before alert. */
    private int debrisThreshold;

    /** Max diamond ore in window before alert. */
    private int diamondThreshold;

    /** Max emerald ore in window before alert. */
    private int emeraldThreshold;

    /** Max total tracked ores in window before alert. */
    private int combinedThreshold;

    // -----------------------------------------------------------------------
    // Per-player rolling windows (timestamps of each find in epoch-ms)
    // -----------------------------------------------------------------------

    private final Deque<Long> debrisTimes    = new ArrayDeque<>();
    private final Deque<Long> diamondTimes   = new ArrayDeque<>();
    private final Deque<Long> emeraldTimes   = new ArrayDeque<>();
    private final Deque<Long> combinedTimes  = new ArrayDeque<>();

    public XRayAlert(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // BlockBreakCheck
    // -----------------------------------------------------------------------

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // Only trigger when the block is actually finished being broken
        if (blockBreak.action != DiggingAction.FINISHED_DIGGING) return;

        StateType block = blockBreak.block.getType();
        long now = System.currentTimeMillis();

        OreCategory category = categorise(block);
        if (category == null) return; // Not a tracked ore — ignore

        // Record the find in the appropriate rolling window
        switch (category) {
            case DEBRIS  -> addAndPrune(debrisTimes, now);
            case DIAMOND -> addAndPrune(diamondTimes, now);
            case EMERALD -> addAndPrune(emeraldTimes, now);
        }
        addAndPrune(combinedTimes, now);

        // Check thresholds
        String blockName = block.getName();

        if (category == OreCategory.DEBRIS && debrisTimes.size() >= debrisThreshold) {
            String verbose = String.format("debris=%d in %.0fs (block=%s, pos=%s)",
                    debrisTimes.size(), windowMs / 1000.0, blockName, blockBreak.position);
            flagAndAlert(verbose);
            debrisTimes.clear(); // Reset after alert to avoid spam
            combinedTimes.clear();
            return;
        }

        if (category == OreCategory.DIAMOND && diamondTimes.size() >= diamondThreshold) {
            String verbose = String.format("diamonds=%d in %.0fs (block=%s, pos=%s)",
                    diamondTimes.size(), windowMs / 1000.0, blockName, blockBreak.position);
            flagAndAlert(verbose);
            diamondTimes.clear();
            combinedTimes.clear();
            return;
        }

        if (category == OreCategory.EMERALD && emeraldTimes.size() >= emeraldThreshold) {
            String verbose = String.format("emeralds=%d in %.0fs (block=%s, pos=%s)",
                    emeraldTimes.size(), windowMs / 1000.0, blockName, blockBreak.position);
            flagAndAlert(verbose);
            emeraldTimes.clear();
            combinedTimes.clear();
            return;
        }

        if (combinedTimes.size() >= combinedThreshold) {
            String verbose = String.format("ores=%d in %.0fs (last=%s, pos=%s)",
                    combinedTimes.size(), windowMs / 1000.0, blockName, blockBreak.position);
            flagAndAlert(verbose);
            // Clear all windows after combined alert
            debrisTimes.clear();
            diamondTimes.clear();
            emeraldTimes.clear();
            combinedTimes.clear();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Adds {@code now} to the deque and prunes entries older than {@link #windowMs}.
     */
    private void addAndPrune(Deque<Long> times, long now) {
        times.addLast(now);
        long cutoff = now - windowMs;
        while (!times.isEmpty() && times.peekFirst() < cutoff) {
            times.pollFirst();
        }
    }

    private enum OreCategory { DEBRIS, DIAMOND, EMERALD }

    /**
     * Returns the category for a given block type, or {@code null} if not tracked.
     */
    private static OreCategory categorise(StateType block) {
        if (block == StateTypes.ANCIENT_DEBRIS) {
            return OreCategory.DEBRIS;
        }
        if (block == StateTypes.DIAMOND_ORE || block == StateTypes.DEEPSLATE_DIAMOND_ORE) {
            return OreCategory.DIAMOND;
        }
        if (block == StateTypes.EMERALD_ORE || block == StateTypes.DEEPSLATE_EMERALD_ORE) {
            return OreCategory.EMERALD;
        }
        return null; // Not a tracked ore
    }

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        // Rolling window duration in seconds (converted to ms)
        this.windowMs          = (long) (config.getDoubleElse("XRayAlert.window-seconds", 60.0) * 1000L);
        this.debrisThreshold   = config.getIntElse("XRayAlert.debris-threshold", 2);
        this.diamondThreshold  = config.getIntElse("XRayAlert.diamond-threshold", 4);
        this.emeraldThreshold  = config.getIntElse("XRayAlert.emerald-threshold", 6);
        this.combinedThreshold = config.getIntElse("XRayAlert.combined-threshold", 8);
    }
}
