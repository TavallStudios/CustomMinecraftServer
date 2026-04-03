package com.customminecraftserver.bedrock;

public final class BedrockPacketIds {
    public static final int RAKNET_UNCONNECTED_PING = 0x01;
    public static final int RAKNET_UNCONNECTED_PONG = 0x1c;
    public static final int RAKNET_OPEN_CONNECTION_REQUEST_1 = 0x05;
    public static final int RAKNET_OPEN_CONNECTION_REPLY_1 = 0x06;
    public static final int RAKNET_OPEN_CONNECTION_REQUEST_2 = 0x07;
    public static final int RAKNET_OPEN_CONNECTION_REPLY_2 = 0x08;
    public static final int RAKNET_CONNECTION_REQUEST = 0x09;
    public static final int RAKNET_CONNECTION_REQUEST_ACCEPTED = 0x10;
    public static final int RAKNET_NEW_INCOMING_CONNECTION = 0x13;
    public static final int RAKNET_CONNECTED_PING = 0x00;
    public static final int RAKNET_CONNECTED_PONG = 0x03;
    public static final int RAKNET_DISCONNECT_NOTIFICATION = 0x15;
    public static final int RAKNET_CONNECTED_DATAGRAM_MIN = 0x80;
    public static final int RAKNET_CONNECTED_DATAGRAM_MAX = 0x8d;
    public static final int BEDROCK_BATCH = 0xfe;
    public static final int RAKNET_ACK = 0xc0;
    public static final int RAKNET_NACK = 0xa0;
    public static final int BEDROCK_REQUEST_NETWORK_SETTINGS = 193;
    public static final int BEDROCK_NETWORK_SETTINGS = 143;
    public static final int BEDROCK_LOGIN = 1;
    public static final int BEDROCK_PLAY_STATUS = 2;
    public static final int BEDROCK_SERVER_TO_CLIENT_HANDSHAKE = 3;
    public static final int BEDROCK_CLIENT_TO_SERVER_HANDSHAKE = 4;
    public static final int BEDROCK_DISCONNECT = 5;
    public static final int BEDROCK_RESOURCE_PACKS_INFO = 6;
    public static final int BEDROCK_RESOURCE_PACK_STACK = 7;
    public static final int BEDROCK_RESOURCE_PACK_CLIENT_RESPONSE = 8;
    public static final int BEDROCK_START_GAME = 11;
    public static final int BEDROCK_REQUEST_CHUNK_RADIUS = 69;
    public static final int BEDROCK_CHUNK_RADIUS_UPDATE = 70;
    public static final int BEDROCK_SET_LOCAL_PLAYER_AS_INITIALIZED = 113;
    public static final int BEDROCK_CLIENT_CACHE_STATUS = 129;

    private BedrockPacketIds() {
    }
}
