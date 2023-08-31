package proxy.androidx.test.core.app;

import android.content.Context;

import proxy.Bridge;

public final class ApplicationProvider {
    private ApplicationProvider() {
    }

    @SuppressWarnings("unchecked")
    public static Context getApplicationContext() {
        return Bridge.getInstance().getContext();
    }
}
