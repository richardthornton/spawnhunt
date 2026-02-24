# SpawnHunt — Development Plan

**Mod Type:** Fabric (client-side)
**Target:** Minecraft Java Edition 1.21.11
**Concept:** A speedrun-style scavenger hunt. The player is given a random survival-obtainable block to find and collect as fast as possible.

---

## 1. Requirements Summary

| Feature | Detail |
|---|---|
| Main Menu | New **"SpawnHunt"** button added to the title screen |
| Block Selection Screen | Shows a randomly chosen block with **Reroll** (unlimited), **Cancel**, and **Start** buttons |
| Block Pool | All blocks obtainable in pure Survival (no commands/creative) |
| World Creation | Instant — default Survival, normal difficulty, random seed, no cheats |
| In-Game HUD | Target block icon + name (one corner) and a running timer (opposite corner) |
| Timer | `HH:MM:SS` format, pauses when the game is paused |
| Win Condition | Player picks up the target block (enters inventory) |
| On Win | Timer freezes, final time displayed prominently on screen, victory sound plays |

---

## 2. Technical Architecture

### 2.1 Project Setup

- **Build system:** Gradle with Fabric Loom
- **Key dependencies:**
  - `fabric-loader` (latest for 1.21.11)
  - `fabric-api` (latest for 1.21.11)
  - Minecraft `1.21.11` mappings (Mojang or Yarn)
- **Package structure:**

```
com.spawnhunt
├── SpawnHuntMod.java              // Mod entrypoint (ClientModInitializer)
├── data/
│   ├── BlockPool.java             // Survival-obtainable block registry & random selection
│   └── HuntState.java            // Active hunt state (target block, timer, win flag)
├── screen/
│   ├── SpawnHuntScreen.java       // Block selection GUI (Reroll / Cancel / Start)
│   └── VictoryOverlay.java        // Win overlay (final time display)
├── hud/
│   └── HuntHudRenderer.java      // In-game HUD (timer + target block icon/name)
├── event/
│   ├── InventoryListener.java     // Detects when the target block enters inventory
│   └── WorldLifecycleHandler.java // Resets hunt state on world exit
└── mixin/
    └── TitleScreenMixin.java      // Injects "SpawnHunt" button into main menu
```

### 2.2 State Management

A singleton `HuntState` object holds all runtime data:

```java
public class HuntState {
    boolean active;           // Is a hunt currently running?
    Identifier targetBlock;   // e.g. minecraft:ancient_debris
    long startTimeMs;         // System.currentTimeMillis() at world load
    long accumulatedMs;       // Total unpaused time
    boolean paused;           // Mirrors game-pause state
    boolean won;              // Set true on block collection
    long finalTimeMs;         // Frozen time at win
}
```

- State is **not** persisted — exiting the world ends the hunt.
- State resets when returning to the title screen.

---

## 3. Block Pool Design

### 3.1 Approach

Rather than maintaining a massive hardcoded list, use a **tag-and-filter** approach:

1. **Start** with every entry in the `Block` registry (`Registries.BLOCK`).
2. **Include only** blocks that have a corresponding `Item` (i.e. `block.asItem() != Items.AIR`).
3. **Exclude** blocks that cannot be obtained in Survival without commands. Maintain a static exclusion set:
   - Technical blocks: `air`, `cave_air`, `void_air`, `light`, `barrier`, `structure_void`, `command_block`, `chain_command_block`, `repeating_command_block`, `jigsaw`, `structure_block`
   - Unobtainable in survival: `bedrock`, `end_portal_frame`, `spawner`, `trial_spawner`, `infested_*` variants (player cannot silk-touch these into their original form), `budding_amethyst`, `reinforced_deepslate`, `petrified_oak_slab`, `farmland`, `dirt_path`, `frogspawn`, `player_head`, `player_wall_head`
   - Creative-only blocks: `debug_stick` target blocks, `knowledge_book` (items, but worth checking block forms)
4. **Validate** the final list manually during development and log the pool size at startup for sanity checking.

### 3.2 Random Selection

```java
public static Block getRandomBlock(Random random) {
    List<Block> pool = getFilteredPool();
    return pool.get(random.nextInt(pool.size()));
}
```

Pool is computed once at mod init and cached.

---

## 4. Feature Breakdown & Tasks

