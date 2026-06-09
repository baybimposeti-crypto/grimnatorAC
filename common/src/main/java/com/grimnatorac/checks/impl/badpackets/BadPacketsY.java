package com.grimnatorac.checks.impl.badpackets;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

@CheckData(name = "BadPacketsY", stableKey = "grimnatorac.badpackets.oob_slot", description = "Sent out of bounds slot id")
public class BadPacketsY extends Check implements PacketCheck {
    public BadPacketsY(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
            if (slot > 8 || slot < 0) { // ban
                if (flagAndAlert("slot=" + slot) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
