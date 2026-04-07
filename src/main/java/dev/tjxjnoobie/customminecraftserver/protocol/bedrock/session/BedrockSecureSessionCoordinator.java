package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockGamePacketWriter;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockProtocolMetadata;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramTransport;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;

import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;

final class BedrockSecureSessionCoordinator {
    private final StructuredConnectionLogger logger;
    private final BedrockDatagramTransport transport;
    private final BedrockInitializationCoordinator initializationCoordinator;

    BedrockSecureSessionCoordinator(
            StructuredConnectionLogger logger,
            BedrockDatagramTransport transport,
            BedrockInitializationCoordinator initializationCoordinator
    ) {
        this.logger = logger;
        this.transport = transport;
        this.initializationCoordinator = initializationCoordinator;
    }

    void handleEncryptedBedrockBatch(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf payload
    ) {
        List<ByteBuf> packets = peerSession.secureSession().decryptPackets(payload);
        for (ByteBuf packet : packets) {
            try {
                handleEncryptedGamePacket(context, session, peerSession, packet);
            } finally {
                packet.release();
            }
        }
    }

    void sendServerHandshake(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession
    ) {
        ByteBuf packet = BedrockGamePacketWriter.serverToClientHandshake(
                context.alloc(),
                peerSession.secureSession().serverHandshakeToken()
        );
        transport.sendFrame(
                context,
                peerSession,
                transport.wrapBatch(packet, true),
                BedrockDatagramTransport.RELIABILITY_RELIABLE_ORDERED
        );
        logger.outboundPacket(session, "SERVER_TO_CLIENT_HANDSHAKE", Map.of(
                "responseType", "SERVER_TO_CLIENT_HANDSHAKE",
                "serverPublicKey", peerSession.secureSession().serverPublicKeyBase64()
        ));
    }

    void sendDisconnect(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            String message
    ) {
        initializationCoordinator.sendDisconnect(context, session, peerSession, message);
    }

    private void handleEncryptedGamePacket(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int packetId = BedrockRakNetCodec.readUnsignedVarInt(packet);
        switch (packetId) {
            case BedrockPacketIds.BEDROCK_CLIENT_TO_SERVER_HANDSHAKE -> handleClientToServerHandshake(context, session, peerSession);
            case BedrockPacketIds.BEDROCK_RESOURCE_PACK_CLIENT_RESPONSE ->
                    initializationCoordinator.handleResourcePackClientResponse(context, session, peerSession, packet);
            case BedrockPacketIds.BEDROCK_REQUEST_CHUNK_RADIUS ->
                    initializationCoordinator.handleRequestChunkRadius(context, session, peerSession, packet);
            case BedrockPacketIds.BEDROCK_SET_LOCAL_PLAYER_AS_INITIALIZED ->
                    initializationCoordinator.handleSetLocalPlayerAsInitialized(context, session, peerSession, packet);
            case BedrockPacketIds.BEDROCK_CLIENT_CACHE_STATUS ->
                    initializationCoordinator.handleClientCacheStatus(session, peerSession, packet);
            default -> logger.warning(session, "bedrock_secure_session_packet_unsupported", Map.of(
                    "packetId", packetId,
                    "payloadHex", previewHex(packet, 96)
            ));
        }
    }

    private void handleClientToServerHandshake(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession
    ) {
        logger.inboundPacket(session, "CLIENT_TO_SERVER_HANDSHAKE", Map.of("bedrockStage", "CLIENT_TO_SERVER_HANDSHAKE"));
        transition(session, SessionState.BEDROCK_INITIALIZING, Map.of("reason", "SECURE_SESSION_ESTABLISHED"));

        initializationCoordinator.sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.playStatusLoginSuccess(context.alloc()),
                "PLAY_STATUS",
                Map.of("status", "login_success")
        );
        initializationCoordinator.sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.resourcePacksInfo(context.alloc()),
                "RESOURCE_PACKS_INFO",
                Map.of("responseType", "RESOURCE_PACKS_INFO")
        );
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
    }

    private String previewHex(ByteBuf payload, int maxBytes) {
        return ByteBufUtil.hexDump(payload, payload.readerIndex(), Math.min(payload.readableBytes(), maxBytes));
    }
}


