
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.ObjectUtil;

/**
 * <p>A promise combiner monitors the outcome of a number of discrete futures, then notifies a final, aggregate promise
 * when all of the combined futures are finished. The aggregate promise will succeed if and only if all of the combined
 * futures succeed. If any of the combined futures fail, the aggregate promise will fail. The cause failure for the
 * aggregate promise will be the failure for one of the failed combined futures; if more than one of the combined
 * futures fails, exactly which cause of failure will be assigned to the aggregate promise is undefined.</p>
 *
 * <p>Callers may populate a promise combiner with any number of futures to be combined via the
 * {@link PromiseCombiner#add(Future)} and {@link PromiseCombiner#addAll(Future[])} methods. When all futures to be
 * combined have been added, callers must provide an aggregate promise to be notified when all combined promises have
 * finished via the {@link PromiseCombiner#finish(Promise)} method.</p>
 *
 * <p>This implementation is <strong>NOT</strong> thread-safe and all methods must be called
 * from the {@link EventExecutor} thread.</p>
 */

/**
 * <p>一个承诺组合器监控多个离散未来的结果，然后当所有组合的未来完成时通知最终的聚合承诺。只有当所有组合的未来都成功时，聚合承诺才会成功。如果任何组合的未来失败，聚合承诺将失败。聚合承诺的失败原因将是其中一个失败组合未来的原因；如果多个组合的未来失败，具体哪个失败原因会被分配给聚合承诺是未定义的。</p>
 *
 * <p>调用者可以通过 {@link PromiseCombiner#add(Future)} 和 {@link PromiseCombiner#addAll(Future[])} 方法向承诺组合器中添加任意数量的未来。当所有要组合的未来都添加完毕后，调用者必须通过 {@link PromiseCombiner#finish(Promise)} 方法提供一个聚合承诺，以便在所有组合承诺完成时收到通知。</p>
 *
 * <p>此实现<strong>不是</strong>线程安全的，所有方法必须从 {@link EventExecutor} 线程调用。</p>
 */
