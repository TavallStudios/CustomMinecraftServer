package dev.tjxjnoobie.customminecraftserver.session;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionSessionRegistry {
    private final Map<String, ConnectionSession> sessions = new ConcurrentHashMap<>();

    public ConnectionSession registerJava(Channel channel, AuthMode authMode) {
        String key = javaKey(channel);
        ConnectionSession session = new ConnectionSession(formatRemote(channel.remoteAddress()), ConnectionEdition.JAVA, authMode);
        session.state(SessionState.JAVA_HANDSHAKE);
        sessions.put(key, session);
        return session;
    }

    public ConnectionSession registerBedrock(SocketAddress remoteAddress, AuthMode authMode) {
        String key = bedrockKey(remoteAddress);
        return sessions.computeIfAbsent(key, ignored -> {
            ConnectionSession session = new ConnectionSession(formatRemote(remoteAddress), ConnectionEdition.BEDROCK, authMode);
            session.state(SessionState.BEDROCK_UNCONNECTED);
            return session;
        });
    }

    public void unregisterJava(Channel channel) {
        sessions.remove(javaKey(channel));
    }

    public void unregisterBedrock(SocketAddress remoteAddress) {
        sessions.remove(bedrockKey(remoteAddress));
    }

    public Collection<ConnectionSession> activeSessions() {
        return sessions.values();
    }

    private String javaKey(Channel channel) {
        return "java:" + channel.id().asLongText();
    }

    private String bedrockKey(SocketAddress remoteAddress) {
        return "bedrock:" + formatRemote(remoteAddress);
    }

    private String formatRemote(SocketAddress remoteAddress) {
        return remoteAddress == null ? "unknown" : remoteAddress.toString();
    }
}

