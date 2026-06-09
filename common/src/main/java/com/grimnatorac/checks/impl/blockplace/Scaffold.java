package com.grimnatorac.checks.impl.blockplace;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockPlaceCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Scaffold — Block placement automation detection.
 *
 * <p>Scaffold clients automatically place blocks under the player while moving:
 * <ol>
 *   <li>Blocks placed at inhuman speeds (< 50ms between placements)</li>
 *   <li>Blocks placed while airborne without proper raytrace alignment</li>
 *   <li>Perfect placement timing synchronized with movement</li>
 *   <li>Placement continues at constant rate regardless of terrain</li>
 * </ol>
 *
 * <p>Human players show:
 * <ul>
 *   <li>Variable placement timing (150-250ms typical, longer when adjusting aim)</li>
 *   <li>Occasional missed placements or misclicks</li>
 *   <li>Speed reduction when bridging (need to aim carefully)</li>
 * </ul>
 *
 * <p>This check flags when:
 * <ul>
 *   <li>Player places {@code minSuspiciousPlacements} blocks within {@code timeWindowMs}ms</li>
 *   <li>Average time between placements is below {@code minAvgDelayMs}ms</li>
 *   <li>Placement occurs while player is moving/airborne (bridging pattern)</li>
 * </ul>
 */
@CheckData(
        name = "Scaffold",
        stableKey = "grimnatorac.blockplace.scaffold",
        description = "Detects automated block placement (scaffold/bridge)",
        decay = 0.05,
        setback = 15,
        experimental = false
)
public class Scaffold extends BlockPlaceCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Time window (ms) to track placements in. */
    private long timeWindowMs;

    /** Minimum number of placements in time window to be suspicious. */
    private int minSuspiciousPlacements;

    /** Minimum average delay (ms) between placements (lower = more suspicious). */
    private long minAvgDelayMs;

    /** Whether to only flag when player is moving. */
    private boolean requireMovement;

    /** Maximum pitch (looking down) to consider for scaffold detection. */
    private double maxPitch;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /** Ring buffer of recent placement timestamps. */
    private final long[] placementTimes;

    /** Current position in ring buffer. */
    private int placementIndex = 0;

    /** Number of valid entries in ring buffer. */
    private int placementCount = 0;

    /** Whether player moved in last tick. */
    private boolean isMoving = false;

    /** Current pitch (looking up/down). */
    private float currentPitch = 0f;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public Scaffold(GrimPlayer player) {
        super(player);
        this.placementTimes = new long[20]; // Track last 20 placements
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return;

        // --- Flying packets track movement ---
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            handleFlyingPacket(event);
            return;
        }

        // --- Block placement packets ---
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handleBlockPlacement(event);
        }
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    private void handleFlyingPacket(PacketReceiveEvent event) {
        // Skip teleports and vehicle riding
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            return;
        }

        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

        // Track movement
        if (wrapper.hasPositionChanged()) {
            isMoving = true;
        } else {
            isMoving = false;
        }

        // Track pitch for scaffold detection
        if (wrapper.hasRotationChanged()) {
            currentPitch = wrapper.getLocation().getPitch();
        }
    }

    private void handleBlockPlacement(PacketReceiveEvent event) {
        long now = System.currentTimeMillis();

        // Check if pitch indicates potential scaffold (looking down)
        // Pitch > 0 = looking down, pitch < 0 = looking up
        if (currentPitch < maxPitch) {
            // Not looking down enough for scaffold
            return;
        }

        // If requireMovement is enabled, only flag when player is moving
        if (requireMovement && !isMoving) {
            return;
        }

        // Add to history
        placementTimes[placementIndex] = now;
        placementIndex = (placementIndex + 1) % placementTimes.length;
        if (placementCount < placementTimes.length) {
            placementCount++;
        }

        // Need enough samples
        if (placementCount < minSuspiciousPlacements) {
            return;
        }

        // Find oldest valid timestamp within time window
        long cutoffTime = now - timeWindowMs;
        int validCount = 0;
        long oldestInWindow = now;

        for (int i = 0; i < placementCount; i++) {
            long time = placementTimes[i];
            if (time >= cutoffTime) {
                validCount++;
                if (time < oldestInWindow) {
                    oldestInWindow = time;
                }
            }
        }

        // Check if we have enough placements in time window
        if (validCount < minSuspiciousPlacements) {
            return;
        }

        // Calculate average delay
        long totalTime = now - oldestInWindow;
        long avgDelay = totalTime / (validCount - 1);

        // Flag if average delay is too fast
        if (avgDelay < minAvgDelayMs) {
            String verbose = String.format(
                    "scaffold: %d blocks in %dms (avg %.0fms/block) pitch=%.1f°%s",
                    validCount, totalTime, (double)avgDelay, currentPitch,
                    isMoving ? " +moving" : ""
            );
            flagAndAlert(verbose);

            // Reset to avoid spam
            placementCount = 0;
            placementIndex = 0;
        }
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        // Time window to track placements.
        // Default: 2000ms (2 seconds) — enough to capture bridging patterns.
        this.timeWindowMs = config.getLongElse("Scaffold.time-window-ms", 2000L);

        // Minimum number of placements to be suspicious.
        // Default: 8 blocks — enough to distinguish from quick building.
        this.minSuspiciousPlacements = config.getIntElse("Scaffold.min-suspicious-placements", 8);

        // Minimum average delay between placements.
        // Humans typically take 150-250ms per block when bridging carefully.
        // Scaffold can place 50-100ms per block.
        // Default: 120ms — catches fast scaffold but allows skilled players.
        this.minAvgDelayMs = config.getLongElse("Scaffold.min-avg-delay-ms", 120L);

        // Whether to only flag when player is moving.
        // Reduces false positives from rapid building while standing still.
        // Default: true — only flag bridging/scaffold movement.
        this.requireMovement = config.getBooleanElse("Scaffold.require-movement", true);

        // Maximum pitch to consider for scaffold.
        // Pitch > 0 = looking down. Scaffold typically places with pitch 70-85°.
        // Default: 45.0° — must be looking significantly downward.
        this.maxPitch = config.getDoubleElse("Scaffold.max-pitch", 45.0);
    }
}
