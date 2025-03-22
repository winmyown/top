package org.top.java.source.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.*;

/**
 * 一种支持在两端插入和移除元素的线性集合。名称 <i>deque</i> 是 "double ended queue" 的缩写，通常发音为 "deck"。大多数 {@code Deque}
 * 实现不限制它们可能包含的元素数量，但此接口支持容量受限的 deque 以及那些没有固定大小限制的 deque。
 *
 * <p>此接口定义了访问 deque 两端元素的方法。提供了插入、移除和检查元素的方法。每种方法都有两种形式：
 * 一种在操作失败时抛出异常，另一种返回特殊值（根据操作的不同，返回 {@code null} 或 {@code false}）。
 * 后一种插入操作形式专门设计用于容量受限的 {@code Deque} 实现；在大多数实现中，插入操作不会失败。
 *
 * <p>上述十二种方法总结如下表：
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Deque 方法摘要</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>第一个元素（头部）</b></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>最后一个元素（尾部）</b></td>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>抛出异常</em></td>
 *    <td ALIGN=CENTER><em>特殊值</em></td>
 *    <td ALIGN=CENTER><em>抛出异常</em></td>
 *    <td ALIGN=CENTER><em>特殊值</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>插入</b></td>
 *    <td>{@link Deque#addFirst addFirst(e)}</td>
 *    <td>{@link Deque#offerFirst offerFirst(e)}</td>
 *    <td>{@link Deque#addLast addLast(e)}</td>
 *    <td>{@link Deque#offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>移除</b></td>
 *    <td>{@link Deque#removeFirst removeFirst()}</td>
 *    <td>{@link Deque#pollFirst pollFirst()}</td>
 *    <td>{@link Deque#removeLast removeLast()}</td>
 *    <td>{@link Deque#pollLast pollLast()}</td>
 *  </tr>
 *  <tr>
 *    <td><b>检查</b></td>
 *    <td>{@link Deque#getFirst getFirst()}</td>
 *    <td>{@link Deque#peekFirst peekFirst()}</td>
 *    <td>{@link Deque#getLast getLast()}</td>
 *    <td>{@link Deque#peekLast peekLast()}</td>
 *  </tr>
 * </table>
 *
 * <p>此接口扩展了 {@link Queue} 接口。当 deque 用作队列时，会产生 FIFO（先进先出）行为。元素在 deque 的末尾添加，并从开头移除。
 * 从 {@code Queue} 接口继承的方法与 {@code Deque} 方法完全等效，如下表所示：
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Queue 和 Deque 方法比较</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>{@code Queue} 方法</b></td>
 *    <td ALIGN=CENTER> <b>等效的 {@code Deque} 方法</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#add add(e)}</td>
 *    <td>{@link #addLast addLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#offer offer(e)}</td>
 *    <td>{@link #offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#remove remove()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#poll poll()}</td>
 *    <td>{@link #pollFirst pollFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#element element()}</td>
 *    <td>{@link #getFirst getFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link Queue#peek peek()}</td>
 *    <td>{@link #peek peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * <p>Deque 也可以用作 LIFO（后进先出）堆栈。应优先使用此接口而不是遗留的 {@link Stack} 类。
 * 当 deque 用作堆栈时，元素从 deque 的开头推入和弹出。堆栈方法与 {@code Deque} 方法完全等效，如下表所示：
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Stack 和 Deque 方法比较</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>Stack 方法</b></td>
 *    <td ALIGN=CENTER> <b>等效的 {@code Deque} 方法</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link #push push(e)}</td>
 *    <td>{@link #addFirst addFirst(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #pop pop()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #peek peek()}</td>
 *    <td>{@link #peekFirst peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * <p>请注意，当 deque 用作队列或堆栈时，{@link #peek peek} 方法同样有效；在两种情况下，元素都是从 deque 的开头提取的。
 *
 * <p>此接口提供了两种移除内部元素的方法，{@link #removeFirstOccurrence removeFirstOccurrence} 和
 * {@link #removeLastOccurrence removeLastOccurrence}。
 *
 * <p>与 {@link List} 接口不同，此接口不支持对元素的索引访问。
 *
 * <p>虽然 {@code Deque} 实现不严格要求禁止插入 null 元素，但强烈建议这样做。任何允许 null 元素的 {@code Deque} 实现的用户
 * 强烈建议 <i>不要</i> 利用插入 null 的能力。这是因为 {@code null} 被各种方法用作特殊返回值，以指示 deque 为空。
 *
 * <p>{@code Deque} 实现通常不定义基于元素的 {@code equals} 和 {@code hashCode} 方法，而是从 {@code Object} 类继承基于身份的方法。
 *
 * <p>此接口是 <a
 * href="{@docRoot}/../technotes/guides/collections/index.html"> Java 集合框架</a> 的成员。
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @since  1.6
 * @param <E> 此集合中元素的类型
 */
