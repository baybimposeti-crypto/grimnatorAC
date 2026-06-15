package com.grimnatorac.checks.impl.sprint;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "SprintG", stableKey = "grimnatorac.sprint.water", description = "Sprinting while in water", experimental = true)
public class SprintG extends Check implements PostPredictionCheck {
    public SprintG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.wasTouchingWater && (player.wasWasTouchingWater || player.getClientVersion() == ClientVersion.V_1_21_4)
                && !player.wasEyeInWater && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)
                && player.wasLastPredictionCompleteChecked && predictionComplete.isChecked()
                && !EntityTypes.isTypeInstanceOf(player.getVehicleType(), EntityTypes.CAMEL)
                && !player.isSwimming) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else {
                reward();
            }
        }
    }
}
