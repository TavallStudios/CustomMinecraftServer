package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockPeerSessionTest {
    @Test
    void tracksSequenceCountersAndFlags() {
        TestLogSupport.logTestStart("BedrockPeerSessionTest.tracksSequenceCountersAndFlags");
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 19132);
        BedrockPeerSession session = new BedrockPeerSession(address);

        assertEquals(address, session.remoteAddress());
        assertEquals(1400, session.mtu());
        assertFalse(session.compressionNegotiated());

        assertEquals(0, session.nextSequenceNumber());
        assertEquals(1, session.nextSequenceNumber());
        assertEquals(0, session.nextMessageIndex());
        assertEquals(1, session.nextMessageIndex());
        assertEquals(0, session.nextOrderIndex());
        assertEquals(1, session.nextOrderIndex());

        session.compressionNegotiated(true);
        session.resourcePackStackSent(true);
        session.clientCacheStatusReceived(true);
        session.waitingForChunkRadiusRequest(true);
        session.waitingForLocalPlayerInitialization(true);

        assertTrue(session.compressionNegotiated());
        assertTrue(session.resourcePackStackSent());
        assertTrue(session.clientCacheStatusReceived());
        assertTrue(session.waitingForChunkRadiusRequest());
        assertTrue(session.waitingForLocalPlayerInitialization());
    }
}
