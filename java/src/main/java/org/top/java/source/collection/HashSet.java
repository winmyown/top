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

import java.io.InvalidObjectException;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.*;

/**
 * 该类实现了<tt>Set</tt>接口，由一个哈希表（实际上是一个<tt>HashMap</tt>实例）支持。它不保证集合的迭代顺序；特别是，它不保证顺序会随着时间的推移保持不变。该类允许<tt>null</tt>元素。
 *
 * <p>该类为基本操作（<tt>add</tt>、<tt>remove</tt>、<tt>contains</tt>和<tt>size</tt>）提供了恒定的时间性能，假设哈希函数将元素正确地分散在桶中。迭代此集合所需的时间与<tt>HashSet</tt>实例的大小（元素的数量）加上支持<tt>HashMap</tt>实例的“容量”（桶的数量）的总和成正比。因此，如果迭代性能很重要，则不要将初始容量设置得太高（或负载因子太低）。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>
 * 如果多个线程同时访问一个哈希集，并且至少有一个线程修改了该集合，则必须在外部进行同步。这通常通过同步某些自然封装集合的对象来实现。
 *
 * 如果不存在这样的对象，则应使用{@link Collections#synchronizedSet Collections.synchronizedSet}方法“包装”该集合。最好在创建时完成此操作，以防止意外的非同步访问集合：<pre>
 *   Set s = Collections.synchronizedSet(new HashSet(...));</pre>
 *
 * <p>该类<tt>iterator</tt>方法返回的迭代器是<i>快速失败</i>的：如果在创建迭代器后的任何时间修改了集合，除非通过迭代器自己的<tt>remove</tt>方法，否则迭代器将抛出{@link ConcurrentModificationException}。因此，面对并发修改，迭代器会快速而干净地失败，而不是在未来的不确定时间冒任意、非确定性行为的风险。
 *
 * <p>请注意，迭代器的快速失败行为无法保证，因为一般来说，在存在非同步并发修改的情况下，无法做出任何硬性保证。快速失败迭代器会尽最大努力抛出<tt>ConcurrentModificationException</tt>。因此，编写依赖于此异常来保证其正确性的程序是错误的：<i>迭代器的快速失败行为应仅用于检测错误。</i>
 *
 * <p>该类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @param <E> 此集合维护的元素类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     java.util.Collection
 * @see     java.util.Set
 * @see     TreeSet
 * @see     java.util.HashMap
 * @since   1.2
 */

