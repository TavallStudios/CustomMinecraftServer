package com.customminecraftserver.javaedition;

public record JavaSessionVerificationRequest(
        String username,
        String serverId,
        byte[] sharedSecret,
        byte[] publicKeyBytes,
        String remoteAddress
) {
}
