package dev.tjxjnoobie.customminecraftserver.integration;

import org.tavall.api.minecraft.MinecraftServerRuntimeSnapshot;
import org.tavall.api.minecraft.MinecraftVisualRenderRequest;
import org.tavall.api.minecraft.frontend.ResourceGameFrontendSurfaceIdentity;
import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceGameCustomRuntimeSnapshotHandlerTest {
    @Test
    void createsCustomRuntimeSurfaceSnapshotFromHandshakeSessions() {
        ConnectionSession session = new ConnectionSession("127.0.0.1:25565", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.username("Miner");
        session.protocolFamily("JAVA_1_21_X");
        session.protocolVersion(767);
        session.state(SessionState.JAVA_PLAY);

        MinecraftServerRuntimeSnapshot snapshot = new ResourceGameCustomRuntimeSnapshotHandler("custom-runtime", 1000L)
                .createSnapshot(ServerSettings.defaults(), List.of(session), 2000L);

        assertEquals(ResourceGameFrontendSurfaceIdentity.CUSTOM_RUNTIME, snapshot.surfaceIdentity());
        assertEquals("custom-runtime", snapshot.serverId());
        assertEquals(1, snapshot.onlinePlayerCount());
        assertEquals("Miner", snapshot.players().get(0).playerName());
        assertEquals("noop-until-world-join", snapshot.metadata().get("visualSurface"));
    }

    @Test
    void visualHandlerDocumentsNoopState() {
        String description = new ResourceGameCustomRuntimeVisualHandler().describeNoop(new MinecraftVisualRenderRequest(
                ResourceGameFrontendSurfaceIdentity.CUSTOM_RUNTIME,
                "custom-runtime",
                "player",
                "CHAT",
                "Title",
                "Body",
                Map.of(),
                "corr"
        ));

        assertTrue(description.contains("no-op"));
        assertTrue(description.contains("CHAT"));
    }
}
