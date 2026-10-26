# SkLeaderboards

A Skript addon designed to provide efficient, lag-free leaderboards with direct Skript variable and player stats support, all without placeholders.

## Why SkLeaderboards?

In Skript, holographic leaderboards are very inefficient and laggy. Scripts typically rely on console commands that directly update a hologram. Constantly updating these holograms, especially many at a time can cause large amounts of lag on a server.

SkLeaderboards is built to solve this problem. The plugin allows for simple configuration of leaderboards directly in Skript, with updating the data being fully left to the plugin. 

SkLeaderboard directly reads Skript variables, player statistics from data files and economy data directly from EssentialsX.

With SkLeaderboards, the host of the holograms can also be easily edited by one line and can be different for every single leaderboard.
> [!NOTE]
> Supported Holgram Hosts:                                                                                                                                                                       
> - Text Displays (Vanilla)                                                                                                                                                                      
> - FancyHolograms                                                                                                                                                                               
> - DecentHolograms

## Requirements

- Paper 1.21.x - 26.x
- Skript 2.14.x - 2.17.x
- Java 21+

## Installation

1. Download the latest release from [Modrinth](https://modrinth.com/plugin/skleaderboards).
2. Download the latest [Skript](https://github.com/SkriptLang/Skript/releases) release.
3. Download your preferred optional dependancies: [FancyHolograms](https://modrinth.com/plugin/fancyholograms), [DecentHolograms](https://modrinth.com/plugin/decentholograms), [EssentialsX](https://modrinth.com/plugin/essentialsx).
4. Drag all downloaded `.jars` into your server's `\plugins` folder and restart the server.

## Usage

| Command | Description |
|---|---|
| `/lb create <id>` | Create a leaderboard hologram that has been registered in a script |
| `/lb delete <id>` | Delete a leaderboard hologram |
| `/lb move <id>` | Move a leaderboard hologram that has already been created |
| `/lb refresh <all / id>` | Refresh all or a particular leaderboard hologram(s) |
| `/lb info <id>` | Display info about an existing leaderboard |
| `/lb list` | List all existing leaderboards and their locations |

## Snippets

```
register a new leaderboard with id "balanceLB":
    title: "&a&lBALANCE LEADERBOARD"
    value: balance
    host: fancy holograms
    size: 10
    refresh: 10 minutes
    format: "&7#%position% &f%name% &8- <#4FF522>$%value%"
    1st format: "&e#%position% &f%name% &8- <#4FF522>$%value%"
    2nd format: "&f#%position% &f%name% &8- <#4FF522>$%value%"
    3rd format: "&6#%position% &f%name% &8- <#4FF522>$%value%"
    footer: "&7Updates every %refresh%"
    shadow: false
    billboard: vertical
    background: none
    sort: descending
```

```
register a new leaderboard with id "testLB":
    title: "&c&lTEST LEADERBOARD&r"
    value: {variable::*}
    host: text display
    size: 10
    refresh: 60 seconds
    format: "&7#%position% &f%name% &8- <#4FF522>%value%"
    1st format: "&e#%position% &f%name% &8- <#4FF522>%value%"
    2nd format: "&f#%position% &f%name% &8- <#4FF522>%value%"
    3rd format: "&6#%position% &f%name% &8- <#4FF522>%value%"
    footer: "&7Updates every %refresh%"
```

# Support
- [Discord](https://discord.gg/yv685kSTqj)
- [GitHub Issues Tracker](https://github.com/skTanky/SkLeaderboards/issues)
