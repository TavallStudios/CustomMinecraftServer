package com.customminecraftserver.bootstrap;

import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.configuration.ServerSettingsLoader;

import java.nio.file.Path;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        Path settingsPath = args.length == 0 ? Path.of("server-settings.json") : Path.of(args[0]);
        ServerSettings settings = new ServerSettingsLoader().load(settingsPath);

        try (ServerBootstrap bootstrap = new ServerBootstrap(settings)) {
            ServerConsole console = new ServerConsole(new ConsoleContext(bootstrap));
            Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("server-shutdown").unstarted(() -> {
                console.close();
                bootstrap.close();
            }));
            bootstrap.start();
            console.start();
            bootstrap.awaitShutdown();
            console.close();
        }
    }

    private static final class ConsoleContext implements ServerConsoleContext {
        private final ServerBootstrap bootstrap;

        private ConsoleContext(ServerBootstrap bootstrap) {
            this.bootstrap = bootstrap;
        }

        @Override
        public ServerConsoleSnapshot snapshot() {
            return bootstrap.snapshot();
        }

        @Override
        public java.util.List<String> activeSessionSummaryLines() {
            return bootstrap.activeSessionSummaryLines();
        }

        @Override
        public void requestStop() {
            bootstrap.close();
        }
    }
}
