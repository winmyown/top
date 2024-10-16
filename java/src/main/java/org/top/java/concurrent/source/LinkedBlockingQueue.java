package org.top.java.concurrent.source;

import org.top.java.concurrent.source.locks.ReentrantLock;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 上午6:59
 */
/**
 * 一个基于链节点的、可选是否有界的 {@linkplain BlockingQueue 阻塞队列}。
 * 该队列按照 FIFO（先进先出）顺序排列元素。
 * 队列的<em>头部</em>是队列中存在时间最长的元素。
 * 队列的<em>尾部</em>是队列中存在时间最短的元素。新元素被插入到队列的尾部，
 * 队列的获取操作则从队列的头部获取元素。
 * 链式队列通常比基于数组的队列有更高的吞吐量，但在大多数并发应用中性能不如数组队列那样可预测。
 *
 * <p>可选的容量限制构造参数提供了一种防止队列过度扩展的方法。
 * 如果没有指定容量，默认容量等于 {@link Integer#MAX_VALUE}。
 * 在每次插入时，都会动态创建链节点，除非这会导致队列超出容量限制。
 *
 * <p>此类及其迭代器实现了 {@link Collection} 和 {@link Iterator} 接口的所有
 * <em>可选</em>方法。
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a>的成员之一。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> 此集合中保存的元素类型
 */

