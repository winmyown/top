
/**
 * This package contains an example SPDY HTTP client. It will behave like a SPDY-enabled browser and you can see the
 * SPDY frames flowing in and out using the {@link io.netty.example.spdy.client.SpdyFrameLogger}.
 *
 * <p>
 * This package relies on the Jetty project's implementation of the Transport Layer Security (TLS) extension for Next
 * Protocol Negotiation (NPN) for OpenJDK 7 is required. NPN allows the application layer to negotiate which
 * protocol, SPDY or HTTP, to use.
 * <p>
 * To start, run {@link io.netty.example.spdy.server.SpdyServer} with the JVM parameter:
 * {@code java -Xbootclasspath/p:<path_to_npn_boot_jar> ...}.
 * The "path_to_npn_boot_jar" is the path on the file system for the NPN Boot Jar file which can be downloaded from
 * Maven at coordinates org.mortbay.jetty.npn:npn-boot. Different versions applies to different OpenJDK versions.
 * See <a href="https://www.eclipse.org/jetty/documentation/current/npn-chapter.html">Jetty docs</a> for more
 * information.
 * <p>
 * After that, you can run {@link io.netty.example.spdy.client.SpdyClient}, also settings the JVM parameter
 * mentioned above.
 * <p>
 * You may also use the {@code run-example.sh} script to start the server and the client from the command line:
 * <pre>
 *     ./run-example spdy-server
 * </pre>
 * Then start the client in a different terminal window:
 * <pre>
 *     ./run-example spdy-client
 * </pre>
 */

/**
 * 该包包含一个SPDY HTTP客户端示例。它将表现得像一个支持SPDY的浏览器，您可以使用
 * {@link io.netty.example.spdy.client.SpdyFrameLogger} 查看SPDY帧的流入和流出。
 *
 * <p>
 * 该包依赖于Jetty项目实现的传输层安全性（TLS）扩展，需要OpenJDK 7的Next Protocol Negotiation（NPN）。
 * NPN允许应用层协商使用哪种协议，SPDY或HTTP。
 * <p>
 * 要启动，请使用JVM参数运行 {@link io.netty.example.spdy.server.SpdyServer}：
 * {@code java -Xbootclasspath/p:<path_to_npn_boot_jar> ...}。
 * "path_to_npn_boot_jar" 是文件系统中NPN Boot Jar文件的路径，可以从Maven下载，坐标为
 * org.mortbay.jetty.npn:npn-boot。不同版本适用于不同的OpenJDK版本。
 * 有关更多信息，请参阅 <a href="https://www.eclipse.org/jetty/documentation/current/npn-chapter.html">Jetty文档</a>。
 * <p>
 * 之后，您可以运行 {@link io.netty.example.spdy.client.SpdyClient}，并设置上述JVM参数。
 * <p>
 * 您也可以使用 {@code run-example.sh} 脚本从命令行启动服务器和客户端：
 * <pre>
 *     ./run-example spdy-server
 * </pre>
 * 然后在不同的终端窗口中启动客户端：
 * <pre>
 *     ./run-example spdy-client
 * </pre>
 */
package org.top.java.netty.example.spdy.client;
