package com.grimnatorac.platform.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * UpdateChecker - Checks for updates from GitHub Releases and downloads them automatically.
 *
 * <p>Setup Instructions:</p>
 * <ol>
 *   <li>Create a GitHub repository for your fork</li>
 *   <li>Push your code to the repository</li>
 *   <li>Create a GitHub Release:
 *     <ul>
 *       <li>Go to your repository → Releases → Create a new release</li>
 *       <li>Tag version: e.g., "v1.0.0", "v1.0.1", etc. (MUST start with 'v')</li>
 *       <li>Release title: e.g., "Version 1.0.0"</li>
 *       <li>Upload your .jar file as a release asset</li>
 *       <li>Publish release</li>
 *     </ul>
 *   </li>
 *   <li>Update the constants below:
 *     <ul>
 *       <li>GITHUB_USER: Your GitHub username</li>
 *       <li>GITHUB_REPO: Your repository name</li>
 *       <li>JAR_NAME: The name of your .jar file (must match the asset name in releases)</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>How it works:</p>
 * <ul>
 *   <li>On server startup, checks the latest GitHub release version</li>
 *   <li>Compares it with the current plugin version from plugin.yml</li>
 *   <li>If a newer version is found, downloads it to plugins/GrimnatorAC-update/</li>
 *   <li>Notifies admins and console about the update</li>
 *   <li>Server owner must manually replace the .jar and restart</li>
 * </ul>
 *
 * <p>Version format: Releases must be tagged as "v1.0.0", "v1.0.1", etc.</p>
 * <p>The plugin.yml version should match (without 'v'): "1.0.0", "1.0.1", etc.</p>
 */
public class UpdateChecker {

    // ============ CONFIGURATION - EDIT THESE ============

    /**
     * Your GitHub username
     * Example: "grimnatorac" or "yourname"
     */
    private static final String GITHUB_USER = "YOUR_GITHUB_USERNAME";

    /**
     * Your GitHub repository name
     * Example: "safedeil" or "grimnatorac-fork"
     */
    private static final String GITHUB_REPO = "YOUR_REPO_NAME";

    /**
     * The name of the .jar file in your GitHub releases
     * Example: "GrimnatorAC.jar" or "safedeil-1.0.0.jar"
     * This MUST match the file name you upload to GitHub releases
     */
    private static final String JAR_NAME = "GrimnatorAC.jar";

    // ====================================================

    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/latest";
    private static final String USER_AGENT = "GrimnatorAC-UpdateChecker";

