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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.*;

/**
 * 该类提供了<tt>Map</tt>接口的骨架实现，以最小化实现此接口所需的工作量。
 *
 * <p>要实现一个不可修改的映射，程序员只需扩展此类并提供<tt>entrySet</tt>方法的实现，该方法返回映射的集合视图。通常，返回的集合将基于<tt>AbstractSet</tt>实现。此集合不应支持<tt>add</tt>或<tt>remove</tt>方法，并且其迭代器不应支持<tt>remove</tt>方法。
 *
 * <p>要实现一个可修改的映射，程序员必须额外重写此类的<tt>put</tt>方法（否则会抛出<tt>UnsupportedOperationException</tt>），并且<tt>entrySet().iterator()</tt>返回的迭代器必须额外实现其<tt>remove</tt>方法。
 *
 * <p>程序员通常应提供一个无参构造函数和一个映射构造函数，按照<tt>Map</tt>接口规范中的建议。
 *
 * <p>此类中每个非抽象方法的文档详细描述了其实现。如果要实现的映射允许更高效的实现，可以重写这些方法。
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @param <K> 此映射维护的键的类型
 * @param <V> 映射值的类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see java.util.Map
 * @see java.util.Collection
 * @since 1.2
 */

public abstract class AbstractMap<K,V> implements java.util.Map<K,V> {
    /**
     * 唯一的构造函数。（通常由子类构造函数隐式调用。）
     */
    protected AbstractMap() {
    }

    // 查询操作

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现返回 <tt>entrySet().size()</tt>。
     */
    public int size() {
        return entrySet().size();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现返回 <tt>size() == 0</tt>。
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现遍历 <tt>entrySet()</tt> 搜索具有指定值的条目。如果找到这样的条目，则返回 <tt>true</tt>。如果迭代结束仍未找到这样的条目，则返回 <tt>false</tt>。请注意，此实现需要线性时间，与映射的大小成正比。
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
        if (value==null) {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getValue()==null)
                    return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (value.equals(e.getValue()))
                    return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现遍历 <tt>entrySet()</tt> 以搜索具有指定键的条目。如果找到这样的条目，
     * 则返回 <tt>true</tt>。如果遍历结束仍未找到这样的条目，则返回 <tt>false</tt>。
     * 请注意，此实现需要线性时间，具体取决于映射的大小；许多实现将覆盖此方法。
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
        if (key==null) {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    return true;
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现遍历 <tt>entrySet()</tt> 以搜索具有指定键的条目。如果找到这样的条目，则返回该条目的值。如果迭代结束时仍未找到这样的条目，则返回 <tt>null</tt>。请注意，此实现需要线性时间，与映射的大小成正比；许多实现将重写此方法。
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V get(Object key) {
        java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
        if (key==null) {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    return e.getValue();
            }
        } else {
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    return e.getValue();
            }
        }
        return null;
    }


    // 修改操作

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现始终抛出
     * <tt>UnsupportedOperationException</tt>。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 该实现遍历 <tt>entrySet()</tt> 以查找具有指定键的条目。如果找到这样的条目，则通过其 <tt>getValue</tt> 操作获取其值，并通过迭代器的 <tt>remove</tt> 操作从集合（以及支持映射）中移除该条目，然后返回保存的值。如果迭代终止时未找到这样的条目，则返回 <tt>null</tt>。请注意，该实现需要线性时间，与映射的大小成正比；许多实现将重写此方法。
     *
     * <p>请注意，如果 <tt>entrySet</tt> 迭代器不支持 <tt>remove</tt> 方法并且此映射包含指定键的映射，则此实现会抛出 <tt>UnsupportedOperationException</tt>。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V remove(Object key) {
        java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
        Entry<K,V> correctEntry = null;
        if (key==null) {
            while (correctEntry==null && i.hasNext()) {
                Entry<K,V> e = i.next();
                if (e.getKey()==null)
                    correctEntry = e;
            }
        } else {
            while (correctEntry==null && i.hasNext()) {
                Entry<K,V> e = i.next();
                if (key.equals(e.getKey()))
                    correctEntry = e;
            }
        }

        V oldValue = null;
        if (correctEntry !=null) {
            oldValue = correctEntry.getValue();
            i.remove();
        }
        return oldValue;
    }


