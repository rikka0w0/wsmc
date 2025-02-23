package wsmc.api;

import javax.annotation.Nullable;

import io.netty.handler.codec.http.HttpRequest;
import net.minecraft.network.Connection;

/**
 * This interface is mixined to {@link net.minecraft.network.Connection}.
 * So you can cast between {@link net.minecraft.network.Connection} and this interface.
 * <p>
 * On the server side, you can use this interface to retrieve the HTTP header during the
 * WebSocket handshake.
 * <p>
 * For instance, the server can get the real client IP from the HTTP header:
 * <pre>
 * MinecraftServer server = ...;
 * // Go through all established connections
 * for (Connection conn: server.getConnection().getConnections()) {
 * 	&#64;Nullable
 * 	HttpRequest httpRequest = IWebSocketServerConnection.of(conn).getWsHandshakeRequest();
 * 	if (httpRequest != null) {
 * 		&#64;Nullable
 * 		String clientRealIP = httpRequest.headers().get("X-Forwarded-For");
 * 		// Your logic
 * 	}
 * }
 * </pre>
 * This is extremely useful if the client connects to the server via a proxy.
 */
public interface IWebSocketServerConnection {
	/**
	 * Only available on the server side.
	 * @return the http request of the WebSocket handshake.
	 */
	@Nullable
	HttpRequest getWsHandshakeRequest();

	@Nullable
	public static IWebSocketServerConnection of(Connection connection) {
		return (IWebSocketServerConnection)(Object)connection;
	}
}
