package com.grimnatorac.command.commands;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.command.BuildableCommand;
import com.grimnatorac.manager.AlertManagerImpl;
import com.grimnatorac.manager.datastore.PlayerToggleStore;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.player.PlatformPlayer;
import com.grimnatorac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GrimBrands implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("GrimnatorAC", "GrimnatorAC")
                        .literal("brands", Description.of("Toggle brands for the sender"))
                        .permission("grimnatorac.brand")
                        .handler(this::handleBrands)
        );
    }

    private void handleBrands(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer p = Objects.requireNonNull(context.sender().getPlatformPlayer());
            AlertManagerImpl am = GrimAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasBrandsEnabled(p);
            am.setBrandsEnabled(p, newState, false);
            GrimAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore()
                    .applyUserToggle(p.getUniqueId(), PlayerToggleStore.KEY_BRANDS, newState);
        } else if (sender.isConsole()) {
            GrimAPI.INSTANCE.getAlertManager().toggleConsoleBrands();
        }
    }
}
