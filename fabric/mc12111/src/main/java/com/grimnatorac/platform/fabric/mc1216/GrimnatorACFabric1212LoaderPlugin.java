package com.grimnatorac.platform.fabric.mc1216;

import com.grimnatorac.platform.fabric.mc1216.command.Fabric1212PlayerSelectorAdapter;
import com.grimnatorac.platform.fabric.command.FabricPlayerSelectorParser;
import com.grimnatorac.platform.fabric.manager.FabricParserDescriptorFactory;
import com.grimnatorac.platform.fabric.mc1194.GrimnatorACFabric1190LoaderPlugin;
import com.grimnatorac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import com.grimnatorac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import com.grimnatorac.platform.fabric.mc1205.Fabric1203PlatformServer;
import com.grimnatorac.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import com.grimnatorac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import com.grimnatorac.platform.fabric.mc1216.convert.Fabric1216ConversionUtil;
import com.grimnatorac.platform.fabric.mc1216.player.Fabric1212PlatformPlayer;
import com.grimnatorac.platform.fabric.mc1216.player.Fabric1215PlatformInventory;
import com.grimnatorac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.grimnatorac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class GrimnatorACFabric1212LoaderPlugin extends GrimnatorACFabric1190LoaderPlugin {

    public GrimnatorACFabric1212LoaderPlugin() {
        super(
                LazyHolder.simple(() -> new FabricParserDescriptorFactory(
                        new FabricPlayerSelectorParser<>(Fabric1212PlayerSelectorAdapter::new)
                )),
                new FabricPlatformPlayerFactory(
                        Fabric1212PlatformPlayer::new,
                        Fabric1194GrimEntity::new,
                        PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_21_4)
                            ? Fabric1215PlatformInventory::new : Fabric1193PlatformInventory::new
                ),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_21_10) ?
                        new Fabric12111PlatformServer() : new Fabric1203PlatformServer(),
                new Fabric1200MessageUtil(),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_21_5)
                        ? new Fabric1216ConversionUtil() : new Fabric1205ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_21_11;
    }
}
