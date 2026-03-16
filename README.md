# SpawnHunt

**SpawnHunt** is a Fabric mod that turns Minecraft into a speedrun scavenger hunt. You're given a random survival-obtainable item — collect it as fast as you can. Play solo in a fresh world or compete with friends on a server.

## How It Works

### Singleplayer

1. Click **SpawnHunt** on the title screen
2. You're shown a random target item — **Reroll** for a new one, or pick from the full **List**
3. Hit **Start** and a new Survival world is created automatically
4. Find and collect the target item as fast as possible
5. Your time is tracked, and your best runs are saved per item

### Multiplayer

1. Install SpawnHunt on your Fabric server (players don't need the mod installed)
2. An OP runs `/spawnhunt start random` or `/spawnhunt start <item>`
3. All players race to find the target item first
4. Players with the mod see a rich HUD; players without see action bar messages
5. The first player to collect the item wins

## Features

- **400+ target items** — every survival-obtainable item in the game
- **Singleplayer** — built-in timer with pause awareness, run history, hardcore mode
- **Multiplayer** — server commands, automatic inventory scanning, works with vanilla clients
- **Zero setup** — singleplayer worlds created with a single click; multiplayer via one command

## Requirements

- Minecraft Java Edition 1.21.11
- [Fabric Loader](https://fabricmc.net/) 0.18.4+
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.141.3+

## Installation

### Client (singleplayer + enhanced multiplayer HUD)
1. Install Fabric Loader and Fabric API for Minecraft 1.21.11
2. Drop the `spawnhunt` jar into your `mods` folder
3. Launch the game — the **SpawnHunt** button appears on the title screen

### Server (multiplayer)
1. Install Fabric Loader and Fabric API on your server
2. Drop the `spawnhunt` jar into the server's `mods` folder
3. Start the server — `/spawnhunt` commands are available to OPs

Players **do not** need the mod installed to participate. They'll see the hunt via action bar messages and chat. Players with the mod get a richer HUD experience.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/spawnhunt start random` | OP | Start a hunt with a random item |
| `/spawnhunt start <item>` | OP | Start a hunt with a specific item (tab-completable) |
| `/spawnhunt stop` | OP | Cancel the current hunt |
| `/spawnhunt restart [item]` | OP | Stop and start a new hunt |
| `/spawnhunt status` | Everyone | Show current hunt info |

## Community & Support

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-5865F2?logo=discord&logoColor=white)](https://discord.gg/nU4Bv64)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/richardthornton)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Support-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/richardthornton)

## License

[MIT](LICENSE)
