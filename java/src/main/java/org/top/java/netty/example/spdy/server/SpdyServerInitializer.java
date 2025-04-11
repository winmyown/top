package org.top.java.netty.example.spdy.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

/**
 * Sets up the Netty pipeline
 */

/**
 * 设置Netty的管道
 */
public class SpdyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public SpdyServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(sslCtx.newHandler(ch.alloc()));
        // Negotiates with the browser if SPDY or HTTP is going to be used
        // 与浏览器协商是否使用SPDY或HTTP
        p.addLast(new SpdyOrHttpHandler());
    }
}
