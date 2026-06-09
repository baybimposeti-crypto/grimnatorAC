package com.grimnatorac.checks.impl.breaking;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockBreakCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "PositionBreakB", stableKey = "grimnatorac.breaking.position_break_b")
public class PositionBreakB extends Check implements BlockBreakCheck {
    private final boolean allowLegacyFace = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10);
    private BlockFace lastFace;

    public PositionBreakB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            flagAndAlert("lastFace=" + lastFace + ", action=" + blockBreak.action);
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            // as of https://github.com/ViaVersion/ViaRewind/commit/e7b0606e187afbccf98ef7c88d3f3af27fe11da3,
            // ViaRewind maps face 255 for 1.7 clients to 0. Let's allow both, just to be safe
            lastFace = blockBreak.faceId == 0 || allowLegacyFace && blockBreak.faceId == 255 ? null : blockBreak.face;
        }
    }
}
