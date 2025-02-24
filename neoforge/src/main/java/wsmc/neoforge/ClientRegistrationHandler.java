package wsmc.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import wsmc.WSMC;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = WSMC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistrationHandler {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		if (!WSMC.debug())
			return;

		wsmc.UnitTest.testParser();
	}
}
