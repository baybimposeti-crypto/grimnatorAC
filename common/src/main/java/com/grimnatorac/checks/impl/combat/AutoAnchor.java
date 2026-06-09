package com.grimnatorac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

/**
 * AutoAnchor — detects automated respawn-anchor place/charge/detonate cycling.
 *
 * <p>The check tracks three action types from client packets:
 * <ol>
 *   <li><b>Place</b> — player places a RESPAWN_ANCHOR item via
 *       {@code PLAYER_BLOCK_PLACEMENT}.</li>
 *   <li><b>Charge</b> — player right-clicks a GLOWSTONE item onto a block
 *       (the server-side charging interaction comes as a block-placement
 *       packet with GLOWSTONE in hand).</li>
 *   <li><b>Detonate</b> — player right-clicks the anchor in the Nether/End
 *       (arrives as an {@code INTERACT_ENTITY} or a second
 *       {@code PLAYER_BLOCK_PLACEMENT}; in this check we use the block
 *       placement packet with RESPAWN_ANCHOR in hand as the detonation
 *       proxy since we cannot distinguish it at the packet level without a
 *       world lookup — flagging conservatively on the timing gap alone).</li>
 * </ol>
 *
 * <p>Each action pair has a configurable minimum gap.  Violations accumulate
 * silently and an alert fires after {@code alertThreshold} (default 5)
 * violations.</p>
 */
@CheckData(
        name = "AutoAnchor",
        stableKey = "grimnatorac.combat.auto_anchor",
        description = "Detects automated respawn-anchor place/charge/detonate cycling",
        decay = 0,
        setback = Integer.MAX_VALUE,
        experimental = false
)
public class AutoAnchor extends Check implements PacketCheck {

    // -----------------------------------------------------------------------
    // Config (hot-reloadable)
    // -----------------------------------------------------------------------

    private long minPlaceMs;
    private long minChargeMs;
    private long minPlaceChargeMs;
    private long minChargeDetonateMs;
    private int  alertThreshold;

    // -----------------------------------------------------------------------
    // Per-player state
    // -----------------------------------------------------------------------

    private long lastAnchorPlaceMs  = 0L;
    private long lastGlowstoneUseMs = 0L;

    /** Accumulated violations. */
    private int violations = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public AutoAnchor(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // PacketCheck
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement pkt = new WrapperPlayClientPlayerBlockPlacement(event);

        var heldItem = pkt.getHand() == InteractionHand.MAIN_HAND
                ? player.inventory.getHeldItem()
                : player.inventory.getOffHand();

        if (heldItem.getType() == ItemTypes.RESPAWN_ANCHOR) {
            handleAnchorAction();
        } else if (heldItem.getType() == ItemTypes.GLOWSTONE) {
            handleChargeAction();
        }
    }

    // -----------------------------------------------------------------------
    // Action handlers
    // -----------------------------------------------------------------------

    private void handleAnchorAction() {
        long now = System.currentTimeMillis();
        long compensation = Math.min(player.getTransactionPing() / 2, 100L);

        // Place rate
        if (lastAnchorPlaceMs != 0L) {
            long gap = now - lastAnchorPlaceMs;
            if (gap < minPlaceMs - compensation) {
                accumulate("anchor-place-rate gap=" + gap + "ms");
            }
        }

        // Detonation proxy: anchor used shortly after being charged
        if (lastGlowstoneUseMs != 0L) {
            long sinceCharge = now - lastGlowstoneUseMs;
            if (sinceCharge < minChargeDetonateMs - compensation) {
                accumulate("charge-detonate gap=" + sinceCharge + "ms");
            } else {
                decay();
            }
        }

        lastAnchorPlaceMs = now;
    }

    private void handleChargeAction() {
        long now = System.currentTimeMillis();
        long compensation = Math.min(player.getTransactionPing() / 2, 100L);

        // Charge rate
        if (lastGlowstoneUseMs != 0L) {
            long gap = now - lastGlowstoneUseMs;
            if (gap < minChargeMs - compensation) {
                accumulate("anchor-charge-rate gap=" + gap + "ms");
            }
        }

        // Place→charge gap
        if (lastAnchorPlaceMs != 0L) {
            long sincePlace = now - lastAnchorPlaceMs;
            if (sincePlace < minPlaceChargeMs - compensation) {
                accumulate("place-charge gap=" + sincePlace + "ms");
            } else {
                decay();
            }
        }

        lastGlowstoneUseMs = now;
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
        minPlaceMs           = config.getIntElse("AutoAnchor.min-place-ms",           200);
        minChargeMs          = config.getIntElse("AutoAnchor.min-charge-ms",          150);
        minPlaceChargeMs     = config.getIntElse("AutoAnchor.min-place-charge-ms",    100);
        minChargeDetonateMs  = config.getIntElse("AutoAnchor.min-charge-detonate-ms", 150);
        alertThreshold       = config.getIntElse("AutoAnchor.alert-threshold",          5);
    }
}
