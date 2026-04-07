package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class BedrockSecureSession {
    private static final byte[] DEFAULT_SALT = new byte[]{(byte) 0xf0, (byte) 0x9f, (byte) 0xa7, (byte) 0x82};

    private final String clientPublicKeyBase64;
    private final String serverPublicKeyBase64;
    private final PrivateKey serverPrivateKey;
    private final byte[] secretKeyBytes;
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    private long sendCounter;
    private long receiveCounter;

    private BedrockSecureSession(
            String clientPublicKeyBase64,
            String serverPublicKeyBase64,
            PrivateKey serverPrivateKey,
            byte[] secretKeyBytes,
            Cipher encryptCipher,
            Cipher decryptCipher
    ) {
        this.clientPublicKeyBase64 = clientPublicKeyBase64;
        this.serverPublicKeyBase64 = serverPublicKeyBase64;
        this.serverPrivateKey = serverPrivateKey;
        this.secretKeyBytes = secretKeyBytes;
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
    }

    public static BedrockSecureSession create(String clientPublicKeyBase64) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp384r1"));
            KeyPair keyPair = generator.generateKeyPair();

            PublicKey clientPublicKey = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(clientPublicKeyBase64))
            );
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(keyPair.getPrivate());
            agreement.doPhase(clientPublicKey, true);
            byte[] sharedSecret = agreement.generateSecret();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(DEFAULT_SALT);
            digest.update(sharedSecret);
            byte[] secretKeyBytes = digest.digest();

            byte[] iv = initialCounter(Arrays.copyOf(secretKeyBytes, 12));
            Cipher encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKeyBytes, "AES"), new IvParameterSpec(iv));

            Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKeyBytes, "AES"), new IvParameterSpec(iv));

            String serverPublicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            return new BedrockSecureSession(
                    clientPublicKeyBase64,
                    serverPublicKeyBase64,
                    keyPair.getPrivate(),
                    secretKeyBytes,
                    encryptCipher,
                    decryptCipher
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize Bedrock secure session", exception);
        }
    }

    public String serverHandshakeToken() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("salt", Base64.getEncoder().encodeToString(DEFAULT_SALT));
        payload.put("signedToken", clientPublicKeyBase64);
        return signJwt(payload);
    }

    public ByteBuf encryptPacket(ByteBufAllocator allocator, ByteBuf packet) {
        ByteBuf framed = allocator.buffer();
        ByteBuf encrypted = allocator.buffer();
        try {
            BedrockRakNetCodec.writeUnsignedVarInt(framed, packet.readableBytes());
            framed.writeBytes(packet, packet.readerIndex(), packet.readableBytes());

            byte[] compressed = deflate(ByteBufUtil.getBytes(framed));
            byte[] plaintext = new byte[1 + compressed.length];
            plaintext[0] = 0x00;
            System.arraycopy(compressed, 0, plaintext, 1, compressed.length);

            byte[] checksum = checksum(plaintext, sendCounter++);
            byte[] input = new byte[plaintext.length + checksum.length];
            System.arraycopy(plaintext, 0, input, 0, plaintext.length);
            System.arraycopy(checksum, 0, input, plaintext.length, checksum.length);

            byte[] encryptedBytes = encryptCipher.update(input);
            encrypted.writeByte(0xfe);
            encrypted.writeBytes(encryptedBytes);
            return encrypted.retain();
        } finally {
            framed.release();
            encrypted.release();
        }
    }

    public List<ByteBuf> decryptPackets(ByteBuf payload) {
        if (!payload.isReadable() || payload.getUnsignedByte(payload.readerIndex()) != 0xfe) {
            throw new IllegalStateException("Encrypted Bedrock payload did not include batch header");
        }

        byte[] encryptedBytes = ByteBufUtil.getBytes(payload, payload.readerIndex() + 1, payload.readableBytes() - 1, false);
        byte[] decrypted = decryptCipher.update(encryptedBytes);
        if (decrypted == null || decrypted.length < 9) {
            throw new IllegalStateException("Bedrock encrypted payload was too short");
        }

        int payloadLength = decrypted.length - 8;
        byte[] plaintext = Arrays.copyOf(decrypted, payloadLength);
        byte[] expectedChecksum = Arrays.copyOfRange(decrypted, payloadLength, decrypted.length);
        byte[] actualChecksum = checksum(plaintext, receiveCounter++);
        if (!Arrays.equals(expectedChecksum, actualChecksum)) {
            throw new IllegalStateException("Bedrock secure-session checksum mismatch");
        }

        if (plaintext[0] != 0x00 && plaintext[0] != (byte) 0xff) {
            throw new IllegalStateException("Unsupported Bedrock secure-session compressor header " + plaintext[0]);
        }

        byte[] framedBytes = plaintext[0] == (byte) 0xff
                ? Arrays.copyOfRange(plaintext, 1, plaintext.length)
                : inflate(Arrays.copyOfRange(plaintext, 1, plaintext.length));
        ByteBuf framed = Unpooled.wrappedBuffer(framedBytes);
        List<ByteBuf> packets = new ArrayList<>();
        while (framed.isReadable()) {
            int packetLength = BedrockRakNetCodec.readUnsignedVarInt(framed);
            packets.add(framed.readRetainedSlice(packetLength));
        }
        framed.release();
        return packets;
    }

    public String serverPublicKeyBase64() {
        return serverPublicKeyBase64;
    }

    private String signJwt(Map<String, Object> payload) {
        String headerJson = "{\"alg\":\"ES384\",\"x5u\":\"" + serverPublicKeyBase64 + "\"}";
        String payloadJson = toJson(payload);
        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;

        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initSign(serverPrivateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] derSignature = signature.sign();
            byte[] joseSignature = derToJose(derSignature, 48);
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(joseSignature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign Bedrock handshake JWT", exception);
        }
    }

    private String toJson(Map<String, Object> payload) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(entry.getKey()).append('"').append(':')
                    .append('"').append(entry.getValue()).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    private byte[] checksum(byte[] plaintext, long counter) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(counter).array());
            digest.update(plaintext);
            digest.update(secretKeyBytes);
            return Arrays.copyOf(digest.digest(), 8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to compute Bedrock secure-session checksum", exception);
        }
    }

    private byte[] deflate(byte[] input) {
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
            throw new IllegalStateException("Unable to inflate Bedrock secure-session payload", exception);
        } finally {
            inflater.end();
        }
    }

    private static byte[] initialCounter(byte[] iv12) {
        byte[] counter = new byte[16];
        System.arraycopy(iv12, 0, counter, 0, iv12.length);
        counter[15] = 0x02;
        return counter;
    }

    private byte[] derToJose(byte[] derSignature, int componentLength) {
        int rLength = derSignature[3] & 0xff;
        byte[] r = Arrays.copyOfRange(derSignature, 4, 4 + rLength);
        int sOffset = 4 + rLength + 2;
        int sLength = derSignature[4 + rLength + 1] & 0xff;
        byte[] s = Arrays.copyOfRange(derSignature, sOffset, sOffset + sLength);

        byte[] jose = new byte[componentLength * 2];
        copyUnsigned(r, jose, 0, componentLength);
        copyUnsigned(s, jose, componentLength, componentLength);
        return jose;
    }

    private void copyUnsigned(byte[] source, byte[] target, int targetOffset, int componentLength) {
        int srcOffset = Math.max(0, source.length - componentLength);
        int copyLength = Math.min(source.length, componentLength);
        System.arraycopy(source, srcOffset, target, targetOffset + componentLength - copyLength, copyLength);
    }
}


