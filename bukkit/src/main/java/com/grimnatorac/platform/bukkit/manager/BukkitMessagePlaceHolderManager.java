package com.grimnatorac.platform.bukkit.manager;

import com.grimnatorac.platform.api.manager.MessagePlaceHolderManager;
import com.grimnatorac.platform.api.player.PlatformPlayer;
import com.grimnatorac.platform.bukkit.player.BukkitPlatformPlayer;
import com.grimnatorac.utils.reflection.ReflectionUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitMessagePlaceHolderManager implements MessagePlaceHolderManager {
    public static final boolean hasPlaceholderAPI = ReflectionUtils.hasClass("me.clip.placeholderapi.PlaceholderAPI");

    @Override
    public @NotNull String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        if (!hasPlaceholderAPI) return string;
        return PlaceholderAPI.setPlaceholders(player instanceof BukkitPlatformPlayer bukkitPlatformPlayer ? bukkitPlatformPlayer.getBukkitPlayer() : null, string);
    }
}
