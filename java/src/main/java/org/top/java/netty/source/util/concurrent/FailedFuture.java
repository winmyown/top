
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.PlatformDependent;

/**
 * The {@link CompleteFuture} which is failed already.  It is
 * recommended to use {@link EventExecutor#newFailedFuture(Throwable)}
 * instead of calling the constructor of this future.
 */

/**
 * 已经失败的 {@link CompleteFuture}。建议使用
 * {@link EventExecutor#newFailedFuture(Throwable)} 而不是直接调用此 future 的构造函数。
 */
public final class FailedFuture<V> extends CompleteFuture<V> {

    private final Throwable cause;

    /**
     * Creates a new instance.
     *
     * @param executor the {@link EventExecutor} associated with this future
     * @param cause   the cause of failure
     */

    /**
     * 创建新实例。
     *
     * @param executor 与此 future 关联的 {@link EventExecutor}
     * @param cause    失败的原因
     */
    public FailedFuture(EventExecutor executor, Throwable cause) {
        super(executor);
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
    public Future<V> sync() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public Future<V> syncUninterruptibly() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public V getNow() {
        return null;
    }
}
