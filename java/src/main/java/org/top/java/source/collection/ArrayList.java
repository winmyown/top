/*
 * 版权所有 (c) 1997, 2013, Oracle 和/或其附属公司。保留所有权利。
 * ORACLE 专有/机密。使用受许可条款约束。
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.top.java.source.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * <tt>List</tt>接口的可调整大小的数组实现。实现了所有可选的列表操作，并允许所有元素，包括<tt>null</tt>。除了实现<tt>List</tt>接口外，此类还提供了一些方法来操作用于内部存储列表的数组的大小。（此类大致相当于<tt>Vector</tt>，只不过它是不同步的。）
 *
 * <p><tt>size</tt>、<tt>isEmpty</tt>、<tt>get</tt>、<tt>set</tt>、<tt>iterator</tt>和<tt>listIterator</tt>操作以常数时间运行。<tt>add</tt>操作以<i>摊还常数时间</i>运行，即添加n个元素需要O(n)时间。所有其他操作大致以线性时间运行。与<tt>LinkedList</tt>实现相比，常数因子较低。
 *
 * <p>每个<tt>ArrayList</tt>实例都有一个<i>容量</i>。容量是用于存储列表中元素的数组的大小。它始终至少与列表大小一样大。随着元素被添加到ArrayList中，其容量会自动增长。除了添加元素的摊还常数时间成本外，增长策略的细节未指定。
 *
 * <p>应用程序可以在添加大量元素之前使用<tt>ensureCapacity</tt>操作来增加<tt>ArrayList</tt>实例的容量。这可能会减少增量重新分配的次数。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>如果多个线程同时访问一个<tt>ArrayList</tt>实例，并且至少有一个线程在结构上修改了列表，则必须在外部进行同步。（结构修改是指任何添加或删除一个或多个元素的操作，或显式调整后备数组的大小；仅设置元素的值不是结构修改。）这通常通过同步某些自然封装列表的对象来实现。
 *
 * 如果不存在这样的对象，则应使用{@link Collections#synchronizedList Collections.synchronizedList}方法“包装”该列表。最好在创建时完成此操作，以防止意外不同步地访问列表：<pre>
 *   List list = Collections.synchronizedList(new ArrayList(...));</pre>
 *
 * <p><a name="fail-fast">
 * 此类返回的{@link #iterator() iterator}和{@link #listIterator(int) listIterator}方法返回的迭代器是<em>快速失败</em>的：</a>如果在创建迭代器之后的任何时候对列表进行结构修改，除非通过迭代器自己的{@link ListIterator#remove() remove}或{@link ListIterator#add(Object) add}方法，否则迭代器将抛出{@link ConcurrentModificationException}。因此，面对并发修改，迭代器会快速而干净地失败，而不是在未来的某个不确定时间冒任意、非确定性行为的风险。
 *
 * <p>请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改的情况下，无法做出任何硬性保证。快速失败迭代器会尽最大努力抛出{@code ConcurrentModificationException}。因此，编写依赖于此异常的程序是错误的：<i>迭代器的快速失败行为应仅用于检测错误。</i>
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     java.util.Collection
 * @see     List
 * @see     LinkedList
 * @see     Vector
 * @since   1.2
 */

