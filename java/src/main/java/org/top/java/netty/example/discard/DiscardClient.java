
package org.top.java.netty.example.discard;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.top.java.netty.example.util.ServerUtil;
import io.netty.handler.ssl.SslContext;

/**
 * Keeps sending random data to the specified address.
 */

/**
 * 持续向指定地址发送随机数据。
 */
public final class DiscardClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        // 配置SSL。
        final SslContext sslCtx = ServerUtil.buildSslContext();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
                     }
                     p.addLast(new DiscardClientHandler());
                 }
             });

            // Make the connection attempt.

            // 尝试建立连接。
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.

            // 等待连接关闭。
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
