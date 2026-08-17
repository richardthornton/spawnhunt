# SpawnHunt

A Fabric mod for Minecraft Java Edition 26.3.
Speedrun-style scavenger hunt: find and collect a random survival-obtainable block as fast as possible.
Supports both singleplayer (client-side) and multiplayer (server-side commands + HUD sync).

## Testing Environment

- **Minecraft instance (macOS):** `/Applications/MultiMC.app/Data/instances/Family 26.2/.minecraft`
  — still a 26.2 instance; a 26.3 one has to be created before the port can be playtested in-game.
- **Minecraft instance (Windows):** `C:\MultiMC\instances\SpawnHunt 26.2\.minecraft` — same.
- The instance's Fabric API must be **at least** `fabric_version` from `gradle.properties`;
  `fabric.mod.json` declares that floor, so Fabric refuses to load with a clear message
  instead of dying on a `NoSuchMethodError` mid-game.
- Built `.jar` goes into the `mods/` folder of that instance
- Requires Fabric Loader + Fabric API for MC 26.3
- **No MultiMC instance needed for a smoke test:** `./gradlew runServer` boots a headless
  dev server with the mod loaded (accept the EULA once in `run/eula.txt`). That exercises
  the common entrypoint, `/spawnhunt`, the item pool against the real registry, and
  `GameModeLockMixin` — everything except the client screens and HUD.

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

- **Build:** Gradle 9.5.1 + Fabric Loom 1.17.19 (`net.fabricmc.fabric-loom` — no-remap for unobfuscated MC)
- **Java:** JDK 25 **or newer** — Gradle itself must run on it. Compilation pins `options.release = 25`,
  so a newer JDK (26 etc.) is fine and no JDK 25 install is required. `settings.gradle` fails fast with
  the fix if the JVM is too old.
- **Dependencies:** fabric-loader 0.19.3, fabric-api 0.157.1+26.3
- **Mappings:** None (MC 26.3 is unobfuscated — uses Mojang official names directly)
- **Language:** Java

## Build Commands

```bash
# Build (needs a JDK 25+; usually just works if JAVA_HOME already points at one)
./gradlew build

# Output jar: build/libs/spawnhunt-<version>.jar
```

**If the build reports the wrong Java version, `JAVA_HOME` is not the thing to fix.**
Gradle takes its JVM from `-Dorg.gradle.java.home` first, then `org.gradle.java.home` in
**`~/.gradle/gradle.properties`**, and only then `JAVA_HOME`. A per-user gradle.properties
pinning an old JDK outranks the environment and is the usual cause — exporting `JAVA_HOME`
has no effect against it. Either fix that file or override per-invocation:

