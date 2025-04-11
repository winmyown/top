
package org.top.java.netty.source.channel;

import org.top.java.netty.source.util.concurrent.Future;
import org.top.java.netty.source.util.concurrent.GenericFutureListener;
import org.top.java.netty.source.util.concurrent.Promise;

/**
 * Special {@link io.netty.channel.ChannelFuture} which is writable.
 */

/**
 * 特殊的 {@link io.netty.channel.ChannelFuture}，可写。
 */
public interface ChannelPromise extends ChannelFuture, Promise<Void> {

    @Override
    Channel channel();

    @Override
    ChannelPromise setSuccess(Void result);

    ChannelPromise setSuccess();

    boolean trySuccess();

    @Override
    ChannelPromise setFailure(Throwable cause);

    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise sync() throws InterruptedException;

    @Override
    ChannelPromise syncUninterruptibly();

    @Override
    ChannelPromise await() throws InterruptedException;

    @Override
    ChannelPromise awaitUninterruptibly();

    /**
     * Returns a new {@link ChannelPromise} if {@link #isVoid()} returns {@code true} otherwise itself.
     */

    /**
     * 如果 {@link #isVoid()} 返回 {@code true}，则返回一个新的 {@link ChannelPromise}，否则返回自身。
     */
    ChannelPromise unvoid();
}
