
package org.top.java.netty.example.udt.echo.rendezvous;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * UDT Message Flow Peer
 * <p>
 * Sends one message when a connection is open and echoes back any received data
 * to the other peer.
 */

/**
 * UDT消息流对等体
 * <p>
 * 当连接打开时发送一条消息，并将接收到的任何数据回显给另一个对等体。
 */
public abstract class MsgEchoPeerBase {

    protected final int messageSize;
    protected final InetSocketAddress self;
    protected final InetSocketAddress peer;

    protected MsgEchoPeerBase(final InetSocketAddress self, final InetSocketAddress peer, final int messageSize) {
        this.messageSize = messageSize;
        this.self = self;
        this.peer = peer;
    }

    public void run() throws Exception {
        // Configure the peer.
        // 配置对等节点。
        final ThreadFactory connectFactory = new DefaultThreadFactory("rendezvous");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
        try {
            final Bootstrap boot = new Bootstrap();
            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.MESSAGE_RENDEZVOUS)
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new MsgEchoPeerHandler(messageSize));
                        }
                    });
            // Start the peer.
            // 启动对等节点。
            final ChannelFuture f = boot.connect(peer, self).sync();
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
