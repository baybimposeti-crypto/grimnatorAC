package com.grimnatorac.platform.fabric.mc1161;

import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.fabric.AbstractFabricPlatformServer;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import net.minecraft.commands.CommandSourceStack;

public class Fabric1140PlatformServer extends AbstractFabricPlatformServer {

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = GrimnatorACFabricLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getCommands().performCommand(commandSource, command);
    }

    // TODO (Cross-platform) implement proper bukkit equivalent for getting TPS over time
    @Override
    public double getTPS() {
        return Math.min(1000.0 / GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getAverageTickTime(), 20.0);
    }
}
