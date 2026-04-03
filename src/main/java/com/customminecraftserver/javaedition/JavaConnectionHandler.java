package com.customminecraftserver.javaedition;

import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.logging.StructuredConnectionLogger;
import com.customminecraftserver.networking.ProtocolVersionDetector;
import com.customminecraftserver.networking.ProtocolVersionDetector.JavaProtocolProfile;
import com.customminecraftserver.session.ConnectionSession;
import com.customminecraftserver.session.ConnectionSessionRegistry;
import com.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ChannelHandler.Sharable
public final class JavaConnectionHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ServerSettings settings;
    private final ConnectionSessionRegistry registry;
    private final StructuredConnectionLogger logger;
    private final ProtocolVersionDetector protocolVersionDetector;
    private final JavaLoginCoordinator loginCoordinator;
    private final JavaPostLoginCoordinator postLoginCoordinator;
    private final JavaOnlineAuthenticationCoordinator onlineAuthenticationCoordinator;

    public JavaConnectionHandler(
            ServerSettings settings,
            ConnectionSessionRegistry registry,
            StructuredConnectionLogger logger,
            ProtocolVersionDetector protocolVersionDetector,
            JavaLoginCoordinator loginCoordinator,
            JavaSessionService sessionService
    ) {
        this.settings = settings;
        this.registry = registry;
        this.logger = logger;
        this.protocolVersionDetector = protocolVersionDetector;
        this.loginCoordinator = loginCoordinator;
        this.postLoginCoordinator = new JavaPostLoginCoordinator(logger);
        this.onlineAuthenticationCoordinator = new JavaOnlineAuthenticationCoordinator(logger, sessionService, postLoginCoordinator);
    }

    public JavaConnectionHandler copy() {
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        ConnectionSession session = registry.registerJava(context.channel(), settings.authMode());
        context.channel().attr(JavaChannelState.SESSION_KEY).set(session);
        logger.connectionOpened(session);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, ByteBuf frame) {
        ConnectionSession session = JavaChannelState.requireSession(context);
        int packetId = MinecraftVarInt.read(frame);
        switch (session.state()) {
            case JAVA_HANDSHAKE -> handleHandshake(context, session, packetId, frame);
            case JAVA_STATUS -> handleStatus(context, session, packetId, frame);
            case JAVA_LOGIN -> handleLogin(context, session, packetId, frame);
            case JAVA_ENCRYPTION_NEGOTIATION -> onlineAuthenticationCoordinator.handleEncryptionResponse(context, session, packetId, frame);
            case JAVA_WAITING_LOGIN_ACK -> postLoginCoordinator.handleLoginAcknowledgement(context, session, packetId);
            case JAVA_CONFIGURING -> postLoginCoordinator.handleConfiguration(context, session, packetId);
            case JAVA_PLAY -> postLoginCoordinator.handlePlay(context, session, packetId, frame);
            case JAVA_WAITING_SETTINGS -> postLoginCoordinator.handleWaitingSettings(context, session, packetId, frame);
            case JAVA_WAITING_PONG -> postLoginCoordinator.handleWaitingPong(context, session, packetId, frame);
            default -> failUnsupportedState(context, session, packetId);
        }
    }

    private void handleHandshake(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId != JavaPacketIds.HANDSHAKE) {
            failAndClose(context, session, "Unexpected Java handshake packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        JavaHandshakePacket packet = JavaHandshakePacket.read(frame);
        JavaProtocolProfile profile = protocolVersionDetector.detectJava(packet.protocolVersion());
        session.protocolVersion(packet.protocolVersion());
        session.protocolFamily(profile.family());
        session.requestedHost(packet.serverAddress());
        session.requestedPort(packet.port());

        logger.inboundPacket(session, "HANDSHAKE", Map.of(
                "packetId", packetId,
                "requestedHost", packet.serverAddress(),
                "requestedPort", packet.port(),
                "nextState", packet.nextState(),
                "supported", profile.supported()
        ));

        SessionState nextState = packet.nextState() == JavaNextState.STATUS ? SessionState.JAVA_STATUS : SessionState.JAVA_LOGIN;
        transition(session, nextState, Map.of("nextState", packet.nextState()));
    }

    private void handleStatus(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId == JavaPacketIds.STATUS_REQUEST) {
            logger.inboundPacket(session, "STATUS_REQUEST", Map.of("packetId", packetId));
            logger.outboundPacket(session, "STATUS_RESPONSE", Map.of("responseType", "STATUS_RESPONSE"));
            context.writeAndFlush(JavaPacketWriter.statusResponse(context.alloc(), statusPayload(session)));
            return;
        }

        if (packetId == JavaPacketIds.STATUS_PING) {
            long payload = frame.readLong();
            logger.inboundPacket(session, "STATUS_PING", Map.of("packetId", packetId, "payload", payload));
            logger.outboundPacket(session, "STATUS_PONG", Map.of("responseType", "STATUS_PONG", "payload", payload));
            context.writeAndFlush(JavaPacketWriter.pong(context.alloc(), payload)).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
            transition(session, SessionState.JAVA_TERMINATED, Map.of("reason", "STATUS_PING_COMPLETED"));
            return;
        }

        failAndClose(context, session, "Unsupported Java status packet id " + packetId, Map.of("packetId", packetId));
    }

    private void handleLogin(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId != JavaPacketIds.LOGIN_START) {
            failAndClose(context, session, "Unsupported Java login packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        JavaLoginStartPacket packet = JavaLoginStartPacket.read(frame);
        session.username(packet.username());
        logger.inboundPacket(session, "LOGIN_START", loginDetails(packet));

        JavaLoginDecision decision = loginCoordinator.decide(session.authMode(), session, packet);
        if (decision.action() == JavaLoginAction.REQUEST_ENCRYPTION) {
            onlineAuthenticationCoordinator.requestEncryption(context, session, decision);
            return;
        }

        JavaChannelState.terminalMessage(context, decision.disconnectMessage());
        postLoginCoordinator.beginPostLoginFlow(context, session, "OFFLINE_LOGIN_ACCEPTED");
    }

    private Map<String, Object> loginDetails(JavaLoginStartPacket packet) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("packetId", JavaPacketIds.LOGIN_START);
        details.put("username", packet.username());
        if (packet.playerUuid() != null) {
            details.put("playerUuid", packet.playerUuid());
        }
        if (!packet.trailingBytesHex().isEmpty()) {
            details.put("trailingBytesHex", packet.trailingBytesHex());
        }
        return details;
    }

    private String statusPayload(ConnectionSession session) {
        String versionName = session.protocolFamily() == null ? "Custom Handshake MVP" : session.protocolFamily();
        int protocol = session.protocolVersion() == null ? -1 : session.protocolVersion();
        return "{"
                + "\"version\":{\"name\":\"" + versionName + "\",\"protocol\":" + protocol + "},"
                + "\"players\":{\"max\":" + settings.maxConnections() + ",\"online\":0},"
                + "\"description\":{\"text\":\"" + settings.motd() + "\"}"
                + "}";
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
    }

    private void failUnsupportedState(ChannelHandlerContext context, ConnectionSession session, int packetId) {
        failAndClose(context, session, "Unsupported Java session state " + session.state(), Map.of("packetId", packetId));
    }

    private void failAndClose(ChannelHandlerContext context, ConnectionSession session, String message, Map<String, ?> details) {
        logger.warning(session, "java_protocol_failure", merge(details, Map.of("message", message)));
        context.close();
    }

    private Map<String, ?> merge(Map<String, ?> first, Map<String, ?> second) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(first);
        merged.putAll(second);
        return merged;
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        postLoginCoordinator.cancelTimeouts(context);
        ConnectionSession session = context.channel().attr(JavaChannelState.SESSION_KEY).get();
        if (session != null) {
            session.state(SessionState.DISCONNECTED);
            logger.connectionClosed(session, "CHANNEL_INACTIVE");
        }
        registry.unregisterJava(context.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        ConnectionSession session = context.channel().attr(JavaChannelState.SESSION_KEY).get();
        logger.failure(session, "java_exception", cause, Map.of());
        context.close();
    }
}
