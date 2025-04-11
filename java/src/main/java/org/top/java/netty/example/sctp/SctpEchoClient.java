
package org.top.java.netty.example.sctp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.SctpChannel;
import io.netty.channel.sctp.SctpChannelOption;
import io.netty.channel.sctp.nio.NioSctpChannel;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server over SCTP connection.
 *
 * Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */

/**
 * 当连接打开时发送一条消息，并将接收到的任何数据通过SCTP连接回显到服务器。
 *
 * 简单来说，回显客户端通过向服务器发送第一条消息来启动回显客户端和服务器之间的乒乓通信。
 */
public final class SctpEchoClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure the client.
        // 配置客户端。
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSctpChannel.class)
             .option(SctpChannelOption.SCTP_NODELAY, true)
             .handler(new ChannelInitializer<SctpChannel>() {
                 @Override
                 public void initChannel(SctpChannel ch) throws Exception {
                     ch.pipeline().addLast(
                             //new LoggingHandler(LogLevel.INFO),
                             //new LoggingHandler(LogLevel.INFO),
                             new SctpEchoClientHandler());
                 }
             });

            // Start the client.

            // 启动客户端。
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.

            // 等待连接关闭。
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            // 关闭事件循环以终止所有线程。
            group.shutdownGracefully();
        }
    }
}
