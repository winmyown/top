
/**
 * This package contains an example SPDY HTTP web server.
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
 * You may also use the {@code run-example.sh} script to start the server from the command line:
 * <pre>
 *     ./run-example spdy-server
 * </pre>
 * <p>
 * Once started, you can test the server with your
 * <a href="https://en.wikipedia.org/wiki/SPDY#Browser_support_and_usage">SPDY enabled web browser</a> by navigating
 * to <a href="https://localhost:8443/">https://localhost:8443/</a>
 */

/**
 * 该包包含一个SPDY HTTP Web服务器的示例。
 * <p>
 * 该包依赖于Jetty项目对OpenJDK 7的传输层安全（TLS）扩展的Next Protocol Negotiation（NPN）实现。NPN允许应用层协商使用哪种协议，SPDY或HTTP。
 * <p>
 * 要启动，请使用JVM参数运行 {@link io.netty.example.spdy.server.SpdyServer}：
 * {@code java -Xbootclasspath/p:<path_to_npn_boot_jar> ...}。
 * "path_to_npn_boot_jar"是NPN Boot Jar文件在文件系统中的路径，该文件可以从Maven下载，坐标为org.mortbay.jetty.npn:npn-boot。不同版本适用于不同的OpenJDK版本。
 * 有关更多信息，请参见<a href="https://www.eclipse.org/jetty/documentation/current/npn-chapter.html">Jetty文档</a>。
 * <p>
 * 你也可以使用 {@code run-example.sh} 脚本从命令行启动服务器：
 * <pre>
 *     ./run-example spdy-server
 * </pre>
 * <p>
 * 启动后，你可以使用<a href="https://en.wikipedia.org/wiki/SPDY#Browser_support_and_usage">支持SPDY的Web浏览器</a>导航到<a href="https://localhost:8443/">https://localhost:8443/</a>来测试服务器。
 */
package org.top.java.netty.example.spdy.server;
