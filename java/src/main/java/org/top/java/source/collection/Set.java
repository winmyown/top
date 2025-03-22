package org.top.java.source.collection;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Iterator;
/**
 * 一个不包含重复元素的集合。更正式地说,集合不包含使得 <code>e1.equals(e2)</code> 为真的元素对
 * <code>e1</code> 和 <code>e2</code>,并且最多包含一个 null 元素。正如其名称所暗示的那样,
 * 该接口模拟了数学中的<i>集合</i>抽象概念。
 *
 * <p><tt>Set</tt> 接口在从 <tt>Collection</tt> 接口继承的契约基础上,对所有的构造函数以及
 * <tt>add</tt>、<tt>equals</tt> 和 <tt>hashCode</tt> 方法的契约增加了额外的规定。为了方便起见,
 * 其他继承方法的声明也包括在此。(这些声明的规范已针对 <tt>Set</tt> 接口进行了调整,但它们
 * 不包含任何额外的规定。)
 *
 * <p>对构造函数的额外规定是,所有的构造函数必须创建一个不包含重复元素(如上所述)的集合。
 *
 * <p>注意:如果可变对象被用作集合元素,必须非常小心。如果一个对象的值在其作为集合元素的
 * 过程中发生了变化,且这种变化影响了 <tt>equals</tt> 比较的结果,那么该集合的行为是未定义的。
 * 这种情况的一个特例是不允许集合将自身作为元素包含。
 *
 * <p>某些集合实现对它们可以包含的元素有限制。例如,一些实现禁止 null 元素,而另一些实现则对
 * 元素的类型有限制。尝试添加不符合条件的元素会抛出未经检查的异常,通常是
 * <tt>NullPointerException</tt> 或 <tt>ClassCastException</tt>。尝试查询不符合条件的元素的
 * 存在性可能会抛出异常,或者可能直接返回 false;一些实现会表现出前者的行为,而另一些实现则
 * 会表现出后者的行为。更一般地说,尝试对不符合条件的元素执行一个操作,如果该操作的完成不会
 * 导致将该不符合条件的元素插入集合中,那么该操作可能会抛出异常,也可能会成功,这取决于实现的选择。
 * 此类异常在本接口的规范中标记为“可选”。
 *
 * <p>本接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a>的成员。
 *
 * @param <E> 此集合维护的元素类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Collection
 * @see List
 * @see SortedSet
 * @see HashSet
 * @see TreeSet
 * @see AbstractSet
 * @see Collections#singleton(java.lang.Object)
 * @see Collections#EMPTY_SET
 * @since 1.2
 */
public interface Set<E> extends Collection<E> {
    // 查询操作

    /**
     * 返回此集合中的元素数量（其基数）。如果此集合包含多于 <tt>Integer.MAX_VALUE</tt> 个元素，则返回 <tt>Integer.MAX_VALUE</tt>。
     *
     * @return 此集合中的元素数量（其基数）
     */
    int size();

    /**
     * 如果此集合不包含任何元素，则返回 <tt>true</tt>。
     *
     * @return 如果此集合不包含任何元素，则返回 <tt>true</tt>
     */
    boolean isEmpty();

    /**
     * 如果此集合包含指定的元素，则返回 <tt>true</tt>。更正式地说，当且仅当此集合包含一个元素 <tt>e</tt> 使得 <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 <tt>true</tt>。
     *
     * @param o 要测试是否在此集合中的元素
     * @return 如果此集合包含指定的元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean contains(Object o);

    /**
     * 返回此集合中元素的迭代器。元素以任意顺序返回（除非此集合是某个类的实例，该类提供了顺序保证）。
     *
     * @return 此集合中元素的迭代器
     */
    Iterator<E> iterator();

    /**
     * 返回包含此集合中所有元素的数组。如果此集合对其迭代器返回元素的顺序有任何保证，则此方法必须以相同的顺序返回元素。
     *
     * <p>返回的数组将是“安全的”，因为此集合不会维护对它的引用。（换句话说，即使此集合由数组支持，此方法也必须分配一个新数组）。调用者可以自由修改返回的数组。
     *
     * <p>此方法充当基于数组和基于集合的 API 之间的桥梁。
     *
     * @return 包含此集合中所有元素的数组
     */
    Object[] toArray();

    /**
     * 返回包含此集合中所有元素的数组；返回数组的运行时类型是指定数组的类型。如果集合适合指定的数组，则在其中返回。否则，将分配一个具有指定数组的运行时类型和此集合大小的新数组。
     *
     * <p>如果此集合适合指定的数组并有剩余空间（即数组的元素比此集合多），则紧接集合末尾的数组元素设置为 <tt>null</tt>。（这仅在调用者知道此集合不包含任何 null 元素时有用，以确定此集合的长度。）
     *
     * <p>如果此集合对其迭代器返回元素的顺序有任何保证，则此方法必须以相同的顺序返回元素。
     *
     * <p>与 {@link #toArray()} 方法类似，此方法充当基于数组和基于集合的 API 之间的桥梁。此外，此方法允许精确控制输出数组的运行时类型，并且在某些情况下可以用于节省分配成本。
     *
     * <p>假设 <tt>x</tt> 是一个仅包含字符串的集合。以下代码可用于将集合转储到新分配的 <tt>String</tt> 数组中：
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * 注意，<tt>toArray(new Object[0])</tt> 在功能上与 <tt>toArray()</tt> 相同。
     *
     * @param a 存储此集合元素的数组，如果它足够大；否则，将为此目的分配一个具有相同运行时类型的新数组。
     * @return 包含此集合中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此集合中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果指定数组为 null
     */
    <T> T[] toArray(T[] a);


    // 修改操作

