package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSessionServiceTest {
    @Test
    void serviceContractReturnsVerificationResult() throws Exception {
        TestLogSupport.logTestStart("JavaSessionServiceTest.serviceContractReturnsVerificationResult");
        JavaSessionService service = request -> CompletableFuture.completedFuture(
                new JavaSessionVerificationResult("id", "name")
        );

        JavaSessionVerificationResult result = service.verifyJoin(
                new JavaSessionVerificationRequest("user", "server", new byte[]{1}, new byte[]{2}, "remote")
        ).get();

        assertEquals("id", result.profileId());
        assertEquals("name", result.profileName());
    }
}
