package org.top.java.source.concurrent;

import org.top.java.source.concurrent.locks.ReentrantLock;

import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午4:51
 */
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {

    /**
     * 序列化 ID。此类依赖于默认的序列化方式，即使对于空的 items 数组也是默认序列化的。
     * 否则它不能被声明为 final，而这里是需要这样的。
     */
    private static final long serialVersionUID = -817911632652898426L;

    /** 已排队的元素 */
    final Object[] items;

    /** 下次 take、poll、peek 或 remove 操作的索引 */
    int takeIndex;

    /** 下次 put、offer 或 add 操作的索引 */
    int putIndex;

    /** 队列中的元素数量 */
    int count;

    /*
     * 并发控制使用经典的双条件算法，见于任何教科书中。
     */

    /** 主锁，控制所有访问 */
    final ReentrantLock lock;

    /** 等待 take 的条件 */
    private final Condition notEmpty;

    /** 等待 put 的条件 */
    private final Condition notFull;

    /**
     * 共享状态，用于当前活跃的迭代器，如果已知没有迭代器则为 null。
     * 允许队列操作更新迭代器状态。
     */
    transient Itrs itrs = null;

    // 内部辅助方法

    /**
     * 环形递减 i。
     */
    final int dec(int i) {
        return ((i == 0) ? items.length : i) - 1;
    }

    /**
     * 返回索引 i 处的元素。
     */
    @SuppressWarnings("unchecked")
    final E itemAt(int i) {
        return (E) items[i];
    }

    /**
     * 如果参数为 null 则抛出 NullPointerException。
     *
     * @param v 元素
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    /**
     * 在当前的 put 位置插入元素，前进索引并发出信号。仅在持有锁时调用。
     */
    private void enqueue(E x) {
        // assert lock.getHoldCount() == 1;
        // assert items[putIndex] == null;
        final Object[] items = this.items;
        items[putIndex] = x;
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        notEmpty.signal();
    }

    /**
     * 在当前的 take 位置提取元素，前进索引并发出信号。仅在持有锁时调用。
     */
    private E dequeue() {
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
        notFull.signal();
        return x;
    }

    /**
     * 删除数组索引 removeIndex 处的元素。
     * 是 remove(Object) 和 iterator.remove 的辅助方法。
     * 仅在持有锁时调用。
     */
    void removeAt(final int removeIndex) {
        // assert lock.getHoldCount() == 1;
        // assert items[removeIndex] != null;
        // assert removeIndex >= 0 && removeIndex < items.length;
        final Object[] items = this.items;
        if (removeIndex == takeIndex) {
            // 删除队首元素；只需前进
            items[takeIndex] = null;
            if (++takeIndex == items.length)
                takeIndex = 0;
            count--;
            if (itrs != null)
                itrs.elementDequeued();
        } else {
            // "内部"删除

            // 滑动所有其余元素直到 putIndex。
            final int putIndex = this.putIndex;
            for (int i = removeIndex;;) {
                int next = i + 1;
                if (next == items.length)
                    next = 0;
                if (next != putIndex) {
                    items[i] = items[next];
                    i = next;
                } else {
                    items[i] = null;
                    this.putIndex = i;
                    break;
                }
            }
            count--;
            if (itrs != null)
                itrs.removedAt(removeIndex);
        }
        notFull.signal();
    }

    /**
     * 使用给定的（固定）容量和默认访问策略创建一个 {@code ArrayBlockingQueue}。
     *
     * @param capacity 该队列的容量
     * @throws IllegalArgumentException 如果 {@code capacity < 1}
     */
    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    /**
     * 使用给定的（固定）容量和指定的访问策略创建一个 {@code ArrayBlockingQueue}。
     *
     * @param capacity 该队列的容量
     * @param fair 如果为 {@code true}，则插入或移除被阻塞的线程按 FIFO 顺序处理；
     *             如果为 {@code false}，访问顺序未指定。
     * @throws IllegalArgumentException 如果 {@code capacity < 1}
     */
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }

    /**
     * 使用给定的（固定）容量、指定的访问策略，并初始化为给定集合中元素的
     * 顺序，创建一个 {@code ArrayBlockingQueue}。
     *
     * @param capacity 该队列的容量
     * @param fair 如果为 {@code true}，则插入或移除被阻塞的线程按 FIFO 顺序处理；
     *             如果为 {@code false}，访问顺序未指定。
     * @param c 初始包含的元素集合，按集合的迭代器遍历顺序添加
     * @throws IllegalArgumentException 如果 {@code capacity} 小于
     *         {@code c.size()}，或小于 1。
     * @throws NullPointerException 如果指定的集合或其中的任何元素为 null
     */
    public ArrayBlockingQueue(int capacity, boolean fair,
                              Collection<? extends E> c) {
        this(capacity, fair);

        final ReentrantLock lock = this.lock;
        lock.lock(); // 仅锁定以确保可见性，而非互斥
        try {
            int i = 0;
            try {
                for (E e : c) {
                    checkNotNull(e);
                    items[i++] = e;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException();
            }
            count = i;
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果队列没有超出容量，立即将指定的元素插入到队列的尾部，
     * 成功时返回 {@code true}，如果队列已满则抛出 {@code IllegalStateException}。
     *
     * @param e 要添加的元素
     * @return {@code true}（按照 {@link Collection#add} 的规定）
     * @throws IllegalStateException 如果队列已满
     * @throws NullPointerException 如果指定的元素为 null
     */
    public boolean add(E e) {
        return super.add(e);
    }

    /**
     * 如果队列没有超出容量，立即将指定的元素插入到队列的尾部，
     * 成功时返回 {@code true}，如果队列已满则返回 {@code false}。
     * 此方法通常优于 {@link #add}，因为后者只能通过抛出异常来插入失败。
     *
     * @throws NullPointerException 如果指定的元素为 null
     */
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length)
                return false;
            else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定的元素插入到队列的尾部，如果队列已满，则等待空间可用。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定的元素插入到队列的尾部，如果队列已满，等待指定的时间以等待空间可用。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {

        checkNotNull(e);
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex); // 当队列为空时返回 null
        } finally {
            lock.unlock();
        }
    }

    // 该注释文档被重写，以删除对大小超过 Integer.MAX_VALUE 的集合的引用。
    /**
     * 返回此队列中的元素数量。
     *
     * @return 此队列中的元素数量
     */
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    // 这个注释文档是继承的注释文档的修改版，删除了对无限队列的引用。
    /**
     * 返回此队列在没有内存或资源限制的理想情况下可以接受的额外元素数量，而不会阻塞。
     * 这总是等于该队列的初始容量减去当前队列的 {@code size}。
     *
     * <p>请注意，通过检查 {@code remainingCapacity}，您<em>不能</em>总是
     * 判断尝试插入元素是否会成功，因为可能在另一个线程即将插入或移除元素的情况下发生变化。
     */
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从此队列中移除指定元素的单个实例（如果存在）。
     * 更正式地说，移除满足 {@code o.equals(e)} 的元素 {@code e}，如果此队列包含一个或多个此类元素。
     * 如果此队列包含指定的元素，则返回 {@code true}（或者等价地，如果此队列因调用而发生更改，则返回 {@code true}）。
     *
     * <p>在基于循环数组的队列中移除内部元素是一个本质上较慢且破坏性的操作，
     * 因此应仅在特殊情况下进行，理想情况下仅在队列已知不被其他线程访问时进行。
     *
     * @param o 要从此队列中移除的元素（如果存在）
     * @return 如果此队列因调用而发生更改，则返回 {@code true}
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i])) {
                        removeAt(i);
                        return true;
                    }
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果此队列包含指定的元素，则返回 {@code true}。
     * 更正式地说，如果且仅当此队列包含至少一个元素 {@code e} 满足 {@code o.equals(e)}，则返回 {@code true}。
     *
     * @param o 要检查是否包含于此队列中的对象
     * @return 如果此队列包含指定的元素，则返回 {@code true}
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    if (o.equals(items[i]))
                        return true;
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回包含此队列中所有元素的数组，按正确顺序排列。
     *
     * <p>返回的数组将是“安全的”，因为此队列不维护对它的引用。（换句话说，此方法必须分配一个新数组）。
     * 调用者可以自由修改返回的数组。
     *
     * <p>此方法充当数组 API 和集合 API 之间的桥梁。
     *
     * @return 包含此队列中所有元素的数组
     */
    public Object[] toArray() {
        Object[] a;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            a = new Object[count];
            int n = items.length - takeIndex;
            if (count <= n)
                System.arraycopy(items, takeIndex, a, 0, count);
            else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
        } finally {
            lock.unlock();
        }
        return a;
    }

    /**
     * 返回包含此队列中所有元素的数组，按正确顺序排列；返回数组的运行时类型与指定数组相同。
     * 如果队列适合指定的数组，则在其中返回。如果数组空间不足，则分配一个具有指定数组运行时类型的新数组并返回。
     *
     * <p>如果此队列适合指定的数组，并且有剩余空间（即，数组中的元素多于队列中的元素），
     * 则紧随队列末尾的数组元素将被设置为 {@code null}。
     *
     * <p>与 {@link #toArray()} 方法类似，此方法充当数组 API 和集合 API 之间的桥梁。
     * 此外，此方法允许对输出数组的运行时类型进行精确控制，并在某些情况下可用于节省分配成本。
     *
     * <p>假设 {@code x} 是一个已知只包含字符串的队列。
     * 以下代码可以将队列内容转储到新分配的 {@code String} 数组中：
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * 注意，{@code toArray(new Object[0])} 在功能上与 {@code toArray()} 相同。
     *
     * @param a 要存储队列元素的数组（如果它足够大）；否则，将为此目的分配一个具有相同运行时类型的新数组
     * @return 包含此队列中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此队列中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果指定的数组为 null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int count = this.count;
            final int len = a.length;
            if (len < count)
                a = (T[])java.lang.reflect.Array.newInstance(
                        a.getClass().getComponentType(), count);
            int n = items.length - takeIndex;
            if (count <= n)
                System.arraycopy(items, takeIndex, a, 0, count);
            else {
                System.arraycopy(items, takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
            if (len > count)
                a[count] = null;
        } finally {
            lock.unlock();
        }
        return a;
    }

    /**
     * 返回此队列的字符串表示形式。队列中的元素将按照从头到尾的顺序出现在字符串中，
     * 每个元素之间用逗号分隔。输出格式类似于 `[e1, e2, e3]`。
     */
    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = count;
            if (k == 0)
                return "[]";

            final Object[] items = this.items;
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = takeIndex; ; ) {
                Object e = items[i];
                sb.append(e == this ? "(this Collection)" : e);
                if (--k == 0)
                    return sb.append(']').toString();
                sb.append(',').append(' ');
                if (++i == items.length)
                    i = 0;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 原子地移除队列中的所有元素。此调用返回后队列将为空。
     */
    public void clear() {
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = count;
            if (k > 0) {
                final int putIndex = this.putIndex;
                int i = takeIndex;
                do {
                    items[i] = null;
                    if (++i == items.length)
                        i = 0;
                } while (i != putIndex);
                takeIndex = putIndex;
                count = 0;
                if (itrs != null)
                    itrs.queueIsEmpty();
                for (; k > 0 && lock.hasWaiters(notFull); k--)
                    notFull.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        checkNotNull(c);
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            int take = takeIndex;
            int i = 0;
            try {
                while (i < n) {
                    @SuppressWarnings("unchecked")
                    E x = (E) items[take];
                    c.add(x);
                    items[take] = null;
                    if (++take == items.length)
                        take = 0;
                    i++;
                }
                return n;
            } finally {
                // 即使 c.add() 抛出了异常，也要恢复不变量
                if (i > 0) {
                    count -= i;
                    takeIndex = take;
                    if (itrs != null) {
                        if (count == 0)
                            itrs.queueIsEmpty();
                        else if (i > take)
                            itrs.takeIndexWrapped();
                    }
                    for (; i > 0 && lock.hasWaiters(notFull); i--)
                        notFull.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回此队列中元素的迭代器，按正确顺序返回。
     * 元素将按照从队首（头）到队尾的顺序返回。
     *
     * <p>返回的迭代器是<a href="package-summary.html#Weakly"><i>弱一致性</i></a>的。
     *
     * @return 按正确顺序返回此队列中元素的迭代器
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 迭代器与其队列之间的共享数据，允许当元素被移除时，队列修改可以更新迭代器。
     *
     * 这为正确处理一些不常见的操作增加了很多复杂性，但循环数组和支持内部删除
     * （即那些不在队列头的删除操作）的组合可能会导致迭代器有时失去其位置和/或
     * 重新报告不应该的元素。为了避免这种情况，当队列有一个或多个迭代器时，它通过以下方式保持迭代器状态一致：
     *
     * (1) 记录“周期”的次数，也就是 takeIndex 已循环回到 0 的次数。
     * (2) 当内部元素被删除（因此其他元素可能会被移动时）通过回调 removedAt 通知所有迭代器。
     *
     * 这些足以消除迭代器的不一致性，但不幸的是，还增加了维护迭代器列表的附加责任。
     * 我们通过一个简单的链表来跟踪所有活动的迭代器（仅在队列的锁被持有时访问），该链表使用对 Itr 的弱引用。
     * 列表通过三种不同的机制进行清理：
     *
     * (1) 每当创建一个新的迭代器时，进行一些 O(1) 的检查来寻找列表中的过时元素。
     *
     * (2) 每当 takeIndex 回到 0 时，检查那些已经超过一个循环周期未使用的迭代器。
     *
     * (3) 每当队列变为空时，通知所有迭代器并丢弃整个数据结构。
     *
     * 因此，除了为保持正确性所必须的 removedAt 回调外，迭代器还有 shutdown 和 takeIndexWrapped 回调，
     * 用于帮助从列表中移除过时的迭代器。
     *
     * 每当检查列表元素时，如果 GC 已确定该迭代器已被丢弃，或者该迭代器报告它是“分离的”
     * （不再需要任何状态更新），则将其删除。当 takeIndex 从未前进，迭代器在耗尽之前就被丢弃，
     * 并且所有删除都是内部删除时，开销最大，在这种情况下，所有过时的迭代器都由 GC 发现。
     * 但即使在这种情况下，我们也不会增加摊销复杂度。
     *
     * 需要小心防止链表清理方法递归调用另一个清理方法，造成微妙的损坏错误。
     */
    class Itrs {

        /**
         * 弱迭代器引用链表中的节点。
         */
        private class Node extends WeakReference<Itr> {
            Node next;

            Node(Itr iterator, Node next) {
                super(iterator);
                this.next = next;
            }
        }

        /** 每当 takeIndex 回到 0 时递增 */
        int cycles = 0;

        /** 弱迭代器引用的链表 */
        private Node head;

        /** 用于清除过时的迭代器 */
        private Node sweeper = null;

        private static final int SHORT_SWEEP_PROBES = 4;
        private static final int LONG_SWEEP_PROBES = 16;

        Itrs(Itr initial) {
            register(initial);
        }

        /**
         * 清理 itrs，查找并删除过时的迭代器。如果至少找到一个，则更努力地寻找更多。
         * 仅从迭代线程调用。
         *
         * @param tryHarder 是否在更努力模式下开始，因为已知至少有一个迭代器需要回收
         */
        void doSomeSweeping(boolean tryHarder) {
            // assert lock.getHoldCount() == 1;
            // assert head != null;
            int probes = tryHarder ? LONG_SWEEP_PROBES : SHORT_SWEEP_PROBES;
            Node o, p;
            final Node sweeper = this.sweeper;
            boolean passedGo;   // 限制搜索至一次完整的清扫

            if (sweeper == null) {
                o = null;
                p = head;
                passedGo = true;
            } else {
                o = sweeper;
                p = o.next;
                passedGo = false;
            }

            for (; probes > 0; probes--) {
                if (p == null) {
                    if (passedGo)
                        break;
                    o = null;
                    p = head;
                    passedGo = true;
                }
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.isDetached()) {
                    // 找到了被丢弃/耗尽的迭代器
                    probes = LONG_SWEEP_PROBES; // “更努力”
                    // 解除 p 的链接
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        head = next;
                        if (next == null) {
                            // 我们已耗尽可跟踪的迭代器；退役
                            itrs = null;
                            return;
                        }
                    }
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }

            this.sweeper = (p == null) ? null : o;
        }

        /**
         * 将一个新迭代器添加到受跟踪的迭代器链表中。
         */
        void register(Itr itr) {
            // assert lock.getHoldCount() == 1;
            head = new Node(itr, head);
        }

        /**
         * 每当 takeIndex 回到 0 时调用。
         *
         * 通知所有迭代器，并清除任何已过时的迭代器。
         */
        void takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            cycles++;
            for (Node o = null, p = head; p != null;) {
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.takeIndexWrapped()) {
                    // 解除 p 的链接
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if (o == null)
                        head = next;
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if (head == null)   // 没有更多迭代器需要跟踪
                itrs = null;
        }

        /**
         * 每当发生内部删除（不在 takeIndex 处）时调用。
         *
         * 通知所有迭代器，并清除任何已过时的迭代器。
         */
        void removedAt(int removedIndex) {
            for (Node o = null, p = head; p != null;) {
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.removedAt(removedIndex)) {
                    // 解除 p 的链接
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if (o == null)
                        head = next;
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if (head == null)   // 没有更多迭代器需要跟踪
                itrs = null;
        }

        /**
         * 每当队列变为空时调用。
         *
         * 通知所有活动迭代器队列已空，清除所有弱引用，并解除 itrs 数据结构的链接。
         */
        void queueIsEmpty() {
            // assert lock.getHoldCount() == 1;
            for (Node p = head; p != null; p = p.next) {
                Itr it = p.get();
                if (it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            head = null;
            itrs = null;
        }

        /**
         * 每当某个元素被出队（在 takeIndex 处）时调用。
         */
        void elementDequeued() {
            // assert lock.getHoldCount() == 1;
            if (count == 0)
                queueIsEmpty();
            else if (takeIndex == 0)
                takeIndexWrapped();
        }
    }

    /**
     * ArrayBlockingQueue 的迭代器。
     *
     * 为了保持与 put 和 take 操作的弱一致性，我们提前读取一个槽，以避免 hasNext 返回 true，
     * 但之后没有元素可返回的情况。
     *
     * 当所有索引都为负值时，或者当 hasNext 第一次返回 false 时，我们会切换到“分离”模式
     * （允许在不依赖 GC 的情况下快速解除与 itrs 的链接）。这使得迭代器可以完全准确地跟踪并发更新，
     * 除了在 hasNext() 返回 false 后用户调用 Iterator.remove() 这种特殊情况。即使在这种情况下，
     * 我们也能通过跟踪要删除的预期元素 lastItem 来确保不会删除错误的元素。
     * 是的，如果 lastItem 因为在分离模式下的交错内部删除操作而被移动，我们可能无法将其从队列中移除。
     */
    private class Itr implements Iterator<E> {
        /** 查找新 nextItem 的索引；在结束时为 NONE */
        private int cursor;

        /** 下次调用 next() 时返回的元素；如果没有则为 null */
        private E nextItem;

        /** nextItem 的索引；如果没有则为 NONE，若被其他地方删除则为 REMOVED */
        private int nextIndex;

        /** 上次返回的元素；如果没有或未分离则为 null */
        private E lastItem;

        /** lastItem 的索引；如果没有则为 NONE，若被其他地方删除则为 REMOVED */
        private int lastRet;

        /** 上一次的 takeIndex 值，若已分离则为 DETACHED */
        private int prevTakeIndex;

        /** 上一次 itrs.cycles 的值 */
        private int prevCycles;

        /** 表示“不可用”或“未定义”的特殊索引值 */
        private static final int NONE = -1;

        /**
         * 表示“被其他地方删除”的特殊索引值，即通过调用此 remove() 以外的其他操作删除。
         */
        private static final int REMOVED = -2;

        /** 表示“分离模式”的 prevTakeIndex 的特殊值 */
        private static final int DETACHED = -3;

        Itr() {
            // assert lock.getHoldCount() == 0;
            lastRet = NONE;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (count == 0) {
                    // assert itrs == null;
                    cursor = NONE;
                    nextIndex = NONE;
                    prevTakeIndex = DETACHED;
                } else {
                    final int takeIndex = ArrayBlockingQueue.this.takeIndex;
                    prevTakeIndex = takeIndex;
                    nextItem = itemAt(nextIndex = takeIndex);
                    cursor = incCursor(takeIndex);
                    if (itrs == null) {
                        itrs = new Itrs(this);
                    } else {
                        itrs.register(this); // 按此顺序
                        itrs.doSomeSweeping(false);
                    }
                    prevCycles = itrs.cycles;
                    // assert takeIndex >= 0;
                    // assert prevTakeIndex == takeIndex;
                    // assert nextIndex >= 0;
                    // assert nextItem != null;
                }
            } finally {
                lock.unlock();
            }
        }

        boolean isDetached() {
            // assert lock.getHoldCount() == 1;
            return prevTakeIndex < 0;
        }

        private int incCursor(int index) {
            // assert lock.getHoldCount() == 1;
            if (++index == items.length)
                index = 0;
            if (index == putIndex)
                index = NONE;
            return index;
        }

        /**
         * 如果给定的出队数量使索引无效，则返回 true，从 prevTakeIndex 开始。
         */
        private boolean invalidated(int index, int prevTakeIndex,
                                    long dequeues, int length) {
            if (index < 0)
                return false;
            int distance = index - prevTakeIndex;
            if (distance < 0)
                distance += length;
            return dequeues > distance;
        }

        /**
         * 调整索引以包含自上次迭代器操作以来的所有出队操作。仅从迭代线程调用。
         */
        private void incorporateDequeues() {
            // assert lock.getHoldCount() == 1;
            // assert itrs != null;
            // assert !isDetached();
            // assert count > 0;

            final int cycles = itrs.cycles;
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;

            if (cycles != prevCycles || takeIndex != prevTakeIndex) {
                final int len = items.length;
                // 自迭代器上次操作以来，takeIndex 前进了多少
                long dequeues = (cycles - prevCycles) * len
                        + (takeIndex - prevTakeIndex);

                // 检查索引是否无效
                if (invalidated(lastRet, prevTakeIndex, dequeues, len))
                    lastRet = REMOVED;
                if (invalidated(nextIndex, prevTakeIndex, dequeues, len))
                    nextIndex = REMOVED;
                if (invalidated(cursor, prevTakeIndex, dequeues, len))
                    cursor = takeIndex;

                if (cursor < 0 && nextIndex < 0 && lastRet < 0)
                    detach();
                else {
                    this.prevCycles = cycles;
                    this.prevTakeIndex = takeIndex;
                }
            }
        }

        /**
         * 当 itrs 应停止跟踪此迭代器时调用，要么因为没有更多索引需要更新
         * （cursor < 0 && nextIndex < 0 && lastRet < 0），
         * 要么作为特殊例外，当 lastRet >= 0 时，因为 hasNext() 第一次返回 false。
         * 仅从迭代线程调用。
         */
        private void detach() {
            // 切换到分离模式
            // assert lock.getHoldCount() == 1;
            // assert cursor == NONE;
            // assert nextIndex < 0;
            // assert lastRet < 0 || nextItem == null;
            // assert lastRet < 0 ^ lastItem != null;
            if (prevTakeIndex >= 0) {
                // assert itrs != null;
                prevTakeIndex = DETACHED;
                // 尝试从 itrs 中解除链接（但不要过于努力）
                itrs.doSomeSweeping(true);
            }
        }

        /**
         * 出于性能考虑，我们希望在常见情况下不在 hasNext 中获取锁。为了实现这一点，
         * 我们只访问不受队列修改操作影响的字段（即 nextItem）。
         */
        public boolean hasNext() {
            // assert lock.getHoldCount() == 0;
            if (nextItem != null)
                return true;
            noNext();
            return false;
        }

        private void noNext() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                // assert cursor == NONE;
                // assert nextIndex == NONE;
                if (!isDetached()) {
                    // assert lastRet >= 0;
                    incorporateDequeues(); // 可能会更新 lastRet
                    if (lastRet >= 0) {
                        lastItem = itemAt(lastRet);
                        // assert lastItem != null;
                        detach();
                    }
                }
                // assert isDetached();
                // assert lastRet < 0 ^ lastItem != null;
            } finally {
                lock.unlock();
            }
        }

        public E next() {
            // assert lock.getHoldCount() == 0;
            final E x = nextItem;
            if (x == null)
                throw new NoSuchElementException();
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached())
                    incorporateDequeues();
                // assert nextIndex != NONE;
                // assert lastItem == null;
                lastRet = nextIndex;
                final int cursor = this.cursor;
                if (cursor >= 0) {
                    nextItem = itemAt(nextIndex = cursor);
                    // assert nextItem != null;
                    this.cursor = incCursor(cursor);
                } else {
                    nextIndex = NONE;
                    nextItem = null;
                }
            } finally {
                lock.unlock();
            }
            return x;
        }

        public void remove() {
            // assert lock.getHoldCount() == 0;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached())
                    incorporateDequeues(); // 可能会更新 lastRet 或分离
                final int lastRet = this.lastRet;
                this.lastRet = NONE;
                if (lastRet >= 0) {
                    if (!isDetached())
                        removeAt(lastRet);
                    else {
                        final E lastItem = this.lastItem;
                        // assert lastItem != null;
                        this.lastItem = null;
                        if (itemAt(lastRet) == lastItem)
                            removeAt(lastRet);
                    }
                } else if (lastRet == NONE)
                    throw new IllegalStateException();
                // 否则，lastRet == REMOVED，且上次返回的元素已通过此 remove() 以外的操作异步移除，因此无事可做。

                if (cursor < 0 && nextIndex < 0)
                    detach();
            } finally {
                lock.unlock();
                // assert lastRet == NONE;
                // assert lastItem == null;
            }
        }

        /**
         * 通知迭代器队列已空，或它已严重滞后，因此应放弃任何进一步的迭代，
         * 除了可能从 next() 返回一个元素，因为 hasNext() 承诺返回 true。
         */
        void shutdown() {
            // assert lock.getHoldCount() == 1;
            cursor = NONE;
            if (nextIndex >= 0)
                nextIndex = REMOVED;
            if (lastRet >= 0) {
                lastRet = REMOVED;
                lastItem = null;
            }
            prevTakeIndex = DETACHED;
            // 不要将 nextItem 设置为 null，因为我们必须继续能够在 next() 中返回它。
            // 调用方会在方便时从 itrs 中解除链接。
        }

        private int distance(int index, int prevTakeIndex, int length) {
            int distance = index - prevTakeIndex;
            if (distance < 0)
                distance += length;
            return distance;
        }

        /**
         * 每当发生内部删除（不在 takeIndex 处）时调用。
         *
         * @return 如果此迭代器应从 itrs 中解除链接，则返回 true
         */
        boolean removedAt(int removedIndex) {
            // assert lock.getHoldCount() == 1;
            if (isDetached())
                return true;

            final int cycles = itrs.cycles;
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;
            final int len = items.length;
            int cycleDiff = cycles - prevCycles;
            if (removedIndex < takeIndex)
                cycleDiff++;
            final int removedDistance =
                    (cycleDiff * len) + (removedIndex - prevTakeIndex);
            // assert removedDistance >= 0;
            int cursor = this.cursor;
            if (cursor >= 0) {
                int x = distance(cursor, prevTakeIndex, len);
                if (x == removedDistance) {
                    if (cursor == putIndex)
                        this.cursor = cursor = NONE;
                }
                else if (x > removedDistance) {
                    // assert cursor != prevTakeIndex;
                    this.cursor = cursor = dec(cursor);
                }
            }
            int lastRet = this.lastRet;
            if (lastRet >= 0) {
                int x = distance(lastRet, prevTakeIndex, len);
                if (x == removedDistance)
                    this.lastRet = lastRet = REMOVED;
                else if (x > removedDistance)
                    this.lastRet = lastRet = dec(lastRet);
            }
            int nextIndex = this.nextIndex;
            if (nextIndex >= 0) {
                int x = distance(nextIndex, prevTakeIndex, len);
                if (x == removedDistance)
                    this.nextIndex = nextIndex = REMOVED;
                else if (x > removedDistance)
                    this.nextIndex = nextIndex = dec(nextIndex);
            }
            else if (cursor < 0 && nextIndex < 0 && lastRet < 0) {
                this.prevTakeIndex = DETACHED;
                return true;
            }
            return false;
        }

        /**
         * 每当 takeIndex 回到零时调用。
         *
         * @return 如果此迭代器应从 itrs 中解除链接，则返回 true
         */
        boolean takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            if (isDetached())
                return true;
            if (itrs.cycles - prevCycles > 1) {
                // 在上次迭代器操作时存在的所有元素都已消失，因此放弃进一步迭代。
                shutdown();
                return true;
            }
            return false;
        }
    }
    /**
     * 返回此队列中元素的 {@link Spliterator}。
     *
     * <p>返回的 spliterator 是
     * <a href="package-summary.html#Weakly"><i>弱一致性</i></a>的。
     *
     * <p>{@code Spliterator} 报告 {@link Spliterator#CONCURRENT}、
     * {@link Spliterator#ORDERED} 和 {@link Spliterator#NONNULL}。
     *
     * @implNote
     * {@code Spliterator} 实现了 {@code trySplit} 以允许有限的并行处理。
     *
     * @return 此队列中元素的 {@code Spliterator}
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
                (this, Spliterator.ORDERED | Spliterator.NONNULL |
                        Spliterator.CONCURRENT);
    }

}


