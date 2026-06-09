package com.grimnatorac.checks.impl.breaking;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.BlockBreakCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.MessageUtil;
import com.grimnatorac.utils.anticheat.update.BlockBreak;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", stableKey = "grimnatorac.breaking.multi_break", experimental = true)
public class MultiBreak extends Check implements BlockBreakCheck {
    private final List<String> flags = new ArrayList<>();
    private boolean hasBroken;
    private BlockFace lastFace;
    private Vector3i lastPos;

    public MultiBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        if (hasBroken && (blockBreak.face != lastFace || !blockBreak.position.equals(lastPos))) {
            final String verbose = "face=" + blockBreak.face + ", lastFace=" + lastFace
                    + ", pos=" + MessageUtil.toUnlabledString(blockBreak.position)
                    + ", lastPos=" + MessageUtil.toUnlabledString(lastPos);
            if (!player.canSkipTicks()) {
                if (flagAndAlert(verbose) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                flags.add(verbose);
            }
        }

        lastFace = blockBreak.face;
        lastPos = blockBreak.position;
        hasBroken = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasBroken = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
