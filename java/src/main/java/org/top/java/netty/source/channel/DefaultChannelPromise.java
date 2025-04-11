
package org.top.java.netty.source.channel;



import org.top.java.netty.source.util.concurrent.DefaultPromise;
import org.top.java.netty.source.util.concurrent.EventExecutor;
import org.top.java.netty.source.util.concurrent.Future;
import org.top.java.netty.source.util.concurrent.GenericFutureListener;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * The default {@link io.netty.channel.ChannelPromise} implementation.  It is recommended to use {@link io.netty.channel.Channel#newPromise()} to create
 * a new {@link io.netty.channel.ChannelPromise} rather than calling the constructor explicitly.
 */

/**
 * 默认的 {@link io.netty.channel.ChannelPromise} 实现。建议使用 {@link io.netty.channel.Channel#newPromise()} 来创建
 * 一个新的 {@link io.netty.channel.ChannelPromise}，而不是显式调用构造函数。
 */
public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise, ChannelFlushPromiseNotifier.FlushCheckpoint {

    private final Channel channel;
    private long checkpoint;

    /**
     * Creates a new instance.
     *
     * @param channel
     *        the {@link io.netty.channel.Channel} associated with this future
     */

    /**
     * 创建一个新的实例。
     *
     * @param channel
     *        与此 future 关联的 {@link io.netty.channel.Channel}
     */
    public DefaultChannelPromise(Channel channel) {
        this.channel = checkNotNull(channel, "channel");
    }

    /**
     * Creates a new instance.
     *
     * @param channel
     *        the {@link io.netty.channel.Channel} associated with this future
     */

    /**
     * 创建一个新的实例。
     *
     * @param channel
     *        与此 future 关联的 {@link io.netty.channel.Channel}
     */
    public DefaultChannelPromise(Channel channel, EventExecutor executor) {
        super(executor);
        this.channel = checkNotNull(channel, "channel");
    }

    @Override
    protected EventExecutor executor() {
        EventExecutor e = super.executor();
        if (e == null) {
            return channel().eventLoop();
        } else {
            return e;
        }
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelPromise setSuccess() {
        return setSuccess(null);
    }

    @Override
    public ChannelPromise setSuccess(Void result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public boolean trySuccess() {
        return trySuccess(null);
    }

    @Override
    public ChannelPromise setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }

    @Override
    public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public ChannelPromise sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ChannelPromise syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ChannelPromise await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ChannelPromise awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public long flushCheckpoint() {
        return checkpoint;
    }

    @Override
    public void flushCheckpoint(long checkpoint) {
        this.checkpoint = checkpoint;
    }

    @Override
    public ChannelPromise promise() {
        return this;
    }

    @Override
    protected void checkDeadLock() {
        if (channel().isRegistered()) {
            super.checkDeadLock();
        }
    }

    @Override
    public ChannelPromise unvoid() {
        return this;
    }

    @Override
    public boolean isVoid() {
        return false;
    }
}
