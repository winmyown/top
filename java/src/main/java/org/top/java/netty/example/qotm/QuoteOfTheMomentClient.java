
package org.top.java.netty.example.qotm;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SocketUtils;

/**
 * A UDP broadcast client that asks for a quote of the moment (QOTM) to {@link QuoteOfTheMomentServer}.
 *
 * Inspired by <a href="https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html">the official
 * Java tutorial</a>.
 */

/**
 * 一个UDP广播客户端，向{@link QuoteOfTheMomentServer}请求时刻名言（QOTM）。
 *
 * 灵感来自<a href="https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html">官方Java教程</a>。
 */
public final class QuoteOfTheMomentClient {

    static final int PORT = Integer.parseInt(System.getProperty("port", "7686"));

    public static void main(String[] args) throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioDatagramChannel.class)
             .option(ChannelOption.SO_BROADCAST, true)
             .handler(new QuoteOfTheMomentClientHandler());

            Channel ch = b.bind(0).sync().channel();

            // Broadcast the QOTM request to port 8080.

            // 将 QOTM 请求广播到端口 8080。
            ch.writeAndFlush(new DatagramPacket(
                    Unpooled.copiedBuffer("QOTM?", CharsetUtil.UTF_8),
                    SocketUtils.socketAddress("255.255.255.255", PORT))).sync();

            // QuoteOfTheMomentClientHandler will close the DatagramChannel when a

            // QuoteOfTheMomentClientHandler 将在接收到数据后关闭 DatagramChannel
            // response is received.  If the channel is not closed within 5 seconds,
            // 接收到响应。如果通道在5秒内未关闭，
            // print an error message and quit.
            // 打印错误信息并退出。
            if (!ch.closeFuture().await(5000)) {
                System.err.println("QOTM request timed out.");
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
