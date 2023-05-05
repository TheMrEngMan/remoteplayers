# RemotePlayers

RemotePlayers is a Minecraft mod that pulls data from a [dynmap](https://github.com/webbukkit/dynmap) server into [Xaero's minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap). By asynchronously fetching data over a REST API, and displaying it through hooks into Xaero96's class files via [Mixins](https://github.com/SpongePowered/Mixin).

What it looks like:

![Comparing in-game waypoint to Dynmap player marker](/images/showcase.png)

## Setup
Requires both Xaero's world map and minimap mods.

Map names in Xaero's world map must match the Dynmap world names in order for in-game waypoints to be shown.

Example setup:

![Map names must match Dynmap world names](/images/maps_setup.png)

## Commands
`/remoteplayers info <player name>` get info about a player

`/remoteplayers list` get info about all players

## Development

This mod can be built by cloning this repository, then running:

```sh
./gradlew build
```

The resulting mod files are stored in `build/libs/`
