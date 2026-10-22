# CobbleAutoSell

A small addon for [CobbleDollars](https://modrinth.com/mod/cobbledollars) that adds an **Auto Seller** block. Put items in it (or feed it with a hopper) and it sells them automatically at the CobbleDollars bank prices. No more trips to the shop.

## Features

- **Auto Seller block**: a hopper-friendly block with 5 slots. Hoppers can insert items, but cannot pull them back out.
- **Sells at a threshold you choose**: the block sells everything inside once the total value reaches your set amount (1 to 1,000,000,000).
- **Uses the CobbleDollars bank list**: item prices come from the CobbleDollars bank. Items missing from the bank fall back to a configurable price list.
- **Owner gets paid**: the money goes to the player who placed the block, even when that player is offline. Only the owner can change the threshold.
- **Handy GUI** with three tabs:
  - **Sell**: shows the value of the items inside and lets you set the sell threshold.
  - **Items**: searchable list of every sellable item with its price.
  - **Top**: leaderboard of the players who earned the most through Auto Sellers.

## Crafting

Shaped recipe: 8 diamond blocks around 1 ender pearl in the center.

## Requirements

- Minecraft 1.21.1
- [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
- [Cobblemon](https://cobblemon.com/)
- [CobbleDollars](https://modrinth.com/mod/cobbledollars) (not bundled, install it separately)

The mod is needed on both the server and the clients.

## Installation

1. Install Fabric for Minecraft 1.21.1.
2. Put the requirements listed above in your `mods` folder.
3. Download `cobble-autosell-<version>.jar` from the [Releases](../../releases) page and put it in the same `mods` folder.

## Configuration

On first launch the mod creates `config/cobbleautosell/fallback_bank.json`. This is the fallback price list for items that are not in the CobbleDollars bank. The CobbleDollars bank always takes priority. Edit the file and restart the game or server to change prices.

```json
{
  "bank": [
    { "item": "minecraft:diamond", "price": 100 }
  ]
}
```

## Building from source

Requires JDK 21.

1. Copy the CobbleDollars jar into the `libs/` folder (it is not included in this repository because it may not be redistributed).
2. Run:

```bash
./gradlew clean build
```

On Windows use `gradlew.bat clean build`. The mod jar ends up in `build/libs/` (use the one without `-sources`).

## License

Released under the [MIT License](LICENSE). You are free to use, modify and redistribute the code, as long as you keep the copyright notice and license text.

Credits: made by Alessandro De Jongh.

CobbleDollars and Cobblemon belong to their respective authors and are not covered by this license.
