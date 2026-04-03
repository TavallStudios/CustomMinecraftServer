package com.customminecraftserver.bedrock;

public record BedrockLoginPayload(
        int protocolVersion,
        String identityJson,
        String clientJwt
) {
}
