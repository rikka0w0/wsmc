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
	public final URI uri;
	public final String sni;
	public final String httpHostname;

	private WebSocketConnectionInfo(ServerAddress owner, URI uri,
		String sni, String httpHostname
	) {
		this.owner = owner;
		this.uri = uri;
		this.sni = sni;
		this.httpHostname = httpHostname;
	}

	/**
	 * Compare URI components, sni and httpHostname.
	 * <p>
	 * Does not check the owner ({@link ServerAddress}).
	 * <p>
	 * Vanilla ServerAddress checks Host and port.
	 */
	public boolean equalTo(WebSocketConnectionInfo that) {
		if (that == null)
			return false;

		if (!Objects.equal(this.uri, that.uri))
			return false;

		if (!Objects.equal(this.sni, that.sni))
			return false;

		if (!Objects.equal(this.httpHostname, that.httpHostname))
			return false;

		return true;
	}

	@Override
	public boolean equals(Object that) {
		if (this == that){
			return true;
		} else if (!(that instanceof WebSocketConnectionInfo)){
			return false;
		}

		WebSocketConnectionInfo other = (WebSocketConnectionInfo) that;

		// Check vanilla fields
		if (!IWebSocketServerAddress.from(this.owner).getRawHost().equals(
				IWebSocketServerAddress.from(other.owner).getRawHost()))
			return false;

		if (this.owner.getPort() != other.owner.getPort())
			return false;

		return this.equalTo(other);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(
			this.owner.getPort(),
			IWebSocketServerAddress.from(this.owner).getRawHost(),
			this.uri,
			this.sni,
			this.httpHostname);
	}

	@Override
	public String toString() {
		return this.uri.toString() + "\n" +
			"TLS SNI: " + this.sni + "\n" +
			"HTTP Hostname: " + this.httpHostname;
	}

	private static String[] splitUserInfo(@Nullable String userInfo) {
		if (null == userInfo)
			return new String[0];

		// Split the UserInfo string, preserve empty strings
		return userInfo.split(":", -1);
	}

	/**
	 * syntax: (wss|ws)://sni.com:host.com@ip.ip.ip.ip[:port][/path]
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

			String sni = null;
			String hostnameInHeader = null;

			if ("wss".equalsIgnoreCase(scheme)) {
				sni = hostname;
				hostnameInHeader = hostname;

				String[] splitted = splitUserInfo(uri.getUserInfo());
				if (splitted.length > 0) {
					sni = splitted[0];
					if (splitted.length == 1) {
						hostnameInHeader = sni;
					} else {
						hostnameInHeader = splitted[1];
					}

					if (hostnameInHeader.isEmpty()) {
						hostnameInHeader = hostname;
						if (sni.isEmpty()) {
							sni = hostname;
						}
					} else if (sni.isEmpty()) {
						sni = hostname;
					}
				}
			} else if ("ws".equals(scheme)) {
				hostnameInHeader = hostname;

				String[] splitted = splitUserInfo(uri.getUserInfo());
				if (splitted.length > 0 && !splitted[0].isBlank())
					hostnameInHeader = splitted[0];
			}

			uri = new URI(
				scheme,
				null,
				hostname,
				port,
				path,
				uri.getQuery(),
				uri.getFragment());

			ServerAddress result = new ServerAddress(hostname, port);
			WebSocketConnectionInfo connInfo = new WebSocketConnectionInfo(result, uri, sni, hostnameInHeader);
			((IWebSocketServerAddress)(Object)result).setWsConnectionInfo(connInfo);
			return result;
		} catch (URISyntaxException e) {
		}

		return null;
	}
}
