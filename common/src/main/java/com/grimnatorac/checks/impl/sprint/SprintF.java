package com.grimnatorac.checks.impl.sprint;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintF", stableKey = "grimnatorac.sprint.gliding", description = "Sprinting while gliding", experimental = true)
public class SprintF extends Check implements PostPredictionCheck {
    public SprintF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.wasGliding && player.isGliding && player.getClientVersion() == ClientVersion.V_1_21_4) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else {
                reward();
            }
        }
    }
}