### Phase 1 — Scaffolding & Project Setup ✅

| # | Task | Status | Detail |
|---|---|---|---|
| 1.1 | Initialize Fabric mod project | ✅ Done | Gradle 9.2.1 + Fabric Loom 1.15.4. Yarn 1.21.11+build.4, Loader 0.18.4, Fabric API 0.141.3+1.21.11. JDK 21 (Adoptium 21.0.10+7). |
| 1.2 | Create `SpawnHuntMod` entrypoint | ✅ Done | `ClientModInitializer` with logger. `HuntState` singleton with delta-accumulation timer. |
| 1.3 | Set up Mixin configuration | ✅ Done | `spawnhunt.mixins.json` wired into `fabric.mod.json`. Placeholder `TitleScreenMixin` ready for Phase 3. |

### Phase 2 — Block Pool ✅

| # | Task | Status | Detail |
|---|---|---|---|
| 2.1 | Implement `BlockPool` class | ✅ Done | Tag-and-filter from registry. Deduplicates by item (handles wall variants). Explicit exclusion set for technical/unobtainable blocks. Cached on first access. |
| 2.2 | Add random selection method | ✅ Done | `getRandomBlock(Random)` — uniform pick from cached pool. |
| 2.3 | Test & validate pool | ✅ Done | `logPool()` called at startup — logs every block at DEBUG, pool size at INFO. Manual review pending first in-game test. |

### Phase 3 — Main Menu Integration ✅

| # | Task | Status | Detail |
|---|---|---|---|
| 3.1 | Create `TitleScreenMixin` | ✅ Done | `@Inject` at TAIL of `TitleScreen.init()`. Extends Screen for access to `addDrawableChild`. |
| 3.2 | Position the button | ✅ Done | Centered, at `height/4 + 156` — below Options/Quit row. Standard 200x20 size. Tested in-game. |
| 3.3 | Wire button to open `SpawnHuntScreen` | ✅ Done | Opens `SpawnHuntScreen` with random block, Reroll/Cancel/Start buttons (Start is placeholder for Phase 5). |

### Phase 4 — Block Selection Screen ✅

| # | Task | Status | Detail |
|---|---|---|---|
| 4.1 | Create `SpawnHuntScreen` (extends `Screen`) | ✅ Done | Centered layout: 4x block icon, translated name, three buttons below. |
| 4.2 | Render the target block | ✅ Done | `DrawContext.drawItem()` at 4x scale via JOML `Matrix3x2fStack`. Translated block name below icon. Uses ARGB colors (alpha required). Dark background overlay. Tested in-game. |
| 4.3 | Implement **Reroll** button | ✅ Done | Calls `BlockPool.getRandomBlock()`, refreshes display. Unlimited. |
| 4.4 | Implement **Cancel** button | ✅ Done | Returns to title screen. |
| 4.5 | Implement **Start** button | ✅ Done | Sets `HuntState.startHunt()` with target block. World creation placeholder for Phase 5. |

### Phase 5 — World Creation

| # | Task | Detail |
|---|---|---|
| 5.1 | Programmatic world creation | Use Minecraft's `CreateWorldScreen` internals or `LevelStorage` + `WorldPresetManager` to create and immediately load a new Survival world. |
| 5.2 | Configure world defaults | Difficulty: Normal. Game mode: Survival. Cheats: off. Seed: random. World name: `"SpawnHunt-<timestamp>"`. |
| 5.3 | Start timer on world load | Listen for `ClientTickEvents.END_CLIENT_TICK` or world-ready event. Set `HuntState.startTimeMs` once the player is in-game. |

### Phase 6 — In-Game HUD

| # | Task | Detail |
|---|---|---|
| 6.1 | Create `HuntHudRenderer` | Register via `HudRenderCallback` (Fabric API). |
| 6.2 | Render timer | Top-right corner. Format: `HH:MM:SS`. Font: default Minecraft. Semi-transparent background for readability. |
| 6.3 | Render target block | Top-left corner. Small item icon (16×16 or 24×24) + block name text beside it. Semi-transparent background. |
| 6.4 | Pause-aware timer logic | On each client tick: if game is paused (`MinecraftClient.getInstance().isPaused()`), do not accumulate time. Track `accumulatedMs` manually using delta between ticks. |

