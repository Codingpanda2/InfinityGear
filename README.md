# InfinityGear

InfinityGear is a Paper 26.2 / Java 25 plugin for persistent, uniquely identified, levelable equipment. It is the staged successor to InfinityPickaxes: the migrated `infinitygear:pickaxe` profile preserves block-breaking progression, managed enchantments, deployed UUIDs, duplicate restrictions, commands, permissions, and legacy API adapters while profile-driven foundations support other tools, weapons, and armor.

## Dependencies

- Required: Paper 26.2, EcoEnchants 2026.33, eco 2026.33.
- Optional: Vault plus an economy provider, Nexo 1.25+ (the build targets its typed 1.27 API), PlaceholderAPI, and WorldGuard.
- SQLite is shaded into the production jar. Nexo and Vault are compile-only server dependencies.

EcoEnchants/Bukkit remain authoritative for canonical enchantment identity, native targets, conflicts, compatibility, metadata, and native maximums. InfinityGear adds administrator caps, disabled policies, symmetric additional conflicts, profile compatibility, sockets, and costs. `enchants.yml` synchronization is additive: missing live entries are appended; administrator edits and temporarily unavailable/orphaned entries are preserved.

## Migration from InfinityPickaxes

Remove the old jar before enabling InfinityGear. InfinityGear refuses to run if the old plugin is active and declares `InfinityPickaxes` as a provided legacy identity.

On first startup, the legacy `plugins/InfinityPickaxes` folder is copied to a dated sibling backup and missing files are copied into `plugins/InfinityGear`. Existing InfinityGear files are never overwritten, the old folder is never deleted, and `.infinitypickaxes-migration-v1` makes the process idempotent. A partial or ambiguous migration fails closed.

Legacy item PDC is read indefinitely and migrated lazily when an item is inspected, scanned, held, or used. Valid migration preserves UUID, level, XP, blocks mined, quarantine state, material, name, lore, and enchantments, writes schema version 1, and mirrors legacy keys during the compatibility window. New data is preferred. A malformed/missing legacy UUID is preserved and reported; InfinityGear never silently creates a replacement UUID.

`duplicates.db` is the single authority copied into the new folder. Its existing tables and records remain in place; a transactional migration adds `schema_migrations`, tracked kind, and tracked type without losing status, timestamps, reason, resolver, replacement UUID, sightings, actor, or location.

Rollback: stop the server, save `plugins/InfinityGear` and its database/WAL files, remove the InfinityGear jar, restore the dated legacy backup as `plugins/InfinityPickaxes`, and restore the old jar. Never run both jars.

## Gear profiles

`profiles.yml` uses stable namespaced IDs and supports accepted vanilla materials, external IDs, default creation item, enabled/auto-convert policy, display/lore, unbreakable behavior, compatible targets, enchantment overrides, base/expanded sockets, socket milestones, XP sources, and a cost multiplier.

- `EXPERIENCE`: intentional configured XP sources. The pickaxe profile keeps block-break progression.
- `ITEM_UPGRADES`: levels only through explicit upgrade operations; the armor starter uses this.
- `STATIC`: no passive progression; starter axe/sword profiles use this.

Only the migrated pickaxe behavior is eligible for automatic vanilla conversion, and conversion remains disabled by the preserved default setting. No passive damage-taking armor XP is implemented.

## Stations and enchantments

`stations.yml` configures a Runic Table, Fusion Altar, and Gear Forge using `VANILLA` material or `NEXO` IDs, interaction range, and an administrator/testing bypass. With the secure default `require-registered-instance: true`, an administrator must target the exact placed block and run `/igear station bind <type>`; bindings persist in `station-instances.yml`, and ordinary blocks with the same material do nothing. Use `/igear station status` or `/igear station unbind` while targeting a block to inspect or remove its ownership record. The Nexo adapter uses the documented typed `NexoItems`, `NexoBlocks`, `NexoFurniture`, and interaction-event APIs—no reflection or console commands.

Station menus select live player inventory slots and render clones. Inputs never move into decorative slots. Confirmation re-reads the station, range, player, slots, item identity/UUID, duplicate state, enchantments, policy, output capacity, and payment. Shift-click, drag, hotbar, collect, double-click, repeated confirmation, close, and disconnect cannot leave deposited GUI items because no real inputs are deposited.

### Runic Table

- Apply: a normal book must contain exactly one managed enchantment. A higher book installs its target level directly; equal/lower is rejected. A new enchantment consumes one socket, while raising an existing one does not. Normal books cannot pass the standard maximum. Existing oversocketed/overcap/disabled equipment is not stripped or deactivated.
- Remove: destroys the selected enchantment and preserves all others. Removing the final enchantment from a book yields a blank ordinary book. `non-removable` can be set per enchantment; overcap removal is allowed with configurable surcharge.
- Transfer: moves one selected enchantment at its exact level to a fresh canonical book, consumes one blank ordinary book and the configured Runic Conduit, and preserves other source enchantments. Over-standard/LimitBroken transfer is rejected.
- View: displays installed managed enchantments and policy information without mutation.

### Fusion Altar

