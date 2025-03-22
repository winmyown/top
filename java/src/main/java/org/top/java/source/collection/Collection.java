package org.top.java.source.collection;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Iterator;
import java.lang.Iterable;

/**
 * 这是<i>集合层次结构</i>中的根接口。集合表示一组对象，这些对象称为其<i>元素</i>。一些集合允许重复元素，而另一些则不允许。一些集合是有序的，而另一些是无序的。JDK 没有提供此接口的任何<i>直接</i>实现：它提供了更具体的子接口的实现，例如 {@code Set} 和 {@code List}。此接口通常用于传递集合并在需要最大通用性的地方操作它们。
 *
 * <p><i>包</i>或<i>多重集</i>（可能包含重复元素的无序集合）应直接实现此接口。
 *
 * <p>所有通用的 {@code Collection} 实现类（通常通过其子接口之一间接实现 {@code Collection}）应提供两个“标准”构造函数：一个无参构造函数，用于创建空集合；另一个构造函数接受一个 {@code Collection} 类型的参数，用于创建一个与参数具有相同元素的新集合。实际上，后者允许用户复制任何集合，生成所需实现类型的等效集合。无法强制执行此约定（因为接口不能包含构造函数），但 Java 平台库中的所有通用 {@code Collection} 实现都遵守此约定。
 *
 * <p>某些方法被指定为<i>可选的</i>。如果集合实现不支持特定操作，则应定义相应方法以抛出 {@code UnsupportedOperationException}。此类方法在集合接口的方法规范中标记为“可选操作”。
 *
 * <p><a id="optional-restrictions"></a>某些集合实现对它们可能包含的元素有限制。例如，某些实现禁止空元素，而某些实现对元素的类型有限制。尝试添加不合格的元素会抛出未经检查的异常，通常是 {@code NullPointerException} 或 {@code ClassCastException}。尝试查询不合格元素的存在可能会抛出异常，或者可能只是返回 false；某些实现会表现出前一种行为，而另一些则会表现出后一种行为。更一般地说，尝试对不合格元素执行操作，如果该操作的完成不会导致将不合格元素插入集合中，则可能会抛出异常或成功，具体取决于实现的选项。此类异常在此接口的规范中标记为“可选”。
 *
 * <p>每个集合自行决定其同步策略。在没有更强保证的情况下，对正在被另一个线程修改的集合调用任何方法可能会导致未定义的行为；这包括直接调用、将集合传递给可能执行调用的方法以及使用现有迭代器检查集合。
 *
 * <p>集合框架接口中的许多方法是根据 {@link Object#equals(Object) equals} 方法定义的。例如，{@link #contains(Object) contains(Object o)} 方法的规范说：“当且仅当此集合包含至少一个元素 {@code e} 使得 {@code (o==null ? e==null : o.equals(e))} 时，返回 {@code true}。” 此规范<i>不应</i>被解释为暗示使用非空参数 {@code o} 调用 {@code Collection.contains} 将导致对任何元素 {@code e} 调用 {@code o.equals(e)}。实现可以自由地实施优化，从而避免 {@code equals} 调用，例如，首先比较两个元素的哈希码。（{@link Object#hashCode()} 规范保证哈希码不相等的两个对象不可能相等。）更一般地说，各种集合框架接口的实现可以自由地利用底层 {@link Object} 方法的指定行为，只要实现者认为合适。
 *
 * <p>某些执行集合递归遍历的集合操作可能会在自引用实例中失败并抛出异常，其中集合直接或间接包含自身。这包括 {@code clone()}、{@code equals()}、{@code hashCode()} 和 {@code toString()} 方法。实现可以选择性地处理自引用场景，但大多数当前实现不这样做。
 *
 * <h2><a id="view">视图集合</a></h2>
 *
 * <p>大多数集合管理它们包含的元素的存储。相比之下，<i>视图集合</i>本身不存储元素，而是依赖后备集合来存储实际元素。视图集合本身不处理的操作会委托给后备集合。视图集合的示例包括由 {@link Collections#checkedCollection Collections.checkedCollection}、{@link Collections#synchronizedCollection Collections.synchronizedCollection} 和 {@link Collections#unmodifiableCollection Collections.unmodifiableCollection} 等方法返回的包装集合。其他视图集合的示例包括提供相同元素的不同表示的集合，例如由 {@link List#subList List.subList}、{@link NavigableSet#subSet NavigableSet.subSet} 或 {@link Map#entrySet Map.entrySet} 提供的集合。对后备集合所做的任何更改在视图集合中都是可见的。相应地，对视图集合所做的任何更改（如果允许更改）也会写回到后备集合。尽管从技术上讲它们不是集合，{@link Iterator} 和 {@link ListIterator} 的实例也可以允许修改写回到后备集合，并且在某些情况下，对后备集合的修改在迭代期间对迭代器可见。
 *
 * <h2><a id="unmodifiable">不可修改集合</a></h2>
 *
 * <p>此接口的某些方法被认为是“破坏性的”，并被称为“修改器”方法，因为它们修改了它们操作的集合中包含的对象组。如果此集合实现不支持该操作，则可以指定它们抛出 {@code UnsupportedOperationException}。如果调用对集合没有影响，则此类方法应（但不是必须）抛出 {@code UnsupportedOperationException}。例如，考虑一个不支持 {@link #add add} 操作的集合。如果在此集合上调用 {@link #addAll addAll} 方法，并以空集合作为参数，会发生什么？添加零个元素没有影响，因此此集合简单地不执行任何操作且不抛出异常是允许的。但是，建议此类情况无条件抛出异常，因为仅在特定情况下抛出异常可能会导致编程错误。
 *
 * <p>一个<i>不可修改集合</i>是一个集合，其所有修改器方法（如上定义）都被指定为抛出 {@code UnsupportedOperationException}。因此，这样的集合不能通过调用其任何方法来修改。为了使集合真正不可修改，从它派生的任何视图集合也必须不可修改。例如，如果列表是不可修改的，则 {@link List#subList List.subList} 返回的列表也是不可修改的。
 *
 * <p>不可修改集合不一定是不可变的。如果包含的元素是可变的，则整个集合显然是可变的，即使它可能是不可修改的。例如，考虑两个包含可变元素的不可修改列表。如果元素已被修改，则调用 {@code list1.equals(list2)} 的结果可能因调用而异，即使两个列表都是不可修改的。但是，如果不可修改集合包含所有不可变元素，则可以认为它是有效不可变的。
 *
 * <h2><a id="unmodview">不可修改视图集合</a></h2>
 *
 * <p>一个<i>不可修改视图集合</i>是一个不可修改的集合，并且也是一个后备集合的视图。其修改器方法抛出 {@code UnsupportedOperationException}，如上所述，而读取和查询方法则委托给后备集合。效果是提供对后备集合的只读访问。这对于组件向用户提供对内部集合的只读访问非常有用，同时防止他们意外修改此类集合。不可修改视图集合的示例包括由 {@link Collections#unmodifiableCollection Collections.unmodifiableCollection}、{@link Collections#unmodifiableList Collections.unmodifiableList} 及相关方法返回的集合。
 *
 * <p>请注意，对后备集合的更改可能仍然是可能的，如果发生更改，则通过不可修改视图可见。因此，不可修改视图集合不一定是不可变的。但是，如果不可修改视图的后备集合是有效不可变的，或者如果后备集合的唯一引用是通过不可修改视图，则该视图可以被认为是有效不可变的。
 *
 * <p>此接口是
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java 集合框架</a>的成员。
 *
 * @implSpec
 * 默认方法实现（继承或其他）不应用任何同步协议。如果 {@code Collection} 实现具有特定的同步协议，则必须覆盖默认实现以应用该协议。
 *
 * @param <E> 此集合中元素的类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Set
 * @see     List
 * @see     Map
 * @see     SortedSet
 * @see     SortedMap
 * @see     HashSet
 * @see     TreeSet
 * @see     ArrayList
 * @see     LinkedList
 * @see     Vector
 * @see     Collections
 * @see     Arrays
 * @see     AbstractCollection
 * @since 1.2
 */
