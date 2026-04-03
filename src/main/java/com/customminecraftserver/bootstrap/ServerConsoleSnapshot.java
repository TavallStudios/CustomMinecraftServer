package com.customminecraftserver.bootstrap;

import com.customminecraftserver.configuration.AuthMode;

public record ServerConsoleSnapshot(
        String host,
        int javaTcpPort,
        int bedrockUdpPort,
        AuthMode authMode,
        int activeSessions,
        boolean running
) {
}
