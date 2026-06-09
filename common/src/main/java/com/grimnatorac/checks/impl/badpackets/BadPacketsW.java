package com.grimnatorac.checks.impl.badpackets;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.player.GrimPlayer;

@CheckData(name = "BadPacketsW", stableKey = "grimnatorac.badpackets.invalid_entity_target", description = "Interacted with non-existent entity", experimental = true)
public class BadPacketsW extends Check {
    public BadPacketsW(GrimPlayer player) {
        super(player);
    }
}
