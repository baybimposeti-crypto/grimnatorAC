package com.grimnatorac.checks.impl.scaffolding;

import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockPlaceCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceB", stableKey = "grimnatorac.scaffolding.invalid_place_b", description = "Sent impossible block face id")
public class InvalidPlaceB extends BlockPlaceCheck {
    public InvalidPlaceB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getFaceId() == 255 && PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        if (place.getFaceId() < 0 || place.getFaceId() > 5) {
            // ban
            if (flagAndAlert("direction=" + place.getFaceId()) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
