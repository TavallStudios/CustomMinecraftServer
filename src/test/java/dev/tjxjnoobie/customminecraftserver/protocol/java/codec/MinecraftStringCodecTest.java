package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftStringCodecTest {
    @Test
    void writesAndReadsMinecraftStrings() {
        TestLogSupport.logTestStart("MinecraftStringCodecTest.writesAndReadsMinecraftStrings");
        ByteBuf buffer = Unpooled.buffer();
        MinecraftStringCodec.write(buffer, "hello");
        String value = MinecraftStringCodec.read(buffer, 32);
        assertEquals("hello", value);
        buffer.release();
    }
}
