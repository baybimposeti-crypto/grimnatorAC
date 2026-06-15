package com.grimnatorac.checks.impl.aim;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.RotationCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimDuplicateLook", stableKey = "grimnatorac.aim.duplicate_look")
public class AimDuplicateLook extends Check implements RotationCheck {
    private boolean exempt;

    public AimDuplicateLook(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate || player.compensatedEntities.self.getRiding() != null) {
            exempt = true;
            return;
        }

        if (exempt) { // Exempt for a tick on teleport
            exempt = false;
            return;
        }

        if (rotationUpdate.getFrom().equals(rotationUpdate.getTo())) {
            flagAndAlert();
        }
    }
}
