package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;

import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;

public interface JavaLoginAdmission {
    JavaLoginDecision decide(ConnectionSession session, JavaLoginStartPacket packet);
}


