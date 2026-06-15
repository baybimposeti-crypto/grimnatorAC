package com.grimnatorac.platform.api.manager;

import com.grimnatorac.platform.api.player.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MessagePlaceHolderManager {
    @NotNull
    String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string);
}
