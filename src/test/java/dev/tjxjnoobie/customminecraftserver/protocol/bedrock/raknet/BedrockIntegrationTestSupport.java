package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BedrockIntegrationTestSupport {
    private BedrockIntegrationTestSupport() {
    }

    static DatagramPacket datagram(ByteBuf content, InetSocketAddress serverAddress, InetSocketAddress clientAddress) {
        return new DatagramPacket(content, serverAddress, clientAddress);
    }
    static ByteBuf unconnectedPing() {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_UNCONNECTED_PING);
        out.writeLong(1L);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        out.writeLong(99L);
        return out;
    }
    static ByteBuf openConnectionRequest1(int protocolVersion, int mtu) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_OPEN_CONNECTION_REQUEST_1);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        out.writeByte(protocolVersion);
        int padding = Math.max(0, mtu - out.readableBytes() - 28);
        out.writeZero(padding);
        return out;
    }
    static ByteBuf openConnectionRequest2(InetSocketAddress serverAddress, int mtu, long clientGuid) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_OPEN_CONNECTION_REQUEST_2);
        out.writeBytes(BedrockRakNetCodec.RAKNET_MAGIC);
        BedrockRakNetCodec.writeAddress(out, serverAddress);
        out.writeShort(mtu);
        out.writeLong(clientGuid);
        return out;
    }
    static ByteBuf connectedDatagram(int sequenceNumber, int reliability, ByteBuf payload) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_CONNECTED_DATAGRAM_MIN);
        BedrockRakNetCodec.writeLittleTriad(out, sequenceNumber);
        out.writeByte(reliability << 5);
        out.writeShort(payload.readableBytes() << 3);
        out.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        return out;
    }
    static ByteBuf connectedReliableDatagram(int sequenceNumber, int messageIndex, int orderIndex, ByteBuf payload) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_CONNECTED_DATAGRAM_MIN);
        BedrockRakNetCodec.writeLittleTriad(out, sequenceNumber);
        out.writeByte(3 << 5);
        out.writeShort(payload.readableBytes() << 3);
        BedrockRakNetCodec.writeLittleTriad(out, messageIndex);
        BedrockRakNetCodec.writeLittleTriad(out, orderIndex);
        out.writeByte(0);
        out.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        return out;
    }
    static ByteBuf connectionRequestPayload(long clientGuid, long requestTimestamp) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.RAKNET_CONNECTION_REQUEST);
        out.writeLong(clientGuid);
        out.writeLong(requestTimestamp);
        out.writeByte(0);
        return out;
    }
    static ByteBuf singleBytePayload(int value) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(value);
        return out;
    }
    static ByteBuf requestNetworkSettingsBatch(int protocolVersion) {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_REQUEST_NETWORK_SETTINGS);
        packet.writeInt(protocolVersion);
        return batch(packet, false);
    }
    static ByteBuf offlineLoginBatch(int protocolVersion, String username, KeyPair clientKey) {
        ByteBuf login = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(login, BedrockPacketIds.BEDROCK_LOGIN);
        login.writeInt(protocolVersion);

        ByteBuf tokens = Unpooled.buffer();
        String identity = "{\"chain\":[]}";
        String clientJwt;
        try {
            clientJwt = BedrockJwtTestSupport.signEs384Jwt(
                    clientKey,
                    BedrockJwtTestSupport.toBase64Der(clientKey),
                    "{\"ThirdPartyName\":\"" + username + "\"}"
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        tokens.writeIntLE(identity.getBytes(StandardCharsets.UTF_8).length);
        tokens.writeCharSequence(identity, StandardCharsets.UTF_8);
        tokens.writeIntLE(clientJwt.getBytes(StandardCharsets.UTF_8).length);
        tokens.writeCharSequence(clientJwt, StandardCharsets.UTF_8);

        BedrockRakNetCodec.writeUnsignedVarInt(login, tokens.readableBytes());
        login.writeBytes(tokens, tokens.readerIndex(), tokens.readableBytes());
        return batch(login, true);
    }
    static ByteBuf onlineLoginBatch(int protocolVersion, String identityJson, String clientJwt) {
        ByteBuf login = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(login, BedrockPacketIds.BEDROCK_LOGIN);
        login.writeInt(protocolVersion);

        ByteBuf tokens = Unpooled.buffer();
        tokens.writeIntLE(identityJson.getBytes(StandardCharsets.UTF_8).length);
        tokens.writeCharSequence(identityJson, StandardCharsets.UTF_8);
        tokens.writeIntLE(clientJwt.getBytes(StandardCharsets.UTF_8).length);
        tokens.writeCharSequence(clientJwt, StandardCharsets.UTF_8);

        BedrockRakNetCodec.writeUnsignedVarInt(login, tokens.readableBytes());
        login.writeBytes(tokens, tokens.readerIndex(), tokens.readableBytes());
        return batch(login, true);
    }
    static ByteBuf clientToServerHandshakePacket() {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_CLIENT_TO_SERVER_HANDSHAKE);
        return packet;
    }
    static ByteBuf resourcePackClientResponsePacket() {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_RESOURCE_PACK_CLIENT_RESPONSE);
        packet.writeByte(4);
        packet.writeShortLE(0);
        return packet;
    }
    static ByteBuf clientCacheStatusPacket(boolean enabled) {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_CLIENT_CACHE_STATUS);
        packet.writeBoolean(enabled);
        return packet;
    }

    static ByteBuf requestChunkRadiusPacket(int chunkRadius, int maxRadius) {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_REQUEST_CHUNK_RADIUS);
        BedrockRakNetCodec.writeZigZag32(packet, chunkRadius);
        packet.writeByte(maxRadius);
        return packet;
    }

    static ByteBuf setLocalPlayerAsInitializedPacket(long runtimeEntityId) {
        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_SET_LOCAL_PLAYER_AS_INITIALIZED);
        BedrockRakNetCodec.writeUnsignedVarLong(packet, runtimeEntityId);
        return packet;
    }

    static void assertConnectedPayloadId(DatagramPacket datagram, int expectedPayloadId) {
        ByteBuf payload = readConnectedPayload(datagram);
        assertEquals(expectedPayloadId, payload.getUnsignedByte(payload.readerIndex()));
    }

    static void assertPacketId(DatagramPacket datagram, int expectedPacketId) {
        assertEquals(expectedPacketId, datagram.content().getUnsignedByte(0));
    }

    static void assertAckPacket(DatagramPacket datagram) {
        assertPacketId(datagram, BedrockPacketIds.RAKNET_ACK);
    }

    static void assertBatchPacketId(DatagramPacket datagram, boolean compressionNegotiated, int expectedPacketId) {
        ByteBuf payload = readConnectedPayload(datagram);
        List<ByteBuf> packets = BedrockRakNetCodec.readBatch(payload.copy(), compressionNegotiated);
        ByteBuf firstPacket = packets.getFirst();
        assertEquals(expectedPacketId, BedrockRakNetCodec.readUnsignedVarInt(firstPacket));
        firstPacket.release();
    }

    static String readServerHandshakeToken(DatagramPacket datagram) {
        ByteBuf payload = readConnectedPayload(datagram);
        List<ByteBuf> packets = BedrockRakNetCodec.readBatch(payload.copy(), true);
        ByteBuf packet = packets.getFirst();
        assertEquals(BedrockPacketIds.BEDROCK_SERVER_TO_CLIENT_HANDSHAKE, BedrockRakNetCodec.readUnsignedVarInt(packet));
        int tokenLength = BedrockRakNetCodec.readUnsignedVarInt(packet);
        byte[] tokenBytes = new byte[tokenLength];
        packet.readBytes(tokenBytes);
        packet.release();
        return new String(tokenBytes, StandardCharsets.UTF_8);
    }

    static String readPlayStatus(DatagramPacket datagram, BedrockClientSecureSession secureSession) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_PLAY_STATUS, BedrockRakNetCodec.readUnsignedVarInt(packet));
        int status = packet.readIntLE();
        packet.release();
        return switch (status) {
            case 0 -> "login_success";
            case 1 -> "failed_client";
            case 2 -> "failed_spawn";
            case 3 -> "player_spawn";
            default -> "unknown";
        };
    }

    static String readResourcePacksInfoVersion(DatagramPacket datagram, BedrockClientSecureSession secureSession) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_RESOURCE_PACKS_INFO, BedrockRakNetCodec.readUnsignedVarInt(packet));
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(0, packet.readUnsignedByte());
        packet.skipBytes(16);
        String worldTemplateVersion = BedrockRakNetCodec.readString(packet);
        assertEquals(0, packet.readUnsignedShortLE());
        packet.release();
        return worldTemplateVersion;
    }

    static String readResourcePackStackVersion(
            DatagramPacket datagram,
            BedrockClientSecureSession secureSession,
            int protocolVersion
    ) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_RESOURCE_PACK_STACK, BedrockRakNetCodec.readUnsignedVarInt(packet));
        assertEquals(0, packet.readUnsignedByte());
        if (protocolVersion < 924) {
            assertEquals(0, BedrockRakNetCodec.readUnsignedVarInt(packet));
        }
        assertEquals(0, BedrockRakNetCodec.readUnsignedVarInt(packet));
        String gameVersion = BedrockRakNetCodec.readString(packet);
        assertEquals(0, packet.readIntLE());
        assertEquals(0, packet.readUnsignedByte());
        assertEquals(0, packet.readUnsignedByte());
        packet.release();
        return gameVersion;
    }

    static int readChunkRadiusUpdate(DatagramPacket datagram, BedrockClientSecureSession secureSession) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_CHUNK_RADIUS_UPDATE, BedrockRakNetCodec.readUnsignedVarInt(packet));
        int chunkRadius = BedrockRakNetCodec.decodeZigZag32(BedrockRakNetCodec.readUnsignedVarInt(packet));
        packet.release();
        return chunkRadius;
    }

    static long readStartGameRuntimeEntityId(DatagramPacket datagram, BedrockClientSecureSession secureSession) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_START_GAME, BedrockRakNetCodec.readUnsignedVarInt(packet));
        assertEquals(1L, BedrockRakNetCodec.decodeZigZag64(BedrockRakNetCodec.readUnsignedVarLong(packet)));
        long runtimeEntityId = BedrockRakNetCodec.readUnsignedVarLong(packet);
        packet.release();
        return runtimeEntityId;
    }

    static String readDisconnectMessage(DatagramPacket datagram, BedrockClientSecureSession secureSession) {
        ByteBuf packet = secureSession.decryptSinglePacket(readConnectedPayload(datagram));
        assertEquals(BedrockPacketIds.BEDROCK_DISCONNECT, BedrockRakNetCodec.readUnsignedVarInt(packet));
        BedrockRakNetCodec.readUnsignedVarInt(packet);
        packet.readBoolean();
        int messageLength = BedrockRakNetCodec.readUnsignedVarInt(packet);
        byte[] messageBytes = new byte[messageLength];
        packet.readBytes(messageBytes);
        packet.release();
        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    static ByteBuf readConnectedPayload(DatagramPacket datagram) {
        ByteBuf in = datagram.content().copy();
        in.readByte();
        BedrockRakNetCodec.readLittleTriad(in);
        int header = in.readUnsignedByte();
        int reliability = (header & 0xe0) >> 5;
        int length = in.readUnsignedShort() >> 3;
        if (reliability == 3) {
            BedrockRakNetCodec.readLittleTriad(in);
            BedrockRakNetCodec.readLittleTriad(in);
            in.readUnsignedByte();
        }
        return in.readSlice(length);
    }

    private static ByteBuf batch(ByteBuf packet, boolean includeCompressionHeader) {
        ByteBuf batch = Unpooled.buffer();
        batch.writeByte(BedrockPacketIds.BEDROCK_BATCH);
        if (includeCompressionHeader) {
            batch.writeByte(0xff);
        }
        BedrockRakNetCodec.writeUnsignedVarInt(batch, packet.readableBytes());
        batch.writeBytes(packet, packet.readerIndex(), packet.readableBytes());
        return batch;
    }
}

