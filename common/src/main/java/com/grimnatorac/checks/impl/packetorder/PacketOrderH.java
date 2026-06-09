package com.grimnatorac.checks.impl.packetorder;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "PacketOrderH", stableKey = "grimnatorac.packetorder.sneak_sprint_order", experimental = true)
public class PacketOrderH extends Check implements PostPredictionCheck {
    public PacketOrderH(final GrimPlayer player) {
        super(player);
    }

    private int invalid;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SPRINTING, STOP_SPRINTING -> {
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2) && player.packetOrderProcessor.isSneaking()) {
                        if (!player.canSkipTicks()) {
                            flagAndAlert();
                        } else {
                            invalid++;
                        }
                    }
                }
                case START_SNEAKING, STOP_SNEAKING -> {
                    if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && player.packetOrderProcessor.isSprinting()) {
                        if (!player.canSkipTicks()) {
                            flagAndAlert();
                        } else {
                            invalid++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
    }
}
