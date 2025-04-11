package org.top.java.netty.example.stomp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;


/**
 * very simple stomp client implementation example, requires running stomp server to actually work
 * uses default username/password and destination values from hornetq message broker
 */


/**
 * 非常简单的stomp客户端实现示例，需要运行stomp服务器才能实际工作
 * 使用来自hornetq消息代理的默认用户名/密码和目标值
 */
public final class StompClient {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "61613"));
    static final String LOGIN = System.getProperty("login", "guest");
    static final String PASSCODE = System.getProperty("passcode", "guest");
    static final String TOPIC = System.getProperty("topic", "jms.topic.exampleTopic");

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("decoder", new StompSubframeDecoder());
                    pipeline.addLast("encoder", new StompSubframeEncoder());
                    pipeline.addLast("aggregator", new StompSubframeAggregator(1048576));
                    pipeline.addLast("handler", new StompClientHandler());
                }
            });

            b.connect(HOST, PORT).sync().channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
