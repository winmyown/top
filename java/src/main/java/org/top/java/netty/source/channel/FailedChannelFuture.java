
package org.top.java.netty.source.channel;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import org.top.java.netty.source.util.concurrent.EventExecutor;

/**
 * The {@link io.netty.channel.CompleteChannelFuture} which is failed already.  It is
 * recommended to use {@link io.netty.channel.Channel#newFailedFuture(Throwable)}
 * instead of calling the constructor of this future.
 */

/**
 * {@link io.netty.channel.CompleteChannelFuture}，它已经失败了。建议使用
 * {@link io.netty.channel.Channel#newFailedFuture(Throwable)} 而不是调用此 future 的构造函数。
 */
final class FailedChannelFuture extends CompleteChannelFuture {

    private final Throwable cause;

    /**
     * Creates a new instance.
     *
     * @param channel the {@link io.netty.channel.Channel} associated with this future
     * @param cause   the cause of failure
     */

    /**
     * 创建一个新实例。
     *
     * @param channel 与此 future 关联的 {@link io.netty.channel.Channel}
     * @param cause   失败的原因
     */
    FailedChannelFuture(Channel channel, EventExecutor executor, Throwable cause) {
        super(channel, executor);
        this.cause = ObjectUtil.checkNotNull(cause, "cause");
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public ChannelFuture sync() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        PlatformDependent.throwException(cause);
        return this;
    }
}
