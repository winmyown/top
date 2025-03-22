package org.top.java.source.lang;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.lang.Object;
/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 上午7:49
 */

/**
 * 该类提供线程局部变量。与普通变量不同，每个线程访问这些变量（通过其 {@code get} 或 {@code set} 方法）时，都会有自己独立初始化的变量副本。{@code ThreadLocal} 实例通常是类中的私有静态字段，这些类希望将状态与线程关联（例如用户ID或事务ID）。
 *
 * <p>例如，下面的类为每个线程生成唯一的局部标识符。
 * 线程的ID在它第一次调用 {@code ThreadId.get()} 时被分配，并且在后续调用中保持不变。
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // 原子整数，包含下一个要分配的线程ID
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // 线程局部变量，包含每个线程的ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // 返回当前线程的唯一ID，如有必要则分配
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>每个线程在其存活期间都隐式地持有对其线程局部变量副本的引用，只要 {@code ThreadLocal} 实例是可访问的；当线程消亡时，所有这些线程局部变量副本都将进行垃圾回收（除非还有其他引用指向这些副本）。
 *
 * @author  Josh Bloch 和 Doug Lea
 * @since   1.2
 */

public class ThreadLocal<T> {
    /**
     * ThreadLocal 依赖于附加到每个线程的每线程线性探测哈希映射 (Thread.threadLocals 和 inheritableThreadLocals)。
     * ThreadLocal 对象作为键，通过 threadLocalHashCode 搜索。
     * 这是一个自定义哈希码（仅在 ThreadLocalMaps 中有用），在常见的情况下，连续构造的 ThreadLocal 被相同的线程使用时，消除了冲突，同时在不太常见的情况下保持良好表现。
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * 下一个要分配的哈希码。以原子方式更新。从零开始。
     */
    private static AtomicInteger nextHashCode = new AtomicInteger();

    /**
     * 连续生成的哈希码之间的差值 - 将隐式顺序线程本地 ID 转换为功率为二大小表的接近最优分布的乘法哈希值。
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * 返回下一个哈希码。
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 返回当前线程的“初始值”对于此 ThreadLocal 变量。
     * 该方法将在第一次通过 {@link #get} 方法访问变量时被调用，除非线程先前调用了 {@link #set} 方法，在这种情况下不会为该线程调用 {@code initialValue} 方法。
     * 通常，此方法每个线程最多调用一次，但在调用 {@link #remove} 后紧跟 {@link #get} 时可能会再次调用。
     *
     * <p>此实现仅返回 {@code null}；如果程序员希望线程本地变量具有除 {@code null} 之外的初始值，则必须子类化 {@code ThreadLocal} 并重写此方法。
     * 通常会使用匿名内部类。</p>
     *
     * @return 该线程本地的初始值
     */
    protected T initialValue() {
        return null;
    }

