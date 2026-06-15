package com.grimnatorac.checks.impl.badpackets;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.player.GrimPlayer;

@CheckData(name = "BadPacketsN", stableKey = "grimnatorac.badpackets.invalid_teleport", setback = 0)
public class BadPacketsN extends Check {
    public BadPacketsN(final GrimPlayer player) {
        super(player);
    }
}
