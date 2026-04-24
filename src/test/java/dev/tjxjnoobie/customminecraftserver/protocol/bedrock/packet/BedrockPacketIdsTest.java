package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockPacketIdsTest {
    @Test
    void constantsMatchExpectedValues() {
        TestLogSupport.logTestStart("BedrockPacketIdsTest.constantsMatchExpectedValues");
        assertEquals(0x01, BedrockPacketIds.RAKNET_UNCONNECTED_PING);
        assertEquals(0x1c, BedrockPacketIds.RAKNET_UNCONNECTED_PONG);
        assertEquals(0xfe, BedrockPacketIds.BEDROCK_BATCH);
        assertTrue(BedrockPacketIds.BEDROCK_LOGIN > 0);
    }
}
