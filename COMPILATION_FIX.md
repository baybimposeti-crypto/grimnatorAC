# Compilation Fix for GrimnatorAC

## Issue

The upstream GrimnatorAC repository has a compilation error in `AimLinearAssist.java`:

```
error: cannot find symbol
    float currentYaw   = rotationUpdate.getTo().getYaw();
                                               ^
  symbol:   method getYaw()
  location: class HeadRotation
```

## Root Cause

`HeadRotation` is a Java record with fields `yaw` and `pitch`, not getter methods. The code incorrectly tries to call `.getYaw()` and `.getPitch()` which don't exist.

## Fix

Edit: `common/src/main/java/com/grimnatorac/checks/impl/aim/AimLinearAssist.java`

**Find (around line 122-123):**
```java
float currentYaw   = rotationUpdate.getTo().getYaw();
float currentPitch = rotationUpdate.getTo().getPitch();
```

**Replace with:**
```java
float currentYaw   = rotationUpdate.getTo().yaw();
float currentPitch = rotationUpdate.getTo().pitch();
```

## Apply the Fix

### Method 1: Manual Edit
1. Open `common/src/main/java/com/grimnatorac/checks/impl/aim/AimLinearAssist.java`
2. Go to lines 122-123
3. Change `.getYaw()` to `.yaw()`
4. Change `.getPitch()` to `.pitch()`
5. Save the file

### Method 2: Command Line (PowerShell)
```powershell
cd C:\Users\user\Desktop\grimnatorAC

# Create backup
Copy-Item common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java.bak

# Apply fix
(Get-Content common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java) `
    -replace 'rotationUpdate\.getTo\(\)\.getYaw\(\)', 'rotationUpdate.getTo().yaw()' `
    -replace 'rotationUpdate\.getTo\(\)\.getPitch\(\)', 'rotationUpdate.getTo().pitch()' |
    Set-Content common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java
```

### Method 3: Copy from Working Repository
```powershell
# Copy the fixed file from safedeil to grimnatorAC
Copy-Item C:\Users\user\Desktop\safedeil\common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java `
          C:\Users\user\Desktop\grimnatorAC\common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java
```

## Verify the Fix

```powershell
cd C:\Users\user\Desktop\grimnatorAC
.\gradlew.bat build
```

You should see:
```
BUILD SUCCESSFUL
```

## Why This Happens

This is a bug in the upstream GrimnatorAC repository. Java records automatically generate accessor methods with the same name as the field, not getters like `getFieldName()`.

**Java Record:**
```java
public record HeadRotation(float yaw, float pitch) {}
```

**Generated Methods:**
- `.yaw()` ✅ Correct
- `.pitch()` ✅ Correct
- `.getYaw()` ❌ Does not exist
- `.getPitch()` ❌ Does not exist

The fix has been applied in the `safedeil` directory and builds successfully.
