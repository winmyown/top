package org.top.java.source.concurrent;

import org.top.java.source.concurrent.atomic.AtomicInteger;
import org.top.java.source.concurrent.locks.AbstractQueuedSynchronizer;
import org.top.java.source.concurrent.locks.ReentrantLock;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午2:37
 */

/**
 *  <p>一个 <code>ExecutorService</code>，它使用可能多个线程池中的一个执行每个提交的任务，通常通过 <code>Executors</code> 工厂方法进行配置。</p>
 * <p>线程池解决了两个不同的问题：它们通常通过减少每个任务调用的开销来提高执行大量异步任务时的性能，并且它们提供了一种限制和管理资源（包括线程）的方法，这些资源在执行一组任务时被消耗。每个 <code>ThreadPoolExecutor</code> 还维护了一些基本统计信息，例如已完成任务的数量。</p>
 * <p>为了在广泛的上下文中有用，此类提供了许多可调参数和扩展钩子。然而，强烈建议程序员使用更方便的 <code>Executors</code> 工厂方法，如 <code>Executors.newCachedThreadPool</code>（无界线程池，具有自动线程回收功能）、<code>Executors.newFixedThreadPool</code>（固定大小的线程池）和 <code>Executors.newSingleThreadExecutor</code>（单个后台线程），这些方法为最常见的使用场景预先配置了设置。否则，在手动配置和调整此类时，请遵循以下指南：</p>
 * <h4 id='核心池大小和最大池大小'>核心池大小和最大池大小</h4>
 * <p><code>ThreadPoolExecutor</code> 将根据 <code>corePoolSize</code>（参见 <code>getCorePoolSize</code>）和 <code>maximumPoolSize</code>（参见 <code>getMaximumPoolSize</code>）设定的界限自动调整池大小（参见 <code>getPoolSize</code>）。当在 <code>execute(Runnable)</code> 方法中提交新任务时，如果运行的线程少于 <code>corePoolSize</code>，则会创建一个新线程来处理请求，即使其他工作线程处于空闲状态。如果运行的线程多于 <code>corePoolSize</code> 但少于 <code>maximumPoolSize</code>，则仅当队列已满时才会创建新线程。通过将 <code>corePoolSize</code> 和 <code>maximumPoolSize</code> 设置为相同值，您可以创建一个固定大小的线程池。通过将 <code>maximumPoolSize</code> 设置为本质上无界的值（如 <code>Integer.MAX_VALUE</code>），您可以允许池容纳任意数量的并发任务。通常，核心和最大池大小仅在构造时设置，但也可以使用 <code>setCorePoolSize</code> 和 <code>setMaximumPoolSize</code> 动态更改它们。</p>
 * <h4 id='按需构造'>按需构造</h4>
 * <p>默认情况下，即使是核心线程，最初也只在有新任务到达时才创建和启动，但可以通过 <code>prestartCoreThread</code> 或 <code>prestartAllCoreThreads</code> 方法动态覆盖此行为。如果您使用非空队列构造池，可能希望预先启动线程。</p>
 * <h4 id='创建新线程'>创建新线程</h4>
 * <p>新线程是使用 <code>ThreadFactory</code> 创建的。如果没有另外指定，则使用 <code>Executors.defaultThreadFactory</code>，它创建的线程都在同一个 <code>ThreadGroup</code> 中，具有相同的 <code>NORM_PRIORITY</code> 优先级和非守护线程状态。通过提供不同的 <code>ThreadFactory</code>，可以更改线程的名称、线程组、优先级、守护线程状态等。如果 <code>ThreadFactory</code> 在被请求时返回 <code>null</code>，从而未能创建线程，执行器将继续运行，但可能无法执行任何任务。线程应具备 <code>modifyThread</code> 运行时权限。如果工作线程或使用该池的其他线程不具备此权限，服务可能会降级：配置更改可能不会及时生效，关闭的池可能会处于可能终止但尚未完成的状态。</p>
 * <h4 id='保持活动时间'>保持活动时间</h4>
 * <p>如果池当前拥有超过 <code>corePoolSize</code> 的线程，则多余的线程在空闲超过 <code>keepAliveTime</code>（参见 <code>getKeepAliveTime(TimeUnit)</code>）后将被终止。这提供了一种在池未被积极使用时减少资源消耗的方法。如果池稍后变得更加活跃，将构建新线程。此参数也可以使用 <code>setKeepAliveTime(long, TimeUnit)</code> 方法动态更改。使用 <code>Long.MAX_VALUE</code> 和 <code>TimeUnit.NANOSECONDS</code> 的值实际上禁止空闲线程在关闭之前终止。默认情况下，保持活动策略仅在线程数超过 <code>corePoolSize</code> 时适用。但只要 <code>keepAliveTime</code> 值不为零，<code>allowCoreThreadTimeOut(boolean)</code> 方法可以用于将此超时策略应用于核心线程。</p>
 * <h4 id='排队'>排队</h4>
 * <p>任何 <code>BlockingQueue</code> 都可以用于传递和保存提交的任务。此队列的使用与池大小调整相关：</p>
 * <ul>
 * <li>如果运行的线程少于 <code>corePoolSize</code>，执行器总是优先添加一个新线程，而不是将任务排队。</li>
 * <li>如果运行的线程数量达到 <code>corePoolSize</code> 或更多，执行器总是优先将任务排队，而不是添加新线程。</li>
 * <li>如果无法排队，则会创建一个新线程，除非这会超过 <code>maximumPoolSize</code>，在这种情况下，任务将被拒绝。</li>
 *
 * </ul>
 * <p>有三种常见的排队策略：</p>
 * <ul>
 * <li><strong>直接传递</strong>。一个好的工作队列默认选择是 <code>SynchronousQueue</code>，它将任务直接传递给线程而不另行保存。如果没有线程可以立即执行任务，排队任务的尝试将失败，因此将构建新线程。此策略避免了处理可能具有内部依赖关系的一组请求时的死锁。直接传递通常需要无界的 <code>maximumPoolSize</code>，以避免新提交任务的拒绝。这反过来又可能导致线程的无界增长，尤其是在命令到达速度平均快于处理速度时。</li>
 * <li><strong>无界队列</strong>。使用无界队列（例如没有预定义容量的 <code>LinkedBlockingQueue</code>）会使新任务在所有核心线程忙碌时等待在队列中。因此，创建的线程数量不会超过 <code>corePoolSize</code>。（因此，<code>maximumPoolSize</code> 的值不会生效。）这在每个任务相互独立时可能合适，例如在网页服务器中。虽然这种排队方式有助于平滑瞬时的请求激增，但它可能导致工作队列的无界增长，特别是当任务到达速度平均快于处理速度时。</li>
 * <li><strong>有界队列</strong>。有界队列（例如 <code>ArrayBlockingQueue</code>）在与有限的 <code>maximumPoolSize</code> 配合使用时有助于防止资源耗尽，但可能更难调整和控制。队列大小和最大池大小可以相互权衡：使用较大的队列和较小的池可以最小化 CPU 使用率、操作系统资源和上下文切换开销，但可能导致吞吐量异常低。如果任务经常阻塞（例如，如果它们是 I/O 密集型任务），系统可能能够调度更多线程的时间超出您允许的限制。使用较小队列通常需要较大的池，这会使 CPU 保持更忙碌，但也可能遇到不可接受的调度开销，从而也会减少吞吐量。</li>
 *
 * </ul>
 * <h4 id='被拒绝的任务'>被拒绝的任务</h4>
 * <p>当执行器已关闭，或执行器使用有限的线程数和工作队列容量且饱和时，方法 <code>execute(Runnable)</code> 中提交的新任务将被拒绝。在这两种情况下，<code>execute</code> 方法会调用其 <code>RejectedExecutionHandler</code> 的 <code>rejectedExecution(Runnable, ThreadPoolExecutor)</code> 方法。提供了四种预定义的处理程序策略：</p>
 * <ul>
 * <li>在默认的 <code>ThreadPoolExecutor.AbortPolicy</code> 中，处理程序在拒绝时抛出 <code>RejectedExecutionException</code> 运行时异常。</li>
 * <li>在 <code>ThreadPoolExecutor.CallerRunsPolicy</code> 中，调用 <code>execute</code> 的线程会自己运行任务。这提供了一种简单的反馈控制机制，该机制将减慢新任务的提交速度。</li>
 * <li>在 <code>ThreadPoolExecutor.DiscardPolicy</code> 中，无法执行的任务将被简单丢弃。</li>
 * <li>在 <code>ThreadPoolExecutor.DiscardOldestPolicy</code> 中，如果执行器没有关闭，则将丢弃工作队列头部的任务，然后重试执行（这可能再次失败，导致重复此操作）。</li>
 *
 * </ul>
 * <p>可以定义和使用其他类型的 <code>RejectedExecutionHandler</code> 类。这样做时需要特别注意，尤其是在策略设计仅在特定容量或排队策略下工作时。</p>
 * <h4 id='钩子方法'>钩子方法</h4>
 * <p>此类提供了受保护的可覆盖的 <code>beforeExecute(Thread, Runnable)</code> 和 <code>afterExecute(Runnable, Throwable)</code> 方法，这些方法在每个任务执行前后调用。可以使用这些方法操作执行环境；例如，重新初始化 <code>ThreadLocals</code>，收集统计数据或添加日志条目。此外，还可以重写 <code>terminated</code> 方法，以在执行器完全终止后执行任何特殊处理。</p>
 * <p>如果钩子或回调方法抛出异常，内部工作线程可能会因此失败并突然终止。</p>
 * <h4 id='队列维护'>队列维护</h4>
 * <p><code>getQueue()</code> 方法允许访问工作队列，供监控和调试之用。强烈建议不要将此方法用于其他目的。当大量排队任务被取消时，可以使用提供的 <code>remove(Runnable)</code> 和 <code>purge</code> 方法来帮助存储回收。</p>
 * <h4 id='终结'>终结</h4>
 * <p>不再被程序引用且没有剩余线程的池将自动关闭。如果希望确保未引用的池被回收，即使用户忘记调用 <code>shutdown</code>，则必须通过设置适当的保持活动时间、使用 0 核心线程的下限和/或设置 <code>allowCoreThreadTimeOut(boolean)</code>，确保未使用的线程最终死亡。</p>
 * <h4 id='扩展示例'>扩展示例</h4>
 * <p>此类</p>
 * <p>的大多数扩展都会覆盖一个或多个受保护的钩子方法。例如，以下是一个添加了简单暂停/恢复功能的子类：</p>
 * <pre><code class='language-java' lang='java'>class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *     private boolean isPaused;
 *     private ReentrantLock pauseLock = new ReentrantLock();
 *     private Condition unpaused = pauseLock.newCondition();
 *
 *     public PausableThreadPoolExecutor(...) {
 *         super(...);
 *     }
 *
 *     protected void beforeExecute(Thread t, Runnable r) {
 *         super.beforeExecute(t, r);
 *         pauseLock.lock();
 *         try {
 *             while (isPaused) unpaused.await();
 *         } catch (InterruptedException ie) {
 *             t.interrupt();
 *         } finally {
 *             pauseLock.unlock();
 *         }
 *     }
 *
 *     public void pause() {
 *         pauseLock.lock();
 *         try {
 *             isPaused = true;
 *         } finally {
 *             pauseLock.unlock();
 *         }
 *     }
 *
 *     public void resume() {
 *         pauseLock.lock();
 *         try {
 *             isPaused = false;
 *             unpaused.signalAll();
 *         } finally {
 *             pauseLock.unlock();
 *         }
 *     }
 * }
 * </code></pre>
 * <h4 id='自-15-起'>自 1.5 起</h4>
 * <p><strong>作者</strong>：Doug Lea</p>
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    /**
     * 主线程池控制状态 ctl，是一个原子整数，包含两个概念字段：
     *   workerCount，表示有效线程的数量
     *   runState，表示线程池的运行状态，如运行、关闭等。
     *
     * 为了将它们打包到一个 int 中，我们将 workerCount 限制为 (2^29)-1（约5亿）个线程，而不是 (2^31)-1（20亿）个。
     * 如果未来这是个问题，可以将变量改为 AtomicLong，并调整下面的移位/掩码常量。但在需要出现之前，使用 int 能使代码更快、更简单。
     *
     * workerCount 是已被允许启动且未被允许停止的工作线程数量。该值可能会暂时与实际存活线程的数量不同，例如当 ThreadFactory
     * 在请求时无法创建线程时，或者当退出线程仍在执行终止前的账目工作时。用户可见的池大小报告为当前 workers 集合的大小。
     *
     * runState 提供主要的生命周期控制，取值如下：
     *
     *   RUNNING:  接受新任务并处理排队任务
     *   SHUTDOWN: 不接受新任务，但处理排队任务
     *   STOP:     不接受新任务，不处理排队任务，并中断进行中的任务
     *   TIDYING:  所有任务都已终止，workerCount 为 0，转换到 TIDYING 状态的线程将运行 terminated() 钩子方法
     *   TERMINATED: terminated() 方法已完成
     *
     * 这些值之间的数值顺序很重要，以便进行有序比较。runState 随时间单调增加，但不一定会经历每个状态。状态转换如下：
     *
     * RUNNING -> SHUTDOWN
     *    在调用 shutdown() 时，可能隐含地在 finalize() 中
     * (RUNNING 或 SHUTDOWN) -> STOP
     *    在调用 shutdownNow() 时
     * SHUTDOWN -> TIDYING
     *    当队列和池都为空时
     * STOP -> TIDYING
     *    当池为空时
     * TIDYING -> TERMINATED
     *    当 terminated() 钩子方法已完成时
     *
     * 调用 awaitTermination() 中等待的线程将在状态到达 TERMINATED 时返回。
     *
     * 检测从 SHUTDOWN 到 TIDYING 的转换比想象的复杂，因为在 SHUTDOWN 状态下，队列可能在空和非空之间切换，
     * 但只有在看到它为空后并且 workerCount 为 0 时才可以终止（有时需要重新检查，见下文）。
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState 存储在高位中
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    // 打包和解包 ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    /*
     * 不需要解包 ctl 的位字段访问器。
     * 这些依赖于位布局以及 workerCount 从不为负。
     */
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * 尝试 CAS 增加 ctl 的 workerCount 字段。
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * 尝试 CAS 减少 ctl 的 workerCount 字段。
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * 减少 ctl 的 workerCount 字段。仅在线程突然终止时调用（参见 processWorkerExit）。其他减少在 getTask 中执行。
     */
    private void decrementWorkerCount() {
        do {} while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * 用于持有任务的队列并将其传递给工作线程。我们不要求 workQueue.poll() 返回 null 必须意味着 workQueue.isEmpty()，
     * 因此仅依赖 isEmpty 来判断队列是否为空（例如在决定是否从 SHUTDOWN 转移到 TIDYING 时必须这样做）。
     * 这适应了像 DelayQueues 这样允许 poll() 返回 null 的特殊队列，即使稍后延迟过期后可能返回非 null。
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * 锁定用于访问 workers 集合和相关的账目工作。虽然我们可以使用某种类型的并发集合，但事实证明，使用锁通常更可取。
     * 原因之一是这会串行化 interruptIdleWorkers，避免在关闭期间不必要的中断风暴，尤其是在线程退出时可能会同时中断尚未中断的线程。
     * 它还简化了一些相关的统计记录工作，比如 largestPoolSize 等。我们还在 shutdown 和 shutdownNow 时持有 mainLock，
     * 以确保在分别检查权限进行中断和实际中断时，workers 集合是稳定的。
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 包含池中所有工作线程的集合。仅在持有 mainLock 时访问。
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * 支持 awaitTermination 的等待条件。
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * 记录曾经达到的最大池大小。仅在 mainLock 下访问。
     */
    private int largestPoolSize;

    /**
     * 已完成任务的计数器。仅在工作线程终止时更新。仅在 mainLock 下访问。
     */
    private long completedTaskCount;

    /*
     * 所有用户控制参数都声明为 volatile，以便基于最新的值进行操作，但无需锁定，
     * 因为没有内部不变性取决于它们与其他操作同步变化。
     */

    /**
     * 用于创建新线程的工厂。所有线程都使用此工厂创建（通过 addWorker 方法）。
     * 所有调用方都必须准备好应对 addWorker 失败，这可能反映系统或用户限制线程数的策略。
     * 即使它不是一个错误，但无法创建线程可能会导致新任务被拒绝或现有任务滞留在队列中。
     *
     * 即使在出现如 OutOfMemoryError 等异常（通常是在线程启动时分配本机堆栈时抛出）时，
     * 我们仍然维护池的不变性。由于线程启动过程中可能会抛出此类错误，用户可能希望进行清理以关闭线程池。
     * 通常有足够的内存可用来完成清理工作，而不会遇到另一个 OutOfMemoryError。
     */
    private volatile ThreadFactory threadFactory;

    /**
     * 在执行时饱和或关闭时调用的处理程序。
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * 空闲线程等待工作的超时时间（纳秒）。当线程数量超过 corePoolSize 或者 allowCoreThreadTimeOut 时，线程会使用这个超时时间。
     * 否则，线程将永远等待新工作。
     */
    private volatile long keepAliveTime;

    /**
     * 如果为 false（默认值），即使空闲，核心线程也会保持存活。如果为 true，核心线程使用 keepAliveTime 来等待工作超时。
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * 核心池大小是保持存活的最小工作线程数（不允许超时等），除非设置了 allowCoreThreadTimeOut，在这种情况下最小值为 0。
     */
    private volatile int corePoolSize;

    /**
     * 最大池大小。请注意，实际的最大值受到 CAPACITY 限制。
     */
    private volatile int maximumPoolSize;

    /**
     * 默认的拒绝执行处理程序。
     */
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();

    /**
     * 调用 shutdown 和 shutdownNow 时需要的权限。
     * 我们还要求调用者具备权限来实际中断工作线程集中的线程（通过 Thread.interrupt 实现，这依赖于 ThreadGroup.checkAccess，
     * 而后者依赖于 SecurityManager.checkAccess）。仅当这些检查通过时，才尝试关闭。
     *
     * 所有对 Thread.interrupt 的实际调用（见 interruptIdleWorkers 和 interruptWorkers）都忽略 SecurityExceptions，
     * 这意味着尝试的中断将默默失败。在关闭时，这种失败不应该发生，除非 SecurityManager 的策略不一致，
     * 有时允许访问线程，有时不允许。在这种情况下，未能实际中断线程可能会禁用或延迟完全终止。
     * 其他使用 interruptIdleWorkers 的情况是建议性的，未能实际中断只会延迟对配置更改的响应，因此不会进行异常处理。
     */
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    /* 用于在执行 finalize 时的上下文，或 null */
    private final AccessControlContext acc;

    /**
     * Worker 类主要维护运行任务的线程的中断控制状态，以及其他一些次要的账目工作。
     * 该类巧妙地继承了 AbstractQueuedSynchronizer 以简化围绕每个任务执行的锁获取和释放。
     * 这保护了线程在等待任务时不会被不相关的中断中断。
     * 我们实现了一个简单的不可重入互斥锁，而不是使用 ReentrantLock，
     * 因为我们不希望工作线程能够在调用池控制方法（例如 setCorePoolSize）时重新获取锁。
     * 此外，为了抑制线程在实际开始运行任务之前被中断，我们将锁的状态初始化为负值，并在启动时清除它（在 runWorker 中）。
     */
    private final class Worker
            extends AbstractQueuedSynchronizer
            implements Runnable {
        /**
         * 这个类永远不会被序列化，但我们提供了 serialVersionUID 以抑制 javac 警告。
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** 运行该 worker 的线程。如果工厂创建线程失败则为 null */
        final Thread thread;
        /** 第一个要运行的任务，可能为 null */
        Runnable firstTask;
        /** 每个线程的任务计数 */
        volatile long completedTasks;

        /**
         * 使用给定的第一个任务和线程工厂创建 worker。
         * @param firstTask 第一个任务（如果没有则为 null）
         */
        Worker(Runnable firstTask) {
            setState(-1); // 抑制在 runWorker 之前的中断
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /** 将主运行循环委托给外部的 runWorker */
        public void run() {
            runWorker(this);
        }

        // 锁定方法
        //
        // 值 0 表示未锁定状态。
        // 值 1 表示已锁定状态。

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    /*
     * 设置控制状态的方法
     */

    /**
     * 将 runState 过渡到给定目标，或者如果已经达到至少给定目标，则不做更改。
     *
     * @param targetState 目标状态，可以是 SHUTDOWN 或 STOP（但不能是 TIDYING 或 TERMINATED -- 使用 tryTerminate 完成这些状态转换）
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 如果池处于 (SHUTDOWN 并且队列和池为空) 或 (STOP 并且池为空)，将状态过渡到 TERMINATED。
     * 如果其他条件满足但 workerCount 不为零，则中断一个空闲线程以确保关闭信号传播。
     * 任何可能使终止变为可能的操作（减少 worker 数量或在关闭期间从队列中移除任务）之后必须调用此方法。
     * 该方法不是私有的，以便 ScheduledThreadPoolExecutor 访问。
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||
                    runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // 有资格终止
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    /*
     * 用于控制工作线程中断的方法
     */

    /**
     * 如果存在安全管理器，确保调用者有权限关闭线程（参见 shutdownPerm）。
     * 如果通过，还需确保调用者有权限中断每个工作线程。即使第一次检查通过，这也可能不成立，
     * 如果 SecurityManager 特殊对待某些线程。
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * 中断所有线程，即使它们是活动的。忽略 SecurityExceptions（在这种情况下某些线程可能保持不中断）。
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 中断可能在等待任务的线程（即未锁定的线程），使它们能够检查终止或配置更改。
     * 忽略 SecurityExceptions（在这种情况下某些线程可能保持不中断）。
     *
     * @param onlyOne 如果为 true，则最多中断一个工作线程。仅在 tryTerminate 中调用，当终止被启用但还有其他工作线程时。
     *                在这种情况下，最多会中断一个等待中的线程以传播关闭信号。
     *                中断任意一个线程可以确保自关闭开始以来到达的线程最终退出。为了保证最终终止，仅中断一个空闲线程就足够了，
     *                但 shutdown() 会中断所有空闲线程，使多余的线程及时退出，而不是等待一个拖延任务完成。
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 通用形式的 interruptIdleWorkers，以避免记住布尔参数的意义。
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * 杂项工具，大多数也导出到 ScheduledThreadPoolExecutor
     */

    /**
     * 为给定命令调用拒绝执行处理程序。包私有供 ScheduledThreadPoolExecutor 使用。
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * 在 shutdown 调用时执行进一步清理状态转换。此处为无操作，但供 ScheduledThreadPoolExecutor 用于取消延迟任务。
     */
    void onShutdown() {
    }

    /**
     * ScheduledThreadPoolExecutor 需要的状态检查，以便在关闭期间允许运行任务。
     *
     * @param shutdownOK 如果应该在 SHUTDOWN 时返回 true，则传递 true
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * 将任务队列中的任务排空到一个新的列表中，通常使用 drainTo。
     * 但如果队列是 DelayQueue 或任何其他可能导致 poll 或 drainTo 无法删除某些元素的队列，
     * 它将一个一个地删除。
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

/*
 * 用于创建、运行和清理工作线程的方法
 */
    /**
     * 检查是否可以根据当前池状态和给定的边界（核心或最大值）添加新的工作线程。
     * 如果可以，则调整工作线程数，并在可能的情况下创建并启动一个新线程，运行 firstTask 作为它的第一个任务。
     * 如果池已停止或有资格关闭，则此方法返回 false。
     * 如果线程工厂创建线程失败（无论是因为工厂返回 null，还是因为异常，通常是 Thread.start() 中的 OutOfMemoryError），
     * 我们会干净地回滚。
     *
     * @param firstTask 新线程应首先运行的任务（如果没有则为 null）。
     *                  当线程数少于 corePoolSize 时，总是会通过此方法创建线程（在 execute() 方法中），
     *                  或者当队列已满时（此时必须绕过队列）。空闲线程通常通过 prestartCoreThread 创建，或者替换其他即将死亡的线程。
     *
     * @param core 如果为 true 使用 corePoolSize 作为边界，否则使用 maximumPoolSize。
     *             使用布尔指示器而不是值，以确保在检查其他池状态后读取最新的值。
     * @return 如果成功则返回 true
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // 检查队列是否为空仅在必要时进行。
            if (rs >= SHUTDOWN &&
                    ! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty()))
                return false;

            for (;;) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();  // 重新读取 ctl
                if (runStateOf(c) != rs)
                    continue retry;
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 持有锁时重新检查。出现 ThreadFactory 失败时或在锁定前关闭时回退。
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // 预检查线程是否可以启动
                            throw new IllegalThreadStateException();
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * 回滚工作线程创建。
     * - 从 workers 中移除工作线程（如果存在）
     * - 减少工作线程计数
     * - 重新检查是否可以终止，因为此工作线程的存在可能延迟了终止
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 为正在死亡的工作线程执行清理和记账工作。仅从工作线程中调用。
     * 除非 completedAbruptly 设置为 true，否则假设工作线程数已调整。
     * 此方法从 worker 集合中移除线程，并在由于用户任务异常退出的情况下，或当前运行的工作线程数少于 corePoolSize 时替换该线程。
     *
     * @param w 工作线程
     * @param completedAbruptly 如果工作线程由于用户异常死亡
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // 如果是突然退出，workerCount 没有调整
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // 不需要替换
            }
            addWorker(null, false);
        }
    }

    /**
     * 根据当前配置设置执行阻塞或定时等待任务，或返回 null。
     * 当以下条件之一成立时，工作线程必须退出：
     * 1. 当前工作线程数超过 maximumPoolSize（通过调用 setMaximumPoolSize）。
     * 2. 线程池已停止。
     * 3. 线程池已关闭且队列为空。
     * 4. 此工作线程在等待任务时超时，且在等待之前和之后超时的线程都可以终止，
     *    并且如果队列非空，此工作线程不是池中的最后一个线程。
     *
     * @return 返回任务，或如果工作线程必须退出则返回 null，在这种情况下 workerCount 会减少
     */
    private Runnable getTask() {
        boolean timedOut = false; // 上一次 poll() 是否超时

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // 检查队列是否为空仅在必要时进行。
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // 工作线程是否会被移除？
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    /**
     * 主工作线程运行循环。重复从队列中获取任务并执行，同时处理多个问题：
     *
     * 1. 我们可能从一个初始任务开始，在这种情况下不需要获取第一个任务。否则，只要池在运行，我们就会从 getTask 获取任务。
     *    如果返回 null，则工作线程会因为池状态或配置参数变化而退出。
     *    其他退出是由于外部代码中的异常抛出，在这种情况下 completedAbruptly 被设置为 true，通常会导致 processWorkerExit 替换此线程。
     *
     * 2. 在运行任何任务之前获取锁，以防止在任务执行时池被中断，
     *    然后确保线程在池未停止时没有被设置为中断状态。
     *
     * 3. 每个任务的执行之前会调用 beforeExecute，这可能会抛出异常，
     *    在这种情况下我们会使线程终止（断开循环，completedAbruptly 设置为 true），并且不会处理该任务。
     *
     * 4. 假设 beforeExecute 正常完成，我们将运行任务，收集它抛出的任何异常并发送给 afterExecute。
     *    我们分别处理 RuntimeException、Error（两者都保证在规范中被捕获）和任意 Throwable。
     *    由于我们不能在 Runnable.run 内重新抛出 Throwable，因此在退出时我们会将它们包装在 Error 中传递给线程的 UncaughtExceptionHandler。
     *    任何抛出的异常还会导致线程保守地终止。
     *
     * 5. 在 task.run 完成后，我们会调用 afterExecute，后者也可能抛出异常，这也会导致线程终止。
     *    根据 JLS Sec 14.20，此异常即使 task.run 抛出也是有效的。
     *
     * 异常处理的净效应是，afterExecute 和线程的 UncaughtExceptionHandler 都会尽可能准确地了解用户代码遇到的任何问题。
     *
     * @param w 工作线程
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // 允许中断
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // 如果池正在停止，确保线程被中断；如果没有，确保线程没有被中断。
                // 这需要在第二种情况下重新检查以处理清除中断时的 shutdownNow 竞争情况
                if ((runStateAtLeast(ctl.get(), STOP) ||
                        (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) &&
                        !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }


    // 公共构造函数和方法

    /**
     * 创建一个新的 {@code ThreadPoolExecutor}，具有给定的初始参数、默认的线程工厂和拒绝执行处理程序。
     * 可能更方便使用 {@link Executors} 工厂方法之一，而不是这个通用的构造函数。
     *
     * @param corePoolSize 线程池中要保持的线程数量，即使它们是空闲的，除非 {@code allowCoreThreadTimeOut} 被设置。
     * @param maximumPoolSize 允许的最大线程数。
     * @param keepAliveTime 当线程数超过核心时，空闲线程等待新任务的最长时间，超过此时间线程将终止。
     * @param unit {@code keepAliveTime} 参数的时间单位。
     * @param workQueue 用于在执行任务之前保存任务的队列。
     * @throws IllegalArgumentException 如果满足以下任意条件：
     *         {@code corePoolSize < 0}，{@code keepAliveTime < 0}，{@code maximumPoolSize <= 0}，
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue} 为空。
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 创建一个新的 {@code ThreadPoolExecutor}，具有给定的初始参数和默认的拒绝执行处理程序。
     *
     * @param corePoolSize 线程池中要保持的线程数量，即使它们是空闲的，除非 {@code allowCoreThreadTimeOut} 被设置。
     * @param maximumPoolSize 允许的最大线程数。
     * @param keepAliveTime 当线程数超过核心时，空闲线程等待新任务的最长时间，超过此时间线程将终止。
     * @param unit {@code keepAliveTime} 参数的时间单位。
     * @param workQueue 用于在执行任务之前保存任务的队列。
     * @param threadFactory 当执行器创建新线程时使用的工厂。
     * @throws IllegalArgumentException 如果满足以下任意条件：
     *         {@code corePoolSize < 0}，{@code keepAliveTime < 0}，{@code maximumPoolSize <= 0}，
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue} 或 {@code threadFactory} 为 null。
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    /**
     * 创建一个新的 {@code ThreadPoolExecutor}，具有给定的初始参数和默认的线程工厂。
     *
     * @param corePoolSize 线程池中要保持的线程数量，即使它们是空闲的，除非 {@code allowCoreThreadTimeOut} 被设置。
     * @param maximumPoolSize 允许的最大线程数。
     * @param keepAliveTime 当线程数超过核心时，空闲线程等待新任务的最长时间，超过此时间线程将终止。
     * @param unit {@code keepAliveTime} 参数的时间单位。
     * @param workQueue 用于在执行任务之前保存任务的队列。
     * @param handler 当执行被阻塞时使用的处理程序，因为线程边界和队列容量已满。
     * @throws IllegalArgumentException 如果满足以下任意条件：
     *         {@code corePoolSize < 0}，{@code keepAliveTime < 0}，{@code maximumPoolSize <= 0}，
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue} 或 {@code handler} 为 null。
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    /**
     * 创建一个新的 {@code ThreadPoolExecutor}，具有给定的初始参数。
     *
     * @param corePoolSize 线程池中要保持的线程数量，即使它们是空闲的，除非 {@code allowCoreThreadTimeOut} 被设置。
     * @param maximumPoolSize 允许的最大线程数。
     * @param keepAliveTime 当线程数超过核心时，空闲线程等待新任务的最长时间，超过此时间线程将终止。
     * @param unit {@code keepAliveTime} 参数的时间单位。
     * @param workQueue 用于在执行任务之前保存任务的队列。
     * @param threadFactory 当执行器创建新线程时使用的工厂。
     * @param handler 当执行被阻塞时使用的处理程序，因为线程边界和队列容量已满。
     * @throws IllegalArgumentException 如果满足以下任意条件：
     *         {@code corePoolSize < 0}，{@code keepAliveTime < 0}，{@code maximumPoolSize <= 0}，
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue}，{@code threadFactory} 或 {@code handler} 为 null。
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 执行给定的任务。
     * 任务可能会在一个新线程中执行，或者在一个现有的线程池线程中执行。
     *
     * 如果任务不能被提交执行，可能是因为此执行器已关闭或者其容量已达到，则任务会通过当前 {@code RejectedExecutionHandler} 处理。
     *
     * @param command 要执行的任务
     * @throws RejectedExecutionException 如果任务不能被接受执行。
     * @throws NullPointerException 如果 {@code command} 为空。
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * 分三步进行：
         *
         * 1. 如果运行的线程少于 corePoolSize，尝试启动一个新线程并将给定的命令作为其第一个任务。
         *    调用 addWorker 会原子地检查 runState 和 workerCount，因此可以防止添加线程的错误。
         *
         * 2. 如果任务可以成功排队，则我们仍然需要重新检查是否应该添加线程（因为现有线程在检查后死亡）或者池在进入该方法后关闭。
         *    因此，我们重新检查状态，如果需要，回滚入队或启动一个新线程。
         *
         * 3. 如果无法将任务排队，则尝试添加一个新线程。如果失败，我们知道池已关闭或已满，因此拒绝任务。
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        } else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * 启动一个有序关闭，已提交的任务将被执行，但不会接受新任务。
     * 如果已关闭，则调用没有额外效果。
     *
     * <p>此方法不会等待已提交的任务完成执行。可以使用 {@link #awaitTermination awaitTermination} 来实现此目的。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // 钩子方法，供 ScheduledThreadPoolExecutor 使用
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * 尝试停止所有正在执行的任务，停止等待中的任务，并返回正在等待执行的任务列表。
     * 这些任务在此方法返回时从任务队列中移除。
     *
     * <p>此方法不会等待正在执行的任务终止。可以使用 {@link #awaitTermination awaitTermination} 来实现此目的。
     *
     * <p>没有保证可以停止正在处理的任务。此实现通过 {@link Thread#interrupt} 来取消任务，
     * 因此任何无法响应中断的任务可能永远不会终止。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * 如果此执行器正在通过 {@link #shutdown} 或 {@link #shutdownNow} 终止，但尚未完全终止，则返回 true。
     * 此方法可能对调试有用。如果在足够长的时间内报告 {@code true}，则可能表明提交的任务忽略或抑制了中断，导致此执行器未正确终止。
     *
     * @return 如果正在终止但尚未终止，则为 {@code true}
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 当此执行器不再被引用并且没有线程时，调用 {@code shutdown}。
     */
    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> { shutdown(); return null; };
            AccessController.doPrivileged(pa, acc);
        }
    }

    /**
     * 设置用于创建新线程的线程工厂。
     *
     * @param threadFactory 新的线程工厂
     * @throws NullPointerException 如果 threadFactory 为 null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * 返回用于创建新线程的线程工厂。
     *
     * @return 当前的线程工厂
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * 设置不可执行任务的新处理程序。
     *
     * @param handler 新的处理程序
     * @throws NullPointerException 如果 handler 为 null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * 返回当前不可执行任务的处理程序。
     *
     * @return 当前的处理程序
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 设置核心线程数。
     * 如果新值小于当前值，则多余的现有线程将在它们下次空闲时终止。
     * 如果新值大于当前值，则根据需要启动新线程来执行排队的任务。
     *
     * @param corePoolSize 新的核心线程数
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // 我们并不知道需要启动多少新线程。
            // 作为启发式方法，预启动足够的线程（最多到新的核心线程数）来处理当前排队的任务，
            // 但如果在执行过程中队列变为空，则停止。
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * 返回核心线程数。
     *
     * @return 核心线程数
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 启动一个核心线程，使其空闲等待工作。这会覆盖默认的仅在执行新任务时启动核心线程的策略。
     * 如果所有核心线程都已启动，则此方法将返回 {@code false}。
     *
     * @return 如果线程已启动，则为 {@code true}
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
                addWorker(null, true);
    }

    /**
     * 与 prestartCoreThread 类似，但即使 corePoolSize 为 0 也会确保至少启动一个线程。
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * 启动所有核心线程，使其空闲等待工作。这会覆盖默认的仅在执行新任务时启动核心线程的策略。
     *
     * @return 启动的线程数
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * 如果此池允许核心线程超时并终止（如果在 keepAlive 时间内没有任务到达），则返回 true。
     * 当为 true 时，应用于非核心线程的相同 keep-alive 策略也适用于核心线程。默认情况下为 false，核心线程永远不会因为缺少任务而终止。
     *
     * @return 如果允许核心线程超时，则为 {@code true}，否则为 {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 设置是否允许核心线程超时并终止（如果在 keep-alive 时间内没有任务到达），
     * 并在新任务到达时替换它们。默认为 false，核心线程永远不会因为缺少任务而终止。
     *
     * @param value 如果应超时，则为 {@code true}，否则为 {@code false}
     * @throws IllegalArgumentException 如果值为 {@code true} 且当前 keep-alive 时间不大于零
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("核心线程必须有非零的 keep-alive 时间");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * 设置允许的最大线程数。这会覆盖构造函数中的任何值。
     * 如果新值小于当前值，则多余的现有线程将在它们下次空闲时终止。
     *
     * @param maximumPoolSize 新的最大值
     * @throws IllegalArgumentException 如果新的最大值小于或等于 0，或者小于核心线程数
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * 返回允许的最大线程数。
     *
     * @return 最大允许的线程数
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置线程在被终止之前可以保持空闲的时间限制。如果线程池中的线程数超过核心线程数，
     * 那么这些超出的线程在等待新任务的空闲时间超过此限制后将被终止。
     * 这会覆盖构造函数中的任何值。
     *
     * @param time 等待的时间。时间值为零时，空闲线程在执行任务后会立即终止。
     * @param unit {@code time} 参数的时间单位
     * @throws IllegalArgumentException 如果 {@code time} 小于零或时间为零且 {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("核心线程必须有非零的 keep-alive 时间");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * 返回线程的 keep-alive 时间，即超出核心线程数的线程在被终止之前可以保持空闲的时间。
     *
     * @param unit 所需结果的时间单位
     * @return 时间限制
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* 用户级别的队列工具 */

    /**
     * 返回此执行器使用的任务队列。访问任务队列主要用于调试和监控。
     * 此队列可能正在使用中。检索任务队列不会阻止排队的任务执行。
     *
     * @return 任务队列
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 如果此任务存在于执行器的内部队列中，则将其移除，从而导致该任务未开始执行时不再执行。
     *
     * <p>此方法在取消方案中可能有用。它可能无法移除已在入队前被转换为其他形式的任务。
     * 例如，使用 {@code submit} 提交的任务可能会被转换为保持 {@code Future} 状态的形式。
     * 但是在这种情况下，可以使用 {@link #purge} 方法来移除那些已被取消的 {@code Future}。
     *
     * @param task 要移除的任务
     * @return 如果任务被移除，则返回 {@code true}
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // 如果处于 SHUTDOWN 状态且队列已为空，进行终止检查
        return removed;
    }

    /**
     * 尝试从工作队列中移除所有 {@link Future} 任务，这些任务已被取消。
     * 此方法可以用作存储回收操作，对功能没有其他影响。
     * 取消的任务永远不会执行，但可能会在工作队列中累积，直到工作线程主动将其移除。调用此方法尝试立即移除它们。
     * 但是，在其他线程干扰的情况下，此方法可能无法移除任务。
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException fallThrough) {
            // 如果在遍历期间遇到干扰，走慢速路径。
            // 生成副本进行遍历，并调用 remove 移除取消的条目。
            // 慢速路径可能是 O(N*N) 复杂度。
            for (Object r : q.toArray()) {
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }

        tryTerminate(); // 如果处于 SHUTDOWN 状态且队列已为空，进行终止检查
    }

    /* 统计信息 */

    /**
     * 返回池中当前的线程数。
     *
     * @return 线程数
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 消除意外情况：isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回正在执行任务的线程数的近似值。
     *
     * @return 正在执行任务的线程数
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers) {
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回池中曾经同时存在的最大线程数。
     *
     * @return 最大线程数
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回曾经计划执行的任务总数的近似值。由于任务和线程的状态可能在计算期间动态变化，返回的值仅为近似值。
     *
     * @return 任务数
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回已完成执行的任务总数的近似值。由于任务和线程的状态可能在计算期间动态变化，返回的值仅为近似值，但不会在连续调用中减少。
     *
     * @return 已完成的任务数
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回一个标识此池及其状态的字符串，包括运行状态和工作线程及任务计数的估计值。
     *
     * @return 标识此池及其状态的字符串
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked()) {
                    ++nactive;
                }
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                        "Shutting down"));
        return super.toString() +
                "[" + rs +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    /* 扩展挂钩 */

    /**
     * 在给定线程中执行给定 Runnable 之前调用的方法。
     * 此方法由将执行任务的线程 {@code t} 调用，可能用于重新初始化 ThreadLocals 或执行日志记录。
     *
     * <p>此实现不执行任何操作，但可以在子类中进行自定义。注意：为了正确嵌套多个重写，子类通常应该在此方法的末尾调用 {@code super.beforeExecute}。
     *
     * @param t 将运行任务 {@code r} 的线程
     * @param r 将要执行的任务
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * 在给定 Runnable 执行完成时调用的方法。此方法由执行任务的线程调用。
     * 如果非空，{@code Throwable} 是导致执行突然终止的未捕获的 {@code RuntimeException} 或 {@code Error}。
     *
     * <p>此实现不执行任何操作，但可以在子类中进行自定义。注意：为了正确嵌套多个重写，子类通常应该在此方法的开头调用 {@code super.afterExecute}。
     *
     * <p><b>注意：</b>当操作封装在任务（例如 {@link FutureTask}）中时，无论是显式封装还是通过 {@code submit} 方法，
     * 这些任务对象都会捕获并维护计算异常，因此它们不会导致突然终止，内部异常不会传递给此方法。
     * 如果希望在此方法中捕获这两种失败情况，您可以进一步探查此类情况，正如以下示例子类所示，
     * 它在任务被中止时打印直接原因或底层异常：
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // 忽略并重置中断
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r 已完成的 runnable
     * @param t 导致终止的异常，或者如果执行正常完成则为 null
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * 当执行器终止时调用的方法。默认实现不执行任何操作。
     * 注意：为了正确嵌套多个重写，子类通常应该在此方法内调用 {@code super.terminated}。
     */
    protected void terminated() { }

    /* 预定义的 RejectedExecutionHandlers */

    /**
     * 当任务被拒绝时执行该任务的处理程序。
     * 如果任务无法被线程池执行，则直接在调用线程中运行该任务，
     * 除非执行器已关闭，在这种情况下任务将被丢弃。
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 CallerRunsPolicy。
         */
        public CallerRunsPolicy() { }

        /**
         * 在调用线程中执行任务 r，除非执行器已关闭，
         * 在这种情况下，任务将被丢弃。
         *
         * @param r 请求执行的 Runnable 任务
         * @param e 试图执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * 任务被拒绝时抛出 {@code RejectedExecutionException} 的处理程序。
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 AbortPolicy。
         */
        public AbortPolicy() { }

        /**
         * 始终抛出 RejectedExecutionException。
         *
         * @param r 请求执行的 Runnable 任务
         * @param e 试图执行此任务的执行器
         * @throws RejectedExecutionException 总是抛出此异常
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("任务 " + r.toString() + " 被拒绝来自 " + e.toString());
        }
    }

    /**
     * 任务被拒绝时静默丢弃该任务的处理程序。
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 DiscardPolicy。
         */
        public DiscardPolicy() { }

        /**
         * 什么都不做，这实际上会丢弃任务 r。
         *
         * @param r 请求执行的 Runnable 任务
         * @param e 试图执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }

    }

    /**
     * 任务被拒绝时丢弃最旧的未处理请求，并随后重试 {@code execute}，除非执行器已关闭，
     * 在这种情况下任务将被丢弃。
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 DiscardOldestPolicy。
         */
        public DiscardOldestPolicy() { }

        /**
         * 获取并忽略执行器本应执行的下一个任务（如果有可用的），
         * 然后重试执行任务 r，除非执行器已关闭，在这种情况下任务 r 将被丢弃。
         *
         * @param r 请求执行的 Runnable 任务
         * @param e 试图执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll(); // 丢弃最旧的任务
                e.execute(r); // 重试执行新任务
            }
        }
    }
}






