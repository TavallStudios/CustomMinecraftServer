package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallenge;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaPacketWriterTest {
    @Test
    void writesEncryptionRequestWithModernFlag() throws Exception {
        TestLogSupport.logTestStart("JavaPacketWriterTest.writesEncryptionRequestWithModernFlag");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        JavaEncryptionChallenge challenge = new JavaEncryptionChallenge(
                "abcd",
                keyPair.getPublic().getEncoded(),
                new byte[]{1, 2, 3, 4},
                keyPair.getPrivate()
        );

        ByteBuf frame = JavaPacketWriter.encryptionRequest(UnpooledByteBufAllocator.DEFAULT, 769, challenge);
        MinecraftVarInt.read(frame);
        assertEquals(JavaPacketIds.ENCRYPTION_REQUEST, MinecraftVarInt.read(frame));
        assertEquals("abcd", MinecraftStringCodec.read(frame, 16));
        int publicKeyLength = MinecraftVarInt.read(frame);
        frame.skipBytes(publicKeyLength);
        int tokenLength = MinecraftVarInt.read(frame);
        frame.skipBytes(tokenLength);
        assertEquals(1, frame.readBoolean() ? 1 : 0);
        frame.release();
    }

    @Test
    void writesStatusResponsePayload() {
        TestLogSupport.logTestStart("JavaPacketWriterTest.writesStatusResponsePayload");
        String payload = "{\"text\":\"ok\"}";
        ByteBuf frame = JavaPacketWriter.statusResponse(UnpooledByteBufAllocator.DEFAULT, payload);
        MinecraftVarInt.read(frame);
        assertEquals(JavaPacketIds.STATUS_RESPONSE, MinecraftVarInt.read(frame));
        assertEquals(payload, MinecraftStringCodec.read(frame, 256));
        frame.release();
    }
}
