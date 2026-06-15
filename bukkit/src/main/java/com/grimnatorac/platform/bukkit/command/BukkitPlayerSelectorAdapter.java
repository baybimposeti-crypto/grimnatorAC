package com.grimnatorac.platform.bukkit.command;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.platform.api.command.PlayerSelector;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.bukkit.sender.BukkitSenderFactory;
import lombok.RequiredArgsConstructor;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class BukkitPlayerSelectorAdapter implements PlayerSelector {
    private final SinglePlayerSelector bukkitSelector;

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return ((BukkitSenderFactory) GrimAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(((BukkitSenderFactory) GrimAPI.INSTANCE.getSenderFactory()).map(bukkitSelector.single()));
    }

    @Override
    public String inputString() {
        return bukkitSelector.inputString();
    }
}
