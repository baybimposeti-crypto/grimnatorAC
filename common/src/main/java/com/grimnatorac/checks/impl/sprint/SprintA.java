package com.grimnatorac.checks.impl.sprint;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SprintA", stableKey = "grimnatorac.sprint.hunger", description = "Sprinting with too low hunger", setback = 0)
public class SprintA extends Check implements PacketCheck {

    public SprintA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate || player.packetStateData.lastPacketWasTeleport) return;

        // Players can sprint if they're able to fly
        // Players can also sprint if they are on a camel, regardless of their hunger level
        if (player.canFly || EntityTypes.isTypeInstanceOf(player.getVehicleType(), EntityTypes.CAMEL)) return;

        if (player.food <= 6.0F) {
            if (player.isSprinting) {
                if (flagAndAlert("hunger=" + player.food)) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    setbackIfAboveSetbackVL();
                }
            } else {
                reward();
            }
        }
    }
}
