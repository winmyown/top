
package org.top.java.netty.source.util.internal;

/**
 * Provides methods for {@link DefaultPriorityQueue} to maintain internal state. These methods should generally not be
 * used outside the scope of {@link DefaultPriorityQueue}.
 */

/**
 * 为{@link DefaultPriorityQueue}提供维护内部状态的方法。这些方法通常不应在{@link DefaultPriorityQueue}的范围之外使用。
 */
public interface PriorityQueueNode {
    /**
     * This should be used to initialize the storage returned by {@link #priorityQueueIndex(DefaultPriorityQueue)}.
     */
    /**
     * 这应该用于初始化由 {@link #priorityQueueIndex(DefaultPriorityQueue)} 返回的存储。
     */
    int INDEX_NOT_IN_QUEUE = -1;

    /**
     * Get the last value set by {@link #priorityQueueIndex(DefaultPriorityQueue, int)} for the value corresponding to
     * {@code queue}.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     */

    /**
     * 获取由 {@link #priorityQueueIndex(DefaultPriorityQueue, int)} 为与 {@code queue} 对应的值设置的最后一个值。
     * <p>
     * 从此方法抛出异常将导致未定义的行为。
     */
    int priorityQueueIndex(DefaultPriorityQueue<?> queue);

    /**
     * Used by {@link DefaultPriorityQueue} to maintain state for an element in the queue.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     * @param queue The queue for which the index is being set.
     * @param i The index as used by {@link DefaultPriorityQueue}.
     */

    /**
     * 由 {@link DefaultPriorityQueue} 使用，用于维护队列中元素的状态。
     * <p>
     * 从此方法抛出异常将导致未定义的行为。
     * @param queue 设置索引的队列。
     * @param i 由 {@link DefaultPriorityQueue} 使用的索引。
     */
    void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i);
}
