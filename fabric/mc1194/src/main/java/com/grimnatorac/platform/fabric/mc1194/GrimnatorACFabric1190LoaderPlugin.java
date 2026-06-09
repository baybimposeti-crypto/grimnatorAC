package com.grimnatorac.platform.fabric.mc1194;

import com.grimnatorac.platform.fabric.AbstractFabricPlatformServer;
import com.grimnatorac.platform.api.manager.CommandAdapter;
import com.grimnatorac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import com.grimnatorac.platform.fabric.command.FabricPlayerSelectorParser;
import com.grimnatorac.platform.fabric.manager.FabricParserDescriptorFactory;
import com.grimnatorac.platform.fabric.mc1171.GrimnatorACFabric1170LoaderPlugin;
import com.grimnatorac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1194.convert.Fabric1190MessageUtil;
import com.grimnatorac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import com.grimnatorac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import com.grimnatorac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import com.grimnatorac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.grimnatorac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.grimnatorac.platform.fabric.utils.message.IFabricMessageUtil;
import com.grimnatorac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class GrimnatorACFabric1190LoaderPlugin extends GrimnatorACFabric1170LoaderPlugin {

    public GrimnatorACFabric1190LoaderPlugin() {
        this(
                () -> new FabricParserDescriptorFactory(
                    new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)
            ),
            new FabricPlatformPlayerFactory(
                    Fabric1170PlatformPlayer::new,
                    Fabric1194GrimEntity::new,
                    PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_19_2)
                            ? Fabric1193PlatformInventory::new : Fabric1161PlatformInventory::new
            ),
            new Fabric1190PlatformServer(),
            new Fabric1190MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected GrimnatorACFabric1190LoaderPlugin(
            LazyHolder<CommandAdapter> parserDescriptorFactory,
            FabricPlatformPlayerFactory platformPlayerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil) {
        super(parserDescriptorFactory, platformPlayerFactory, platformServer, fabricMessageUtil, fabricConversionUtil);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_19_4;
    }
}