public class ArrayList<E> extends AbstractList<E>
        implements List<E>, java.util.RandomAccess, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 8683452581122892189L;

    /**
     * 默认初始容量。
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * 用于空实例的共享空数组实例。
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};

    /**
     * 用于默认大小空实例的共享空数组实例。我们将其与 EMPTY_ELEMENTDATA 区分开来，以便知道在添加第一个元素时需要扩容多少。
     */
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * ArrayList的元素存储在该数组缓冲区中。
     * ArrayList的容量是该数组缓冲区的长度。任何
     * 带有elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA的空ArrayList
     * 将在添加第一个元素时扩展为DEFAULT_CAPACITY。
     */
    transient Object[] elementData; // 非私有以简化嵌套类访问

    /**
     * ArrayList 的大小（包含的元素数量）。
     *
     * @serial
     */
    private int size;

    /**
     * 构造一个具有指定初始容量的空列表。
     *
     * @param  initialCapacity  列表的初始容量
     * @throws IllegalArgumentException 如果指定的初始容量为负数
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    /**
     * 构造一个初始容量为十的空列表。
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    /**
     * 构造一个包含指定集合元素的列表，元素的顺序与集合的迭代器返回的顺序相同。
     *
     * @param c 包含要放入此列表的元素的集合
     * @throws NullPointerException 如果指定的集合为null
     */
    public ArrayList(java.util.Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray 可能（错误地）不返回 Object[]（参见 6260652）
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // 替换为空数组。
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    /**
     * 将此 <tt>ArrayList</tt> 实例的容量修剪为列表的当前大小。
     * 应用程序可以使用此操作来最小化 <tt>ArrayList</tt> 实例的存储空间。
     */
    public void trimToSize() {
        modCount++;
        if (size < elementData.length) {
            elementData = (size == 0)
              ? EMPTY_ELEMENTDATA
              : Arrays.copyOf(elementData, size);
        }
    }

    /**
     * 增加此 <tt>ArrayList</tt> 实例的容量，如果需要，以确保它至少可以容纳由最小容量参数指定的元素数量。
     *
     * @param   minCapacity   所需的最小容量
     */
    public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            // 任何大小，如果不是默认元素表
            ? 0
            // 比默认的空表更大。它已经是
            // 假设为默认大小。
            : DEFAULT_CAPACITY;

        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // 溢出感知代码
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }

    /**
     * 数组分配的最大大小。
     * 一些虚拟机在数组中保留了一些头信息。
     * 尝试分配更大的数组可能会导致
     * OutOfMemoryError: 请求的数组大小超过虚拟机限制
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 增加容量以确保至少可以容纳最小容量参数指定的元素数量。
     *
     * @param minCapacity 所需的最小容量
     */
    private void grow(int minCapacity) {
        // 溢出感知代码
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity 通常接近 size，所以这是一个优化：
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // 溢出
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * 返回此列表中的元素数量。
     *
     * @return 此列表中的元素数量
     */
    public int size() {
        return size;
    }

    /**
     * 如果此列表不包含任何元素，则返回 <tt>true</tt>。
     *
     * @return 如果此列表不包含任何元素，则返回 <tt>true</tt>
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 如果此列表包含指定元素，则返回<tt>true</tt>。
     * 更正式地说，当且仅当此列表包含至少一个元素<tt>e</tt>，
     * 使得<tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>时，
     * 返回<tt>true</tt>。
     *
     * @param o 要测试是否在此列表中的元素
     * @return <tt>true</tt> 如果此列表包含指定元素
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * 返回指定元素在此列表中首次出现的索引，
     * 如果此列表不包含该元素，则返回 -1。
     * 更正式地说，返回满足以下条件的最小索引 <tt>i</tt>：
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>，
     * 如果不存在这样的索引，则返回 -1。
     */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回指定元素在此列表中最后一次出现的索引，
     * 如果此列表不包含该元素，则返回 -1。
     * 更正式地说，返回最高索引 <tt>i</tt>，使得
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>，
     * 如果没有这样的索引，则返回 -1。
     */
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size-1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = size-1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回此 <tt>ArrayList</tt> 实例的浅拷贝。（元素本身不会被复制。）
     *
     * @return 此 <tt>ArrayList</tt> 实例的克隆
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // 这不应该发生，因为我们是可克隆的
            throw new InternalError(e);
        }
    }

    /**
     * 返回一个包含此列表中所有元素的数组，元素的顺序与列表中的顺序一致（从第一个元素到最后一个元素）。
     *
     * <p>返回的数组将是“安全的”，因为此列表不会保留对它的引用。（换句话说，此方法必须分配一个新数组）。因此，调用者可以自由地修改返回的数组。
     *
     * <p>此方法充当基于数组和基于集合的API之间的桥梁。
     *
     * @return 一个包含此列表中所有元素的数组，元素的顺序与列表中的顺序一致
     */
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    /**
     * 返回一个包含此列表中所有元素的数组，元素顺序与列表中的顺序一致（从第一个元素到最后一个元素）；返回数组的运行时类型与指定数组的类型相同。如果列表适合指定的数组，则返回该数组。否则，将分配一个新数组，其运行时类型与指定数组的类型相同，大小与此列表的大小相同。
     *
     * <p>如果列表适合指定的数组且有剩余空间（即数组的元素多于列表），则数组紧接列表末尾的元素将被设置为<tt>null</tt>。（这仅在调用者知道列表不包含任何null元素时，可用于确定列表的长度。）
     *
     * @param a 用于存储列表元素的数组，如果数组足够大；否则，将为此目的分配一个相同运行时类型的新数组。
     * @return 包含列表元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此列表中每个元素运行时类型的超类型
     * @throws NullPointerException 如果指定的数组为null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // 创建一个新的数组，类型与a的运行时类型相同，但内容为我的内容：
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    // 位置访问操作

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * 返回列表中指定位置的元素。
     *
     * @param index 要返回的元素的索引
     * @return 列表中指定位置的元素
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E get(int index) {
        rangeCheck(index);

        return elementData(index);
    }

    /**
     * 用指定元素替换列表中指定位置的元素。
     *
     * @param index 要替换的元素的索引
     * @param element 要存储在指定位置的元素
     * @return 之前位于指定位置的元素
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E set(int index, E element) {
        rangeCheck(index);

        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    /**
     * 将指定元素追加到此列表的末尾。
     *
     * @param e 要追加到此列表的元素
     * @return <tt>true</tt>（由 {@link java.util.Collection#add} 指定）
     */
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // 增加 modCount！！
        elementData[size++] = e;
        return true;
    }

    /**
     * 在列表的指定位置插入指定元素。将当前位于该位置的元素（如果有）以及
     * 任何后续元素向右移动（将其索引加一）。
     *
     * @param index 要插入指定元素的索引
     * @param element 要插入的元素
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);  // 增加 modCount！！
        System.arraycopy(elementData, index, elementData, index + 1,
                         size - index);
        elementData[index] = element;
        size++;
    }

    /**
     * 移除列表中指定位置的元素。
     * 将任何后续元素向左移动（从它们的索引中减去一）。
     *
     * @param index 要移除的元素的索引
     * @return 从列表中移除的元素
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public E remove(int index) {
        rangeCheck(index);

        modCount++;
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // 清除以让GC完成其工作

        return oldValue;
    }

    /**
     * 从此列表中移除第一次出现的指定元素（如果存在）。如果列表不包含该元素，则列表保持不变。
     * 更正式地说，移除满足以下条件的最低索引 <tt>i</tt> 的元素：
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * （如果存在这样的元素）。如果此列表包含指定元素，则返回 <tt>true</tt>（或者等价地，如果此列表因调用而改变）。
     *
     * @param o 要从列表中移除的元素（如果存在）
     * @return <tt>true</tt> 如果此列表包含指定元素
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    /*
     * 私有移除方法，跳过边界检查并且不返回被移除的值。
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // 清除以让GC完成其工作
    }

    /**
     * 移除此列表中的所有元素。调用此方法后，列表将为空。
     */
    public void clear() {
        modCount++;

        // 清除以让GC完成其工作
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        size = 0;
    }

    /**
     * 将指定集合中的所有元素追加到此列表的末尾，按照指定集合的迭代器返回的顺序。
     * 如果在操作进行过程中修改了指定的集合，则此操作的行为是未定义的。
     * （这意味着如果指定的集合是此列表，并且此列表非空，则此调用的行为是未定义的。）
     *
     * @param c 包含要添加到此列表的元素的集合
     * @return <tt>true</tt> 如果此列表因调用而改变
     * @throws NullPointerException 如果指定的集合为 null
     */
    public boolean addAll(java.util.Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // 增加modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 将指定集合中的所有元素插入到此列表中，从指定位置开始。将当前位于该位置的元素（如果有）以及所有后续元素向右移动（增加它们的索引）。新元素将按照指定集合的迭代器返回的顺序出现在列表中。
     *
     * @param index 插入指定集合中第一个元素的索引
     * @param c 包含要添加到此列表中的元素的集合
     * @return <tt>true</tt> 如果此列表因调用而改变
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException 如果指定的集合为 null
     */
    public boolean addAll(int index, java.util.Collection<? extends E> c) {
        rangeCheckForAdd(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // 增加modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 从此列表中移除所有索引介于 {@code fromIndex}（包含）和 {@code toIndex}（不包含）之间的元素。
     * 将任何后续元素向左移动（减少它们的索引）。
     * 此调用将列表缩短 {@code (toIndex - fromIndex)} 个元素。
     * （如果 {@code toIndex==fromIndex}，此操作无效。）
     *
     * @throws IndexOutOfBoundsException 如果 {@code fromIndex} 或
     *         {@code toIndex} 超出范围
     *         （{@code fromIndex < 0 ||
     *          fromIndex >= size() ||
     *          toIndex > size() ||
     *          toIndex < fromIndex}）
     */
    protected void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                         numMoved);

        // 清除以让GC完成其工作
        int newSize = size - (toIndex-fromIndex);
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }
        size = newSize;
    }

    /**
     * 检查给定索引是否在范围内。如果不在范围内，则抛出适当的运行时异常。
     * 此方法*不*检查索引是否为负数：它总是在数组访问之前立即使用，
     * 如果索引为负数，数组访问会抛出ArrayIndexOutOfBoundsException。
     */
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 用于add和addAll的rangeCheck版本。
     */
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 构造一个 IndexOutOfBoundsException 的详细信息。
     * 在错误处理代码的众多可能重构中，
     * 这种“概述”在服务器和客户端虚拟机中表现最佳。
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    /**
     * 从此列表中移除包含在指定集合中的所有元素。
     *
     * @param c 包含要从此列表中移除的元素的集合
     * @return 如果此列表因调用而发生变化，则返回 {@code true}
     * @throws ClassCastException 如果此列表的某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此列表包含 null 元素且指定集合不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see java.util.Collection#contains(Object)
     */
    public boolean removeAll(java.util.Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, false);
    }

    /**
     * 仅保留此列表中包含在指定集合中的元素。换句话说，从此列表中移除所有不包含在指定集合中的元素。
     *
     * @param c 包含要保留在此列表中的元素的集合
     * @return 如果此列表因调用而更改，则返回 {@code true}
     * @throws ClassCastException 如果此列表的某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此列表包含 null 元素且指定集合不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定的集合为 null
     * @see java.util.Collection#contains(Object)
     */
    public boolean retainAll(java.util.Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }

    private boolean batchRemove(java.util.Collection<?> c, boolean complement) {
        final Object[] elementData = this.elementData;
        int r = 0, w = 0;
        boolean modified = false;
        try {
            for (; r < size; r++)
                if (c.contains(elementData[r]) == complement)
                    elementData[w++] = elementData[r];
        } finally {
            // 保留与 AbstractCollection 的行为兼容性
            // 即使 c.contains() 抛出异常。
            if (r != size) {
                System.arraycopy(elementData, r,
                                 elementData, w,
                                 size - r);
                w += size - r;
            }
            if (w != size) {
                // 清除以让GC完成其工作
                for (int i = w; i < size; i++)
                    elementData[i] = null;
                modCount += size - w;
                size = w;
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 将<tt>ArrayList</tt>实例的状态保存到流中（即序列化它）。
     *
     * @serialData 首先发送<tt>ArrayList</tt>实例的底层数组的长度（int），
     *             然后按顺序发送所有元素（每个元素为<tt>Object</tt>）。
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
        // 写出元素数量，以及任何隐藏的内容
        int expectedModCount = modCount;
        s.defaultWriteObject();

        // 将大小写作为容量，以保持与 clone() 的行为兼容性
        s.writeInt(size);

        // 以正确的顺序写出所有元素。
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }

        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * 从流中重建 <tt>ArrayList</tt> 实例（即反序列化它）。
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;

        // 读取大小，以及任何隐藏的内容
        s.defaultReadObject();

        // 读取容量
        s.readInt(); // 忽略

        if (size > 0) {
            // 类似于 clone()，根据大小而不是容量分配数组
            ensureCapacityInternal(size);

            Object[] a = elementData;
            // 以正确的顺序读取所有元素。
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }

    /**
     * 返回一个列表迭代器，该迭代器从列表中的指定位置开始，按正确顺序遍历列表中的元素。
     * 指定的索引表示初始调用 {@link ListIterator#next next} 将返回的第一个元素。
     * 初始调用 {@link ListIterator#previous previous} 将返回指定索引减一的元素。
     *
     * <p>返回的列表迭代器是<a href="#fail-fast"><i>快速失败</i></a>的。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    /**
     * 返回一个列表迭代器，用于遍历此列表中的元素（按正确顺序）。
     *
     * <p>返回的列表迭代器是<a href="#fail-fast"><i>快速失败</i></a>的。
     *
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     * 返回一个按正确顺序遍历此列表中元素的迭代器。
     *
     * <p>返回的迭代器是<a href="#fail-fast"><i>快速失败</i></a>的。
     *
     * @return 一个按正确顺序遍历此列表中元素的迭代器
     */
    public java.util.Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * AbstractList.Itr 的优化版本
     */
    private class Itr implements java.util.Iterator<E> {
        int cursor;       // 返回下一个元素的索引
        int lastRet = -1; // 返回的最后一个元素的索引；如果没有这样的元素，则为-1
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            // 在迭代结束时更新一次以减少堆写入流量
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * AbstractList.ListItr 的优化版本
     */
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];
        }

        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * 返回此列表中指定 {@code fromIndex}（包含）和 {@code toIndex}（不包含）之间的部分的视图。
     * （如果 {@code fromIndex} 和 {@code toIndex} 相等，则返回的列表为空。）
     * 返回的列表由该列表支持，因此返回列表中的非结构性更改会反映在此列表中，反之亦然。
     * 返回的列表支持所有可选的列表操作。
     *
     * <p>此方法消除了对显式范围操作（通常存在于数组中的那种操作）的需求。
     * 任何期望列表的操作都可以通过传递子列表视图而不是整个列表来用作范围操作。
     * 例如，以下惯用法从列表中删除一系列元素：
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * 类似的惯用法可以用于 {@link #indexOf(Object)} 和 {@link #lastIndexOf(Object)}，
     * 并且 {@link Collections} 类中的所有算法都可以应用于子列表。
     *
     * <p>如果支持列表（即此列表）以任何方式进行了<i>结构性修改</i>（除了通过返回的列表之外），
     * 则此方法返回的列表的语义将变为未定义。
     * （结构性修改是指那些更改此列表大小或以其他方式干扰列表的方式，导致正在进行的迭代可能产生不正确结果。）
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
    }

    private class SubList extends AbstractList<E> implements RandomAccess {
        private final AbstractList<E> parent;
        private final int parentOffset;
        private final int offset;
        int size;

        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }

        public E set(int index, E e) {
            rangeCheck(index);
            checkForComodification();
            E oldValue = ArrayList.this.elementData(offset + index);
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }

        public E get(int index) {
            rangeCheck(index);
            checkForComodification();
            return ArrayList.this.elementData(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);
            this.modCount = parent.modCount;
            this.size++;
        }

        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }

        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                               parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(java.util.Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        public Iterator<E> iterator() {
            return listIterator();
        }

        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // 在迭代结束时更新一次以减少堆写入流量
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }

        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                                               offset + this.size, this.modCount);
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * 创建一个<em><a href="Spliterator.html#binding">延迟绑定</a></em>
     * 和<em>快速失败</em>的 {@link Spliterator}，用于遍历此列表中的元素。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED}、
     * {@link Spliterator#SUBSIZED} 和 {@link Spliterator#ORDERED}。
     * 覆盖实现应记录其他特征值的报告。
     *
     * @return 一个 {@code Spliterator}，用于遍历此列表中的元素
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator<>(this, 0, -1, 0);
    }

    /** 基于索引的二分拆分，延迟初始化的Spliterator */
    static final class ArrayListSpliterator<E> implements Spliterator<E> {

        /*
         * 如果ArrayList是不可变的，或者在结构上是不可变的（没有添加、删除等操作），
         * 我们可以使用Arrays.spliterator来实现它们的spliterators。然而，实际上我们
         * 需要在遍历过程中尽可能多地检测干扰，同时不牺牲太多性能。我们主要依赖
         * modCounts。这些并不能保证检测到并发冲突，有时对线程内的干扰过于保守，
         * 但在实践中检测到足够多的问题，因此是值得的。为了实现这一点，我们（1）懒
         * 加载fence和expectedModCount，直到我们需要提交到我们正在检查的状态的最后一刻；
         * 从而提高精度。（这不适用于SubLists，它们使用当前非懒加载的值创建spliterators）。
         * （2）我们只在forEach结束时执行一次ConcurrentModificationException检查
         * （这是最性能敏感的方法）。当使用forEach（而不是迭代器）时，我们通常只能在
         * 操作后检测到干扰，而不是在操作前。进一步的CME触发检查适用于所有其他可能的
         * 假设违反，例如null或过小的elementData数组给定其size()，这些只能是由于干扰
         * 而发生的。这允许forEach的内部循环在没有任何进一步检查的情况下运行，并简化了
         * lambda解析。虽然这确实需要进行一些检查，但请注意，在常见的list.stream().forEach(a)
         * 情况下，除了forEach本身之外，不会在其他地方进行任何检查或其他计算。其他较少使用
         * 的方法无法利用这些优化。
         */

        private final ArrayList<E> list;
        private int index; // 当前索引，在前进/拆分时修改
        private int fence; // -1 直到使用；然后为最后一个索引的下一个
        private int expectedModCount; // 当栅栏设置时初始化

        /** 创建覆盖给定范围的新spliterator */
        ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
                             int expectedModCount) {
            this.list = list; // 如果为空则正常，除非被遍历
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // 在首次使用时初始化围栏到指定大小
            int hi; // （一个专门的变体出现在方法 forEach 中）
            ArrayList<E> lst;
            if ((hi = fence) < 0) {
                if ((lst = list) == null)
                    hi = fence = 0;
                else {
                    expectedModCount = lst.modCount;
                    hi = fence = lst.size;
                }
            }
            return hi;
        }

        public ArrayListSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : // 除非范围太小，否则将范围分成两半
                new ArrayListSpliterator<E>(list, lo, index = mid,
                                            expectedModCount);
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                @SuppressWarnings("unchecked") E e = (E)list.elementData[i];
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // 将访问和检查从循环中提升
            ArrayList<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null && (a = lst.elementData) != null) {
                if ((hi = fence) < 0) {
                    mc = lst.modCount;
                    hi = lst.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) a[i];
                        action.accept(e);
                    }
                    if (lst.modCount == mc)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // 找出需要移除的元素
        // 在此阶段从过滤器谓词抛出的任何异常
        // 将保持集合不被修改
        int removeCount = 0;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            if (filter.test(element)) {
                removeSet.set(i);
                removeCount++;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }

        // 将幸存元素左移，覆盖被移除元素留下的空格
        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                i = removeSet.nextClearBit(i);
                elementData[j] = elementData[i];
            }
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;  // 让gc完成它的工作
            }
            this.size = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
}
