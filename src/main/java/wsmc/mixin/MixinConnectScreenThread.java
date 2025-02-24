package wsmc.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import wsmc.IConnectionEx;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreenThread {
	@Unique
	private ServerAddress serverAddress;

	@Inject(method = "<init>", at = @At("RETURN"), require = 1)
	protected void init(ConnectScreen connectScreen, String str, ServerAddress serverAddress,
			Minecraft minecraft, CompletableFuture<?> dummyFuture, CallbackInfo callback) {
		// This will remain constant
		this.serverAddress = serverAddress;
	}

	@Inject(method = "run", locals = LocalCapture.CAPTURE_FAILHARD, require = 1, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/Connection;connectToServer(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/Connection;"))
	public void beforeCallConnectToServer(CallbackInfo callback) {
		IWebSocketServerAddress wsAddress = IWebSocketServerAddress.from(serverAddress);
		IConnectionEx.connectToServerArg.push(wsAddress);
	}
}
