package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

public final class JavaPacketIds {
    public static final int HANDSHAKE = 0x00;
    public static final int STATUS_REQUEST = 0x00;
    public static final int STATUS_RESPONSE = 0x00;
    public static final int STATUS_PING = 0x01;
    public static final int STATUS_PONG = 0x01;
    public static final int LOGIN_START = 0x00;
    public static final int LOGIN_DISCONNECT = 0x00;
    public static final int ENCRYPTION_REQUEST = 0x01;
    public static final int ENCRYPTION_RESPONSE = 0x01;
    public static final int LOGIN_SUCCESS = 0x02;
    public static final int LOGIN_ACKNOWLEDGED = 0x03;
    public static final int CONFIGURATION_DISCONNECT = 0x02;
    public static final int FINISH_CONFIGURATION = 0x03;
    public static final int PLAY_CLIENTBOUND_KEEP_ALIVE = 0x27;
    public static final int PLAY_SERVERBOUND_KEEP_ALIVE = 0x1a;
    public static final int PLAY_SERVERBOUND_SETTINGS = 0x0c;
    public static final int PLAY_CLIENTBOUND_PING = 0x37;
    public static final int PLAY_SERVERBOUND_PONG = 0x2b;
    public static final int PLAY_LEGACY_DISCONNECT = 0x40;
    public static final int PLAY_MODERN_DISCONNECT = 0x1d;

    private JavaPacketIds() {
    }
}

