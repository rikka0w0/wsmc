package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import wsmc.IConnectionEx;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(ServerStatusPinger.class)
public class MixinServerStatusPinger {
	@Inject(method = "pingServer", require = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/Connection;connectToServer(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/debugchart/LocalSampleLogger;)Lnet/minecraft/network/Connection;"))
	public void beforeCallConnect(CallbackInfo callback, @Local(ordinal = 0, argsOnly = false) ServerAddress serverAddress) {
		IWebSocketServerAddress wsAddress = IWebSocketServerAddress.from(serverAddress);
		IConnectionEx.connectToServerArg.push(wsAddress);
	}
}
