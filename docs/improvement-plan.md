# SpawnHunt Improvement Plan

> **Status (2026-08-13):** all six PRs are implemented on `worktree-new-version` and land
> together as `3.1.0` rather than as separate `3.0.1` / `3.1.0` releases. Every code task
> below is done and the build passes with JDK 25. The **manual in-game tests listed in each
> PR have not been run** — that is the remaining work before release. Deviations from the
> plan as written:
>
> - The `SERVER_STOPPED` handler lives in `ServerHuntManager.register()` rather than
>   `SpawnHuntCommon.onInitialize()`, so it can reset `tickCounter` alongside the state.
> - The win scan uses `player.gameMode().isSurvival()`, which admits **adventure** as well
>   as survival (`GameType.isSurvival()` covers both) and excludes only creative and
>   spectator — the two modes that actually break the hunt. The plan's "survival only"
>   recommendation would have locked out legitimate adventure-mode servers.
> - `ServerHuntState.start` now takes an `Item`; `getTargetItem()` returns the `Item` and
>   the new `getTargetId()` returns the `Identifier`.

Plan for addressing the findings from the July 2026 code review, broken into six PRs
ordered by priority. Each PR is independently shippable and small enough to review in
one sitting. PRs 1–2 are correctness fixes suitable for a `3.0.1` patch release;
PRs 3–6 are performance/hygiene work that can ride in `3.1.0`.

Branch naming follows existing convention: `hotfix/<name>` for PR 1–2, `feature/<name>`
or `chore/<name>` for the rest. All PRs target `dev`.

---

## PR 1 — Fix server hunt state leaking across worlds & creative-mode wins

**Branch:** `hotfix/server-state-integrity`
**Severity:** High — both are user-visible correctness bugs in multiplayer.

### Problem 1: `ServerHuntState` survives server shutdown

`ServerHuntState` is a static singleton and nothing resets it when the (integrated)
server stops. `WorldLifecycleHandler` only resets *client* state on disconnect.

Repro: open world A to LAN → `/spawnhunt start` → exit to title → open world B.
The hunt is still active in world B and the timer includes menu time, because
`ServerHuntManager.onServerTick` resumes immediately.

### Problem 2: win scan ignores game mode

`ServerHuntManager.scanInventories` (`event/ServerHuntManager.java:62`) checks every
player, including creative/spectator. On a dedicated server (where `GameModeLockMixin`
deliberately does not apply), a creative player can grab the target item from the
creative inventory and instantly win for the whole server.

### Tasks

- [ ] In `SpawnHuntCommon.onInitialize()`, register `ServerLifecycleEvents.SERVER_STOPPED`
      → `ServerHuntState.reset()` (import `net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents`).
- [ ] Also reset `ServerHuntManager.tickCounter` on server stop (add a small
      `ServerHuntManager.reset()` or make the tick handler tolerant — counter is
      already zeroed when no hunt is active, so a comment may be enough; decide in-PR).
- [ ] In `scanInventories`, skip players not in survival:
      `if (!player.gameMode.isSurvival()) continue;`
      (verify the MC 26.1 accessor name — may be `player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL`).
      Decide whether adventure mode should count; recommendation: survival only, matching
      the singleplayer game-mode lock.
- [ ] Manual test (integrated): LAN world A → start hunt → exit → open world B → confirm
      no HUD/action bar and `/spawnhunt status` reports no hunt.
- [ ] Manual test (integrated, two accounts or dedicated server): player in creative
      picks up target item → no win; switches to survival with item still in inventory →
      win fires (this is acceptable: they legitimately possess it in survival — note in PR
      description; if unwanted, clear-cut alternative is ignoring items while any
      non-survival stint is active, which is out of scope).
- [ ] Build with JDK 25, install jar into `C:\MultiMC\instances\SpawnHunt 26.1\.minecraft\mods`, smoke test SP hunt still works.

---

## PR 2 — Thread safety for shared static caches

**Branch:** `hotfix/thread-safety`
**Severity:** Medium-high — latent race, hard to repro, worst case corrupts the display
name cache or publishes a partially built pool on the integrated server (server thread
and render thread both touch these).

### Findings

- `ItemPool.displayNameCache` (`data/ItemPool.java:23`) is a plain `HashMap` written from
  the render thread (screens, HUD) *and* the server thread (`ServerHuntManager`,
  commands) on integrated servers.
