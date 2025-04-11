
package org.top.java.netty.source.buffer;

import io.netty.util.internal.LongCounter;
import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Description of algorithm for PageRun/PoolSubpage allocation from PoolChunk
 *
 * Notation: The following terms are important to understand the code
 * > page  - a page is the smallest unit of memory chunk that can be allocated
 * > run   - a run is a collection of pages
 * > chunk - a chunk is a collection of runs
 * > in this code chunkSize = maxPages * pageSize
 *
 * To begin we allocate a byte array of size = chunkSize
 * Whenever a ByteBuf of given size needs to be created we search for the first position
 * in the byte array that has enough empty space to accommodate the requested size and
 * return a (long) handle that encodes this offset information, (this memory segment is then
 * marked as reserved so it is always used by exactly one ByteBuf and no more)
 *
 * For simplicity all sizes are normalized according to {@link PoolArena#size2SizeIdx(int)} method.
 * This ensures that when we request for memory segments of size > pageSize the normalizedCapacity
 * equals the next nearest size in {@link SizeClasses}.
 *
 *
 *  A chunk has the following layout:
 *
 *     /-----------------
 *     | run             |
 *     |                 |
 *     |                 |
 *     |-----------------|
 *     | run             |
 *     |                 |
 *     |-----------------|
 *     | unalloctated    |
 *     | (freed)         |
 *     |                 |
 *     |-----------------|
 *     | subpage         |
 *     |-----------------|
 *     | unallocated     |
 *     | (freed)         |
 *     | ...             |
 *     | ...             |
 *     | ...             |
 *     |                 |
 *     |                 |
 *     |                 |
 *     -----------------/
 *
 *
 * handle:
 * -------
 * a handle is a long number, the bit layout of a run looks like:
 *
 * oooooooo ooooooos ssssssss ssssssue bbbbbbbb bbbbbbbb bbbbbbbb bbbbbbbb
 *
 * o: runOffset (page offset in the chunk), 15bit
 * s: size (number of pages) of this run, 15bit
 * u: isUsed?, 1bit
 * e: isSubpage?, 1bit
 * b: bitmapIdx of subpage, zero if it's not subpage, 32bit
 *
 * runsAvailMap:
 * ------
 * a map which manages all runs (used and not in used).
 * For each run, the first runOffset and last runOffset are stored in runsAvailMap.
 * key: runOffset
 * value: handle
 *
 * runsAvail:
 * ----------
 * an array of {@link PriorityQueue}.
 * Each queue manages same size of runs.
 * Runs are sorted by offset, so that we always allocate runs with smaller offset.
 *
 *
 * Algorithm:
 * ----------
 *
 *   As we allocate runs, we update values stored in runsAvailMap and runsAvail so that the property is maintained.
 *
 * Initialization -
 *  In the beginning we store the initial run which is the whole chunk.
 *  The initial run:
 *  runOffset = 0
 *  size = chunkSize
 *  isUsed = no
 *  isSubpage = no
 *  bitmapIdx = 0
 *
 *
 * Algorithm: [allocateRun(size)]
 * ----------
 * 1) find the first avail run using in runsAvails according to size
 * 2) if pages of run is larger than request pages then split it, and save the tailing run
 *    for later using
 *
 * Algorithm: [allocateSubpage(size)]
 * ----------
 * 1) find a not full subpage according to size.
 *    if it already exists just return, otherwise allocate a new PoolSubpage and call init()
 *    note that this subpage object is added to subpagesPool in the PoolArena when we init() it
 * 2) call subpage.allocate()
 *
 * Algorithm: [free(handle, length, nioBuffer)]
 * ----------
 * 1) if it is a subpage, return the slab back into this subpage
 * 2) if the subpage is not used or it is a run, then start free this run
 * 3) merge continuous avail runs
 * 4) save the merged run
 *
 */

