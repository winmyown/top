/*
 * 版权所有 (c) 1997, 2012, Oracle 和/或其附属公司。保留所有权利。
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
import java.util.RandomAccess;
import java.util.*;

/**
 * 该类提供了 {@link List} 接口的骨架实现，以最小化实现此接口所需的工作量，
 * 前提是底层数据存储支持“随机访问”（例如数组）。对于顺序访问的数据（例如链表），
 * 应优先使用 {@link AbstractSequentialList} 而不是该类。
 *
 * <p>要实现一个不可修改的列表，程序员只需扩展该类并提供
 * {@link #get(int)} 和 {@link List#size() size()} 方法的实现。
 *
 * <p>要实现一个可修改的列表，程序员必须额外重写
 * {@link #set(int, Object) set(int, E)} 方法（否则会抛出 {@code UnsupportedOperationException}）。
 * 如果列表是可变大小的，程序员还必须额外重写
 * {@link #add(int, Object) add(int, E)} 和 {@link #remove(int)} 方法。
 *
 * <p>程序员通常应提供一个无参构造函数和一个集合构造函数，
 * 如 {@link java.util.Collection} 接口规范中所建议的那样。
 *
 * <p>与其他抽象集合实现不同，程序员不需要提供迭代器实现；
 * 迭代器和列表迭代器由该类在“随机访问”方法之上实现：
 * {@link #get(int)}、
 * {@link #set(int, Object) set(int, E)}、
 * {@link #add(int, Object) add(int, E)} 和
 * {@link #remove(int)}。
 *
 * <p>该类中每个非抽象方法的文档详细描述了其实现。
 * 如果正在实现的集合允许更高效的实现，可以重写这些方法。
 *
 * <p>该类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @since 1.2
 */

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    /**
     * 唯一的构造函数。（通常由子类构造函数隐式调用。）
     */
    protected AbstractList() {
    }

    /**
     * 将指定元素追加到此列表的末尾（可选操作）。
     *
     * <p>支持此操作的列表可能会对可以添加到此列表的元素施加限制。特别是，一些
     * 列表会拒绝添加 null 元素，而其他列表会对可以添加的元素类型施加限制。列表
     * 类应在其文档中明确说明对可以添加的元素的任何限制。
     *
     * <p>此实现调用 {@code add(size(), e)}。
     *
     * <p>请注意，除非 {@link #add(int, Object) add(int, E)} 被重写，否则此实现会抛出
     * {@code UnsupportedOperationException}。
     *
     * @param e 要追加到此列表的元素
     * @return {@code true}（由 {@link java.util.Collection#add} 指定）
     * @throws UnsupportedOperationException 如果此列表不支持 {@code add} 操作
     * @throws ClassCastException 如果指定元素的类阻止其添加到此列表
     * @throws NullPointerException 如果指定元素为 null 且此列表不允许 null 元素
     * @throws IllegalArgumentException 如果此元素的某些属性阻止其添加到此列表
     */
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    abstract public E get(int index);

    /**
     * {@inheritDoc}
     *
     * <p>此实现始终抛出
     * {@code UnsupportedOperationException}。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现始终抛出
     * {@code UnsupportedOperationException}。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现始终抛出
     * {@code UnsupportedOperationException}。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }


    // 搜索操作

    /**
     * {@inheritDoc}
     *
     * <p>此实现首先获取一个列表迭代器（通过
     * {@code listIterator()}）。然后，它遍历列表，直到找到指定元素或到达列表末尾。
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public int indexOf(Object o) {
        ListIterator<E> it = listIterator();
        if (o==null) {
            while (it.hasNext())
                if (it.next()==null)
                    return it.previousIndex();
        } else {
            while (it.hasNext())
                if (o.equals(it.next()))
                    return it.previousIndex();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现首先获取一个指向列表末尾的列表迭代器（使用 {@code listIterator(size())}）。然后，它向后遍历列表，直到找到指定的元素或到达列表的开头。
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public int lastIndexOf(Object o) {
        ListIterator<E> it = listIterator(size());
        if (o==null) {
            while (it.hasPrevious())
                if (it.previous()==null)
                    return it.nextIndex();
        } else {
            while (it.hasPrevious())
                if (o.equals(it.previous()))
                    return it.nextIndex();
        }
        return -1;
    }


    // 批量操作

    /**
     * 移除该列表中的所有元素（可选操作）。
     * 调用此方法后，列表将为空。
     *
     * <p>此实现调用 {@code removeRange(0, size())}。
     *
     * <p>请注意，除非重写了 {@code remove(int index)} 或 {@code removeRange(int fromIndex, int toIndex)}，
     * 否则此实现将抛出 {@code UnsupportedOperationException}。
     *
     * @throws UnsupportedOperationException 如果该列表不支持 {@code clear} 操作
     */
    public void clear() {
        removeRange(0, size());
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现获取指定集合的迭代器并遍历它，将迭代器获得的元素逐个插入到此列表中的适当位置，使用 {@code add(int, E)}。
     * 许多实现将出于效率考虑重写此方法。
     *
     * <p>请注意，除非 {@link #add(int, Object) add(int, E)} 被重写，否则此实现会抛出
     * {@code UnsupportedOperationException}。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        boolean modified = false;
        for (E e : c) {
            add(index++, e);
            modified = true;
        }
        return modified;
    }


    // 迭代器

    /**
     * 返回一个按正确顺序遍历此列表中元素的迭代器。
     *
     * <p>此实现返回一个简单的迭代器接口实现，依赖于支持列表的 {@code size()}、
     * {@code get(int)} 和 {@code remove(int)} 方法。
     *
     * <p>请注意，除非列表的 {@code remove(int)} 方法被重写，否则此方法返回的迭代器
     * 在调用其 {@code remove} 方法时将抛出 {@link UnsupportedOperationException}。
     *
     * <p>此实现可以在检测到并发修改时抛出运行时异常，如（受保护的）{@link #modCount} 字段的规范中所述。
     *
     * @return 一个按正确顺序遍历此列表中元素的迭代器
     */
    public java.util.Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现返回 {@code listIterator(0)}。
     *
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现返回一个直接的 {@code ListIterator} 接口实现，该实现扩展了由 {@code iterator()} 方法返回的 {@code Iterator} 接口实现。
     * {@code ListIterator} 实现依赖于支持列表的 {@code get(int)}、{@code set(int, E)}、{@code add(int, E)} 和 {@code remove(int)} 方法。
     *
     * <p>请注意，此实现返回的列表迭代器在调用其 {@code remove}、{@code set} 和 {@code add} 方法时，
     * 除非列表的 {@code remove(int)}、{@code set(int, E)} 和 {@code add(int, E)} 方法被重写，否则将抛出 {@link UnsupportedOperationException}。
     *
     * <p>此实现可以在检测到并发修改时抛出运行时异常，如（受保护的）{@link #modCount} 字段的规范中所述。
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(final int index) {
        rangeCheckForAdd(index);

        return new ListItr(index);
    }

    private class Itr implements Iterator<E> {
        /**
         * 要由后续调用next返回的元素的索引。
         */
        int cursor = 0;

        /**
         * 由最近一次调用 next 或 previous 返回的元素的索引。
         * 如果此元素被 remove 调用删除，则重置为 -1。
         */
        int lastRet = -1;

        /**
         * 迭代器认为支持列表应该具有的 modCount 值。
         * 如果此期望被违反，迭代器将检测到并发修改。
         */
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }

        public E next() {
            checkForComodification();
            try {
                int i = cursor;
                E next = get(i);
                lastRet = i;
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                AbstractList.this.remove(lastRet);
                if (lastRet < cursor)
                    cursor--;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public E previous() {
            checkForComodification();
            try {
                int i = cursor - 1;
                E previous = get(i);
                lastRet = cursor = i;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                AbstractList.this.set(lastRet, e);
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                AbstractList.this.add(i, e);
                lastRet = -1;
                cursor = i + 1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>此实现返回一个继承自
     * {@code AbstractList} 的子列表。该子类在私有字段中存储了子列表在支持列表中的偏移量、子列表的大小
     * （在其生命周期内可以更改）以及支持列表的预期
     * {@code modCount} 值。子类有两个变体，其中一个实现了 {@code RandomAccess}。
     * 如果此列表实现了 {@code RandomAccess}，则返回的列表将是实现了 {@code RandomAccess} 的子类的实例。
     *
     * <p>子类的 {@code set(int, E)}、{@code get(int)}、
     * {@code add(int, E)}、{@code remove(int)}、{@code addAll(int,
     * Collection)} 和 {@code removeRange(int, int)} 方法都会在边界检查索引并调整偏移量后，
     * 委托给支持抽象列表的相应方法。{@code addAll(Collection c)} 方法仅返回 {@code addAll(size,
     * c)}。
     *
     * <p>{@code listIterator(int)} 方法返回一个“包装器对象”，
     * 该对象包装了支持列表上的列表迭代器，该迭代器是通过支持列表上的相应方法创建的。{@code iterator} 方法
     * 仅返回 {@code listIterator()}，而 {@code size} 方法
     * 仅返回子类的 {@code size} 字段。
     *
     * <p>所有方法首先检查支持列表的实际 {@code modCount} 是否等于其预期值，
     * 如果不相等，则抛出 {@code ConcurrentModificationException}。
     *
     * @throws IndexOutOfBoundsException 如果端点索引值超出范围
     *         {@code (fromIndex < 0 || toIndex > size)}
     * @throws IllegalArgumentException 如果端点索引顺序错误
     *         {@code (fromIndex > toIndex)}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        return (this instanceof RandomAccess ?
                new RandomAccessSubList<>(this, fromIndex, toIndex) :
                new SubList<>(this, fromIndex, toIndex));
    }

    // 比较和哈希

    /**
     * 将指定对象与此列表进行相等性比较。当且仅当指定对象也是一个列表、两个列表大小相同、并且两个列表中所有对应的元素对都<i>相等</i>时，返回 {@code true}。（两个元素 {@code e1} 和 {@code e2} 是<i>相等</i>的，如果 {@code (e1==null ? e2==null : e1.equals(e2))}。）换句话说，如果两个列表以相同的顺序包含相同的元素，则它们被定义为相等。<p>
     *
     * 此实现首先检查指定对象是否为此列表。如果是，则返回 {@code true}；如果不是，则检查指定对象是否为一个列表。如果不是，则返回 {@code false}；如果是，则遍历两个列表，比较对应的元素对。如果任何比较返回 {@code false}，则此方法返回 {@code false}。如果任一迭代器在另一个之前用完元素，则返回 {@code false}（因为列表长度不等）；否则，当迭代完成时返回 {@code true}。
     *
     * @param o 要与此列表进行相等性比较的对象
     * @return {@code true} 如果指定对象与此列表相等
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        ListIterator<E> e1 = listIterator();
        ListIterator<?> e2 = ((List<?>) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    /**
     * 返回此列表的哈希码值。
     *
     * <p>此实现使用的代码与 {@link List#hashCode} 方法文档中定义的列表哈希函数代码完全相同。
     *
     * @return 此列表的哈希码值
     */
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
    }

    /**
     * 从此列表中移除所有索引介于 {@code fromIndex}（包含）和 {@code toIndex}（不包含）之间的元素。
     * 将任何后续元素向左移动（减少它们的索引）。
     * 此调用会将列表缩短 {@code (toIndex - fromIndex)} 个元素。
     * （如果 {@code toIndex==fromIndex}，此操作无效。）
     *
     * <p>此方法由列表及其子列表上的 {@code clear} 操作调用。
     * 覆盖此方法以利用列表实现的内部结构，可以<i>显著</i>提高此列表及其子列表上 {@code clear} 操作的性能。
     *
     * <p>此实现获取一个位于 {@code fromIndex} 之前的列表迭代器，并重复调用 {@code ListIterator.next}
     * 后跟 {@code ListIterator.remove}，直到整个范围被移除。<b>注意：如果 {@code ListIterator.remove} 需要线性时间，
     * 此实现需要二次时间。</b>
     *
     * @param fromIndex 要移除的第一个元素的索引
     * @param toIndex 要移除的最后一个元素之后的索引
     */
    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i=0, n=toIndex-fromIndex; i<n; i++) {
            it.next();
            it.remove();
        }
    }

    /**
     * 该列表被<i>结构性修改</i>的次数。
     * 结构性修改是指那些改变列表大小的操作，或者以其他方式干扰列表，
     * 使得正在进行的迭代可能产生不正确的结果。
     *
     * <p>该字段由 {@code iterator} 和 {@code listIterator} 方法返回的迭代器和列表迭代器实现使用。
     * 如果该字段的值意外更改，迭代器（或列表迭代器）将在响应 {@code next}、{@code remove}、
     * {@code previous}、{@code set} 或 {@code add} 操作时抛出 {@code ConcurrentModificationException}。
     * 这提供了<i>快速失败</i>行为，而不是在迭代期间面对并发修改时的非确定性行为。
     *
     * <p><b>子类对该字段的使用是可选的。</b> 如果子类希望提供快速失败的迭代器（和列表迭代器），
     * 则只需在其 {@code add(int, E)} 和 {@code remove(int)} 方法（以及它覆盖的任何其他导致列表结构性修改的方法）中增加该字段的值。
     * 对 {@code add(int, E)} 或 {@code remove(int)} 的单个调用必须使该字段的值增加不超过一次，
     * 否则迭代器（和列表迭代器）将抛出虚假的 {@code ConcurrentModificationExceptions}。
     * 如果实现不希望提供快速失败的迭代器，则可以忽略该字段。
     */
    protected transient int modCount = 0;

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size();
    }
}

