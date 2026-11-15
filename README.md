# RespawnBlocks

**Regenerating ores and blocks inside WorldGuard regions.**

Mark a WorldGuard region as a *respawn region* and every ore mined inside it turns into a placeholder block (bedrock by default). After a configurable time the original block comes back. Every block has its own timer. Ideal for mine islands, public mines, prison mines and resource worlds.

- **Minecraft:** 1.16.5 up to the latest version
- **Server software:** Spigot, Paper and Paper forks (Purpur, Pufferfish, ...)
- **Java:** 8 or newer (whatever your Minecraft version needs)
- **Required:** WorldGuard 7.x, which itself needs WorldEdit or FastAsyncWorldEdit

---

## Features

### Per-block respawning
- When a respawn block is mined, a placeholder appears right away. The original block returns once its own timer runs out.
- The exact original block comes back, including its block state (rotation, lit redstone ore, ...).
- Every block type has its own respawn time, and optionally its own placeholder and drop mode.
- Comes with defaults for all ores: overworld, deepslate, copper, nether quartz, nether gold and ancient debris.
- Any block can be a respawn block, not just ores. Logs, stone, crops, you name it.
- A small effect (sound and block particles) plays when a block respawns. You can configure or disable it.

### Chunk-safe and restart-safe
Blocks in unloaded chunks are a classic problem for regenerating mines. RespawnBlocks handles them like this:
- **It never force-loads chunks.** A block only respawns while its chunk is loaded.
- When a block's time runs out in an unloaded chunk, it waits and respawns **as soon as that chunk loads again**.
- All waiting blocks are saved to `data.yml`: on shutdown and every minute (configurable). Placeholders survive restarts and crashes, and still turn back into the original block.
- Respawn times use real time, so server downtime counts towards the timer.
- On shutdown, placeholders in loaded chunks can be restored straight away (`respawn-loaded-on-shutdown`).

### Placeholder protection
Placeholders can't be removed by:
- players in survival mode
- explosions (TNT, creepers, beds, ...)
- pistons
- fire
- mobs such as endermen, withers and ravagers

If something else ends up on a placeholder's spot (for example when an admin rebuilds the mine), that block is left alone and not overwritten.

### Normal mining, nothing hacked around it
The break itself is a normal vanilla break, so these keep working:
- Fortune, Silk Touch, tool durability, experience and Mending
- statistics
- other plugins that react to block breaks, such as jobs and mcMMO

### Drop modes
| Mode | What the player gets |
|---|---|
| `NATURAL` | Normal vanilla drops on the ground |
| `INVENTORY` | Normal vanilla drops straight into the inventory (overflow drops on the ground) |
| `BLOCK` | The block itself in the inventory, like the classic MineIsland plugin |

Set a global default in `config.yml` and override it per block if you want. In `INVENTORY` and `BLOCK` mode, experience is given directly to the player.

### WorldGuard integration
- Works with any WorldGuard region, in any world.
- Mines are usually protected with `build deny`. With `override-protection: true`, respawn blocks can still be mined while everything else stays protected.
- With `block-other-breaks: true`, only respawn blocks can be mined inside a respawn region. Other blocks are blocked and the player gets a (configurable) message.
- Respawn regions can be disabled temporarily without removing them.

### In-game GUI (`/rb gui`)
- **Main menu:** open the block and region menus, turn the plugin on or off, respawn all waiting blocks at once, and reload.
- **Blocks menu:**
  - Shows all respawn blocks with their time, placeholder, drop mode and status.
  - **Add a block** by clicking it in your own inventory.
  - Shift + right-click removes a block.
- **Block editor:**
  - Change the respawn time with −60 / −10 / −1 / +1 / +10 / +60 second buttons.
  - Set the placeholder by clicking a block in your inventory. Right-click resets it to the default.
  - Cycle through the drop modes.
  - Enable or disable the block, or remove it.
- **Regions menu:**
  - Shows every WorldGuard region in a world.
  - Click a region to make it a respawn region, and click again to enable or disable it.
  - Shift + right-click removes it.
  - Switch worlds, and filter on "respawn regions only".
  - Regions that no longer exist in WorldGuard are marked, so you can clean them up.

