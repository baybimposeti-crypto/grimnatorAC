package com.grimnatorac.manager.init.start;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

public class ExemptOnlinePlayersOnReload implements StartableInitable {

    // Runs on plugin startup adding all online players to exempt list; will be empty unless reload
    // This essentially exists to stop you from shooting yourself in the foot by being stupid and using /reload
    @Override
    public void start() {
        for (PlatformPlayer player : GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player.getNative());
            GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.add(user);
        }
    }
}
