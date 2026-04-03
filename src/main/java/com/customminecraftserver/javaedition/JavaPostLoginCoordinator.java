package com.customminecraftserver.javaedition;

import com.customminecraftserver.logging.StructuredConnectionLogger;
import com.customminecraftserver.session.ConnectionSession;
import com.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class JavaPostLoginCoordinator {
    private final StructuredConnectionLogger logger;

    JavaPostLoginCoordinator(StructuredConnectionLogger logger) {
        this.logger = logger;
    }

    void beginPostLoginFlow(ChannelHandlerContext context, ConnectionSession session, String transitionReason) {
        UUID playerUuid = JavaPlayerUuidResolver.resolve(session);
        String username = session.username() == null || session.username().isBlank() ? "unknown-player" : session.username();

        logger.outboundPacket(session, "LOGIN_SUCCESS", Map.of(
                "username", username,
                "playerUuid", playerUuid.toString()
        ));

        if (isModernJava(session.protocolVersion())) {
            transition(session, SessionState.JAVA_WAITING_LOGIN_ACK, Map.of("reason", transitionReason));
            context.writeAndFlush(JavaPacketWriter.loginSuccess(context.alloc(), session.protocolVersion(), playerUuid, username));
            return;
        }

        String message = JavaChannelState.terminalMessage(context, session);
        transition(session, SessionState.JAVA_TERMINATED, Map.of("reason", transitionReason));
        context.write(JavaPacketWriter.loginSuccess(context.alloc(), session.protocolVersion(), playerUuid, username));
        logger.outboundPacket(session, "PLAY_DISCONNECT", Map.of("disconnectMessage", message));
        context.writeAndFlush(JavaPacketWriter.legacyPlayDisconnect(context.alloc(), message)).addListener(ChannelFutureListener.CLOSE);
    }

    void handleLoginAcknowledgement(ChannelHandlerContext context, ConnectionSession session, int packetId) {
        if (packetId != JavaPacketIds.LOGIN_ACKNOWLEDGED) {
            failAndClose(context, session, "Unsupported Java post-login packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        logger.inboundPacket(session, "LOGIN_ACKNOWLEDGED", Map.of("packetId", packetId));
        transition(session, SessionState.JAVA_CONFIGURING, Map.of("reason", "LOGIN_ACKNOWLEDGED"));
        logger.outboundPacket(session, "FINISH_CONFIGURATION", Map.of("packetId", JavaPacketIds.FINISH_CONFIGURATION));
        context.writeAndFlush(JavaPacketWriter.finishConfiguration(context.alloc()));
    }

    void handleConfiguration(ChannelHandlerContext context, ConnectionSession session, int packetId) {
        if (packetId != JavaPacketIds.FINISH_CONFIGURATION) {
            failAndClose(context, session, "Unsupported Java configuration packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        logger.inboundPacket(session, "FINISH_CONFIGURATION", Map.of("packetId", packetId));
        transition(session, SessionState.JAVA_PLAY, Map.of("reason", "CONFIGURATION_COMPLETED"));
        context.channel().attr(JavaChannelState.KEEPALIVE_ID_KEY).set(JavaChannelState.MODERN_PLAY_KEEPALIVE_ID);
        logger.outboundPacket(session, "PLAY_KEEP_ALIVE", Map.of("keepAliveId", JavaChannelState.MODERN_PLAY_KEEPALIVE_ID));
        context.writeAndFlush(JavaPacketWriter.playKeepAlive(context.alloc(), JavaChannelState.MODERN_PLAY_KEEPALIVE_ID));
    }

    void handlePlay(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId == JavaPacketIds.PLAY_SERVERBOUND_KEEP_ALIVE) {
            handlePlayKeepAlive(context, session, packetId, frame);
            return;
        }
        if (packetId == JavaPacketIds.PLAY_SERVERBOUND_SETTINGS) {
            recordSettingsPacket(context, session, packetId, frame);
            return;
        }
        failAndClose(context, session, "Unsupported Java play packet id " + packetId, Map.of("packetId", packetId));
    }

    void handleWaitingSettings(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId != JavaPacketIds.PLAY_SERVERBOUND_SETTINGS) {
            failAndClose(context, session, "Unsupported Java play packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        JavaChannelState.cancelSettingsTimeout(context);
        recordSettingsPacket(context, session, packetId, frame);
        beginPlayPingRoundtrip(context, session, "PLAY_SETTINGS_RECEIVED");
    }

    void handleWaitingPong(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId != JavaPacketIds.PLAY_SERVERBOUND_PONG) {
            failAndClose(context, session, "Unsupported Java play packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        int pingId = frame.readInt();
        Integer expectedPingId = context.channel().attr(JavaChannelState.PING_ID_KEY).get();
        logger.inboundPacket(session, "PLAY_PONG", Map.of(
                "packetId", packetId,
                "pingId", pingId
        ));
        if (expectedPingId != null && expectedPingId.intValue() != pingId) {
            failAndClose(context, session, "Unexpected Java play ping id " + pingId, Map.of(
                    "packetId", packetId,
                    "expectedPingId", expectedPingId
            ));
            return;
        }

        JavaChannelState.cancelPongTimeout(context);
        disconnectModernPlay(context, session, "PLAY_PONG_RECEIVED");
    }

    void cancelTimeouts(ChannelHandlerContext context) {
        JavaChannelState.cancelSettingsTimeout(context);
        JavaChannelState.cancelPongTimeout(context);
    }

    private void handlePlayKeepAlive(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        long keepAliveId = frame.readLong();
        Long expectedKeepAliveId = context.channel().attr(JavaChannelState.KEEPALIVE_ID_KEY).get();
        logger.inboundPacket(session, "PLAY_KEEP_ALIVE", Map.of(
                "packetId", packetId,
                "keepAliveId", keepAliveId
        ));

        if (expectedKeepAliveId != null && expectedKeepAliveId.longValue() != keepAliveId) {
            failAndClose(context, session, "Unexpected Java play keepalive id " + keepAliveId, Map.of(
                    "packetId", packetId,
                    "expectedKeepAliveId", expectedKeepAliveId
            ));
            return;
        }

        transition(session, SessionState.JAVA_WAITING_SETTINGS, Map.of("reason", "PLAY_KEEPALIVE_ROUNDTRIP_COMPLETED"));
        if (Boolean.TRUE.equals(context.channel().attr(JavaChannelState.SETTINGS_OBSERVED_KEY).get())) {
            beginPlayPingRoundtrip(context, session, "PLAY_SETTINGS_ALREADY_OBSERVED");
            return;
        }
        scheduleSettingsTimeout(context, session);
    }

    private void recordSettingsPacket(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        JavaClientSettingsPacket settingsPacket = JavaClientSettingsPacket.read(frame);
        context.channel().attr(JavaChannelState.SETTINGS_OBSERVED_KEY).set(true);
        logger.inboundPacket(session, "SETTINGS", Map.of(
                "packetId", packetId,
                "locale", settingsPacket.locale(),
                "viewDistance", settingsPacket.viewDistance(),
                "chatFlags", settingsPacket.chatFlags(),
                "chatColors", settingsPacket.chatColors(),
                "mainHand", settingsPacket.mainHand(),
                "particleStatus", settingsPacket.particleStatus()
        ));
    }

    private void scheduleSettingsTimeout(ChannelHandlerContext context, ConnectionSession session) {
        JavaChannelState.cancelSettingsTimeout(context);
        ScheduledFuture<?> timeout = context.executor().schedule(
                () -> {
                    if (context.channel().isActive() && session.state() == SessionState.JAVA_WAITING_SETTINGS) {
                        disconnectModernPlay(context, session, "PLAY_SETTINGS_TIMEOUT");
                    }
                },
                JavaChannelState.SETTINGS_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS
        );
        context.channel().attr(JavaChannelState.SETTINGS_TIMEOUT_KEY).set(timeout);
    }

    private void beginPlayPingRoundtrip(ChannelHandlerContext context, ConnectionSession session, String transitionReason) {
        JavaChannelState.cancelSettingsTimeout(context);
        JavaChannelState.cancelPongTimeout(context);
        context.channel().attr(JavaChannelState.PING_ID_KEY).set(JavaChannelState.MODERN_PLAY_PING_ID);
        logger.outboundPacket(session, "PLAY_PING", Map.of("pingId", JavaChannelState.MODERN_PLAY_PING_ID));
        transition(session, SessionState.JAVA_WAITING_PONG, Map.of("reason", transitionReason));
        context.writeAndFlush(JavaPacketWriter.playPing(context.alloc(), JavaChannelState.MODERN_PLAY_PING_ID));
        schedulePongTimeout(context, session);
    }

    private void schedulePongTimeout(ChannelHandlerContext context, ConnectionSession session) {
        JavaChannelState.cancelPongTimeout(context);
        ScheduledFuture<?> timeout = context.executor().schedule(
                () -> {
                    if (context.channel().isActive() && session.state() == SessionState.JAVA_WAITING_PONG) {
                        disconnectModernPlay(context, session, "PLAY_PONG_TIMEOUT");
                    }
                },
                JavaChannelState.PONG_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS
        );
        context.channel().attr(JavaChannelState.PONG_TIMEOUT_KEY).set(timeout);
    }

    private void disconnectModernPlay(ChannelHandlerContext context, ConnectionSession session, String transitionReason) {
        JavaChannelState.cancelSettingsTimeout(context);
        JavaChannelState.cancelPongTimeout(context);
        String message = JavaChannelState.terminalMessage(context, session);
        logger.outboundPacket(session, "PLAY_DISCONNECT", Map.of("disconnectMessage", message));
        transition(session, SessionState.JAVA_TERMINATED, Map.of("reason", transitionReason));
        context.writeAndFlush(JavaPacketWriter.modernPlayDisconnect(context.alloc(), message)).addListener(ChannelFutureListener.CLOSE);
    }

    private boolean isModernJava(Integer protocolVersion) {
        return protocolVersion != null && protocolVersion >= 767;
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
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
}
