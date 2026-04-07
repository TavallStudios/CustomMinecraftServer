package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockDatagramFrameTest {
    @Test
    void recordCarriesFrameFields() {
        TestLogSupport.logTestStart("BedrockDatagramFrameTest.recordCarriesFrameFields");
        var payload = Unpooled.buffer();
        payload.writeByte(1);
        BedrockDatagramFrame frame = new BedrockDatagramFrame(false, 0, 0, 0, payload);
        assertEquals(false, frame.split());
        assertEquals(0, frame.splitCount());
        assertEquals(0, frame.splitId());
        assertEquals(0, frame.splitIndex());
        assertEquals(1, frame.payload().readUnsignedByte());
        payload.release();
    }
}
