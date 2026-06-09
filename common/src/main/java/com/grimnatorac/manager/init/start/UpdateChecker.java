package com.grimnatorac.manager.init.start;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.commands.GrimVersion;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimVersion.checkForUpdatesAsync(GrimAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
