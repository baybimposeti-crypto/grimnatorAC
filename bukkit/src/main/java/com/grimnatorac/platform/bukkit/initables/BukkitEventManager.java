package com.grimnatorac.platform.bukkit.initables;

import com.grimnatorac.manager.init.start.StartableInitable;
import com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin;
import com.grimnatorac.platform.bukkit.events.PistonEvent;
import com.grimnatorac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Registering singular bukkit event... (PistonEvent)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), GrimnatorACBukkitLoaderPlugin.LOADER);
    }
}