- `ItemPool.pool` is lazily initialized without safe publication.
- `GameModeLockMixin` reads client-written `HuntState` statics (`active`, `won`) from the
  server thread with no `volatile`.

### Tasks

- [ ] Change `displayNameCache` to `ConcurrentHashMap`; use `computeIfAbsent` in
      `getDisplayName` instead of get/put.
- [ ] Make `ItemPool.pool` safely published: either mark the field `volatile`, or build
      eagerly (call `getPool()` from both entrypoints' init). Prefer `volatile` — eager
      init from `SpawnHuntCommon` would run on dedicated servers too, which is fine but
      changes startup logging.
- [ ] Mark `HuntState.active` and `HuntState.won` as `volatile` (these are the two fields
      `GameModeLockMixin` reads cross-thread). Add a short comment on the class noting
      which thread writes and which reads.
- [ ] Review `ClientHuntState` for the same pattern: written via `client.execute(...)` so
      it's render-thread-only — confirm and leave as is (comment optional).
- [ ] Build + smoke test: SP hunt start/win, MP `/spawnhunt start` + status, item chooser
      search all behave identically.

---

## PR 3 — Client render performance

**Branch:** `chore/client-render-perf`
**Severity:** Medium — per-frame allocation and repeated translation lookups.

### Tasks

- [ ] **Cache list-entry ItemStacks.** `ItemChooserScreen.Entry.extractContent`
      (`screen/ItemChooserScreen.java:148`) allocates `new ItemStack(item)` every frame per
      visible row. Create the stack once in the `Entry` constructor (same fix already
      applied to `SpawnHuntScreen` in 3.0.0).
- [ ] **Cache HUD best time.** `HuntHudRenderer.renderHud` calls
      `ResultStore.getBestTime(targetId)` every frame (`hud/HuntHudRenderer.java:117`).
      Add `cachedBestTimeMs` next to `cachedTargetId`/`cachedStack`; recompute when the
      target changes and after `ResultStore.recordResult` (either invalidate via
      `HuntHudRenderer.resetCache()`-style hook or re-query once on win). Ensure the
      "Best:" line still updates after finishing a run and immediately starting another
      hunt for the same item.
- [ ] **Precompute search names.** `ItemChooserScreen.refreshList` resolves
      `getDisplayName(item).getString().toLowerCase()` for the whole pool on every
      keystroke, and the initial sort does the same O(n log n) times. In `init()`, build a
      sorted `List<Entry-like record (Item item, String lowerName)>` once, then filter on
      the precomputed strings. (Names can't change mid-screen; language switches recreate
      the screen.)
- [ ] Manual test: open item chooser, type/erase quickly with the full pool — no stutter;
      double-click select still works; HUD "Best:" line correct across two consecutive
      runs of the same target.

---

## PR 4 — Server tick efficiency

**Branch:** `chore/server-tick-perf`
**Severity:** Low-medium — wasted per-tick work and post-win network chatter.

### Tasks

- [ ] **Resolve the target `Item` once.** `scanInventories` calls
      `BuiltInRegistries.ITEM.getValue(targetId)` 20×/sec
      (`event/ServerHuntManager.java:66`). Store the resolved `Item` in `ServerHuntState`
      alongside the `Identifier` when the hunt starts; clear on reset.
      `broadcastActionBarToVanillaClients` and `handleWin` can reuse it too.
- [ ] **Stop sync spam after a win.** Once won, `broadcastSyncToModClients` keeps firing
      every 5 ticks until `/spawnhunt stop`, but the state never changes. After
      `handleWin`'s final sync, suppress the periodic 5-tick broadcast while
      `ServerHuntState.isWon()` — keep the JOIN-time sync (late joiners still need the
      won-state payload) and keep sending on stop.
- [ ] Manual test on integrated LAN (mod client + second account if available): win a
      hunt → HUD shows won state and stays correct; a player joining *after* the win sees
      the won HUD; `/spawnhunt restart` works from the won state (2.2.1 behaviour intact).

---

## PR 5 — Network codec cleanup

**Branch:** `chore/payload-codecs`
**Severity:** Low — hygiene; no exploitable issue today.

Current codecs are hand-rolled anonymous `StreamCodec`s with asymmetric string bounds:
decode uses `readUtf(256)`/`readUtf(64)` but encode uses unbounded `writeUtf`. If a bound
were ever exceeded the *client* throws on decode and disconnects. Identifiers are also
round-tripped through strings.

### Tasks

- [ ] Rewrite `HuntSyncS2CPayload.CODEC` using `StreamCodec.composite(...)` with per-field
      codecs; use `Identifier.STREAM_CODEC` for the target item instead of a string
      (verify the MC 26.1 name for it, e.g. `Identifier.STREAM_CODEC` — check against
      vanilla payload classes). Represent "no target" explicitly (optional codec or keep
      empty-string sentinel — decide in-PR; optional is cleaner).
- [ ] Same for `HuntWinS2CPayload.CODEC`.
- [ ] If any field stays a raw string (winner name), use the same length bound on encode
      and decode.
- [ ] Update `ClientHuntState.update`/`handleWin` and `ServerHuntManager.buildSyncPayload`
      /`sendStopSync` for the new field types; delete the `Identifier.tryParse` fallbacks
      that become unnecessary.
- [ ] **Compatibility note:** this changes the wire format. Server and client must both
      run the new version — that's already the norm for this mod (payload IDs unchanged,
      version gate is informal). Mention in changelog.
