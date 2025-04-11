
package org.top.java.netty.example.http2.helloworld.frame.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.CharsetUtil;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * A simple handler that responds with the message "Hello World!".
 *
 * <p>This example is making use of the "frame codec" http2 API. This API is very experimental and incomplete.
 */

/**
 * 一个简单的处理器，响应消息 "Hello World!"。
 *
 * <p>此示例使用了“frame codec” http2 API。此 API 非常实验性且不完整。
 */
@Sharable
public class HelloWorldHttp2Handler extends ChannelDuplexHandler {

    static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(
            copiedBuffer("Hello World", CharsetUtil.UTF_8)).asReadOnly();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */

    /**
     * 如果接收到带有结束流的帧，发送一个预定义的响应。
     */
    private static void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) throws Exception {
        Http2FrameStream stream = data.stream();

        if (data.isEndStream()) {
            sendResponse(ctx, stream, data.content());
        } else {
            // We do not send back the response to the remote-peer, so we need to release it.
            // 我们不将响应发送回远程对等方，因此需要释放它。
            data.release();
        }

        // Update the flowcontroller

        // 更新流控制器
        ctx.write(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(stream));
    }

    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */

    /**
     * 如果接收到带有结束流的帧，发送一个预定义的响应。
     */
    private static void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headers)
            throws Exception {
        if (headers.isEndStream()) {
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(RESPONSE_BYTES.duplicate());
            ByteBufUtil.writeAscii(content, " - via HTTP/2");
            sendResponse(ctx, headers.stream(), content);
        }
    }

    /**
     * Sends a "Hello World" DATA frame to the client.
     */

    /**
     * 向客户端发送“Hello World”数据帧。
     */
    private static void sendResponse(ChannelHandlerContext ctx, Http2FrameStream stream, ByteBuf payload) {
        // Send a frame for the response status
        // 发送响应状态的帧
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers).stream(stream));
        ctx.write(new DefaultHttp2DataFrame(payload, true).stream(stream));
    }
}
