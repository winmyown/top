
package org.top.java.netty.source.channel;

import io.netty.channel.*;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.UnstableApi;
import org.top.java.netty.source.util.concurrent.AbstractFuture;
import org.top.java.netty.source.util.concurrent.Future;
import org.top.java.netty.source.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;

@UnstableApi
public final class VoidChannelPromise extends AbstractFuture<Void> implements ChannelPromise {

    private final Channel channel;
    // Will be null if we should not propagate exceptions through the pipeline on failure case.
    // 如果不应在失败情况下通过管道传播异常，则为 null。
    private final ChannelFutureListener fireExceptionListener;

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
    public VoidChannelPromise(final Channel channel, boolean fireException) {
        ObjectUtil.checkNotNull(channel, "channel");
        this.channel = channel;
        if (fireException) {
            fireExceptionListener = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        fireException0(cause);
                    }
                }
            };
        } else {
            fireExceptionListener = null;
        }
    }

    @Override
    public VoidChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        fail();
        return this;
    }

    @Override
    public VoidChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        fail();
        return this;
    }

    @Override
    public VoidChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        // NOOP
        // NOOP
        return this;
    }

    @Override
    public VoidChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        // NOOP
        // NOOP
        return this;
    }

    @Override
    public VoidChannelPromise await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        fail();
        return false;
    }

    @Override
    public boolean await(long timeoutMillis) {
        fail();
        return false;
    }

    @Override
    public VoidChannelPromise awaitUninterruptibly() {
        fail();
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        fail();
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        fail();
        return false;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean setUncancellable() {
        return true;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public VoidChannelPromise sync() {
        fail();
        return this;
    }

    @Override
    public VoidChannelPromise syncUninterruptibly() {
        fail();
        return this;
    }

    @Override
    public VoidChannelPromise setFailure(Throwable cause) {
        fireException0(cause);
        return this;
    }

    @Override
    public VoidChannelPromise setSuccess() {
        return this;
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        fireException0(cause);
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning this value has no effect in this implementation.
     */

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning 该值在此实现中无效。
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean trySuccess() {
        return false;
    }

    private static void fail() {
        throw new IllegalStateException("void future");
    }

    @Override
    public VoidChannelPromise setSuccess(Void result) {
        return this;
    }

    @Override
    public boolean trySuccess(Void result) {
        return false;
    }

    @Override
    public Void getNow() {
        return null;
    }

    @Override
    public ChannelPromise unvoid() {
        ChannelPromise promise = new DefaultChannelPromise(channel);
        if (fireExceptionListener != null) {
            promise.addListener(fireExceptionListener);
        }
        return promise;
    }

    @Override
    public boolean isVoid() {
        return true;
    }

    private void fireException0(Throwable cause) {
        // Only fire the exception if the channel is open and registered
        // 仅在通道打开并注册时抛出异常
        // if not the pipeline is not setup and so it would hit the tail
        // 如果管道未设置，则会命中尾部
        // of the pipeline.
        // 管道的。
        // See https://github.com/netty/netty/issues/1517
        // 参见 https://github.com/netty/netty/issues/1517
        if (fireExceptionListener != null && channel.isRegistered()) {
            channel.pipeline().fireExceptionCaught(cause);
        }
    }
}
