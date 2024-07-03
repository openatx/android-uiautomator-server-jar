package com.wetest.uia2;

import android.os.SystemClock;

import io.appium.uiautomator2.common.exceptions.SessionRemovedException;
import io.appium.uiautomator2.server.ServerInstrumentation;

public class Main {
    public static void main(String... args) throws Exception {
        ServerInstrumentation serverInstrumentation = ServerInstrumentation.getInstance();
        Ln.i("[AppiumUiAutomator2Server] Starting Server");
        try {
            while (!serverInstrumentation.isServerStopped()) {
                SystemClock.sleep(1000);
                serverInstrumentation.startMjpegServer();
                serverInstrumentation.startServer();
            }
        } catch (SessionRemovedException e) {
            //Ignoring SessionRemovedException
        }
    }
}
