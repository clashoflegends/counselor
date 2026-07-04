# What's New in Counselor

## 04-JUL-2026 - v2.1.898

### Smoother turn loading
- Fixed a crash that could stop certain turn results from opening.
- If a turn file was made by a **newer Counselor than yours**, the message now says exactly that and points you to the update, instead of a vague "wrong version" notice.

## 03-JUL-2026 - v2.1.897

> **Please update.** This version is required to open turn files produced by the current server. If you're on an older Counselor and a new turn won't open, updating fixes it.

### See your characters at a glance
- The **characters table** now has a **Gender** column, so you can tell your commanders, agents and emissaries apart more easily.

### Safer duels & assassinations
- Orders like **duel** and **assassination** now **hide your allies** by default, so you can't accidentally target a friend. Need to betray one? Tick **ALL** to reveal every possible target.
- The **ALL** checkbox now **clears itself after each order you save**, so it can't quietly stay on and catch you out on the next action. (If you'd rather it stay ticked, set `MaintainOrdersAllChecked=1` in your config.)

### Copy anything
- **Copy & paste now works everywhere** - right-click any text (reports, order details, dialogs) for a Copy/Cut/Paste menu, or just use Ctrl+C.

### Website (clashlegends.com)
- **Auctions:** sending a character to *find out the bids* when **nobody has bid** now completes normally - your character still trains and the usual cost applies - and reports that the field was empty. Empty is useful intel, not a wasted order.
- Improvements to **wild dragon** behaviour.

## 29-JUN-2026 - v2.1.893

### Counselor can update itself
- **One-click updates that actually install.** When a newer version is available, clicking the update notice now does the right thing for your build: the portable (JAR) and macOS builds **install the update for you** and apply it the next time you start Counselor; the Linux and Windows-portable builds download it and open the folder; the Windows installer opens the download page. Updates **never restart Counselor on their own**, so an update can't interrupt orders you haven't sent, and the previous version is kept until the new one is verified - a failed update rolls back instead of breaking your install.

### Know whether your orders are in
- **New "orders sent" indicator** in the status bar (bottom-right). At a glance it tells you whether the orders open in Counselor match what the server already received (green), whether you have changes you haven't sent yet (amber), or that it can't tell right now (grey) - and reminds you if you still need to set your upload token (red). Hover for details.

### Smaller touches
- The **About box** now shows which build you're running (installer, portable, macOS, etc.).
- Fixed a stray empty `counselor.log` file being left next to your turn file when opening it by double-click.

### Character portraits that keep themselves current
- **Portraits now update on their own.** Counselor notices when a newer portrait pack is available and offers it with a single click - or fetches it quietly in the background if you switch on the new **Auto-download portrait updates** option in **Settings**. Fresh character art (including the latest Fire & Dragons cast) now arrives without you hunting for a download.

### Fire & Dragons
- **Army banners for the Blacks and the Greens** now appear on the map, so the rival Targaryen factions are easy to tell apart at a glance.

## 27-JUN-2026 - v2.1.891

### See the whole map at a glance
- **New Map Legend.** A **Legend** button on the map toolbar (next to Display Portraits) opens a handy key to every map symbol - terrain types, settlement sizes (camp to city), fortifications (tower to citadel), capitals, ports and docks, roads, rivers, bridges, fords and landings, plus armies, fleets, characters, magic items, gold mines, combat and movement paths. It floats beside the map so you can keep it open while you plan.

### One-click updates
- **Counselor can fetch its own update.** When a newer version is available, the update notice is now clickable - Counselor downloads the right installer for your system and opens the folder so you can run it.

### Your personal upload token
- **Order uploads now carry your personal token**, so the game master knows they came from you. The first time you submit, Counselor sets it up - fetch it with your website login, or paste it from the website - and you can view or change it anytime under **Settings** or on the website's **My Counselor Token** page.
- **Please upgrade:** from **July 1** you will need this version (or later) to upload orders. Older Counselors will be turned away.

### Website (clashlegends.com)
- **My Counselor Token page.** View, copy or regenerate your upload token on the website (linked from **Links**) - handy if you ever need to reset it.

## 24-JUN-2026 - v2.1.886

### Smoother map
- **No more jumping around when you click through armies.** Selecting an army now gently centres the map on its hex (only when it is off-screen) instead of flashing to the corner and back.
- **The map remembers your overlays.** Your **Scouts** and **Army-path** toggle choices now persist between sessions (and the Scouts button no longer just mirrors the Fog button).
- **Keyboard zoom in clean 10% steps.** **Ctrl/Cmd +** and **-** zoom the map in fixed 10% increments, **Ctrl/Cmd 0** resets to 100%, and the wheel still zooms smoothly. On macOS this is **Cmd** (and it no longer clashes with the system screen-zoom gesture).

