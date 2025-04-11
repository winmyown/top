
package org.top.java.netty.source.util.concurrent;

import org.jetbrains.annotations.Async.Schedule;
import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.ThreadExecutorMap;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-thread singleton {@link EventExecutor}.  It starts the thread automatically and stops it when there is no
 * task pending in the task queue for 1 second.  Please note it is not scalable to schedule large number of tasks to
 * this executor; use a dedicated executor.
 */

/**
 * 单线程单例 {@link EventExecutor}。它会自动启动线程，并在任务队列中没有任务挂起1秒时停止线程。请注意，将大量任务调度到此执行器是不可扩展的；请使用专用的执行器。
 */
public final class GlobalEventExecutor extends AbstractScheduledEventExecutor implements OrderedEventExecutor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalEventExecutor.class);

    private static final long SCHEDULE_QUIET_PERIOD_INTERVAL = TimeUnit.SECONDS.toNanos(1);

    public static final GlobalEventExecutor INSTANCE = new GlobalEventExecutor();

    final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
    final ScheduledFutureTask<Void> quietPeriodTask = new ScheduledFutureTask<Void>(
            this, Executors.<Void>callable(new Runnable() {
        @Override
        public void run() {
            // NOOP
            // NOOP
        }
    }, null),
            // note: the getCurrentTimeNanos() call here only works because this is a final class, otherwise the method
            // 注意：这里的 getCurrentTimeNanos() 调用之所以有效，是因为这是一个 final 类，否则该方法
            // could be overridden leading to unsafe initialization here!
            // 这里可能被重写，导致不安全的初始化！
            deadlineNanos(getCurrentTimeNanos(), SCHEDULE_QUIET_PERIOD_INTERVAL),
            -SCHEDULE_QUIET_PERIOD_INTERVAL
    );

    // because the GlobalEventExecutor is a singleton, tasks submitted to it can come from arbitrary threads and this

    // 因为 GlobalEventExecutor 是一个单例，提交给它的任务可以来自任意线程，并且这些
    // can trigger the creation of a thread from arbitrary thread groups; for this reason, the thread factory must not
    // 可以触发从任意线程组创建线程；因此，线程工厂不得
    // be sticky about its thread group
    // 坚持其线程组
    // visible for testing
    // 测试可见
    final ThreadFactory threadFactory;
    private final TaskRunner taskRunner = new TaskRunner();
    private final AtomicBoolean started = new AtomicBoolean();
    volatile Thread thread;

    private final Future<?> terminationFuture = new FailedFuture<Object>(this, new UnsupportedOperationException());

    private GlobalEventExecutor() {
        scheduledTaskQueue().add(quietPeriodTask);
        threadFactory = ThreadExecutorMap.apply(new DefaultThreadFactory(
                DefaultThreadFactory.toPoolName(getClass()), false, Thread.NORM_PRIORITY, null), this);
    }

    /**
     * Take the next {@link Runnable} from the task queue and so will block if no task is currently present.
     *
     * @return {@code null} if the executor thread has been interrupted or waken up.
     */

    /**
     * 从任务队列中取出下一个 {@link Runnable}，如果当前没有任务，则会阻塞。
     *
     * @return 如果执行线程被中断或唤醒，则返回 {@code null}。
     */
    Runnable takeTask() {
        BlockingQueue<Runnable> taskQueue = this.taskQueue;
        for (;;) {
            ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
            if (scheduledTask == null) {
                Runnable task = null;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException e) {
                    // Ignore
                    // 忽略
                }
                return task;
            } else {
                long delayNanos = scheduledTask.delayNanos();
                Runnable task = null;
                if (delayNanos > 0) {
                    try {
                        task = taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        // Waken up.
                        // 醒来。
                        return null;
                    }
                }
                if (task == null) {
                    // We need to fetch the scheduled tasks now as otherwise there may be a chance that
                    // 我们现在需要获取计划任务，否则可能会有机会
                    // scheduled tasks are never executed if there is always one task in the taskQueue.
                    // 如果 taskQueue 中始终有一个任务，则计划任务永远不会执行。
                    // This is for example true for the read task of OIO Transport
                    // 例如，对于OIO Transport的读取任务，这是正确的
                    // See https://github.com/netty/netty/issues/1614
                    // 参见 https://github.com/netty/netty/issues/1614
                    fetchFromScheduledTaskQueue();
                    task = taskQueue.poll();
                }

                if (task != null) {
                    return task;
                }
            }
        }
    }

    private void fetchFromScheduledTaskQueue() {
        long nanoTime = getCurrentTimeNanos();
        Runnable scheduledTask = pollScheduledTask(nanoTime);
        while (scheduledTask != null) {
            taskQueue.add(scheduledTask);
            scheduledTask = pollScheduledTask(nanoTime);
        }
    }

    /**
     * Return the number of tasks that are pending for processing.
     */

    /**
     * 返回待处理的任务数量。
     */
    public int pendingTasks() {
        return taskQueue.size();
    }

    /**
     * Add a task to the task queue, or throws a {@link RejectedExecutionException} if this instance was shutdown
     * before.
     */

    /**
     * 将任务添加到任务队列中，如果此实例在之前已关闭，则抛出 {@link RejectedExecutionException}。
     */
    private void addTask(Runnable task) {
        taskQueue.add(ObjectUtil.checkNotNull(task, "task"));
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShuttingDown() {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    /**
     * Waits until the worker thread of this executor has no tasks left in its task queue and terminates itself.
     * Because a new worker thread will be started again when a new task is submitted, this operation is only useful
     * when you want to ensure that the worker thread is terminated <strong>after</strong> your application is shut
     * down and there's no chance of submitting a new task afterwards.
     *
     * @return {@code true} if and only if the worker thread has been terminated
     */

    /**
     * 等待直到此执行器的工作线程的任务队列中没有任务剩余并自行终止。
     * 由于当提交新任务时，将再次启动一个新的工作线程，因此此操作仅在您希望确保工作线程在应用程序关闭后终止且之后没有机会提交新任务时有用。
     *
     * @return {@code true} 当且仅当工作线程已被终止
     */
    public boolean awaitInactivity(long timeout, TimeUnit unit) throws InterruptedException {
        ObjectUtil.checkNotNull(unit, "unit");

        final Thread thread = this.thread;
        if (thread == null) {
            throw new IllegalStateException("thread was not started");
        }
        thread.join(unit.toMillis(timeout));
        return !thread.isAlive();
    }

    @Override
    public void execute(Runnable task) {
        execute0(task);
    }

    private void execute0(@Schedule Runnable task) {
        addTask(ObjectUtil.checkNotNull(task, "task"));
        if (!inEventLoop()) {
            startThread();
        }
    }

    private void startThread() {
        if (started.compareAndSet(false, true)) {
            final Thread t = threadFactory.newThread(taskRunner);
            // Set to null to ensure we not create classloader leaks by holds a strong reference to the inherited
            // 设置为 null 以确保我们不会通过持有对继承的强引用来创建类加载器泄漏
            // classloader.
            // 类加载器
            // See:
            // 参见：
            // - https://github.com/netty/netty/issues/7290

// 该问题是由于在`Http2FrameCodec`中，当接收到一个`RST_STREAM`帧时，会立即关闭流，
// 而不会等待所有待处理的数据帧被处理完毕。这可能导致在流关闭之前，某些数据帧被丢弃。
// 为了解决这个问题，我们需要在关闭流之前，确保所有待处理的数据帧都被处理完毕。

            // - https://bugs.openjdk.java.net/browse/JDK-7008595
            // 问题描述：
// 在JDK 7中，当使用`javac`编译某些代码时，可能会导致`NullPointerException`。
// 该问题通常发生在处理泛型类型时，特别是在类型推断过程中。
// 此问题已被报告为JDK-7008595，并在后续版本中修复。
// 修复方案包括对类型推断算法的改进，以避免在特定情况下引发`NullPointerException`。
// 如果遇到此问题，建议升级到包含修复的JDK版本。
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    t.setContextClassLoader(null);
                    return null;
                }
            });

            // Set the thread before starting it as otherwise inEventLoop() may return false and so produce

            // 在启动线程之前设置它，否则 inEventLoop() 可能返回 false，从而导致
            // an assert error.
            // 一个断言错误。
            // See https://github.com/netty/netty/issues/4357
            // 参见 https://github.com/netty/netty/issues/4357
            thread = t;
            t.start();
        }
    }

    final class TaskRunner implements Runnable {
        @Override
        public void run() {
            for (;;) {
                Runnable task = takeTask();
                if (task != null) {
                    try {
                        runTask(task);
                    } catch (Throwable t) {
                        logger.warn("Unexpected exception from the global event executor: ", t);
                    }

                    if (task != quietPeriodTask) {
                        continue;
                    }
                }

                Queue<ScheduledFutureTask<?>> scheduledTaskQueue = GlobalEventExecutor.this.scheduledTaskQueue;
                // Terminate if there is no task in the queue (except the noop task).
                // 如果队列中没有任务（除了noop任务），则终止。
                if (taskQueue.isEmpty() && (scheduledTaskQueue == null || scheduledTaskQueue.size() == 1)) {
                    // Mark the current thread as stopped.
                    // 将当前线程标记为已停止。
                    // The following CAS must always success and must be uncontended,
                    // 以下CAS必须始终成功且必须无竞争，
                    // because only one thread should be running at the same time.
                    // 因为同一时间只应该有一个线程在运行。
                    boolean stopped = started.compareAndSet(true, false);
                    assert stopped;

                    // Check if there are pending entries added by execute() or schedule*() while we do CAS above.

                    // 检查在执行CAS操作时是否有由execute()或schedule*()添加的待处理条目。
                    // Do not check scheduledTaskQueue because it is not thread-safe and can only be mutated from a
                    // 不要检查scheduledTaskQueue，因为它不是线程安全的，并且只能从一个
                    // TaskRunner actively running tasks.
                    // TaskRunner 正在运行任务。
                    if (taskQueue.isEmpty()) {
                        // A) No new task was added and thus there's nothing to handle
                        // A) 没有添加新任务，因此没有需要处理的内容
                        //    -> safe to terminate because there's nothing left to do
                        //    -> 安全终止，因为没有其他事情可做
                        // B) A new thread started and handled all the new tasks.
                        // B) 一个新线程启动并处理了所有新任务。
                        //    -> safe to terminate the new thread will take care the rest
                        //    -> 安全终止，新线程将处理其余部分
                        break;
                    }

                    // There are pending tasks added again.

                    // 又有待处理的任务被添加了。
                    if (!started.compareAndSet(false, true)) {
                        // startThread() started a new thread and set 'started' to true.
                        // startThread() 启动了一个新线程并将 'started' 设置为 true。
                        // -> terminate this thread so that the new thread reads from taskQueue exclusively.
                        // -> 终止此线程，以便新线程独占从taskQueue读取。
                        break;
                    }

                    // New tasks were added, but this worker was faster to set 'started' to true.

                    // 新任务已添加，但此工作人员更快地将“started”设置为true。
                    // i.e. a new worker thread was not started by startThread().
                    // 即 startThread() 未启动新的工作线程。
                    // -> keep this thread alive to handle the newly added entries.
                    // -> 保持此线程存活以处理新添加的条目。
                }
            }
        }
    }
}
