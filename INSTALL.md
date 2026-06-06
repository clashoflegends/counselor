# Counselor — Installation Guide

Counselor is the client application for Clash of Legends players. It lets you open your turn results, review what happened on the map, compose your orders, and submit them to the game server (Judge).

---

## Choose your download

Go to the [Releases page](https://github.com/clashoflegends/counselor/releases) and pick the file that fits your situation:

| File | Best for |
|---|---|
| `Counselor-X.X.X.msi` | Windows — standard install with Java included |
| `Counselor-portable-X.X.X.zip` | macOS / Linux / Windows for players that already have Java. Loads faster! |
| `Counselor-windows-portable-X.X.X.zip` | Windows — with Java included and no admin rights (for company laptop) |
| `Counselor-X.X.X.dmg` | macOS — standard install with Java included |
| `Counselor-X.X.X.deb` | Linux — standard install with Java included |

---

## Windows installer (.msi)

**Best for:** most Windows players who want the simplest setup. Gives you a Start menu shortcut, desktop icon, `.egf` file association (double-click a turn file to open it directly), and a normal uninstaller. Takes a few more seconds to load.

1. Download `Counselor-X.X.X.msi`.
2. Double-click to run. If Windows shows **"Windows protected your PC"**, click **More info → Run anyway**.
3. Follow the installer steps. Counselor is installed to `Program Files`.
4. Launch from the Start menu shortcut.

**Config file location:** `%APPDATA%\Counselor\properties.config`
*(paste `%APPDATA%\Counselor\` into Explorer's address bar to open the folder)*

**Log file location:** `%APPDATA%\Counselor\counselor.log`

---

## JAR + launcher ZIP (Counselor-portable-X.X.X.zip)

**Best for:** Windows, macOS, and Linux players who already have Java 21 installed and want the fastest startup time.

**Requires:** Java 21 or later installed and on your PATH. Download from [Adoptium](https://adoptium.net) if needed.

1. Download `Counselor-portable-X.X.X.zip`.
2. Extract all files anywhere.
3. Run:
   - **Windows:** double-click `run.bat`
   - **macOS / Linux:** open a terminal in the folder and run `bash run.sh`

**Config file location:** `dist\properties.config` (next to the JAR, inside the extracted folder)

**Log file location:** `dist\counselor.log`

---

## Windows portable ZIP — no admin rights (Counselor-windows-portable-X.X.X.zip)

**Best for:** players on a company or managed Windows laptop where you cannot install software. Bundled with its own Java runtime — no Java installation needed.

1. Download `Counselor-windows-portable-X.X.X.zip`.
2. Extract all files anywhere you have write access — your Documents folder, a USB drive, etc.
3. Open the extracted `Counselor` folder and double-click `Counselor.exe`.

No installation, no admin rights, no registry changes. Delete the folder to uninstall.

**Config file location:** `Counselor\app\properties.config` (next to `Counselor.exe`)

**Log file location:** `Counselor\app\counselor.log`

---

## macOS installer (.dmg)

**Best for:** Mac players who want a standard application install.

1. Download `Counselor-X.X.X.dmg`.
2. Open the `.dmg` file. Drag **Counselor** to your **Applications** folder.
3. On first launch, macOS may show **"Counselor cannot be opened because it is from an unidentified developer."**
   - Open **System Settings → Privacy & Security**, scroll down, and click **Open Anyway**.
4. Launch from Applications or Spotlight.

**Config file location:** `~/Library/Application Support/Counselor/properties.config`

**Log file location:** `~/Library/Application Support/Counselor/counselor.log`

---

## Linux installer (.deb)

**Best for:** Debian/Ubuntu Linux players who want a standard package install.

1. Download `Counselor-X.X.X.deb`.
2. Install:
   ```
   sudo dpkg -i Counselor-X.X.X.deb
   ```
3. Launch from your applications menu or run `Counselor` from a terminal.

**Config file location:** `~/.counselor/properties.config`

**Log file location:** `~/.counselor/counselor.log`

---

## Trying it out

The `dist/examples/` folder (ZIP downloads) and the installation directory (MSI/DMG/DEB) include sample turn files from real past games. You can open these before you receive your first real turn to explore the interface:

- Double-click an `.rr.egf` file (if you used the MSI installer), or
- Use **File → Open** inside Counselor and browse to the `examples/` folder.

---

## Checking your version

Open Counselor and go to **Help → About**. The **Counselor Version** line (e.g. `2.856`) should match the version number on the [Releases page](https://github.com/clashoflegends/counselor/releases).

---

## Troubleshooting

- **Counselor won't start:** check the log file at the path listed above for your install type.
- **Old turn file won't open:** files from games that ran before 2026 may use an older format. Counselor handles these automatically. If you see an error, send the log file to the game admin.
- **Questions or problems:** email the game admin or post to the [mailing list](http://groups.google.com/group/clashoflegends).
