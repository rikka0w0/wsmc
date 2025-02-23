package wsmc;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * This will be mixined into vanilla class {@link ServerAddress}.
 * Assumes only {@link ServerAddress} implements this interface.
 * <p>
 * You can cast between {@link ServerAddress} and this interface.
 */
public interface IWebSocketServerAddress {
	void setWsConnectionInfo(@Nullable WebSocketConnectionInfo connInfo);

	@Nullable
	WebSocketConnectionInfo getWsConnectionInfo();

	/**
	 * Unlike {@link ServerAddress#getHost()}, this function:
	 * @return the raw host name that may contain non-ascii characters.
	 */
	String getRawHost();

	default boolean isVanilla() {
		return getWsConnectionInfo() == null;
	}

	default ServerAddress asServerAddress() {
		return (ServerAddress) (Object) this;
	}

	public static IWebSocketServerAddress from(ServerAddress serverAddress) {
		return (IWebSocketServerAddress) (Object) serverAddress;
	}
}
