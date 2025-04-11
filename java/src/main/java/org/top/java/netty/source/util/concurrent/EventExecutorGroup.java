
package org.top.java.netty.source.util.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link EventExecutorGroup} is responsible for providing the {@link EventExecutor}'s to use
 * via its {@link #next()} method. Besides this, it is also responsible for handling their
 * life-cycle and allows shutting them down in a global fashion.
 *
 */

/**
 * {@link EventExecutorGroup} 负责通过其 {@link #next()} 方法提供要使用的 {@link EventExecutor}。
 * 除此之外，它还负责处理它们的生命周期，并允许以全局方式关闭它们。
 *
 */
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor> {

    /**
     * Returns {@code true} if and only if all {@link EventExecutor}s managed by this {@link EventExecutorGroup}
     * are being {@linkplain #shutdownGracefully() shut down gracefully} or was {@linkplain #isShutdown() shut down}.
     */

    /**
     * 当且仅当由此 {@link EventExecutorGroup} 管理的所有 {@link EventExecutor} 正在被 {@linkplain #shutdownGracefully() 优雅关闭} 或已经 {@linkplain #isShutdown() 关闭} 时，返回 {@code true}。
     */
    boolean isShuttingDown();

    /**
     * Shortcut method for {@link #shutdownGracefully(long, long, TimeUnit)} with sensible default values.
     *
     * @return the {@link #terminationFuture()}
     */

    /**
     * 为 {@link #shutdownGracefully(long, long, TimeUnit)} 提供默认值的快捷方法。
     *
     * @return {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully();

    /**
     * Signals this executor that the caller wants the executor to be shut down.  Once this method is called,
     * {@link #isShuttingDown()} starts to return {@code true}, and the executor prepares to shut itself down.
     * Unlike {@link #shutdown()}, graceful shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
     * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
     * it is guaranteed to be accepted and the quiet period will start over.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
     *                    regardless if a task was submitted during the quiet period
     * @param unit        the unit of {@code quietPeriod} and {@code timeout}
     *
     * @return the {@link #terminationFuture()}
     */

    /**
     * 向此执行器发出信号，表示调用者希望执行器关闭。一旦调用此方法，
     * {@link #isShuttingDown()} 开始返回 {@code true}，并且执行器准备关闭自身。
     * 与 {@link #shutdown()} 不同，优雅关闭确保在关闭之前 <i>'静默期'</i>
     * （通常为几秒钟）内不会提交任何任务。如果在静默期内提交了任务，
     * 它将被保证接受，并且静默期将重新开始。
     *
     * @param quietPeriod 静默期，如文档所述
     * @param timeout     无论是否在静默期内提交了任务，等待执行器 {@linkplain #shutdown()} 的最大时间
     * @param unit        {@code quietPeriod} 和 {@code timeout} 的单位
     *
     * @return {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);

    /**
     * Returns the {@link Future} which is notified when all {@link EventExecutor}s managed by this
     * {@link EventExecutorGroup} have been terminated.
     */

    /**
     * 返回当此 {@link EventExecutorGroup} 管理的所有 {@link EventExecutor} 都已终止时被通知的 {@link Future}。
     */
    Future<?> terminationFuture();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */

    /**
     * @deprecated 请使用 {@link #shutdownGracefully(long, long, TimeUnit)} 或 {@link #shutdownGracefully()} 代替。
     */
    @Override
    @Deprecated
    void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */

    /**
     * @deprecated 请使用 {@link #shutdownGracefully(long, long, TimeUnit)} 或 {@link #shutdownGracefully()} 代替。
     */
    @Override
    @Deprecated
    List<Runnable> shutdownNow();

    /**
     * Returns one of the {@link EventExecutor}s managed by this {@link EventExecutorGroup}.
     */

    /**
     * 返回由此 {@link EventExecutorGroup} 管理的其中一个 {@link EventExecutor}。
     */
    EventExecutor next();

    @Override
    Iterator<EventExecutor> iterator();

    @Override
    Future<?> submit(Runnable task);

    @Override
    <T> Future<T> submit(Runnable task, T result);

    @Override
    <T> Future<T> submit(Callable<T> task);

    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
