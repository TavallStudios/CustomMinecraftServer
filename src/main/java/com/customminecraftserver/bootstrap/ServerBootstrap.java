package com.customminecraftserver.bootstrap;

import com.customminecraftserver.bedrock.BedrockDatagramHandler;
import com.customminecraftserver.bedrock.BedrockJwtVerifier;
import com.customminecraftserver.bedrock.BedrockLoginCoordinator;
import com.customminecraftserver.bedrock.OfflineBedrockLoginAdmission;
import com.customminecraftserver.bedrock.OnlineBedrockLoginAdmission;
import com.customminecraftserver.configuration.AuthMode;
import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.javaedition.JavaConnectionHandler;
import com.customminecraftserver.javaedition.JavaEncryptionChallengeFactory;
import com.customminecraftserver.javaedition.JavaLoginCoordinator;
import com.customminecraftserver.javaedition.MojangJavaSessionService;
import com.customminecraftserver.javaedition.OfflineJavaLoginAdmission;
import com.customminecraftserver.javaedition.OnlineJavaLoginAdmission;
import com.customminecraftserver.logging.StructuredConnectionLogger;
import com.customminecraftserver.networking.ProtocolVersionDetector;
import com.customminecraftserver.session.ConnectionSession;
import com.customminecraftserver.session.ConnectionSessionRegistry;

import java.util.Comparator;
import java.util.List;

public final class ServerBootstrap implements AutoCloseable {
    private final ServerSettings settings;
    private final StructuredConnectionLogger logger;
    private final ConnectionSessionRegistry registry;
    private final JavaTcpServerBootstrap javaBootstrap;
    private final BedrockUdpServerBootstrap bedrockBootstrap;
    private volatile boolean running;

    public ServerBootstrap(ServerSettings settings) {
        this.settings = settings;
        this.logger = new StructuredConnectionLogger();
        this.registry = new ConnectionSessionRegistry();
        ProtocolVersionDetector protocolVersionDetector = new ProtocolVersionDetector();
        JavaEncryptionChallengeFactory challengeFactory = JavaEncryptionChallengeFactory.createGenerated(
                settings.javaAuthentication().rsaKeySizeBits()
        );
        JavaLoginCoordinator loginCoordinator = new JavaLoginCoordinator(
                new OfflineJavaLoginAdmission(),
                new OnlineJavaLoginAdmission(challengeFactory)
        );

        JavaConnectionHandler javaHandler = new JavaConnectionHandler(
                settings,
                registry,
                logger,
                protocolVersionDetector,
                loginCoordinator,
                new MojangJavaSessionService(settings.javaAuthentication())
        );
        BedrockJwtVerifier bedrockJwtVerifier = new BedrockJwtVerifier();
        BedrockLoginCoordinator bedrockLoginCoordinator = new BedrockLoginCoordinator(
                new OfflineBedrockLoginAdmission(bedrockJwtVerifier),
                new OnlineBedrockLoginAdmission(bedrockJwtVerifier, settings.bedrockAuthentication())
        );
        BedrockDatagramHandler bedrockHandler = new BedrockDatagramHandler(
                settings,
                registry,
                logger,
                protocolVersionDetector,
                bedrockLoginCoordinator
        );
        this.javaBootstrap = new JavaTcpServerBootstrap(settings, javaHandler);
        this.bedrockBootstrap = new BedrockUdpServerBootstrap(settings, bedrockHandler);
    }

    public void start() throws InterruptedException {
        logger.serverStartup(settings);
        if (settings.authMode() == AuthMode.OFFLINE) {
            logger.warning(null, "offline_mode_enabled", java.util.Map.of("message", "Server is running in OFFLINE auth mode for MVP testing"));
        } else {
            logger.warning(null, "online_mode_partial", java.util.Map.of(
                    "message", "ONLINE auth mode is enabled. Java session verification is active; Bedrock secure-session handshake remains a documented next step."
            ));
        }
        bedrockBootstrap.start();
        javaBootstrap.start();
        logger.serverStarted(settings);
        running = true;
    }

    public void awaitShutdown() throws InterruptedException {
        javaBootstrap.awaitClose();
        bedrockBootstrap.awaitClose();
    }

    public ServerConsoleSnapshot snapshot() {
        return new ServerConsoleSnapshot(
                settings.host(),
                settings.javaTcpPort(),
                settings.bedrockUdpPort(),
                settings.authMode(),
                registry.activeSessions().size(),
                running
        );
    }

    public List<String> activeSessionSummaryLines() {
        return registry.activeSessions().stream()
                .sorted(Comparator.comparing(ConnectionSession::sessionId))
                .map(this::describeSession)
                .toList();
    }

    @Override
    public void close() {
        running = false;
        bedrockBootstrap.close();
        javaBootstrap.close();
    }

    private String describeSession(ConnectionSession session) {
        StringBuilder description = new StringBuilder()
                .append("sessionId=").append(session.sessionId())
                .append(" edition=").append(session.edition())
                .append(" remote=").append(session.remoteAddress())
                .append(" state=").append(session.state());
        if (session.protocolVersion() != null) {
            description.append(" protocolVersion=").append(session.protocolVersion());
        }
        if (session.protocolFamily() != null) {
            description.append(" protocolFamily=").append(session.protocolFamily());
        }
        if (session.username() != null) {
            description.append(" username=").append(session.username());
        }
        if (session.authenticatedIdentity() != null) {
            description.append(" authenticatedIdentity=").append(session.authenticatedIdentity());
        }
        if (session.authenticatedXuid() != null) {
            description.append(" authenticatedXuid=").append(session.authenticatedXuid());
        }
        return description.toString();
    }
}
