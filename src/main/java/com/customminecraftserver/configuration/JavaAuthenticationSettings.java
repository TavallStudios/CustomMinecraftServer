package com.customminecraftserver.configuration;

public record JavaAuthenticationSettings(
        String sessionServerUrl,
        boolean includeClientIpInSessionVerification,
        int rsaKeySizeBits
) {
    public JavaAuthenticationSettings {
        sessionServerUrl = sessionServerUrl == null || sessionServerUrl.isBlank()
                ? "https://sessionserver.mojang.com"
                : sessionServerUrl;
        includeClientIpInSessionVerification = includeClientIpInSessionVerification;
        rsaKeySizeBits = rsaKeySizeBits <= 0 ? 1024 : rsaKeySizeBits;
    }

    public static JavaAuthenticationSettings defaults() {
        return new JavaAuthenticationSettings(
                "https://sessionserver.mojang.com",
                false,
                1024
        );
    }
}
