
package org.top.java.netty.source.buffer;

/**
 * Metrics for a list of chunks.
 */

/**
 * 用于块列表的指标。
 */
public interface PoolChunkListMetric extends Iterable<PoolChunkMetric> {

    /**
     * Return the minimum usage of the chunk list before which chunks are promoted to the previous list.
     */

    /**
     * 返回在将块提升到前一个列表之前，块列表的最小使用量。
     */
    int minUsage();

    /**
     * Return the maximum usage of the chunk list after which chunks are promoted to the next list.
     */

    /**
     * 返回块列表的最大使用次数，超过该次数后块将被提升到下一个列表。
     */
    int maxUsage();
}
