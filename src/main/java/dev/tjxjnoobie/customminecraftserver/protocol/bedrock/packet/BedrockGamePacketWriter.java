package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public final class BedrockGamePacketWriter {
    private static final String EMPTY_WORLD_TEMPLATE_VERSION = "0.0.0";
    private static final int PLAY_STATUS_LOGIN_SUCCESS = 0;
    private static final int PLAY_STATUS_PLAYER_SPAWN = 3;

    public static final long START_GAME_RUNTIME_ENTITY_ID = 1L;

    private BedrockGamePacketWriter() {
    }

    public static ByteBuf networkSettings(ByteBufAllocator allocator) {
        return packet(allocator, BedrockPacketIds.BEDROCK_NETWORK_SETTINGS, body -> {
            body.writeShortLE(65535);
            body.writeShortLE(0);
            body.writeBoolean(false);
            body.writeByte(0);
            body.writeFloatLE(0.0f);
        });
    }

    public static ByteBuf serverToClientHandshake(ByteBufAllocator allocator, String token) {
        return packet(allocator, BedrockPacketIds.BEDROCK_SERVER_TO_CLIENT_HANDSHAKE, body -> BedrockRakNetCodec.writeString(body, token));
    }

    public static ByteBuf playStatusLoginSuccess(ByteBufAllocator allocator) {
        return playStatus(allocator, PLAY_STATUS_LOGIN_SUCCESS);
    }

    public static ByteBuf playStatusPlayerSpawn(ByteBufAllocator allocator) {
        return playStatus(allocator, PLAY_STATUS_PLAYER_SPAWN);
    }

    public static ByteBuf resourcePacksInfo(ByteBufAllocator allocator) {
        return packet(allocator, BedrockPacketIds.BEDROCK_RESOURCE_PACKS_INFO, body -> {
            body.writeBoolean(false);
            body.writeBoolean(false);
            body.writeBoolean(false);
            body.writeBoolean(false);
            body.writeZero(16);
            BedrockRakNetCodec.writeString(body, EMPTY_WORLD_TEMPLATE_VERSION);
            body.writeShortLE(0);
        });
    }

    public static ByteBuf resourcePackStack(ByteBufAllocator allocator, int protocolVersion, String gameVersion) {
        return packet(
                allocator,
                BedrockPacketIds.BEDROCK_RESOURCE_PACK_STACK,
                body -> writeResourcePackStackBody(body, protocolVersion, gameVersion)
        );
    }

    public static ByteBuf chunkRadiusUpdate(ByteBufAllocator allocator, int chunkRadius) {
        return packet(allocator, BedrockPacketIds.BEDROCK_CHUNK_RADIUS_UPDATE, body -> BedrockRakNetCodec.writeZigZag32(body, chunkRadius));
    }

    public static ByteBuf startGame(ByteBufAllocator allocator, int protocolVersion) {
        ByteBuf packet = allocator.buffer();
        packet.writeBytes(BedrockStartGameTemplates.startGameTemplate(protocolVersion));
        return packet;
    }

    public static ByteBuf disconnect(ByteBufAllocator allocator, String message) {
        return packet(allocator, BedrockPacketIds.BEDROCK_DISCONNECT, body -> {
            BedrockRakNetCodec.writeUnsignedVarInt(body, 0);
            body.writeBoolean(false);
            BedrockRakNetCodec.writeString(body, message);
            BedrockRakNetCodec.writeString(body, "");
        });
    }

    private static ByteBuf packet(ByteBufAllocator allocator, int packetId, BodyWriter writer) {
        ByteBuf packet = allocator.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, packetId);
        writer.write(packet);
        return packet;
    }

    private static ByteBuf playStatus(ByteBufAllocator allocator, int status) {
        return packet(allocator, BedrockPacketIds.BEDROCK_PLAY_STATUS, body -> body.writeIntLE(status));
    }

    private static void writeResourcePackStackBody(ByteBuf body, int protocolVersion, String gameVersion) {
        body.writeBoolean(false);
        if (protocolVersion < 924) {
            BedrockRakNetCodec.writeUnsignedVarInt(body, 0);
        }
        BedrockRakNetCodec.writeUnsignedVarInt(body, 0);
        BedrockRakNetCodec.writeString(body, gameVersion);
        body.writeIntLE(0);
        body.writeBoolean(false);
        body.writeBoolean(false);
    }

    @FunctionalInterface
    private interface BodyWriter {
        void write(ByteBuf body);
    }
}


