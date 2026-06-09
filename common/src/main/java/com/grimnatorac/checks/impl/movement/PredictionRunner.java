package com.grimnatorac.checks.impl.movement;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.type.PositionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PositionUpdate;

public class PredictionRunner extends Check implements PositionCheck {
    public PredictionRunner(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (!player.inVehicle()) {
            player.movementCheckRunner.processAndCheckMovementPacket(positionUpdate);
        }
    }
}
