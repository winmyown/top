

package org.top.java.netty.example.http2.tiles;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import java.util.concurrent.TimeUnit;

/**
 * Handles the requests for the tiled image using HTTP 1.x as a protocol.
 */

/**
 * 使用HTTP 1.x协议处理平铺图像的请求。
 */
public final class Http1RequestHandler extends Http2RequestHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER));
        }
        super.channelRead0(ctx, request);
    }

    @Override
    protected void sendResponse(final ChannelHandlerContext ctx, String streamId, int latency,
            final FullHttpResponse response, final FullHttpRequest request) {
        HttpUtil.setContentLength(response, response.content().readableBytes());
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                if (isKeepAlive(request)) {
                    if (request.protocolVersion().equals(HTTP_1_0)) {
                        response.headers().set(CONNECTION, KEEP_ALIVE);
                    }
                    ctx.writeAndFlush(response);
                } else {
                    // Tell the client we're going to close the connection.
                    // 告诉客户端我们将关闭连接。
                    response.headers().set(CONNECTION, CLOSE);
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }, latency, TimeUnit.MILLISECONDS);
    }
}
