package com.customminecraftserver.networking;

public final class ProtocolVersionDetector {
    public JavaProtocolProfile detectJava(int protocolVersion) {
        if (protocolVersion == 47) {
            return new JavaProtocolProfile(protocolVersion, "JAVA_1_8_X", true);
        }
        if (protocolVersion >= 767 && protocolVersion <= 774) {
            return new JavaProtocolProfile(protocolVersion, "JAVA_1_21_X", true);
        }
        return new JavaProtocolProfile(protocolVersion, "JAVA_UNSUPPORTED", false);
    }

    public BedrockProtocolProfile detectBedrock(int protocolVersion) {
        return new BedrockProtocolProfile(protocolVersion, "BEDROCK_DYNAMIC", protocolVersion > 0);
    }

    public record JavaProtocolProfile(int protocolVersion, String family, boolean supported) {
    }

    public record BedrockProtocolProfile(int protocolVersion, String family, boolean supported) {
    }
}
