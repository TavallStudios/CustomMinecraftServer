package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.Inflater;

public final class BedrockRakNetCodec {
    public static final byte[] RAKNET_MAGIC = new byte[]{
            0x00, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, 0x12, 0x34, 0x56, 0x78
    };

    private BedrockRakNetCodec() {
    }

    public static int readUnsignedVarInt(ByteBuf in) {
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = in.readByte();
            value |= (current & 0x7f) << position;
            position += 7;
            if (position > 35) {
                throw new IllegalArgumentException("Bedrock VarInt is too large");
            }
        } while ((current & 0x80) == 0x80);
        return value;
    }

    public static void writeUnsignedVarInt(ByteBuf out, int value) {
        int remaining = value;
        do {
            byte part = (byte) (remaining & 0x7f);
            remaining >>>= 7;
            if (remaining != 0) {
                part |= (byte) 0x80;
            }
            out.writeByte(part);
        } while (remaining != 0);
    }

    public static long readUnsignedVarLong(ByteBuf in) {
        long value = 0L;
        int position = 0;
        byte current;
        do {
            current = in.readByte();
            value |= (long) (current & 0x7f) << position;
            position += 7;
            if (position > 70) {
                throw new IllegalArgumentException("Bedrock VarLong is too large");
            }
        } while ((current & 0x80) == 0x80);
        return value;
    }

    public static void writeUnsignedVarLong(ByteBuf out, long value) {
        long remaining = value;
        do {
            byte part = (byte) (remaining & 0x7fL);
            remaining >>>= 7;
            if (remaining != 0L) {
                part |= (byte) 0x80;
            }
            out.writeByte(part);
        } while (remaining != 0L);
    }

    public static int encodeZigZag32(int value) {
        return (value << 1) ^ (value >> 31);
    }

    public static int decodeZigZag32(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    public static long encodeZigZag64(long value) {
        return (value << 1) ^ (value >> 63);
    }

    public static long decodeZigZag64(long value) {
        return (value >>> 1) ^ -(value & 1L);
    }

    public static void writeZigZag32(ByteBuf out, int value) {
        writeUnsignedVarInt(out, encodeZigZag32(value));
    }

    public static void writeZigZag64(ByteBuf out, long value) {
        writeUnsignedVarLong(out, encodeZigZag64(value));
    }

    public static int readLittleTriad(ByteBuf in) {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        return b1 | (b2 << 8) | (b3 << 16);
    }

    public static void writeLittleTriad(ByteBuf out, int value) {
        out.writeByte(value & 0xff);
        out.writeByte((value >>> 8) & 0xff);
        out.writeByte((value >>> 16) & 0xff);
    }

    public static void writeString(ByteBuf out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeUnsignedVarInt(out, bytes.length);
        out.writeBytes(bytes);
    }

    public static String readString(ByteBuf in) {
        int length = readUnsignedVarInt(in);
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeRakNetString(ByteBuf out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.writeBytes(bytes);
    }

    public static String readLittleString(ByteBuf in) {
        int length = in.readIntLE();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static InetSocketAddress readAddress(ByteBuf in) {
        int version = in.readUnsignedByte();
        if (version != 4) {
            throw new IllegalArgumentException("Only IPv4 Bedrock addresses are supported in the MVP");
        }
        int b1 = (~in.readUnsignedByte()) & 0xff;
        int b2 = (~in.readUnsignedByte()) & 0xff;
        int b3 = (~in.readUnsignedByte()) & 0xff;
        int b4 = (~in.readUnsignedByte()) & 0xff;
        int port = in.readUnsignedShort();
        return new InetSocketAddress(b1 + "." + b2 + "." + b3 + "." + b4, port);
    }

    public static void writeAddress(ByteBuf out, InetSocketAddress address) {
        out.writeByte(4);
        byte[] octets = resolveIpv4(address.getHostString());
        for (byte octet : octets) {
            out.writeByte(~octet);
        }
        out.writeShort(address.getPort());
    }

    public static List<ByteBuf> readBatch(ByteBuf in, boolean compressionNegotiated) {
        in.readByte();
        ByteBuf payload = in.readSlice(in.readableBytes());
        ByteBuf packetStream;
        if (compressionNegotiated) {
            short compressorHeader = payload.readUnsignedByte();
            if (compressorHeader == 0xff) {
                packetStream = payload.readSlice(payload.readableBytes());
            } else if (compressorHeader == 0x00) {
                packetStream = inflateRaw(payload);
            } else {
                throw new IllegalArgumentException("Unsupported Bedrock compressor header: " + compressorHeader);
            }
        } else {
            packetStream = payload;
        }

        List<ByteBuf> packets = new ArrayList<>();
        while (packetStream.isReadable()) {
            int packetLength = readUnsignedVarInt(packetStream);
            packets.add(packetStream.readRetainedSlice(packetLength));
        }
        return packets;
    }

    public static String decodeBase64Url(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static ByteBuf inflateRaw(ByteBuf input) {
        byte[] compressed = new byte[input.readableBytes()];
        input.readBytes(compressed);
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressed);
        byte[] buffer = new byte[8192];
        ByteBuf output = Unpooled.buffer();
        try {
            while (!inflater.finished() && !inflater.needsInput()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    break;
                }
                output.writeBytes(buffer, 0, count);
            }
            return output;
        } catch (Exception exception) {
            output.release();
            throw new IllegalStateException("Unable to inflate Bedrock batch", exception);
        } finally {
            inflater.end();
        }
    }

    private static byte[] resolveIpv4(String host) {
        String normalized = host == null || host.isBlank() || host.equals("0.0.0.0") ? "127.0.0.1" : host;
        String[] segments = normalized.split("\\.");
        if (segments.length != 4) {
            throw new IllegalArgumentException("Only IPv4 addresses are supported in the MVP: " + host);
        }
        byte[] octets = new byte[4];
        for (int index = 0; index < 4; index++) {
            octets[index] = (byte) Integer.parseInt(segments[index]);
        }
        return octets;
    }
}

