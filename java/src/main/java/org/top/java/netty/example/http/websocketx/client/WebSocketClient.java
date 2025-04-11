package org.top.java.netty.example.http.websocketx.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * This is an example of a WebSocket client.
 * <p>
 * In order to run this example you need a compatible WebSocket server.
 * Therefore you can either start the WebSocket server from the examples
 * by running {@link io.netty.example.http.websocketx.server.WebSocketServer}
 * or connect to an existing WebSocket server such as
 * <a href="https://www.websocket.org/echo.html">ws://echo.websocket.org</a>.
 * <p>
 * The client will attempt to connect to the URI passed to it as the first argument.
 * You don't have to specify any arguments if you want to connect to the example WebSocket server,
 * as this is the default.
 */

/**
 * 这是一个WebSocket客户端的示例。
 * <p>
 * 为了运行此示例，您需要一个兼容的WebSocket服务器。
 * 因此，您可以通过运行{@link io.netty.example.http.websocketx.server.WebSocketServer}
 * 来启动示例中的WebSocket服务器，或者连接到现有的WebSocket服务器，例如
 * <a href="https://www.websocket.org/echo.html">ws://echo.websocket.org</a>。
 * <p>
 * 客户端将尝试连接到作为第一个参数传递给它的URI。
 * 如果您想连接到示例WebSocket服务器，则无需指定任何参数，因为这是默认设置。
 */
public final class WebSocketClient {

    static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");

    public static void main(String[] args) throws Exception {
        URI uri = new URI(URL);
        String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // 使用 V13 (RFC 6455 即 HyBi-17) 进行连接。您可以将其更改为 V08 或 V00。
            // If you change it to V00, ping is not supported and remember to change
            // 如果更改为 V00，则不支持 ping，请记得更改
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            // 将HttpResponseDecoder替换为WebSocketHttpResponseDecoder到管道中。
            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                     }
                     p.addLast(
                             new HttpClientCodec(),
                             new HttpObjectAggregator(8192),
                             WebSocketClientCompressionHandler.INSTANCE,
                             handler);
                 }
             });

            Channel ch = b.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();
                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    ch.writeAndFlush(new CloseWebSocketFrame());
                    ch.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                    ch.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    ch.writeAndFlush(frame);
                }
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
