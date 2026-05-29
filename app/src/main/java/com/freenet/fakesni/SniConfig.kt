package com.freenet.fakesni

data class SniConfig(
    val listenHost: String = "127.0.0.1",
    val listenPort: String = "40443",
    val connectHost: String = "104.19.229.21",
    val connectPort: String = "443",
    val fakeSni: String = "hcaptcha.com",
    val utls: String = "firefox",
    val injector: String = "passive",   // passive = AF_PACKET (works on Android without nfqueue)
    val fakeRepeat: Int = 1,
    val fakeDelay: String = "2ms",
    val ackTimeout: String = "2s",
    val enableFragment: Boolean = false,
    val fragmentDelay: String = "500ms",
    val sniChunk: Int = 3,
    val networkInterface: String = "rmnet_data0",
    val addIpRule: Boolean = false   // binary handles its own fwmark routing via ensureMarkedRoute()
) {
    fun toBinaryArgs(binaryPath: String) = buildString {
        append("'$binaryPath'")
        append(" -listen '$listenHost:$listenPort'")
        append(" -connect '$connectHost:$connectPort'")
        append(" -fake-sni '$fakeSni'")
        append(" -utls '$utls'")
        append(" -injector $injector")
        append(" -fake-repeat $fakeRepeat")
        append(" -fake-delay $fakeDelay")
        append(" -ack-timeout $ackTimeout")
        append(" -enable-fragment=$enableFragment")
        if (enableFragment) {
            append(" -fragment-delay $fragmentDelay")
            append(" -sni-chunk $sniChunk")
        }
    }

    companion object {
        val UTLS_OPTIONS     = listOf("firefox", "chrome", "safari", "ios", "android", "randomized", "none")
        val INJECTOR_OPTIONS = listOf("passive", "active")
    }
}
