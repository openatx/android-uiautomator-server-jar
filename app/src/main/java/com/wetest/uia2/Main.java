package com.wetest.uia2;

import android.os.SystemClock;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.wetest.uia2.stub.AutomatorHttpServer;
import com.wetest.uia2.stub.AutomatorService;
import com.wetest.uia2.stub.AutomatorServiceImpl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

import uiautomator.InstrumentShellWrapper;


public class Main {
    private static final int LAUNCH_TIMEOUT = 5000;
    // http://www.jsonrpc.org/specification#error_object
    private static final int CUSTOM_ERROR_CODE = -32001;

    public static void main(String... args) throws Exception {
//        ServerInstrumentation serverInstrumentation = ServerInstrumentation.getInstance();
//        try {
//            while (!serverInstrumentation.isServerStopped()) {
//                SystemClock.sleep(1000);
//                serverInstrumentation.startMjpegServer();
//                serverInstrumentation.startServer();
//            }
//        } catch (SessionRemovedException e) {
//            //Ignoring SessionRemovedException
//        }
        Ln.i("[UiAutomator2Server] Starting Server");
        int PORT = 9008;

        InstrumentShellWrapper.getInstance().getContext();
        UiDevice device = UiDevice.getInstance(InstrumentShellWrapper.getInstance());
        device.wakeUp();

        JsonRpcServer jrs = new JsonRpcServer(new ObjectMapper(), new AutomatorServiceImpl(), AutomatorService.class);
        jrs.setShouldLogInvocationErrors(true);
        jrs.setErrorResolver(new ErrorResolver() {
            @Override
            public JsonError resolveError(Throwable throwable, Method method, List<JsonNode> list) {
                String data = throwable.getMessage();
                if (!throwable.getClass().equals(UiObjectNotFoundException.class)) {
                    throwable.printStackTrace();
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    data = sw.toString();
                }
                return new JsonError(CUSTOM_ERROR_CODE, throwable.getClass().getName(), data);
            }
        });

        AutomatorHttpServer server = new AutomatorHttpServer(PORT);
        server.route("/jsonrpc/0", jrs);
        server.start();
    }
}
