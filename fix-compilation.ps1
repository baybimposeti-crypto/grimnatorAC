# Quick fix script for GrimnatorAC compilation error
# Run this in the grimnatorAC directory after cloning

Write-Host "Fixing GrimnatorAC compilation error..." -ForegroundColor Yellow

$file = "common\src\main\java\com\grimnatorac\checks\impl\aim\AimLinearAssist.java"

if (!(Test-Path $file)) {
    Write-Host "Error: File not found: $file" -ForegroundColor Red
    Write-Host "Make sure you're running this from the grimnatorAC directory" -ForegroundColor Red
    exit 1
}

# Create backup
Write-Host "Creating backup..." -ForegroundColor Cyan
Copy-Item $file "$file.bak" -Force

# Apply fix
Write-Host "Applying fix..." -ForegroundColor Cyan
(Get-Content $file) `
    -replace 'rotationUpdate\.getTo\(\)\.getYaw\(\)', 'rotationUpdate.getTo().yaw()' `
    -replace 'rotationUpdate\.getTo\(\)\.getPitch\(\)', 'rotationUpdate.getTo().pitch()' |
    Set-Content $file

Write-Host "Fix applied successfully!" -ForegroundColor Green
Write-Host "Backup saved as: $file.bak" -ForegroundColor Gray
Write-Host ""
Write-Host "Now run: .\gradlew.bat build" -ForegroundColor Yellow
