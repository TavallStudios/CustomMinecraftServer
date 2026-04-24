package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public final class JavaLoginCoordinator {
    private final JavaLoginAdmission offlineAdmission;
    private final JavaLoginAdmission onlineAdmission;

    public JavaLoginCoordinator(JavaLoginAdmission offlineAdmission, JavaLoginAdmission onlineAdmission) {
        this.offlineAdmission = offlineAdmission;
        this.onlineAdmission = onlineAdmission;
    }

    public JavaLoginDecision decide(AuthMode authMode, ConnectionSession session, JavaLoginStartPacket packet) {
        return switch (authMode) {
            case OFFLINE -> offlineAdmission.decide(session, packet);
            case ONLINE -> onlineAdmission.decide(session, packet);
        };
    }
}


