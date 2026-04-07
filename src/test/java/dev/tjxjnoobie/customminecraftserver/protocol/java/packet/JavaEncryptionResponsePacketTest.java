package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JavaEncryptionResponsePacketTest {
    @Test
    void readsEncryptedResponseArrays() {
        TestLogSupport.logTestStart("JavaEncryptionResponsePacketTest.readsEncryptedResponseArrays");
        ByteBuf buffer = Unpooled.buffer();
        byte[] sharedSecret = new byte[]{1, 2, 3};
        byte[] verifyToken = new byte[]{4, 5};
        MinecraftVarInt.write(buffer, sharedSecret.length);
        buffer.writeBytes(sharedSecret);
        MinecraftVarInt.write(buffer, verifyToken.length);
        buffer.writeBytes(verifyToken);

        JavaEncryptionResponsePacket packet = JavaEncryptionResponsePacket.read(buffer);

        assertArrayEquals(sharedSecret, packet.sharedSecret());
        assertArrayEquals(verifyToken, packet.verifyToken());
        buffer.release();
    }
}
