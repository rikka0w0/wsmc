package wsmc.client;

import wsmc.ArgHolder;
import wsmc.IWebSocketServerAddress;

public interface IConnectionEx {
	public final static ArgHolder<IWebSocketServerAddress> connectToServerArg = ArgHolder.nullable();

	IWebSocketServerAddress getWsInfo();
	void setWsInfo(IWebSocketServerAddress wsInfo);
}
