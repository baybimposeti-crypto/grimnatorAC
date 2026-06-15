package com.grimnatorac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.grimnatorac.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleCheck extends AbstractCheck {

    void process(final VehiclePositionUpdate vehicleUpdate);
}
