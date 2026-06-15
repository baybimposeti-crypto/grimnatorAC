// This file was designed and is an original check for GrimnatorAC
// Copyright (C) 2021 DefineOutside
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.grimnatorac.checks.impl.combat;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.collisions.datatypes.SimpleCollisionBox;
import com.grimnatorac.utils.data.packetentity.PacketEntity;
import com.grimnatorac.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import com.grimnatorac.utils.nmsutil.ReachUtils;
import com.grimnatorac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.viaversion.viaversion.api.Via;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * HitboxCheck — detects hitbox expansion cheats by verifying that player
 * look rays intersect the server-authoritative entity bounding box.
 *
 * <p>This check employs a deferred evaluation model: interactions are queued
 * on packet receipt and evaluated after look direction is confirmed on the next
 * movement update, minimizing false positives from look-direction uncertainty.
 *
 * <p>Unlike the Reach check which measures distance violations, HitboxCheck
 * focuses on ray-intersection failures: cases where no candidate look direction
 * produces a ray that intersects the target's expanded bounding box.
 */
@CheckData(
    name = "HitboxCheck",
    stableKey = "grimnatorac.combat.hitbox_check",
    description = "Detects hitbox expansion cheats by ray-box intersection failures",
    decay = 0.05,
    setback = 10
)
public class HitboxCheck extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Configuration (hot-reloadable)
    // -----------------------------------------------------------------------

    /** Tolerance added to the canonical hitbox before performing intersection tests. */
    private double threshold = 0.0005;

    /** VL threshold for triggering setback. */
    private double setbackVl = 10;

    /** Whether to cancel interaction packets on violation. */
    private boolean cancelOnFlag = false;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    /**
     * Queued interactions waiting for look confirmation.
     * Maps entity ID to interaction record.
     * Max size: 10 (queue-flooding protection).
     */
    private final Int2ObjectMap<InteractionRecord> queuedInteractions;

    /**
     * Cancel buffer for aggressive packet cancellation after violations.
     * When a violation is detected, this is set to 1.0 to enable aggressive cancellation
     * for the next few hits. Decrements by 0.25 on each clean hit.
     * Similar to Reach check's cancelBuffer pattern.
     */
    private double cancelBuffer = 0.0;

    // -----------------------------------------------------------------------
    // Record classes
    // -----------------------------------------------------------------------

    /**
     * Stores snapshot of player state at interaction packet receipt time.
     * Used for deferred evaluation after look confirmation.
     */
    private record InteractionRecord(
        double eyeX,
        double eyeY,
        double eyeZ,
        float yaw,
        float pitch
    ) {}

    /**
     * Result of hitbox intersection evaluation.
     */
    private record CheckResult(
        boolean isViolation,
        double minDistance,  // Closest approach distance (for verbose)
        String reason        // "miss" or "ok"
    ) {}

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public HitboxCheck(GrimPlayer player) {
        super(player);
        this.queuedInteractions = new Int2ObjectOpenHashMap<>();
    }

    // -----------------------------------------------------------------------
    // Configuration reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ac.grim.grimac.api.config.ConfigManager config) {
        // Load threshold configuration
        double configThreshold = config.getDoubleElse("HitboxCheck.threshold", 0.0005);
        if (configThreshold < 0.0 || configThreshold > 5.0) {
            player.user.sendMessage(net.kyori.adventure.text.Component.text(
                "§cHitboxCheck: Invalid threshold value " + configThreshold + ", must be in range [0.0, 5.0]. Using default 0.0005."
            ));
            this.threshold = 0.0005;
        } else {
            this.threshold = configThreshold;
        }

        // Load setback VL configuration
        double configSetbackVl = config.getDoubleElse("HitboxCheck.setbackvl", 10.0);
        if (configSetbackVl < 1.0 || configSetbackVl > 1000.0) {
            player.user.sendMessage(net.kyori.adventure.text.Component.text(
                "§cHitboxCheck: Invalid setbackvl value " + configSetbackVl + ", must be in range [1, 1000]. Using default 10."
            ));
            this.setbackVl = 10.0;
        } else {
            this.setbackVl = configSetbackVl;
        }

        // Load cancelOnFlag configuration
        this.cancelOnFlag = config.getBooleanElse("HitboxCheck.cancelOnFlag", false);
    }

    // -----------------------------------------------------------------------
    // PacketCheck implementation
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Handle ATTACK packets
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            handleInteraction(event, packet.getEntityId());
        }

        // Handle INTERACT_ENTITY packets
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            handleInteraction(event, packet.getEntityId());
        }

        // Handle movement update packets (flying packets, CLIENT_TICK_END, transactions)
        // After look direction is confirmed, evaluate all queued interactions
        if (isUpdate(event.getPacketType())) {
            evaluateQueuedInteractions();
            queuedInteractions.clear();
        }
    }

    /**
     * Handles interaction packets (ATTACK or INTERACT_ENTITY).
     * Applies exemption checks and queues valid interactions for deferred evaluation.
     */
    private void handleInteraction(PacketReceiveEvent event, int entityId) {
        // Exemption: Permission node bypass
        if (isExemptPermission()) {
            return;
        }

        // Exemption: Don't let the player teleport to bypass hitbox detection
        if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        // Fetch the entity from compensated entities
        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);

        // Exemption: Stop people from freezing transactions before an entity spawns
        // Only cancel if we are tracking this entity (we don't track paintings)
        if (entity == null || entity instanceof PacketEntityEnderDragonPart) {
            if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(entityId)) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        // Exemption: Dead entities cause false flags
        if (entity.isDead) {
            return;
        }

        // Exemption: Creative and spectator mode players
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) {
            return;
        }

        // Exemption: Player is riding a vehicle
        if (player.inVehicle()) {
            return;
        }

        // Exemption: Blacklisted entity types (boat, chest boat, shulker)
        if (entity.getType() == EntityTypes.BOAT
            || entity.getType() == EntityTypes.CHEST_BOAT
            || entity.getType() == EntityTypes.SHULKER) {
            return;
        }

        // Queue the interaction for deferred evaluation
        queueInteraction(event, entityId);
    }

    /**
     * Queues an interaction for deferred evaluation after look confirmation.
     * Applies queue-flooding protection (max 10 entries).
     * Implements aggressive packet cancellation when cancelBuffer is active.
     */
    private void queueInteraction(PacketReceiveEvent event, int entityId) {
        // Queue-flooding protection: if queue is already at capacity, cancel and discard
        if (queuedInteractions.size() >= 10) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        // Aggressive packet cancellation when cancelBuffer is active
        // After a violation is detected, cancelOnFlag sets cancelBuffer = 1.0
        // This enables immediate cancellation of subsequent packets until the player
        // demonstrates clean hits (cancelBuffer decrements by 0.25 per clean hit)
        if (shouldModifyPackets() && cancelOnFlag && cancelBuffer > 0) {
            // Perform immediate evaluation to determine if this packet should be cancelled
            PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);

            // If entity exists and is not exempt, evaluate immediately for cancellation
            if (entity != null && !(entity instanceof PacketEntityEnderDragonPart)) {
                InteractionRecord immediateRecord = new InteractionRecord(
                    player.x,
                    player.y,
                    player.z,
                    player.yaw,
                    player.pitch
                );

                CheckResult immediateResult = evaluateInteraction(immediateRecord, entity);

                // Cancel if this is a violation
                if (immediateResult.isViolation) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    return;
                }
            }
        }

        // Create interaction record with current player state snapshot
        InteractionRecord record = new InteractionRecord(
            player.x,
            player.y,
            player.z,
            player.yaw,
            player.pitch
        );

        // Store in queue (overwrites previous entry for same entity)
        queuedInteractions.put(entityId, record);
    }

    /**
     * Evaluates all queued interactions after look direction is confirmed.
     * For each queued interaction:
     * - Fetches the target entity from compensated entity map
     * - Evaluates ray-box intersection using confirmed look direction
     * - Flags violations or rewards clean hits
     * - Handles despawned entities silently
     *
     * This method is called on movement update packets (flying, CLIENT_TICK_END, transactions).
     */
    private void evaluateQueuedInteractions() {
        // Iterate over all queued interactions
        for (Int2ObjectMap.Entry<InteractionRecord> entry : queuedInteractions.int2ObjectEntrySet()) {
            int entityId = entry.getIntKey();
            InteractionRecord record = entry.getValue();

            // Fetch entity from compensated entity map
            PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);

            // Skip silently if entity despawned
            if (entity == null) {
                continue;
            }

            // Evaluate the interaction using ray-box intersection
            CheckResult result = evaluateInteraction(record, entity);

            // Handle violation or reward based on result
            if (result.isViolation) {
                // Format verbose message with entity type and miss distance
                String verbose = String.format("type=%s miss=%.4f",
                    entity.getType().getName().getKey(),
                    result.minDistance);

                // Flag the violation (increments VL)
                flag(verbose);

                // ALWAYS send alert immediately, bypassing punishment manager thresholds
                // This ensures admins/ops are notified on EVERY hitbox expansion detection
                // Format: [GrimnatorAC] PlayerName failed HitboxCheck. VL: X type=entity miss=0.1234
                String alertText = String.format(
                    "<red>[GrimnatorAC]</red> <white>%s</white> <red>failed</red> <white>%s</white><red>.</red> <white>VL:</white> <red>%.1f</red> <white>%s</white>",
                    player.user.getName(),
                    getCheckName(),
                    getViolations(),
                    verbose
                );

                // Send to all players with alert permission using the alert manager
                net.kyori.adventure.text.Component message = com.grimnatorac.utils.anticheat.MessageUtil.miniMessage(alertText);
                com.grimnatorac.GrimAPI.INSTANCE.getAlertManager().sendAlert(message, null);

                // Set cancel buffer for aggressive cancellation on next hits
                // This enables packet cancellation for the next ~4 hits (decrements 0.25 per clean hit)
                if (cancelOnFlag) {
                    cancelBuffer = 1.0;
                }

                // Trigger setback if VL exceeds threshold
                if (getViolations() >= setbackVl) {
                    setbackIfAboveSetbackVL();
                }
            } else {
                // Reward clean hit (decay VL)
                reward();

                // Decrement cancel buffer on clean hits
                // This gradually disables aggressive cancellation after violations stop
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }
    }

    /**
     * Evaluates a single interaction by testing ray-box intersection.
     *
     * Implements the core hitbox expansion and ray-box intersection logic:
     * 1. Constructs canonical hitbox from entity's possible collision boxes
     * 2. Applies threshold expansion, movement threshold, and ViaVersion margin
     * 3. Tests ray-box intersection for all candidate look directions and eye heights
     * 4. Returns violation if no ray intersects the expanded box
     *
     * @param record The interaction record containing eye position and look angles
     * @param entity The target entity to check intersection against
     * @return CheckResult indicating whether this is a violation
     */
    private CheckResult evaluateInteraction(InteractionRecord record, PacketEntity entity) {
        // Step 1: Get canonical hitbox (union of all possible collision boxes for interpolation)
        // This accounts for entity movement uncertainty between ticks
        SimpleCollisionBox box = entity.getPossibleCollisionBoxes();

        // Step 2: Apply threshold expansion
        // This is the configured tolerance to absorb network jitter and latency uncertainty
        box.expand(threshold);

        // Step 3: Check movement threshold condition
        // If the last last movement didn't include position, expand by movement threshold
        // This handles 0.03 movement uncertainty when player hasn't sent position updates
        if (!player.packetStateData.didLastLastMovementIncludePosition) {
            box.expand(player.getMovementThreshold());
        }

        // Step 4: Check ViaVersion 1.8 margin
        // If ViaVersion is available and configured to use 1.8 hitbox margin, add 0.1 expansion
        // This matches the margin applied by the Reach check for consistency
        if (ViaVersionUtil.isAvailable) {
            try {
                if (Via.getConfig().getValues().containsKey("use-1_8-hitbox-margin")
                    && Via.getConfig().use1_8HitboxMargin()) {
                    box.expand(0.1);
                }
            } catch (Exception e) {
                // Silently ignore if ViaVersion API throws - fail safe to clean hit
            }
        }

        // Step 5: Build candidate look vectors based on client version
        // The check tests multiple look-direction combinations because:
        // - Network latency can cause look direction to be uncertain between packet receipt and evaluation
        // - Different client versions handle look updates differently (1.7 vs 1.8 vs 1.9+)
        // - We queue interactions at packet receipt but evaluate after look confirmation
        //
        // We test combinations of:
        // - record.yaw/pitch: the look direction at packet receipt time (queued)
        // - player.lastYaw/lastPitch: the confirmed look direction at evaluation time
        //
        // This accounts for the possibility that the client's actual look direction
        // at the moment of interaction may differ from either snapshot due to timing.

        java.util.List<com.grimnatorac.utils.math.Vector3dm> candidateLooks = new java.util.ArrayList<>();

        // Vector 1: Use queued yaw and pitch (the values at packet receipt time)
        // This is the primary look vector and is tested for all client versions
        candidateLooks.add(ReachUtils.getLook(player, record.yaw, record.pitch));

        // Vector 2: Use confirmed lastYaw with queued pitch (1.8+ only)
        // 1.8+ clients may have yaw updated but pitch from previous tick
        if (player.getClientVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_8)) {
            candidateLooks.add(ReachUtils.getLook(player, player.lastYaw, record.pitch));
        }

        // Vector 3: Use queued yaw with confirmed lastPitch (1.9+ only)
        // 1.9+ clients may have pitch updated but yaw from previous tick due to tick skipping
        if (player.getClientVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_9)) {
            candidateLooks.add(ReachUtils.getLook(player, record.yaw, player.lastPitch));
        }

        // Vector 4: Use both confirmed lastYaw and lastPitch (1.9+ only)
        // 1.9+ clients may be a full tick behind on both axes
        if (player.getClientVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_9)) {
            candidateLooks.add(ReachUtils.getLook(player, player.lastYaw, player.lastPitch));
        }

        // Step 6: Test ray-box intersection for all (lookVector, eyeHeight) combinations
        // This is the core hitbox expansion detection logic.
        // We test every combination of:
        // - Candidate look vectors (1-4 depending on client version)
        // - Possible eye heights (accounts for sneaking, swimming, elytra, etc.)
        //
        // If ANY combination produces a ray that intersects the expanded box,
        // OR if the eye position is strictly inside the box, then it's a clean hit.
        //
        // If ALL combinations miss, then we have a hitbox expansion violation.

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        double minDistance = Double.MAX_VALUE;
        boolean hitFound = false;

        // Ray distance: 6.5 blocks (max interaction range + safety margin)
        final double rayDistance = 6.5;

        // Test all combinations of look vectors and eye heights
        for (com.grimnatorac.utils.math.Vector3dm lookVector : candidateLooks) {
            // Scale look vector to ray distance
            com.grimnatorac.utils.math.Vector3dm scaledLook = new com.grimnatorac.utils.math.Vector3dm(
                lookVector.getX() * rayDistance,
                lookVector.getY() * rayDistance,
                lookVector.getZ() * rayDistance
            );

            for (double eyeHeight : possibleEyeHeights) {
                // Build eye position for this candidate
                Vector3d eyePos = new Vector3d(
                    record.eyeX,
                    record.eyeY + eyeHeight,
                    record.eyeZ
                );

                // Check if eye is strictly inside the box (exclusive boundary)
                // This handles legitimate close-range interactions where the player
                // is essentially inside the entity's hitbox
                if (ReachUtils.isVecInside(box, eyePos)) {
                    // Eye inside box = instant clean hit, no violation
                    return new CheckResult(false, 0.0, "ok");
                }

                // Build end position for the ray
                Vector3d endPos = eyePos.add(
                    scaledLook.getX(),
                    scaledLook.getY(),
                    scaledLook.getZ()
                );

                // Calculate ray-box intersection
                Vector3d intercept = ReachUtils.calculateIntercept(box, eyePos, endPos).first();

                // If ray intersects the box, this is a potential hit
                if (intercept != null) {
                    hitFound = true;
                    // Track the minimum distance to intercept across all candidates
                    // This is used for verbose output
                    double distanceToIntercept = eyePos.distance(intercept);
                    minDistance = Math.min(minDistance, distanceToIntercept);
                }
            }
        }

        // Step 7: Determine final result based on whether any ray hit the box
        if (hitFound) {
            // At least one candidate ray intersected the box — clean hit, no violation
            return new CheckResult(false, minDistance, "ok");
        } else {
            // ALL candidate rays missed the box — hitbox expansion violation
            // Compute minimum distance from eye to box surface for verbose output
            // This helps admins understand how far off the hit was
            double minMissDistance = ReachUtils.getMinReachToBox(player, box);
            return new CheckResult(true, minMissDistance, "miss");
        }
    }
}
