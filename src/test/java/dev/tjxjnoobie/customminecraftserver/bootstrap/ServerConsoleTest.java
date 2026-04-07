package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ServerConsoleTest {
    @Test
    void closeWithoutStartDoesNotThrow() {
        TestLogSupport.logTestStart("ServerConsoleTest.closeWithoutStartDoesNotThrow");
        ServerConsoleContext context = new ServerConsoleContext() {
            @Override
            public ServerConsoleSnapshot snapshot() {
                return new ServerConsoleSnapshot("0.0.0.0", 25565, 19132, AuthMode.OFFLINE, 0, false);
            }

            @Override
            public List<String> activeSessionSummaryLines() {
                return List.of();
            }

            @Override
            public void requestStop() {
            }
        };
        ServerConsole console = new ServerConsole(context);
        assertDoesNotThrow(console::close);
    }
}
