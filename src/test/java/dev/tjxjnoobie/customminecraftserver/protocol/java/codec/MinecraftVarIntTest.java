package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinecraftVarIntTest {
    @Test
    void writesAndReadsVarInts() {
        TestLogSupport.logTestStart("MinecraftVarIntTest.writesAndReadsVarInts");
        ByteBuf buffer = Unpooled.buffer();
        MinecraftVarInt.write(buffer, 300);
        MinecraftVarInt.write(buffer, 1);

        assertEquals(300, MinecraftVarInt.read(buffer));
        assertEquals(1, MinecraftVarInt.read(buffer));
        buffer.release();
    }

    @Test
    void tryReadReturnsNullForIncompleteValue() {
        TestLogSupport.logTestStart("MinecraftVarIntTest.tryReadReturnsNullForIncompleteValue");
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x80);

        assertNull(MinecraftVarInt.tryRead(buffer));
        assertEquals(1, buffer.readableBytes());
        buffer.release();
    }
}
