package org.top.java.netty.example.spdy.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.spdy.SpdyFrameCodec;
import io.netty.handler.codec.spdy.SpdyHttpDecoder;
import io.netty.handler.codec.spdy.SpdyHttpEncoder;
import io.netty.handler.codec.spdy.SpdyHttpResponseStreamIdHandler;
import io.netty.handler.codec.spdy.SpdySessionHandler;
import io.netty.handler.codec.spdy.SpdyVersion;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * Negotiates with the browser if SPDY or HTTP is going to be used. Once decided, the Netty pipeline is setup with
 * the correct handlers for the selected protocol.
 */

/**
 * 与浏览器协商是否使用SPDY或HTTP。一旦决定，Netty管道将使用所选协议的正确处理程序进行设置。
 */
public class SpdyOrHttpHandler extends ApplicationProtocolNegotiationHandler {

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;

    protected SpdyOrHttpHandler() {
        super(ApplicationProtocolNames.HTTP_1_1);
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        if (ApplicationProtocolNames.SPDY_3_1.equals(protocol)) {
            configureSpdy(ctx, SpdyVersion.SPDY_3_1);
            return;
        }

        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            configureHttp1(ctx);
            return;
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }

    private static void configureSpdy(ChannelHandlerContext ctx, SpdyVersion version) throws Exception {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(new SpdyFrameCodec(version));
        p.addLast(new SpdySessionHandler(version, true));
        p.addLast(new SpdyHttpEncoder(version));
        p.addLast(new SpdyHttpDecoder(version, MAX_CONTENT_LENGTH));
        p.addLast(new SpdyHttpResponseStreamIdHandler());
        p.addLast(new SpdyServerHandler());
    }

    private static void configureHttp1(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        p.addLast(new SpdyServerHandler());
    }
}
