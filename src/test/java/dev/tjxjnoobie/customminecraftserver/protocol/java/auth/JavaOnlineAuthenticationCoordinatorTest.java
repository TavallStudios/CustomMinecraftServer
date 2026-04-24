package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaPostLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaPacketIds;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaOnlineAuthenticationCoordinatorTest {
    @Test
    void requestEncryptionWritesPacketAndTransitionsState() {
        TestLogSupport.logTestStart("JavaOnlineAuthenticationCoordinatorTest.requestEncryptionWritesPacketAndTransitionsState");
        JavaEncryptionChallengeFactory factory = JavaEncryptionChallengeFactory.createGenerated(1024);
        JavaEncryptionChallenge challenge = factory.create();

        JavaOnlineAuthenticationCoordinator coordinator = new JavaOnlineAuthenticationCoordinator(
                new StructuredConnectionLogger(),
                request -> CompletableFuture.completedFuture(new JavaSessionVerificationResult("id", "name")),
                new JavaPostLoginCoordinator(new StructuredConnectionLogger())
        );

        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, dev.tjxjnoobie.customminecraftserver.config.AuthMode.ONLINE);
        session.protocolVersion(769);

        JavaLoginDecision decision = JavaLoginDecision.requestEncryption(challenge);
        coordinator.requestEncryption(context, session, decision);

        ByteBuf outbound = channel.readOutbound();
        assertNotNull(outbound);
        MinecraftVarInt.read(outbound);
        assertEquals(JavaPacketIds.ENCRYPTION_REQUEST, MinecraftVarInt.read(outbound));
        assertEquals(SessionState.JAVA_ENCRYPTION_NEGOTIATION, session.state());
        outbound.release();
        channel.finishAndReleaseAll();
    }
}
