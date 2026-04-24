package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaEncryptionChallengeFactoryTest {
    @Test
    void generatesRandomChallengeValues() {
        TestLogSupport.logTestStart("JavaEncryptionChallengeFactoryTest.generatesRandomChallengeValues");
        JavaEncryptionChallengeFactory factory = JavaEncryptionChallengeFactory.createGenerated(1024);
        JavaEncryptionChallenge challenge = factory.create();

        assertNotNull(challenge.serverId());
        assertEquals(8, challenge.serverId().length());
        assertEquals(4, challenge.verifyToken().length);
        assertNotNull(challenge.publicKeyBytes());
        assertNotNull(challenge.privateKey());
    }
}
