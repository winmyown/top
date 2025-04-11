/*
 * 版权所有 (c) 1998, 2013, Oracle 和/或其附属公司。保留所有权利。
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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.*;

/**
 * 基于 {@link java.util.TreeMap} 的 {@link NavigableSet} 实现。
 * 元素的排序使用它们的 {@linkplain Comparable 自然顺序}，
 * 或者在集合创建时提供的 {@link Comparator}，具体取决于使用的构造函数。
 *
 * <p>此实现为基本操作（{@code add}、{@code remove} 和 {@code contains}）
 * 提供了保证的 log(n) 时间复杂度。
 *
 * <p>请注意，集合维护的顺序（无论是否提供了显式比较器）必须与 equals 一致，
 * 以正确实现 {@code Set} 接口。（有关“与 equals 一致”的准确定义，请参阅
 * {@code Comparable} 或 {@code Comparator}。）这是因为 {@code Set} 接口
 * 是根据 {@code equals} 操作定义的，但 {@code TreeSet} 实例使用其
 * {@code compareTo}（或 {@code compare}）方法执行所有元素比较，因此从集合的
 * 角度来看，通过此方法被认为相等的两个元素是相等的。即使集合的顺序与 equals 不一致，
 * 其行为也是明确定义的；它只是未能遵守 {@code Set} 接口的一般约定。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>
 * 如果多个线程同时访问一个树集，并且至少有一个线程修改了集合，则必须在外部进行同步。
 * 这通常通过同步某个自然封装集合的对象来实现。如果不存在这样的对象，则应使用
 * {@link Collections#synchronizedSortedSet Collections.synchronizedSortedSet}
 * 方法“包装”集合。最好在创建时完成此操作，以防止意外的非同步访问集合：<pre>
 *   SortedSet s = Collections.synchronizedSortedSet(new TreeSet(...));</pre>
 *
 * <p>此类返回的迭代器是<i>快速失败</i>的：如果在创建迭代器之后的任何时候修改了集合，
 * 除非通过迭代器自身的 {@code remove} 方法，否则迭代器将抛出
 * {@link ConcurrentModificationException}。因此，在面对并发修改时，迭代器会快速
 * 而干净地失败，而不是在未来不确定的时间冒任意、非确定性行为的风险。
 *
 * <p>请注意，无法保证迭代器的快速失败行为，因为一般来说，在存在非同步并发修改的情况下，
 * 不可能做出任何硬性保证。快速失败迭代器会尽最大努力抛出
 * {@code ConcurrentModificationException}。因此，编写依赖于此异常的程序的正确性是错误的：
 * <i>迭代器的快速失败行为应仅用于检测错误。</i>
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a>的成员。
 *
 * @param <E> 此集合维护的元素类型
 *
 * @author  Josh Bloch
 * @see     java.util.Collection
 * @see     Set
 * @see     HashSet
 * @see     Comparable
 * @see     Comparator
 * @see     java.util.TreeMap
 * @since   1.2
 */

public class TreeSet<E> extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable
{
    /**
     * 后备映射。
     */
    private transient java.util.NavigableMap<E,Object> m;

    // 与后备Map中的对象关联的虚拟值
    private static final Object PRESENT = new Object();

    /**
     * 构造一个由指定可导航映射支持的集合。
     */
    TreeSet(NavigableMap<E,Object> m) {
        this.m = m;
    }

    /**
     * 构造一个新的、空的树集，根据元素的自然顺序进行排序。所有插入到该集合中的元素都必须实现 {@link Comparable} 接口。
     * 此外，所有此类元素必须是<i>相互可比较的</i>：对于集合中的任何元素 {@code e1} 和 {@code e2}，{@code e1.compareTo(e2)} 不得抛出 {@code ClassCastException}。
     * 如果用户尝试向集合中添加违反此约束的元素（例如，用户尝试向元素为整数的集合中添加字符串元素），则 {@code add} 调用将抛出 {@code ClassCastException}。
     */
    public TreeSet() {
        this(new java.util.TreeMap<E,Object>());
    }

    /**
     * 构造一个新的、空的树集，根据指定的比较器进行排序。插入集合的所有元素必须通过指定的比较器相互可比：
     * {@code comparator.compare(e1, e2)} 对于集合中的任何元素 {@code e1} 和 {@code e2}，
     * 不得抛出 {@code ClassCastException}。如果用户尝试向集合中添加违反此约束的元素，
     * {@code add} 调用将抛出 {@code ClassCastException}。
     *
     * @param comparator 用于对此集合进行排序的比较器。如果为 {@code null}，则将使用元素的
     *        {@linkplain Comparable 自然顺序}。
     */
    public TreeSet(Comparator<? super E> comparator) {
        this(new java.util.TreeMap<>(comparator));
    }

