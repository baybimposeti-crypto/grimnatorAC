package com.grimnatorac.manager.init.start;

import com.grimnatorac.platform.api.command.CommandService;
import com.grimnatorac.utils.anticheat.LogUtil;

public record CommandRegister(CommandService service) implements StartableInitable {

    @Override
    public void start() {
        try {
            if (service != null) {
                service.registerCommands();
            }
        } catch (Throwable t) {
            // This is the ultimate safety net. If command registration fails, Grim keeps running.
            LogUtil.error("Failed to register commands! Grim will run without command support.", t);
        }
    }
}
