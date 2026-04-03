package com.customminecraftserver.bedrock;

final class BedrockProtocolMetadata {
    static final int ADVERTISEMENT_PROTOCOL = 944;
    static final String ADVERTISEMENT_VERSION = "26.10";

    private BedrockProtocolMetadata() {
    }

    static String gameVersion(Integer protocolVersion) {
        if (protocolVersion == null) {
            return ADVERTISEMENT_VERSION;
        }
        return switch (protocolVersion) {
            case 944 -> "26.10";
            case 924 -> "1.26.0";
            case 898 -> "1.21.130";
            case 860 -> "1.21.124";
            case 859 -> "1.21.120";
            case 827 -> "1.21.100";
            default -> ADVERTISEMENT_VERSION;
        };
    }
}
