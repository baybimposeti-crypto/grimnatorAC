package com.grimnatorac.checks.impl.chat;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;

// this can false from click events, but I doubt this would actually
// happen unless they're trying to flag, or if the server is set up badly
@CheckData(name = "ChatB", stableKey = "grimnatorac.exploit.spigot_antispam_bypass", description = "Invalid chat message")
public class ChatB extends Check implements PacketCheck {
    public ChatB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            String message = new WrapperPlayClientChatMessage(event).getMessage();
            if (message.isEmpty() || !message.trim().equals(message)
                    || message.startsWith("/") && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
                if (flagAndAlert("message=" + message) && shouldModifyPackets()) {
                    player.onPacketCancel();
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {
            String command = "/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand();
            if (!command.stripTrailing().equals(command)) {
                if (flagAndAlert("command=" + command)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            String command = "/" + new WrapperPlayClientChatCommand(event).getCommand();
            if (!command.trim().equals(command)) {
                if (flagAndAlert("command=" + command)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
