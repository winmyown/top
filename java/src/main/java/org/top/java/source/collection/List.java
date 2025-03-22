/*
 * 版权所有 (c) 1997, 2014, Oracle 和/或其附属公司。保留所有权利。
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * 一个有序集合（也称为<i>序列</i>）。该接口的用户可以精确控制每个元素在列表中的插入位置。用户可以通过元素的整数索引（在列表中的位置）访问元素，并搜索列表中的元素。<p>
 *
 * 与集合不同，列表通常允许重复元素。更正式地说，列表通常允许元素对 <tt>e1</tt> 和 <tt>e2</tt> 满足 <tt>e1.equals(e2)</tt>，并且如果允许空元素，它们通常允许多个空元素。不难想象，有人可能希望实现一个禁止重复元素的列表，当用户尝试插入重复元素时抛出运行时异常，但我们预计这种用法很少见。<p>
 *
 * <tt>List</tt> 接口在 <tt>Collection</tt> 接口的基础上，对 <tt>iterator</tt>、<tt>add</tt>、<tt>remove</tt>、<tt>equals</tt> 和 <tt>hashCode</tt> 方法的契约提出了额外的规定。为了方便起见，这里还包含了其他继承方法的声明。<p>
 *
 * <tt>List</tt> 接口提供了四种用于按位置（索引）访问列表元素的方法。列表（如 Java 数组）是基于零的。请注意，对于某些实现（例如 <tt>LinkedList</tt> 类），这些操作可能需要与索引值成比例的时间。因此，如果调用者不知道实现，通常更倾向于迭代列表中的元素，而不是通过索引访问。<p>
 *
 * <tt>List</tt> 接口提供了一个特殊的迭代器，称为 <tt>ListIterator</tt>，它允许元素的插入和替换，以及双向访问，除了 <tt>Iterator</tt> 接口提供的正常操作外。还提供了一个方法，用于获取从列表中指定位置开始的列表迭代器。<p>
 *
 * <tt>List</tt> 接口提供了两种方法来搜索指定对象。从性能的角度来看，应谨慎使用这些方法。在许多实现中，它们将执行代价高昂的线性搜索。<p>
 *
 * <tt>List</tt> 接口提供了两种方法，用于在列表中的任意点高效地插入和删除多个元素。<p>
 *
 * 注意：虽然允许列表将自身作为元素包含在内，但强烈建议谨慎使用：<tt>equals</tt> 和 <tt>hashCode</tt> 方法在此类列表上不再有明确定义。
 *
 * <p>某些列表实现对其可能包含的元素有限制。例如，某些实现禁止空元素，而某些实现对其元素的类型有限制。尝试添加不合格的元素会抛出未检查的异常，通常是 <tt>NullPointerException</tt> 或 <tt>ClassCastException</tt>。尝试查询不合格元素的存在可能会抛出异常，或者可能只是返回 false；某些实现会表现出前一种行为，而某些实现会表现出后一种行为。更一般地说，尝试对不合格元素执行操作，如果操作完成不会导致将不合格元素插入列表，则可能会抛出异常或成功，具体取决于实现的选项。此类异常在接口规范中标记为“可选”。
 *
 * <p>此接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @param <E> 此列表中元素的类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see java.util.Collection
 * @see Set
 * @see ArrayList
 * @see LinkedList
 * @see Vector
 * @see Arrays#asList(Object[])
 * @see Collections#nCopies(int, Object)
 * @see Collections#EMPTY_LIST
 * @see AbstractList
 * @see AbstractSequentialList
 * @since 1.2
 */

public interface List<E> extends java.util.Collection<E> {
    // 查询操作

    /**
     * 返回此列表中的元素数量。如果此列表包含的元素多于 <tt>Integer.MAX_VALUE</tt>，则返回
     * <tt>Integer.MAX_VALUE</tt>。
     *
     * @return 此列表中的元素数量
     */
    int size();

    /**
     * 如果此列表不包含任何元素，则返回 <tt>true</tt>。
     *
     * @return 如果此列表不包含任何元素，则返回 <tt>true</tt>
     */
    boolean isEmpty();

    /**
     * 如果此列表包含指定元素，则返回 <tt>true</tt>。
     * 更正式地说，当且仅当此列表包含至少一个元素 <tt>e</tt>，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 <tt>true</tt>。
     *
     * @param o 要测试是否在此列表中的元素
     * @return <tt>true</tt> 如果此列表包含指定元素
     * @throws ClassCastException 如果指定元素的类型与此列表不兼容
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean contains(Object o);

    /**
     * 返回一个按正确顺序遍历此列表中元素的迭代器。
     *
     * @return 一个按正确顺序遍历此列表中元素的迭代器
     */
    Iterator<E> iterator();

