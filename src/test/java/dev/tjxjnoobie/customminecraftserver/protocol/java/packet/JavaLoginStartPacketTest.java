package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaLoginStartPacketTest {
    @Test
    void readsUsernameAndUuid() {
        TestLogSupport.logTestStart("JavaLoginStartPacketTest.readsUsernameAndUuid");
        ByteBuf buffer = Unpooled.buffer();
        MinecraftStringCodec.write(buffer, "Player");
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());

        JavaLoginStartPacket packet = JavaLoginStartPacket.read(buffer);

        assertEquals("Player", packet.username());
        assertEquals(uuid, packet.playerUuid());
        assertNotNull(packet.trailingBytesHex());
        buffer.release();
    }
}
