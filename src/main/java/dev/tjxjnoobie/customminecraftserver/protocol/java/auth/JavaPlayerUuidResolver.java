package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class JavaPlayerUuidResolver {
    private JavaPlayerUuidResolver() {
    }

    public static UUID resolve(ConnectionSession session) {
        String authenticatedIdentity = session.authenticatedIdentity();
        if (authenticatedIdentity != null && !authenticatedIdentity.isBlank()) {
            return parse(authenticatedIdentity);
        }

        String username = session.username() == null || session.username().isBlank()
                ? "unknown-player"
                : session.username();
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        session.authenticatedIdentity(offlineUuid.toString().replace("-", ""));
        return offlineUuid;
    }

    private static UUID parse(String value) {
        String normalized = value.trim();
        if (normalized.contains("-")) {
            return UUID.fromString(normalized);
        }
        if (normalized.length() == 32) {
            return UUID.fromString(
                    normalized.substring(0, 8) + "-"
                            + normalized.substring(8, 12) + "-"
                            + normalized.substring(12, 16) + "-"
                            + normalized.substring(16, 20) + "-"
                            + normalized.substring(20)
            );
        }
        return UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8));
    }
}

