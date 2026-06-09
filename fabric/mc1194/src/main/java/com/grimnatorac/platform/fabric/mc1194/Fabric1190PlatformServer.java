package com.grimnatorac.platform.fabric.mc1194;

import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.mc1171.Fabric1171PlatformServer;
import net.minecraft.commands.CommandSourceStack;

public class Fabric1190PlatformServer extends Fabric1171PlatformServer {
    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = GrimnatorACFabricLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        GrimnatorACFabricLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(commandSource, command);
    }
}
