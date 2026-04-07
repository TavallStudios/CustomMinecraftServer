package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockGamePacketWriter;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockProtocolMetadata;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockStartGameTemplates;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramTransport;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;

import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.TimeUnit;

final class BedrockInitializationCoordinator {
    private static final long CHUNK_RADIUS_TIMEOUT_MILLIS = 2000L;
    private static final long LOCAL_PLAYER_INITIALIZATION_TIMEOUT_MILLIS = 2000L;
    private static final int RESOURCE_PACK_STATUS_COMPLETED = 4;

    private final StructuredConnectionLogger logger;
    private final BedrockDatagramTransport transport;

    BedrockInitializationCoordinator(StructuredConnectionLogger logger, BedrockDatagramTransport transport) {
        this.logger = logger;
        this.transport = transport;
    }

    void handleResourcePackClientResponse(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int responseStatus = packet.readUnsignedByte();
        int resourcePackCount = packet.readUnsignedShortLE();
        for (int index = 0; index < resourcePackCount; index++) {
            BedrockRakNetCodec.readString(packet);
        }

        int responseNumber = peerSession.incrementResourcePackResponses();
        String responseStatusName = resourcePackStatusName(responseStatus);
        logger.inboundPacket(session, "RESOURCE_PACK_CLIENT_RESPONSE", Map.of(
                "responseStatus", responseStatusName,
                "resourcePackCount", resourcePackCount,
                "responseNumber", responseNumber
        ));

        if (responseStatus != RESOURCE_PACK_STATUS_COMPLETED) {
            sendDisconnect(context, session, peerSession, "Bedrock resource-pack response unsupported: " + responseStatusName);
            return;
        }

        if (!peerSession.resourcePackStackSent()) {
            peerSession.resourcePackStackSent(true);
            sendEncryptedPacket(
                    context,
                    session,
                    peerSession,
                    BedrockGamePacketWriter.resourcePackStack(
                            context.alloc(),
                            currentProtocolVersion(session),
                            BedrockProtocolMetadata.gameVersion(session.protocolVersion())
                    ),
                    "RESOURCE_PACK_STACK",
                    Map.of("gameVersion", BedrockProtocolMetadata.gameVersion(session.protocolVersion()))
            );
            return;
        }

        transition(session, SessionState.BEDROCK_WAITING_CHUNK_RADIUS, Map.of("reason", "RESOURCE_PACK_NEGOTIATION_COMPLETED"));
        peerSession.waitingForChunkRadiusRequest(true);
        scheduleChunkRadiusTimeout(context, session, peerSession);
    }

    void handleClientCacheStatus(ConnectionSession session, BedrockPeerSession peerSession, ByteBuf packet) {
        boolean enabled = packet.readBoolean();
        peerSession.clientCacheStatusReceived(true);
        logger.inboundPacket(session, "CLIENT_CACHE_STATUS", Map.of("enabled", enabled));
    }

