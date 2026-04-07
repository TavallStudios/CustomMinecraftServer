package dev.tjxjnoobie.customminecraftserver.config;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BedrockAuthenticationSettingsTest {
    @Test
    void defaultsProvideTrustedKeys() {
        TestLogSupport.logTestStart("BedrockAuthenticationSettingsTest.defaultsProvideTrustedKeys");
        BedrockAuthenticationSettings settings = BedrockAuthenticationSettings.defaults();
        assertFalse(settings.trustedRootPublicKeys().isEmpty());
        assertEquals(true, settings.requireTrustedRootChain());
    }

    @Test
    void customKeysAreCopied() {
        TestLogSupport.logTestStart("BedrockAuthenticationSettingsTest.customKeysAreCopied");
        List<String> keys = new ArrayList<>(List.of("key-1"));
        BedrockAuthenticationSettings settings = new BedrockAuthenticationSettings(true, keys);
        keys.add("key-2");
        assertEquals(List.of("key-1"), settings.trustedRootPublicKeys());
        assertThrows(UnsupportedOperationException.class, () -> settings.trustedRootPublicKeys().add("key-3"));
    }
}
