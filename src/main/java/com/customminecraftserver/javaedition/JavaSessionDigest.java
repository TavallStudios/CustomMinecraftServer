package com.customminecraftserver.javaedition;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class JavaSessionDigest {
    private JavaSessionDigest() {
    }

    public static String serverHash(String serverId, byte[] sharedSecret, byte[] publicKeyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(sharedSecret);
            digest.update(publicKeyBytes);
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }
}
