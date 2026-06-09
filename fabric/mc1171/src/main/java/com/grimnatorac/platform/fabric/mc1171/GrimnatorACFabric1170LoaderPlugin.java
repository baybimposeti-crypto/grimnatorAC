package com.grimnatorac.platform.fabric.mc1171;

import com.grimnatorac.platform.fabric.AbstractFabricPlatformServer;
import com.grimnatorac.platform.api.manager.CommandAdapter;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.command.FabricPlayerSelectorParser;
import com.grimnatorac.platform.fabric.manager.FabricParserDescriptorFactory;
import com.grimnatorac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1161.Fabric1140PlatformServer;
import com.grimnatorac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import com.grimnatorac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import com.grimnatorac.platform.fabric.mc1171.entity.Fabric1170GrimEntity;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import com.grimnatorac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.grimnatorac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.grimnatorac.platform.fabric.utils.message.IFabricMessageUtil;
import com.grimnatorac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class GrimnatorACFabric1170LoaderPlugin extends GrimnatorACFabricLoaderPlugin {

    public GrimnatorACFabric1170LoaderPlugin() {
        this(() -> new FabricParserDescriptorFactory(
                        new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)
                ),
                new FabricPlatformPlayerFactory(
                        Fabric1170PlatformPlayer::new,
                        Fabric1170GrimEntity::new,
                        Fabric1161PlatformInventory::new
                ),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_17)
                        ? new Fabric1171PlatformServer() : new Fabric1140PlatformServer(),
                new Fabric1161MessageUtil(),
                new Fabric1140ConversionUtil()
        );
    }

    protected GrimnatorACFabric1170LoaderPlugin(LazyHolder<CommandAdapter> parserDescriptorFactory,
                                           FabricPlatformPlayerFactory playerFactory,
                                           AbstractFabricPlatformServer platformServer,
                                           IFabricMessageUtil fabricMessageUtil,
                                           IFabricConversionUtil fabricConversionUtil) {
        super(
                parserDescriptorFactory,
                playerFactory,
                platformServer,
                fabricMessageUtil,
                fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_17_1;
    }
}