### Phase 7 — Win Detection

| # | Task | Detail |
|---|---|---|
| 7.1 | Create `InventoryListener` | On every client tick (or via screen handler events), scan the player's inventory for the target block's item form. |
| 7.2 | Trigger win | When item detected: set `HuntState.won = true`, freeze `finalTimeMs`. |
| 7.3 | Play victory sound | Use `MinecraftClient.getInstance().player.playSound()` with a suitable sound (e.g. `SoundEvents.UI_TOAST_CHALLENGE_COMPLETE` or `SoundEvents.ENTITY_PLAYER_LEVELUP`). |
| 7.4 | Show final time on screen | Replace the timer HUD element with a larger, centred "final time" display. Optionally animate it in. Keep it visible until the player exits. |

### Phase 8 — Lifecycle & Cleanup

| # | Task | Detail |
|---|---|---|
| 8.1 | Reset state on world exit | When returning to title screen, fully reset `HuntState`. |
| 8.2 | Edge cases | Handle: player dies (timer keeps running — this is a hunt!), player opens inventory/chat (timer keeps running — only pause-menu pauses it), creative mode shouldn't be reachable (cheats off). |
| 8.3 | Handle disconnect/crash | Since state isn't persisted, a crash just ends the hunt. No recovery needed for MVP. |

### Phase 9 — Polish & Testing

| # | Task | Detail |
|---|---|---|
| 9.1 | Visual polish | Ensure HUD elements don't overlap with vanilla HUD (hotbar, XP bar, boss bars). Add subtle background boxes. |
| 9.2 | Screen polish | Add a block-spinning animation or particle effect on the selection screen. |
| 9.3 | Test block pool | Spot-check 20+ random rolls to ensure no unobtainable blocks appear. |
| 9.4 | Playtesting | Full runs with easy targets (e.g. `dirt`) and hard targets (e.g. `ancient_debris`, `sponge`). Verify timer accuracy over 1+ hour sessions. |
| 9.5 | Build & distribute | Produce final `.jar`. Test clean install with only Fabric Loader + Fabric API. |

---

## 5. Key Technical Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **Programmatic world creation** is not a clean public API in Minecraft | Could require deep mixins or brittle reflection | Research `CreateWorldScreen` flow early in Phase 5. Fallback: pre-fill and auto-click a hidden `CreateWorldScreen`. |
| **Block pool accuracy** — missing or including wrong blocks | Player frustration (impossible hunt) | Maintain the exclusion list as a config-editable resource. Log pool on startup. Community feedback loop. |
| **Inventory scanning performance** | Checking every tick could lag on large inventories | Player inventory is only 36 slots + armor + offhand — trivially fast. No concern at this scale. |
| **Timer drift** | `System.currentTimeMillis()` can drift across pause/unpause | Use a delta-accumulation approach rather than start-time subtraction. Track pause transitions explicitly. |
| **Fabric API breakage** on MC updates | Mod stops working on next MC patch | Pin to 1.21.11, use only stable Fabric API modules. Minimise mixin surface area. |

---

## 6. Estimated Effort

| Phase | Estimate |
|---|---|
| 1 — Scaffolding | ~1 hour |
| 2 — Block Pool | ~2 hours |
| 3 — Main Menu Button | ~1 hour |
| 4 — Selection Screen | ~3 hours |
| 5 — World Creation | ~3–5 hours (highest uncertainty) |
| 6 — In-Game HUD | ~3 hours |
| 7 — Win Detection | ~2 hours |
| 8 — Lifecycle | ~1 hour |
| 9 — Polish & Testing | ~3–4 hours |
| **Total** | **~19–22 hours** |

---

## 7. Definition of Done

A single `.jar` file that, when installed alongside Fabric Loader and Fabric API on Minecraft 1.21.11:

1. Adds a **"SpawnHunt"** button to the title screen.
2. Opens a screen showing a random survival-obtainable block with Reroll / Cancel / Start.
3. **Start** creates and loads a new default Survival world immediately.
4. A **HH:MM:SS** timer runs in the top-right, pausing only when the game is paused.
5. The **target block icon + name** is displayed in the top-left.
6. When the target block's item enters the player's inventory, the **timer freezes**, the **final time is displayed prominently**, and a **victory sound** plays.
7. Exiting the world cleanly resets all state.
