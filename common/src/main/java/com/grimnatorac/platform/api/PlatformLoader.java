package com.grimnatorac.platform.api;

import ac.grim.grimac.api.plugin.GrimPlugin;
import com.grimnatorac.platform.api.command.CommandService;
import com.grimnatorac.platform.api.manager.ItemResetHandler;
import com.grimnatorac.platform.api.manager.MessagePlaceHolderManager;
import com.grimnatorac.platform.api.manager.PermissionRegistrationManager;
import com.grimnatorac.platform.api.manager.PlatformPluginManager;
import com.grimnatorac.platform.api.player.PlatformPlayerFactory;
import com.grimnatorac.platform.api.scheduler.PlatformScheduler;
import com.grimnatorac.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.jetbrains.annotations.NotNull;

public interface PlatformLoader {
    PlatformScheduler getScheduler();

    PlatformPlayerFactory getPlatformPlayerFactory();

    PacketEventsAPI<?> getPacketEvents();

    ItemResetHandler getItemResetHandler();

    CommandService getCommandService();

    SenderFactory<?> getSenderFactory();

    GrimPlugin getPlugin();

    PlatformPluginManager getPluginManager();

    PlatformServer getPlatformServer();

    // Intended for use for platform specific service/API bringup
    // Method will be called when InitManager.load() is called
    void registerAPIService();

    // Used to replace text placeholders in messages
    // Currently only supports PlaceHolderAPI on Bukkit
    @NotNull
    MessagePlaceHolderManager getMessagePlaceHolderManager();

    PermissionRegistrationManager getPermissionManager();
}
