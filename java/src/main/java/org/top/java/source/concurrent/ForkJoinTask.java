package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午1:59
 */

import org.top.java.source.concurrent.locks.ReentrantLock;
import org.top.java.source.lang.Thread;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 在 {@link ForkJoinPool} 中运行的任务的抽象基类。
 * {@code ForkJoinTask} 是一种比普通线程轻量得多的线程类实体。
 * 大量的任务和子任务可以由 ForkJoinPool 中少量的实际线程来承载，
 * 代价是使用上的一些限制。
 *
 * <p>一个“主”{@code ForkJoinTask} 在被显式提交到 {@link ForkJoinPool} 时开始执行，
 * 或者如果尚未参与 ForkJoin 计算，则通过 {@link #fork}、{@link #invoke} 或相关方法在 {@link ForkJoinPool#commonPool()} 中启动。
 * 一旦启动，它通常会依次启动其他子任务。
 * 如本类名称所示，许多使用 {@code ForkJoinTask} 的程序仅使用 {@link #fork} 和 {@link #join} 方法，
 * 或其派生方法如 {@link #invokeAll(ForkJoinTask...) invokeAll}。
 * 然而，本类还提供了一些在高级使用中可能派上用场的方法，以及一些允许支持新型 fork/join 处理的扩展机制。
 *
 * <p>{@code ForkJoinTask} 是 {@link Future} 的轻量形式。
 * {@code ForkJoinTask} 的效率来自于一组限制（这些限制只能部分通过静态方式强制执行），
 * 这些限制反映了其主要用途是计算纯函数或操作纯隔离的对象。
 * 主要的协调机制是 {@link #fork}，它安排异步执行，
 * 以及 {@link #join}，它在任务的结果被计算出来之前不会继续执行。
 * 计算应尽量避免使用 {@code synchronized} 方法或块，
 * 并应尽量减少除连接其他任务或使用如 Phaser 这样与 fork/join 调度协作的同步器外的其他阻塞同步。
 * 可细分的任务还不应执行阻塞 I/O，并且应尽量访问与其他正在运行的任务完全独立的变量。
 * 这些准则通过不允许抛出检查异常如 {@code IOException} 来松散地执行。
 * 但是，计算仍可能遇到未检查异常，这些异常会在调用者尝试连接它们时重新抛出。
 * 这些异常还可能包括由于内部资源耗尽（例如无法分配内部任务队列）引发的 {@link RejectedExecutionException}。
 * 重新抛出的异常与常规异常的行为相同，但如果可能，它们会包含发起计算的线程以及实际遇到异常的线程的堆栈跟踪
 * （例如通过 {@code ex.printStackTrace()} 显示）；至少包含后者的堆栈跟踪。
 *
 * <p>可以定义和使用可能阻塞的 ForkJoinTasks，但这样做需要考虑三个方面：
 * (1) 很少或根本没有<em>其他</em>任务应依赖于一个在外部同步或 I/O 上阻塞的任务。
 * 事件风格的异步任务通常不被连接（例如，继承自 {@link CountedCompleter} 的任务）常常属于这种情况。
 * (2) 为了最小化资源影响，任务应该很小；理想情况下只执行（可能）阻塞的操作。
 * (3) 除非使用了 {@link ForkJoinPool.ManagedBlocker} API，
 * 或者可能阻塞的任务数量已知小于池的 {@link ForkJoinPool#getParallelism} 级别，
 * 否则池不能保证有足够的线程来确保进展或良好的性能。
 *
 * <p>等待任务完成并提取结果的主要方法是 {@link #join}，但也有几个变体：
 * {@link Future#get} 方法支持可中断和/或定时的等待完成，并使用 {@code Future} 约定报告结果。
 * 方法 {@link #invoke} 在语义上等同于 {@code fork(); join();}，但总是尝试在当前线程中开始执行。
 * 这些方法的“<em>静默</em>”形式不提取结果或报告异常。
 * 当一组任务正在执行时，这些形式可能很有用，并且您需要在所有任务完成之前延迟处理结果或异常。
 * 方法 {@code invokeAll}（有多个版本可用）执行最常见的并行调用形式：分叉一组任务并连接它们。
 *
 * <p>在最典型的使用中，fork-join 对像并行递归函数的调用（fork）和返回（join）。
 * 与其他形式的递归调用一样，返回（连接）应首先从最内层开始。
 * 例如，{@code a.fork(); b.fork(); b.join(); a.join();} 通常比先连接 {@code a} 再连接 {@code b} 效率高得多。
 *
 * <p>任务的执行状态可以在多个详细级别上查询：{@link #isDone} 在任务以任何方式完成时为 true
 * （包括任务被取消而不执行的情况）；{@link #isCompletedNormally} 在任务未取消或未遇到异常时为 true；
 * {@link #isCancelled} 如果任务被取消，则为 true（在这种情况下 {@link #getException} 返回一个 {@link java.util.concurrent.CancellationException}）；
 * 而 {@link #isCompletedAbnormally} 在任务被取消或遇到异常时为 true，在这种情况下 {@link #getException} 将返回遇到的异常或 {@link java.util.concurrent.CancellationException}。
 *
 * <p>通常不直接子类化 ForkJoinTask。
 * 相反，您可以子类化支持特定类型的 fork/join 处理的抽象类，通常是用于不返回结果的大多数计算的 {@link RecursiveAction}，
 * 用于返回结果的计算的 {@link RecursiveTask}，以及用于完成操作触发其他操作的 {@link CountedCompleter}。
 * 通常，一个具体的 ForkJoinTask 子类声明由其构造函数中建立的参数组成的字段，
 * 然后定义一个 {@code compute} 方法，该方法以某种方式使用该基类提供的控制方法。
 *
 * <p>方法 {@link #join} 及其变体仅适用于当完成依赖关系是无环的情况；
 * 即并行计算可以描述为一个有向无环图（DAG）。否则，执行可能会遇到某种形式的死锁，因为任务会相互循环等待。
 * 但是，此框架支持其他方法和技术（例如使用 {@link Phaser}、{@link #helpQuiesce} 和 {@link #complete}），
 * 它们可能在构造非静态结构为 DAG 的自定义子类时派上用场。
 * 为了支持这些用法，可以使用 {@link #setForkJoinTaskTag} 或 {@link #compareAndSetForkJoinTaskTag} 原子地<em>标记</em>一个 ForkJoinTask 的 {@code short} 值，
 * 并通过 {@link #getForkJoinTaskTag} 进行检查。
 * ForkJoinTask 实现不使用这些 {@code protected} 方法或标签用于任何目的，但它们可能在构造专门子类时派上用场。
 * 例如，并行图遍历可以使用提供的方法来避免重新访问已处理的节点/任务。
 * （标签方法的名称部分冗长是为了鼓励定义反映其使用模式的方法。）
 *
 * <p>大多数基础支持方法都是 {@code final} 的，以防止覆盖与底层轻量任务调度框架本质上相关的实现。
 * 创建新的基本样式的 fork/join 处理的开发人员至少应实现 {@code protected} 方法 {@link #exec}、{@link #setRawResult} 和 {@link #getRawResult}，
 * 同时引入可以在其子类中实现的抽象计算方法，可能依赖于本类提供的其他 {@code protected} 方法。
 *
 * <p>ForkJoinTasks 应执行相对较少的计算。
 * 大任务应分解为较小的子任务，通常通过递归分解来完成。
 * 一个粗略的经验法则是，任务应执行超过 100 但少于 10000 个基本计算步骤，并应避免无限循环。
 * 如果任务过大，那么并行性无法提高吞吐量。如果任务过小，则内存和内部任务维护的开销可能会超过处理能力。
 *
 * <p>此类提供了用于 {@link Runnable} 和 {@link Callable} 的 {@code adapt} 方法，
 * 当将 {@code ForkJoinTasks} 与其他类型的任务混合执行时，这些方法可能派上用场。
 * 当所有任务都属于这种形式时，考虑在<em>异步模式</em>下构造一个池。
 *
 * <p>ForkJoinTasks 是 {@code Serializable} 的，这使它们可以用于诸如远程执行框架之类的扩展中。
 * 将任务序列化仅应在执行之前或之后，而不是在执行过程中进行。
 * 执行本身不依赖于序列化。
 *
 * @since 1.7
 * @author Doug Lea
 */

public abstract class ForkJoinTask<V> implements Future<V>, Serializable {

    /*
     * 有关类 ForkJoinPool 的一般实现概述，请参见其内部文档。
     * ForkJoinTasks 主要负责在 relays 到 ForkJoinWorkerThread 和 ForkJoinPool 方法时保持它们的 "status" 字段。
     *
     * 此类的方法大致分为：
     * (1) 基础状态维护
     * (2) 执行与等待完成
     * (3) 用户级方法，此外还报告结果。
     * 这有时难以看清，因为此文件按 Javadocs 的顺序导出了方法。
     */

    /*
     * 状态字段持有运行控制状态位，这些位被打包到一个单一的 int 中以最小化占用空间并确保原子性（通过 CAS 实现）。
     * 状态初始为 0，并在完成之前取非负值，完成后状态（和 DONE_MASK）持有值 NORMAL、CANCELLED 或 EXCEPTIONAL。
     * 任务被其他线程阻塞等待时，其 SIGNAL 位被设置。
     * 完成带有 SIGNAL 位的被偷窃的任务会通过 notifyAll 唤醒任何等待者。
     * 尽管对于某些目的而言次优，我们使用基本的内建 wait/notify 来利用 JVM 中的 "监视器膨胀"，否则我们需要模拟它以避免增加更多的任务级别的 bookkeeping 开销。
     * 我们希望这些监视器是 "胖的"，即不使用偏向锁或薄锁技术，因此使用一些奇怪的编码习惯来避免它们，主要是通过安排每个 synchronized 块执行 wait、notifyAll 或两者。
     *
     * 这些控制位仅占用了状态字段的上半部分（16 位）。下半部分用于用户定义的标签。
     */

    /** 任务的运行状态 */
    volatile int status; // 由池和工人直接访问
    static final int DONE_MASK   = 0xf0000000;  // 屏蔽掉非完成位
    static final int NORMAL      = 0xf0000000;  // 必须是负数
    static final int CANCELLED   = 0xc0000000;  // 必须小于 NORMAL
    static final int EXCEPTIONAL = 0x80000000;  // 必须小于 CANCELLED
    static final int SIGNAL      = 0x00010000;  // 必须大于等于 1 << 16
    static final int SMASK       = 0x0000ffff;  // 用于标签的短位

    /**
     * 标记任务完成并唤醒等待加入此任务的线程。
     *
     * @param completion 是 NORMAL, CANCELLED, EXCEPTIONAL 之一
     * @return 退出时的完成状态
     */
    private int setCompletion(int completion) {
        for (int s;;) {
            if ((s = status) < 0)
                return s;
            if (U.compareAndSwapInt(this, STATUS, s, s | completion)) {
                if ((s >>> 16) != 0)
                    synchronized (this) { notifyAll(); }
                return completion;
            }
        }
    }

    // 以下为简化版注释翻译...

    /**
     * 主要执行被偷窃任务的方法。除非完成，否则调用 exec 并在完成时记录状态，但不会等待完成。
     *
     * @return 此方法退出时的状态
     */
    final int doExec() {
        int s; boolean completed;
        if ((s = status) >= 0) {
            try {
                completed = exec();
            } catch (Throwable rex) {
                return setExceptionalCompletion(rex);
            }
            if (completed)
                s = setCompletion(NORMAL);
        }
        return s;
    }

    /**
     * 如果未完成，设置 SIGNAL 状态并执行 Object.wait(timeout)。
     * 此任务在退出时可能已完成，也可能未完成。忽略中断。
     *
     * @param timeout 使用 Object.wait 的惯例
     */
    final void internalWait(long timeout) {
        int s;
        if ((s = status) >= 0 && // 强制完成者发出通知
                U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
            synchronized (this) {
                if (status >= 0)
                    try { wait(timeout); } catch (InterruptedException ie) { }
                else
                    notifyAll();
            }
        }
    }

    /**
     * 阻塞非工作线程直到任务完成。
     *
     * @return 完成时的状态
     */
    private int externalAwaitDone() {
        int s = ((this instanceof CountedCompleter) ? // 尝试帮助
                ForkJoinPool.common.externalHelpComplete(
                        (CountedCompleter<?>)this, 0) :
                ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0);
        if (s >= 0 && (s = status) >= 0) {
            boolean interrupted = false;
            do {
                if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        if (status >= 0) {
                            try {
                                wait(0L);
                            } catch (InterruptedException ie) {
                                interrupted = true;
                            }
                        }
                        else
                            notifyAll();
                    }
                }
            } while ((s = status) >= 0);
            if (interrupted)
                Thread.currentThread().interrupt();
        }
        return s;
    }

    /**
     * 阻塞非工作线程直到任务完成或中断。
     */
    private int externalInterruptibleAwaitDone() throws InterruptedException {
        int s;
        if (Thread.interrupted())
            throw new InterruptedException();
        if ((s = status) >= 0 &&
                (s = ((this instanceof CountedCompleter) ?
                        ForkJoinPool.common.externalHelpComplete(
                                (CountedCompleter<?>)this, 0) :
                        ForkJoinPool.common.tryExternalUnpush(this) ? doExec() :
                                0)) >= 0) {
            while ((s = status) >= 0) {
                if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        if (status >= 0)
                            wait(0L);
                        else
                            notifyAll();
                    }
                }
            }
        }
        return s;
    }

    /**
     * 实现 join, get, quietlyJoin 的逻辑。直接处理已经完成、外部等待和 unfork+exec 的情况。
     * 其他情况转发到 ForkJoinPool.awaitJoin。
     *
     * @return 完成时的状态
     */
    private int doJoin() {
        int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
        return (s = status) < 0 ? s :
                ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                        (w = (wt = (ForkJoinWorkerThread)t).workQueue).
                                tryUnpush(this) && (s = doExec()) < 0 ? s :
                                wt.pool.awaitJoin(w, this, 0L) :
                        externalAwaitDone();
    }

    /**
     * 实现 invoke 和 quietlyInvoke 的逻辑。
     *
     * @return 完成时的状态
     */
    private int doInvoke() {
        int s; Thread t; ForkJoinWorkerThread wt;
        return (s = doExec()) < 0 ? s :
                ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                        (wt = (ForkJoinWorkerThread)t).pool.
                                awaitJoin(wt.workQueue, this, 0L) :
                        externalAwaitDone();
    }

    // 异常表支持

    /**
     * 存储任务抛出的异常的表，用于允许调用者报告。
     * 因为异常很少见，所以我们不直接将它们保存在任务对象中，而是使用一个弱引用表。
     * 请注意，取消异常不会出现在表中，而是被记录为状态值。
     *
     * 注意：这些静态变量会在下面的静态代码块中初始化。
     */
    private static final ExceptionNode[] exceptionTable;
    private static final ReentrantLock exceptionTableLock;
    private static final ReferenceQueue<Object> exceptionTableRefQueue;

    /**
     * exceptionTable 的固定容量。
     */
    private static final int EXCEPTION_MAP_CAPACITY = 32;

    /**
     * 异常表的键值节点。该链式哈希表使用身份比较、完全锁定以及弱引用作为键。
     * 该表具有固定容量，因为它仅维护任务异常足够长的时间供加入者访问，
     * 因此在持续时间内不应变得非常大。然而，由于我们不知道最后一个加入者何时完成，
     * 我们必须使用弱引用并在操作时进行清除（因此需要完全锁定）。
     * 另外，当池进入静止状态时，ForkJoinPool 中的某个线程将调用 helpExpungeStaleExceptions。
     */
    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        ExceptionNode next;
        final long thrower;  // 使用线程 ID，而不是引用，以避免弱引用循环
        final int hashCode;  // 在弱引用消失前存储任务的哈希码
        ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next) {
            super(task, exceptionTableRefQueue);
            this.ex = ex;
            this.next = next;
            this.thrower = Thread.currentThread().getId();
            this.hashCode = System.identityHashCode(task);
        }
    }

    /**
     * 记录异常并设置状态。
     *
     * @return 退出时的状态
     */
    final int recordExceptionalCompletion(Throwable ex) {
        int s;
        if ((s = status) >= 0) {
            int h = System.identityHashCode(this);
            final ReentrantLock lock = exceptionTableLock;
            lock.lock();
            try {
                expungeStaleExceptions();
                ExceptionNode[] t = exceptionTable;
                int i = h & (t.length - 1);
                for (ExceptionNode e = t[i]; ; e = e.next) {
                    if (e == null) {
                        t[i] = new ExceptionNode(this, ex, t[i]);
                        break;
                    }
                    if (e.get() == this) // 已存在
                        break;
                }
            } finally {
                lock.unlock();
            }
            s = setCompletion(EXCEPTIONAL);
        }
        return s;
    }

    /**
     * 记录异常并可能传播。
     *
     * @return 退出时的状态
     */
    private int setExceptionalCompletion(Throwable ex) {
        int s = recordExceptionalCompletion(ex);
        if ((s & DONE_MASK) == EXCEPTIONAL)
            internalPropagateException(ex);
        return s;
    }

    /**
     * 异常传播支持的钩子，用于有 completers 的任务。
     */
    void internalPropagateException(Throwable ex) {
    }

    /**
     * 取消任务，忽略由 cancel 抛出的任何异常。
     * 在工作线程和池关闭期间使用。cancel 被规范为不抛出任何异常，但如果它确实抛出了异常，
     * 我们在关闭期间没有其他手段，因此需要防范这种情况。
     */
    static final void cancelIgnoringExceptions(ForkJoinTask<?> t) {
        if (t != null && t.status >= 0) {
            try {
                t.cancel(false);
            } catch (Throwable ignore) {
            }
        }
    }

    /**
     * 移除异常节点并清除状态。
     */
    private void clearExceptionalCompletion() {
        int h = System.identityHashCode(this);
        final ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            ExceptionNode[] t = exceptionTable;
            int i = h & (t.length - 1);
            ExceptionNode e = t[i];
            ExceptionNode pred = null;
            while (e != null) {
                ExceptionNode next = e.next;
                if (e.get() == this) {
                    if (pred == null)
                        t[i] = next;
                    else
                        pred.next = next;
                    break;
                }
                pred = e;
                e = next;
            }
            expungeStaleExceptions();
            status = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回给定任务的可重抛出的异常（如果存在）。
     * 为了提供准确的堆栈跟踪，如果异常不是由当前线程抛出的，
     * 我们尝试创建一个与抛出的异常类型相同的新异常，但以记录的异常为其原因。
     * 如果没有这样的构造函数，我们尝试使用无参构造函数，然后使用 initCause 达到同样的效果。
     * 如果这些方法都不适用，或由于其他异常导致失败，我们返回记录的异常，尽管它可能包含误导性的堆栈跟踪。
     *
     * @return 异常，或 null（如果没有）
     */
    private Throwable getThrowableException() {
        if ((status & DONE_MASK) != EXCEPTIONAL)
            return null;
        int h = System.identityHashCode(this);
        ExceptionNode e;
        final ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] t = exceptionTable;
            e = t[h & (t.length - 1)];
            while (e != null && e.get() != this)
                e = e.next;
        } finally {
            lock.unlock();
        }
        Throwable ex;
        if (e == null || (ex = e.ex) == null)
            return null;
        if (e.thrower != Thread.currentThread().getId()) {
            Class<? extends Throwable> ec = ex.getClass();
            try {
                Constructor<?> noArgCtor = null;
                Constructor<?>[] cs = ec.getConstructors(); // 只使用 public 构造函数
                for (int i = 0; i < cs.length; ++i) {
                    Constructor<?> c = cs[i];
                    Class<?>[] ps = c.getParameterTypes();
                    if (ps.length == 0)
                        noArgCtor = c;
                    else if (ps.length == 1 && ps[0] == Throwable.class) {
                        Throwable wx = (Throwable)c.newInstance(ex);
                        return (wx == null) ? ex : wx;
                    }
                }
                if (noArgCtor != null) {
                    Throwable wx = (Throwable)(noArgCtor.newInstance());
                    if (wx != null) {
                        wx.initCause(ex);
                        return wx;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return ex;
    }

    /**
     * 清理过时的引用并将它们移除。仅在持有锁时调用。
     */
    private static void expungeStaleExceptions() {
        for (Object x; (x = exceptionTableRefQueue.poll()) != null;) {
            if (x instanceof ExceptionNode) {
                int hashCode = ((ExceptionNode)x).hashCode;
                ExceptionNode[] t = exceptionTable;
                int i = hashCode & (t.length - 1);
                ExceptionNode e = t[i];
                ExceptionNode pred = null;
                while (e != null) {
                    ExceptionNode next = e.next;
                    if (e == x) {
                        if (pred == null)
                            t[i] = next;
                        else
                            pred.next = next;
                        break;
                    }
                    pred = e;
                    e = next;
                }
            }
        }
    }

    /**
     * 如果锁可用，则清理过时的引用并将它们移除。
     * 当 ForkJoinPool 进入静止状态时调用。
     */
    static final void helpExpungeStaleExceptions() {
        final ReentrantLock lock = exceptionTableLock;
        if (lock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 一个版本的“sneaky throw”来传递异常。
     */
    static void rethrow(Throwable ex) {
        if (ex != null)
            ForkJoinTask.<RuntimeException>uncheckedThrow(ex);
    }

    /**
     * sneaky throw 的 sneaky 部分，依赖泛型限制来规避编译器对抛出未检查异常的投诉。
     */
    @SuppressWarnings("unchecked") static <T extends Throwable>
    void uncheckedThrow(Throwable t) throws T {
        throw (T)t; // 依赖于 vacuous 强制转换
    }

    /**
     * 抛出与给定状态相关联的异常（如果有）。
     */
    private void reportException(int s) {
        if (s == CANCELLED)
            throw new CancellationException();
        if (s == EXCEPTIONAL)
            rethrow(getThrowableException());
    }

    // 公共方法

    /**
     * 安排在当前任务运行的线程池中异步执行此任务（如果适用），
     * 或使用 {@link ForkJoinPool#commonPool()}（如果不在 {@link #inForkJoinPool} 中）。
     * 尽管这不是强制执行的，但多次 fork 一个任务（除非它已经完成并被重新初始化）是一个使用错误。
     * 此任务状态或其操作的数据的后续修改在调用 {@link #join} 或相关方法之前，
     * 不一定对其他线程一致可见，或者调用 {@link #isDone} 返回 {@code true}。
     *
     * @return {@code this}，以简化使用
     */
    public final ForkJoinTask<V> fork() {
        Thread t;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            ((ForkJoinWorkerThread)t).workQueue.push(this);
        else
            ForkJoinPool.common.externalPush(this);
        return this;
    }

    /**
     * 当任务 {@link #isDone} 完成时返回其计算结果。
     * 此方法不同于 {@link #get()}，异常完成导致抛出 {@code RuntimeException} 或 {@code Error}，
     * 而非 {@code ExecutionException}，并且调用线程的中断不会导致此方法通过抛出
     * {@code InterruptedException} 来中断返回。
     *
     * @return 计算结果
     */
    public final V join() {
        int s;
        if ((s = doJoin() & DONE_MASK) != NORMAL)
            reportException(s);
        return getRawResult();
    }

    /**
     * 开始执行此任务，如果必要，等待其完成，并返回其结果，
     * 或抛出（未检查的）{@code RuntimeException} 或 {@code Error}，如果底层计算抛出异常。
     *
     * @return 计算结果
     */
    public final V invoke() {
        int s;
        if ((s = doInvoke() & DONE_MASK) != NORMAL)
            reportException(s);
        return getRawResult();
    }

    /**
     * 分别 fork 给定的两个任务，返回时 {@code isDone} 对于每个任务均为真，
     * 或者如果遇到（未检查的）异常，则抛出异常。如果超过一个任务遇到异常，
     * 则此方法抛出其中一个异常。如果任何任务遇到异常，另一个可能会被取消。
     * 然而，异常返回时不保证个别任务的执行状态。
     * 可以使用 {@link #getException()} 和相关方法获取每个任务的状态，
     * 以检查它们是否已被取消、正常完成、异常完成或未处理。
     *
     * @param t1 第一个任务
     * @param t2 第二个任务
     * @throws NullPointerException 如果任何任务为 null
     */
    public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2) {
        int s1, s2;
        t2.fork();
        if ((s1 = t1.doInvoke() & DONE_MASK) != NORMAL)
            t1.reportException(s1);
        if ((s2 = t2.doJoin() & DONE_MASK) != NORMAL)
            t2.reportException(s2);
    }

    /**
     * 并行执行给定的任务，当每个任务 {@code isDone} 为 true 或遇到（未检查的）异常时返回，
     * 在遇到异常的情况下，异常将被重新抛出。如果多个任务遇到异常，则此方法会抛出其中一个异常。
     * 如果任何任务遇到异常，其他任务可能会被取消。然而，在异常返回时，无法保证每个任务的执行状态。
     * 可以使用 {@link #getException()} 和相关方法来检查它们是否已被取消，正常完成，异常完成或未被处理。
     *
     * @param tasks 要执行的任务
     * @throws NullPointerException 如果任何任务为 null
     */
    public static void invokeAll(ForkJoinTask<?>... tasks) {
        Throwable ex = null;
        int last = tasks.length - 1;
        // 从最后一个任务开始遍历并 fork 任务
        for (int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = tasks[i];
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            }
            else if (i != 0)
                t.fork();  // fork 除第一个以外的任务
            else if (t.doInvoke() < NORMAL && ex == null)
                ex = t.getException();  // 执行第一个任务
        }
        // 遍历任务，处理执行结果
        for (int i = 1; i <= last; ++i) {
            ForkJoinTask<?> t = tasks[i];
            if (t != null) {
                if (ex != null)
                    t.cancel(false);  // 如果存在异常，则取消任务
                else if (t.doJoin() < NORMAL)
                    ex = t.getException();  // 如果任务未正常完成，记录异常
            }
        }
        // 如果有异常，重新抛出
        if (ex != null)
            rethrow(ex);
    }

    /**
     * 分别 fork 给定的任务集合，返回时 {@code isDone} 对于每个任务均为真，
     * 或者如果遇到（未检查的）异常，则抛出异常。如果超过一个任务遇到异常，
     * 则此方法抛出其中一个异常。如果任何任务遇到异常，其他可能会被取消。
     * 然而，异常返回时不保证个别任务的执行状态。
     * 可以使用 {@link #getException()} 和相关方法获取每个任务的状态，
     * 以检查它们是否已被取消、正常完成、异常完成或未处理。
     *
     * @param tasks 任务集合
     * @param <T> 任务返回值的类型
     * @return 任务集合，简化使用
     * @throws NullPointerException 如果任务集合或其任意元素为 null
     */
    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if (!(tasks instanceof RandomAccess) || !(tasks instanceof List<?>)) {
            invokeAll(tasks.toArray(new ForkJoinTask<?>[tasks.size()]));
            return tasks;
        }
        @SuppressWarnings("unchecked")
        List<? extends ForkJoinTask<?>> ts =
                (List<? extends ForkJoinTask<?>>) tasks;
        Throwable ex = null;
        int last = ts.size() - 1;
        for (int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = ts.get(i);
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            }
            else if (i != 0)
                t.fork();
            else if (t.doInvoke() < NORMAL && ex == null)
                ex = t.getException();
        }
        for (int i = 1; i <= last; ++i) {
            ForkJoinTask<?> t = ts.get(i);
            if (t != null) {
                if (ex != null)
                    t.cancel(false);
                else if (t.doJoin() < NORMAL)
                    ex = t.getException();
            }
        }
        if (ex != null)
            rethrow(ex);
        return tasks;
    }

    /**
     * 尝试取消任务的执行。如果任务已经完成或因其他原因无法取消，
     * 此尝试将失败。如果成功，并且在调用 {@code cancel} 时任务尚未启动，
     * 则任务的执行将被抑制。在此方法成功返回之后，除非有中间的 {@link #reinitialize} 调用，
     * 否则对 {@link #isCancelled}、{@link #isDone} 和 {@code cancel} 的调用将返回 {@code true}，
     * 对 {@link #join} 和相关方法的调用将导致抛出 {@code CancellationException}。
     *
     * <p>此方法可以在子类中被重写，但如果重写，仍需确保这些属性保持一致。
     * 特别是，{@code cancel} 方法本身不能抛出异常。
     *
     * <p>此方法设计用于由 <em>其他</em> 任务调用。要终止当前任务，您可以直接从计算方法中返回或抛出未检查的异常，
     * 或者调用 {@link #completeExceptionally(Throwable)}。
     *
     * @param mayInterruptIfRunning 此值在默认实现中没有效果，因为中断不用于控制取消。
     * @return {@code true} 如果此任务现在被取消
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return (setCompletion(CANCELLED) & DONE_MASK) == CANCELLED;
    }

    /**
     * 返回 {@code true}，如果此任务已经完成。
     */
    public final boolean isDone() {
        return status < 0;
    }

    /**
     * 返回 {@code true}，如果此任务已经被取消。
     */
    public final boolean isCancelled() {
        return (status & DONE_MASK) == CANCELLED;
    }

    /**
     * 返回 {@code true}，如果此任务抛出了异常或被取消。
     */
    public final boolean isCompletedAbnormally() {
        return status < NORMAL;
    }

    /**
     * 返回 {@code true}，如果此任务完成且未抛出异常，也未被取消。
     */
    public final boolean isCompletedNormally() {
        return (status & DONE_MASK) == NORMAL;
    }

    /**
     * 返回任务执行时抛出的异常，或返回 {@code CancellationException}，如果任务被取消，或者返回 {@code null}，
     * 如果没有异常或任务尚未完成。
     */
    public final Throwable getException() {
        int s = status & DONE_MASK;
        return ((s >= NORMAL)    ? null :
                (s == CANCELLED) ? new CancellationException() :
                        getThrowableException());
    }

    /**
     * 以异常的方式完成此任务，如果任务尚未被中止或取消，则使其在 {@code join} 和相关操作中抛出给定的异常。
     * 此方法可用于在异步任务中引发异常，或者强制完成可能不会正常完成的任务。
     * 在其他情况下使用此方法是不鼓励的。此方法可以被重写，但重写的版本必须调用 {@code super} 实现以维持保证。
     *
     * @param ex 要抛出的异常。如果此异常不是 {@code RuntimeException} 或 {@code Error}，
     * 则实际抛出的异常将是以 {@code ex} 为原因的 {@code RuntimeException}。
     */
    public void completeExceptionally(Throwable ex) {
        setExceptionalCompletion((ex instanceof RuntimeException) ||
                (ex instanceof Error) ? ex :
                new RuntimeException(ex));
    }

    /**
     * 以给定的值作为结果完成此任务，并且如果任务尚未被中止或取消，
     * 则将该值作为 {@code join} 和相关操作的结果返回。
     * 此方法可以用于提供异步任务的结果，或提供可能不会正常完成的任务的替代处理。
     * 在其他情况下使用此方法是不鼓励的。此方法可以被重写，但重写的版本必须调用 {@code super} 实现以维持保证。
     *
     * @param value 此任务的结果值
     */
    public void complete(V value) {
        try {
            setRawResult(value);
        } catch (Throwable rex) {
            setExceptionalCompletion(rex);
            return;
        }
        setCompletion(NORMAL);
    }

    /**
     * 正常完成此任务而不设置值。{@link #setRawResult} 最近设置的值（默认情况下为 {@code null}）
     * 将作为后续 {@code join} 和相关操作的结果返回。
     */
    public final void quietlyComplete() {
        setCompletion(NORMAL);
    }

    /**
     * 如果必要，等待计算完成，然后获取其结果。
     *
     * @return 计算结果
     * @throws CancellationException 如果任务已被取消
     * @throws ExecutionException 如果任务执行过程中抛出了异常
     * @throws InterruptedException 如果当前线程不是 {@link ForkJoinPool} 的成员，
     * 且在等待时被中断
     */
    public final V get() throws InterruptedException, ExecutionException {
        int s = (Thread.currentThread() instanceof ForkJoinWorkerThread) ?
                doJoin() : externalInterruptibleAwaitDone();
        Throwable ex;
        if ((s &= DONE_MASK) == CANCELLED)
            throw new CancellationException();
        if (s == EXCEPTIONAL && (ex = getThrowableException()) != null)
            throw new ExecutionException(ex);
        return getRawResult();
    }

    /**
     * 如果必要，等待计算完成，最多等待给定时间，然后获取其结果（如果可用）。
     *
     * @param timeout 最大等待时间
     * @param unit 超时参数的时间单位
     * @return 计算结果
     * @throws CancellationException 如果任务已被取消
     * @throws ExecutionException 如果任务执行过程中抛出了异常
     * @throws InterruptedException 如果当前线程不是 {@link ForkJoinPool} 的成员，
     * 且在等待时被中断
     * @throws TimeoutException 如果等待超时
     */
    public final V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        int s;
        long nanos = unit.toNanos(timeout);
        if (Thread.interrupted())
            throw new InterruptedException();
        if ((s = status) >= 0 && nanos > 0L) {
            long d = System.nanoTime() + nanos;
            long deadline = (d == 0L) ? 1L : d; // 避免 0
            Thread t = Thread.currentThread();
            if (t instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
                s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
            }
            else if ((s = ((this instanceof CountedCompleter) ?
                    ForkJoinPool.common.externalHelpComplete(
                            (CountedCompleter<?>)this, 0) :
                    ForkJoinPool.common.tryExternalUnpush(this) ?
                            doExec() : 0)) >= 0) {
                long ns, ms; // 用纳秒测量，但以毫秒等待
                while ((s = status) >= 0 &&
                        (ns = deadline - System.nanoTime()) > 0L) {
                    if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) > 0L &&
                            U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                        synchronized (this) {
                            if (status >= 0)
                                wait(ms); // 可以抛出 InterruptedException
                            else
                                notifyAll();
                        }
                    }
                }
            }
        }
        if (s >= 0)
            s = status;
        if ((s &= DONE_MASK) != NORMAL) {
            Throwable ex;
            if (s == CANCELLED)
                throw new CancellationException();
            if (s != EXCEPTIONAL)
                throw new TimeoutException();
            if ((ex = getThrowableException()) != null)
                throw new ExecutionException(ex);
        }
        return getRawResult();
    }

    /**
     * 加入此任务，但不返回其结果或抛出其异常。
     * 当处理一组任务时，如果某些任务已取消或已知已中止，则此方法可能很有用。
     */
    public final void quietlyJoin() {
        doJoin();
    }

    /**
     * 开始执行此任务，并在必要时等待其完成，而不返回其结果或抛出其异常。
     */
    public final void quietlyInvoke() {
        doInvoke();
    }

    /**
     * 在当前任务运行的池 {@link ForkJoinPool#isQuiescent} 静止之前，可能执行任务。
     * 此方法可能在许多任务被 fork，但没有显式加入的设计中有用，而是执行它们直到所有任务被处理完毕。
     */
    public static void helpQuiesce() {
        Thread t;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
            wt.pool.helpQuiescePool(wt.workQueue);
        }
        else
            ForkJoinPool.quiesceCommonPool();
    }

    /**
     * 重置此任务的内部状态，以允许后续 {@code fork} 操作。
     * 此方法允许重复使用此任务，但仅在此任务从未被 fork 或已被 fork，
     * 然后完成且所有等待加入此任务的操作都已完成时才可重复使用。
     * 在任何其他使用条件下，其效果都不能保证。
     * 此方法在循环中执行预构建的子任务树时可能很有用。
     */
    public void reinitialize() {
        if ((status & DONE_MASK) == EXCEPTIONAL)
            clearExceptionalCompletion();
        else
            status = 0;
    }

    /**
     * 返回承载当前任务执行的池，如果此任务在任何 ForkJoinPool 之外执行，则返回 null。
     *
     * @see #inForkJoinPool
     * @return 当前池，或 {@code null} 如果没有
     */
    public static ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        return (t instanceof ForkJoinWorkerThread) ?
                ((ForkJoinWorkerThread) t).pool : null;
    }

    /**
     * 如果当前线程是一个 {@link ForkJoinWorkerThread}，并且作为 ForkJoinPool 计算执行，返回 {@code true}。
     *
     * @return 如果当前线程是 {@link ForkJoinWorkerThread} 并且作为 ForkJoinPool 计算执行，则返回 {@code true}，
     * 否则返回 {@code false}
     */
    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }

    /**
     * 尝试取消此任务的调度执行。如果此任务是当前线程最近 fork 的任务，
     * 并且尚未在其他线程中开始执行，则此方法通常（但不保证）成功。
     * 此方法在安排任务的本地处理时可能很有用，这些任务本可以被偷取，但尚未被处理。
     *
     * @return {@code true} 如果取消调度成功
     */
    public boolean tryUnfork() {
        Thread t;
        return (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                ((ForkJoinWorkerThread)t).workQueue.tryUnpush(this) :
                ForkJoinPool.common.tryExternalUnpush(this));
    }

    /**
     * 返回估计的当前工作线程排队但尚未执行的任务数量。
     * 此值对于是否 fork 其他任务的启发式决策可能有用。
     *
     * @return 任务的数量
     */
    public static int getQueuedTaskCount() {
        Thread t; ForkJoinPool.WorkQueue q;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            q = ((ForkJoinWorkerThread)t).workQueue;
        else
            q = ForkJoinPool.commonSubmitterQueue();
        return (q == null) ? 0 : q.queueSize();
    }

    /**
     * 返回当前工作线程持有的局部排队任务比其他可能偷取它们的工作线程还多的任务估计数量，
     * 或者如果当前线程没有在 ForkJoinPool 中操作则返回 0。此值对于是否 fork 其他任务的启发式决策可能有用。
     * 在许多 ForkJoinTask 使用中，达到稳态时，每个工作线程应当维持一个小的常数数量的任务剩余（例如 3），
     * 并在超过此阈值时本地处理计算。
     *
     * @return 任务的剩余数量，可能为负
     */
    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }

    // 扩展方法

    /**
     * 返回通过 {@link #join} 返回的结果，即使此任务异常完成或未完成，或者如果此任务尚未被认为完成则返回 {@code null}。
     * 此方法旨在帮助调试以及支持扩展。在其他上下文中使用此方法是不鼓励的。
     *
     * @return 结果，或者如果尚未完成则返回 {@code null}
     */
    public abstract V getRawResult();

    /**
     * 强制给定的值作为结果返回。此方法旨在支持扩展，不应在一般情况下调用。
     *
     * @param value 结果值
     */
    protected abstract void setRawResult(V value);

    /**
     * 立即执行此任务的基本操作并返回 {@code true}，如果在此方法返回时，此任务保证已正常完成。
     * 否则，此方法可能返回 {@code false}，表示此任务未必完成（或尚未知其是否完成），例如在需要显式调用完成方法的异步操作中。
     * 此方法也可能抛出（未检查的）异常以表示异常退出。此方法旨在支持扩展，不应在一般情况下调用。
     *
     * @return {@code true}，如果已知此任务已正常完成
     */
    protected abstract boolean exec();

    /**
     * 返回（但不取消调度或执行）当前线程排队但尚未执行的任务，如果有可用任务的话。
     * 无法保证此任务实际上会被轮询或下一步执行。相反，此方法可能返回 {@code null}，
     * 即使存在任务但无法在不与其他线程争用的情况下访问该任务。
     * 此方法主要用于支持扩展，在其他情况下不太可能有用。
     *
     * @return 下一个任务，或者如果没有可用任务则返回 {@code null}
     */
    protected static ForkJoinTask<?> peekNextLocalTask() {
        Thread t; ForkJoinPool.WorkQueue q;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            q = ((ForkJoinWorkerThread)t).workQueue;
        else
            q = ForkJoinPool.commonSubmitterQueue();
        return (q == null) ? null : q.peek();
    }

    /**
     * 如果当前线程在 ForkJoinPool 中操作，则取消调度并返回下一个排队任务而不执行，
     * 如果当前线程中没有可用任务的话。此方法主要用于支持扩展，在其他情况下不太可能有用。
     *
     * @return 下一个任务，或者如果没有可用任务则返回 {@code null}
     */
    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread t;
        return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                ((ForkJoinWorkerThread)t).workQueue.nextLocalTask() :
                null;
    }

    /**
     * 如果当前线程在 ForkJoinPool 中操作，则取消调度并返回下一个排队任务而不执行，
     * 如果没有可用的本地任务，则返回其他线程 fork 的任务（如果有的话）。
     * 任务的可用性可能是暂时的，因此 {@code null} 结果并不一定意味着此任务操作的池已静止。
     * 此方法主要用于支持扩展，在其他情况下不太可能有用。
     *
     * @return 一个任务，或者如果没有可用任务则返回 {@code null}
     */
    protected static ForkJoinTask<?> pollTask() {
        Thread t; ForkJoinWorkerThread wt;
        return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                (wt = (ForkJoinWorkerThread)t).pool.nextTaskFor(wt.workQueue) :
                null;
    }

    // 标签操作

    /**
     * 返回此任务的标签。
     *
     * @return 此任务的标签
     * @since 1.8
     */
    public final short getForkJoinTaskTag() {
        return (short)status;
    }

    /**
     * 原子地设置此任务的标签值。
     *
     * @param tag 标签值
     * @return 先前的标签值
     * @since 1.8
     */
    public final short setForkJoinTaskTag(short tag) {
        for (int s;;) {
            if (U.compareAndSwapInt(this, STATUS, s = status,
                    (s & ~SMASK) | (tag & SMASK)))
                return (short)s;
        }
    }

    /**
     * 原子地、有条件地设置此任务的标签值。标签可用于作为操作图中的访问标记，
     * 例如方法可检查：{@code if (task.compareAndSetForkJoinTaskTag((short)0, (short)1))}，
     * 然后处理节点/任务，否则退出，因为节点/任务已经被处理过了。
     *
     * @param e 期望的标签值
     * @param tag 新的标签值
     * @return {@code true}，如果成功；即当前值等于 e 并且现在等于 tag
     * @since 1.8
     */
    public final boolean compareAndSetForkJoinTaskTag(short e, short tag) {
        for (int s;;) {
            if ((short)(s = status) != e)
                return false;
            if (U.compareAndSwapInt(this, STATUS, s,
                    (s & ~SMASK) | (tag & SMASK)))
                return true;
        }
    }

    /**
     * 适配器用于 {@link Runnable}。此类实现了 RunnableFuture，以便在 ForkJoinPool 中使用时符合 AbstractExecutorService 的约束。
     */
    static final class AdaptedRunnable<T> extends ForkJoinTask<T>
            implements RunnableFuture<T> {
        final Runnable runnable;
        T result;
        AdaptedRunnable(Runnable runnable, T result) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
            this.result = result; // 即使在完成前设置此值也是可以的
        }
        public final T getRawResult() { return result; }
        public final void setRawResult(T v) { result = v; }
        public final boolean exec() { runnable.run(); return true; }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * 适配器用于没有返回结果的 {@link Runnable}
     */
    static final class AdaptedRunnableAction extends ForkJoinTask<Void>
            implements RunnableFuture<Void> {
        final Runnable runnable;
        AdaptedRunnableAction(Runnable runnable) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) { }
        public final boolean exec() { runnable.run(); return true; }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * 适配器用于运行时失败会强制引发工作线程异常的 {@link Runnable}
     */
    static final class RunnableExecuteAction extends ForkJoinTask<Void> {
        final Runnable runnable;
        RunnableExecuteAction(Runnable runnable) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) { }
        public final boolean exec() { runnable.run(); return true; }
        void internalPropagateException(Throwable ex) {
            rethrow(ex); // 在 exec() 捕获之外重新抛出异常。
        }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * 适配器用于 {@link Callable}
     */
    static final class AdaptedCallable<T> extends ForkJoinTask<T>
            implements RunnableFuture<T> {
        final Callable<? extends T> callable;
        T result;
        AdaptedCallable(Callable<? extends T> callable) {
            if (callable == null) throw new NullPointerException();
            this.callable = callable;
        }
        public final T getRawResult() { return result; }
        public final void setRawResult(T v) { result = v; }
        public final boolean exec() {
            try {
                result = callable.call();
                return true;
            } catch (Error err) {
                throw err;
            } catch (RuntimeException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 2838392045355241008L;
    }

    /**
     * 返回一个新的 {@code ForkJoinTask}，该任务执行给定 {@code Runnable} 的 {@code run} 方法作为其操作，
     * 并在 {@link #join} 时返回 null 结果。
     *
     * @param runnable 可运行的操作
     * @return 该任务
     */
    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnableAction(runnable);
    }

    /**
     * 返回一个新的 {@code ForkJoinTask}，该任务执行给定 {@code Runnable} 的 {@code run} 方法作为其操作，
     * 并在 {@link #join} 时返回给定的结果。
     *
     * @param runnable 可运行的操作
     * @param result 完成时的结果
     * @param <T> 结果的类型
     * @return 该任务
     */
    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable<>(runnable, result);
    }

    /**
     * 返回一个新的 {@code ForkJoinTask}，该任务执行给定 {@code Callable} 的 {@code call} 方法作为其操作，
     * 并在 {@link #join} 时返回其结果，将遇到的任何检查异常转换为 {@code RuntimeException}。
     *
     * @param callable 可调用的操作
     * @param <T> 可调用结果的类型
     * @return 该任务
     */
    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable<>(callable);
    }

    // 序列化支持

    private static final long serialVersionUID = -7721805057305804111L;

    /**
     * 将此任务保存到流中（即，将其序列化）。
     *
     * @param s 流
     * @throws java.io.IOException 如果发生 I/O 错误
     * @serialData 当前运行状态以及执行期间抛出的异常，如果没有则为 {@code null}
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        s.defaultWriteObject();
        s.writeObject(getException());
    }

    /**
     * 从流中重构此任务（即，将其反序列化）。
     *
     * @param s 流
     * @throws ClassNotFoundException 如果找不到序列化对象的类
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        Object ex = s.readObject();
        if (ex != null)
            setExceptionalCompletion((Throwable)ex);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATUS;

    static {
        exceptionTableLock = new ReentrantLock();
        exceptionTableRefQueue = new ReferenceQueue<>();
        exceptionTable = new ExceptionNode[EXCEPTION_MAP_CAPACITY];
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ForkJoinTask.class;
            STATUS = U.objectFieldOffset
                    (k.getDeclaredField("status"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}




