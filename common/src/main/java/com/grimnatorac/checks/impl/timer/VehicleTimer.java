package com.grimnatorac.checks.impl.timer;

import com.grimnatorac.checks.CheckData;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(name = "VehicleTimer", stableKey = "grimnatorac.timer.vehicle", setback = 10)
public class VehicleTimer extends Timer {
    private boolean isDummy = false;

    public VehicleTimer(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {
        // Ignore teleports
        if (player.packetStateData.lastPacketWasTeleport) return false;

        if (packetType == PacketType.Play.Client.VEHICLE_MOVE) {
            isDummy = false;
            return true; // Client controlling vehicle
        }

        if (packetType == PacketType.Play.Client.STEER_VEHICLE) {
            if (isDummy) { // Server is controlling vehicle
                return true;
            }
            isDummy = true; // Client is controlling vehicle
        }

        return false;
    }
}
