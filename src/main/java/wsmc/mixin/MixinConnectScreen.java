package wsmc.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import wsmc.IWebSocketServerAddress;

@Debug(export = true)
@Mixin(ConnectScreen.class)
public class MixinConnectScreen {
	@Shadow
	@Final
	static Logger LOGGER;

	@Inject(at = @At("HEAD"), method = "connect", require = 1, cancellable = true)
	private void connect(CallbackInfo callback, @Local(ordinal = 0, argsOnly = true) final ServerAddress serverAddress) {
		if (!IWebSocketServerAddress.from(serverAddress).isVanilla())
			LOGGER.info("Connecting to Websocket server: " + serverAddress.toString());
	}
}
