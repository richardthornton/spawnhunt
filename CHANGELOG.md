## v3.1.0

A correctness and performance pass over multiplayer hunts and the client UI.

### Fixed

- Hunts no longer carry over between worlds — a hunt started in one singleplayer or LAN world stayed active in the next world opened, with the timer still running through the main menu
- Creative and spectator players can no longer win a multiplayer hunt by taking the target item from the creative inventory
- A hunt whose world creation never completed no longer attaches its timer to the next world or server joined
- Shared item and hunt state is now safe to read from both the render thread and the server thread on integrated servers

### Improved

- Smoother item chooser — display names are resolved once instead of on every keystroke, and list rows no longer rebuild their item icon every frame
- The HUD's "Best:" time is cached rather than re-read from disk state every frame
- Servers do less work per tick and stop re-broadcasting an unchanging state after a hunt is won
- Network payloads rebuilt on Minecraft's standard stream codecs (server and client must run matching versions, as before)

## v3.0.0

26.1 support is here! Golden Dandelions watch out...

### Added

- SpawnHunt now supports Minecraft Java Edition version 26.1

## v2.2.0

This release adds a new slot machine rolling animation to the single player menu.

### Added

- Slot machine rolling animation in the singleplayer menu

## v2.1.0

This release prevents players from circumventing the hunt by switching game modes during an active singleplayer session.

### Fixed

- Game mode is now locked to Survival during active singleplayer hunts, preventing players from opening to LAN with cheats to switch to Creative mode

## v2.0.0

SpawnHunt now supports multiplayer — run hunts on any Fabric server with full HUD sync for mod clients and action bar fallback for vanilla players.

### Added

- Multiplayer support — server-authoritative hunts managed via /spawnhunt commands (start, stop, restart, status)
- Server-to-client HUD sync — mod clients see the full SpawnHunt HUD (target item, timer) during multiplayer hunts
- Vanilla client compatibility — players without the mod receive target and timer updates via the action bar, plus chat messages for start/stop/win events
- Server-side win detection — inventory scanning and victory announcements are handled by the server

### Improved

- HUD rendering — updated to support both singleplayer and multiplayer state sources seamlessly
- Hunt state cleanup — state resets properly on disconnect for both singleplayer and multiplayer sessions

## v1.3.0

This release redesigns the main menu and HUD for a cleaner look, improves item display names, and fixes several item pool and layout issues.

### Added

- Vertical main menu layout optimised for vertical content creation

### Improved

- HUD redesigned with a minimal, centred timer layout without background panel
- Music discs now display as "Music Disc - {song}" without the artist name
- Selection screen layout refactored with named constants for spacing and scaling
- History panels sized to match title width for a consistent look

### Fixed

- Test Block and Test Instance Block excluded from the item pool
