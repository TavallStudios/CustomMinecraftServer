package dev.tjxjnoobie.customminecraftserver.logging;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class StructuredConnectionLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredConnectionLogger.class);

    public void serverStartup(ServerSettings settings) {
        info("server_startup", null, Map.of(
                "host", settings.host(),
                "javaTcpPort", settings.javaTcpPort(),
                "bedrockUdpPort", settings.bedrockUdpPort(),
                "authMode", settings.authMode(),
                "structuredLoggingEnabled", settings.structuredLoggingEnabled()
        ));
    }

    public void serverStarted(ServerSettings settings) {
        info("server_started", null, Map.of(
                "host", settings.host(),
                "javaTcpPort", settings.javaTcpPort(),
                "bedrockUdpPort", settings.bedrockUdpPort(),
                "authMode", settings.authMode()
        ));
    }

    public void connectionOpened(ConnectionSession session) {
        info("connection_opened", session, Map.of());
    }

    public void inboundPacket(ConnectionSession session, String packetName, Map<String, ?> details) {
        info("packet_inbound", session, merge(details, Map.of("packet", packetName)));
    }

    public void outboundPacket(ConnectionSession session, String packetName, Map<String, ?> details) {
        info("packet_outbound", session, merge(details, Map.of("packet", packetName)));
    }

    public void transition(ConnectionSession session, SessionState from, SessionState to, Map<String, ?> details) {
        info("state_transition", session, merge(details, Map.of("fromState", from, "toState", to)));
    }

    public void warning(ConnectionSession session, String event, Map<String, ?> details) {
        warn(event, session, details);
    }

    public void failure(ConnectionSession session, String event, Throwable error, Map<String, ?> details) {
        String message = format(event, session, details);
        LOGGER.error(message, error);
    }

    public void connectionClosed(ConnectionSession session, String reason) {
        info("connection_closed", session, Map.of("reason", reason));
    }

    private void info(String event, ConnectionSession session, Map<String, ?> details) {
        LOGGER.info(format(event, session, details));
    }

    private void warn(String event, ConnectionSession session, Map<String, ?> details) {
        LOGGER.warn(format(event, session, details));
    }

    private Map<String, ?> merge(Map<String, ?> left, Map<String, ?> right) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(left);
        merged.putAll(right);
        return merged;
    }

    private String format(String event, ConnectionSession session, Map<String, ?> details) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", event);
        if (session != null) {
            fields.put("sessionId", session.sessionId());
            fields.put("remote", session.remoteAddress());
            fields.put("edition", session.edition());
            fields.put("authMode", session.authMode());
            fields.put("state", session.state());
            if (session.protocolVersion() != null) {
                fields.put("protocolVersion", session.protocolVersion());
            }
            if (session.protocolFamily() != null) {
                fields.put("protocolFamily", session.protocolFamily());
            }
            if (session.username() != null) {
                fields.put("username", session.username());
            }
            if (session.authenticatedIdentity() != null) {
                fields.put("authenticatedIdentity", session.authenticatedIdentity());
            }
            if (session.authenticatedXuid() != null) {
                fields.put("authenticatedXuid", session.authenticatedXuid());
            }
        }
        fields.putAll(details);

        StringJoiner joiner = new StringJoiner(" ");
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            joiner.add(field.getKey() + "=" + render(field.getValue()));
        }
        return joiner.toString();
    }

    private String render(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.chars().noneMatch(Character::isWhitespace) && !text.contains("\"")) {
            return text;
        }
        return "\"" + text.replace("\"", "\\\"") + "\"";
    }
}

