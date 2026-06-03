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
