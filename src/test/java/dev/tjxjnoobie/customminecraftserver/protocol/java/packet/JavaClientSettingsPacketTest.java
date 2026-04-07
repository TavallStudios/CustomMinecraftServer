package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaClientSettingsPacketTest {
    @Test
    void readsSettingsFields() {
        TestLogSupport.logTestStart("JavaClientSettingsPacketTest.readsSettingsFields");
        ByteBuf buffer = Unpooled.buffer();
        MinecraftStringCodec.write(buffer, "en_us");
        buffer.writeByte(8);
        MinecraftVarInt.write(buffer, 1);
        buffer.writeBoolean(true);
        buffer.writeByte(0x7f);
        MinecraftVarInt.write(buffer, 0);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        MinecraftVarInt.write(buffer, 2);

        JavaClientSettingsPacket packet = JavaClientSettingsPacket.read(buffer);

        assertEquals("en_us", packet.locale());
        assertEquals(8, packet.viewDistance());
        assertEquals(1, packet.chatFlags());
        assertTrue(packet.chatColors());
        assertEquals(2, packet.particleStatus());
        buffer.release();
    }
}
