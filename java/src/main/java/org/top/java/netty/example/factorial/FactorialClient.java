
package org.top.java.netty.example.factorial;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.top.java.netty.example.util.ServerUtil;

/**
 * Sends a sequence of integers to a {@link FactorialServer} to calculate
 * the factorial of the specified integer.
 */

/**
 * 将整数序列发送到 {@link FactorialServer} 以计算指定整数的阶乘。
 */
public final class FactorialClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8322"));
    static final int COUNT = Integer.parseInt(System.getProperty("count", "1000"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        // 配置SSL。
        final SslContext sslCtx = ServerUtil.buildSslContext();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new FactorialClientInitializer(sslCtx));

            // Make a new connection.

            // 建立新连接。
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Get the handler instance to retrieve the answer.

            // 获取处理程序实例以检索答案。
            FactorialClientHandler handler =
                (FactorialClientHandler) f.channel().pipeline().last();

            // Print out the answer.

            // 打印出答案。
            System.err.format("Factorial of %,d is: %,d", COUNT, handler.getFactorial());
        } finally {
            group.shutdownGracefully();
        }
    }
}
