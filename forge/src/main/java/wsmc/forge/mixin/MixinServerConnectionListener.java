package wsmc.forge.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.Channel;

import wsmc.HttpGetSniffer;

@Debug(export = true)
@Mixin(targets="net.minecraft.server.network.ServerConnectionListener$1")
public class MixinServerConnectionListener {
	@Inject(at = @At("HEAD"), method = "initChannel")
	private void initChannel(Channel channel, CallbackInfo callback) {
		// Server side
		channel.pipeline().addFirst("WsmcHttpGetSniffer", new HttpGetSniffer());
	}
}
