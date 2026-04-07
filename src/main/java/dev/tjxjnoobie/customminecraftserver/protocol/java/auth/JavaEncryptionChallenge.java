package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import java.security.PrivateKey;

public record JavaEncryptionChallenge(
        String serverId,
        byte[] publicKeyBytes,
        byte[] verifyToken,
        PrivateKey privateKey
) {
}

