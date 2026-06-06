# What's New in Counselor

## 2026 - Major Update

This is the biggest update to Counselor in many years. Here is what changed for you as a player.

---

### Easier to install

Counselor now comes with five download options to fit every situation. From a standard Windows installer to a portable version that runs from a USB drive with no Java installation required. See the [Installation Guide](INSTALL.md) for details of each option.

- **Windows installer** - includes Java, creates a shortcut, and registers `.egf` files so you can double-click any turn file to open it directly.
- **Portable options** - run Counselor anywhere, even on a corporate laptop with no admin rights.
- **Mac and Linux** - native installers for macOS and Debian/Ubuntu Linux.

---

### Open turn files your way

- **Double-click** any `.rr.egf` file on your desktop or in a folder to open it directly in Counselor (Windows installer).
- **Double-clicking** a `.rc.egf` file automatically opens the corresponding `.rr.egf` results file.
- **Drag and drop** a turn file onto the Counselor window to open it. No need to use File → Open.

---

### Richer city results

The City results tab now shows much more detail at a glance:

- Changes in loyalty, population size, fortifications, and resources are now displayed clearly, so you can track what happened to each city without guessing.
- Encounter messages from events in and around cities now appear in the results.

---

### Resource transport on the map

The map now draws the routes and icons for resources being transported between cities, giving you a clear visual of what is moving and where.

---

### Gameplay fixes

- **Supremacy victory condition** - the victory points graph no longer counts Barbarians and White Walkers as enemies, which was inflating the score incorrectly.
- **Scenario-aware upkeep** - city and character upkeep costs are no longer shown in scenarios that do not use them, such as War of Dwarves and Orcs. The panel now shows only what is relevant to your game.
- **Graphs restored** - the victory condition graphs are back.

---

### Old turn files open correctly

Turn files from games going back 20+ years will open without errors. If you had files that used to crash or show a blank screen, they should work now.

---

### Player names with accents now display correctly

Names with accents, cedillas, and other special characters now show correctly throughout the game website and in results.

---

### Stability improvements

- Fixed several crashes that some players experienced in specific game situations. Thom, you should not see crashes anymore.
- Player portraits now download and display reliably.
- Fixed a layout issue in the main data panel where content was cut off or misaligned.
- Counselor now runs on Java 21, which is faster and more stable on modern systems. The installer bundles Java, so you do not need to install anything separately unless you want to.

---

### Better diagnostics

The log file now includes more detail to help track down problems. If you ever need to report an issue to the game admin, the log will give a clearer picture of what went wrong.

---

### Try it before your first turn

The installation now includes a set of sample turn files from real past games. Open them to explore the map, review results, and get familiar with the interface before your first real turn arrives.

---

### About box improvements

**Help → About** now shows:

- Your exact Counselor version number, useful when reporting issues.
- Clickable links to your configuration file and log folder, so you can find them instantly without digging through system folders.

---

### Website improvements

- Players can now select their country, and a small flag is displayed next to their name throughout the site — in the Hall of Fame, Leaderboard, and other pages.
- Some game statistics are now available on the website.

---

### Security improvements

Orders are now submitted with an additional per-turn security token, making it harder for anyone to tamper with your uploads. This happens automatically. No action required on your part.

---

### Known issues

- The Windows installer may show a SmartScreen warning on first download. Click **More info → Run anyway** to proceed. This warning disappears over time as more players download the release.
