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

package org.top.java.source.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.*;

/**
 * 该接口对实现它的每个类的对象施加了一个全序。这种排序被称为类的<i>自然排序</i>，类的<tt>compareTo</tt>方法被称为其<i>自然比较方法</i>。<p>
 *
 * 实现此接口的对象列表（和数组）可以通过 {@link Collections#sort(List) Collections.sort}（和
 * {@link Arrays#sort(Object[]) Arrays.sort}）自动排序。实现此接口的对象可以用作{@linkplain SortedMap 有序映射}中的键或
 * {@linkplain SortedSet 有序集合}中的元素，而无需指定{@linkplain java.util.Comparator 比较器}。<p>
 *
 * 对于类<tt>C</tt>，自然排序被称为<i>与equals一致</i>，当且仅当<tt>e1.compareTo(e2) == 0</tt>与<tt>e1.equals(e2)</tt>对于类<tt>C</tt>的每个<tt>e1</tt>和<tt>e2</tt>具有相同的布尔值时。注意，<tt>null</tt>不是任何类的实例，并且<tt>e.compareTo(null)</tt>应该抛出<tt>NullPointerException</tt>，即使<tt>e.equals(null)</tt>返回<tt>false</tt>。<p>
 *
 * 强烈建议（尽管不是必须的）自然排序与equals一致。这是因为没有显式比较器的有序集合（和有序映射）在与自然排序与equals不一致的元素（或键）一起使用时，会表现得“奇怪”。特别是，这样的有序集合（或有序映射）违反了集合（或映射）的一般契约，该契约是根据<tt>equals</tt>方法定义的。<p>
 *
 * 例如，如果将两个键<tt>a</tt>和<tt>b</tt>添加到不使用显式比较器的有序集合中，且满足{@code (!a.equals(b) && a.compareTo(b) == 0)}，则第二个<tt>add</tt>操作将返回false（并且有序集合的大小不会增加），因为从有序集合的角度来看，<tt>a</tt>和<tt>b</tt>是等价的。<p>
 *
 * 几乎所有实现<tt>Comparable</tt>的Java核心类都具有与equals一致的自然排序。一个例外是<tt>java.math.BigDecimal</tt>，其自然排序将具有相同值但不同精度的<tt>BigDecimal</tt>对象（例如4.0和4.00）视为相等。<p>
 *
 * 对于数学爱好者来说，定义给定类C上的自然排序的<i>关系</i>是：<pre>
 *       {(x, y) 使得 x.compareTo(y) &lt;= 0}。
 * </pre> 该全序的<i>商</i>是：<pre>
 *       {(x, y) 使得 x.compareTo(y) == 0}。
 * </pre>
 *
 * 从<tt>compareTo</tt>的契约中立即得出，商是<tt>C</tt>上的<i>等价关系</i>，并且自然排序是<tt>C</tt>上的<i>全序</i>。当我们说一个类的自然排序<i>与equals一致</i>时，我们指的是自然排序的商是由类的{@link Object#equals(Object) equals(Object)}方法定义的等价关系：<pre>
 *     {(x, y) 使得 x.equals(y)}。 </pre><p>
 *
 * 该接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java集合框架</a>的成员。
 *
 * @param <T> 此对象可以与之比较的对象的类型
 *
 * @author  Josh Bloch
 * @see Comparator
 * @since 1.2
 */
public interface Comparable<T> {
    /**
     * 将此对象与指定对象进行比较以确定顺序。返回一个负整数、零或正整数，分别表示此对象小于、等于或大于指定对象。
     *
     * <p>实现者必须确保对于所有的<tt>x</tt>和<tt>y</tt>，<tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt>。（这意味着<tt>x.compareTo(y)</tt>必须在且仅当<tt>y.compareTo(x)</tt>抛出异常时抛出异常。）
     *
     * <p>实现者还必须确保关系是可传递的：<tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt>意味着<tt>x.compareTo(z)&gt;0</tt>。
     *
     * <p>最后，实现者必须确保<tt>x.compareTo(y)==0</tt>意味着对于所有的<tt>z</tt>，<tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>。
     *
     * <p>强烈建议，但<i>不</i>严格要求<tt>(x.compareTo(y)==0) == (x.equals(y))</tt>。一般来说，任何实现<tt>Comparable</tt>接口但违反此条件的类都应明确说明这一事实。建议的语言是“注意：此类的自然顺序与equals不一致。”
     *
     * <p>在前述描述中，符号<tt>sgn(</tt><i>expression</i><tt>)</tt>表示数学中的<i>符号</i>函数，其定义为根据<i>expression</i>的值为负、零或正分别返回<tt>-1</tt>、<tt>0</tt>或<tt>1</tt>。
     *
     * @param   o 要进行比较的对象。
     * @return  一个负整数、零或正整数，分别表示此对象小于、等于或大于指定对象。
     *
     * @throws NullPointerException 如果指定对象为null
     * @throws ClassCastException 如果指定对象的类型阻止其与此对象进行比较。
     */
    public int compareTo(T o);
}