    void handleRequestChunkRadius(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        int requestedChunkRadius = BedrockRakNetCodec.decodeZigZag32(BedrockRakNetCodec.readUnsignedVarInt(packet));
        int maxRadius = packet.readUnsignedByte();
        logger.inboundPacket(session, "REQUEST_CHUNK_RADIUS", Map.of(
                "requestedChunkRadius", requestedChunkRadius,
                "maxRadius", maxRadius
        ));

        if (!peerSession.waitingForChunkRadiusRequest()) {
            return;
        }

        peerSession.waitingForChunkRadiusRequest(false);
        cancelInitializationTimeout(peerSession);
        sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.chunkRadiusUpdate(context.alloc(), requestedChunkRadius),
                "CHUNK_RADIUS_UPDATE",
                Map.of("chunkRadius", requestedChunkRadius)
        );
        sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.startGame(context.alloc(), currentProtocolVersion(session)),
                "START_GAME",
                Map.of(
                        "runtimeEntityId", BedrockGamePacketWriter.START_GAME_RUNTIME_ENTITY_ID,
                        "gameVersion", BedrockProtocolMetadata.gameVersion(session.protocolVersion())
                )
        );
        sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.playStatusPlayerSpawn(context.alloc()),
                "PLAY_STATUS",
                Map.of("status", "player_spawn")
        );

        transition(session, SessionState.BEDROCK_WAITING_LOCAL_PLAYER_INITIALIZATION, Map.of("reason", "START_GAME_BOOTSTRAPPED"));
        peerSession.waitingForLocalPlayerInitialization(true);
        scheduleLocalPlayerInitializationTimeout(context, session, peerSession);
    }

    void handleSetLocalPlayerAsInitialized(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet
    ) {
        long runtimeEntityId = BedrockRakNetCodec.readUnsignedVarLong(packet);
        logger.inboundPacket(session, "SET_LOCAL_PLAYER_AS_INITIALIZED", Map.of("runtimeEntityId", runtimeEntityId));

        if (!peerSession.waitingForLocalPlayerInitialization()) {
            return;
        }
        if (runtimeEntityId != BedrockGamePacketWriter.START_GAME_RUNTIME_ENTITY_ID) {
            sendDisconnect(
                    context,
                    session,
                    peerSession,
                    "Bedrock local player initialization used an unexpected runtime entity id: " + runtimeEntityId
            );
            return;
        }

        peerSession.waitingForLocalPlayerInitialization(false);
        cancelInitializationTimeout(peerSession);
        transition(session, SessionState.BEDROCK_CONNECTED, Map.of("reason", "LOCAL_PLAYER_INITIALIZED"));
        sendEncryptedPacket(
                context,
                session,
                peerSession,
                BedrockGamePacketWriter.disconnect(context.alloc(), peerSession.terminalMessage()),
                "DISCONNECT",
                Map.of("disconnectMessage", peerSession.terminalMessage())
        );
    }

    void sendDisconnect(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            String message
    ) {
        cancelInitializationTimeout(peerSession);
        peerSession.waitingForChunkRadiusRequest(false);
        peerSession.waitingForLocalPlayerInitialization(false);

        ByteBuf packet = BedrockGamePacketWriter.disconnect(context.alloc(), message);
        ByteBuf outbound = peerSession.secureSession() != null
                ? peerSession.secureSession().encryptPacket(context.alloc(), packet)
                : transport.wrapBatch(packet, true);
        transport.sendFrame(context, peerSession, outbound, BedrockDatagramTransport.RELIABILITY_RELIABLE_ORDERED);
        logger.outboundPacket(session, "DISCONNECT", Map.of(
                "responseType", "DISCONNECT",
                "disconnectMessage", message
        ));
    }

    void sendEncryptedPacket(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf packet,
            String responseType,
            Map<String, ?> details
    ) {
        transport.sendFrame(
                context,
                peerSession,
                peerSession.secureSession().encryptPacket(context.alloc(), packet),
                BedrockDatagramTransport.RELIABILITY_RELIABLE_ORDERED
        );
        logger.outboundPacket(session, responseType, details);
    }

    private int currentProtocolVersion(ConnectionSession session) {
        return session.protocolVersion() == null ? BedrockProtocolMetadata.ADVERTISEMENT_PROTOCOL : session.protocolVersion();
    }

    private String resourcePackStatusName(int status) {
        return switch (status) {
            case 0 -> "none";
            case 1 -> "refused";
            case 2 -> "send_packs";
            case 3 -> "have_all_packs";
            case RESOURCE_PACK_STATUS_COMPLETED -> "completed";
            default -> "unknown_" + status;
        };
    }

    private void scheduleChunkRadiusTimeout(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession
    ) {
        scheduleInitializationTimeout(
                context,
                peerSession,
                CHUNK_RADIUS_TIMEOUT_MILLIS,
                () -> {
                    if (!peerSession.waitingForChunkRadiusRequest()) {
                        return;
                    }
                    peerSession.waitingForChunkRadiusRequest(false);
                    sendDisconnect(context, session, peerSession, peerSession.terminalMessage());
                }
        );
    }

    private void scheduleLocalPlayerInitializationTimeout(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession
    ) {
        scheduleInitializationTimeout(
                context,
                peerSession,
                LOCAL_PLAYER_INITIALIZATION_TIMEOUT_MILLIS,
                () -> {
                    if (!peerSession.waitingForLocalPlayerInitialization()) {
                        return;
                    }
                    peerSession.waitingForLocalPlayerInitialization(false);
                    sendDisconnect(context, session, peerSession, peerSession.terminalMessage());
                }
        );
    }

    private void scheduleInitializationTimeout(
            ChannelHandlerContext context,
            BedrockPeerSession peerSession,
            long timeoutMillis,
            Runnable action
    ) {
        cancelInitializationTimeout(peerSession);
        peerSession.initializationTimeout(context.executor().schedule(action, timeoutMillis, TimeUnit.MILLISECONDS));
    }

    private void cancelInitializationTimeout(BedrockPeerSession peerSession) {
        if (peerSession.initializationTimeout() != null) {
            peerSession.initializationTimeout().cancel(false);
            peerSession.initializationTimeout(null);
        }
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
    }
}


