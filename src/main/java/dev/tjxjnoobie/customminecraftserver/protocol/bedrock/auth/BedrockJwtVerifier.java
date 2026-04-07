package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class BedrockJwtVerifier {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BedrockAuthenticatedIdentity verify(String identityJson, String clientJwt, BedrockAuthenticationSettings settings) {
        List<String> chain = extractChain(identityJson);
        if (chain.isEmpty()) {
            throw new IllegalStateException("Missing Bedrock certificate chain for ONLINE auth");
        }

        ChainVerificationResult chainResult = verifyChain(chain, settings);
        JsonNode clientPayload = verifyEs384Jwt(clientJwt, publicKeyFromBase64(chainResult.identityPublicKey()));

        String displayName = firstNonBlank(
                text(chainResult.extraData(), "displayName"),
                text(chainResult.extraData(), "xname"),
                text(clientPayload, "ThirdPartyName"),
                text(clientPayload, "displayName"),
                "unknown-bedrock-player"
        );
        String identity = firstNonBlank(
                text(chainResult.extraData(), "identity"),
                text(clientPayload, "identity"),
                "unknown-identity"
        );
        String xuid = firstNonBlank(
                text(chainResult.extraData(), "XUID"),
                text(chainResult.extraData(), "xuid"),
                text(clientPayload, "XUID"),
                text(clientPayload, "xuid"),
                "0"
        );
        String handshakePublicKey = firstNonBlank(
                text(clientPayload, "cpk"),
                text(clientPayload, "clientPublicKey"),
                headerX5u(clientJwt),
                chainResult.identityPublicKey()
        );

        return new BedrockAuthenticatedIdentity(
                displayName,
                identity,
                xuid,
                chainResult.identityPublicKey(),
                handshakePublicKey
        );
    }

    public String extractDisplayName(String identityJson, String clientJwt) {
        try {
            for (String token : extractChain(identityJson)) {
                JsonNode payload = decodeUnsignedPayload(token);
                String candidate = text(payload.path("extraData"), "displayName");
                if (candidate != null) {
                    return candidate;
                }
                candidate = text(payload.path("extraData"), "xname");
                if (candidate != null) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            JsonNode payload = decodeUnsignedPayload(clientJwt);
            String candidate = text(payload, "ThirdPartyName");
            if (candidate != null) {
                return candidate;
            }
            candidate = text(payload.path("extraData"), "displayName");
            if (candidate != null) {
                return candidate;
            }
            candidate = text(payload, "xname");
            if (candidate != null) {
                return candidate;
            }
        } catch (Exception ignored) {
        }

        return "unknown-bedrock-player";
    }

    public String extractHandshakePublicKey(String identityJson, String clientJwt) {
        try {
            JsonNode payload = decodeUnsignedPayload(clientJwt);
            String handshakePublicKey = firstNonBlank(
                    text(payload, "cpk"),
                    text(payload, "clientPublicKey"),
                    headerX5u(clientJwt)
            );
            if (handshakePublicKey != null) {
                return handshakePublicKey;
            }
        } catch (RuntimeException ignored) {
        }

        try {
            List<String> chain = extractChain(identityJson);
            if (!chain.isEmpty()) {
                JsonNode payload = decodeUnsignedPayload(chain.getLast());
                return firstNonBlank(text(payload, "identityPublicKey"), headerX5u(chain.getFirst()));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private ChainVerificationResult verifyChain(List<String> chain, BedrockAuthenticationSettings settings) {
        PublicKey verificationKey = publicKeyFromBase64(headerX5u(chain.getFirst()));
        ObjectNode mergedExtraData = objectMapper.createObjectNode();
        String finalIdentityPublicKey = null;
        boolean trustedRootSeen = false;

        for (String token : chain) {
            JsonNode payload = verifyEs384Jwt(token, verificationKey);
            JsonNode extraData = payload.path("extraData");
            if (extraData.isObject()) {
                mergedExtraData.setAll((ObjectNode) extraData);
            }

            String headerKey = headerX5u(token);
            if (settings.trustedRootPublicKeys().contains(headerKey) && extraData.path("XUID").isMissingNode()) {
                trustedRootSeen = true;
            }

            finalIdentityPublicKey = firstNonBlank(text(payload, "identityPublicKey"), headerKey);
            verificationKey = publicKeyFromBase64(finalIdentityPublicKey);
        }

        if (settings.requireTrustedRootChain() && !trustedRootSeen) {
            throw new IllegalStateException("Bedrock identity chain was not signed by a trusted root");
        }

        return new ChainVerificationResult(mergedExtraData, finalIdentityPublicKey);
    }

    private List<String> extractChain(String identityJson) {
        try {
            JsonNode identity = objectMapper.readTree(identityJson);
            JsonNode chainNode = identity.path("chain");
            if (chainNode.isMissingNode() && identity.has("Certificate")) {
                chainNode = objectMapper.readTree(identity.path("Certificate").asText()).path("chain");
            }
            List<String> chain = new ArrayList<>();
            if (chainNode.isArray()) {
                for (JsonNode token : chainNode) {
                    if (!token.asText().isBlank()) {
                        chain.add(token.asText());
                    }
                }
            }
            return chain;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Bedrock identity chain", exception);
        }
    }

    private JsonNode verifyEs384Jwt(String jwt, PublicKey key) {
        String[] segments = segments(jwt);
        byte[] signatureBytes = Base64.getUrlDecoder().decode(segments[2]);
        byte[] derSignature = joseToDer(signatureBytes);

        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initVerify(key);
            signature.update((segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(derSignature)) {
                throw new IllegalStateException("Invalid Bedrock JWT signature");
            }
            return objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
        } catch (GeneralSecurityException | java.io.IOException exception) {
            throw new IllegalStateException("Unable to verify Bedrock JWT", exception);
        }
    }

    private JsonNode decodeUnsignedPayload(String jwt) {
        try {
            return objectMapper.readTree(Base64.getUrlDecoder().decode(segments(jwt)[1]));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decode Bedrock JWT payload", exception);
        }
    }

    private String headerX5u(String jwt) {
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(segments(jwt)[0]));
            JsonNode x5u = header.path("x5u");
            if (x5u.isMissingNode() || x5u.isNull() || x5u.asText().isBlank()) {
                throw new IllegalStateException("Bedrock JWT header did not include x5u");
            }
            return x5u.asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decode Bedrock JWT header", exception);
        }
    }

    private PublicKey publicKeyFromBase64(String derBase64) {
        try {
            byte[] der = Base64.getDecoder().decode(derBase64);
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to decode Bedrock public key", exception);
        }
    }

    private byte[] joseToDer(byte[] joseSignature) {
        int componentLength = joseSignature.length / 2;
        byte[] r = stripLeadingZeros(joseSignature, 0, componentLength);
        byte[] s = stripLeadingZeros(joseSignature, componentLength, componentLength);

        int totalLength = 2 + r.length + 2 + s.length;
        byte[] der = new byte[2 + totalLength];
        der[0] = 0x30;
        der[1] = (byte) totalLength;
        der[2] = 0x02;
        der[3] = (byte) r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        int offset = 4 + r.length;
        der[offset] = 0x02;
        der[offset + 1] = (byte) s.length;
        System.arraycopy(s, 0, der, offset + 2, s.length);
        return der;
    }

    private byte[] stripLeadingZeros(byte[] source, int offset, int length) {
        byte[] slice = new byte[length];
        System.arraycopy(source, offset, slice, 0, length);
        BigInteger integer = new BigInteger(1, slice);
        byte[] encoded = integer.toByteArray();
        if ((encoded[0] & 0x80) != 0) {
            return encoded;
        }
        return encoded;
    }

    private String[] segments(String jwt) {
        String[] segments = jwt.split("\\.");
        if (segments.length != 3) {
            throw new IllegalStateException("Invalid Bedrock JWT format");
        }
        return segments;
    }

    private String text(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (child.isMissingNode() || child.isNull() || child.asText().isBlank()) {
            return null;
        }
        return child.asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ChainVerificationResult(JsonNode extraData, String identityPublicKey) {
    }
}

