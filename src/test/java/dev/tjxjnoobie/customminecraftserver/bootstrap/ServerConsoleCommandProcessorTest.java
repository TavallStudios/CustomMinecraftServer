package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerConsoleCommandProcessorTest {
    @Test
    void statusCommandReturnsSnapshotDetails() {
        TestConsoleContext context = new TestConsoleContext(
                new ServerConsoleSnapshot("0.0.0.0", 25565, 19132, AuthMode.OFFLINE, 2, true),
                List.of("session-1", "session-2")
        );
        ServerConsoleCommandProcessor processor = new ServerConsoleCommandProcessor(context);

        ServerConsoleCommandResult result = processor.handle("status");

        assertFalse(result.stopRequested());
        assertEquals(
                "status running=true host=0.0.0.0 javaTcpPort=25565 bedrockUdpPort=19132 authMode=OFFLINE activeSessions=2",
                result.message()
        );
    }

    @Test
    void sessionsCommandReturnsFriendlyEmptyMessage() {
        TestConsoleContext context = new TestConsoleContext(
                new ServerConsoleSnapshot("0.0.0.0", 25565, 19132, AuthMode.OFFLINE, 0, true),
                List.of()
        );
        ServerConsoleCommandProcessor processor = new ServerConsoleCommandProcessor(context);

        ServerConsoleCommandResult result = processor.handle("sessions");

        assertFalse(result.stopRequested());
        assertEquals("No active sessions.", result.message());
    }

    @Test
    void stopCommandRequestsShutdown() {
        TestConsoleContext context = new TestConsoleContext(
                new ServerConsoleSnapshot("0.0.0.0", 25565, 19132, AuthMode.OFFLINE, 0, true),
                List.of()
        );
        ServerConsoleCommandProcessor processor = new ServerConsoleCommandProcessor(context);

        ServerConsoleCommandResult result = processor.handle("stop");

        assertTrue(context.stopRequested);
        assertTrue(result.stopRequested());
        assertEquals("Stopping server...", result.message());
    }

    private static final class TestConsoleContext implements ServerConsoleContext {
        private final ServerConsoleSnapshot snapshot;
        private final List<String> sessions;
        private boolean stopRequested;

        private TestConsoleContext(ServerConsoleSnapshot snapshot, List<String> sessions) {
            this.snapshot = snapshot;
            this.sessions = sessions;
        }

        @Override
        public ServerConsoleSnapshot snapshot() {
            return snapshot;
        }

        @Override
        public List<String> activeSessionSummaryLines() {
            return sessions;
        }

        @Override
        public void requestStop() {
            stopRequested = true;
        }
    }
}