    private final JavaPlugin plugin;
    private String latestVersion = null;
    private String downloadUrl = null;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        if (!isConfigured()) {
            plugin.getLogger().warning("Grim » Update Checker is not configured!");
            plugin.getLogger().warning("Grim » Please edit UpdateChecker.java and set GITHUB_USER, GITHUB_REPO, and JAR_NAME");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (fetchLatestVersion()) {
                    String currentVersion = plugin.getDescription().getVersion();

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        plugin.getLogger().info("Grim » ========================================");
                        plugin.getLogger().info("Grim » NEW UPDATE AVAILABLE!");
                        plugin.getLogger().info("Grim » Current version: " + currentVersion);
                        plugin.getLogger().info("Grim » Latest version:  " + latestVersion);
                        plugin.getLogger().info("Grim » ========================================");
                        plugin.getLogger().info("Grim » Starting automatic download...");

                        if (downloadUpdate()) {
                            plugin.getLogger().info("Grim » ========================================");
                            plugin.getLogger().info("Grim » UPDATE DOWNLOADED SUCCESSFULLY!");
                            plugin.getLogger().info("Grim » Location: plugins/GrimnatorAC-update/" + JAR_NAME);
                            plugin.getLogger().info("Grim » Please replace the current .jar and restart the server");
                            plugin.getLogger().info("Grim » ========================================");

                            // Notify online admins
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    if (player.isOp() || player.hasPermission("grimnatorac.admin")) {
                                        player.sendMessage("§f§l[GrimnatorAC] §a§lNew update available: §e" + latestVersion);
                                        player.sendMessage("§f§l[GrimnatorAC] §7Downloaded to: §fplugins/GrimnatorAC-update/" + JAR_NAME);
                                        player.sendMessage("§f§l[GrimnatorAC] §7Please replace and restart the server");
                                    }
                                }
                            });
                        } else {
                            plugin.getLogger().warning("Grim » Failed to download update. Download manually from:");
                            plugin.getLogger().warning("Grim » https://github.com/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases");
                        }
                    } else {
                        plugin.getLogger().info("Grim » You are running the latest version: " + currentVersion);
                    }
                } else {
                    plugin.getLogger().warning("Grim » Failed to check for updates");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Grim » Error checking for updates", e);
            }
        });
    }

    /**
     * Check if the update checker is properly configured
     */
    private boolean isConfigured() {
        return !GITHUB_USER.equals("YOUR_GITHUB_USERNAME")
            && !GITHUB_REPO.equals("YOUR_REPO_NAME")
            && !JAR_NAME.equals("GrimnatorAC.jar");
    }

    /**
     * Fetch the latest version from GitHub API
     */
    private boolean fetchLatestVersion() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("Grim » GitHub API returned code: " + responseCode);
                return false;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response (simple manual parsing to avoid dependencies)
            String json = response.toString();

            // Extract "tag_name"
            int tagIndex = json.indexOf("\"tag_name\"");
            if (tagIndex == -1) return false;

            int colonIndex = json.indexOf(":", tagIndex);
            int startQuote = json.indexOf("\"", colonIndex) + 1;
            int endQuote = json.indexOf("\"", startQuote);

            latestVersion = json.substring(startQuote, endQuote);

            // Remove 'v' prefix if present (e.g., "v1.0.0" -> "1.0.0")
            if (latestVersion.startsWith("v")) {
                latestVersion = latestVersion.substring(1);
            }

            // Extract download URL for the JAR asset
            int assetsIndex = json.indexOf("\"assets\"");
            if (assetsIndex == -1) return false;

            int browserDownloadUrlIndex = json.indexOf("\"browser_download_url\"", assetsIndex);
            if (browserDownloadUrlIndex == -1) return false;

            int urlColonIndex = json.indexOf(":", browserDownloadUrlIndex);
            int urlStartQuote = json.indexOf("\"", urlColonIndex) + 1;
            int urlEndQuote = json.indexOf("\"", urlStartQuote);

            downloadUrl = json.substring(urlStartQuote, urlEndQuote);

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Grim » Error fetching latest version", e);
            return false;
        }
    }

    /**
     * Compare versions (format: "1.0.0")
     * Returns true if remote is newer than current
     */
    private boolean isNewerVersion(String current, String remote) {
        try {
            // Remove any non-numeric prefixes
            current = current.replaceAll("[^0-9.]", "");
            remote = remote.replaceAll("[^0-9.]", "");

            String[] currentParts = current.split("\\.");
            String[] remoteParts = remote.split("\\.");

            int maxLength = Math.max(currentParts.length, remoteParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;

                if (remotePart > currentPart) {
                    return true;
                } else if (remotePart < currentPart) {
                    return false;
                }
            }

            return false; // Versions are equal

        } catch (Exception e) {
            plugin.getLogger().warning("Grim » Error comparing versions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Download the update .jar file
     */
    private boolean downloadUpdate() {
        try {
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                plugin.getLogger().warning("Grim » No download URL found");
                return false;
            }

            // Create update directory
            File updateDir = new File(plugin.getDataFolder().getParentFile(), "GrimnatorAC-update");
            if (!updateDir.exists()) {
                updateDir.mkdirs();
            }

            File outputFile = new File(updateDir, JAR_NAME);

            // Download file
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 302 || responseCode == 301) {
                // Follow redirect
                String newUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(newUrl).openConnection();
                connection.setRequestProperty("User-Agent", USER_AGENT);
            }

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            plugin.getLogger().info("Grim » Downloaded " + outputFile.length() + " bytes");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Grim » Error downloading update", e);
            return false;
        }
    }

    /**
     * Get the latest version string (null if not checked yet)
     */
    public String getLatestVersion() {
        return latestVersion;
    }
}
