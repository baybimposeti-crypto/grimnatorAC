package com.grimnatorac.checks.impl.crash;

import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockPlaceCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockBreak;
import com.grimnatorac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

@CheckData(name = "CrashG", stableKey = "grimnatorac.crash.negative_sequence", description = "Sent negative sequence id")
public class CrashG extends BlockPlaceCheck {

    public CrashG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion()) {
            WrapperPlayClientUseItem use = new WrapperPlayClientUseItem(event);
            if (use.getSequence() < 0) {
                flagAndAlert();
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.sequence < 0 && isSupportedVersion()) {
            flagAndAlert();
            blockBreak.cancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (place.sequence < 0 && isSupportedVersion()) {
            flagAndAlert();
            place.resync();
        }
    }

    private boolean isSupportedVersion() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);
    }

}
