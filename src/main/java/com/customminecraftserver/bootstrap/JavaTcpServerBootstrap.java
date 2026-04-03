package com.customminecraftserver.bootstrap;

import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.javaedition.JavaConnectionHandler;
import com.customminecraftserver.javaedition.JavaPacketFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public final class JavaTcpServerBootstrap implements AutoCloseable {
    private final ServerSettings settings;
    private final JavaConnectionHandler connectionHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public JavaTcpServerBootstrap(ServerSettings settings, JavaConnectionHandler connectionHandler) {
        this.settings = settings;
        this.connectionHandler = connectionHandler;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast("java-frame-decoder", new JavaPacketFrameDecoder());
                        channel.pipeline().addLast("java-connection-handler", connectionHandler.copy());
                    }
                });

        channel = bootstrap.bind(settings.host(), settings.javaTcpPort()).sync().channel();
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
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
