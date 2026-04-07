package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;

import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallenge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class JavaPacketWriter {
    private JavaPacketWriter() {
    }

    public static ByteBuf statusResponse(ByteBufAllocator allocator, String payload) {
        return framedPacket(allocator, JavaPacketIds.STATUS_RESPONSE, body -> MinecraftStringCodec.write(body, payload));
    }

    public static ByteBuf pong(ByteBufAllocator allocator, long payload) {
        return framedPacket(allocator, JavaPacketIds.STATUS_PONG, body -> body.writeLong(payload));
    }

    public static ByteBuf disconnect(ByteBufAllocator allocator, String message) {
        return framedPacket(
                allocator,
                JavaPacketIds.LOGIN_DISCONNECT,
                body -> MinecraftStringCodec.write(body, textMessageJson(message))
        );
    }

    public static ByteBuf loginSuccess(ByteBufAllocator allocator, int protocolVersion, UUID playerUuid, String username) {
        return framedPacket(allocator, JavaPacketIds.LOGIN_SUCCESS, body -> {
            if (protocolVersion >= 767) {
                body.writeLong(playerUuid.getMostSignificantBits());
                body.writeLong(playerUuid.getLeastSignificantBits());
                MinecraftStringCodec.write(body, username);
                MinecraftVarInt.write(body, 0);
                return;
            }

            MinecraftStringCodec.write(body, playerUuid.toString());
            MinecraftStringCodec.write(body, username);
        });
    }

    public static ByteBuf legacyPlayDisconnect(ByteBufAllocator allocator, String message) {
        return framedPacket(
                allocator,
                JavaPacketIds.PLAY_LEGACY_DISCONNECT,
                body -> MinecraftStringCodec.write(body, textMessageJson(message))
        );
    }

    public static ByteBuf configurationDisconnect(ByteBufAllocator allocator, String message) {
        return anonymousTextPacket(allocator, JavaPacketIds.CONFIGURATION_DISCONNECT, message);
    }

    public static ByteBuf finishConfiguration(ByteBufAllocator allocator) {
        return framedPacket(allocator, JavaPacketIds.FINISH_CONFIGURATION, body -> {
        });
    }

    public static ByteBuf playKeepAlive(ByteBufAllocator allocator, long keepAliveId) {
        return framedPacket(allocator, JavaPacketIds.PLAY_CLIENTBOUND_KEEP_ALIVE, body -> body.writeLong(keepAliveId));
    }

    public static ByteBuf playPing(ByteBufAllocator allocator, int pingId) {
        return framedPacket(allocator, JavaPacketIds.PLAY_CLIENTBOUND_PING, body -> body.writeInt(pingId));
    }

    public static ByteBuf modernPlayDisconnect(ByteBufAllocator allocator, String message) {
        return anonymousTextPacket(allocator, JavaPacketIds.PLAY_MODERN_DISCONNECT, message);
    }

    public static ByteBuf encryptionRequest(ByteBufAllocator allocator, int protocolVersion, JavaEncryptionChallenge challenge) {
        return framedPacket(allocator, JavaPacketIds.ENCRYPTION_REQUEST, body -> {
            MinecraftStringCodec.write(body, challenge.serverId());
            writeByteArray(body, challenge.publicKeyBytes());
            writeByteArray(body, challenge.verifyToken());
            if (protocolVersion >= 767) {
                body.writeBoolean(true);
            }
        });
    }

    public static ByteBuf framedPacket(ByteBufAllocator allocator, int packetId, PacketBodyWriter writer) {
        ByteBuf payload = allocator.buffer();
        ByteBuf framed = allocator.buffer();
        try {
            MinecraftVarInt.write(payload, packetId);
            writer.write(payload);
            MinecraftVarInt.write(framed, payload.readableBytes());
            framed.writeBytes(payload);
            return framed;
        } finally {
            payload.release();
        }
    }

    private static ByteBuf anonymousTextPacket(ByteBufAllocator allocator, int packetId, String message) {
        return framedPacket(allocator, packetId, body -> writeAnonymousTextComponent(body, message));
    }

    private static String textMessageJson(String message) {
        return "{\"text\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private static void writeAnonymousTextComponent(ByteBuf body, String message) {
        byte[] fieldName = "text".getBytes(StandardCharsets.UTF_8);
        byte[] text = message.getBytes(StandardCharsets.UTF_8);

        body.writeByte(0x0a);
        body.writeByte(0x08);
        body.writeShort(fieldName.length);
        body.writeBytes(fieldName);
        body.writeShort(text.length);
        body.writeBytes(text);
        body.writeByte(0x00);
    }

    private static void writeByteArray(ByteBuf body, byte[] data) {
        MinecraftVarInt.write(body, data.length);
        body.writeBytes(data);
    }

    @FunctionalInterface
    public interface PacketBodyWriter {
        void write(ByteBuf body);
    }
}



