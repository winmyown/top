package org.top.java.source.collection;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.SortedMap;

/**
 * {@link SortedMap} 的扩展，提供了导航方法，返回与给定搜索目标最接近的匹配项。
 * 方法 {@code lowerEntry}、{@code floorEntry}、{@code ceilingEntry} 和 {@code higherEntry}
 * 分别返回与小于、小于或等于、大于或等于以及大于给定键的键相关联的 {@code Map.Entry} 对象，
 * 如果没有这样的键，则返回 {@code null}。类似地，方法 {@code lowerKey}、{@code floorKey}、
 * {@code ceilingKey} 和 {@code higherKey} 仅返回关联的键。所有这些方法都设计用于定位条目，而不是遍历条目。
 *
 * <p>{@code NavigableMap} 可以按升序或降序访问和遍历键。
 * 方法 {@code descendingMap} 返回一个视图，其中所有关系和方向方法的意义都被反转。
 * 升序操作和视图的性能可能比降序操作更快。
 * 方法 {@code subMap}、{@code headMap} 和 {@code tailMap} 与同名的 {@code SortedMap} 方法不同，
 * 它们接受额外的参数来描述下界和上界是包含还是排除的。
 * 任何 {@code NavigableMap} 的子映射都必须实现 {@code NavigableMap} 接口。
 *
 * <p>此接口还定义了方法 {@code firstEntry}、{@code pollFirstEntry}、{@code lastEntry} 和
 * {@code pollLastEntry}，它们返回和/或移除最小和最大的映射（如果存在），否则返回 {@code null}。
 *
 * <p>返回条目的方法的实现应返回表示映射快照的 {@code Map.Entry} 对，
 * 因此通常<em>不支持</em>可选的 {@code Entry.setValue} 方法。
 * 但请注意，可以使用 {@code put} 方法更改关联映射中的映射。
 *
 * <p>方法
 * {@link #subMap(Object, Object) subMap(K, K)}、
 * {@link #headMap(Object) headMap(K)} 和
 * {@link #tailMap(Object) tailMap(K)}
 * 被指定为返回 {@code SortedMap}，以允许现有的 {@code SortedMap} 实现兼容地改造为
 * 实现 {@code NavigableMap}，但鼓励此接口的扩展和实现重写这些方法以返回
 * {@code NavigableMap}。类似地，{@link #keySet()} 可以被重写以返回 {@code NavigableSet}。
 *
 * <p>此接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @param <K> 此映射维护的键的类型
 * @param <V> 映射值的类型
 * @since 1.6
 */

public interface NavigableMap<K,V> extends SortedMap<K,V> {
    /**
     * 返回与严格小于给定键的最大键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 与小于 {@code key} 的最大键关联的条目，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    Map.Entry<K,V> lowerEntry(K key);

    /**
     * 返回严格小于给定键的最大键，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 小于 {@code key} 的最大键，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    K lowerKey(K key);

    /**
     * 返回与小于或等于给定键的最大键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 与小于或等于 {@code key} 的最大键关联的条目，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    Map.Entry<K,V> floorEntry(K key);

    /**
     * 返回小于或等于给定键的最大键，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 小于或等于 {@code key} 的最大键，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    K floorKey(K key);

    /**
     * 返回与大于或等于给定键的最小键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 与大于或等于 {@code key} 的最小键关联的条目，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 极
     */
    Map.Entry<K,V> ceilingEntry(K key);

    /**
     * 返回大于或等于给定键的最小键，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 大于或等于 {@code key} 的最小键，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    K ceilingKey(K key);

    /**
     * 返回与严格大于给定键的最小键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 与大于 {@code key} 的最小键关联的条目，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    Map.Entry<K,V> higherEntry(K key);

    /**
     * 返回严格大于给定键的最小键，如果没有这样的键，则返回 {@code null}。
     *
     * @param key 键
     * @return 大于 {@code key} 的最小键，如果没有这样的键，则返回 {@code null}
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null 且此映射不允许 null 键
     */
    K higherKey(K key);

    /**
     * 返回与此映射中的最小键关联的键值映射，如果映射为空，则返回 {@code null}。
     *
     * @return 与最小键关联的条目，如果此映射为空，则返回 {@code null}
     */
    Map.Entry<K,V> firstEntry();

    /**
     * 返回与此映射中的最大键关联的键值映射，如果映射为空，则返回 {@code null}。
     *
     * @return 与最大键关联的条目，如果此映射为空，则返回 {@code null}
     */
    Map.Entry<K,V> lastEntry();

    /**
     * 移除并返回与此映射中的最小键关联的键值映射，如果映射为空，则返回 {@code null}。
     *
     * @return 被移除的第一个条目，如果此映射为空，则返回 {@code null}
     */
    Map.Entry<K,V> pollFirstEntry();

    /**
     * 移除并返回与此映射中的最大键关联的键值映射，如果映射为空，则返回 {@code null}。
     *
     * @return 被移除的最后一个条目，如果此映射为空，则返回 {@code null}
     */
    Map.Entry<K,V> pollLastEntry();

    /**
     * 返回此映射中包含的映射的反序视图。降序映射由该映射支持，因此对映射的更改会反映在降序映射中，反之亦然。如果在迭代任一映射的集合视图时修改了任一映射（除了通过迭代器自身的 {@code remove} 操作），则迭代的结果是未定义的。
     *
     * <p>返回的映射的排序等同于
     * <tt>{@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator())</tt>。
     * 表达式 {@code m.descendingMap().descendingMap()} 返回的视图与 {@code m} 基本等价。
     *
     * @return 此映射的反序视图
     */
    NavigableMap<K,V> descendingMap();

