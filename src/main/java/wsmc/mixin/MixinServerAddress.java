package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(ServerAddress.class)
public class MixinServerAddress implements IWebSocketServerAddress {
	@Shadow
	@Final
	private HostAndPort hostAndPort;

	@Unique
	private String scheme = null;

	@Unique
	private String path = null;

	@Override
	public void setSchemeAndPath(String scheme, String path) {
		this.path = path;
		this.scheme = scheme;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}

	@Inject(at = @At("HEAD"), method = "toString", cancellable = true)
	private void toStringCustom(CallbackInfoReturnable<String> callback) {
		if (!this.isVanilla()) {
			// Skip vanilla logic if scheme is non-null
			String hostName = this.asServerAddress().getHost();
			int port = this.asServerAddress().getPort();
			callback.setReturnValue(this.scheme + "://" + hostName + ":" + port + this.path);
		}
	}

	@Inject(at = @At("HEAD"), method = "equals", cancellable = true)
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

				// Scheme and path must equal
				if (!ws.getScheme().equalsIgnoreCase(scheme) || !ws.getPath().equalsIgnoreCase(path))
					callback.setReturnValue(false);

				// Let vanilla code check for hostAndPort
				return;
			}

			callback.setReturnValue(false);
		}
	}

	@Inject(at = @At("HEAD"), method = "hashCode", cancellable = true)
	private void hashCodeCustom(CallbackInfoReturnable<Integer> callback) {
		if (!this.isVanilla()) {
			// We are modded, cover scheme and path in the hash
			int hash = Objects.hashCode(this.hostAndPort, this.scheme, this.path);
			callback.setReturnValue(hash);
		}
	}

	@Inject(at = @At("HEAD"), method = "parseString", cancellable = true)
	private static void parseString(String uriString, CallbackInfoReturnable<ServerAddress> callback) {
		ServerAddress serverAddress = IWebSocketServerAddress.fromWsUri(uriString);
		if (serverAddress != null)
			callback.setReturnValue(serverAddress);
	}

	@Inject(at = @At("HEAD"), method = "isValidAddress", cancellable = true)
	private static void isValidAddress(String uriString, CallbackInfoReturnable<Boolean> callback) {
		if (IWebSocketServerAddress.fromWsUri(uriString) != null)
			callback.setReturnValue(true);
	}
}
