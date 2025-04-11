
package org.top.java.netty.source.util.internal;

import java.util.Queue;

public interface PriorityQueue<T> extends Queue<T> {
    /**
     * Same as {@link #remove(Object)} but typed using generics.
     */
    /**
     * 与 {@link #remove(Object)} 相同，但使用泛型进行类型化。
     */
    boolean removeTyped(T node);

    /**
     * Same as {@link #contains(Object)} but typed using generics.
     */

    /**
     * 与 {@link #contains(Object)} 相同，但使用泛型进行类型化。
     */
    boolean containsTyped(T node);

    /**
     * Notify the queue that the priority for {@code node} has changed. The queue will adjust to ensure the priority
     * queue properties are maintained.
     * @param node An object which is in this queue and the priority may have changed.
     */

    /**
     * 通知队列 {@code node} 的优先级已更改。队列将进行调整以确保优先级队列属性得到维护。
     * @param node 队列中的一个对象，其优先级可能已更改。
     */
    void priorityChanged(T node);

    /**
     * Removes all of the elements from this {@link PriorityQueue} without calling
     * {@link PriorityQueueNode#priorityQueueIndex(DefaultPriorityQueue)} or explicitly removing references to them to
     * allow them to be garbage collected. This should only be used when it is certain that the nodes will not be
     * re-inserted into this or any other {@link PriorityQueue} and it is known that the {@link PriorityQueue} itself
     * will be garbage collected after this call.
     */

    /**
     * 从该 {@link PriorityQueue} 中移除所有元素，而不调用
     * {@link PriorityQueueNode#priorityQueueIndex(DefaultPriorityQueue)} 或显式移除对它们的引用，
     * 以允许它们被垃圾回收。仅当确定节点不会被重新插入到该或任何其他 {@link PriorityQueue} 中，
     * 并且已知 {@link PriorityQueue} 本身在此调用后将被垃圾回收时，才应使用此方法。
     */
    void clearIgnoringIndexes();
}
