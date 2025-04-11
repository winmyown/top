
package org.top.java.netty.example.udt.echo.message;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 * UDT Message Flow client
 * <p>
 * Sends one message when a connection is open and echoes back any received data
 * to the server. Simply put, the echo client initiates the ping-pong traffic
 * between the echo client and server by sending the first message to the
 * server.
 */

/**
 * UDT消息流客户端
 * <p>
 * 当连接打开时发送一条消息，并将接收到的任何数据回显给服务器。简单来说，回显客户端通过向服务器发送第一条消息来启动回显客户端和服务器之间的“乒乓”通信。
 */
public final class MsgEchoClient {

    private static final Logger log = Logger.getLogger(MsgEchoClient.class.getName());

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {

        // Configure the client.

        // 配置客户端。
        final ThreadFactory connectFactory = new DefaultThreadFactory("connect");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
        try {
            final Bootstrap boot = new Bootstrap();
            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.MESSAGE_CONNECTOR)
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new MsgEchoClientHandler());
                        }
                    });
            // Start the client.
            // 启动客户端。
            final ChannelFuture f = boot.connect(HOST, PORT).sync();
            // Wait until the connection is closed.
            // 等待连接关闭。
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            // 关闭事件循环以终止所有线程。
            connectGroup.shutdownGracefully();
        }
    }
}