    /**
     * 返回一个包含此列表中所有元素的数组，元素的顺序与列表中的顺序一致（从第一个元素到最后一个元素）。
     *
     * <p>返回的数组将是“安全”的，因为此列表不会维护对它的任何引用。（换句话说，即使此列表由数组支持，此方法也必须分配一个新数组）。
     * 调用者因此可以自由修改返回的数组。
     *
     * <p>此方法充当基于数组和基于集合的API之间的桥梁。
     *
     * @return 一个包含此列表中所有元素的数组，元素的顺序与列表中的顺序一致
     * @see Arrays#asList(Object[])
     */
    Object[] toArray();

    /**
     * 返回一个包含此列表中所有元素的数组，元素顺序与列表中的顺序一致（从第一个元素到最后一个元素）；返回数组的运行时类型与指定数组的类型相同。如果列表适合指定数组，则返回该数组。否则，将分配一个具有指定数组的运行时类型和此列表大小的新数组。
     *
     * <p>如果列表适合指定数组且有剩余空间（即数组的元素比列表多），则数组紧接列表末尾的元素将被设置为<tt>null</tt>。
     * （这在确定列表长度时<i>仅</i>在调用者知道列表不包含任何null元素时有用。）
     *
     * <p>与{@link #toArray()}方法类似，此方法充当基于数组和基于集合的API之间的桥梁。此外，此方法允许精确控制输出数组的运行时类型，并且在某些情况下可用于节省分配成本。
     *
     * <p>假设<tt>x</tt>是一个已知仅包含字符串的列表。以下代码可用于将列表转储到新分配的<tt>String</tt>数组中：
     *
     * <pre>{@code
     *     String[] y = x.toArray(new String[0]);
     * }</pre>
     *
     * 注意，<tt>toArray(new Object[0])</tt>在功能上与<tt>toArray()</tt>相同。
     *
     * @param a 用于存储此列表元素的数组，如果它足够大；否则，将为此目的分配一个具有相同运行时类型的新数组。
     * @return 包含此列表元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此列表中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果指定数组为null
     */
    <T> T[] toArray(T[] a);


    // 修改操作

    /**
     * 将指定元素追加到此列表的末尾（可选操作）。
     *
     * <p>支持此操作的列表可能会对可以添加到此列表的元素施加限制。
     * 特别是，某些列表会拒绝添加 null 元素，而其他列表会对可以添加的元素类型施加限制。
     * 列表类应在其文档中明确说明对可以添加的元素的任何限制。
     *
     * @param e 要追加到此列表的元素
     * @return <tt>true</tt>（由 {@link java.util.Collection#add} 指定）
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>add</tt> 操作
     * @throws ClassCastException 如果指定元素的类阻止其添加到此列表
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     * @throws IllegalArgumentException 如果此元素的某些属性阻止其添加到此列表
     */
    boolean add(E e);

    /**
     * 从列表中移除第一次出现的指定元素（如果存在）（可选操作）。如果列表中不包含该元素，则列表保持不变。
     * 更正式地说，移除索引最低的元素 <tt>i</tt>，使得
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * （如果存在这样的元素）。如果列表包含指定的元素，则返回 <tt>true</tt>
     * （或者等效地，如果列表因调用而发生了改变）。
     *
     * @param o 要从列表中移除的元素（如果存在）
     * @return <tt>true</tt> 如果列表包含指定的元素
     * @throws ClassCastException 如果指定元素的类型与此列表不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>remove</tt> 操作
     */
    boolean remove(Object o);


    // 批量修改操作

    /**
     * 如果此列表包含指定集合中的所有元素，则返回 <tt>true</tt>。
     *
     * @param  c 要检查是否包含在此列表中的集合
     * @return <tt>true</tt> 如果此列表包含指定集合中的所有元素
     * @throws ClassCastException 如果指定集合中的一个或多个元素的类型与此列表不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定集合包含一个或多个 null 元素且此列表不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定的集合为 null
     * @see #contains(Object)
     */
    boolean containsAll(java.util.Collection<?> c);

