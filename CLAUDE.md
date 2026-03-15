# SpawnHunt

A Fabric client-side mod for Minecraft Java Edition 1.21.11.
Speedrun-style scavenger hunt: find and collect a random survival-obtainable block as fast as possible.

## Testing Environment

- **Minecraft instance (macOS):** `/Applications/MultiMC.app/Data/instances/SpawnHunt/.minecraft`
- **Minecraft instance (Windows):** `C:\MultiMC\instances\SpawnHunt\.minecraft`
- Built `.jar` goes into the `mods/` folder of that instance
- Requires Fabric Loader + Fabric API for MC 1.21.11

## Project Structure

```
com.spawnhunt
├── SpawnHuntMod.java              // Entrypoint (ClientModInitializer)
├── data/
│   ├── ItemPool.java              // Survival-obtainable item registry & random selection
│   ├── HuntState.java            // Active hunt state (target, timer, win flag)
│   └── ResultStore.java          // Persisted run results (last/top times per item)
├── screen/
│   ├── SpawnHuntScreen.java       // Item selection GUI (Cancel / List / Reroll / Start)
│   └── ItemChooserScreen.java     // Searchable item list picker (Back / Select)
├── hud/
│   └── HuntHudRenderer.java      // In-game HUD (timer + target icon/name, green border/name on win)
├── event/
│   ├── InventoryListener.java     // Detects target block entering inventory
│   └── WorldLifecycleHandler.java // Resets hunt state on world exit
└── mixin/
    ├── TitleScreenMixin.java      // Injects "SpawnHunt" button into main menu
    └── CreateWorldScreenMixin.java // Auto-configures and triggers world creation
```

## Tech Stack

- **Build:** Gradle 9.2.1 + Fabric Loom 1.15.4
- **Java:** JDK 21 (Eclipse Adoptium 21.0.10+7) at `C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`
- **Dependencies:** fabric-loader 0.18.4, fabric-api 0.141.3+1.21.11, Yarn 1.21.11+build.4
- **Language:** Java

## Build Commands

```bash
# Build (must use JDK 21)
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew build

# Output jar: build/libs/spawnhunt-<version>.jar
```

## Development Phases & Progress

### Phase 1 — Scaffolding & Project Setup
- [x] 1.1 Initialize Fabric mod project (template, fabric.mod.json, Gradle, mappings)
- [x] 1.2 Create `SpawnHuntMod` entrypoint (ClientModInitializer, register events, init state)
- [x] 1.3 Set up Mixin configuration (spawnhunt.mixins.json, wire into fabric.mod.json)

### Phase 2 — Block Pool
- [x] 2.1 Implement `BlockPool` class (iterate registry, filter by item, apply exclusions, cache)
- [x] 2.2 Add random selection method
- [x] 2.3 Test & validate pool (log contents at startup, manual review)

### Phase 3 — Main Menu Integration
- [x] 3.1 Create `TitleScreenMixin` (@Inject into TitleScreen.init(), add "SpawnHunt" button)
- [x] 3.2 Position button (below existing menu buttons, avoid overlap)
- [x] 3.3 Wire button to open `SpawnHuntScreen`

### Phase 4 — Block Selection Screen
- [x] 4.1 Create `SpawnHuntScreen` (extends Screen, layout with block icon + name + buttons)
- [x] 4.2 Render target block (ItemRenderer at 4x scale, translated block name)
- [x] 4.3 Implement Reroll button (unlimited)
- [x] 4.4 Implement Cancel button (return to title screen)
- [x] 4.5 Implement Start button (set HuntState, trigger world creation)

### Phase 5 — World Creation
- [x] 5.1 Programmatic world creation (Survival, normal difficulty, cheats off, random seed)
- [x] 5.2 World defaults (name: "SpawnHunt-<timestamp>")
- [x] 5.3 Start timer on world load

### Phase 6 — In-Game HUD
- [x] 6.1 Create `HuntHudRenderer` (HudRenderCallback)
- [x] 6.2 Render timer (mm:ss.000 format, semi-transparent background)
- [x] 6.3 Render target block (top-left, 16x16 icon + name + timer, bordered box)
- [x] 6.4 Pause-aware timer logic (delta-accumulation, no drift)

### Phase 7 — Win Detection
- [x] 7.1 Create `InventoryListener` (scan player inventory each tick for target block item)
- [x] 7.2 Trigger win (set won flag, freeze finalTimeMs)
- [x] 7.3 Play victory sound (UI_TOAST_CHALLENGE_COMPLETE)
- [x] 7.4 Win indicated by green HUD border + item name (VictoryOverlay removed)

### Phase 8 — Lifecycle & Cleanup
- [x] 8.1 Reset state on world exit (WorldLifecycleHandler on DISCONNECT)
- [x] 8.2 Edge cases (death pauses timer, cheats off, no creative access)
- [x] 8.3 Handle disconnect/crash (no persistence needed for MVP)

### Phase 9 — Polish & Testing
- [x] 9.1 Visual polish (bordered HUD boxes, no overlap with vanilla HUD)
- [x] 9.2 Screen polish (bobbing block animation on selection screen)
- [x] 9.3 Test block pool (spot-check 20+ random rolls)
- [x] 9.4 Playtesting (easy + hard targets, timer accuracy)
- [x] 9.5 Build & distribute (final .jar, clean install test)

## Key Design Decisions

- **State is not persisted** — exiting the world ends the hunt. Crash = hunt over.
- **Item pool** uses tag-and-filter: start with all registry items, filter survival-obtainables, exclude unobtainables via a static exclusion set.
- **Win state** — no separate overlay; HUD border and item name turn green, victory sound plays.
- **Timer** uses delta-accumulation (not start-time subtraction) to avoid drift across pause/unpause.
- **Inventory scanning** is trivially fast (36 slots + armor + offhand per tick).

## Key Risks

- **Programmatic world creation** has no clean public API — may need deep mixins or auto-click approach as fallback.
- **Item pool accuracy** — maintain exclusion list carefully, log pool on startup, iterate via community feedback.

## Versioning

Uses [SemVer](https://semver.org/). Version is set in `gradle.properties` (`mod_version`).

| Version | Date       | Notes                                    |
|---------|------------|------------------------------------------|
| 1.3.0 | 2026-03-15 | HUD redesign, vertical menu layout, music disc naming, item pool fixes |
| 1.2.0   | 2026-03-12 | Music disc song names, display name caching |
| 1.1.0   | 2026-02-26 | UI polish, win state rework, world naming   |
| 1.0.0   | 2026-02-26 | Initial public release on Modrinth          |

## Conventions

- Keep mixin surface area minimal to reduce breakage on MC updates.
- Pin to MC 1.21.11, use only stable Fabric API modules.
