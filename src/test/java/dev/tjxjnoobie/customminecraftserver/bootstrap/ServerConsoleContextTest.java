package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConsoleContextTest {
    @Test
    void contextProvidesSnapshotAndSessions() {
        TestLogSupport.logTestStart("ServerConsoleContextTest.contextProvidesSnapshotAndSessions");
        ServerConsoleSnapshot expected = new ServerConsoleSnapshot("host", 1, 2, AuthMode.OFFLINE, 3, true);
        ServerConsoleContext context = new ServerConsoleContext() {
            @Override
            public ServerConsoleSnapshot snapshot() {
                return expected;
            }

            @Override
            public List<String> activeSessionSummaryLines() {
                return List.of("session");
            }

            @Override
            public void requestStop() {
            }
        };

        assertEquals(expected, context.snapshot());
        assertEquals(List.of("session"), context.activeSessionSummaryLines());
    }
}
