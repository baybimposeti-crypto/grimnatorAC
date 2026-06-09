package com.grimnatorac.manager.tick.impl;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.manager.tick.Tickable;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            // channel was somehow closed without us getting a disconnect event
            if (!ChannelHelper.isOpen(player.user.getChannel())) {
                GrimAPI.INSTANCE.getPlayerDataManager().onDisconnect(player.user);
                continue;
            }

            player.pollData();
        }
    }
}
