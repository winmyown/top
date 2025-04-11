
package org.top.java.netty.source.channel;


/**
 * Will handle all the I/O operations for a {@link io.netty.channel.Channel} once registered.
 *
 * One {@link EventLoop} instance will usually handle more than one {@link Channel} but this may depend on
 * implementation details and internals.
 *
 */

import org.top.java.netty.source.util.concurrent.OrderedEventExecutor;

/**
 * 一旦注册，将处理 {@link io.netty.channel.Channel} 的所有 I/O 操作。
 *
 * 一个 {@link EventLoop} 实例通常会处理多个 {@link Channel}，但这可能取决于实现细节和内部结构。
 *
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();
}
