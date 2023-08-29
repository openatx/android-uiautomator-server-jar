package proxy.androidx.test.core.app;

import android.content.Context;

public final class ApplicationProvider {
    private static JarContext mContext = new JarContext();
    private ApplicationProvider() {
    }

    /**
     * Returns the application {@link Context} for the application under test.
     *
     * @see {@link Context#getApplicationContext()}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Context> T getApplicationContext() {
        return (T) mContext;
    }
}