### Sharper in dark mode
- More crisp, theme-aware icons: the order-tab buttons (OK / clear / repeat / detach / help), the pop-out detach buttons, and the Battle Simulator.

### Website (clashlegends.com)
- **The site now speaks your language - for everyone.** Portuguese, Spanish, Italian and Catalan are out of beta and live for all players; pick yours in **My Config** and it applies right away.
- **Quick links on the home page.** The Links (Counselor downloads, guides, change logs, Request a Game, My Connections) now also sit in a table at the bottom of the home page.

### In the forge - coming soon
- **GOT: Fire & Dragons** - a brand-new Game of Thrones scenario is taking shape: the age of the Targaryens, dragonfire over Westeros, and a world rebuilt for those bold enough to ride. We are crafting it now - watch the New Games list for the call to arms. The age of fire is coming.

## 22-JUN-2026 - v2.1.884

### New nation power: Relocate Capital
- A new **Startup Package** you can buy at the very start of a game: **move your capital** to one of your own large cities (a burg or metropolis). Pick the destination city; the move happens before the turn runs and is announced to the other nations. (Game of Thrones scenario.)

### Map and navigation
- **Zoom with the keyboard:** **Ctrl +** and **Ctrl -** now zoom the map (alongside Ctrl+wheel), handy on laptops and trackpads.
- **Double-click to jump to the map:** double-click any row in a data table (a character, city, army...) and the map centres on that hex.
- Clicking hexes **while zoomed** now lands on the hex you actually clicked (fixed an edge-of-hex off-by-one at fractional zoom).

### Your tables, your layout
- Counselor now **remembers your sort order and column widths** for each table between turns, and **reopens on the tab you were last using** instead of always jumping back to Characters.

### Website (clashlegends.com)
- **New My Connections page:** a personal map of the players you have shared games with - **green** lines for people you have mostly teamed with, **red** for opponents, thicker lines for more games together. Find it on the **Links** page.

## 20-JUN-2026 - v2.1.882

### Much faster startup
- Counselor now **opens your game about 3x faster**. We removed a slow image-loading step at launch, so you reach your turn in a few seconds instead of waiting.

### Zoom out to see the whole map
- The map can now **zoom out below 100%** (hold **Ctrl** and scroll down), so you can fit a whole large map on screen instead of only zooming in. **Ctrl+0** snaps back to **100%**.
- Counselor now **remembers your zoom for each screen**, so docking a laptop, switching monitors, or changing resolution keeps the right zoom on every display.

### Cleaner toolbar and map
- The **toolbar icons** are now crisp and adapt to light and dark mode.
- The **distance bubbles** on the map (range, march, sail) are sharp at any zoom and repaint faster.

### Copy and export from any table
- Right-click **Copy** and **Export to CSV** are now available on **every table** in Counselor, including the World Builder and the Battle Simulator.

### Easier updates
- When a newer version is available, the **About box** now shows a **download link** so you can grab it in one click.

### Website (clashlegends.com)
- **The site now speaks your language (Beta):** Portuguese, Spanish, Italian and Catalan across the menus and player pages, including the Statistics charts. Turn it on with the new **Update channel** setting in **My Config** (choose Beta), and pick your **language** there too. On the Beta channel your email notifications arrive translated as well.
- **Clearer team set-up:** the **Sign Up** page now walks teams through joining in four quick steps (everyone signs up, one player names the team, invites teammates, and they Accept right on the page), and **Request a Game** explains how teaming works.

## 18-JUN-2026 - v2.1.881

### Zoom fixes & polish
- Fixed the **right-click action menu while zoomed in**: the bubbles now scale and sit correctly over the surrounding hexes at any zoom, their labels are bolder and easier to read, and they clear properly when you zoom again.
- The **selected-hex outline** now stays **sharp** when zoomed instead of looking pixelated.

### Website (clashlegends.com)
- **Set your preferred language** in **My Config** (English, Portuguese, Spanish, Italian, Catalan), shown with a flag - and Counselor sets it for you automatically on your next upload.
- **Game-request notifications:** opt in to an email when a moderator **approves** or **cancels** your game request (or it auto-expires). Off by default; toggle it on **My Config** or **Request a Game**.

## 17-JUN-2026 - v2.1.880

### Zoom the map
- The map can now be **zoomed**: hold **Ctrl** and scroll the mouse wheel to zoom in and out, and **Ctrl+0** snaps back to the default. A small badge shows the current zoom level while you adjust it.
- On **large / high-resolution screens** the map now starts at a comfortable size automatically (and remembers your choice) instead of looking tiny and hard to read.

