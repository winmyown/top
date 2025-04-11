
package org.top.java.netty.source.channel;


/**
 * The {@link CompleteChannelFuture} which is succeeded already.  It is
 * recommended to use {@link io.netty.channel.Channel#newSucceededFuture()} instead of
 * calling the constructor of this future.
 */

import org.top.java.netty.source.util.concurrent.EventExecutor;

/**
 * {@link CompleteChannelFuture} 已经成功完成。建议使用 {@link io.netty.channel.Channel#newSucceededFuture()} 而不是调用此 future 的构造函数。
 */
final class SucceededChannelFuture extends CompleteChannelFuture {

    /**
     * Creates a new instance.
     *
     * @param channel the {@link io.netty.channel.Channel} associated with this future
     */

    /**
     * 创建一个新的实例。
     *
     * @param channel 与此 future 关联的 {@link io.netty.channel.Channel}
     */
    SucceededChannelFuture(Channel channel, EventExecutor executor) {
        super(channel, executor);
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }
}
