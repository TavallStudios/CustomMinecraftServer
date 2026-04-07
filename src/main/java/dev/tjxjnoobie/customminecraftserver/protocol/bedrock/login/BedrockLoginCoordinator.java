package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockLoginAdmission;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public final class BedrockLoginCoordinator {
    private final BedrockLoginAdmission offlineAdmission;
    private final BedrockLoginAdmission onlineAdmission;

    public BedrockLoginCoordinator(BedrockLoginAdmission offlineAdmission, BedrockLoginAdmission onlineAdmission) {
        this.offlineAdmission = offlineAdmission;
        this.onlineAdmission = onlineAdmission;
    }

    public BedrockLoginDecision decide(AuthMode authMode, ConnectionSession session, BedrockLoginPayload payload) {
        return switch (authMode) {
            case OFFLINE -> offlineAdmission.decide(session, payload);
            case ONLINE -> onlineAdmission.decide(session, payload);
        };
    }
}