    // 批量操作

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现遍历指定映射的
     * <tt>entrySet()</tt> 集合，并为迭代返回的每个条目调用此映射的 <tt>put</tt>
     * 操作。
     *
     * <p>请注意，如果此映射不支持
     * <tt>put</tt> 操作且指定的映射非空，则此实现会抛出
     * <tt>UnsupportedOperationException</tt>。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public void putAll(java.util.Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现调用 <tt>entrySet().clear()</tt>。
     *
     * <p>请注意，如果 <tt>entrySet</tt> 不支持 <tt>clear</tt> 操作，此实现会抛出
     * <tt>UnsupportedOperationException</tt>。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public void clear() {
        entrySet().clear();
    }


    // 视图

    /**
     * 这些字段在第一次请求此视图时被初始化为包含适当视图的实例。视图是无状态的，因此没有理由创建多个实例。
     *
     * <p>由于在访问这些字段时没有执行同步操作，因此期望使用这些字段的 java.util.Map 视图类没有非 final 字段（或除外部 this 之外的任何字段）。遵守此规则将使这些字段上的竞争成为良性竞争。
     *
     * <p>同样重要的是，实现只读取字段一次，如下所示：
     *
     * <pre> {@code
     * public Set<K> keySet() {
     *   Set<K> ks = keySet;  // 单次竞争读取
     *   if (ks == null) {
     *     ks = new KeySet();
     *     keySet = ks;
     *   }
     *   return ks;
     * }
     *}</pre>
     */
    transient java.util.Set<K> keySet;
    transient java.util.Collection<V> values;

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现返回一个继承自 {@link AbstractSet} 的集合。
     * 子类的迭代器方法返回一个“包装对象”，包装了此映射的 <tt>entrySet()</tt> 迭代器。
     * <tt>size</tt> 方法委托给此映射的 <tt>size</tt> 方法，
     * 而 <tt>contains</tt> 方法委托给此映射的 <tt>containsKey</tt> 方法。
     *
     * <p>该集合在第一次调用此方法时创建，并在后续所有调用中返回。
     * 未执行同步操作，因此存在多次调用此方法可能不会返回相同集合的轻微可能性。
     */
    public java.util.Set<K> keySet() {
        java.util.Set<K> ks = keySet;
        if (ks == null) {
            ks = new AbstractSet<K>() {
                public java.util.Iterator<K> iterator() {
                    return new java.util.Iterator<K>() {
                        private java.util.Iterator<Entry<K,V>> i = entrySet().iterator();

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public K next() {
                            return i.next().getKey();
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();
                }

                public boolean contains(Object k) {
                    return AbstractMap.this.containsKey(k);
                }
            };
            keySet = ks;
        }
        return ks;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 此实现返回一个继承自 {@link AbstractCollection} 的集合。
     * 该子类的迭代器方法返回一个“包装对象”，该对象包装了此映射的 <tt>entrySet()</tt> 迭代器。
     * <tt>size</tt> 方法委托给此映射的 <tt>size</tt> 方法，
     * <tt>contains</tt> 方法委托给此映射的 <tt>containsValue</tt> 方法。
     *
     * <p>该集合在第一次调用此方法时创建，并在后续所有调用中返回。
     * 未进行同步操作，因此存在多次调用此方法可能不会返回相同集合的轻微可能性。
     */
    public java.util.Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {
            vals = new AbstractCollection<V>() {
                public java.util.Iterator<V> iterator() {
                    return new java.util.Iterator<V>() {
                        private java.util.Iterator<Entry<K,V>> i = entrySet().iterator();

                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public V next() {
                            return i.next().getValue();
                        }

                        public void remove() {
                            i.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractMap.this.size();
                }

                public boolean isEmpty() {
                    return AbstractMap.this.isEmpty();
                }

                public void clear() {
                    AbstractMap.this.clear();
                }

                public boolean contains(Object v) {
                    return AbstractMap.this.containsValue(v);
                }
            };
            values = vals;
        }
        return vals;
    }

    public abstract java.util.Set<Entry<K,V>> entrySet();


    // 比较和哈希

    /**
     * 将指定对象与此映射进行比较以判断是否相等。如果给定对象也是一个映射，并且两个映射表示相同的映射关系，则返回
     * <tt>true</tt>。更正式地说，如果<tt>m1.entrySet().equals(m2.entrySet())</tt>，则两个映射<tt>m1</tt>和
     * <tt>m2</tt>表示相同的映射关系。这确保了<tt>equals</tt>方法在<tt>Map</tt>接口的不同实现中正常工作。
     *
     * @implSpec
     * 此实现首先检查指定对象是否为此映射；如果是，则返回<tt>true</tt>。然后，它检查指定对象是否是一个大小与此映射相同的映射；如果
     * 不是，则返回<tt>false</tt>。如果是，则遍历此映射的<tt>entrySet</tt>集合，并检查指定映射是否包含此映射包含的每个映射关系。
     * 如果指定映射未能包含此类映射关系，则返回<tt>false</tt>。如果迭代完成，则返回<tt>true</tt>。
     *
     * @param o 要与此映射进行相等性比较的对象
     * @return <tt>true</tt> 如果指定对象等于此映射
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof java.util.Map))
            return false;
        java.util.Map<?,?> m = (java.util.Map<?,?>) o;
        if (m.size() != size())
            return false;

        try {
            java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }

    /**
     * 返回此映射的哈希码值。映射的哈希码定义为映射的<tt>entrySet()</tt>视图中每个条目的哈希码之和。
     * 这确保了<tt>m1.equals(m2)</tt>意味着<tt>m1.hashCode()==m2.hashCode()</tt>对于任何两个映射<tt>m1</tt>和<tt>m2</tt>，
     * 符合{@link Object#hashCode}的通用约定。
     *
     * @implSpec
     * 此实现遍历<tt>entrySet()</tt>，对集合中的每个元素（条目）调用{@link Entry#hashCode hashCode()}，
     * 并将结果相加。
     *
     * @return 此映射的哈希码值
     * @see Entry#hashCode()
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
        int h = 0;
        java.util.Iterator<Entry<K,V>> i = entrySet().iterator();
        while (i.hasNext())
            h += i.next().hashCode();
        return h;
    }

    /**
     * 返回此映射的字符串表示形式。字符串表示形式由映射的<tt>entrySet</tt>视图的迭代器返回的键值映射列表组成，用大括号（<tt>"{}"</tt>）括起来。
     * 相邻的映射用字符<tt>", "</tt>（逗号和空格）分隔。每个键值映射表示为键后跟等号（<tt>"="</tt>）后跟关联的值。
     * 键和值通过{@link String#valueOf(Object)}转换为字符串。
     *
     * @return 此映射的字符串表示形式
     */
    public String toString() {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key   == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (! i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

    /**
     * 返回此 <tt>AbstractMap</tt> 实例的浅拷贝：键和值本身不会被克隆。
     *
     * @return 此映射的浅拷贝
     */
    protected Object clone() throws CloneNotSupportedException {
        AbstractMap<?,?> result = (AbstractMap<?,?>)super.clone();
        result.keySet = null;
        result.values = null;
        return result;
    }

    /**
     * 用于SimpleEntry和SimpleImmutableEntry的工具方法。
     * 测试相等性，检查null值。
     *
     * 注意：在JDK-8015417问题解决之前，不要替换为Object.equals。
     */
    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    // 实现说明：SimpleEntry 和 SimpleImmutableEntry
    // 是独立的无关类，即使它们共享
    // 一些代码。由于无法添加或删除final属性
    // 子类中的字段，它们不能共享表示形式，
    // 重复代码的数量太少，不足以证明
    /**
 * 暴露一个通用的抽象类。
 */


    /**
     * 一个维护键和值的条目。可以使用<tt>setValue</tt>方法更改值。此类
     * 有助于构建自定义映射实现的过程。例如，在方法
     * <tt>Map.entrySet().toArray</tt>中返回<tt>SimpleEntry</tt>实例的数组
     * 可能很方便。
     *
     * @since 1.6
     */
    public static class SimpleEntry<K,V>
        implements Entry<K,V>, java.io.Serializable
    {
        private static final long serialVersionUID = -8499721149061103585L;

        private final K key;
        private V value;

        /**
         * 创建一个表示从指定键到指定值的映射的条目。
         *
         * @param key 该条目表示的键
         * @param value 该条目表示的值
         */
        public SimpleEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * 创建一个表示与指定条目相同映射的条目。
         *
         * @param entry 要复制的条目
         */
        public SimpleEntry(Entry<? extends K, ? extends V> entry) {
            this.key   = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * 返回与此条目对应的键。
         *
         * @return 与此条目对应的键
         */
        public K getKey() {
            return key;
        }

        /**
         * 返回与此条目对应的值。
         *
         * @return 与此条目对应的值
         */
        public V getValue() {
            return value;
        }

        /**
         * 用指定的值替换此条目对应的值。
         *
         * @param value 要存储在此条目中的新值
         * @return 此条目对应的旧值
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * 将指定的对象与此条目进行比较以确定是否相等。
         * 如果给定对象也是一个映射条目并且两个条目表示相同的映射，则返回 {@code true}。
         * 更正式地说，两个条目 {@code e1} 和 {@code e2} 表示相同的映射，如果<pre>
         *   (e1.getKey()==null ?
         *    e2.getKey()==null :
         *    e1.getKey().equals(e2.getKey()))
         *   &amp;&amp;
         *   (e1.getValue()==null ?
         *    e2.getValue()==null :
         *    e1.getValue().equals(e2.getValue()))</pre>
         * 这确保了 {@code equals} 方法在不同的 {@code Map.Entry} 接口实现中正常工作。
         *
         * @param o 要与此映射条目进行比较以确定是否相等的对象
         * @return 如果指定的对象等于此映射条目，则返回 {@code true}
         * @see    #hashCode
         */
        public boolean equals(Object o) {
            if (!(o instanceof java.util.Map.Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        /**
         * 返回此映射条目的哈希码值。映射条目 {@code e} 的哈希码
         * 定义为：<pre>
         *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
         * 这确保了 {@code e1.equals(e2)} 意味着
         * {@code e1.hashCode()==e2.hashCode()} 对于任何两个条目
         * {@code e1} 和 {@code e2}，符合 {@link Object#hashCode} 的
         * 通用约定。
         *
         * @return 此映射条目的哈希码值
         * @see    #equals
         */
        public int hashCode() {
            return (key   == null ? 0 :   key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        /**
         * 返回此映射项的字符串表示形式。此实现返回此
         * 项的键的字符串表示形式，后跟等号字符（"<tt>=</tt>"），
         * 再后跟此项的值的字符串表示形式。
         *
         * @return 此映射项的字符串表示形式
         */
        public String toString() {
            return key + "=" + value;
        }

    }

    /**
     * 一个维护不可变键和值的Entry。此类不支持<tt>setValue</tt>方法。此类可能适用于返回键值映射的线程安全快照的方法。
     *
     * @since 1.6
     */
    public static class SimpleImmutableEntry<K,V>
        implements Entry<K,V>, java.io.Serializable
    {
        private static final long serialVersionUID = 7138329143949025153L;

        private final K key;
        private final V value;

        /**
         * 创建一个表示从指定键到指定值的映射的条目。
         *
         * @param key 该条目表示的键
         * @param value 该条目表示的值
         */
        public SimpleImmutableEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * 创建一个表示与指定条目相同映射的条目。
         *
         * @param entry 要复制的条目
         */
        public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
            this.key   = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * 返回与此条目对应的键。
         *
         * @return 与此条目对应的键
         */
        public K getKey() {
            return key;
        }

        /**
         * 返回与此条目对应的值。
         *
         * @return 与此条目对应的值
         */
        public V getValue() {
            return value;
        }

        /**
         * 将此条目对应的值替换为指定的值（可选操作）。此实现仅抛出
         * <tt>UnsupportedOperationException</tt>，因为此类实现了一个
         * <i>不可变</i>的映射条目。
         *
         * @param value 要存储在此条目中的新值
         * @return （不返回）
         * @throws UnsupportedOperationException 始终抛出
         */
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        /**
         * 将指定的对象与此条目进行比较以确定是否相等。
         * 如果给定对象也是一个映射条目并且两个条目表示相同的映射，则返回 {@code true}。
         * 更正式地说，两个条目 {@code e1} 和 {@code e2} 表示相同的映射，如果<pre>
         *   (e1.getKey()==null ?
         *    e2.getKey()==null :
         *    e1.getKey().equals(e2.getKey()))
         *   &amp;&amp;
         *   (e1.getValue()==null ?
         *    e2.getValue()==null :
         *    e1.getValue().equals(e2.getValue()))</pre>
         * 这确保了 {@code equals} 方法在不同的 {@code Map.Entry} 接口实现中正常工作。
         *
         * @param o 要与此映射条目进行比较以确定是否相等的对象
         * @return 如果指定的对象等于此映射条目，则返回 {@code true}
         * @see    #hashCode
         */
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        /**
         * 返回此映射条目的哈希码值。映射条目 {@code e} 的哈希码
         * 定义为：<pre>
         *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
         * 这确保了 {@code e1.equals(e2)} 意味着
         * {@code e1.hashCode()==e2.hashCode()} 对于任何两个条目
         * {@code e1} 和 {@code e2}，符合 {@link Object#hashCode} 的
         * 通用约定。
         *
         * @return 此映射条目的哈希码值
         * @see    #equals
         */
        public int hashCode() {
            return (key   == null ? 0 :   key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        /**
         * 返回此映射项的字符串表示形式。此实现返回此
         * 项的键的字符串表示形式，后跟等号字符（"<tt>=</tt>"），
         * 再后跟此项的值的字符串表示形式。
         *
         * @return 此映射项的字符串表示形式
         */
        public String toString() {
            return key + "=" + value;
        }

    }

}
