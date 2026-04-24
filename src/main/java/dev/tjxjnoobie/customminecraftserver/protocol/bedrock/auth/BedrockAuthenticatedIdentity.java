package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

public record BedrockAuthenticatedIdentity(
        String displayName,
        String identity,
        String xuid,
        String identityPublicKey,
        String handshakePublicKey
) {
}

