package wsmc.mixin;


import java.net.InetSocketAddress;
import java.util.Optional;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;

import wsmc.IConnectionEx;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreenThread {
	@Unique
	private ServerAddress serverAddress;

	@Inject(method = "<init>", at = @At("RETURN"))
	protected void init(ConnectScreen connectScreen, String str, ServerAddress serverAddress,
			Minecraft minecraft, ServerData serverData, CallbackInfo callback) {
		// This will remain constant
		this.serverAddress = serverAddress;
	}

	@Inject(method = "run", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/Connection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;"))
	public void beforeCallConnect(CallbackInfo callback, InetSocketAddress inetsocketaddress,
			Optional<InetSocketAddress> optional, Connection connection) {
		IWebSocketServerAddress wsAddress = IWebSocketServerAddress.from(serverAddress);
		IConnectionEx con = (IConnectionEx) connection;
		con.setWsInfo(wsAddress);
	}
}