```bash
/usr/libexec/java_home -V                       # list installed JDKs (macOS)
./gradlew build -Dorg.gradle.java.home=/path/to/jdk-25-or-newer
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

### Phase 26.2 — Port to Minecraft 26.2 "Chaos Cubed"
- [x] P1 Toolchain bump (MC 26.2, loader 0.19.3, fabric-api 0.157.0+26.2, Loom 1.17.19, Gradle 9.5.1)
- [x] P2 Build reliability (scoped Fabric repo, raised HTTP timeouts)
- [x] P3 Gui/Hud split migration (`Minecraft.gui.setScreen`, `Minecraft.gui.screen()`)
- [x] P4 Verify mixin targets and access widener still resolve against 26.2
- [x] P5 Audit the 31 new items for survival obtainability (no exclusions needed)

### Phase 26.3 — Port to Minecraft 26.3 "Dappled Forest"
- [x] Q1 Toolchain bump (MC 26.3-snapshot-8, fabric-api 0.157.1+26.3; loader, Loom and Gradle unchanged)
- [x] Q2 `fabric.mod.json` predicate `~26.3-` — the trailing hyphen is what makes it match
      snapshot builds as well as the eventual 26.3 release (Fabric API declares its own the same way)
- [x] Q3 Verify mixin targets and the access widener still resolve against 26.3 (all unchanged)
- [x] Q4 Audit the 121 new items for survival obtainability (no exclusions needed — see below)
- [x] Q5 Headless `runServer` smoke test on 26.3-snapshot-8

**The 26.3 item audit.** 121 items added, none removed, so the pool goes 1409 → 1530.
Every one is survival-obtainable, so `ItemPool.EXCLUDED` is untouched:

- Poplar wood set, wool/concrete stairs and slabs, cushions, straw bed, boats — craftable.
- Poplar logs/leaves/sapling, red shrub, shelf mushroom — block drops.
- 16 new map items — this is the only interesting call. Explorer maps used to be `filled_map`
  with components (hence the `filled_map` exclusion); in 26.3 they are their own registry
  entries, so they enter the pool on their own IDs. All are reachable — abandoned-camp chest
  loot, shipwreck/ruin loot, or cartographer trades — but some are *slow*: the village and
  ocean/swamp explorer maps are trade-only, and `woodland_explorer_map` needs a Master-level
  cartographer. Kept in, because the pool's rule is "survival-obtainable", and it already
  contains comparably long targets (`nether_star`, `dragon_egg`, `elytra`).

The audit is reproducible without launching the game: the item registry is exactly the set of
`assets/minecraft/items/*.json` entries in the client jar, and `data/minecraft/{recipe,loot_table,
villager_trade}` says how each one is obtained. Diff two client jars to get the delta.

### Phase H — Hardening (from the July 2026 code review)
- [x] H1 Server state lifecycle (reset `ServerHuntState` + tick counter on `SERVER_STOPPED`)
- [x] H2 Game-mode gate on multiplayer wins (survival/adventure only)
- [x] H3 Thread safety for shared statics (concurrent name cache, volatile pool + `HuntState.active`/`won`)
- [x] H4 Client render perf (cached entry ItemStacks, cached HUD best time, precomputed search names)
- [x] H5 Server tick perf (pre-resolved target `Item`, no periodic sync after a win)
- [x] H6 Composite payload codecs (`Identifier.STREAM_CODEC`, symmetric string bounds)
- [x] H7 Hygiene (vanilla broadcast, stale armed-hunt guard on the title screen)

## Key Design Decisions

- **State is not persisted** — exiting the world ends the hunt. Crash = hunt over.
- **Item pool** uses tag-and-filter: start with all registry items, filter survival-obtainables, exclude unobtainables via a static exclusion set.
- **Win state** — no separate overlay; HUD border and item name turn green, victory sound plays.
- **Singleplayer timer** uses delta-accumulation (not start-time subtraction) to avoid drift across pause/unpause.
- **Multiplayer timer** uses server wall-clock (no pause concept); displayed in mm:ss format (no milliseconds).
- **Inventory scanning** is trivially fast (36 slots + armor + offhand per tick).
- **Cursor and crafting grid are not scanned** — an item held on the cursor or sitting in
  the 2×2 crafting grid doesn't trigger a win until it lands in the inventory proper.
  Deliberate: in practice that happens within moments, and scanning those containers adds
  surface area for no real gain.
- **Multiplayer wins require survival or adventure** — creative players can pull the target
  from the creative inventory and spectators can't legitimately hold items, so both are
  skipped by the win scan. This is the dedicated-server equivalent of `GameModeLockMixin`,
  which only applies to integrated servers.
- **Two separate state paths** — singleplayer uses `HuntState` (client static), multiplayer uses `ServerHuntState` (server) synced to `ClientHuntState` (client mirror). No shared mutable state.
- **Thread ownership of the statics** — on integrated servers the render thread and server
  thread share a JVM, so: `ItemPool` is touched by both (concurrent name cache, volatile
  pool); `HuntState` is written only by the client but `active`/`won` are read from the
  server thread by `GameModeLockMixin`, hence volatile; `ClientHuntState` is render-thread
  only (packet handlers hand off via `client.execute`). Keep new statics in one of these
  three buckets rather than inventing a fourth.
- **Static state is reset on lifecycle boundaries, not on demand** — `ServerHuntState` on
  `SERVER_STOPPED`, `HuntState`/`ClientHuntState` on client DISCONNECT, and an armed-but-
  unstarted `HuntState` on `TitleScreen.init`. Singletons outlive the integrated server, so
  anything not reset leaks into the next world.
- **Vanilla client support** — players without the mod see action bar messages during active hunts and chat messages for start/stop/win events.
- **Multiplayer commands** require OP level 2 (GAMEMASTERS); `/spawnhunt status` is available to all players.
- **Pre-world item rendering** — MC 26.3 binds item components (including `ITEM_MODEL`) during world load, but the selection screen runs pre-world. `ItemPool.ensureComponentsBound()` binds a minimal `DataComponentMap` with `ITEM_MODEL` set to the item's registry ID so `ItemStack` creation and rendering works on the title screen. Vanilla overwrites with full data-driven components during world load.
- **Display names** use `Component.translatable(item.getDescriptionId())` instead of `ItemStack.getHoverName()` to avoid creating ItemStacks for name resolution (safe pre-world).

## Key Risks

- **Programmatic world creation** has no clean public API — may need deep mixins or auto-click approach as fallback.
- **Item pool accuracy** — maintain exclusion list carefully, log pool on startup, iterate via community feedback.
- **Pre-world component binding** — `ensureComponentsBound()` binds minimal components before vanilla does. If vanilla changes the binding lifecycle or adds validation, this could break. Monitor across MC updates.

## Versioning & Release Process

Uses [SemVer](https://semver.org/). **`CHANGELOG.md` is the version history — it is
maintained by hand, and no automation writes to it. Don't add version tables here.**

`mod_version` in `gradle.properties` is the single source of truth for the version
number: `build.gradle` reads it into `project.version`, and `processResources` expands
it into `fabric.mod.json`'s `${version}`. Nothing else hardcodes a version.

The release flow:

1. Work lands on `dev`. You don't need to know the version number while working.
2. When the work is complete, cut `release/X.Y.Z` from `dev`. The `Set Release Version`
   workflow reads the version out of the branch name and commits the `mod_version` bump.
3. Write the `CHANGELOG.md` entry by hand on the release branch.
4. On approval, merge `release/X.Y.Z` into `main`. That builds the jar and publishes the
   GitHub release, tagged `vX.Y.Z`, with notes taken from the top `CHANGELOG.md` section.
5. Merge the release branch back into `dev` yourself — this is deliberately not automated.

Only a merged `release/*` branch cuts a release; a `dev`/`hotfix` merge to `main` builds nothing.

## API Notes (MC 26.3)

The 26.2 → 26.3 port needed **no source changes at all** — it is a version bump plus metadata.
Worth recording, because "nothing moved" is itself the finding:

- Every mixin and access-widener target is byte-identical in shape: `TitleScreen.init()`,
  `CreateWorldScreen.init()`/`onCreate()`/`getUiState()`, and
  `ServerPlayer.setGameMode(GameType)` still returning `boolean`.
- The fragile pre-world path survived intact — `Item.builtInRegistryHolder()` (still deprecated,
  still present), `Holder.Reference.bindComponents`/`areComponentsBound`, and
  `DataComponents.ITEM_MODEL` typed as `DataComponentType<Identifier>`.
- `WorldCreationUiState` kept `setName`/`getName`/`setGameMode`/`setDifficulty`/`setAllowCommands`
  and the `SelectedGameMode.SURVIVAL`/`HARDCORE` constants.
- Loom 1.17.19 and Gradle 9.5.1 handle 26.3 snapshots as-is; no toolchain bump was needed.

Carried over from 26.2 (still current):

- MC 26.3 is unobfuscated, same as 26.2 — no mappings.
- `Gui` owns the screen stack and the HUD: `Minecraft.gui.setScreen(s)`, `Minecraft.gui.screen()`,
  `Minecraft.gui.hud`.

Changes introduced by the earlier 26.1 → 26.2 port:

- `Gui` now owns the screen stack and the HUD. `Minecraft.setScreen(s)` → `Minecraft.gui.setScreen(s)`,
  and the `Minecraft.screen` field → `Minecraft.gui.screen()`. `Minecraft.gui.hud` is the new
  in-game HUD object (`net.minecraft.client.gui.Hud`), split out of `Gui`.
- `GuiGraphicsExtractor`, `HudElementRegistry`, `Font.width`, and `context.text/item/fill` are
  unchanged — the `Font.drawInBatch`/`PreparedText` rework in 26.2 sits below the extractor API,
  so the HUD and screens needed no render changes.
- `Item.builtInRegistryHolder()` is now **deprecated** (still present and functional). The pre-world
  component binding in `ItemPool.ensureComponentsBound()` depends on it — this is the most likely
  thing to break on the next MC update.
- Mixin targets all survived: `TitleScreen.init`, `CreateWorldScreen.init`/`onCreate`/`getUiState`,
  and `ServerPlayer.setGameMode(GameType)`.
- The Fabric maven is slow enough to time out Gradle's 30s default mid-resolve; `gradle.properties`
  raises the HTTP timeouts and `settings.gradle` scopes the Fabric repo to `net.fabricmc*` so
  third-party artifacts (ASM etc.) resolve from Maven Central instead.

Carried over from 26.1 (still current):

- MC 26.2 is **unobfuscated** — uses Mojang official names, no Yarn/intermediary mappings
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
- Target MC 26.3, use only stable Fabric API modules.
