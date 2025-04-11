

package org.top.java.netty.source.buffer;

import java.util.List;

/**
 * Expose metrics for an arena.
 */

/**
 * 暴露竞技场的指标。
 */
public interface PoolArenaMetric extends SizeClassesMetric {

    /**
     * Returns the number of thread caches backed by this arena.
     */

    /**
     * 返回由此arena支持的线程缓存数量。
     */
    int numThreadCaches();

    /**
     * Returns the number of tiny sub-pages for the arena.
     *
     * @deprecated Tiny sub-pages have been merged into small sub-pages.
     */

    /**
     * 返回竞技场中的微小子页面数量。
     *
     * @deprecated 微小子页面已合并到小子页面中。
     */
    @Deprecated
    int numTinySubpages();

    /**
     * Returns the number of small sub-pages for the arena.
     */

    /**
     * 返回竞技场的小子页面数量。
     */
    int numSmallSubpages();

    /**
     * Returns the number of chunk lists for the arena.
     */

    /**
     * 返回竞技场的区块列表数量。
     */
    int numChunkLists();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for tiny sub-pages.
     *
     * @deprecated Tiny sub-pages have been merged into small sub-pages.
     */

    /**
     * 返回一个不可修改的{@link List}，其中包含用于微小子页面的{@link PoolSubpageMetric}。
     *
     * @deprecated 微小子页面已合并到小子页面中。
     */
    @Deprecated
    List<PoolSubpageMetric> tinySubpages();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for small sub-pages.
     */

    /**
     * 返回一个不可修改的{@link List}，其中包含用于小内存页的{@link PoolSubpageMetric}。
     */
    List<PoolSubpageMetric> smallSubpages();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolChunkListMetric}s.
     */

    /**
     * 返回一个不可修改的 {@link List}，其中包含 {@link PoolChunkListMetric}。
     */
    List<PoolChunkListMetric> chunkLists();

    /**
     * Return the number of allocations done via the arena. This includes all sizes.
     */

    /**
     * 返回通过arena分配的次数。这包括所有大小的分配。
     */
    long numAllocations();

    /**
     * Return the number of tiny allocations done via the arena.
     *
     * @deprecated Tiny allocations have been merged into small allocations.
     */

    /**
     * 返回通过arena进行的微小分配的数量。
     *
     * @deprecated 微小分配已合并到小分配中。
     */
    @Deprecated
    long numTinyAllocations();

    /**
     * Return the number of small allocations done via the arena.
     */

    /**
     * 返回通过arena进行的小分配的数量。
     */
    long numSmallAllocations();

    /**
     * Return the number of normal allocations done via the arena.
     */

    /**
     * 返回通过竞技场完成的普通分配的数量。
     */
    long numNormalAllocations();

    /**
     * Return the number of huge allocations done via the arena.
     */

    /**
     * 返回通过arena完成的巨大分配的数量。
     */
    long numHugeAllocations();

    /**
     * Return the number of deallocations done via the arena. This includes all sizes.
     */

    /**
     * 返回通过竞技场完成的释放次数。这包括所有大小。
     */
    long numDeallocations();

    /**
     * Return the number of tiny deallocations done via the arena.
     *
     * @deprecated Tiny deallocations have been merged into small deallocations.
     */

    /**
     * 返回通过arena进行的微小内存释放次数。
     *
     * @deprecated 微小内存释放已合并到小内存释放中。
     */
    @Deprecated
    long numTinyDeallocations();

    /**
     * Return the number of small deallocations done via the arena.
     */

    /**
     * 返回通过arena进行的小型释放操作的次数。
     */
    long numSmallDeallocations();

    /**
     * Return the number of normal deallocations done via the arena.
     */

    /**
     * 返回通过竞技场完成的正常释放次数。
     */
    long numNormalDeallocations();

    /**
     * Return the number of huge deallocations done via the arena.
     */

    /**
     * 返回通过竞技场完成的巨大释放次数。
     */
    long numHugeDeallocations();

    /**
     * Return the number of currently active allocations.
     */

    /**
     * 返回当前活跃的分配数量。
     */
    long numActiveAllocations();

    /**
     * Return the number of currently active tiny allocations.
     *
     * @deprecated Tiny allocations have been merged into small allocations.
     */

    /**
     * 返回当前活跃的微小分配的数量。
     *
     * @deprecated 微小分配已合并到小分配中。
     */
    @Deprecated
    long numActiveTinyAllocations();

    /**
     * Return the number of currently active small allocations.
     */

    /**
     * 返回当前活跃的小分配数量。
     */
    long numActiveSmallAllocations();

    /**
     * Return the number of currently active normal allocations.
     */

    /**
     * 返回当前活跃的正常分配数量。
     */
    long numActiveNormalAllocations();

    /**
     * Return the number of currently active huge allocations.
     */

    /**
     * 返回当前活跃的大内存分配的数量。
     */
    long numActiveHugeAllocations();

    /**
     * Return the number of active bytes that are currently allocated by the arena.
     */

    /**
     * 返回当前由arena分配的活跃字节数。
     */
    long numActiveBytes();
}
