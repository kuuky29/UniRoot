# UniRoot

**One-click root for Samsung Galaxy devices — powered by CVE-2026-43499 / dirty-pipe payloads with a KernelSU late-load.**

Made by **kuuky**.

<p align="center"><b>I am not responsible for bricked phones.</b></p>

---

## What it does

UniRoot packages the full rooting chain into a single app:

- **Exploit pipeline** — CVE-2026-43499 payloads (LD_PRELOAD / app.so) executed either locally through the root helper or as `shell` (UID 2000) via **Shizuku**, depending on the profile.
- **KernelSU injection** — on success the payload stages `ksud` and late-loads the KernelSU module; the manager app is force-stopped and relaunched automatically so it picks up the fresh state.
- **Root My Galaxy integration** — the app talks to the official [Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) support feed on GitHub, matches your model + kernel version and downloads the pinned exploit/ksud artifacts for supported devices (S24, S25 series, Z Fold 7…).
- **Profiles** — bundled, validated profiles (S25 ZZHL/ZZI4, S26 Ultra ZZHK, S93XX, Oppo) plus a full profile editor: swap the exploit `.so`, the KernelSU module (`.ko`), `ksud`, or the CVE helpers per profile. Custom KASLR offset supported.
- **GhostLock-style logs** — colored, timestamped execution log, a dedicated Logs page, and every run is archived as a shareable `.txt` box (Completed / Failed / Crash / Reboot required).
- **Optional: latest KernelSU builds** — off by default. When enabled, the newest official `ksud` + prebuilt `.ko` for your kernel KMI are pulled from the [KernelSU releases](https://github.com/tiann/kernelSU/releases). Not recommended: newer builds can be unstable.

## Supported devices (bundled)

| Device | Kernel | Profile |
|---|---|---|
| Galaxy S25 (SM-S931…) | 6.6.127 | S25 ZZHL / ZZI4 |
| Galaxy S26 Ultra (SM-S948…) | 6.12.69 | S26 Ultra ZZHK (Shizuku, preload v10) |
| Galaxy S25 series | varies | S93XX |
| Oppo | varies | Oppo X9 |
| + everything in the Root My Galaxy feed | — | auto-added as `RMG · …` profiles |

## Build

Requirements: JDK 21, Android SDK 37, NDK 28+ (the root helper `libcve43499root.so` is prebuilt).

```sh
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Install the APK, start **Shizuku** (needed for the S26 Ultra profile).
2. Pick the profile (auto-detected when your device is supported) and hit **Root now**.
3. Watch the Logs page. On success the KernelSU manager is restarted and you are rooted (per boot — re-run after a reboot).

If a run fails without crashing: **reboot, then run once** (the pipe page budget is per-boot).

## Credits

- UI based on [GhostLock](https://github.com/YuKongA/ghostlock-app) (YuKongA)
- Device feed & payloads: [Root My Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy)
- KernelSU: [tiann/kernelSU](https://github.com/tiann/kernelSU)
