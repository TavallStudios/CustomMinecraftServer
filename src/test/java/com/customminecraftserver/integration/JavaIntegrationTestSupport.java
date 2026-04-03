package com.customminecraftserver.integration;

import com.customminecraftserver.javaedition.JavaPacketIds;
import com.customminecraftserver.javaedition.MinecraftStringCodec;
import com.customminecraftserver.javaedition.MinecraftVarInt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class JavaIntegrationTestSupport {
    private JavaIntegrationTestSupport() {
    }

    static ByteBuf handshakePacket(int protocolVersion, String host, int port, int nextState) {
        return framedPacket(JavaPacketIds.HANDSHAKE, payload -> {
            MinecraftVarInt.write(payload, protocolVersion);
            MinecraftStringCodec.write(payload, host);
            payload.writeShort(port);
            MinecraftVarInt.write(payload, nextState);
        });
    }

    static ByteBuf loginStartPacket(String username, UUID uuid) {
        return framedPacket(JavaPacketIds.LOGIN_START, payload -> {
            MinecraftStringCodec.write(payload, username);
            if (uuid != null) {
                payload.writeLong(uuid.getMostSignificantBits());
                payload.writeLong(uuid.getLeastSignificantBits());
            }
        });
    }

    static ByteBuf loginAcknowledgedPacket() {
        return framedPacket(JavaPacketIds.LOGIN_ACKNOWLEDGED, payload -> {
        });
    }

    static ByteBuf configurationFinishedPacket() {
        return framedPacket(JavaPacketIds.FINISH_CONFIGURATION, payload -> {
        });
    }

    static ByteBuf playKeepAliveResponsePacket(long keepAliveId) {
        return framedPacket(JavaPacketIds.PLAY_SERVERBOUND_KEEP_ALIVE, payload -> payload.writeLong(keepAliveId));
    }

    static ByteBuf settingsPacket() {
        return framedPacket(JavaPacketIds.PLAY_SERVERBOUND_SETTINGS, payload -> {
            MinecraftStringCodec.write(payload, "en_US");
            payload.writeByte(12);
            MinecraftVarInt.write(payload, 0);
            payload.writeBoolean(true);
            payload.writeByte(0x7f);
            MinecraftVarInt.write(payload, 1);
            payload.writeBoolean(false);
            payload.writeBoolean(true);
            MinecraftVarInt.write(payload, 0);
        });
    }

    static ByteBuf playPongPacket(int pingId) {
        return framedPacket(JavaPacketIds.PLAY_SERVERBOUND_PONG, payload -> payload.writeInt(pingId));
    }

    static ByteBuf encryptionResponsePacket(byte[] publicKeyBytes, byte[] verifyToken, byte[] sharedSecret) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedSecret = rsa.doFinal(sharedSecret);
        byte[] encryptedVerifyToken = rsa.doFinal(verifyToken);

        return framedPacket(JavaPacketIds.ENCRYPTION_RESPONSE, payload -> {
            writeByteArray(payload, encryptedSecret);
            writeByteArray(payload, encryptedVerifyToken);
        });
    }

    static EncryptionRequest readEncryptionRequest(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.ENCRYPTION_REQUEST, MinecraftVarInt.read(payload));
        String serverId = MinecraftStringCodec.read(payload, 32);
        byte[] publicKeyBytes = readByteArray(payload);
        byte[] verifyToken = readByteArray(payload);
        boolean shouldAuthenticate = payload.isReadable() && payload.readBoolean();
        return new EncryptionRequest(serverId, publicKeyBytes, verifyToken, shouldAuthenticate);
    }

    static LoginSuccess readLegacyLoginSuccess(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.LOGIN_SUCCESS, MinecraftVarInt.read(payload));
        String uuid = MinecraftStringCodec.read(payload, 64);
        String username = MinecraftStringCodec.read(payload, 16);
        return new LoginSuccess(uuid, username);
    }

    static LoginSuccess readModernLoginSuccess(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.LOGIN_SUCCESS, MinecraftVarInt.read(payload));
        UUID uuid = new UUID(payload.readLong(), payload.readLong());
        String username = MinecraftStringCodec.read(payload, 16);
        assertEquals(0, MinecraftVarInt.read(payload));
        return new LoginSuccess(uuid.toString(), username);
    }

    static String readLegacyPlayDisconnect(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.PLAY_LEGACY_DISCONNECT, MinecraftVarInt.read(payload));
        return MinecraftStringCodec.read(payload, 32767);
    }

    static void readFinishConfiguration(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.FINISH_CONFIGURATION, MinecraftVarInt.read(payload));
        assertEquals(0, payload.readableBytes());
    }

    static long readPlayKeepAlive(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.PLAY_CLIENTBOUND_KEEP_ALIVE, MinecraftVarInt.read(payload));
        return payload.readLong();
    }

    static int readPlayPing(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.PLAY_CLIENTBOUND_PING, MinecraftVarInt.read(payload));
        return payload.readInt();
    }

    static String readModernPlayDisconnect(ByteBuf frame) {
        ByteBuf payload = readFramePayload(frame);
        assertEquals(JavaPacketIds.PLAY_MODERN_DISCONNECT, MinecraftVarInt.read(payload));
        assertEquals(0x0a, payload.readUnsignedByte());
        assertEquals(0x08, payload.readUnsignedByte());
        int fieldLength = payload.readUnsignedShort();
        byte[] fieldName = new byte[fieldLength];
        payload.readBytes(fieldName);
        assertEquals("text", new String(fieldName, StandardCharsets.UTF_8));
        int textLength = payload.readUnsignedShort();
        byte[] textBytes = new byte[textLength];
        payload.readBytes(textBytes);
        assertEquals(0x00, payload.readUnsignedByte());
        return new String(textBytes, StandardCharsets.UTF_8);
    }

    static ByteBuf awaitOutboundFrame(EmbeddedChannel channel) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        ByteBuf outbound;
        while ((outbound = channel.readOutbound()) == null && System.currentTimeMillis() < deadline) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Thread.sleep(25);
        }
        assertNotNull(outbound, "Timed out waiting for outbound frame");
        return outbound;
    }

    private static ByteBuf framedPacket(int packetId, PacketWriter writer) {
        ByteBuf payload = Unpooled.buffer();
        try {
            MinecraftVarInt.write(payload, packetId);
            writer.write(payload);
            ByteBuf frame = Unpooled.buffer();
            MinecraftVarInt.write(frame, payload.readableBytes());
            frame.writeBytes(payload);
            return frame;
        } finally {
            payload.release();
        }
    }

    private static ByteBuf readFramePayload(ByteBuf frame) {
        int frameLength = MinecraftVarInt.read(frame);
        return frame.readSlice(frameLength);
    }

    private static byte[] readByteArray(ByteBuf payload) {
        int length = MinecraftVarInt.read(payload);
        byte[] bytes = new byte[length];
        payload.readBytes(bytes);
        return bytes;
    }

    private static void writeByteArray(ByteBuf payload, byte[] bytes) {
        MinecraftVarInt.write(payload, bytes.length);
        payload.writeBytes(bytes);
    }

    record EncryptionRequest(String serverId, byte[] publicKeyBytes, byte[] verifyToken, boolean shouldAuthenticate) {
    }

    record LoginSuccess(String uuid, String username) {
    }

    @FunctionalInterface
    private interface PacketWriter {
        void write(ByteBuf payload);
    }

    static final class JavaCipherStream {
        private final Cipher encryptCipher;
        private final Cipher decryptCipher;

        JavaCipherStream(byte[] sharedSecret) throws Exception {
            encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), new IvParameterSpec(sharedSecret));
            decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), new IvParameterSpec(sharedSecret));
        }

        ByteBuf encrypt(ByteBuf plainFrame) {
            byte[] plainBytes = new byte[plainFrame.readableBytes()];
            plainFrame.readBytes(plainBytes);
            return Unpooled.wrappedBuffer(encryptCipher.update(plainBytes));
        }

        ByteBuf decrypt(ByteBuf encryptedFrame) {
            byte[] encryptedBytes = new byte[encryptedFrame.readableBytes()];
            encryptedFrame.readBytes(encryptedBytes);
            return Unpooled.wrappedBuffer(decryptCipher.update(encryptedBytes));
        }
    }
}
