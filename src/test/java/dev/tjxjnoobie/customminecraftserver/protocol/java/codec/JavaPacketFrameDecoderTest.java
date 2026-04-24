package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaPacketFrameDecoderTest {
    @Test
    void decodesFramedPackets() {
        TestLogSupport.logTestStart("JavaPacketFrameDecoderTest.decodesFramedPackets");
        EmbeddedChannel channel = new EmbeddedChannel(new JavaPacketFrameDecoder());

        ByteBuf payload = Unpooled.buffer();
        payload.writeByte(0x01);
        payload.writeByte(0x02);

        ByteBuf framed = Unpooled.buffer();
        MinecraftVarInt.write(framed, payload.readableBytes());
        framed.writeBytes(payload, payload.readerIndex(), payload.readableBytes());

        channel.writeInbound(framed);
        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals(2, decoded.readableBytes());
        decoded.release();
        payload.release();
        channel.finishAndReleaseAll();
    }
}
