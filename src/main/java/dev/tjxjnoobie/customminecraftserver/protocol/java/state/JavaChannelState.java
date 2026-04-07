package dev.tjxjnoobie.customminecraftserver.protocol.java.state;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallenge;

import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;

public final class JavaChannelState {
    public static final long MODERN_PLAY_KEEPALIVE_ID = 0x1020304050607080L;
    public static final int MODERN_PLAY_PING_ID = 0x10203040;
    public static final long SETTINGS_TIMEOUT_MILLIS = 1500L;
    public static final long PONG_TIMEOUT_MILLIS = 1500L;

    public static final AttributeKey<ConnectionSession> SESSION_KEY = AttributeKey.valueOf("java-session");
    public static final AttributeKey<JavaEncryptionChallenge> ENCRYPTION_CHALLENGE_KEY = AttributeKey.valueOf("java-encryption-challenge");
    public static final AttributeKey<String> TERMINAL_MESSAGE_KEY = AttributeKey.valueOf("java-terminal-message");
    public static final AttributeKey<Long> KEEPALIVE_ID_KEY = AttributeKey.valueOf("java-keepalive-id");
    public static final AttributeKey<Integer> PING_ID_KEY = AttributeKey.valueOf("java-ping-id");
    public static final AttributeKey<Boolean> SETTINGS_OBSERVED_KEY = AttributeKey.valueOf("java-settings-observed");
    public static final AttributeKey<ScheduledFuture<?>> SETTINGS_TIMEOUT_KEY = AttributeKey.valueOf("java-settings-timeout");
    public static final AttributeKey<ScheduledFuture<?>> PONG_TIMEOUT_KEY = AttributeKey.valueOf("java-pong-timeout");

    private JavaChannelState() {
    }

    public static ConnectionSession requireSession(ChannelHandlerContext context) {
        ConnectionSession session = context.channel().attr(SESSION_KEY).get();
        if (session == null) {
            throw new IllegalStateException("Java session was not initialized");
        }
        return session;
    }

    public static void terminalMessage(ChannelHandlerContext context, String message) {
        context.channel().attr(TERMINAL_MESSAGE_KEY).set(message);
    }

    public static String terminalMessage(ChannelHandlerContext context, ConnectionSession session) {
        String message = context.channel().attr(TERMINAL_MESSAGE_KEY).get();
        if (message != null && !message.isBlank()) {
            return message;
        }
        String protocolFamily = session.protocolFamily() == null ? "JAVA_UNKNOWN" : session.protocolFamily();
        return "Custom server handshake reached successfully [" + protocolFamily + " " + session.authMode() + "]";
    }

    public static void cancelSettingsTimeout(ChannelHandlerContext context) {
        cancelTimeout(context.channel().attr(SETTINGS_TIMEOUT_KEY).getAndSet(null));
    }

    public static void cancelPongTimeout(ChannelHandlerContext context) {
        cancelTimeout(context.channel().attr(PONG_TIMEOUT_KEY).getAndSet(null));
    }

    public static void cancelTimeout(ScheduledFuture<?> timeout) {
        if (timeout != null) {
            timeout.cancel(false);
        }
    }
}


