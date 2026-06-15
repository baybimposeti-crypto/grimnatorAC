package com.grimnatorac.checks.impl.sprint;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintC", stableKey = "grimnatorac.sprint.using_item", description = "Sprinting while using an item", setback = 5, experimental = true)
public class SprintC extends Check implements PostPredictionCheck {
    private boolean flaggedLastTick = false;

    public SprintC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.packetStateData.isSlowedByUsingItem()) {
            ClientVersion version = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14_2) && version != ClientVersion.V_1_21_4) {
                return;
            }

            if (!player.wasTouchingWater || version.isOlderThan(ClientVersion.V_1_13)) {
                flaggedLastTick = false;
                return;
            }

            if (player.isSprinting) {
                if (flaggedLastTick) flagAndAlertWithSetback();
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
    }
}
