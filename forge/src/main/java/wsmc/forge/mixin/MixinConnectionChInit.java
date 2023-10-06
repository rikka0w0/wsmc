package wsmc.forge.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import wsmc.IWebSocketServerAddress;
import wsmc.client.IConnectionEx;

@Debug(export = true)
@Mixin(targets = "net.minecraft.network.Connection$1")
public class MixinConnectionChInit {
	@Unique
	private Connection connection;

	@Inject(method = "<init>", at = @At("RETURN"))
	protected void init(Connection connection, CallbackInfo callback) {
		// These will remain constant
		this.connection = connection;
	}

	@Inject(method = "initChannel", at = @At("RETURN"))
	protected void initChannel(Channel channel, CallbackInfo callback) {
		IConnectionEx connection = (IConnectionEx) this.connection;
		IWebSocketServerAddress wsInfo = connection.getWsInfo();
		// TODO: hookPipeline(channel, wsInfo);
	}
}
