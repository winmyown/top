package org.top.java.concurrent.source;

import sun.security.util.SecurityConstants;

import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午3:26
 */

/**
 * <p>为 <code>Executor</code>、<code>ExecutorService</code>、<code>ScheduledExecutorService</code>、<code>ThreadFactory</code> 和 <code>Callable</code> 类提供工厂和工具方法的类定义在此包中。该类支持以下类型的方法：</p>
 * <ul>
 * <li>创建并返回具有常用配置设置的 <code>ExecutorService</code> 的方法。</li>
 * <li>创建并返回具有常用配置设置的 <code>ScheduledExecutorService</code> 的方法。</li>
 * <li>创建并返回一个“包装”后的 <code>ExecutorService</code> 的方法，该 <code>ExecutorService</code> 通过使特定实现的方法不可访问来禁用重新配置。</li>
 * <li>创建并返回一个 <code>ThreadFactory</code> 的方法，该 <code>ThreadFactory</code> 将新创建的线程设置为已知状态。</li>
 * <li>创建并返回一个 <code>Callable</code> 对象的方法，将其他类似闭包的形式转换为 <code>Callable</code>，使它们可以用于需要 <code>Callable</code> 的执行方法。</li>
 *
 * </ul>
 * <p><strong>自：</strong>
 * 1.5</p>
 * <p><strong>作者：</strong>
 * Doug Lea</p>
 */
public class Executors {

    /**
     * 创建一个线程池，该线程池重用固定数量的线程，操作一个共享的无界队列。在任何时候，最多有 {@code nThreads} 个线程正在处理任务。
     * 如果所有线程都在运行时提交了其他任务，它们将会在队列中等待，直到有线程可用。如果在关闭之前，任何线程在执行期间因失败而终止，
     * 则需要时将创建一个新线程来执行后续任务。池中的线程将一直存在，直到显式地调用 {@link ExecutorService#shutdown shutdown}。
     *
     * @param nThreads 池中的线程数量
     * @return 新创建的线程池
     * @throws IllegalArgumentException 如果 {@code nThreads <= 0}
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 创建一个线程池，该线程池维护足够的线程以支持给定的并行级别，并可能使用多个队列以减少争用。
     * 并行级别对应于同时参与或可参与任务处理的最大线程数。实际的线程数量可能动态增长和缩减。
     * 工作窃取池不保证任务按提交顺序执行。
     *
     * @param parallelism 目标并行级别
     * @return 新创建的线程池
     * @throws IllegalArgumentException 如果 {@code parallelism <= 0}
     * @since 1.8
     */
    public static ExecutorService newWorkStealingPool(int parallelism) {
        return new ForkJoinPool
                (parallelism,
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                        null, true);
    }

    /**
     * 创建一个工作窃取线程池，使用所有 {@link Runtime#availableProcessors 可用处理器}
     * 作为其目标并行级别。
     * @return 新创建的线程池
     * @see #newWorkStealingPool(int)
     * @since 1.8
     */
    public static ExecutorService newWorkStealingPool() {
        return new ForkJoinPool
                (Runtime.getRuntime().availableProcessors(),
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                        null, true);
    }