public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable
{
    static final long serialVersionUID = -5024744406713321676L;

    private transient HashMap<E,Object> map;

    // 与后备Map中的对象关联的虚拟值
    private static final Object PRESENT = new Object();

    /**
     * 构造一个新的空集合；其背后的<tt>HashMap</tt>实例具有默认的初始容量（16）和加载因子（0.75）。
     */
    public HashSet() {
        map = new HashMap<>();
    }

    /**
     * 构造一个包含指定集合中元素的新集合。使用默认负载因子（0.75）和足以包含指定集合中元素的初始容量创建<tt>HashMap</tt>。
     *
     * @param c 要将其元素放入此集合的集合
     * @throws NullPointerException 如果指定的集合为null
     */
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    /**
     * 构造一个新的空集合；其支持的<tt>HashMap</tt>实例具有指定的初始容量和指定的负载因子。
     *
     * @param      initialCapacity   哈希映射的初始容量
     * @param      loadFactor        哈希映射的负载因子
     * @throws     IllegalArgumentException 如果初始容量小于零，或者负载因子为非正数
     */
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 构造一个新的空集合；其支持的<tt>HashMap</tt>实例具有指定的初始容量和默认的加载因子（0.75）。
     *
     * @param      initialCapacity   哈希表的初始容量
     * @throws     IllegalArgumentException 如果初始容量小于零
     */
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    /**
     * 构造一个新的、空的链接哈希集。（此包私有构造函数仅由LinkedHashSet使用。）支持
     * 的HashMap实例是一个具有指定初始容量和指定负载因子的LinkedHashMap。
     *
     * @param      initialCapacity   哈希表的初始容量
     * @param      loadFactor        哈希表的负载因子
     * @param      dummy             忽略（用于区分此构造函数与其他int, float构造函数。）
     * @throws     IllegalArgumentException 如果初始容量小于零，或者负载因子为非正数
     */
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 返回一个遍历此集合中元素的迭代器。元素
     * 以无特定顺序返回。
     *
     * @return 一个遍历此集合中元素的迭代器
     * @see ConcurrentModificationException
     */
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * 返回此集合中的元素数量（其基数）。
     *
     * @return 此集合中的元素数量（其基数）
     */
    public int size() {
        return map.size();
    }

    /**
     * 如果此集合不包含任何元素，则返回 <tt>true</tt>。
     *
     * @return 如果此集合不包含任何元素，则返回 <tt>true</tt>
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 如果此集合包含指定元素，则返回 <tt>true</tt>。
     * 更正式地说，当且仅当此集合包含一个元素 <tt>e</tt> 使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 <tt>true</tt>。
     *
     * @param o 要测试是否在此集合中的元素
     * @return <tt>true</tt> 如果此集合包含指定元素
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * 如果指定的元素尚未存在，则将其添加到此集合中。
     * 更正式地说，如果此集合不包含任何元素 <tt>e2</tt>，使得
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>，
     * 则将指定的元素 <tt>e</tt> 添加到此集合中。
     * 如果此集合已经包含该元素，则调用不会更改集合并返回 <tt>false</tt>。
     *
     * @param e 要添加到此集合中的元素
     * @return <tt>true</tt> 如果此集合之前不包含指定的元素
     */
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    /**
     * 如果指定元素存在于此集合中，则将其移除。
     * 更正式地说，移除一个元素 <tt>e</tt>，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>，
     * 如果此集合包含这样的元素。如果此集合包含该元素（或者等价地，如果此集合因调用而改变），则返回 <tt>true</tt>。
     * （在调用返回后，此集合将不再包含该元素。）
     *
     * @param o 要从此集合中移除的对象（如果存在）
     * @return <tt>true</tt> 如果集合包含指定元素
     */
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    /**
     * 移除该集合中的所有元素。
     * 此调用返回后，集合将为空。
     */
    public void clear() {
        map.clear();
    }

    /**
     * 返回此 <tt>HashSet</tt> 实例的浅拷贝：元素本身不会被克隆。
     *
     * @return 此集合的浅拷贝
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            HashSet<E> newSet = (HashSet<E>) super.clone();
            newSet.map = (HashMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * 将此 <tt>HashSet</tt> 实例的状态保存到流中（即序列化它）。
     *
     * @serialData 支持此 <tt>HashSet</tt> 的 <tt>HashMap</tt> 实例的容量
     *             (int)，及其负载因子 (float) 会被发出，接着是集合的大小（它包含的元素数量）
     *             (int)，然后是其所有元素（每个元素为 Object），顺序不限。
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // 写出任何隐藏的序列化魔法
        s.defaultWriteObject();

        // 写出HashMap的容量和负载因子
        s.writeInt(map.capacity());
        s.writeFloat(map.loadFactor());

        // 写出大小
        s.writeInt(map.size());

        // 按正确的顺序写出所有元素。
        for (E e : map.keySet())
            s.writeObject(e);
    }

    /**
     * 从流中重建<tt>HashSet</tt>实例（即反序列化它）。
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // 读取任何隐藏的序列化魔法
        s.defaultReadObject();

        // 读取容量并验证非负。
        int capacity = s.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Illegal capacity: " +
                                             capacity);
        }

        // 读取负载因子并验证其为正数且非NaN。
        float loadFactor = s.readFloat();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        }

        // 读取大小并验证非负。
        int size = s.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Illegal size: " +
                                             size);
        }

        // 根据大小和负载因子设置容量，确保
        // HashMap 至少填充了 25%，但限制在最大容量。
        capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
                HashMap.MAXIMUM_CAPACITY);

        // 创建支持HashMap
        map = (((HashSet<?>)this) instanceof LinkedHashSet ?
               new LinkedHashMap<E,Object>(capacity, loadFactor) :
               new HashMap<E,Object>(capacity, loadFactor));

        // 以正确的顺序读取所有元素。
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
                E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

    /**
     * 创建一个对此集合中元素的<em><a href="Spliterator.html#binding">延迟绑定</a></em>
     * 且<em>快速失败</em>的 {@link Spliterator}。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED} 和
     * {@link Spliterator#DISTINCT}。覆盖实现应记录额外特征值的报告。
     *
     * @return 一个对此集合中元素的 {@code Spliterator}
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new HashMap.KeySpliterator<E,Object>(map, 0, -1, 0, 0);
    }
}
