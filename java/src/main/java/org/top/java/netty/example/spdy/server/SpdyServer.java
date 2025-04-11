package org.top.java.netty.example.spdy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * A SPDY Server that responds to a GET request with a Hello World.
 * <p>
 * This class must be run with the JVM parameter: {@code java -Xbootclasspath/p:<path_to_npn_boot_jar> ...}.
 * The "path_to_npn_boot_jar" is the path on the file system for the NPN Boot Jar file which can be downloaded from
 * Maven at coordinates org.mortbay.jetty.npn:npn-boot. Different versions applies to different OpenJDK versions.
 * See <a href="https://www.eclipse.org/jetty/documentation/current/npn-chapter.html">Jetty docs</a> for more
 * information.
 * <p>
 * You may also use the {@code run-example.sh} script to start the server from the command line:
 * <pre>
 *     ./run-example.sh spdy-server
 * </pre>
 * <p>
 * Once started, you can test the server with your
 * <a href="https://en.wikipedia.org/wiki/SPDY#Browser_support_and_usage">SPDY enabled web browser</a> by navigating
 * to <a href="https://localhost:8443/">https://localhost:8443/</a>
 */

/**
 * 一个SPDY服务器，响应GET请求并返回Hello World。
 * <p>
 * 运行此类时必须使用JVM参数：{@code java -Xbootclasspath/p:<path_to_npn_boot_jar> ...}。
 * "path_to_npn_boot_jar"是文件系统中NPN Boot Jar文件的路径，该文件可以从Maven下载，坐标为org.mortbay.jetty.npn:npn-boot。不同的版本适用于不同的OpenJDK版本。
 * 有关更多信息，请参阅<a href="https://www.eclipse.org/jetty/documentation/current/npn-chapter.html">Jetty文档</a>。
 * <p>
 * 你也可以使用{@code run-example.sh}脚本从命令行启动服务器：
 * <pre>
 *     ./run-example.sh spdy-server
 * </pre>
 * <p>
 * 启动后，你可以使用<a href="https://en.wikipedia.org/wiki/SPDY#Browser_support_and_usage">支持SPDY的Web浏览器</a>通过访问<a href="https://localhost:8443/">https://localhost:8443/</a>来测试服务器。
 */
public final class SpdyServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8443"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        // 配置SSL。
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                        Protocol.NPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        // NO_ADVERTISE 是目前 OpenSsl 和 JDK 提供程序都支持的唯一模式。
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        // ACCEPT 是目前唯一受 OpenSsl 和 JDK 提供程序支持的模式。
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.SPDY_3_1,
                        ApplicationProtocolNames.HTTP_1_1))
            .build();

        // Configure the server.

        // 配置服务器。
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new SpdyServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Open your SPDY-enabled web browser and navigate to https://127.0.0.1:" + PORT + '/');
            System.err.println("If using Chrome browser, check your SPDY sessions at chrome://net-internals/#spdy");

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
