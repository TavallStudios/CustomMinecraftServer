package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallengeFactory;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.MojangJavaSessionService;
import dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaConnectionHandler;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OfflineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OnlineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JavaTcpServerBootstrapTest {
    @Test
    void closeAndAwaitWithoutStartDoNotThrow() {
        TestLogSupport.logTestStart("JavaTcpServerBootstrapTest.closeAndAwaitWithoutStartDoNotThrow");
        ServerSettings settings = ServerSettings.defaults();
        StructuredConnectionLogger logger = new StructuredConnectionLogger();
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        ProtocolVersionDetector detector = new ProtocolVersionDetector();
        JavaEncryptionChallengeFactory challengeFactory = JavaEncryptionChallengeFactory.createGenerated(
                settings.javaAuthentication().rsaKeySizeBits()
        );
        JavaLoginCoordinator loginCoordinator = new JavaLoginCoordinator(
                new OfflineJavaLoginAdmission(),
                new OnlineJavaLoginAdmission(challengeFactory)
        );
        JavaConnectionHandler handler = new JavaConnectionHandler(
                settings,
                registry,
                logger,
                detector,
                loginCoordinator,
                new MojangJavaSessionService(settings.javaAuthentication())
        );
        JavaTcpServerBootstrap bootstrap = new JavaTcpServerBootstrap(settings, handler);

        assertDoesNotThrow(bootstrap::awaitClose);
        assertDoesNotThrow(bootstrap::close);
    }
}
