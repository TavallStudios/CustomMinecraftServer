package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

public record BedrockLoginPayload(
        int protocolVersion,
        String identityJson,
        String clientJwt
) {
}

