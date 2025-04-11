package org.top.java.netty.example.http2.helloworld.frame.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;

/**
 * Configures client pipeline to support HTTP/2 frames via {@link Http2FrameCodec} and {@link Http2MultiplexHandler}.
 */

/**
 * 配置客户端管道以通过 {@link Http2FrameCodec} 和 {@link Http2MultiplexHandler} 支持 HTTP/2 帧。
 */
public final class Http2ClientFrameInitializer extends ChannelInitializer<Channel> {

    private final SslContext sslCtx;

    public Http2ClientFrameInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // ensure that our 'trust all' SSL handler is the first in the pipeline if SSL is enabled.
        // 确保如果启用了 SSL，我们的 'trust all' SSL 处理程序是管道中的第一个。
        if (sslCtx != null) {
            ch.pipeline().addFirst(sslCtx.newHandler(ch.alloc()));
        }

        final Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forClient()
            .initialSettings(Http2Settings.defaultSettings()) // this is the default, but shows it can be changed.
            .build();
        ch.pipeline().addLast(http2FrameCodec);
        ch.pipeline().addLast(new Http2MultiplexHandler(new SimpleChannelInboundHandler() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                // NOOP (this is the handler for 'inbound' streams, which is not relevant in this example)
                // NOOP（这是用于“inbound”流的处理程序，在此示例中不相关）
            }
        }));
    }

}
