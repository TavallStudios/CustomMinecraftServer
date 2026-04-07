package dev.tjxjnoobie.customminecraftserver.protocol.java.state;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaChannelStateTest {
    @Test
    void buildsTerminalMessageFromSessionOrOverride() {
        TestLogSupport.logTestStart("JavaChannelStateTest.buildsTerminalMessageFromSessionOrOverride");
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.protocolFamily("JAVA_1_8_X");

        String defaultMessage = JavaChannelState.terminalMessage(context, session);
        assertEquals("Custom server handshake reached successfully [JAVA_1_8_X OFFLINE]", defaultMessage);

        JavaChannelState.terminalMessage(context, "Override message");
        assertEquals("Override message", JavaChannelState.terminalMessage(context, session));

        channel.finishAndReleaseAll();
    }
}
