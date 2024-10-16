package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:39
 */


import org.top.java.source.concurrent.atomic.AtomicLong;
import org.top.java.source.concurrent.locks.ReentrantLock;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Delayed;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * {@link ThreadPoolExecutor} 的一个扩展，支持在给定的延迟之后执行任务，或定期执行任务。
 * 当需要多个工作线程，或者需要 {@link ThreadPoolExecutor} 提供的额外灵活性或功能时，
 * 该类优于 {@link java.util.Timer}。
 *
 * <p>延迟任务不会早于它们被启用的时间执行，但没有任何实时保证启用后它们何时开始执行。
 * 为相同执行时间调度的任务按照提交的先进先出（FIFO）顺序启用。
 *
 * <p>当一个已提交的任务在运行之前被取消时，任务执行将被抑制。默认情况下，这样被取消的任务不会自动从工作队列中删除，直到它的延迟时间过去。
 * 虽然这允许进一步的检查和监控，但它可能导致无限期保留已取消的任务。为避免这种情况，可以将 {@link #setRemoveOnCancelPolicy} 设置为 {@code true}，
 * 这样会在取消时立即将任务从工作队列中删除。
 *
 * <p>通过 {@code scheduleAtFixedRate} 或 {@code scheduleWithFixedDelay} 调度的任务的连续执行不会重叠。
 * 尽管不同的执行可能由不同的线程执行，但前一次执行的效果<a href="package-summary.html#MemoryVisibility"><i>先行发生</i></a>于后续执行。
 *
 * <p>虽然该类继承自 {@link ThreadPoolExecutor}，但部分继承的方法对于此类没有意义。
 * 特别是，由于该类使用了固定大小的线程池，使用 {@code corePoolSize} 线程和一个无界队列，
 * 因此调整 {@code maximumPoolSize} 没有实际作用。另外，几乎没有理由将 {@code corePoolSize} 设置为零或使用 {@code allowCoreThreadTimeOut}，
 * 因为这样可能导致池中没有足够的线程来处理变得有资格运行的任务。
 *
 * <p><b>扩展说明：</b>此类重写了 {@link ThreadPoolExecutor#execute(Runnable) execute} 和
 * {@link AbstractExecutorService#submit(Runnable) submit} 方法，以生成内部 {@link ScheduledFuture} 对象来控制每个任务的延迟和调度。
 * 为了保持功能，子类中对这些方法的任何进一步重写必须调用超类版本，这实际上禁用了额外的任务自定义。然而，此类提供了替代的受保护扩展方法
 * {@code decorateTask}（为 {@code Runnable} 和 {@code Callable} 各有一个版本），可用于自定义通过 {@code execute}、{@code submit}、
 * {@code schedule}、{@code scheduleAtFixedRate} 和 {@code scheduleWithFixedDelay} 输入的命令使用的具体任务类型。
 * 默认情况下，{@code ScheduledThreadPoolExecutor} 使用扩展自 {@link FutureTask} 的任务类型。然而，这可以使用如下形式的子类来修改或替换：
 *
 * <pre> {@code
 * public class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableScheduledFuture<V> { ... }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Runnable r, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(r, task);
 *   }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Callable<V> c, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(c, task);
 *   }
 *   // ... 添加构造函数等。
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ScheduledThreadPoolExecutor
        extends ThreadPoolExecutor
        implements ScheduledExecutorService {

/*
 * 此类专门实现了 ThreadPoolExecutor，提供了：
 *
 * 1. 使用自定义任务类型 ScheduledFutureTask，即使对于不需要调度的任务
 *    （即通过 ExecutorService execute 提交的任务，而不是通过 ScheduledExecutorService 方法提交的任务），
 *    这些任务也被视为延迟为零的任务。
 *
 * 2. 使用自定义队列（DelayedWorkQueue），它是无界 DelayQueue 的一种变体。
 *    由于没有容量限制，并且 corePoolSize 和 maximumPoolSize 实际上相同，
 *    与 ThreadPoolExecutor 相比，执行机制简化了（参见 delayedExecute）。
 *
 * 3. 支持可选的关机后运行参数，这导致了对关机方法的覆盖，以移除和取消在关机后不应执行的任务，
 *    以及当任务重新提交时与关机重叠的不同重新检查逻辑。
 *
 * 4. 任务装饰方法，允许拦截和记录，因为子类无法通过重写 submit 方法实现这些效果。这些不会影响池的控制逻辑。
 */

    /**
     * 如果在关闭时应该取消/抑制定时任务，则为 false。
     */
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;

    /**
     * 如果在关闭时应该取消非定时任务，则为 false。
     */
    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;

    /**
     * 如果 ScheduledFutureTask.cancel 应该从队列中移除，则为 true。
     */
    private volatile boolean removeOnCancel = false;

    /**
     * 序列号，用于打破调度中的平局，并依次保证在打平条目中按 FIFO 顺序。
     */
    private static final AtomicLong sequencer = new AtomicLong();

    /**
     * 返回当前的纳秒时间。
     */
    final long now() {
        return System.nanoTime();
    }

    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {

        /** 序列号，用于打破 FIFO 中的平局 */
        private final long sequenceNumber;

        /** 任务将在纳秒时间单位中执行的时间 */
        private long time;

        /**
         * 任务的周期，以纳秒为单位。正值表示固定频率执行。负值表示固定延迟执行。值为 0 表示一次性任务。
         */
        private final long period;

        /** 实际任务，将由 reExecutePeriodic 重新排队 */
        RunnableScheduledFuture<V> outerTask = this;

        /**
         * 在延迟队列中的索引，用于支持更快的取消操作。
         */
        int heapIndex;

        /**
         * 创建一个具有给定纳秒时间触发时间的一次性操作。
         */
        ScheduledFutureTask(Runnable r, V result, long ns) {
            super(r, result);
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        /**
         * 创建一个具有给定纳秒时间和周期的定期操作。
         */
        ScheduledFutureTask(Runnable r, V result, long ns, long period) {
            super(r, result);
            this.time = ns;
            this.period = period;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        /**
         * 创建一个具有给定纳秒时间触发时间的一次性操作。
         */
        ScheduledFutureTask(Callable<V> callable, long ns) {
            super(callable);
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(time - now(), NANOSECONDS);
        }

        public int compareTo(Delayed other) {
            if (other == this) // 如果是同一个对象则返回 0
                return 0;
            if (other instanceof ScheduledFutureTask) {
                ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;
                long diff = time - x.time;
                if (diff < 0)
                    return -1;
                else if (diff > 0)
                    return 1;
                else if (sequenceNumber < x.sequenceNumber)
                    return -1;
                else
                    return 1;
            }
            long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        /**
         * 如果这是定期（而不是一次性）操作，则返回 {@code true}。
         *
         * @return {@code true} 如果是定期的
         */
        public boolean isPeriodic() {
            return period != 0;
        }

        /**
         * 为定期任务设置下一次执行的时间。
         */
        private void setNextRunTime() {
            long p = period;
            if (p > 0)
                time += p;
            else
                time = triggerTime(-p);
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled && removeOnCancel && heapIndex >= 0)
                remove(this);
            return cancelled;
        }

        /**
         * 覆盖 FutureTask 版本，以便在定期任务时重置/重新排队。
         */
        public void run() {
            boolean periodic = isPeriodic();
            if (!canRunInCurrentRunState(periodic))
                cancel(false);
            else if (!periodic)
                ScheduledFutureTask.super.run();
            else if (ScheduledFutureTask.super.runAndReset()) {
                setNextRunTime();
                reExecutePeriodic(outerTask);
            }
        }
    }

    /**
     * 如果可以在当前运行状态下运行给定任务，则返回 true。
     *
     * @param periodic true 如果此任务为定期任务，false 如果是延迟任务
     */
    boolean canRunInCurrentRunState(boolean periodic) {
        return isRunningOrShutdown(periodic ?
                continueExistingPeriodicTasksAfterShutdown :
                executeExistingDelayedTasksAfterShutdown);
    }

    /**
     * 用于延迟或定期任务的主要执行方法。如果线程池关闭，则拒绝任务。否则将任务添加到队列中，
     * 并在必要时启动一个线程来运行它。（我们不能预先启动线程来运行任务，因为任务可能尚未应该运行。）
     * 如果线程池在任务添加时关闭，则根据状态和关闭后的运行参数取消并移除任务。
     *
     * @param task 任务
     */
    private void delayedExecute(RunnableScheduledFuture<?> task) {
        if (isShutdown())
            reject(task);
        else {
            super.getQueue().add(task);
            if (isShutdown() &&
                    !canRunInCurrentRunState(task.isPeriodic()) &&
                    remove(task))
                task.cancel(false);
            else
                ensurePrestart();
        }
    }

    /**
     * 重新排队定期任务，除非当前运行状态禁止它。与 delayedExecute 类似，只是丢弃任务而不是拒绝。
     *
     * @param task 任务
     */
    void reExecutePeriodic(RunnableScheduledFuture<?> task) {
        if (canRunInCurrentRunState(true)) {
            super.getQueue().add(task);
            if (!canRunInCurrentRunState(true) && remove(task))
                task.cancel(false);
            else
                ensurePrestart();
        }
    }

    /**
     * 当为 `shutdown` 设置 `false` 时，取消并清除不应运行的所有任务。
     * 在 `super.shutdown` 内部调用。
     */
    @Override void onShutdown() {
        BlockingQueue<Runnable> q = super.getQueue();
        boolean keepDelayed =
                getExecuteExistingDelayedTasksAfterShutdownPolicy();
        boolean keepPeriodic =
                getContinueExistingPeriodicTasksAfterShutdownPolicy();
        if (!keepDelayed && !keepPeriodic) {
            for (Object e : q.toArray())
                if (e instanceof RunnableScheduledFuture<?>)
                    ((RunnableScheduledFuture<?>) e).cancel(false);
            q.clear();
        }
        else {
            // 遍历快照，避免迭代器异常
            for (Object e : q.toArray()) {
                if (e instanceof RunnableScheduledFuture) {
                    RunnableScheduledFuture<?> t =
                            (RunnableScheduledFuture<?>)e;
                    if ((t.isPeriodic() ? !keepPeriodic : !keepDelayed) ||
                            t.isCancelled()) { // 也移除已取消的任务
                        if (q.remove(t))
                            t.cancel(false);
                    }
                }
            }
        }
        tryTerminate();
    }

    /**
     * 修改或替换用于执行 `Runnable` 的任务。
     * 此方法可用于覆盖用于管理内部任务的具体类。
     * 默认实现仅返回给定的任务。
     *
     * @param runnable 提交的 Runnable
     * @param task 被创建来执行 runnable 的任务
     * @param <V> 任务结果的类型
     * @return 可以执行 runnable 的任务
     * @since 1.6
     */
    protected <V> RunnableScheduledFuture<V> decorateTask(
            Runnable runnable, RunnableScheduledFuture<V> task) {
        return task;
    }

    /**
     * 修改或替换用于执行 `Callable` 的任务。
     * 此方法可用于覆盖用于管理内部任务的具体类。
     * 默认实现仅返回给定的任务。
     *
     * @param callable 提交的 Callable
     * @param task 被创建来执行 callable 的任务
     * @param <V> 任务结果的类型
     * @return 可以执行 callable 的任务
     * @since 1.6
     */
    protected <V> RunnableScheduledFuture<V> decorateTask(
            Callable<V> callable, RunnableScheduledFuture<V> task) {
        return task;
    }

    /**
     * 创建一个具有给定核心线程池大小的 {@code ScheduledThreadPoolExecutor}。
     *
     * @param corePoolSize 要保持在池中的线程数，即使它们处于空闲状态，除非设置了 {@code allowCoreThreadTimeOut}
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     */
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                new DelayedWorkQueue());
    }

    /**
     * 使用给定的初始参数创建一个新的 {@code ScheduledThreadPoolExecutor}。
     *
     * @param corePoolSize 要保持在池中的线程数，即使它们处于空闲状态，除非设置了 {@code allowCoreThreadTimeOut}
     * @param threadFactory 当执行器创建新线程时使用的工厂
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     * @throws NullPointerException 如果 {@code threadFactory} 为 null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize,
                                       ThreadFactory threadFactory) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                new DelayedWorkQueue(), threadFactory);
    }

    /**
     * 使用给定的初始参数创建一个新的 ScheduledThreadPoolExecutor。
     *
     * @param corePoolSize 要保持在池中的线程数，即使它们处于空闲状态，除非设置了 {@code allowCoreThreadTimeOut}
     * @param handler 当线程边界和队列容量已达到时使用的处理程序
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     * @throws NullPointerException 如果 {@code handler} 为 null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                new DelayedWorkQueue(), handler);
    }

    /**
     * 使用给定的初始参数创建一个新的 ScheduledThreadPoolExecutor。
     *
     * @param corePoolSize 要保持在池中的线程数，即使它们处于空闲状态，除非设置了 {@code allowCoreThreadTimeOut}
     * @param threadFactory 当执行器创建新线程时使用的工厂
     * @param handler 当线程边界和队列容量已达到时使用的处理程序
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     * @throws NullPointerException 如果 {@code threadFactory} 或 {@code handler} 为 null
     */
    public ScheduledThreadPoolExecutor(int corePoolSize,
                                       ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                new DelayedWorkQueue(), threadFactory, handler);
    }

    /**
     * 返回延迟任务的触发时间。
     */
    private long triggerTime(long delay, TimeUnit unit) {
        return triggerTime(unit.toNanos((delay < 0) ? 0 : delay));
    }

    /**
     * 返回延迟任务的触发时间。
     */
    long triggerTime(long delay) {
        return now() +
                ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
    }

    /**
     * 限制队列中所有延迟的值在 Long.MAX_VALUE 范围内，避免 compareTo 中的溢出。
     * 如果一个任务有资格出队，但尚未出队，而其他任务以 Long.MAX_VALUE 延迟被添加，
     * 则可能发生这种情况。
     */
    private long overflowFree(long delay) {
        Delayed head = (Delayed) super.getQueue().peek();
        if (head != null) {
            long headDelay = head.getDelay(NANOSECONDS);
            if (headDelay < 0 && (delay - headDelay < 0))
                delay = Long.MAX_VALUE + headDelay;
        }
        return delay;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay,
                                       TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        RunnableScheduledFuture<?> t = decorateTask(command,
                new ScheduledFutureTask<Void>(command, null,
                        triggerTime(delay, unit)));
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay,
                                           TimeUnit unit) {
        if (callable == null || unit == null)
            throw new NullPointerException();
        RunnableScheduledFuture<V> t = decorateTask(callable,
                new ScheduledFutureTask<V>(callable,
                        triggerTime(delay, unit)));
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (period <= 0)
            throw new IllegalArgumentException();
        ScheduledFutureTask<Void> sft =
                new ScheduledFutureTask<Void>(command,
                        null,
                        triggerTime(initialDelay, unit),
                        unit.toNanos(period));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (delay <= 0)
            throw new IllegalArgumentException();
        ScheduledFutureTask<Void> sft =
                new ScheduledFutureTask<Void>(command,
                        null,
                        triggerTime(initialDelay, unit),
                        unit.toNanos(-delay));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;
        delayedExecute(t);
        return t;
    }

    /**
     * 使用零延迟执行 {@code command}。
     * 这与 {@link #schedule(Runnable,long,TimeUnit) schedule(command, 0, anyUnit)} 效果相同。
     * 注意，队列和 {@code shutdownNow} 返回的列表会访问零延迟的 {@link ScheduledFuture}，而不是 {@code command} 本身。
     *
     * <p>使用 {@code ScheduledFuture} 对象的一个结果是，即使 {@code command} 突然终止，
     * {@link ThreadPoolExecutor#afterExecute afterExecute} 始终会用空的 {@code Throwable} 参数调用。
     * 相反，此类任务抛出的 {@code Throwable} 可以通过 {@link Future#get} 获得。
     *
     * @throws RejectedExecutionException 如果任务由于执行器已关闭而无法接受执行
     * @throws NullPointerException {@inheritDoc}
     */
    public void execute(Runnable command) {
        schedule(command, 0, NANOSECONDS);
    }

    // 覆盖 AbstractExecutorService 方法

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public Future<?> submit(Runnable task) {
        return schedule(task, 0, NANOSECONDS);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <T> Future<T> submit(Runnable task, T result) {
        return schedule(Executors.callable(task, result), 0, NANOSECONDS);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0, NANOSECONDS);
    }

    /**
     * 设置是否在执行器 {@code shutdown} 后继续执行现有的周期性任务的策略。
     * 在这种情况下，这些任务仅在 {@code shutdownNow} 时或在已关闭时设置策略为 {@code false} 后终止。
     * 此值默认是 {@code false}。
     *
     * @param value 如果为 {@code true}，则在关闭后继续执行，否则不执行
     * @see #getContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        continueExistingPeriodicTasksAfterShutdown = value;
        if (!value && isShutdown())
            onShutdown();
    }

    /**
     * 获取是否在执行器 {@code shutdown} 后继续执行现有的周期性任务的策略。
     * 在这种情况下，这些任务仅在 {@code shutdownNow} 时或在已关闭时设置策略为 {@code false} 后终止。
     * 此值默认是 {@code false}。
     *
     * @return {@code true} 表示在关闭后继续执行
     * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return continueExistingPeriodicTasksAfterShutdown;
    }

    /**
     * 设置在执行器 {@code shutdown} 后是否执行现有的延迟任务的策略。
     * 在这种情况下，这些任务仅在 {@code shutdownNow} 时终止，或在已关闭时设置策略为 {@code false} 后终止。
     * 此值默认是 {@code true}。
     *
     * @param value 如果为 {@code true}，则在关闭后执行，否则不执行
     * @see #getExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        executeExistingDelayedTasksAfterShutdown = value;
        if (!value && isShutdown())
            onShutdown();
    }

    /**
     * 获取在执行器 {@code shutdown} 后是否执行现有的延迟任务的策略。
     * 在这种情况下，这些任务仅在 {@code shutdownNow} 时终止，或在已关闭时设置策略为 {@code false} 后终止。
     * 此值默认是 {@code true}。
     *
     * @return {@code true} 表示在关闭后执行
     * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return executeExistingDelayedTasksAfterShutdown;
    }

    /**
     * 设置是否在取消任务时立即将其从工作队列中移除的策略。
     * 此值默认是 {@code false}。
     *
     * @param value 如果为 {@code true}，则在取消时立即移除，否则不移除
     * @see #getRemoveOnCancelPolicy
     * @since 1.7
     */
    public void setRemoveOnCancelPolicy(boolean value) {
        removeOnCancel = value;
    }

    /**
     * 获取在取消任务时是否立即将其从工作队列中移除的策略。
     * 此值默认是 {@code false}。
     *
     * @return {@code true} 表示取消任务时立即移除
     * @see #setRemoveOnCancelPolicy
     * @since 1.7
     */
    public boolean getRemoveOnCancelPolicy() {
        return removeOnCancel;
    }

    /**
     * 启动一个有序的关闭操作，其中先前提交的任务会被执行，但不会接受新任务。
     * 如果已经关闭，则调用没有额外效果。
     *
     * <p>此方法不会等待先前提交的任务完成执行。使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     *
     * <p>如果 {@code ExecuteExistingDelayedTasksAfterShutdownPolicy} 已设置为 {@code false}，
     * 那么在延迟尚未过期的现有延迟任务将被取消。并且，除非 {@code ContinueExistingPeriodicTasksAfterShutdownPolicy} 设置为 {@code true}，
     * 否则现有周期性任务的未来执行将被取消。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        super.shutdown();
    }

    /**
     * 尝试停止所有正在执行的任务，暂停正在等待的任务，并返回一个等待执行的任务列表。
     *
     * <p>此方法不会等待正在执行的任务终止。使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     *
     * <p>除了最佳努力外，不保证能停止正在执行的任务。此实现通过 {@link Thread#interrupt} 来取消任务，
     * 因此无法响应中断的任务可能永远不会终止。
     *
     * @return 未开始执行的任务的列表。此列表中的每个元素都是 {@link ScheduledFuture}，包括使用 {@code execute} 提交的任务，
     * 它们用于调度目的作为零延迟的 {@code ScheduledFuture}。
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        return super.shutdownNow();
    }

    /**
     * 返回此执行器使用的任务队列。此队列中的每个元素都是 {@link ScheduledFuture}，
     * 包括那些使用 {@code execute} 提交的任务，它们用于调度目的作为零延迟的 {@code ScheduledFuture}。
     * 遍历此队列不保证按任务执行顺序遍历。
     *
     * @return 任务队列
     */
    public BlockingQueue<Runnable> getQueue() {
        return super.getQueue();
    }
    /**
     * 专门的延迟队列。为了与 TPE 声明配合使用，此类必须声明为 BlockingQueue<Runnable>，
     * 尽管它只能包含 RunnableScheduledFutures。
     */
    static class DelayedWorkQueue extends AbstractQueue<Runnable>
            implements BlockingQueue<Runnable> {

        /*
         * DelayedWorkQueue 基于堆结构的数据结构，类似于 DelayQueue 和 PriorityQueue 中的那些，
         * 只是每个 ScheduledFutureTask 还记录它在堆数组中的索引。这样可以消除在取消时找到任务的需要，
         * 极大加快了删除的速度（从 O(n) 降低到 O(log n)），并减少了垃圾保留情况，
         * 否则会在任务上升到堆顶之前清理掉。此外，因为队列还可能持有不是 ScheduledFutureTask 的
         * RunnableScheduledFutures，因此我们不能保证始终能够获得这样的索引，
         * 在这种情况下，我们会退回到线性搜索。（我们预计大多数任务不会装饰过，
         * 因此更快速的情况会更为常见。）
         *
         * 所有堆操作都必须记录索引变化——主要是在 siftUp 和 siftDown 内部。
         * 在删除时，任务的 heapIndex 设置为 -1。请注意，ScheduledFutureTasks 在队列中最多出现一次，
         * 所以它们的 heapIndex 是唯一标识符。
         */

        private static final int INITIAL_CAPACITY = 16;
        private RunnableScheduledFuture<?>[] queue =
                new RunnableScheduledFuture<?>[INITIAL_CAPACITY];
        private final ReentrantLock lock = new ReentrantLock();
        private int size = 0;

        /**
         * 分配给等待队列头部任务的线程。该变体使用了 Leader-Follower 模式
         * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/)，
         * 旨在尽量减少不必要的定时等待。当一个线程成为领导者时，它仅等待下一个延迟到期，
         * 而其他线程则无限期等待。在从 take() 或 poll(...) 返回之前，领导线程必须通知其他一些线程，
         * 除非在此期间有其他线程成为领导者。每当队列头部的任务被一个更早的任务替换时，
         * 领导字段会通过重置为 null 来无效，并通知一些等待线程（但不一定是当前的领导者）。
         * 因此，等待线程必须准备在等待时获取和失去领导权。
         */
        private Thread leader = null;

        /**
         * 当有更新的任务可在队列头部使用，或者可能需要一个新线程成为领导者时，会发出信号的条件。
         */
        private final Condition available = lock.newCondition();

        /**
         * 如果 f 是一个 ScheduledFutureTask，则设置它的 heapIndex。
         */
        private void setIndex(RunnableScheduledFuture<?> f, int idx) {
            if (f instanceof ScheduledFutureTask)
                ((ScheduledFutureTask<?>) f).heapIndex = idx;
        }

        /**
         * 将添加在底部的元素按堆序上浮到其正确位置。仅在持有锁时调用。
         */
        private void siftUp(int k, RunnableScheduledFuture<?> key) {
            while (k > 0) {
                int parent = (k - 1) >>> 1;
                RunnableScheduledFuture<?> e = queue[parent];
                if (key.compareTo(e) >= 0)
                    break;
                queue[k] = e;
                setIndex(e, k);
                k = parent;
            }
            queue[k] = key;
            setIndex(key, k);
        }

        /**
         * 将添加在顶部的元素按堆序下沉到其正确位置。仅在持有锁时调用。
         */
        private void siftDown(int k, RunnableScheduledFuture<?> key) {
            int half = size >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                RunnableScheduledFuture<?> c = queue[child];
                int right = child + 1;
                if (right < size && c.compareTo(queue[right]) > 0)
                    c = queue[child = right];
                if (key.compareTo(c) <= 0)
                    break;
                queue[k] = c;
                setIndex(c, k);
                k = child;
            }
            queue[k] = key;
            setIndex(key, k);
        }

        /**
         * 调整堆数组的大小。仅在持有锁时调用。
         */
        private void grow() {
            int oldCapacity = queue.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1); // 增长 50%
            if (newCapacity < 0) // 溢出
                newCapacity = Integer.MAX_VALUE;
            queue = Arrays.copyOf(queue, newCapacity);
        }

        /**
         * 查找给定对象的索引，若不存在则返回 -1。
         */
        private int indexOf(Object x) {
            if (x != null) {
                if (x instanceof ScheduledFutureTask) {
                    int i = ((ScheduledFutureTask<?>) x).heapIndex;
                    // 检查合理性：x 可能是来自其他池的 ScheduledFutureTask。
                    if (i >= 0 && i < size && queue[i] == x)
                        return i;
                } else {
                    for (int i = 0; i < size; i++)
                        if (x.equals(queue[i]))
                            return i;
                }
            }
            return -1;
        }

        public boolean contains(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return indexOf(x) != -1;
            } finally {
                lock.unlock();
            }
        }

        public boolean remove(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = indexOf(x);
                if (i < 0)
                    return false;

                setIndex(queue[i], -1);
                int s = --size;
                RunnableScheduledFuture<?> replacement = queue[s];
                queue[s] = null;
                if (s != i) {
                    siftDown(i, replacement);
                    if (queue[i] == replacement)
                        siftUp(i, replacement);
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }

        public RunnableScheduledFuture<?> peek() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return queue[0];
            } finally {
                lock.unlock();
            }
        }

        public boolean offer(Runnable x) {
            if (x == null)
                throw new NullPointerException();
            RunnableScheduledFuture<?> e = (RunnableScheduledFuture<?>) x;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = size;
                if (i >= queue.length)
                    grow();
                size = i + 1;
                if (i == 0) {
                    queue[0] = e;
                    setIndex(e, 0);
                } else {
                    siftUp(i, e);
                }
                if (queue[0] == e) {
                    leader = null;
                    available.signal();
                }
            } finally {
                lock.unlock();
            }
            return true;
        }

        public void put(Runnable e) {
            offer(e);
        }

        public boolean add(Runnable e) {
            return offer(e);
        }

        public boolean offer(Runnable e, long timeout, TimeUnit unit) {
            return offer(e);
        }

        /**
         * 为 poll 和 take 执行常见的清理工作：用最后一个元素替换第一个元素并下沉它。仅在持有锁时调用。
         * @param f 要删除并返回的任务
         */
        private RunnableScheduledFuture<?> finishPoll(RunnableScheduledFuture<?> f) {
            int s = --size;
            RunnableScheduledFuture<?> x = queue[s];
            queue[s] = null;
            if (s != 0)
                siftDown(0, x);
            setIndex(f, -1);
            return f;
        }

        public RunnableScheduledFuture<?> poll() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first = queue[0];
                if (first == null || first.getDelay(NANOSECONDS) > 0)
                    return null;
                else
                    return finishPoll(first);
            } finally {
                lock.unlock();
            }
        }
        public RunnableScheduledFuture<?> take() throws InterruptedException {
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for (;;) {
                    RunnableScheduledFuture<?> first = queue[0];
                    if (first == null)
                        available.await();
                    else {
                        long delay = first.getDelay(NANOSECONDS);
                        if (delay <= 0)
                            return finishPoll(first);
                        first = null; // 不在等待时保持引用
                        if (leader != null)
                            available.await();
                        else {
                            Thread thisThread = Thread.currentThread();
                            leader = thisThread;
                            try {
                                available.awaitNanos(delay);
                            } finally {
                                if (leader == thisThread)
                                    leader = null;
                            }
                        }
                    }
                }
            } finally {
                if (leader == null && queue[0] != null)
                    available.signal();
                lock.unlock();
            }
        }

        public RunnableScheduledFuture<?> poll(long timeout, TimeUnit unit)
                throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for (;;) {
                    RunnableScheduledFuture<?> first = queue[0];
                    if (first == null) {
                        if (nanos <= 0)
                            return null;
                        else
                            nanos = available.awaitNanos(nanos);
                    } else {
                        long delay = first.getDelay(NANOSECONDS);
                        if (delay <= 0)
                            return finishPoll(first);
                        if (nanos <= 0)
                            return null;
                        first = null; // 不在等待时保持引用
                        if (nanos < delay || leader != null)
                            nanos = available.awaitNanos(nanos);
                        else {
                            Thread thisThread = Thread.currentThread();
                            leader = thisThread;
                            try {
                                long timeLeft = available.awaitNanos(delay);
                                nanos -= delay - timeLeft;
                            } finally {
                                if (leader == thisThread)
                                    leader = null;
                            }
                        }
                    }
                }
            } finally {
                if (leader == null && queue[0] != null)
                    available.signal();
                lock.unlock();
            }
        }

        public void clear() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                for (int i = 0; i < size; i++) {
                    RunnableScheduledFuture<?> t = queue[i];
                    if (t != null) {
                        queue[i] = null;
                        setIndex(t, -1);
                    }
                }
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        /**
         * 仅在持有锁时调用。仅在 drainTo 时使用，返回第一个已过期的元素。
         */
        private RunnableScheduledFuture<?> peekExpired() {
            // assert lock.isHeldByCurrentThread();
            RunnableScheduledFuture<?> first = queue[0];
            return (first == null || first.getDelay(NANOSECONDS) > 0) ?
                    null : first;
        }

        public int drainTo(Collection<? super Runnable> c) {
            if (c == null)
                throw new NullPointerException();
            if (c == this)
                throw new IllegalArgumentException();
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first;
                int n = 0;
                while ((first = peekExpired()) != null) {
                    c.add(first);   // 按顺序添加，防止 add() 抛出异常
                    finishPoll(first);
                    ++n;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }

        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            if (c == null)
                throw new NullPointerException();
            if (c == this)
                throw new IllegalArgumentException();
            if (maxElements <= 0)
                return 0;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first;
                int n = 0;
                while (n < maxElements && (first = peekExpired()) != null) {
                    c.add(first);   // 按顺序添加，防止 add() 抛出异常
                    finishPoll(first);
                    ++n;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }

        public Object[] toArray() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return Arrays.copyOf(queue, size, Object[].class);
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if (a.length < size)
                    return (T[]) Arrays.copyOf(queue, size, a.getClass());
                System.arraycopy(queue, 0, a, 0, size);
                if (a.length > size)
                    a[size] = null;
                return a;
            } finally {
                lock.unlock();
            }
        }

        public Iterator<Runnable> iterator() {
            return new Itr(Arrays.copyOf(queue, size));
        }

        /**
         * 基于底层队列副本的快照迭代器。
         */
        private class Itr implements Iterator<Runnable> {
            final RunnableScheduledFuture<?>[] array;
            int cursor = 0;     // 下一个要返回的元素索引
            int lastRet = -1;   // 上一个元素的索引，或 -1 表示无此元素

            Itr(RunnableScheduledFuture<?>[] array) {
                this.array = array;
            }

            public boolean hasNext() {
                return cursor < array.length;
            }

            public Runnable next() {
                if (cursor >= array.length)
                    throw new NoSuchElementException();
                lastRet = cursor;
                return array[cursor++];
            }

            public void remove() {
                if (lastRet < 0)
                    throw new IllegalStateException();
                DelayedWorkQueue.this.remove(array[lastRet]);
                lastRet = -1;
            }
        }
    }
}





