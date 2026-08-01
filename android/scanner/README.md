<div align="center">
  <img src="logo.svg" alt="Nova Radar" width="80" />
  <h1>Nova Radar</h1>
  <p><strong>Cloudflare IP Scanner — Desktop Application</strong></p>
  <p>
    <a href="README.fa.md">فارسی</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Go-1.25-blue?logo=go" />
    <img src="https://img.shields.io/badge/React-19-61dafb?logo=react" />
    <img src="https://img.shields.io/badge/Wails-v3--alpha-333?logo=wails" />
    <img src="https://img.shields.io/badge/platform-Windows-blue?logo=windows" />
  </p>
</div>

---

## Overview

Nova Radar is a **desktop IP scanner** built with [Wails v3](https://wails.io) (Go backend + React/TypeScript frontend). It scans Cloudflare IP ranges from multiple sources, performs real protocol verification (TCP + TLS handshake), and outputs working IPs sorted by latency.

> Built by [**Nova Proxy Group**](https://github.com/IRNova)

## Features

- **Multi-source IP scanning** — 9 selectable IP sources (Cloudflare official, AS13335, AS209242, AS24429, AS199524, CM list, reverse proxies, domains)
- **Two-phase verification** — Quick TCP scan → Deep TLS handshake test (3 attempts, pass ≥2)
- **Latency-based sorting** — Results sorted by response time (fastest first)
- **Real-time progress** — Live IP display, progress bar, ETA, scanned/alive/dead counts
- **Port selection** — 12 ports (443, 2053, 2083, 2087, 2096, 8443, 80, 2052, 2082, 2086, 2095, 8080)
- **Export results** — Copy to clipboard or save to `.txt` file
- **Dark-only theme** — Sleek dark UI with animated radar visualization
- **Frameless window** — Custom title bar with system tray

## How It Works

1. **Source Fetching** — On scan start, enabled IP sources are fetched from GitHub in parallel
2. **IP Generation** — Random IPs are generated from CIDR ranges (up to 512 per scan)
3. **Quick Scan (Phase 1)** — TCP connect to each IP:port with random offset (800 concurrent workers)
4. **Deep Test (Phase 2)** — TLS handshake verification for TLS ports, TCP read for HTTP ports (3 attempts, pass ≥2)
5. **Result Sorting** — Working IPs are sorted by latency, fastest first

## IP Sources

| Source | Type | Description |
|--------|------|-------------|
| Cloudflare Official | CIDR | `cloudflare.com/ips-v4` |
| CM List | CIDR | Curated CF CIDR list |
| AS13335 | CIDR | Cloudflare ASN |
| AS209242 | CIDR | Cloudflare ASN |
| AS24429 | CIDR | Alibaba ASN |
| AS199524 | CIDR | G-Core ASN |
| Reverse Proxy IPs | Proxy IP | Public reverse proxy IPs |
| Foreign Domains | Domain | Resolved foreign domains |
| Iranian Domains | Domain | Resolved Iranian domains |

---

<div align="center">
  <h3>Support the Project</h3>
  <p>Support us with a donation</p>
  <p><a href="https://daramet.com/NovaPr" target="_blank">🔗 https://daramet.com/NovaPr</a></p>
  <hr>
  <h4>Wallets</h4>
  <p><strong>BTC:</strong></p>
  <pre><code>bc1qc54su3gz20ulq8df7k0pcskk4zz4sy0e7z7hws</code></pre>
  <p><strong>TON:</strong></p>
  <pre><code>UQD51lGC35rP_SbVYgbFA7CEEii4GVMFgqj4N8fiGi6m425w</code></pre>
</div>

---

## Stargazers over time

[![Stargazers over time](https://starchart.cc/IRNova/NovaRadar.svg?variant=adaptive)](https://starchart.cc/IRNova/NovaRadar)

## License

This project is for educational purposes only.

---

<div align="center">
  <a href="https://github.com/IRNova">GitHub</a> •
  <a href="https://t.me/irnova_proxy">Telegram Channel</a> •
  <a href="https://t.me/irnovaproxy">Author</a>
</div>
