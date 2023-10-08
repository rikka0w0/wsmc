package wsmc.mixin;

import java.net.InetSocketAddress;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.netty.handler.codec.http.HttpRequest;
import net.minecraft.network.Connection;
import net.minecraft.util.SampleLogger;

import wsmc.IConnectionEx;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(Connection.class)
public class MixinConnection implements IConnectionEx {
	@Unique
	private IWebSocketServerAddress wsInfo = null;

	@Unique
	private HttpRequest wsHandshakeRequest = null;

	/*
	 * Prior to the invocation of connectToServer(), call {@link wsmc.ArgHolder.connectToServerArg.push}
	 * to get the ServerAddress-sensitive(IWebSocketServerAddress) version of connectToServer().
	 */
	@Inject(method = "connectToServer", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/Connection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;"))
	private static void beforeCallConnect(InetSocketAddress socketAddress, boolean preferEPoll,
			SampleLogger logger, CallbackInfoReturnable<Connection> callback, Connection connection) {
		IWebSocketServerAddress wsAddress = IConnectionEx.connectToServerArg.pop();
		IConnectionEx con = (IConnectionEx) connection;
		con.setWsInfo(wsAddress);
	}

	@Override
	public IWebSocketServerAddress getWsInfo() {
		return this.wsInfo;
	}

	@Override
	public void setWsInfo(IWebSocketServerAddress wsInfo) {
		this.wsInfo = wsInfo;
	}

	@Override
	public HttpRequest getWsHandshakeRequest() {
		return this.wsHandshakeRequest;
	}

	@Override
	public void setWsHandshakeRequest(HttpRequest wsHandshakeRequest) {
		this.wsHandshakeRequest = wsHandshakeRequest;
	}
}
