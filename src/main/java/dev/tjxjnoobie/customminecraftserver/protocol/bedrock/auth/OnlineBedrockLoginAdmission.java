package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginPayload;

import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public final class OnlineBedrockLoginAdmission implements BedrockLoginAdmission {
    private final BedrockJwtVerifier jwtVerifier;
    private final BedrockAuthenticationSettings settings;

    public OnlineBedrockLoginAdmission(BedrockJwtVerifier jwtVerifier, BedrockAuthenticationSettings settings) {
        this.jwtVerifier = jwtVerifier;
        this.settings = settings;
    }

    @Override
    public BedrockLoginDecision decide(ConnectionSession session, BedrockLoginPayload payload) {
        BedrockAuthenticatedIdentity identity = jwtVerifier.verify(payload.identityJson(), payload.clientJwt(), settings);
        String message = "Custom server auth reached successfully [BEDROCK protocol=" + payload.protocolVersion() + " ONLINE xuid=" + identity.xuid() + "]";
        return new BedrockLoginDecision(message, "DISCONNECT_ONLINE_VERIFIED", identity);
    }
}


