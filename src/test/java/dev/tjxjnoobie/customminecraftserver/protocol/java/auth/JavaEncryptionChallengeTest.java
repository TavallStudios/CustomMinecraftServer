package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaEncryptionChallengeTest {
    @Test
    void recordCarriesChallengeFields() throws Exception {
        TestLogSupport.logTestStart("JavaEncryptionChallengeTest.recordCarriesChallengeFields");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        JavaEncryptionChallenge challenge = new JavaEncryptionChallenge(
                "abcd",
                keyPair.getPublic().getEncoded(),
                new byte[]{1, 2, 3, 4},
                keyPair.getPrivate()
        );

        assertEquals("abcd", challenge.serverId());
        assertEquals(4, challenge.verifyToken().length);
        assertNotNull(challenge.privateKey());
    }
}
