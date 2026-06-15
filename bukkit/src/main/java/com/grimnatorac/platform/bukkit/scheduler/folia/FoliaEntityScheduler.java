package com.grimnatorac.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import com.grimnatorac.platform.api.entity.GrimEntity;
import com.grimnatorac.platform.api.scheduler.EntityScheduler;
import com.grimnatorac.platform.api.scheduler.TaskHandle;
import com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin;
import com.grimnatorac.platform.bukkit.entity.BukkitGrimEntity;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delay) {
        ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().execute(GrimnatorACBukkitLoaderPlugin.LOADER, task, retired, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        ScheduledTask scheduled = ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().run(
                GrimnatorACBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        ScheduledTask scheduled = ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runDelayed(
                GrimnatorACBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired,
                delayTicks
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        ScheduledTask scheduled = ((BukkitGrimEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(
                GrimnatorACBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired,
                initialDelayTicks,
                periodTicks
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }
}
