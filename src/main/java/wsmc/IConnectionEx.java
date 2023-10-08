package wsmc;

import javax.annotation.Nullable;

import io.netty.handler.codec.http.HttpRequest;

public interface IConnectionEx {
	public final static ArgHolder<IWebSocketServerAddress> connectToServerArg = ArgHolder.nullable();

	IWebSocketServerAddress getWsInfo();
	void setWsInfo(IWebSocketServerAddress wsInfo);

	void setWsHandshakeRequest(HttpRequest wsHandshakeRequest);

	/**
	 * Only available on the server side.
	 * @return the http request of the WebSocket handshake.
	 */
	@Nullable
	HttpRequest getWsHandshakeRequest();
}
