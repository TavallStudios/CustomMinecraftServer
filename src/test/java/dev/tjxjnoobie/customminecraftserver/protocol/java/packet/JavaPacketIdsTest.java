package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaPacketIdsTest {
    @Test
    void packetIdsExposeHandshakeAndStatusConstants() {
        TestLogSupport.logTestStart("JavaPacketIdsTest.packetIdsExposeHandshakeAndStatusConstants");
        assertEquals(0x00, JavaPacketIds.HANDSHAKE);
        assertEquals(JavaPacketIds.STATUS_REQUEST, JavaPacketIds.HANDSHAKE);
        assertEquals(JavaPacketIds.STATUS_PING, JavaPacketIds.STATUS_PONG);
    }
}
