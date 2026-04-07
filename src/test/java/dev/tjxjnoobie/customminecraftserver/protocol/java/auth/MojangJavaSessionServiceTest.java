package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MojangJavaSessionServiceTest {
    @Test
    void verifiesSessionJoinUsingHttpClient() throws Exception {
        TestLogSupport.logTestStart("MojangJavaSessionServiceTest.verifiesSessionJoinUsingHttpClient");
        JavaAuthenticationSettings settings = new JavaAuthenticationSettings("http://example", false, 1024);
        StubHttpClient httpClient = new StubHttpClient(new StubHttpResponse(200, "{\"id\":\"abc\",\"name\":\"Verified\"}"));
        MojangJavaSessionService service = new MojangJavaSessionService(settings, httpClient, new ObjectMapper());

        JavaSessionVerificationRequest request = new JavaSessionVerificationRequest(
                "Player",
                "server",
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6},
                "127.0.0.1:25565"
        );

        JavaSessionVerificationResult result = service.verifyJoin(request).get(2, TimeUnit.SECONDS);
        assertEquals("abc", result.profileId());
        assertEquals("Verified", result.profileName());
        assertNotNull(httpClient.lastRequest);
    }

    private static final class StubHttpClient extends HttpClient {
        private final StubHttpResponse response;
        private HttpRequest lastRequest;

        private StubHttpClient(StubHttpResponse response) {
            this.response = response;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("send not supported in stub");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            this.lastRequest = request;
            @SuppressWarnings("unchecked")
            HttpResponse<T> casted = (HttpResponse<T>) response;
            return CompletableFuture.completedFuture(casted);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private static final class StubHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;

        private StubHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("http://example")).build();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://example");
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }
    }
}
