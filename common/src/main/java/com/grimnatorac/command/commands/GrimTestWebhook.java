package com.grimnatorac.command.commands;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.BuildableCommand;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.utils.anticheat.LogUtil;
import com.grimnatorac.utils.anticheat.MessageUtil;
import com.grimnatorac.utils.data.webhook.discord.WebhookMessage;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class GrimTestWebhook implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("GrimnatorAC", "GrimnatorAC")
                        .literal("testwebhook")
                        .permission("grimnatorac.testwebhook")
                        .handler(this::handleTestWebhook)
        );
    }

    private void handleTestWebhook(@NotNull CommandContext<Sender> context) {
        if (GrimAPI.INSTANCE.getDiscordManager().isDisabled()) {
            context.sender().sendMessage(MessageUtil.miniMessage(GrimAPI.INSTANCE.getConfigManager().getWebhookNotEnabled()));
            return;
        }

        WebhookMessage webhookMessage = new WebhookMessage().content(GrimAPI.INSTANCE.getConfigManager().getWebhookTestMessage());
        GrimAPI.INSTANCE.getDiscordManager().sendWebhookMessage(webhookMessage).whenCompleteAsync(((successful, throwable) -> {
            if (successful == true) {
                context.sender().sendMessage(MessageUtil.miniMessage(GrimAPI.INSTANCE.getConfigManager().getWebhookTestSucceeded()));
                return;
            }

            context.sender().sendMessage(MessageUtil.miniMessage(GrimAPI.INSTANCE.getConfigManager().getWebhookTestFailed()));

            if (throwable != null) {
                LogUtil.error("Exception caught while sending a Discord webhook test alert", throwable);
            }
        }));
    }
}
