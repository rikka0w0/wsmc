package wsmc.mixin;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(ConnectScreen.class)
public class MixinConnectScreen {
	@Shadow
	@Final
	static Logger LOGGER;

	@Inject(at = @At("HEAD"), method = "connect", cancellable = true)
	private void connect(final Minecraft mc, final ServerAddress serverAddress,
			@Nullable final ServerData serverData, CallbackInfo callback) {
		if (!IWebSocketServerAddress.from(serverAddress).isVanilla())
			LOGGER.info("Connecting to Websocket server: " + serverAddress.toString());
	}
}