### Commands with tab completion
Everything can also be done with commands. Tab completion suggests:
- sub commands
- WorldGuard regions in your current world (only ones that aren't respawn regions yet when adding)
- worlds
- block names
- common respawn times

### Fully configurable texts
- Every chat message, the prefix and every GUI text live in `messages.yml`.
- Supports `&` color codes and hex colors (`&#RRGGBB`).
- Leave a message empty (`''`) to not send it at all.

---

## Installation

1. Install **WorldEdit** (or FastAsyncWorldEdit) and **WorldGuard**.
2. Put `RespawnBlocks.jar` in your `plugins` folder and restart the server.
3. Create a WorldGuard region around your mine:
   ```
   //wand
   /rg define mine
   ```
4. Make it a respawn region:
   ```
   /rb region add mine
   ```
   Or open `/rb gui` → **Respawn regions** and click the region.
5. Mine an ore inside the region. It turns into bedrock and comes back after its respawn time.

---

## Commands

Main command: `/respawnblocks`. Aliases: `/rb` and `/mineisland`.

| Command | Description |
|---|---|
| `/rb` or `/rb gui` | Open the management GUI |
| `/rb help` | Show the help page |
| `/rb region add <region> [world]` | Mark a WorldGuard region as respawn region |
| `/rb region remove <region> [world]` | Unmark a respawn region |
| `/rb region toggle <region> [world]` | Enable or disable a respawn region |
| `/rb region list [world]` | List the respawn regions of a world |
| `/rb block add <block\|hand> [seconds]` | Add a respawn block (`hand` = the block you're holding) |
| `/rb block remove <block>` | Remove a respawn block |
| `/rb block settime <block> <seconds>` | Change the respawn time of a block |
| `/rb block list` | List all respawn blocks |
| `/rb respawnall [region]` | Respawn all waiting blocks right now (optionally only in one region) |
| `/rb toggle` | Turn the whole plugin on or off |
| `/rb reload` | Reload all configuration files |

When `[world]` is left out, your current world is used.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `respawnblocks.admin` | op | All commands and the GUI (includes `respawnblocks.reload`) |
| `respawnblocks.reload` | op | Only `/rb reload` |
| `respawnblocks.bypass` | op | Break *any* block inside respawn regions. In creative mode, also break placeholders, which cancels their respawn |

Normal players need **no** permission to mine respawn blocks. Mining is controlled by your regions.

---

## Configuration files

All files are in `plugins/RespawnBlocks/`.

| File | Contents | Edit by hand? |
|---|---|---|
| `config.yml` | General settings | Yes, then `/rb reload` |
| `messages.yml` | Prefix, messages, GUI texts | Yes, then `/rb reload` |
| `blocks.yml` | Respawn blocks and their settings | Yes, or use the GUI or commands |
| `regions.yml` | Respawn regions per world | Yes, or use the GUI or commands |
| `data.yml` | Blocks currently waiting to respawn | **No**, managed by the plugin |

### config.yml

```yaml
enabled: true                  # master switch
default-placeholder: BEDROCK   # block shown while waiting to respawn
default-respawn-time: 60       # seconds, for blocks added without a time
drop-mode: INVENTORY           # NATURAL | INVENTORY | BLOCK
block-other-breaks: true       # only respawn blocks can be mined in respawn regions
override-protection: true      # mine respawn blocks even if WorldGuard denies building
ignore-creative: true          # creative players break blocks normally
check-interval-ticks: 20       # how often respawns are checked (20 ticks = 1 second)
autosave-minutes: 1            # how often waiting blocks are saved to data.yml
respawn-loaded-on-shutdown: true

respawn-effect:
  sound: BLOCK_STONE_PLACE     # sound name or key (block.stone.place), NONE to disable
  volume: 0.6
  pitch: 1.0
  particles: true
```

### blocks.yml

```yaml
blocks:
  DIAMOND_ORE:
    respawn-time: 180          # seconds
  EMERALD_ORE:
    respawn-time: 240
    placeholder: STONE         # optional: own placeholder
    drop-mode: BLOCK           # optional: own drop mode
  OAK_LOG:
    respawn-time: 30
    enabled: false             # optional: temporarily disabled
```

Blocks that don't exist on your Minecraft version are skipped, for example deepslate ores on 1.16. They stay in the file, so they start working once you update your server.

### regions.yml

```yaml
regions:
  world:
    mine: true                 # enabled
    vip_mine: false            # disabled
  world_nether:
    nether_mine: true
```

### messages.yml

Every text can be changed. Placeholders such as `%region%`, `%block%`, `%time%`, `%world%` and `%amount%` are filled in by the plugin. The time format (for example `1m 30s`) can be changed under `time:`.

---

## How it works (technical)

1. **Player breaks a block.** RespawnBlocks checks whether the block is inside an enabled respawn region (one WorldGuard region lookup) and whether it is a respawn block.
2. **The break goes ahead normally.** If WorldGuard or another plugin cancelled it and `override-protection` is on, it is allowed again. This only happens for respawn blocks in respawn regions.
3. **The block is registered:** its position, original block data, placeholder, region and respawn time.
4. **One tick later** the empty spot is filled with the placeholder.
5. **Every second** (`check-interval-ticks`), all waiting blocks whose time is up *and* whose chunk is loaded are put back.
6. **When a chunk loads**, waiting blocks in that chunk whose time is up are put back one tick later.
7. **On shutdown and every minute**, waiting blocks are saved to `data.yml` and loaded again on startup.

Only the placeholder (or air or a liquid) is ever replaced. If something else was built on the spot in the meantime, it is left alone.

---

## FAQ

**Players get "Hey! Sorry, but you can't break that block here." while mining.**
That message comes from WorldGuard when the region has `build deny`. The ore is still mined because of `override-protection`. To get rid of the message, allow breaking in the region and let RespawnBlocks block everything else:
```
/rg flag mine block-break allow
```
Keep `block-other-breaks: true`.

**A placeholder is stuck and doesn't respawn.**
Its chunk is probably not loaded. It respawns as soon as a player comes near. To force it, use `/rb respawnall`. Admins with `respawnblocks.bypass` in creative mode can also break the placeholder to cancel its respawn.

**Can I use a different placeholder than bedrock?**
Yes. Change `default-placeholder` in `config.yml`, or set one per block in the GUI by clicking a block in your inventory.

**Does it work with FastAsyncWorldEdit?**
Yes. RespawnBlocks only talks to WorldGuard, and WorldGuard works with FAWE.

**I'm coming from the old MineIsland plugin.**
Add your old mine regions again with `/rb region add <region>`. `/mineisland` still works as an alias. Use `drop-mode: BLOCK` to keep the old behaviour where players receive the ore block itself.

---

## Building from source

```bash
./gradlew build
```

The jar ends up in `build/libs/`. The build:
- compiles against the Spigot **1.16.5** API with Java 8 bytecode, so it runs on every supported version
- also compiles against the newest Paper API (`checkLatestApi`), to make sure nothing the plugin uses was removed

A test server can be started with `./gradlew runServer`.
