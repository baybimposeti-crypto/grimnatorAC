package com.grimnatorac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.grimnatorac.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
