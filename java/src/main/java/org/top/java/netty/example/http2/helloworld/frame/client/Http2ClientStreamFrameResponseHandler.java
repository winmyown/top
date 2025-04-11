package org.top.java.netty.example.http2.helloworld.frame.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles HTTP/2 stream frame responses. This is a useful approach if you specifically want to check
 * the main HTTP/2 response DATA/HEADERs, but in this example it's used purely to see whether
 * our request (for a specific stream id) has had a final response (for that same stream id).
 */

/**
 * 处理HTTP/2流帧响应。如果您特别想检查主HTTP/2响应的DATA/HEADER，这是一种有用的方法，但在本例中，它仅用于查看我们的请求（针对特定流ID）是否已收到最终响应（针对同一流ID）。
 */
public final class Http2ClientStreamFrameResponseHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        System.out.println("Received HTTP/2 'stream' frame: " + msg);

        // isEndStream() is not from a common interface, so we currently must check both

        // isEndStream() 并不来自一个通用接口，因此目前我们必须同时检查两者
        if (msg instanceof Http2DataFrame && ((Http2DataFrame) msg).isEndStream()) {
            latch.countDown();
        } else if (msg instanceof Http2HeadersFrame && ((Http2HeadersFrame) msg).isEndStream()) {
            latch.countDown();
        }
    }

    /**
     * Waits for the latch to be decremented (i.e. for an end of stream message to be received), or for
     * the latch to expire after 5 seconds.
     * @return true if a successful HTTP/2 end of stream message was received.
     */

    /**
     * 等待闩锁被递减（即接收到流结束消息），或者在5秒后闩锁过期。
     * @return 如果成功接收到HTTP/2流结束消息，则返回true。
     */
    public boolean responseSuccessfullyCompleted() {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            System.err.println("Latch exception: " + ie.getMessage());
            return false;
        }
    }

}
