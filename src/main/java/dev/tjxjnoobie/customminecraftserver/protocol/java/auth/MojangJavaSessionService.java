package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class MojangJavaSessionService implements JavaSessionService {
    private final JavaAuthenticationSettings settings;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MojangJavaSessionService(JavaAuthenticationSettings settings) {
        this(settings, HttpClient.newHttpClient(), new ObjectMapper());
    }

    public MojangJavaSessionService(JavaAuthenticationSettings settings, HttpClient httpClient, ObjectMapper objectMapper) {
        this.settings = settings;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<JavaSessionVerificationResult> verifyJoin(JavaSessionVerificationRequest request) {
        String sessionHash = JavaSessionDigest.serverHash(request.serverId(), request.sharedSecret(), request.publicKeyBytes());
        StringBuilder query = new StringBuilder(settings.sessionServerUrl())
                .append("/session/minecraft/hasJoined?username=")
                .append(URLEncoder.encode(request.username(), StandardCharsets.UTF_8))
                .append("&serverId=")
                .append(URLEncoder.encode(sessionHash, StandardCharsets.UTF_8));

        String clientIp = extractClientIp(request.remoteAddress());
        if (settings.includeClientIpInSessionVerification() && clientIp != null) {
            query.append("&ip=").append(URLEncoder.encode(clientIp, StandardCharsets.UTF_8));
        }

        return httpClient.sendAsync(
                        java.net.http.HttpRequest.newBuilder(URI.create(query.toString())).GET().build(),
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Mojang session verification returned HTTP " + response.statusCode());
                    }
                    try {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode id = root.path("id");
                        JsonNode name = root.path("name");
                        if (id.isMissingNode() || id.isNull() || name.isMissingNode() || name.isNull()) {
                            throw new IllegalStateException("Mojang session verification response did not include id/name");
                        }
                        return new JavaSessionVerificationResult(id.asText(), name.asText());
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to parse Mojang session verification response", exception);
                    }
                });
    }

    private String extractClientIp(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return null;
        }
        String normalized = remoteAddress.startsWith("/") ? remoteAddress.substring(1) : remoteAddress;
        int lastColon = normalized.lastIndexOf(':');
        if (lastColon <= 0) {
            return normalized;
        }
        return normalized.substring(0, lastColon);
    }
}

