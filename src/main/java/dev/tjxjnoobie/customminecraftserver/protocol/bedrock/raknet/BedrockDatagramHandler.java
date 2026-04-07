package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockProtocolMetadata;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session.BedrockGameSessionCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session.BedrockPeerSession;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class BedrockDatagramHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final ServerSettings settings;
    private final ConnectionSessionRegistry registry;
    private final StructuredConnectionLogger logger;
    private final ProtocolVersionDetector versionDetector;
    private final Map<String, BedrockPeerSession> peers = new HashMap<>();
    private final BedrockDatagramTransport transport = new BedrockDatagramTransport();
    private final BedrockGameSessionCoordinator gameSessionCoordinator;
    private final long serverGuid = ThreadLocalRandom.current().nextLong();
    public BedrockDatagramHandler(
            ServerSettings settings,
            ConnectionSessionRegistry registry,
            StructuredConnectionLogger logger,
            ProtocolVersionDetector versionDetector,
            BedrockLoginCoordinator loginCoordinator
    ) {
        this.settings = settings;
        this.registry = registry;
        this.logger = logger;
        this.versionDetector = versionDetector;
        this.gameSessionCoordinator = new BedrockGameSessionCoordinator(logger, versionDetector, loginCoordinator, transport);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        ConnectionSession session = registry.registerBedrock(packet.sender(), settings.authMode());
        ByteBuf content = packet.content();
        int packetId = content.getUnsignedByte(content.readerIndex());

        switch (packetId) {
            case BedrockPacketIds.RAKNET_UNCONNECTED_PING -> handleUnconnectedPing(context, packet, session);
            case BedrockPacketIds.RAKNET_OPEN_CONNECTION_REQUEST_1 -> handleOpenConnectionRequest1(context, packet, session);
            case BedrockPacketIds.RAKNET_OPEN_CONNECTION_REQUEST_2 -> handleOpenConnectionRequest2(context, packet, session);
            case BedrockPacketIds.RAKNET_ACK -> logger.inboundPacket(session, "ACK", Map.of("bedrockStage", "ACK"));
            case BedrockPacketIds.RAKNET_NACK -> logger.inboundPacket(session, "NACK", Map.of("bedrockStage", "NACK"));
            default -> handleUnknownOrConnectedPacket(context, packet, session, packetId);
        }
    }
    private void handleUnknownOrConnectedPacket(
            ChannelHandlerContext context,
            DatagramPacket packet,
            ConnectionSession session,
            int packetId
    ) {
        if (isConnectedDatagram(packetId)) {
            handleConnectedDatagram(context, packet, session);
            return;
        }

        logger.warning(session, "bedrock_packet_unsupported", Map.of(
                "packetId", packetId,
                "payloadHex", previewHex(packet.content(), 64)
        ));
    }

    private boolean isConnectedDatagram(int packetId) {
        return packetId >= BedrockPacketIds.RAKNET_CONNECTED_DATAGRAM_MIN
                && packetId <= BedrockPacketIds.RAKNET_CONNECTED_DATAGRAM_MAX;
    }

    private void handleUnconnectedPing(ChannelHandlerContext context, DatagramPacket packet, ConnectionSession session) {
        ByteBuf in = packet.content().copy();
        in.readByte();
        long time = in.readLong();
        byte[] magic = new byte[BedrockRakNetCodec.RAKNET_MAGIC.length];
        in.readBytes(magic);
        long clientGuid = in.readLong();

        session.state(SessionState.BEDROCK_UNCONNECTED);
        logger.inboundPacket(session, "UNCONNECTED_PING", Map.of(
                "bedrockStage", "UNCONNECTED_PING",
                "clientGuid", clientGuid,
                "pingTime", time
        ));

        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_UNCONNECTED_PONG);
        out.writeLong(time);
        out.writeLong(serverGuid);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        BedrockRakNetCodec.writeRakNetString(out, buildUnconnectedPong());

        logger.outboundPacket(session, "UNCONNECTED_PONG", Map.of("responseType", "UNCONNECTED_PONG"));
        context.writeAndFlush(new DatagramPacket(out, packet.sender()));
    }

    private void handleOpenConnectionRequest1(ChannelHandlerContext context, DatagramPacket packet, ConnectionSession session) {
        ByteBuf in = packet.content().copy();
        in.readByte();
        byte[] magic = new byte[BedrockRakNetCodec.RAKNET_MAGIC.length];
        in.readBytes(magic);
        int rakNetProtocolVersion = in.readUnsignedByte();
        int mtu = Math.min(packet.content().readableBytes() + 28, 1400);

        session.protocolVersion(rakNetProtocolVersion);
        session.protocolFamily(versionDetector.detectBedrock(rakNetProtocolVersion).family());
        transition(session, SessionState.BEDROCK_OPEN_CONNECTION, Map.of("bedrockStage", "OPEN_CONNECTION_REQUEST_1"));
        logger.inboundPacket(session, "OPEN_CONNECTION_REQUEST_1", Map.of(
                "bedrockStage", "OPEN_CONNECTION_REQUEST_1",
                "rakNetProtocolVersion", rakNetProtocolVersion,
                "mtu", mtu
        ));

        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_OPEN_CONNECTION_REPLY_1);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        out.writeLong(serverGuid);
        out.writeBoolean(false);
        out.writeShort(mtu);

        logger.outboundPacket(session, "OPEN_CONNECTION_REPLY_1", Map.of(
                "responseType", "OPEN_CONNECTION_REPLY_1",
                "rakNetProtocolVersion", rakNetProtocolVersion,
                "mtu", mtu
        ));
        context.writeAndFlush(new DatagramPacket(out, packet.sender()));
    }

    private void handleOpenConnectionRequest2(ChannelHandlerContext context, DatagramPacket packet, ConnectionSession session) {
        ByteBuf in = packet.content().copy();
        in.readByte();
        byte[] magic = new byte[BedrockRakNetCodec.RAKNET_MAGIC.length];
        in.readBytes(magic);
        InetSocketAddress clientReportedAddress = BedrockRakNetCodec.readAddress(in);
        int mtu = in.readUnsignedShort();
        long clientGuid = in.readLong();

        BedrockPeerSession peerSession = peerSession((InetSocketAddress) packet.sender());
        peerSession.clientGuid(clientGuid);
        peerSession.mtu(Math.min(mtu, 1400));

        logger.inboundPacket(session, "OPEN_CONNECTION_REQUEST_2", Map.of(
                "bedrockStage", "OPEN_CONNECTION_REQUEST_2",
                "clientGuid", clientGuid,
                "clientReportedAddress", clientReportedAddress,
                "mtu", mtu
        ));

        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_OPEN_CONNECTION_REPLY_2);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        out.writeLong(serverGuid);
        BedrockRakNetCodec.writeAddress(out, new InetSocketAddress(settings.host(), settings.bedrockUdpPort()));
        out.writeShort(mtu);
        out.writeBoolean(false);

        logger.outboundPacket(session, "OPEN_CONNECTION_REPLY_2", Map.of(
                "responseType", "OPEN_CONNECTION_REPLY_2",
                "mtu", mtu
        ));
        context.writeAndFlush(new DatagramPacket(out, packet.sender()));
    }

    private void handleConnectedDatagram(ChannelHandlerContext context, DatagramPacket packet, ConnectionSession session) {
        BedrockPeerSession peerSession = peerSession((InetSocketAddress) packet.sender());
        ByteBuf in = packet.content().copy();
        in.readUnsignedByte();
        int sequenceNumber = BedrockRakNetCodec.readLittleTriad(in);
        transport.sendAck(context, packet.sender(), sequenceNumber);

        while (in.isReadable()) {
            BedrockDatagramFrame frame = transport.readFrame(in);
            ByteBuf payload = transport.reassemble(peerSession, frame);
            if (payload == null) {
                continue;
            }
            try {
                handleConnectedPayload(context, session, peerSession, payload);
            } finally {
                payload.release();
            }
        }
    }

    private void handleConnectedPayload(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf payload
    ) {
        int packetId = payload.getUnsignedByte(payload.readerIndex());
        switch (packetId) {
            case BedrockPacketIds.RAKNET_CONNECTION_REQUEST -> handleConnectionRequest(context, session, peerSession, payload);
            case BedrockPacketIds.RAKNET_NEW_INCOMING_CONNECTION -> handleNewIncomingConnection(session);
            case BedrockPacketIds.RAKNET_CONNECTED_PING -> handleConnectedPing(context, session, peerSession, payload);
            case BedrockPacketIds.RAKNET_DISCONNECT_NOTIFICATION -> handleDisconnectNotification(session, peerSession);
            case BedrockPacketIds.BEDROCK_BATCH -> gameSessionCoordinator.handleBatchPayload(context, session, peerSession, payload);
            default -> logger.warning(session, "bedrock_connected_payload_unsupported", Map.of(
                    "packetId", packetId,
                    "payloadHex", previewHex(payload, 96)
            ));
        }
    }

    private void handleConnectionRequest(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf payload
    ) {
        payload.readByte();
        long clientGuid = payload.readLong();
        long requestTimestamp = payload.readLong();
        payload.readByte();

        logger.inboundPacket(session, "CONNECTION_REQUEST", Map.of(
                "bedrockStage", "CONNECTION_REQUEST",
                "clientGuid", clientGuid,
                "requestTimestamp", requestTimestamp
        ));

        ByteBuf reply = Unpooled.buffer();
        reply.writeByte(BedrockPacketIds.RAKNET_CONNECTION_REQUEST_ACCEPTED);
        BedrockRakNetCodec.writeAddress(reply, peerSession.remoteAddress());
        reply.writeShort(0);
        for (int index = 0; index < 20; index++) {
            BedrockRakNetCodec.writeAddress(reply, new InetSocketAddress("0.0.0.0", 0));
        }
        reply.writeLong(requestTimestamp);
        reply.writeLong(Instant.now().toEpochMilli());

        transport.sendFrame(context, peerSession, reply, BedrockDatagramTransport.RELIABILITY_UNRELIABLE);
        logger.outboundPacket(session, "CONNECTION_REQUEST_ACCEPTED", Map.of("responseType", "CONNECTION_REQUEST_ACCEPTED"));
    }

    private void handleNewIncomingConnection(ConnectionSession session) {
        transition(session, SessionState.BEDROCK_CONNECTED, Map.of("bedrockStage", "NEW_INCOMING_CONNECTION"));
        logger.inboundPacket(session, "NEW_INCOMING_CONNECTION", Map.of("bedrockStage", "NEW_INCOMING_CONNECTION"));
    }

    private void handleConnectedPing(
            ChannelHandlerContext context,
            ConnectionSession session,
            BedrockPeerSession peerSession,
            ByteBuf payload
    ) {
        payload.readByte();
        long clientTimestamp = payload.readLong();
        logger.inboundPacket(session, "CONNECTED_PING", Map.of(
                "bedrockStage", "CONNECTED_PING",
                "clientTimestamp", clientTimestamp
        ));

        ByteBuf reply = Unpooled.buffer();
        reply.writeByte(BedrockPacketIds.RAKNET_CONNECTED_PONG);
        reply.writeLong(clientTimestamp);
        reply.writeLong(Instant.now().toEpochMilli());

        transport.sendFrame(context, peerSession, reply, BedrockDatagramTransport.RELIABILITY_UNRELIABLE);
        logger.outboundPacket(session, "CONNECTED_PONG", Map.of("responseType", "CONNECTED_PONG"));
    }

    private void handleDisconnectNotification(ConnectionSession session, BedrockPeerSession peerSession) {
        session.state(SessionState.DISCONNECTED);
        logger.inboundPacket(session, "DISCONNECT_NOTIFICATION", Map.of("bedrockStage", "DISCONNECT_NOTIFICATION"));
        registry.unregisterBedrock(peerSession.remoteAddress());
        peers.remove(peerSession.remoteAddress().toString());
    }

    private BedrockPeerSession peerSession(InetSocketAddress address) {
        return peers.computeIfAbsent(address.toString(), ignored -> new BedrockPeerSession(address));
    }

    private String buildUnconnectedPong() {
        return "MCPE;" + settings.motd() + ";" + BedrockProtocolMetadata.ADVERTISEMENT_PROTOCOL + ";"
                + BedrockProtocolMetadata.ADVERTISEMENT_VERSION + ";0;" + settings.maxConnections() + ";"
                + serverGuid + ";CustomMvp;Survival;";
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