### Update notifier fixed
- Counselor again **reliably checks for new versions** and lets you know when one is available.

### Website (clashlegends.com)
- **Manage your game requests:** the **Request a Game** page now lists **your pending requests** with **Edit** and **Cancel** buttons, so you can tweak or withdraw a request before a moderator approves it.

## 16-JUN-2026 - v2.1.879

### A fresh new look
- Counselor got a **modern visual makeover** - cleaner, flatter, and easier on the eyes, while everything stays exactly where you expect it.

### Pick your theme - including Dark mode
- New **Theme** picker in **Settings**: choose **Modern (light)**, **Modern (dark)**, your **System default**, or the classic **Cross-platform** look. **Dark mode** is now fully supported end to end - the info bar, character tables, map, and action counter all adapt so everything stays readable. (Pick a theme, then restart Counselor to apply it.)

### Title bar that matches Windows
- Counselor's **window title bar** now follows your **Windows light/dark setting**, just like your other apps - no more lone light title bar when the rest of your desktop is dark.

### Website (clashlegends.com)
- **See when you sent your orders:** your **My Games** cards now show the **date and time of your last order upload** (in GMT), right next to the status light.
- **Friendlier emails:** deadline reminders, order receipts, and invites got a warmer, clearer rewrite.

## 15-JUN-2026 - v2.1.878

### Less interrupting, more informative
- Confirmations no longer pop up a window you have to click away. Messages like **orders submitted**, **a new version is available**, and **your actions are ready to send** now slide up as a brief notification at the **bottom-left** and fade on their own - nothing blocks what you're doing. (Errors still stop you, as they should.)

### Reopen a recent turn faster
- New **Recent** button beside Open Results: drop down your **recently opened turn files** and reopen one in a click - no more digging through folders every turn.

### Feedback while a turn loads
- Opening a big turn file used to look frozen for a second or two. Now an **"Opening…" overlay** shows while it loads, so you can see it's working.

### Smoother and more reliable
- The character-portrait download is more robust - it no longer hangs if the server is slow or unreachable.
- Counselor's log now **rotates daily**: if the GM ever needs your log to track down an issue, today's is a single, easy-to-find file.

### Website (clashlegends.com)
- **Play again in one click:** finished games now have a **Rematch** button on your My Games page.
- **Final standings** for every finished game - ranking by victory points, winner flagged.
- **Get notified when a game opens:** opt in (My Config) to email alerts when a public game opens, hits 50% full, or needs just one more player.
- **Team up with invitations** - no more shared team passwords; invite teammates from the player list and they accept with one click.
- **Deadline reminders** are now opt-out per player (My Config), and deadlines and the My Games countdown are clearly shown in **GMT**.
- The order-punctuality chart is cleaner - paused games no longer skew it.

---

## 12-JUN-2026 - v2.1.877

### Make the map easier to read
- You can now **choose your own colors** for the Barbarian and Unknown nations on the map. Their default white and grey can be hard to tell apart - especially for colorblind players - so set whatever stands out for you. It's under **Settings** (Barbarian Color / Unknown Color), and the map updates right away.

### Updating is smoother
- Installing a new version now reliably replaces the old one, instead of sometimes leaving you on the previous build.
- **Mac:** there's now a dedicated build for Intel Macs, plus a double-click launcher for the portable version (no Terminal needed).
- The installation guide now spells out what each download needs - and the right Java to grab for your Mac.

### Little things
- The window title always shows which turn file you have open, even when you open it by double-clicking the file.

### Website (clashlegends.com)
- The Statistics page now shows the **share of players by nationality** across the community.

---

## 10-JUN-2026 - v2.1.1

### Export any list in one click
- Right-click any list (characters, armies, cities, orders, and the rest) to **copy it to the clipboard with column headers** - paste straight into a spreadsheet or email.
- Or choose **Export to CSV...** to save the list as a file you can open in Excel. Counselor suggests a filename (based on your game) and lets you pick the folder. Accented names come through correctly, in any language.

### See how hard each order is
- Saved orders now have a **Difficulty** column, color-coded by level: red = hard, yellow = average, green = easy or automatic, blue = varies. A quick way for newer players to learn which orders are reliable.
- Don't want the colors? Turn them off under Settings.

### Smoother on Windows
- Fixed a Windows display glitch where panels could smear, go blank, or appear doubled after opening and closing a dialog. The screen now redraws cleanly.

---

## 09-JUN-2026 - v2.0.8

### Faster, smoother submitting
- Submitting your orders no longer freezes the window. A spinner shows while it uploads (it can take a few seconds), and the app stays responsive.
- New option to save orders without the file dialog: turn on "Save orders without asking" in Settings and Counselor writes the orders file with the standard name, right next to your results file.

