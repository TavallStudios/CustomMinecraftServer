package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.protocol.java.state.JavaNextState;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaHandshakePacketTest {
    @Test
    void readsHandshakeFields() {
        TestLogSupport.logTestStart("JavaHandshakePacketTest.readsHandshakeFields");
        ByteBuf buffer = Unpooled.buffer();
        MinecraftVarInt.write(buffer, 765);
        MinecraftStringCodec.write(buffer, "localhost");
        buffer.writeShort(25565);
        MinecraftVarInt.write(buffer, JavaNextState.LOGIN.id());

        JavaHandshakePacket packet = JavaHandshakePacket.read(buffer);

        assertEquals(765, packet.protocolVersion());
        assertEquals("localhost", packet.serverAddress());
        assertEquals(25565, packet.port());
        assertEquals(JavaNextState.LOGIN, packet.nextState());
        buffer.release();
    }
}
