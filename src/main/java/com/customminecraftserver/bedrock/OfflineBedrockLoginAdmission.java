package com.customminecraftserver.bedrock;

import com.customminecraftserver.session.ConnectionSession;

public final class OfflineBedrockLoginAdmission implements BedrockLoginAdmission {
    private final BedrockJwtVerifier jwtVerifier;

    public OfflineBedrockLoginAdmission(BedrockJwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    public BedrockLoginDecision decide(ConnectionSession session, BedrockLoginPayload payload) {
        String username = jwtVerifier.extractDisplayName(payload.identityJson(), payload.clientJwt());
        String handshakePublicKey = jwtVerifier.extractHandshakePublicKey(payload.identityJson(), payload.clientJwt());
        String message = "Custom server handshake reached successfully [BEDROCK protocol=" + payload.protocolVersion() + " OFFLINE]";
        return new BedrockLoginDecision(
                message,
                "DISCONNECT",
                new BedrockAuthenticatedIdentity(username, null, null, null, handshakePublicKey)
        );
    }
}
