package org.top.java.source.collection;

import java.util.Comparator;

/**
 * 一个 {@link Map}，进一步提供了对其键的<em>全序</em>。
 * 该映射根据其键的 {@linkplain Comparable 自然顺序} 或通常在创建有序映射时提供的 {@link Comparator} 进行排序。
 * 这种顺序在遍历有序映射的集合视图（由 {@code entrySet}、{@code keySet} 和 {@code values} 方法返回）时反映出来。
 * 提供了几种额外的操作以利用这种排序。（此接口是 {@link SortedSet} 的映射类比。）
 *
 * <p>插入到有序映射中的所有键必须实现 {@code Comparable} 接口（或被指定的比较器接受）。
 * 此外，所有这些键必须是<em>相互可比较的</em>：对于有序映射中的任何键 {@code k1} 和 {@code k2}，
 * {@code k1.compareTo(k2)}（或 {@code comparator.compare(k1, k2)}）不得抛出 {@code ClassCastException}。
 * 试图违反此限制将导致违规方法或构造函数调用抛出 {@code ClassCastException}。
 *
 * <p>请注意，如果有序映射要正确实现 {@code Map} 接口，则有序映射维护的顺序（无论是否提供了显式比较器）
 * 必须<em>与 equals 一致</em>。（有关<em>与 equals 一致</em>的准确定义，请参阅 {@code Comparable} 接口或 {@code Comparator} 接口。）
 * 这是因为 {@code Map} 接口是根据 {@code equals} 操作定义的，但有序映射使用其 {@code compareTo}（或 {@code compare}）方法执行所有键比较，
 * 因此，从有序映射的角度来看，被此方法视为相等的两个键是相等的。
 * 即使有序映射的顺序与 equals 不一致，树映射的行为<em>也是</em>明确定义的；它只是未能遵守 {@code Map} 接口的通用约定。
 *
 * <p>所有通用的有序映射实现类都应提供四个“标准”构造函数。
 * 尽管无法强制执行此建议，因为接口无法指定所需的构造函数。
 * 所有有序映射实现预期的“标准”构造函数如下：
 * <ol>
 *   <li>一个无参构造函数，创建一个空的有序映射，根据其键的自然顺序排序。</li>
 *   <li>一个接受 {@code Comparator} 类型参数的构造函数，创建一个空的有序映射，根据指定的比较器排序。</li>
 *   <li>一个接受 {@code Map} 类型参数的构造函数，创建一个新映射，其键值映射与参数相同，根据键的自然顺序排序。</li>
 *   <li>一个接受 {@code SortedMap} 类型参数的构造函数，创建一个新的有序映射，其键值映射和顺序与输入的有序映射相同。</li>
 * </ol>
 *
 * <p><strong>注意</strong>：有几种方法返回具有受限键范围的子映射。
 * 这些范围是<em>半开</em>的，即它们包括低端点但不包括高端点（如果适用）。
 * 如果您需要一个<em>闭区间</em>（包括两个端点），并且键类型允许计算给定键的后继键，
 * 只需请求从 {@code lowEndpoint} 到 {@code successor(highEndpoint)} 的子范围。
 * 例如，假设 {@code m} 是一个键为字符串的映射。以下惯用法获取一个视图，
 * 其中包含 {@code m} 中键在 {@code low} 和 {@code high} 之间（包括两端）的所有键值映射：<pre>
 *   SortedMap&lt;String, V&gt; sub = m.subMap(low, high+"\0");</pre>
 *
 * 类似的技术可用于生成<em>开区间</em>（不包括两个端点）。
 * 以下惯用法获取一个视图，其中包含 {@code m} 中键在 {@code low} 和 {@code high} 之间（不包括两端）的所有键值映射：<pre>
 *   SortedMap&lt;String, V&gt; sub = m.subMap(low+"\0", high);</pre>
 *
 * <p>此接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @param <K> 此映射维护的键的类型
 * @param <V> 映射值的类型
 *
 * @author  Josh Bloch
 * @see Map
 * @see TreeMap
 * @see SortedSet
 * @see Comparator
 * @see Comparable
 * @see Collection
 * @see ClassCastException
 * @since 1.2
 */

public interface SortedMap<K,V> extends Map<K,V> {
    /**
     * 返回用于对此映射中的键进行排序的比较器，如果此映射使用键的
     * {@linkplain Comparable 自然顺序}，则返回 {@code null}。
     *
     * @return 用于对此映射中的键进行排序的比较器，如果此映射使用键的自然顺序，则返回 {@code null}
     */
    Comparator<? super K> comparator();

