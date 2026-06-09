package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

/**
 * AutoCrystal — detects automated end-crystal placement/explosion cycling.
 *
 * <p>Legitimate players cannot place a crystal, then immediately attack a
 * different crystal within the same or the next tick.  This check tracks:
 * <ol>
 *   <li><b>Place rate</b> — END_CRYSTAL item placements faster than
 *       {@code minPlaceMs} (default 100 ms) minus ping compensation.</li>
 *   <li><b>Break rate</b> — END_CRYSTAL entity attacks faster than
 *       {@code minBreakMs} (default 100 ms) minus ping compensation.</li>
 *   <li><b>Place→Break sequence</b> — breaking a crystal within
 *       {@code minPlaceBreakMs} (default 50 ms) of placing one, repeated
 *       {@code sequenceThreshold} (default 3) times, indicates automation.</li>
 * </ol>
 *
 * <p>Each sub-detection accumulates a violation counter.  An alert fires
 * after {@code alertThreshold} (default 5) accumulated violations.
 * Violations decay by 1 after each clean block-place or clean crystal break.</p>
 */
@CheckData(
        name = "AutoCrystal",
        stableKey = "grimnatorac.combat.auto_crystal",
        description = "Detects automated end-crystal place/break cycling",
        decay = 0,
        setback = Integer.MAX_VALUE,
        experimental = false
)
public class AutoCrystal extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    private long minPlaceMs;
    private long minBreakMs;
    private long minPlaceBreakMs;
    private int  sequenceThreshold;
    private int  alertThreshold;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    private long lastCrystalPlaceMs = 0L;
    private long lastCrystalBreakMs = 0L;

    /** How many consecutive place→break sequences were suspiciously fast. */
    private int sequenceCount = 0;

    /** Accumulated violations across all sub-detections. */
    private int violations = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public AutoCrystal(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

        // ---- Crystal placement ----
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement pkt = new WrapperPlayClientPlayerBlockPlacement(event);
            // Check if the item being placed is an END_CRYSTAL
            var heldItem = pkt.getHand() == com.github.retrooper.packetevents.protocol.player.InteractionHand.MAIN_HAND
                    ? player.inventory.getHeldItem()
                    : player.inventory.getOffHand();
            if (heldItem.getType() == ItemTypes.END_CRYSTAL) {
                handleCrystalPlace();
            }
            return;
        }

        // ---- Crystal attack ----
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity pkt = new WrapperPlayClientInteractEntity(event);
            if (pkt.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            // Check entity type from tracked entities
            var entity = player.compensatedEntities.entityMap.get(pkt.getEntityId());
            if (entity == null || entity.getType() != EntityTypes.END_CRYSTAL) return;

            handleCrystalBreak();
        }
    }

    // -----------------------------------------------------------------------
    // Sub-detection handlers
    // -----------------------------------------------------------------------

    private void handleCrystalPlace() {
        long now = System.currentTimeMillis();
        long pingMs = player.getTransactionPing();
        long compensation = Math.min(pingMs / 2, 100L);

        if (lastCrystalPlaceMs != 0L) {
            long gap = now - lastCrystalPlaceMs;
            if (gap < minPlaceMs - compensation) {
                accumulate("place-rate gap=" + gap + "ms");
            }
        }

        lastCrystalPlaceMs = now;
    }

    private void handleCrystalBreak() {
        long now = System.currentTimeMillis();
        long pingMs = player.getTransactionPing();
        long compensation = Math.min(pingMs / 2, 100L);

        // Break-rate check
        if (lastCrystalBreakMs != 0L) {
            long gap = now - lastCrystalBreakMs;
            if (gap < minBreakMs - compensation) {
                accumulate("break-rate gap=" + gap + "ms");
            } else {
                decay();
            }
        }

        // Place→break sequence check
        if (lastCrystalPlaceMs != 0L) {
            long sincePlace = now - lastCrystalPlaceMs;
            if (sincePlace < minPlaceBreakMs - compensation) {
                sequenceCount++;
                if (sequenceCount >= sequenceThreshold) {
                    accumulate("place-break-seq count=" + sequenceCount);
                    sequenceCount = 0;
                }
            } else {
                sequenceCount = Math.max(0, sequenceCount - 1);
            }
        }

        lastCrystalBreakMs = now;
    }

    // -----------------------------------------------------------------------
    // Violation helpers
    // -----------------------------------------------------------------------

    private void accumulate(String verbose) {
        violations++;
        if (violations >= alertThreshold) {
            flagAndAlert(verbose);
            violations = 0;
        }
    }

    private void decay() {
        violations = Math.max(0, violations - 1);
    }

    // -----------------------------------------------------------------------
    // Config reload
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        minPlaceMs       = config.getIntElse("AutoCrystal.min-place-ms",        100);
        minBreakMs       = config.getIntElse("AutoCrystal.min-break-ms",        100);
        minPlaceBreakMs  = config.getIntElse("AutoCrystal.min-place-break-ms",   50);
        sequenceThreshold = config.getIntElse("AutoCrystal.sequence-threshold",   3);
        alertThreshold   = config.getIntElse("AutoCrystal.alert-threshold",       5);
    }
}
