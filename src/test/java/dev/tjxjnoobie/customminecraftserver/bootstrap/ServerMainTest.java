package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMainTest {
    @Test
    void consoleContextDelegatesToBootstrap() throws Exception {
        TestLogSupport.logTestStart("ServerMainTest.consoleContextDelegatesToBootstrap");
        ServerBootstrap bootstrap = new ServerBootstrap(ServerSettings.defaults());
        try {
            Class<?> consoleContextClass = Class.forName("dev.tjxjnoobie.customminecraftserver.bootstrap.ServerMain$ConsoleContext");
            Constructor<?> constructor = consoleContextClass.getDeclaredConstructor(ServerBootstrap.class);
            constructor.setAccessible(true);
            Object context = constructor.newInstance(bootstrap);

            Method snapshotMethod = consoleContextClass.getDeclaredMethod("snapshot");
            snapshotMethod.setAccessible(true);
            Object snapshot = snapshotMethod.invoke(context);
            assertNotNull(snapshot);

            Method activeSessionsMethod = consoleContextClass.getDeclaredMethod("activeSessionSummaryLines");
            activeSessionsMethod.setAccessible(true);
            Object activeSessions = activeSessionsMethod.invoke(context);
            assertTrue(activeSessions instanceof List);
        } finally {
            bootstrap.close();
        }
    }
}
