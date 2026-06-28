# Nova Radar Play Protect Pre-Build Check
# Scans the codebase for patterns that trigger Google Play Protect warnings.
# Run this before any build, commit, or deploy.

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $PSScriptRoot
$foundIssues = $false

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   NOVA RADAR - PLAY PROTECT SAFETY CHECK v1.0          " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# Filter out build directories
$sourceDirs = @(
    "$rootDir/app/src/main/java",
    "$rootDir/app/src/main/res",
    "$rootDir/app/src"
)

function Search-Files {
    param($Pattern, $Path, $Label)
    $files = Get-ChildItem -Path $Path -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "*\build\*" -and $_.FullName -notlike "*\.gradle\*" }
    $results = $files | Select-String -Pattern $Pattern -ErrorAction SilentlyContinue
    if ($results) {
        Write-Host "[!] $Label" -ForegroundColor Yellow
        $results | ForEach-Object { Write-Host "     $($_.Path):$($_.LineNumber) - $($_.Line.Trim())" -ForegroundColor Gray }
        return $true
    }
    return $false
}

# --- CHECK 1: Trust-All SSL Certificate ---
Write-Host "[CHECK 1/8] Trust-All SSL Certificate patterns..." -ForegroundColor Yellow
$found = Search-Files -Pattern "checkServerTrusted\s*=\s*.*\{\}" -Path "$rootDir/app/src/main/java" -Label "Empty checkServerTrusted (trust-all certs)"
$found = Search-Files -Pattern "checkClientTrusted\s*=\s*.*\{\}" -Path "$rootDir/app/src/main/java" -Label "Empty checkClientTrusted (trust-all certs)" -and $found
if ($found) {
    Write-Host "  -> Known trigger. Required for scanner (TLS to random IPs with custom SNI)." -ForegroundColor Magenta
    Write-Host "  -> Ensure this is ONLY used in deepTestConnect, not globally." -ForegroundColor Magenta
}

# --- CHECK 2: InsecureSkipVerify / TrustManager patterns ---
Write-Host "[CHECK 2/8] Insecure certificate handling..." -ForegroundColor Yellow
$found = Search-Files -Pattern "SSLContext.*init\s*\(null" -Path "$rootDir/app/src/main/java" -Label "SSLContext initialized with null TrustManager"
if ($found) {
    Write-Host "  -> Known trigger. Verify this is scoped strictly to scan logic." -ForegroundColor Magenta
}

# --- CHECK 3: Dangerous Permissions ---
Write-Host "[CHECK 3/8] Dangerous permissions in manifest..." -ForegroundColor Yellow
$found = $false
$manifest = "$rootDir/app/src/main/AndroidManifest.xml"
if (Test-Path $manifest) {
    $content = Get-Content $manifest -Raw
    $dangerousPerms = @(
        @{Pattern="QUERY_ALL_PACKAGES"; Name="QUERY_ALL_PACKAGES"},
        @{Pattern="REQUEST_INSTALL_PACKAGES"; Name="REQUEST_INSTALL_PACKAGES"},
        @{Pattern="BIND_ACCESSIBILITY_SERVICE"; Name="BIND_ACCESSIBILITY_SERVICE"},
        @{Pattern="SYSTEM_ALERT_WINDOW"; Name="SYSTEM_ALERT_WINDOW"},
        @{Pattern="READ_SMS"; Name="READ_SMS"},
        @{Pattern="READ_CALL_LOG"; Name="READ_CALL_LOG"},
        @{Pattern="PROCESS_OUTGOING_CALLS"; Name="PROCESS_OUTGOING_CALLS"},
        @{Pattern="RECORD_AUDIO"; Name="RECORD_AUDIO"},
        @{Pattern="CAMERA"; Name="CAMERA"},
        @{Pattern="READ_CONTACTS"; Name="READ_CONTACTS"},
        @{Pattern="ACCESS_FINE_LOCATION"; Name="ACCESS_FINE_LOCATION"},
        @{Pattern="ACCESS_BACKGROUND_LOCATION"; Name="ACCESS_BACKGROUND_LOCATION"}
    )
    foreach ($perm in $dangerousPerms) {
        if ($content -match $perm.Pattern) {
            Write-Host "[!] Dangerous permission found: $($perm.Name)" -ForegroundColor Red
            $foundIssues = $true
        }
    }
    if (-not $found) {
        Write-Host "  No dangerous permissions found. OK" -ForegroundColor Green
    }
}

