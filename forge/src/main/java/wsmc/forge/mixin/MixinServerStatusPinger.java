package wsmc.forge.mixin;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import wsmc.IWebSocketServerAddress;
import wsmc.client.IConnectionEx;

@Debug(export = true)
@Mixin(ServerStatusPinger.class)
public class MixinServerStatusPinger {
	@Inject(method = "pingServer", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/Connection;connectToServer(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/SampleLogger;)Lnet/minecraft/network/Connection;"))
	public void beforeCallConnect(ServerData serverData, Runnable runnable, CallbackInfo callback,
			ServerAddress serverAddress, Optional<InetSocketAddress> dummy1, InetSocketAddress dummy2) {
		IWebSocketServerAddress wsAddress = IWebSocketServerAddress.from(serverAddress);
		IConnectionEx.connectToServerArg.push(wsAddress);
	}
}