    /**
     * 创建一个线程池，该线程池重用固定数量的线程，操作一个共享的无界队列，使用提供的 ThreadFactory 在需要时创建新线程。
     * 在任何时候，最多有 {@code nThreads} 个线程正在处理任务。如果所有线程都在运行时提交了其他任务，
     * 它们将会在队列中等待，直到有线程可用。如果在关闭之前，任何线程在执行期间因失败而终止，
     * 则需要时将创建一个新线程来执行后续任务。池中的线程将一直存在，直到显式地调用 {@link ExecutorService#shutdown shutdown}。
     *
     * @param nThreads 池中的线程数量
     * @param threadFactory 创建新线程时使用的工厂
     * @return 新创建的线程池
     * @throws NullPointerException 如果 threadFactory 为 null
     * @throws IllegalArgumentException 如果 {@code nThreads <= 0}
     */
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                threadFactory);
    }

    /**
     * 创建一个执行器，它使用单个工作线程操作一个无界队列。
     * （但请注意，如果该单线程在关闭之前因执行失败而终止，则在需要时会创建一个新线程来执行后续任务。）
     * 任务将保证按顺序执行，并且在任何给定时间不会有多个任务同时处于活动状态。
     * 与功能上相当的 {@code newFixedThreadPool(1)} 不同，返回的执行器保证不能重新配置为使用额外的线程。
     *
     * @return 新创建的单线程执行器
     */
    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
                (new ThreadPoolExecutor(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>()));
    }

    /**
     * 创建一个执行器，它使用单个工作线程操作一个无界队列，并使用提供的 ThreadFactory 在需要时创建新线程。
     * 与功能上相当的 {@code newFixedThreadPool(1, threadFactory)} 不同，返回的执行器保证不能重新配置为使用额外的线程。
     *
     * @param threadFactory 创建新线程时使用的工厂
     * @return 新创建的单线程执行器
     * @throws NullPointerException 如果 threadFactory 为 null
     */
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableDelegatedExecutorService
                (new ThreadPoolExecutor(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        threadFactory));
    }

    /**
     * 创建一个线程池，该线程池根据需要创建新线程，但会重用以前构建的线程（如果它们可用）。
     * 这些池通常可以提高执行许多短暂异步任务的程序的性能。调用 {@code execute} 时，
     * 如果有可用的先前构建的线程，它们将被重用。如果没有现有线程可用，则会创建一个新线程并将其添加到池中。
     * 没有使用超过 60 秒的线程将被终止并从缓存中移除。因此，长时间空闲的池不会消耗任何资源。
     * 请注意，具有类似属性但细节不同（例如超时参数）的池可以通过 {@link ThreadPoolExecutor} 构造函数创建。
     *
     * @return 新创建的线程池
     */
    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    /**
     * 创建一个线程池，该线程池根据需要创建新线程，但会重用以前构建的线程（如果它们可用），
     * 并使用提供的 ThreadFactory 创建新线程（如果需要）。
     *
     * @param threadFactory 创建新线程时使用的工厂
     * @return 新创建的线程池
     * @throws NullPointerException 如果 threadFactory 为 null
     */
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                threadFactory);
    }

    /**
     * 创建一个单线程的执行器，它可以调度命令在给定的延迟后运行，或者定期执行。
     * （但请注意，如果该单线程在关闭之前因执行失败而终止，则在需要时会创建一个新线程来执行后续任务。）
     * 任务将保证按顺序执行，并且在任何给定时间不会有多个任务同时处于活动状态。
     * 与功能上相当的 {@code newScheduledThreadPool(1)} 不同，返回的执行器保证不能重新配置为使用额外的线程。
     *
     * @return 新创建的单线程调度执行器
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new DelegatedScheduledExecutorService
                (new ScheduledThreadPoolExecutor(1));
    }

    /**
     * 创建一个单线程的执行器，它可以调度命令在给定的延迟后运行，或者定期执行。
     * 使用提供的 ThreadFactory 创建新线程。
     * 与功能上相当的 {@code newScheduledThreadPool(1, threadFactory)} 不同，返回的执行器保证不能重新配置为使用额外的线程。
     *
     * @param threadFactory 创建新线程时使用的工厂
     * @return 新创建的调度执行器
     * @throws NullPointerException 如果 threadFactory 为 null
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return new DelegatedScheduledExecutorService
                (new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    /**
     * 创建一个线程池，该线程池可以调度命令在给定的延迟后运行，或者定期执行。
     *
     * @param corePoolSize 即使空闲，池中保持的线程数量
     * @return 新创建的调度线程池
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     */
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }

    /**
     * 创建一个线程池，该线程池可以调度命令在给定的延迟后运行，或者定期执行。
     *
     * @param corePoolSize 即使空闲，池中保持的线程数量
     * @param threadFactory 创建新线程时使用的工厂
     * @return 新创建的调度线程池
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     * @throws NullPointerException 如果 threadFactory 为 null
     */
    public static ScheduledExecutorService newScheduledThreadPool(
            int corePoolSize, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    /**
     * 返回一个对象，该对象将所有定义的 {@link ExecutorService} 方法委托给给定的执行器，
     * 但不委托其他可能通过类型转换访问的方法。这提供了一种安全的方式来“冻结”配置，
     * 并禁止对给定的具体实现进行调整。
     *
     * @param executor 底层实现
     * @return 一个 {@code ExecutorService} 实例
     * @throws NullPointerException 如果 executor 为 null
     */
    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        if (executor == null)
            throw new NullPointerException();
        return new DelegatedExecutorService(executor);
    }

    /**
     * 返回一个对象，该对象将所有定义的 {@link ScheduledExecutorService} 方法委托给给定的执行器，
     * 但不委托其他可能通过类型转换访问的方法。这提供了一种安全的方式来“冻结”配置，
     * 并禁止对给定的具体实现进行调整。
     *
     * @param executor 底层实现
     * @return 一个 {@code ScheduledExecutorService} 实例
     * @throws NullPointerException 如果 executor 为 null
     */
    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        if (executor == null)
            throw new NullPointerException();
        return new DelegatedScheduledExecutorService(executor);
    }

    /**
     * 返回一个默认的线程工厂，用于创建新线程。
     * 该工厂创建的所有新线程都在同一个 {@link ThreadGroup} 中。如果有 {@link java.lang.SecurityManager}，
     * 它使用 {@link System#getSecurityManager} 的组，否则使用调用此 {@code defaultThreadFactory} 方法的线程组。
     * 每个新线程都是一个非守护线程，优先级设置为 {@code Thread.NORM_PRIORITY} 和线程组允许的最大优先级中较小的一个。
     * 新线程的名称可以通过 {@link Thread#getName} 访问，格式为 <em>pool-N-thread-M</em>，
     * 其中 <em>N</em> 是此工厂的序列号，<em>M</em> 是此工厂创建的线程的序列号。
     *
     * @return 一个线程工厂
     */
    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    /**
     * 返回一个线程工厂，用于创建与当前线程具有相同权限的新线程。
     * 此工厂创建的线程与 {@link Executors#defaultThreadFactory} 相同的设置，
     * 并且另外设置新线程的 `AccessControlContext` 和 `contextClassLoader`
     * 与调用此 {@code privilegedThreadFactory} 方法的线程相同。
     * 新的 {@code privilegedThreadFactory} 可以在
     * {@link AccessController#doPrivileged AccessController.doPrivileged}
     * 操作中创建，设置当前线程的访问控制上下文，以创建具有该操作中持有的选定权限设置的线程。
     *
     * <p>注意，虽然在这些线程中运行的任务将具有与当前线程相同的访问控制和类加载器设置，
     * 但它们不一定具有相同的 {@link java.lang.ThreadLocal} 或 {@link java.lang.InheritableThreadLocal} 值。
     * 如果需要，可以在任何任务运行之前，在 {@link ThreadPoolExecutor} 的子类中使用
     * {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)} 方法设置或重置特定的线程本地值。
     * 此外，如果有必要初始化工作线程，使其具有与其他指定线程相同的 InheritableThreadLocal 设置，
     * 则可以创建一个自定义的 ThreadFactory，在该线程等待并处理创建其他继承其值的请求。
     *
     * @return 一个线程工厂
     * @throws AccessControlException 如果当前访问控制上下文没有权限获取和设置上下文类加载器
     */
    public static ThreadFactory privilegedThreadFactory() {
        return new PrivilegedThreadFactory();
    }

    /**
     * 返回一个 {@link Callable} 对象，当被调用时，将运行给定的任务并返回给定的结果。
     * 当将需要 {@code Callable} 的方法应用于本质上无返回值的操作时，这可能会很有用。
     *
     * @param task 要运行的任务
     * @param result 要返回的结果
     * @param <T> 返回结果的类型
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 task 为 null
     */
    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null)
            throw new NullPointerException();
        return new RunnableAdapter<T>(task, result);
    }

    /**
     * 返回一个 {@link Callable} 对象，当被调用时，将运行给定的任务并返回 {@code null}。
     *
     * @param task 要运行的任务
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 task 为 null
     */
    public static Callable<Object> callable(Runnable task) {
        if (task == null)
            throw new NullPointerException();
        return new RunnableAdapter<Object>(task, null);
    }

    /**
     * 返回一个 {@link Callable} 对象，当被调用时，将运行给定的特权操作并返回其结果。
     *
     * @param action 要运行的特权操作
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 action 为 null
     */
    public static Callable<Object> callable(final PrivilegedAction<?> action) {
        if (action == null)
            throw new NullPointerException();
        return new Callable<Object>() {
            public Object call() { return action.run(); }};
    }

    /**
     * 返回一个 {@link Callable} 对象，当被调用时，将运行给定的特权异常操作并返回其结果。
     *
     * @param action 要运行的特权异常操作
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 action 为 null
     */
    public static Callable<Object> callable(final PrivilegedExceptionAction<?> action) {
        if (action == null)
            throw new NullPointerException();
        return new Callable<Object>() {
            public Object call() throws Exception { return action.run(); }};
    }

    /**
     * 返回一个 {@link Callable} 对象，该对象在被调用时，将在当前访问控制上下文下执行给定的 {@code callable}。
     * 此方法通常应在 {@link AccessController#doPrivileged} 操作中调用，以创建在可能的情况下在该操作中持有的选定权限设置下执行的 callable；
     * 否则，将抛出关联的 {@link AccessControlException}。
     *
     * @param callable 底层任务
     * @param <T> callable 返回结果的类型
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 callable 为 null
     */
    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        if (callable == null)
            throw new NullPointerException();
        return new PrivilegedCallable<T>(callable);
    }

    /**
     * 返回一个 {@link Callable} 对象，该对象在被调用时，将在当前访问控制上下文下，
     * 并使用当前的类加载器作为上下文类加载器执行给定的 {@code callable}。
     * 此方法通常应在 {@link AccessController#doPrivileged AccessController.doPrivileged}
     * 操作中调用，以创建在可能的情况下在该操作中持有的选定权限设置下执行的 callable；
     * 如果无法执行，将抛出相关的 {@link AccessControlException}。
     *
     * @param callable 底层任务
     * @param <T> callable 返回结果的类型
     * @return 一个 callable 对象
     * @throws NullPointerException 如果 callable 为 null
     * @throws AccessControlException 如果当前访问控制上下文没有权限设置和获取上下文类加载器
     */
    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        if (callable == null)
            throw new NullPointerException();
        return new PrivilegedCallableUsingCurrentClassLoader<T>(callable);
    }

    // 私有类支持公共方法

    /**
     * 一个 callable 对象，运行给定的任务并返回给定的结果
     */
    static final class RunnableAdapter<T> implements Callable<T> {
        final Runnable task;
        final T result;
        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }
        public T call() {
            task.run();
            return result;
        }
    }

    /**
     * 一个在已建立的访问控制设置下运行的 callable 对象
     */
    static final class PrivilegedCallable<T> implements Callable<T> {
        private final Callable<T> task;
        private final AccessControlContext acc;

        PrivilegedCallable(Callable<T> task) {
            this.task = task;
            this.acc = AccessController.getContext();
        }

        public T call() throws Exception {
            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<T>() {
                            public T run() throws Exception {
                                return task.call();
                            }
                        }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    /**
     * 一个在已建立的访问控制设置和当前类加载器下运行的 callable 对象
     */
    static final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {
        private final Callable<T> task;
        private final AccessControlContext acc;
        private final ClassLoader ccl;

        PrivilegedCallableUsingCurrentClassLoader(Callable<T> task) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
                sm.checkPermission(new RuntimePermission("setContextClassLoader"));
            }
            this.task = task;
            this.acc = AccessController.getContext();
            this.ccl = Thread.currentThread().getContextClassLoader();
        }

        public T call() throws Exception {
            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<T>() {
                            public T run() throws Exception {
                                Thread t = Thread.currentThread();
                                ClassLoader cl = t.getContextClassLoader();
                                if (ccl == cl) {
                                    return task.call();
                                } else {
                                    t.setContextClassLoader(ccl);
                                    try {
                                        return task.call();
                                    } finally {
                                        t.setContextClassLoader(cl);
                                    }
                                }
                            }
                        }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    /**
     * 默认线程工厂
     */
    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    /**
     * 一个线程工厂，用于捕获访问控制上下文和类加载器。
     */
    static class PrivilegedThreadFactory extends DefaultThreadFactory {
        private final AccessControlContext acc;
        private final ClassLoader ccl;

        PrivilegedThreadFactory() {
            super();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // 从这个类调用 getContextClassLoader 不会触发安全检查，
                // 但我们还是检查调用者是否拥有这个权限。
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);

                // 快速失败
                sm.checkPermission(new RuntimePermission("setContextClassLoader"));
            }
            this.acc = AccessController.getContext();
            this.ccl = Thread.currentThread().getContextClassLoader();
        }

        public Thread newThread(final Runnable r) {
            return super.newThread(new Runnable() {
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        public Void run() {
                            Thread.currentThread().setContextClassLoader(ccl);
                            r.run();
                            return null;
                        }
                    }, acc);
                }
            });
        }
    }

    /**
     * 一个包装类，仅公开 ExecutorService 实现的 ExecutorService 方法。
     */
    static class DelegatedExecutorService extends AbstractExecutorService {
        private final ExecutorService e;
        DelegatedExecutorService(ExecutorService executor) { e = executor; }
        public void execute(Runnable command) { e.execute(command); }
        public void shutdown() { e.shutdown(); }
        public List<Runnable> shutdownNow() { return e.shutdownNow(); }
        public boolean isShutdown() { return e.isShutdown(); }
        public boolean isTerminated() { return e.isTerminated(); }
        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }
        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }
        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }
        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return e.invokeAll(tasks);
        }
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                             long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    /**
     * 一个可终结的 DelegatedExecutorService。
     */
    static class FinalizableDelegatedExecutorService
            extends DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }
        protected void finalize() {
            super.shutdown();
        }
    }

    /**
     * 一个包装类，仅公开 ScheduledExecutorService 实现的 ScheduledExecutorService 方法。
     */
    static class DelegatedScheduledExecutorService
            extends DelegatedExecutorService
            implements ScheduledExecutorService {
        private final ScheduledExecutorService e;
        DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
            super(executor);
            e = executor;
        }
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return e.schedule(command, delay, unit);
        }
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return e.schedule(callable, delay, unit);
        }
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return e.scheduleAtFixedRate(command, initialDelay, period, unit);
        }
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }


    /** 无法实例化。 */
    private Executors() {}
}



