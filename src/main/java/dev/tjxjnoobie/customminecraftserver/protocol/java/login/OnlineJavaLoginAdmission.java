package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallengeFactory;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;

import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public final class OnlineJavaLoginAdmission implements JavaLoginAdmission {
    private final JavaEncryptionChallengeFactory challengeFactory;

    public OnlineJavaLoginAdmission() {
        this(JavaEncryptionChallengeFactory.createGenerated(1024));
    }

    public OnlineJavaLoginAdmission(JavaEncryptionChallengeFactory challengeFactory) {
        this.challengeFactory = challengeFactory;
    }

    @Override
    public JavaLoginDecision decide(ConnectionSession session, JavaLoginStartPacket packet) {
        return JavaLoginDecision.requestEncryption(challengeFactory.create());
    }
}


