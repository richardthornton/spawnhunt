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
