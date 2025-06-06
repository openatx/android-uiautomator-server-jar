package com.wetest.uia2;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.wetest.uia2.stub.AutomatorHttpServer;
import com.wetest.uia2.stub.AutomatorService;
import com.wetest.uia2.stub.AutomatorServiceImpl;
import com.wetest.uia2.stub.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

import uiautomator.InstrumentShellWrapper;


public class Main {
    // http://www.jsonrpc.org/specification#error_object
    private static final int CUSTOM_ERROR_CODE = -32001;

    public static void main(String... args) {
        try {
            runServer(9008);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void setupTmpdir() {
        File tmpDir = new File("/data/local/tmp/u2");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        Ln.i("tmpdir is " + tmpDir.getAbsolutePath());
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());
    }

    public static void runServer(int port) throws Exception {
        Ln.i("[UiAutomator2Server] Starting Server");
        // make sure Looper.prepareMainLooper() is called
        InstrumentShellWrapper.getInstance().getContext();

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

        setupTmpdir();

        AutomatorHttpServer server = new AutomatorHttpServer(port);
        server.route("/jsonrpc/0", jrs);
        server.start();

        Ln.i("http server listening on *:" + port);
        while (server.isAlive()) {
            Thread.sleep(500);
        }
    }
}
