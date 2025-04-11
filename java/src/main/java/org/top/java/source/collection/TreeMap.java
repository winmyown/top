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

import java.io.Serializable;
// 导入 java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 一个基于红黑树的 {@link java.util.NavigableMap} 实现。
 * 该映射根据键的 {@linkplain Comparable 自然顺序} 排序，或者在创建映射时提供的 {@link Comparator} 进行排序，具体取决于使用的构造函数。
 *
 * <p>此实现保证了 {@code containsKey}、{@code get}、{@code put} 和 {@code remove} 操作的 log(n) 时间复杂度。算法改编自 Cormen、Leiserson 和 Rivest 的《算法导论》。
 *
 * <p>请注意，与任何排序映射一样，树映射维护的顺序，无论是否提供了显式比较器，都必须 <em>与 {@code equals} 一致</em>，如果此排序映射要正确实现 {@code Map} 接口。（有关 <em>与 equals 一致</em> 的准确定义，请参阅 {@code Comparable} 或 {@code Comparator}。）这是因为 {@code Map} 接口是根据 {@code equals} 操作定义的，但排序映射使用其 {@code compareTo}（或 {@code compare}）方法执行所有键比较，因此，从排序映射的角度来看，被此方法视为相等的两个键是相等的。即使排序映射的顺序与 {@code equals} 不一致，其行为 <em>也是</em> 定义良好的；只是它未能遵守 {@code Map} 接口的通用约定。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>
 * 如果多个线程并发访问一个映射，并且至少有一个线程在结构上修改了映射，则必须从外部进行同步。（结构修改是指添加或删除一个或多个映射的任何操作；仅更改与现有键关联的值不是结构修改。）这通常通过同步某个自然封装映射的对象来实现。
 * 如果不存在这样的对象，则应使用 {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap} 方法“包装”映射。最好在创建时执行此操作，以防止意外地非同步访问映射：<pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));</pre>
 *
 * <p>通过此类的所有“集合视图方法”返回的集合的 {@code iterator} 方法返回的迭代器是 <em>快速失败</em> 的：如果在迭代器创建后的任何时候对映射进行结构修改，除了通过迭代器自身的 {@code remove} 方法之外，迭代器将抛出 {@link ConcurrentModificationException}。因此，面对并发修改，迭代器会快速而干净地失败，而不是冒着在未来不确定的时间出现任意、非确定性行为的风险。
 *
 * <p>请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在非同步并发修改的情况下，无法做出任何硬性保证。快速失败迭代器会尽最大努力抛出 {@code ConcurrentModificationException}。因此，编写依赖于此异常的程序是错误的：<em>迭代器的快速失败行为应仅用于检测错误。</em>
 *
 * <p>通过此类及其视图中的方法返回的所有 {@code Map.Entry} 对表示生成时的映射快照。它们 <strong>不</strong> 支持 {@code Entry.setValue} 方法。（但请注意，可以使用 {@code put} 更改关联映射中的映射。）
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @param <K> 此映射维护的键的类型
 * @param <V> 映射值的类型
 *
 * @author  Josh Bloch 和 Doug Lea
 * @see java.util.Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see java.util.Collection
 * @since 1.2
 */

