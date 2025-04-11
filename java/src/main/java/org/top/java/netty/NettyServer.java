package org.top.java.netty;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class NettyServer {
    private int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // 1. 创建两个EventLoopGroup：bossGroup和workerGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 接收连接
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 处理I/O

        try {
            // 2. 创建服务器引导程序ServerBootstrap
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 3. 配置ServerBootstrap
            serverBootstrap.group(bossGroup, workerGroup) // 设置EventLoopGroup
                    .channel(NioServerSocketChannel.class) // 指定Channel类型
                    .option(ChannelOption.SO_BACKLOG, 128) // 设置ServerChannel参数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置ChildChannel参数
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 设置ChildChannel的Pipeline
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 4. 配置ChannelPipeline
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加解码器（入站）
                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            // 添加业务处理器（入站）
                            pipeline.addLast(new ServerBusinessHandler());
                            // 添加编码器（出站）
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                        }
                    });

            // 5. 绑定端口并启动服务
            System.out.println("Netty服务器启动中...");
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            System.out.println("Netty服务器已启动，监听端口: " + port);

            // 6. 等待服务器Channel关闭
            channelFuture.channel().closeFuture().sync();

        } finally {
            // 7. 优雅关闭EventLoopGroup
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            System.out.println("Netty服务器已关闭");
        }
    }

    // 8. 自定义业务处理器
    private static class ServerBusinessHandler extends SimpleChannelInboundHandler<String> {
        // 9. 处理入站消息
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("服务器接收到消息: " + msg);

            // 10. 处理业务逻辑
            String response = "已收到消息: " + msg + "\n";

            // 11. 写入响应（触发出站操作）
            ctx.writeAndFlush(response);
        }

        // 12. 处理连接建立事件
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("客户端连接成功: " + ctx.channel().remoteAddress());
            ctx.writeAndFlush("欢迎连接到Netty服务器\n");
        }

        // 13. 处理连接断开事件
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("客户端断开连接: " + ctx.channel().remoteAddress());
        }

        // 14. 处理异常事件
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8888;
        new NettyServer(port).start();
    }
}