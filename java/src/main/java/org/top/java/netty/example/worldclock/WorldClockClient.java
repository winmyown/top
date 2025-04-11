
package org.top.java.netty.example.worldclock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.top.java.netty.example.util.ServerUtil;
import io.netty.handler.ssl.SslContext;

import java.util.Arrays;
import java.util.List;

/**
 * Sends a list of continent/city pairs to a {@link WorldClockServer} to
 * get the local times of the specified cities.
 */

/**
 * 将一系列大陆/城市对发送到 {@link WorldClockServer} 以获取指定城市的本地时间。
 */
public final class WorldClockClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8463"));
    static final List<String> CITIES = Arrays.asList(System.getProperty(
            "cities", "Asia/Seoul,Europe/Berlin,America/Los_Angeles").split(","));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        // 配置SSL。
        final SslContext sslCtx = ServerUtil.buildSslContext();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new WorldClockClientInitializer(sslCtx));

            // Make a new connection.

            // 建立新连接。
            Channel ch = b.connect(HOST, PORT).sync().channel();

            // Get the handler instance to initiate the request.

            // 获取处理程序实例以发起请求。
            WorldClockClientHandler handler = ch.pipeline().get(WorldClockClientHandler.class);

            // Request and get the response.

            // 请求并获取响应。
            List<String> response = handler.getLocalTimes(CITIES);

            // Close the connection.

            // 关闭连接。
            ch.close();

            // Print the response at last but not least.

            // 最后但同样重要的是，打印响应。
            for (int i = 0; i < CITIES.size(); i ++) {
                System.out.format("%28s: %s%n", CITIES.get(i), response.get(i));
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
