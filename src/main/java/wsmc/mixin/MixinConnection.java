package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import io.netty.handler.codec.http.HttpRequest;
import net.minecraft.network.Connection;
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
	@Inject(method = "connectToServer", require = 1,
		at = @At(value = "NEW", target = "()Lio/netty/bootstrap/Bootstrap;"))
	private static void beforeCallConnect(CallbackInfoReturnable<Connection> callback, @Local(ordinal = 0, argsOnly = false) Connection connection) {
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
