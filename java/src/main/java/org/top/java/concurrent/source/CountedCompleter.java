package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午2:24
 */

import java.util.concurrent.RecursiveAction;
import org.top.java.lang.source.Thread;
/**
 * 一个具有完成操作的 {@link ForkJoinTask}，当触发且没有剩余的挂起操作时执行。
 * 与其他形式的 ForkJoinTasks 相比，CountedCompleter 通常在子任务挂起和阻塞的情况下更稳健，
 * 但编程起来不太直观。使用 CountedCompleter 类似于其他基于完成的组件的使用
 * （例如 {@link java.nio.channels.CompletionHandler}），不同之处在于可能需要多个
 * <em>挂起</em> 完成才能触发完成操作 {@link #onCompletion(CountedCompleter)}，而不仅仅是一个。
 * 除非另外初始化，否则 {@linkplain #getPendingCount 挂起计数} 初始值为零，
 * 但可以使用 {@link #setPendingCount}、{@link #addToPendingCount} 和 {@link #compareAndSetPendingCount}
 * 方法（原子性地）更改。调用 {@link #tryComplete} 时，如果挂起操作计数不为零，则将其递减；
 * 否则，将执行完成操作，并且如果此 completer 本身有 completer，则继续与其 completer 一起进行。
 * 与类似的同步组件（如 {@link java.util.concurrent.Phaser Phaser} 和
 * {@link java.util.concurrent.Semaphore Semaphore}）一样，这些方法只影响内部计数；
 * 它们不建立任何进一步的内部簿记。特别是，挂起任务的身份不会被记录。如下面所示，
 * 可以创建在需要时记录一些或所有挂起任务或其结果的子类。如下面所示，还提供了支持自定义完成遍历的实用方法。
 * 然而，由于 CountedCompleters 仅提供基本的同步机制，因此创建进一步的抽象子类以维护链接、字段和适用于
 * 一组相关用法的其他支持方法可能是有用的。
 *
 * <p>具体的 CountedCompleter 类必须定义 {@link #compute} 方法，在大多数情况下
 * （如下面所示），在返回之前应调用 {@code tryComplete()} 一次。类还可以选择性地重写
 * {@link #onCompletion(CountedCompleter)} 方法以在正常完成时执行某个操作，并重写
 * {@link #onExceptionalCompletion(Throwable, CountedCompleter)} 方法以在出现异常时执行某个操作。
 *
 * <p>CountedCompleter 通常不产生结果，在这种情况下，它们通常声明为 {@code CountedCompleter<Void>}，
 * 并且始终返回 {@code null} 作为结果值。在其他情况下，应重写 {@link #getRawResult} 方法，
 * 以便从 {@code join(), invoke()} 及相关方法中提供结果。通常，此方法应返回一个字段的值
 * （或一个或多个字段的函数），该字段在 CountedCompleter 对象完成时保存结果。
 * {@link #setRawResult} 方法默认情况下在 CountedCompleters 中不起作用。
 * 可以（但很少适用）重写此方法以维护其他对象或字段来保存结果数据。
 *
 * <p>一个没有 completer 的 CountedCompleter（即 {@link #getCompleter} 返回 {@code null} 的）可以作为具有
 * 额外功能的常规 ForkJoinTask 使用。然而，任何在其后还有另一个 completer 的 completer 只作为其他计算的内部帮助器，
 * 因此其自身的任务状态（如方法 {@link ForkJoinTask#isDone} 中报告的）是任意的；
 * 此状态仅在显式调用 {@link #complete}、{@link ForkJoinTask#cancel}、
 * {@link ForkJoinTask#completeExceptionally(Throwable)} 或在 {@code compute} 方法的异常完成时改变。
 * 在任何异常完成时，异常可以传递给任务的 completer（以及其 completer，依此类推），
 * 如果存在且尚未完成。同样，取消内部 CountedCompleter 只会对该 completer 产生局部影响，
 * 因此通常没有太大用处。
 *
 * <p><b>示例用法。</b>
 *
 * <p><b>并行递归分解。</b> CountedCompleters 可以安排在类似于通常与 {@link RecursiveAction}s 一起使用的树中，
 * 尽管设置它们所涉及的构造通常有所不同。在这里，每个任务的 completer 是其在计算树中的父节点。
 * 即使它们需要更多的簿记，CountedCompleters 在应用于数组或集合的可能耗时的操作（无法进一步细分）
 * 时可能是更好的选择；特别是当操作完成的时间因某些元素的不同而显著不同时，
 * 无论是由于内在差异（例如 I/O）还是辅助影响（如垃圾回收）。
 * 由于 CountedCompleters 提供了它们自己的延续，其他线程不需要阻塞等待执行它们。
 *
 * <p>例如，下面是一个使用二分递归分解将工作分解为单个部分（叶子任务）的类的初始版本。
 * 即使将工作分解为单个调用，基于树的技术通常比直接分叉叶子任务更可取，
 * 因为它们减少了线程间的通信并改善了负载平衡。在递归情况下，第二个完成的子任务触发其父任务的完成
 * （由于未执行结果组合，因此未重写 {@code onCompletion} 方法的默认空操作实现）。
 * 一个静态实用方法设置基本任务并调用它（在此，隐式使用 {@link ForkJoinPool#commonPool()}）。
 * <pre> {@code
 * class MyOperation<E> { void apply(E e) { ... }  }
 *
 * class ForEach<E> extends CountedCompleter<Void> {
 *
 *   public static <E> void forEach(E[] array, MyOperation<E> op) {
 *     new ForEach<E>(null, array, op, 0, array.length).invoke();
 *   }
 *
 *   final E[] array; final MyOperation<E> op; final int lo, hi;
 *   ForEach(CountedCompleter<?> p, E[] array, MyOperation<E> op, int lo, int hi) {
 *     super(p);
 *     this.array = array; this.op = op; this.lo = lo; this.hi = hi;
 *   }
 *
 *   public void compute() { // 版本1
 *     if (hi - lo >= 2) {
 *       int mid = (lo + hi) >>> 1;
 *       setPendingCount(2); // 必须在分叉之前设置挂起计数
 *       new ForEach(this, array, op, mid, hi).fork(); // 右子任务
 *       new ForEach(this, array, op, lo, mid).fork(); // 左子任务
 *     }
 *     else if (hi > lo)
 *       op.apply(array[lo]);
 *     tryComplete();
 *   }
 * }}</pre>
 *
 * 这个设计可以通过观察递归情况来改进，任务在分叉其右子任务后没有其他操作需要执行，
 * 因此可以直接调用其左子任务并返回。（这是尾递归消除的类比）。此外，因为任务在执行完其左子任务后就返回了
 * （而不是继续执行 {@code tryComplete}），挂起计数设置为 1：
 *
 * <pre> {@code
 * class ForEach<E> ...
 *   public void compute() { // 版本2
 *     if (hi - lo >= 2) {
 *       int mid = (lo + hi) >>> 1;
 *       setPendingCount(1); // 只有一个挂起任务
 *       new ForEach(this, array, op, mid, hi).fork(); // 右子任务
 *       new ForEach(this, array, op, lo, mid).compute(); // 直接调用
 *     }
 *     else {
 *       if (hi > lo)
 *         op.apply(array[lo]);
 *       tryComplete();
 *     }
 *   }
 * }</pre>
 *
 * 进一步改进，可以注意到左子任务甚至不需要存在。我们可以迭代使用原始任务，而不是创建新的任务，
 * 并为每个分叉任务增加挂起计数。此外，因为树中的任何任务都没有实现
 * {@link #onCompletion(CountedCompleter)} 方法，{@code tryComplete()} 可以替换为 {@link #propagateCompletion}。
 *
 * <pre> {@code
 * class ForEach<E> ...
 *   public void compute() { // 版本3
 *     int l = lo,  h = hi;
 *     while (h - l >= 2) {
 *       int mid = (l + h) >>> 1;
 *       addToPendingCount(1);
 *       new ForEach(this, array, op, mid, h).fork(); // 右子任务
 *       h = mid;
 *     }
 *     if (h > l)
 *       op.apply(array[l]);
 *     propagateCompletion();
 *   }
 * }</pre>
 *
 * 这种类的额外改进可能包括预计算挂起计数，以便可以在构造函数中设定，针对叶节点步骤进行类专门化，
 * 使用四分法代替每次迭代的二分法，并使用自适应阈值而不是总是分解到单个元素。
 *
 * <p><b>搜索。</b> CountedCompleter 的树可以在数据结构的不同部分中搜索一个值或属性，
 * 并在 {@link java.util.concurrent.atomic.AtomicReference AtomicReference} 中报告结果，一旦找到结果，其他任务可以轮询结果以避免不必要的工作。
 * （你也可以 {@linkplain #cancel 取消} 其他任务，但通常更简单且更高效的方法是让它们注意到结果已设置，然后跳过进一步的处理）。
 * 再次以数组为例，使用完全分区法（实际上，叶任务几乎总是处理多个元素）：
 *
 * <pre> {@code
 * class Searcher<E> extends CountedCompleter<E> {
 *   final E[] array; final AtomicReference<E> result; final int lo, hi;
 *   Searcher(CountedCompleter<?> p, E[] array, AtomicReference<E> result, int lo, int hi) {
 *     super(p);
 *     this.array = array; this.result = result; this.lo = lo; this.hi = hi;
 *   }
 *   public E getRawResult() { return result.get(); }
 *   public void compute() { // 类似于 ForEach 版本3
 *     int l = lo,  h = hi;
 *     while (result.get() == null && h >= l) {
 *       if (h - l >= 2) {
 *         int mid = (l + h) >>> 1;
 *         addToPendingCount(1);
 *         new Searcher(this, array, result, mid, h).fork();
 *         h = mid;
 *       }
 *       else {
 *         E x = array[l];
 *         if (matches(x) && result.compareAndSet(null, x))
 *           quietlyCompleteRoot(); // 根任务现在可以 join 了
 *         break;
 *       }
 *     }
 *     tryComplete(); // 正常完成，无论是否找到
 *   }
 *   boolean matches(E e) { ... } // 返回 true 如果找到
 *
 *   public static <E> E search(E[] array) {
 *       return new Searcher<E>(null, array, new AtomicReference<E>(), 0, array.length).invoke();
 *   }
 * }}</pre>
 * 在这个示例中，以及其他仅与 `compareAndSet` 一个公共结果相关的任务中，最后无条件调用 `tryComplete` 可以改为有条件的
 * (`if (result.get() == null) tryComplete();`)，因为一旦根任务完成，管理完成的进一步操作就不再需要任何额外的簿记。
 *
 * <p><b>记录子任务。</b> 合并多个子任务结果的 CountedCompleter 任务通常需要在 {@link #onCompletion(CountedCompleter)} 方法中访问这些结果。
 * 如以下类所示（该类执行一种简化形式的 map-reduce，其中映射和归约都属于类型 {@code E}），一种常见的方式是让每个子任务记录其兄弟任务，
 * 以便可以在 {@code onCompletion} 方法中访问。这种技术适用于合并左右结果顺序不重要的归约操作；
 * 如果需要有序归约，则需要显式的左/右指定。此技术可以应用于之前示例中看到的其他简化流程。
 *
 * <pre> {@code
 * class MyMapper<E> { E apply(E v) {  ...  } }
 * class MyReducer<E> { E apply(E x, E y) {  ...  } }
 * class MapReducer<E> extends CountedCompleter<E> {
 *   final E[] array; final MyMapper<E> mapper;
 *   final MyReducer<E> reducer; final int lo, hi;
 *   MapReducer<E> sibling;
 *   E result;
 *   MapReducer(CountedCompleter<?> p, E[] array, MyMapper<E> mapper,
 *              MyReducer<E> reducer, int lo, int hi) {
 *     super(p);
 *     this.array = array; this.mapper = mapper;
 *     this.reducer = reducer; this.lo = lo; this.hi = hi;
 *   }
 *   public void compute() {
 *     if (hi - lo >= 2) {
 *       int mid = (lo + hi) >>> 1;
 *       MapReducer<E> left = new MapReducer(this, array, mapper, reducer, lo, mid);
 *       MapReducer<E> right = new MapReducer(this, array, mapper, reducer, mid, hi);
 *       left.sibling = right;
 *       right.sibling = left;
 *       setPendingCount(1); // 只有右侧挂起
 *       right.fork();
 *       left.compute();     // 直接执行左侧任务
 *     }
 *     else {
 *       if (hi > lo)
 *           result = mapper.apply(array[lo]);
 *       tryComplete();
 *     }
 *   }
 *   public void onCompletion(CountedCompleter<?> caller) {
 *     if (caller != this) {
 *       MapReducer<E> child = (MapReducer<E>)caller;
 *       MapReducer<E> sibling = child.sibling;
 *       if (sibling == null || sibling.result == null)
 *         result = child.result;
 *       else
 *         result = reducer.apply(child.result, sibling.result);
 *     }
 *   }
 *   public E getRawResult() { return result; }
 *
 *   public static <E> E mapReduce(E[] array, MyMapper<E> mapper, MyReducer<E> reducer) {
 *     return new MapReducer<E>(null, array, mapper, reducer,
 *                              0, array.length).invoke();
 *   }
 * }}</pre>
 *
 * 在此示例中，方法 {@code onCompletion} 的形式在许多合并结果的完成设计中常见。
 * 此回调样式的方法每个任务触发一次，触发方式有两种情况：（1）当任务自己调用 {@code tryComplete} 且挂起计数为 0 时，
 * 或（2）当其任何子任务完成并将挂起计数减少到 0 时。通过 `caller` 参数可以区分这两种情况。大多数情况下，当 `caller` 是 {@code this} 时，无需执行任何操作。
 * 否则，`caller` 参数可以用作提供值（和/或链接到其他值）的来源，供合并使用。假设正确使用了挂起计数，则 {@code onCompletion} 内的操作
 * （每个任务及其子任务完成时执行一次）是线程安全的，无需额外同步操作。
 *
 * <p><b>完成遍历</b>。如果使用 {@code onCompletion} 来处理完成操作不适用或不方便，您可以使用 {@link #firstComplete} 和 {@link #nextComplete}
 * 方法来创建自定义的遍历。例如，要定义一个类似第三个 ForEach 示例中的只拆分右侧任务的 `MapReducer`，完成操作必须协作地沿着未完成的子任务链接进行归约，
 * 这可以通过如下方式完成：
 *
 * <pre> {@code
 * class MapReducer<E> extends CountedCompleter<E> { // 版本2
 *   final E[] array; final MyMapper<E> mapper;
 *   final MyReducer<E> reducer; final int lo, hi;
 *   MapReducer<E> forks, next; // 记录子任务分叉的链表
 *   E result;
 *   MapReducer(CountedCompleter<?> p, E[] array, MyMapper<E> mapper,
 *              MyReducer<E> reducer, int lo, int hi, MapReducer<E> next) {
 *     super(p);
 *     this.array = array; this.mapper = mapper; this.reducer = reducer;
 *     this.lo = lo; this.hi = hi; this.next = next;
 *   }
 *   public void compute() {
 *     int l = lo,  h = hi;
 *     while (h - l >= 2) {
 *       int mid = (l + h) >>> 1;
 *       addToPendingCount(1);
 *       (forks = new MapReducer(this, array, mapper, reducer, mid, h, forks)).fork();
 *       h = mid;
 *     }
 *     if (h > l)
 *       result = mapper.apply(array[l]);
 *     // 通过减少并推进子任务链来处理完成操作
 *     for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
 *       for (MapReducer t = (MapReducer)c, s = t.forks;  s != null; s = t.forks = s.next)
 *         t.result = reducer.apply(t.result, s.result);
 *     }
 *   }
 *   public E getRawResult() { return result; }
 *
 *   public static <E> E mapReduce(E[] array, MyMapper<E> mapper, MyReducer<E> reducer) {
 *     return new MapReducer<E>(null, array, mapper, reducer,
 *                              0, array.length, null).invoke();
 *   }
 * }}</pre>
 *
 * <p><b>触发器。</b> 某些 CountedCompleter 本身从不被分叉，但作为其他设计中的触发器；
 * 包括那些通过一个或多个异步任务的完成来触发另一个异步任务的设计。例如：
 *
 * <pre> {@code
 * class HeaderBuilder extends CountedCompleter<...> { ... }
 * class BodyBuilder extends CountedCompleter<...> { ... }
 * class PacketSender extends CountedCompleter<...> {
 *   PacketSender(...) { super(null, 1); ... } // 在第二次完成时触发
 *   public void compute() { } // 永不调用
 *   public void onCompletion(CountedCompleter<?> caller) { sendPacket(); }
 * }
 * // 示例使用：
 * PacketSender p = new PacketSender();
 * new HeaderBuilder(p, ...).fork();
 * new BodyBuilder(p, ...).fork();
 * }</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long serialVersionUID = 5232453752276485070L;

    /** 此任务的 completer（完成器），如果没有则为 null */
    final CountedCompleter<?> completer;
    /** 到完成前的挂起任务数 */
    volatile int pending;

    /**
     * 创建一个带有指定 completer 和初始挂起计数的 CountedCompleter。
     *
     * @param completer 此任务的 completer，如果没有则为 {@code null}
     * @param initialPendingCount 初始挂起计数
     */
    protected CountedCompleter(CountedCompleter<?> completer, int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    /**
     * 创建一个带有指定 completer 且挂起计数为零的 CountedCompleter。
     *
     * @param completer 此任务的 completer，如果没有则为 {@code null}
     */
    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    /**
     * 创建一个没有 completer 且挂起计数为零的 CountedCompleter。
     */
    protected CountedCompleter() {
        this.completer = null;
    }

    /**
     * 此任务执行的主要计算。
     */
    public abstract void compute();

    /**
     * 当调用 {@link #tryComplete} 方法且挂起计数为零时，或者调用了无条件的 {@link #complete} 方法时执行操作。
     * 默认情况下，此方法不执行任何操作。您可以通过检查给定的 `caller` 参数来区分情况。如果不等于 {@code this}，
     * 那么通常它是一个子任务，可能包含要合并的结果（和/或链接到其他结果）。
     *
     * @param caller 调用此方法的任务（可能是该任务本身）
     */
    public void onCompletion(CountedCompleter<?> caller) {
    }

    /**
     * 当调用 {@link #completeExceptionally(Throwable)} 方法或 {@link #compute} 方法抛出异常时执行操作，
     * 且此任务尚未正常完成。在进入此方法时，此任务 {@link ForkJoinTask#isCompletedAbnormally}。
     * 该方法的返回值决定了异常是否传播：如果 {@code true} 且此任务有一个未完成的 completer，
     * 则此 completer 也将使用与此 completer 相同的异常完成。
     * 此方法的默认实现不执行任何操作，除了返回 {@code true}。
     *
     * @param ex 抛出的异常
     * @param caller 调用此方法的任务（可能是该任务本身）
     * @return 如果此异常应传播到此任务的 completer（如果存在），则返回 {@code true}
     */
    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
        return true;
    }

    /**
     * 返回在此任务的构造函数中建立的 completer，如果没有则返回 {@code null}。
     *
     * @return completer
     */
    public final CountedCompleter<?> getCompleter() {
        return completer;
    }

    /**
     * 返回当前的挂起计数。
     *
     * @return 当前的挂起计数
     */
    public final int getPendingCount() {
        return pending;
    }

    /**
     * 将挂起计数设置为指定值。
     *
     * @param count 挂起计数
     */
    public final void setPendingCount(int count) {
        pending = count;
    }

    /**
     * （原子地）将给定值添加到挂起计数。
     *
     * @param delta 要添加的值
     */
    public final void addToPendingCount(int delta) {
        U.getAndAddInt(this, PENDING, delta);
    }

    /**
     * 仅当当前值与预期值相等时，原子地将挂起计数设置为给定值。
     *
     * @param expected 预期值
     * @param count 新值
     * @return 如果成功则返回 {@code true}
     */
    public final boolean compareAndSetPendingCount(int expected, int count) {
        return U.compareAndSwapInt(this, PENDING, expected, count);
    }

    /**
     * 如果挂起计数非零，则原子地将其递减。
     *
     * @return 此方法进入时的初始（未递减的）挂起计数
     */
    public final int decrementPendingCountUnlessZero() {
        int c;
        do {} while ((c = pending) != 0 &&
                !U.compareAndSwapInt(this, PENDING, c, c - 1));
        return c;
    }

    /**
     * 返回当前计算的根任务；即如果该任务没有 completer，则返回该任务，否则返回其 completer 的根任务。
     *
     * @return 当前计算的根任务
     */
    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> a = this, p;
        while ((p = a.completer) != null)
            a = p;
        return a;
    }

    /**
     * 如果挂起计数为非零，则递减该计数；否则调用 {@link #onCompletion(CountedCompleter)}，
     * 然后类似地尝试完成此任务的 completer（如果存在），否则标记该任务为已完成。
     */
    public final void tryComplete() {
        CountedCompleter<?> a = this, s = a;
        for (int c;;) {
            if ((c = a.pending) == 0) {
                a.onCompletion(s);
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(a, PENDING, c, c - 1)) {
                return;
            }
        }
    }

    /**
     * 等效于 {@link #tryComplete}，但不在完成路径上调用 {@link #onCompletion(CountedCompleter)}。
     */
    public final void propagateCompletion() {
        CountedCompleter<?> a = this, s = a;
        for (int c;;) {
            if ((c = a.pending) == 0) {
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(a, PENDING, c, c - 1)) {
                return;
            }
        }
    }

    /**
     * 无论挂起计数如何，调用 {@link #onCompletion(CountedCompleter)}，将该任务标记为已完成，
     * 并进一步触发该任务 completer 的 {@link #tryComplete}，如果有的话。
     */
    public void complete(T rawResult) {
        CountedCompleter<?> p;
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        if ((p = completer) != null)
            p.tryComplete();
    }

    /**
     * 如果此任务的挂起计数为零，则返回此任务；否则递减其挂起计数并返回 {@code null}。
     * 该方法旨在与 {@link #nextComplete} 一起用于完成遍历循环。
     *
     * @return 如果挂起计数为零，则返回此任务；否则返回 {@code null}
     */
    public final CountedCompleter<?> firstComplete() {
        for (int c;;) {
            if ((c = pending) == 0)
                return this;
            else if (U.compareAndSwapInt(this, PENDING, c, c - 1))
                return null;
        }
    }

    /**
     * 如果此任务没有 completer，则调用 {@link ForkJoinTask#quietlyComplete} 并返回 {@code null}。
     * 否则，如果 completer 的挂起计数非零，则递减该挂起计数并返回 {@code null}。
     * 否则，返回 completer。此方法可用作同类任务层次结构完成遍历循环的一部分：
     */
    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> p;
        if ((p = completer) != null)
            return p.firstComplete();
        else {
            quietlyComplete();
            return null;
        }
    }

    /**
     * 等效于 {@code getRoot().quietlyComplete()}。
     */
    public final void quietlyCompleteRoot() {
        for (CountedCompleter<?> a = this, p;;) {
            if ((p = a.completer) == null) {
                a.quietlyComplete();
                return;
            }
            a = p;
        }
    }

    /**
     * 如果此任务尚未完成，尝试处理最多给定数量的其他未处理任务，这些任务位于此任务的完成路径上。
     *
     * @param maxTasks 要处理的最大任务数。如果小于或等于零，则不处理任何任务。
     */
    public final void helpComplete(int maxTasks) {
        Thread t; ForkJoinWorkerThread wt;
        if (maxTasks > 0 && status >= 0) {
            if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
                (wt = (ForkJoinWorkerThread)t).pool.helpComplete(wt.workQueue, this, maxTasks);
            else
                ForkJoinPool.common.externalHelpComplete(this, maxTasks);
        }
    }

    /**
     * 支持 ForkJoinTask 异常传播。
     */
    void internalPropagateException(Throwable ex) {
        CountedCompleter<?> a = this, s = a;
        while (a.onExceptionalCompletion(ex, s) &&
                (a = (s = a).completer) != null && a.status >= 0 &&
                a.recordExceptionalCompletion(ex) == EXCEPTIONAL)
            ;
    }

    /**
     * 实现 CountedCompleters 的执行约定。
     */
    protected final boolean exec() {
        compute();
        return false;
    }

    /**
     * 返回计算结果。默认返回 {@code null}，这对于 {@code Void} 动作是合适的，
     * 但在其他情况下，应该重写此方法，几乎总是返回完成时保存结果的字段或字段函数。
     *
     * @return 计算结果
     */
    public T getRawResult() { return null; }

    /**
     * 一个可选的 result-bearing CountedCompleters 方法帮助维护结果数据。默认不执行任何操作。
     * 不推荐重写此方法。但是，如果要覆盖此方法以更新现有对象或字段，则必须通常定义为线程安全。
     */
    protected void setRawResult(T t) { }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long PENDING;
    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            PENDING = U.objectFieldOffset(CountedCompleter.class.getDeclaredField("pending"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}

