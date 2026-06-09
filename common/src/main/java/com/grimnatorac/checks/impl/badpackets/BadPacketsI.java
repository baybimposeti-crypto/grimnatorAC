package com.grimnatorac.checks.impl.badpackets;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;

@CheckData(name = "BadPacketsI", stableKey = "grimnatorac.badpackets.spoofed_abilities", description = "Claimed to be flying while unable to fly")
public class BadPacketsI extends Check implements PacketCheck {
    public BadPacketsI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES
                && new WrapperPlayClientPlayerAbilities(event).isFlying() && !player.canFly
                && flagAndAlert() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
