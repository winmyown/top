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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * <p>哈希表和双向链表的实现，具有可预测的迭代顺序。这个实现与<tt>HashMap</tt>的不同之处在于，它维护了一个贯穿所有条目的双向链表。这个链表定义了迭代顺序，通常是键插入到映射中的顺序（<i>插入顺序</i>）。请注意，如果键被<i>重新插入</i>到映射中，插入顺序不会受到影响。（如果调用<tt>m.put(k, v)</tt>时，<tt>m.containsKey(k)</tt>在调用之前会立即返回<tt>true</tt>，则键<tt>k</tt>被重新插入到映射<tt>m</tt>中。）
 *
 * <p>这个实现使客户端免受{@link java.util.HashMap}（和{@link Hashtable}）提供的未指定、通常混乱的排序的影响，而不会产生与{@link TreeMap}相关的额外成本。它可以用来生成一个与原始映射具有相同顺序的副本，无论原始映射的实现如何：
 * <pre>
 *     void foo(Map m) {
 *         Map copy = new LinkedHashMap(m);
 *         ...
 *     }
 * </pre>
 * 如果一个模块接收一个映射作为输入，复制它，并稍后返回其顺序由该副本决定的结果，这种技术特别有用。（客户端通常希望以与输入相同的顺序返回结果。）
 *
 * <p>提供了一个特殊的{@link #LinkedHashMap(int,float,boolean) 构造函数}来创建一个链接哈希映射，其迭代顺序是其条目最后被访问的顺序，从最近最少访问到最近最多访问（<i>访问顺序</i>）。这种映射非常适合构建LRU缓存。调用{@code put}、{@code putIfAbsent}、{@code get}、{@code getOrDefault}、{@code compute}、{@code computeIfAbsent}、{@code computeIfPresent}或{@code merge}方法会导致对相应条目的访问（假设调用完成后该条目存在）。{@code replace}方法仅在值被替换时才会导致条目的访问。{@code putAll}方法为指定映射中的每个映射生成一个条目访问，顺序由指定映射的条目集迭代器提供。<i>没有其他方法会生成条目访问。</i> 特别是，对集合视图的操作<i>不会</i>影响后备映射的迭代顺序。
 *
 * <p>可以覆盖{@link #removeEldestEntry(java.util.Map.Entry)}方法，以在向映射添加新映射时自动删除陈旧映射的策略。
 *
 * <p>此类提供了所有可选的<tt>Map</tt>操作，并允许空元素。与<tt>HashMap</tt>一样，它提供了对基本操作（<tt>add</tt>、<tt>contains</tt>和<tt>remove</tt>）的常数时间性能，假设哈希函数将元素适当地分散在桶中。由于维护链表的额外开销，性能可能略低于<tt>HashMap</tt>，但有一个例外：对<tt>LinkedHashMap</tt>的集合视图的迭代所需的时间与映射的<i>大小</i>成正比，而与其容量无关。对<tt>HashMap</tt>的迭代可能更昂贵，所需的时间与其<i>容量</i>成正比。
 *
 * <p>链接哈希映射有两个影响其性能的参数：<i>初始容量</i>和<i>负载因子</i>。它们的定义与<tt>HashMap</tt>完全相同。但请注意，对于此类来说，选择过高的初始容量值的惩罚不如<tt>HashMap</tt>严重，因为此类的迭代时间不受容量的影响。
 *
 * <p><strong>请注意，此实现不是同步的。</strong>
 * 如果多个线程同时访问一个链接哈希映射，并且至少有一个线程在结构上修改了映射，则必须从外部进行同步。这通常通过同步某个自然封装映射的对象来实现。
 *
 * 如果不存在这样的对象，则应使用{@link Collections#synchronizedMap Collections.synchronizedMap}方法“包装”映射。最好在创建时执行此操作，以防止意外的非同步访问映射：<pre>
 *   Map m = Collections.synchronizedMap(new LinkedHashMap(...));</pre>
 *
 * 结构修改是指添加或删除一个或多个映射的任何操作，或者在访问顺序链接哈希映射的情况下，影响迭代顺序。在插入顺序链接哈希映射中，仅更改已包含在映射中的键的关联值不是结构修改。<strong>在访问顺序链接哈希映射中，仅使用<tt>get</tt>查询映射就是结构修改。</strong>)
 *
 * <p>由此类的所有集合视图方法返回的集合的<tt>iterator</tt>方法返回的迭代器是<em>快速失败</em>的：如果在创建迭代器后的任何时候对映射进行结构修改，除非通过迭代器自己的<tt>remove</tt>方法，否则迭代器将抛出{@link ConcurrentModificationException}。因此，面对并发修改，迭代器会快速而干净地失败，而不是在未来的某个不确定时间冒任意、非确定性行为的风险。
 *
 * <p>请注意，无法保证迭代器的快速失败行为，因为一般来说，在存在非同步并发修改的情况下，不可能做出任何硬性保证。快速失败迭代器会尽最大努力抛出<tt>ConcurrentModificationException</tt>。因此，编写依赖于此异常的程序是错误的：<i>迭代器的快速失败行为应仅用于检测错误。</i>
 *
 * <p>由此类的所有集合视图方法的spliterator方法返回的spliterator是<em><a href="Spliterator.html#binding">延迟绑定</a></em>、<em>快速失败</em>的，并且还报告{@link Spliterator#ORDERED}。
 *
 * <p>此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @implNote
 * 由此类的所有集合视图方法的spliterator方法返回的spliterator是从相应集合的迭代器创建的。
 *
 * @param <K> 此映射维护的键的类型
 * @param <V> 映射值的类型
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     java.util.Collection
 * @see     java.util.Map
 * @see     java.util.HashMap
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.4
 */
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements java.util.Map<K,V>
{

    /*
     * 实现说明。此类的先前版本在内部结构上略有不同。由于超类 HashMap 现在对其某些节点使用树结构，类
     * LinkedHashMap.Entry 现在被视为中间节点类，也可以转换为树形式。此类的名称 LinkedHashMap.Entry
     * 在当前的上下文中有几种令人困惑的方式，但不能更改。否则，即使它没有在此包之外导出，已知一些现有的源代码
     * 依赖于在调用 removeEldestEntry 时抑制由于歧义用法引起的编译错误的符号解析特殊情况规则。因此，我们保留
     * 该名称以保持未修改的编译能力。
     *
     * 节点类的更改还要求使用两个字段（head, tail）而不是指向头节点的指针来维护双向链表。此类以前也使用不同的
     * 回调方法风格来处理访问、插入和删除操作。
     */

    /**
     * HashMap.Node 的子类，用于普通的 LinkedHashMap 条目。
     */
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    private static final long serialVersionUID = 3801124242820219131L;

    /**
     * 双向链表的头节点（最老的）。
     */
    transient Entry<K,V> head;

    /**
     * 双向链表的尾部（最年轻）。
     */
    transient Entry<K,V> tail;

    /**
     * 此链接哈希映射的迭代顺序方法：<tt>true</tt> 表示访问顺序，<tt>false</tt> 表示插入顺序。
     *
     * @serial
     */
    final boolean accessOrder;

    // 内部工具

    // 在列表末尾的链接
    private void linkNodeLast(Entry<K,V> p) {
        Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }

    // 将 src 的链接应用到 dst
    private void transferLinks(Entry<K,V> src,
                               Entry<K,V> dst) {
        Entry<K,V> b = dst.before = src.before;
        Entry<K,V> a = dst.after = src.after;
        if (b == null)
            head = dst;
        else
            b.after = dst;
        if (a == null)
            tail = dst;
        else
            a.before = dst;
    }

    // HashMap钩子方法的重写

    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        Entry<K,V> p =
            new Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        Entry<K,V> q = (Entry<K,V>)p;
        Entry<K,V> t =
            new Entry<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        Entry<K,V> q = (Entry<K,V>)p;
        TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    void afterNodeRemoval(Node<K,V> e) { // 取消链接
        Entry<K,V> p =
            (Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

    void afterNodeInsertion(boolean evict) { // 可能移除最年长的
        Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    void afterNodeAccess(Node<K,V> e) { // 将节点移动到末尾
        Entry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            Entry<K,V> p =
                (Entry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        for (Entry<K,V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }

    /**
     * 构造一个具有指定初始容量和负载因子的空插入顺序的<tt>LinkedHashMap</tt>实例。
     *
     * @param  initialCapacity 初始容量
     * @param  loadFactor      负载因子
     * @throws IllegalArgumentException 如果初始容量为负数
     *         或负载因子为非正数
     */
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    /**
     * 构造一个具有指定初始容量和默认负载因子（0.75）的空插入顺序<tt>LinkedHashMap</tt>实例。
     *
     * @param  initialCapacity 初始容量
     * @throws IllegalArgumentException 如果初始容量为负数
     */
    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    /**
     * 构造一个具有默认初始容量（16）和加载因子（0.75）的空插入顺序的<tt>LinkedHashMap</tt>实例。
     */
    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    /**
     * 构造一个与指定映射具有相同映射关系的插入顺序的 <tt>LinkedHashMap</tt> 实例。
     * <tt>LinkedHashMap</tt> 实例以默认的加载因子（0.75）和足以容纳指定映射中所有映射的初始容量创建。
     *
     * @param  m 要将其映射关系放入此映射的映射
     * @throws NullPointerException 如果指定的映射为 null
     */
    public LinkedHashMap(java.util.Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    /**
     * 构造一个具有指定初始容量、负载因子和排序模式的空 <tt>LinkedHashMap</tt> 实例。
     *
     * @param  initialCapacity 初始容量
     * @param  loadFactor      负载因子
     * @param  accessOrder     排序模式 - <tt>true</tt> 表示访问顺序，
     *                         <tt>false</tt> 表示插入顺序
     * @throws IllegalArgumentException 如果初始容量为负数或负载因子为非正数
     */
    public LinkedHashMap(int initialCapacity,
                         float loadFactor,
                         boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }


    /**
     * 如果此映射将一个或多个键映射到指定值，则返回<tt>true</tt>。
     *
     * @param value 要测试是否在此映射中存在的值
     * @return <tt>true</tt> 如果此映射将一个或多个键映射到指定值
     */
    public boolean containsValue(Object value) {
        for (Entry<K,V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

    /**
     * 返回指定键所映射的值，
     * 如果此映射不包含该键的映射关系，则返回 {@code null}。
     *
     * <p>更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射关系，
     * 使得 {@code (key==null ? k==null : key.equals(k))}，则此方法返回 {@code v}；否则
     * 返回 {@code null}。（最多只能有一个这样的映射关系。）
     *
     * <p>返回 {@code null} 并不<i>必然</i>表示映射不包含该键的映射关系；
     * 也可能映射显式地将该键映射到 {@code null}。
     * 可以使用 {@link #containsKey containsKey} 操作来区分这两种情况。
     */
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    /**
     * {@inheritDoc}
     */
    public V getOrDefault(Object key, V defaultValue) {
       Node<K,V> e;
       if ((e = getNode(hash(key), key)) == null)
           return defaultValue;
       if (accessOrder)
           afterNodeAccess(e);
       return e.value;
   }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        super.clear();
        head = tail = null;
    }

    /**
     * 如果此映射应移除其最旧的条目，则返回 <tt>true</tt>。
     * 此方法在将新条目插入映射后由 <tt>put</tt> 和 <tt>putAll</tt> 调用。
     * 它为实现者提供了每次添加新条目时移除最旧条目的机会。
     * 如果映射表示缓存，则这非常有用：它允许映射通过删除陈旧条目来减少内存消耗。
     *
     * <p>示例用法：此覆盖将允许映射增长到 100 个条目，然后在每次添加新条目时删除最旧的条目，保持 100 个条目的稳定状态。
     * <pre>
     *     private static final int MAX_ENTRIES = 100;
     *
     *     protected boolean removeEldestEntry(Map.Entry eldest) {
     *        return size() &gt; MAX_ENTRIES;
     *     }
     * </pre>
     *
     * <p>此方法通常不会以任何方式修改映射，而是允许映射根据其返回值指示进行自我修改。
     * 此方法允许直接修改映射，但如果这样做，它 <i>必须</i> 返回 <tt>false</tt>（表示映射不应尝试任何进一步的修改）。
     * 在此方法内修改映射后返回 <tt>true</tt> 的效果是未指定的。
     *
     * <p>此实现仅返回 <tt>false</tt>（因此此映射的行为类似于普通映射 - 最旧的元素永远不会被移除）。
     *
     * @param    eldest 映射中最久未插入的条目，或者如果这是访问顺序映射，则是最久未访问的条目。
     *           如果此方法返回 <tt>true</tt>，则将移除此条目。
     *           如果在导致此调用的 <tt>put</tt> 或 <tt>putAll</tt> 调用之前映射为空，则这将是刚刚插入的条目；
     *           换句话说，如果映射包含单个条目，则最旧的条目也是最新的条目。
     * @return   <tt>true</tt> 如果应从映射中移除最旧的条目；<tt>false</tt> 如果应保留它。
     */
    protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest) {
        return false;
    }

    /**
     * 返回此映射中包含的键的 {@link java.util.Set} 视图。
     * 该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射（除了通过迭代器自己的 <tt>remove</tt> 操作），迭代的结果是未定义的。该集合支持元素移除，通过 <tt>Iterator.remove</tt>、<tt>Set.remove</tt>、<tt>removeAll</tt>、<tt>retainAll</tt> 和 <tt>clear</tt> 操作从映射中移除相应的映射。它不支持 <tt>add</tt> 或 <tt>addAll</tt> 操作。
     * 它的 {@link Spliterator} 通常提供更快的顺序性能，但并行性能比 {@code HashMap} 差得多。
     *
     * @return 此映射中包含的键的集合视图
     */
    public java.util.Set<K> keySet() {
        java.util.Set<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    final class LinkedKeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final java.util.Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator()  {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.key);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * 返回此映射中包含的值的 {@link java.util.Collection} 视图。
     * 该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射
     * （除了通过迭代器自身的 <tt>remove</tt> 操作），迭代的结果是未定义的。该集合支持元素移除，
     * 通过 <tt>Iterator.remove</tt>、<tt>Collection.remove</tt>、<tt>removeAll</tt>、
     * <tt>retainAll</tt> 和 <tt>clear</tt> 操作从映射中移除相应的映射关系。它不支持
     * <tt>add</tt> 或 <tt>addAll</tt> 操作。其 {@link Spliterator} 通常提供更快的顺序性能，
     * 但并行性能比 {@code HashMap} 差得多。
     *
     * @return 此映射中包含的值的视图
     */
    public java.util.Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new LinkedValues();
            values = vs;
        }
        return vs;
    }

    final class LinkedValues extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final java.util.Iterator<V> iterator() {
            return new LinkedValueIterator();
        }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED);
        }
        public final void forEach(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.value);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * 返回此映射中包含的映射关系的 {@link java.util.Set} 视图。
     * 该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。如果在迭代集合的过程中修改了映射（除了通过迭代器自己的 <tt>remove</tt> 操作，或者通过迭代器返回的映射条目的 <tt>setValue</tt> 操作），则迭代的结果是未定义的。该集合支持元素移除，通过 <tt>Iterator.remove</tt>、<tt>Set.remove</tt>、<tt>removeAll</tt>、<tt>retainAll</tt> 和 <tt>clear</tt> 操作从映射中移除相应的映射关系。它不支持 <tt>add</tt> 或 <tt>addAll</tt> 操作。
     * 其 {@link Spliterator} 通常提供比 {@code HashMap} 更快的顺序性能，但并行性能要差得多。
     *
     * @return 此映射中包含的映射关系的集合视图
     */
    public java.util.Set<java.util.Map.Entry<K,V>> entrySet() {
        Set<java.util.Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }

    final class LinkedEntrySet extends AbstractSet<java.util.Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { LinkedHashMap.this.clear(); }
        public final java.util.Iterator<java.util.Map.Entry<K,V>> iterator() {
            return new LinkedEntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof java.util.Map.Entry))
                return false;
            java.util.Map.Entry<?,?> e = (java.util.Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof java.util.Map.Entry) {
                java.util.Map.Entry<?,?> e = (java.util.Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<java.util.Map.Entry<K,V>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.SIZED |
                                            Spliterator.ORDERED |
                                            Spliterator.DISTINCT);
        }
        public final void forEach(Consumer<? super java.util.Map.Entry<K,V>> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    // 映射覆盖

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null)
            throw new NullPointerException();
        int mc = modCount;
        for (Entry<K,V> e = head; e != null; e = e.after)
            action.accept(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null)
            throw new NullPointerException();
        int mc = modCount;
        for (Entry<K,V> e = head; e != null; e = e.after)
            e.value = function.apply(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }

    // 迭代器

    abstract class LinkedHashIterator {
        Entry<K,V> next;
        Entry<K,V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextNode() {
            Entry<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            current = e;
            next = e.after;
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class LinkedKeyIterator extends LinkedHashIterator
        implements java.util.Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

    final class LinkedValueIterator extends LinkedHashIterator
        implements java.util.Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class LinkedEntryIterator extends LinkedHashIterator
        implements Iterator<java.util.Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }


}