/**
 * PageRun/PoolSubpage从PoolChunk分配的算法描述
 *
 * 符号说明：以下术语对于理解代码非常重要
 * > page  - 页面是内存块中可以分配的最小单位
 * > run   - run是页面的集合
 * > chunk - chunk是run的集合
 * > 在本代码中 chunkSize = maxPages * pageSize
 *
 * 首先我们分配一个大小为 chunkSize 的字节数组
 * 每当需要创建一个给定大小的ByteBuf时，我们在字节数组中搜索第一个有足够空闲空间来容纳请求大小的位置，
 * 并返回一个（long）句柄，该句柄编码了此偏移信息（此内存段随后被标记为保留，因此它始终只由一个ByteBuf使用，且不再被其他使用）
 *
 * 为简单起见，所有大小都根据 {@link PoolArena#size2SizeIdx(int)} 方法进行归一化。
 * 这确保了当我们请求大小 > pageSize 的内存段时，normalizedCapacity 等于 {@link SizeClasses} 中的下一个最近大小。
 *
 *
 * 一个chunk的布局如下：
 *
 *     /-----------------\
 *     | run             |
 *     |                 |
 *     |                 |
 *     |-----------------|
 *     | run             |
 *     |                 |
 *     |-----------------|
 *     | unalloctated    |
 *     | (freed)         |
 *     |                 |
 *     |-----------------|
 *     | subpage         |
 *     |-----------------|
 *     | unallocated     |
 *     | (freed)         |
 *     | ...             |
 *     | ...             |
 *     | ...             |
 *     |                 |
 *     |                 |
 *     |                 |
 *     \-----------------/
 *
 *
 * 句柄：
 * -------
 * 句柄是一个long数字，run的位布局如下：
 *
 * oooooooo ooooooos ssssssss ssssssue bbbbbbbb bbbbbbbb bbbbbbbb bbbbbbbb
 *
 * o: runOffset (chunk中的页面偏移量), 15bit
 * s: 此run的大小（页面数）, 15bit
 * u: 是否已使用?, 1bit
 * e: 是否是subpage?, 1bit
 * b: subpage的bitmapIdx，如果不是subpage则为零, 32bit
 *
 * runsAvailMap:
 * ------
 * 一个管理所有run（已使用和未使用）的map。
 * 对于每个run，第一个runOffset和最后一个runOffset存储在runsAvailMap中。
 * 键: runOffset
 * 值: 句柄
 *
 * runsAvail:
 * ----------
 * 一个 {@link PriorityQueue} 数组。
 * 每个队列管理相同大小的run。
 * run按偏移量排序，因此我们总是分配偏移量较小的run。
 *
 *
 * 算法：
 * ----------
 *
 *   当我们分配run时，我们更新存储在runsAvailMap和runsAvail中的值，以保持属性。
 *
 * 初始化 -
 *  开始时我们存储初始run，即整个chunk。
 *  初始run：
 *  runOffset = 0
 *  size = chunkSize
 *  isUsed = 否
 *  isSubpage = 否
 *  bitmapIdx = 0
 *
 *
 * 算法: [allocateRun(size)]
 * ----------
 * 1) 根据大小在runsAvails中找到第一个可用的run
 * 2) 如果run的页面数大于请求的页面数，则将其拆分，并保存尾部的run以备后用
 *
 * 算法: [allocateSubpage(size)]
 * ----------
 * 1) 根据大小找到一个未满的subpage。
 *    如果它已经存在，则直接返回，否则分配一个新的PoolSubpage并调用init()
 *    注意，当我们调用init()时，此subpage对象会添加到PoolArena中的subpagesPool中
 * 2) 调用subpage.allocate()
 *
 * 算法: [free(handle, length, nioBuffer)]
 * ----------
 * 1) 如果它是一个subpage，则将slab返回到此subpage中
 * 2) 如果subpage未使用或它是一个run，则开始释放此run
 * 3) 合并连续的可用run
 * 4) 保存合并后的run
 *
 */
