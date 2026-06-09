package com.grimnatorac.checks.impl.flight;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

// This check catches 100% of cheaters.
public class FlightA extends Check implements PacketCheck {
    public FlightA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // If the player sends a flying packet, but they aren't flying, then they are cheating.
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.isFlying) {
            flag();
        }
    }
}
