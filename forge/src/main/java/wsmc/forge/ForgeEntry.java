package wsmc.forge;

import net.minecraftforge.fml.common.Mod;

import wsmc.WSMC;

@Mod(WSMC.MODID)
public class ForgeEntry {
	public static ForgeEntry instance;

	public ForgeEntry() {
		if (instance == null)
			instance = this;
		else
			throw new RuntimeException("Duplicated Class Instantiation: net.mobz.forge.MobZ");
	}

	@Mod.EventBusSubscriber(modid = WSMC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public final static class ModEventBusHandler {

	}

	@Mod.EventBusSubscriber(modid = WSMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public final static class ForgeEventBusHandler {

	}
}
