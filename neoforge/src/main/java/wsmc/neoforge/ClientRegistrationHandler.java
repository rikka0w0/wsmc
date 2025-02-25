package wsmc.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import wsmc.WSMC;

@EventBusSubscriber(value = Dist.CLIENT, modid = WSMC.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ClientRegistrationHandler {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		if (!WSMC.debug())
			return;

		wsmc.UnitTest.testParser();
	}
}