public interface Deque<E> extends Queue<E> {
    /**
     * 如果可以在不违反容量限制的情况下立即将指定元素插入此双端队列的前面，
     * 如果当前没有可用空间，则抛出 {@code IllegalStateException}。
     * 在使用容量受限的双端队列时，通常更推荐使用方法 {@link #offerFirst}。
     *
     * @param e 要添加的元素
     * @throws IllegalStateException 如果由于容量限制此时无法添加元素
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列
     */
    void addFirst(E e);

    /**
     * 如果可以在不违反容量限制的情况下立即将指定元素插入到此双端队列的末尾，则插入该元素，
     * 如果当前没有可用空间，则抛出 {@code IllegalStateException}。在使用容量受限的双端队列时，
     * 通常更推荐使用 {@link #offerLast} 方法。
     *
     * <p>此方法等效于 {@link #add}。
     *
     * @param e 要添加的元素
     * @throws IllegalStateException 如果由于容量限制无法在此时添加元素
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列
     */
    void addLast(E e);

    /**
     * 将指定元素插入此双端队列的前端，除非这会违反容量限制。在使用容量受限的双端队列时，
     * 此方法通常比 {@link #addFirst} 方法更可取，后者仅在抛出异常时无法插入元素。
     *
     * @param e 要添加的元素
     * @return 如果元素被添加到此双端队列中，则返回 {@code true}，否则返回 {@code false}
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列中
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列中
     */
    boolean offerFirst(E e);

    /**
     * 将指定元素插入到此双端队列的末尾，除非这会违反容量限制。在使用容量受限的双端队列时，
     * 此方法通常比 {@link #addLast} 方法更可取，后者仅通过抛出异常来插入元素失败。
     *
     * @param e 要添加的元素
     * @return 如果元素被添加到此双端队列，则返回 {@code true}，否则返回 {@code false}
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列
     */
    boolean offerLast(E e);

    /**
     * 检索并移除此双端队列的第一个元素。此方法与 {@link #pollFirst pollFirst} 的不同之处在于，如果此双端队列为空，则抛出异常。
     *
     * @return 此双端队列的头部
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E removeFirst();

    /**
     * 检索并移除此双端队列的最后一个元素。此方法与 {@link #pollLast pollLast} 的不同之处在于，如果此双端队列为空，则会抛出异常。
     *
     * @return 此双端队列的尾部元素
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E removeLast();

    /**
     * 检索并移除此双端队列的第一个元素，
     * 如果此双端队列为空，则返回 {@code null}。
     *
     * @return 此双端队列的头部，如果此双端队列为空，则返回 {@code null}
     */
    E pollFirst();

    /**
     * 检索并移除此双端队列的最后一个元素，
     * 如果此双端队列为空，则返回 {@code null}。
     *
     * @return 此双端队列的尾部元素，如果此双端队列为空，则返回 {@code null}
     */
    E pollLast();

