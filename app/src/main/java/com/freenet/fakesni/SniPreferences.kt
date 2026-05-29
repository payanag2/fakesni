package com.freenet.fakesni

import android.content.Context

class SniPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sni_config", Context.MODE_PRIVATE)

    fun save(c: SniConfig) = prefs.edit().apply {
        putString("listenHost", c.listenHost)
        putString("listenPort", c.listenPort)
        putString("connectHost", c.connectHost)
        putString("connectPort", c.connectPort)
        putString("fakeSni", c.fakeSni)
        putString("utls", c.utls)
        putString("injector", c.injector)
        putInt("fakeRepeat", c.fakeRepeat)
        putString("fakeDelay", c.fakeDelay)
        putString("ackTimeout", c.ackTimeout)
        putBoolean("enableFragment", c.enableFragment)
        putString("fragmentDelay", c.fragmentDelay)
        putInt("sniChunk", c.sniChunk)
        putString("networkInterface", c.networkInterface)
        putBoolean("addIpRule", c.addIpRule)
    }.apply()

    fun load() = SniConfig(
        listenHost      = prefs.getString("listenHost", "127.0.0.1") ?: "127.0.0.1",
        listenPort      = prefs.getString("listenPort", "40443") ?: "40443",
        connectHost     = prefs.getString("connectHost", "104.19.229.21") ?: "104.19.229.21",
        connectPort     = prefs.getString("connectPort", "443") ?: "443",
        fakeSni         = prefs.getString("fakeSni", "hcaptcha.com") ?: "hcaptcha.com",
        utls            = prefs.getString("utls", "firefox") ?: "firefox",
        injector        = prefs.getString("injector", "passive") ?: "passive",
        fakeRepeat      = prefs.getInt("fakeRepeat", 1),
        fakeDelay       = prefs.getString("fakeDelay", "2ms") ?: "2ms",
        ackTimeout      = prefs.getString("ackTimeout", "2s") ?: "2s",
        enableFragment  = prefs.getBoolean("enableFragment", false),
        fragmentDelay   = prefs.getString("fragmentDelay", "500ms") ?: "500ms",
        sniChunk        = prefs.getInt("sniChunk", 3),
        networkInterface = prefs.getString("networkInterface", "rmnet_data0") ?: "rmnet_data0",
        addIpRule       = prefs.getBoolean("addIpRule", false)
    )
}
