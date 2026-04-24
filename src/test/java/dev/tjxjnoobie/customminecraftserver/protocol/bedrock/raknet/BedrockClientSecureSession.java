package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BedrockClientSecureSession {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secretKeyBytes;
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    private long sendCounter;
    private long receiveCounter;

    BedrockClientSecureSession(PrivateKey privateKey, String serverHandshakeToken) throws Exception {
        String[] segments = serverHandshakeToken.split("\\.");
        JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[0]));
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));

        PublicKey serverPublicKey = KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(header.path("x5u").asText()))
        );
        byte[] salt = Base64.getDecoder().decode(payload.path("salt").asText());

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(privateKey);
        agreement.doPhase(serverPublicKey, true);
        byte[] sharedSecret = agreement.generateSecret();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        digest.update(sharedSecret);
        secretKeyBytes = digest.digest();

        byte[] iv = initialCounter(Arrays.copyOf(secretKeyBytes, 12));
        encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKeyBytes, "AES"), new IvParameterSpec(iv));
        decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKeyBytes, "AES"), new IvParameterSpec(iv));
    }

    ByteBuf encrypt(ByteBuf packet) {
        ByteBuf frame = Unpooled.buffer();
        try {
            BedrockRakNetCodec.writeUnsignedVarInt(frame, packet.readableBytes());
            frame.writeBytes(packet, packet.readerIndex(), packet.readableBytes());
            byte[] compressed = deflate(frame);
            byte[] plaintext = new byte[1 + compressed.length];
            plaintext[0] = 0x00;
            System.arraycopy(compressed, 0, plaintext, 1, compressed.length);
            byte[] checksum = checksum(plaintext, sendCounter++);
            byte[] input = new byte[plaintext.length + checksum.length];
            System.arraycopy(plaintext, 0, input, 0, plaintext.length);
            System.arraycopy(checksum, 0, input, plaintext.length, checksum.length);
            byte[] encrypted = encryptCipher.update(input);
            ByteBuf payload = Unpooled.buffer();
            payload.writeByte(0xfe);
            payload.writeBytes(encrypted);
            return payload;
        } finally {
            frame.release();
        }
    }

    ByteBuf decryptSinglePacket(ByteBuf payload) {
        byte[] encryptedBytes = new byte[payload.readableBytes() - 1];
        payload.skipBytes(1);
        payload.readBytes(encryptedBytes);

        byte[] decrypted = decryptCipher.update(encryptedBytes);
        int plaintextLength = decrypted.length - 8;
        byte[] plaintext = Arrays.copyOf(decrypted, plaintextLength);
        byte[] expectedChecksum = Arrays.copyOfRange(decrypted, plaintextLength, decrypted.length);
        assertTrue(Arrays.equals(expectedChecksum, checksum(plaintext, receiveCounter++)));

        byte[] framed = inflate(Arrays.copyOfRange(plaintext, 1, plaintext.length));
        ByteBuf framedBuffer = Unpooled.wrappedBuffer(framed);
        int packetLength = BedrockRakNetCodec.readUnsignedVarInt(framedBuffer);
        return framedBuffer.readRetainedSlice(packetLength);
    }

    private byte[] checksum(byte[] plaintext, long counter) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(counter);
            digest.update(buffer.array());
            digest.update(plaintext);
            digest.update(secretKeyBytes);
            return Arrays.copyOf(digest.digest(), 8);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] deflate(ByteBuf frame) {
        byte[] input = new byte[frame.readableBytes()];
        frame.getBytes(frame.readerIndex(), input);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            output.write(buffer, 0, count);
        }
        deflater.end();
        return output.toByteArray();
    }

    private byte[] inflate(byte[] input) {
        Inflater inflater = new Inflater(true);
        inflater.setInput(input);
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while (!inflater.finished() && !inflater.needsInput()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    break;
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        } finally {
            inflater.end();
        }
    }

    private byte[] initialCounter(byte[] iv12) {
        byte[] counter = new byte[16];
        System.arraycopy(iv12, 0, counter, 0, iv12.length);
        counter[15] = 0x02;
        return counter;
    }
}

