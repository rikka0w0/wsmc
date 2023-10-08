package wsmc;

public class WSMC {
	public static final String MODID = "wsmc";
	private static boolean debug =
			System.getProperty("wsmc.debug", "false").equalsIgnoreCase("true");
	public static boolean dumpBytes =
			System.getProperty("wsmc.dumpBytes", "false").equalsIgnoreCase("true");

	public static void debug(String msg) {
		if (!debug) return;
		System.out.println("[WSMC D] " + msg);
	}

	public static void info(String msg) {
		System.out.println("[WSMC I] " + msg);
	}

	public static boolean debug() {
		return debug;
	}
}
