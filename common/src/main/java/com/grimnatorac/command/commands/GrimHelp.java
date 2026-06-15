package com.grimnatorac.command.commands;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.BuildableCommand;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class GrimHelp implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("GrimnatorAC", "GrimnatorAC")
                        .literal("help", Description.of("Display help information"))
                        .permission("grimnatorac.help")
                        .handler(this::handleHelp)
        );
    }

    private void handleHelp(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        for (String string : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("help")) {
            if (string == null) continue;
            string = MessageUtil.replacePlaceholders(sender, string);
            sender.sendMessage(MessageUtil.miniMessage(string));
        }
    }
}
