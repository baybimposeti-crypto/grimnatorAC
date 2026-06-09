package com.grimnatorac.platform.fabric.mc1216.command;

import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

public class Fabric1212PlayerSelectorAdapter extends Fabric1161PlayerSelectorAdapter {

    public Fabric1212PlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        super(fabricSelector);
    }

    // 1.21.2 .getCommandSource() moves from entity to player
    @Override
    public Sender getSinglePlayer() {
        return GrimnatorACFabricLoaderPlugin.LOADER.getFabricSenderFactory().wrap(fabricSelector.single().createCommandSourceStack());
    }
}
