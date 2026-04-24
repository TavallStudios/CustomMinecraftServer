package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSessionDigestTest {
    @Test
    void computesServerHashWithSha1() throws Exception {
        TestLogSupport.logTestStart("JavaSessionDigestTest.computesServerHashWithSha1");
        String serverId = "abcd";
        byte[] sharedSecret = new byte[]{1, 2, 3, 4};
        byte[] publicKey = new byte[]{5, 6, 7, 8};

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        digest.update(sharedSecret);
        digest.update(publicKey);
        String expected = new BigInteger(digest.digest()).toString(16);

        assertEquals(expected, JavaSessionDigest.serverHash(serverId, sharedSecret, publicKey));
    }
}
