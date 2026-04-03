package com.customminecraftserver.javaedition;

import com.customminecraftserver.session.ConnectionSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;

final class JavaChannelState {
    static final long MODERN_PLAY_KEEPALIVE_ID = 0x1020304050607080L;
    static final int MODERN_PLAY_PING_ID = 0x10203040;
    static final long SETTINGS_TIMEOUT_MILLIS = 1500L;
    static final long PONG_TIMEOUT_MILLIS = 1500L;

    static final AttributeKey<ConnectionSession> SESSION_KEY = AttributeKey.valueOf("java-session");
    static final AttributeKey<JavaEncryptionChallenge> ENCRYPTION_CHALLENGE_KEY = AttributeKey.valueOf("java-encryption-challenge");
    static final AttributeKey<String> TERMINAL_MESSAGE_KEY = AttributeKey.valueOf("java-terminal-message");
    static final AttributeKey<Long> KEEPALIVE_ID_KEY = AttributeKey.valueOf("java-keepalive-id");
    static final AttributeKey<Integer> PING_ID_KEY = AttributeKey.valueOf("java-ping-id");
    static final AttributeKey<Boolean> SETTINGS_OBSERVED_KEY = AttributeKey.valueOf("java-settings-observed");
    static final AttributeKey<ScheduledFuture<?>> SETTINGS_TIMEOUT_KEY = AttributeKey.valueOf("java-settings-timeout");
    static final AttributeKey<ScheduledFuture<?>> PONG_TIMEOUT_KEY = AttributeKey.valueOf("java-pong-timeout");

    private JavaChannelState() {
    }

    static ConnectionSession requireSession(ChannelHandlerContext context) {
        ConnectionSession session = context.channel().attr(SESSION_KEY).get();
        if (session == null) {
            throw new IllegalStateException("Java session was not initialized");
        }
        return session;
    }

    static void terminalMessage(ChannelHandlerContext context, String message) {
        context.channel().attr(TERMINAL_MESSAGE_KEY).set(message);
    }

    static String terminalMessage(ChannelHandlerContext context, ConnectionSession session) {
        String message = context.channel().attr(TERMINAL_MESSAGE_KEY).get();
        if (message != null && !message.isBlank()) {
            return message;
        }
        String protocolFamily = session.protocolFamily() == null ? "JAVA_UNKNOWN" : session.protocolFamily();
        return "Custom server handshake reached successfully [" + protocolFamily + " " + session.authMode() + "]";
    }

    static void cancelSettingsTimeout(ChannelHandlerContext context) {
        cancelTimeout(context.channel().attr(SETTINGS_TIMEOUT_KEY).getAndSet(null));
    }

    static void cancelPongTimeout(ChannelHandlerContext context) {
        cancelTimeout(context.channel().attr(PONG_TIMEOUT_KEY).getAndSet(null));
    }

    static void cancelTimeout(ScheduledFuture<?> timeout) {
        if (timeout != null) {
            timeout.cancel(false);
        }
    }
}