    /**
     * 构造一个新的树集，包含指定集合中的元素，并根据元素的<i>自然顺序</i>进行排序。所有插入到集合中的元素都必须实现
     * {@link Comparable} 接口。此外，所有这些元素必须是<i>相互可比较的</i>：对于集合中的任何元素 {@code e1} 和
     * {@code e2}，{@code e1.compareTo(e2)} 不得抛出 {@code ClassCastException}。
     *
     * @param c 其元素将构成新集合的集合
     * @throws ClassCastException 如果 {@code c} 中的元素不是 {@link Comparable}，或者不是相互可比较的
     * @throws NullPointerException 如果指定的集合为 null
     */
    public TreeSet(java.util.Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * 构造一个新的树集，包含与指定排序集相同的元素，
     * 并使用相同的排序方式。
     *
     * @param s 排序集，其元素将组成新集
     * @throws NullPointerException 如果指定的排序集为null
     */
    public TreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }

    /**
     * 返回一个按升序遍历此集合中元素的迭代器。
     *
     * @return 一个按升序遍历此集合中元素的迭代器
     */
    public java.util.Iterator<E> iterator() {
        return m.navigableKeySet().iterator();
    }

    /**
     * 返回一个按降序遍历此集合中元素的迭代器。
     *
     * @return 一个按降序遍历此集合中元素的迭代器
     * @since 1.6
     */
    public Iterator<E> descendingIterator() {
        return m.descendingKeySet().iterator();
    }

    /**
     * @since 1.6
     */
    public NavigableSet<E> descendingSet() {
        return new TreeSet<>(m.descendingMap());
    }

    /**
     * 返回此集合中的元素数量（其基数）。
     *
     * @return 此集合中的元素数量（其基数）
     */
    public int size() {
        return m.size();
    }

    /**
     * 如果此集合不包含任何元素，则返回 {@code true}。
     *
     * @return {@code true} 如果此集合不包含任何元素
     */
    public boolean isEmpty() {
        return m.isEmpty();
    }

    /**
     * 如果此集合包含指定元素，则返回 {@code true}。
     * 更正式地说，当且仅当此集合包含一个元素 {@code e} 使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 {@code true}。
     *
     * @param o 要检查是否包含在此集合中的对象
     * @return {@code true} 如果此集合包含指定元素
     * @throws ClassCastException 如果指定对象无法与当前集合中的元素进行比较
     * @throws NullPointerException 如果指定元素为 null 且此集合使用自然排序，
     *         或其比较器不允许 null 元素
     */
    public boolean contains(Object o) {
        return m.containsKey(o);
    }

    /**
     * 如果指定的元素尚未存在，则将其添加到此集合中。
     * 更正式地说，将指定的元素 {@code e} 添加到此集合中，如果
     * 集合中不包含元素 {@code e2}，使得
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>。
     * 如果此集合已包含该元素，则调用将保持集合不变并返回 {@code false}。
     *
     * @param e 要添加到此集合中的元素
     * @return {@code true} 如果此集合尚未包含指定的元素
     * @throws ClassCastException 如果指定的对象无法与当前集合中的元素进行比较
     * @throws NullPointerException 如果指定的元素为 null
     *         并且此集合使用自然排序，或者其比较器不允许 null 元素
     */
    public boolean add(E e) {
        return m.put(e, PRESENT)==null;
    }

    /**
     * 如果存在指定的元素，则从该集合中移除该元素。
     * 更正式地说，移除一个元素 {@code e}，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>，
     * 如果该集合包含这样的元素。如果该集合包含该元素（或者等价地，如果该集合
     * 由于此调用而改变），则返回 {@code true}。（一旦调用返回，该集合将不再包含该元素。）
     *
     * @param o 要从该集合中移除的对象（如果存在）
     * @return {@code true} 如果该集合包含指定的元素
     * @throws ClassCastException 如果指定的对象无法与当前集合中的元素进行比较
     * @throws NullPointerException 如果指定的元素为 null，并且该集合使用自然排序，或者其比较器不允许 null 元素
     */
    public boolean remove(Object o) {
        return m.remove(o)==PRESENT;
    }

    /**
     * 移除该集合中的所有元素。
     * 调用此方法后，集合将为空。
     */
    public void clear() {
        m.clear();
    }