    /**
     * 返回此映射中包含的键的 {@link NavigableSet} 视图。集合的迭代器按升序返回键。集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合时修改了映射（除了通过迭代器自身的 {@code remove} 操作），则迭代的结果是未定义的。集合支持元素移除，通过 {@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 操作从映射中移除相应的映射。它不支持 {@code add} 或 {@code addAll} 操作。
     *
     * @return 此映射中键的可导航集合视图
     */
    NavigableSet<K> navigableKeySet();

    /**
     * 返回此映射中包含的键的反序 {@link NavigableSet} 视图。集合的迭代器按降序返回键。集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合时修改了映射（除了通过迭代器自身的 {@code remove} 操作），则迭代的结果是未定义的。集合支持元素移除，通过 {@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 操作从映射中移除相应的映射。它不支持 {@code add} 或 {@code addAll} 操作。
     *
     * @return 此映射中键的反序可导航集合视图
     */
    NavigableSet<K> descendingKeySet();

    /**
     * 返回此映射中键的范围从 {@code fromKey} 到 {@code toKey} 的部分视图。如果 {@code fromKey} 和 {@code toKey} 相等，则返回的映射为空，除非 {@code fromInclusive} 和 {@code toInclusive} 都为 true。返回的映射由该映射支持，因此对返回映射的更改会反映在该映射中，反之亦然。返回的映射支持该映射支持的所有可选映射操作。
     *
     * <p>返回的映射将尝试插入超出其范围的键时抛出 {@code IllegalArgumentException}，或尝试构造一个子映射，其任一端点超出其范围时抛出该异常。
     *
     * @param fromKey 返回映射中键的低端点
     * @param fromInclusive {@code true} 如果低端点包含在返回的视图中
     * @param toKey 返回映射中键的高端点
     * @param toInclusive {@code true} 如果高端点包含在返回的视图中
     * @return 此映射中键的范围从 {@code fromKey} 到 {@code toKey} 的部分视图
     * @throws ClassCastException 如果 {@code fromKey} 和 {@code toKey} 无法使用此映射的比较器相互比较（或者，如果映射没有比较器，则使用自然顺序）。
     *         实现可以选择，但不要求，如果 {@code fromKey} 或 {@code toKey} 无法与当前映射中的键进行比较，则抛出此异常。
     * @throws NullPointerException 如果 {@code fromKey} 或 {@code toKey} 为 null 且此映射不允许 null 键
     * @throws IllegalArgumentException 如果 {@code fromKey} 大于 {@code toKey}；或者如果此映射本身具有受限范围，并且 {@code fromKey} 或 {@code toKey} 超出该范围
     */
    NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                             K toKey,   boolean toInclusive);

    /**
     * 返回此映射中键小于（或等于，如果 {@code inclusive} 为 true）{@code toKey} 的部分视图。返回的映射由该映射支持，因此对返回映射的更改会反映在该映射中，反之亦然。返回的映射支持该映射支持的所有可选映射操作。
     *
     * <p>返回的映射将尝试插入超出其范围的键时抛出 {@code IllegalArgumentException}。
     *
     * @param toKey 返回映射中键的高端点
     * @param inclusive {@code true} 如果高端点包含在返回的视图中
     * @return 此映射中键小于（或等于，如果 {@code inclusive} 为 true）{@code toKey} 的部分视图
     * @throws ClassCastException 如果 {@code toKey} 与此映射的比较器不兼容（或者，如果映射没有比较器，则 {@code toKey} 未实现 {@link Comparable}）。
     *         实现可以选择，但不要求，如果 {@code toKey} 无法与当前映射中的键进行比较，则抛出此异常。
     * @throws NullPointerException 如果 {@code toKey} 为 null 且此映射不允许 null 键
     * @throws IllegalArgumentException 如果此映射本身具有受限范围，并且 {@code toKey} 超出该范围
     */
    NavigableMap<K,V> headMap(K toKey, boolean inclusive);

    /**
     * 返回此映射中键大于（或等于，如果 {@code inclusive} 为 true）{@code fromKey} 的部分视图。返回的映射由该映射支持，因此对返回映射的更改会反映在该映射中，反之亦然。返回的映射支持该映射支持的所有可选映射操作。
     *
     * <p>返回的映射将尝试插入超出其范围的键时抛出 {@code IllegalArgumentException}。
     *
     * @param fromKey 返回映射中键的低端点
     * @param inclusive {@code true} 如果低端点包含在返回的视图中
     * @return 此映射中键大于（或等于，如果 {@code inclusive} 为 true）{@code fromKey} 的部分视图
     * @throws ClassCastException 如果 {@code fromKey} 与此映射的比较器不兼容（或者，如果映射没有比较器，则 {@code fromKey} 未实现 {@link Comparable}）。
     *         实现可以选择，但不要求，如果 {@code fromKey} 无法与当前映射中的键进行比较，则抛出此异常。
     * @throws NullPointerException 如果 {@code fromKey} 为 null 且此映射不允许 null 键
     * @throws IllegalArgumentException 如果此映射本身具有受限范围，并且 {@code fromKey} 超出该范围
     */
    NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);

    /**
     * {@inheritDoc}
     *
     * <p>等同于 {@code subMap(fromKey, true, toKey, false)}。
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * {@inheritDoc}
     *
     * <p>等同于 {@code headMap(toKey, false)}。
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> headMap(K toKey);

    /**
     * {@inheritDoc}
     *
     * <p>等同于 {@code tailMap(fromKey, true)}。
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> tailMap(K fromKey);
}
