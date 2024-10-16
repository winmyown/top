package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午5:12
 */

import org.top.java.source.concurrent.locks.ReentrantLock;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 一个线程安全的 {@link java.util.ArrayList} 变体，其中所有的变更操作（如 {@code add}、{@code set} 等）都通过创建底层数组的新副本来实现。
 *
 * <p>尽管通常这种操作代价较高，但在遍历操作远多于变更操作时，它可能比其他替代方案更高效。并且在你不希望或不能对遍历进行同步时，
 * 但又需要避免并发线程之间的干扰时，该类非常有用。"快照"风格的迭代器方法使用的是在迭代器创建时的数组状态的引用。
 * 这个数组在迭代器的生命周期内不会改变，因此不会发生干扰，且迭代器保证不会抛出 {@code ConcurrentModificationException}。
 * 迭代器不会反映自其创建以来对列表进行的新增、移除或修改操作。迭代器本身的修改操作（如 {@code remove}、{@code set} 和 {@code add}）
 * 不支持，这些方法会抛出 {@code UnsupportedOperationException}。
 *
 * <p>允许所有元素，包括 {@code null}。
 *
 * <p>内存一致性效应：与其他并发集合类似，某线程中的操作在将一个对象放入 {@code CopyOnWriteArrayList} 之前，
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * 另一线程从 {@code CopyOnWriteArrayList} 中访问或移除该元素之后的操作。
 *
 * <p>该类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> 集合中持有的元素类型
 */
