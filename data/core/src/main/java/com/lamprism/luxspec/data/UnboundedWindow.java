package com.lamprism.luxspec.data;

/**
 * Requests a complete collection where the application role permits it.
 *
 * @author RollW
 */
public final class UnboundedWindow implements QueryWindow {
    private static final UnboundedWindow INSTANCE = new UnboundedWindow();

    private UnboundedWindow() {
    }

    /**
     * Returns the shared unbounded window.
     *
     * @return the unbounded window
     */
    public static UnboundedWindow getInstance() {
        return INSTANCE;
    }
}
