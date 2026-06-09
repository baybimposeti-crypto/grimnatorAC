package com.grimnatorac.platform.fabric.mc1205.player;

import com.grimnatorac.platform.fabric.GrimnatorACFabricLoaderPlugin;
import com.grimnatorac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import net.minecraft.server.level.ServerPlayer;

public class Fabric1202PlatformPlayer extends Fabric1170PlatformPlayer {
    public Fabric1202PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.connection.disconnect(GrimnatorACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }
}
