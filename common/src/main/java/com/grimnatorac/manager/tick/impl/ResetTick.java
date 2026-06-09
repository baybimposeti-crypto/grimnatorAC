package com.grimnatorac.manager.tick.impl;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.manager.tick.Tickable;
import com.grimnatorac.player.GrimPlayer;

public class ResetTick implements Tickable {
    @Override
    public void tick() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.checkManager.getPacketEntityReplication().tickStartTick();
        }
    }
}
