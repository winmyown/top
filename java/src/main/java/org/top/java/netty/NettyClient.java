package org.top.java.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.util.Scanner;

public class NettyClient {
    private String host;
    private int port;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        // 1. 创建EventLoopGroup
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 2. 创建客户端引导程序Bootstrap
            Bootstrap bootstrap = new Bootstrap();

            // 3. 配置Bootstrap
            bootstrap.group(group) // 设置EventLoopGroup
                    .channel(NioSocketChannel.class) // 指定Channel类型
                    .option(ChannelOption.TCP_NODELAY, true) // 设置Channel参数
                    .handler(new ChannelInitializer<SocketChannel>() { // 设置Channel的Pipeline
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 4. 配置ChannelPipeline
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加解码器（入站）
                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            // 添加业务处理器（入站）
                            pipeline.addLast(new ClientBusinessHandler());
                            // 添加编码器（出站）
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                        }
                    });

            // 5. 连接服务器
            System.out.println("Netty客户端启动中...");
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            System.out.println("Netty客户端已启动，连接到: " + host + ":" + port);

            Channel channel = channelFuture.channel();

            // 6. 从控制台读取数据发送到服务器
            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入消息发送到服务器（输入'exit'退出）:");

            while (true) {
                String line = scanner.nextLine();
                if ("exit".equals(line)) {
                    break;
                }

                // 7. 发送消息到服务器（带换行符以触发LineBasedFrameDecoder）
                channel.writeAndFlush(line + "\n");
            }

            // 8. 等待连接关闭
            channel.closeFuture().sync();

        } finally {
            // 9. 优雅关闭EventLoopGroup
            group.shutdownGracefully();
            System.out.println("Netty客户端已关闭");
        }
    }

    // 10. 自定义业务处理器
    private static class ClientBusinessHandler extends SimpleChannelInboundHandler<String> {
        // 11. 处理入站消息
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("收到服务器消息: " + msg);
        }

        // 12. 处理连接建立事件
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("连接到服务器成功");
        }

        // 13. 处理连接断开事件
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("与服务器连接断开");
        }

        // 14. 处理异常事件
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8888;
        new NettyClient(host, port).start();
    }
}
