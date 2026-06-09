package com.grimnatorac.checks.impl.multiactions;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockBreakCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

@CheckData(name = "MultiActionsB", stableKey = "grimnatorac.multiactions.break_while_using", description = "Breaking blocks while using an item", experimental = true)
public class MultiActionsB extends Check implements BlockBreakCheck {
    public MultiActionsB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.packetStateData.isSlowedByUsingItem() && (player.packetStateData.lastSlotSelected == player.packetStateData.getSlowedByUsingItemSlot() || player.packetStateData.itemInUseHand == InteractionHand.OFF_HAND)) {
            // this is vanilla on 1.7
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                return;
            }

            if (flagAndAlert() && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
