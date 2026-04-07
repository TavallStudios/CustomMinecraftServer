package dev.tjxjnoobie.customminecraftserver.test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public final class BedrockJwtTestSupport {
    private BedrockJwtTestSupport() {
    }

    public static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        return generator.generateKeyPair();
    }

    public static String toBase64Der(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public static String signEs384Jwt(KeyPair keyPair, String x5u, String payloadJson) throws Exception {
        String headerJson = "{\"alg\":\"ES384\",\"x5u\":\"" + x5u + "\"}";
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        java.security.Signature signature = java.security.Signature.getInstance("SHA384withECDSA");
        signature.initSign(keyPair.getPrivate());
        signature.update((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
        byte[] derSignature = signature.sign();
        byte[] joseSignature = derToJose(derSignature, 48);

        return header + "." + payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(joseSignature);
    }

    public static String authenticatedIdentityJson(KeyPair clientKey, KeyPair trustedRootKey, KeyPair identityKey) throws Exception {
        String clientPublic = toBase64Der(clientKey);
        String trustedRootPublic = toBase64Der(trustedRootKey);
        String identityPublic = toBase64Der(identityKey);

        String clientToken = signEs384Jwt(clientKey, clientPublic, "{\"identityPublicKey\":\"" + trustedRootPublic + "\"}");
        String trustedToken = signEs384Jwt(trustedRootKey, trustedRootPublic, "{\"identityPublicKey\":\"" + identityPublic + "\"}");
        String identityToken = signEs384Jwt(
                identityKey,
                identityPublic,
                "{\"extraData\":{\"displayName\":\"VerifiedBedrock\",\"identity\":\"bedrock-player-identity\",\"XUID\":\"2535400000000001\"}}"
        );

        return "{\"Certificate\":\"{\\\"chain\\\":[\\\"" + clientToken + "\\\",\\\"" + trustedToken + "\\\",\\\"" + identityToken + "\\\"]}\"}";
    }

    private static byte[] derToJose(byte[] derSignature, int componentLength) {
        int rLength = derSignature[3] & 0xff;
        byte[] r = new byte[rLength];
        System.arraycopy(derSignature, 4, r, 0, rLength);
        int sOffset = 4 + rLength + 2;
        int sLength = derSignature[4 + rLength + 1] & 0xff;
        byte[] s = new byte[sLength];
        System.arraycopy(derSignature, sOffset, s, 0, sLength);

        byte[] jose = new byte[componentLength * 2];
        copyUnsigned(r, jose, 0, componentLength);
        copyUnsigned(s, jose, componentLength, componentLength);
        return jose;
    }

    private static void copyUnsigned(byte[] source, byte[] target, int targetOffset, int componentLength) {
        int srcOffset = Math.max(0, source.length - componentLength);
        int copyLength = Math.min(source.length, componentLength);
        System.arraycopy(source, srcOffset, target, targetOffset + componentLength - copyLength, copyLength);
    }
}