### Stay up to date
- Counselor now checks for a newer release on startup and tells you in the status bar and title bar when one is available.
- The window title now shows the game file you have open, so it's easy to tell windows apart.

### Nicer to look at and to run
- The splash screen is back on every download type (installer and portable), so you get feedback during the (sometimes slow) first load.
- The hex icon now appears on every window, including Hex View, About, and the floating order/help windows (no more generic Java cup).

### More reliable
- Opening a damaged or incompatible turn file now shows a clear message instead of crashing, and the details are saved to the log so the GM can help.
- A crash when opening certain files (with the "Owned" default filter) is fixed.

### Better language support
- All on-screen text was spell-checked and proofread.
- Portuguese, Spanish, and Italian translations are now complete (many previously-untranslated screens are now in your language).
- Catalan is now available. Pick it in Settings under Language.

### Website (clashlegends.com)
- Request your own game: supporters and seasoned players can now request a new game from a form: pick a base game, adjust the settings, and submit. The GM team is notified and reviews it. You can even choose your own password!
- Multi-nation games: games can now let a player run more than one nation, with a sign-up field for how many you intend to play.
- More statistics: new player and community charts, plus a refreshed stats area.
- Behind the scenes: faster, safer order uploads and better crash diagnostics so issues get spotted and fixed quicker. Nothing you need to do.

---

## 07-JUN-2026 - Major Update

This is the biggest update to Counselor in many years. Here is what changed for you as a player.

---

### Richer city results

The City results tab now shows much more detail at a glance:

- Changes in loyalty, population size, fortifications, and resources are now displayed clearly, so you can track what happened to each city without guessing.
- Encounter messages from events in and around cities now appear in the results.

### Resource transport on the map

The map now draws the routes and icons for resources being transported between cities, giving you a clear visual of what is moving and where.

### Gameplay fixes

- **Supremacy victory condition** - the victory points graph no longer counts Barbarians and White Walkers as enemies, which was inflating the score incorrectly.
- **Scenario-aware upkeep** - city and character upkeep costs are no longer shown in scenarios that do not use them, such as War of Dwarves and Orcs. The panel now shows only what is relevant to your game.
- **Graphs restored** - the victory condition graphs are back.

### Easier to install

Counselor now comes with five download options to fit every situation. Options range from a standard Windows installer to a portable version that runs from a USB drive with no Java installation required. See the [Installation Guide](INSTALL.md) for details of each option.

- **Windows installer** - includes Java, creates a shortcut, and registers `.egf` files so you can double-click any turn file to open it directly.
- **Portable options** - run Counselor anywhere, even on a corporate laptop with no admin rights.
- **Mac and Linux** - native installers for macOS and Debian/Ubuntu Linux.

### Open turn files your way

- **Double-click** any `.rr.egf` file on your desktop or in a folder to open it directly in Counselor (Windows installer).
- **Double-clicking** a `.rc.egf` file automatically opens the corresponding `.rr.egf` results file.
- **Drag and drop** a turn file onto the Counselor window to open it. No need to use File → Open.

### Old turn files open correctly

Turn files from games going back 20+ years will open without errors. If you had files that used to crash or show a blank screen, they should work now.

### Player names with accents now display correctly

Names with accents, cedillas, and other special characters now show correctly throughout the game website and in results.

### Stability improvements

- Fixed several crashes that some players experienced in specific game situations. Thom, you should not see crashes anymore.
- Player portraits now download and display reliably.
- Fixed a layout issue in the main data panel where content was cut off or misaligned.
- Counselor now runs on Java 21, which is faster and more stable on modern systems. The installer bundles Java, so you do not need to install anything separately unless you want to.

### Better diagnostics

The log file now includes more detail to help track down problems. If you ever need to report an issue to the game admin, the log will give a clearer picture of what went wrong.

### Try it before your first turn

The installation now includes a set of sample turn files from real past games. Open them to explore the map, review results, and get familiar with the interface before your first real turn arrives.

### About box improvements

**Help → About** now shows:

- Your exact Counselor version number, useful when reporting issues.
- Clickable links to your configuration file and log folder, so you can find them instantly without digging through system folders.

### Website improvements

- Players can now select their country, and a small flag is displayed next to their name throughout the site, in the Hall of Fame, Leaderboard, and other pages.
- Some game statistics are now available on the website.
- A new **Links** page collects useful links in one place: Counselor downloads, the install guide, the changelog, and more.

### Security improvements

Orders are now submitted with an additional per-turn security token, making it harder for anyone to tamper with your uploads. This happens automatically. No action required on your part.

### Known issues

- The Windows installer may show a SmartScreen warning on first download. Click **More info → Run anyway** to proceed. This warning disappears over time as more players download the release.
