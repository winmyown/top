
package org.top.java.netty.example.worldclock;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.top.java.netty.example.util.ServerUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Receives a list of continent/city pairs from a {@link WorldClockClient} to
 * get the local times of the specified cities.
 */

/**
 * 从 {@link WorldClockClient} 接收一个大陆/城市对列表，
 * 以获取指定城市的本地时间。
 */
public final class WorldClockServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8463"));

    public static void main(String[] args) throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new WorldClockServerInitializer(ServerUtil.buildSslContext()));

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
