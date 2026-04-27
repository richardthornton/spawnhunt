# SpawnHunt

A Fabric mod for Minecraft Java Edition 26.1.
Speedrun-style scavenger hunt: find and collect a random survival-obtainable block as fast as possible.
Supports both singleplayer (client-side) and multiplayer (server-side commands + HUD sync).

## Testing Environment

- **Minecraft instance (macOS):** `/Applications/MultiMC.app/Data/instances/SpawnHunt 26.1/.minecraft`
- **Minecraft instance (Windows):** `C:\MultiMC\instances\SpawnHunt 26.1\.minecraft`
- Built `.jar` goes into the `mods/` folder of that instance
- Requires Fabric Loader + Fabric API for MC 26.1

## Project Structure

```
com.spawnhunt
├── SpawnHuntMod.java              // Client entrypoint (ClientModInitializer)
├── SpawnHuntCommon.java           // Common entrypoint (ModInitializer) — packets, commands, server tick
├── data/
│   ├── ItemPool.java              // Survival-obtainable item registry & random selection
│   ├── HuntState.java            // Singleplayer hunt state (target, timer, win flag)
│   ├── ServerHuntState.java      // Server-authoritative multiplayer hunt state
│   └── ResultStore.java          // Persisted run results (last/top times per item)
├── command/
│   └── SpawnHuntCommand.java      // /spawnhunt command tree (Brigadier)
├── network/
│   ├── SpawnHuntPayloads.java     // Payload ID constants + registration
│   ├── HuntSyncS2CPayload.java   // Periodic state sync (server -> client)
│   ├── HuntWinS2CPayload.java    // Win announcement (server -> client)
│   └── ClientHuntState.java      // Client-side mirror of server state
├── screen/
│   ├── SpawnHuntScreen.java       // Item selection GUI (Cancel / List / Reroll / Start)
│   └── ItemChooserScreen.java     // Searchable item list picker (Back / Select)
├── hud/
│   └── HuntHudRenderer.java      // In-game HUD (dual source: SP HuntState / MP ClientHuntState)
├── event/
│   ├── InventoryListener.java     // Detects target block entering inventory (singleplayer)
│   ├── WorldLifecycleHandler.java // Resets hunt state on world exit
│   └── ServerHuntManager.java     // Server tick: inventory scan, timer broadcast, win detection
└── mixin/
    ├── TitleScreenMixin.java      // Injects "SpawnHunt" button into main menu
    ├── CreateWorldScreenMixin.java // Auto-configures and triggers world creation
    └── GameModeLockMixin.java     // Prevents game mode changes during active SP hunts
```

## Tech Stack

- **Build:** Gradle 9.4.0 + Fabric Loom 1.16.1 (`net.fabricmc.fabric-loom` — no-remap for unobfuscated MC)
- **Java:** JDK 25 (Eclipse Adoptium 25.0.2+10) at `C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot`
- **Dependencies:** fabric-loader 0.18.4, fabric-api 0.144.4+26.1
- **Mappings:** None (MC 26.1 is unobfuscated — uses Mojang official names directly)
- **Language:** Java

## Build Commands

