package com.customminecraftserver.bedrock;

public record BedrockLoginDecision(
        String disconnectMessage,
        String responseType,
        BedrockAuthenticatedIdentity authenticatedIdentity
) {
}
