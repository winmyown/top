

package org.top.java.netty.source.util;

import org.top.java.netty.source.util.concurrent.DefaultThreadFactory;
import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.StringUtil;
import org.top.java.netty.source.util.internal.SystemPropertyUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks if a thread is alive periodically and runs a task when a thread dies.
 * <p>
 * This thread starts a daemon thread to check the state of the threads being watched and to invoke their
 * associated {@link Runnable}s.  When there is no thread to watch (i.e. all threads are dead), the daemon thread
 * will terminate itself, and a new daemon thread will be started again when a new watch is added.
 * </p>
 *
 * @deprecated will be removed in the next major release
 */

/**
 * 定期检查线程是否存活，并在线程死亡时运行任务。
 * <p>
 * 该线程启动一个守护线程来检查被监视线程的状态，并调用它们关联的 {@link Runnable}。当没有线程需要监视时（即所有线程都已死亡），守护线程将自行终止，并在添加新的监视时再次启动新的守护线程。
 * </p>
 *
 * @deprecated 将在下一个主要版本中移除
 */
@Deprecated
public final class ThreadDeathWatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadDeathWatcher.class);
    // visible for testing
    // 测试可见
    static final ThreadFactory threadFactory;

    // Use a MPMC queue as we may end up checking isEmpty() from multiple threads which may not be allowed to do

    // 使用 MPMC 队列，因为我们可能会从多个线程检查 isEmpty()，这可能是不允许的
    // concurrently depending on the implementation of it in a MPSC queue.
    // 根据MPSC队列中的实现并发执行。
    private static final Queue<Entry> pendingEntries = new ConcurrentLinkedQueue<Entry>();
    private static final Watcher watcher = new Watcher();
    private static final AtomicBoolean started = new AtomicBoolean();
    private static volatile Thread watcherThread;

    static {
        String poolName = "threadDeathWatcher";
        String serviceThreadPrefix = SystemPropertyUtil.get("io.netty.serviceThreadPrefix");
        if (!StringUtil.isNullOrEmpty(serviceThreadPrefix)) {
            poolName = serviceThreadPrefix + poolName;
        }
        // because the ThreadDeathWatcher is a singleton, tasks submitted to it can come from arbitrary threads and
        // 由于 ThreadDeathWatcher 是一个单例，提交给它的任务可以来自任意线程
        // this can trigger the creation of a thread from arbitrary thread groups; for this reason, the thread factory
        // 这可能会触发从任意线程组创建线程；因此，线程工厂
        // must not be sticky about its thread group
        // 不能对它的线程组有粘性
        threadFactory = new DefaultThreadFactory(poolName, true, Thread.MIN_PRIORITY, null);
    }

    /**
     * Schedules the specified {@code task} to run when the specified {@code thread} dies.
     *
     * @param thread the {@link Thread} to watch
     * @param task the {@link Runnable} to run when the {@code thread} dies
     *
     * @throws IllegalArgumentException if the specified {@code thread} is not alive
     */

    /**
     * 调度指定的 {@code task} 在指定的 {@code thread} 死亡时运行。
     *
     * @param thread 要监视的 {@link Thread}
     * @param task 当 {@code thread} 死亡时要运行的 {@link Runnable}
     *
     * @throws IllegalArgumentException 如果指定的 {@code thread} 不是活动的
     */
    public static void watch(Thread thread, Runnable task) {
        ObjectUtil.checkNotNull(thread, "thread");
        ObjectUtil.checkNotNull(task, "task");

        if (!thread.isAlive()) {
            throw new IllegalArgumentException("thread must be alive.");
        }

        schedule(thread, task, true);
    }

    /**
     * Cancels the task scheduled via {@link #watch(Thread, Runnable)}.
     */

    /**
     * 取消通过 {@link #watch(Thread, Runnable)} 安排的任务。
     */
    public static void unwatch(Thread thread, Runnable task) {
        schedule(ObjectUtil.checkNotNull(thread, "thread"),
                ObjectUtil.checkNotNull(task, "task"),
                false);
    }

    private static void schedule(Thread thread, Runnable task, boolean isWatch) {
        pendingEntries.add(new Entry(thread, task, isWatch));

        if (started.compareAndSet(false, true)) {
            final Thread watcherThread = threadFactory.newThread(watcher);
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
                    watcherThread.setContextClassLoader(null);
                    return null;
                }
            });

            watcherThread.start();
            ThreadDeathWatcher.watcherThread = watcherThread;
        }
    }

    /**
     * Waits until the thread of this watcher has no threads to watch and terminates itself.
     * Because a new watcher thread will be started again on {@link #watch(Thread, Runnable)},
     * this operation is only useful when you want to ensure that the watcher thread is terminated
     * <strong>after</strong> your application is shut down and there's no chance of calling
     * {@link #watch(Thread, Runnable)} afterwards.
     *
     * @return {@code true} if and only if the watcher thread has been terminated
     */

    /**
     * 等待直到此监视器的线程没有要监视的线程并自行终止。
     * 由于在调用 {@link #watch(Thread, Runnable)} 时会再次启动一个新的监视器线程，
     * 此操作仅在你希望确保在应用程序关闭后并且没有机会再次调用 {@link #watch(Thread, Runnable)} 时，
     * 监视器线程已被终止的情况下有用。
     *
     * @return {@code true} 当且仅当监视器线程已被终止
     */
    public static boolean awaitInactivity(long timeout, TimeUnit unit) throws InterruptedException {
        ObjectUtil.checkNotNull(unit, "unit");

        Thread watcherThread = ThreadDeathWatcher.watcherThread;
        if (watcherThread != null) {
            watcherThread.join(unit.toMillis(timeout));
            return !watcherThread.isAlive();
        } else {
            return true;
        }
    }

    private ThreadDeathWatcher() { }

    private static final class Watcher implements Runnable {

        private final List<Entry> watchees = new ArrayList<Entry>();

        @Override
        public void run() {
            for (;;) {
                fetchWatchees();
                notifyWatchees();

                // Try once again just in case notifyWatchees() triggered watch() or unwatch().

                // 再试一次，以防 notifyWatchees() 触发了 watch() 或 unwatch()。
                fetchWatchees();
                notifyWatchees();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    // Ignore the interrupt; do not terminate until all tasks are run.
                    // 忽略中断；在所有任务运行完毕之前不要终止。
                }

                if (watchees.isEmpty() && pendingEntries.isEmpty()) {

                    // Mark the current worker thread as stopped.

                    // 将当前工作线程标记为已停止。
                    // The following CAS must always success and must be uncontended,
                    // 以下CAS必须始终成功且必须无竞争，
                    // because only one watcher thread should be running at the same time.
                    // 因为同一时间应该只有一个监听线程在运行。
                    boolean stopped = started.compareAndSet(true, false);
                    assert stopped;

                    // Check if there are pending entries added by watch() while we do CAS above.

                    // 检查在执行上述CAS操作时是否有由watch()添加的待处理条目。
                    if (pendingEntries.isEmpty()) {
                        // A) watch() was not invoked and thus there's nothing to handle
                        // A) watch() 未被调用，因此没有需要处理的内容
                        //    -> safe to terminate because there's nothing left to do
                        //    -> 安全终止，因为没有其他事情可做
                        // B) a new watcher thread started and handled them all
                        // B) 一个新的监视器线程启动并处理了所有这些
                        //    -> safe to terminate the new watcher thread will take care the rest
                        //    -> 安全终止，新的监视器线程将处理其余部分
                        break;
                    }

                    // There are pending entries again, added by watch()

                    // 又有待处理的条目了，由 watch() 添加
                    if (!started.compareAndSet(false, true)) {
                        // watch() started a new watcher thread and set 'started' to true.
                        // watch() 启动了一个新的监视器线程并将 'started' 设置为 true。
                        // -> terminate this thread so that the new watcher reads from pendingEntries exclusively.
                        // -> 终止此线程，以便新的观察者仅从pendingEntries读取。
                        break;
                    }

                    // watch() added an entry, but this worker was faster to set 'started' to true.

                    // watch() 添加了一个条目，但这个 worker 更快地将 'started' 设置为 true。
                    // i.e. a new watcher thread was not started
                    // 即未启动新的观察者线程
                    // -> keep this thread alive to handle the newly added entries.
                    // -> 保持此线程存活以处理新添加的条目。
                }
            }
        }

        private void fetchWatchees() {
            for (;;) {
                Entry e = pendingEntries.poll();
                if (e == null) {
                    break;
                }

                if (e.isWatch) {
                    watchees.add(e);
                } else {
                    watchees.remove(e);
                }
            }
        }

        private void notifyWatchees() {
            List<Entry> watchees = this.watchees;
            for (int i = 0; i < watchees.size();) {
                Entry e = watchees.get(i);
                if (!e.thread.isAlive()) {
                    watchees.remove(i);
                    try {
                        e.task.run();
                    } catch (Throwable t) {
                        logger.warn("Thread death watcher task raised an exception:", t);
                    }
                } else {
                    i ++;
                }
            }
        }
    }

    private static final class Entry {
        final Thread thread;
        final Runnable task;
        final boolean isWatch;

        Entry(Thread thread, Runnable task, boolean isWatch) {
            this.thread = thread;
            this.task = task;
            this.isWatch = isWatch;
        }

        @Override
        public int hashCode() {
            return thread.hashCode() ^ task.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof Entry)) {
                return false;
            }

            Entry that = (Entry) obj;
            return thread == that.thread && task == that.task;
        }
    }
}
