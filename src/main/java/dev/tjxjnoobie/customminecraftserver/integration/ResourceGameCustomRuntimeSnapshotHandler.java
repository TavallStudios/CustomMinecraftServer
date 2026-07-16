package dev.tjxjnoobie.customminecraftserver.integration;

import com.tavall.hytale.resourcegame.shared.frontend.MinecraftPlayerRuntimeSnapshot;
import com.tavall.hytale.resourcegame.shared.frontend.MinecraftServerRuntimeSnapshot;
import com.tavall.hytale.resourcegame.shared.frontend.ResourceGameFrontendSurfaceIdentity;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResourceGameCustomRuntimeSnapshotHandler {
    private final String serverId;
    private final long startedAtEpochMillis;

    public ResourceGameCustomRuntimeSnapshotHandler(String serverId, long startedAtEpochMillis) {
        this.serverId = serverId == null || serverId.isBlank() ? "custom-minecraft-runtime" : serverId;
        this.startedAtEpochMillis = startedAtEpochMillis;
    }

    public MinecraftServerRuntimeSnapshot createSnapshot(
            ServerSettings settings,
            Collection<ConnectionSession> sessions,
            long observedAtEpochMillis
    ) {
        List<MinecraftPlayerRuntimeSnapshot> players = new ArrayList<>();
        for (ConnectionSession session : sessions) {
            players.add(toPlayerSnapshot(session));
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("runtime", "custom-minecraft-server");
        metadata.put("visualSurface", "noop-until-world-join");
        metadata.put("javaTcpPort", Integer.toString(settings.javaTcpPort()));
        metadata.put("bedrockUdpPort", Integer.toString(settings.bedrockUdpPort()));

        return new MinecraftServerRuntimeSnapshot(
                ResourceGameFrontendSurfaceIdentity.CUSTOM_RUNTIME,
                serverId,
                null,
                hostname(),
                startedAtEpochMillis,
                observedAtEpochMillis,
                players.size(),
                settings.maxConnections(),
                players,
                metadata
        );
    }

    private MinecraftPlayerRuntimeSnapshot toPlayerSnapshot(ConnectionSession session) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("edition", session.edition().name());
        metadata.put("state", session.state().name());
        metadata.put("remoteAddress", session.remoteAddress());
        if (session.protocolFamily() != null) {
            metadata.put("protocolFamily", session.protocolFamily());
        }
        if (session.protocolVersion() != null) {
            metadata.put("protocolVersion", session.protocolVersion().toString());
        }

        return new MinecraftPlayerRuntimeSnapshot(
                session.sessionId(),
                session.username() == null || session.username().isBlank() ? session.sessionId().toString() : session.username(),
                "custom-runtime-handshake",
                0.0d,
                0.0d,
                0.0d,
                0.0f,
                0.0f,
                0.0d,
                0,
                "HANDSHAKE",
                true,
                metadata
        );
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "custom-minecraft-runtime";
        }
    }
}
