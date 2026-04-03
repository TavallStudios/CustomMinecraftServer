package com.customminecraftserver.bedrock;

public record BedrockAuthenticatedIdentity(
        String displayName,
        String identity,
        String xuid,
        String identityPublicKey,
        String handshakePublicKey
) {
}
