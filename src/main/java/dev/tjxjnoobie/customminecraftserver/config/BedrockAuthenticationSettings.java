package dev.tjxjnoobie.customminecraftserver.config;

import java.util.List;

public record BedrockAuthenticationSettings(
        boolean requireTrustedRootChain,
        List<String> trustedRootPublicKeys
) {
    public BedrockAuthenticationSettings {
        requireTrustedRootChain = requireTrustedRootChain;
        trustedRootPublicKeys = trustedRootPublicKeys == null || trustedRootPublicKeys.isEmpty()
                ? List.of("MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJMcUpruK4QveTfJSTp3Shlq4Gk34cD/4GUWwkv0DVuzeuB+tXija7HBxii03NHDbPAD0AKnLr2wdAp")
                : List.copyOf(trustedRootPublicKeys);
    }

    public static BedrockAuthenticationSettings defaults() {
        return new BedrockAuthenticationSettings(
                true,
                List.of("MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJMcUpruK4QveTfJSTp3Shlq4Gk34cD/4GUWwkv0DVuzeuB+tXija7HBxii03NHDbPAD0AKnLr2wdAp")
        );
    }
}

