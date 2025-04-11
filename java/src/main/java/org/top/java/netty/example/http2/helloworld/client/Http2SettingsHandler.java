package org.top.java.netty.example.http2.helloworld.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.concurrent.TimeUnit;

/**
 * Reads the first {@link Http2Settings} object and notifies a {@link io.netty.channel.ChannelPromise}
 */

/**
 * 读取第一个 {@link Http2Settings} 对象并通知 {@link io.netty.channel.ChannelPromise}
 */
public class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {
    private final ChannelPromise promise;

    /**
     * Create new instance
     *
     * @param promise Promise object used to notify when first settings are received
     */

    /**
     * 创建新实例
     *
     * @param promise 用于在收到第一个设置时通知的 Promise 对象
     */
    public Http2SettingsHandler(ChannelPromise promise) {
        this.promise = promise;
    }

    /**
     * Wait for this handler to be added after the upgrade to HTTP/2, and for initial preface
     * handshake to complete.
     *
     * @param timeout Time to wait
     * @param unit {@link java.util.concurrent.TimeUnit} for {@code timeout}
     * @throws Exception if timeout or other failure occurs
     */

    /**
     * 等待此处理程序在升级到HTTP/2后被添加，并等待初始前言握手完成。
     *
     * @param timeout 等待时间
     * @param unit {@link java.util.concurrent.TimeUnit} 用于 {@code timeout}
     * @throws Exception 如果超时或其他失败发生
     */
    public void awaitSettings(long timeout, TimeUnit unit) throws Exception {
        if (!promise.awaitUninterruptibly(timeout, unit)) {
            throw new IllegalStateException("Timed out waiting for settings");
        }
        if (!promise.isSuccess()) {
            throw new RuntimeException(promise.cause());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Settings msg) throws Exception {
        promise.setSuccess();

        // Only care about the first settings message

        // 仅关注第一条设置消息
        ctx.pipeline().remove(this);
    }
}
