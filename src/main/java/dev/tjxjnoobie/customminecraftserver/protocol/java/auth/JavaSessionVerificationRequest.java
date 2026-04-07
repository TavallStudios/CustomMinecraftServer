package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

public record JavaSessionVerificationRequest(
        String username,
        String serverId,
        byte[] sharedSecret,
        byte[] publicKeyBytes,
        String remoteAddress
) {
}

