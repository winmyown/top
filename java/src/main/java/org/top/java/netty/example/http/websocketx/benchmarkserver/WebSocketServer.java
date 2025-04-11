package org.top.java.netty.example.http.websocketx.benchmarkserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.top.java.netty.example.util.ServerUtil;
import io.netty.handler.ssl.SslContext;

/**
 * A Benchmark application for websocket which is served at:
 *
 * http://localhost:8080/websocket
 *
 * Open your browser at http://localhost:8080/, then the benchmark page will be loaded and a Web Socket connection will
 * be made automatically.
 */

/**
 * 一个用于WebSocket的基准测试应用程序，它服务于：
 *
 * http://localhost:8080/websocket
 *
 * 在浏览器中打开 http://localhost:8080/，然后基准测试页面将被加载，并且Web Socket连接将自动建立。
 */
public final class WebSocketServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        // 配置SSL。
        final SslContext sslCtx = ServerUtil.buildSslContext();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new WebSocketServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            System.out.println("Open your web browser and navigate to " +
                    (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
