# WSMC
Enable Websocket support for Minecraft Java.
Since most CDN providers(at least for their free tier) do not support raw TCP proxy, with the help of this mod, the owner can now hide the server behind a CDN and let the players connect via WebSocket, thus preventing DDoS attacks.

For Minecraft Forge, Neoforge and Fabric:
* 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4
* 1.20.2, 1.20.3, 1.20.4
* 1.20.1
* 1.19.2

This branch is for 1.20.1 only.

This mod runs standalone and does not have any dependency.

## When this mod is installed on a server:
* The server would allow players to connect via WebSocket.
* Players can still join using vanilla TCP.
* The server accepts and handles TCP and WebSocket connections on the same listening port.
* Without installing this mod on the client side, a player can still join a server that has this mod using vanilla TCP.
* The server can acquire client statistics (e.g., real IP) from the WebSocket handshake..

## When this mod is installed on a client:
* The client can join WebSocket-enabled servers using URI like `ws://hostname.com:port/path_to_minecraft_endpoint`.
* The client can join any servers using vanilla TCP using the old syntax, e.g. `hostname_or_ip:port`.

## Note
* This mod does not affect any gameplay.
* This mod does not modify any GUI.
* Vanilla clients can join your server even if you install this mod, note that other mods you have may prevent vanilla clients from joining.
* Installing this mod on your client does not prevent you from joining other vanilla or mod servers.
* The server can still get the real IP of the players who joined via CDN-proxied WebSocket.
* This mod is compatible with other TCP-WebSocket proxies, such as websocat.

## Client Options
Sometimes the DNS returns a slow IP for the HTTP hostname (ws) or the SNI (wss). The client may want to control how to resolve the IP address.

The client can optionally control the HTTP hostname and the SNI used during WebSocket handshake:
```
Insecure WebSocket connection with http hostname specified:
ws://host.com@ip.ip.ip.ip

Specify sni and http hostname to the same value(sni-host.com), resolve server IP from ip.ip.ip.ip:
wss://sni-host.com@ip.ip.ip.ip

Set sni and http hostname differently, resolve server IP from host.com:
wss://sni.com:@host.com[:port]

Set sni and http hostname differently, resolve server IP from sni.com:
wss://:host.com@sni.com[:port]

Set sni, http hostname, and the server address seperately
wss://sni.com:host.com@ip.ip.ip.ip
```

Port and path specification can be appended at the same time.

## Configuration
The configuration of this mod is passed in the "system properties". You can use `-D` in the JVM command line to pass such options.

| Property Key               | Type     | Usage                                                                                                                                                                                        | Side          | Default | Example  |
|----------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|---------|----------|
| wsmc.disableVanillaTCP     | boolean  | Disable vanilla TCP login and server status.                                                                                                                                                 | Server        | false   | true     |
| wsmc.wsmcEndpoint          | string   | Set the WebSocket Endpoint for Minecraft login and server status. If this property does not exist, a client can join the game via ANY WebSocket Endpoint. Must start with /, case-sensitive. | Server        | Not set | /mc      |
| wsmc.debug                 | boolean  | Show debug logs.                                                                                                                                                                             | Server Client | false   | true     |
| wsmc.dumpBytes             | boolean  | Dump raw WebSocket binary frames. Work only if `wsmc.debug` is set to `true`.                                                                                                                | Server Client | false   | true     |
| wsmc.maxFramePayloadLength | integer  | Maximum allowable frame payload length. Setting this value to your modpack's requirement else Netty will throw error "Max frame length of x has been exceeded".                              | Server Client | 65536   | 65536    |

## Dependencies
### Forge Version
* `netty-codec-http` for handling HTTP and WebSocket

### Fabric Version
You need to install Fabric Loader and then install this mod. Fabric API is optional but highly recommended.

## For developers
To modify and debug the code, first import the "forge" or "fabric" folder as a Gradle project in Eclipse IDE, and then run the gradle task `genEclipseRuns`.

Windows users need to replace `./` and `../` with `.\` and `..\`, respectively.

The codebase uses Minecraft official mapping.

On the server side, if a client joins via WebSocket, its handshake request can be accessed via the vanilla `net.minecraft.network.Connection` class.
To obtain such information, cast a Connection instance into IConnectionEx, then calls `IConnectionEx.getWsHandshakeRequest()`.

This is useful for obtaining information about the original request if the Minecraft server is behind a reverse proxy (e.g. a CDN).
For example, header `X-Forwarded-For` and `CF-IPCountry` indicate the client IP address and the client country code, respectively.

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
* Since 1.18.1, Minecraft requires Java 17
* Since 1.20.5, Minecraft requires Java 21
