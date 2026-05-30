# FakeSNI

**Android frontend for SNI-spoofing DPI bypass — no special setup, just root.**

FakeSNI wraps the [`sni-spoofing`](https://github.com/aleskxyz/SNI-Spoofing) Go binary in a Material You Compose UI. It runs a local TLS proxy that replaces the real SNI in the ClientHello with a decoy hostname, fooling deep-packet-inspection firewalls into letting the connection through. Works on cellular and Wi-Fi, auto-rebinds when the network changes, and cleans up routing rules on stop.

<table width="100%">
  <tr>
    <td width="50%" align="center"><strong>🌐 Main Interface</strong></td>
    <td width="50%" align="center"><strong>⚙️ Advanced Settings</strong></td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/8dbd7302-f8c8-4ec2-97fe-432b6e322421" width="60%" alt="Main View">
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/9b7fd5b6-8067-4fb1-8926-7e555839c40b" width="60%" alt="Settings View">
    </td>
  </tr>
</table>
---

## Requirements

| | |
|---|---|
| Android | 8.0 Oreo (API 26) or later |
| Architecture | arm64-v8a or armeabi-v7a |
| Root | **Required** — the proxy binary runs as root via `su` |

---

## Download

Grab the latest signed APK from [**Releases**](../../releases/latest):

| File | For |
|---|---|
| `*-arm64-v8a-release.apk` | Most modern phones (recommended) |
| `*-armeabi-v7a-release.apk` | Older 32-bit devices |
| `*-universal-release.apk` | Any architecture (larger file) |

---

## How it works

```
Your app  ──►  127.0.0.1:40443 (FakeSNI proxy)
                      │
                      │  TLS with fake SNI (e.g. hcaptcha.com)
                      ▼
              DPI firewall sees allowed hostname ✓
                      │
                      ▼
              Real server (e.g. 104.19.229.21:443)
```

1. FakeSNI starts a foreground service that extracts and runs the Go `sni-spoofing` binary as root.
2. The binary listens locally, intercepts the TLS handshake, injects a spoofed SNI into the ClientHello, and relays traffic to the real server.
3. The uTLS library impersonates a real browser's TLS fingerprint so the handshake looks legitimate.
4. On network changes (SIM swap, Wi-Fi ↔ cellular), the proxy auto-restarts so it stays bound to the correct interface.

---

## Quick start

1. Install the APK and grant root access when prompted.
2. Set **Connect IP** and **Connect Port** to your server's real address.
3. Set **Fake SNI** to any hostname your ISP/firewall allows (e.g. `hcaptcha.com`, `www.google.com`).
4. Tap **Start Spoofing**.
5. Point your app or VPN client at `127.0.0.1:40443`.

Tap **Test** (while stopped) to run the built-in test matrix — it tries all injector/uTLS combinations against your target and logs which ones succeed. Takes ~30 seconds.

---

## Configuration

### Proxy

| Field | Default | Description |
|---|---|---|
| Listen IP | `127.0.0.1` | Local address the proxy binds to |
| Listen Port | `40443` | Local port your client connects to |
| Connect IP | `104.19.229.21` | Real server IP |
| Connect Port | `443` | Real server port |

### SNI Spoofing

| Field | Default | Options | Description |
|---|---|---|---|
| Fake SNI | `hcaptcha.com` | Any hostname | Hostname injected into the TLS ClientHello |
| uTLS fingerprint | `firefox` | `firefox` `chrome` `safari` `ios` `android` `randomized` `none` | Browser TLS fingerprint to impersonate |
| Injector | `passive` | `passive` `active` | How SNI injection is performed (see below) |

### Advanced — Timing

| Field | Default | Description |
|---|---|---|
| Fake repeat | `1` | How many times to repeat the fake SNI packet (1–20) |
| Fake delay | `2ms` | Delay between fake and real packets |
| ACK timeout | `2s` | How long to wait for a TCP ACK before giving up |

### Advanced — Fragmentation

Splits the TLS ClientHello across multiple TCP segments to defeat DPI that reassembles only the first packet.

| Field | Default | Description |
|---|---|---|
| Enable fragmentation | off | Toggle TCP fragmentation |
| Fragment delay | `500ms` | Delay between fragments |
| SNI chunk | `3` | Number of bytes per fragment |

### Advanced — Routing

| Field | Default | Description |
|---|---|---|
| Add IP rule for uid 0 | off | Adds `ip rule add uidrange 0-0 lookup <iface> pref 1500` so root traffic exits the right interface instead of going through a VPN |
| Network interface | `rmnet_data0` | Fallback interface name if auto-detection fails |

> **Note:** The `active` injector manages its own `fwmark` ip rules and `NFQUEUE` iptables rules automatically. FakeSNI cleans them up gracefully on stop.

---

## Injector modes

### `passive` (default)
Uses `AF_PACKET` raw sockets to inject the fake SNI packet. Works on any rooted Android device without kernel NFQUEUE support. Recommended for most users.

### `active`
Uses Linux `NFQUEUE` to intercept and rewrite packets in-kernel. Requires a kernel with `CONFIG_NETFILTER_NETLINK_QUEUE`. Adds iptables rules for the target IP; they are removed on stop.

---

## Use with v2rayNG / VPN clients

Set the v2rayNG outbound (or any SOCKS/HTTP proxy chain) to connect to `127.0.0.1:40443` instead of your server directly. FakeSNI handles the SNI layer; your VPN client handles the protocol above TLS.

If your VPN routes root traffic back through itself and the binary can't reach the real server, enable **Add IP rule for uid 0** in the Routing section to bypass the VPN for the proxy's own connection.

---

## Building from source

```bash
git clone https://github.com/freenetio/FakeSNI.git
cd FakeSNI
./gradlew assembleDebug
# APKs → app/build/outputs/apk/debug/
```

### Release build

Configure signing in `~/.gradle/gradle.properties`:

```properties
FAKESNI_STORE_FILE=/path/to/keystore.jks
FAKESNI_STORE_PASSWORD=...
FAKESNI_KEY_ALIAS=...
FAKESNI_KEY_PASSWORD=...
```

Then:

```bash
./gradlew assembleRelease
```

### CI / releases

- **CI** runs on every push and pull request (lint + build check).
- **Release** is triggered by pushing a `v*` tag — GitHub Actions builds signed per-ABI APKs and publishes them to a GitHub Release automatically.

```bash
git tag v0.6.1
git push origin v0.6.1
```

---

## Acknowledgements

- [**@aleskxyz**](https://github.com/aleskxyz) — [`SNI-Spoofing`](https://github.com/aleskxyz/SNI-Spoofing) Go binary
- [**@patterniha**](https://github.com/patterniha) — contributions to the SNI-spoofing technique

---

## License

This project is provided as-is for research and educational purposes. Use responsibly and in accordance with local laws.
