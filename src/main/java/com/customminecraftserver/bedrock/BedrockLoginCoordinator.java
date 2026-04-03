package com.customminecraftserver.bedrock;

import com.customminecraftserver.configuration.AuthMode;
import com.customminecraftserver.session.ConnectionSession;

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
