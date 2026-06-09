package com.grimnatorac.platform.fabric.initables;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.manager.init.start.AbstractTickEndEvent;
import com.grimnatorac.player.GrimPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class FabricTickEndEvent extends AbstractTickEndEvent {

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }

        // Register the end-of-tick callback
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
    }

    private void onEndServerTick(MinecraftServer server) {
        tickAllPlayers();
    }

    private void tickAllPlayers() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableGrim) continue;
            super.onEndOfTick(player, true);
        }
    }
}