public final class PromiseCombiner {
    private int expectedCount;
    private int doneCount;
    private Promise<Void> aggregatePromise;
    private Throwable cause;
    private final GenericFutureListener<Future<?>> listener = new GenericFutureListener<Future<?>>() {
        @Override
        public void operationComplete(final Future<?> future) {
            if (executor.inEventLoop()) {
                operationComplete0(future);
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        operationComplete0(future);
                    }
                });
            }
        }

        private void operationComplete0(Future<?> future) {
            assert executor.inEventLoop();
            ++doneCount;
            if (!future.isSuccess() && cause == null) {
                cause = future.cause();
            }
            if (doneCount == expectedCount && aggregatePromise != null) {
                tryPromise();
            }
        }
    };

    private final EventExecutor executor;

    /**
     * Deprecated use {@link PromiseCombiner#PromiseCombiner(EventExecutor)}.
     */

    /**
     * 已弃用，请使用 {@link PromiseCombiner#PromiseCombiner(EventExecutor)}。
     */
    @Deprecated
    public PromiseCombiner() {
        this(ImmediateEventExecutor.INSTANCE);
    }

    /**
     * The {@link EventExecutor} to use for notifications. You must call {@link #add(Future)}, {@link #addAll(Future[])}
     * and {@link #finish(Promise)} from within the {@link EventExecutor} thread.
     *
     * @param executor the {@link EventExecutor} to use for notifications.
     */

    /**
     * 用于通知的 {@link EventExecutor}。必须在 {@link EventExecutor} 线程中调用 {@link #add(Future)}、{@link #addAll(Future[])}
     * 和 {@link #finish(Promise)}。
     *
     * @param executor 用于通知的 {@link EventExecutor}。
     */
    public PromiseCombiner(EventExecutor executor) {
        this.executor = ObjectUtil.checkNotNull(executor, "executor");
    }

    /**
     * Adds a new promise to be combined. New promises may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param promise the promise to add to this promise combiner
     *
     * @deprecated Replaced by {@link PromiseCombiner#add(Future)}.
     */

    /**
     * 添加一个新的 promise 进行组合。在通过 {@link PromiseCombiner#finish(Promise)} 方法添加聚合 promise 之前，可以继续添加新的 promise。
     *
     * @param promise 要添加到此 promise 组合器中的 promise
     *
     * @deprecated 已替换为 {@link PromiseCombiner#add(Future)}。
     */
    @Deprecated
    public void add(Promise promise) {
        add((Future) promise);
    }

    /**
     * Adds a new future to be combined. New futures may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param future the future to add to this promise combiner
     */

    /**
     * 添加一个新的 future 进行组合。在通过 {@link PromiseCombiner#finish(Promise)} 方法添加聚合 promise 之前，可以继续添加新的 future。
     *
     * @param future 要添加到此 promise 组合器中的 future
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void add(Future future) {
        checkAddAllowed();
        checkInEventLoop();
        ++expectedCount;
        future.addListener(listener);
    }

    /**
     * Adds new promises to be combined. New promises may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param promises the promises to add to this promise combiner
     *
     * @deprecated Replaced by {@link PromiseCombiner#addAll(Future[])}
     */

    /**
     * 添加新的promise以进行组合。在通过{@link PromiseCombiner#finish(Promise)}方法添加聚合promise之前，可以继续添加新的promise。
     *
     * @param promises 要添加到此promise组合器中的promise
     *
     * @deprecated 已由{@link PromiseCombiner#addAll(Future[])}替代
     */
    @Deprecated
    public void addAll(Promise... promises) {
        addAll((Future[]) promises);
    }

    /**
     * Adds new futures to be combined. New futures may be added until an aggregate promise is added via the
     * {@link PromiseCombiner#finish(Promise)} method.
     *
     * @param futures the futures to add to this promise combiner
     */

    /**
     * 添加要组合的新Future。在通过 {@link PromiseCombiner#finish(Promise)} 方法添加聚合Promise之前，可以继续添加新的Future。
     *
     * @param futures 要添加到此Promise组合器中的Future
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addAll(Future... futures) {
        for (Future future : futures) {
            this.add(future);
        }
    }

    /**
     * <p>Sets the promise to be notified when all combined futures have finished. If all combined futures succeed,
     * then the aggregate promise will succeed. If one or more combined futures fails, then the aggregate promise will
     * fail with the cause of one of the failed futures. If more than one combined future fails, then exactly which
     * failure will be assigned to the aggregate promise is undefined.</p>
     *
     * <p>After this method is called, no more futures may be added via the {@link PromiseCombiner#add(Future)} or
     * {@link PromiseCombiner#addAll(Future[])} methods.</p>
     *
     * @param aggregatePromise the promise to notify when all combined futures have finished
     */

    /**
     * <p>设置当所有组合的 future 都完成时要通知的 promise。如果所有组合的 future 都成功，
     * 则聚合的 promise 将成功。如果一个或多个组合的 future 失败，则聚合的 promise 将失败，
     * 并且失败原因将是其中一个失败的 future 的原因。如果有多个组合的 future 失败，
     * 则具体哪个失败原因会被分配给聚合的 promise 是未定义的。</p>
     *
     * <p>调用此方法后，不能再通过 {@link PromiseCombiner#add(Future)} 或
     * {@link PromiseCombiner#addAll(Future[])} 方法添加更多的 future。</p>
     *
     * @param aggregatePromise 当所有组合的 future 都完成时要通知的 promise
     */
    public void finish(Promise<Void> aggregatePromise) {
        ObjectUtil.checkNotNull(aggregatePromise, "aggregatePromise");
        checkInEventLoop();
        if (this.aggregatePromise != null) {
            throw new IllegalStateException("Already finished");
        }
        this.aggregatePromise = aggregatePromise;
        if (doneCount == expectedCount) {
            tryPromise();
        }
    }

    private void checkInEventLoop() {
        if (!executor.inEventLoop()) {
            throw new IllegalStateException("Must be called from EventExecutor thread");
        }
    }

    private boolean tryPromise() {
        return (cause == null) ? aggregatePromise.trySuccess(null) : aggregatePromise.tryFailure(cause);
    }

    private void checkAddAllowed() {
        if (aggregatePromise != null) {
            throw new IllegalStateException("Adding promises is not allowed after finished adding");
        }
    }
}
