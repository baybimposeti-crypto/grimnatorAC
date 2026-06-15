package com.grimnatorac.checks.impl.vehicle;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;

@CheckData(name = "VehicleA", stableKey = "grimnatorac.vehicle.impossible_input", description = "Impossible input values")
public class VehicleA extends Check implements PacketCheck {
    public VehicleA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            final WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

            if (Math.abs(packet.getForward()) > 0.98f || Math.abs(packet.getSideways()) > 0.98f) {
                if (flagAndAlert("forwards=" + packet.getForward() + ", sideways=" + packet.getSideways()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