public interface Collection<E> extends Iterable<E> {
    /**
     * 返回此集合中的元素数量。如果此集合包含的元素数量超过 <tt>Integer.MAX_VALUE</tt>，
     * 则返回 <tt>Integer.MAX_VALUE</tt>。
     *
     * @return 此集合中的元素数量
     */
    int size();

    /**
     * 如果此集合不包含任何元素，则返回 <tt>true</tt>。
     *
     * @return 如果此集合不包含任何元素，则返回 <tt>true</tt>
     */
    boolean isEmpty();

    /**
     * 如果此集合包含指定元素，则返回 <tt>true</tt>。
     * 更正式地说，当且仅当此集合包含至少一个满足 <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * 的元素 <tt>e</tt> 时，返回 <tt>true</tt>。
     *
     * @param o 要测试是否在此集合中存在的元素
     * @return 如果此集合包含指定元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     *         （<a href="#optional-restrictions">可选</a>）
     */
    boolean contains(Object o);

    /**
     * 返回一个遍历此集合中元素的迭代器。不保证元素返回的顺序
     * （除非此集合是某个提供顺序保证的类的实例）。
     *
     * @return 一个遍历此集合中元素的 <tt>Iterator</tt>
     */
    Iterator<E> iterator();

    /**
     * 返回一个包含此集合中所有元素的数组。
     * 如果此集合对其迭代器返回元素的顺序有任何保证，则此方法必须按相同顺序返回元素。
     *
     * <p>返回的数组是“安全的”，因为此集合不会维护对它的引用。
     * （换句话说，即使此集合由数组支持，此方法也必须分配一个新数组。）
     * 调用者可以自由修改返回的数组。
     *
     * <p>此方法充当基于数组和基于集合的 API 之间的桥梁。
     *
     * @return 一个包含此集合中所有元素的数组
     */
    Object[] toArray();

