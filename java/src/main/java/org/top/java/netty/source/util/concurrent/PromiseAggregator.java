

package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.ObjectUtil;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @deprecated Use {@link PromiseCombiner#PromiseCombiner(EventExecutor)}.
 *
 * {@link GenericFutureListener} implementation which consolidates multiple {@link Future}s
 * into one, by listening to individual {@link Future}s and producing an aggregated result
 * (success/failure) when all {@link Future}s have completed.
 *
 * @param <V> the type of value returned by the {@link Future}
 * @param <F> the type of {@link Future}
 */

/**
 * @deprecated 使用 {@link PromiseCombiner#PromiseCombiner(EventExecutor)}。
 *
 * {@link GenericFutureListener} 的实现，通过监听多个 {@link Future} 并将它们合并为一个，
 * 当所有 {@link Future} 完成时生成一个聚合结果（成功/失败）。
 *
 * @param <V> {@link Future} 返回值的类型
 * @param <F> {@link Future} 的类型
 */
@Deprecated
public class PromiseAggregator<V, F extends Future<V>> implements GenericFutureListener<F> {

    private final Promise<?> aggregatePromise;
    private final boolean failPending;
    private Set<Promise<V>> pendingPromises;

    /**
     * Creates a new instance.
     *
     * @param aggregatePromise  the {@link Promise} to notify
     * @param failPending  {@code true} to fail pending promises, false to leave them unaffected
     */

    /**
     * 创建新实例。
     *
     * @param aggregatePromise  要通知的 {@link Promise}
     * @param failPending  {@code true} 表示使待处理的 promises 失败，false 表示不影响它们
     */
    public PromiseAggregator(Promise<Void> aggregatePromise, boolean failPending) {
        this.aggregatePromise = ObjectUtil.checkNotNull(aggregatePromise, "aggregatePromise");
        this.failPending = failPending;
    }

    /**
     * See {@link PromiseAggregator#PromiseAggregator(Promise, boolean)}.
     * Defaults {@code failPending} to true.
     */

    /**
     * 参见 {@link PromiseAggregator#PromiseAggregator(Promise, boolean)}.
     * 默认将 {@code failPending} 设置为 true。
     */
    public PromiseAggregator(Promise<Void> aggregatePromise) {
        this(aggregatePromise, true);
    }

    /**
     * Add the given {@link Promise}s to the aggregator.
     */

    /**
     * 将给定的 {@link Promise} 添加到聚合器中。
     */
    @SafeVarargs
    public final PromiseAggregator<V, F> add(Promise<V>... promises) {
        ObjectUtil.checkNotNull(promises, "promises");
        if (promises.length == 0) {
            return this;
        }
        synchronized (this) {
            if (pendingPromises == null) {
                int size;
                if (promises.length > 1) {
                    size = promises.length;
                } else {
                    size = 2;
                }
                pendingPromises = new LinkedHashSet<Promise<V>>(size);
            }
            for (Promise<V> p : promises) {
                if (p == null) {
                    continue;
                }
                pendingPromises.add(p);
                p.addListener(this);
            }
        }
        return this;
    }

    @Override
    public synchronized void operationComplete(F future) throws Exception {
        if (pendingPromises == null) {
            aggregatePromise.setSuccess(null);
        } else {
            pendingPromises.remove(future);
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                aggregatePromise.setFailure(cause);
                if (failPending) {
                    for (Promise<V> pendingFuture : pendingPromises) {
                        pendingFuture.setFailure(cause);
                    }
                }
            } else {
                if (pendingPromises.isEmpty()) {
                    aggregatePromise.setSuccess(null);
                }
            }
        }
    }

}
