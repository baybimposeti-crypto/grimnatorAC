package com.grimnatorac.utils.blockplace;

import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(GrimPlayer player, BlockPlace place);
}
