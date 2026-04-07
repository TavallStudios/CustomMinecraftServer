package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramHandler;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public final class BedrockUdpServerBootstrap implements AutoCloseable {
    private final ServerSettings settings;
    private final BedrockDatagramHandler handler;
    private EventLoopGroup group;
    private Channel channel;

    public BedrockUdpServerBootstrap(ServerSettings settings, BedrockDatagramHandler handler) {
        this.settings = settings;
        this.handler = handler;
    }

    public void start() throws InterruptedException {
        group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(handler);
        channel = bootstrap.bind(settings.host(), settings.bedrockUdpPort()).sync().channel();
    }

    public void awaitClose() throws InterruptedException {
        if (channel != null) {
            channel.closeFuture().sync();
        }
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}

