package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginPayload;

import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public interface BedrockLoginAdmission {
    BedrockLoginDecision decide(ConnectionSession session, BedrockLoginPayload payload);
}


