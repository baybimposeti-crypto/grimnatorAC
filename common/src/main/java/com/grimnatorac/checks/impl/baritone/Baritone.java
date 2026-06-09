package com.grimnatorac.checks.impl.baritone;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.impl.aim.processor.AimProcessor;
import com.grimnatorac.checks.type.RotationCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.RotationUpdate;
import com.grimnatorac.utils.data.HeadRotation;
import com.grimnatorac.utils.math.GrimMath;

// This check has been patched by Baritone for a long time, and it also seems to false with cinematic camera now, so it is disabled.
@CheckData(name = "Baritone", stableKey = "grimnatorac.baritone.baritone")
public class Baritone extends Check implements RotationCheck {
    private int verbose;

    public Baritone(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final HeadRotation from = rotationUpdate.getFrom();
        final HeadRotation to = rotationUpdate.getTo();

        final float deltaPitch = Math.abs(to.pitch() - from.pitch());

        // Baritone works with small degrees, limit to 1 degree to pick up on baritone slightly moving aim to bypass anticheats
        if (rotationUpdate.getDeltaXRot() == 0 && deltaPitch > 0 && deltaPitch < 1 && Math.abs(to.pitch()) != 90.0f) {
            if (rotationUpdate.getProcessor().divisorY < GrimMath.MINIMUM_DIVISOR) {
                verbose++;
                if (verbose > 8) {
                    flagAndAlert("Divisor " + AimProcessor.convertToSensitivity(rotationUpdate.getProcessor().divisorX));
                }
            } else {
                verbose = 0;
            }
        }
    }
}
