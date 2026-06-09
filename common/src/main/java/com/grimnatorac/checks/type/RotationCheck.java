package com.grimnatorac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import com.grimnatorac.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends AbstractCheck {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
