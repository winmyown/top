
package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.concurrent.FastThreadLocalThread;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.util.internal.SystemPropertyUtil.getInt;
import static java.lang.Math.max;

/**
 * Allows a way to register some {@link Runnable} that will executed once there are no references to an {@link Object}
 * anymore.
 */

/**
 * 提供了一种注册 {@link Runnable} 的方法，当某个 {@link Object} 不再被引用时，该 Runnable 将会被执行。
 */
public final class ObjectCleaner {
    private static final int REFERENCE_QUEUE_POLL_TIMEOUT_MS =
            max(500, getInt("io.netty.util.internal.ObjectCleaner.refQueuePollTimeout", 10000));

    // Package-private for testing

    // 包内私有，用于测试
    static final String CLEANER_THREAD_NAME = ObjectCleaner.class.getSimpleName() + "Thread";
    // This will hold a reference to the AutomaticCleanerReference which will be removed once we called cleanup()
    // 这将持有对 AutomaticCleanerReference 的引用，该引用将在调用 cleanup() 后被移除
    private static final Set<AutomaticCleanerReference> LIVE_SET = new ConcurrentSet<AutomaticCleanerReference>();
    private static final ReferenceQueue<Object> REFERENCE_QUEUE = new ReferenceQueue<Object>();
    private static final AtomicBoolean CLEANER_RUNNING = new AtomicBoolean(false);
    private static final Runnable CLEANER_TASK = new Runnable() {
        @Override
        public void run() {
            boolean interrupted = false;
            for (;;) {
                // Keep on processing as long as the LIVE_SET is not empty and once it becomes empty
                // 只要 LIVE_SET 不为空就继续处理，一旦它变为空
                // See if we can let this thread complete.
                // 看看我们能否让这个线程完成。
                while (!LIVE_SET.isEmpty()) {
                    final AutomaticCleanerReference reference;
                    try {
                        reference = (AutomaticCleanerReference) REFERENCE_QUEUE.remove(REFERENCE_QUEUE_POLL_TIMEOUT_MS);
                    } catch (InterruptedException ex) {
                        // Just consume and move on
                        // 只需消费并继续前进
                        interrupted = true;
                        continue;
                    }
                    if (reference != null) {
                        try {
                            reference.cleanup();
                        } catch (Throwable ignored) {
                            // ignore exceptions, and don't log in case the logger throws an exception, blocks, or has
                            // 忽略异常，如果记录器抛出异常、阻塞或
                            // other unexpected side effects.
                            // 其他意外的副作用。
                        }
                        LIVE_SET.remove(reference);
                    }
                }
                CLEANER_RUNNING.set(false);

                // Its important to first access the LIVE_SET and then CLEANER_RUNNING to ensure correct

                // 首先访问LIVE_SET，然后访问CLEANER_RUNNING以确保正确
                // behavior in multi-threaded environments.
                // 多线程环境中的行为。
                if (LIVE_SET.isEmpty() || !CLEANER_RUNNING.compareAndSet(false, true)) {
                    // There was nothing added after we set STARTED to false or some other cleanup Thread
                    // 在我们将STARTED设置为false或进行其他清理线程操作后，没有添加任何内容
                    // was started already so its safe to let this Thread complete now.
                    // 已经启动，所以现在可以让这个线程安全地完成了。
                    break;
                }
            }
            if (interrupted) {
                // As we caught the InterruptedException above we should mark the Thread as interrupted.
                // 由于我们捕获了上面的InterruptedException，应该将线程标记为已中断。
                Thread.currentThread().interrupt();
            }
        }
    };

    /**
     * Register the given {@link Object} for which the {@link Runnable} will be executed once there are no references
     * to the object anymore.
     *
     * This should only be used if there are no other ways to execute some cleanup once the Object is not reachable
     * anymore because it is not a cheap way to handle the cleanup.
     */

    /**
     * 注册给定的 {@link Object}，当该对象不再有引用时，将执行 {@link Runnable}。
     *
     * 仅当没有其他方法可以在对象不再可达时执行清理操作时，才应使用此方法，因为这不是一种廉价的清理处理方式。
     */
    public static void register(Object object, Runnable cleanupTask) {
        AutomaticCleanerReference reference = new AutomaticCleanerReference(object,
                ObjectUtil.checkNotNull(cleanupTask, "cleanupTask"));
        // Its important to add the reference to the LIVE_SET before we access CLEANER_RUNNING to ensure correct
        // 在访问 CLEANER_RUNNING 之前，添加对 LIVE_SET 的引用非常重要，以确保正确性
        // behavior in multi-threaded environments.
        // 多线程环境中的行为。
        LIVE_SET.add(reference);

        // Check if there is already a cleaner running.

        // 检查是否已经有清理程序在运行。
        if (CLEANER_RUNNING.compareAndSet(false, true)) {
            final Thread cleanupThread = new FastThreadLocalThread(CLEANER_TASK);
            cleanupThread.setPriority(Thread.MIN_PRIORITY);
            // Set to null to ensure we not create classloader leaks by holding a strong reference to the inherited
            // 设置为null以确保我们不会通过持有对继承的强引用来创建类加载器泄漏
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
                    cleanupThread.setContextClassLoader(null);
                    return null;
                }
            });
            cleanupThread.setName(CLEANER_THREAD_NAME);

            // Mark this as a daemon thread to ensure that we the JVM can exit if this is the only thread that is

            // 将此标记为守护线程，以确保如果这是唯一的线程，JVM可以退出
            // running.
            // 运行中。
            cleanupThread.setDaemon(true);
            cleanupThread.start();
        }
    }

    public static int getLiveSetCount() {
        return LIVE_SET.size();
    }

    private ObjectCleaner() {
        // Only contains a static method.
        // 仅包含一个静态方法。
    }

    private static final class AutomaticCleanerReference extends WeakReference<Object> {
        private final Runnable cleanupTask;

        AutomaticCleanerReference(Object referent, Runnable cleanupTask) {
            super(referent, REFERENCE_QUEUE);
            this.cleanupTask = cleanupTask;
        }

        void cleanup() {
            cleanupTask.run();
        }

        @Override
        public Thread get() {
            return null;
        }

        @Override
        public void clear() {
            LIVE_SET.remove(this);
            super.clear();
        }
    }
}
