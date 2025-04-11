
package org.top.java.netty.source.buffer;

import io.netty.util.internal.StringUtil;

import java.util.List;

/**
 * Exposed metric for {@link PooledByteBufAllocator}.
 */

/**
 * {@link PooledByteBufAllocator} 暴露的指标。
 */
@SuppressWarnings("deprecation")
public final class PooledByteBufAllocatorMetric implements ByteBufAllocatorMetric {

    private final PooledByteBufAllocator allocator;

    PooledByteBufAllocatorMetric(PooledByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    /**
     * Return the number of heap arenas.
     */

    /**
     * 返回堆内存区域的数量。
     */
    public int numHeapArenas() {
        return allocator.numHeapArenas();
    }

    /**
     * Return the number of direct arenas.
     */

    /**
     * 返回直接竞技场的数量。
     */
    public int numDirectArenas() {
        return allocator.numDirectArenas();
    }

    /**
     * Return a {@link List} of all heap {@link PoolArenaMetric}s that are provided by this pool.
     */

    /**
     * 返回由该池提供的所有堆 {@link PoolArenaMetric} 的 {@link List}。
     */
    public List<PoolArenaMetric> heapArenas() {
        return allocator.heapArenas();
    }

    /**
     * Return a {@link List} of all direct {@link PoolArenaMetric}s that are provided by this pool.
     */

    /**
     * 返回由该池提供的所有直接 {@link PoolArenaMetric} 的 {@link List}。
     */
    public List<PoolArenaMetric> directArenas() {
        return allocator.directArenas();
    }

    /**
     * Return the number of thread local caches used by this {@link PooledByteBufAllocator}.
     */

    /**
     * 返回此 {@link PooledByteBufAllocator} 使用的线程本地缓存数量。
     */
    public int numThreadLocalCaches() {
        return allocator.numThreadLocalCaches();
    }

    /**
     * Return the size of the tiny cache.
     *
     * @deprecated Tiny caches have been merged into small caches.
     */

    /**
     * 返回微小缓存的大小。
     *
     * @deprecated 微小缓存已合并到小型缓存中。
     */
    @Deprecated
    public int tinyCacheSize() {
        return allocator.tinyCacheSize();
    }

    /**
     * Return the size of the small cache.
     */

    /**
     * 返回小缓存的大小。
     */
    public int smallCacheSize() {
        return allocator.smallCacheSize();
    }

    /**
     * Return the size of the normal cache.
     */

    /**
     * 返回普通缓存的大小。
     */
    public int normalCacheSize() {
        return allocator.normalCacheSize();
    }

    /**
     * Return the chunk size for an arena.
     */

    /**
     * 返回竞技场的区块大小。
     */
    public int chunkSize() {
        return allocator.chunkSize();
    }

    @Override
    public long usedHeapMemory() {
        return allocator.usedHeapMemory();
    }

    @Override
    public long usedDirectMemory() {
        return allocator.usedDirectMemory();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(StringUtil.simpleClassName(this))
                .append("(usedHeapMemory: ").append(usedHeapMemory())
                .append("; usedDirectMemory: ").append(usedDirectMemory())
                .append("; numHeapArenas: ").append(numHeapArenas())
                .append("; numDirectArenas: ").append(numDirectArenas())
                .append("; smallCacheSize: ").append(smallCacheSize())
                .append("; normalCacheSize: ").append(normalCacheSize())
                .append("; numThreadLocalCaches: ").append(numThreadLocalCaches())
                .append("; chunkSize: ").append(chunkSize()).append(')');
        return sb.toString();
    }
}
