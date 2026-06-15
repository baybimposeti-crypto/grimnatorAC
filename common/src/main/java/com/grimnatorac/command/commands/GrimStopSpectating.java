package com.grimnatorac.command.commands;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.BuildableCommand;
import com.grimnatorac.command.CloudCommandService;
import com.grimnatorac.command.requirements.PlayerSenderRequirement;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.utils.anticheat.MessageUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.Objects;

public class GrimStopSpectating implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("GrimnatorAC", "GrimnatorAC")
                        .literal("stopspectating")
                        .permission("grimnatorac.spectate")
                        .optional("here", StringParser.stringParser(), SuggestionProvider.blocking((ctx, in) -> {
                            if (ctx.sender().hasPermission("grimnatorac.spectate.stophere")) {
                                return List.of(Suggestion.suggestion("here"));
                            }
                            return List.of(); // No suggestions if no permission
                        }))
                        .handler(this::onStopSpectate)
                        .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.INSTANCE))
        );
    }

    public void onStopSpectate(CommandContext<Sender> commandContext) {
        Sender sender = commandContext.sender();
        String string = commandContext.getOrDefault("here", null);
        if (GrimAPI.INSTANCE.getSpectateManager().isSpectating(sender.getUniqueId())) {
            boolean teleportBack = string == null || !string.equalsIgnoreCase("here") || !sender.hasPermission("grimnatorac.spectate.stophere");
            GrimAPI.INSTANCE.getSpectateManager().disable(Objects.requireNonNull(sender.getPlatformPlayer()), teleportBack);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-spectate-return", "%prefix% &cYou can only do this after spectating a player."));
        }
    }
}
