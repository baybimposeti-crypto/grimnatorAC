package com.grimnatorac.command.commands;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.BuildableCommand;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class GrimReload implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("GrimnatorAC", "GrimnatorAC")
                        .literal("reload")
                        .permission("grimnatorac.reload")
                        .handler(this::handleReload)
        );
    }

    private void handleReload(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        // reload config
        sender.sendMessage(MessageUtil.getParsedComponent(sender, "reloading", "%prefix% &7Reloading config..."));

        GrimAPI.INSTANCE.getExternalAPI().reloadAsync().exceptionally(throwable -> false)
                .thenAccept(bool -> {
                    Component message = bool
                            ? MessageUtil.getParsedComponent(sender, "reloaded", "%prefix% &fConfig has been reloaded.")
                            : MessageUtil.getParsedComponent(sender, "reload-failed", "%prefix% &cFailed to reload config.");
                    sender.sendMessage(message);
                });
    }
}
