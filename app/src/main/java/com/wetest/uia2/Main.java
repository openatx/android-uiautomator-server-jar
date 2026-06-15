package com.wetest.uia2;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genymobile.scrcpy.Workarounds;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.wetest.uia2.stub.AutomatorHttpServer;
import com.wetest.uia2.stub.AutomatorService;
import com.wetest.uia2.stub.AutomatorServiceImpl;
import com.wetest.uia2.stub.Log;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

import uiautomator.InstrumentShellWrapper;


public class Main {
    // http://www.jsonrpc.org/specification#error_object
    private static final int CUSTOM_ERROR_CODE = -32001;
    private static final int DEFAULT_PORT = 9008;

    public static void main(String... args) {
        Options options = new Options();
        options.addOption("p", "port", true, "Port to listen on (1-65535, default: " + DEFAULT_PORT + ")");
        options.addOption("h", "help", false, "Print this help message");

        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("uiautomator2-server", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("uiautomator2-server", options);
            return;
        }

        int port = DEFAULT_PORT;
        if (cmd.hasOption("p")) {
            String portStr = cmd.getOptionValue("p");
            try {
                long parsed = Long.parseLong(portStr);
                if (parsed < 1 || parsed > 65535) {
                    System.err.println("Invalid port: " + portStr + ". Must be between 1 and 65535.");
                    System.exit(1);
                    return;
                }
                port = (int) parsed;
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + portStr + ". Must be an integer between 1 and 65535.");
                System.exit(1);
                return;
            }
        }

        try {
            runServer(port);
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
        Workarounds.apply(true, true);
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
