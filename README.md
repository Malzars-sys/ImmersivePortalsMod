Update: this repo is not being maintained now. Forks are welcomed.

# Immersive Portals Mod

## Minecraft 26.1 Port

This branch contains an in-progress port to Minecraft 26.1. The build toolchain
and dependencies target Minecraft 26.1, Fabric Loader 0.19.3, Fabric API
0.145.1+26.1, Gradle 9.4, and Java 25.

The port is not yet playable. Minecraft 26.1 replaced major parts of the
rendering and chunk-loading systems used by Immersive Portals, so those systems
still need dedicated migration work before the project can produce a working
mod jar.

It's a Minecraft mod that provides see-through portals and seamless teleportation. It also can create "Non-Euclidean" (Uneuclidean) space effect.

![immptl.png](https://i.loli.net/2021/09/30/chHMG45dsnZNqep.png)

[On CurseForge](https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod)     [On Modrinth](https://modrinth.com/mod/immersiveportals)     [Website](https://qouteall.fun/immptl/)

This mod changes a lot of underlying Minecraft mechanics. This mod allows the client to load multiple dimensions at the same time and synchronize remote world information(blocks/entities) to client. It can render portal-in-portals. The portal rendering is roughly compatible with some versions of Sodium and Iris. The portal can transform player scale and gravity direction.  [Implementation Details](https://qouteall.fun/immptl/wiki/Implementation-Details)

(This is the Fabric version of Immersive Portals. [The Forge version](https://github.com/iPortalTeam/ImmersivePortalsModForNeo))

## API

This mod also provides some API for:

* Manage see-through portals
* Dynamically add dimensions
* Synchronize remote chunks to client
* Render the world into GUI
* Other utilities

[API description](https://qouteall.fun/immptl/wiki/API-for-Other-Mods.html).

## How to run this code

Install Java 25, then run:

```shell
./gradlew build
```

See the [Fabric 26.1 porting guide](https://docs.fabricmc.net/develop/porting/)
for the toolchain and API changes introduced by Minecraft 26.1.

## Other

[Wiki](https://qouteall.fun/immptl/wiki/)

[Discord Server](https://discord.gg/BZxgURK)

[Support qouteall on Patreon](https://www.patreon.com/qouteall)

