package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockJwtVerifier;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OfflineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OnlineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramHandler;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BedrockUdpServerBootstrapTest {
    @Test
    void closeAndAwaitWithoutStartDoNotThrow() {
        TestLogSupport.logTestStart("BedrockUdpServerBootstrapTest.closeAndAwaitWithoutStartDoNotThrow");
        ServerSettings settings = ServerSettings.defaults();
        StructuredConnectionLogger logger = new StructuredConnectionLogger();
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        ProtocolVersionDetector detector = new ProtocolVersionDetector();
        BedrockJwtVerifier verifier = new BedrockJwtVerifier();
        BedrockLoginCoordinator loginCoordinator = new BedrockLoginCoordinator(
                new OfflineBedrockLoginAdmission(verifier),
                new OnlineBedrockLoginAdmission(verifier, settings.bedrockAuthentication())
        );
        BedrockDatagramHandler handler = new BedrockDatagramHandler(settings, registry, logger, detector, loginCoordinator);
        BedrockUdpServerBootstrap bootstrap = new BedrockUdpServerBootstrap(settings, handler);

        assertDoesNotThrow(bootstrap::awaitClose);
        assertDoesNotThrow(bootstrap::close);
    }
}
