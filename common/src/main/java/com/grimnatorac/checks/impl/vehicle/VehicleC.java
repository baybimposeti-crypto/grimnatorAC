package com.grimnatorac.checks.impl.vehicle;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.player.GrimPlayer;

@CheckData(name = "VehicleC", stableKey = "grimnatorac.vehicle.vehicle_control")
public class VehicleC extends Check {
    public VehicleC(GrimPlayer player) {
        super(player);
    }
}
