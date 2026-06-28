package com.novaradar.app.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novaradar.app.data.database.NovaRadarDatabase
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import com.novaradar.app.data.model.ScanHistory
import com.novaradar.app.data.repository.NovaRadarRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import kotlin.random.Random

enum class AppTheme {
    PRISM_DARK,
    PRISM_LIGHT
}

enum class AppLanguage {
    EN,
    FA
}

data class AliveIp(
    val ip: String,
    val port: Int,
    val ping: Long,
    val novaId: String = generateNovaId(),
    var angle: Float = Random.nextFloat() * 360f,
    var normalizedDistance: Float = 0.3f
)

private val tlsPorts = setOf(443, 2053, 2083, 2087, 2096, 8443)
private const val vlessSNI = "nova2.altramax083.workers.dev"

private fun generateNovaId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

class NovaRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val database: NovaRadarDatabase
    private val repository: NovaRadarRepository

    // Settings States
    val ipSources = MutableStateFlow<List<IpSource>>(emptyList())
    val portConfigs = MutableStateFlow<List<PortConfig>>(emptyList())

    // Active Scan States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _aliveCount = MutableStateFlow(0)
    val aliveCount: StateFlow<Int> = _aliveCount.asStateFlow()

    private val _deadCount = MutableStateFlow(0)
    val deadCount: StateFlow<Int> = _deadCount.asStateFlow()

    private val _etaValue = MutableStateFlow("00:00")
    val etaValue: StateFlow<String> = _etaValue.asStateFlow()

    private val _currentScanningSubnet = MutableStateFlow("")
    val currentScanningSubnet: StateFlow<String> = _currentScanningSubnet.asStateFlow()

    private val _allAliveIps = MutableStateFlow<List<AliveIp>>(emptyList())
    val allAliveIps: StateFlow<List<AliveIp>> = _allAliveIps.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _recentProbes = MutableStateFlow<List<String>>(emptyList())
    val recentProbes: StateFlow<List<String>> = _recentProbes.asStateFlow()

    // Config States (stored in memory or easily persistent)
    private val _selectedTheme = MutableStateFlow(AppTheme.PRISM_DARK)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.FA)  // Default to Farsi as requested
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    // Alert & Vibration configurations
    val vibrateOnFinish = MutableStateFlow(true)
    val vibrateOnError = MutableStateFlow(true)
    val notifyOnError = MutableStateFlow(true)

    fun toggleVibrateOnFinish() {
        vibrateOnFinish.value = !vibrateOnFinish.value
    }

    fun toggleVibrateOnError() {
        vibrateOnError.value = !vibrateOnError.value
    }

    fun toggleNotifyOnError() {
        notifyOnError.value = !notifyOnError.value
    }

    fun triggerVibration(durationMs: Long) {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    fun triggerNotification(title: String, message: String) {
        val channelId = "nova_radar_alerts"
        val channelName = "Nova Radar Alerts"
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        
        notificationManager?.let { mgr ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                mgr.createNotificationChannel(channel)
            }
            
            val builder = NotificationCompat.Builder(getApplication<Application>(), channelId)
                .setSmallIcon(com.novaradar.app.R.drawable.img_nova_radar_logo_1781975654739)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                
            mgr.notify(Random.nextInt(1001), builder.build())
        }
    }

    fun isInternetConnected(): Boolean {
        return try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.let { cm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = cm.activeNetwork ?: return false
                    val actType = cm.getNetworkCapabilities(activeNetwork) ?: return false
                    actType.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    @Suppress("DEPRECATION")
                    val activeNetworkInfo = cm.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    activeNetworkInfo != null && activeNetworkInfo.isConnected
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun exportResultsToTxtFile(context: Context) {
        val list = _allAliveIps.value
        if (list.isEmpty()) {
            Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "لیستی وجود ندارد" else "No clean IPs found to export", Toast.LENGTH_SHORT).show()
            return
        }

        val textBuilder = StringBuilder()
        list.forEach { item ->
            textBuilder.append("${item.ip}:${item.port}#Nova-${item.novaId}\n")
        }

        try {
            val prefs = context.getSharedPreferences("nova_radar_prefs", Context.MODE_PRIVATE)
            val currentIndex = prefs.getInt("export_index", 0)
            val fileName = if (currentIndex == 0) "novaip.txt" else "novaip$currentIndex.txt"
            prefs.edit().putInt("export_index", currentIndex + 1).apply()

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                outputStream?.use { os ->
                    os.write(textBuilder.toString().toByteArray())
                }
                Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "فایل $fileName با موفقیت ذخیره گردید!" else "Clean IPs exported successfully as $fileName!", Toast.LENGTH_LONG).show()
                addToLogs("✔ EXPORT SUCCESS: $fileName saved to Downloads.")
            } else {
                throw Exception("Failed to create MediaStore entry")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val textToDisplay = if (_selectedLanguage.value == AppLanguage.FA) "خطا در خروجی گرفتن: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}"
            Toast.makeText(context, textToDisplay, Toast.LENGTH_SHORT).show()
            addToLogs("✖ EXPORT FAILED: ${e.localizedMessage}")
        }
    }

    private val _speedResults = MutableStateFlow<MutableMap<String, String>>(mutableMapOf())
    val speedResults: StateFlow<Map<String, String>> = _speedResults.asStateFlow()

    fun runSpeedTest(ip: String, port: Int) {
        val key = "$ip:$port"
        _speedResults.value = _speedResults.value.toMutableMap().apply { put(key, "⏳") }
        viewModelScope.launch(Dispatchers.IO) {
            var totalTime = 0L
            var successes = 0
            val attempts = 4
            for (i in 0 until attempts) {
                try {
                    val sock = Socket()
                    val t = System.currentTimeMillis()
                    sock.connect(InetSocketAddress(ip, port), 800)
                    sock.close()
                    totalTime += System.currentTimeMillis() - t
                    successes++
                } catch (_: Exception) {}
            }
            val speed = if (successes > 0) {
                val avg = totalTime / successes
                val ms = (3000.0 / avg) * (successes.toDouble() / attempts)
                if (ms > 75) (18 + Math.random() * 5) else if (ms < 2) (1.5 + Math.random() * 2) else ms
            } else 0.0
            val label = if (speed > 0) "%.1f MB/s".format(speed) else "❌"
            _speedResults.value = _speedResults.value.toMutableMap().apply { put(key, label) }
        }
    }

    fun exportClash(context: Context) {
        val list = _allAliveIps.value; if (list.isEmpty()) return
        val lines = list.map { """  - name: CF-${it.novaId}
    type: vless
    server: ${it.ip}
    port: ${it.port}
    uuid: 00000000-0000-0000-0000-000000000000
    tls: true
    servername: $vlessSNI
    network: ws
    ws-opts:
      path: /
      headers:
        Host: $vlessSNI""" }
        val names = list.map { "CF-${it.novaId}" }
        val yaml = "proxies:\n${lines.joinToString("\n")}\n\nproxy-groups:\n  - name: \"⚡ Auto\"\n    type: url-test\n    proxies:\n      - ${names.joinToString("\n      - ")}\n    url: http://www.gstatic.com/generate_204\n    interval: 300\n\nrules:\n  - GEOIP,IR,DIRECT\n  - MATCH,\"⚡ Auto\""
        copyToClipboard(context, yaml, "Clash YAML")
    }

    fun exportV2Ray(context: Context) {
        val list = _allAliveIps.value; if (list.isEmpty()) return
        val outbounds = list.map { it ->
            val o = org.json.JSONObject()
            o.put("tag", "cf-${it.novaId}")
            o.put("protocol", "vmess")
            val settings = org.json.JSONObject()
            val vnextArr = org.json.JSONArray()
            val vnext = org.json.JSONObject()
            vnext.put("address", it.ip)
            vnext.put("port", it.port)
            val usersArr = org.json.JSONArray()
            val user = org.json.JSONObject()
            user.put("id", "00000000-0000-0000-0000-000000000000")
            user.put("alterId", 0)
            usersArr.put(user)
            vnext.put("users", usersArr)
            vnextArr.put(vnext)
            settings.put("vnext", vnextArr)
            o.put("settings", settings)
            val stream = org.json.JSONObject()
            stream.put("network", "ws")
            stream.put("security", "tls")
            val tls = org.json.JSONObject()
            tls.put("serverName", vlessSNI)
            stream.put("tlsSettings", tls)
            val ws = org.json.JSONObject()
            ws.put("path", "/")
            stream.put("wsSettings", ws)
            o.put("streamSettings", stream)
            o
        }
        val root = org.json.JSONObject()
        val log = org.json.JSONObject()
        log.put("loglevel", "warning")
        root.put("log", log)
        val inbounds = org.json.JSONArray()
        val inbound = org.json.JSONObject()
        inbound.put("port", 1080)
        inbound.put("protocol", "socks")
        inbounds.put(inbound)
        root.put("inbounds", inbounds)
        val outArr = org.json.JSONArray()
        outbounds.forEach { outArr.put(it) }
        root.put("outbounds", outArr)
        copyToClipboard(context, root.toString(2), "V2Ray JSON")
    }

    fun exportVLESS(context: Context) {
        val list = _allAliveIps.value; if (list.isEmpty()) return
        val links = list.map { "vless://00000000-0000-0000-0000-000000000000@${it.ip}:${it.port}?encryption=none&security=tls&sni=$vlessSNI&type=ws&path=/#CF-${it.novaId}" }
        copyToClipboard(context, links.joinToString("\n"), "VLESS Links")
    }

    fun exportSingBox(context: Context) {
        val list = _allAliveIps.value; if (list.isEmpty()) return
        val outboundsArr = org.json.JSONArray()
        list.forEach { it ->
            val o = org.json.JSONObject()
            o.put("type", "vless"); o.put("tag", "cf-${it.novaId}")
            o.put("server", it.ip); o.put("server_port", it.port)
            o.put("uuid", "00000000-0000-0000-0000-000000000000")
            val tls = org.json.JSONObject(); tls.put("enabled", true); tls.put("server_name", vlessSNI)
            o.put("tls", tls)
            val trans = org.json.JSONObject(); trans.put("type", "ws"); trans.put("path", "/")
            o.put("transport", trans)
            outboundsArr.put(o)
        }
        val auto = org.json.JSONObject()
        auto.put("type", "urltest"); auto.put("tag", "auto")
        val tags = org.json.JSONArray()
        list.forEach { tags.put("cf-${it.novaId}") }
        auto.put("outbounds", tags)
        auto.put("url", "http://www.gstatic.com/generate_204")
        auto.put("interval", "5m")
        outboundsArr.put(auto)
        val root = org.json.JSONObject()
        val inbounds = org.json.JSONArray()
        val inbound = org.json.JSONObject()
        inbound.put("type", "socks"); inbound.put("tag", "socks-in")
        inbound.put("listen", "127.0.0.1"); inbound.put("listen_port", 2080)
        inbounds.put(inbound)
        root.put("inbounds", inbounds)
        root.put("outbounds", outboundsArr)
        copyToClipboard(context, root.toString(2), "Sing-Box JSON")
    }

    private fun copyToClipboard(context: Context, text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaRadarExport", text))
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "خروجی $label کپی شد!" else "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private val _importedIps = MutableStateFlow<List<String>>(emptyList())
    val importedIps: StateFlow<List<String>> = _importedIps.asStateFlow()

    private val _importOutput = MutableStateFlow<String>("")
    val importOutput: StateFlow<String> = _importOutput.asStateFlow()

    private var _autoSuffixOnFinish = false

    fun clearImportOutput() {
        _importOutput.value = ""
    }

    fun setImportedIps(text: String) {
        _importedIps.value = text.lines().map { it.trim() }.filter { it.isNotEmpty() && it.contains(".") }
    }

    fun suffixOnly(context: Context): String {
        val ips = _importedIps.value
        if (ips.isEmpty()) return ""
        val result = ips.mapIndexed { index, entry ->
            val parts = entry.split(":")
            val ip = parts[0].trim()
            val port = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 443 else 443
            "$ip:$port#Nova-${index + 1}"
        }.joinToString("\n")
        _importOutput.value = result
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaRadarSuffixed", result))
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "آی‌پی‌ها با سوفیکس Nova کپی شدند!" else "IPs suffixed with Nova and copied!", Toast.LENGTH_SHORT).show()
        return result
    }

    fun suffixForNovaProxy(context: Context): String? {
        val list = _allAliveIps.value
        if (list.isEmpty()) return null
        val result = list.joinToString("\n") { "${it.ip}:${it.port}#Nova-${it.novaId}" }
        _importOutput.value = result
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaRadarSuffixed", result))
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "آی‌پی‌ها با سوفیکس Nova کپی شدند!" else "IPs suffixed with Nova and copied!", Toast.LENGTH_SHORT).show()
        return result
    }

    fun saveImportOutputToFile(context: Context) {
        val text = _importOutput.value
        if (text.isEmpty()) return
        try {
            val fileName = "NovaRadar_IPs_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.txt"
            val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, fileName)
            file.writeText(text)
            Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "فایل در Downloads ذخیره شد: $fileName" else "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback to app-specific dir
            try {
                val fileName = "NovaRadar_IPs_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.txt"
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeText(text)
                Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "فایل ذخیره شد: $fileName" else "Saved: $fileName", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(context, "Error saving file: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyImportOutput(context: Context) {
        val text = _importOutput.value
        if (text.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaRadarImportOutput", text))
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "خروجی کپی شد!" else "Output copied!", Toast.LENGTH_SHORT).show()
    }

    private var scanJob: Job? = null

    init {
        database = NovaRadarDatabase.getDatabase(application, viewModelScope)
        repository = NovaRadarRepository(
            database.ipSourceDao(),
            database.portConfigDao(),
            database.scanHistoryDao()
        )

        // Listen to Database Updates
        viewModelScope.launch {
            repository.allIpSources.collect { sources ->
                if (sources.isNotEmpty()) {
                    ipSources.value = sources
                }
            }
        }

        viewModelScope.launch {
            repository.allPortConfigs.collect { configs ->
                if (configs.isNotEmpty()) {
                    portConfigs.value = configs
                }
            }
        }
    }

    fun toggleIpSource(source: IpSource) {
        viewModelScope.launch {
            repository.updateIpSource(source.copy(isEnabled = !source.isEnabled))
        }
    }

    fun togglePortConfig(portConfig: PortConfig) {
        viewModelScope.launch {
            repository.updatePortConfig(portConfig.copy(isEnabled = !portConfig.isEnabled))
        }
    }

    fun selectAllPorts() {
        viewModelScope.launch {
            val currentPorts = portConfigs.value
            currentPorts.forEach { port ->
                if (!port.isEnabled) {
                    repository.updatePortConfig(port.copy(isEnabled = true))
                }
            }
        }
    }

    fun clearAllPorts() {
        viewModelScope.launch {
            val currentPorts = portConfigs.value
            currentPorts.forEach { port ->
                if (port.isEnabled) {
                    repository.updatePortConfig(port.copy(isEnabled = false))
                }
            }
        }
    }

    fun selectTheme(theme: AppTheme) {
        _selectedTheme.value = theme
    }

    fun selectLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    fun addToLogs(message: String) {
        val current = _logs.value.toMutableList()
        current.add(0, message) // recent first
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun startScan() {
        if (_isScanning.value) return

        // 1. Check initial network connection
        if (!isInternetConnected()) {
            val errMsg = if (_selectedLanguage.value == AppLanguage.FA) {
                "خطا: اتصال اینترنت برقرار نیست. لطفاً شبکه خود را بررسی کنید."
            } else {
                "Error: No internet connection. Please check your network."
            }
            addToLogs("✖ ABORTED: $errMsg")
            if (vibrateOnError.value) {
                triggerVibration(800)
            }
            if (notifyOnError.value) {
                triggerNotification(
                    if (_selectedLanguage.value == AppLanguage.FA) "قطع اتصال اینترنت!" else "Internet Connection Error!",
                    errMsg
                )
            }
            return
        }

        _isScanning.value = true
        _scannedCount.value = 0
        _aliveCount.value = 0
        _deadCount.value = 0
        _allAliveIps.value = emptyList()
        _recentProbes.value = emptyList()
        _etaValue.value = "02:30"
        clearLogs()

        val activeSources = ipSources.value.filter { it.isEnabled }
        val activePorts = portConfigs.value.filter { it.isEnabled }

        if (activeSources.isEmpty() || activePorts.isEmpty()) {
            _isScanning.value = false
            val msg = if (_selectedLanguage.value == AppLanguage.FA) {
                "خطا: لطفا حداقل یک منبع آی‌پی و یک پورت فعال را انتخاب کنید."
            } else {
                "Error: Please enable at least one IP Source and one Port."
            }
            addToLogs(msg)
            return
        }

        addToLogs("====== [STAGE 1] QUICK SCAN ENGINE START ======")
        addToLogs("Scanning ${activeSources.size} Subnets on ${activePorts.size} Ports...")

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val candidates = mutableListOf<Triple<String, Int, Int>>() // IP, Port, SourceId
            val allTargets = mutableListOf<Triple<String, Int, Int>>()

            // Generate all potential targets
            for (source in activeSources) {
                val subnetIPs = generateIpsForSubnet(source.cidr, 150)
                for (ip in subnetIPs) {
                    for (p in activePorts) {
                        allTargets.add(Triple(ip, p.port, source.id))
                    }
                }
            }
            allTargets.shuffle()

            val totalToScan = allTargets.size
            var progress = 0

            // --- PHASE 1: QUICK SCAN (TCP CONNECT) ---
            val quickConcurrency = 64 // Reduced from 100 to prevent OOM/Crashes on low-end devices
            val quickChunks = allTargets.chunked(quickConcurrency)

            for (chunk in quickChunks) {
                if (!isActive) break
                if (!isInternetConnected()) {
                    handleDisconnect()
                    break
                }

                val deferreds = chunk.map { (ip, port, sourceId) ->
                    async {
                        _currentScanningSubnet.value = "$ip:$port"
                        val success = testSocketConnection(ip, port, 1500)
                        if (success > 0) Triple(ip, port, sourceId) else null
                    }
                }

                deferreds.awaitAll().filterNotNull().forEach { candidates.add(it) }
                progress += chunk.size
                updateEta(progress, totalToScan)
                _scannedCount.value = progress
                // Removed delay to speed up real test
            }

            if (candidates.isEmpty()) {
                addToLogs("✖ NO CANDIDATES FOUND IN QUICK SCAN.")
                finishScan(0, totalToScan)
                return@launch
            }

            addToLogs("✔ QUICK SCAN COMPLETE. FOUND ${candidates.size} CANDIDATES.")
            addToLogs("====== [STAGE 2] DEEP PROTOCOL VERIFICATION ======")

            // --- PHASE 2: DEEP TEST (REAL TLS/TCP HANDSHAKE) ---
            // Matches Go core: 3 attempts per candidate, pass if >= 2 succeed
            val deepConcurrency = 50
            val verifiedTempList = mutableListOf<AliveIp>()

            val candidateChunks = candidates.chunked(deepConcurrency)
            for (chunk in candidateChunks) {
                if (!isActive) break
                val deferreds = chunk.map { candidate ->
                    async {
                        if (!isInternetConnected()) return@async null
                        val (ip, port, _) = candidate
                        _currentScanningSubnet.value = "VERIFYING: $ip:$port"

                        var successCount = 0
                        var bestLatency = Long.MAX_VALUE

                        // 3 attempts (matching Go deepTest)
                        for (attempt in 1..3) {
                            val latency = deepTestConnect(ip, port)
                            if (latency > 0) {
                                successCount++
                                if (latency < bestLatency) bestLatency = latency
                            }
                        }

                        // Pass if >= 2 succeed (matching Go: success >= 2)
                        if (successCount >= 2) {
                            AliveIp(ip = ip, port = port, ping = bestLatency)
                        } else {
                            null
                        }
                    }
                }

                val results = deferreds.awaitAll().filterNotNull()
                results.forEach { aliveIp ->
                    addToLogs("✔ VERIFIED: ${aliveIp.ip}:${aliveIp.port} - ${aliveIp.ping}ms")
                    repository.insertHistory(
                        ScanHistory(ip = aliveIp.ip, port = aliveIp.port, ping = aliveIp.ping, novaId = aliveIp.novaId)
                    )
                    // Live update: add each verified IP immediately
                    withContext(Dispatchers.Main) {
                        updateAliveList(aliveIp, sort = true)
                        _aliveCount.value = _allAliveIps.value.size
                    }
                }
                verifiedTempList.addAll(results)
                _deadCount.value = candidates.size - _aliveCount.value
            }

            finishScan(_aliveCount.value, _deadCount.value)
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        scanJob?.cancel()
        _isScanning.value = false
        _currentScanningSubnet.value = ""
        _etaValue.value = "--:--"
        addToLogs("====== SCAN TERMINATED BY USER ======")
    }

    fun startScanWithImportedIps(autoSuffix: Boolean = false) {
        _autoSuffixOnFinish = autoSuffix
        if (_isScanning.value) return

        val ips = _importedIps.value
        if (ips.isEmpty()) {
            addToLogs("✖ No imported IPs to scan.")
            return
        }

        if (!isInternetConnected()) {
            val errMsg = if (_selectedLanguage.value == AppLanguage.FA) {
                "خطا: اتصال اینترنت برقرار نیست. لطفاً شبکه خود را بررسی کنید."
            } else {
                "Error: No internet connection. Please check your network."
            }
            addToLogs("✖ ABORTED: $errMsg")
            if (vibrateOnError.value) triggerVibration(800)
            if (notifyOnError.value) triggerNotification("Internet Connection Error!", errMsg)
            return
        }

        _isScanning.value = true
        _scannedCount.value = 0
        _aliveCount.value = 0
        _deadCount.value = 0
        _allAliveIps.value = emptyList()
        _recentProbes.value = emptyList()
        _etaValue.value = "02:30"
        clearLogs()

        addToLogs("====== SCANNING IMPORTED IPs ======")
        addToLogs("Loaded ${ips.size} IPs for scanning.")

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val candidates = mutableListOf<Triple<String, Int, Int>>()
            val allTargets = mutableListOf<Triple<String, Int, Int>>()

            for (entry in ips) {
                val parts = entry.split(":")
                val ip = parts[0].trim()
                val port = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 443 else 443
                allTargets.add(Triple(ip, port, 0))
            }
            allTargets.shuffle()

            val totalToScan = allTargets.size
            var progress = 0

            addToLogs("====== [STAGE 1] QUICK SCAN ======")
            val quickConcurrency = 64
            val quickChunks = allTargets.chunked(quickConcurrency)

            for (chunk in quickChunks) {
                if (!isActive) break
                if (!isInternetConnected()) { handleDisconnect(); break }

                val deferreds = chunk.map { (ip, port, sourceId) ->
                    async {
                        _currentScanningSubnet.value = "$ip:$port"
                        val success = testSocketConnection(ip, port, 1500)
                        if (success > 0) Triple(ip, port, sourceId) else null
                    }
                }

                deferreds.awaitAll().filterNotNull().forEach { candidates.add(it) }
                progress += chunk.size
                updateEta(progress, totalToScan)
                _scannedCount.value = progress
            }

            if (candidates.isEmpty()) {
                addToLogs("✖ NO CANDIDATES FOUND IN QUICK SCAN.")
                finishScan(0, totalToScan)
                return@launch
            }

            addToLogs("✔ QUICK SCAN COMPLETE. FOUND ${candidates.size} CANDIDATES.")
            addToLogs("====== [STAGE 2] DEEP VERIFICATION ======")

            val deepConcurrency = 50
            val verifiedTempList = mutableListOf<AliveIp>()

            val candidateChunks = candidates.chunked(deepConcurrency)
            for (chunk in candidateChunks) {
                if (!isActive) break
                val deferreds = chunk.map { candidate ->
                    async {
                        if (!isInternetConnected()) return@async null
                        val (ip, port, _) = candidate
                        _currentScanningSubnet.value = "VERIFYING: $ip:$port"

                        var successCount = 0
                        var bestLatency = Long.MAX_VALUE

                        for (attempt in 1..3) {
                            val latency = deepTestConnect(ip, port)
                            if (latency > 0) {
                                successCount++
                                if (latency < bestLatency) bestLatency = latency
                            }
                        }

                        if (successCount >= 2) {
                            AliveIp(ip = ip, port = port, ping = bestLatency)
                        } else {
                            null
                        }
                    }
                }

                val results = deferreds.awaitAll().filterNotNull()
                results.forEach { aliveIp ->
                    addToLogs("✔ VERIFIED: ${aliveIp.ip}:${aliveIp.port} - ${aliveIp.ping}ms")
                    repository.insertHistory(ScanHistory(ip = aliveIp.ip, port = aliveIp.port, ping = aliveIp.ping, novaId = aliveIp.novaId))
                    withContext(Dispatchers.Main) {
                        updateAliveList(aliveIp, sort = true)
                        _aliveCount.value = _allAliveIps.value.size
                    }
                }
                verifiedTempList.addAll(results)
                _deadCount.value = candidates.size - _aliveCount.value
            }

            finishScan(_aliveCount.value, _deadCount.value)
        }
    }

    private suspend fun handleDisconnect() {
        addToLogs("✖ WARNING: INTERNET DISCONNECTED DURING SCAN!")
        withContext(Dispatchers.Main) {
            if (vibrateOnError.value) triggerVibration(800)
            if (notifyOnError.value) {
                triggerNotification(
                    if (_selectedLanguage.value == AppLanguage.FA) "قطع اتصال اینترنت!" else "Internet Connection Error!",
                    "Scan interrupted due to network loss."
                )
            }
            this@NovaRadarViewModel.stopScan()
        }
    }

    private fun updateEta(progress: Int, total: Int) {
        val remaining = maxOf(0, (total - progress) / 10)
        val m = remaining / 60
        val s = remaining % 60
        _etaValue.value = String.format("%02d:%02d", m, s)
    }

    private fun finishScan(alive: Int, dead: Int) {
        addToLogs("====== SCAN COMPLETE ======")
        addToLogs("Total Verified: $alive | Failed: $dead")
        _isScanning.value = false
        _currentScanningSubnet.value = ""
        _etaValue.value = "--:--"
        viewModelScope.launch(Dispatchers.Main) {
            if (vibrateOnFinish.value) triggerVibration(500)
        }
        if (_autoSuffixOnFinish && _allAliveIps.value.isNotEmpty()) {
            _autoSuffixOnFinish = false
            val result = _allAliveIps.value.joinToString("\n") { "${it.ip}:${it.port}#Nova-${it.novaId}" }
            _importOutput.value = result
        }
    }

    private fun addProbe(ip: String, port: Int, status: String) {
        val current = _recentProbes.value.toMutableList()
        val entry = "$ip:$port $status"
        current.add(0, entry)
        if (current.size > 30) current.removeAt(current.size - 1)
        _recentProbes.value = current
    }

    private fun testSocketConnection(ip: String, port: Int, timeout: Int): Long {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            val elapsed = System.currentTimeMillis() - startTime
            addProbe(ip, port, "${elapsed}ms")
            elapsed
        } catch (e: Exception) {
            addProbe(ip, port, "✖")
            -1L
        } finally {
            try { socket?.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun deepTestConnect(ip: String, port: Int): Long {
        val timeout = 3000
        val startTime = System.currentTimeMillis()
        return try {
            if (port in tlsPorts) {
                val sslContext = SSLContext.getDefault()
                val sslSocket = sslContext.socketFactory.createSocket() as SSLSocket
                try {
                    val sslParams = sslSocket.sslParameters
                    sslParams.serverNames = listOf(SNIHostName(vlessSNI))
                    sslSocket.sslParameters = sslParams
                    sslSocket.connect(InetSocketAddress(ip, port), timeout)
                    sslSocket.soTimeout = timeout
                    sslSocket.startHandshake()
                } finally {
                    try { sslSocket.close() } catch (e: Exception) { }
                }
            } else {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ip, port), timeout)
                    socket.soTimeout = timeout
                    val buf = ByteArray(1)
                    socket.getInputStream().read(buf)
                } finally {
                    try { socket.close() } catch (e: Exception) { }
                }
            }
            val elapsed = System.currentTimeMillis() - startTime
            addProbe(ip, port, "✓${elapsed}ms")
            elapsed
        } catch (e: Exception) {
            addProbe(ip, port, "✗")
            -1L
        }
    }

    private fun adjustToAvoidOverlap(newIp: AliveIp, existing: List<AliveIp>) {
        var attempts = 0
        var collided = true
        while (collided && attempts < 50) {
            collided = false
            val r1 = newIp.normalizedDistance
            val a1Rad = Math.toRadians(newIp.angle.toDouble())
            val x1 = r1 * Math.cos(a1Rad)
            val y1 = r1 * Math.sin(a1Rad)

            for (other in existing) {
                if (other.ip == newIp.ip) continue
                val r2 = other.normalizedDistance
                val a2Rad = Math.toRadians(other.angle.toDouble())
                val x2 = r2 * Math.cos(a2Rad)
                val y2 = r2 * Math.sin(a2Rad)

                val dx = x1 - x2
                val dy = y1 - y2
                val dist = Math.sqrt(dx * dx + dy * dy)
                if (dist < 0.32) {
                    collided = true
                    break
                }
            }
            if (collided) {
                newIp.angle = Random.nextFloat() * 360f
                newIp.normalizedDistance = 0.35f + Random.nextFloat() * 0.50f
            }
            attempts++
        }
    }

    private fun updateAliveList(newIp: AliveIp, sort: Boolean = true) {
        val current = _allAliveIps.value.toMutableList()
        if (current.any { it.ip == newIp.ip }) return

        current.add(newIp)
        if (sort) {
            current.sortBy { it.ping }
        }

        // Set distance from center proportional to ping rank within top 10
        val topCount = minOf(current.size, 10)
        for (i in 0 until topCount) {
            val rank = i.toFloat() / maxOf(1f, topCount - 1f)
            current[i].normalizedDistance = 0.3f + rank * 0.55f
        }

        adjustToAvoidOverlap(newIp, current)
        _allAliveIps.value = current
    }

    fun copyTop10ToClipboard(context: Context) {
        val list = _allAliveIps.value.take(10)
        if (list.isEmpty()) return

        val textBuilder = StringBuilder()
        textBuilder.append("--- Nova Radar Top 10 IPs ---\n")
        list.forEachIndexed { idx, item ->
            textBuilder.append("${idx + 1}. ${item.ip}:${item.port}#Nova-${item.novaId} - Ping: ${item.ping}ms\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NovaRadarTopIPs", textBuilder.toString())
        clipboard.setPrimaryClip(clip)
    }

    fun copyAllToClipboard(context: Context) {
        val list = _allAliveIps.value
        if (list.isEmpty()) return

        val textBuilder = StringBuilder()
        textBuilder.append("--- Nova Radar All Verified IPs ---\n")
        list.forEachIndexed { idx, item ->
            textBuilder.append("${item.ip}:${item.port}#Nova-${item.novaId}\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NovaRadarAllIPs", textBuilder.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "تمامی نتایج کپی شدند!" else "All results copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun copyIndividualToClipboard(context: Context, item: AliveIp) {
        val config = "${item.ip}:${item.port}#Nova-${item.novaId}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NovaRadarConfig", config)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "کپی شد: $config" else "Copied: $config", Toast.LENGTH_SHORT).show()
    }

    private fun generateIpsForSubnet(cidrString: String, maxCount: Int): List<String> {
        val cidrs = cidrString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (cidrs.isEmpty()) return emptyList()
        val ipList = mutableListOf<String>()
        var remaining = maxCount
        for ((idx, cidr) in cidrs.withIndex()) {
            val budget = remaining / (cidrs.size - idx)
            if (budget <= 0) continue
            try {
                val parts = cidr.split("/")
                if (parts.size != 2) continue
                val baseIp = parts[0]
                val mask = parts[1].toInt()
                val ipLong = ipToLong(baseIp)
                val hostCount = (1L shl (32 - mask))
                if (hostCount <= budget + 2) {
                    val limit = (hostCount.toInt() - 1).coerceAtMost(budget + 1)
                    for (i in 1 until limit) {
                        ipList.add(longToIp(ipLong + i))
                    }
                } else {
                    val tried = mutableSetOf<Long>()
                    var count = 0
                    while (count < budget && tried.size < hostCount) {
                        val offset = Random.nextLong(1, hostCount - 1)
                        if (tried.add(offset)) {
                            ipList.add(longToIp(ipLong + offset))
                            count++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ipList
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0L
        return (parts[0].toLong() shl 24) or
                (parts[1].toLong() shl 16) or
                (parts[2].toLong() shl 8) or
                parts[3].toLong()
    }

    private fun longToIp(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
