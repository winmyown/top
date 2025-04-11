
package org.top.java.netty.source.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of {@link MultithreadEventExecutorGroup} which will use {@link DefaultEventExecutor} instances
 * to handle the tasks.
 */

/**
 * {@link MultithreadEventExecutorGroup} 的默认实现，它将使用 {@link DefaultEventExecutor} 实例来处理任务。
 */
public class DefaultEventExecutorGroup extends MultithreadEventExecutorGroup {
    /**
     * @see #DefaultEventExecutorGroup(int, ThreadFactory)
     */
    /**
     * @see #DefaultEventExecutorGroup(int, ThreadFactory)
     */
    public DefaultEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param threadFactory     the ThreadFactory to use, or {@code null} if the default should be used.
     */

    /**
     * 创建一个新实例。
     *
     * @param nThreads          此实例将使用的线程数。
     * @param threadFactory     要使用的ThreadFactory，如果使用默认值则为{@code null}。
     */
    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SingleThreadEventExecutor.DEFAULT_MAX_PENDING_EXECUTOR_TASKS,
                RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance.
     *
     * @param nThreads          the number of threads that will be used by this instance.
     * @param threadFactory     the ThreadFactory to use, or {@code null} if the default should be used.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     */

    /**
     * 创建新实例。
     *
     * @param nThreads          此实例将使用的线程数。
     * @param threadFactory     要使用的ThreadFactory，如果使用默认值则为{@code null}。
     * @param maxPendingTasks   新任务被拒绝之前的最大挂起任务数。
     * @param rejectedHandler   要使用的{@link RejectedExecutionHandler}。
     */
    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks,
                                     RejectedExecutionHandler rejectedHandler) {
        super(nThreads, threadFactory, maxPendingTasks, rejectedHandler);
    }

    @Override
    protected EventExecutor newChild(Executor executor, Object... args) throws Exception {
        return new DefaultEventExecutor(this, executor, (Integer) args[0], (RejectedExecutionHandler) args[1]);
    }
}