    /**
     * 返回一个包含此集合中所有元素的数组；返回数组的运行时类型是指定数组的类型。
     * 如果集合适合指定的数组，则返回该数组。否则，将分配一个具有指定数组的运行时类型
     * 和此集合大小的新数组。
     *
     * <p>如果此集合适合指定的数组并有剩余空间（即数组的元素比此集合多），
     * 则紧跟在集合末尾之后的数组元素设置为 <tt>null</tt>。
     * （这仅在调用者知道此集合不包含任何 <tt>null</tt> 元素时，可用于确定此集合的长度。）
     *
     * <p>如果此集合对其迭代器返回元素的顺序有任何保证，则此方法必须按相同顺序返回元素。
     *
     * <p>与 {@link #toArray()} 方法类似，此方法充当基于数组和基于集合的 API 之间的桥梁。
     * 此外，此方法允许精确控制输出数组的运行时类型，并且在某些情况下可用于节省分配成本。
     *
     * <p>假设 <tt>x</tt> 是一个仅包含字符串的集合。
     * 以下代码可用于将集合转储到新分配的 <tt>String</tt> 数组中：
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * 注意，<tt>toArray(new Object[0])</tt> 在功能上与 <tt>toArray()</tt> 相同。
     *
     * @param <T> 包含集合的数组的运行时类型
     * @param a 存储此集合元素的数组（如果它足够大）；否则，将为此目的分配一个具有相同运行时类型的新数组。
     * @return 一个包含此集合中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此集合中每个元素的运行时类型的超类型
     * @throws NullPointerException 如果指定数组为 null
     */
    <T> T[] toArray(T[] a);

    /**
     * 确保此集合包含指定元素（可选操作）。
     * 如果此集合因调用而发生变化，则返回 <tt>true</tt>。
     * （如果此集合不允许重复且已经包含指定元素，则返回 <tt>false</tt>。）
     *
     * <p>支持此操作的集合可能会对可以添加到此集合的元素施加限制。
     * 特别是，某些集合会拒绝添加 <tt>null</tt> 元素，而其他集合会对可以添加的元素类型施加限制。
     * 集合类应在其文档中明确指定可以添加的元素的任何限制。
     *
     * <p>如果集合因任何原因拒绝添加特定元素（而不是因为它已经包含该元素），
     * 则它 <i>必须</i> 抛出异常（而不是返回 <tt>false</tt>）。
     * 这保留了在此调用返回后集合始终包含指定元素的不变性。
     *
     * @param e 要确保其在此集合中存在的元素
     * @return 如果此集合因调用而发生变化，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>add</tt> 操作
     * @throws ClassCastException 如果指定元素的类阻止其添加到此集合
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     * @throws IllegalArgumentException 如果元素的某些属性阻止其添加到此集合
     * @throws IllegalStateException 由于插入限制，此时无法添加元素
     */
    boolean add(E e);

    /**
     * 从此集合中移除指定元素的单个实例（如果存在）（可选操作）。
     * 更正式地说，移除满足 <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * 的元素 <tt>e</tt>（如果此集合包含一个或多个此类元素）。
     * 如果此集合包含指定元素（或等效地，如果此集合因调用而发生变化），则返回 <tt>true</tt>。
     *
     * @param o 要从此集合中移除的元素（如果存在）
     * @return 如果此集合因调用而移除元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws NullPointerException 如果指定元素为 null 且此集合不允许 null 元素
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>remove</tt> 操作
     */
    boolean remove(Object o);

    /**
     * 如果此集合包含指定集合中的所有元素，则返回 <tt>true</tt>。
     *
     * @param c 要检查是否包含在此集合中的集合
     * @return 如果此集合包含指定集合中的所有元素，则返回 <tt>true</tt>
     * @throws ClassCastException 如果指定集合中一个或多个元素的类型与此集合不兼容
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws NullPointerException 如果指定集合包含一个或多个 null 元素且此集合不允许 null 元素，
     *         或者如果指定集合为 null
     * @see    #contains(Object)
     */
    boolean containsAll(Collection<?> c);

