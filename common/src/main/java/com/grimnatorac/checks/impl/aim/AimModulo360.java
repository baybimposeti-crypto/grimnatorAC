package com.grimnatorac.checks.impl.aim;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.RotationCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.RotationUpdate;

// Based on Kauri AimA,
// I also discovered this flaw before open source Kauri, but did not want to open source its detection.
// It works on clients who % 360 their rotation.
@CheckData(name = "AimModulo360", stableKey = "grimnatorac.aim.modulo_360", decay = 0.005)
public class AimModulo360 extends Check implements RotationCheck {

    private float lastDeltaYaw;

    public AimModulo360(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        // Exempt for teleport, entering a vehicle due to rotation reset or
        // after forced, client-sided rotation change after interacting with a horse (not necessarily mounting it)
        if (player.packetStateData.lastPacketWasTeleport || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation) {
            lastDeltaYaw = rotationUpdate.getDeltaXRot();
            return;
        }

        if (player.yaw < 360 && player.yaw > -360 && Math.abs(rotationUpdate.getDeltaXRot()) > 320 && Math.abs(lastDeltaYaw) < 30) {
            flagAndAlert();
        } else {
            reward();
        }

        lastDeltaYaw = rotationUpdate.getDeltaXRot();
    }
}
