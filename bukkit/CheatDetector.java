package com.smp.cheatdetector;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.util.crypto.MessageSignData;
import com.github.retrooper.packetevents.util.crypto.SaltSignature;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

public class CheatDetector
extends JavaPlugin
implements Listener,
TabCompleter {
    private final Map<UUID, SignCheckData> signChecks = new ConcurrentHashMap<UUID, SignCheckData>();
    private final Set<UUID> flagged = Collections.synchronizedSet(new HashSet());
    private final Map<UUID, Integer> strikes = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, String> playerBrands = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Integer> signTimeouts = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Set<String>> playerChannels = new ConcurrentHashMap<UUID, Set<String>>();
    private final Map<UUID, Long> brandTimestamps = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Set<String>> pendingSignDetections = new ConcurrentHashMap<UUID, Set<String>>();
    private final Map<UUID, Location> freecamLockedPos = new ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, Long> freecamLockedSince = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Integer> freecamRotationCount = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> freecamLastAlert = new ConcurrentHashMap<UUID, Long>();
    private static final long FREECAM_LOCK_DURATION_MS = 8000L;
    private static final int FREECAM_MIN_ROTATIONS = 30;
    private static final long FREECAM_ALERT_COOLDOWN_MS = 120000L;
    private final Map<UUID, Integer> freecamPositionStreak = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> freecamPositionLastAlert = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Float> lastYaw = new ConcurrentHashMap<UUID, Float>();
    private final Map<UUID, Float> lastPitch = new ConcurrentHashMap<UUID, Float>();
    private final Map<UUID, Integer> fastRotStreak = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> fastRotLastAlert = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> alertsDisabled = Collections.synchronizedSet(new HashSet());
    private final Set<UUID> manualBypass = Collections.synchronizedSet(new HashSet());
    private final Deque<HistoryEntry> history = new ConcurrentLinkedDeque<HistoryEntry>();
    private boolean notifyAdmins;
    private boolean logToConsole;
    private boolean brandAlerts;
    private Sound alertSound;
    private boolean checkSignChecks;
    private boolean signChecksOnJoin;
    private boolean checkChannelDetection;
    private boolean checkBrandDetection;
    private boolean checkVanillaSpoof;
    private boolean checkSignTimeoutAlert;
    private boolean checkFreecamBehavior;
    private boolean fastRotEnabled;
    private boolean fastRotAutoKick;
    private double fastRotMaxDeg;
    private int fastRotThreshold;
    private boolean freecamPosEnabled;
    private boolean freecamPosAutoKick;
    private int freecamPosThreshold;
    private boolean radarEnabled;
    private boolean radarAutoKick;
    private boolean nocrEnabled;
    private boolean nocrAutoKick;
    private int nocrTimeoutSeconds;
    private List<String> nocrExemptChannels = new ArrayList<String>();
    private final Set<UUID> sentChatSession = ConcurrentHashMap.newKeySet();
    private boolean unsignedChatEnabled;
    private boolean unsignedChatAutoKick;
    private int unsignedChatKickDelaySeconds;
    private boolean unsignedChatProbeEnabled;
    private boolean unsignedChatProbeAutoKick;
    private List<String> unsignedChatExemptChannels = new ArrayList<String>();
    private final Map<UUID, OpsecProbeState> opsecProbes = new ConcurrentHashMap<UUID, OpsecProbeState>();
    private final Set<UUID> unsignedChatFlagged = Collections.synchronizedSet(new HashSet());
    private boolean alertFreecam;
    private boolean alertFastRotation;
    private List<String> cheatBrands = new ArrayList<String>();
    private List<String> cheatChannels = new ArrayList<String>();
    private List<String> vanillaBrandList = new ArrayList<String>();
    private List<String> radarChannels = new ArrayList<String>();
    private List<List<SignProbeEntry>> signProbeGroups = new ArrayList<List<SignProbeEntry>>();
    private String firstStrike;
    private String secondStrike;
    private List<String> punishmentCommands;
    private Map<String, String> kickMessages = new HashMap<String, String>();
    private String banMessage;
    private List<String> warnOnlyMods;
    private Map<String, List<String>> modWhitelist = new HashMap<String, List<String>>();
    private int historySize;
    private boolean webhookEnabled;
    private String webhookUrl;
    private boolean updateCheckerEnabled;
    private String updateSource;
    private int spigotResourceId;
    private int bbbResourceId;
    private volatile String latestVersion;
    private ScheduledTask updateCheckInitialTask;
    private ScheduledTask updateCheckTimerTask;
    private boolean hasFloodgate = false;
    private static final String[] FABRIC_CHANNELS = new String[]{"fabric:", "fabric-", "c:version", "c:register"};
    private static final String[] FORGE_CHANNELS = new String[]{"fml:handshake", "fml:play", "fml:loginwrapper", "fml:", "forge:handshake", "forge:tier_sorting", "forge:", "neoforge:", "neoforge:server_config"};
    private static final String SIGN_FALLBACK = "MCD_CHECK";
    private static final SimpleDateFormat HISTORY_TIME_FMT = new SimpleDateFormat("HH:mm:ss");
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final List<String> SUBCOMMANDS = List.of("reload", "check", "info", "history", "reset", "bypass", "alerts", "signchecks", "mute", "toggle");
    private static final List<String> MUTE_CATEGORIES = List.of("freecam", "rotation");
    private static final List<String> TOGGLE_CATEGORIES = List.of("opsec", "nochatreports", "wurst");
    private static final List<String> PLAYER_SUBCOMMANDS = List.of("check", "info", "bypass", "reset", "clearflags", "resetwarn");
    private final Sched sched = new Sched();

    public void onLoad() {
        PacketEvents.setAPI((PacketEventsAPI)SpigotPacketEventsBuilder.build((Plugin)this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadSettings();
        boolean bl = this.hasFloodgate = this.getServer().getPluginManager().getPlugin("floodgate") != null;
        if (this.hasFloodgate) {
            this.getLogger().info("Floodgate detected - Bedrock players will be skipped.");
        }
        PacketEvents.getAPI().init();
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.registerPacketListeners();
        if (this.getCommand("cheatdetector") != null) {
            this.getCommand("cheatdetector").setTabCompleter((TabCompleter)this);
        }
        this.scheduleUpdateCheck();
        this.getLogger().info("CheatDetector enabled.");
    }

    public void onDisable() {
        try {
            if (PacketEvents.getAPI() != null) {
                PacketEvents.getAPI().terminate();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        for (Map.Entry<UUID, SignCheckData> entry : this.signChecks.entrySet()) {
            Player p;
            SignCheckData d = entry.getValue();
            if (d.signLoc == null || d.originalData == null || (p = Bukkit.getPlayer((UUID)entry.getKey())) == null || !p.isOnline()) continue;
            try {
                User user = PacketEvents.getAPI().getPlayerManager().getUser((Object)p);
                if (user == null) continue;
                Vector3i pos = new Vector3i(d.signLoc.getBlockX(), d.signLoc.getBlockY(), d.signLoc.getBlockZ());
                WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData((BlockData)d.originalData);
                user.sendPacket((PacketWrapper)new WrapperPlayServerBlockChange(pos, state));
            }
            catch (Exception exception) {}
        }
        this.flagged.clear();
        this.signChecks.clear();
        this.playerBrands.clear();
        this.signTimeouts.clear();
        this.playerChannels.clear();
        this.brandTimestamps.clear();
    }

    private void loadSettings() {
        this.reloadConfig();
        this.notifyAdmins = this.getConfig().getBoolean("notify-admins", true);
        this.logToConsole = this.getConfig().getBoolean("log-to-console", true);
        this.brandAlerts = this.getConfig().getBoolean("brand-alerts", true);
        String soundName = this.getConfig().getString("alert-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.alertSound = this.parseSound(soundName);
        this.checkSignChecks = this.getConfig().getBoolean("checks.sign-checks", true);
        this.signChecksOnJoin = this.getConfig().getBoolean("checks.sign-checks-on-join", true);
        this.checkChannelDetection = this.getConfig().getBoolean("checks.channel-detection", true);
        this.checkBrandDetection = this.getConfig().getBoolean("checks.brand-detection", true);
        this.checkVanillaSpoof = this.getConfig().getBoolean("checks.vanillaspoof", true);
        this.checkSignTimeoutAlert = this.getConfig().getBoolean("checks.sign-timeout-alert", true);
        this.checkFreecamBehavior = this.getConfig().getBoolean("checks.freecam-behavior", true);
        this.fastRotEnabled = this.getConfig().getBoolean("checks.fast-rotation.enabled", true);
        this.fastRotAutoKick = this.getConfig().getBoolean("checks.fast-rotation.auto-kick", false);
        this.fastRotMaxDeg = this.getConfig().getDouble("checks.fast-rotation.max-degrees-per-tick", 180.0);
        this.fastRotThreshold = this.getConfig().getInt("checks.fast-rotation.threshold", 5);
        this.freecamPosEnabled = this.getConfig().getBoolean("checks.freecam-position.enabled", true);
        this.freecamPosAutoKick = this.getConfig().getBoolean("checks.freecam-position.auto-kick", false);
        this.freecamPosThreshold = this.getConfig().getInt("checks.freecam-position.threshold", 3);
        this.radarEnabled = this.getConfig().getBoolean("checks.radar.enabled", false);
        this.radarAutoKick = this.getConfig().getBoolean("checks.radar.auto-kick", false);
        this.nocrEnabled = this.getConfig().getBoolean("checks.nochatreports.enabled", true);
        this.nocrAutoKick = this.getConfig().getBoolean("checks.nochatreports.auto-kick", true);
        this.nocrTimeoutSeconds = Math.max(1, this.getConfig().getInt("checks.nochatreports.timeout-seconds", 10));
        this.unsignedChatEnabled = this.getConfig().getBoolean("checks.unsigned-chat.enabled", true);
        this.unsignedChatAutoKick = this.getConfig().getBoolean("checks.unsigned-chat.auto-kick", true);
        this.unsignedChatKickDelaySeconds = Math.max(0, this.getConfig().getInt("checks.unsigned-chat.kick-delay-seconds", 20));
        this.unsignedChatProbeEnabled = this.getConfig().getBoolean("checks.unsigned-chat.probe-for-opsec", false);
        this.unsignedChatProbeAutoKick = this.getConfig().getBoolean("checks.unsigned-chat.probe-auto-kick", true);
        this.unsignedChatExemptChannels = CheatDetector.lowerList(this.getConfig().getStringList("checks.unsigned-chat.exempt-channels"));
        if (this.unsignedChatExemptChannels.isEmpty()) {
            this.unsignedChatExemptChannels = new ArrayList<String>(List.of("nopryingeyes:", "viafabricplus:", "viaversion:", "viabackwards:"));
        }
        this.nocrExemptChannels = CheatDetector.lowerList(this.getConfig().getStringList("checks.nochatreports.exempt-channels"));
        if (this.nocrExemptChannels.isEmpty()) {
            this.nocrExemptChannels = new ArrayList<String>(List.of("nochatreports:", "noreports:", "essential:", "e.gg", "lunarclient:", "lunar:", "viafabricplus:", "viaversion:", "viabackwards:"));
        }
        this.alertFreecam = this.getConfig().getBoolean("behavior-alerts.freecam", false);
        this.alertFastRotation = this.getConfig().getBoolean("behavior-alerts.fast-rotation", false);
        this.cheatBrands = CheatDetector.lowerList(this.getConfig().getStringList("blacklists.brands"));
        this.cheatChannels = CheatDetector.lowerList(this.getConfig().getStringList("blacklists.channels"));
        this.vanillaBrandList = CheatDetector.lowerList(this.getConfig().getStringList("blacklists.vanilla-brands"));
        if (this.vanillaBrandList.isEmpty()) {
            this.vanillaBrandList.add("vanilla");
        }
        this.radarChannels = CheatDetector.lowerList(this.getConfig().getStringList("blacklists.radar-channels"));
        this.loadSignProbeKeys();
        this.firstStrike = this.getConfig().getString("first-strike", "KICK").toUpperCase();
        this.secondStrike = this.getConfig().getString("second-strike", "BAN").toUpperCase();
        this.punishmentCommands = this.getConfig().getStringList("punishment-commands");
        this.warnOnlyMods = this.getConfig().getStringList("warn-only");
        this.kickMessages.clear();
        String contactLine = "<gray>If you think this was a mistake, contact the owner on our Discord.</gray>";
        String warnLine = "<yellow>This is a warning. Next time you will be permanently banned.</yellow>";
        this.kickMessages.put("default", this.getConfig().getString("kick-messages.default", "<white>You have been kicked from this server.</white>\\n\\n<red><bold>Reason:</bold> %mod%</red>\\n\\n" + warnLine + "\\n\\n" + contactLine));
        this.kickMessages.put("vanilla-spoof", this.getConfig().getString("kick-messages.vanilla-spoof", "<white>You have been kicked from this server.</white>\\n\\n<red><bold>Reason:</bold> Client spoofing (%mod%)</red>\\n\\n" + warnLine + "\\n\\n" + contactLine));
        this.kickMessages.put("freecam", this.getConfig().getString("kick-messages.freecam", "<white>You have been kicked from this server.</white>\\n\\n<red><bold>Reason:</bold> %mod%</red>\\n\\n" + warnLine + "\\n\\n" + contactLine));
        this.kickMessages.put("fast-rotation", this.getConfig().getString("kick-messages.fast-rotation", "<white>You have been kicked from this server.</white>\\n\\n<red><bold>Reason:</bold> Aimbot / Spinbot detected</red>\\n\\n" + warnLine + "\\n\\n" + contactLine));
        this.banMessage = this.getConfig().getString("ban-message", "<white>You have been permanently banned from this server.</white>\\n\\n<red><bold>Reason:</bold> %mod%</red>\\n\\n" + contactLine);
        this.modWhitelist = new HashMap<String, List<String>>();
        ConfigurationSection ws = this.getConfig().getConfigurationSection("mod-whitelist");
        if (ws != null) {
            for (String key : ws.getKeys(false)) {
                List players = ws.getStringList(key);
                ArrayList<String> lowered = new ArrayList<String>(players.size());
                for (String p : players) {
                    lowered.add(p.toLowerCase());
                }
                this.modWhitelist.put(key.toLowerCase(), lowered);
            }
        }
        this.historySize = Math.max(1, this.getConfig().getInt("history-size", 50));
        this.webhookEnabled = this.getConfig().getBoolean("discord-webhook.enabled", false);
        this.webhookUrl = this.getConfig().getString("discord-webhook.url", "");
        this.updateCheckerEnabled = this.getConfig().getBoolean("update-checker.enabled", true);
        this.updateSource = this.getConfig().getString("update-checker.source", "spigot");
        this.spigotResourceId = this.getConfig().getInt("update-checker.spigot-resource-id", 134018);
        this.bbbResourceId = this.getConfig().getInt("update-checker.builtbybit-resource-id", 102038);
    }

    private void loadSignProbeKeys() {
        this.signProbeGroups.clear();
        List raw = this.getConfig().getList("sign-probe.keys");
        if (raw == null) {
            return;
        }
        ArrayList<SignProbeEntry> all = new ArrayList<SignProbeEntry>();
        for (Object o : raw) {
            if (!(o instanceof Map)) continue;
            Map map = (Map)o;
            Object translation = map.get("translation");
            Object mod = map.get("mod");
            Object match = map.get("match");
            if (translation == null || mod == null) continue;
            all.add(new SignProbeEntry(String.valueOf(translation), String.valueOf(mod), match == null ? "" : String.valueOf(match)));
        }
        for (int i = 0; i < all.size(); i += 4) {
            this.signProbeGroups.add(new ArrayList(all.subList(i, Math.min(i + 4, all.size()))));
        }
    }

    private static List<String> lowerList(List<String> in) {
        ArrayList<String> out = new ArrayList<String>(in.size());
        for (String s : in) {
            if (s == null) continue;
            out.add(s.toLowerCase().trim());
        }
        return out;
    }

    private Sound parseSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf((String)name.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            this.getLogger().warning("Unknown alert-sound: " + name);
            return null;
        }
    }

    private boolean isPlayerWhitelistedFor(Player player, String mod) {
        if (mod == null) {
            return false;
        }
        List<String> list = this.modWhitelist.get(mod.toLowerCase());
        if (list == null || list.isEmpty()) {
            return false;
        }
        return list.contains(player.getName().toLowerCase());
    }

    private boolean isBedrockPlayer(Player player) {
        if (!this.hasFloodgate) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean isExempt(Player p) {
        return p.hasPermission("cheatdetector.bypass") || p.isOp() || this.manualBypass.contains(p.getUniqueId());
    }

    private String joinNatural(Collection<String> items) {
        ArrayList<String> list = new ArrayList<String>(items);
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return (String)list.get(0);
        }
        if (list.size() == 2) {
            return (String)list.get(0) + " and " + (String)list.get(1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() - 1; ++i) {
            sb.append((String)list.get(i)).append(", ");
        }
        sb.append("and ").append((String)list.get(list.size() - 1));
        return sb.toString();
    }

    private List<String> splitMods(String mods) {
        if (!mods.contains(",")) {
            return List.of(mods);
        }
        ArrayList<String> out = new ArrayList<String>();
        for (String s : mods.split(",")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            out.add(t);
        }
        return out;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.notifyAdminOfUpdate(player);
        if (this.isExempt(player)) {
            return;
        }
        if (this.isBedrockPlayer(player)) {
            return;
        }
        this.flagged.remove(player.getUniqueId());
        if (this.checkSignChecks && this.signChecksOnJoin && !this.signProbeGroups.isEmpty()) {
            long base = 100L;
            int i = 0;
            while (i < this.signProbeGroups.size()) {
                int idx = i++;
                this.sched.runForEntityLater((Entity)player, () -> {
                    if (!player.isOnline() || this.flagged.contains(player.getUniqueId())) {
                        return;
                    }
                    this.runSignCheck(player, this.signProbeGroups.get(idx), idx + 1);
                }, base + (long)idx * 10L);
            }
            this.sched.runForEntityLater((Entity)player, () -> this.finalizeSignDetections(player), base + (long)this.signProbeGroups.size() * 10L + 20L);
        }
        if (this.checkVanillaSpoof) {
            this.sched.runForEntityLater((Entity)player, () -> {
                if (!player.isOnline() || this.flagged.contains(player.getUniqueId())) {
                    return;
                }
                if (this.isExempt(player)) {
                    return;
                }
                this.checkVanillaSpoofPlayer(player);
            }, 100L);
            this.sched.runForEntityLater((Entity)player, () -> {
                if (!player.isOnline() || this.flagged.contains(player.getUniqueId())) {
                    return;
                }
                if (this.isExempt(player)) {
                    return;
                }
                this.checkVanillaSpoofPlayer(player);
            }, 300L);
        }
        if (this.nocrEnabled) {
            this.sched.runForEntityLater((Entity)player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (this.isExempt(player)) {
                    return;
                }
                if (this.flagged.contains(player.getUniqueId())) {
                    return;
                }
                if (this.sentChatSession.contains(player.getUniqueId())) {
                    return;
                }
                if (this.hasNoChatReportsExemptChannel(player)) {
                    return;
                }
                String detail = "Wurst or NoPryingEyes";
                if (this.nocrAutoKick) {
                    this.onDetected(player, detail, "default");
                } else {
                    this.onWarnOnly(player, detail);
                }
            }, (long)this.nocrTimeoutSeconds * 20L);
        }
    }

    private boolean hasNoChatReportsExemptChannel(Player player) {
        return this.hasAnyExemptChannel(player, this.nocrExemptChannels);
    }

    private boolean hasUnsignedChatExemptChannel(Player player) {
        return this.hasAnyExemptChannel(player, this.unsignedChatExemptChannels);
    }

    private boolean hasAnyExemptChannel(Player player, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        Set<String> ours = this.playerChannels.get(uuid);
        Set bukkit = player.getListeningPluginChannels();
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) continue;
            if (ours != null) {
                for (String ch : ours) {
                    if (!ch.startsWith(prefix)) continue;
                    return true;
                }
            }
            for (String ch : bukkit) {
                if (!ch.toLowerCase().startsWith(prefix)) continue;
                return true;
            }
        }
        return false;
    }

    private void checkVanillaSpoofPlayer(Player player) {
        String brand = player.getClientBrandName();
        if (brand == null) {
            return;
        }
        String lowerBrand = brand.toLowerCase().trim();
        boolean isVanilla = false;
        for (String v : this.vanillaBrandList) {
            if (!lowerBrand.equals(v)) continue;
            isVanilla = true;
            break;
        }
        if (!isVanilla) {
            return;
        }
        Set channels = player.getListeningPluginChannels();
        for (String ch : channels) {
            String lower = ch.toLowerCase();
            for (String fabricCh : FABRIC_CHANNELS) {
                if (!lower.startsWith(fabricCh) && !lower.equals(fabricCh)) continue;
                this.onDetected(player, "VanillaSpoof (Fabric channel: " + ch + ")", "vanilla-spoof");
                return;
            }
            for (String forgeCh : FORGE_CHANNELS) {
                if (!lower.startsWith(forgeCh) && !lower.equals(forgeCh)) continue;
                this.onDetected(player, "VanillaSpoof (Forge channel: " + ch + ")", "vanilla-spoof");
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        this.playerBrands.remove(uuid);
        this.signTimeouts.remove(uuid);
        this.playerChannels.remove(uuid);
        this.brandTimestamps.remove(uuid);
        this.freecamLockedPos.remove(uuid);
        this.freecamLockedSince.remove(uuid);
        this.freecamRotationCount.remove(uuid);
        this.freecamLastAlert.remove(uuid);
        this.freecamPositionStreak.remove(uuid);
        this.freecamPositionLastAlert.remove(uuid);
        this.lastYaw.remove(uuid);
        this.lastPitch.remove(uuid);
        this.fastRotStreak.remove(uuid);
        this.fastRotLastAlert.remove(uuid);
        this.pendingSignDetections.remove(uuid);
        this.sentChatSession.remove(uuid);
        this.opsecProbes.remove(uuid);
        this.unsignedChatFlagged.remove(uuid);
        this.cleanup(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        boolean rotChanged;
        Player player = event.getPlayer();
        if (this.isExempt(player)) {
            return;
        }
        if (this.isBedrockPlayer(player)) {
            return;
        }
        if (player.isInsideVehicle() || player.isSleeping() || player.isDead()) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (this.freecamPosEnabled) {
            this.checkFreecamPosition(player, to, uuid);
        }
        if (!this.checkFreecamBehavior) {
            return;
        }
        boolean posChanged = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        boolean bl = rotChanged = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
        if (posChanged) {
            this.freecamLockedPos.put(uuid, to.clone());
            this.freecamLockedSince.put(uuid, System.currentTimeMillis());
            this.freecamRotationCount.put(uuid, 0);
            return;
        }
        if (!this.freecamLockedPos.containsKey(uuid)) {
            this.freecamLockedPos.put(uuid, to.clone());
            this.freecamLockedSince.put(uuid, System.currentTimeMillis());
            this.freecamRotationCount.put(uuid, 0);
            return;
        }
        if (rotChanged) {
            this.freecamRotationCount.merge(uuid, 1, Integer::sum);
        }
        long since = this.freecamLockedSince.getOrDefault(uuid, System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - since;
        int rotations = this.freecamRotationCount.getOrDefault(uuid, 0);
        if (elapsed < 8000L || rotations < 30) {
            return;
        }
        long lastAlert = this.freecamLastAlert.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastAlert < 120000L) {
            return;
        }
        this.freecamLastAlert.put(uuid, System.currentTimeMillis());
        this.reportFreecamBehavior(player, elapsed, rotations);
    }

    private void checkFreecamPosition(Player player, Location to, UUID uuid) {
        long lastAlert;
        boolean inBlock;
        Block block = to.getBlock();
        boolean bl = inBlock = block != null && block.getType().isSolid() && block.getType().isOccluding();
        if (!inBlock) {
            this.freecamPositionStreak.put(uuid, 0);
            return;
        }
        int streak = this.freecamPositionStreak.getOrDefault(uuid, 0) + 1;
        this.freecamPositionStreak.put(uuid, streak);
        if (streak < this.freecamPosThreshold) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - (lastAlert = this.freecamPositionLastAlert.getOrDefault(uuid, 0L).longValue()) < 120000L) {
            return;
        }
        this.freecamPositionLastAlert.put(uuid, now);
        this.freecamPositionStreak.put(uuid, 0);
        String detail = "Freecam-Position (in " + block.getType().name().toLowerCase() + " x" + streak + ")";
        if (this.freecamPosAutoKick) {
            this.onDetected(player, detail, "freecam");
        } else {
            this.onWarnOnly(player, detail, !this.alertFreecam);
        }
    }

    private void reportFreecamBehavior(Player player, long elapsedMs, int rotations) {
        if (this.logToConsole) {
            this.getLogger().warning("FREECAM-BEHAVIOR: " + player.getName() + " - position locked for " + elapsedMs / 1000L + "s with " + rotations + " rotation packets");
        }
        String detail = "Freecam-Behavior (frozen " + elapsedMs / 1000L + "s, " + rotations + " rotations)";
        this.onWarnOnly(player, detail, !this.alertFreecam);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        Player player = event.getPlayer();
        if (this.isExempt(player)) {
            return;
        }
        String channel = event.getChannel().toLowerCase();
        if (this.checkChannelDetection) {
            for (String cheat : this.cheatChannels) {
                if (!channel.contains(cheat)) continue;
                this.onDetected(player, "cheat channel: " + event.getChannel(), "default");
                return;
            }
        }
        if (this.radarEnabled) {
            for (String radar : this.radarChannels) {
                if (!channel.contains(radar)) continue;
                String detail = "Radar/Minimap channel: " + event.getChannel();
                if (this.radarAutoKick) {
                    this.onDetected(player, detail, "default");
                } else {
                    this.onWarnOnly(player, detail);
                }
                return;
            }
        }
        for (String forgeCh : FORGE_CHANNELS) {
            if (!channel.startsWith(forgeCh) && !channel.equals(forgeCh)) continue;
            if (this.logToConsole) {
                this.getLogger().info(player.getName() + " registered Forge/NeoForge channel: " + event.getChannel());
            }
            return;
        }
    }

    private void runSignCheck(Player player, List<SignProbeEntry> entries, int checkNum) {
        if (!player.isOnline() || this.flagged.contains(player.getUniqueId())) {
            return;
        }
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Location playerLoc = player.getLocation();
        Location signLoc = new Location(playerLoc.getWorld(), (double)playerLoc.getBlockX(), (double)Math.max(playerLoc.getWorld().getMinHeight(), playerLoc.getBlockY() - 4), (double)playerLoc.getBlockZ());
        Block block = signLoc.getBlock();
        BlockData originalData = block.getBlockData();
        Vector3i pos = new Vector3i(signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ());
        SignCheckData data = new SignCheckData();
        data.signLoc = signLoc;
        data.originalData = originalData;
        data.active = true;
        data.entries = entries;
        data.checkNum = checkNum;
        this.signChecks.put(player.getUniqueId(), data);
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser((Object)player);
            if (user == null) {
                this.cleanup(player.getUniqueId());
                return;
            }
            WrappedBlockState signState = StateTypes.OAK_SIGN.createBlockState();
            WrappedBlockState originalState = SpigotConversionUtil.fromBukkitBlockData((BlockData)originalData);
            NBTCompound signNbt = this.buildSignNbt(entries);
            user.writePacket((PacketWrapper)new WrapperPlayServerBlockChange(pos, signState));
            user.writePacket((PacketWrapper)new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, signNbt));
            user.writePacket((PacketWrapper)new WrapperPlayServerOpenSignEditor(pos, true));
            user.writePacket((PacketWrapper)new WrapperPlayServerBlockChange(pos, originalState));
            user.flushPackets();
        }
        catch (Exception e) {
            if (this.logToConsole) {
                this.getLogger().warning("Failed phantom sign for " + player.getName() + ": " + e.getMessage());
            }
            this.cleanup(player.getUniqueId());
            return;
        }
        this.sched.runForEntityLater((Entity)player, () -> {
            SignCheckData d = this.signChecks.get(player.getUniqueId());
            if (d != null && d.active && d.checkNum == checkNum) {
                this.cleanup(player.getUniqueId());
                if (!player.isOnline()) {
                    return;
                }
                int timeouts = this.signTimeouts.getOrDefault(player.getUniqueId(), 0) + 1;
                this.signTimeouts.put(player.getUniqueId(), timeouts);
                if (this.logToConsole) {
                    this.getLogger().info(player.getName() + " sign check " + checkNum + " timed out. (total timeouts: " + timeouts + ")");
                }
                if (timeouts >= 2 && this.checkSignTimeoutAlert) {
                    this.signTimeouts.remove(player.getUniqueId());
                    if (this.logToConsole) {
                        this.getLogger().warning(player.getName() + " - sign checks blocked (possible anti-detection mod)");
                    }
                    if (this.notifyAdmins) {
                        this.sendAdminAlert(NamedTextColor.GOLD, ((TextComponent)Component.text((String)player.getName(), (TextColor)NamedTextColor.YELLOW).append((Component)Component.text((String)" - sign checks blocked ", (TextColor)NamedTextColor.GRAY))).append((Component)Component.text((String)"(possible anti-detection mod)", (TextColor)NamedTextColor.DARK_GRAY)));
                    }
                }
            }
        }, 100L);
    }

    private void cleanup(UUID uuid) {
        SignCheckData data = this.signChecks.remove(uuid);
        if (data == null || data.signLoc == null || data.originalData == null) {
            return;
        }
        Player p = Bukkit.getPlayer((UUID)uuid);
        if (p == null || !p.isOnline()) {
            return;
        }
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser((Object)p);
            if (user == null) {
                return;
            }
            Vector3i pos = new Vector3i(data.signLoc.getBlockX(), data.signLoc.getBlockY(), data.signLoc.getBlockZ());
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData((BlockData)data.originalData);
            user.sendPacket((PacketWrapper)new WrapperPlayServerBlockChange(pos, state));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private NBTCompound buildSignNbt(List<SignProbeEntry> entries) {
        NBTCompound root = new NBTCompound();
        root.setTag("is_waxed", (NBT)new NBTByte(false));
        root.setTag("front_text", (NBT)this.buildSignSide(entries, true));
        root.setTag("back_text", (NBT)this.buildSignSide(entries, false));
        return root;
    }

    private NBTCompound buildSignSide(List<SignProbeEntry> entries, boolean front) {
        NBTCompound side = new NBTCompound();
        NBTList messages = NBTList.createCompoundList();
        for (int i = 0; i < 4; ++i) {
            NBTCompound msg = new NBTCompound();
            if (front) {
                String key = i < entries.size() ? entries.get((int)i).translation : "";
                msg.setTag("translate", (NBT)new NBTString(key));
                msg.setTag("fallback", (NBT)new NBTString(SIGN_FALLBACK));
            } else {
                msg.setTag("text", (NBT)new NBTString(""));
            }
            messages.addTag((NBT)msg);
        }
        side.setTag("messages", (NBT)messages);
        side.setTag("color", (NBT)new NBTString("black"));
        side.setTag("has_glowing_text", (NBT)new NBTByte(false));
        return side;
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener((PacketListenerCommon)new PacketListenerAbstract(PacketListenerPriority.HIGHEST){

            public void onPacketReceive(PacketReceiveEvent event) {
                Object object = event.getPlayer();
                if (!(object instanceof Player)) {
                    return;
                }
                Player player = (Player)object;
                if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
                    CheatDetector.this.handleUpdateSign(event, player);
                } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
                    CheatDetector.this.handlePluginMessage(event, player);
                } else if (event.getPacketType() == PacketType.Play.Client.CHAT_SESSION_UPDATE) {
                    CheatDetector.this.sentChatSession.add(player.getUniqueId());
                } else if ((CheatDetector.this.unsignedChatEnabled || CheatDetector.this.unsignedChatProbeEnabled) && event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
                    CheatDetector.this.handleChatMessage(event, player);
                } else if (CheatDetector.this.fastRotEnabled && CheatDetector.this.isRotationPacket(event.getPacketType())) {
                    CheatDetector.this.handleRotation(event, player);
                }
            }
        });
    }

    private boolean isRotationPacket(Object type) {
        return type == PacketType.Play.Client.PLAYER_ROTATION || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private void handleRotation(PacketReceiveEvent event, Player player) {
        long lastAlert;
        float dPitch;
        double total;
        float pitch;
        float yaw;
        if (this.isExempt(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        try {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            if (wrapper.getLocation() == null) {
                return;
            }
            yaw = wrapper.getLocation().getYaw();
            pitch = wrapper.getLocation().getPitch();
        }
        catch (Exception ex) {
            return;
        }
        Float prevYaw = this.lastYaw.get(uuid);
        Float prevPitch = this.lastPitch.get(uuid);
        this.lastYaw.put(uuid, Float.valueOf(yaw));
        this.lastPitch.put(uuid, Float.valueOf(pitch));
        if (prevYaw == null || prevPitch == null) {
            return;
        }
        float dYaw = Math.abs(yaw - prevYaw.floatValue());
        if (dYaw > 180.0f) {
            dYaw = 360.0f - dYaw;
        }
        if ((total = (double)(dYaw + (dPitch = Math.abs(pitch - prevPitch.floatValue())))) < this.fastRotMaxDeg) {
            this.fastRotStreak.put(uuid, 0);
            return;
        }
        int streak = this.fastRotStreak.getOrDefault(uuid, 0) + 1;
        this.fastRotStreak.put(uuid, streak);
        if (streak < this.fastRotThreshold) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - (lastAlert = this.fastRotLastAlert.getOrDefault(uuid, 0L).longValue()) < 120000L) {
            return;
        }
        this.fastRotLastAlert.put(uuid, now);
        this.fastRotStreak.put(uuid, 0);
        String detail = String.format("Fast-Rotation (%.1f deg/tick x%d)", total, streak);
        this.sched.runForEntity((Entity)player, () -> {
            if (this.fastRotAutoKick) {
                this.onDetected(player, detail, "fast-rotation");
            } else {
                this.onWarnOnly(player, detail, !this.alertFastRotation);
            }
        });
    }

    private void handleChatMessage(PacketReceiveEvent event, Player player) {
        boolean signed;
        String message;
        if (this.isExempt(player)) {
            return;
        }
        if (this.isBedrockPlayer(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        try {
            WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);
            message = wrapper.getMessage();
            signed = this.isMessageSigned(wrapper);
        }
        catch (Exception ex) {
            return;
        }
        if (message == null) {
            return;
        }
        long delayTicks = Math.max(0L, (long)this.unsignedChatKickDelaySeconds * 20L);
        OpsecProbeState probe = this.opsecProbes.get(uuid);
        if (probe != null && signed && message.equals(probe.unsignedMessage) && System.currentTimeMillis() - probe.sentAt < 2500L) {
            this.opsecProbes.remove(uuid);
            this.sched.runForEntity((Entity)player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (this.unsignedChatProbeAutoKick) {
                    this.onDetected(player, "OpSec", "default", delayTicks);
                } else {
                    this.onWarnOnly(player, "OpSec");
                }
            });
            return;
        }
        if (signed) {
            return;
        }
        if (this.hasUnsignedChatExemptChannel(player)) {
            return;
        }
        if (this.unsignedChatEnabled && this.unsignedChatFlagged.add(uuid)) {
            this.sched.runForEntity((Entity)player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (this.unsignedChatAutoKick) {
                    this.onDetected(player, "OpSec", "default", delayTicks);
                } else {
                    this.onWarnOnly(player, "OpSec");
                }
            });
        }
        if (this.unsignedChatProbeEnabled) {
            OpsecProbeState state = new OpsecProbeState();
            state.unsignedMessage = message;
            state.sentAt = System.currentTimeMillis();
            this.opsecProbes.put(uuid, state);
            this.sched.runForEntity((Entity)player, () -> {
                if (player.isOnline()) {
                    player.sendMessage((Component)Component.translatable((String)"chat.disabled.missingProfileKey"));
                }
            });
            this.sched.runSyncLater(() -> this.opsecProbes.remove(uuid), 60L);
        }
    }

    private boolean isMessageSigned(WrapperPlayClientChatMessage wrapper) {
        try {
            Optional data = wrapper.getMessageSignData();
            if (data == null || data.isEmpty()) {
                return false;
            }
            SaltSignature ss = ((MessageSignData)data.get()).getSaltSignature();
            if (ss == null) {
                return false;
            }
            byte[] sig = ss.getSignature();
            return sig != null && sig.length > 0;
        }
        catch (Throwable ignored) {
            return true;
        }
    }

    private void handleUpdateSign(PacketReceiveEvent event, Player player) {
        SignCheckData data = this.signChecks.get(player.getUniqueId());
        if (data == null || !data.active) {
            return;
        }
        data.active = false;
        event.setCancelled(true);
        WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
        String[] lines = wrapper.getTextLines();
        List<SignProbeEntry> entries = data.entries;
        int checkNum = data.checkNum;
        if (lines == null || lines.length == 0) {
            this.sched.runForEntity((Entity)player, () -> {
                this.cleanup(player.getUniqueId());
                if (this.logToConsole) {
                    this.getLogger().info(player.getName() + " sign check " + checkNum + " response: empty/null");
                }
            });
            return;
        }
        if (this.logToConsole) {
            for (int i = 0; i < Math.min(lines.length, entries.size()); ++i) {
                String l = lines[i] != null ? lines[i] : "";
                this.getLogger().info(player.getName() + " check " + checkNum + " line " + i + " [" + entries.get((int)i).mod + "]: \"" + l + "\"");
            }
        }
        this.sched.runForEntity((Entity)player, () -> {
            this.cleanup(player.getUniqueId());
            for (int i = 0; i < Math.min(lines.length, entries.size()); ++i) {
                String line = lines[i] != null ? lines[i].toLowerCase().trim() : "";
                SignProbeEntry entry = (SignProbeEntry)entries.get(i);
                if (line.isEmpty() || line.equals(SIGN_FALLBACK.toLowerCase())) continue;
                if (line.contains("key.") || line.contains("translate")) {
                    if (!this.logToConsole) continue;
                    this.getLogger().info("Sign line " + i + " raw key literal for " + player.getName() + " - skipping: \"" + lines[i] + "\"");
                    continue;
                }
                String matchPattern = entry.match;
                if (matchPattern != null && !matchPattern.isEmpty()) {
                    boolean matched = false;
                    for (String term : matchPattern.split(",")) {
                        String t = term.trim().toLowerCase();
                        if (t.isEmpty() || !line.contains(t)) continue;
                        matched = true;
                        break;
                    }
                    if (!matched) {
                        if (!this.logToConsole) continue;
                        this.getLogger().info("Sign line " + i + " failed match filter for " + player.getName() + ": \"" + lines[i] + "\" (expected: " + matchPattern + ")");
                        continue;
                    }
                }
                String detected = entry.mod;
                if (this.logToConsole) {
                    this.getLogger().info("Sign match: " + player.getName() + " - key=" + entry.translation + " response=\"" + lines[i] + "\"");
                }
                if (this.isPlayerWhitelistedFor(player, detected)) {
                    if (!this.logToConsole) continue;
                    this.getLogger().info(player.getName() + " whitelisted for " + detected + " - skipped.");
                    continue;
                }
                if (this.isWarnOnly(detected)) {
                    this.onWarnOnly(player, detected);
                    continue;
                }
                this.pendingSignDetections.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashSet()).add(detected);
            }
            if (this.logToConsole) {
                this.getLogger().info(player.getName() + " passed sign check " + checkNum + ".");
            }
        });
    }

    private void handlePluginMessage(PacketReceiveEvent event, Player player) {
        if (this.isExempt(player)) {
            return;
        }
        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
        String channelName = wrapper.getChannelName().toLowerCase();
        if (channelName.equals("minecraft:brand")) {
            byte[] payload = wrapper.getData();
            if (payload == null) {
                return;
            }
            String rawBrand = new String(payload, StandardCharsets.UTF_8).toLowerCase().trim();
            if (!rawBrand.isEmpty() && rawBrand.charAt(0) < ' ') {
                rawBrand = rawBrand.substring(1);
            }
            String brand = rawBrand;
            UUID uuid = player.getUniqueId();
            String previousBrand = this.playerBrands.get(uuid);
            this.playerBrands.put(uuid, brand);
            if (previousBrand != null && !previousBrand.equals(brand)) {
                String prev = previousBrand;
                if (this.logToConsole) {
                    this.getLogger().warning(player.getName() + " changed brand mid-session: \"" + prev + "\" -> \"" + brand + "\"");
                }
                this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "Brand Spoof (\"" + prev + "\" -> \"" + brand + "\")", "default"));
                return;
            }
            Long lastBrand = this.brandTimestamps.get(uuid);
            long now = System.currentTimeMillis();
            this.brandTimestamps.put(uuid, now);
            if (lastBrand != null && now - lastBrand < 1000L && previousBrand != null && this.logToConsole) {
                this.getLogger().info(player.getName() + " sent brand packet twice within 1s");
            }
            if (this.checkBrandDetection) {
                for (String cheatBrand : this.cheatBrands) {
                    if (!brand.contains(cheatBrand)) continue;
                    String b = brand;
                    this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "client brand: " + b, "default"));
                    return;
                }
            }
            if (previousBrand == null && this.brandAlerts && this.notifyAdmins) {
                this.sched.runSync(() -> {
                    if (this.logToConsole) {
                        this.getLogger().info(player.getName() + " joined with brand=\"" + brand + "\"");
                    }
                    String displayBrand = this.resolveClientBrand(brand);
                    this.sendAdminAlert(NamedTextColor.DARK_AQUA, ((TextComponent)Component.text((String)player.getName(), (TextColor)NamedTextColor.YELLOW).append((Component)Component.text((String)" joined using ", (TextColor)NamedTextColor.GRAY))).append(Component.text((String)displayBrand, (TextColor)NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)));
                });
            }
            return;
        }
        if (this.checkChannelDetection) {
            for (String cheat : this.cheatChannels) {
                if (!channelName.contains(cheat)) continue;
                this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "cheat payload: " + channelName, "default"));
                return;
            }
        }
        if (channelName.equals("minecraft:register")) {
            long fabricCount;
            String storedBrand;
            String lower;
            Object payload = wrapper.getData();
            if (payload == null) {
                return;
            }
            UUID uuid = player.getUniqueId();
            String[] registeredChannels = new String((byte[])payload, StandardCharsets.UTF_8).split("\u0000");
            Set accumulated = this.playerChannels.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
            for (String ch2 : registeredChannels) {
                accumulated.add(ch2.toLowerCase().trim());
            }
            if (this.checkChannelDetection) {
                for (String ch2 : registeredChannels) {
                    lower = ch2.toLowerCase().trim();
                    for (String cheat : this.cheatChannels) {
                        if (!lower.contains(cheat)) continue;
                        this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "registered channel: " + ch2, "default"));
                        return;
                    }
                }
            }
            if (this.radarEnabled) {
                for (String ch2 : registeredChannels) {
                    lower = ch2.toLowerCase().trim();
                    for (String radar : this.radarChannels) {
                        if (!lower.contains(radar)) continue;
                        String r = ch2;
                        this.sched.runForEntity((Entity)player, () -> {
                            String detail = "Radar/Minimap channel: " + r;
                            if (this.radarAutoKick) {
                                this.onDetected(player, detail, "default");
                            } else {
                                this.onWarnOnly(player, detail);
                            }
                        });
                        return;
                    }
                }
            }
            if (this.checkVanillaSpoof && (storedBrand = this.playerBrands.get(uuid)) != null && this.vanillaBrandList.contains(storedBrand)) {
                for (String ch3 : registeredChannels) {
                    String lower2 = ch3.toLowerCase().trim();
                    for (String fabricCh : FABRIC_CHANNELS) {
                        if (!lower2.startsWith(fabricCh) && !lower2.equals(fabricCh)) continue;
                        String detectedCh = ch3;
                        this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "VanillaSpoof (Fabric channel: " + detectedCh + ")", "vanilla-spoof"));
                        return;
                    }
                    for (String forgeCh : FORGE_CHANNELS) {
                        if (!lower2.startsWith(forgeCh) && !lower2.equals(forgeCh)) continue;
                        String detectedCh = ch3;
                        this.sched.runForEntity((Entity)player, () -> this.onDetected(player, "VanillaSpoof (Forge channel: " + detectedCh + ")", "vanilla-spoof"));
                        return;
                    }
                }
            }
            if ((fabricCount = accumulated.stream().filter(ch -> ch.startsWith("fabric:") || ch.startsWith("fabric-") || ch.startsWith("c:")).count()) >= 5L && this.notifyAdmins) {
                long fc = fabricCount;
                this.sched.runSync(() -> {
                    if (this.logToConsole) {
                        this.getLogger().info(player.getName() + " has " + fc + " Fabric channels (heavily modded)");
                    }
                });
            }
        }
    }

    private Component buildPrefix(NamedTextColor accentColor) {
        return ((TextComponent)Component.text((String)"[", (TextColor)NamedTextColor.DARK_GRAY).append(Component.text((String)"CD", (TextColor)accentColor).decoration(TextDecoration.BOLD, true))).append((Component)Component.text((String)"] ", (TextColor)NamedTextColor.DARK_GRAY));
    }

    private void sendAdminAlert(NamedTextColor accentColor, Component body) {
        Component alert = this.buildPrefix(accentColor).append(body);
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (!op.hasPermission("cheatdetector.admin") || this.alertsDisabled.contains(op.getUniqueId())) continue;
            op.sendMessage(alert);
            if (this.alertSound == null) continue;
            op.playSound(op.getLocation(), this.alertSound, 1.0f, 1.0f);
        }
    }

    private String resolveClientBrand(String brand) {
        if (brand == null || brand.isEmpty()) {
            return "Unknown";
        }
        if (brand.equals("vanilla")) {
            return "Vanilla";
        }
        if (brand.equals("fabric")) {
            return "Fabric";
        }
        if (brand.contains("neoforge")) {
            return "NeoForge";
        }
        if (brand.contains("fml,forge") || brand.contains("forge")) {
            return "Forge";
        }
        if (brand.contains("lunarclient") || brand.contains("lunar")) {
            return "Lunar Client";
        }
        if (brand.contains("badlion")) {
            return "Badlion Client";
        }
        if (brand.contains("feather")) {
            return "Feather Client";
        }
        if (brand.contains("labymod")) {
            return "LabyMod";
        }
        if (brand.contains("optifine")) {
            return "OptiFine";
        }
        if (brand.contains("quilt")) {
            return "Quilt";
        }
        return brand.substring(0, 1).toUpperCase() + brand.substring(1);
    }

    private String resolveModName(String reason) {
        if (reason.equalsIgnoreCase("Wurst or NoPryingEyes")) {
            return "Wurst or NoPryingEyes";
        }
        if (reason.equalsIgnoreCase("OpSec")) {
            return "OpSec";
        }
        for (List<SignProbeEntry> group : this.signProbeGroups) {
            for (SignProbeEntry e : group) {
                if (!reason.equals(e.mod)) continue;
                return e.mod;
            }
        }
        if (reason.startsWith("VanillaSpoof")) {
            return reason.startsWith("VanillaSpoof (Forge") ? "VanillaSpoof (likely Forge-based cheat)" : "VanillaSpoof (likely Wurst / LiquidBounce / Fabric-cheat)";
        }
        if (reason.startsWith("Brand Spoof")) {
            return "Brand Spoof";
        }
        if (reason.startsWith("Freecam-Behavior")) {
            return "Freecam (behavior)";
        }
        if (reason.startsWith("Freecam-Position")) {
            return "Freecam (in-block)";
        }
        if (reason.startsWith("Fast-Rotation")) {
            return "Aimbot / Spinbot";
        }
        if (reason.startsWith("Radar/Minimap")) {
            return "Radar / Minimap";
        }
        String lower = reason.toLowerCase();
        if (lower.contains("meteor")) {
            return "Meteor Client";
        }
        if (lower.contains("wurst")) {
            return "Wurst Client";
        }
        if (lower.contains("impact")) {
            return "Impact Client";
        }
        if (lower.contains("aristois")) {
            return "Aristois";
        }
        if (lower.contains("inertia")) {
            return "Inertia";
        }
        if (lower.contains("kami")) {
            return "Kami Blue";
        }
        if (lower.contains("lambda")) {
            return "Lambda";
        }
        if (lower.contains("rusherhack")) {
            return "RusherHack";
        }
        if (lower.contains("baritone")) {
            return "Baritone";
        }
        if (lower.contains("freecam")) {
            return "FreeCam";
        }
        if (lower.contains("liquidbounce")) {
            return "LiquidBounce";
        }
        if (lower.contains("aoba")) {
            return "Aoba Client";
        }
        if (lower.contains("bleachhack")) {
            return "BleachHack";
        }
        if (lower.contains("cheatutils")) {
            return "CheatUtils";
        }
        if (lower.contains("3arthh4ck")) {
            return "3arthh4ck";
        }
        return reason.replace("client brand: ", "").replace("cheat payload: ", "").replace("registered channel: ", "").replace("cheat channel: ", "");
    }

    private void finalizeSignDetections(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> detected = this.pendingSignDetections.remove(uuid);
        if (detected == null || detected.isEmpty()) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        if (this.flagged.contains(uuid)) {
            return;
        }
        String combined = String.join((CharSequence)", ", detected);
        this.onDetected(player, combined, "default");
    }

    private boolean isWarnOnly(String detected) {
        for (String mod : this.warnOnlyMods) {
            if (!detected.equalsIgnoreCase(mod)) continue;
            return true;
        }
        return false;
    }

    private void onWarnOnly(Player player, String reason) {
        this.onWarnOnly(player, reason, false);
    }

    private void onWarnOnly(Player player, String reason, boolean silentChat) {
        if (!player.isOnline()) {
            return;
        }
        if (this.logToConsole) {
            this.getLogger().warning("WARN-ONLY: " + player.getName() + " - " + reason + (silentChat ? " [chat muted]" : ""));
        }
        if (this.notifyAdmins && !silentChat) {
            this.sendAdminAlert(NamedTextColor.GOLD, ((TextComponent)((TextComponent)Component.text((String)player.getName(), (TextColor)NamedTextColor.YELLOW).append((Component)Component.text((String)" flagged for ", (TextColor)NamedTextColor.GRAY))).append(Component.text((String)reason, (TextColor)NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))).append((Component)Component.text((String)" (alert only)", (TextColor)NamedTextColor.DARK_GRAY)));
        }
        this.recordHistory(player.getName(), reason, "Alert only");
        this.sendWebhook(player.getName(), reason, "Alert only");
    }

    private void onDetected(Player player, String reason, String kickTemplate) {
        this.onDetected(player, reason, kickTemplate, 20L);
    }

    private void onDetected(Player player, String reason, String kickTemplate, long delayTicks) {
        if (!player.isOnline()) {
            return;
        }
        if (this.flagged.contains(player.getUniqueId())) {
            return;
        }
        this.sched.runForEntityLater((Entity)player, () -> {
            String actionType;
            if (!player.isOnline()) {
                return;
            }
            if (this.flagged.contains(player.getUniqueId())) {
                return;
            }
            if (this.isExempt(player)) {
                return;
            }
            this.flagged.add(player.getUniqueId());
            this.cleanup(player.getUniqueId());
            int strike = this.strikes.getOrDefault(player.getUniqueId(), 0) + 1;
            this.strikes.put(player.getUniqueId(), strike);
            String rawList = reason.contains(",") ? reason : this.resolveModName(reason);
            List<String> mods = this.splitMods(rawList);
            String modPhrase = this.joinNatural(mods);
            String string = actionType = strike == 1 ? this.firstStrike : this.secondStrike;
            if (this.logToConsole) {
                this.getLogger().warning("DETECTED: " + player.getName() + " - " + reason + " [" + modPhrase + "] (strike " + strike + ", action: " + actionType + ")");
            }
            if (this.notifyAdmins) {
                String verb = switch (actionType) {
                    case "BAN" -> "was banned";
                    case "COMMAND" -> "was punished";
                    case "NONE" -> "was detected";
                    default -> "was kicked";
                };
                NamedTextColor actionColor = switch (actionType) {
                    case "BAN" -> NamedTextColor.DARK_RED;
                    case "COMMAND" -> NamedTextColor.RED;
                    case "NONE" -> NamedTextColor.GOLD;
                    default -> NamedTextColor.RED;
                };
                this.sendAdminAlert(actionColor, ((TextComponent)Component.text((String)player.getName(), (TextColor)NamedTextColor.YELLOW).append((Component)Component.text((String)(" " + verb + " for using "), (TextColor)NamedTextColor.GRAY))).append(Component.text((String)modPhrase, (TextColor)NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)));
            }
            String actionLabel = switch (actionType) {
                case "BAN" -> "Banned";
                case "COMMAND" -> "Custom command";
                case "NONE" -> "Alert only";
                default -> "Kicked";
            };
            this.recordHistory(player.getName(), modPhrase, actionLabel + " (strike " + strike + ")");
            this.sendWebhook(player.getName(), modPhrase, actionLabel + " (strike " + strike + ")");
            switch (actionType) {
                case "BAN": {
                    String msg = this.banMessage.replace("%mod%", modPhrase).replace("%player%", player.getName()).replace("%strike%", String.valueOf(strike));
                    Component banComponent = this.formatKickMessage(msg);
                    String banReason = LegacyComponentSerializer.legacySection().serialize(banComponent);
                    player.ban(banReason, (Date)null, "CheatDetector");
                    break;
                }
                case "COMMAND": {
                    for (String cmd : this.punishmentCommands) {
                        String parsed = cmd.replace("%player%", player.getName()).replace("%mod%", modPhrase).replace("%strike%", String.valueOf(strike));
                        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)parsed);
                    }
                    break;
                }
                case "NONE": {
                    break;
                }
                default: {
                    String template = this.kickMessages.getOrDefault(kickTemplate, this.kickMessages.get("default"));
                    String msg = template.replace("%mod%", modPhrase).replace("%player%", player.getName()).replace("%strike%", String.valueOf(strike));
                    player.kick(this.formatKickMessage(msg));
                }
            }
        }, delayTicks);
    }

    private Component formatKickMessage(String msg) {
        String resolved = msg.replace("\\n", "\n");
        try {
            return MINI.deserialize((Object)resolved);
        }
        catch (Exception ex) {
            TextComponent result = Component.empty();
            String[] parts = resolved.split("\n");
            for (int i = 0; i < parts.length; ++i) {
                if (i > 0) {
                    result = result.append((Component)Component.text((String)"\n"));
                }
                result = result.append((Component)Component.text((String)parts[i], (TextColor)NamedTextColor.RED));
            }
            return result;
        }
    }

    private void recordHistory(String player, String mods, String action) {
        this.history.addFirst(new HistoryEntry(System.currentTimeMillis(), player, mods, action));
        while (this.history.size() > this.historySize) {
            this.history.pollLast();
        }
    }

    private void sendWebhook(String playerName, String modName, String action) {
        if (!this.webhookEnabled || this.webhookUrl.isEmpty()) {
            return;
        }
        this.sched.runAsync(() -> {
            block9: {
                try {
                    String json = "{\"embeds\":[{\"title\":\"CheatDetector Alert\",\"color\":16711680,\"fields\":[{\"name\":\"Player\",\"value\":\"" + CheatDetector.escapeJson(playerName) + "\",\"inline\":true},{\"name\":\"Detected\",\"value\":\"" + CheatDetector.escapeJson(modName) + "\",\"inline\":true},{\"name\":\"Action\",\"value\":\"" + CheatDetector.escapeJson(action) + "\",\"inline\":false}]}]}";
                    HttpURLConnection conn = (HttpURLConnection)URI.create(this.webhookUrl).toURL().openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream();){
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }
                    conn.getResponseCode();
                    conn.disconnect();
                }
                catch (Exception e) {
                    if (!this.logToConsole) break block9;
                    this.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
                }
            }
        });
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cheatdetector.admin")) {
            sender.sendMessage((Component)Component.text((String)"No permission.", (TextColor)NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload": {
                return this.cmdReload(sender);
            }
            case "check": {
                return this.cmdCheck(sender, args);
            }
            case "info": {
                return this.cmdInfo(sender, args);
            }
            case "history": {
                return this.cmdHistory(sender);
            }
            case "clearflags": 
            case "reset": 
            case "resetwarn": {
                return this.cmdClearflags(sender, args);
            }
            case "bypass": {
                return this.cmdBypass(sender, args);
            }
            case "alerts": {
                return this.cmdAlerts(sender);
            }
            case "signchecks": {
                return this.cmdSignChecks(sender, args);
            }
            case "mute": {
                return this.cmdMute(sender, args);
            }
            case "toggle": {
                return this.cmdToggle(sender, args);
            }
        }
        this.sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Usage: ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"/cd <reload|check|info|history|reset|bypass|alerts|signchecks|mute|toggle>", (TextColor)NamedTextColor.WHITE)));
    }

    private boolean cmdMute(CommandSender sender, String[] args) {
        String configKey;
        String label;
        boolean newValue;
        String category;
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Behavior alert status:", (TextColor)NamedTextColor.GRAY)));
            sender.sendMessage(this.muteStatusLine("freecam", this.alertFreecam));
            sender.sendMessage(this.muteStatusLine("rotation", this.alertFastRotation));
            sender.sendMessage(Component.text((String)"  Toggle with ", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)"/cd mute <freecam|rotation>", (TextColor)NamedTextColor.WHITE)));
            return true;
        }
        switch (category = args[1].toLowerCase()) {
            case "freecam": {
                newValue = this.alertFreecam = !this.alertFreecam;
                label = "Freecam";
                configKey = "behavior-alerts.freecam";
                break;
            }
            case "rotation": 
            case "fast-rotation": 
            case "aimbot": {
                newValue = this.alertFastRotation = !this.alertFastRotation;
                label = "Fast-rotation";
                configKey = "behavior-alerts.fast-rotation";
                break;
            }
            default: {
                sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Unknown category. Use ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"freecam", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)" or ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"rotation", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
                return true;
            }
        }
        this.getConfig().set(configKey, (Object)newValue);
        this.saveConfig();
        sender.sendMessage(this.buildPrefix(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD).append((Component)Component.text((String)(label + " alerts "), (TextColor)NamedTextColor.GRAY)).append(Component.text((String)(newValue ? "unmuted" : "muted"), (TextColor)(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD)).decoration(TextDecoration.BOLD, true)).append((Component)Component.text((String)". Detection still runs.", (TextColor)NamedTextColor.GRAY)));
        return true;
    }

    private Component muteStatusLine(String name, boolean enabled) {
        return Component.text((String)("  " + name + ": "), (TextColor)NamedTextColor.GRAY).append(Component.text((String)(enabled ? "ON" : "MUTED"), (TextColor)(enabled ? NamedTextColor.GREEN : NamedTextColor.GOLD)).decoration(TextDecoration.BOLD, true));
    }

    private boolean cmdToggle(CommandSender sender, String[] args) {
        String configKey;
        String label;
        boolean newValue;
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Detection toggles:", (TextColor)NamedTextColor.GRAY)));
            sender.sendMessage(this.toggleStatusLine("opsec", this.unsignedChatEnabled));
            sender.sendMessage(this.toggleStatusLine("nochatreports", this.nocrEnabled));
            sender.sendMessage(this.toggleStatusLine("wurst", this.nocrEnabled));
            sender.sendMessage(Component.text((String)"  Flip with ", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)"/cd toggle <opsec|nochatreports|wurst>", (TextColor)NamedTextColor.WHITE)));
            sender.sendMessage((Component)Component.text((String)"  Note: wurst & nochatreports share the same check.", (TextColor)NamedTextColor.DARK_GRAY));
            return true;
        }
        String category = args[1].toLowerCase();
        boolean isWurst = false;
        switch (category) {
            case "opsec": 
            case "unsigned-chat": 
            case "unsignedchat": {
                newValue = this.unsignedChatEnabled = !this.unsignedChatEnabled;
                label = "OpSec / unsigned-chat";
                configKey = "checks.unsigned-chat.enabled";
                break;
            }
            case "nochatreports": 
            case "ncr": 
            case "no-chat-reports": {
                newValue = this.nocrEnabled = !this.nocrEnabled;
                label = "NoChatReports";
                configKey = "checks.nochatreports.enabled";
                break;
            }
            case "wurst": {
                newValue = this.nocrEnabled = !this.nocrEnabled;
                label = "Wurst (via nochatreports check)";
                configKey = "checks.nochatreports.enabled";
                isWurst = true;
                break;
            }
            default: {
                sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Unknown category. Use ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"opsec", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)", ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"nochatreports", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)", or ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"wurst", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
                return true;
            }
        }
        this.getConfig().set(configKey, (Object)newValue);
        this.saveConfig();
        sender.sendMessage(this.buildPrefix(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD).append((Component)Component.text((String)(label + " detection "), (TextColor)NamedTextColor.GRAY)).append(Component.text((String)(newValue ? "ENABLED" : "DISABLED"), (TextColor)(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD)).decoration(TextDecoration.BOLD, true)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
        if (isWurst && !newValue) {
            sender.sendMessage((Component)Component.text((String)"  Note: Wurst is also caught by channel/brand blacklists. ", (TextColor)NamedTextColor.DARK_GRAY));
            sender.sendMessage((Component)Component.text((String)"  Edit blacklists.channels & blacklists.brands in config.yml ", (TextColor)NamedTextColor.DARK_GRAY));
            sender.sendMessage((Component)Component.text((String)"  and run /cd reload to fully disable Wurst detection.", (TextColor)NamedTextColor.DARK_GRAY));
        }
        return true;
    }

    private Component toggleStatusLine(String name, boolean enabled) {
        return Component.text((String)("  " + name + ": "), (TextColor)NamedTextColor.GRAY).append(Component.text((String)(enabled ? "ENABLED" : "DISABLED"), (TextColor)(enabled ? NamedTextColor.GREEN : NamedTextColor.GOLD)).decoration(TextDecoration.BOLD, true));
    }

    private boolean cmdSignChecks(CommandSender sender, String[] args) {
        boolean newValue;
        if (args.length < 2) {
            String state = this.signChecksOnJoin ? "ON" : "OFF";
            NamedTextColor stateColor = this.signChecksOnJoin ? NamedTextColor.GREEN : NamedTextColor.GOLD;
            sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Sign checks on join: ", (TextColor)NamedTextColor.GRAY)).append(Component.text((String)state, (TextColor)stateColor).decoration(TextDecoration.BOLD, true)));
            sender.sendMessage(((TextComponent)Component.text((String)"  Use ", (TextColor)NamedTextColor.GRAY).append((Component)Component.text((String)"/cd signchecks <on|off>", (TextColor)NamedTextColor.WHITE))).append((Component)Component.text((String)" to change.", (TextColor)NamedTextColor.GRAY)));
            return true;
        }
        String arg = args[1].toLowerCase();
        if (arg.equals("on") || arg.equals("true") || arg.equals("enable")) {
            newValue = true;
        } else if (arg.equals("off") || arg.equals("false") || arg.equals("disable")) {
            newValue = false;
        } else {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Use ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"on", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)" or ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"off", (TextColor)NamedTextColor.WHITE)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
            return true;
        }
        this.signChecksOnJoin = newValue;
        this.getConfig().set("checks.sign-checks-on-join", (Object)newValue);
        this.saveConfig();
        sender.sendMessage(this.buildPrefix(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD).append((Component)Component.text((String)"Sign checks on join ", (TextColor)NamedTextColor.GRAY)).append(Component.text((String)(newValue ? "enabled" : "disabled"), (TextColor)(newValue ? NamedTextColor.GREEN : NamedTextColor.GOLD)).decoration(TextDecoration.BOLD, true)).append((Component)Component.text((String)". Manual /cd check still works.", (TextColor)NamedTextColor.GRAY)));
        return true;
    }

    private boolean cmdReload(CommandSender sender) {
        this.loadSettings();
        this.flagged.clear();
        this.signChecks.clear();
        this.scheduleUpdateCheck();
        sender.sendMessage(this.buildPrefix(NamedTextColor.GREEN).append((Component)Component.text((String)"Config reloaded.", (TextColor)NamedTextColor.GREEN)));
        return true;
    }

    private boolean cmdCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.GRAY).append((Component)Component.text((String)"Usage: ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"/cd check <player>", (TextColor)NamedTextColor.WHITE)));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED)));
            return true;
        }
        if (this.signProbeGroups.isEmpty()) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"No sign-probe keys configured.", (TextColor)NamedTextColor.RED)));
            return true;
        }
        sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Running sign checks on ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)target.getName(), (TextColor)NamedTextColor.YELLOW)).append((Component)Component.text((String)"...", (TextColor)NamedTextColor.GRAY)));
        this.flagged.remove(target.getUniqueId());
        this.cleanup(target.getUniqueId());
        int i = 0;
        while (i < this.signProbeGroups.size()) {
            int idx = i++;
            this.sched.runForEntityLater((Entity)target, () -> {
                if (!target.isOnline() || this.flagged.contains(target.getUniqueId())) {
                    return;
                }
                this.runSignCheck(target, this.signProbeGroups.get(idx), idx + 1);
            }, (long)idx * 10L);
        }
        this.sched.runForEntityLater((Entity)target, () -> this.finalizeSignDetections(target), (long)this.signProbeGroups.size() * 10L + 20L);
        return true;
    }

    private boolean cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.GRAY).append((Component)Component.text((String)"Usage: ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"/cd info <player>", (TextColor)NamedTextColor.WHITE)));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED)));
            return true;
        }
        UUID uuid = target.getUniqueId();
        String apiBrand = target.getClientBrandName();
        String brand = this.playerBrands.getOrDefault(uuid, apiBrand != null ? apiBrand : "(unknown)");
        Set<String> channels = this.playerChannels.get(uuid);
        Set bukkitChannels = target.getListeningPluginChannels();
        int timeouts = this.signTimeouts.getOrDefault(uuid, 0);
        int playerStrikes = this.strikes.getOrDefault(uuid, 0);
        boolean isFlagged = this.flagged.contains(uuid);
        boolean bypassed = this.manualBypass.contains(uuid);
        sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)"Info for ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)target.getName(), (TextColor)NamedTextColor.YELLOW)));
        sender.sendMessage(this.infoLine("Brand", brand, NamedTextColor.WHITE));
        sender.sendMessage(this.infoLine("Brand (API)", apiBrand != null ? apiBrand : "(null)", NamedTextColor.WHITE));
        sender.sendMessage(this.infoLine("Strikes", String.valueOf(playerStrikes), playerStrikes > 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
        sender.sendMessage(this.infoLine("Sign Timeouts", String.valueOf(timeouts), timeouts > 0 ? NamedTextColor.GOLD : NamedTextColor.GREEN));
        sender.sendMessage(this.infoLine("Flagged", String.valueOf(isFlagged), isFlagged ? NamedTextColor.RED : NamedTextColor.GREEN));
        sender.sendMessage(this.infoLine("Bypass", String.valueOf(bypassed), bypassed ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        if (channels != null && !channels.isEmpty()) {
            sender.sendMessage(this.infoLine("Channels (" + channels.size() + ")", "", NamedTextColor.GRAY));
            for (String ch : channels) {
                boolean isFabric = ch.startsWith("fabric:") || ch.startsWith("fabric-") || ch.startsWith("c:");
                sender.sendMessage(Component.text((String)"    ", (TextColor)NamedTextColor.DARK_GRAY).append((Component)Component.text((String)ch, (TextColor)(isFabric ? NamedTextColor.RED : NamedTextColor.DARK_GRAY))));
            }
        } else if (!bukkitChannels.isEmpty()) {
            sender.sendMessage(this.infoLine("Bukkit Channels (" + bukkitChannels.size() + ")", "", NamedTextColor.GRAY));
            for (String ch : bukkitChannels) {
                sender.sendMessage(Component.text((String)"    ", (TextColor)NamedTextColor.DARK_GRAY).append((Component)Component.text((String)ch, (TextColor)NamedTextColor.DARK_GRAY)));
            }
        } else {
            sender.sendMessage(this.infoLine("Channels", "none", NamedTextColor.DARK_GRAY));
        }
        return true;
    }

    private Component infoLine(String key, String value, NamedTextColor valueColor) {
        Component line = Component.text((String)"  ", (TextColor)NamedTextColor.DARK_GRAY).append((Component)Component.text((String)(key + ": "), (TextColor)NamedTextColor.GRAY));
        if (!value.isEmpty()) {
            line = line.append((Component)Component.text((String)value, (TextColor)valueColor));
        }
        return line;
    }

    private boolean cmdHistory(CommandSender sender) {
        if (this.history.isEmpty()) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.GRAY).append((Component)Component.text((String)"No detections recorded.", (TextColor)NamedTextColor.GRAY)));
            return true;
        }
        sender.sendMessage(this.buildPrefix(NamedTextColor.DARK_AQUA).append((Component)Component.text((String)("Recent detections (" + this.history.size() + ")"), (TextColor)NamedTextColor.GRAY)));
        int shown = 0;
        for (HistoryEntry e : this.history) {
            if (shown++ >= 15) break;
            String time = HISTORY_TIME_FMT.format(new Date(e.timestamp));
            sender.sendMessage(((TextComponent)((TextComponent)((TextComponent)Component.text((String)("  " + time + " "), (TextColor)NamedTextColor.DARK_GRAY).append((Component)Component.text((String)e.player, (TextColor)NamedTextColor.YELLOW))).append((Component)Component.text((String)" - ", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)e.mods, (TextColor)NamedTextColor.WHITE))).append((Component)Component.text((String)(" (" + e.action + ")"), (TextColor)NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean cmdClearflags(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.GRAY).append((Component)Component.text((String)"Usage: ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"/cd reset <player>", (TextColor)NamedTextColor.WHITE)));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        UUID uuid = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer((String)args[1]).getUniqueId();
        int hadStrikes = this.strikes.getOrDefault(uuid, 0);
        this.flagged.remove(uuid);
        this.strikes.remove(uuid);
        this.signTimeouts.remove(uuid);
        this.fastRotStreak.remove(uuid);
        this.freecamPositionStreak.remove(uuid);
        this.fastRotLastAlert.remove(uuid);
        this.freecamPositionLastAlert.remove(uuid);
        this.freecamLastAlert.remove(uuid);
        sender.sendMessage(this.buildPrefix(NamedTextColor.GREEN).append((Component)Component.text((String)"Reset ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)args[1], (TextColor)NamedTextColor.YELLOW)).append((Component)Component.text((String)" - next detection will be treated as their first warning.", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)(" (was at " + hadStrikes + " strike" + (hadStrikes == 1 ? "" : "s") + ")"), (TextColor)NamedTextColor.DARK_GRAY)));
        return true;
    }

    private boolean cmdBypass(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.GRAY).append((Component)Component.text((String)"Usage: ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"/cd bypass <player>", (TextColor)NamedTextColor.WHITE)));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Player not found.", (TextColor)NamedTextColor.RED)));
            return true;
        }
        UUID uuid = target.getUniqueId();
        if (this.manualBypass.contains(uuid)) {
            this.manualBypass.remove(uuid);
            sender.sendMessage(this.buildPrefix(NamedTextColor.GOLD).append((Component)Component.text((String)"Bypass removed from ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)target.getName(), (TextColor)NamedTextColor.YELLOW)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
        } else {
            this.manualBypass.add(uuid);
            sender.sendMessage(this.buildPrefix(NamedTextColor.GREEN).append((Component)Component.text((String)"Bypass enabled for ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)target.getName(), (TextColor)NamedTextColor.YELLOW)).append((Component)Component.text((String)".", (TextColor)NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean cmdAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.buildPrefix(NamedTextColor.RED).append((Component)Component.text((String)"Players only.", (TextColor)NamedTextColor.RED)));
            return true;
        }
        Player p = (Player)sender;
        UUID uuid = p.getUniqueId();
        if (this.alertsDisabled.contains(uuid)) {
            this.alertsDisabled.remove(uuid);
            p.sendMessage(this.buildPrefix(NamedTextColor.GREEN).append((Component)Component.text((String)"Detection alerts ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"enabled", (TextColor)NamedTextColor.GREEN)).append((Component)Component.text((String)" for you.", (TextColor)NamedTextColor.GRAY)));
        } else {
            this.alertsDisabled.add(uuid);
            p.sendMessage(this.buildPrefix(NamedTextColor.GOLD).append((Component)Component.text((String)"Detection alerts ", (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)"disabled", (TextColor)NamedTextColor.GOLD)).append((Component)Component.text((String)" for you.", (TextColor)NamedTextColor.GRAY)));
        }
        return true;
    }

    private void scheduleUpdateCheck() {
        this.cancelUpdateCheckTasks();
        if (!this.updateCheckerEnabled) {
            return;
        }
        this.updateCheckInitialTask = this.sched.runAsyncLater(this::performUpdateCheck, 100L);
        this.updateCheckTimerTask = this.sched.runAsyncTimer(this::performUpdateCheck, 432000L, 432000L);
    }

    private void cancelUpdateCheckTasks() {
        this.sched.cancel(this.updateCheckInitialTask);
        this.updateCheckInitialTask = null;
        this.sched.cancel(this.updateCheckTimerTask);
        this.updateCheckTimerTask = null;
    }

    private void performUpdateCheck() {
        String apiUrl = "https://api.spigotmc.org/legacy/update.php?resource=" + this.spigotResourceId;
        try {
            HttpURLConnection conn = (HttpURLConnection)URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "CheatDetector/" + this.getDescription().getVersion());
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
            conn.setRequestProperty("Pragma", "no-cache");
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code != 200) {
                conn.disconnect();
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));){
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            conn.disconnect();
            String latest = CheatDetector.normalizeVersion(sb.toString());
            if (latest.isEmpty()) {
                return;
            }
            this.latestVersion = latest;
            String current = CheatDetector.normalizeVersion(this.getDescription().getVersion());
            if (!CheatDetector.isNewerVersion(latest, current)) {
                return;
            }
            this.getLogger().warning("=================================================");
            this.getLogger().warning("A new version of CheatDetector is available!");
            this.getLogger().warning("  Running: " + current);
            this.getLogger().warning("  Latest:  " + latest);
            this.getLogger().warning("  Download: " + this.getDownloadUrl());
            this.getLogger().warning("=================================================");
            this.sched.runSync(() -> {
                for (Player op : Bukkit.getOnlinePlayers()) {
                    this.notifyAdminOfUpdate(op);
                }
            });
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static boolean isNewerVersion(String remote, String current) {
        if (remote == null || current == null) {
            return false;
        }
        String[] r = remote.split("\\.");
        String[] c = current.split("\\.");
        int len = Math.max(r.length, c.length);
        for (int i = 0; i < len; ++i) {
            int ci;
            int ri = i < r.length ? CheatDetector.parseLeadingInt(r[i]) : 0;
            int n = ci = i < c.length ? CheatDetector.parseLeadingInt(c[i]) : 0;
            if (ri > ci) {
                return true;
            }
            if (ri >= ci) continue;
            return false;
        }
        return false;
    }

    private static int parseLeadingInt(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (!Character.isDigit(ch)) break;
            sb.append(ch);
        }
        if (sb.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(sb.toString());
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalizeVersion(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c <= ' ' || c >= '\u007f') continue;
            out.append(c);
        }
        String result = out.toString();
        if (result.length() > 1 && (result.charAt(0) == 'v' || result.charAt(0) == 'V')) {
            result = result.substring(1);
        }
        return result;
    }

    private String getDownloadUrl() {
        if ("builtbybit".equalsIgnoreCase(this.updateSource)) {
            return "https://builtbybit.com/resources/cheatdetector." + this.bbbResourceId + "/";
        }
        return "https://www.spigotmc.org/resources/cheatdetector." + this.spigotResourceId + "/";
    }

    private boolean hasUpdate() {
        if (this.latestVersion == null) {
            return false;
        }
        return CheatDetector.isNewerVersion(this.latestVersion, CheatDetector.normalizeVersion(this.getDescription().getVersion()));
    }

    private void notifyAdminOfUpdate(Player player) {
        if (!this.hasUpdate()) {
            return;
        }
        if (!player.hasPermission("cheatdetector.admin")) {
            return;
        }
        String url = this.getDownloadUrl();
        Component header = this.buildPrefix(NamedTextColor.GOLD).append((Component)Component.text((String)"Update available! ", (TextColor)NamedTextColor.GOLD)).append((Component)Component.text((String)this.getDescription().getVersion(), (TextColor)NamedTextColor.GRAY)).append((Component)Component.text((String)" \u2192 ", (TextColor)NamedTextColor.DARK_GRAY)).append(Component.text((String)this.latestVersion, (TextColor)NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        Component link = Component.text((String)"  Download: ", (TextColor)NamedTextColor.GRAY).append(((TextComponent)Component.text((String)url, (TextColor)NamedTextColor.AQUA).decoration(TextDecoration.UNDERLINED, true)).clickEvent(ClickEvent.openUrl((String)url)));
        this.sched.runForEntityLater((Entity)player, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage(header);
            player.sendMessage(link);
        }, 60L);
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("cheatdetector.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            ArrayList<String> out = new ArrayList<String>();
            for (String s : SUBCOMMANDS) {
                if (!s.startsWith(prefix)) continue;
                out.add(s);
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (PLAYER_SUBCOMMANDS.contains(sub)) {
                ArrayList<String> out = new ArrayList<String>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getName().toLowerCase().startsWith(prefix)) continue;
                    out.add(p.getName());
                }
                return out;
            }
            if (sub.equals("signchecks")) {
                ArrayList<String> out = new ArrayList<String>();
                for (String s : List.of("on", "off")) {
                    if (!s.startsWith(prefix)) continue;
                    out.add(s);
                }
                return out;
            }
            if (sub.equals("mute")) {
                ArrayList<String> out = new ArrayList<String>();
                for (String s : MUTE_CATEGORIES) {
                    if (!s.startsWith(prefix)) continue;
                    out.add(s);
                }
                return out;
            }
            if (sub.equals("toggle")) {
                ArrayList<String> out = new ArrayList<String>();
                for (String s : TOGGLE_CATEGORIES) {
                    if (!s.startsWith(prefix)) continue;
                    out.add(s);
                }
                return out;
            }
        }
        return Collections.emptyList();
    }

    private final class Sched {
        private Sched() {
        }

        void runSync(Runnable task) {
            Bukkit.getGlobalRegionScheduler().execute((Plugin)CheatDetector.this, task);
        }

        ScheduledTask runSyncLater(Runnable task, long delayTicks) {
            return Bukkit.getGlobalRegionScheduler().runDelayed((Plugin)CheatDetector.this, t -> task.run(), Math.max(1L, delayTicks));
        }

        void runForEntity(Entity entity, Runnable task) {
            entity.getScheduler().run((Plugin)CheatDetector.this, t -> task.run(), null);
        }

        ScheduledTask runForEntityLater(Entity entity, Runnable task, long delayTicks) {
            return entity.getScheduler().runDelayed((Plugin)CheatDetector.this, t -> task.run(), null, Math.max(1L, delayTicks));
        }

        ScheduledTask runAsync(Runnable task) {
            return Bukkit.getAsyncScheduler().runNow((Plugin)CheatDetector.this, t -> task.run());
        }

        ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
            return Bukkit.getAsyncScheduler().runDelayed((Plugin)CheatDetector.this, t -> task.run(), Math.max(50L, delayTicks * 50L), TimeUnit.MILLISECONDS);
        }

        ScheduledTask runAsyncTimer(Runnable task, long initialDelayTicks, long periodTicks) {
            return Bukkit.getAsyncScheduler().runAtFixedRate((Plugin)CheatDetector.this, t -> task.run(), Math.max(50L, initialDelayTicks * 50L), Math.max(50L, periodTicks * 50L), TimeUnit.MILLISECONDS);
        }

        void cancel(ScheduledTask task) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    private static class SignCheckData {
        Location signLoc;
        BlockData originalData;
        volatile boolean active;
        List<SignProbeEntry> entries;
        int checkNum;

        private SignCheckData() {
        }
    }

    private static class SignProbeEntry {
        final String translation;
        final String mod;
        final String match;

        SignProbeEntry(String translation, String mod, String match) {
            this.translation = translation;
            this.mod = mod;
            this.match = match;
        }
    }

    private static class OpsecProbeState {
        volatile String unsignedMessage;
        volatile long sentAt;

        private OpsecProbeState() {
        }
    }

    private static class HistoryEntry {
        final long timestamp;
        final String player;
        final String mods;
        final String action;

        HistoryEntry(long timestamp, String player, String mods, String action) {
            this.timestamp = timestamp;
            this.player = player;
            this.mods = mods;
            this.action = action;
        }
    }
}
