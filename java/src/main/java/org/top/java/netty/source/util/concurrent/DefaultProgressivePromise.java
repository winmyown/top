

package org.top.java.netty.source.util.concurrent;

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

public class DefaultProgressivePromise<V> extends DefaultPromise<V> implements ProgressivePromise<V> {

    /**
     * Creates a new instance.
     *
     * It is preferable to use {@link EventExecutor#newProgressivePromise()} to create a new progressive promise
     *
     * @param executor
     *        the {@link EventExecutor} which is used to notify the promise when it progresses or it is complete
     */

    /**
     * 创建一个新实例。
     *
     * 建议使用 {@link EventExecutor#newProgressivePromise()} 来创建一个新的渐进式 Promise
     *
     * @param executor
     *        用于在 Promise 进展或完成时通知的 {@link EventExecutor}
     */
    public DefaultProgressivePromise(EventExecutor executor) {
        super(executor);
    }

    protected DefaultProgressivePromise() { /* only for subclasses */ }

    @Override
    public ProgressivePromise<V> setProgress(long progress, long total) {
        if (total < 0) {
            // total unknown
            // 总数未知
            total = -1; // normalize
            checkPositiveOrZero(progress, "progress");
        } else if (progress < 0 || progress > total) {
            throw new IllegalArgumentException(
                    "progress: " + progress + " (expected: 0 <= progress <= total (" + total + "))");
        }

        if (isDone()) {
            throw new IllegalStateException("complete already");
        }

        notifyProgressiveListeners(progress, total);
        return this;
    }

    @Override
    public boolean tryProgress(long progress, long total) {
        if (total < 0) {
            total = -1;
            if (progress < 0 || isDone()) {
                return false;
            }
        } else if (progress < 0 || progress > total || isDone()) {
            return false;
        }

        notifyProgressiveListeners(progress, total);
        return true;
    }

    @Override
    public ProgressivePromise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ProgressivePromise<V> syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ProgressivePromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> setSuccess(V result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public ProgressivePromise<V> setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }
}
