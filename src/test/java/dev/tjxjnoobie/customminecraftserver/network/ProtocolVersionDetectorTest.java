package dev.tjxjnoobie.customminecraftserver.network;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolVersionDetectorTest {
    @Test
    void detectsJavaProfiles() {
        TestLogSupport.logTestStart("ProtocolVersionDetectorTest.detectsJavaProfiles");
        ProtocolVersionDetector detector = new ProtocolVersionDetector();

        ProtocolVersionDetector.JavaProtocolProfile legacy = detector.detectJava(47);
        assertEquals("JAVA_1_8_X", legacy.family());
        assertTrue(legacy.supported());

        ProtocolVersionDetector.JavaProtocolProfile modern = detector.detectJava(769);
        assertEquals("JAVA_1_21_X", modern.family());
        assertTrue(modern.supported());

        ProtocolVersionDetector.JavaProtocolProfile unsupported = detector.detectJava(100);
        assertEquals("JAVA_UNSUPPORTED", unsupported.family());
        assertFalse(unsupported.supported());
    }

    @Test
    void detectsBedrockProfiles() {
        TestLogSupport.logTestStart("ProtocolVersionDetectorTest.detectsBedrockProfiles");
        ProtocolVersionDetector detector = new ProtocolVersionDetector();
        ProtocolVersionDetector.BedrockProtocolProfile profile = detector.detectBedrock(944);
        assertEquals("BEDROCK_DYNAMIC", profile.family());
        assertTrue(profile.supported());
    }
}
