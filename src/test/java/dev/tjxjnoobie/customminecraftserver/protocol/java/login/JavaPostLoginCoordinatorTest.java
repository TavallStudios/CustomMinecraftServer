package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.java.state.JavaChannelState;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaPostLoginCoordinatorTest {
    @Test
    void legacyPostLoginSendsDisconnectAfterLoginSuccess() {
        TestLogSupport.logTestStart("JavaPostLoginCoordinatorTest.legacyPostLoginSendsDisconnectAfterLoginSuccess");
        JavaPostLoginCoordinator coordinator = new JavaPostLoginCoordinator(new StructuredConnectionLogger());
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.protocolVersion(47);
        session.protocolFamily("JAVA_1_8_X");
        session.username("LegacyUser");
        JavaChannelState.terminalMessage(context, "Legacy disconnect");

        coordinator.beginPostLoginFlow(context, session, "OFFLINE_LOGIN_ACCEPTED");

        ByteBuf loginSuccess = channel.readOutbound();
        ByteBuf disconnect = channel.readOutbound();

        assertNotNull(loginSuccess);
        assertNotNull(disconnect);

        MinecraftVarInt.read(loginSuccess);
        assertEquals(JavaPacketIds.LOGIN_SUCCESS, MinecraftVarInt.read(loginSuccess));

        MinecraftVarInt.read(disconnect);
        assertEquals(JavaPacketIds.PLAY_LEGACY_DISCONNECT, MinecraftVarInt.read(disconnect));

        loginSuccess.release();
        disconnect.release();
        channel.finishAndReleaseAll();
    }
}
