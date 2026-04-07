package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import dev.tjxjnoobie.customminecraftserver.test.BedrockClientSessionCrypto;
import dev.tjxjnoobie.customminecraftserver.test.BedrockJwtTestSupport;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockSecureSessionTest {
    @Test
    void encryptsAndDecryptsPacketsBetweenClientAndServer() throws Exception {
        TestLogSupport.logTestStart("BedrockSecureSessionTest.encryptsAndDecryptsPacketsBetweenClientAndServer");
        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        String clientPublicKey = BedrockJwtTestSupport.toBase64Der(clientKey);

        BedrockSecureSession serverSession = BedrockSecureSession.create(clientPublicKey);
        BedrockClientSessionCrypto clientSession = new BedrockClientSessionCrypto(
                clientKey.getPrivate(),
                serverSession.serverHandshakeToken()
        );

        ByteBuf clientPacket = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(clientPacket, BedrockPacketIds.BEDROCK_CLIENT_CACHE_STATUS);
        clientPacket.writeBoolean(true);

        ByteBuf encryptedForServer = clientSession.encrypt(clientPacket);
        List<ByteBuf> decryptedPackets = serverSession.decryptPackets(encryptedForServer);
        ByteBuf decryptedPacket = decryptedPackets.get(0);
        assertEquals(ByteBufUtil.hexDump(clientPacket), ByteBufUtil.hexDump(decryptedPacket));
        decryptedPacket.release();
        encryptedForServer.release();
        clientPacket.release();

        ByteBuf serverPacket = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(serverPacket, BedrockPacketIds.BEDROCK_PLAY_STATUS);
        serverPacket.writeIntLE(0);

        ByteBuf encryptedForClient = serverSession.encryptPacket(UnpooledByteBufAllocator.DEFAULT, serverPacket);
        ByteBuf decryptedForClient = clientSession.decryptSinglePacket(encryptedForClient);
        assertEquals(ByteBufUtil.hexDump(serverPacket), ByteBufUtil.hexDump(decryptedForClient));
        decryptedForClient.release();
        encryptedForClient.release();
        serverPacket.release();
    }
}
