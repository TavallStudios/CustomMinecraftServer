package dev.tjxjnoobie.customminecraftserver.config;

public record ServerSettings(
        String host,
        int javaTcpPort,
        int bedrockUdpPort,
        String motd,
        int maxConnections,
        boolean structuredLoggingEnabled,
        AuthMode authMode,
        JavaAuthenticationSettings javaAuthentication,
        BedrockAuthenticationSettings bedrockAuthentication
) {
    public ServerSettings {
        host = host == null || host.isBlank() ? "0.0.0.0" : host;
        javaTcpPort = javaTcpPort <= 0 ? 25565 : javaTcpPort;
        bedrockUdpPort = bedrockUdpPort <= 0 ? 19132 : bedrockUdpPort;
        motd = motd == null || motd.isBlank() ? "Custom server handshake reached successfully" : motd;
        maxConnections = maxConnections <= 0 ? 128 : maxConnections;
        authMode = authMode == null ? AuthMode.OFFLINE : authMode;
        javaAuthentication = javaAuthentication == null ? JavaAuthenticationSettings.defaults() : javaAuthentication;
        bedrockAuthentication = bedrockAuthentication == null ? BedrockAuthenticationSettings.defaults() : bedrockAuthentication;
    }

    public static ServerSettings defaults() {
        return new ServerSettings(
                "0.0.0.0",
                25565,
                19132,
                "Custom server handshake reached successfully",
                128,
                true,
                AuthMode.OFFLINE,
                JavaAuthenticationSettings.defaults(),
                BedrockAuthenticationSettings.defaults()
        );
    }
}

