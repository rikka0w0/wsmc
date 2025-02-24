package wsmc.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import net.minecraft.network.Connection;

import wsmc.HttpGetSniffer;
import wsmc.IConnectionEx;

@Debug(export = true)
@Mixin(targets="net.minecraft.server.network.ServerConnectionListener$1")
public class MixinServerConnectionListener {
	@Inject(at = @At("RETURN"), method = "initChannel", locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
	private void initChannel(Channel channel, CallbackInfo callback,
			int maxPacketPerSecond, Connection connection) {
		IConnectionEx connectionEx = (IConnectionEx) connection;
		ChannelPipeline pipeline = channel.pipeline();

		// Server side
		pipeline.addAfter("timeout", "WsmcHttpGetSniffer", new HttpGetSniffer(connectionEx::setWsHandshakeRequest));
	}
}
