# Fivem-Vehicle-Builder

Simple JavaFX tool to convert GTA V `.rpf` vehicle DLCs into FiveM-ready resources.

## Features
- Drag & drop `.rpf` files or extracted folders
- Uses `rpf-cli` to extract RPFs (including nested RPFs)
- Automatically builds FiveM structure:
  stream/vehicle_name/
  data/vehicle_name/
  fxmanifest.lua
- Supports multiple DLCs
- Single or multi resource export
- Optional ZIP export
- Dark mode UI

## Requirements
- Java 21+
- `rpf-cli.exe` (https://github.com/VIRUXE/rpf-cli)

## Usage
1. Start the app
2. Select `rpf-cli.exe`
3. Drag one or more `.rpf` files into the window
4. Click **Export**
5. Copy the exported resource into your FiveM `resources` folder

## Notes
- Only vehicle DLCs are supported
- Stream files (`.yft`, `.ytd`, `.ydd`, etc.) are extracted automatically
- Map / prop DLCs are not supported

## Credits
- https://github.com/VIRUXE/rpf-cli (rpf-cli)
