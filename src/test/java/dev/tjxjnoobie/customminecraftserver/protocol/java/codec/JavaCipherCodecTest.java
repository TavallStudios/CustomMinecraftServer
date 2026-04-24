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

class JavaCipherCodecTest {
    @Test
    void encryptsAndDecryptsFrames() throws Exception {
        TestLogSupport.logTestStart("JavaCipherCodecTest.encryptsAndDecryptsFrames");
        byte[] secret = new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        };
        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
        Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));

        EmbeddedChannel encoderChannel = new EmbeddedChannel(new JavaCipherEncoder(encryptCipher));
        EmbeddedChannel decoderChannel = new EmbeddedChannel(new JavaCipherDecoder(decryptCipher));

        ByteBuf plain = Unpooled.buffer();
        plain.writeBytes(new byte[]{0x01, 0x02, 0x03});

        encoderChannel.writeOutbound(plain.retainedDuplicate());
        ByteBuf encrypted = encoderChannel.readOutbound();
        assertNotNull(encrypted);

        decoderChannel.writeInbound(encrypted.retainedDuplicate());
        ByteBuf decrypted = decoderChannel.readInbound();
        assertNotNull(decrypted);

        assertEquals(ByteBufUtil.hexDump(plain), ByteBufUtil.hexDump(decrypted));
        plain.release();
        encrypted.release();
        decrypted.release();
        encoderChannel.finishAndReleaseAll();
        decoderChannel.finishAndReleaseAll();
    }
}
