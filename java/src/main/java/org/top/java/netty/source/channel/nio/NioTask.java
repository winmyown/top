
package org.top.java.netty.source.channel.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * An arbitrary task that can be executed by {@link NioEventLoop} when a {@link SelectableChannel} becomes ready.
 *
 * @see NioEventLoop#register(SelectableChannel, int, NioTask)
 */

/**
 * 一个可由 {@link NioEventLoop} 在 {@link SelectableChannel} 准备就绪时执行的任意任务。
 *
 * @see NioEventLoop#register(SelectableChannel, int, NioTask)
 */
public interface NioTask<C extends SelectableChannel> {
    /**
     * Invoked when the {@link SelectableChannel} has been selected by the {@link Selector}.
     */
    /**
     * 当{@link SelectableChannel}被{@link Selector}选中时调用。
     */
    void channelReady(C ch, SelectionKey key) throws Exception;

    /**
     * Invoked when the {@link SelectionKey} of the specified {@link SelectableChannel} has been cancelled and thus
     * this {@link NioTask} will not be notified anymore.
     *
     * @param cause the cause of the unregistration. {@code null} if a user called {@link SelectionKey#cancel()} or
     *              the event loop has been shut down.
     */

    /**
     * 当指定 {@link SelectableChannel} 的 {@link SelectionKey} 被取消时调用，因此
     * 此 {@link NioTask} 将不再收到通知。
     *
     * @param cause 取消注册的原因。如果用户调用了 {@link SelectionKey#cancel()} 或
     *              事件循环已关闭，则为 {@code null}。
     */
    void channelUnregistered(C ch, Throwable cause) throws Exception;
}
