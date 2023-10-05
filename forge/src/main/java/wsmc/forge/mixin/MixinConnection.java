package wsmc.forge.mixin;


import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import wsmc.HttpGetSniffer;

@Debug(export = true)
@Mixin(Connection.class)
public class MixinConnection {
	@Inject(at = @At("HEAD"), method = "configureSerialization")
	private static void configureSerialization(ChannelPipeline pipeline, PacketFlow packetFlow,
			BandwidthDebugMonitor monitor, CallbackInfo callback) {
		if (packetFlow == PacketFlow.SERVERBOUND) {
			// Server side
			pipeline.addFirst("WsmcHttpGetSniffer", new HttpGetSniffer());
		} else {
			// TODO: Client side
		}
	}
}
