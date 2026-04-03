package com.customminecraftserver.javaedition;

import java.security.PrivateKey;

public record JavaEncryptionChallenge(
        String serverId,
        byte[] publicKeyBytes,
        byte[] verifyToken,
        PrivateKey privateKey
) {
}