    /**
     * 将指定集合中的所有元素添加到此集合中。
     *
     * @param c 包含要添加到此集合中的元素的集合
     * @return 如果此集合因调用而更改，则返回 {@code true}
     * @throws ClassCastException 如果提供的元素无法与当前集合中的元素进行比较
     * @throws NullPointerException 如果指定的集合为 null，或者
     *         任何元素为 null 且此集合使用自然排序，或者
     *         其比较器不允许 null 元素
     */
    public  boolean addAll(Collection<? extends E> c) {
        // 如果适用，使用线性时间版本
        if (m.size()==0 && c.size() > 0 &&
            c instanceof SortedSet &&
            m instanceof TreeMap) {
            SortedSet<? extends E> set = (SortedSet<? extends E>) c;
            TreeMap<E,Object> map = (TreeMap<E, Object>) m;
            Comparator<?> cc = set.comparator();
            Comparator<? super E> mc = map.comparator();
            if (cc==mc || (cc != null && cc.equals(mc))) {
                map.addAllForTreeSet(set, PRESENT);
                return true;
            }
        }
        return super.addAll(c);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromElement} 或 {@code toElement}
     *         为 null 并且此集合使用自然排序，或其比较器
     *         不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                  E toElement,   boolean toInclusive) {
        return new TreeSet<>(m.subMap(fromElement, fromInclusive,
                                       toElement,   toInclusive));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code toElement} 为 null 且
     *         此集合使用自然排序，或其比较器不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new TreeSet<>(m.headMap(toElement, inclusive));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromElement} 为 null 且
     *         此集合使用自然排序，或其比较器不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new TreeSet<>(m.tailMap(fromElement, inclusive));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromElement} 或
     *         {@code toElement} 为 null 并且此集合使用自然排序，
     *         或其比较器不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code toElement} 为 null
     *         并且此集合使用自然排序，或其比较器不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromElement} 为 null
     *         且此集合使用自然排序，或其比较器不允许 null 元素
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    public Comparator<? super E> comparator() {
        return m.comparator();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E first() {
        return m.firstKey();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E last() {
        return m.lastKey();
    }

    // NavigableSet API methods

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定元素为 null
     *         并且此集合使用自然排序，或其比较器
     *         不允许 null 元素
     * @since 1.6
     */
    public E lower(E e) {
        return m.lowerKey(e);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定元素为 null
     *         并且此集合使用自然排序，或其比较器
     *         不允许 null 元素
     * @since 1.6
     */
    public E floor(E e) {
        return m.floorKey(e);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定元素为 null
     *         并且此集合使用自然排序，或其比较器
     *         不允许 null 元素
     * @since 1.6
     */
    public E ceiling(E e) {
        return m.ceilingKey(e);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定元素为 null
     *         并且此集合使用自然排序，或其比较器
     *         不允许 null 元素
     * @since 1.6
     */
    public E higher(E e) {
        return m.higherKey(e);
    }

    /**
     * @since 1.6
     */
    public E pollFirst() {
        java.util.Map.Entry<E,?> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }

    /**
     * @since 1.6
     */
    public E pollLast() {
        Map.Entry<E,?> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }

    /**
     * 返回此 {@code TreeSet} 实例的浅拷贝。（元素本身不会被克隆。）
     *
     * @return 此集合的浅拷贝
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        TreeSet<E> clone;
        try {
            clone = (TreeSet<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        clone.m = new java.util.TreeMap<>(m);
        return clone;
    }

    /**
     * 将 {@code TreeSet} 实例的状态保存到流中（即，序列化它）。
     *
     * @serialData 发出用于排序此集合的比较器，或者
     *             如果它遵循其元素的自然排序，则发出 {@code null}
     *             (Object)，后跟集合的大小（它包含的
     *             元素数量）(int)，然后是它的所有元素（每个元素都是
     *             Object）按顺序排列（由集合的 Comparator 确定，
     *             如果集合没有 Comparator，则由元素的自然排序确定）。
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // 写出任何隐藏的内容
        s.defaultWriteObject();

        // 写出Comparator
        s.writeObject(m.comparator());

        // 写出大小
        s.writeInt(m.size());

        // 按正确顺序写出所有元素。
        for (E e : m.keySet())
            s.writeObject(e);
    }

    /**
     * 从流中重构 {@code TreeSet} 实例（即反序列化它）。
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // 读取任何隐藏的内容
        s.defaultReadObject();

        // 读取比较器
        @SuppressWarnings("unchecked")
            Comparator<? super E> c = (Comparator<? super E>) s.readObject();

        // 创建支持TreeMap
        TreeMap<E,Object> tm = new TreeMap<>(c);
        m = tm;

        // 读取大小
        int size = s.readInt();

        tm.readTreeSet(size, s, PRESENT);
    }

    /**
     * 创建一个<em><a href="Spliterator.html#binding">延迟绑定</a></em>
     * 且<em>快速失败</em>的 {@link Spliterator}，用于遍历此集合中的元素。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED}、
     * {@link Spliterator#DISTINCT}、{@link Spliterator#SORTED} 和
     * {@link Spliterator#ORDERED}。覆盖实现应记录额外特性值的报告。
     *
     * <p>如果集合的比较器（参见 {@link #comparator()}）为 {@code null}，
     * 则 spliterator 的比较器（参见 {@link Spliterator#getComparator()}）为 {@code null}。
     * 否则，spliterator 的比较器与集合的比较器相同或施加相同的总排序。
     *
     * @return 一个 {@code Spliterator}，用于遍历此集合中的元素
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return TreeMap.keySpliteratorFor(m);
    }

    private static final long serialVersionUID = -2479143000061671589L;
}