    /**
     * 创建一个线程本地变量。变量的初始值通过调用 {@code Supplier} 的 {@code get} 方法确定。
     *
     * @param <S> 线程本地值的类型
     * @param supplier 用于确定初始值的供应商
     * @return 一个新的线程本地变量
     * @throws NullPointerException 如果指定的供应商为 null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * 创建一个线程本地变量。
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * 返回当前线程的此线程本地变量的值。如果当前线程没有该变量的值，则首先初始化为通过调用 {@link #initialValue} 方法返回的值。
     *
     * @return 当前线程的线程本地变量的值
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }

    /**
     * 设置初始值的变体，用于代替 set() 的情况，以防用户重写了 set() 方法。
     *
     * @return 初始值
     */
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }

    /**
     * 将当前线程的此线程本地变量的副本设置为指定值。大多数子类不需要重写此方法，而只依赖 {@link #initialValue} 方法来设置线程本地的值。
     *
     * @param value 要存储在当前线程的线程本地副本中的值。
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * 删除当前线程的此线程本地变量的值。如果当前线程随后 {@linkplain #get 读取} 该线程本地变量，将通过调用 {@link #initialValue} 方法重新初始化其值，除非在此期间该线程设置了其值。
     * 这可能会导致在当前线程中多次调用 {@code initialValue} 方法。
     *
     * @since 1.5
     */
    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            m.remove(this);
    }

    /**
     * 获取与 ThreadLocal 关联的映射。在 InheritableThreadLocal 中重写。
     *
     * @param  t 当前线程
     * @return 与 ThreadLocal 关联的映射
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * 创建与 ThreadLocal 关联的映射。在 InheritableThreadLocal 中重写。
     *
     * @param t 当前线程
     * @param firstValue 映射的初始值
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * 工厂方法，用于创建继承的线程本地变量的映射。只在 Thread 构造函数中调用。
     *
     * @param  parentMap 父线程关联的映射
     * @return 包含父线程继承绑定的映射
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * 子类 InheritableThreadLocal 中定义了 childValue 方法，但在此内部定义是为了提供 createInheritedMap 工厂方法，而不需要子类化映射类。
     * 这种技术优于在方法中嵌入 instanceof 测试的替代方案。
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * ThreadLocal 的扩展类，通过指定的 {@code Supplier} 获取其初始值。
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap 是一种定制的哈希映射，仅适合维护线程本地值。没有对外部 ThreadLocal 类导出的操作。
     * 该类是包私有的，以允许在 Thread 类中声明字段。为处理非常大的长期使用，哈希表条目使用 WeakReferences 作为键。
     * 然而，由于没有使用引用队列，只有当表开始空间不足时，才保证删除过时的条目。
     */
    static class ThreadLocalMap {

        /**
         * 此哈希映射中的条目扩展了 WeakReference，使用其主引用字段作为键（始终是 ThreadLocal 对象）。
         * 注意，空键（即 entry.get() == null）意味着该键不再被引用，因此可以从表中删除该条目。此类条目在后续代码中称为“过时条目”。
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** 与此 ThreadLocal 关联的值。 */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * 初始容量——必须是二的幂。
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 表，根据需要调整大小。table.length 必须始终是二的幂。
         */
        private Entry[] table;

        /**
         * 表中的条目数。
         */
        private int size = 0;

        /**
         * 触发调整大小的下一个大小值。
         */
        private int threshold; // 默认值为 0

        /**
         * 设置调整大小的阈值，以保持最多 2/3 的负载系数。
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * 对 i 取模 len 递增。
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 对 i 取模 len 递减。
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 构造一个最初包含 (firstKey, firstValue) 的新映射。ThreadLocalMaps 是懒加载构建的，因此只有在有至少一个条目时才创建。
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * 构造一个包含给定父映射中所有可继承的线程本地变量的新映射。仅由 createInheritedMap 调用。
         *
         * @param parentMap 与父线程关联的映射。
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 获取与 key 相关的条目。此方法仅处理快速路径：现有键的直接命中。
         * 其余情况转发到 getEntryAfterMiss 方法。设计此方法是为了在直接命中时最大化性能，部分通过使此方法易于内联。
         *
         * @param key 线程本地对象
         * @return 与键相关的条目，或 null（如果没有该条目）
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 当 key 未在其直接哈希插槽中找到时使用的 getEntry 方法版本。
         *
         * @param key 线程本地对象
         * @param i 键的哈希码在表中的索引
         * @param e 表[i] 的条目
         * @return 与键相关的条目，或 null（如果没有该条目）
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    expungeStaleEntry(i);
                else
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * 设置与键关联的值。
         *
         * @param key 线程本地对象
         * @param value 要设置的值
         */
        private void set(ThreadLocal<?> key, Object value) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len - 1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * 删除与键关联的条目。
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len - 1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * 替换 set 操作期间遇到的过时条目，并为指定键创建条目。无论是否已经存在键的条目，传入的 value 都存储在条目中。
         *
         * 该方法会删除运行过程中遇到的所有过时条目。（运行是指两个空插槽之间的一系列条目）。
         *
         * @param key 键
         * @param value 与键关联的值
         * @param staleSlot 搜索键时遇到的第一个过时条目的索引。
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value, int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // 备份以检查当前运行中的先前过时条目。
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // 查找运行中的键或尾随空插槽，以先到者为准
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // 如果找到键，则需要将其与过时条目交换，以维护哈希表顺序。
                // 新的过时插槽或上面遇到的任何其他过时插槽可以传递给 expungeStaleEntry，以删除或重新散列运行中的所有其他条目。
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // 如果存在，则从前一个过时条目开始删除
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // 如果在向后扫描中没有找到过时条目，则在扫描键时看到的第一个过时条目是运行中仍然存在的第一个条目。
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // 如果没有找到键，将新条目放入过时插槽
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // 如果运行中有任何其他过时条目，请删除它们
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * 通过重新散列位于 staleSlot 和下一个空插槽之间的任何可能冲突的条目来删除过时条目。
         * 这也会删除在尾随空插槽之前遇到的任何其他过时条目。参考 Knuth，第 6.4 节。
         *
         * @param staleSlot 已知具有空键的插槽的索引
         * @return staleSlot 后面的下一个空插槽的索引（staleSlot 和此插槽之间的所有条目都将被检查删除）。
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 删除 staleSlot 处的条目
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // 重新散列直到遇到 null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // 与 Knuth 6.4 算法 R 不同，我们必须扫描直到遇到 null，因为可能有多个条目已过时。
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * 启发式扫描一些单元格以查找过时的条目。
         * 这是在添加新元素或删除另一个过时条目时调用的。它执行对数数量的扫描，介于不扫描（快速但保留垃圾）和与元素数量成正比的扫描之间，后者会找到所有垃圾但会导致某些插入操作的时间复杂度为 O(n)。
         *
         * @param i 已知不包含过时条目的位置。扫描从 i 之后的元素开始。
         * @param n 扫描控制：{@code log2(n)} 个单元格将被扫描，除非找到过时条目，在这种情况下，将扫描 {@code log2(table.length)-1} 额外单元格。
         * 当从插入调用时，此参数为元素的数量，但当从 replaceStaleEntry 调用时，它是表的长度。
         *
         * @return 如果删除了任何过时条目，则返回 true。
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ((n >>>= 1) != 0);
            return removed;
        }

        /**
         * 重新打包和/或调整表的大小。首先扫描整个表以删除过时的条目。如果这不足以充分缩小表的大小，则将表大小加倍。
         */
        private void rehash() {
            expungeStaleEntries();

            // 使用较低的调整阈值以避免滞后
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * 将表的容量加倍。
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // 帮助垃圾回收
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * 删除表中所有的过时条目。
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}