package com.customminecraftserver.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ServerConsole implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConsole.class);

    private final ServerConsoleCommandProcessor commandProcessor;
    private volatile boolean running;
    private Thread consoleThread;

    public ServerConsole(ServerConsoleContext context) {
        this.commandProcessor = new ServerConsoleCommandProcessor(context);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        consoleThread = Thread.ofPlatform()
                .name("server-console")
                .daemon(true)
                .start(this::runLoop);
    }

    @Override
    public void close() {
        running = false;
        if (consoleThread != null) {
            consoleThread.interrupt();
        }
    }

    private void runLoop() {
        LOGGER.info("Console ready. Type help for commands.");
        printPrompt();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (running) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException exception) {
                LOGGER.warn("Console input closed unexpectedly.", exception);
                return;
            }

            if (line == null) {
                LOGGER.info("Console input closed. Server will keep running until it is stopped externally.");
                return;
            }

            ServerConsoleCommandResult result = commandProcessor.handle(line);
            if (!result.message().isBlank()) {
                LOGGER.info(result.message());
            }
            if (result.stopRequested()) {
                return;
            }
            printPrompt();
        }
    }

    private void printPrompt() {
        System.out.print("custommc> ");
        System.out.flush();
    }
}
