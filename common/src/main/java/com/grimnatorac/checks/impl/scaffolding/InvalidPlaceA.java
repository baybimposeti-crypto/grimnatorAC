package com.grimnatorac.checks.impl.scaffolding;

import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockPlaceCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "InvalidPlaceA", stableKey = "grimnatorac.scaffolding.invalid_place_a", description = "Sent invalid cursor position")
public class InvalidPlaceA extends BlockPlaceCheck {
    public InvalidPlaceA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;
        if (!Float.isFinite(cursor.x) || !Float.isFinite(cursor.y) || !Float.isFinite(cursor.z)) {
            if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
