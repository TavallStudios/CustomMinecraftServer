package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

public final class JavaEncryptionChallengeFactory {
    private final KeyPair keyPair;
    private final SecureRandom secureRandom;

    public JavaEncryptionChallengeFactory(KeyPair keyPair) {
        this.keyPair = keyPair;
        this.secureRandom = new SecureRandom();
    }

    public JavaEncryptionChallenge create() {
        byte[] verifyToken = new byte[4];
        secureRandom.nextBytes(verifyToken);
        byte[] serverIdBytes = new byte[4];
        secureRandom.nextBytes(serverIdBytes);
        return new JavaEncryptionChallenge(
                toHex(serverIdBytes),
                keyPair.getPublic().getEncoded(),
                verifyToken,
                keyPair.getPrivate()
        );
    }

    public static JavaEncryptionChallengeFactory createGenerated(int keySizeBits) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySizeBits);
            return new JavaEncryptionChallengeFactory(generator.generateKeyPair());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate Java online-mode RSA key pair", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(String.format("%02x", value));
        }
        return out.toString();
    }
}

