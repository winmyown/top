
package org.top.java.netty.source.channel;

import io.netty.channel.*;

/**
 * {@link io.netty.channel.ChannelHandler} which adds callbacks for state changes. This allows the user
 * to hook in to state changes easily.
 */

/**
 * {@link io.netty.channel.ChannelHandler} 用于添加状态变化的回调。这使得用户可以轻松地钩入状态变化。
 */
public interface ChannelInboundHandler extends ChannelHandler {

    /**
     * The {@link io.netty.channel.Channel} of the {@link ChannelHandlerContext} was registered with its {@link EventLoop}
     */

    /**
     * {@link io.netty.channel.Channel} 的 {@link ChannelHandlerContext} 已注册到其 {@link EventLoop}
     */
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link io.netty.channel.Channel} of the {@link ChannelHandlerContext} was unregistered from its {@link EventLoop}
     */

    /**
     * {@link io.netty.channel.Channel} 的 {@link ChannelHandlerContext} 已从其 {@link EventLoop} 中注销
     */
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link io.netty.channel.Channel} of the {@link ChannelHandlerContext} is now active
     */

    /**
     * {@link io.netty.channel.Channel} 的 {@link ChannelHandlerContext} 现在处于活动状态
     */
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link io.netty.channel.Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its
     * end of lifetime.
     */

    /**
     * {@link io.netty.channel.Channel} 的 {@link ChannelHandlerContext} 已注册的通道现在处于非活动状态，并已达到其生命周期的终点。
     */
    void channelInactive(ChannelHandlerContext ctx) throws Exception;

    /**
     * Invoked when the current {@link io.netty.channel.Channel} has read a message from the peer.
     */

    /**
     * 当当前 {@link io.netty.channel.Channel} 从对等端读取到消息时调用。
     */
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * Invoked when the last message read by the current read operation has been consumed by
     * {@link #channelRead(ChannelHandlerContext, Object)}.  If {@link ChannelOption#AUTO_READ} is off, no further
     * attempt to read an inbound data from the current {@link io.netty.channel.Channel} will be made until
     * {@link ChannelHandlerContext#read()} is called.
     */

    /**
     * 当当前读取操作读取的最后一条消息已被 {@link #channelRead(ChannelHandlerContext, Object)} 消费时调用。
     * 如果 {@link ChannelOption#AUTO_READ} 关闭，则在调用 {@link ChannelHandlerContext#read()} 之前，
     * 不会尝试从当前 {@link io.netty.channel.Channel} 读取更多入站数据。
     */
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if an user event was triggered.
     */

    /**
     * 在用户事件触发时调用。
     */
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;

    /**
     * Gets called once the writable state of a {@link io.netty.channel.Channel} changed. You can check the state with
     * {@link Channel#isWritable()}.
     */

    /**
     * 当 {@link io.netty.channel.Channel} 的可写状态发生变化时调用。你可以通过
     * {@link Channel#isWritable()} 来检查状态。
     */
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     */

    /**
     * 当抛出 {@link Throwable} 时调用。
     */
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
