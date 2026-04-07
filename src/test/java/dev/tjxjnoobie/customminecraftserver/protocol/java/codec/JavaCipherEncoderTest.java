package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaCipherEncoderTest {
    @Test
    void encodesPlaintextToEncryptedBytes() throws Exception {
        TestLogSupport.logTestStart("JavaCipherEncoderTest.encodesPlaintextToEncryptedBytes");
        byte[] secret = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));

        byte[] plain = new byte[]{0x01, 0x02, 0x03};
        byte[] expectedEncrypted = encryptCipher.update(plain);

        Cipher encoderCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        encoderCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
        EmbeddedChannel channel = new EmbeddedChannel(new JavaCipherEncoder(encoderCipher));

        ByteBuf plainBuffer = Unpooled.wrappedBuffer(plain);
        channel.writeOutbound(plainBuffer.retainedDuplicate());
        ByteBuf encrypted = channel.readOutbound();
        assertNotNull(encrypted);
        assertEquals(ByteBufUtil.hexDump(Unpooled.wrappedBuffer(expectedEncrypted)), ByteBufUtil.hexDump(encrypted));
        encrypted.release();
        channel.finishAndReleaseAll();
        plainBuffer.release();
    }
}
