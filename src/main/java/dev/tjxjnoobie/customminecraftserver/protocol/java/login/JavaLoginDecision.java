package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallenge;

public record JavaLoginDecision(
        JavaLoginAction action,
        String responseType,
        String disconnectMessage,
        JavaEncryptionChallenge encryptionChallenge
) {
    public static JavaLoginDecision disconnect(String disconnectMessage, String responseType) {
        return new JavaLoginDecision(JavaLoginAction.DISCONNECT, responseType, disconnectMessage, null);
    }

    public static JavaLoginDecision requestEncryption(JavaEncryptionChallenge challenge) {
        return new JavaLoginDecision(JavaLoginAction.REQUEST_ENCRYPTION, "ENCRYPTION_REQUEST", null, challenge);
    }
}


