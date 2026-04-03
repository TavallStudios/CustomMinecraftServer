package com.customminecraftserver.javaedition;

import com.customminecraftserver.session.ConnectionSession;

public interface JavaLoginAdmission {
    JavaLoginDecision decide(ConnectionSession session, JavaLoginStartPacket packet);
}
