package com.grimnatorac.platform.fabric.mc1205;

import com.grimnatorac.platform.fabric.command.FabricPlayerSelectorParser;
import com.grimnatorac.platform.fabric.manager.FabricParserDescriptorFactory;
import com.grimnatorac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1194.Fabric1190PlatformServer;
import com.grimnatorac.platform.fabric.mc1194.GrimnatorACFabric1190LoaderPlugin;
import com.grimnatorac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import com.grimnatorac.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import com.grimnatorac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import com.grimnatorac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import com.grimnatorac.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import com.grimnatorac.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import com.grimnatorac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;

public class GrimnatorACFabric1200LoaderPlugin extends GrimnatorACFabric1190LoaderPlugin {

    public GrimnatorACFabric1200LoaderPlugin() {
        super(
                () -> new FabricParserDescriptorFactory(
                        new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)
                ),
                new FabricPlatformPlayerFactory(
                        FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_1)
                                ? Fabric1202PlatformPlayer::new : Fabric1170PlatformPlayer::new,
                        Fabric1194GrimEntity::new,
                        Fabric1193PlatformInventory::new
                ),
                FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_2)
                        ? new Fabric1203PlatformServer() : new Fabric1190PlatformServer(),
                new Fabric1200MessageUtil(),
                FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_4)
                        ? new Fabric1205ConversionUtil() : new Fabric1140ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_20_5;
    }
}