    /**
     * 将指定集合中的所有元素添加到此集合中（可选操作）。
     * 如果在操作进行时修改了指定集合，则此操作的行为是未定义的。
     * （这意味着如果指定集合是此集合且此集合非空，则此调用的行为是未定义的。）
     *
     * @param c 包含要添加到此集合中的元素的集合
     * @return 如果此集合因调用而发生变化，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>addAll</tt> 操作
     * @throws ClassCastException 如果指定集合中某个元素的类阻止其添加到此集合
     * @throws NullPointerException 如果指定集合包含 null 元素且此集合不允许 null 元素，
     *         或者如果指定集合为 null
     * @throws IllegalArgumentException 如果指定集合中某个元素的某些属性阻止其添加到此集合
     * @throws IllegalStateException 由于插入限制，此时无法添加所有元素
     * @see #add(Object)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * 移除此集合中所有也包含在指定集合中的元素（可选操作）。
     * 此调用返回后，此集合将不包含与指定集合相同的任何元素。
     *
     * @param c 包含要移除此集合中元素的集合
     * @return 如果此集合因调用而发生变化，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>removeAll</tt> 方法
     * @throws ClassCastException 如果此集合中一个或多个元素的类型与指定集合不兼容
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws NullPointerException 如果此集合包含一个或多个 null 元素且指定集合不支持 null 元素，
     *         或者如果指定集合为 null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * 移除此集合中满足给定谓词的所有元素。在迭代期间或由谓词抛出的错误或运行时异常会传递给调用者。
     *
     * @implSpec
     * 默认实现使用集合的 {@link #iterator} 遍历所有元素。每个匹配的元素使用
     * {@link Iterator#remove()} 移除。如果集合的迭代器不支持移除，则在第一个匹配元素上抛出
     * {@code UnsupportedOperationException}。
     *
     * @param filter 一个谓词，返回 {@code true} 的元素将被移除
     * @return 如果移除了任何元素，则返回 {@code true}
     * @throws NullPointerException 如果指定的谓词为 null
     * @throws UnsupportedOperationException 如果无法从此集合中移除元素。如果无法移除匹配元素
     *         或通常不支持移除，则实现可能会抛出此异常。
     * @since 1.8
     */
    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * 仅保留此集合中包含在指定集合中的元素（可选操作）。
     * 换句话说，移除此集合中所有不包含在指定集合中的元素。
     *
     * @param c 包含要保留在此集合中的元素的集合
     * @return 如果此集合因调用而发生变化，则返回 <tt>true</tt>
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>retainAll</tt> 操作
     * @throws ClassCastException 如果此集合中一个或多个元素的类型与指定集合不兼容
     *         （<a href="#optional-restrictions">可选</a>）
     * @throws NullPointerException 如果此集合包含一个或多个 null 元素且指定集合不允许 null 元素，
     *         或者如果指定集合为 null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * 移除此集合中的所有元素（可选操作）。此方法返回后，集合将为空。
     *
     * @throws UnsupportedOperationException 如果此集合不支持 <tt>clear</tt> 操作
     */
    void clear();

    /**
     * 将指定对象与此集合进行比较以判断是否相等。
     *
     * <p>虽然 <tt>Collection</tt> 接口没有为 <tt>Object.equals</tt> 的通用契约添加任何规定，
     * 但直接实现 <tt>Collection</tt> 接口的程序员（换句话说，创建一个是 <tt>Collection</tt>
     * 但不是 <tt>Set</tt> 或 <tt>List</tt> 的类）如果选择覆盖 <tt>Object.equals</tt>，
     * 则必须小心。不必这样做，最简单的做法是依赖 <tt>Object</tt> 的实现，
     * 但实现者可能希望实现“值比较”以替代默认的“引用比较”。
     * （<tt>List</tt> 和 <tt>Set</tt> 接口要求进行此类值比较。）
     *
     * <p><tt>Object.equals</tt> 方法的通用契约规定，equals 必须是对称的
     * （换句话说，<tt>a.equals(b)</tt> 当且仅当 <tt>b.equals(a)</tt>）。
     * <tt>List.equals</tt> 和 <tt>Set.equals</tt> 的契约规定，列表仅与其他列表相等，
     * 集合仅与其他集合相等。因此，对于既不实现 <tt>List</tt> 也不实现 <tt>Set</tt> 接口的集合类，
     * 其自定义的 <tt>equals</tt> 方法在此集合与任何列表或集合比较时必须返回 <tt>false</tt>。
     * （根据同样的逻辑，不可能编写一个正确实现 <tt>Set</tt> 和 <tt>List</tt> 接口的类。）
     *
     * @param o 要与此集合进行相等性比较的对象
     * @return 如果指定对象与此集合相等，则返回 <tt>true</tt>
     *
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     * @see List#equals(Object)
     */
    boolean equals(Object o);

