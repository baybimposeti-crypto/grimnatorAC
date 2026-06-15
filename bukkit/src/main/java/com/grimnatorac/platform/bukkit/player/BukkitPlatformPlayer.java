package com.grimnatorac.platform.bukkit.player;

import com.grimnatorac.platform.api.entity.GrimEntity;
import com.grimnatorac.platform.api.player.BlockTranslator;
import com.grimnatorac.platform.api.player.PlatformInventory;
import com.grimnatorac.platform.api.player.PlatformPlayer;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin;
import com.grimnatorac.platform.bukkit.entity.BukkitGrimEntity;
import com.grimnatorac.platform.bukkit.utils.anticheat.MultiLibUtil;
import com.grimnatorac.platform.bukkit.utils.convert.BukkitConversionUtils;
import com.grimnatorac.platform.bukkit.utils.reflection.PaperUtils;
import com.grimnatorac.utils.common.arguments.CommonGrimArguments;
import com.grimnatorac.utils.math.Location;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitPlatformPlayer extends BukkitGrimEntity implements PlatformPlayer {

    private static final BukkitAudiences audiences = BukkitAudiences.create(GrimnatorACBukkitLoaderPlugin.LOADER);

    @Getter
    private final Player bukkitPlayer;
    @Getter
    private final PlatformInventory inventory;

    private final @Nullable User user;

    public BukkitPlatformPlayer(@NotNull Player bukkitPlayer) {
        super(bukkitPlayer);
        this.bukkitPlayer = bukkitPlayer;
        this.inventory = new BukkitPlatformInventory(bukkitPlayer);
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value()) {
            Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(bukkitPlayer.getUniqueId());
            this.user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        } else {
            this.user = null;
        }
    }

    @Override
    public void kickPlayer(String textReason) {
        bukkitPlayer.kickPlayer(textReason);
    }

    @Override
    public boolean hasPermission(String s) {
        return bukkitPlayer.hasPermission(s);
    }

    @Override
    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return this.bukkitPlayer.hasPermission(new Permission(s, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    public boolean isSneaking() {
        return bukkitPlayer.isSneaking();
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        bukkitPlayer.setSneaking(isSneaking);
    }

    @Override
    public void sendMessage(String message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            bukkitPlayer.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            audiences.player(bukkitPlayer).sendMessage(message);
        }
    }

    @Override
    public boolean isOnline() {
        return bukkitPlayer.isOnline();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void updateInventory() {
        bukkitPlayer.updateInventory();
    }

    @Override
    public Vector3d getPosition() {
        if (CAN_USE_DIRECT_GETTERS) {
            return new Vector3d(this.bukkitPlayer.getX(), this.bukkitPlayer.getY(), this.bukkitPlayer.getZ());
        } else {
            org.bukkit.Location location = this.bukkitPlayer.getLocation();
            return new Vector3d(location.getX(), location.getY(), location.getZ());
        }
    }

    @Override
    public @Nullable GrimEntity getVehicle() {
        return bukkitPlayer.getVehicle() == null ? null : new BukkitGrimEntity(bukkitPlayer.getVehicle());
    }

    @Override
    public GameMode getGameMode() {
        return SpigotConversionUtil.fromBukkitGameMode(bukkitPlayer.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        bukkitPlayer.setGameMode(SpigotConversionUtil.toBukkitGameMode(gameMode));
    }

    public World getBukkitWorld() {
        return bukkitPlayer.getWorld();
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean eject() {
        return bukkitPlayer.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.bukkitPlayer, bLoc);
    }

    @Override
    public boolean isExternalPlayer() {
        return MultiLibUtil.isExternalPlayer(this.bukkitPlayer);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        this.bukkitPlayer.sendPluginMessage(GrimnatorACBukkitLoaderPlugin.LOADER, channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return GrimnatorACBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(this.bukkitPlayer);
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return BlockTranslator.IDENTITY;
    }

    @Override
    @NotNull
    public Player getNative() {
        return this.bukkitPlayer;
    }
}
