package com.customminecraftserver.javaedition;

import com.customminecraftserver.session.ConnectionSession;

public final class OfflineJavaLoginAdmission implements JavaLoginAdmission {
    @Override
    public JavaLoginDecision decide(ConnectionSession session, JavaLoginStartPacket packet) {
        String protocolFamily = session.protocolFamily() == null ? "JAVA_UNKNOWN" : session.protocolFamily();
        String message = "Custom server handshake reached successfully [" + protocolFamily + " OFFLINE]";
        return JavaLoginDecision.disconnect(message, "LOGIN_DISCONNECT");
    }
}
