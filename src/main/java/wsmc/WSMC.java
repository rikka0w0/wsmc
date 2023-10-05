package wsmc;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class WSMC {
	public static final String MODID = "wsmc";

	// Configs
	public static Configs configs = null;

	public static void initConfig() {
		WSMC.configs = AutoConfig.register(Configs.class, JanksonConfigSerializer::new).getConfig();
	}
}
