package com.grimnatorac.predictionengine.blockeffects;

import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.math.Vector3dm;

import java.util.List;

public interface BlockEffectsResolver {

    void applyEffectsFromBlocks(GrimPlayer player, Vector3dm clientVelocity, boolean onlyApplyVelocity, List<GrimPlayer.Movement> movements);

}
