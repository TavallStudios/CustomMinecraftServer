package com.customminecraftserver.bedrock;

import com.customminecraftserver.session.ConnectionSession;

public interface BedrockLoginAdmission {
    BedrockLoginDecision decide(ConnectionSession session, BedrockLoginPayload payload);
}
