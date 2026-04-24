package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import io.netty.util.concurrent.ScheduledFuture;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class BedrockPeerSession {
    private final InetSocketAddress remoteAddress;
    private final Map<Integer, Map<Integer, byte[]>> splitPackets = new HashMap<>();
    private int mtu = 1400;
    private long clientGuid;
    private int nextSequenceNumber;
    private int nextMessageIndex;
    private int nextOrderIndex;
    private boolean compressionNegotiated;
    private BedrockSecureSession secureSession;
    private String terminalMessage;
    private int resourcePackResponses;
    private boolean resourcePackStackSent;
    private boolean clientCacheStatusReceived;
    private boolean waitingForChunkRadiusRequest;
    private boolean waitingForLocalPlayerInitialization;
    private ScheduledFuture<?> initializationTimeout;

    public BedrockPeerSession(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public Map<Integer, Map<Integer, byte[]>> splitPackets() {
        return splitPackets;
    }

    public int mtu() {
        return mtu;
    }

    public void mtu(int mtu) {
        this.mtu = mtu;
    }

    public long clientGuid() {
        return clientGuid;
    }

    public void clientGuid(long clientGuid) {
        this.clientGuid = clientGuid;
    }

    public int nextSequenceNumber() {
        return nextSequenceNumber++;
    }

    public int nextMessageIndex() {
        return nextMessageIndex++;
    }

    public int nextOrderIndex() {
        return nextOrderIndex++;
    }

    public boolean compressionNegotiated() {
        return compressionNegotiated;
    }

    public void compressionNegotiated(boolean compressionNegotiated) {
        this.compressionNegotiated = compressionNegotiated;
    }

    public BedrockSecureSession secureSession() {
        return secureSession;
    }

    public void secureSession(BedrockSecureSession secureSession) {
        this.secureSession = secureSession;
    }

    public String terminalMessage() {
        return terminalMessage;
    }

    public void terminalMessage(String terminalMessage) {
        this.terminalMessage = terminalMessage;
    }

    public int resourcePackResponses() {
        return resourcePackResponses;
    }

    public int incrementResourcePackResponses() {
        resourcePackResponses++;
        return resourcePackResponses;
    }

    public boolean resourcePackStackSent() {
        return resourcePackStackSent;
    }

    public void resourcePackStackSent(boolean resourcePackStackSent) {
        this.resourcePackStackSent = resourcePackStackSent;
    }

    public boolean clientCacheStatusReceived() {
        return clientCacheStatusReceived;
    }

    public void clientCacheStatusReceived(boolean clientCacheStatusReceived) {
        this.clientCacheStatusReceived = clientCacheStatusReceived;
    }

    public boolean waitingForChunkRadiusRequest() {
        return waitingForChunkRadiusRequest;
    }

    public void waitingForChunkRadiusRequest(boolean waitingForChunkRadiusRequest) {
        this.waitingForChunkRadiusRequest = waitingForChunkRadiusRequest;
    }

    public boolean waitingForLocalPlayerInitialization() {
        return waitingForLocalPlayerInitialization;
    }

    public void waitingForLocalPlayerInitialization(boolean waitingForLocalPlayerInitialization) {
        this.waitingForLocalPlayerInitialization = waitingForLocalPlayerInitialization;
    }

    public ScheduledFuture<?> initializationTimeout() {
        return initializationTimeout;
    }

    public void initializationTimeout(ScheduledFuture<?> initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
    }
}

