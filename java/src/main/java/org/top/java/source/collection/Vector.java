/*
 * 版权所有 (c) 1994, 2013, Oracle 和/或其附属公司。保留所有权利。
 * ORACLE 专有/机密。使用须遵守许可条款。
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.RandomAccess;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@code Vector} 类实现了一个可增长的对象数组。与数组类似，它包含可以通过整数索引访问的组件。然而，{@code Vector} 的大小可以根据需要增长或缩小，以适应在 {@code Vector} 创建后添加和删除的项目。
 *
 * <p>每个向量通过维护一个 {@code capacity} 和一个 {@code capacityIncrement} 来优化存储管理。{@code capacity} 始终至少与向量的大小一样大；它通常更大，因为随着组件被添加到向量中，向量的存储会以 {@code capacityIncrement} 大小的块增加。应用程序可以在插入大量组件之前增加向量的容量；这减少了增量重新分配的次数。
 *
 * <p><a name="fail-fast">
 * 此类的 {@link #iterator() iterator} 和 {@link #listIterator(int) listIterator} 方法返回的迭代器是 <em>fail-fast</em></a>：
 * 如果在创建迭代器之后的任何时候对向量进行结构修改，除了通过迭代器自身的 {@link ListIterator#remove() remove} 或 {@link ListIterator#add(Object) add} 方法之外，迭代器将抛出 {@link ConcurrentModificationException}。因此，在面对并发修改时，迭代器会快速而干净地失败，而不是在未来不确定的时间冒任意、非确定性行为的风险。{@link #elements() elements} 方法返回的 {@link Enumeration Enumerations} 不是 fail-fast。
 *
 * <p>请注意，迭代器的 fail-fast 行为无法得到保证，因为一般来说，在存在非同步并发修改的情况下，无法做出任何硬性保证。Fail-fast 迭代器会尽最大努力抛出 {@code ConcurrentModificationException}。因此，编写依赖此异常来保证程序正确性的程序是错误的：<i>迭代器的 fail-fast 行为应仅用于检测错误。</i>
 *
 * <p>从 Java 2 平台 v1.2 开始，此类被改造为实现 {@link java.util.List} 接口，使其成为 <a href="{@docRoot}/../technotes/guides/collections/index.html">Java 集合框架</a> 的成员。与新的集合实现不同，{@code Vector} 是同步的。如果不需要线程安全的实现，建议使用 {@link ArrayList} 代替 {@code Vector}。
 *
 * @author  Lee Boynton
 * @author  Jonathan Payne
 * @see java.util.Collection
 * @see LinkedList
 * @since   JDK1.0
 */
