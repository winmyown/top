package org.top.java.netty.example.spdy.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.spdy.SpdyFrameCodec;
import io.netty.handler.codec.spdy.SpdyHttpDecoder;
import io.netty.handler.codec.spdy.SpdyHttpEncoder;
import io.netty.handler.codec.spdy.SpdySessionHandler;
import io.netty.handler.ssl.SslContext;

import static io.netty.handler.codec.spdy.SpdyVersion.*;
import static io.netty.util.internal.logging.InternalLogLevel.*;

public class SpdyClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_SPDY_CONTENT_LENGTH = 1024 * 1024; // 1 MB

    private final SslContext sslCtx;
    private final HttpResponseClientHandler httpResponseHandler;

    public SpdyClientInitializer(SslContext sslCtx, HttpResponseClientHandler httpResponseHandler) {
        this.sslCtx = sslCtx;
        this.httpResponseHandler = httpResponseHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        pipeline.addLast("spdyFrameCodec", new SpdyFrameCodec(SPDY_3_1));
        pipeline.addLast("spdyFrameLogger", new SpdyFrameLogger(INFO));
        pipeline.addLast("spdySessionHandler", new SpdySessionHandler(SPDY_3_1, false));
        pipeline.addLast("spdyHttpEncoder", new SpdyHttpEncoder(SPDY_3_1));
        pipeline.addLast("spdyHttpDecoder", new SpdyHttpDecoder(SPDY_3_1, MAX_SPDY_CONTENT_LENGTH));
        pipeline.addLast("spdyStreamIdHandler", new SpdyClientStreamIdHandler());
        pipeline.addLast("httpHandler", httpResponseHandler);
    }
}
