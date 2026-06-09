package com.grimnatorac.platform.fabric;

import com.grimnatorac.platform.api.PlatformServer;
import com.grimnatorac.platform.api.sender.Sender;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFabricPlatformServer implements PlatformServer {

    public int getOperatorPermissionLevel() {
        return GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getOperatorUserPermissionLevel();
    }

    public boolean hasPermission(CommandSourceStack stack, int level) {
        return stack.hasPermission(level);
    }

    @Override
    public String getPlatformImplementationString() {
        // Return the Fabric server version
        return "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString() + " (MC: " + GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getServerVersion() + ")";
    }

    @Override
    public Sender getConsoleSender() {
        CommandSourceStack consoleSource = GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.createCommandSourceStack();
        return GrimnatorACFabricLoaderPlugin.LOADER.getFabricSenderFactory().wrap(consoleSource);
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public GameProfile getProfileByName(String name) {
        return GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getProfileCache().get(name);
    }
}
