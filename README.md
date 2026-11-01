# UniRoot

**One-tap root for Samsung Galaxy devices — New exploit (DirtyFrag CVE-2026-43284, unprivileged) + Old exploit profiles, with auto-root at every boot.**

Made by **kuuky**.

<p align="center"><b>I am not responsible for bricked phones.</b></p>

---

## What's new in V5

- **New exploit (fast)** — the unprivileged DirtyFrag engine (CVE-2026-43284): IpSecManager allocates the SA, the native engine corrupts the kernel page cache, ksud is late-loaded from memory. No profile, no Shizuku, no setup — press **Root now**.
- **ksud updater, in the app** — a full-width button (New exploit section) downloads the **latest official ksud** (KernelSU Next *or* classic, per the selected flavor) and patches it **on device** with the bundled **Samsung KDP+DEFEX .kos**, then the result is used immediately (Next → auto-selected profile, classic → `ksud-classic-latest`). Already updated? → *"you are already updated"*.
- **ksud profiles** — profile bands (same style as the old exploit profiles) for both flavors: bundled + downloaded profiles, selectable, and deletable from **Advanced settings**.
- **Auto-root at boot** — a foreground boot service waits for the first unlock and re-runs the chain (up to 6 attempts), with a partial WakeLock so the phone can't sleep mid-exploit.
- **Progress UI** — animated progress bar with live engine stages (IpSec SA → SPI → ksud staged → arming → **Phone rooted ✓**), and a bottom progress popup for the ksud updater. Fully in English.
- **Root state banner** — clean Rooted / Not rooted banner driven by a live `su -c id` probe (no more false "rooted" after a reboot).

## KernelSU Next update compatibility

This build includes Samsung KDP/DEFEX/RKP modules from GhostSam v0.2.4,
based on KernelSU Next **3.4.2 / 33355 / UAPI 5**. Their pinned source commits
and SHA-256 hashes are recorded in
[`SOURCE.json`](app/src/main/assets/df/samsung-ko/SOURCE.json).
The updater accepts a Next release only when it matches these modules, and
compares saved profile bytes so an older module bundle under the same release
name is replaced. Use the matching 3.4.2 manager. After updating the profile,
reboot and re-root to replace a kernel module already loaded in memory.

## Old exploit (slow)

The classic profile pipeline stays untouched: CVE-2026-43499 payloads (local or Shizuku), Root My Galaxy integration, per-device validated profiles (S25 ZZHL/ZZI4, S26 Ultra ZZHK, S93XX, Oppo), profile editor, instant root, and run-log archives.

## Build

```sh
./gradlew assembleRelease   # -> app/build/outputs/apk/release/
```

For a debuggable test build, run `./gradlew testDebugUnitTest assembleDebug`.
Set `KSUD_TEST_FIXTURE` to the SHA-256-verified official ksud v3.4.2 binary to
also run the real-binary regression test. The **Debug APK** GitHub Actions
workflow runs all nine tests with that fixture and uploads a downloadable APK
for each successful run. Open its run summary for the download and SHA-256.
Artifacts require GitHub sign-in and are retained for 90 days. CI uses a
temporary debug signing key, so its APK cannot update an installation signed
with another key.

Review build for [PR #35](https://github.com/kuuky29/UniRoot/pull/35):
[download the Debug APK](https://github.com/shedowe19/UniRoot/actions/runs/41330874318/artifacts/13093696209) from the
[successful test/build run](https://github.com/shedowe19/UniRoot/actions/runs/41330874318)
at commit `20dafbac040c865d70c6f837afd703fef0f12e49`.
APK SHA-256: `971c7b6bb5a15640e077b77eeaceee6c47fdb43267e2641320e45c18e005e9ed`.

## Credits

This app stands on the shoulders of:

- **diabl0w** — [DFRoot](https://github.com/diabl0w/DFRoot): the DirtyFrag engine this whole project is built around
- **snothin** — [GhostSam](https://github.com/snothin/GhostSam): the Samsung-patched kos, the ksud patching approach and the staged native API
- **polygraphene** — the DFRoot fork lineage and the DEFEX bypass work
- **tiann** — [KernelSU](https://github.com/tiann/KernelSU)
- **rifsxd & the KernelSU-Next team** — [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next)
- **YuKongA** — [GhostLock](https://github.com/YuKongA/GhostLock): the UI this app's interface is based on

Thank you all 🙏