    /**
     * 检索但不移除此双端队列的第一个元素。
     *
     * 此方法与 {@link #peekFirst peekFirst} 的不同之处在于，如果此双端队列为空，则会抛出异常。
     *
     * @return 此双端队列的头部元素
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E getFirst();

    /**
     * 检索但不移除此双端队列的最后一个元素。
     * 此方法与 {@link #peekLast peekLast} 的不同之处在于，如果此双端队列为空，则抛出异常。
     *
     * @return 此双端队列的尾部元素
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E getLast();

    /**
     * 检索但不移除此双端队列的第一个元素，
     * 如果此双端队列为空，则返回 {@code null}。
     *
     * @return 此双端队列的头部元素，如果此双端队列为空，则返回 {@code null}
     */
    E peekFirst();

    /**
     * 检索但不移除此双端队列的最后一个元素，
     * 如果此双端队列为空，则返回 {@code null}。
     *
     * @return 此双端队列的尾部元素，如果此双端队列为空，则返回 {@code null}
     */
    E peekLast();

    /**
     * 从该双端队列中移除指定元素的第一个匹配项。
     * 如果双端队列不包含该元素，则队列保持不变。
     * 更正式地说，移除第一个满足 {@code e} 的元素，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * （如果存在这样的元素）。
     * 如果该双端队列包含指定元素，则返回 {@code true}
     * （或者等价地，如果该双端队列因调用而改变）。
     *
     * @param o 要从该双端队列中移除的元素（如果存在）
     * @return 如果该调用导致元素被移除，则返回 {@code true}
     * @throws ClassCastException 如果指定元素的类与此双端队列不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * 从此双端队列中移除指定元素的最后一次出现。
     * 如果双端队列不包含该元素，则队列保持不变。
     * 更正式地说，移除最后一个元素 {@code e}，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * （如果存在这样的元素）。
     * 如果此双端队列包含指定元素，则返回 {@code true}
     * （或等效地，如果此调用导致队列发生变化）。
     *
     * @param o 如果存在，则要从此双端队列中移除的元素
     * @return 如果此调用导致移除元素，则返回 {@code true}
     * @throws ClassCastException 如果指定元素的类与此双端队列不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean removeLastOccurrence(Object o);

    // *** 队列方法 ***

    /**
     * 将指定元素插入由此双端队列表示的队列中
     * （换句话说，在此双端队列的尾部），如果可以在不违反容量限制的情况下立即执行此操作，
     * 则返回 {@code true}，如果没有可用空间，则抛出
     * {@code IllegalStateException}。
     * 当使用容量受限的双端队列时，通常更推荐使用 {@link #offer(Object) offer}。
     *
     * <p>此方法等效于 {@link #addLast}。
     *
     * @param e 要添加的元素
     * @return {@code true}（由 {@link Collection#add} 指定）
     * @throws IllegalStateException 如果由于容量限制此时无法添加元素
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列
     */
    boolean add(E e);

    /**
     * 如果可以在不违反容量限制的情况下立即将指定元素插入此双端队列表示的队列中（换句话说，在此双端队列的尾部），则插入该元素，成功时返回
     * {@code true}，如果当前没有可用空间，则返回 {@code false}。当使用容量受限的双端队列时，此方法通常优于 {@link #add} 方法，
     * 后者只有在抛出异常时才会无法插入元素。
     *
     * <p>此方法等效于 {@link #offerLast}。
     *
     * @param e 要添加的元素
     * @return 如果元素成功添加到此双端队列中，则返回 {@code true}，否则返回 {@code false}
     * @throws ClassCastException 如果指定元素的类阻止其添加到此双端队列中
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其添加到此双端队列中
     */
    boolean offer(E e);

    /**
     * 检索并移除由此双端队列表示的队列的头部
     * （换句话说，此双端队列的第一个元素）。
     * 此方法与 {@link #poll poll} 的不同之处在于，如果此双端队列为空，则会抛出异常。
     *
     * <p>此方法等效于 {@link #removeFirst()}。
     *
     * @return 由此双端队列表示的队列的头部
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E remove();

    /**
     * 检索并移除由此双端队列表示的队列的头部元素
     * （换句话说，此双端队列的第一个元素），如果此双端队列为空，则返回
     * {@code null}。
     *
     * <p>此方法等效于 {@link #pollFirst()}。
     *
     * @return 此双端队列的第一个元素，如果此双端队列为空，则返回 {@code null}
     */
    E poll();