```bash
# Build (must use JDK 25)
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot" ./gradlew build

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
- [x] 6.1 Create `HuntHudRenderer` (HudElementRegistry)
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

### Phase M — Multiplayer Support
- [x] M1 Common infrastructure (SpawnHuntCommon entrypoint, payload registration, fabric.mod.json)
- [x] M2 Server state + commands (ServerHuntState, /spawnhunt command tree)
- [x] M3 Server tick handler (inventory scan, mod client sync, vanilla action bar, win detection)
- [x] M4 Client integration (ClientHuntState, packet receivers, dual-source HUD)
- [x] M5 Polish (seconds-only timer for MP, win timer fix, action bar cleanup)

## Key Design Decisions

- **State is not persisted** — exiting the world ends the hunt. Crash = hunt over.
- **Item pool** uses tag-and-filter: start with all registry items, filter survival-obtainables, exclude unobtainables via a static exclusion set.
- **Win state** — no separate overlay; HUD border and item name turn green, victory sound plays.
- **Singleplayer timer** uses delta-accumulation (not start-time subtraction) to avoid drift across pause/unpause.
- **Multiplayer timer** uses server wall-clock (no pause concept); displayed in mm:ss format (no milliseconds).
- **Inventory scanning** is trivially fast (36 slots + armor + offhand per tick).
- **Two separate state paths** — singleplayer uses `HuntState` (client static), multiplayer uses `ServerHuntState` (server) synced to `ClientHuntState` (client mirror). No shared mutable state.
- **Vanilla client support** — players without the mod see action bar messages during active hunts and chat messages for start/stop/win events.
- **Multiplayer commands** require OP level 2 (GAMEMASTERS); `/spawnhunt status` is available to all players.
- **Pre-world item rendering** — MC 26.1 binds item components (including `ITEM_MODEL`) during world load, but the selection screen runs pre-world. `ItemPool.ensureComponentsBound()` binds a minimal `DataComponentMap` with `ITEM_MODEL` set to the item's registry ID so `ItemStack` creation and rendering works on the title screen. Vanilla overwrites with full data-driven components during world load.
- **Display names** use `Component.translatable(item.getDescriptionId())` instead of `ItemStack.getHoverName()` to avoid creating ItemStacks for name resolution (safe pre-world).

## Key Risks

- **Programmatic world creation** has no clean public API — may need deep mixins or auto-click approach as fallback.
- **Item pool accuracy** — maintain exclusion list carefully, log pool on startup, iterate via community feedback.
- **Pre-world component binding** — `ensureComponentsBound()` binds minimal components before vanilla does. If vanilla changes the binding lifecycle or adds validation, this could break. Monitor across MC updates.

## Versioning

Uses [SemVer](https://semver.org/). Version is set in `gradle.properties` (`mod_version`).

| Version | Date       | Notes                                    |
|---------|------------|------------------------------------------|
| 3.0.0 | 2026-04-16 | Port to MC 26.1: Java 25, Mojang mappings, HudElementRegistry, unobfuscated build |
| 2.2.1 | 2026-03-23 |  |
| 2.2.0 | 2026-03-22 | Slot machine rolling animation |
| 2.1.0 | 2026-03-17 | Lock to survival during active singleplayer hunts |
| 2.0.0 | 2026-03-16 | Multiplayer support: server commands, HUD sync, vanilla client action bar |
| 1.3.0 | 2026-03-15 | HUD redesign, vertical menu layout, music disc naming, item pool fixes |
| 1.2.0   | 2026-03-12 | Music disc song names, display name caching |
| 1.1.0   | 2026-02-26 | UI polish, win state rework, world naming   |
| 1.0.0   | 2026-02-26 | Initial public release on Modrinth          |

## API Notes (MC 26.1)

- MC 26.1 is **unobfuscated** — uses Mojang official names, no Yarn/intermediary mappings
- `GuiGraphicsExtractor` replaces old `DrawContext`/`GuiGraphics` — methods: `item()`, `text()`, `centeredText()`, `fill()`
- Screen render method is `extractRenderState()`, list entry render is `extractContent()`
- `HudRenderCallback` removed — use `HudElementRegistry.addLast(Identifier, HudElement)` instead
- `HudElement` interface: `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`
- Matrix stack from `pose()` is JOML `Matrix3x2fStack` — uses `pushMatrix()`/`popMatrix()` (not pushPose/popPose)
- `CustomPayload` → `CustomPacketPayload`, `Id<>` → `Type<>`, `getId()` → `type()`
- `readString`/`writeString` → `readUtf`/`writeUtf` on FriendlyByteBuf
- `PayloadTypeRegistry.playS2C()` → `.clientboundPlay()`
- `Identifier` lives at `net.minecraft.resources.Identifier` (not `util` or `ResourceLocation`)
- `Screen.client` field → `Screen.minecraft`
- `player.sendMessage(text, true)` → `player.sendOverlayMessage(text)` (action bar)
- `player.sendMessage(text, false)` → `player.sendSystemMessage(text)` (chat)
- `EditBox.getText()` → `getValue()`, `setChangedListener()` → `setResponder()`
- `ObjectSelectionList.getSelectedOrNull()` → `getSelected()`
- `Checkbox.Builder.checked()` → `selected()`, `.callback()` → `.onValueChange()`
- `Button.dimensions()` → `bounds()`, `addDrawableChild()` → `addRenderableWidget()`
- `Screen.close()` → `onClose()` override
- `getWindow().getScaledWidth()` → `getWindow().getGuiScaledWidth()`
- `source.sendError()` → `sendFailure()`, `sendFeedback()` → `sendSuccess()`
- `Permission.Level` → `Permission.HasCommandLevel`, `source.getPermissions()` → `source.permissions()`
- `isDead()` → `isDeadOrDying()`, `getEntityWorld()` → `level()`
- `server.getPlayerManager().getPlayerList()` → `server.getPlayerList().getPlayers()`
- `server.getCurrentPlayerCount()` → `server.getPlayerCount()`
- `server.isDedicated()` → `server.isDedicatedServer()`
- `CreateWorldScreen.show()` → `openFresh()`, `createLevel()` → `onCreate()`
- `WorldCreator` → `WorldCreationUiState`, `getWorldCreator()` → `getUiState()`
- `WorldCreator.Mode.HARDCORE` → `WorldCreationUiState.SelectedGameMode.HARDCORE`
- `setCheatsEnabled()` → `setAllowCommands()`, `setWorldName()`/`getWorldName()` → `setName()`/`getName()`
- `MinecraftClient.send()` → `Minecraft.execute()`
- `IdentifierArgumentType.identifier()` → `IdentifierArgument.id()`, `.getIdentifier()` → `.getId()`
- `Registries.ITEM` → `BuiltInRegistries.ITEM`, `.getId()` → `.getKey()`, `.containsId()` → `.containsKey()`
- `Registry.get(Identifier)` now returns `Optional<Reference<T>>` — use `DefaultedRegistry.getValue()` for direct lookup
- `Item.components()` delegates to holder — crashes if components not bound; use `Item.getDescriptionId()` for safe pre-world name access
- Access widener namespace must be `official` (not `named`) for unobfuscated MC
- `DataComponents.ITEM_MODEL` (`Identifier`) drives item rendering — must be set for `context.item()` to render

## Conventions

- Keep mixin surface area minimal to reduce breakage on MC updates.
- Target MC 26.1, use only stable Fabric API modules.
