# Update Checker Setup Guide

The Update Checker automatically checks for new versions on GitHub and downloads them for you.

## Setup Instructions

### 1. Configure UpdateChecker.java

Edit the file: `bukkit/src/main/java/com/grimnatorac/platform/bukkit/utils/UpdateChecker.java`

Find these lines near the top of the class and update them:

```java
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
```

**Example Configuration:**
```java
private static final String GITHUB_USER = "john-doe";
private static final String GITHUB_REPO = "grimnatorac-custom";
private static final String JAR_NAME = "GrimnatorAC.jar";
```

### 2. Create a GitHub Repository

1. Go to https://github.com/new
2. Name your repository (e.g., "grimnatorac-custom" or "safedeil-fork")
3. Make it public (required for update checker) or private (requires authentication)
4. Create the repository

### 3. Push Your Code to GitHub

```bash
cd C:\Users\user\Desktop\safedeil

# Initialize git (if not already done)
git init

# Add your remote repository
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

# Add all files
git add .

# Commit
git commit -m "Initial commit"

# Push to GitHub
git push -u origin main
```

### 4. Create a GitHub Release

1. Go to your repository on GitHub
2. Click on **"Releases"** (right side of the page)
3. Click **"Create a new release"**

4. **Tag version**: Enter a version tag (MUST start with 'v')
   - Examples: `v1.0.0`, `v1.0.1`, `v1.1.0`, `v2.0.0`
   - The version MUST match your plugin.yml version (without the 'v')
   
5. **Release title**: Enter a title (e.g., "Version 1.0.0")

6. **Description**: (Optional) Describe what's new in this version
   ```
   ## Changes in v1.0.0
   - Added translation key mod detection
   - Added automatic update checker
   - Fixed various bugs
   ```

7. **Upload your .jar file**:
   - Build your plugin: `gradlew.bat obfuscate`
   - Find the .jar in: `build/libs/` folder
   - Drag and drop the .jar file into the "Attach binaries" section
   - **IMPORTANT**: The filename MUST match the `JAR_NAME` you set in UpdateChecker.java

8. Click **"Publish release"**

### 5. Version Numbering

The plugin uses semantic versioning (MAJOR.MINOR.PATCH):

- **MAJOR**: Breaking changes (1.0.0 → 2.0.0)
- **MINOR**: New features (1.0.0 → 1.1.0)
- **PATCH**: Bug fixes (1.0.0 → 1.0.1)

**Important:**
- GitHub release tag: `v1.0.0` (with 'v')
- plugin.yml version: `1.0.0` (without 'v')

### 6. Update Your plugin.yml

Edit `bukkit/src/main/resources/plugin.yml`:

```yaml
name: GrimnatorAC
version: 1.0.0  # Update this with each release
main: com.grimnatorac.platform.bukkit.GrimnatorACBukkitLoaderPlugin
api-version: 1.17
```

**Every time you create a new release, increment this version number!**

## How It Works

### On Server Startup

1. Server starts → UpdateChecker runs after 3 seconds
2. Checks GitHub API for latest release
3. Compares with current plugin version from plugin.yml
4. If newer version found:
   - Downloads the .jar file to `plugins/GrimnatorAC-update/`
   - Logs update info to console
   - Notifies online admins with OP permission

### Console Output

```
[GrimnatorAC] ========================================
[GrimnatorAC] NEW UPDATE AVAILABLE!
[GrimnatorAC] Current version: 1.0.0
[GrimnatorAC] Latest version:  1.0.1
[GrimnatorAC] ========================================
[GrimnatorAC] Starting automatic download...
[GrimnatorAC] ========================================
[GrimnatorAC] UPDATE DOWNLOADED SUCCESSFULLY!
[GrimnatorAC] Location: plugins/GrimnatorAC-update/GrimnatorAC.jar
[GrimnatorAC] Please replace the current .jar and restart the server
[GrimnatorAC] ========================================
```

### Admin Notification

Players with OP or `grimnatorac.admin` permission will see:
```
[GrimnatorAC] New update available: 1.0.1
[GrimnatorAC] Downloaded to: plugins/GrimnatorAC-update/GrimnatorAC.jar
[GrimnatorAC] Please replace and restart the server
```

## Installing Updates

1. **Stop your server**
2. Navigate to `plugins/GrimnatorAC-update/`
3. Copy the new .jar file
4. Replace the old .jar in `plugins/` folder
5. Delete the `GrimnatorAC-update/` folder
6. **Start your server**

## Troubleshooting

### "Update Checker is not configured!"

You forgot to edit the constants in UpdateChecker.java. Set `GITHUB_USER`, `GITHUB_REPO`, and `JAR_NAME`.

### "Failed to check for updates"

**Possible causes:**
1. Your repository doesn't exist or is private
2. You haven't created any releases yet
3. Internet connection issues
4. GitHub API rate limit (60 requests/hour for unauthenticated)

### "Failed to download update"

**Possible causes:**
1. The .jar file name in the release doesn't match `JAR_NAME` in UpdateChecker.java
2. You didn't upload a .jar file to the release
3. Network issues during download

### Version not detected correctly

Make sure:
- GitHub release tag starts with 'v' (e.g., `v1.0.0`)
- plugin.yml version does NOT have 'v' (e.g., `1.0.0`)
- Both versions use format: MAJOR.MINOR.PATCH (e.g., 1.0.0, not 1.0 or 1.0.0.0)

## Example Workflow

### Initial Release (v1.0.0)

1. Edit UpdateChecker.java with your GitHub details
2. Edit plugin.yml: `version: 1.0.0`
3. Build: `gradlew.bat obfuscate`
4. Create GitHub release with tag `v1.0.0`
5. Upload the .jar file
6. Publish release

### Bug Fix Release (v1.0.1)

1. Fix bugs in your code
2. Edit plugin.yml: `version: 1.0.1`
3. Commit and push to GitHub
4. Build: `gradlew.bat obfuscate`
5. Create GitHub release with tag `v1.0.1`
6. Upload the new .jar file
7. Publish release
8. Servers with v1.0.0 will automatically detect and download v1.0.1

### Feature Release (v1.1.0)

1. Add new features
2. Edit plugin.yml: `version: 1.1.0`
3. Commit and push to GitHub
4. Build: `gradlew.bat obfuscate`
5. Create GitHub release with tag `v1.1.0`
6. Upload the new .jar file
7. Publish release
8. All older versions will automatically detect and download v1.1.0

## Security Notes

- Update checker uses HTTPS for all connections
- Only downloads from GitHub (no third-party servers)
- Does NOT auto-install (manual replacement required)
- Server owners must manually replace .jar files for safety
- GitHub API rate limit: 60 requests/hour (plenty for most servers)

## Disabling Update Checker

If you want to disable the update checker:

1. Open `bukkit/src/main/java/com/grimnatorac/platform/bukkit/GrimnatorACBukkitLoaderPlugin.java`
2. Find the `onEnable()` method
3. Comment out or remove this block:
   ```java
   // Check for updates from GitHub releases
   try {
       updateChecker = new UpdateChecker(this);
       Bukkit.getScheduler().runTaskLater(this, () -> updateChecker.checkForUpdates(), 60L);
   } catch (Throwable t) {
       getLogger().warning("Grim » Failed to initialize Update Checker: " + t.getMessage());
   }
   ```
4. Rebuild: `gradlew.bat obfuscate`

## Support

If you encounter issues with the update checker:
1. Check console logs for error messages
2. Verify your GitHub repository is public and has releases
3. Make sure release tags start with 'v'
4. Ensure .jar file names match in release and UpdateChecker.java
