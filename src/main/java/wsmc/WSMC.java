package wsmc;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class WSMC {
	public static final String MODID = "wsmc";
	private static boolean debug = false;

	// Configs
	public static Configs configs = null;

	public static void initConfig() {
		WSMC.configs = AutoConfig.register(Configs.class, JanksonConfigSerializer::new).getConfig();
	}

	public static void debug(String msg) {
		if (!debug) return;
		System.out.println("[WSMC] " + msg);
	}

	public static boolean debug() {
		return debug;
	}
}
