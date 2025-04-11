
package org.top.java.netty.source.channel;

import io.netty.channel.*;

import java.net.SocketAddress;

/**
 * {@link io.netty.channel.ChannelHandler} which will get notified for IO-outbound-operations.
 */

/**
 * {@link io.netty.channel.ChannelHandler} 用于获取IO出站操作的通知。
 */
public interface ChannelOutboundHandler extends ChannelHandler {
    /**
     * Called once a bind operation is made.
     *
     * @param ctx           the {@link ChannelHandlerContext} for which the bind operation is made
     * @param localAddress  the {@link SocketAddress} to which it should bound
     * @param promise       the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception    thrown if an error occurs
     */
    /**
     * 在绑定操作完成后调用。
     *
     * @param ctx           进行绑定操作的 {@link ChannelHandlerContext}
     * @param localAddress  要绑定的 {@link SocketAddress}
     * @param promise       操作完成后通知的 {@link ChannelPromise}
     * @throws Exception    如果发生错误时抛出
     */
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;

    /**
     * Called once a connect operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the connect operation is made
     * @param remoteAddress     the {@link SocketAddress} to which it should connect
     * @param localAddress      the {@link SocketAddress} which is used as source on connect
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */

    /**
     * 在连接操作进行时调用。
     *
     * @param ctx               进行连接操作的 {@link ChannelHandlerContext}
     * @param remoteAddress     要连接的 {@link SocketAddress}
     * @param localAddress      作为连接源的 {@link SocketAddress}
     * @param promise           操作完成时通知的 {@link ChannelPromise}
     * @throws Exception        如果发生错误则抛出
     */
    void connect(
            ChannelHandlerContext ctx, SocketAddress remoteAddress,
            SocketAddress localAddress, ChannelPromise promise) throws Exception;

    /**
     * Called once a disconnect operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the disconnect operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */

    /**
     * 在断开连接操作执行时调用。
     *
     * @param ctx               断开连接操作对应的 {@link ChannelHandlerContext}
     * @param promise           操作完成后通知的 {@link ChannelPromise}
     * @throws Exception        如果发生错误时抛出
     */
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Called once a close operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */

    /**
     * 在关闭操作发生时调用。
     *
     * @param ctx               进行关闭操作的 {@link ChannelHandlerContext}
     * @param promise           操作完成后通知的 {@link ChannelPromise}
     * @throws Exception        如果发生错误则抛出
     */
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Called once a deregister operation is made from the current registered {@link EventLoop}.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */

    /**
     * 当从当前注册的 {@link EventLoop} 执行取消注册操作时调用。
     *
     * @param ctx               执行关闭操作的 {@link ChannelHandlerContext}
     * @param promise           操作完成后通知的 {@link ChannelPromise}
     * @throws Exception        如果发生错误时抛出
     */
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Intercepts {@link ChannelHandlerContext#read()}.
     */

    /**
     * 拦截 {@link ChannelHandlerContext#read()}。
     */
    void read(ChannelHandlerContext ctx) throws Exception;

    /**
    * Called once a write operation is made. The write operation will write the messages through the
     * {@link ChannelPipeline}. Those are then ready to be flushed to the actual {@link io.netty.channel.Channel} once
     * {@link Channel#flush()} is called
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the write operation is made
     * @param msg               the message to write
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */

    /**
    * 在写操作执行时调用。写操作将通过 {@link ChannelPipeline} 写入消息。这些消息在调用 {@link Channel#flush()} 时准备好被刷新到实际的 {@link io.netty.channel.Channel}。
    *
    * @param ctx               进行写操作的 {@link ChannelHandlerContext}
    * @param msg               要写入的消息
    * @param promise           操作完成后通知的 {@link ChannelPromise}
    * @throws Exception        如果发生错误时抛出
    */
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;

    /**
     * Called once a flush operation is made. The flush operation will try to flush out all previous written messages
     * that are pending.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the flush operation is made
     * @throws Exception        thrown if an error occurs
     */

    /**
     * 在刷新操作被调用时执行。刷新操作将尝试刷新所有之前写入的待处理消息。
     *
     * @param ctx               进行刷新操作的 {@link ChannelHandlerContext}
     * @throws Exception        如果发生错误时抛出
     */
    void flush(ChannelHandlerContext ctx) throws Exception;
}
