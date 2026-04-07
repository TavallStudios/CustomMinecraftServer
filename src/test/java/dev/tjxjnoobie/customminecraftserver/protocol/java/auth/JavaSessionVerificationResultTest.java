package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSessionVerificationResultTest {
    @Test
    void recordCarriesProfileFields() {
        TestLogSupport.logTestStart("JavaSessionVerificationResultTest.recordCarriesProfileFields");
        JavaSessionVerificationResult result = new JavaSessionVerificationResult("profile-id", "profile-name");
        assertEquals("profile-id", result.profileId());
        assertEquals("profile-name", result.profileName());
    }
}
