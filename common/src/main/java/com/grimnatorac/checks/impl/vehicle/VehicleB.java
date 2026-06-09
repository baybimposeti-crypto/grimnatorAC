package com.grimnatorac.checks.impl.vehicle;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "VehicleB", stableKey = "grimnatorac.vehicle.spoofed_vehicle", description = "Claimed to be in a vehicle while not in a vehicle")
public class VehicleB extends Check implements PacketCheck {
    public VehicleB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE && !player.inVehicle()
                && flagAndAlert() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
