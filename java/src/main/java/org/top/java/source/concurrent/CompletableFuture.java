package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午5:31
 */

import org.top.java.source.concurrent.locks.LockSupport;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 一个 {@link Future}，它可以显式完成（设置其值和状态），并且可以用作 {@link CompletionStage}，
 * 支持在其完成时触发的依赖函数和操作。
 *
 * <p>当两个或更多线程尝试 {@link #complete complete}、{@link #completeExceptionally completeExceptionally} 或 {@link #cancel cancel}
 * 一个 CompletableFuture 时，只有其中一个成功。
 *
 * <p>除了直接操作状态和结果的这些相关方法外，CompletableFuture 还通过以下策略实现了接口 {@link CompletionStage}：<ul>
 *
 * <li>为 <em>非异步</em> 方法提供的依赖完成的操作可以由完成当前 CompletableFuture 的线程执行，
 * 或由任何其他调用完成方法的线程执行。</li>
 *
 * <li>所有 <em>异步</em> 方法如果没有显式的 Executor 参数，则会使用 {@link ForkJoinPool#commonPool()} 执行任务
 * （除非它不支持至少两级并行度，在这种情况下，会创建一个新线程来运行每个任务）。为了简化监控、调试和跟踪，所有生成的异步任务
 * 都是标记接口 {@link AsynchronousCompletionTask} 的实例。</li>
 *
 * <li>所有 CompletionStage 方法独立于其他公共方法实现，因此某个方法的行为不会受到子类中其他方法重写的影响。</li> </ul>
 *
 * <p>CompletableFuture 还通过以下策略实现了 {@link Future}：<ul>
 *
 * <li>由于（与 {@link FutureTask} 不同），该类没有直接控制导致其完成的计算，因此取消操作只是另一种异常完成的形式。
 * 方法 {@link #cancel cancel} 的效果与 {@code completeExceptionally(new CancellationException())} 相同。
 * 方法 {@link #isCompletedExceptionally} 可以用于判断 CompletableFuture 是否以任何异常方式完成。</li>
 *
 * <li>在遇到 CompletionException 异常完成的情况下，方法 {@link #get()} 和 {@link #get(long, TimeUnit)} 会抛出
 * {@link ExecutionException}，其原因与对应的 CompletionException 中持有的原因相同。为了简化大多数情况下的使用，
 * 此类还定义了方法 {@link #join()} 和 {@link #getNow}，在这些情况下，它们直接抛出 CompletionException。</li> </ul>
 *
 * @author Doug Lea
 * @since 1.8
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    /*
     * 概述：
     *
     * CompletableFuture 可能会有依赖的完成操作，这些操作收集在一个链表栈中。
     * 它通过 CAS（比较并交换）操作原子性地完成一个结果字段，然后弹出并运行这些操作。
     * 这适用于正常与异常结果、同步与异步操作、二元触发器和各种形式的完成。
     *
     * 通过 CAS 设置字段 result 的非空性表明已完成。AltResult 用于对 null 进行封装，并存储异常。
     * 使用单个字段可以简化完成的检测和触发。编码和解码过程虽然简单，但增加了捕获和关联异常与目标的复杂性。
     * 依赖于（静态的）NIL（用于封装 null 结果）是唯一带有 null 异常字段的 AltResult，因此我们通常不需要显式比较。
     * 尽管某些泛型类型转换是未经检查的（请参阅 SuppressWarnings 注释），它们被放置在适当的位置，即使进行检查也是合适的。
     *
     * 依赖的操作通过 Completion 对象表示，这些对象以 Treiber 栈的形式链接，由字段 "stack" 引导。
     * 每种操作都有相应的 Completion 类，它们分为单输入（UniCompletion）、双输入（BiCompletion）、投射（BiCompletion 使用两个输入之一）、共享（CoCompletion，由两个来源中的第二个使用）、零输入源操作和解锁等待线程的 Signallers。
     * Completion 类继承了 ForkJoinTask 以实现异步执行（因为我们利用其 "tag" 方法来保持声明权，所以不增加空间开销）。
     * 它还被声明为 Runnable，以允许与任意执行器一起使用。
     *
     * 每种 CompletionStage 的支持依赖于一个单独的类，以及两个 CompletableFuture 方法：
     *
     * * 一个名称为 X 的 Completion 类对应于某个函数，前缀为 "Uni"、"Bi" 或 "Or"。每个类都包含来源、操作和依赖项的字段。
     *   它们乏味地相似，彼此之间的差异仅与底层功能形式有关。我们这样做是为了让用户在常见使用中不会遇到适配器层。
     *   我们还包括不对应于用户方法的 "Relay" 类/方法；它们将结果从一个阶段复制到另一个阶段。
     *
     * * 带有 x(...) 的 Boolean CompletableFuture 方法（例如 uniApply）接受所有参数来检查操作是否可以触发，
     *   然后运行操作或通过执行其 Completion 参数（如果存在）来安排其异步执行。如果已知完成，则方法返回 true。
     *
     * * Completion 方法 tryFire(int mode) 使用其持有的参数调用相关的 x 方法，并在成功后进行清理。
     *   mode 参数允许 tryFire 被调用两次（SYNC，然后 ASYNC）；第一次是屏蔽和捕获异常，同时安排执行；第二次是从任务中调用时。
     *   claim() 回调会抑制函数调用，如果已被其他线程声明。
     *
     * * CompletableFuture 方法 xStage(...) 从 CompletableFuture x 的公共阶段方法中调用。它筛选用户参数，并调用或创建阶段对象。
     *   如果不是异步的并且 x 已经完成，则立即运行操作。否则，创建 Completion c，将其推送到 x 的栈中（除非已完成），并通过 c.tryFire 启动或触发。
     *   这还涵盖了 x 在推送时完成的竞争情况。带有两个输入的类（例如 BiApply）在推送操作时处理两个输入之间的竞争。
     *   第二个完成是指向第一个的 CoCompletion，最多只有一个执行该操作。多元方法 allOf 和 anyOf 成对地执行此操作，以形成完成树。
     *
     * 注意，方法的泛型类型参数根据 "this" 是来源、依赖项还是完成项而变化。
     *
     * postComplete 方法在完成后调用，除非目标保证不可见（即尚未返回或链接）。
     * 多个线程可以调用 postComplete，它原子性地弹出每个依赖操作，并尝试通过 tryFire 方法在 NESTED 模式下触发它。
     * 触发可以递归传播，因此 NESTED 模式返回其已完成的依赖项（如果存在），以供其调用者进一步处理（请参阅 postFire 方法）。
     *
     * 阻塞方法 get() 和 join() 依赖于 Signaller Completion，它们会唤醒等待的线程。
     * 其机制类似于 FutureTask、Phaser 和 SynchronousQueue 中使用的 Treiber 栈等待节点。有关算法细节，请参阅其内部文档。
     *
     * 如果没有采取预防措施，CompletableFutures 在完成链条构建时容易产生垃圾积累，每个链条指向其来源。
     * 因此，我们尽可能早地将字段置空（尤其是 Completion.detach 方法）。所需的筛选检查会无害地忽略可能在与线程竞争时获得的空参数，这些线程可能已将字段置空。
     * 我们还尝试取消与可能永远不会弹出的栈链接的 Completion（请参阅 postFire 方法）。
     * Completion 字段不需要声明为 final 或 volatile，因为它们只有在安全发布时才对其他线程可见。
     */
    volatile Object result;       // result字段，保存完成结果或封装的AltResult
    volatile Completion stack;    // Treiber栈顶，保存依赖操作

    // 内部完成方法，通过CAS操作将result字段从null变为r
    final boolean internalComplete(Object r) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, r);
    }

    // CAS操作修改栈顶Completion
    final boolean casStack(Completion cmp, Completion val) {
        return UNSAFE.compareAndSwapObject(this, STACK, cmp, val);
    }

    // 成功将c推入栈顶时返回true
    final boolean tryPushStack(Completion c) {
        Completion h = stack;
        lazySetNext(c, h);
        return UNSAFE.compareAndSwapObject(this, STACK, h, c);
    }

    // 无条件地将c推入栈，必要时重试
    final void pushStack(Completion c) {
        do {} while (!tryPushStack(c));
    }

    /* ------------- 结果的编码与解码 -------------- */

    // AltResult类封装异常或null值，见上文
    static final class AltResult {
        final Throwable ex;        // null 仅用于 NIL
        AltResult(Throwable x) { this.ex = x; }
    }

    // null值的封装
    static final AltResult NIL = new AltResult(null);

    // 使用null值完成，除非已完成
    final boolean completeNull() {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, NIL);
    }

    // 返回给定非异常值的编码
    final Object encodeValue(T t) {
        return (t == null) ? NIL : t;
    }

    // 使用非异常结果完成，除非已完成
    final boolean completeValue(T t) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, (t == null) ? NIL : t);
    }

    // 返回给定（非空）异常的封装版本，如果已是CompletionException则直接返回
    static AltResult encodeThrowable(Throwable x) {
        return new AltResult((x instanceof CompletionException) ? x : new CompletionException(x));
    }

    // 使用异常结果完成，除非已完成
    final boolean completeThrowable(Throwable x) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, encodeThrowable(x));
    }

    // 返回封装后的异常，若已经封装为CompletionException则可能返回原始r
    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        } else if (r instanceof AltResult && x == ((AltResult)r).ex) {
            return r;
        }
        return new AltResult(x);
    }

    // 使用给定的异常或r完成，除非已完成
    final boolean completeThrowable(Throwable x, Object r) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, encodeThrowable(x, r));
    }

    // 根据异常或正常值对结果进行编码
    Object encodeOutcome(T t, Throwable x) {
        return (x == null) ? (t == null) ? NIL : t : encodeThrowable(x);
    }

    // 复制结果时封装或重新包装异常
    static Object encodeRelay(Object r) {
        Throwable x;
        return (((r instanceof AltResult) &&
                (x = ((AltResult)r).ex) != null &&
                !(x instanceof CompletionException)) ?
                new AltResult(new CompletionException(x)) : r);
    }

    // 使用r或其副本完成，若为异常则先封装为CompletionException
    final boolean completeRelay(Object r) {
        return UNSAFE.compareAndSwapObject(this, RESULT, null, encodeRelay(r));
    }

    // 使用Future.get的约定报告结果
    private static <T> T reportGet(Object r) throws InterruptedException, ExecutionException {
        if (r == null) // 按约定，null表示中断
            throw new InterruptedException();
        if (r instanceof AltResult) {
            Throwable x, cause;
            if ((x = ((AltResult)r).ex) == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException)x;
            if ((x instanceof CompletionException) && (cause = x.getCause()) != null)
                x = cause;
            throw new ExecutionException(x);
        }
        @SuppressWarnings("unchecked") T t = (T) r;
        return t;
    }

    // 解码结果，返回结果或抛出异常
    private static <T> T reportJoin(Object r) {
        if (r instanceof AltResult) {
            Throwable x;
            if ((x = ((AltResult)r).ex) == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException)x;
            if (x instanceof CompletionException)
                throw (CompletionException)x;
            throw new CompletionException(x);
        }
        @SuppressWarnings("unchecked") T t = (T) r;
        return t;
    }

    /* ------------- 异步任务的预备部分 -------------- */

    /**
     * 一个标记接口，标识由{@code async}方法生成的异步任务。
     * 这可能对监控、调试和跟踪异步活动有用。
     *
     * @since 1.8
     */
    public static interface AsynchronousCompletionTask {
    }

    // 是否使用公共池
    private static final boolean useCommonPool =
            (ForkJoinPool.getCommonPoolParallelism() > 1);

    // 默认执行器 - ForkJoinPool.commonPool()，除非它不支持并行度
    private static final Executor asyncPool = useCommonPool ?
            ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();

    // 当ForkJoinPool.commonPool()不支持并行度时的回退
    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) { new Thread(r).start(); }
    }

    // 检查并转换用户提供的执行器参数，如果是公共池则使用asyncPool
    static Executor screenExecutor(Executor e) {
        if (!useCommonPool && e == ForkJoinPool.commonPool())
            return asyncPool;
        if (e == null) throw new NullPointerException();
        return e;
    }

    // Completion.tryFire的模式。符号意义很重要。
    static final int SYNC   =  0;
    static final int ASYNC  =  1;
    static final int NESTED = -1;

    /* ------------- 基础Completion类及操作 -------------- */

    @SuppressWarnings("serial")
    abstract static class Completion extends ForkJoinTask<Void>
            implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;      // Treiber栈链接

        /**
         * 如果触发，则执行完成操作，返回可能需要传播的依赖项（如果存在）。
         *
         * @param mode SYNC, ASYNC 或 NESTED
         */
        abstract CompletableFuture<?> tryFire(int mode);

        /** 返回true如果可能仍然可触发。由cleanStack调用。 */
        abstract boolean isLive();

        public final void run()                { tryFire(ASYNC); }
        public final boolean exec()            { tryFire(ASYNC); return true; }
        public final Void getRawResult()       { return null; }
        public final void setRawResult(Void v) {}
    }

    // 延迟设置c的next为next
    static void lazySetNext(Completion c, Completion next) {
        UNSAFE.putOrderedObject(c, NEXT, next);
    }

    /**
     * 弹出并尝试触发所有可达的依赖项。仅在已知完成时调用。
     */
    final void postComplete() {
        CompletableFuture<?> f = this; Completion h;
        while ((h = f.stack) != null || (f != this && (h = (f = this).stack) != null)) {
            CompletableFuture<?> d; Completion t;
            if (f.casStack(h, t = h.next)) {
                if (t != null) {
                    if (f != this) {
                        pushStack(h);
                        continue;
                    }
                    h.next = null;    // 解除连接
                }
                f = (d = h.tryFire(NESTED)) == null ? this : d;
            }
        }
    }

    // 遍历栈并取消不活动的Completion
    final void cleanStack() {
        for (Completion p = null, q = stack; q != null;) {
            Completion s = q.next;
            if (q.isLive()) {
                p = q;
                q = s;
            } else if (p == null) {
                casStack(q, s);
                q = stack;
            } else {
                p.next = s;
                if (p.isLive())
                    q = s;
                else {
                    p = null;  // 重启
                    q = stack;
                }
            }
        }
    }
    /* ------------- 一输入 Completion -------------- */

    /** 一个 Completion，包含源、依赖和执行器。 */
    @SuppressWarnings("serial")
    abstract static class UniCompletion<T,V> extends Completion {
        Executor executor;                 // 要使用的执行器（如果为null，则没有）
        CompletableFuture<V> dep;          // 要完成的依赖
        CompletableFuture<T> src;          // 源动作

        UniCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.executor = executor;
            this.dep = dep;
            this.src = src;
        }

        /**
         * 返回true如果可以运行动作。仅在知道可触发时调用。使用FJ的标记位确保只有一个线程声明所有权。
         * 如果是异步任务，将以任务开始 - 以后调用tryFire运行动作。
         */
        final boolean claim() {
            Executor e = executor;
            if (compareAndSetForkJoinTaskTag((short)0, (short)1)) {
                if (e == null)
                    return true;
                executor = null; // 禁用
                e.execute(this);
            }
            return false;
        }

        final boolean isLive() { return dep != null; }
    }

    /** 将给定的 Completion（如果存在）推入栈，除非已完成。 */
    final void push(UniCompletion<?,?> c) {
        if (c != null) {
            while (result == null && !tryPushStack(c)) {
                lazySetNext(c, null); // 推入失败时清除
            }
        }
    }

    /**
     * 成功触发UniCompletion的tryFire后，依赖项的后处理。尝试清理源a的栈，
     * 然后根据模式决定是调用postComplete还是返回this给调用者。
     */
    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (a != null && a.stack != null) {
            if (mode < 0 || a.result == null)
                a.cleanStack();
            else
                a.postComplete();
        }
        if (result != null && stack != null) {
            if (mode < 0)
                return this;
            else
                postComplete();
        }
        return null;
    }

    @SuppressWarnings("serial")
    static final class UniApply<T,V> extends UniCompletion<T,V> {
        Function<? super T,? extends V> fn;
        UniApply(Executor executor, CompletableFuture<V> dep,
                 CompletableFuture<T> src,
                 Function<? super T,? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniApply(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniApply(CompletableFuture<S> a,
                               Function<? super S,? extends T> f,
                               UniApply<S,T> c) {
        Object r; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") S s = (S) r;
                completeValue(f.apply(s));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <V> CompletableFuture<V> uniApplyStage(
            Executor e, Function<? super T,? extends V> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<V> d =  new CompletableFuture<V>();
        if (e != null || !d.uniApply(this, f, null)) {
            UniApply<T,V> c = new UniApply<T,V>(e, d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniAccept<T> extends UniCompletion<T,Void> {
        Consumer<? super T> fn;
        UniAccept(Executor executor, CompletableFuture<Void> dep,
                  CompletableFuture<T> src, Consumer<? super T> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniAccept(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniAccept(CompletableFuture<S> a,
                                Consumer<? super S> f, UniAccept<S> c) {
        Object r; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") S s = (S) r;
                f.accept(s);
                completeNull();
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private CompletableFuture<Void> uniAcceptStage(Executor e,
                                                   Consumer<? super T> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.uniAccept(this, f, null)) {
            UniAccept<T> c = new UniAccept<T>(e, d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniRun<T> extends UniCompletion<T,Void> {
        Runnable fn;
        UniRun(Executor executor, CompletableFuture<Void> dep,
               CompletableFuture<T> src, Runnable fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniRun(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniRun(CompletableFuture<?> a, Runnable f, UniRun<?> c) {
        Object r; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        if (result == null) {
            if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                completeThrowable(x, r);
            else
                try {
                    if (c != null && !c.claim())
                        return false;
                    f.run();
                    completeNull();
                } catch (Throwable ex) {
                    completeThrowable(ex);
                }
        }
        return true;
    }

    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.uniRun(this, f, null)) {
            UniRun<T> c = new UniRun<T>(e, d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniWhenComplete<T> extends UniCompletion<T,T> {
        BiConsumer<? super T, ? super Throwable> fn;
        UniWhenComplete(Executor executor, CompletableFuture<T> dep,
                        CompletableFuture<T> src,
                        BiConsumer<? super T, ? super Throwable> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniWhenComplete(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniWhenComplete(CompletableFuture<T> a,
                                  BiConsumer<? super T,? super Throwable> f,
                                  UniWhenComplete<T> c) {
        Object r; T t; Throwable x = null;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    x = ((AltResult)r).ex;
                    t = null;
                } else {
                    @SuppressWarnings("unchecked") T tr = (T) r;
                    t = tr;
                }
                f.accept(t, x);
                if (x == null) {
                    internalComplete(r);
                    return true;
                }
            } catch (Throwable ex) {
                if (x == null)
                    x = ex;
            }
            completeThrowable(x, r);
        }
        return true;
    }

    private CompletableFuture<T> uniWhenCompleteStage(
            Executor e, BiConsumer<? super T, ? super Throwable> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<T> d = new CompletableFuture<T>();
        if (e != null || !d.uniWhenComplete(this, f, null)) {
            UniWhenComplete<T> c = new UniWhenComplete<T>(e, d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniHandle<T,V> extends UniCompletion<T,V> {
        BiFunction<? super T, Throwable, ? extends V> fn;
        UniHandle(Executor executor, CompletableFuture<V> dep,
                  CompletableFuture<T> src,
                  BiFunction<? super T, Throwable, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniHandle(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniHandle(CompletableFuture<S> a,
                                BiFunction<? super S, Throwable, ? extends T> f,
                                UniHandle<S,T> c) {
        Object r; S s; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    x = ((AltResult)r).ex;
                    s = null;
                } else {
                    x = null;
                    @SuppressWarnings("unchecked") S ss = (S) r;
                    s = ss;
                }
                completeValue(f.apply(s, x));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <V> CompletableFuture<V> uniHandleStage(
            Executor e, BiFunction<? super T, Throwable, ? extends V> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<V> d = new CompletableFuture<V>();
        if (e != null || !d.uniHandle(this, f, null)) {
            UniHandle<T,V> c = new UniHandle<T,V>(e, d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniExceptionally<T> extends UniCompletion<T,T> {
        Function<? super Throwable, ? extends T> fn;
        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src,
                         Function<? super Throwable, ? extends T> fn) {
            super(null, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) { // 永不异步
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            if ((d = dep) == null || !d.uniExceptionally(a = src, fn, this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniExceptionally(CompletableFuture<T> a,
                                   Function<? super Throwable, ? extends T> f,
                                   UniExceptionally<T> c) {
        Object r; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        if (result == null) {
            try {
                if (r instanceof AltResult && (x = ((AltResult)r).ex) != null) {
                    if (c != null && !c.claim())
                        return false;
                    completeValue(f.apply(x));
                } else
                    internalComplete(r);
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private CompletableFuture<T> uniExceptionallyStage(
            Function<Throwable, ? extends T> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<T> d = new CompletableFuture<T>();
        if (!d.uniExceptionally(this, f, null)) {
            UniExceptionally<T> c = new UniExceptionally<T>(d, this, f);
            push(c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniRelay<T> extends UniCompletion<T,T> { // 用于Compose
        UniRelay(CompletableFuture<T> dep, CompletableFuture<T> src) {
            super(null, dep, src);
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            if ((d = dep) == null || !d.uniRelay(a = src))
                return null;
            src = null; dep = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniRelay(CompletableFuture<T> a) {
        Object r;
        if (a == null || (r = a.result) == null)
            return false;
        if (result == null) // 无需声明
            completeRelay(r);
        return true;
    }

    @SuppressWarnings("serial")
    static final class UniCompose<T,V> extends UniCompletion<T,V> {
        Function<? super T, ? extends CompletionStage<V>> fn;
        UniCompose(Executor executor, CompletableFuture<V> dep,
                   CompletableFuture<T> src,
                   Function<? super T, ? extends CompletionStage<V>> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            if ((d = dep) == null ||
                    !d.uniCompose(a = src, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniCompose(
            CompletableFuture<S> a,
            Function<? super S, ? extends CompletionStage<T>> f,
            UniCompose<S,T> c) {
        Object r; Throwable x;
        if (a == null || (r = a.result) == null || f == null)
            return false;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") S s = (S) r;
                CompletableFuture<T> g = f.apply(s).toCompletableFuture();
                if (g.result == null || !uniRelay(g)) {
                    UniRelay<T> copy = new UniRelay<T>(this, g);
                    g.push(copy);
                    copy.tryFire(SYNC);
                    if (result == null)
                        return false;
                }
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <V> CompletableFuture<V> uniComposeStage(
            Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
        if (f == null) throw new NullPointerException();
        Object r; Throwable x;
        if (e == null && (r = result) != null) {
            // 尝试直接返回函数结果
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    return new CompletableFuture<V>(encodeThrowable(x, r));
                }
                r = null;
            }
            try {
                @SuppressWarnings("unchecked") T t = (T) r;
                CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                Object s = g.result;
                if (s != null)
                    return new CompletableFuture<V>(encodeRelay(s));
                CompletableFuture<V> d = new CompletableFuture<V>();
                UniRelay<V> copy = new UniRelay<V>(d, g);
                g.push(copy);
                copy.tryFire(SYNC);
                return d;
            } catch (Throwable ex) {
                return new CompletableFuture<V>(encodeThrowable(ex));
            }
        }
        CompletableFuture<V> d = new CompletableFuture<V>();
        UniCompose<T,V> c = new UniCompose<T,V>(e, d, this, f);
        push(c);
        c.tryFire(SYNC);
        return d;
    }
    /* ------------- Two-input Completions -------------- */

    /** 代表具有两个输入源的操作的Completion */
    @SuppressWarnings("serial")
    abstract static class BiCompletion<T,U,V> extends UniCompletion<T,V> {
        CompletableFuture<U> snd; // 第二个操作源
        BiCompletion(Executor executor, CompletableFuture<V> dep,
                     CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(executor, dep, src);
            this.snd = snd;
        }
    }

    /**
     * 代表一个双Completion的代理
     * 当有两个输入的Completion触发时，委托给此Completion进行处理
     */
    @SuppressWarnings("serial")
    static final class CoCompletion extends Completion {
        BiCompletion<?,?,?> base;
        CoCompletion(BiCompletion<?,?,?> base) {
            this.base = base;
        }

        final CompletableFuture<?> tryFire(int mode) {
            BiCompletion<?,?,?> c; CompletableFuture<?> d;
            if ((c = base) == null || (d = c.tryFire(mode)) == null)
                return null;
            base = null; // 分离
            return d;
        }

        final boolean isLive() {
            BiCompletion<?,?,?> c;
            return (c = base) != null && c.dep != null;
        }
    }

    /** 如果未完成，则将完成任务推送到this和b */
    final void bipush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
        if (c != null) {
            Object r;
            while ((r = result) == null && !tryPushStack(c))
                lazySetNext(c, null); // 失败时清除
            if (b != null && b != this && b.result == null) {
                Completion q = (r != null) ? c : new CoCompletion(c);
                while (b.result == null && !b.tryPushStack(q))
                    lazySetNext(q, null); // 失败时清除
            }
        }
    }

    /** BiCompletion tryFire成功后的后处理操作 */
    final CompletableFuture<T> postFire(CompletableFuture<?> a,
                                        CompletableFuture<?> b, int mode) {
        if (b != null && b.stack != null) { // 清理第二个源
            if (mode < 0 || b.result == null)
                b.cleanStack();
            else
                b.postComplete();
        }
        return postFire(a, mode);
    }

    @SuppressWarnings("serial")
    static final class BiApply<T,U,V> extends BiCompletion<T,U,V> {
        BiFunction<? super T,? super U,? extends V> fn;
        BiApply(Executor executor, CompletableFuture<V> dep,
                CompletableFuture<T> src, CompletableFuture<U> snd,
                BiFunction<? super T,? super U,? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.biApply(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S> boolean biApply(CompletableFuture<R> a,
                                CompletableFuture<S> b,
                                BiFunction<? super R,? super S,? extends T> f,
                                BiApply<R,S,T> c) {
        Object r, s; Throwable x;
        if (a == null || (r = a.result) == null ||
                b == null || (s = b.result) == null || f == null)
            return false;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                if ((x = ((AltResult)s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") R rr = (R) r;
                @SuppressWarnings("unchecked") S ss = (S) s;
                completeValue(f.apply(rr, ss));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U,V> CompletableFuture<V> biApplyStage(
            Executor e, CompletionStage<U> o,
            BiFunction<? super T,? super U,? extends V> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<V> d = new CompletableFuture<V>();
        if (e != null || !d.biApply(this, b, f, null)) {
            BiApply<T,U,V> c = new BiApply<T,U,V>(e, d, this, b, f);
            bipush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiAccept<T,U> extends BiCompletion<T,U,Void> {
        BiConsumer<? super T,? super U> fn;
        BiAccept(Executor executor, CompletableFuture<Void> dep,
                 CompletableFuture<T> src, CompletableFuture<U> snd,
                 BiConsumer<? super T,? super U> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.biAccept(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S> boolean biAccept(CompletableFuture<R> a,
                                 CompletableFuture<S> b,
                                 BiConsumer<? super R,? super S> f,
                                 BiAccept<R,S> c) {
        Object r, s; Throwable x;
        if (a == null || (r = a.result) == null ||
                b == null || (s = b.result) == null || f == null)
            return false;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                if ((x = ((AltResult)s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") R rr = (R) r;
                @SuppressWarnings("unchecked") S ss = (S) s;
                f.accept(rr, ss);
                completeNull();
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U> CompletableFuture<Void> biAcceptStage(
            Executor e, CompletionStage<U> o,
            BiConsumer<? super T,? super U> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.biAccept(this, b, f, null)) {
            BiAccept<T,U> c = new BiAccept<T,U>(e, d, this, b, f);
            bipush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiRun<T,U> extends BiCompletion<T,U,Void> {
        Runnable fn;
        BiRun(Executor executor, CompletableFuture<Void> dep,
              CompletableFuture<T> src,
              CompletableFuture<U> snd,
              Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.biRun(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final boolean biRun(CompletableFuture<?> a, CompletableFuture<?> b,
                        Runnable f, BiRun<?,?> c) {
        Object r, s; Throwable x;
        if (a == null || (r = a.result) == null ||
                b == null || (s = b.result) == null || f == null)
            return false;
        if (result == null) {
            if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                completeThrowable(x, r);
            else if (s instanceof AltResult && (x = ((AltResult)s).ex) != null)
                completeThrowable(x, s);
            else
                try {
                    if (c != null && !c.claim())
                        return false;
                    f.run();
                    completeNull();
                } catch (Throwable ex) {
                    completeThrowable(ex);
                }
        }
        return true;
    }

    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> o,
                                               Runnable f) {
        CompletableFuture<?> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.biRun(this, b, f, null)) {
            BiRun<T,?> c = new BiRun<>(e, d, this, b, f);
            bipush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiRelay<T,U> extends BiCompletion<T,U,Void> { // 用于组合操作
        BiRelay(CompletableFuture<Void> dep,
                CompletableFuture<T> src,
                CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null || !d.biRelay(a = src, b = snd))
                return null;
            src = null; snd = null; dep = null;
            return d.postFire(a, b, mode);
        }
    }

    boolean biRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        Object r, s; Throwable x;
        if (a == null || (r = a.result) == null ||
                b == null || (s = b.result) == null)
            return false;
        if (result == null) {
            if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                completeThrowable(x, r);
            else if (s instanceof AltResult && (x = ((AltResult)s).ex) != null)
                completeThrowable(x, s);
            else
                completeNull();
        }
        return true;
    }

    /** 递归构建一棵完成树 */
    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs,
                                           int lo, int hi) {
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (lo > hi) // 空
            d.result = NIL;
        else {
            CompletableFuture<?> a, b;
            int mid = (lo + hi) >>> 1;
            if ((a = (lo == mid ? cfs[lo] :
                    andTree(cfs, lo, mid))) == null ||
                    (b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] :
                            andTree(cfs, mid+1, hi)))  == null)
                throw new NullPointerException();
            if (!d.biRelay(a, b)) {
                BiRelay<?,?> c = new BiRelay<>(d, a, b);
                a.bipush(b, c);
                c.tryFire(SYNC);
            }
        }
        return d;
    }
    /* ------------- Projected (Ored) BiCompletions -------------- */

    /** 如果未完成，则将完成任务推送到this和b，除非二者都已完成 */
    final void orpush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
        if (c != null) {
            while ((b == null || b.result == null) && result == null) {
                if (tryPushStack(c)) {
                    if (b != null && b != this && b.result == null) {
                        Completion q = new CoCompletion(c);
                        while (result == null && b.result == null &&
                                !b.tryPushStack(q))
                            lazySetNext(q, null); // 失败时清除
                    }
                    break;
                }
                lazySetNext(c, null); // 失败时清除
            }
        }
    }

    @SuppressWarnings("serial")
    static final class OrApply<T,U extends T,V> extends BiCompletion<T,U,V> {
        Function<? super T,? extends V> fn;
        OrApply(Executor executor, CompletableFuture<V> dep,
                CompletableFuture<T> src,
                CompletableFuture<U> snd,
                Function<? super T,? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.orApply(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S extends R> boolean orApply(CompletableFuture<R> a,
                                          CompletableFuture<S> b,
                                          Function<? super R, ? extends T> f,
                                          OrApply<R,S,T> c) {
        Object r; Throwable x;
        if (a == null || b == null ||
                ((r = a.result) == null && (r = b.result) == null) || f == null)
            return false;
        tryComplete: if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    if ((x = ((AltResult)r).ex) != null) {
                        completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                @SuppressWarnings("unchecked") R rr = (R) r;
                completeValue(f.apply(rr));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U extends T,V> CompletableFuture<V> orApplyStage(
            Executor e, CompletionStage<U> o,
            Function<? super T, ? extends V> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<V> d = new CompletableFuture<V>();
        if (e != null || !d.orApply(this, b, f, null)) {
            OrApply<T,U,V> c = new OrApply<T,U,V>(e, d, this, b, f);
            orpush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class OrAccept<T,U extends T> extends BiCompletion<T,U,Void> {
        Consumer<? super T> fn;
        OrAccept(Executor executor, CompletableFuture<Void> dep,
                 CompletableFuture<T> src,
                 CompletableFuture<U> snd,
                 Consumer<? super T> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.orAccept(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S extends R> boolean orAccept(CompletableFuture<R> a,
                                           CompletableFuture<S> b,
                                           Consumer<? super R> f,
                                           OrAccept<R,S> c) {
        Object r; Throwable x;
        if (a == null || b == null ||
                ((r = a.result) == null && (r = b.result) == null) || f == null)
            return false;
        tryComplete: if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    if ((x = ((AltResult)r).ex) != null) {
                        completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                @SuppressWarnings("unchecked") R rr = (R) r;
                f.accept(rr);
                completeNull();
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(
            Executor e, CompletionStage<U> o, Consumer<? super T> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.orAccept(this, b, f, null)) {
            OrAccept<T,U> c = new OrAccept<T,U>(e, d, this, b, f);
            orpush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class OrRun<T,U> extends BiCompletion<T,U,Void> {
        Runnable fn;
        OrRun(Executor executor, CompletableFuture<Void> dep,
              CompletableFuture<T> src,
              CompletableFuture<U> snd,
              Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null ||
                    !d.orRun(a = src, b = snd, fn, mode > 0 ? null : this))
                return null;
            dep = null; src = null; snd = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final boolean orRun(CompletableFuture<?> a, CompletableFuture<?> b,
                        Runnable f, OrRun<?,?> c) {
        Object r; Throwable x;
        if (a == null || b == null ||
                ((r = a.result) == null && (r = b.result) == null) || f == null)
            return false;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                    completeThrowable(x, r);
                else {
                    f.run();
                    completeNull();
                }
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> o,
                                               Runnable f) {
        CompletableFuture<?> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (e != null || !d.orRun(this, b, f, null)) {
            OrRun<T,?> c = new OrRun<>(e, d, this, b, f);
            orpush(b, c);
            c.tryFire(SYNC);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class OrRelay<T,U> extends BiCompletion<T,U,Object> { // 用于组合操作
        OrRelay(CompletableFuture<Object> dep, CompletableFuture<T> src,
                CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        final CompletableFuture<Object> tryFire(int mode) {
            CompletableFuture<Object> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            if ((d = dep) == null || !d.orRelay(a = src, b = snd))
                return null;
            src = null; snd = null; dep = null;
            return d.postFire(a, b, mode);
        }
    }

    final boolean orRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        Object r;
        if (a == null || b == null ||
                ((r = a.result) == null && (r = b.result) == null))
            return false;
        if (result == null)
            completeRelay(r);
        return true;
    }

    /** 递归构建一个组合操作的完成树 */
    static CompletableFuture<Object> orTree(CompletableFuture<?>[] cfs,
                                            int lo, int hi) {
        CompletableFuture<Object> d = new CompletableFuture<Object>();
        if (lo <= hi) {
            CompletableFuture<?> a, b;
            int mid = (lo + hi) >>> 1;
            if ((a = (lo == mid ? cfs[lo] :
                    orTree(cfs, lo, mid))) == null ||
                    (b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] :
                            orTree(cfs, mid+1, hi)))  == null)
                throw new NullPointerException();
            if (!d.orRelay(a, b)) {
                OrRelay<?,?> c = new OrRelay<>(d, a, b);
                a.orpush(b, c);
                c.tryFire(SYNC);
            }
        }
        return d;
    }

    /* ------------- Zero-input Async forms -------------- */

    @SuppressWarnings("serial")
    static final class AsyncSupply<T> extends ForkJoinTask<Void>
            implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep; Supplier<T> fn;
        AsyncSupply(CompletableFuture<T> dep, Supplier<T> fn) {
            this.dep = dep; this.fn = fn;
        }

        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { run(); return true; }

        public void run() {
            CompletableFuture<T> d; Supplier<T> f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        d.completeValue(f.get());
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                d.postComplete();
            }
        }
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor e,
                                                     Supplier<U> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<U> d = new CompletableFuture<U>();
        e.execute(new AsyncSupply<U>(d, f));
        return d;
    }

    @SuppressWarnings("serial")
    static final class AsyncRun extends ForkJoinTask<Void>
            implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep; Runnable fn;
        AsyncRun(CompletableFuture<Void> dep, Runnable fn) {
            this.dep = dep; this.fn = fn;
        }

        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { run(); return true; }

        public void run() {
            CompletableFuture<Void> d; Runnable f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        f.run();
                        d.completeNull();
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                d.postComplete();
            }
        }
    }

    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        e.execute(new AsyncRun(d, f));
        return d;
    }

    /* ------------- Signallers -------------- */

    /**
     * 用于记录和释放等待线程的完成操作。此类实现ManagedBlocker以避免ForkJoinPools中
     * 出现阻塞操作堆积导致的饥饿问题。
     */
    @SuppressWarnings("serial")
    static final class Signaller extends Completion
            implements ForkJoinPool.ManagedBlocker {
        long nanos;                    // 如果计时，等待时间
        final long deadline;           // 如果计时，非零
        volatile int interruptControl; // > 0: 可中断，< 0: 已中断
        volatile Thread thread;

        Signaller(boolean interruptible, long nanos, long deadline) {
            this.thread = Thread.currentThread();
            this.interruptControl = interruptible ? 1 : 0;
            this.nanos = nanos;
            this.deadline = deadline;
        }

        final CompletableFuture<?> tryFire(int ignore) {
            Thread w; // 无需原子获取
            if ((w = thread) != null) {
                thread = null;
                LockSupport.unpark(w);
            }
            return null;
        }

        public boolean isReleasable() {
            if (thread == null)
                return true;
            if (Thread.interrupted()) {
                int i = interruptControl;
                interruptControl = -1;
                if (i > 0)
                    return true;
            }
            if (deadline != 0L &&
                    (nanos <= 0L || (nanos = deadline - System.nanoTime()) <= 0L)) {
                thread = null;
                return true;
            }
            return false;
        }

        public boolean block() {
            if (isReleasable())
                return true;
            else if (deadline == 0L)
                LockSupport.park(this);
            else if (nanos > 0L)
                LockSupport.parkNanos(this, nanos);
            return isReleasable();
        }

        final boolean isLive() { return thread != null; }
    }
    /**
     * 返回原始结果，等待完成，或null如果可中断且被中断
     */
    private Object waitingGet(boolean interruptible) {
        Signaller q = null;
        boolean queued = false;
        int spins = -1;
        Object r;
        while ((r = result) == null) {
            if (spins < 0)
                spins = (Runtime.getRuntime().availableProcessors() > 1) ?
                        1 << 8 : 0; // 在多处理器上使用短暂的自旋等待
            else if (spins > 0) {
                if (ThreadLocalRandom.nextSecondarySeed() >= 0)
                    --spins;
            }
            else if (q == null)
                q = new Signaller(interruptible, 0L, 0L);
            else if (!queued)
                queued = tryPushStack(q);
            else if (interruptible && q.interruptControl < 0) {
                q.thread = null;
                cleanStack();
                return null;
            }
            else if (q.thread != null && result == null) {
                try {
                    ForkJoinPool.managedBlock(q);
                } catch (InterruptedException ie) {
                    q.interruptControl = -1;
                }
            }
        }
        if (q != null) {
            q.thread = null;
            if (q.interruptControl < 0) {
                if (interruptible)
                    r = null; // 报告中断
                else
                    Thread.currentThread().interrupt();
            }
        }
        postComplete();
        return r;
    }

    /**
     * 返回原始结果，等待完成，或null如果被中断，或在超时时抛出TimeoutException
     */
    private Object timedGet(long nanos) throws TimeoutException {
        if (Thread.interrupted())
            return null;
        if (nanos <= 0L)
            throw new TimeoutException();
        long d = System.nanoTime() + nanos;
        Signaller q = new Signaller(true, nanos, d == 0L ? 1L : d); // 避免0
        boolean queued = false;
        Object r;
        // 我们故意不在此处进行自旋等待（如waitingGet），因为上面的nanoTime调用已经起到类似作用。
        while ((r = result) == null) {
            if (!queued)
                queued = tryPushStack(q);
            else if (q.interruptControl < 0 || q.nanos <= 0L) {
                q.thread = null;
                cleanStack();
                if (q.interruptControl < 0)
                    return null;
                throw new TimeoutException();
            }
            else if (q.thread != null && result == null) {
                try {
                    ForkJoinPool.managedBlock(q);
                } catch (InterruptedException ie) {
                    q.interruptControl = -1;
                }
            }
        }
        if (q.interruptControl < 0)
            r = null;
        q.thread = null;
        postComplete();
        return r;
    }

    /* ------------- public methods -------------- */

    /**
     * 创建一个新的未完成的 CompletableFuture。
     */
    public CompletableFuture() {
    }

    /**
     * 使用给定的编码结果创建一个新的已完成的 CompletableFuture。
     */
    private CompletableFuture(Object r) {
        this.result = r;
    }

    /**
     * 返回一个新的CompletableFuture，该任务将在{@link ForkJoinPool#commonPool()}中运行，
     * 并通过调用给定的Supplier完成。
     *
     * @param supplier 返回用于完成新CompletableFuture的值的函数
     * @param <U> 函数的返回类型
     * @return 新的CompletableFuture
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return asyncSupplyStage(asyncPool, supplier);
    }

    /**
     * 返回一个新的CompletableFuture，任务将在给定的Executor中运行，并通过调用给定的Supplier完成。
     *
     * @param supplier 返回用于完成新CompletableFuture的值的函数
     * @param executor 用于异步执行的执行器
     * @param <U> 函数的返回类型
     * @return 新的CompletableFuture
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), supplier);
    }

    /**
     * 返回一个新的CompletableFuture，该任务将在{@link ForkJoinPool#commonPool()}中运行，
     * 并在运行给定的动作后完成。
     *
     * @param runnable 在完成返回的CompletableFuture之前运行的动作
     * @return 新的CompletableFuture
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return asyncRunStage(asyncPool, runnable);
    }

    /**
     * 返回一个新的CompletableFuture，任务将在给定的Executor中运行，
     * 并在运行给定的动作后完成。
     *
     * @param runnable 在完成返回的CompletableFuture之前运行的动作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable,
                                                   Executor executor) {
        return asyncRunStage(screenExecutor(executor), runnable);
    }

    /**
     * 返回一个新的已完成的CompletableFuture，值为给定值。
     *
     * @param value 值
     * @param <U> 值的类型
     * @return 已完成的CompletableFuture
     */
    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture<U>((value == null) ? NIL : value);
    }

    /**
     * 如果以任何方式完成（正常、异常或通过取消），则返回{@code true}。
     *
     * @return {@code true} 如果已完成
     */
    public boolean isDone() {
        return result != null;
    }

    /**
     * 如有必要，等待此future完成，然后返回其结果。
     *
     * @return 结果值
     * @throws CancellationException 如果此future已被取消
     * @throws ExecutionException 如果此future以异常方式完成
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    public T get() throws InterruptedException, ExecutionException {
        Object r;
        return reportGet((r = result) == null ? waitingGet(true) : r);
    }

    /**
     * 如有必要，最多等待给定时间以使此future完成，然后返回其结果（如果可用）。
     *
     * @param timeout 等待的最长时间
     * @param unit 超时参数的时间单位
     * @return 结果值
     * @throws CancellationException 如果此future已被取消
     * @throws ExecutionException 如果此future以异常方式完成
     * @throws InterruptedException 如果当前线程在等待时被中断
     * @throws TimeoutException 如果等待超时
     */
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Object r;
        long nanos = unit.toNanos(timeout);
        return reportGet((r = result) == null ? timedGet(nanos) : r);
    }

    /**
     * 返回完成时的结果值，或者在以异常方式完成时抛出(未经检查的)异常。
     * 为了更好地符合常用的函数形式，如果完成此CompletableFuture的计算抛出了异常，
     * 此方法将抛出(未经检查的) {@link CompletionException}，其中包含底层异常作为其原因。
     *
     * @return 结果值
     * @throws CancellationException 如果计算被取消
     * @throws CompletionException 如果此future以异常方式完成或完成计算抛出异常
     */
    public T join() {
        Object r;
        return reportJoin((r = result) == null ? waitingGet(false) : r);
    }

    /**
     * 如果已完成，则返回结果值（或抛出任何遇到的异常），否则返回给定的valueIfAbsent。
     *
     * @param valueIfAbsent 如果未完成，则返回的值
     * @return 结果值（如果已完成），否则返回给定的valueIfAbsent
     * @throws CancellationException 如果计算被取消
     * @throws CompletionException 如果此future以异常方式完成或完成计算抛出异常
     */
    public T getNow(T valueIfAbsent) {
        Object r;
        return ((r = result) == null) ? valueIfAbsent : reportJoin(r);
    }

    /**
     * 如果尚未完成，则将通过{@link #get()}和相关方法返回的值设置为给定值。
     *
     * @param value 结果值
     * @return {@code true} 如果此调用导致此CompletableFuture进入完成状态，则返回{@code false}
     */
    public boolean complete(T value) {
        boolean triggered = completeValue(value);
        postComplete();
        return triggered;
    }

    /**
     * 如果尚未完成，则导致{@link #get()}和相关方法抛出给定异常。
     *
     * @param ex 异常
     * @return {@code true} 如果此调用导致此CompletableFuture进入完成状态，则返回{@code false}
     */
    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        boolean triggered = internalComplete(new AltResult(ex));
        postComplete();
        return triggered;
    }
    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此
     * CompletableFuture完成时通过给定函数的结果进行完成，
     * 如果此CompletableFuture异常完成，抛出异常。
     *
     * @param fn 用于计算新CompletableFuture结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenApply(
            Function<? super T,? extends U> fn) {
        return uniApplyStage(null, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时通过给定函数的结果进行完成。
     *
     * @param fn 用于计算新CompletableFuture结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenApplyAsync(
            Function<? super T,? extends U> fn) {
        return uniApplyStage(asyncPool, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时通过给定函数的结果进行完成，并使用提供的Executor。
     *
     * @param fn 用于计算新CompletableFuture结果的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenApplyAsync(
            Function<? super T,? extends U> fn, Executor executor) {
        return uniApplyStage(screenExecutor(executor), fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此
     * CompletableFuture完成时执行给定操作。
     *
     * @param action 要执行的操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时执行给定操作。
     *
     * @param action 要执行的操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(asyncPool, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时执行给定操作，并使用提供的Executor。
     *
     * @param action 要执行的操作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action,
                                                   Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此
     * CompletableFuture完成时执行给定的Runnable任务。
     *
     * @param action 要执行的Runnable
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时执行给定的Runnable任务。
     *
     * @param action 要执行的Runnable
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(asyncPool, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此
     * CompletableFuture完成时执行给定的Runnable任务，并使用提供的Executor。
     *
     * @param action 要执行的Runnable
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> thenRunAsync(Runnable action,
                                                Executor executor) {
        return uniRunStage(screenExecutor(executor), action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 和其他CompletionStage都完成时使用两个结果并通过提供的函数计算。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于合并两个结果的函数
     * @return 新的CompletableFuture
     */
    public <U,V> CompletableFuture<V> thenCombine(
            CompletionStage<? extends U> other,
            BiFunction<? super T,? super U,? extends V> fn) {
        return biApplyStage(null, other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时使用两个结果并通过提供的函数计算。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于合并两个结果的函数
     * @return 新的CompletableFuture
     */
    public <U,V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T,? super U,? extends V> fn) {
        return biApplyStage(asyncPool, other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时使用两个结果并通过提供的函数计算，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于合并两个结果的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U,V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T,? super U,? extends V> fn, Executor executor) {
        return biApplyStage(screenExecutor(executor), other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 和其他CompletionStage都完成时执行两个结果上的提供操作。
     *
     * @param other 另一个CompletionStage
     * @param action 操作
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时执行两个结果上的提供操作。
     *
     * @param other 另一个CompletionStage
     * @param action 操作
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(asyncPool, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时执行两个结果上的提供操作，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param action 操作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 和其他CompletionStage都完成时执行提供的Runnable操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other,
                                                Runnable action) {
        return biRunStage(null, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时执行提供的Runnable操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other,
                                                     Runnable action) {
        return biRunStage(asyncPool, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 和其他CompletionStage都完成时执行提供的Runnable操作，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other,
                                                     Runnable action,
                                                     Executor executor) {
        return biRunStage(screenExecutor(executor), other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 或其他CompletionStage任一完成时使用第一个完成结果并通过提供的函数计算。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于第一个完成结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> applyToEither(
            CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(null, other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时使用第一个完成结果并通过提供的函数计算。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于第一个完成结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(asyncPool, other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时使用第一个完成结果并通过提供的函数计算，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param fn 用于第一个完成结果的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return orApplyStage(screenExecutor(executor), other, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Consumer操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Consumer操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> acceptEither(
            CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(null, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Consumer操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Consumer操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(asyncPool, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Consumer操作，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Consumer操作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return orAcceptStage(screenExecutor(executor), other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Runnable操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other,
                                                  Runnable action) {
        return orRunStage(null, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Runnable操作。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                       Runnable action) {
        return orRunStage(asyncPool, other, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 或其他CompletionStage任一完成时执行提供的Runnable操作，并使用提供的Executor。
     *
     * @param other 另一个CompletionStage
     * @param action 要执行的Runnable操作
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                       Runnable action,
                                                       Executor executor) {
        return orRunStage(screenExecutor(executor), other, action);
    }
    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 完成时通过给定的函数计算一个新的CompletionStage。
     *
     * @param fn 用于此CompletableFuture结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(null, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时通过给定的函数计算一个新的CompletionStage。
     *
     * @param fn 用于此CompletableFuture结果的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(asyncPool, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时通过给定的函数计算一个新的CompletionStage，并使用提供的Executor。
     *
     * @param fn 用于此CompletableFuture结果的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return uniComposeStage(screenExecutor(executor), fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 完成时通过给定的函数处理结果或异常。
     *
     * @param action 当CompletableFuture完成时执行的函数
     * @return 新的CompletableFuture
     */
    public CompletableFuture<T> whenComplete(
            BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时通过给定的函数处理结果或异常。
     *
     * @param action 当CompletableFuture完成时执行的函数
     * @return 新的CompletableFuture
     */
    public CompletableFuture<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(asyncPool, action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时通过给定的函数处理结果或异常，并使用提供的Executor。
     *
     * @param action 当CompletableFuture完成时执行的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public CompletableFuture<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在此CompletableFuture
     * 完成时使用给定的函数处理结果或异常。
     *
     * @param fn 处理结果或异常的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> handle(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(null, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时使用给定的函数处理结果或异常。
     *
     * @param fn 处理结果或异常的函数
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(asyncPool, fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture异步地在此CompletableFuture
     * 完成时使用给定的函数处理结果或异常，并使用提供的Executor。
     *
     * @param fn 处理结果或异常的函数
     * @param executor 用于异步执行的执行器
     * @return 新的CompletableFuture
     */
    public <U> CompletableFuture<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return uniHandleStage(screenExecutor(executor), fn);
    }

    /**
     * 返回这个 CompletableFuture.
     *
     * @return this CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // 不在CompletionStage接口的方法

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture通过给定函数处理此CompletableFuture的异常，
     * 如果没有异常发生，则使用相同的正常值。
     *
     * @param fn 处理异常的函数
     * @return 新的CompletableFuture
     */
    public CompletableFuture<T> exceptionally(
            Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(fn);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在所有给定的CompletableFuture完成后完成。
     * 如果其中一个CompletableFuture异常完成，那么返回的CompletableFuture也将异常完成。
     *
     * @param cfs CompletableFuture的集合
     * @return 一个新的CompletableFuture，当所有的CompletableFuture完成时完成
     */
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    /**
     * 返回一个新的CompletableFuture，该CompletableFuture在任意一个给定的CompletableFuture完成后完成，
     * 并且带有相同的结果。如果其中一个CompletableFuture异常完成，返回的CompletableFuture也会异常完成。
     *
     * @param cfs CompletableFuture的集合
     * @return 一个新的CompletableFuture，当任意一个CompletableFuture完成时完成
     */
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        return orTree(cfs, 0, cfs.length - 1);
    }

    /* ------------- 控制和状态方法 -------------- */

    /**
     * 如果尚未完成，则用CancellationException完成此CompletableFuture。
     * 依赖的CompletableFutures将以CompletionException异常完成，并且该CompletionException由CancellationException引起。
     *
     * @param mayInterruptIfRunning 此实现中该值无效，因为中断不用于控制处理
     * @return 如果任务现在取消了，则返回true
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = (result == null) &&
                internalComplete(new AltResult(new CancellationException()));
        postComplete();
        return cancelled || isCancelled();
    }

    /**
     * 如果此CompletableFuture在正常完成之前被取消，则返回true。
     *
     * @return 如果此CompletableFuture在正常完成之前被取消，则返回true
     */
    public boolean isCancelled() {
        Object r;
        return ((r = result) instanceof AltResult) &&
                (((AltResult)r).ex instanceof CancellationException);
    }

    /**
     * 如果此CompletableFuture以任何形式异常完成，则返回true。
     * 完成的可能原因包括取消、显式调用completeExceptionally、或CompletionStage动作的异常终止。
     *
     * @return 如果此CompletableFuture以任何形式异常完成，则返回true
     */
    public boolean isCompletedExceptionally() {
        Object r;
        return ((r = result) instanceof AltResult) && r != NIL;
    }

    /**
     * 强制设置或重置此CompletableFuture的值，后续调用get()和相关方法将返回该值，
     * 无论此CompletableFuture是否已经完成。
     * 这个方法主要用于错误恢复操作，并且即使在这种情况下，依赖的CompletableFuture可能仍会使用原来的结果。
     *
     * @param value 要设置的完成值
     */
    public void obtrudeValue(T value) {
        result = (value == null) ? NIL : value;
        postComplete();
    }

    /**
     * 强制使后续对get()和相关方法的调用抛出给定的异常，忽略此CompletableFuture是否已经完成。
     * 这个方法主要用于错误恢复操作，即使在这种情况下，依赖的CompletableFuture可能仍会使用原来的结果。
     *
     * @param ex 要抛出的异常
     * @throws NullPointerException 如果异常为null
     */
    public void obtrudeException(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        result = new AltResult(ex);
        postComplete();
    }

    /**
     * 返回等待此CompletableFuture完成的依赖CompletableFutures的估计数量。
     * 该方法主要用于系统状态监控，而不是用于同步控制。
     *
     * @return 依赖此CompletableFuture完成的CompletableFutures的数量
     */
    public int getNumberOfDependents() {
        int count = 0;
        for (Completion p = stack; p != null; p = p.next)
            ++count;
        return count;
    }

    /**
     * 返回一个标识此CompletableFuture及其完成状态的字符串。
     * 状态在方括号中，可能的值为“Completed Normally”或“Completed Exceptionally”，
     * 如果尚未完成，可能会显示等待它完成的CompletableFutures的数量。
     *
     * @return 标识此CompletableFuture及其状态的字符串
     */
    public String toString() {
        Object r = result;
        int count;
        return super.toString() +
                ((r == null) ?
                        (((count = getNumberOfDependents()) == 0) ?
                                "[Not completed]" :
                                "[Not completed, " + count + " dependents]") :
                        (((r instanceof AltResult) && ((AltResult)r).ex != null) ?
                                "[Completed exceptionally]" :
                                "[Completed normally]"));
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long RESULT;
    private static final long STACK;
    private static final long NEXT;
    static {
        try {
            final sun.misc.Unsafe u;
            UNSAFE = u = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CompletableFuture.class;
            RESULT = u.objectFieldOffset(k.getDeclaredField("result"));
            STACK = u.objectFieldOffset(k.getDeclaredField("stack"));
            NEXT = u.objectFieldOffset
                    (Completion.class.getDeclaredField("next"));
        } catch (Exception x) {
            throw new Error(x);
        }
    }
}









