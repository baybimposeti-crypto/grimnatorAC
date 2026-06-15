package com.grimnatorac;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.storage.backend.BackendRegistry;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.internal.event.OptimizedEventBus;
import ac.grim.grimac.internal.storage.backend.BackendRegistryImpl;
import ac.grim.grimac.internal.storage.backend.memory.InMemoryBackendProvider;
import ac.grim.grimac.internal.storage.backend.mongo.MongoBackendProvider;
import ac.grim.grimac.internal.storage.backend.mysql.MysqlBackendProvider;
import ac.grim.grimac.internal.storage.backend.postgres.PostgresBackendProvider;
import ac.grim.grimac.internal.storage.backend.redis.RedisBackendProvider;
import ac.grim.grimac.internal.storage.backend.sqlite.SqliteBackendProvider;
import com.grimnatorac.manager.AlertManagerImpl;
import com.grimnatorac.manager.DiscordManager;
import com.grimnatorac.manager.InitManager;
import com.grimnatorac.manager.SpectateManager;
import com.grimnatorac.manager.TickManager;
import com.grimnatorac.manager.config.BaseConfigManager;
import com.grimnatorac.manager.datastore.DataStoreLifecycle;
import com.grimnatorac.manager.init.Initable;
import com.grimnatorac.platform.api.Platform;
import com.grimnatorac.platform.api.PlatformLoader;
import com.grimnatorac.platform.api.PlatformServer;
import com.grimnatorac.platform.api.command.CommandService;
import com.grimnatorac.platform.api.manager.ItemResetHandler;
import com.grimnatorac.platform.api.manager.MessagePlaceHolderManager;
import com.grimnatorac.platform.api.manager.PermissionRegistrationManager;
import com.grimnatorac.platform.api.manager.PlatformPluginManager;
import com.grimnatorac.platform.api.player.PlatformPlayerFactory;
import com.grimnatorac.platform.api.scheduler.PlatformScheduler;
import com.grimnatorac.platform.api.sender.SenderFactory;
import com.grimnatorac.utils.anticheat.PlayerDataManager;
import com.grimnatorac.utils.common.arguments.CommonGrimArguments;
import com.grimnatorac.utils.reflection.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class GrimAPI {
    public static final GrimAPI INSTANCE = new GrimAPI();

    private final Platform platform = detectPlatform();
    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final GrimExtensionManager extensionManager;
    private final EventBus eventBus;
    private final GrimExternalAPI externalAPI;
    private DataStoreLifecycle dataStoreLifecycle;
    private final BackendRegistry backendRegistry = buildBackendRegistry();
    private PlatformLoader loader;
    private InitManager initManager;
    private boolean initialized = false;

    private GrimAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.extensionManager = new GrimExtensionManager();
        this.eventBus = new OptimizedEventBus(extensionManager);
        this.externalAPI = new GrimExternalAPI(this);
    }

    // the order matters
    private static Platform detectPlatform() {
        Platform override = CommonGrimArguments.PLATFORM_OVERRIDE.value();
        if (override != null) return override;
        if (ReflectionUtils.hasClass("io.papermc.paper.threadedregions.RegionizedServer")) return Platform.FOLIA;
        if (ReflectionUtils.hasClass("org.bukkit.Bukkit")) return Platform.BUKKIT;
        if (ReflectionUtils.hasClass("net.fabricmc.loader.api.FabricLoader")) return Platform.FABRIC;
        throw new IllegalStateException("Unknown platform!");
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        this.loader = platformLoader;
        this.dataStoreLifecycle = new DataStoreLifecycle(getGrimPlugin(), backendRegistry);
        this.initManager = new InitManager(loader.getPacketEvents(), platformSpecificInitables);
        this.initManager.load();
        this.initialized = true;
    }

    private static BackendRegistry buildBackendRegistry() {
        BackendRegistryImpl registry = new BackendRegistryImpl();
        registry.register(new SqliteBackendProvider());
        registry.register(new InMemoryBackendProvider());
        registry.register(new MysqlBackendProvider());
        registry.register(new PostgresBackendProvider());
        registry.register(new MongoBackendProvider());
        registry.register(new RedisBackendProvider());
        return registry;
    }

    public void start() {
        checkInitialized();
        initManager.start();
    }

    public void stop() {
        checkInitialized();
        initManager.stop();
    }

    public PlatformScheduler getScheduler() {
        return loader.getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return loader.getPlatformPlayerFactory();
    }

    public GrimPlugin getGrimPlugin() {
        return loader.getPlugin();
    }

    public SenderFactory<?> getSenderFactory() {
        return loader.getSenderFactory();
    }

    public ItemResetHandler getItemResetHandler() {
        return loader.getItemResetHandler();
    }

    public PlatformPluginManager getPluginManager() {
        return loader.getPluginManager();
    }

    public PlatformServer getPlatformServer() {
        return loader.getPlatformServer();
    }

    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return loader.getMessagePlaceHolderManager();
    }

    public CommandService getCommandService() {
        return loader.getCommandService();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("GrimAPI has not been initialized!");
        }
    }

    public PermissionRegistrationManager getPermissionManager() {
        return loader.getPermissionManager();
    }
}
