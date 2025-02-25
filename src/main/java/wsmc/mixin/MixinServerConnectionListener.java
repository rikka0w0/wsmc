package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import net.minecraft.network.Connection;

import wsmc.HttpGetSniffer;
import wsmc.IConnectionEx;

@Debug(export = true)
@Mixin(targets="net.minecraft.server.network.ServerConnectionListener$1")
public class MixinServerConnectionListener {
	@Inject(at = @At("RETURN"), method = "initChannel", require = 1)
	private void initChannel(CallbackInfo callback,
			@Local(ordinal = 0, argsOnly = true) Channel channel,
			@Local(ordinal = 0, argsOnly = false) Connection connection) {
		ChannelPipeline pipeline = channel.pipeline();
		IConnectionEx connectionEx = (IConnectionEx) connection;

		// Server side
		pipeline.addAfter("timeout", "WsmcHttpGetSniffer", new HttpGetSniffer(connectionEx::setWsHandshakeRequest));
	}
}
