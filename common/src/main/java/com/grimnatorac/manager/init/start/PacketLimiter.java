package com.grimnatorac.manager.init.start;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.player.GrimPlayer;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
