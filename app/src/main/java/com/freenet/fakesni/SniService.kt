package com.freenet.fakesni

import android.app.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.Calendar

class SniService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        const val ACTION_TEST  = "ACTION_TEST"
        const val CHANNEL_ID   = "sni_channel"
        const val NOTIF_ID     = 1
        const val BINARY_NAME       = "sni-spoofing"       // on-device filename (always)
        const val ASSET_ARM64       = "sni-spoofing-arm64"
        const val ASSET_ARM7        = "sni-spoofing-arm7"

        val isRunning = MutableStateFlow(false)
        val logLines  = MutableStateFlow<List<String>>(emptyList())
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    private var currentConfig: SniConfig? = null

    // Guard: prevents double-cleanup when onDestroy() fires after ACTION_STOP already cleaned up.
    @Volatile private var cleanupDone = false

    // Incremented on every start/stop. runProxy/runTest check this in their finally block
    // so a stale coroutine from a previous session cannot tear down a freshly started proxy.
    @Volatile private var sessionToken = 0

    // Auto-rebind on connectivity changes (SIM/APN swap, Wi-Fi <-> cellular). When the
    // default network's interface or addresses change, the binary's old route/socket to
    // the connect IP goes stale ("can't reach request address"); we relaunch it so it
    // re-detects the new route. See registerNetworkMonitor().
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var currentLinkSig: String? = null
    private var restartJob: Job? = null


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (isRunning.value) return START_NOT_STICKY
                cleanupDone = false
                val token = ++sessionToken
                val config = SniPreferences(applicationContext).load()
                currentConfig = config
                startForeground(NOTIF_ID, buildNotification(config))
                clearLog()
                registerNetworkMonitor()
                scope.launch { runProxy(config, token) }
            }
            ACTION_STOP -> {
                ++sessionToken  // Invalidate any in-flight runProxy/runTest coroutine
                performCleanup()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TEST -> {
                if (isRunning.value) return START_NOT_STICKY
                cleanupDone = false
                val token = ++sessionToken
                val config = SniPreferences(applicationContext).load()
                currentConfig = config
                startForeground(NOTIF_ID, buildNotification(config))
                clearLog()
                scope.launch { runTest(config, token) }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runProxy(config: SniConfig, token: Int) {
        val filesDir   = applicationContext.filesDir
        val binaryFile = File(filesDir, BINARY_NAME)
        val scriptFile = File(filesDir, "launch.sh")

        try {
            // Pick asset that matches device ABI.
            val assetName = resolveAssetName()
            log("[*] ABI: ${Build.SUPPORTED_ABIS.firstOrNull()}  →  $assetName")

            // Verify the asset actually exists using open() — works for both
            // compressed and uncompressed entries (openFd() fails on compressed).
            val assetExists = try { assets.open(assetName).close(); true } catch (_: Exception) { false }
            if (!assetExists) {
                log("[!] Asset '$assetName' not found in APK — add it to app/src/main/assets/")
                throw IllegalStateException("Binary asset missing: $assetName")
            }

            // Get size via openFd (works when noCompress is set; falls back to 0 if still compressed).
            val assetSize = try { assets.openFd(assetName).use { it.length } } catch (_: Exception) { 0L }

            // Re-extract if missing, empty, or APK has a different-sized binary (new release).
            if (!binaryFile.exists() || binaryFile.length() == 0L || (assetSize > 0L && binaryFile.length() != assetSize)) {
                log("[*] Installing binary (${if (assetSize > 0) "${assetSize / 1024} KB" else "size unknown"})...")
                assets.open(assetName).use { src ->
                    binaryFile.outputStream().use { dst -> src.copyTo(dst) }
                }
            }

            // Write a shell script so we get proper auto-detection, logging,
            // and clean error handling rather than cramming everything into
            // a single su -c argument.
            scriptFile.writeText(buildLaunchScript(config, binaryFile.absolutePath))
            scriptFile.setExecutable(true)

            log("[*] Listen  : ${config.listenHost}:${config.listenPort}")
            log("[*] Connect : ${config.connectHost}:${config.connectPort}")
            log("[*] Fake SNI: ${config.fakeSni}  uTLS: ${config.utls}")

            process = Runtime.getRuntime().exec(
                arrayOf("su", "-c", "sh '${scriptFile.absolutePath}'")
            )
            isRunning.value = true

            // The Go binary writes ALL its normal logging to stderr (Go's log package
            // default), so stderr is NOT an error channel here — treat both streams as
            // plain output. Prefixing stderr with "[err]" made the whole log render red.
            val outJob = scope.launch {
                try { process!!.inputStream.bufferedReader().forEachLine { log(it) } } catch (_: Exception) {}
            }
            val errJob = scope.launch {
                try { process!!.errorStream.bufferedReader().forEachLine { log(it) } } catch (_: Exception) {}
            }

            val exitCode = runCatching { withContext(Dispatchers.IO) { process!!.waitFor() } }.getOrDefault(-1)
            outJob.join()
            errJob.join()

            if (isRunning.value) log("[!] Process exited (code $exitCode)")
        } catch (e: Exception) {
            log("[!] ${e.message ?: "Unknown error"}")
        } finally {
            // Only tear down if we are still the active session. If a new start came in while
            // this coroutine was winding down, sessionToken will have been incremented and we
            // must not set isRunning=false or call stopSelf() — that would kill the new session.
            if (sessionToken == token) {
                isRunning.value = false
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private suspend fun runTest(config: SniConfig, token: Int) {
        val binaryFile = File(applicationContext.filesDir, BINARY_NAME)
        try {
            val assetName = resolveAssetName()
            val assetSize = try { assets.openFd(assetName).use { it.length } } catch (_: Exception) { 0L }
            if (!binaryFile.exists() || binaryFile.length() == 0L || (assetSize > 0L && binaryFile.length() != assetSize)) {
                log("[*] Installing binary...")
                assets.open(assetName).use { src -> binaryFile.outputStream().use { dst -> src.copyTo(dst) } }
            }

            val cmd = "chmod +x '${binaryFile.absolutePath}'" +
                "; '${binaryFile.absolutePath}'" +
                " -test" +
                " -connect '${config.connectHost}:${config.connectPort}'" +
                " -fake-sni '${config.fakeSni}'"

            log("[*] Running built-in test matrix...")
            log("[*] Target: ${config.connectHost}:${config.connectPort}  SNI: ${config.fakeSni}")
            log("[*] This tests all injector/uTLS combinations — takes ~30s")

            process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            isRunning.value = true

            val outJob = scope.launch {
                try { process!!.inputStream.bufferedReader().forEachLine { log(it) } } catch (_: Exception) {}
            }
            val errJob = scope.launch {
                try { process!!.errorStream.bufferedReader().forEachLine { log(it) } } catch (_: Exception) {}
            }

            val exitCode = runCatching { withContext(Dispatchers.IO) { process!!.waitFor() } }.getOrDefault(-1)
            outJob.join()
            errJob.join()
            log("[*] Test finished (exit $exitCode)")
        } catch (e: Exception) {
            log("[!] ${e.message ?: "Unknown error"}")
        } finally {
            if (sessionToken == token) {
                isRunning.value = false
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun resolveAssetName(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.contains("arm64-v8a") -> ASSET_ARM64
            abis.contains("armeabi-v7a") -> ASSET_ARM7
            else -> ASSET_ARM64
        }
    }

    private fun buildLaunchScript(config: SniConfig, binaryPath: String): String {
        val connectIp  = config.connectHost
        val fallbackIf = config.networkInterface
        val binaryArgs = config.toBinaryArgs(binaryPath)

        return """
#!/system/bin/sh

chmod +x '$binaryPath'

# ── Teardown previous session ────────────────────────────────────────────────
# SIGTERM first so the binary can self-clean its iptables/ip-rule state, then WAIT
# (real 0.1s polls, up to ~4s) for it to actually exit before resorting to SIGKILL.
# A no-delay spin used to hard-kill it mid-cleanup and leak rules across cycles.
pkill -TERM -f '$BINARY_NAME' 2>/dev/null; true
i=0; while pgrep -f '$BINARY_NAME' >/dev/null 2>&1 && [ ${'$'}i -lt 40 ]; do sleep 0.1; i=${'$'}((i+1)); done
if pgrep -f '$BINARY_NAME' >/dev/null 2>&1; then
    kill -9 ${'$'}(pgrep -f '$BINARY_NAME' 2>/dev/null) 2>/dev/null; pkill -9 -f '$BINARY_NAME' 2>/dev/null
    i=0; while pgrep -f '$BINARY_NAME' >/dev/null 2>&1 && [ ${'$'}i -lt 20 ]; do sleep 0.05; i=${'$'}((i+1)); done
fi
true

${if (config.addIpRule) """
# ── Routing setup ───────────────────────────────────────────────────────────
# Remove stale pref-1500 rules from any previous session
while ip rule del pref 1500 2>/dev/null; do true; done

# Auto-detect the interface that currently reaches the target IP.
# This handles VPN setups where root traffic needs a specific interface.
IFACE=$(ip route get $connectIp 2>/dev/null | grep -o 'dev [^ ]*' | awk '{print ${'$'}2}')

# If that failed, try the default route interface
if [ -z "${'$'}IFACE" ]; then
    IFACE=$(ip route show default 2>/dev/null | grep -o 'dev [^ ]*' | awk '{print ${'$'}2}' | head -1)
fi

# Final fallback: user-configured interface
if [ -z "${'$'}IFACE" ]; then
    IFACE='$fallbackIf'
    echo "[*] Interface auto-detect failed, using configured: ${'$'}IFACE"
else
    echo "[*] Auto-detected interface: ${'$'}IFACE"
fi

ip rule add uidrange 0-0 lookup "${'$'}IFACE" pref 1500 2>/dev/null \
    && echo "[*] IP rule added: uid 0 → ${'$'}IFACE (pref 1500)" \
    || echo "[!] IP rule add failed (interface '${'$'}IFACE' may not have a routing table)"

# Show active rules so user can verify in the log
echo "[*] Active ip rules:"
ip rule show | grep -E 'pref (0|1500|32766|32767)' | while read line; do echo "    ${'$'}line"; done
""" else ""}

# ── Launch ──────────────────────────────────────────────────────────────────
echo "[*] Exec: $binaryArgs"
exec $binaryArgs
""".trimIndent()
    }

    // ── Connectivity-change auto-rebind ──────────────────────────────────────────
    // Registered once when the proxy starts; unregistered in performCleanup(). A
    // SIM/APN swap or Wi-Fi<->cellular switch changes the default network's interface
    // and/or IP, which strands the binary on a dead route. We detect the change via a
    // link "signature" (interface name + sorted addresses) and relaunch the binary so
    // it re-detects the route to the connect IP.
    private fun registerNetworkMonitor() {
        if (networkCallback != null) return
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
                val sig = (lp.interfaceName ?: "?") + "|" +
                    lp.linkAddresses
                        .mapNotNull { it.address.hostAddress }
                        .sorted()
                        .joinToString(",")
                val prev = currentLinkSig
                currentLinkSig = sig
                // First callback after registering just establishes the baseline
                // (this is the network the proxy launched on) — don't restart for it.
                if (prev == null || sig == prev) return
                triggerRebind("$prev → $sig")
            }
        }
        networkCallback = cb
        try { cm.registerDefaultNetworkCallback(cb) } catch (_: Exception) { networkCallback = null }
    }

    private fun unregisterNetworkMonitor() {
        restartJob?.cancel(); restartJob = null
        val cb = networkCallback ?: return
        networkCallback = null
        currentLinkSig = null
        try { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
    }

    private fun triggerRebind(reason: String) {
        if (!isRunning.value) return
        // Debounce: connectivity changes arrive as a burst; coalesce into one restart
        // and give the new network a moment to settle (DHCP / route install) first.
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(2000)
            if (!isRunning.value) return@launch
            val config = currentConfig ?: return@launch
            log("[*] Network changed ($reason) — restarting proxy")
            // Invalidate the current runProxy so its teardown won't stop the service,
            // then drop the old process; the relaunch's own teardown kills any leftover
            // binary (with a grace period) before exec.
            val token = ++sessionToken
            process?.destroy()
            runProxy(config, token)
        }
    }

    override fun onDestroy() {
        performCleanup()
        scope.cancel()
        super.onDestroy()
    }

    private fun performCleanup() {
        if (cleanupDone) return
        cleanupDone = true

        unregisterNetworkMonitor()
        isRunning.value = false
        process?.destroy()
        process = null

        val config = currentConfig ?: return

        // SIGTERM, then WAIT (with a real delay) for the binary to exit on its own.
        // The binary catches SIGINT/SIGTERM and removes its own iptables NFQUEUE +
        // fwmark rules before exiting — but that cleanup shells out to iptables/ip
        // (tens to >100ms). The old loop spun pgrep with no delay and reached kill -9
        // within a few hundred ms, occasionally hard-killing the binary mid-cleanup
        // and leaking a rule; after a couple of start/stop cycles the leftovers were
        // enough to break routing. Poll every 0.1s for up to ~4s and only kill -9 if
        // it is genuinely stuck, so graceful self-cleanup almost always completes.
        val killCmd = buildString {
            append("pkill -TERM -f '$BINARY_NAME' 2>/dev/null; true")
            append("; i=0; while pgrep -f '$BINARY_NAME' >/dev/null 2>&1 && [ \$i -lt 40 ]; do sleep 0.1; i=\$((i+1)); done")
            append("; if pgrep -f '$BINARY_NAME' >/dev/null 2>&1; then")
            append(" kill -9 \$(pgrep -f '$BINARY_NAME' 2>/dev/null) 2>/dev/null; pkill -9 -f '$BINARY_NAME' 2>/dev/null;")
            append(" i=0; while pgrep -f '$BINARY_NAME' >/dev/null 2>&1 && [ \$i -lt 20 ]; do sleep 0.05; i=\$((i+1)); done;")
            append(" fi; true")
            if (config.addIpRule) {
                append("; while ip rule del pref 1500 2>/dev/null; do true; done; true")
            }
        }
        try { Runtime.getRuntime().exec(arrayOf("su", "-c", killCmd)).waitFor() } catch (_: Exception) {}

        // Backstop for the binary's OWN fwmark routing rule, scoped to the exact
        // pref band the binary uses: 12000–12499 (see ensureMarkedRoute in
        // SNI-Spoofing-Go — pref = 12000 + (queueNum % 500); active/nfqueue mode
        // only, passive AF_PACKET mode adds no rules). The binary handles both
        // SIGINT and SIGTERM and removes this rule itself on graceful shutdown;
        // this only matters if it was hard-killed (SIGKILL) before cleanup ran.
        //
        // CRITICAL: we must ONLY delete rules inside 12000–12499. The previous
        // "pref < 10000 is safe" assumption was wrong and is what broke v2rayNG:
        // Android's netd installs its core VPN / network-selection fwmark rules at
        // LOW prefs (SECURE_VPN≈90, PROHIBIT_NON_VPN≈200, network rules 300–2000,
        // VPN fallthrough≈2000, etc.) — all below 10000. Deleting those silently
        // tears down all VPN routing and nothing re-creates them until the next
        // reboot, which is exactly why stopping the app killed v2rayNG until
        // restart. We never flush whole chains or routing tables, since those are
        // shared with Android's netd rules and interface routing.
        val routingCmd = buildString {
            append("ip rule show 2>/dev/null | grep fwmark | awk -F: '{print \$1}' | while read p; do")
            append(" [ \"\$p\" -ge 12000 ] 2>/dev/null && [ \"\$p\" -le 12499 ] 2>/dev/null")
            append(" && ip rule del pref \"\$p\" 2>/dev/null;")
            append(" done; true")
        }
        try { Runtime.getRuntime().exec(arrayOf("su", "-c", routingCmd)).waitFor() } catch (_: Exception) {}

        // Backstop for leaked NFQUEUE iptables rules (active/nfqueue injector only;
        // passive AF_PACKET mode adds none, so this is a no-op there). setupIptables
        // inserts two rules into the filter OUTPUT/INPUT chains for the connect IP:
        //   -I OUTPUT -p tcp -d <ip> --dport <port> -m mark ! --mark <m> -j NFQUEUE --queue-num N
        //   -I INPUT  -p tcp -s <ip> --sport <port>                      -j NFQUEUE --queue-num N
        // They carry NO --queue-bypass, so if the binary is hard-killed before its
        // own cleanupIptables runs, the rules survive with no process bound to the
        // queue and the kernel DROPS every packet to/from the connect IP — which
        // silently kills the v2rayNG tunnel that routes through the proxy, until a
        // reboot. We delete only NFQUEUE rules that reference this connect IP, so
        // Android's own netd rules (which never NFQUEUE the user's target) are safe.
        // Reconstruct each delete from iptables-save: turn "-A <chain> ..." into
        // "iptables -D <chain> ...".
        val ip = config.connectHost.replace("'", "")
        val iptablesCmd = buildString {
            append("iptables-save 2>/dev/null | grep -- '-j NFQUEUE' | grep -F '$ip' | while read -r l; do")
            append(" set -- \$l; chain=\$2; shift 2;")
            append(" iptables -D \"\$chain\" \"\$@\" 2>/dev/null;")
            append(" done; true")
        }
        try { Runtime.getRuntime().exec(arrayOf("su", "-c", iptablesCmd)).waitFor() } catch (_: Exception) {}

        log("[*] Cleanup complete")
    }

    private fun log(line: String) {
        val cal = Calendar.getInstance()
        val ts  = "%02d:%02d:%02d".format(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
        logLines.update { lines -> (lines + "$ts  $line").takeLast(500) }
    }

    private fun clearLog() { logLines.value = emptyList() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "SNI Proxy", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Proxy running status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(config: SniConfig): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, SniService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FakeSNI Active")
            .setContentText("${config.listenHost}:${config.listenPort}  →  ${config.fakeSni}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
