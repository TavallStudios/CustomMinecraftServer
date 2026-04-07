package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockProtocolMetadataTest {
    @Test
    void resolvesKnownGameVersions() {
        TestLogSupport.logTestStart("BedrockProtocolMetadataTest.resolvesKnownGameVersions");
        assertEquals("26.10", BedrockProtocolMetadata.gameVersion(944));
        assertEquals("1.21.130", BedrockProtocolMetadata.gameVersion(898));
        assertEquals(BedrockProtocolMetadata.ADVERTISEMENT_VERSION, BedrockProtocolMetadata.gameVersion(null));
    }
}
