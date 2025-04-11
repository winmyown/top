
package org.top.java.netty.source.util.concurrent;

/**
 * The {@link CompleteFuture} which is succeeded already.  It is
 * recommended to use {@link EventExecutor#newSucceededFuture(Object)} instead of
 * calling the constructor of this future.
 */

/**
 * 已经成功的{@link CompleteFuture}。建议使用{@link EventExecutor#newSucceededFuture(Object)}而不是调用此future的构造函数。
 */
public final class SucceededFuture<V> extends CompleteFuture<V> {
    private final V result;

    /**
     * Creates a new instance.
     *
     * @param executor the {@link EventExecutor} associated with this future
     */

    /**
     * 创建新实例。
     *
     * @param executor 与此 future 关联的 {@link EventExecutor}
     */
    public SucceededFuture(EventExecutor executor, V result) {
        super(executor);
        this.result = result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public V getNow() {
        return result;
    }
}
