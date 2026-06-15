package com.grimnatorac.checks.impl.sprint;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.grimnatorac.utils.enums.Pose;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.Collections;

@CheckData(name = "SprintB", stableKey = "grimnatorac.sprint.sneaking", description = "Sprinting while sneaking or crawling", setback = 5, experimental = true)
public class SprintB extends Check implements PostPredictionCheck {
    public SprintB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.isSlowMovement && player.sneakingSpeedMultiplier < 0.8f && predictionComplete.isChecked()) {
            ClientVersion version = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14_2) && version != ClientVersion.V_1_21_4) {
                return;
            }

            // https://github.com/GrimnatorAC/GrimnatorAC/issues/1932
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14) && player.wasFlying && player.lastPose == Pose.FALL_FLYING && !player.isGliding) {
                return;
            }

            // https://github.com/GrimnatorAC/GrimnatorAC/issues/1948
            if (version == ClientVersion.V_1_21_4 && (Collections.max(player.uncertaintyHandler.pistonX) != 0
                    || Collections.max(player.uncertaintyHandler.pistonY) != 0
                    || Collections.max(player.uncertaintyHandler.pistonZ) != 0)) {
                return;
            }

            if (!player.wasTouchingWater || version.isOlderThan(ClientVersion.V_1_13)) {
                return;
            }

            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else reward();
        }
    }
}
