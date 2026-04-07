package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConsoleCommandResultTest {
    @Test
    void continuesRunningWhenRequested() {
        TestLogSupport.logTestStart("ServerConsoleCommandResultTest.continuesRunningWhenRequested");
        ServerConsoleCommandResult result = ServerConsoleCommandResult.continueRunning("ok");
        assertEquals("ok", result.message());
        assertFalse(result.stopRequested());
    }

    @Test
    void stopsWhenRequested() {
        TestLogSupport.logTestStart("ServerConsoleCommandResultTest.stopsWhenRequested");
        ServerConsoleCommandResult result = ServerConsoleCommandResult.stop("stop");
        assertEquals("stop", result.message());
        assertTrue(result.stopRequested());
    }
}
