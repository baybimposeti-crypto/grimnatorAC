package com.grimnatorac.platform.bukkit.initables;

import com.grimnatorac.manager.init.start.StartableInitable;
import com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin;
import com.grimnatorac.utils.anticheat.Constants;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements StartableInitable {
    @Override
    public void start() {
        try {
            new Metrics(GrimnatorACBukkitLoaderPlugin.LOADER, Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }
}