public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, Serializable
{
    /**
     * 用于维护此树映射中顺序的比较器，如果使用键的自然顺序，则为null。
     *
     * @serial
     */
    private final Comparator<? super K> comparator;

    private transient Entry<K,V> root;

    /**
     * 树中的条目数量
     */
    private transient int size = 0;

    /**
     * 树的结构修改次数。
     */
    private transient int modCount = 0;

    /**
     * 构造一个新的、空的树映射，使用键的自然顺序。所有插入映射的键必须实现 {@link Comparable}
     * 接口。此外，所有这些键必须是<em>相互可比较的</em>：对于映射中的任何键 {@code k1} 和 {@code k2}，
     * {@code k1.compareTo(k2)} 不得抛出 {@code ClassCastException}。如果用户尝试将违反此约束的键
     * 放入映射中（例如，用户尝试将字符串键放入键为整数的映射中），则 {@code put(Object key, Object value)}
     * 调用将抛出 {@code ClassCastException}。
     */
    public TreeMap() {
        comparator = null;
    }

    /**
     * 构造一个新的、空的树映射，根据给定的比较器进行排序。插入到映射中的所有键必须通过给定的比较器<em>相互可比较</em>：
     * {@code comparator.compare(k1, k2)} 对于映射中的任何键 {@code k1} 和 {@code k2} 不得抛出
     * {@code ClassCastException}。如果用户尝试将一个违反此约束的键放入映射中，
     * {@code put(Object key, Object value)} 调用将抛出 {@code ClassCastException}。
     *
     * @param comparator 用于对此映射进行排序的比较器。如果为 {@code null}，则将使用键的
     *        {@linkplain Comparable 自然排序}。
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * 构造一个新的树映射，包含与给定映射相同的映射关系，并根据键的<em>自然顺序</em>进行排序。
     * 所有插入到新映射中的键必须实现 {@link Comparable} 接口。此外，所有这些键必须是
     * <em>相互可比较的</em>：对于映射中的任何键 {@code k1} 和 {@code k2}，
     * {@code k1.compareTo(k2)} 不得抛出 {@code ClassCastException}。此方法的时间复杂度为 n*log(n)。
     *
     * @param  m 要将其映射放入此映射中的映射
     * @throws ClassCastException 如果 m 中的键不实现 {@link Comparable} 接口，
     *         或者不是相互可比较的
     * @throws NullPointerException 如果指定的映射为 null
     */
    public TreeMap(java.util.Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }

    /**
     * 构造一个新的树映射，包含与指定排序映射相同的映射，并使用相同的排序顺序。
     * 该方法在线性时间内运行。
     *
     * @param  m 要将其映射放入此映射中的排序映射，并使用其比较器对此映射进行排序
     * @throws NullPointerException 如果指定的映射为 null
     */
    public TreeMap(java.util.SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }


    // 查询操作

    /**
     * 返回此映射中键值对的数量。
     *
     * @return 此映射中键值对的数量
     */
    public int size() {
        return size;
    }

    /**
     * 如果此映射包含指定键的映射关系，则返回 {@code true}。
     *
     * @param key 要测试是否存在于此映射中的键
     * @return 如果此映射包含指定键的映射关系，则返回 {@code true}
     * @throws ClassCastException 如果指定键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定键为 null 且此映射使用自然排序，
     *         或其比较器不允许 null 键
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * 如果此映射将一个或多个键映射到指定值，则返回 {@code true}。更正式地说，当且仅当此映射包含至少一个映射到值 {@code v} 时，返回 {@code true}，使得 {@code (value==null ? v==null : value.equals(v))}。对于大多数实现，此操作可能需要与映射大小成线性关系的时间。
     *
     * @param value 要测试其在此映射中是否存在的值
     * @return 如果存在映射到 {@code value} 的键，则返回 {@code true}；
     *         否则返回 {@code false}
     * @since 1.2
     */
    public boolean containsValue(Object value) {
        for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
            if (valEquals(value, e.value))
                return true;
        return false;
    }

    /**
     * 返回指定键所映射的值，
     * 如果此映射不包含该键的映射关系，则返回 {@code null}。
     *
     * <p>更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射关系，
     * 使得 {@code key} 根据映射的排序与 {@code k} 相等，则此方法返回 {@code v}；
     * 否则返回 {@code null}。
     * （最多只能有一个这样的映射关系。）
     *
     * <p>返回值为 {@code null} 并不<em>必然</em>表示映射中不包含该键的映射关系；
     * 也有可能映射显式地将该键映射为 {@code null}。
     * 可以使用 {@link #containsKey containsKey} 操作来区分这两种情况。
     *
     * @throws ClassCastException 如果指定键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定键为 null，
     *         并且此映射使用自然排序，或其比较器不允许 null 键
     */
    public V get(Object key) {
        Entry<K,V> p = getEntry(key);
        return (p==null ? null : p.value);
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K firstKey() {
        return key(getFirstEntry());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K lastKey() {
        return key(getLastEntry());
    }

    /**
     * 将指定映射中的所有映射关系复制到此映射中。
     * 这些映射关系将替换此映射中当前存在的任何键的映射关系。
     *
     * @param  map 要存储在此映射中的映射关系
     * @throws ClassCastException 如果指定映射中的键或值的类阻止其存储在此映射中
     * @throws NullPointerException 如果指定映射为null，或者
     *         指定映射包含null键且此映射不允许null键
     */
    public void putAll(java.util.Map<? extends K, ? extends V> map) {
        int mapSize = map.size();
        if (size==0 && mapSize!=0 && map instanceof java.util.SortedMap) {
            Comparator<?> c = ((java.util.SortedMap<?,?>)map).comparator();
            if (c == comparator || (c != null && c.equals(comparator))) {
                ++modCount;
                try {
                    buildFromSorted(mapSize, map.entrySet().iterator(),
                                    null, null);
                } catch (java.io.IOException cannotHappen) {
                } catch (ClassNotFoundException cannotHappen) {
                }
                return;
            }
        }
        super.putAll(map);
    }

    /**
     * 返回此映射中指定键的条目，如果映射不包含该键的条目，则返回 {@code null}。
     *
     * @return 此映射中指定键的条目，如果映射不包含该键的条目，则返回 {@code null}
     * @throws ClassCastException 如果指定键无法与映射中的当前键进行比较
     * @throws NullPointerException 如果指定键为 null 且此映射使用自然排序，或其比较器不允许 null 键
     */
    final Entry<K,V> getEntry(Object key) {
        // 为了性能考虑，卸载基于比较器的版本
        if (comparator != null)
            return getEntryUsingComparator(key);
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.key);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else
                return p;
        }
        return null;
    }

    /**
     * 使用比较器的getEntry版本。从getEntry中分离出来
     * 以提高性能。（对于大多数方法来说，这样做并不值得，
     * 因为它们对比较器性能的依赖性较低，但在这里是值得的。）
     */
    final Entry<K,V> getEntryUsingComparator(Object key) {
        @SuppressWarnings("unchecked")
            K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K,V> p = root;
            while (p != null) {
                int cmp = cpr.compare(k, p.key);
                if (cmp < 0)
                    p = p.left;
                else if (cmp > 0)
                    p = p.right;
                else
                    return p;
            }
        }
        return null;
    }

    /**
     * 获取与指定键对应的条目；如果不存在这样的条目，
     * 则返回大于指定键的最小键对应的条目；如果不存在这样的条目
     * （即树中的最大键小于指定键），则返回 {@code null}。
     */
    final Entry<K,V> getCeilingEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null)
                    p = p.left;
                else
                    return p;
            } else if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else
                return p;
        }
        return null;
    }

    /**
     * 获取与指定键对应的条目；如果不存在这样的条目，
     * 则返回小于指定键的最大键对应的条目；如果仍不存在这样的条目，
     * 返回 {@code null}。
     */
    final Entry<K,V> getFloorEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null)
                    p = p.right;
                else
                    return p;
            } else if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else
                return p;

        }
        return null;
    }

    /**
     * 获取大于指定键的最小键对应的条目；如果不存在这样的条目，则返回大于指定键的最小键对应的条目；如果仍不存在这样的条目，则返回 {@code null}。
     */
    final Entry<K,V> getHigherEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null)
                    p = p.left;
                else
                    return p;
            } else {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    /**
     * 返回小于指定键的最大键的条目；如果不存在这样的条目（即树中的最小键大于指定键），则返回 {@code null}。
     */
    final Entry<K,V> getLowerEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null)
                    p = p.right;
                else
                    return p;
            } else {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    /**
     * 将指定值与指定键关联在此映射中。
     * 如果映射之前包含键的映射关系，则旧值将被替换。
     *
     * @param key 与指定值关联的键
     * @param value 与指定键关联的值
     *
     * @return 与 {@code key} 关联的先前值，如果 {@code key} 没有映射关系，则返回 {@code null}。
     *         （返回 {@code null} 也可能表示之前将 {@code null} 与 {@code key} 关联。）
     * @throws ClassCastException 如果指定键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定键为 null，并且此映射使用自然排序，或其比较器不允许 null 键
     */
    public V put(K key, V value) {
        Entry<K,V> t = root;
        if (t == null) {
            compare(key, key); // 类型（可能为空）检查

            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K,V> parent;
        // 分割比较器和可比较路径
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        else {
            if (key == null)
                throw new NullPointerException();
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * 如果存在，则从此 TreeMap 中移除指定键的映射。
     *
     * @param key 要移除映射的键
     * @return 与 {@code key} 关联的先前值，如果 {@code key} 没有映射，则返回 {@code null}。
     *         （返回 {@code null} 也可能表示此映射先前将 {@code null} 与 {@code key} 关联。）
     * @throws ClassCastException 如果指定的键无法与当前映射中的键进行比较
     * @throws NullPointerException 如果指定的键为 null，并且此映射使用自然排序，或者其比较器不允许 null 键
     */
    public V remove(Object key) {
        Entry<K,V> p = getEntry(key);
        if (p == null)
            return null;

        V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    /**
     * 从此映射中移除所有映射关系。
     * 此调用返回后，映射将为空。
     */
    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }

    /**
     * 返回此 {@code TreeMap} 实例的浅拷贝。（键和值本身不会被克隆。）
     *
     * @return 此映射的浅拷贝
     */
    public Object clone() {
        TreeMap<?,?> clone;
        try {
            clone = (TreeMap<?,?>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        // 将克隆置于“原始”状态（除了比较器）
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        clone.entrySet = null;
        clone.navigableKeySet = null;
        clone.descendingMap = null;

        // 使用我们的映射初始化克隆
        try {
            clone.buildFromSorted(size, entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }

        return clone;
    }

    // NavigableMap API methods

    /**
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    /**
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> lastEntry() {
        return exportEntry(getLastEntry());
    }

    /**
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> pollFirstEntry() {
        Entry<K,V> p = getFirstEntry();
        java.util.Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            deleteEntry(p);
        return result;
    }

    /**
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> pollLastEntry() {
        Entry<K,V> p = getLastEntry();
        java.util.Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            deleteEntry(p);
        return result;
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public java.util.Map.Entry<K,V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException 如果指定的键为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @since 1.6
     */
    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }

    // 视图

    /**
     * 字段在首次请求该视图时初始化为包含条目集视图的实例。
     * 视图是无状态的，因此没有理由创建多个实例。
     */
    private transient EntrySet entrySet;
    private transient KeySet<K> navigableKeySet;
    private transient java.util.NavigableMap<K,V> descendingMap;

    /**
     * 返回此映射中包含的键的 {@link java.util.Set} 视图。
     *
     * <p>该集合的迭代器按升序返回键。
     * 该集合的 spliterator 是
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em>、
     * <em>快速失败</em>，并且还报告 {@link Spliterator#SORTED}
     * 和 {@link Spliterator#ORDERED}，其遇到顺序为升序键顺序。
     * 如果树映射的比较器（参见 {@link #comparator()}）为 {@code null}，
     * 则 spliterator 的比较器（参见 {@link Spliterator#getComparator()}）为 {@code null}。
     * 否则，spliterator 的比较器与树映射的比较器相同或强加相同的总顺序。
     *
     * <p>该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。
     * 如果在集合的迭代过程中修改了映射（除了通过迭代器自己的 {@code remove} 操作），
     * 则迭代的结果是未定义的。
     * 该集合支持元素移除，通过 {@code Iterator.remove}、{@code Set.remove}、
     * {@code removeAll}、{@code retainAll} 和 {@code clear} 操作从映射中移除相应的映射。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    public java.util.Set<K> keySet() {
        return navigableKeySet();
    }

    /**
     * @since 1.6
     */
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> nks = navigableKeySet;
        return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
    }

    /**
     * @since 1.6
     */
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * 返回此映射中包含的值的 {@link java.util.Collection} 视图。
     *
     * <p>集合的迭代器按对应键的升序返回值。集合的 spliterator 是
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em>的，
     * <em>快速失败</em>的，并且还报告 {@link Spliterator#ORDERED}，
     * 其遍历顺序是对应键的升序。
     *
     * <p>该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在
     * 对集合进行迭代的过程中修改了映射（除了通过迭代器自身的 {@code remove} 操作），
     * 则迭代的结果是未定义的。集合支持元素移除，通过 {@code Iterator.remove}、
     * {@code Collection.remove}、{@code removeAll}、{@code retainAll} 和
     * {@code clear} 操作从映射中移除对应的映射关系。它不支持 {@code add} 或
     * {@code addAll} 操作。
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    /**
     * 返回此映射中包含的映射关系的 {@link java.util.Set} 视图。
     *
     * <p>该集合的迭代器按升序键顺序返回条目。集合的分割迭代器是
     * <em><a href="Spliterator.html#binding">延迟绑定</a></em>的，
     * <em>快速失败</em>的，并且额外报告 {@link Spliterator#SORTED} 和
     * {@link Spliterator#ORDERED}，其遍历顺序为升序键顺序。
     *
     * <p>该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在集合上进行迭代时修改了映射（除非通过迭代器自身的 {@code remove} 操作，或通过迭代器返回的映射条目的 {@code setValue} 操作），则迭代的结果是未定义的。该集合支持元素移除，通过 {@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 操作从映射中移除相应的映射关系。它不支持 {@code add} 或 {@code addAll} 操作。
     */
    public java.util.Set<java.util.Map.Entry<K,V>> entrySet() {
        EntrySet es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    /**
     * @since 1.6
     */
    public java.util.NavigableMap<K, V> descendingMap() {
        java.util.NavigableMap<K, V> km = descendingMap;
        return (km != null) ? km :
            (descendingMap = new DescendingSubMap<>(this,
                                                    true, null, true,
                                                    true, null, true));
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromKey} 或 {@code toKey} 为
     *         null 并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public java.util.NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                              K toKey, boolean toInclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, fromInclusive,
                                     false, toKey,   toInclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code toKey} 为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public java.util.NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     true,  null,  true,
                                     false, toKey, inclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromKey} 为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public java.util.NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, inclusive,
                                     true,  null,    true);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromKey} 或 {@code toKey} 为
     *         null 并且此映射使用自然排序，或者其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public java.util.SortedMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code toKey} 为 null
     *         且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public java.util.SortedMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException 如果 {@code fromKey} 为 null
     *         并且此映射使用自然排序，或其比较器
     *         不允许 null 键
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public java.util.SortedMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Entry<K,V> p = getEntry(key);
        if (p!=null && Objects.equals(oldValue, p.value)) {
            p.value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Entry<K,V> p = getEntry(key);
        if (p!=null) {
            V oldValue = p.value;
            p.value = value;
            return oldValue;
        }
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            e.value = function.apply(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // 视图类支持

    class Values extends AbstractCollection<V> {
        public java.util.Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        public int size() {
            return TreeMap.this.size();
        }

        public boolean contains(Object o) {
            return TreeMap.this.containsValue(o);
        }

        public boolean remove(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (valEquals(e.getValue(), o)) {
                    deleteEntry(e);
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            TreeMap.this.clear();
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    class EntrySet extends java.util.AbstractSet<java.util.Map.Entry<K,V>> {
        public java.util.Iterator<java.util.Map.Entry<K,V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        public boolean contains(Object o) {
            if (!(o instanceof java.util.Map.Entry))
                return false;
            java.util.Map.Entry<?,?> entry = (java.util.Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            return p != null && valEquals(p.getValue(), value);
        }

        public boolean remove(Object o) {
            if (!(o instanceof java.util.Map.Entry))
                return false;
            java.util.Map.Entry<?,?> entry = (java.util.Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            if (p != null && valEquals(p.getValue(), value)) {
                deleteEntry(p);
                return true;
            }
            return false;
        }

        public int size() {
            return TreeMap.this.size();
        }

        public void clear() {
            TreeMap.this.clear();
        }

        public Spliterator<java.util.Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    /*
     * 与Values和EntrySet不同，KeySet类是静态的，
     * 委托给NavigableMap以允许SubMaps使用，
     * 这比需要在以下Iterator方法中进行类型测试的丑陋性更为重要，
     * 这些方法在主类和子映射类中适当地定义。
     */

    java.util.Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    java.util.Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    static final class KeySet<E> extends java.util.AbstractSet<E> implements NavigableSet<E> {
        private final java.util.NavigableMap<E, ?> m;
        KeySet(java.util.NavigableMap<E,?> map) { m = map; }

        public java.util.Iterator<E> iterator() {
            if (m instanceof TreeMap)
                return ((TreeMap<E,?>)m).keyIterator();
            else
                return ((NavigableSubMap<E,?>)m).keyIterator();
        }

        public java.util.Iterator<E> descendingIterator() {
            if (m instanceof TreeMap)
                return ((TreeMap<E,?>)m).descendingKeyIterator();
            else
                return ((NavigableSubMap<E,?>)m).descendingKeyIterator();
        }

        public int size() { return m.size(); }
        public boolean isEmpty() { return m.isEmpty(); }
        public boolean contains(Object o) { return m.containsKey(o); }
        public void clear() { m.clear(); }
        public E lower(E e) { return m.lowerKey(e); }
        public E floor(E e) { return m.floorKey(e); }
        public E ceiling(E e) { return m.ceilingKey(e); }
        public E higher(E e) { return m.higherKey(e); }
        public E first() { return m.firstKey(); }
        public E last() { return m.lastKey(); }
        public Comparator<? super E> comparator() { return m.comparator(); }
        public E pollFirst() {
            java.util.Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        public E pollLast() {
            java.util.Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        public boolean remove(Object o) {
            int oldSize = size();
            m.remove(o);
            return size() != oldSize;
        }
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                      E toElement,   boolean toInclusive) {
            return new KeySet<>(m.subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive));
        }
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<>(m.headMap(toElement, inclusive));
        }
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<>(m.tailMap(fromElement, inclusive));
        }
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        public NavigableSet<E> descendingSet() {
            return new KeySet<>(m.descendingMap());
        }

        public Spliterator<E> spliterator() {
            return keySpliteratorFor(m);
        }
    }

    /**
     * TreeMap迭代器的基类
     */
    abstract class PrivateEntryIterator<T> implements java.util.Iterator<T> {
        Entry<K,V> next;
        Entry<K,V> lastReturned;
        int expectedModCount;

        PrivateEntryIterator(Entry<K,V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = successor(e);
            lastReturned = e;
            return e;
        }

        final Entry<K,V> prevEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = predecessor(e);
            lastReturned = e;
            return e;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            // 已删除的条目被其后继者替换
            if (lastReturned.left != null && lastReturned.right != null)
                next = lastReturned;
            deleteEntry(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    final class EntryIterator extends PrivateEntryIterator<java.util.Map.Entry<K,V>> {
        EntryIterator(Entry<K,V> first) {
            super(first);
        }
        public java.util.Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends PrivateEntryIterator<V> {
        ValueIterator(Entry<K,V> first) {
            super(first);
        }
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends PrivateEntryIterator<K> {
        KeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return nextEntry().key;
        }
    }

    final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return prevEntry().key;
        }
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            deleteEntry(lastReturned);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    // 小工具

    /**
     * 使用此 TreeMap 的正确比较方法比较两个键。
     */
    @SuppressWarnings("unchecked")
    final int compare(Object k1, Object k2) {
        return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
            : comparator.compare((K)k1, (K)k2);
    }

    /**
     * 测试两个值是否相等。与 o1.equals(o2) 的不同之处仅在于
     * 它能正确处理 {@code null} 的 o1。
     */
    static final boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    /**
     * 返回条目的SimpleImmutableEntry，如果为null则返回null
     */
    static <K,V> java.util.Map.Entry<K,V> exportEntry(Entry<K,V> e) {
        return (e == null) ? null :
            new SimpleImmutableEntry<>(e);
    }

    /**
     * 返回条目的键，如果为null则返回null
     */
    static <K,V> K keyOrNull(Entry<K,V> e) {
        return (e == null) ? null : e.key;
    }

    /**
     * 返回与指定Entry对应的键。
     * @throws NoSuchElementException 如果Entry为null
     */
    static <K> K key(Entry<K,?> e) {
        if (e==null)
            throw new NoSuchElementException();
        return e.key;
    }


    // 子地图

    /**
     * 用作无边界SubMapIterators的无法匹配的栅栏键的虚拟值
     */
    private static final Object UNBOUNDED = new Object();

    /**
     * @serial 包含
     */
    abstract static class NavigableSubMap<K,V> extends java.util.AbstractMap<K,V>
        implements java.util.NavigableMap<K,V>, Serializable {
        private static final long serialVersionUID = -2102997345730753016L;
        /**
         * 后备映射。
         */
        final TreeMap<K,V> m;

        /**
         * 端点表示为三元组 (fromStart, lo, loInclusive) 和 (toEnd, hi, hiInclusive)。如果 fromStart 为 true，
         * 则低（绝对）边界是后备映射的起始位置，其他值将被忽略。否则，如果 loInclusive 为 true，则 lo 是包含边界，
         * 否则 lo 是排除边界。对于上界同理。
         */
        final K lo, hi;
        final boolean fromStart, toEnd;
        final boolean loInclusive, hiInclusive;

        NavigableSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (m.compare(lo, hi) > 0)
                    throw new IllegalArgumentException("fromKey > toKey");
            } else {
                if (!fromStart) // 类型检查
                    m.compare(lo, lo);
                if (!toEnd)
                    m.compare(hi, hi);
            }

            this.m = m;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        // 内部工具

        final boolean tooLow(Object key) {
            if (!fromStart) {
                int c = m.compare(key, lo);
                if (c < 0 || (c == 0 && !loInclusive))
                    return true;
            }
            return false;
        }

        final boolean tooHigh(Object key) {
            if (!toEnd) {
                int c = m.compare(key, hi);
                if (c > 0 || (c == 0 && !hiInclusive))
                    return true;
            }
            return false;
        }

        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        final boolean inClosedRange(Object key) {
            return (fromStart || m.compare(key, lo) >= 0)
                && (toEnd || m.compare(hi, key) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /*
         * 关系操作的绝对版本。
         * 子类使用同名的“sub”版本映射到这些操作，
         * 这些版本会反转降序映射的感知
         */

        final TreeMap.Entry<K,V> absLowest() {
            TreeMap.Entry<K,V> e =
                (fromStart ?  m.getFirstEntry() :
                 (loInclusive ? m.getCeilingEntry(lo) :
                                m.getHigherEntry(lo)));
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absHighest() {
            TreeMap.Entry<K,V> e =
                (toEnd ?  m.getLastEntry() :
                 (hiInclusive ?  m.getFloorEntry(hi) :
                                 m.getLowerEntry(hi)));
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absCeiling(K key) {
            if (tooLow(key))
                return absLowest();
            TreeMap.Entry<K,V> e = m.getCeilingEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absHigher(K key) {
            if (tooLow(key))
                return absLowest();
            TreeMap.Entry<K,V> e = m.getHigherEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absFloor(K key) {
            if (tooHigh(key))
                return absHighest();
            TreeMap.Entry<K,V> e = m.getFloorEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absLower(K key) {
            if (tooHigh(key))
                return absHighest();
            TreeMap.Entry<K,V> e = m.getLowerEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /** 返回用于升序遍历的绝对高围栏 */
        final TreeMap.Entry<K,V> absHighFence() {
            return (toEnd ? null : (hiInclusive ?
                                    m.getHigherEntry(hi) :
                                    m.getCeilingEntry(hi)));
        }

        /** 返回用于降序遍历的绝对低围栏 */
        final TreeMap.Entry<K,V> absLowFence() {
            return (fromStart ? null : (loInclusive ?
                                        m.getLowerEntry(lo) :
                                        m.getFloorEntry(lo)));
        }

        // 在升序与降序类中定义的抽象方法
        // 这些中继到相应的绝对版本

        abstract TreeMap.Entry<K,V> subLowest();
        abstract TreeMap.Entry<K,V> subHighest();
        abstract TreeMap.Entry<K,V> subCeiling(K key);
        abstract TreeMap.Entry<K,V> subHigher(K key);
        abstract TreeMap.Entry<K,V> subFloor(K key);
        abstract TreeMap.Entry<K,V> subLower(K key);

        /** 返回从此子映射的角度升序迭代器 */
        abstract java.util.Iterator<K> keyIterator();

        abstract Spliterator<K> keySpliterator();

        /** 返回从该子映射的角度来看的降序迭代器 */
        abstract java.util.Iterator<K> descendingKeyIterator();

        // 公共方法

        public boolean isEmpty() {
            return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
        }

        public int size() {
            return (fromStart && toEnd) ? m.size() : entrySet().size();
        }

        public final boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        public final V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return m.put(key, value);
        }

        public final V get(Object key) {
            return !inRange(key) ? null :  m.get(key);
        }

        public final V remove(Object key) {
            return !inRange(key) ? null : m.remove(key);
        }

        public final Entry<K,V> ceilingEntry(K key) {
            return exportEntry(subCeiling(key));
        }

        public final K ceilingKey(K key) {
            return keyOrNull(subCeiling(key));
        }

        public final Entry<K,V> higherEntry(K key) {
            return exportEntry(subHigher(key));
        }

        public final K higherKey(K key) {
            return keyOrNull(subHigher(key));
        }

        public final Entry<K,V> floorEntry(K key) {
            return exportEntry(subFloor(key));
        }

        public final K floorKey(K key) {
            return keyOrNull(subFloor(key));
        }

        public final Entry<K,V> lowerEntry(K key) {
            return exportEntry(subLower(key));
        }

        public final K lowerKey(K key) {
            return keyOrNull(subLower(key));
        }

        public final K firstKey() {
            return key(subLowest());
        }

        public final K lastKey() {
            return key(subHighest());
        }

        public final Entry<K,V> firstEntry() {
            return exportEntry(subLowest());
        }

        public final Entry<K,V> lastEntry() {
            return exportEntry(subHighest());
        }

        public final Entry<K,V> pollFirstEntry() {
            TreeMap.Entry<K,V> e = subLowest();
            Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.deleteEntry(e);
            return result;
        }

        public final Entry<K,V> pollLastEntry() {
            TreeMap.Entry<K,V> e = subHighest();
            Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.deleteEntry(e);
            return result;
        }

        // 视图
        transient java.util.NavigableMap<K,V> descendingMapView;
        transient EntrySetView entrySetView;
        transient KeySet<K> navigableKeySetView;

        public final NavigableSet<K> navigableKeySet() {
            KeySet<K> nksv = navigableKeySetView;
            return (nksv != null) ? nksv :
                (navigableKeySetView = new KeySet<>(this));
        }

        public final java.util.Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public final java.util.SortedMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public final java.util.SortedMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public final java.util.SortedMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        // 查看类

        abstract class EntrySetView extends AbstractSet<Entry<K,V>> {
            private transient int size = -1, sizeModCount;

            public int size() {
                if (fromStart && toEnd)
                    return m.size();
                if (size == -1 || sizeModCount != m.modCount) {
                    sizeModCount = m.modCount;
                    size = 0;
                    java.util.Iterator<?> i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            public boolean isEmpty() {
                TreeMap.Entry<K,V> n = absLowest();
                return n == null || tooHigh(n.key);
            }

            public boolean contains(Object o) {
                if (!(o instanceof java.util.Map.Entry))
                    return false;
                Entry<?,?> entry = (Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                TreeMap.Entry<?,?> node = m.getEntry(key);
                return node != null &&
                    valEquals(node.getValue(), entry.getValue());
            }

            public boolean remove(Object o) {
                if (!(o instanceof java.util.Map.Entry))
                    return false;
                Entry<?,?> entry = (Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                TreeMap.Entry<K,V> node = m.getEntry(key);
                if (node!=null && valEquals(node.getValue(),
                                            entry.getValue())) {
                    m.deleteEntry(node);
                    return true;
                }
                return false;
            }
        }

        /**
         * 子映射的迭代器
         */
        abstract class SubMapIterator<T> implements java.util.Iterator<T> {
            TreeMap.Entry<K,V> lastReturned;
            TreeMap.Entry<K,V> next;
            final Object fenceKey;
            int expectedModCount;

            SubMapIterator(TreeMap.Entry<K,V> first,
                           TreeMap.Entry<K,V> fence) {
                expectedModCount = m.modCount;
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? UNBOUNDED : fence.key;
            }

            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            final TreeMap.Entry<K,V> nextEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = successor(e);
                lastReturned = e;
                return e;
            }

            final TreeMap.Entry<K,V> prevEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = predecessor(e);
                lastReturned = e;
                return e;
            }

            final void removeAscending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                // 已删除的条目被其后继者替换
                if (lastReturned.left != null && lastReturned.right != null)
                    next = lastReturned;
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

            final void removeDescending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

        }

        final class SubMapEntryIterator extends SubMapIterator<Entry<K,V>> {
            SubMapEntryIterator(TreeMap.Entry<K,V> first,
                                TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public Entry<K,V> next() {
                return nextEntry();
            }
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends SubMapIterator<Entry<K,V>> {
            DescendingSubMapEntryIterator(TreeMap.Entry<K,V> last,
                                          TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }

            public Entry<K,V> next() {
                return prevEntry();
            }
            public void remove() {
                removeDescending();
            }
        }

        // 作为 KeySpliterator 的备用实现最小化的 Spliterator
        final class SubMapKeyIterator extends SubMapIterator<K>
            implements Spliterator<K> {
            SubMapKeyIterator(TreeMap.Entry<K,V> first,
                              TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public K next() {
                return nextEntry().key;
            }
            public void remove() {
                removeAscending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                    Spliterator.SORTED;
            }
            public final Comparator<? super K>  getComparator() {
                return NavigableSubMap.this.comparator();
            }
        }

        final class DescendingSubMapKeyIterator extends SubMapIterator<K>
            implements Spliterator<K> {
            DescendingSubMapKeyIterator(TreeMap.Entry<K,V> last,
                                        TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }
            public K next() {
                return prevEntry().key;
            }
            public void remove() {
                removeDescending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        }
    }

    /**
     * @serial 包含
     */
    static final class AscendingSubMap<K,V> extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        public java.util.NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                                  K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                                         false, fromKey, fromInclusive,
                                         false, toKey,   toInclusive);
        }

        public java.util.NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                                         fromStart, lo,    loInclusive,
                                         false,     toKey, inclusive);
        }

        public java.util.NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m,
                                         false, fromKey, inclusive,
                                         toEnd, hi,      hiInclusive);
        }

        public java.util.NavigableMap<K,V> descendingMap() {
            java.util.NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new DescendingSubMap<>(m,
                                        fromStart, lo, loInclusive,
                                        toEnd,     hi, hiInclusive));
        }

        java.util.Iterator<K> keyIterator() {
            return new TreeMap.NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        Spliterator<K> keySpliterator() {
            return new TreeMap.NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        java.util.Iterator<K> descendingKeyIterator() {
            return new TreeMap.NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends TreeMap.NavigableSubMap.EntrySetView {
            public Iterator<Entry> iterator() {
                return new TreeMap.NavigableSubMap.SubMapEntryIterator(absLowest(), absHighFence());
            }
        }

        public java.util.Set<Entry<K,V>> entrySet() {
            TreeMap.NavigableSubMap.EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
        }

        TreeMap.Entry<K,V> subLowest()       { return absLowest(); }
        TreeMap.Entry<K,V> subHighest()      { return absHighest(); }
        TreeMap.Entry<K,V> subCeiling(K key) { return absCeiling(key); }
        TreeMap.Entry<K,V> subHigher(K key)  { return absHigher(key); }
        TreeMap.Entry<K,V> subFloor(K key)   { return absFloor(key); }
        TreeMap.Entry<K,V> subLower(K key)   { return absLower(key); }
    }

    /**
     * @serial 包含
     */
    static final class DescendingSubMap<K,V>  extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866120460L;
        DescendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        private final Comparator<? super K> reverseComparator =
            Collections.reverseOrder(m.comparator);

        public Comparator<? super K> comparator() {
            return reverseComparator;
        }

        public java.util.NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                                  K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                                          false, toKey,   toInclusive,
                                          false, fromKey, fromInclusive);
        }

        public java.util.NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                                          false, toKey, inclusive,
                                          toEnd, hi,    hiInclusive);
        }

        public java.util.NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m,
                                          fromStart, lo, loInclusive,
                                          false, fromKey, inclusive);
        }

        public java.util.NavigableMap<K,V> descendingMap() {
            java.util.NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new AscendingSubMap<>(m,
                                       fromStart, lo, loInclusive,
                                       toEnd,     hi, hiInclusive));
        }

        java.util.Iterator<K> keyIterator() {
            return new TreeMap.NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        Spliterator<K> keySpliterator() {
            return new TreeMap.NavigableSubMap.DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        java.util.Iterator<K> descendingKeyIterator() {
            return new TreeMap.NavigableSubMap.SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends TreeMap.NavigableSubMap.EntrySetView {
            public Iterator<Entry> iterator() {
                return new TreeMap.NavigableSubMap.DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }

        public java.util.Set<Entry<K,V>> entrySet() {
            TreeMap.NavigableSubMap.EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
        }

        TreeMap.Entry<K,V> subLowest()       { return absHighest(); }
        TreeMap.Entry<K,V> subHighest()      { return absLowest(); }
        TreeMap.Entry<K,V> subCeiling(K key) { return absFloor(key); }
        TreeMap.Entry<K,V> subHigher(K key)  { return absLower(key); }
        TreeMap.Entry<K,V> subFloor(K key)   { return absCeiling(key); }
        TreeMap.Entry<K,V> subLower(K key)   { return absHigher(key); }
    }

    /**
     * 这个类仅为了与之前版本的TreeMap序列化兼容而存在，
     * 之前的版本不支持NavigableMap。它将旧版本的SubMap
     * 转换为新版本的AscendingSubMap。这个类在其他情况下
     * 不会被使用。
     *
     * @serial include
     */
    private class SubMap extends AbstractMap<K,V>
        implements java.util.SortedMap<K,V>, Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        private boolean fromStart = false, toEnd = false;
        private K fromKey, toKey;
        private Object readResolve() {
            return new AscendingSubMap<>(TreeMap.this,
                                         fromStart, fromKey, true,
                                         toEnd, toKey, false);
        }
        public Set<Entry<K,V>> entrySet() { throw new InternalError(); }
        public K lastKey() { throw new InternalError(); }
        public K firstKey() { throw new InternalError(); }
        public java.util.SortedMap<K,V> subMap(K fromKey, K toKey) { throw new InternalError(); }
        public java.util.SortedMap<K,V> headMap(K toKey) { throw new InternalError(); }
        public SortedMap<K,V> tailMap(K fromKey) { throw new InternalError(); }
        public Comparator<? super K> comparator() { throw new InternalError(); }
    }


    // 红黑树机制

    private static final boolean RED   = false;
    private static final boolean BLACK = true;

    /**
     * 树中的节点。同时作为向用户传递键值对的一种方式（参见 Map.Entry）。
     */

    static final class Entry<K,V> implements java.util.Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left;
        Entry<K,V> right;
        Entry<K,V> parent;
        boolean color = BLACK;

        /**
         * 创建一个具有给定键、值和父节点的新节点，并且
         * 子链接为 {@code null}，颜色为黑色。
         */
        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * 返回键值。
         *
         * @return 键值
         */
        public K getKey() {
            return key;
        }

        /**
         * 返回与键关联的值。
         *
         * @return 与键关联的值
         */
        public V getValue() {
            return value;
        }

        /**
         * 用给定的值替换当前与键关联的值。
         *
         * @return 调用此方法前与键关联的值
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof java.util.Map.Entry))
                return false;
            java.util.Map.Entry<?,?> e = (java.util.Map.Entry<?,?>)o;

            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * 返回TreeMap中的第一个Entry（根据TreeMap的键排序函数）。如果TreeMap为空，则返回null。
     */
    final Entry<K,V> getFirstEntry() {
        Entry<K,V> p = root;
        if (p != null)
            while (p.left != null)
                p = p.left;
        return p;
    }

    /**
     * 返回 TreeMap 中的最后一个 Entry（根据 TreeMap 的键排序函数）。如果 TreeMap 为空，则返回 null。
     */
    final Entry<K,V> getLastEntry() {
        Entry<K,V> p = root;
        if (p != null)
            while (p.right != null)
                p = p.right;
        return p;
    }

    /**
     * 返回指定Entry的后继节点，如果没有则返回null。
     */
    static <K,V> Entry<K,V> successor(Entry<K,V> t) {
        if (t == null)
            return null;
        else if (t.right != null) {
            Entry<K,V> p = t.right;
            while (p.left != null)
                p = p.left;
            return p;
        } else {
            Entry<K,V> p = t.parent;
            Entry<K,V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * 返回指定Entry的前驱节点，如果不存在则返回null。
     */
    static <K,V> Entry<K,V> predecessor(Entry<K,V> t) {
        if (t == null)
            return null;
        else if (t.left != null) {
            Entry<K,V> p = t.left;
            while (p.right != null)
                p = p.right;
            return p;
        } else {
            Entry<K,V> p = t.parent;
            Entry<K,V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * 平衡操作。
     *
     * 在插入和删除期间重新平衡的实现与CLR版本略有不同。我们没有使用虚拟的nil节点，而是使用一组能够正确处理null的访问器。它们用于避免在主算法中围绕null检查的混乱。
     */

    private static <K,V> boolean colorOf(Entry<K,V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
        return (p == null ? null: p.parent);
    }

    private static <K,V> void setColor(Entry<K,V> p, boolean c) {
        if (p != null)
            p.color = c;
    }

    private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
        return (p == null) ? null: p.left;
    }

    private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
        return (p == null) ? null: p.right;
    }

    /** 来自CLR */
    private void rotateLeft(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> r = p.right;
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    /** 来自CLR */
    private void rotateRight(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> l = p.left;
            p.left = l.right;
            if (l.right != null) l.right.parent = p;
            l.parent = p.parent;
            if (p.parent == null)
                root = l;
            else if (p.parent.right == p)
                p.parent.right = l;
            else p.parent.left = l;
            l.right = p;
            p.parent = l;
        }
    }

    /** 来自CLR */
    private void fixAfterInsertion(Entry<K,V> x) {
        x.color = RED;

        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * 删除节点 p，然后重新平衡树。
     */
    private void deleteEntry(Entry<K,V> p) {
        modCount++;
        size--;

        // 如果是严格内部节点，将后继节点的元素复制到p，然后使p
        // 指向后继节点。
        if (p.left != null && p.right != null) {
            Entry<K,V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p 有 2 个子节点

        // 如果替换节点存在，则开始修复。
        Entry<K,V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // 链接替换到父级
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left  = replacement;
            else
                p.parent.right = replacement;

            // 将链接置空，以便fixAfterDeletion可以安全地使用它们。
            p.left = p.right = p.parent = null;

            // 修复替换
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        } else if (p.parent == null) { // 如果我们是唯一的节点，则返回。
            root = null;
        } else { // 没有子节点。使用自身作为幻影替换并取消链接。
            if (p.color == BLACK)
                fixAfterDeletion(p);

            if (p.parent != null) {
                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }

    /** 来自CLR */
    private void fixAfterDeletion(Entry<K,V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<K,V> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib))  == BLACK &&
                    colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // 对称
                Entry<K,V> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK &&
                    colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
    }

    private static final long serialVersionUID = 919286545866124006L;

    /**
     * 将 {@code TreeMap} 实例的状态保存到流中（即，序列化它）。
     *
     * @serialData 首先发出 TreeMap 的 <em>大小</em>（键值对的数量）（int），
     *             然后是每个键值对的键（Object）和值（Object）。键值对按照键的顺序
     *             发出（由 TreeMap 的 Comparator 决定，如果 TreeMap 没有 Comparator，
     *             则按照键的自然顺序）。
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // 写出Comparator和任何隐藏的东西
        s.defaultWriteObject();

        // 写出大小（映射数量）
        s.writeInt(size);

        // 交替写出键和值
        for (java.util.Iterator<java.util.Map.Entry<K,V>> i = entrySet().iterator(); i.hasNext(); ) {
            java.util.Map.Entry<K,V> e = i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * 从流中重建 {@code TreeMap} 实例（即，反序列化它）。
     */
    private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // 读取Comparator和任何隐藏的内容
        s.defaultReadObject();

        // 读取大小
        int size = s.readInt();

        buildFromSorted(size, null, s, null);
    }

    /** 仅用于从 TreeSet.readObject 调用 */
    void readTreeSet(int size, java.io.ObjectInputStream s, V defaultVal)
        throws java.io.IOException, ClassNotFoundException {
        buildFromSorted(size, null, s, defaultVal);
    }

    /** 仅用于从 TreeSet.addAll 调用 */
    void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
        try {
            buildFromSorted(set.size(), set.iterator(), null, defaultVal);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }


    /**
     * 从排序数据构建树的线性时间算法。可以接受来自迭代器或流的键和/或值。这导致了
     * 过多的参数，但似乎比其他替代方案更好。该方法接受的四种格式为：
     *
     *    1) 一个Map.Entry的迭代器。      (it != null, defaultVal == null).
     *    2) 一个键的迭代器。            (it != null, defaultVal != null).
     *    3) 交替序列化键和值的流。
     *                                   (it == null, defaultVal == null).
     *    4) 序列化键的流。              (it == null, defaultVal != null).
     *
     * 假设在调用此方法之前已经设置了TreeMap的比较器。
     *
     * @param size 要从迭代器或流中读取的键（或键值对）的数量
     * @param it 如果非空，则从此迭代器读取的条目或键创建新条目。
     * @param str 如果非空，则从此流中读取的序列化形式的键和可能的值创建新条目。
     *        it和str中应恰好有一个非空。
     * @param defaultVal 如果非空，则此默认值用于映射中的每个值。如果为空，则从
     *        迭代器或流中读取每个值，如上所述。
     * @throws java.io.IOException 从流读取传播的异常。如果str为空，则不会发生。
     * @throws ClassNotFoundException 从readObject传播的异常。
     *         如果str为空，则不会发生。
     */
    private void buildFromSorted(int size, java.util.Iterator<?> it,
                                 java.io.ObjectInputStream str,
                                 V defaultVal)
        throws  java.io.IOException, ClassNotFoundException {
        this.size = size;
        root = buildFromSorted(0, 0, size-1, computeRedLevel(size),
                               it, str, defaultVal);
    }

    /**
     * 递归的“辅助方法”，执行前一个方法的实际工作。同名参数具有相同的定义。
     * 其他参数在下面有详细说明。假设在调用此方法之前，TreeMap 的比较器和大小字段已经设置。
     * （此方法忽略这两个字段。）
     *
     * @param level 树的当前层级。初始调用应为 0。
     * @param lo 此子树的第一个元素索引。初始应为 0。
     * @param hi 此子树的最后一个元素索引。初始应为 size-1。
     * @param redLevel 节点应为红色的层级。必须等于此大小的树的 computeRedLevel。
     */
    @SuppressWarnings("unchecked")
    private final Entry<K,V> buildFromSorted(int level, int lo, int hi,
                                             int redLevel,
                                             Iterator<?> it,
                                             java.io.ObjectInputStream str,
                                             V defaultVal)
        throws  java.io.IOException, ClassNotFoundException {
        /*
         * 策略：根节点是中间的元素。要获取它，我们必须首先递归地构建整个左子树，
         * 以便获取其所有元素。然后我们可以继续构建右子树。
         *
         * lo 和 hi 参数是当前子树要从迭代器或流中提取的最小和最大索引。
         * 它们实际上并没有被索引，我们只是按顺序进行，
         * 确保元素以相应的顺序被提取。
         */

        if (hi < lo) return null;

        int mid = (lo + hi) >>> 1;

        Entry<K,V> left  = null;
        if (lo < mid)
            left = buildFromSorted(level+1, lo, mid - 1, redLevel,
                                   it, str, defaultVal);

        // 从迭代器或流中提取键和/或值
        K key;
        V value;
        if (it != null) {
            if (defaultVal==null) {
                java.util.Map.Entry<?,?> entry = (java.util.Map.Entry<?,?>)it.next();
                key = (K)entry.getKey();
                value = (V)entry.getValue();
            } else {
                key = (K)it.next();
                value = defaultVal;
            }
        } else { // 使用流
            key = (K) str.readObject();
            value = (defaultVal != null ? defaultVal : (V) str.readObject());
        }

        Entry<K,V> middle =  new Entry<>(key, value, null);

        // 将非满的最底层节点着色为红色
        if (level == redLevel)
            middle.color = RED;

        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }

        if (mid < hi) {
            Entry<K,V> right = buildFromSorted(level+1, mid+1, hi, redLevel,
                                               it, str, defaultVal);
            middle.right = right;
            right.parent = middle;
        }

        return middle;
    }

    /**
     * 找到将所有节点分配为黑色的层级。这是由buildTree生成的完全二叉树的最后一个“完整”层级。
     * 剩余的节点被分配为红色。（这为未来的插入提供了一个“良好”的颜色分配。）该层级数通过计算到达第零个节点所需的拆分次数来确定。
     * （答案约为lg(N)，但在任何情况下都必须通过相同的O(lg(N))快速循环计算。）
     */
    private static int computeRedLevel(int sz) {
        int level = 0;
        for (int m = sz - 1; m >= 0; m = m / 2 - 1)
            level++;
        return level;
    }

    /**
     * 目前，我们仅支持完整映射的 Spliterator 版本，无论是普通形式还是降序形式，否则依赖于默认实现，因为子映射的大小估计会主导成本。
     * 检查键视图所需的类型测试并不十分优雅，但避免了对现有类结构的干扰。如果此方法返回 null，调用者必须使用普通的默认 spliterator。
     */
    static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K,?> m) {
        if (m instanceof TreeMap) {
            @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                (TreeMap<K,Object>) m;
            return t.keySpliterator();
        }
        if (m instanceof DescendingSubMap) {
            @SuppressWarnings("unchecked") DescendingSubMap<K,?> dm =
                (DescendingSubMap<K,?>) m;
            TreeMap<K,?> tm = dm.m;
            if (dm == tm.descendingMap) {
                @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                    (TreeMap<K,Object>) tm;
                return t.descendingKeySpliterator();
            }
        }
        @SuppressWarnings("unchecked") NavigableSubMap<K,?> sm =
            (NavigableSubMap<K,?>) m;
        return sm.keySpliterator();
    }

    final Spliterator<K> keySpliterator() {
        return new KeySpliterator<K,V>(this, null, null, 0, -1, 0);
    }

    final Spliterator<K> descendingKeySpliterator() {
        return new DescendingKeySpliterator<K,V>(this, null, null, 0, -2, 0);
    }

    /**
     * 分割迭代器的基类。迭代从给定的起点开始，继续到但不包括给定的边界（或null表示结束）。
     * 在顶层，对于升序情况，第一次分割使用根作为左边界/右起点。从那里开始，右分割将当前边界替换为其左子节点，
     * 同时也作为分割出的迭代器的起点。左分割是对称的。降序版本将起点放在末尾，并反转升序分割规则。
     * 这个基类对于方向性或顶层分割迭代器是否覆盖整个树没有明确承诺。这意味着实际的分割机制位于子类中。
     * 一些子类的trySplit方法是相同的（除了返回类型），但不能很好地分解。
     *
     * 目前，子类版本仅存在于完整映射中（包括通过其降序映射的降序键）。其他版本是可能的，
     * 但目前不值得，因为子映射需要O(n)计算来确定大小，这大大限制了使用自定义分割迭代器与默认机制的潜在加速。
     *
     * 为了引导初始化，外部构造函数使用负大小估计：-1表示升序，-2表示降序。
     */
    static class TreeMapSpliterator<K,V> {
        final TreeMap<K,V> tree;
        Entry<K,V> current; // 遍历器; 初始时为范围内的第一个节点
        Entry<K,V> fence;   // 上一个，或null
        int side;                   // 0: 顶部, -1: 是左分割, +1: 右
        int est;                    // 大小估计（仅对顶级精确）
        int expectedModCount;       // 用于CME检查

        TreeMapSpliterator(TreeMap<K,V> tree,
                           Entry<K,V> origin, Entry<K,V> fence,
                           int side, int est, int expectedModCount) {
            this.tree = tree;
            this.current = origin;
            this.fence = fence;
            this.side = side;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEstimate() { // 强制初始化
            int s; TreeMap<K,V> t;
            if ((s = est) < 0) {
                if ((t = tree) != null) {
                    current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
                    s = est = t.size;
                    expectedModCount = t.modCount;
                }
                else
                    s = est = 0;
            }
            return s;
        }

        public final long estimateSize() {
            return (long)getEstimate();
        }
    }

    static final class KeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(TreeMap<K,V> tree,
                       Entry<K,V> origin, Entry<K,V> fence,
                       int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // 强制初始化
            int d = side;
            Entry<K,V> e = current, f = fence,
                s = ((e == null || e == f) ? null :      // 空
                     (d == 0)              ? tree.root : // 是顶部
                     (d >  0)              ? e.right :   // 是正确的
                     (d <  0 && f != null) ? f.left :    // 被留下
                     null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e 尚未超过 s
                side = 1;
                return new KeySpliterator<>
                    (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // 排气
                do {
                    action.accept(e.key);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        public final Comparator<? super K>  getComparator() {
            return tree.comparator;
        }

    }

    static final class DescendingKeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        DescendingKeySpliterator(TreeMap<K,V> tree,
                                 Entry<K,V> origin, Entry<K,V> fence,
                                 int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public DescendingKeySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // 强制初始化
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // 空
                         (d == 0)              ? tree.root : // 是顶部
                         (d <  0)              ? e.left :    // 被留下
                         (d >  0 && f != null) ? f.right :   // 是正确的
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) > 0) {       // e 尚未超过 s
                side = 1;
                return new DescendingKeySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            Entry<K,V> f = fence, e, p, pr;
            if ((e = current) != null && e != f) {
                current = f; // 排气
                do {
                    action.accept(e.key);
                    if ((p = e.left) != null) {
                        while ((pr = p.right) != null)
                            p = pr;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.left)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            if ((e = current) == null || e == fence)
                return false;
            current = predecessor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.ORDERED;
        }
    }

    static final class ValueSpliterator<K,V>
            extends TreeMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(TreeMap<K,V> tree,
                         Entry<K,V> origin, Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // 强制初始化
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // 空
                         (d == 0)              ? tree.root : // 是顶部
                         (d >  0)              ? e.right :   // 是正确的
                         (d <  0 && f != null) ? f.left :    // 被留下
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e 尚未超过 s
                side = 1;
                return new ValueSpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // 排气
                do {
                    action.accept(e.value);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e.value);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
        }
    }

    static final class EntrySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<java.util.Map.Entry<K,V>> {
        EntrySpliterator(TreeMap<K,V> tree,
                         Entry<K,V> origin, Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // 强制初始化
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // 空
                         (d == 0)              ? tree.root : // 是顶部
                         (d >  0)              ? e.right :   // 是正确的
                         (d <  0 && f != null) ? f.left :    // 被留下
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e 尚未超过 s
                side = 1;
                return new EntrySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super java.util.Map.Entry<K, V>> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // 排气
                do {
                    action.accept(e);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super java.util.Map.Entry<K,V>> action) {
            Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // 强制初始化
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        @Override
        public Comparator<java.util.Map.Entry<K, V>> getComparator() {
            // 适配或创建一个基于键的比较器
            if (tree.comparator != null) {
                return java.util.Map.Entry.comparingByKey(tree.comparator);
            }
            else {
                return (Comparator<Map.Entry<K, V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }
}
