package com.customminecraftserver.session;

import com.customminecraftserver.configuration.AuthMode;

import java.util.UUID;

public final class ConnectionSession {
    private final UUID sessionId;
    private final String remoteAddress;
    private final ConnectionEdition edition;
    private final AuthMode authMode;
    private SessionState state;
    private Integer protocolVersion;
    private String protocolFamily;
    private String username;
    private String requestedHost;
    private Integer requestedPort;
    private String authenticatedIdentity;
    private String authenticatedXuid;

    public ConnectionSession(String remoteAddress, ConnectionEdition edition, AuthMode authMode) {
        this.sessionId = UUID.randomUUID();
        this.remoteAddress = remoteAddress;
        this.edition = edition;
        this.authMode = authMode;
        this.state = SessionState.CONNECTED;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public String remoteAddress() {
        return remoteAddress;
    }

    public ConnectionEdition edition() {
        return edition;
    }

    public AuthMode authMode() {
        return authMode;
    }

    public SessionState state() {
        return state;
    }

    public void state(SessionState state) {
        this.state = state;
    }

    public Integer protocolVersion() {
        return protocolVersion;
    }

    public void protocolVersion(Integer protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String protocolFamily() {
        return protocolFamily;
    }

    public void protocolFamily(String protocolFamily) {
        this.protocolFamily = protocolFamily;
    }

    public String username() {
        return username;
    }

    public void username(String username) {
        this.username = username;
    }

    public String requestedHost() {
        return requestedHost;
    }

    public void requestedHost(String requestedHost) {
        this.requestedHost = requestedHost;
    }

    public Integer requestedPort() {
        return requestedPort;
    }

    public void requestedPort(Integer requestedPort) {
        this.requestedPort = requestedPort;
    }

    public String authenticatedIdentity() {
        return authenticatedIdentity;
    }

    public void authenticatedIdentity(String authenticatedIdentity) {
        this.authenticatedIdentity = authenticatedIdentity;
    }

    public String authenticatedXuid() {
        return authenticatedXuid;
    }

    public void authenticatedXuid(String authenticatedXuid) {
        this.authenticatedXuid = authenticatedXuid;
    }
}
