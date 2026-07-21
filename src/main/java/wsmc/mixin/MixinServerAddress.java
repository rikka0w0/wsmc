package wsmc.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.net.HostAndPort;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import wsmc.IWebSocketServerAddress;
import wsmc.WebSocketConnectionInfo;

@Debug(export = true)
@Mixin(ServerAddress.class)
public class MixinServerAddress implements IWebSocketServerAddress {
	@Shadow
	@Final
	private HostAndPort hostAndPort;

	@Unique
	@Nullable
	private WebSocketConnectionInfo connInfo;

	@Redirect(
		method = "<init>(Ljava/lang/String;I)V",
		at = @At(value = "INVOKE", target = "Lcom/google/common/net/HostAndPort;fromParts(Ljava/lang/String;I)Lcom/google/common/net/HostAndPort;"),
		require = 1
	)
	private static HostAndPort parseWebSocketUriForConstructor(String host, int port) {
		WebSocketConnectionInfo connInfo = WebSocketConnectionInfo.parseUri(host);
		if (connInfo != null)
			return HostAndPort.fromParts(connInfo.uri.getHost(), connInfo.uri.getPort());

		return HostAndPort.fromParts(host, port);
	}

	@Inject(method = "<init>(Ljava/lang/String;I)V", at = @At("RETURN"), require = 1)
	private void initWebSocketConnectionInfo(String host, int port, CallbackInfo callback) {
		WebSocketConnectionInfo connInfo = WebSocketConnectionInfo.parseUri(host);
		if (connInfo != null)
			this.connInfo = connInfo;
	}

	@Override
	public void setWsConnectionInfo(@Nullable WebSocketConnectionInfo connInfo) {
		this.connInfo = connInfo;
	}

	@Override
	public WebSocketConnectionInfo getWsConnectionInfo() {
		return this.connInfo;
	}

	@Override
	public String getRawHost() {
		return hostAndPort.getHost();
	}

	@Inject(at = @At("HEAD"), method = "toString", require = 1, cancellable = true)
	private void toStringCustom(CallbackInfoReturnable<String> callback) {
		if (!this.isVanilla()) {
			callback.setReturnValue(this.connInfo.toString());
		}
	}

	@Inject(at = @At("HEAD"), method = "equals", require = 1, cancellable = true)
	private void equalsCustom(Object object, CallbackInfoReturnable<Boolean> callback) {
		// Redirects the vanilla equals();

		if (this == object)
			callback.setReturnValue(true);

		if (this.isVanilla()) {
			// We are vanilla

			if (object instanceof IWebSocketServerAddress) {
				IWebSocketServerAddress ws = (IWebSocketServerAddress) object;

				// We are vanilla but input is modded
				if (!ws.isVanilla())
					callback.setReturnValue(false);
			}

			// Let vanilla code check for hostAndPort
			return;
		} else {
			// We are modded

			if (object instanceof IWebSocketServerAddress) {
				IWebSocketServerAddress ws = (IWebSocketServerAddress) object;

				if (ws.isVanilla())
					callback.setReturnValue(false);

				if (!this.connInfo.equalTo(ws.getWsConnectionInfo()))
					callback.setReturnValue(false);

				// Let vanilla code check for hostAndPort
				return;
			}

			callback.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "hashCode", require = 1, cancellable = true)
	private void hashCodeCustom(CallbackInfoReturnable<Integer> callback) {
		if (!this.isVanilla()) {
			// We are modded, cover hostname, port, and connInfo in the hash
			int hash = this.getWsConnectionInfo().hashCode();
			callback.setReturnValue(hash);
		}
	}

	@Inject(at = @At("HEAD"), method = "parseString", require = 1, cancellable = true)
	private static void parseString(String uriString, CallbackInfoReturnable<ServerAddress> callback) {
		WebSocketConnectionInfo connInfo = WebSocketConnectionInfo.parseUri(uriString);
		if (connInfo != null) {
			ServerAddress serverAddress = new ServerAddress(connInfo.uri.getHost(), connInfo.uri.getPort());
			((IWebSocketServerAddress)(Object)serverAddress).setWsConnectionInfo(connInfo);
			callback.setReturnValue(serverAddress);
		}
	}

	@Inject(at = @At("HEAD"), method = "isValidAddress", require = 1, cancellable = true)
	private static void isValidAddress(String uriString, CallbackInfoReturnable<Boolean> callback) {
		if (WebSocketConnectionInfo.parseUri(uriString) != null)
			callback.setReturnValue(true);
	}
}