public class CopyOnWriteArrayList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    /** 保护所有变更操作的锁 */
    final transient ReentrantLock lock = new ReentrantLock();

    /** 数组，仅通过 getArray/setArray 访问。 */
    private transient volatile Object[] array;

    /**
     * 获取数组。非私有，以便 CopyOnWriteArraySet 类也可以访问。
     */
    final Object[] getArray() {
        return array;
    }

    /**
     * 设置数组。
     */
    final void setArray(Object[] a) {
        array = a;
    }

    /**
     * 创建一个空列表。
     */
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    /**
     * 创建一个包含指定集合中元素的列表，顺序与集合迭代器返回的顺序相同。
     *
     * @param c 初始化元素的集合
     * @throws NullPointerException 如果指定的集合为 null
     */
    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class)
            elements = ((CopyOnWriteArrayList<?>)c).getArray();
        else {
            elements = c.toArray();
            if (elements.getClass() != Object[].class)
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
        }
        setArray(elements);
    }

    /**
     * 创建一个持有给定数组副本的列表。
     *
     * @param toCopyIn 要复制的数组（此数组的副本将用作内部数组）
     * @throws NullPointerException 如果指定的数组为 null
     */
    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    /**
     * 返回此列表中的元素数量。
     *
     * @return 此列表中的元素数量
     */
    public int size() {
        return getArray().length;
    }

    /**
     * 如果此列表不包含任何元素，则返回 {@code true}。
     *
     * @return 如果此列表不包含任何元素则返回 {@code true}
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 用于检查两个对象是否相等，处理 `null` 值。
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }

    /**
     * indexOf 的静态版本，允许在不需要每次重新获取数组的情况下多次调用。
     *
     * @param o 要搜索的元素
     * @param elements 数组
     * @param index 开始搜索的第一个索引
     * @param fence 结束搜索的最后一个索引
     * @return 元素的索引，如果不存在则返回 -1
     */
    private static int indexOf(Object o, Object[] elements,
                               int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i < fence; i++)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * lastIndexOf 的静态版本。
     * @param o 要搜索的元素
     * @param elements 数组
     * @param index 要搜索的第一个索引
     * @return 元素的索引，如果不存在则返回 -1
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回 {@code true} 如果此列表包含指定的元素。
     * 更正式地讲，当且仅当此列表包含至少一个元素 {@code e}，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时返回 {@code true}。
     *
     * @param o 要测试是否存在于此列表中的元素
     * @return 如果此列表包含指定的元素，则返回 {@code true}
     */
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    /**
     * 返回从 {@code index} 开始，第一次出现指定元素的索引，或者如果没有找到该元素则返回 -1。
     * 更正式地说，返回最小的索引 {@code i}，使得
     * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>，
     * 如果没有找到这样的索引，则返回 -1。
     *
     * @param e 要搜索的元素
     * @param index 开始搜索的索引
     * @return 从 {@code index} 开始，元素在此列表中第一次出现的位置的索引；如果未找到该元素则返回 {@code -1}。
     * @throws IndexOutOfBoundsException 如果指定的索引为负数
     */
    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    /**
     * {@inheritDoc}
     */
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    /**
     * 返回从 {@code index} 开始，最后一次出现指定元素的索引，或者如果没有找到该元素则返回 -1。
     * 更正式地讲，返回最大索引 {@code i}，使得
     * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>，
     * 如果没有找到这样的索引，则返回 -1。
     *
     * @param e 要搜索的元素
     * @param index 开始向后搜索的索引
     * @return 从 {@code index} 向后，元素在此列表中最后一次出现的位置的索引；如果未找到该元素则返回 {@code -1}。
     * @throws IndexOutOfBoundsException 如果指定的索引大于或等于此列表的当前大小
     */
    public int lastIndexOf(E e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }

    /**
     * 返回此列表的浅表副本。（元素本身不被复制。）
     *
     * @return 此列表的克隆
     */
    public Object clone() {
        try {
            @SuppressWarnings("unchecked")
            CopyOnWriteArrayList<E> clone =
                    (CopyOnWriteArrayList<E>) super.clone();
            clone.resetLock();
            return clone;
        } catch (CloneNotSupportedException e) {
            // 这不应该发生，因为我们是可克隆的
            throw new InternalError();
        }
    }

    /**
     * 返回一个包含此列表中所有元素的数组，按适当顺序（从第一个到最后一个元素）。
     *
     * <p>返回的数组将是“安全的”，因为没有对它的引用由此列表维护。（换句话说，此方法必须分配一个新数组。）
     * 调用者可以自由地修改返回的数组。
     *
     * <p>此方法作为基于数组和基于集合的 API 之间的桥梁。
     *
     * @return 包含此列表中所有元素的数组
     */
    public Object[] toArray() {
        Object[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    /**
     * 返回一个包含此列表中所有元素的数组，按适当顺序（从第一个到最后一个元素）；返回数组的运行时类型是指定数组的类型。
     * 如果列表适合指定的数组，则将其返回；否则，将分配一个具有指定数组运行时类型和此列表大小的新数组。
     *
     * <p>如果此列表适合指定的数组并且还有空余空间（即数组比列表元素多），则紧随列表末尾的数组元素将设置为 {@code null}。
     * （仅当调用者知道此列表不包含任何 {@code null} 元素时，此操作有助于确定此列表的长度。）
     *
     * <p>像 {@link #toArray()} 方法一样，此方法作为基于数组和基于集合的 API 之间的桥梁。
     * 此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况下可以节省分配成本。
     *
     * <p>假设 {@code x} 是一个仅包含字符串的列表。以下代码可以用来将列表转储到新分配的 {@code String} 数组中：
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * 请注意，{@code toArray(new Object[0])} 的功能与 {@code toArray()} 相同。
     *
     * @param a 要存储列表元素的数组，如果它足够大；否则，将分配一个具有相同运行时类型的新数组
     * @return 包含此列表中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此列表中每个元素的运行时类型的超类
     * @throws NullPointerException 如果指定数组为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        if (a.length < len)
            return (T[]) Arrays.copyOf(elements, len, a.getClass());
        else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

    // 位置访问操作

    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        return get(getArray(), index);
    }

    /**
     * 使用指定的元素替换此列表中指定位置的元素。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            E oldValue = get(elements, index);

            if (oldValue != element) {
                int len = elements.length;
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element;
                setArray(newElements);
            } else {
                // 确保 volatile 写语义
                setArray(elements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定元素追加到此列表的末尾。
     *
     * @param e 要追加到此列表的元素
     * @return {@code true} （如 {@link Collection#add} 所指定）
     */
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在指定位置插入指定的元素。将当前位置的元素（如果有）和任何后续元素右移（将其索引加一）。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                        ", Size: "+len);
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + 1);
            else {
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1,
                        numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除此列表中指定位置的元素。将所有后续元素左移（其索引减 1）。返回从列表中移除的元素。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index,
                        numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除此列表中第一次出现的指定元素（如果存在）。如果此列表不包含该元素，则列表不做改动。
     * 更正式地讲，移除元素 {@code o} 的最小索引 {@code i}，使得
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * （如果这样的元素存在）。如果此列表包含该元素，则返回 {@code true}（或者说，列表因调用而改变）。
     *
     * @param o 要从此列表中移除的元素（如果存在）
     * @return 如果此列表包含指定的元素，则返回 {@code true}
     */
    public boolean remove(Object o) {
        Object[] snapshot = getArray();
        int index = indexOf(o, snapshot, 0, snapshot.length);
        return (index < 0) ? false : remove(o, snapshot, index);
    }

    /**
     * 使用一个强提示，表示给定快照中包含元素 {@code o}，在给定索引上移除该元素。
     */
    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) findIndex: {
                int prefix = Math.min(index, len);
                for (int i = 0; i < prefix; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;
                        break findIndex;
                    }
                }
                if (index >= len)
                    return false;
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)
                    return false;
            }
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从此列表中移除索引位于 {@code fromIndex}（包括）到 {@code toIndex}（不包括）之间的所有元素。
     * 将任何后续元素左移（减少它们的索引）。调用后列表缩短了 {@code (toIndex - fromIndex)} 个元素。
     * 如果 {@code toIndex==fromIndex}，则此操作没有效果。
     *
     * @param fromIndex 要移除的第一个元素的索引
     * @param toIndex 移除的最后一个元素的下一个索引
     * @throws IndexOutOfBoundsException 如果 {@code fromIndex} 或 {@code toIndex} 超出范围
     *         ({@code fromIndex < 0 || toIndex > size() || toIndex < fromIndex})
     */
    void removeRange(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;

            if (fromIndex < 0 || toIndex > len || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();
            int newlen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, newlen));
            else {
                Object[] newElements = new Object[newlen];
                System.arraycopy(elements, 0, newElements, 0, fromIndex);
                System.arraycopy(elements, toIndex, newElements, fromIndex, numMoved);
                setArray(newElements);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果指定的元素尚未存在，则将其追加到列表中。
     *
     * @param e 要添加到列表中的元素（如果不存在）
     * @return 如果元素已添加，返回 {@code true}
     */
    public boolean addIfAbsent(E e) {
        Object[] snapshot = getArray();
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false :
                addIfAbsent(e, snapshot);
    }

    /**
     * 一个使用强提示的 addIfAbsent 版本，表明最近的快照不包含元素 {@code e}。
     */
    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // 优化丢失的对另一个 addXXX 操作的竞争
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                if (indexOf(e, current, common, len) >= 0)
                    return false;
            }
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果此列表包含指定集合的所有元素，则返回 {@code true}。
     *
     * @param c 要检查其是否包含在此列表中的集合
     * @return 如果此列表包含指定集合的所有元素，则返回 {@code true}
     * @throws NullPointerException 如果指定的集合为 {@code null}
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Object e : c) {
            if (indexOf(e, elements, 0, len) < 0)
                return false;
        }
        return true;
    }

    /**
     * 从此列表中移除包含在指定集合中的所有元素。
     * 由于需要内部的临时数组，因此此类中的此操作特别耗时。
     *
     * @param c 包含要从此列表中移除的元素的集合
     * @return 如果此列表因调用而发生更改，则返回 {@code true}
     * @throws ClassCastException 如果此列表中元素的类与指定集合不兼容
     * @throws NullPointerException 如果此列表包含 {@code null} 元素并且指定集合不允许 {@code null} 元素，或者指定集合为 {@code null}
     * @see #remove(Object)
     */
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (!c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 仅保留此列表中包含在指定集合中的元素。换句话说，移除不包含在指定集合中的所有元素。
     *
     * @param c 包含要保留在此列表中的元素的集合
     * @return 如果此列表因调用而发生更改，则返回 {@code true}
     * @throws ClassCastException 如果此列表中元素的类与指定集合不兼容
     * @throws NullPointerException 如果此列表包含 {@code null} 元素并且指定集合不允许 {@code null} 元素，或者指定集合为 {@code null}
     * @see #remove(Object)
     */
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定集合中不存在于此列表中的所有元素追加到此列表的末尾，按集合的迭代器返回的顺序。
     *
     * @param c 包含要添加到此列表的元素的集合
     * @return 添加的元素数量
     * @throws NullPointerException 如果指定的集合为 {@code null}
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray();
        if (cs.length == 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            int added = 0;
            for (int i = 0; i < cs.length; ++i) {
                Object e = cs[i];
                if (indexOf(e, elements, 0, len) < 0 &&
                        indexOf(e, cs, 0, added) < 0)
                    cs[added++] = e;
            }
            if (added > 0) {
                Object[] newElements = Arrays.copyOf(elements, len + added);
                System.arraycopy(cs, 0, newElements, len, added);
                setArray(newElements);
            }
            return added;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除此列表中的所有元素。调用此方法后，列表将为空。
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(new Object[0]);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定集合中的所有元素追加到此列表的末尾，按集合的迭代器返回的顺序。
     *
     * @param c 包含要添加到此列表的元素的集合
     * @return 如果此列表因调用而发生更改，则返回 {@code true}
     * @throws NullPointerException 如果指定的集合为 {@code null}
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
                ((CopyOnWriteArrayList<?>)c).getArray() : c.toArray();
        if (cs.length == 0)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && cs.getClass() == Object[].class)
                setArray(cs);
            else {
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将指定集合中的所有元素插入到此列表的指定位置。将当前位于该位置的元素（如果有）及其后续元素右移（将其索引加 1）。
     * 新元素将按照指定集合的迭代器返回的顺序出现在此列表中。
     *
     * @param index 在此列表中插入第一个元素的索引
     * @param c 包含要添加到此列表的元素的集合
     * @return 如果此列表因调用而发生更改，则返回 {@code true}
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException 如果指定的集合为 {@code null}
     * @see #add(int, Object)
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            if (cs.length == 0)
                return false;
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + cs.length);
            else {
                newElements = new Object[len + cs.length];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + cs.length, numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void forEach(Consumer<? super E> action) {
        if (action == null) throw new NullPointerException();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            @SuppressWarnings("unchecked") E e = (E) elements[i];
            action.accept(e);  // 对每个元素执行给定的操作
        }
    }

    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();  // 加锁以确保线程安全
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    if (!filter.test(e)) // 仅保留不符合条件的元素
                        temp[newlen++] = e;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));  // 更新数组
                    return true;  // 如果有元素被删除，返回 true
                }
            }
            return false;  // 如果没有元素被删除，返回 false
        } finally {
            lock.unlock();  // 解锁
        }
    }

    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();  // 加锁以确保线程安全
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            for (int i = 0; i < len; ++i) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                newElements[i] = operator.apply(e);  // 对每个元素应用变换函数
            }
            setArray(newElements);  // 设置新数组
        } finally {
            lock.unlock();  // 解锁
        }
    }

    public void sort(Comparator<? super E> c) {
        final ReentrantLock lock = this.lock;
        lock.lock();  // 加锁以确保线程安全
        try {
            Object[] elements = getArray();
            Object[] newElements = Arrays.copyOf(elements, elements.length);
            @SuppressWarnings("unchecked") E[] es = (E[])newElements;
            Arrays.sort(es, c);  // 对数组进行排序
            setArray(newElements);  // 设置排序后的数组
        } finally {
            lock.unlock();  // 解锁
        }
    }

    /**
     * 将此列表保存到流中（即序列化）。
     *
     * @param s 输出流
     * @throws java.io.IOException 如果发生 I/O 错误
     * @serialData 输出列表的长度（int），然后依次输出所有元素（每个元素为 Object）
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();  // 写入默认对象数据

        Object[] elements = getArray();
        // 写出数组的长度
        s.writeInt(elements.length);

        // 按顺序写出所有元素
        for (Object element : elements)
            s.writeObject(element);
    }

    /**
     * 从流中重新构造此列表（即反序列化）。
     *
     * @param s 输入流
     * @throws ClassNotFoundException 如果找不到序列化对象的类
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();  // 读取默认对象数据

        // 绑定新锁
        resetLock();

        // 读取数组长度并分配数组
        int len = s.readInt();
        Object[] elements = new Object[len];

        // 按顺序读取所有元素
        for (int i = 0; i < len; i++)
            elements[i] = s.readObject();
        setArray(elements);  // 设置反序列化后的数组
    }

    /**
     * 返回此列表的字符串表示形式。字符串表示由列表中元素的字符串表示按顺序组成，
     * 用方括号（"[]"）括起来。相邻元素之间用", "分隔。元素通过 {@link String#valueOf(Object)} 转换为字符串。
     *
     * @return 此列表的字符串表示
     */
    public String toString() {
        return Arrays.toString(getArray());
    }

    /**
     * 比较指定对象与此列表是否相等。
     * 当指定对象与此对象相同，或指定对象也是 {@link List} 且指定列表的迭代器返回的元素序列与此列表的迭代器返回的序列相同，则返回 true。
     *
     * @param o 要与此列表进行比较的对象
     * @return 如果指定对象等于此列表，返回 true
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        List<?> list = (List<?>)(o);
        Iterator<?> it = list.iterator();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i)
            if (!it.hasNext() || !eq(elements[i], it.next()))
                return false;
        if (it.hasNext())
            return false;
        return true;
    }

    /**
     * 返回此列表的哈希码值。
     *
     * @return 此列表的哈希码值
     */
    public int hashCode() {
        int hashCode = 1;
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            Object obj = elements[i];
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    // 迭代器和其他扩展操作

    /**
     * 返回此列表中的元素的迭代器（按适当的顺序）。
     *
     * <p>返回的迭代器提供了列表状态的快照。当构造迭代器时，不需要同步遍历操作。
     * 迭代器不支持 {@code remove}、{@code set} 和 {@code add} 方法。
     *
     * @return 此列表中元素的迭代器，按适当的顺序
     */
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);
    }
    /**
     * {@inheritDoc}
     *
     * <p>返回的迭代器提供了迭代器构建时列表状态的快照。在遍历迭代器时不需要同步操作。
     * 该迭代器不支持 {@code remove}、{@code set} 或 {@code add} 方法。
     */
    public ListIterator<E> listIterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * 返回此列表中的元素的列表迭代器（按适当的顺序），从列表中的指定位置开始。
     * 指定的索引指示列表中的第一个元素。
     *
     * @param index 列表中的初始位置
     * @return 此列表中元素的列表迭代器，按适当的顺序
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index < 0 || index > len)
            throw new IndexOutOfBoundsException("Index: " + index);
        return new COWIterator<E>(elements, index);
    }

    /**
     * 返回此列表元素的 {@link Spliterator}。
     *
     * <p>{@code Spliterator} 报告 {@link Spliterator#IMMUTABLE}、
     * {@link Spliterator#ORDERED}、{@link Spliterator#SIZED} 和
     * {@link Spliterator#SUBSIZED} 特性。
     *
     * <p>此 spliterator 提供了列表在 spliterator 构造时的状态快照。在使用 spliterator 时不需要同步操作。
     *
     * @return 一个遍历此列表元素的 {@code Spliterator}
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(
                getArray(), Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    /**
     * 用于 CopyOnWriteArrayList 的快照迭代器。
     * 迭代器是不可变的，不支持 {@code remove}、{@code set} 和 {@code add} 方法。
     */
    static final class COWIterator<E> implements ListIterator<E> {
        /** 数组的快照 */
        private final Object[] snapshot;
        /** 下一个要返回的元素的索引 */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }

    /**
     * 返回此列表的部分视图，指定范围内的元素包含在返回的子列表中。
     *
     * @param fromIndex 子列表的低端点（包含）
     * @param toIndex 子列表的高端点（不包含）
     * @return 指定范围内的视图
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (fromIndex < 0 || toIndex > len || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();
            return new COWSubList<E>(this, fromIndex, toIndex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * CopyOnWriteArrayList 的子列表视图。继承 AbstractList 以简化实现。
     */
    private static class COWSubList<E> extends AbstractList<E> implements RandomAccess {
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // 仅在持有 l 的锁时调用
        COWSubList(CopyOnWriteArrayList<E> list, int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.getArray();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // 仅在持有 l 的锁时调用，检查是否发生并发修改
        private void checkForComodification() {
            if (l.getArray() != expectedArray)
                throw new ConcurrentModificationException();
        }

        // 仅在持有 l 的锁时调用，检查索引是否在范围内
        private void rangeCheck(int index) {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        public E set(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E x = l.set(index + offset, element);
                expectedArray = l.getArray();
                return x;
            } finally {
                lock.unlock();
            }
        }

        public E get(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                return l.get(index + offset);
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return size;
            } finally {
                lock.unlock();
            }
        }

        public void add(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException();
                l.add(index + offset, element);
                expectedArray = l.getArray();
                size++;
            } finally {
                lock.unlock();
            }
        }

        public E remove(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index + offset);
                expectedArray = l.getArray();
                size--;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index == -1)
                return false;
            remove(index);
            return true;
        }

        public void clear() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                l.removeRange(offset, offset + size);
                expectedArray = l.getArray();
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        public Iterator<E> iterator() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return new COWSubListIterator<>(l, 0, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public ListIterator<E> listIterator(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
                return new COWSubListIterator<>(l, index, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
                    throw new IndexOutOfBoundsException();
                return new COWSubList<>(l, fromIndex + offset, toIndex + offset);
            } finally {
                lock.unlock();
            }
        }

        public void forEach(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            for (int i = lo; i < hi; ++i) {
                @SuppressWarnings("unchecked") E e = (E) a[i];
                action.accept(e);
            }
        }

        public void replaceAll(UnaryOperator<E> operator) {
            if (operator == null) throw new NullPointerException();
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                for (int i = lo; i < hi; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    newElements[i] = operator.apply(e);
                }
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public void sort(Comparator<? super E> c) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                @SuppressWarnings("unchecked") E[] es = (E[]) newElements;
                Arrays.sort(es, lo, hi, c);
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (!c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean removeIf(Predicate<? super E> filter) {
            if (filter == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) elements[i];
                        if (!filter.test(e))
                            temp[newSize++] = e;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public Spliterator<E> spliterator() {
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            return Spliterators.spliterator
                    (a, lo, hi, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        }
    }

    // COWSubListIterator 类实现
    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index + offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return it.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return it.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int s = size;
            ListIterator<E> i = it;
            while (nextIndex() < s) {
                action.accept(i.next());
            }
        }
    }

    // 支持序列化期间重置锁定
    private void resetLock() {
        UNSAFE.putObjectVolatile(this, lockOffset, new ReentrantLock());
    }
    private static final sun.misc.Unsafe UNSAFE;
    private static final long lockOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CopyOnWriteArrayList.class;
            lockOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("lock"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}