    /**
     * 返回此集合的哈希码值。虽然 <tt>Collection</tt> 接口没有为 <tt>Object.hashCode</tt> 方法
     * 的通用契约添加任何规定，但程序员应注意，任何覆盖 <tt>Object.equals</tt> 方法的类
     * 也必须覆盖 <tt>Object.hashCode</tt> 方法，以满足 <tt>Object.hashCode</tt> 方法的通用契约。
     * 特别是，<tt>c1.equals(c2)</tt> 意味着 <tt>c1.hashCode()==c2.hashCode()</tt>。
     *
     * @return 此集合的哈希码值
     *
     * @see Object#hashCode()
     * @see Object#equals(Object)
     */
    int hashCode();

    /**
     * 创建一个 {@link Spliterator} 来遍历此集合中的元素。
     *
     * <p>实现应记录由 spliterator 报告的特征值。如果 spliterator 报告 {@link Spliterator#SIZED}
     * 且此集合不包含任何元素，则不需要报告此类特征值。
     *
     * <p>默认实现应被子类覆盖，以返回更高效的 spliterator。为了保留 {@link #stream()} 和
     * {@link #parallelStream()} 方法的预期惰性行为，spliterator 应具有 {@code IMMUTABLE}
     * 或 {@code CONCURRENT} 特征，或者是 <em><a href="Spliterator.html#binding">延迟绑定</a></em>。
     * 如果这些都不切实际，则覆盖类应描述 spliterator 的绑定和结构干扰的文档策略，
     * 并应覆盖 {@link #stream()} 和 {@link #parallelStream()} 方法以使用 spliterator 的
     * {@code Supplier} 创建流，例如：
     * <pre>{@code
     *     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
     * }</pre>
     * <p>这些要求确保由 {@link #stream()} 和 {@link #parallelStream()} 方法生成的流将反映
     * 终端流操作启动时集合的内容。
     *
     * @implSpec
     * 默认实现从集合的 {@code Iterator} 创建一个
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em> 的 spliterator。
     * 该 spliterator 继承集合迭代器的 <em>快速失败</em> 属性。
     * <p>
     * 创建的 {@code Spliterator} 报告 {@link Spliterator#SIZED}。
     *
     * @implNote
     * 创建的 {@code Spliterator} 还报告 {@link Spliterator#SUBSIZED}。
     *
     * <p>如果 spliterator 不覆盖任何元素，则报告除 {@code SIZED} 和 {@code SUBSIZED} 之外的
     * 其他特征值不会帮助客户端控制、专门化或简化计算。然而，这确实允许共享使用不可变且空的
     * spliterator 实例（参见 {@link Spliterators#emptySpliterator()}）用于空集合，
     * 并使客户端能够确定此类 spliterator 是否不覆盖任何元素。
     *
     * @return 一个 {@code Spliterator}，用于遍历此集合中的元素
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        //return Spliterators.spliterator(this, 0);
        return null;
    }

    /**
     * 返回一个以此集合为源的顺序 {@code Stream}。
     *
     * <p>当 {@link #spliterator()} 方法无法返回 {@code IMMUTABLE}、{@code CONCURRENT}
     * 或 <em>延迟绑定</em> 的 spliterator 时，应覆盖此方法。（参见 {@link #spliterator()} 的详细信息。）
     *
     * @implSpec
     * 默认实现从集合的 {@code Spliterator} 创建一个顺序 {@code Stream}。
     *
     * @return 一个顺序 {@code Stream}，用于遍历此集合中的元素
     * @since 1.8
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回一个以此集合为源的可能是并行的 {@code Stream}。允许此方法返回顺序流。
     *
     * <p>当 {@link #spliterator()} 方法无法返回 {@code IMMUTABLE}、{@code CONCURRENT}
     * 或 <em>延迟绑定</em> 的 spliterator 时，应覆盖此方法。（参见 {@link #spliterator()} 的详细信息。）
     *
     * @implSpec
     * 默认实现从集合的 {@code Spliterator} 创建一个并行 {@code Stream}。
     *
     * @return 一个可能是并行的 {@code Stream}，用于遍历此集合中的元素
     * @since 1.8
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