final class PoolChunk<T> implements PoolChunkMetric {
    private static final int SIZE_BIT_LENGTH = 15;
    private static final int INUSED_BIT_LENGTH = 1;
    private static final int SUBPAGE_BIT_LENGTH = 1;
    private static final int BITMAP_IDX_BIT_LENGTH = 32;

    static final int IS_SUBPAGE_SHIFT = BITMAP_IDX_BIT_LENGTH;
    static final int IS_USED_SHIFT = SUBPAGE_BIT_LENGTH + IS_SUBPAGE_SHIFT;
    static final int SIZE_SHIFT = INUSED_BIT_LENGTH + IS_USED_SHIFT;
    static final int RUN_OFFSET_SHIFT = SIZE_BIT_LENGTH + SIZE_SHIFT;

    final PoolArena<T> arena;
    final Object base;
    final T memory;
    final boolean unpooled;

    /**
     * store the first page and last page of each avail run
     */

    /**
     * 存储每个可用运行的首页和末页
     */
    private final LongLongHashMap runsAvailMap;

    /**
     * manage all avail runs
     */

    /**
     * 管理所有可用的运行
     */
    private final LongPriorityQueue[] runsAvail;

    private final ReentrantLock runsAvailLock;

    /**
     * manage all subpages in this chunk
     */

    /**
     * 管理此块中的所有子页面
     */
    private final PoolSubpage<T>[] subpages;

    /**
     * Accounting of pinned memory – memory that is currently in use by ByteBuf instances.
     */

    /**
     * 已固定内存的统计——当前被ByteBuf实例使用的内存。
     */
    private final LongCounter pinnedBytes = PlatformDependent.newLongCounter();

    private final int pageSize;
    private final int pageShifts;
    private final int chunkSize;

    // Use as cache for ByteBuffer created from the memory. These are just duplicates and so are only a container

    // 用作从内存创建的ByteBuffer的缓存。这些只是副本，因此仅作为容器
    // around the memory itself. These are often needed for operations within the Pooled*ByteBuf and so
    // 围绕内存本身。这些通常在 Pooled*ByteBuf 中的操作中是必需的，因此
    // may produce extra GC, which can be greatly reduced by caching the duplicates.
    // 可能会产生额外的GC，通过缓存重复项可以大大减少这种情况。
    //
    // This may be null if the PoolChunk is unpooled as pooling the ByteBuffer instances does not make any sense here.
    // 如果 PoolChunk 未被池化，则此值可能为 null，因为在此处池化 ByteBuffer 实例没有意义。
    private final Deque<ByteBuffer> cachedNioBuffers;

    int freeBytes;

    PoolChunkList<T> parent;
    PoolChunk<T> prev;
    PoolChunk<T> next;

    // TODO: Test if adding padding helps under contention

    // TODO: 测试在竞争情况下添加填充是否有帮助
    //private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;
    //private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;

    @SuppressWarnings("unchecked")
    PoolChunk(PoolArena<T> arena, Object base, T memory, int pageSize, int pageShifts, int chunkSize, int maxPageIdx) {
        unpooled = false;
        this.arena = arena;
        this.base = base;
        this.memory = memory;
        this.pageSize = pageSize;
        this.pageShifts = pageShifts;
        this.chunkSize = chunkSize;
        freeBytes = chunkSize;

        runsAvail = newRunsAvailqueueArray(maxPageIdx);
        runsAvailLock = new ReentrantLock();
        runsAvailMap = new LongLongHashMap(-1);
        subpages = new PoolSubpage[chunkSize >> pageShifts];

        //insert initial run, offset = 0, pages = chunkSize / pageSize

        //插入初始运行，偏移量 = 0，页数 = 块大小 / 页大小
        int pages = chunkSize >> pageShifts;
        long initHandle = (long) pages << SIZE_SHIFT;
        insertAvailRun(0, pages, initHandle);

        cachedNioBuffers = new ArrayDeque<ByteBuffer>(8);
    }