- [ ] Manual test: MP hunt start/sync/win/stop against the new build on both sides;
      vanilla client on the server still gets action bar + chat only.

---

## PR 6 — Small hygiene fixes

**Branch:** `chore/hygiene`
**Severity:** Low.

### Tasks

- [ ] **Use vanilla broadcast.** Replace the manual loop in
      `ServerHuntManager.broadcastMessage` with
      `server.getPlayerList().broadcastSystemMessage(message, false)`.
- [ ] **Guard stale singleplayer state.** If `HuntState.startHunt()` runs but world
      creation never completes (user somehow backs out, or creation fails), the state
      stays active and the JOIN handler (`SpawnHuntMod.java:35`) starts the timer on the
      next world/server joined. Add a guard: when `TitleScreen` re-opens (or on
      `ClientPlayConnectionEvents.JOIN` to a server where no SpawnHunt world was just
      created), reset `HuntState` if the timer never began. Simplest robust option:
      reset `HuntState` in the Cancel path and in `TitleScreenMixin.init` when
      `HuntState.isActive()` but no timer has started (`beginTimer` never called →
      `startTimeMs == 0`) — expose a `hasTimerStarted()` accessor rather than poking
      fields.
- [ ] **Document the cursor/crafting-grid gap.** Win scan only covers
      `player.getInventory()`; an item held on the cursor or in the 2×2 crafting grid
      isn't detected until moved. Decision: leave as is (item lands in inventory within
      moments in practice). Add a line to CLAUDE.md "Key Design Decisions" so it's a
      recorded choice, not an oversight.
- [ ] Manual test: Start → world creation flow unchanged; Cancel from selection screen →
      join an MP server → no phantom HUD/timer.

---

## Suggested order & releases

| Order | PR | Release |
|-------|----|---------|
| 1 | PR 1 — server state integrity | 3.0.1 |
| 2 | PR 2 — thread safety | 3.0.1 |
| 3 | PR 3 — client render perf | 3.1.0 |
| 4 | PR 4 — server tick perf | 3.1.0 |
| 5 | PR 5 — payload codecs | 3.1.0 |
| 6 | PR 6 — hygiene | 3.1.0 |

Notes:

- PR 1 and PR 2 don't touch the same files' hot paths and can be developed in parallel,
  but land PR 1 first — it's the one players can hit.
- PR 5 changes the wire format; land it in the same minor release as PR 4 so the sync
  cadence change and codec change are tested together in MP.
- Every PR: build with JDK 25 (`JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot" ./gradlew build`),
  drop the jar into the MultiMC 26.1 instance, and run the smoke tests listed in the PR.

## Explicitly out of scope (reviewed, no action)

- **Static-singleton architecture overall** — converting `ServerHuntState` to a
  per-`MinecraftServer` object would be the "right" long-term shape, but the lifecycle
  reset in PR 1 removes the only observable bug, and the mod's single-hunt-per-server
  model doesn't need more. Revisit only if per-world or concurrent hunts become a feature.
- **Command suggestion caching** (`SpawnHuntCommand.ITEM_SUGGESTIONS` iterates the pool
  per request) — command-frequency work, not worth the code.
- **Client-side timer extrapolation between MP syncs** — display is seconds-only; the
  250 ms sync cadence is already finer than the display resolution.
