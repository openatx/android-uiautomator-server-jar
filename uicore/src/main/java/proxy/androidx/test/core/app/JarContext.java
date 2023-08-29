package proxy.androidx.test.core.app;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;

public class JarContext extends ContextWrapper {
    public JarContext() {
        super(null);
    }

    @Override
    public Object getSystemService(String name) {
        switch (name) {
            case Context.POWER_SERVICE:
            case Context.CLIPBOARD_SERVICE:
            case Context.CONNECTIVITY_SERVICE:
            case Context.TELEPHONY_SERVICE:
            case Context.WIFI_SERVICE:
            case Context.DISPLAY_SERVICE:
        }
        return null;
    }

}
