package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午6:39
 */

import org.top.java.concurrent.source.locks.LockSupport;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * 一个可取消的异步计算类。此类提供了 {@link Future} 的基本实现，包含启动和取消计算的方法、
 * 查询计算是否完成的方法，以及获取计算结果的方法。只有在计算完成后，才能获取结果；
 * 如果计算尚未完成，{@code get} 方法将阻塞。一旦计算完成，计算不能被重新启动或取消
 * （除非使用 {@link #runAndReset} 来调用计算）。
 *
 * <p>{@code FutureTask} 可以用于包装 {@link Callable} 或 {@link Runnable} 对象。
 * 由于 {@code FutureTask} 实现了 {@code Runnable} 接口，因此可以将 {@code FutureTask}
 * 提交给 {@link Executor} 进行执行。
 *
 * <p>除了作为独立类使用外，此类还提供了 {@code protected} 功能，在创建自定义任务类时可能有用。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> 此 FutureTask 的 {@code get} 方法返回的结果类型
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * 修订说明：与依赖于 AbstractQueuedSynchronizer 的前版本不同，
     * 主要是为了避免用户在取消竞争中对中断状态的保留感到困惑。
     * 当前设计的同步控制依赖于通过 CAS 更新的 "state" 字段来跟踪完成状态，
     * 以及一个简单的 Treiber 栈来保存等待线程。
     *
     * 风格说明：我们一如既往地避免使用 AtomicXFieldUpdaters 带来的开销，
     * 而是直接使用 Unsafe 内部机制。
     */

    /**
     * 该任务的运行状态，初始为 NEW。运行状态仅在 set、setException 和 cancel 方法中
     * 过渡到终止状态。在完成期间，状态可能会变为 COMPLETING（设置结果时）或 INTERRUPTING
     * （只有在取消时中断执行线程）。这些中间状态到最终状态的转换使用了廉价的有序/延迟写入，
     * 因为这些值是唯一的且不能进一步修改。
     *
     * 可能的状态转换：
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /** 底层的 callable；运行后设置为 null */
    private Callable<V> callable;
    /** 要从 get() 返回的结果或要抛出的异常 */
    private Object outcome; // 非 volatile，受 state 读/写保护
    /** 执行 callable 的线程；在 run() 期间通过 CAS 设置 */
    private volatile Thread runner;
    /** Treiber 栈中的等待线程 */
    private volatile WaitNode waiters;

    /**
     * 返回结果或抛出异常，用于已完成的任务。
     *
     * @param s 已完成的状态值
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL)
            return (V)x;
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }

    /**
     * 创建一个 {@code FutureTask}，运行时将执行给定的 {@code Callable}。
     *
     * @param  callable 要执行的任务
     * @throws NullPointerException 如果 callable 为 null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // 确保 callable 的可见性
    }

    /**
     * 创建一个 {@code FutureTask}，运行时将执行给定的 {@code Runnable}，
     * 并安排在成功完成时 {@code get} 方法返回给定的结果。
     *
     * @param runnable 要执行的任务
     * @param result 成功完成时要返回的结果。如果不需要特定结果，建议使用如下构造：
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException 如果 runnable 为 null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // 确保 callable 的可见性
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW &&
                UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                        mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {    // 如果调用中断抛出异常
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // 最终状态
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }

    /**
     * {@inheritDoc}
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
                (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        return report(s);
    }

    /**
     * 当此任务过渡到状态 {@code isDone}（无论是正常完成还是通过取消）时调用的受保护方法。
     * 默认实现不做任何处理。子类可以覆盖此方法以调用完成回调或执行记录操作。
     * 请注意，您可以在此方法的实现中查询状态，以确定此任务是否已取消。
     */
    protected void done() { }

    /**
     * 将此任务的结果设置为给定的值，除非此任务已经设置或已取消。
     *
     * <p>此方法在 {@link #run} 方法成功完成计算时由内部调用。
     *
     * @param v 结果值
     */
    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // 进入最终状态
            finishCompletion();
        }
    }

    /**
     * 使此 Future 报告具有给定 throwable 的 {@link ExecutionException}，作为其失败的原因，
     * 除非此 Future 已经设置或已取消。
     *
     * <p>此方法在计算失败时由 {@link #run} 方法内部调用。
     *
     * @param t 失败的原因
     */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // 进入最终状态
            finishCompletion();
        }
    }

    /**
     * 执行计算。如果任务已完成、已取消，或者已处于非 NEW 状态，则不会执行任何操作。
     * 该方法确保任务状态安全地过渡到最终状态（NORMAL、EXCEPTIONAL 或 CANCELLED）。
     */
    public void run() {
        if (state != NEW ||
                !UNSAFE.compareAndSwapObject(this, runnerOffset,
                        null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            // runner 必须为非 null 直到状态确定为止，以防止并发调用 run()
            runner = null;
            // 状态必须在 nulling runner 之后重新读取，以防止中断泄露
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * 执行计算，但不设置其结果，然后将此 Future 重置为初始状态，如果计算遇到异常或被取消，则重置失败。
     * 设计用于任务本质上可以多次执行的场景。
     *
     * @return 如果成功执行并重置，则返回 {@code true}
     */
    protected boolean runAndReset() {
        if (state != NEW ||
                !UNSAFE.compareAndSwapObject(this, runnerOffset,
                        null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // 不设置结果
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner 必须为非 null 直到状态确定为止，以防止并发调用 run()
            runner = null;
            // 状态必须在 nulling runner 之后重新读取，以防止中断泄露
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * 确保从可能的 cancel(true) 中发送的中断只在 run 或 runAndReset 中传递到任务。
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // 我们的中断器可能会在获得机会中断我们之前暂停。我们可以耐心等待。
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // 等待挂起的中断

        // assert state == INTERRUPTED;

        // 我们希望清除从 cancel(true) 收到的任何中断。然而，可以允许使用中断
        // 作为任务与调用者通信的独立机制，没有办法只清除取消中断。
        // Thread.interrupted();
    }

    /**
     * 用于在 Treiber 栈中记录等待线程的简单链表节点。
     * 有关更详细的说明，请参阅其他类，如 Phaser 和 SynchronousQueue。
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * 移除并唤醒所有等待线程，调用 done()，并将 callable 设置为 null。
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // 断开链接以帮助 GC
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // 减少内存占用
    }

    /**
     * 等待任务完成或在中断或超时时中止。
     *
     * @param timed 是否使用超时等待
     * @param nanos 等待时间（如果是超时等待）
     * @return 完成时的状态
     */
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETING) {
                if (q != null)
                    q.thread = null;
                return s;
            }
            else if (s == COMPLETING) // 尚未超时
                Thread.yield();
            else if (q == null)
                q = new WaitNode();
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                        q.next = waiters, q);
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            else
                LockSupport.park(this);
        }
    }

    /**
     * 尝试取消超时或中断的等待节点，以避免积累垃圾。
     * 内部节点直接取消链接而不使用 CAS，因为即使它们仍然会被释放者遍历也是无害的。
     * 为了避免从已移除的节点中取消链接的影响，在出现明显竞争的情况下重新遍历列表。
     * 当有很多节点时，这会很慢，但我们不期望列表足够长以超过高开销方案的效果。
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // 在 removeWaiter 竞争时重新开始
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) // 检查竞争
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                            q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe 机制
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
