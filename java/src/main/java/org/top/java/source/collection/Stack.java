/*
 * 版权所有 (c) 1994, 2010, Oracle 和/或其附属公司。保留所有权利。
 * ORACLE 专有/机密。使用须遵守许可条款。
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

import java.util.Deque;
import java.util.EmptyStackException;
import java.util.Vector;

/**
 * <code>Stack</code> 类表示一个后进先出
 * (LIFO) 的对象堆栈。它通过五个操作扩展了 <tt>Vector</tt> 类，
 * 允许将向量视为堆栈。提供了常见的 <tt>push</tt> 和 <tt>pop</tt> 操作，
 * 以及一个用于 <tt>peek</tt> 查看堆栈顶部项的方法，一个用于测试
 * 堆栈是否 <tt>empty</tt> 的方法，以及一个用于 <tt>search</tt>
 * 堆栈中项并发现其距顶部多远的方法。
 * <p>
 * 当堆栈首次创建时，它不包含任何项。
 *
 * <p>更完整和一致的 LIFO 堆栈操作集由
 * {@link Deque} 接口及其实现提供，应优先使用此类。
 * 例如：
 * <pre>   {@code
 *   Deque<Integer> stack = new ArrayDeque<Integer>();}</pre>
 *
 * @author  Jonathan Payne
 * @since   JDK1.0
 */
public
class Stack<E> extends java.util.Vector<E> {
    /**
     * 创建一个空的栈。
     */
    public Stack() {
    }

    /**
     * 将项目推送到此堆栈的顶部。这与以下代码具有完全相同的效果：
     * <blockquote><pre>
     * addElement(item)</pre></blockquote>
     *
     * @param   项目   要推送到此堆栈上的项目。
     * @return  <code>项目</code>参数。
     * @see     Vector#addElement
     */
    public E push(E item) {
        addElement(item);

        return item;
    }

    /**
     * 移除并返回此栈顶部的对象。
     *
     * @return  此栈顶部的对象（<tt>Vector</tt>对象的最后一个元素）。
     * @throws EmptyStackException  如果此栈为空。
     */
    public synchronized E pop() {
        E       obj;
        int     len = size();

        obj = peek();
        removeElementAt(len - 1);

        return obj;
    }

    /**
     * 查看此堆栈顶部的对象而不将其从堆栈中移除。
     *
     * @return  此堆栈顶部的对象（<tt>Vector</tt> 对象的最后一个元素）。
     * @throws  EmptyStackException  如果此堆栈为空。
     */
    public synchronized E peek() {
        int     len = size();

        if (len == 0)
            throw new EmptyStackException();
        return elementAt(len - 1);
    }

    /**
     * 测试此栈是否为空。
     *
     * @return  <code>true</code> 当且仅当此栈不包含任何元素；
     *          <code>false</code> 否则。
     */
    public boolean empty() {
        return size() == 0;
    }

    /**
     * 返回对象在此堆栈中的基于1的位置。
     * 如果对象 <tt>o</tt> 作为此堆栈中的一个项出现，此
     * 方法返回从堆栈顶部到最靠近堆栈顶部的该对象的距离；
     * 堆栈顶部的项被视为距离为 <tt>1</tt>。使用 <tt>equals</tt>
     * 方法将 <tt>o</tt> 与此堆栈中的项进行比较。
     *
     * @param   o   所需的对象。
     * @return  从堆栈顶部到对象位置的基于1的距离；
     *          返回值 <code>-1</code> 表示对象不在堆栈中。
     */
    public synchronized int search(Object o) {
        int i = lastIndexOf(o);

        if (i >= 0) {
            return size() - i;
        }
        return -1;
    }

    /** 使用JDK 1.0.2的serialVersionUID以确保互操作性 */
    private static final long serialVersionUID = 1224463164541339165L;
}
