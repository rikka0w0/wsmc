package wsmc.mixin;

import com.mojang.brigadier.CommandDispatcher;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TransferCommand;
import wsmc.WsmcTransferCommand;

@Mixin(TransferCommand.class)
public class MixinTransferCommand {
	@Inject(method = "register", at = @At("TAIL"), require = 1)
	private static void registerWebSocketTransfer(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
		WsmcTransferCommand.register(dispatcher);
	}
}
