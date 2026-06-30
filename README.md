## Clash of Legends — Counselor

The Counselor is the player client for *Clash of Legends*, a computer-moderated turn-based multiplayer strategy game. It loads your result file (`.egf`), displays the map and all game information, and lets you prepare and submit your orders for the next turn.

---

## Download and run (players)

Pre-built packages are available on the [Releases](../../releases/latest) page:

| Platform | File | Notes |
|---|---|---|
| Windows | `Counselor-x.y.z.msi` | Standard installer, requires admin rights |
| Windows | `Counselor-windows-portable.zip` | No install, no admin rights — extract and run |
| Windows | `Counselor-portable.zip` | JAR + launchers, requires Java 21 on PATH |
| macOS | `Counselor-x.y.z.dmg` | Standard installer |
| Linux | `counselor_x.y.z_amd64.deb` | Standard package |

The `.msi`, `.dmg`, and `.deb` installers bundle a Java 21 runtime — no separate JDK required. The Windows portable ZIP also bundles the runtime. The `Counselor-portable.zip` (JAR only) requires Java 21 already installed.

**Corporate/restricted laptops:** use `Counselor-windows-portable.zip` — extract anywhere, run `Counselor\Counselor.exe`, no admin rights needed.

**First run:** copy `properties.config.example` to `properties.config` and fill in your `counselorToken` (provided by the game admin when you join).

---

## Updating

Counselor checks for a newer release at startup and shows a clickable notification when one is available. Clicking it does the right thing for your build. **Updates always take effect the next time you start Counselor — Counselor never relaunches itself, so updating can never interrupt or discard unsaved orders.**

| Build | What clicking the update notice does |
|---|---|
| `Counselor-portable.zip` (JAR) | **Auto-installs:** downloads the new build and swaps it in on the next launch. |
| macOS `.dmg` | **Auto-installs:** downloads and replaces the app; active on the next launch. (If macOS security blocks replacing an unsigned app, it falls back to opening the download for you to install manually.) |
| Linux `.deb` | Downloads the package and opens its folder — install it with your package manager. |
| `Counselor-windows-portable.zip` | Downloads the ZIP and opens its folder — extract it over your existing copy. |
| Windows `.msi` | Opens the [Releases](../../releases/latest) page to download the installer manually. |

The download itself always happens only when you click the notice — nothing is fetched or installed silently. The portable-JAR and `.dmg` auto-install keeps the previous build until the new one is verified in place, so a failed update rolls back rather than leaving a broken install.

The `.msi` and `windows-portable` builds don't auto-install yet: until the binaries are code-signed, auto-downloading and launching an unsigned Windows installer trips SmartScreen. They'll join the auto-install path once code signing is in place.

---

## Build from source

**Prerequisites**
- JDK 21 (tested with Eclipse Temurin 21.0.x; use the JDK, not just JRE)
- Apache Ant 1.9 or higher **or** Apache Maven 3.8+

**Clone**
```
git clone https://github.com/clashoflegends/PbmCommons
git clone https://github.com/clashoflegends/counselor
```
Both repos must be siblings in the same parent directory.

**Build with Ant**
```
cd counselor
ant jar \
  -Dreference.PbmCommons.jar=lib/PbmCommons.jar \
  -Dreference.PbmPersistenceCommons.jar=lib/PbmPersistenceCommons.jar \
  -Dplatforms.JDK_21_Temurin.home=$JAVA_HOME
```
Output: `dist/PbmCounselor.jar`

**Build with Maven**
```
cd counselor
mvn package
```

**Note:** `lib/PbmPersistenceCommons.jar` is a prebuilt binary committed to this repo (private source). Both build systems use it as-is.

---

## Run from the build output

**Windows**
```
run.bat [path\to\game.egf]
```

**macOS / Linux**
```
./run.sh [path/to/game.egf]
```

These scripts pass the required `--add-opens` flags for XStream under Java 21.

---

## About the game

Clash of Legends is a play-by-mail strategy game with strong wargame and economic elements, and a small RPG component. Each turn you receive a result file with all available information, spend some time planning, then submit your orders. The Judge processes all orders and distributes the next result. One turn per week is typical.

The Counselor shows you the full map, your generals, wizards, and rogues, enemy movements, city status, and everything else you need to make your decisions — then uploads your orders directly to the server.

---

## License

Counselor is released under the [MIT License](LICENSE). © 2014-2026 Clash of Legends.

---

## Code signing policy

Free code signing provided by [SignPath.io](https://about.signpath.io), certificate by [SignPath Foundation](https://signpath.org).

**Team roles:**

- **Committers and reviewers:** [@clashGM01](https://github.com/clashGM01) and approved contributors (each pull request from a non-committer is reviewed before merge).
- **Approvers:** [@clashGM01](https://github.com/clashGM01) — sole signing approver.

**Privacy policy:** This program does not transfer any information to other networked systems unless specifically requested by the user. Network interactions are limited to: loading turn-result files from local disk, submitting orders to the configured game server (`clashlegends.com`), and downloading player portraits from the same server when the user opts in. No telemetry, no analytics, no automatic updates beyond what the user explicitly invokes.
