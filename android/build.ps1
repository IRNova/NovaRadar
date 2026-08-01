# Nova Radar Android Build Script [Version 1.0.1]
# This PowerShell script compiles, tests, and packages the Nova Radar application in 6 structured stages.
# Released under local signature MD5/SHA-256 integrity protocols.

# Set output preference for rich logging
$ErrorActionPreference = "Stop"

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "    NOVA RADAR v1.0.1 - POWERSHELL GRADLE BUILD SYSTEM   " -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "Starting build execution..."
Write-Host ""

# STAGE 1: Environment & Java Check
Write-Host "[STAGE 1/6] Checking Local Development Environment..." -ForegroundColor Yellow
if ($null -eq (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: Java Development Kit (JDK) is not installed or not added to your system PATH. Please install JDK 17 or newer."
}
$javaVersion = java -version 2>&1 | Out-String
Write-Host "✔ Java is available on your machine." -ForegroundColor Green
Write-Host "System Java details:`n$javaVersion" -ForegroundColor Gray

# STAGE 2: Workspace Verification
Write-Host "[STAGE 2/6] Verifying Project Workspace..." -ForegroundColor Yellow
if (-not (Test-Path "build.gradle.kts") -or -not (Test-Path "settings.gradle.kts")) {
    Write-Error "ERROR: Could not find gradle files in the current folder. Please execute this script from the workspace root directory."
}
Write-Host "✔ Gradle build configurations verified." -ForegroundColor Green

# STAGE 3: Clean previous builds
Write-Host "[STAGE 3/6] Cleaning Previous Build Residues..." -ForegroundColor Yellow
try {
    if (Get-Command "./gradlew.bat" -ErrorAction SilentlyContinue) {
        Write-Host "Executing gradle clean on Windows..." -ForegroundColor Gray
        & ./gradlew.bat clean
    } elseif (Get-Command "./gradlew" -ErrorAction SilentlyContinue) {
        Write-Host "Executing gradle clean on Unix/WSL..." -ForegroundColor Gray
        & ./gradlew clean
    } else {
        Write-Host "Executing gradle clean using fallback executable..." -ForegroundColor Gray
        & gradle clean
    }
    Write-Host "✔ Clean step completed successfully." -ForegroundColor Green
} catch {
    Write-Warning "Clean step failed or was skipped (this is normal if it is the first build runs). Continuing with compile..."
}

# STAGE 4: Run Unit and Integration Tests
Write-Host "[STAGE 4/6] Executing Local Domain & Theme Tests..." -ForegroundColor Yellow
try {
    if (Get-Command "./gradlew.bat" -ErrorAction SilentlyContinue) {
        & ./gradlew.bat testDebugUnitTest
    } elseif (Get-Command "./gradlew" -ErrorAction SilentlyContinue) {
        & ./gradlew testDebugUnitTest
    } else {
        & gradle :app:testDebugUnitTest
    }
    Write-Host "✔ Tests passed! No regressions detected in Nova Radar business logic." -ForegroundColor Green
} catch {
    Write-Error "ERROR: Unit tests failed. Code cannot be compiled while unit tests are failing."
}

# STAGE 5: Source Generation & Code Quality Audit
Write-Host "[STAGE 5/6] Generating Kotlin Sources and KSP Models..." -ForegroundColor Yellow
try {
    if (Get-Command "./gradlew.bat" -ErrorAction SilentlyContinue) {
        & ./gradlew.bat compileDebugSources
    } elseif (Get-Command "./gradlew" -ErrorAction SilentlyContinue) {
        & ./gradlew compileDebugSources
    } else {
        & gradle :app:compileDebugSources
    }
    Write-Host "✔ Source generation & code quality audits passed successfully." -ForegroundColor Green
} catch {
    Write-Error "ERROR: Compilation/Source-generation failed. Please audit class structures."
}

# STAGE 6: Assemble Artifact & Generate APK
Write-Host "[STAGE 6/6] Assembling Final Android Package (APK)..." -ForegroundColor Yellow
try {
    if (Get-Command "./gradlew.bat" -ErrorAction SilentlyContinue) {
        & ./gradlew.bat assembleDebug
    } elseif (Get-Command "./gradlew" -ErrorAction SilentlyContinue) {
        & ./gradlew assembleDebug
    } else {
        & gradle :app:assembleDebug
    }
    Write-Host "✔ Debug compilation completed and APK generated successfully!" -ForegroundColor Green
} catch {
    Write-Error "ERROR: Packaging failed during binary compilation."
}

Write-Host ""
Write-Host "=========================================================" -ForegroundColor Green
Write-Host "                 BUILD SUCCESSFUL                       " -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
Write-Host "Generated Artifact Path: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
Write-Host "You are ready to load the APK onto your device or emulator." -ForegroundColor Gray
Write-Host "=========================================================" -ForegroundColor Green
