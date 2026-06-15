package com.grimnatorac.checks.debug;

import com.grimnatorac.checks.Check;
import com.grimnatorac.player.GrimPlayer;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(GrimPlayer player) {
        super(player);
    }

    public abstract void toggleListener(GrimPlayer player);

    public abstract boolean toggleConsoleOutput();
}
