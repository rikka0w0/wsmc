# WSMC
Enable Websocket support for Minecraft Java.
Since most CDN providers(at least for their free tier) do not support raw TCP proxy, with the help of this mod, the owner can now hide the server behind a CDN and let the players connect via WebSocket, thus preventing DDoS attacks.

For Minecraft 1.20.2, Forge and Fabric.

This branch is for 1.20.2,
hopefully it will work in future versions (although very unlikely).

## When this mod is installed on a server:
* The server would allow players to connect via WebSocket.
* Players can still join using vanilla TCP.
* The server accepts and handles TCP and WebSocket connection on the same listening port.
* Without installing this mod on the client side, a player can still join a server that has this mod using vanilla TCP.

## When this mod is installed on a client:
* The client can join WebSocket-enabled servers.
* The client can join any servers using vanilla TCP.

## Note
* This mod can be installed on both 
* Only the Lan server owner need to install this mod.
* Vanilla clients can join your server even if you install this mod, note that other mods you have may prevent vanilla clients from joining.
* Installing this mod does not prevent you from joining other vanilla or mod servers.
* If a player joins via CDN-proxied WebSocket, the server can still get the real IP of the player.

## Milestones
1. Server listens for WebSocket connections. (Done)
2. Client connects to a WebSocket server.
3. Server acquires client statistics (e.g. real IP).

## Dependencies
### Forge Version
* `netty-codec-http` for handling HTTP and WebSocket

### Fabric Version
You need to install Fabric Loader and then install this mod. Fabric API is optional but highly recommended.

## For developers
To modify and debug the code, first import the "forge" or "fabric" folder as a Gradle project in Eclipse IDE, and then run the gradle task `genEclipseRuns`.

Windows users need to replace `./` and `../` with `.\` and `..\`, respectively.

The codebase uses Minecraft official mapping.

### Compile Fabric artifact
```
git clone https://github.com/rikka0w0/wsmc.git
cd wsmc/fabric
./gradlew build
```

To debug in Fabric, one may need to create `run/config/fabric_loader_dependencies.json` with the following content:
```
{
  "version": 1,
  "overrides": {
    "wsmc": {
      "-depends": {
        "minecraft": "IGNORED",
        "fabricloader": "IGNORED"
      }
    }
  }
} 
```

### Compile Forge artifact
```
git clone https://github.com/rikka0w0/wsmc.git
cd wsmc/forge
./gradlew build
```

### To specify JRE path (Since 1.18.1, Minecraft requires Java 17):
```
./gradlew -Dorg.gradle.java.home=/path_to_jdk_directory <commands>
```
