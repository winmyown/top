
package org.top.java.netty.source.channel;


/**
 * Special {@link EventExecutorGroup} which allows registering {@link Channel}s that get
 * processed for later selection during the event loop.
 *
 */

import org.top.java.netty.source.util.concurrent.EventExecutorGroup;

/**
 * 特殊的 {@link EventExecutorGroup}，允许注册 {@link Channel}，这些 {@link Channel} 会在事件循环中被处理以进行后续的选择。
 *
 */
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * Return the next {@link EventLoop} to use
     */
    /**
     * 返回下一个要使用的 {@link EventLoop}
     */
    @Override
    EventLoop next();

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The returned {@link ChannelFuture}
     * will get notified once the registration was complete.
     */

    /**
     * 将一个 {@link Channel} 注册到该 {@link EventLoop} 中。返回的 {@link ChannelFuture}
     * 将在注册完成后收到通知。
     */
    ChannelFuture register(Channel channel);

    /**
     * Register a {@link Channel} with this {@link EventLoop} using a {@link ChannelFuture}. The passed
     * {@link ChannelFuture} will get notified once the registration was complete and also will get returned.
     */

    /**
     * 使用 {@link ChannelFuture} 将 {@link Channel} 注册到此 {@link EventLoop}。传递的
     * {@link ChannelFuture} 将在注册完成后收到通知，并且也会被返回。
     */
    ChannelFuture register(ChannelPromise promise);

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The passed {@link ChannelFuture}
     * will get notified once the registration was complete and also will get returned.
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */

    /**
     * 将 {@link Channel} 注册到此 {@link EventLoop}。传递的 {@link ChannelFuture}
     * 将在注册完成后收到通知，并且也会被返回。
     *
     * @deprecated 请使用 {@link #register(ChannelPromise)} 代替。
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
