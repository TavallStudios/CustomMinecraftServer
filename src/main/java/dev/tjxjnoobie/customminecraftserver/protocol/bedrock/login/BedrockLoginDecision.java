package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockAuthenticatedIdentity;

public record BedrockLoginDecision(
        String disconnectMessage,
        String responseType,
        BedrockAuthenticatedIdentity authenticatedIdentity
) {
}