    /**
     * 将指定集合中的所有元素追加到此列表的末尾，按照指定集合的迭代器返回的顺序（可选操作）。如果在操作过程中修改了指定的集合，则此操作的行为是未定义的。
     * （注意，如果指定的集合是此列表，并且它非空，则会发生这种情况。）
     *
     * @param c 包含要添加到此列表的元素的集合
     * @return <tt>true</tt> 如果此列表因调用而改变
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>addAll</tt> 操作
     * @throws ClassCastException 如果指定集合的元素的类阻止其被添加到此列表
     * @throws NullPointerException 如果指定的集合包含一个或多个 null 元素且此列表不允许 null 元素，或者指定的集合为 null
     * @throws IllegalArgumentException 如果指定集合的元素的某些属性阻止其被添加到此列表
     * @see #add(Object)
     */
    boolean addAll(java.util.Collection<? extends E> c);

    /**
     * 将指定集合中的所有元素插入到此列表中的指定位置（可选操作）。将当前位于该位置的元素（如果有）以及所有后续元素向右移动（增加它们的索引）。新元素将按照指定集合的迭代器返回的顺序出现在此列表中。如果在操作过程中修改了指定的集合，则此操作的行为是未定义的。（请注意，如果指定的集合是此列表，并且它非空，则会发生这种情况。）
     *
     * @param index 要插入指定集合中第一个元素的索引
     * @param c 包含要添加到此列表中的元素的集合
     * @return 如果此列表因调用而更改，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>addAll</tt> 操作
     * @throws ClassCastException 如果指定集合中的某个元素的类阻止其添加到此列表
     * @throws NullPointerException 如果指定集合包含一个或多个 null 元素，并且此列表不允许 null 元素，或者指定的集合为 null
     * @throws IllegalArgumentException 如果指定集合中的某个元素的某些属性阻止其添加到此列表
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    boolean addAll(int index, java.util.Collection<? extends E> c);

    /**
     * 从该列表中移除所有包含在指定集合中的元素（可选操作）。
     *
     * @param c 包含要从该列表中移除的元素的集合
     * @return 如果该列表因调用而改变，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果该列表不支持 <tt>removeAll</tt> 操作
     * @throws ClassCastException 如果该列表中某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果该列表包含 null 元素且指定集合不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(java.util.Collection<?> c);

    /**
     * 仅保留此列表中包含在指定集合中的元素（可选操作）。换句话说，移除此列表中所有未包含在指定集合中的元素。
     *
     * @param c 包含要保留在此列表中的元素的集合
     * @return <tt>true</tt> 如果此列表因调用而更改
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>retainAll</tt> 操作
     * @throws ClassCastException 如果此列表的某个元素的类与指定集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果此列表包含 null 元素，并且指定集合不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定集合为 null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * 将此列表中的每个元素替换为对该元素应用操作符的结果。操作符抛出的错误或运行时异常会传递给调用者。
     *
     * @implSpec
     * 默认实现等同于，对于此 {@code list}：
     * <pre>{@code
     *     final ListIterator<E> li = list.listIterator();
     *     while (li.hasNext()) {
     *         li.set(operator.apply(li.next()));
     *     }
     * }</pre>
     *
     * 如果列表的列表迭代器不支持 {@code set} 操作，则在替换第一个元素时将抛出 {@code UnsupportedOperationException}。
     *
     * @param operator 要应用于每个元素的操作符
     * @throws UnsupportedOperationException 如果此列表不可修改。
     *         如果无法替换元素，或者通常不支持修改，则实现可能会抛出此异常
     * @throws NullPointerException 如果指定的操作符为 null，或者
     *         如果操作符结果为 null 值且此列表不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     * @since 1.8
     */
    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<E> li = this.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }

    /**
     * 根据指定的 {@link Comparator} 所诱导的顺序对此列表进行排序。
     *
     * <p>此列表中的所有元素必须使用指定的比较器<i>相互可比</i>（即，对于列表中的任何元素 {@code e1} 和 {@code e2}，
     * {@code c.compare(e1, e2)} 不得抛出 {@code ClassCastException}）。
     *
     * <p>如果指定的比较器为 {@code null}，则此列表中的所有元素必须实现 {@link Comparable} 接口，并且应使用元素的
     * {@linkplain Comparable 自然顺序}。
     *
     * <p>此列表必须是可修改的，但不一定是可调整大小的。
     *
     * @implSpec
     * 默认实现获取包含此列表中所有元素的数组，对数组进行排序，然后遍历此列表，从数组中的相应位置重置每个元素。
     * （这避免了尝试对链表进行原地排序时可能产生的 n<sup>2</sup> log(n) 性能问题。）
     *
     * @implNote
     * 此实现是一种稳定的、自适应的、迭代的归并排序，当输入数组部分有序时，所需的比较次数远少于 n lg(n)，
     * 同时在输入数组随机排序时提供传统归并排序的性能。如果输入数组几乎有序，则实现大约需要 n 次比较。
     * 临时存储需求从近乎有序输入数组的小常数到随机排序输入数组的 n/2 个对象引用不等。
     *
     * <p>实现充分利用了输入数组中的升序和降序，并且可以利用同一输入数组中不同部分的升序和降序。
     * 它非常适合合并两个或多个已排序的数组：只需将数组连接起来并对结果数组进行排序。
     *
     * <p>该实现改编自 Tim Peters 为 Python 编写的列表排序算法
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">TimSort</a>)。
     * 它使用了 Peter McIlroy 的“乐观排序和信息论复杂性”中的技术，该论文发表于第四届 ACM-SIAM 离散算法研讨会论文集，pp 467-474，1993 年 1 月。
     *
     * @param c 用于比较列表元素的 {@code Comparator}。{@code null} 值表示应使用元素的
     *          {@linkplain Comparable 自然顺序}
     * @throws ClassCastException 如果列表包含使用指定比较器<i>不可相互比较</i>的元素
     * @throws UnsupportedOperationException 如果列表的列表迭代器不支持 {@code set} 操作
     * @throws IllegalArgumentException
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     *         如果发现比较器违反了 {@link Comparator} 契约
     * @since 1.8
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }

    /**
     * 移除该列表中的所有元素（可选操作）。
     * 调用返回后，该列表将为空。
     *
     * @throws UnsupportedOperationException 如果该列表不支持 <tt>clear</tt> 操作
     */
    void clear();


    // 比较和哈希

    /**
     * 将指定对象与此列表进行相等性比较。当且仅当指定对象也是一个列表，两个列表具有相同的大小，并且两个列表中所有对应的元素对都相等时，返回 <tt>true</tt>。（两个元素 <tt>e1</tt> 和 <tt>e2</tt> 相等，如果 <tt>(e1==null ? e2==null : e1.equals(e2))</tt>。）换句话说，如果两个列表以相同的顺序包含相同的元素，则它们被定义为相等。此定义确保 equals 方法在 <tt>List</tt> 接口的不同实现中正常工作。
     *
     * @param o 要与此列表进行相等性比较的对象
     * @return 如果指定对象等于此列表，则返回 <tt>true</tt>
     */
    boolean equals(Object o);

    /**
     * 返回此列表的哈希码值。列表的哈希码定义为以下计算的结果：
     * <pre>{@code
     *     int hashCode = 1;
     *     for (E e : list)
     *         hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
     * }</pre>
     * 这确保了对于任何两个列表<tt>list1</tt>和<tt>list2</tt>，<tt>list1.equals(list2)</tt>意味着<tt>list1.hashCode()==list2.hashCode()</tt>，符合{@link Object#hashCode}的一般约定。
     *
     * @return 此列表的哈希码值
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    int hashCode();


    // 位置访问操作

    /**
     * 返回此列表中指定位置的元素。
     *
     * @param index 要返回的元素的索引
     * @return 此列表中指定位置的元素
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    E get(int index);

    /**
     * 将列表中指定位置的元素替换为指定的元素（可选操作）。
     *
     * @param index 要替换的元素的索引
     * @param element 要存储在指定位置的元素
     * @return 先前在指定位置的元素
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>set</tt> 操作
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此列表
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此列表
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    E set(int index, E element);

    /**
     * 在列表的指定位置插入指定元素（可选操作）。将当前位于该位置的元素（如果有）以及所有后续元素向右移动（将其索引加一）。
     *
     * @param index 要插入指定元素的索引
     * @param element 要插入的元素
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>add</tt> 操作
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此列表
     * @throws NullPointerException 如果指定元素为 null 并且此列表不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此列表
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    void add(int index, E element);

    /**
     * 移除列表中指定位置的元素（可选操作）。将任何后续元素向左移动（从它们的索引中减去一）。
     * 返回从列表中移除的元素。
     *
     * @param index 要移除的元素的索引
     * @return 之前位于指定位置的元素
     * @throws UnsupportedOperationException 如果此列表不支持 <tt>remove</tt> 操作
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    E remove(int index);


    // 搜索操作

    /**
     * 返回指定元素在此列表中第一次出现的索引，
     * 如果此列表不包含该元素，则返回 -1。
     * 更正式地说，返回满足 <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt> 的最低索引 <tt>i</tt>，
     * 如果没有这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @return 指定元素在此列表中第一次出现的索引，
     *         如果此列表不包含该元素，则返回 -1
     * @throws ClassCastException 如果指定元素的类型与此列表不兼容
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    int indexOf(Object o);

    /**
     * 返回指定元素在此列表中最后一次出现的索引，如果此列表不包含该元素，则返回 -1。
     * 更正式地说，返回满足 <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt> 的最高索引 <tt>i</tt>，
     * 如果没有这样的索引，则返回 -1。
     *
     * @param o 要搜索的元素
     * @return 指定元素在此列表中最后一次出现的索引，如果此列表不包含该元素，则返回 -1
     * @throws ClassCastException 如果指定元素的类型与此列表不兼容
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 并且此列表不允许 null 元素
     *         (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    int lastIndexOf(Object o);


    // 列表迭代器
// 这些Java注释以原格式翻译，保留注释字符。仅输出翻译结果，无需解释，无需注释。

    /**
     * 返回此列表中元素的列表迭代器（按适当顺序）。
     *
     * @return 此列表中元素的列表迭代器（按适当顺序）
     */
    ListIterator<E> listIterator();

    /**
     * 返回一个列表迭代器，该迭代器从列表中的指定位置开始，按适当顺序遍历列表中的元素。
     * 指定的索引表示第一次调用 {@link ListIterator#next next} 时将返回的第一个元素。
     * 第一次调用 {@link ListIterator#previous previous} 时将返回指定索引减一的元素。
     *
     * @param index 列表迭代器第一次调用 {@link ListIterator#next next} 时返回的第一个元素的索引
     * @return 从列表中指定位置开始的列表迭代器（按适当顺序）
     * @throws IndexOutOfBoundsException 如果索引超出范围
     *         ({@code index < 0 || index > size()})
     */
    ListIterator<E> listIterator(int index);

    // 视图

    /**
     * 返回此列表中指定的 <tt>fromIndex</tt>（包含）和 <tt>toIndex</tt>（不包含）之间的部分的视图。
     * （如果 <tt>fromIndex</tt> 和 <tt>toIndex</tt> 相等，则返回的列表为空。）
     * 返回的列表由该列表支持，因此返回列表中的非结构性更改会反映在此列表中，反之亦然。
     * 返回的列表支持此列表支持的所有可选列表操作。<p>
     *
     * 此方法消除了对显式范围操作的需求（通常存在于数组中的那种操作）。
     * 任何期望列表的操作都可以通过传递子列表视图而不是整个列表来用作范围操作。
     * 例如，以下惯用法从列表中删除一系列元素：
     * <pre>{@code
     *      list.subList(from, to).clear();
     * }</pre>
     * 类似的惯用法可以用于 <tt>indexOf</tt> 和 <tt>lastIndexOf</tt>，
     * 并且 <tt>Collections</tt> 类中的所有算法都可以应用于子列表。<p>
     *
     * 如果支持列表（即此列表）以任何方式（除了通过返回的列表）进行结构性修改，
     * 则此方法返回的列表的语义将变为未定义。
     * （结构性修改是指那些更改此列表大小或以其他方式扰乱它的操作，可能导致进行中的迭代产生不正确的结果。）
     *
     * @param fromIndex 子列表的低端点（包含）
     * @param toIndex 子列表的高端点（不包含）
     * @return 此列表中指定范围的视图
     * @throws IndexOutOfBoundsException 如果端点索引值非法
     *         (<tt>fromIndex &lt; 0 || toIndex &gt; size ||
     *         fromIndex &gt; toIndex</tt>)
     */
    List<E> subList(int fromIndex, int toIndex);

    /**
     * 创建一个 {@link Spliterator} 用于遍历此列表中的元素。
     *
     * <p>该 {@code Spliterator} 报告 {@link Spliterator#SIZED} 和
     * {@link Spliterator#ORDERED}。实现应记录其他特性值的报告。
     *
     * @implSpec
     * 默认实现从列表的 {@code Iterator} 创建一个
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em> 的 spliterator。
     * 该 spliterator 继承了列表迭代器的 <em>快速失败</em> 属性。
     *
     * @implNote
     * 创建的 {@code Spliterator} 额外报告 {@link Spliterator#SUBSIZED}。
     *
     * @return 一个 {@code Spliterator} 用于遍历此列表中的元素
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED);
    }
}