Two books must each contain one identical canonical managed enchantment at the same level. They produce one fresh level+1 book through Bukkit’s enchantment API; arbitrary PDC, unrelated enchantments, and malicious metadata are not copied. Fusion cannot pass the standard maximum and has no chance of failure.

`Fuse All Matching` performs binary compression, preserves unmatched original books, recomputes its complete plan on confirmation, refuses insufficient output capacity, and charges the sum of every pairwise fusion using result-level weights. Five level-III books therefore produce a level-V book and retain one original level-III book.

### Gear Forge

Socket expansion consumes the configured Runic Rivet/payment, permanently adds one socket to that item, and stops at the profile’s expanded maximum. Installed enchantments are untouched; already-oversocketed items remain grandfathered.

## Artifacts and LimitBreak

`items.yml` defines provider, vanilla material or Nexo item ID, name, lore, model data, enabled/required/consumed policy, and unique tracking for Runic Eraser, Runic Conduit, and Runic Rivet. Nexo supplies the visual template; InfinityGear stamps the UUID/kind/type/schema/quarantine identity. A raw Nexo item or stacked tracked singleton is invalid.

Specific and universal legacy LimitBreak books remain recognized. The enchantment must already exist at its configured standard maximum; LimitBreak adds exactly one, cannot introduce an enchantment, and cannot exceed the absolute profile/progression maximum. Used overcap levels are derived as `current - standard`; there is no separate counter. Universal books require explicit selection. Runes, pity, reward distribution, Black Archive gacha, dungeons, and NoxwardArchives are deliberately outside this project.

## Costs

`costs.yml` defines reusable payment options for application, fusion, removal, transfer, and socket expansion. Components inside an option are logical AND; options are logical OR. An empty component list is the explicit representation of free.

Components support Vault money, total XP points, XP levels, vanilla items, Nexo items, and uniquely tracked InfinityGear artifacts. Missing Vault/Nexo providers or invalid IDs disable affected options and never turn them into free operations. The GUI displays and lets players cycle configured options. Charging journals removed items—including unique UUID-bearing catalysts—and compensates earlier components in reverse order if a later withdrawal or mutation fails.

Fusion result weights charge every actual pair. Removal money uses `base × enchantment weight × level weight × profile multiplier`, then the configured per-overcap-level surcharge. Level tables use floor lookup; values beyond the last entry use the last weight, while levels before the first use the documented fallback.
Socket expansion uses the same structured floor-weight approach keyed by resulting capacity, allowing progressively larger Rivet/item/money requirements without an expression evaluator.

## Duplicate protection

Tracked kinds include `GEAR`, `RUNIC_ERASER`, `RUNIC_CONDUIT`, and `RUNIC_RIVET`; ordinary books and currencies are not tracked. Physical player/ender inventories, stable physical storage identities, double containers, retained opened storage, nested containers, dropped items, and debounced scans retain quarantine/revocation/rekey behavior and persistent sightings.

Detection is observational. It cannot prove detection of copies that are never simultaneously visible (for example, an offline inventory and an unopened chest), and arbitrary virtual inventories owned by other plugins are not globally enumerable.

## API compatibility

`InfinityGearService` is registered through Bukkit’s service manager. It provides immutable inspection snapshots, profile resolution, gear/artifact creation, explicit mutation results and reason/message codes, enchantment application, socket inspection, duplicate/quarantine status, and eligible policy queries. Mutation methods require the primary server thread.

`com.infinitypickaxes.api.InfinityPickaxesAPI` and legacy model/manager accessors remain deprecated adapters. They represent only `infinitygear:pickaxe`; non-pickaxe profiles return empty/null instead of masquerading as pickaxes. Generalized `GearEnchantChangeEvent` fires alongside compatible legacy pickaxe events for migrated pickaxe operations, and cancellation remains authoritative.

## Commands and permissions

- `/igear`: read-only held gear overview (`infinitygear.use`).
- `/igear give <profile> <player> [level]`: create profile gear.
- `/igear artifact <runic_eraser|runic_conduit|runic_rivet> <player>`: issue a tracked singleton.
- `/igear station bind <type>`, `status`, and `unbind`: manage exact proprietary station instances.
- `/igear station <runic-table|fusion-altar|gear-forge>`: administrator/test GUI bypass.
- `/igear reload`, `setlevel`, `addxp`, `duplicate ...`, and `migration`: administration and diagnostics.
- `/ipickaxe` and `/infinitypickaxe` remain deprecated aliases with `infinitypickaxes.*` compatibility permissions. Generic `/pickaxe` is intentionally not claimed.

See `plugin.yml` for granular give, artifact, reload, station, migration, duplicate, and station-bypass permissions. Locale/config synchronization only adds missing defaults and does not overwrite administrator edits.

## Build and verification

```bash
./gradlew test
./gradlew build
```

The production artifact is `build/libs/InfinityGear-2.0.0-SNAPSHOT.jar`. Unit tests cover pure enchantment/fusion/cost/transform/socket rules, compensation, inventory capacity, PDC parsing, data-folder idempotency, SQLite record migration, duplicate scanner hardening, GUI cancellation, commands, and legacy behavior. A real Paper test server is still required to exercise plugin lifecycle, EcoEnchants/Nexo/Vault event timing, rendered menus, persistence across restarts, and production inventory interaction end to end.
