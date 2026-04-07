package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockStartGameTemplatesTest {
    @Test
    void templatesVaryByProtocol() {
        TestLogSupport.logTestStart("BedrockStartGameTemplatesTest.templatesVaryByProtocol");
        byte[] legacy = BedrockStartGameTemplates.startGameTemplate(898);
        byte[] modern = BedrockStartGameTemplates.startGameTemplate(944);
        assertTrue(legacy.length > 0);
        assertTrue(modern.length > 0);
        assertNotEquals(legacy.length, modern.length);
    }
}
