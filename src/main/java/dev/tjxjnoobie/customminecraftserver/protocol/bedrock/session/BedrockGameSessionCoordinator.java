package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockAuthenticatedIdentity;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginPayload;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockGamePacketWriter;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramTransport;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockGameSessionCoordinator {
    private final StructuredConnectionLogger logger;
    private final ProtocolVersionDetector versionDetector;
    private final BedrockLoginCoordinator loginCoordinator;
    private final BedrockDatagramTransport transport;
    private final BedrockSecureSessionCoordinator secureSessionCoordinator;

    public BedrockGameSessionCoordinator(
            StructuredConnectionLogger logger,
            ProtocolVersionDetector versionDetector,
            BedrockLoginCoordinator loginCoordinator,
            BedrockDatagramTransport transport
    ) {
        this.logger = logger;
        this.versionDetector = versionDetector;
        this.loginCoordinator = loginCoordinator;
        this.transport = transport;
        BedrockInitializationCoordinator initializationCoordinator = new BedrockInitializationCoordinator(logger, transport);
        this.secureSessionCoordinator = new BedrockSecureSessionCoordinator(logger, transport, initializationCoordinator);
    }

    public void handleBatchPayload(ChannelHandlerContext context, ConnectionSession session, BedrockPeerSession peerSession, ByteBuf payload) {
        if (peerSession.secureSession() != null) {
            secureSessionCoordinator.handleEncryptedBedrockBatch(context, session, peerSession, payload);
            return;
        }

        List<ByteBuf> packets = BedrockRakNetCodec.readBatch(payload.copy(), peerSession.compressionNegotiated());
        for (ByteBuf packet : packets) {
            try {
                handleGamePacket(context, session, peerSession, packet);
            } finally {
                packet.release();
            }
        }
    }

    private void handleGamePacket(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int packetId = BedrockRakNetCodec.readUnsignedVarInt(packet);
        switch (packetId) {
            case BedrockPacketIds.BEDROCK_REQUEST_NETWORK_SETTINGS -> handleRequestNetworkSettings(context, session, peerSession, packet);
            case BedrockPacketIds.BEDROCK_LOGIN -> handleLogin(context, session, peerSession, packet);
            default -> logger.warning(session, "bedrock_game_packet_unsupported", Map.of(
                    "packetId", packetId,
                    "payloadHex", previewHex(packet, 96)
            ));
        }
    }

    private void handleRequestNetworkSettings(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int clientProtocol = packet.readInt();
        session.protocolVersion(clientProtocol);
        session.protocolFamily(versionDetector.detectBedrock(clientProtocol).family());
        logger.inboundPacket(session, "REQUEST_NETWORK_SETTINGS", Map.of(
                "bedrockStage", "REQUEST_NETWORK_SETTINGS",
                "clientProtocol", clientProtocol
        ));

        ByteBuf batch = transport.wrapBatch(BedrockGamePacketWriter.networkSettings(context.alloc()), false);
        transport.sendFrame(context, peerSession, batch, BedrockDatagramTransport.RELIABILITY_RELIABLE_ORDERED);
        peerSession.compressionNegotiated(true);
        logger.outboundPacket(session, "NETWORK_SETTINGS", Map.of("responseType", "NETWORK_SETTINGS"));
    }

    private void handleLogin(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int protocolVersion = packet.readInt();
        int tokenLength = BedrockRakNetCodec.readUnsignedVarInt(packet);
        ByteBuf loginTokens = packet.readSlice(tokenLength);
        String identity = BedrockRakNetCodec.readLittleString(loginTokens);
        String client = BedrockRakNetCodec.readLittleString(loginTokens);
        BedrockLoginPayload loginPayload = new BedrockLoginPayload(protocolVersion, identity, client);

        session.protocolVersion(protocolVersion);
        session.protocolFamily(versionDetector.detectBedrock(protocolVersion).family());
        transition(session, SessionState.BEDROCK_LOGIN, Map.of("bedrockStage", "LOGIN"));

        try {
            BedrockLoginDecision decision = loginCoordinator.decide(session.authMode(), session, loginPayload);
            BedrockAuthenticatedIdentity authenticatedIdentity = decision.authenticatedIdentity();
            applyAuthenticatedIdentity(session, authenticatedIdentity);
            logLogin(session, protocolVersion);

            if (session.authMode() == AuthMode.ONLINE) {
                transition(session, SessionState.BEDROCK_AUTHENTICATING, Map.of("reason", "ONLINE_AUTH_VERIFIED"));
            }

            if (authenticatedIdentity == null || authenticatedIdentity.handshakePublicKey() == null || authenticatedIdentity.handshakePublicKey().isBlank()) {
                throw new IllegalStateException("Bedrock login did not provide a client handshake public key");
            }

            peerSession.secureSession(BedrockSecureSession.create(authenticatedIdentity.handshakePublicKey()));
            peerSession.terminalMessage(decision.disconnectMessage());
            transition(session, SessionState.BEDROCK_SECURE_SESSION, Map.of("reason", "SECURE_SESSION_REQUIRED"));
            secureSessionCoordinator.sendServerHandshake(context, session, peerSession);
        } catch (RuntimeException exception) {
            logger.warning(session, "bedrock_login_failed", Map.of("message", rootMessage(exception)));
            secureSessionCoordinator.sendDisconnect(context, session, peerSession, "Bedrock ONLINE auth failed: " + rootMessage(exception));
        }
    }

    private void applyAuthenticatedIdentity(ConnectionSession session, BedrockAuthenticatedIdentity authenticatedIdentity) {
        if (authenticatedIdentity == null) {
            return;
        }
        session.username(authenticatedIdentity.displayName());
        session.authenticatedIdentity(authenticatedIdentity.identity());
        session.authenticatedXuid(authenticatedIdentity.xuid());
    }

    private void logLogin(ConnectionSession session, int protocolVersion) {
        LinkedHashMap<String, Object> loginDetails = new LinkedHashMap<>();
        loginDetails.put("bedrockStage", "LOGIN");
        loginDetails.put("clientProtocol", protocolVersion);
        loginDetails.put("username", session.username());
        if (session.authenticatedXuid() != null) {
            loginDetails.put("xuid", session.authenticatedXuid());
        }
        logger.inboundPacket(session, "LOGIN", loginDetails);
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
    }

    private String previewHex(ByteBuf payload, int maxBytes) {
        return ByteBufUtil.hexDump(payload, payload.readerIndex(), Math.min(payload.readableBytes(), maxBytes));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}


