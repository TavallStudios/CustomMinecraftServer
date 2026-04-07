package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;

public record ServerConsoleSnapshot(
        String host,
        int javaTcpPort,
        int bedrockUdpPort,
        AuthMode authMode,
        int activeSessions,
        boolean running
) {
}

