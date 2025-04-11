
package org.top.java.netty.example.sctp.multihoming;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.SctpChannel;
import io.netty.channel.sctp.SctpServerChannel;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import org.top.java.netty.example.sctp.SctpEchoServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.SocketUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * SCTP Echo Server with multi-homing support.
 */

/**
 * 支持多归属的SCTP回显服务器。
 */
public final class SctpMultiHomingEchoServer {

    private static final String SERVER_PRIMARY_HOST = System.getProperty("host.primary", "127.0.0.1");
    private static final String SERVER_SECONDARY_HOST = System.getProperty("host.secondary", "127.0.0.2");

    private static final int SERVER_PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure the server.
        // 配置服务器。
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioSctpServerChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SctpChannel>() {
                 @Override
                 public void initChannel(SctpChannel ch) throws Exception {
                     ch.pipeline().addLast(
//                             new LoggingHandler(LogLevel.INFO),
//                             new LoggingHandler(LogLevel.INFO),
                             new SctpEchoServerHandler());
                 }
             });

            InetSocketAddress localAddress = SocketUtils.socketAddress(SERVER_PRIMARY_HOST, SERVER_PORT);
            InetAddress localSecondaryAddress = SocketUtils.addressByName(SERVER_SECONDARY_HOST);

            // Bind the server to primary address.

            // 将服务器绑定到主地址。
            ChannelFuture bindFuture = b.bind(localAddress).sync();

            //Get the underlying sctp channel

            //获取底层的sctp通道
            SctpServerChannel channel = (SctpServerChannel) bindFuture.channel();

            //Bind the secondary address

            //绑定次要地址
            ChannelFuture connectFuture = channel.bindAddress(localSecondaryAddress).sync();

            // Wait until the connection is closed.

            // 等待连接关闭。
            connectFuture.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            // 关闭所有事件循环以终止所有线程。
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
