package wsmc;

public final class ArgHolder<T> {
	private final ThreadLocal<T> wsAddress =
			ThreadLocal.withInitial(() -> null);

	private final boolean nullable;

	private ArgHolder(boolean nullable) {
		this.nullable = nullable;
	}

	public static <T> ArgHolder<T> nullable() {
		return new ArgHolder<>(true);
	}

	public static <T> ArgHolder<T> nonnull() {
		return new ArgHolder<>(false);
	}

	public void push(T val) {
		if (this.wsAddress.get() != null) {
			throw new RuntimeException("Previous WebsocketUriHolder.wsAddress has not been used!");
		}

		this.wsAddress.set(val);
	}

	/**
	 * Get the value
	 * @return the value set previously
	 */
	public T peek() {
		T ret = this.wsAddress.get();

		if (ret == null && !nullable) {
			throw new RuntimeException("WebsocketUriHolder.wsAddress is not available!");
		}

		return ret;
	}

	/**
	 * Get the value and then reset the context.
	 * @return the value set previously
	 */
	public T pop() {
		T ret = this.peek();
		this.wsAddress.set(null);
		return ret;
	}
}
