
package org.top.java.netty.example.sctp.multihoming;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.SctpChannel;
import io.netty.channel.sctp.SctpChannelOption;
import io.netty.channel.sctp.nio.NioSctpChannel;
import org.top.java.netty.example.sctp.SctpEchoClientHandler;
import io.netty.util.internal.SocketUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * SCTP Echo Client with multi-homing support.
 */

/**
 * 支持多宿主的SCTP回显客户端。
 */
public final class SctpMultiHomingEchoClient {

    private static final String CLIENT_PRIMARY_HOST = System.getProperty("host.primary", "127.0.0.1");
    private static final String CLIENT_SECONDARY_HOST = System.getProperty("host.secondary", "127.0.0.2");

    private static final int CLIENT_PORT = Integer.parseInt(System.getProperty("port.local", "8008"));

    private static final String SERVER_REMOTE_HOST = System.getProperty("host.remote", "127.0.0.1");
    private static final int SERVER_REMOTE_PORT = Integer.parseInt(System.getProperty("port.remote", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure the client.
        // 配置客户端。
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSctpChannel.class)
             .option(SctpChannelOption.SCTP_NODELAY, true)
             .handler(new ChannelInitializer<SctpChannel>() {
                 @Override
                 public void initChannel(SctpChannel ch) throws Exception {
                     ch.pipeline().addLast(
//                             new LoggingHandler(LogLevel.INFO),
//                             new LoggingHandler(LogLevel.INFO),
                             new SctpEchoClientHandler());
                 }
             });

            InetSocketAddress localAddress = SocketUtils.socketAddress(CLIENT_PRIMARY_HOST, CLIENT_PORT);
            InetAddress localSecondaryAddress = SocketUtils.addressByName(CLIENT_SECONDARY_HOST);

            InetSocketAddress remoteAddress = SocketUtils.socketAddress(SERVER_REMOTE_HOST, SERVER_REMOTE_PORT);

            // Bind the client channel.

            // 绑定客户端通道。
            ChannelFuture bindFuture = b.bind(localAddress).sync();

            // Get the underlying sctp channel

            // 获取底层的sctp通道
            SctpChannel channel = (SctpChannel) bindFuture.channel();

            // Bind the secondary address.

            // 绑定次要地址。
            // Please note that, bindAddress in the client channel should be done before connecting if you have not
            // 请注意，如果您尚未连接，则应在连接之前在客户端通道中完成 bindAddress
            // enable Dynamic Address Configuration. See net.sctp.addip_enable kernel param
            // 启用动态地址配置。参见 net.sctp.addip_enable 内核参数
            channel.bindAddress(localSecondaryAddress).sync();

            // Finish connect

            // 完成连接
            ChannelFuture connectFuture = channel.connect(remoteAddress).sync();

            // Wait until the connection is closed.

            // 等待连接关闭。
            connectFuture.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            // 关闭事件循环以终止所有线程。
            group.shutdownGracefully();
        }
    }
}
