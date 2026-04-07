package dev.tjxjnoobie.customminecraftserver.config;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaAuthenticationSettingsTest {
    @Test
    void defaultsApplyWhenValuesMissing() {
        TestLogSupport.logTestStart("JavaAuthenticationSettingsTest.defaultsApplyWhenValuesMissing");
        JavaAuthenticationSettings settings = new JavaAuthenticationSettings("", false, 0);
        assertEquals("https://sessionserver.mojang.com", settings.sessionServerUrl());
        assertEquals(false, settings.includeClientIpInSessionVerification());
        assertEquals(1024, settings.rsaKeySizeBits());
    }

    @Test
    void defaultsFactoryMatchesExpected() {
        TestLogSupport.logTestStart("JavaAuthenticationSettingsTest.defaultsFactoryMatchesExpected");
        JavaAuthenticationSettings settings = JavaAuthenticationSettings.defaults();
        assertEquals("https://sessionserver.mojang.com", settings.sessionServerUrl());
        assertEquals(false, settings.includeClientIpInSessionVerification());
        assertEquals(1024, settings.rsaKeySizeBits());
    }
}
