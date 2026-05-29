package com.freenet.fakesni

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = SniPreferences(app)

    private val _config = kotlinx.coroutines.flow.MutableStateFlow(prefs.load())
    val config: StateFlow<SniConfig> = _config

    val isRunning: StateFlow<Boolean> = SniService.isRunning
    val logLines: StateFlow<List<String>> = SniService.logLines

    fun update(config: SniConfig) {
        _config.value = config
        if (!isRunning.value) prefs.save(config)
    }

    fun start() {
        prefs.save(_config.value)
        val ctx = getApplication<Application>()
        ctx.startForegroundService(
            Intent(ctx, SniService::class.java).apply { action = SniService.ACTION_START }
        )
    }

    fun stop() {
        val ctx = getApplication<Application>()
        ctx.startService(
            Intent(ctx, SniService::class.java).apply { action = SniService.ACTION_STOP }
        )
    }

    fun test() {
        prefs.save(_config.value)
        val ctx = getApplication<Application>()
        ctx.startForegroundService(
            Intent(ctx, SniService::class.java).apply { action = SniService.ACTION_TEST }
        )
    }
}
