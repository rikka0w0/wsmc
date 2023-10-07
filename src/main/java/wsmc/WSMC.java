package wsmc;

public class WSMC {
	public static final String MODID = "wsmc";
	private static boolean debug = false;

	public static void debug(String msg) {
		if (!debug) return;
		System.out.println("[WSMC] " + msg);
	}

	public static boolean debug() {
		return debug;
	}
}