    /**
     * 如果指定的元素尚未存在，则将其添加到此集合中（可选操作）。更正式地说，如果集合中不包含元素 <tt>e2</tt> 使得 <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>，则将指定的元素 <tt>e</tt> 添加到此集合中。如果此集合已包含该元素，则调用不会更改集合并返回 <tt>false</tt>。结合构造函数的限制，这确保了集合永远不会包含重复元素。
     *
     * <p>上述规定并不意味着集合必须接受所有元素；集合可以拒绝添加任何特定元素，包括 <tt>null</tt>，并抛出异常，如 {@link Collection#add Collection.add} 规范中所述。各个集合实现应清楚地记录它们可能包含的元素的任何限制。
     *
     * @param e 要添加到此集合的元素
     * @return 如果此集合尚未包含指定的元素，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>add</tt> 操作
     * @throws ClassCastException 如果指定元素的类阻止其添加到此集合
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其添加到此集合
     */
    boolean add(E e);


    /**
     * 如果存在指定的元素，则从此集合中移除它（可选操作）。更正式地说，移除一个元素 <tt>e</tt> 使得 <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>，如果此集合包含这样的元素。如果此集合包含该元素，则返回 <tt>true</tt>（或等效地，如果此集合因调用而更改）。（一旦调用返回，此集合将不再包含该元素。）
     *
     * @param o 要从此集合中移除的对象（如果存在）
     * @return 如果此集合包含指定的元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>remove</tt> 操作
     */
    boolean remove(Object o);


    // 批量操作

    /**
     * 如果此集合包含指定集合中的所有元素，则返回 <tt>true</tt>。如果指定的集合也是一个集合，则当它是此集合的<i>子集</i>时，此方法返回 <tt>true</tt>。
     *
     * @param  c 要检查是否包含在此集合中的集合
     * @return 如果此集合包含指定集合中的所有元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定集合中的一个或多个元素的类型与此集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定集合包含一个或多个 null 元素且此集合不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see    #contains(Object)
     */
    boolean containsAll(Collection<?> c);

    /**
     * 将指定集合中的所有元素添加到此集合中（如果它们尚未存在）（可选操作）。如果指定的集合也是一个集合，则 <tt>addAll</tt> 操作有效地修改此集合，使其值为两个集合的<i>并集</i>。如果在操作进行时修改了指定的集合，则此操作的行为是未定义的。
     *
     * @param  c 包含要添加到此集合中的元素的集合
     * @return 如果此集合因调用而更改，则返回 <tt>true</tt>
     *
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>addAll</tt> 操作
     * @throws ClassCastException 如果指定集合中某个元素的类阻止其添加到此集合
     * @throws NullPointerException 如果指定集合包含一个或多个 null 元素且此集合不允许 null 元素，或者如果指定集合为 null
     * @throws IllegalArgumentException 如果指定集合中某个元素的某些属性阻止其添加到此集合
     * @see #add(Object)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * 仅保留此集合中包含在指定集合中的元素（可选操作）。换句话说，从此集合中移除所有未包含在指定集合中的元素。如果指定的集合也是一个集合，则此操作有效地修改此集合，使其值为两个集合的<i>交集</i>。
     *
     * @param  c 包含要保留在此集合中的元素的集合
     * @return 如果此集合因调用而更改，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>retainAll</tt> 操作
     * @throws ClassCastException 如果此集合中某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此集合包含 null 元素且指定集合不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see #remove(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * 从此集合中移除所有包含在指定集合中的元素（可选操作）。如果指定的集合也是一个集合，则此操作有效地修改此集合，使其值为两个集合的<i>不对称集合差</i>。
     *
     * @param  c 包含要从此集合中移除的元素的集合
     * @return 如果此集合因调用而更改，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>removeAll</tt> 操作
     * @throws ClassCastException 如果此集合中某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此集合包含 null 元素且指定集合不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * 从此集合中移除所有元素（可选操作）。此调用返回后，集合将为空。
     *
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>clear</tt> 方法
     */
    void clear();


    // 比较和哈希

    /**
     * 将指定对象与此集合进行相等性比较。如果指定对象也是一个集合，两个集合具有相同的大小，并且指定集合的每个成员都包含在此集合中（或等效地，此集合的每个成员都包含在指定集合中），则返回 <tt>true</tt>。此定义确保 equals 方法在不同集合接口实现之间正常工作。
     *
     * @param o 要与此集合进行相等性比较的对象
     * @return 如果指定对象等于此集合，则返回 <tt>true</tt>
     */
    boolean equals(Object o);

    /**
     * 返回此集合的哈希码值。集合的哈希码定义为集合中元素的哈希码之和，其中 null 元素的哈希码定义为零。这确保 <tt>s1.equals(s2)</tt> 意味着 <tt>s1.hashCode()==s2.hashCode()</tt> 对于任何两个集合 <tt>s1</tt> 和 <tt>s2</tt>，如 {@link Object#hashCode} 的一般合同所要求的。
     *
     * @return 此集合的哈希码值
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    int hashCode();

    /**
     * 创建一个 {@code Spliterator} 来遍历此集合中的元素。
     *
     * <p>{@code Spliterator} 报告 {@link Spliterator#DISTINCT}。实现应记录其他特征值的报告。
     *
     * @implSpec
     * 默认实现从集合的 {@code Iterator} 创建一个
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em> 的 spliterator。spliterator 继承了集合迭代器的 <em>快速失败</em> 属性。
     * <p>
     * 创建的 {@code Spliterator} 还报告 {@link Spliterator#SIZED}。
     *
     * @implNote
     * 创建的 {@code Spliterator} 还报告 {@link Spliterator#SUBSIZED}。
     *
     * @return 一个 {@code Spliterator} 用于遍历此集合中的元素
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        // return Spliterators.spliterator(this, Spliterator.DISTINCT);
        return null;
    }
}
