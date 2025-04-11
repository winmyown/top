
package org.top.java.netty.example.http.cors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.top.java.netty.example.util.ServerUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

/**
 * This example server aims to demonstrate
 * <a href="https://www.w3.org/TR/cors/">Cross Origin Resource Sharing</a> (CORS) in Netty.
 * It does not have a client like most of the other examples, but instead has
 * a html page that is loaded to try out CORS support in a web browser.
 * <p>
 *
 * CORS is configured in {@link HttpCorsServerInitializer} and by updating the config you can
 * try out various combinations, like using a specific origin instead of a
 * wildcard origin ('*').
 * <p>
 *
 * The file {@code src/main/resources/cors/cors.html} contains a very basic example client
 * which can be used to try out different configurations. For example, you can add
 * custom headers to force a CORS preflight request to make the request fail. Then
 * to enable a successful request, configure the CorsHandler to allow that/those
 * request headers.
 *
 * <h2>Testing CORS</h2>
 * You can either load the file {@code src/main/resources/cors/cors.html} using a web server
 * or load it from the file system using a web browser.
 *
 * <h3>Using a web server</h3>
 * To test CORS support you can serve the file {@code src/main/resources/cors/cors.html}
 * using a web server. You can then add a new host name to your systems hosts file, for
 * example if you are on Linux you may update /etc/hosts to add an additional name
 * for you local system:
 * <pre>
 * 127.0.0.1   localhost domain1.com
 * </pre>
 * Now, you should be able to access {@code http://domain1.com/cors.html} depending on how you
 * have configured you local web server the exact url may differ.
 *
 * <h3>Using a web browser</h3>
 * Open the file {@code src/main/resources/cors/cors.html} in a web browser. You should see
 * loaded page and in the text area the following message:
 * <pre>
 * 'CORS is not working'
 * </pre>
 *
 * If you inspect the headers being sent using your browser you'll see that the 'Origin'
 * request header is {@code 'null'}. This is expected and happens when you load a file from the
 * local file system. Netty can handle this by configuring the CorsHandler which is done
 * in the {@link HttpCorsServerInitializer}.
 *
 */

/**
 * 此示例服务器旨在演示在 Netty 中的
 * <a href="https://www.w3.org/TR/cors/">跨域资源共享</a> (CORS)。
 * 它不像大多数其他示例那样有一个客户端，而是有一个 HTML 页面，该页面被加载以在 Web 浏览器中尝试 CORS 支持。
 * <p>
 *
 * CORS 在 {@link HttpCorsServerInitializer} 中配置，通过更新配置，您可以尝试各种组合，例如使用特定源而不是通配符源 ('*')。
 * <p>
 *
 * 文件 {@code src/main/resources/cors/cors.html} 包含一个非常基本的示例客户端，可用于尝试不同的配置。例如，您可以添加自定义标头以强制 CORS 预检请求，从而使请求失败。然后，要启用成功的请求，请配置 CorsHandler 以允许该/那些请求标头。
 *
 * <h2>测试 CORS</h2>
 * 您可以使用 Web 服务器加载文件 {@code src/main/resources/cors/cors.html}，或者使用 Web 浏览器从文件系统加载它。
 *
 * <h3>使用 Web 服务器</h3>
 * 要测试 CORS 支持，您可以使用 Web 服务器提供文件 {@code src/main/resources/cors/cors.html}。然后，您可以将新主机名添加到系统的 hosts 文件中，例如，如果您在 Linux 上，您可以更新 /etc/hosts 以添加本地系统的额外名称：
 * <pre>
 * 127.0.0.1   localhost domain1.com
 * </pre>
 * 现在，您应该能够访问 {@code http://domain1.com/cors.html}，具体取决于您如何配置本地 Web 服务器，确切的 URL 可能会有所不同。
 *
 * <h3>使用 Web 浏览器</h3>
 * 在 Web 浏览器中打开文件 {@code src/main/resources/cors/cors.html}。您应该看到加载的页面，并且在文本区域中看到以下消息：
 * <pre>
 * 'CORS 未工作'
 * </pre>
 *
 * 如果您使用浏览器检查发送的标头，您会看到 'Origin' 请求标头是 {@code 'null'}。这是预期的，当您从本地文件系统加载文件时会发生这种情况。Netty 可以通过配置 CorsHandler 来处理此问题，这是在 {@link HttpCorsServerInitializer} 中完成的。
 *
 */
public final class HttpCorsServer {

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
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpCorsServerInitializer(sslCtx));

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
