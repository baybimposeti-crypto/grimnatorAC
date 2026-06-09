package com.grimnatorac.platform.fabric.mc1161;

import com.grimnatorac.platform.fabric.AbstractFabricPlatformServer;
import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.command.FabricPlayerSelectorParser;
import com.grimnatorac.platform.fabric.manager.FabricParserDescriptorFactory;
import com.grimnatorac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import com.grimnatorac.platform.fabric.mc1161.entity.Fabric1161GrimEntity;
import com.grimnatorac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import com.grimnatorac.platform.fabric.mc1161.player.Fabric1161PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import com.grimnatorac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.grimnatorac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.grimnatorac.platform.fabric.utils.message.IFabricMessageUtil;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class GrimnatorACFabric1161LoaderPlugin extends GrimnatorACFabricLoaderPlugin {

    public GrimnatorACFabric1161LoaderPlugin() {
        this(
            new FabricPlatformPlayerFactory(
                Fabric1161PlatformPlayer::new,
                Fabric1161GrimEntity::new,
                Fabric1161PlatformInventory::new
            ),
            new Fabric1140PlatformServer(),
            new Fabric1161MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected GrimnatorACFabric1161LoaderPlugin(
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        super(() -> new FabricParserDescriptorFactory(new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)),
            playerFactory,
            platformServer,
            fabricMessageUtil,
            fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_16_1;
    }
}
