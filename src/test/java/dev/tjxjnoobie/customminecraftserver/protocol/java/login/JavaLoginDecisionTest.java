package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallenge;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaLoginDecisionTest {
    @Test
    void factoriesCreateExpectedDecisionTypes() throws Exception {
        TestLogSupport.logTestStart("JavaLoginDecisionTest.factoriesCreateExpectedDecisionTypes");
        JavaLoginDecision disconnect = JavaLoginDecision.disconnect("bye", "LOGIN_DISCONNECT");
        assertEquals(JavaLoginAction.DISCONNECT, disconnect.action());
        assertEquals("bye", disconnect.disconnectMessage());

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        JavaEncryptionChallenge challenge = new JavaEncryptionChallenge("id", keyPair.getPublic().getEncoded(), new byte[]{1}, keyPair.getPrivate());
        JavaLoginDecision request = JavaLoginDecision.requestEncryption(challenge);
        assertEquals(JavaLoginAction.REQUEST_ENCRYPTION, request.action());
        assertNotNull(request.encryptionChallenge());
    }
}
