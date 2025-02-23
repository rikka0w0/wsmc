package wsmc;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * This class defines additional information added to the vanilla {@link ServerAddress}.
 * <p>
 * You are not supposed to compare two {@link WebSocketConnectionInfo} instances directly.
 * <p>
 * Compare vanilla {@link ServerAddress} instances instead.
 * <p>
 * Both vanilla {@link ServerAddress} and this class are immutable.
 */
public class WebSocketConnectionInfo {
	public final ServerAddress owner;
	public final String path;
	public final String scheme;

	private WebSocketConnectionInfo(ServerAddress owner, String scheme, String path) {
		this.owner = owner;
		this.scheme = scheme;
		this.path = path;
	}

	/**
	 * Compare our unique fields only.
	 * <p>
	 * Does not check the owner ({@link ServerAddress}).
	 * <p>
	 * Vanilla ServerAddress checks Host and port.
	 */
	public boolean equalTo(WebSocketConnectionInfo other) {
		if (other == null)
			return false;

		if (!this.path.equals(other.path))
			return false;

		if (!this.scheme.equals(other.scheme))
			return false;

		return true;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object){
			return true;
		} else if (!(object instanceof WebSocketConnectionInfo)){
			return false;
		}

		WebSocketConnectionInfo other = (WebSocketConnectionInfo) object;

		// Check vanilla fields
		if (!this.owner.getHost().equals(other.owner.getHost()))
			return false;

		if (this.owner.getPort() != other.owner.getPort())
			return false;

		return this.equalTo(other);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(
			this.owner.getPort(),
			this.owner.getHost(),
			this.scheme,
			this.path);
	}

	@Override
	public String toString() {
		IWebSocketServerAddress wsInfo = IWebSocketServerAddress.from(this.owner);
		return this.scheme + "://" + wsInfo.getRawHost() + ":" + this.owner.getPort() + this.path;
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
				if (scheme.equalsIgnoreCase("ws")) {
					port = 80;
				} else if (scheme.equalsIgnoreCase("wss")) {
					port = 443;
				} else {
					port = 25565;
				}
			}

			String path = uri.getPath();
			if (path == null) {
				path = "/";
			}

			ServerAddress result = new ServerAddress(hostname, port);
			WebSocketConnectionInfo connInfo = new WebSocketConnectionInfo(result, scheme, path);
			((IWebSocketServerAddress)(Object)result).setWsConnectionInfo(connInfo);
			return result;
		} catch (URISyntaxException e) {
		}

		return null;
	}

	public URI toURI() throws URISyntaxException {
		IWebSocketServerAddress wsInfo = IWebSocketServerAddress.from(this.owner);
		String hostName = wsInfo.getRawHost();
		int port = this.owner.getPort();

		URI uri = new URI(
			this.scheme,
			null,
			hostName,
			port,
			this.path,
			null,
			null);

		return uri;
	}
}
