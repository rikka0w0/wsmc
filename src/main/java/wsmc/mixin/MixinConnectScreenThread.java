package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.multiplayer.resolver.ServerAddress;

import wsmc.IConnectionEx;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreenThread {
	@Unique
	private ServerAddress serverAddress;

	@Inject(method = "<init>", at = @At("RETURN"), require = 1)
	protected void init(CallbackInfo callback, @Local(ordinal = 0, argsOnly = true) ServerAddress serverAddress) {
		// This will remain constant
		this.serverAddress = serverAddress;
	}

	@Inject(method = "run", require = 1, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/Connection;connectToServer(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/Connection;"))
	public void beforeCallConnect(CallbackInfo callback) {
		IWebSocketServerAddress wsAddress = IWebSocketServerAddress.from(serverAddress);
		IConnectionEx.connectToServerArg.push(wsAddress);
	}
}