public class Vector<E>
    extends AbstractList<E>
    implements java.util.List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    /**
     * 用于存储向量组件的数组缓冲区。向量的容量是该数组缓冲区的长度，
     * 并且至少足够大以包含向量的所有元素。
     *
     * <p>向量中最后一个元素之后的任何数组元素均为 null。
     *
     * @serial
     */
    protected Object[] elementData;

    /**
     * 此 {@code Vector} 对象中有效组件的数量。
     * 组件 {@code elementData[0]} 到
     * {@code elementData[elementCount-1]} 是实际的项目。
     *
     * @serial
     */
    protected int elementCount;

    /**
     * 当向量的大小超过其容量时，容量自动增加的量。如果容量增量小于或等于零，则每次需要增长时，向量的容量将翻倍。
     *
     * @serial
     */
    protected int capacityIncrement;

    /** 使用JDK 1.0.2中的serialVersionUID以确保互操作性 */
    private static final long serialVersionUID = -2767605614048989439L;

    /**
     * 使用指定的初始容量和容量增量构造一个空向量。
     *
     * @param   initialCapacity     向量的初始容量
     * @param   capacityIncrement    当向量溢出时容量增加的量
     * @throws IllegalArgumentException 如果指定的初始容量为负数
     */
    public Vector(int initialCapacity, int capacityIncrement) {
        super();
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        this.elementData = new Object[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * 构造一个具有指定初始容量的空向量，其容量增量为零。
     *
     * @param   initialCapacity   向量的初始容量
     * @throws IllegalArgumentException 如果指定的初始容量为负数
     */
    public Vector(int initialCapacity) {
        this(initialCapacity, 0);
    }

    /**
     * 构造一个空向量，使其内部数据数组的大小为 {@code 10}，其标准容量增量为零。
     */
    public Vector() {
        this(10);
    }

    /**
     * 构造一个包含指定集合元素的向量，元素的顺序与集合的迭代器返回的顺序相同。
     *
     * @param c 要将其元素放入此向量的集合
     * @throws NullPointerException 如果指定的集合为 null
     * @since   1.2
     */
    public Vector(java.util.Collection<? extends E> c) {
        elementData = c.toArray();
        elementCount = elementData.length;
        // c.toArray 可能（错误地）不返回 Object[]（参见 6260652）
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, elementCount, Object[].class);
    }

    /**
     * 将此向量的组件复制到指定的数组中。
     * 此向量中索引为 {@code k} 的元素被复制到
     * {@code anArray} 的组件 {@code k} 中。
     *
     * @param  anArray 组件被复制到的数组
     * @throws NullPointerException 如果给定的数组为 null
     * @throws IndexOutOfBoundsException 如果指定的数组不足以
     *         容纳此向量的所有组件
     * @throws ArrayStoreException 如果此向量的组件不是
     *         可以存储在指定数组中的运行时类型
     * @see #toArray(Object[])
     */
    public synchronized void copyInto(Object[] anArray) {
        System.arraycopy(elementData, 0, anArray, 0, elementCount);
    }

    /**
     * 将此向量的容量修剪为向量的当前大小。如果此向量的容量大于其当前大小，
     * 则通过将其内部数据数组（保存在字段 {@code elementData} 中）替换为
     * 较小的数组，将容量更改为等于当前大小。应用程序可以使用此操作来最小化
     * 向量的存储空间。
     */
    public synchronized void trimToSize() {
        modCount++;
        int oldCapacity = elementData.length;
        if (elementCount < oldCapacity) {
            elementData = Arrays.copyOf(elementData, elementCount);
        }
    }

    /**
     * 如有必要，增加此向量的容量，以确保其至少可以容纳由最小容量参数指定的组件数量。
     *
     * <p>如果此向量的当前容量小于 {@code minCapacity}，则通过替换其内部数据数组（保存在字段 {@code elementData} 中）来增加其容量。新数据数组的大小将为旧大小加上 {@code capacityIncrement}，除非 {@code capacityIncrement} 的值小于或等于零，在这种情况下，新容量将为旧容量的两倍；但如果此新大小仍然小于 {@code minCapacity}，则新容量将为 {@code minCapacity}。
     *
     * @param minCapacity 所需的最小容量
     */
    public synchronized void ensureCapacity(int minCapacity) {
        if (minCapacity > 0) {
            modCount++;
            ensureCapacityHelper(minCapacity);
        }
    }

    /**
     * 这实现了 ensureCapacity 的未同步语义。
     * 该类中的同步方法可以在内部调用此方法以确保容量，而无需承担额外同步的开销。
     *
     * @see #ensureCapacity(int)
     */
    private void ensureCapacityHelper(int minCapacity) {
        // 溢出意识代码
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

    private void grow(int minCapacity) {
        // 溢出意识代码
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                         capacityIncrement : oldCapacity);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
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
     * 设置此向量的大小。如果新大小大于当前大小，则向向量末尾添加新的{@code null}项。
     * 如果新大小小于当前大小，则丢弃索引{@code newSize}及以上的所有元素。
     *
     * @param  newSize   此向量的新大小
     * @throws ArrayIndexOutOfBoundsException 如果新大小为负数
     */
    public synchronized void setSize(int newSize) {
        modCount++;
        if (newSize > elementCount) {
            ensureCapacityHelper(newSize);
        } else {
            for (int i = newSize ; i < elementCount ; i++) {
                elementData[i] = null;
            }
        }
        elementCount = newSize;
    }

    /**
     * 返回此向量的当前容量。
     *
     * @return  当前容量（其内部数据数组的长度，保存在此向量的字段 {@code elementData} 中）
     */
    public synchronized int capacity() {
        return elementData.length;
    }

    /**
     * 返回此向量中的组件数量。
     *
     * @return  此向量中的组件数量
     */
    public synchronized int size() {
        return elementCount;
    }

    /**
     * 测试此向量是否没有分量。
     *
     * @return  {@code true} 当且仅当此向量没有分量，
     *          即其大小为零；
     *          {@code false} 否则。
     */
    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * 返回此向量组件的枚举。返回的 {@code Enumeration} 对象将生成此向量中的所有项。
     * 生成的第一个项是索引为 {@code 0} 的项，然后是索引为 {@code 1} 的项，依此类推。
     *
     * @return  此向量组件的枚举
     * @see     java.util.Iterator
     */
    public Enumeration<E> elements() {
        return new Enumeration<E>() {
            int count = 0;

            public boolean hasMoreElements() {
                return count < elementCount;
            }

            public E nextElement() {
                synchronized (Vector.this) {
                    if (count < elementCount) {
                        return elementData(count++);
                    }
                }
                throw new NoSuchElementException("Vector Enumeration");
            }
        };
    }

    /**
     * 如果此向量包含指定元素，则返回 {@code true}。
     * 更正式地说，当且仅当此向量包含至少一个元素 {@code e}，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 {@code true}。
     *
     * @param o 要测试是否在此向量中的元素
     * @return {@code true} 如果此向量包含指定元素
     */
    public boolean contains(Object o) {
        return indexOf(o, 0) >= 0;
    }

    /**
     * 返回指定元素在此向量中第一次出现的索引，
     * 如果此向量不包含该元素，则返回 -1。
     * 更正式地说，返回满足条件的最低索引 {@code i}，
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * 如果没有这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @return 指定元素在此向量中第一次出现的索引，
     *         如果此向量不包含该元素，则返回 -1
     */
    public int indexOf(Object o) {
        return indexOf(o, 0);
    }

    /**
     * 返回指定元素在此向量中第一次出现的索引，从 {@code index} 开始向前搜索，如果未找到该元素，则返回 -1。
     * 更正式地说，返回满足以下条件的最小索引 {@code i}：
     * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i))))</tt>，
     * 如果没有这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @param index 开始搜索的索引
     * @return 元素在向量中从 {@code index} 位置或之后第一次出现的索引；
     *         如果未找到该元素，则返回 {@code -1}。
     * @throws IndexOutOfBoundsException 如果指定的索引为负数
     * @see     Object#equals(Object)
     */
    public synchronized int indexOf(Object o, int index) {
        if (o == null) {
            for (int i = index ; i < elementCount ; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index ; i < elementCount ; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回指定元素在此向量中最后一次出现的索引，如果此向量不包含该元素，则返回 -1。
     * 更正式地说，返回满足 <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * 的最高索引 {@code i}，如果没有这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @return 指定元素在此向量中最后一次出现的索引，如果此向量不包含该元素，则返回 -1
     */
    public synchronized int lastIndexOf(Object o) {
        return lastIndexOf(o, elementCount-1);
    }

    /**
     * 返回指定元素在此向量中最后一次出现的索引，从 {@code index} 开始向后搜索，如果未找到该元素，则返回 -1。
     * 更正式地说，返回满足以下条件的最高索引 {@code i}：
     * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i))))</tt>，
     * 如果不存在这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @param index 开始向后搜索的索引
     * @return 该元素在 {@code index} 或之前最后一次出现的索引；
     *         如果未找到该元素，则返回 -1。
     * @throws IndexOutOfBoundsException 如果指定的索引大于或等于此向量的当前大小
     */
    public synchronized int lastIndexOf(Object o, int index) {
        if (index >= elementCount)
            throw new IndexOutOfBoundsException(index + " >= "+ elementCount);

        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回指定索引处的组件。
     *
     * <p>此方法在功能上与 {@link #get(int)} 方法相同
     * （该方法属于 {@link java.util.List} 接口）。
     *
     * @param      index   此向量的索引
     * @return     指定索引处的组件
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized E elementAt(int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
        }

        return elementData(index);
    }

    /**
     * 返回此向量的第一个组件（索引为 {@code 0} 的项）。
     *
     * @return     此向量的第一个组件
     * @throws NoSuchElementException 如果此向量没有组件
     */
    public synchronized E firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(0);
    }

    /**
     * 返回向量的最后一个分量。
     *
     * @return  向量的最后一个分量，即索引为
     *          <code>size()&nbsp;-&nbsp;1</code>的分量。
     * @throws NoSuchElementException 如果该向量为空
     */
    public synchronized E lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(elementCount - 1);
    }

    /**
     * 将此向量的指定 {@code index} 处的组件设置为指定对象。该位置之前的组件将被丢弃。
     *
     * <p>索引必须是一个大于等于 {@code 0} 且小于向量当前大小的值。
     *
     * <p>此方法在功能上与 {@link #set(int, Object) set(int, E)} 方法（属于 {@link java.util.List} 接口的一部分）相同。请注意，
     * {@code set} 方法反转了参数的顺序，以更接近数组的使用方式。另请注意，{@code set} 方法返回存储在指定位置的旧值。
     *
     * @param      obj     要设置的组件
     * @param      index   指定的索引
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized void setElementAt(E obj, int index) {
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " +
                                                     elementCount);
        }
        elementData[index] = obj;
    }

    /**
     * 删除指定索引处的组件。此向量中索引大于或等于指定
     * {@code index} 的每个组件将向下移动，其索引值将比之前的值小一。
     * 此向量的大小将减小 {@code 1}。
     *
     * <p>索引必须是一个大于或等于 {@code 0} 且小于向量当前大小的值。
     *
     * <p>此方法在功能上与 {@link #remove(int)} 方法相同（该方法属于
     * {@link java.util.List} 接口）。请注意，{@code remove} 方法返回
     * 存储在指定位置的原值。
     *
     * @param      index   要删除的对象的索引
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index >= size()})
     */
    public synchronized void removeElementAt(int index) {
        modCount++;
        if (index >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(index + " >= " +
                                                     elementCount);
        }
        else if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int j = elementCount - index - 1;
        if (j > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, j);
        }
        elementCount--;
        elementData[elementCount] = null; /* 让垃圾回收器完成其工作 */
    }

    /**
     * 将指定对象作为组件插入到此向量的指定 {@code index} 处。此向量中索引大于或等于指定 {@code index} 的每个组件都将向上移动，使其索引比之前的值大1。
     *
     * <p>索引必须是一个大于或等于 {@code 0} 且小于或等于向量当前大小的值。（如果索引等于向量的当前大小，则新元素将被追加到向量中。）
     *
     * <p>此方法在功能上与 {@link #add(int, Object) add(int, E)} 方法（该方法属于 {@link java.util.List} 接口）相同。请注意，{@code add} 方法反转了参数的顺序，以更接近数组的使用方式。
     *
     * @param      obj     要插入的组件
     * @param      index   插入新组件的位置
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index > size()})
     */
    public synchronized void insertElementAt(E obj, int index) {
        modCount++;
        if (index > elementCount) {
            throw new ArrayIndexOutOfBoundsException(index
                                                     + " > " + elementCount);
        }
        ensureCapacityHelper(elementCount + 1);
        System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
        elementData[index] = obj;
        elementCount++;
    }

    /**
     * 将指定组件添加到此向量的末尾，
     * 使其大小增加一。如果此向量的大小超过其容量，
     * 则其容量会增加。
     *
     * <p>此方法在功能上与
     * {@link #add(Object) add(E)}
     * 方法（属于 {@link java.util.List} 接口的一部分）相同。
     *
     * @param   obj   要添加的组件
     */
    public synchronized void addElement(E obj) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = obj;
    }

    /**
     * 从该向量中移除参数第一次（索引最低的）出现的位置。如果在该向量中找到该对象，则向量中索引大于或等于该对象索引的每个组件都会向下移动，使其索引比之前的值小一。
     *
     * <p>此方法在功能上与{@link #remove(Object)}方法（属于{@link java.util.List}接口的一部分）相同。
     *
     * @param   obj   要移除的组件
     * @return  {@code true} 如果参数是该向量的一个组件；否则返回 {@code false}。
     */
    public synchronized boolean removeElement(Object obj) {
        modCount++;
        int i = indexOf(obj);
        if (i >= 0) {
            removeElementAt(i);
            return true;
        }
        return false;
    }

    /**
     * 移除该向量中的所有组件并将其大小设置为零。
     *
     * <p>此方法在功能上与{@link #clear}方法相同（该方法是{@link java.util.List}接口的一部分）。
     */
    public synchronized void removeAllElements() {
        modCount++;
        // 让gc完成它的工作
        for (int i = 0; i < elementCount; i++)
            elementData[i] = null;

        elementCount = 0;
    }

    /**
     * 返回此向量的克隆。副本将包含对内部数据数组的克隆的引用，
     * 而不是对此 {@code Vector} 对象的原始内部数据数组的引用。
     *
     * @return  此向量的克隆
     */
    public synchronized Object clone() {
        try {
            @SuppressWarnings("unchecked")
                Vector<E> v = (Vector<E>) super.clone();
            v.elementData = Arrays.copyOf(elementData, elementCount);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // 这不应该发生，因为我们是可克隆的
            throw new InternalError(e);
        }
    }

    /**
     * 返回一个包含此 Vector 中所有元素的数组，顺序正确。
     *
     * @since 1.2
     */
    public synchronized Object[] toArray() {
        return Arrays.copyOf(elementData, elementCount);
    }

    /**
     * 返回一个包含此 Vector 中所有元素的数组，顺序正确；返回数组的运行时类型是指定数组的类型。如果 Vector 适合指定的数组，则返回该数组。否则，将分配一个新数组，其运行时类型为指定数组的类型，大小为该 Vector 的大小。
     *
     * <p>如果 Vector 适合指定的数组且有剩余空间（即，数组的元素多于 Vector），则将数组紧接 Vector 末尾的元素设置为 null。（这在确定 Vector 的长度时<em>仅</em>在调用者知道 Vector 不包含任何 null 元素时有用。）
     *
     * @param a 用于存储 Vector 元素的数组，如果它足够大；否则，将为此目的分配一个具有相同运行时类型的新数组。
     * @return 包含 Vector 元素的数组
     * @throws ArrayStoreException 如果 a 的运行时类型不是此 Vector 中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果给定的数组为 null
     * @since 1.2
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T[] toArray(T[] a) {
        if (a.length < elementCount)
            return (T[]) Arrays.copyOf(elementData, elementCount, a.getClass());

        System.arraycopy(elementData, 0, a, 0, elementCount);

        if (a.length > elementCount)
            a[elementCount] = null;

        return a;
    }

    // 位置访问操作

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * 返回此Vector中指定位置的元素。
     *
     * @param index 要返回的元素的索引
     * @return 指定索引处的对象
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *            ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E get(int index) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        return elementData(index);
    }

    /**
     * 用指定的元素替换此向量中指定位置的元素。
     *
     * @param index 要替换的元素的索引
     * @param element 要存储在指定位置的元素
     * @return 先前在指定位置的元素
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index >= size()})
     * @since 1.2
     */
    public synchronized E set(int index, E element) {
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    /**
     * 将指定元素追加到此 Vector 的末尾。
     *
     * @param e 要追加到此 Vector 的元素
     * @return {@code true}（由 {@link java.util.Collection#add} 指定）
     * @since 1.2
     */
    public synchronized boolean add(E e) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = e;
        return true;
    }

    /**
     * 移除此 Vector 中第一次出现的指定元素。
     * 如果 Vector 不包含该元素，则 Vector 保持不变。更正式地说，移除具有最低索引 i 的元素，
     * 使得 {@code (o==null ? get(i)==null : o.equals(get(i)))}（如果存在这样的元素）。
     *
     * @param o 要从此 Vector 中移除的元素（如果存在）
     * @return 如果 Vector 包含指定元素，则返回 true
     * @since 1.2
     */
    public boolean remove(Object o) {
        return removeElement(o);
    }

    /**
     * 在此Vector的指定位置插入指定的元素。
     * 将当前位于该位置的元素（如果有）以及任何后续元素向右移动（将其索引加一）。
     *
     * @param index 要插入指定元素的索引
     * @param element 要插入的元素
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index > size()})
     * @since 1.2
     */
    public void add(int index, E element) {
        insertElementAt(element, index);
    }

    /**
     * 移除Vector中指定位置的元素。
     * 将所有后续元素向左移动（将它们的索引减一）。返回从Vector中移除的元素。
     *
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index >= size()})
     * @param index 要移除的元素的索引
     * @return 被移除的元素
     * @since 1.2
     */
    public synchronized E remove(int index) {
        modCount++;
        if (index >= elementCount)
            throw new ArrayIndexOutOfBoundsException(index);
        E oldValue = elementData(index);

        int numMoved = elementCount - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--elementCount] = null; // 让gc完成它的工作

        return oldValue;
    }

    /**
     * 移除该向量中的所有元素。在此调用返回后，向量将为空（除非抛出异常）。
     *
     * @since 1.2
     */
    public void clear() {
        removeAllElements();
    }

    // 批量操作

    /**
     * 如果此 Vector 包含指定集合中的所有元素，则返回 true。
     *
     * @param   c 一个集合，其元素将被测试是否包含在此 Vector 中
     * @return  如果此 Vector 包含指定集合中的所有元素，则返回 true
     * @throws  NullPointerException 如果指定的集合为 null
     */
    public synchronized boolean containsAll(java.util.Collection<?> c) {
        return super.containsAll(c);
    }

    /**
     * 将指定集合中的所有元素追加到此向量的末尾，按照指定集合的迭代器返回的顺序。
     * 如果在操作过程中修改了指定的集合，则此操作的行为是未定义的。
     * （这意味着如果指定的集合是此向量，并且此向量非空，则此调用的行为是未定义的。）
     *
     * @param c 要插入到此向量中的元素
     * @return 如果此向量因调用而更改，则返回 {@code true}
     * @throws NullPointerException 如果指定的集合为 null
     * @since 1.2
     */
    public synchronized boolean addAll(java.util.Collection<? extends E> c) {
        modCount++;
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityHelper(elementCount + numNew);
        System.arraycopy(a, 0, elementData, elementCount, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

    /**
     * 从该Vector中移除所有包含在指定集合中的元素。
     *
     * @param c 包含要从Vector中移除的元素的集合
     * @return 如果此Vector因调用而改变，则返回true
     * @throws ClassCastException 如果此Vector中的一个或多个元素的类型与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此Vector包含一个或多个null元素，而指定集合不支持null元素
     * (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定的集合为null
     * @since 1.2
     */
    public synchronized boolean removeAll(java.util.Collection<?> c) {
        return super.removeAll(c);
    }

    /**
     * 仅保留此 Vector 中存在于指定集合中的元素。换句话说，从该 Vector 中移除所有不包含在指定集合中的元素。
     *
     * @param c 包含要保留在此 Vector 中的元素的集合
     *          （所有其他元素将被移除）
     * @return 如果此 Vector 因调用而更改，则返回 true
     * @throws ClassCastException 如果此 Vector 中一个或多个元素的类型与指定集合不兼容
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此 Vector 包含一个或多个 null 元素，而指定集合不支持 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定的集合为 null
     * @since 1.2
     */
    public synchronized boolean retainAll(java.util.Collection<?> c) {
        return super.retainAll(c);
    }

    /**
     * 将指定集合中的所有元素插入到此 Vector 的指定位置。将当前位于该位置的元素（如果有）以及任何后续元素向右移动（增加它们的索引）。新元素将按照指定集合的迭代器返回的顺序出现在 Vector 中。
     *
     * @param index 插入指定集合中第一个元素的索引
     * @param c 要插入到此 Vector 中的元素
     * @return 如果此 Vector 因调用而更改，则返回 {@code true}
     * @throws ArrayIndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index > size()})
     * @throws NullPointerException 如果指定的集合为 null
     * @since 1.2
     */
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        modCount++;
        if (index < 0 || index > elementCount)
            throw new ArrayIndexOutOfBoundsException(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityHelper(elementCount + numNew);

        int numMoved = elementCount - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        elementCount += numNew;
        return numNew != 0;
    }

    /**
     * 将指定的对象与此 Vector 进行相等性比较。当且仅当指定的对象也是一个 List，两个 List 具有相同的大小，并且两个 List 中所有对应的元素对都<em>相等</em>时，返回 true。
     * （两个元素 {@code e1} 和 {@code e2} 是<em>相等</em>的，如果 {@code (e1==null ? e2==null : e1.equals(e2))}。）
     * 换句话说，如果两个 List 以相同的顺序包含相同的元素，则它们被定义为相等。
     *
     * @param o 要与该 Vector 进行相等性比较的对象
     * @return 如果指定的对象等于此 Vector，则返回 true
     */
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * 返回此向量的哈希码值。
     */
    public synchronized int hashCode() {
        return super.hashCode();
    }

    /**
     * 返回此向量的字符串表示形式，包含
     * 每个元素的字符串表示形式。
     */
    public synchronized String toString() {
        return super.toString();
    }

    /**
     * 返回此列表中从fromIndex（包括）到toIndex（不包括）之间的部分的视图。
     * （如果fromIndex和toIndex相等，则返回的列表为空。）
     * 返回的列表由该列表支持，因此返回列表中的更改会反映在此列表中，反之亦然。
     * 返回的列表支持此列表支持的所有可选列表操作。
     *
     * <p>此方法消除了对显式范围操作的需求（类似于数组的常见操作）。
     * 任何期望列表的操作都可以通过操作子列表视图来作为范围操作。
     * 例如，以下惯用法从列表中删除一系列元素：
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * 类似的惯用法可以用于indexOf和lastIndexOf，
     * 并且Collections类中的所有算法都可以应用于子列表。
     *
     * <p>如果支持列表（即此列表）以任何方式进行了<i>结构修改</i>，
     * 而不是通过返回的列表进行修改，则此方法返回的列表的语义将变得未定义。
     * （结构修改是指那些更改列表大小或以其他方式干扰列表的修改，
     * 可能导致进行中的迭代产生不正确的结果。）
     *
     * @param fromIndex 子列表的低端点（包括）
     * @param toIndex 子列表的高端点（不包括）
     * @return 此列表中指定范围的视图
     * @throws IndexOutOfBoundsException 如果端点索引值超出范围
     *         {@code (fromIndex < 0 || toIndex > size)}
     * @throws IllegalArgumentException 如果端点索引顺序错误
     *         {@code (fromIndex > toIndex)}
     */
    public synchronized List<E> subList(int fromIndex, int toIndex) {
        return Collections.synchronizedList(super.subList(fromIndex, toIndex),
                                            this);
    }

    /**
     * 从该列表中移除所有索引在 {@code fromIndex}（包含）和 {@code toIndex}（不包含）之间的元素。
     * 将任何后续元素向左移动（减少它们的索引）。
     * 此调用将列表缩短 {@code (toIndex - fromIndex)} 个元素。
     * （如果 {@code toIndex==fromIndex}，此操作无效。）
     */
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = elementCount - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                         numMoved);

        // 让gc完成它的工作
        int newElementCount = elementCount - (toIndex-fromIndex);
        while (elementCount != newElementCount)
            elementData[--elementCount] = null;
    }

    /**
     * 将 {@code Vector} 实例的状态保存到流中（即，序列化它）。
     * 此方法执行同步以确保序列化数据的一致性。
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        final java.io.ObjectOutputStream.PutField fields = s.putFields();
        final Object[] data;
        synchronized (this) {
            fields.put("capacityIncrement", capacityIncrement);
            fields.put("elementCount", elementCount);
            data = elementData.clone();
        }
        fields.put("elementData", data);
        s.writeFields();
    }

    /**
     * 返回一个列表迭代器，用于遍历此列表中的元素（按正确顺序），从列表中的指定位置开始。
     * 指定的索引表示初始调用 {@link ListIterator#next next} 将返回的第一个元素。
     * 初始调用 {@link ListIterator#previous previous} 将返回指定索引减一的元素。
     *
     * <p>返回的列表迭代器是 <a href="#fail-fast"><i>fail-fast</i></a> 的。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized ListIterator<E> listIterator(int index) {
        if (index < 0 || index > elementCount)
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
    public synchronized ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     * 返回一个按正确顺序遍历此列表中元素的迭代器。
     *
     * <p>返回的迭代器是<a href="#fail-fast"><i>快速失败</i></a>的。
     *
     * @return 一个按正确顺序遍历此列表中元素的迭代器
     */
    public synchronized java.util.Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * AbstractList.Itr 的优化版本
     */
    private class Itr implements Iterator<E> {
        int cursor;       // 下一个要返回的元素的索引
        int lastRet = -1; // 返回的最后一个元素的索引；如果没有则返回-1
        int expectedModCount = modCount;

        public boolean hasNext() {
            // 虽然有些大胆，但在规范范围内，因为修改已被检查
            // 在同步内部或之后，在下一个/上一个
            return cursor != elementCount;
        }

        public E next() {
            synchronized (Vector.this) {
                checkForComodification();
                int i = cursor;
                if (i >= elementCount)
                    throw new NoSuchElementException();
                cursor = i + 1;
                return elementData(lastRet = i);
            }
        }

        public void remove() {
            if (lastRet == -1)
                throw new IllegalStateException();
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.remove(lastRet);
                expectedModCount = modCount;
            }
            cursor = lastRet;
            lastRet = -1;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            synchronized (Vector.this) {
                final int size = elementCount;
                int i = cursor;
                if (i >= size) {
                    return;
                }
        @SuppressWarnings("unchecked")
                final E[] elementData = (E[]) Vector.this.elementData;
                if (i >= elementData.length) {
                    throw new ConcurrentModificationException();
                }
                while (i != size && modCount == expectedModCount) {
                    action.accept(elementData[i++]);
                }
                // 在迭代结束时更新一次以减少堆写入流量
                cursor = i;
                lastRet = i - 1;
                checkForComodification();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * AbstractList.ListItr 的优化版本
     */
    final class ListItr extends Itr implements ListIterator<E> {
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

        public E previous() {
            synchronized (Vector.this) {
                checkForComodification();
                int i = cursor - 1;
                if (i < 0)
                    throw new NoSuchElementException();
                cursor = i;
                return elementData(lastRet = i);
            }
        }

        public void set(E e) {
            if (lastRet == -1)
                throw new IllegalStateException();
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.set(lastRet, e);
            }
        }

        public void add(E e) {
            int i = cursor;
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.add(i, e);
                expectedModCount = modCount;
            }
            cursor = i + 1;
            lastRet = -1;
        }
    }

    @Override
    public synchronized void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int elementCount = this.elementCount;
        for (int i=0; modCount == expectedModCount && i < elementCount; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // 找出需要移除的元素
        // 在此阶段从过滤器谓词抛出的任何异常
        // 将保持集合不变
        int removeCount = 0;
        final int size = elementCount;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
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

        // 将剩余元素左移以填补被移除元素留下的空位
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
            elementCount = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = elementCount;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, elementCount, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    /**
     * 创建一个<em><a href="Spliterator.html#binding">延迟绑定</a></em>
     * 且<em>快速失败</em>的 {@link Spliterator}，用于遍历此列表中的元素。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED}、
     * {@link Spliterator#SUBSIZED} 和 {@link Spliterator#ORDERED}。
     * 重写实现应记录额外特征值的报告。
     *
     * @return 一个 {@code Spliterator}，用于遍历此列表中的元素
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new VectorSpliterator<>(this, null, 0, -1, 0);
    }

    /** 类似于 ArrayList 的 Spliterator */
    static final class VectorSpliterator<E> implements Spliterator<E> {
        private final Vector<E> list;
        private Object[] array;
        private int index; // 当前索引，在前进/分割时修改
        private int fence; // -1 直到使用；然后最后一个索引的下一个
        private int expectedModCount; // 在设置围栏时初始化

        /** 创建覆盖给定范围的新spliterator */
        VectorSpliterator(Vector<E> list, Object[] array, int origin, int fence,
                          int expectedModCount) {
            this.list = list;
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // 在首次使用时初始化
            int hi;
            if ((hi = fence) < 0) {
                synchronized(list) {
                    array = list.elementData;
                    expectedModCount = list.modCount;
                    hi = fence = list.elementCount;
                }
            }
            return hi;
        }

        public Spliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                new VectorSpliterator<E>(list, array, lo, index = mid,
                                         expectedModCount);
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            int i;
            if (action == null)
                throw new NullPointerException();
            if (getFence() > (i = index)) {
                index = i + 1;
                action.accept((E)array[i]);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi; // 将访问和检查从循环中提升
            Vector<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null) {
                if ((hi = fence) < 0) {
                    synchronized(lst) {
                        expectedModCount = lst.modCount;
                        a = array = lst.elementData;
                        hi = fence = lst.elementCount;
                    }
                }
                else
                    a = array;
                if (a != null && (i = index) >= 0 && (index = hi) <= a.length) {
                    while (i < hi)
                        action.accept((E) a[i++]);
                    if (lst.modCount == expectedModCount)
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
}
