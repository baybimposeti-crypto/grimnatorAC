package com.grimnatorac.checks.impl.badpackets;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(name = "BadPacketsB", stableKey = "grimnatorac.badpackets.ignored_rotation", description = "Ignored set rotation packet")
public class BadPacketsB extends Check implements PacketCheck {

    public BadPacketsB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTransaction(event.getPacketType())) {
            player.pendingRotations.removeIf(data -> {
                if (player.getLastTransactionReceived() > data.getTransaction()) {
                    if (!data.isAccepted()) {
                        flagAndAlert();
                    }

                    return true;
                }

                return false;
            });
        }
    }
}
