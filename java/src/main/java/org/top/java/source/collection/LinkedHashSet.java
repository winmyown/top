/*
 * 版权所有 (c) 2000, 2013, Oracle 和/或其附属公司。保留所有权利。
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
import java.util.Set;
import java.util.*;

/**
 * <p>哈希表和链表实现的<tt>Set</tt>接口，具有可预测的迭代顺序。此实现与<tt>HashSet</tt>的不同之处在于，它维护了一个贯穿所有条目的双向链表。这个链表定义了迭代顺序，即元素插入到集合中的顺序（<i>插入顺序</i>）。请注意，如果元素<i>重新插入</i>到集合中，插入顺序<i>不会</i>受到影响。（如果<tt>s.add(e)</tt>在<tt>s.contains(e)</tt>返回<tt>true</tt>之前立即调用，则元素<tt>e</tt>被重新插入到集合<tt>s</tt>中。）
 *
 * <p>此实现使客户端免受{@link java.util.HashSet}提供的未指定的、通常混乱的排序，而不会产生与{@link TreeSet}相关的增加的成本。它可以用于生成一个与原始集合具有相同顺序的集合副本，而不管原始集合的实现如何：
 * <pre>
 *     void foo(Set s) {
 *         Set copy = new LinkedHashSet(s);
 *         ...
 *     }
 * </pre>
 * 如果一个模块接收一个集合作为输入，复制它，然后返回由该副本的顺序确定的结果，这种技术特别有用。（客户端通常希望以它们呈现的顺序返回结果。）
 *
 * <p>此类提供了所有可选的<tt>Set</tt>操作，并允许空元素。与<tt>HashSet</tt>一样，它为基本操作（<tt>add</tt>、<tt>contains</tt>和<tt>remove</tt>）提供了恒定时间的性能，假设哈希函数将元素正确地分散到桶中。由于维护链表的额外开销，性能可能略低于<tt>HashSet</tt>，但有一个例外：对<tt>LinkedHashSet</tt>的迭代需要与集合的<i>大小</i>成正比的时间，而与其容量无关。对<tt>HashSet</tt>的迭代可能更昂贵，需要与其<i>容量</i>成正比的时间。
 *
 * <p>链接哈希集有两个影响其性能的参数：<i>初始容量</i>和<i>负载因子</i>。它们的定义与<tt>HashSet</tt>完全相同。但请注意，选择过高的初始容量值对此类的惩罚比<tt>HashSet</tt>要轻，因为此类的迭代时间不受容量的影响。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>
 * 如果多个线程同时访问一个链接哈希集，并且至少有一个线程修改了该集合，则它<em>必须</em>在外部进行同步。这通常通过同步某些自然封装集合的对象来实现。
 *
 * 如果不存在这样的对象，则应使用{@link Collections#synchronizedSet Collections.synchronizedSet}方法“包装”该集合。最好在创建时完成此操作，以防止意外地不同步访问集合：<pre>
 *   Set s = Collections.synchronizedSet(new LinkedHashSet(...));</pre>
 *
 * <p>此类<tt>iterator</tt>方法返回的迭代器是<em>快速失败</em>的：如果在创建迭代器之后的任何时间修改了集合，除非通过迭代器自己的<tt>remove</tt>方法，否则迭代器将抛出{@link ConcurrentModificationException}。因此，在并发修改的情况下，迭代器会快速而干净地失败，而不是冒着在未来的不确定时间出现任意、非确定性行为的风险。
 *
 * <p>请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改的情况下，无法做出任何硬性保证。快速失败迭代器会尽最大努力抛出<tt>ConcurrentModificationException</tt>。因此，编写依赖此异常来保证程序正确性的程序是错误的：<i>迭代器的快速失败行为应仅用于检测错误。</i>
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @param <E> 此集合维护的元素类型
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     java.util.Collection
 * @see     java.util.Set
 * @see     java.util.HashSet
 * @see     TreeSet
 * @see     Hashtable
 * @since   1.4
 */

public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    /**
     * 构造一个新的、空的链接哈希集，具有指定的初始容量和负载因子。
     *
     * @param      initialCapacity 链接哈希集的初始容量
     * @param      loadFactor      链接哈希集的负载因子
     * @throws     IllegalArgumentException 如果初始容量小于零，或者负载因子为非正数
     */
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    /**
     * 构造一个新的、空的链接哈希集，具有指定的初始容量和默认负载因子（0.75）。
     *
     * @param   initialCapacity   链接哈希集的初始容量
     * @throws  IllegalArgumentException 如果初始容量小于零
     */
    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    /**
     * 构造一个新的、空的链接哈希集，具有默认的初始容量（16）和加载因子（0.75）。
     */
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    /**
     * 构造一个新的链接哈希集，包含指定集合中的相同元素。
     * 链接哈希集的初始容量足以容纳指定集合中的元素，并使用默认的加载因子（0.75）。
     *
     * @param c  要将其元素放入此集合的集合
     * @throws NullPointerException 如果指定的集合为null
     */
    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }

    /**
     * 创建一个<em><a href="Spliterator.html#binding">延迟绑定</a></em>
     * 并且<em>快速失败</em>的 {@code Spliterator}，用于遍历此集合中的元素。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED}、
     * {@link Spliterator#DISTINCT} 和 {@code ORDERED}。实现应记录其他特征值的报告。
     *
     * @implNote
     * 该实现从集合的 {@code Iterator} 创建一个
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em> 的 spliterator。
     * 该 spliterator 继承了集合迭代器的<em>快速失败</em>属性。
     * 创建的 {@code Spliterator} 还报告 {@link Spliterator#SUBSIZED}。
     *
     * @return 一个 {@code Spliterator}，用于遍历此集合中的元素
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
