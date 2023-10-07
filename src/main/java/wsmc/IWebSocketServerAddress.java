package wsmc;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * This will be mixined into vanilla class {@link ServerAddress}.
 * Assumes only {@link ServerAddress} implements this interface.
 */
public interface IWebSocketServerAddress {
	void setSchemeAndPath(@Nullable String scheme, String path);

	String getPath();

	@Nullable
	String getScheme();

	default boolean isVanilla() {
		return getScheme() == null;
	}

	default ServerAddress asServerAddress() {
		return (ServerAddress) (Object) this;
	}

	public static IWebSocketServerAddress from(ServerAddress serverAddress) {
		return (IWebSocketServerAddress) (Object) serverAddress;
	}

	/**
	 *
	 * @param uriString
	 * @return null if uriString is not a valid WebSocket Uri (including vanilla TCP).
	 */
	@Nullable
	public static ServerAddress fromWsUri(String uriString) {
		try {
			URI uri = new URI(uriString);

			String scheme = uri.getScheme();
			String hostname = uri.getHost();

			if (hostname == null)
				return null;

			IDN.toASCII(hostname);

			if (scheme == null)
				return null;

			// If the scheme is null, treat as vanilla TCP connection.
			// If the scheme is ws or wss, treat as WebSocket connection.
			// Otherwise, unsupported.

			if (!scheme.equalsIgnoreCase("ws") && !scheme.equalsIgnoreCase("wss"))
				return null;

			int port = uri.getPort();
			if (port < 0 || port > 65535) {
				// Default port
				port = 25565;
			}

			String path = uri.getPath();
			if (path == null) {
				path = "/";
			}

			ServerAddress result = new ServerAddress(hostname, port);
			((IWebSocketServerAddress)(Object)result).setSchemeAndPath(scheme, path);
			return result;
		} catch (URISyntaxException e) {
		}

		return null;
	}
}