    /** Creates a special chunk that is not pooled. */

    /** 创建一个不被池化的特殊块。 */
    PoolChunk(PoolArena<T> arena, Object base, T memory, int size) {
        unpooled = true;
        this.arena = arena;
        this.base = base;
        this.memory = memory;
        pageSize = 0;
        pageShifts = 0;
        runsAvailMap = null;
        runsAvail = null;
        runsAvailLock = null;
        subpages = null;
        chunkSize = size;
        cachedNioBuffers = null;
    }

    private static LongPriorityQueue[] newRunsAvailqueueArray(int size) {
        LongPriorityQueue[] queueArray = new LongPriorityQueue[size];
        for (int i = 0; i < queueArray.length; i++) {
            queueArray[i] = new LongPriorityQueue();
        }
        return queueArray;
    }

    private void insertAvailRun(int runOffset, int pages, long handle) {
        int pageIdxFloor = arena.pages2pageIdxFloor(pages);
        LongPriorityQueue queue = runsAvail[pageIdxFloor];
        queue.offer(handle);

        //insert first page of run

        //插入第一页的运行
        insertAvailRun0(runOffset, handle);
        if (pages > 1) {
            //insert last page of run
            //插入最后一页的运行
            insertAvailRun0(lastPage(runOffset, pages), handle);
        }
    }

    private void insertAvailRun0(int runOffset, long handle) {
        long pre = runsAvailMap.put(runOffset, handle);
        assert pre == -1;
    }

    private void removeAvailRun(long handle) {
        int pageIdxFloor = arena.pages2pageIdxFloor(runPages(handle));
        LongPriorityQueue queue = runsAvail[pageIdxFloor];
        removeAvailRun(queue, handle);
    }

    private void removeAvailRun(LongPriorityQueue queue, long handle) {
        queue.remove(handle);

        int runOffset = runOffset(handle);
        int pages = runPages(handle);
        //remove first page of run
        //移除运行的第一页
        runsAvailMap.remove(runOffset);
        if (pages > 1) {
            //remove last page of run
            //移除运行的最后一页
            runsAvailMap.remove(lastPage(runOffset, pages));
        }
    }

    private static int lastPage(int runOffset, int pages) {
        return runOffset + pages - 1;
    }

    private long getAvailRunByOffset(int runOffset) {
        return runsAvailMap.get(runOffset);
    }

    @Override
    public int usage() {
        final int freeBytes;
        arena.lock();
        try {
            freeBytes = this.freeBytes;
        } finally {
            arena.unlock();
        }
        return usage(freeBytes);
    }

    private int usage(int freeBytes) {
        if (freeBytes == 0) {
            return 100;
        }

        int freePercentage = (int) (freeBytes * 100L / chunkSize);
        if (freePercentage == 0) {
            return 99;
        }
        return 100 - freePercentage;
    }

    boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int sizeIdx, PoolThreadCache cache) {
        final long handle;
        if (sizeIdx <= arena.smallMaxSizeIdx) {
            // small
            // 小的
            handle = allocateSubpage(sizeIdx);
            if (handle < 0) {
                return false;
            }
            assert isSubpage(handle);
        } else {
            // normal
            // 正常
            // runSize must be multiple of pageSize
            // runSize 必须是 pageSize 的倍数
            int runSize = arena.sizeIdx2size(sizeIdx);
            handle = allocateRun(runSize);
            if (handle < 0) {
                return false;
            }
            assert !isSubpage(handle);
        }

        ByteBuffer nioBuffer = cachedNioBuffers != null? cachedNioBuffers.pollLast() : null;
        initBuf(buf, nioBuffer, handle, reqCapacity, cache);
        return true;
    }

    private long allocateRun(int runSize) {
        int pages = runSize >> pageShifts;
        int pageIdx = arena.pages2pageIdx(pages);

        runsAvailLock.lock();
        try {
            //find first queue which has at least one big enough run
            //找到第一个至少有一个足够大的运行的队列
            int queueIdx = runFirstBestFit(pageIdx);
            if (queueIdx == -1) {
                return -1;
            }

            //get run with min offset in this queue

            //获取队列中最小偏移量的运行
            LongPriorityQueue queue = runsAvail[queueIdx];
            long handle = queue.poll();

            assert handle != LongPriorityQueue.NO_VALUE && !isUsed(handle) : "invalid handle: " + handle;

            removeAvailRun(queue, handle);

            if (handle != -1) {
                handle = splitLargeRun(handle, pages);
            }

            int pinnedSize = runSize(pageShifts, handle);
            freeBytes -= pinnedSize;
            return handle;
        } finally {
            runsAvailLock.unlock();
        }
    }

    private int calculateRunSize(int sizeIdx) {
        int maxElements = 1 << pageShifts - SizeClasses.LOG2_QUANTUM;
        int runSize = 0;
        int nElements;

        final int elemSize = arena.sizeIdx2size(sizeIdx);

        //find lowest common multiple of pageSize and elemSize

        //找到pageSize和elemSize的最小公倍数
        do {
            runSize += pageSize;
            nElements = runSize / elemSize;
        } while (nElements < maxElements && runSize != nElements * elemSize);

        while (nElements > maxElements) {
            runSize -= pageSize;
            nElements = runSize / elemSize;
        }

        assert nElements > 0;
        assert runSize <= chunkSize;
        assert runSize >= elemSize;

        return runSize;
    }

    private int runFirstBestFit(int pageIdx) {
        if (freeBytes == chunkSize) {
            return arena.nPSizes - 1;
        }
        for (int i = pageIdx; i < arena.nPSizes; i++) {
            LongPriorityQueue queue = runsAvail[i];
            if (queue != null && !queue.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private long splitLargeRun(long handle, int needPages) {
        assert needPages > 0;

        int totalPages = runPages(handle);
        assert needPages <= totalPages;

        int remPages = totalPages - needPages;

        if (remPages > 0) {
            int runOffset = runOffset(handle);

            // keep track of trailing unused pages for later use

            // 跟踪未使用的页面以便后续使用
            int availOffset = runOffset + needPages;
            long availRun = toRunHandle(availOffset, remPages, 0);
            insertAvailRun(availOffset, remPages, availRun);

            // not avail

            // 不可用
            return toRunHandle(runOffset, needPages, 1);
        }

        //mark it as used

        //标记为已使用
        handle |= 1L << IS_USED_SHIFT;
        return handle;
    }

    /**
     * Create / initialize a new PoolSubpage of normCapacity. Any PoolSubpage created / initialized here is added to
     * subpage pool in the PoolArena that owns this PoolChunk
     *
     * @param sizeIdx sizeIdx of normalized size
     *
     * @return index in memoryMap
     */

    /**
     * 创建 / 初始化一个具有 normCapacity 的 PoolSubpage。在此创建 / 初始化的任何 PoolSubpage 都会被添加到拥有此 PoolChunk 的 PoolArena 的子页池中
     *
     * @param sizeIdx 归一化大小的 sizeIdx
     *
     * @return memoryMap 中的索引
     */
    private long allocateSubpage(int sizeIdx) {
        // Obtain the head of the PoolSubPage pool that is owned by the PoolArena and synchronize on it.
        // 获取由PoolArena拥有的PoolSubPage池的头部，并对其进行同步。
        // This is need as we may add it back and so alter the linked-list structure.
        // 这是需要的，因为我们可能会将其添加回来，从而改变链表结构。
        PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);
        head.lock();
        try {
            //allocate a new run
            //分配一个新的运行
            int runSize = calculateRunSize(sizeIdx);
            //runSize must be multiples of pageSize
            //runSize必须是pageSize的倍数
            long runHandle = allocateRun(runSize);
            if (runHandle < 0) {
                return -1;
            }

            int runOffset = runOffset(runHandle);
            assert subpages[runOffset] == null;
            int elemSize = arena.sizeIdx2size(sizeIdx);

            PoolSubpage<T> subpage = new PoolSubpage<T>(head, this, pageShifts, runOffset,
                               runSize(pageShifts, runHandle), elemSize);

            subpages[runOffset] = subpage;
            return subpage.allocate();
        } finally {
            head.unlock();
        }
    }

    /**
     * Free a subpage or a run of pages When a subpage is freed from PoolSubpage, it might be added back to subpage pool
     * of the owning PoolArena. If the subpage pool in PoolArena has at least one other PoolSubpage of given elemSize,
     * we can completely free the owning Page so it is available for subsequent allocations
     *
     * @param handle handle to free
     */

    /**
     * 释放一个子页面或一系列页面 当从PoolSubpage释放一个子页面时，它可能会被添加回所属PoolArena的子页面池。
     * 如果PoolArena的子页面池中至少有一个其他给定elemSize的PoolSubpage，我们可以完全释放所属的Page，使其可用于后续分配
     *
     * @param handle 要释放的句柄
     */
    void free(long handle, int normCapacity, ByteBuffer nioBuffer) {
        int runSize = runSize(pageShifts, handle);
        if (isSubpage(handle)) {
            int sizeIdx = arena.size2SizeIdx(normCapacity);
            PoolSubpage<T> head = arena.findSubpagePoolHead(sizeIdx);

            int sIdx = runOffset(handle);
            PoolSubpage<T> subpage = subpages[sIdx];

            // Obtain the head of the PoolSubPage pool that is owned by the PoolArena and synchronize on it.

            // 获取由PoolArena拥有的PoolSubPage池的头部，并对其进行同步。
            // This is need as we may add it back and so alter the linked-list structure.
            // 这是需要的，因为我们可能会将其添加回来，从而改变链表结构。
            head.lock();
            try {
                assert subpage != null && subpage.doNotDestroy;
                if (subpage.free(head, bitmapIdx(handle))) {
                    //the subpage is still used, do not free it
                    //该子页面仍在使用，不要释放它
                    return;
                }
                assert !subpage.doNotDestroy;
                // Null out slot in the array as it was freed and we should not use it anymore.
                // 将数组中的槽位置空，因为它已被释放，我们不应再使用它。
                subpages[sIdx] = null;
            } finally {
                head.unlock();
            }
        }

        //start free run

        //开始自由运行
        runsAvailLock.lock();
        try {
            // collapse continuous runs, successfully collapsed runs
            // 折叠连续运行，成功折叠运行
            // will be removed from runsAvail and runsAvailMap
            // 将从 runsAvail 和 runsAvailMap 中移除
            long finalRun = collapseRuns(handle);

            //set run as not used

            //设置 run 为未使用
            finalRun &= ~(1L << IS_USED_SHIFT);
            //if it is a subpage, set it to run
            //如果是子页面，设置它运行
            finalRun &= ~(1L << IS_SUBPAGE_SHIFT);

            insertAvailRun(runOffset(finalRun), runPages(finalRun), finalRun);
            freeBytes += runSize;
        } finally {
            runsAvailLock.unlock();
        }

        if (nioBuffer != null && cachedNioBuffers != null &&
            cachedNioBuffers.size() < PooledByteBufAllocator.DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK) {
            cachedNioBuffers.offer(nioBuffer);
        }
    }

    private long collapseRuns(long handle) {
        return collapseNext(collapsePast(handle));
    }

    private long collapsePast(long handle) {
        for (;;) {
            int runOffset = runOffset(handle);
            int runPages = runPages(handle);

            long pastRun = getAvailRunByOffset(runOffset - 1);
            if (pastRun == -1) {
                return handle;
            }

            int pastOffset = runOffset(pastRun);
            int pastPages = runPages(pastRun);

            //is continuous

            //是连续的
            if (pastRun != handle && pastOffset + pastPages == runOffset) {
                //remove past run
                //移除过去的运行
                removeAvailRun(pastRun);
                handle = toRunHandle(pastOffset, pastPages + runPages, 0);
            } else {
                return handle;
            }
        }
    }

    private long collapseNext(long handle) {
        for (;;) {
            int runOffset = runOffset(handle);
            int runPages = runPages(handle);

            long nextRun = getAvailRunByOffset(runOffset + runPages);
            if (nextRun == -1) {
                return handle;
            }

            int nextOffset = runOffset(nextRun);
            int nextPages = runPages(nextRun);

            //is continuous

            //是连续的
            if (nextRun != handle && runOffset + runPages == nextOffset) {
                //remove next run
                //移除下次运行
                removeAvailRun(nextRun);
                handle = toRunHandle(runOffset, runPages + nextPages, 0);
            } else {
                return handle;
            }
        }
    }

    private static long toRunHandle(int runOffset, int runPages, int inUsed) {
        return (long) runOffset << RUN_OFFSET_SHIFT
               | (long) runPages << SIZE_SHIFT
               | (long) inUsed << IS_USED_SHIFT;
    }

    void initBuf(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity,
                 PoolThreadCache threadCache) {
        if (isSubpage(handle)) {
            initBufWithSubpage(buf, nioBuffer, handle, reqCapacity, threadCache);
        } else {
            int maxLength = runSize(pageShifts, handle);
            buf.init(this, nioBuffer, handle, runOffset(handle) << pageShifts,
                    reqCapacity, maxLength, arena.parent.threadCache());
        }
    }

    void initBufWithSubpage(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity,
                            PoolThreadCache threadCache) {
        int runOffset = runOffset(handle);
        int bitmapIdx = bitmapIdx(handle);

        PoolSubpage<T> s = subpages[runOffset];
        assert s.doNotDestroy;
        assert reqCapacity <= s.elemSize : reqCapacity + "<=" + s.elemSize;

        int offset = (runOffset << pageShifts) + bitmapIdx * s.elemSize;
        buf.init(this, nioBuffer, handle, offset, reqCapacity, s.elemSize, threadCache);
    }

    void incrementPinnedMemory(int delta) {
        assert delta > 0;
        pinnedBytes.add(delta);
    }

    void decrementPinnedMemory(int delta) {
        assert delta > 0;
        pinnedBytes.add(-delta);
    }

    @Override
    public int chunkSize() {
        return chunkSize;
    }

    @Override
    public int freeBytes() {
        arena.lock();
        try {
            return freeBytes;
        } finally {
            arena.unlock();
        }
    }

    public int pinnedBytes() {
        return (int) pinnedBytes.value();
    }

    @Override
    public String toString() {
        final int freeBytes;
        arena.lock();
        try {
            freeBytes = this.freeBytes;
        } finally {
            arena.unlock();
        }

        return new StringBuilder()
                .append("Chunk(")
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append(": ")
                .append(usage(freeBytes))
                .append("%, ")
                .append(chunkSize - freeBytes)
                .append('/')
                .append(chunkSize)
                .append(')')
                .toString();
    }

    void destroy() {
        arena.destroyChunk(this);
    }

    static int runOffset(long handle) {
        return (int) (handle >> RUN_OFFSET_SHIFT);
    }

    static int runSize(int pageShifts, long handle) {
        return runPages(handle) << pageShifts;
    }

    static int runPages(long handle) {
        return (int) (handle >> SIZE_SHIFT & 0x7fff);
    }

    static boolean isUsed(long handle) {
        return (handle >> IS_USED_SHIFT & 1) == 1L;
    }

    static boolean isRun(long handle) {
        return !isSubpage(handle);
    }

    static boolean isSubpage(long handle) {
        return (handle >> IS_SUBPAGE_SHIFT & 1) == 1L;
    }

    static int bitmapIdx(long handle) {
        return (int) handle;
    }
}
