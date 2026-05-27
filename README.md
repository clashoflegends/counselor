## How to build

**Prerequisites**
- JDK 8 (tested with 1.8.0_301 / 1.8.0_461; use the JDK, not just JRE)
- Apache Ant 1.8.0 or higher

**Steps**

1. Clone this repo and `PbmCommons` as siblings:
   ```
   git clone https://github.com/clashoflegends/PbmCommons
   git clone https://github.com/clashoflegends/PbmCounselor
   ```
   Both repos must live in the same parent directory (`nbproject/project.properties` references `../PbmCommons`).

2. Copy the example config:
   ```
   cp properties.config.example properties.config
   ```
   Edit `properties.config` with your SMTP credentials and preferred paths.

3. Build:
   ```
   ant jar
   ```
   Output lands in `dist/PbmCounselor.jar`.

**Note:** `lib/PbmPersistenceCommons.jar` is a prebuilt binary committed to this repo (private source). The build uses it as-is.

---

This game is a computer moderated turn based multiplayer board game. It has strong elements from war games with a nice economical side to it. Also, it has some small pieces of RPG mixed in the bunch.

The general mechanics are quite simple. For each turn you receive a result turn with all available information. Then you spend some time thinking and communicating with your allies (if any). Then you input your commands and send them to the moderator. The moderator (or Judge) will process all commands according to the scenario turn sequencing and then distribute the results. Then we play another turn until a winner is declared or the time limit is reached.

The goal is that you should spend 10-20 minutes per week preparing you commands, after the initial learning curve is done. Some people may want to run the turns more often, but I do not recommend it for beginners.

The idea is to have a client program (the Counselor) to help both new and experienced players and to leverage knowledge. From the Counselor, all information will be made available. You can review your generals and wizards. You can check upon your assassins and your enemies' rogues if they are double agents.

An easy to view map will help you see all theatre of operations in a glance. You review the information and give your commands at your own pace as we are all busy people.

The game is quite stable and recently translated into English. It is far from finished, but we passed the initial beta and I'm looking for more players.

Are you interested in having some fun while crushing your enemies?
