package org.top.java.netty.example.ipfilter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilter;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Discards any incoming data from a blacklisteded IP address subnet and accepts the rest.
 */

/**
 * 丢弃来自黑名单IP地址子网的任何传入数据，并接受其余数据。
 */
public final class IpSubnetFilterExample {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);

        try {
            List<IpSubnetFilterRule> rules = new ArrayList<IpSubnetFilterRule>();

            // Reject 10.10.10.0/24 and 192.168.0.0/16 ranges but accept the rest

            // 拒绝 10.10.10.0/24 和 192.168.0.0/16 范围，但接受其余范围
            rules.add(new IpSubnetFilterRule("10.10.10.0", 24, IpFilterRuleType.REJECT));
            rules.add(new IpSubnetFilterRule("192.168.0.0", 16, IpFilterRuleType.REJECT));

            // Share this same Handler instance with multiple ChannelPipeline(s).

            // 将此相同的Handler实例与多个ChannelPipeline共享。
            final IpSubnetFilter ipFilter = new IpSubnetFilter(rules);

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addFirst(ipFilter);

                            p.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    System.out.println("Received data from: " + ctx.channel().remoteAddress());
                                }
                            });
                        }
                    });

            // Bind and start to accept incoming connections.

            // 绑定并开始接受传入连接。
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.

            // 等待服务器套接字关闭。
            // In this example, this does not happen, but you can do that to gracefully
            // 在这个例子中，这种情况不会发生，但你可以这样做以优雅地
            // shut down your server.
            // 关闭你的服务器。
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
