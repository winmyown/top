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
import java.util.Set;
import java.util.*;

/**
 * 该类提供了<tt>Set</tt>接口的骨架实现，以最小化实现该接口所需的工作量。<p>
 *
 * 通过扩展此类来实现集合的过程与通过扩展AbstractCollection来实现集合的过程相同，
 * 除了此类的子类中的所有方法和构造函数必须遵守<tt>Set</tt>接口施加的额外约束
 * （例如，add方法不允许将对象的多个实例添加到集合中）。<p>
 *
 * 请注意，此类不会覆盖<tt>AbstractCollection</tt>类中的任何实现。
 * 它仅仅添加了<tt>equals</tt>和<tt>hashCode</tt>的实现。<p>
 *
 * 此类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @param <E> 此集合维护的元素类型
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see java.util.Collection
 * @see AbstractCollection
 * @see java.util.Set
 * @since 1.2
 */

public abstract class AbstractSet<E> extends AbstractCollection<E> implements java.util.Set<E> {
    /**
     * 唯一构造函数。（通常由子类构造函数隐式调用。）
     */
    protected AbstractSet() {
    }

    // 比较和哈希

    /**
     * 将指定对象与此集合进行相等性比较。如果给定对象也是一个集合，两个集合具有相同的大小，并且给定集合的每个成员都包含在此集合中，则返回 <tt>true</tt>。
     * 这确保了 <tt>equals</tt> 方法在不同的 <tt>Set</tt> 接口实现中都能正常工作。<p>
     *
     * 此实现首先检查指定对象是否为此集合；如果是，则返回 <tt>true</tt>。然后，它检查指定对象是否是一个大小与此集合相同的集合；
     * 如果不是，则返回 false。如果是，则返回 <tt>containsAll((Collection) o)</tt>。
     *
     * @param o 要与此集合进行相等性比较的对象
     * @return <tt>true</tt> 如果指定对象等于此集合
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof java.util.Set))
            return false;
        java.util.Collection<?> c = (java.util.Collection<?>) o;
        if (c.size() != size())
            return false;
        try {
            return containsAll(c);
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /**
     * 返回此集合的哈希码值。集合的哈希码定义为集合中元素的哈希码之和，
     * 其中 <tt>null</tt> 元素的哈希码定义为零。这确保了 <tt>s1.equals(s2)</tt> 意味着
     * <tt>s1.hashCode()==s2.hashCode()</tt> 对于任何两个集合 <tt>s1</tt>
     * 和 <tt>s2</tt>，符合 {@link Object#hashCode} 的通用约定。
     *
     * <p>此实现遍历集合，对集合中的每个元素调用 <tt>hashCode</tt> 方法，并将结果相加。
     *
     * @return 此集合的哈希码值
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
        int h = 0;
        java.util.Iterator<E> i = iterator();
        while (i.hasNext()) {
            E obj = i.next();
            if (obj != null)
                h += obj.hashCode();
        }
        return h;
    }

    /**
     * 从该集合中移除所有包含在指定集合中的元素（可选操作）。如果指定的集合也是一个集合，则此操作有效地修改该集合，使其值为两个集合的<i>非对称集合差</i>。
     *
     * <p>此实现通过调用每个集合的<tt>size</tt>方法来确定该集合和指定集合中较小的一个。如果该集合的元素较少，则实现会迭代该集合，依次检查迭代器返回的每个元素是否包含在指定的集合中。如果包含，则使用迭代器的<tt>remove</tt>方法从该集合中移除该元素。如果指定的集合元素较少，则实现会迭代指定的集合，使用该集合的<tt>remove</tt>方法从该集合中移除迭代器返回的每个元素。
     *
     * <p>请注意，如果<tt>iterator</tt>方法返回的迭代器未实现<tt>remove</tt>方法，则此实现将抛出<tt>UnsupportedOperationException</tt>。
     *
     * @param  c 包含要从该集合中移除的元素的集合
     * @return 如果该集合因调用而更改，则返回<tt>true</tt>
     * @throws UnsupportedOperationException 如果该集合不支持<tt>removeAll</tt>操作
     * @throws ClassCastException 如果该集合的某个元素的类与指定的集合不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果该集合包含null元素且指定的集合不允许null元素
     * (<a href="Collection.html#optional-restrictions">可选</a>),
     *         或者如果指定的集合为null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;

        if (size() > c.size()) {
            for (java.util.Iterator<?> i = c.iterator(); i.hasNext(); )
                modified |= remove(i.next());
        } else {
            for (Iterator<?> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

}