    /**
     * 检索但不移除由此双端队列表示的队列的头部（换句话说，此双端队列的第一个元素）。
     * 此方法与 {@link #peek peek} 的不同之处在于，如果此双端队列为空，它将抛出异常。
     *
     * <p>此方法等效于 {@link #getFirst()}。
     *
     * @return 由此双端队列表示的队列的头部
     * @throws NoSuchElementException 如果此双端队列为空
     */
    E element();

    /**
     * 获取但不移除由此双端队列表示的队列的头部（换句话说，此双端队列的第一个元素），
     * 如果此双端队列为空，则返回 {@code null}。
     *
     * <p>此方法等效于 {@link #peekFirst()}。
     *
     * @return 由此双端队列表示的队列的头部，如果此双端队列为空，则返回 {@code null}
     */
    E peek();


    // *** 栈方法 ***

    /**
     * 将一个元素推入由此双端队列表示的栈中（换句话说，在此双端队列的头部），如果可以在不违反容量限制的情况下立即执行此操作，
     * 如果没有可用空间，则抛出 {@code IllegalStateException}。
     *
     * <p>此方法等效于 {@link #addFirst}。
     *
     * @param e 要推入的元素
     * @throws IllegalStateException 如果由于容量限制此时无法添加元素
     * @throws ClassCastException 如果指定元素的类阻止其被添加到此双端队列中
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * @throws IllegalArgumentException 如果指定元素的某些属性阻止其被添加到此双端队列中
     */
    void push(E e);

    /**
     * 从由该双端队列表示的栈中弹出一个元素。换句话说，移除并返回该双端队列的第一个元素。
     *
     * <p>该方法等效于 {@link #removeFirst()}。
     *
     * @return 该双端队列前端的元素（即由该双端队列表示的栈的顶部元素）
     * @throws NoSuchElementException 如果该双端队列为空
     */
    E pop();


    // *** 集合方法 ***

    /**
     * 从该双端队列中移除指定元素的第一个匹配项。
     * 如果双端队列不包含该元素，则保持不变。
     * 更正式地说，移除第一个满足 {@code e} 的元素，使得
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * （如果存在这样的元素）。
     * 如果该双端队列包含指定元素，则返回 {@code true}
     * （或等效地，如果调用导致该双端队列发生变化）。
     *
     * <p>该方法等同于 {@link #removeFirstOccurrence(Object)}。
     *
     * @param o 要从该双端队列中移除的元素（如果存在）
     * @return {@code true} 如果该调用导致移除了一个元素
     * @throws ClassCastException 如果指定元素的类与该双端队列不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且该双端队列不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean remove(Object o);

    /**
     * 如果此双端队列包含指定元素，则返回 {@code true}。
     * 更正式地说，当且仅当此双端队列包含至少一个元素 {@code e} 满足
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt> 时，返回 {@code true}。
     *
     * @param o 要测试是否在此双端队列中的元素
     * @return {@code true} 如果此双端队列包含指定元素
     * @throws ClassCastException 如果指定元素的类型与此双端队列不兼容
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     * @throws NullPointerException 如果指定元素为 null 且此双端队列不允许 null 元素
     * (<a href="Collection.html#optional-restrictions">可选</a>)
     */
    boolean contains(Object o);

    /**
     * 返回此双端队列中的元素数量。
     *
     * @return 此双端队列中的元素数量
     */
    public int size();

    /**
     * 返回一个按正确顺序遍历此双端队列中元素的迭代器。
     * 元素将按从第一个（头部）到最后一个（尾部）的顺序返回。
     *
     * @return 一个按正确顺序遍历此双端队列中元素的迭代器
     */
    java.util.Iterator<E> iterator();

    /**
     * 返回一个按逆序遍历此双端队列中元素的迭代器。元素将按照从最后一个（尾部）到第一个（头部）的顺序返回。
     *
     * @return 一个按逆序遍历此双端队列中元素的迭代器
     */
    Iterator<E> descendingIterator();

}