    /**
     * 返回此映射中键的范围从 {@code fromKey}（包括）到 {@code toKey}（不包括）的部分的视图。
     * （如果 {@code fromKey} 和 {@code toKey} 相等，则返回的映射为空。）返回的映射由
     * 此映射支持，因此返回映射中的更改会反映在此映射中，反之亦然。返回的映射支持此映射支持的所有可选映射操作。
     *
     * <p>返回的映射在尝试插入超出其范围的键时将抛出 {@code IllegalArgumentException}。
     *
     * @param fromKey 返回映射中键的低端点（包括）
     * @param toKey 返回映射中键的高端点（不包括）
     * @return 此映射中键的范围从 {@code fromKey}（包括）到 {@code toKey}（不包括）的部分的视图
     * @throws ClassCastException 如果 {@code fromKey} 和 {@code toKey} 无法使用此映射的比较器
     *         相互比较（或者，如果映射没有比较器，则使用自然顺序）。如果 {@code fromKey} 或 {@code toKey}
     *         无法与映射中当前的键进行比较，则实现可以选择（但不是必须）抛出此异常。
     * @throws NullPointerException 如果 {@code fromKey} 或 {@code toKey} 为 null 并且此映射不允许 null 键
     * @throws IllegalArgumentException 如果 {@code fromKey} 大于 {@code toKey}；或者如果此映射本身有
     *         限制范围，并且 {@code fromKey} 或 {@code toKey} 超出该范围的边界
     */
    SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * 返回此映射中键严格小于 {@code toKey} 的部分的视图。返回的映射由此映射支持，因此返回映射中的更改会
     * 反映在此映射中，反之亦然。返回的映射支持此映射支持的所有可选映射操作。
     *
     * <p>返回的映射在尝试插入超出其范围的键时将抛出 {@code IllegalArgumentException}。
     *
     * @param toKey 返回映射中键的高端点（不包括）
     * @return 此映射中键严格小于 {@code toKey} 的部分的视图
     * @throws ClassCastException 如果 {@code toKey} 与此映射的比较器不兼容（或者，如果映射没有比较器，
     *         如果 {@code toKey} 未实现 {@link Comparable}）。如果 {@code toKey} 无法与映射中
     *         当前的键进行比较，则实现可以选择（但不是必须）抛出此异常。
     * @throws NullPointerException 如果 {@code toKey} 为 null 并且此映射不允许 null 键
     * @throws IllegalArgumentException 如果此映射本身有限制范围，并且 {@code toKey} 超出该范围的边界
     */
    SortedMap<K,V> headMap(K toKey);

    /**
     * 返回此映射中键大于或等于 {@code fromKey} 的部分的视图。返回的映射由此映射支持，因此返回映射中的
     * 更改会反映在此映射中，反之亦然。返回的映射支持此映射支持的所有可选映射操作。
     *
     * <p>返回的映射在尝试插入超出其范围的键时将抛出 {@code IllegalArgumentException}。
     *
     * @param fromKey 返回映射中键的低端点（包括）
     * @return 此映射中键大于或等于 {@code fromKey} 的部分的视图
     * @throws ClassCastException 如果 {@code fromKey} 与此映射的比较器不兼容（或者，如果映射没有比较器，
     *         如果 {@code fromKey} 未实现 {@link Comparable}）。如果 {@code fromKey} 无法与映射中
     *         当前的键进行比较，则实现可以选择（但不是必须）抛出此异常。
     * @throws NullPointerException 如果 {@code fromKey} 为 null 并且此映射不允许 null 键
     * @throws IllegalArgumentException 如果此映射本身有限制范围，并且 {@code fromKey} 超出该范围的边界
     */
    SortedMap<K,V> tailMap(K fromKey);

    /**
     * 返回此映射中当前第一个（最低）键。
     *
     * @return 此映射中当前第一个（最低）键
     * @throws NoSuchElementException 如果此映射为空
     */
    K firstKey();

    /**
     * 返回此映射中当前最后一个（最高）键。
     *
     * @return 此映射中当前最后一个（最高）键
     * @throws NoSuchElementException 如果此映射为空
     */
    K lastKey();

    /**
     * 返回此映射中包含的键的 {@link Set} 视图。集合的迭代器按升序返回键。集合由映射支持，因此对映射的
     * 更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射（除了通过迭代器自己的 {@code remove}
     * 操作），则迭代的结果是未定义的。集合支持元素移除，通过 {@code Iterator.remove}、{@code Set.remove}、
     * {@code removeAll}、{@code retainAll} 和 {@code clear} 操作从映射中移除相应的映射。它不支持
     * {@code add} 或 {@code addAll} 操作。
     *
     * @return 此映射中包含的键的集合视图，按升序排序
     */
    Set<K> keySet();

    /**
     * 返回此映射中包含的值的 {@link Collection} 视图。集合的迭代器按对应键的升序返回值。集合由映射支持，
     * 因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射（除了通过迭代器自己的
     * {@code remove} 操作），则迭代的结果是未定义的。集合支持元素移除，通过 {@code Iterator.remove}、
     * {@code Collection.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 操作
     * 从映射中移除相应的映射。它不支持 {@code add} 或 {@code addAll} 操作。
     *
     * @return 此映射中包含的值的集合视图，按键的升序排序
     */
    Collection<V> values();

    /**
     * 返回此映射中包含的映射的 {@link Set} 视图。集合的迭代器按键的升序返回条目。集合由映射支持，因此对
     * 映射的更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射（除了通过迭代器自己的 {@code remove}
     * 操作，或者通过迭代器返回的映射条目的 {@code setValue} 操作），则迭代的结果是未定义的。集合支持元素移除，
     * 通过 {@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和
     * {@code clear} 操作从映射中移除相应的映射。它不支持 {@code add} 或 {@code addAll} 操作。
     *
     * @return 此映射中包含的映射的集合视图，按键的升序排序
     */
    Set<Map.Entry<K, V>> entrySet();
}
