
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * {@link EventExecutor} implementation which makes no guarantees about the ordering of task execution that
 * are submitted because there may be multiple threads executing these tasks.
 * This implementation is most useful for protocols that do not need strict ordering.
 *
 * <strong>Because it provides no ordering care should be taken when using it!</strong>
 */

/**
 * {@link EventExecutor} 实现，该实现不保证提交的任务的执行顺序，因为可能有多个线程在执行这些任务。
 * 此实现对于不需要严格顺序的协议最为有用。
 *
 * <strong>由于它不提供顺序保证，使用时需格外小心！</strong>
 */
public final class UnorderedThreadPoolEventExecutor extends ScheduledThreadPoolExecutor implements EventExecutor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(
            UnorderedThreadPoolEventExecutor.class);

    private final Promise<?> terminationFuture = GlobalEventExecutor.INSTANCE.newPromise();
    private final Set<EventExecutor> executorSet = Collections.singleton((EventExecutor) this);

    /**
     * Calls {@link UnorderedThreadPoolEventExecutor#UnorderedThreadPoolEventExecutor(int, ThreadFactory)}
     * using {@link DefaultThreadFactory}.
     */

    /**
     * 调用 {@link UnorderedThreadPoolEventExecutor#UnorderedThreadPoolEventExecutor(int, ThreadFactory)}
     * 使用 {@link DefaultThreadFactory}。
     */
    public UnorderedThreadPoolEventExecutor(int corePoolSize) {
        this(corePoolSize, new DefaultThreadFactory(UnorderedThreadPoolEventExecutor.class));
    }

    /**
     * See {@link ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory)}
     */

    /**
     * 参见 {@link ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory)}
     */
    public UnorderedThreadPoolEventExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /**
     * Calls {@link UnorderedThreadPoolEventExecutor#UnorderedThreadPoolEventExecutor(int,
     * ThreadFactory, RejectedExecutionHandler)} using {@link DefaultThreadFactory}.
     */

    /**
     * 使用 {@link DefaultThreadFactory} 调用 {@link UnorderedThreadPoolEventExecutor#UnorderedThreadPoolEventExecutor(int,
     * ThreadFactory, RejectedExecutionHandler)}。
     */
    public UnorderedThreadPoolEventExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        this(corePoolSize, new DefaultThreadFactory(UnorderedThreadPoolEventExecutor.class), handler);
    }

    /**
     * See {@link ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory, RejectedExecutionHandler)}
     */

    /**
     * 参见 {@link ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory, RejectedExecutionHandler)}
     */
    public UnorderedThreadPoolEventExecutor(int corePoolSize, ThreadFactory threadFactory,
                                            RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    public EventExecutor next() {
        return this;
    }

    @Override
    public EventExecutorGroup parent() {
        return this;
    }

    @Override
    public boolean inEventLoop() {
        return false;
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return false;
    }

    @Override
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<V>(this);
    }

    @Override
    public <V> Future<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    @Override
    public <V> Future<V> newFailedFuture(Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }

    @Override
    public boolean isShuttingDown() {
        return isShutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks = super.shutdownNow();
        terminationFuture.trySuccess(null);
        return tasks;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        terminationFuture.trySuccess(null);
    }

    @Override
    public Future<?> shutdownGracefully() {
        return shutdownGracefully(2, 15, TimeUnit.SECONDS);
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        // TODO: At the moment this just calls shutdown but we may be able to do something more smart here which
        // TODO: 目前这只是调用了 shutdown，但我们可能可以在这里做一些更智能的操作
        //       respects the quietPeriod and timeout.
        //       尊重quietPeriod和timeout。
        shutdown();
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return executorSet.iterator();
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return runnable instanceof NonNotifyRunnable ?
                task : new RunnableScheduledFutureTask<V>(this, task, false);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return new RunnableScheduledFutureTask<V>(this, task, true);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return (ScheduledFuture<?>) super.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return (ScheduledFuture<V>) super.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return (ScheduledFuture<?>) super.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return (ScheduledFuture<?>) super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return (Future<?>) super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return (Future<T>) super.submit(task);
    }

    @Override
    public void execute(Runnable command) {
        super.schedule(new NonNotifyRunnable(command), 0, NANOSECONDS);
    }

    private static final class RunnableScheduledFutureTask<V> extends PromiseTask<V>
            implements RunnableScheduledFuture<V>, ScheduledFuture<V> {
        private final RunnableScheduledFuture<V> future;
        private final boolean wasCallable;

        RunnableScheduledFutureTask(EventExecutor executor, RunnableScheduledFuture<V> future, boolean wasCallable) {
            super(executor, future);
            this.future = future;
            this.wasCallable = wasCallable;
        }

        @Override
        V runTask() throws Throwable {
            V result =  super.runTask();
            if (result == null && wasCallable) {
                // If this RunnableScheduledFutureTask wraps a RunnableScheduledFuture that wraps a Callable we need
                // 如果这个 RunnableScheduledFutureTask 包装了一个 RunnableScheduledFuture，而该 RunnableScheduledFuture 又包装了一个 Callable，我们需要
                // to ensure that we return the correct result by calling future.get().
                // 确保通过调用 future.get() 返回正确的结果。
                //
                // See https://github.com/netty/netty/issues/11072
                // 参见 https://github.com/netty/netty/issues/11072
                assert future.isDone();
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    // unwrap exception.
                    // 解包异常。
                    throw e.getCause();
                }
            }
            return result;
        }

        @Override
        public void run() {
            if (!isPeriodic()) {
                super.run();
            } else if (!isDone()) {
                try {
                    // Its a periodic task so we need to ignore the return value
                    // 这是一个周期性任务，因此需要忽略返回值
                    runTask();
                } catch (Throwable cause) {
                    if (!tryFailureInternal(cause)) {
                        logger.warn("Failure during execution of task", cause);
                    }
                }
            }
        }

        @Override
        public boolean isPeriodic() {
            return future.isPeriodic();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return future.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return future.compareTo(o);
        }
    }

    // This is a special wrapper which we will be used in execute(...) to wrap the submitted Runnable. This is needed as

    // 这是一个特殊的包装器，我们将在 execute(...) 中使用它来包装提交的 Runnable。这是必需的，因为
    // ScheduledThreadPoolExecutor.execute(...) will delegate to submit(...) which will then use decorateTask(...).
    // ScheduledThreadPoolExecutor.execute(...) 将委托给 submit(...)，然后使用 decorateTask(...)。
    // The problem with this is that decorateTask(...) needs to ensure we only do our own decoration if we not call
    // 这里的问题是 decorateTask(...) 需要确保只有在我们不调用时才进行我们自己的装饰
    // from execute(...) as otherwise we may end up creating an endless loop because DefaultPromise will call
    // 来自 execute(...)，否则我们可能会陷入无限循环，因为 DefaultPromise 将会调用
    // EventExecutor.execute(...) when notify the listeners of the promise.
    // EventExecutor.execute(...) 当通知 promise 的监听器时。
    //
    // See https://github.com/netty/netty/issues/6507

// 此问题与 Netty 的 ByteBuf 处理相关，具体是在某些情况下，ByteBuf 的释放可能会导致内存泄漏。
// 问题描述：当使用 CompositeByteBuf 时，如果其中一个组件已经被释放，而 CompositeByteBuf 仍然持有对该组件的引用，可能会导致内存泄漏。
// 解决方案：在释放 CompositeByteBuf 之前，确保所有组件都已被正确释放，或者使用 ReferenceCountUtil.release() 来确保所有组件都被释放。
// 相关代码示例：
// CompositeByteBuf compositeBuf = Unpooled.compositeBuffer();
// ByteBuf componentBuf = Unpooled.buffer();
// compositeBuf.addComponent(true, componentBuf);
// componentBuf.release(); // 释放组件
// compositeBuf.release(); // 释放 CompositeByteBuf
// 此问题的修复已在 Netty 4.1.6 版本中合并。

    private static final class NonNotifyRunnable implements Runnable {

        private final Runnable task;

        NonNotifyRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }
    }
}