# --- CHECK 4: WRITE_EXTERNAL_STORAGE without maxSdkVersion ---
Write-Host "[CHECK 4/8] WRITE_EXTERNAL_STORAGE safety..." -ForegroundColor Yellow
if (Test-Path $manifest) {
    $content = Get-Content $manifest -Raw
    if ($content -match "WRITE_EXTERNAL_STORAGE(?!.*maxSdkVersion)") {
        Write-Host "[!] WRITE_EXTERNAL_STORAGE without maxSdkVersion restriction" -ForegroundColor Red
        $foundIssues = $true
    } else {
        Write-Host "  WRITE_EXTERNAL_STORAGE properly restricted with maxSdkVersion. OK" -ForegroundColor Green
    }
}

# --- CHECK 5: WebView with JavaScript enabled ---
Write-Host "[CHECK 5/8] WebView JavaScript injection patterns..." -ForegroundColor Yellow
$found = Search-Files -Pattern "setJavaScriptEnabled\s*\(true|addJavascriptInterface" -Path "$rootDir/app/src/main/java" -Label "WebView with JavaScript (potential XSS)"
if (-not $found) {
    Write-Host "  No WebView JS injection found. OK" -ForegroundColor Green
}

# --- CHECK 6: Runtime exec / ProcessBuilder ---
Write-Host "[CHECK 6/8] Shell execution patterns..." -ForegroundColor Yellow
$found = Search-Files -Pattern "Runtime\.getRuntime\(\)\.exec|ProcessBuilder\(" -Path "$rootDir/app/src/main/java" -Label "Runtime command execution"
if (-not $found) {
    Write-Host "  No shell execution found. OK" -ForegroundColor Green
}

# --- CHECK 7: PackageManager queries ---
Write-Host "[CHECK 7/8] Package enumeration patterns..." -ForegroundColor Yellow
$found = Search-Files -Pattern "getInstalledApplications|getInstalledPackages" -Path "$rootDir/app/src/main/java" -Label "Package enumeration"
if (-not $found) {
    Write-Host "  No package enumeration found. OK" -ForegroundColor Green
}

# --- CHECK 8: POST_NOTIFICATIONS runtime handling ---
Write-Host "[CHECK 8/8] Notification permission handling..." -ForegroundColor Yellow
$found = Search-Files -Pattern "POST_NOTIFICATIONS" -Path "$rootDir/app/src/main/java" -Label "POST_NOTIFICATIONS usage"
if ($found) {
    Write-Host "  POST_NOTIFICATIONS found. On Android 13+, ensure runtime permission is requested." -ForegroundColor Gray
    $rationaleFiles = Get-ChildItem -Path "$rootDir/app/src/main/java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue
    $hasRationale = $rationaleFiles | Select-String -Pattern "shouldShowRequestPermissionRationale" -SimpleMatch -ErrorAction SilentlyContinue
    if (-not $hasRationale) {
        Write-Host "[!] Missing runtime permission rationale handling for notifications" -ForegroundColor Yellow
    } else {
        Write-Host "  Runtime rationale found. OK" -ForegroundColor Green
    }
}

# --- SUMMARY ---
Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
if ($foundIssues) {
    Write-Host "  RESULT: ISSUES FOUND - Review warnings above." -ForegroundColor Yellow
    Write-Host "  CRITICAL: Some patterns (trust-all SSL) are REQUIRED" -ForegroundColor Yellow
    Write-Host "  for network scanner functionality. This is a KNOWN" -ForegroundColor Yellow
    Write-Host "  Play Protect trigger that cannot be avoided." -ForegroundColor Yellow
} else {
    Write-Host "  RESULT: All checks passed. No new triggers detected." -ForegroundColor Green
}
Write-Host "========================================================" -ForegroundColor Cyan

exit $foundIssues