public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -6903933977591709194L;

    /*
     * "双锁队列"算法的变种。putLock 控制 put 和 offer 方法的进入，
     * 并且有一个关联的条件用于等待 put 操作。同样，takeLock 控制取操作。
     * 这两个操作都依赖的“count”字段是以原子方式维护的，以避免在大多数情况下需要获取两个锁。
     * 此外，为了尽量减少 put 需要获取 takeLock 和反之的情况，使用了级联通知。
     * 当一个 put 操作发现它已经启用了至少一个 take 操作时，它会通知取操作。
     * 那个取操作随后如果在信号后有更多项目被加入，会通知其他取操作。take 操作通知 put 操作也是对称的。
     * 诸如 remove(Object) 和迭代器等操作会同时获取两个锁。
     *
     * 读写之间的可见性如下提供：
     *
     * 每当一个元素入队时，putLock 会被获取并更新 count。
     * 随后的读取器保证可以看到已入队的节点，
     * 通过获取 putLock（通过 fullyLock 方法）或获取 takeLock，然后读取 n = count.get()；
     * 这可以保证对前 n 个项目的可见性。
     *
     * 为了实现弱一致的迭代器，我们需要确保所有从前驱节点出队的节点在 GC 上仍然可到达。
     * 这会导致两个问题：
     * - 允许一个恶意的迭代器导致无限制的内存保留
     * - 如果一个节点在存活时已经晋升到更高代，则会导致旧节点与新节点跨代链接，
     *   这对分代 GC 来说处理困难，会导致重复的大规模垃圾回收。
     * 但是，只有非删除节点需要从已出队的节点中可到达，并且可达性不一定需要由 GC 理解。
     * 我们使用的技巧是将刚出队的节点链接到其自身。这样的自我链接隐含的意思是前进到 head.next。
     */

    /**
     * 链表节点类
     */
    static class Node<E> {
        E item;

        /**
         * 其中之一：
         * - 实际的后继节点
         * - 此节点，表示后继节点是 head.next
         * - null，表示没有后继节点（这是最后一个节点）
         */
        Node<E> next;

        Node(E x) { item = x; }
    }

    /** 容量限制，如果没有则为 Integer.MAX_VALUE */
    private final int capacity;

    /** 当前元素的数量 */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * 链表头节点。
     * 不变量：head.item == null
     */
    transient Node<E> head;

    /**
     * 链表尾节点。
     * 不变量：last.next == null
     */
    private transient Node<E> last;

    /** 由 take、poll 等操作持有的锁 */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** 等待取操作的等待队列 */
    private final Condition notEmpty = takeLock.newCondition();

    /** 由 put、offer 等操作持有的锁 */
    private final ReentrantLock putLock = new ReentrantLock();

    /** 等待 put 操作的等待队列 */
    private final Condition notFull = putLock.newCondition();

    /**
     * 通知等待取操作。仅在 put/offer 中调用（通常不需要锁住 takeLock）。
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 通知等待 put 操作。仅在 take/poll 中调用。
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 将节点链接到队列末尾。
     *
     * @param node 节点
     */
    private void enqueue(Node<E> node) {
        // assert putLock.isHeldByCurrentThread();
        // assert last.next == null;
        last = last.next = node;
    }

    /**
     * 从队列头部移除一个节点。
     *
     * @return 移除的节点
     */
    private E dequeue() {
        // assert takeLock.isHeldByCurrentThread();
        // assert head.item == null;
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // 帮助 GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    /**
     * 锁住 put 和 take 操作以阻止它们。
     */
    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * 解锁以允许 put 和 take 操作。
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    /**
     * 创建一个容量为 {@link Integer#MAX_VALUE} 的 {@code LinkedBlockingQueue}。
     */
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    /**
     * 创建一个具有给定（固定）容量的 {@code LinkedBlockingQueue}。
     *
     * @param capacity 队列的容量
     * @throws IllegalArgumentException 如果 {@code capacity} 小于或等于 0
     */
    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }

    /**
     * 创建一个容量为 {@link Integer#MAX_VALUE} 的 {@code LinkedBlockingQueue}，
     * 并初始包含给定集合中的元素，按照集合的迭代器顺序添加。
     *
     * @param c 要初始包含的元素集合
     * @throws NullPointerException 如果指定的集合或集合中的任何元素为 null
     */
    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // 从未争用，但为了可见性必要
        try {
            int n = 0;
            for (E e : c) {
                if (e == null)
                    throw new NullPointerException();
                if (n == capacity)
                    throw new IllegalStateException("队列已满");
                enqueue(new Node<E>(e));
                ++n;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }

    // 重写此文档注释，以去除对大小大于 Integer.MAX_VALUE 的集合的引用
    /**
     * 返回此队列中的元素数量。
     *
     * @return 队列中的元素数量
     */
    public int size() {
        return count.get();
    }

    // 此文档注释是继承的文档注释的修改版，去除了对无限队列的引用。
    /**
     * 返回此队列能够（在没有内存或资源限制的情况下）在不阻塞的情况下理想地接受的额外元素的数量。
     * 这始终等于此队列的初始容量减去当前 {@code size}。
     *
     * <p>请注意，您<em>不能</em>通过检查 {@code remainingCapacity} 来判断插入一个元素的尝试是否会成功，
     * 因为可能有其他线程即将插入或移除元素。
     */
    public int remainingCapacity() {
        return capacity - count.get();
    }

    /**
     * 将指定的元素插入此队列的尾部，必要时等待空间可用。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        // 注意：在所有的 put/take 等操作中，惯例是预先将保存 count 的局部变量设置为负数，
        // 以表示失败，除非成功时将其设置为其他值。
        int c = -1;
        Node<E> node = new Node<>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            /*
             * 注意，虽然 count 没有被锁保护，但它仍然被用于等待保护。这是可行的，
             * 因为在这个时候 count 只会减少（所有其他的插入操作都被锁阻止了），
             * 并且如果 count 从容量发生变化，我们（或其他正在等待插入的线程）将会被唤醒。
             * 类似的情况也适用于 count 在其他等待保护中的使用。
             */
            while (count.get() == capacity) {
                notFull.await();
            }
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
    }

    /**
     * 将指定的元素插入此队列的尾部，必要时等待指定的等待时间让空间可用。
     *
     * @return 如果成功，则返回 {@code true}；如果在指定的等待时间内空间不可用，则返回 {@code false}
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return true;
    }

    /**
     * 如果可以在不超过队列容量的情况下立即插入指定的元素，则将其插入此队列的尾部，成功时返回 {@code true}，
     * 如果此队列已满，则返回 {@code false}。
     * 当使用容量受限的队列时，通常此方法优于 {@link BlockingQueue#add add} 方法，后者只能通过抛出异常来失败。
     *
     * @throws NullPointerException 如果指定的元素为 null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return false;
        int c = -1;
        Node<E> node = new Node<>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return c >= 0;
    }

    /**
     * 检索并移除此队列的头部，必要时等待直到元素可用。
     *
     * @return 队列头部的元素
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    /**
     * 检索并移除此队列的头部，必要时等待指定的等待时间，直到元素可用。
     *
     * @param timeout 等待时间
     * @param unit 时间单位
     * @return 队列头部的元素，或在超时时返回 {@code null}
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x = null;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    /**
     * 检索并移除此队列的头部。如果此队列为空，则返回 {@code null}。
     *
     * @return 队列头部的元素，或 {@code null} 如果队列为空
     */
    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1)
                    notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    /**
     * 检索但不移除此队列的头部；如果此队列为空，则返回 {@code null}。
     *
     * @return 队列头部的元素，或 {@code null} 如果队列为空
     */
    public E peek() {
        if (count.get() == 0)
            return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.item;
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 解锁内部节点 p 并连接到 trail 前驱节点。
     */
    void unlink(Node<E> p, Node<E> trail) {
        // assert isFullyLocked();
        // p.next 未被修改，以允许遍历到 p 的迭代器保持其弱一致性保证。
        p.item = null;
        trail.next = p.next;
        if (last == p)
            last = trail;
        if (count.getAndDecrement() == capacity)
            notFull.signal();
    }

    /**
     * 从此队列中移除指定元素的单个实例（如果存在）。
     * 更确切地说，移除等于 {@code o.equals(e)} 的元素 {@code e}，如果队列包含一个或多个此类元素。
     * 如果此队列包含指定的元素，则返回 {@code true}（换句话说，如果此调用改变了队列的状态，则返回 {@code true}）。
     *
     * @param o 要从队列中移除的元素（如果存在）
     * @return 如果此队列因为调用而发生变化，则返回 {@code true}
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> trail = head, p = trail.next;
                 p != null;
                 trail = p, p = p.next) {
                if (o.equals(p.item)) {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 如果此队列包含指定的元素，则返回 {@code true}。
     * 更确切地说，如果此队列至少包含一个等于 {@code o.equals(e)} 的元素，则返回 {@code true}。
     *
     * @param o 要检查是否包含在此队列中的对象
     * @return 如果此队列包含指定的元素，则返回 {@code true}
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        fullyLock();
        try {
            for (Node<E> p = head.next; p != null; p = p.next)
                if (o.equals(p.item))
                    return true;
            return false;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 返回包含此队列中所有元素的数组，按正确的顺序排列。
     *
     * <p>返回的数组是“安全的”，因为此队列不会保留对它的引用。（换句话说，此方法必须分配一个新的数组）。
     * 因此，调用者可以自由修改返回的数组。
     *
     * <p>此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
     *
     * @return 包含此队列中所有元素的数组
     */
    public Object[] toArray() {
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next)
                a[k++] = p.item;
            return a;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 返回包含此队列中所有元素的数组，按正确的顺序排列；返回数组的运行时类型与指定数组相同。
     * 如果队列的大小比指定数组小，则该数组将被返回。
     * 否则，将分配一个新的数组，其运行时类型与指定数组相同，且大小等于队列的大小。
     *
     * <p>如果此队列正好适合指定的数组，并且数组中有空余空间，则紧接队列末尾的元素被设置为 {@code null}。
     *
     * <p>与 {@link #toArray()} 方法一样，此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
     * 此外，此方法允许对输出数组的运行时类型进行精确控制，并在某些情况下用于节省分配成本。
     *
     * <p>假设 {@code x} 是一个已知仅包含字符串的队列。以下代码可以用于将队列中的元素放入一个新分配的字符串数组中：
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * 请注意，{@code toArray(new Object[0])} 在功能上与 {@code toArray()} 相同。
     *
     * @param a 用于存储队列中元素的数组，如果数组足够大；否则，将分配一个与指定数组相同类型的新数组
     * @return 包含此队列中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是队列中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果指定的数组为 null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            if (a.length < size)
                a = (T[])java.lang.reflect.Array.newInstance
                        (a.getClass().getComponentType(), size);

            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next)
                a[k++] = (T)p.item;
            if (a.length > k)
                a[k] = null;
            return a;
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 返回此队列的字符串表示形式。
     *
     * @return 此队列的字符串表示形式
     */
    public String toString() {
        fullyLock();
        try {
            Node<E> p = head.next;
            if (p == null)
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (;;) {
                E e = p.item;
                sb.append(e == this ? "(this Collection)" : e);
                p = p.next;
                if (p == null)
                    return sb.append(']').toString();
                sb.append(',').append(' ');
            }
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 原子地移除此队列中的所有元素。调用此方法后，队列将为空。
     */
    public void clear() {
        fullyLock();
        try {
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            // assert head.item == null && head.next == null;
            if (count.getAndSet(0) == capacity)
                notFull.signal();
        } finally {
            fullyUnlock();
        }
    }
    /**
     * @throws UnsupportedOperationException {@inheritDoc} // 继承文档中的说明
     * @throws ClassCastException            {@inheritDoc} // 继承文档中的说明
     * @throws NullPointerException          {@inheritDoc} // 继承文档中的说明
     * @throws IllegalArgumentException      {@inheritDoc} // 继承文档中的说明
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * 将此队列中的元素转移到指定的集合中，直到队列为空或已传输的元素数达到指定的最大元素数。
     * 返回已传输的元素数。
     *
     * @param c 要将元素传输到的集合
     * @param maxElements 要传输的最大元素数
     * @return 实际传输的元素数
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          如果指定的集合为 null
     * @throws IllegalArgumentException      如果指定的集合是此队列，或 {@code maxElements} 小于等于 0
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            // count.get 提供前 n 个节点的可见性
            Node<E> h = head;
            int i = 0;
            try {
                while (i < n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    ++i;
                }
                return n;
            } finally {
                // 即使 c.add() 抛出异常，也要恢复不变量
                if (i > 0) {
                    // assert h.item == null;
                    head = h;
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull)
                signalNotFull();
        }
    }

    /**
     * 返回此队列中元素的迭代器，按正确的顺序返回。
     * 元素将按从第一个（队列头部）到最后一个（队列尾部）的顺序返回。
     *
     * <p>返回的迭代器是 <a href="package-summary.html#Weakly"><i>弱一致性</i></a> 的。
     *
     * @return 按正确顺序遍历此队列的迭代器
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        /*
         * 基本的弱一致性迭代器。在任何时候都持有下一个要返回的元素，
         * 这样即使 hasNext() 报告 true，我们也可以在与 take 等操作的竞争中返回它。
         */
        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null)
                    currentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        /**
         * 返回 p 的下一个活跃的后继节点，或如果没有此类节点，则返回 null。
         *
         * 与其他遍历方法不同，迭代器需要处理以下两种情况：
         * - 已出队的节点（p.next == p）
         * - （可能有多个）内部移除的节点（p.item == null）
         */
        private Node<E> nextNode(Node<E> p) {
            for (;;) {
                Node<E> s = p.next;
                if (s == p)
                    return head.next;
                if (s == null || s.item != null)
                    return s;
                p = s;
            }
        }

        public E next() {
            fullyLock();
            try {
                if (current == null)
                    throw new NoSuchElementException();
                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next;
                     p != null;
                     trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }

    /** Spliterators.IteratorSpliterator 的自定义变体 */
    static final class LBQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // 最大批次数组大小
        final LinkedBlockingQueue<E> queue;
        Node<E> current;    // 当前节点；在初始化之前为 null
        int batch;          // 分割批次大小
        boolean exhausted;  // 没有更多节点时为 true
        long est;           // 大小估计值
        LBQSpliterator(LinkedBlockingQueue<E> queue) {
            this.queue = queue;
            this.est = queue.size();
        }

        public long estimateSize() { return est; }

        public Spliterator<E> trySplit() {
            Node<E> h;
            final LinkedBlockingQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                    ((h = current) != null || (h = q.head.next) != null) &&
                    h.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                Node<E> p = current;
                q.fullyLock();
                try {
                    if (p != null || (p = q.head.next) != null) {
                        do {
                            if ((a[i] = p.item) != null)
                                ++i;
                        } while ((p = p.next) != null && i < n);
                    }
                } finally {
                    q.fullyUnlock();
                }
                if ((current = p) == null) {
                    est = 0L;
                    exhausted = true;
                }
                else if ((est -= i) < 0L)
                    est = 0L;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator
                            (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                                    Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                exhausted = true;
                Node<E> p = current;
                do {
                    E e = null;
                    q.fullyLock();
                    try {
                        if (p == null)
                            p = q.head.next;
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null)
                                break;
                        }
                    } finally {
                        q.fullyUnlock();
                    }
                    if (e != null)
                        action.accept(e);
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final LinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                E e = null;
                q.fullyLock();
                try {
                    if (current == null)
                        current = q.head.next;
                    while (current != null) {
                        e = current.item;
                        current = current.next;
                        if (e != null)
                            break;
                    }
                } finally {
                    q.fullyUnlock();
                }
                if (current == null)
                    exhausted = true;
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                    Spliterator.CONCURRENT;
        }
    }

    /**
     * 返回此队列中元素的 {@link Spliterator}。
     *
     * <p>返回的 Spliterator 是
     * <a href="package-summary.html#Weakly"><i>弱一致性</i></a> 的。
     *
     * <p>{@code Spliterator} 报告 {@link Spliterator#CONCURRENT}，
     * {@link Spliterator#ORDERED}，以及 {@link Spliterator#NONNULL}。
     *
     * @implNote
     * {@code Spliterator} 实现了 {@code trySplit} 以允许有限的并行性。
     *
     * @return 此队列中元素的 {@code Spliterator}
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new LBQSpliterator<E>(this);
    }

    /**
     * 将此队列保存到流（即，序列化它）。
     *
     * @param s 流
     * @throws java.io.IOException 如果发生 I/O 错误
     * @serialData 容量以整数形式发出，后跟按适当顺序排列的所有元素（每个都是 {@code Object}），最后以 null 结束。
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        fullyLock();
        try {
            // 写出所有隐藏数据和容量
            s.defaultWriteObject();

            // 按适当的顺序写出所有元素
            for (Node<E> p = head.next; p != null; p = p.next)
                s.writeObject(p.item);

            // 使用结尾的 null 作为哨兵
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
    }

    /**
     * 从流中重构此队列（即，反序列化它）。
     *
     * @param s 流
     * @throws ClassNotFoundException 如果无法找到序列化对象的类
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // 读取容量以及所有隐藏数据
        s.defaultReadObject();

        count.set(0);
        last = head = new Node<E>(null);

        // 读取所有元素并放入队列
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E)s.readObject();
            if (item == null)
                break;
            add(item);
        }
    }


}



