package org.top.java.netty.source.buffer;

import static org.top.java.netty.source.buffer.PoolThreadCache.log2;

/**
 * SizeClasses requires {@code pageShifts} to be defined prior to inclusion,
 * and it in turn defines:
 * <p>
 *   LOG2_SIZE_CLASS_GROUP: Log of size class count for each size doubling.
 *   LOG2_MAX_LOOKUP_SIZE: Log of max size class in the lookup table.
 *   sizeClasses: Complete table of [index, log2Group, log2Delta, nDelta, isMultiPageSize,
 *                 isSubPage, log2DeltaLookup] tuples.
 *     index: Size class index.
 *     log2Group: Log of group base size (no deltas added).
 *     log2Delta: Log of delta to previous size class.
 *     nDelta: Delta multiplier.
 *     isMultiPageSize: 'yes' if a multiple of the page size, 'no' otherwise.
 *     isSubPage: 'yes' if a subpage size class, 'no' otherwise.
 *     log2DeltaLookup: Same as log2Delta if a lookup table size class, 'no'
 *                      otherwise.
 * <p>
 *   nSubpages: Number of subpages size classes.
 *   nSizes: Number of size classes.
 *   nPSizes: Number of size classes that are multiples of pageSize.
 *
 *   smallMaxSizeIdx: Maximum small size class index.
 *
 *   lookupMaxClass: Maximum size class included in lookup table.
 *   log2NormalMinClass: Log of minimum normal size class.
 * <p>
 *   The first size class and spacing are 1 << LOG2_QUANTUM.
 *   Each group has 1 << LOG2_SIZE_CLASS_GROUP of size classes.
 *
 *   size = 1 << log2Group + nDelta * (1 << log2Delta)
 *
 *   The first size class has an unusual encoding, because the size has to be
 *   split between group and delta*nDelta.
 *
 *   If pageShift = 13, sizeClasses looks like this:
 *
 *   (index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup)
 * <p>
 *   ( 0,     4,        4,         0,       no,             yes,        4)
 *   ( 1,     4,        4,         1,       no,             yes,        4)
 *   ( 2,     4,        4,         2,       no,             yes,        4)
 *   ( 3,     4,        4,         3,       no,             yes,        4)
 * <p>
 *   ( 4,     6,        4,         1,       no,             yes,        4)
 *   ( 5,     6,        4,         2,       no,             yes,        4)
 *   ( 6,     6,        4,         3,       no,             yes,        4)
 *   ( 7,     6,        4,         4,       no,             yes,        4)
 * <p>
 *   ( 8,     7,        5,         1,       no,             yes,        5)
 *   ( 9,     7,        5,         2,       no,             yes,        5)
 *   ( 10,    7,        5,         3,       no,             yes,        5)
 *   ( 11,    7,        5,         4,       no,             yes,        5)
 *   ...
 *   ...
 *   ( 72,    23,       21,        1,       yes,            no,        no)
 *   ( 73,    23,       21,        2,       yes,            no,        no)
 *   ( 74,    23,       21,        3,       yes,            no,        no)
 *   ( 75,    23,       21,        4,       yes,            no,        no)
 * <p>
 *   ( 76,    24,       22,        1,       yes,            no,        no)
 */

/**
 * SizeClasses 要求在包含之前定义 {@code pageShifts}，
 * 然后它定义了：
 * <p>
 *   LOG2_SIZE_CLASS_GROUP: 每个大小翻倍的尺寸类数量的对数。
 *   LOG2_MAX_LOOKUP_SIZE: 查找表中最大尺寸类的对数。
 *   sizeClasses: 完整的 [index, log2Group, log2Delta, nDelta, isMultiPageSize,
 *                 isSubPage, log2DeltaLookup] 元组表。
 *     index: 尺寸类索引。
 *     log2Group: 组基础大小的对数（未添加增量）。
 *     log2Delta: 与前一个尺寸类的增量的对数。
 *     nDelta: 增量乘数。
 *     isMultiPageSize: 如果是页面大小的倍数，则为 'yes'，否则为 'no'。
 *     isSubPage: 如果是子页面尺寸类，则为 'yes'，否则为 'no'。
 *     log2DeltaLookup: 如果是查找表尺寸类，则与 log2Delta 相同，否则为 'no'。
 * <p>
 *   nSubpages: 子页面尺寸类的数量。
 *   nSizes: 尺寸类的数量。
 *   nPSizes: 是页面大小倍数的尺寸类的数量。
 *
 *   smallMaxSizeIdx: 最大小尺寸类索引。
 *
 *   lookupMaxClass: 查找表中包含的最大尺寸类。
 *   log2NormalMinClass: 最小正常尺寸类的对数。
 * <p>
 *   第一个尺寸类和间距为 1 << LOG2_QUANTUM。
 *   每个组有 1 << LOG2_SIZE_CLASS_GROUP 个尺寸类。
 *
 *   size = 1 << log2Group + nDelta * (1 << log2Delta)
 *
 *   第一个尺寸类有一个不寻常的编码，因为大小必须在组和 delta*nDelta 之间拆分。
 *
 *   如果 pageShift = 13，sizeClasses 看起来像这样：
 *
 *   (index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup)
 * <p>
 *   ( 0,     4,        4,         0,       no,             yes,        4)
 *   ( 1,     4,        4,         1,       no,             yes,        4)
 *   ( 2,     4,        4,         2,       no,             yes,        4)
 *   ( 3,     4,        4,         3,       no,             yes,        4)
 * <p>
 *   ( 4,     6,        4,         1,       no,             yes,        4)
 *   ( 5,     6,        4,         2,       no,             yes,        4)
 *   ( 6,     6,        4,         3,       no,             yes,        4)
 *   ( 7,     6,        4,         4,       no,             yes,        4)
 * <p>
 *   ( 8,     7,        5,         1,       no,             yes,        5)
 *   ( 9,     7,        5,         2,       no,             yes,        5)
 *   ( 10,    7,        5,         3,       no,             yes,        5)
 *   ( 11,    7,        5,         4,       no,             yes,        5)
 *   ...
 *   ...
 *   ( 72,    23,       21,        1,       yes,            no,        no)
 *   ( 73,    23,       21,        2,       yes,            no,        no)
 *   ( 74,    23,       21,        3,       yes,            no,        no)
 *   ( 75,    23,       21,        4,       yes,            no,        no)
 * <p>
 *   ( 76,    24,       22,        1,       yes,            no,        no)
 */
abstract class SizeClasses implements SizeClassesMetric {

    static final int LOG2_QUANTUM = 4;

    private static final int LOG2_SIZE_CLASS_GROUP = 2;
    private static final int LOG2_MAX_LOOKUP_SIZE = 12;

    private static final int INDEX_IDX = 0;
    private static final int LOG2GROUP_IDX = 1;
    private static final int LOG2DELTA_IDX = 2;
    private static final int NDELTA_IDX = 3;
    private static final int PAGESIZE_IDX = 4;
    private static final int SUBPAGE_IDX = 5;
    private static final int LOG2_DELTA_LOOKUP_IDX = 6;

    private static final byte no = 0, yes = 1;

    protected final int pageSize;
    protected final int pageShifts;
    protected final int chunkSize;
    protected final int directMemoryCacheAlignment;

    final int nSizes;
    final int nSubpages;
    final int nPSizes;
    final int lookupMaxSize;
    final int smallMaxSizeIdx;

    private final int[] pageIdx2sizeTab;

    // lookup table for sizeIdx <= smallMaxSizeIdx

    // 查找表，用于 sizeIdx <= smallMaxSizeIdx
    private final int[] sizeIdx2sizeTab;

    // lookup table used for size <= lookupMaxClass

    // 查找表用于 size <= lookupMaxClass
    // spacing is 1 << LOG2_QUANTUM, so the size of array is lookupMaxClass >> LOG2_QUANTUM
    // 间距为 1 << LOG2_QUANTUM，因此数组的大小为 lookupMaxClass >> LOG2_QUANTUM
    private final int[] size2idxTab;

    protected SizeClasses(int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment) {
        int group = log2(chunkSize) + 1 - LOG2_QUANTUM;

        //generate size classes

        //生成大小类
        //[index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup]
        //[索引, log2分组, log2增量, n增量, 是否多页大小, 是否子页, log2增量查找]
        short[][] sizeClasses = new short[group << LOG2_SIZE_CLASS_GROUP][7];

        int normalMaxSize = -1;
        int nSizes = 0;
        int size = 0;

        int log2Group = LOG2_QUANTUM;
        int log2Delta = LOG2_QUANTUM;
        int ndeltaLimit = 1 << LOG2_SIZE_CLASS_GROUP;

        //First small group, nDelta start at 0.

        //第一个小组，nDelta 从 0 开始。
        //first size class is 1 << LOG2_QUANTUM
        //第一个大小类是 1 << LOG2_QUANTUM
        for (int nDelta = 0; nDelta < ndeltaLimit; nDelta++, nSizes++) {
            short[] sizeClass = newSizeClass(nSizes, log2Group, log2Delta, nDelta, pageShifts);
            sizeClasses[nSizes] = sizeClass;
            size = sizeOf(sizeClass, directMemoryCacheAlignment);
        }

        log2Group += LOG2_SIZE_CLASS_GROUP;

        //All remaining groups, nDelta start at 1.

        //所有剩余的组，nDelta 从 1 开始。
        for (; size < chunkSize; log2Group++, log2Delta++) {
            for (int nDelta = 1; nDelta <= ndeltaLimit && size < chunkSize; nDelta++, nSizes++) {
                short[] sizeClass = newSizeClass(nSizes, log2Group, log2Delta, nDelta, pageShifts);
                sizeClasses[nSizes] = sizeClass;
                size = normalMaxSize = sizeOf(sizeClass, directMemoryCacheAlignment);
            }
        }

        //chunkSize must be normalMaxSize

        //chunkSize 必须为 normalMaxSize
        assert chunkSize == normalMaxSize;

        int smallMaxSizeIdx = 0;
        int lookupMaxSize = 0;
        int nPSizes = 0;
        int nSubpages = 0;
        for (int idx = 0; idx < nSizes; idx++) {
            short[] sz = sizeClasses[idx];
            if (sz[PAGESIZE_IDX] == yes) {
                nPSizes++;
            }
            if (sz[SUBPAGE_IDX] == yes) {
                nSubpages++;
                smallMaxSizeIdx = idx;
            }
            if (sz[LOG2_DELTA_LOOKUP_IDX] != no) {
                lookupMaxSize = sizeOf(sz, directMemoryCacheAlignment);
            }
        }
        this.smallMaxSizeIdx = smallMaxSizeIdx;
        this.lookupMaxSize = lookupMaxSize;
        this.nPSizes = nPSizes;
        this.nSubpages = nSubpages;
        this.nSizes = nSizes;

        this.pageSize = pageSize;
        this.pageShifts = pageShifts;
        this.chunkSize = chunkSize;
        this.directMemoryCacheAlignment = directMemoryCacheAlignment;

        //generate lookup tables

        //生成查找表
        sizeIdx2sizeTab = newIdx2SizeTab(sizeClasses, nSizes, directMemoryCacheAlignment);
        pageIdx2sizeTab = newPageIdx2sizeTab(sizeClasses, nSizes, nPSizes, directMemoryCacheAlignment);
        size2idxTab = newSize2idxTab(lookupMaxSize, sizeClasses);
    }

    //calculate size class

    //计算尺寸类
    private static short[] newSizeClass(int index, int log2Group, int log2Delta, int nDelta, int pageShifts) {
        short isMultiPageSize;
        if (log2Delta >= pageShifts) {
            isMultiPageSize = yes;
        } else {
            int pageSize = 1 << pageShifts;
            int size = calculateSize(log2Group, nDelta, log2Delta);

            isMultiPageSize = size == size / pageSize * pageSize? yes : no;
        }

        int log2Ndelta = nDelta == 0? 0 : log2(nDelta);

        byte remove = 1 << log2Ndelta < nDelta? yes : no;

        int log2Size = log2Delta + log2Ndelta == log2Group? log2Group + 1 : log2Group;
        if (log2Size == log2Group) {
            remove = yes;
        }

        short isSubpage = log2Size < pageShifts + LOG2_SIZE_CLASS_GROUP? yes : no;

        int log2DeltaLookup = log2Size < LOG2_MAX_LOOKUP_SIZE ||
                              log2Size == LOG2_MAX_LOOKUP_SIZE && remove == no
                ? log2Delta : no;

        return new short[] {
                (short) index, (short) log2Group, (short) log2Delta,
                (short) nDelta, isMultiPageSize, isSubpage, (short) log2DeltaLookup
        };
    }

    private static int[] newIdx2SizeTab(short[][] sizeClasses, int nSizes, int directMemoryCacheAlignment) {
        int[] sizeIdx2sizeTab = new int[nSizes];

        for (int i = 0; i < nSizes; i++) {
            short[] sizeClass = sizeClasses[i];
            sizeIdx2sizeTab[i] = sizeOf(sizeClass, directMemoryCacheAlignment);
        }
        return sizeIdx2sizeTab;
    }

    private static int calculateSize(int log2Group, int nDelta, int log2Delta) {
        return (1 << log2Group) + (nDelta << log2Delta);
    }

    private static int sizeOf(short[] sizeClass, int directMemoryCacheAlignment) {
        int log2Group = sizeClass[LOG2GROUP_IDX];
        int log2Delta = sizeClass[LOG2DELTA_IDX];
        int nDelta = sizeClass[NDELTA_IDX];

        int size = calculateSize(log2Group, nDelta, log2Delta);

        return alignSizeIfNeeded(size, directMemoryCacheAlignment);
    }

    private static int[] newPageIdx2sizeTab(short[][] sizeClasses, int nSizes, int nPSizes,
                                            int directMemoryCacheAlignment) {
        int[] pageIdx2sizeTab = new int[nPSizes];
        int pageIdx = 0;
        for (int i = 0; i < nSizes; i++) {
            short[] sizeClass = sizeClasses[i];
            if (sizeClass[PAGESIZE_IDX] == yes) {
                pageIdx2sizeTab[pageIdx++] = sizeOf(sizeClass, directMemoryCacheAlignment);
            }
        }
        return pageIdx2sizeTab;
    }

    private static int[] newSize2idxTab(int lookupMaxSize, short[][] sizeClasses) {
        int[] size2idxTab = new int[lookupMaxSize >> LOG2_QUANTUM];
        int idx = 0;
        int size = 0;

        for (int i = 0; size <= lookupMaxSize; i++) {
            int log2Delta = sizeClasses[i][LOG2DELTA_IDX];
            int times = 1 << log2Delta - LOG2_QUANTUM;

            while (size <= lookupMaxSize && times-- > 0) {
                size2idxTab[idx++] = i;
                size = idx + 1 << LOG2_QUANTUM;
            }
        }
        return size2idxTab;
    }

    @Override
    public int sizeIdx2size(int sizeIdx) {
        return sizeIdx2sizeTab[sizeIdx];
    }

    @Override
    public int sizeIdx2sizeCompute(int sizeIdx) {
        int group = sizeIdx >> LOG2_SIZE_CLASS_GROUP;
        int mod = sizeIdx & (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        int groupSize = group == 0? 0 :
                1 << LOG2_QUANTUM + LOG2_SIZE_CLASS_GROUP - 1 << group;

        int shift = group == 0? 1 : group;
        int lgDelta = shift + LOG2_QUANTUM - 1;
        int modSize = mod + 1 << lgDelta;

        return groupSize + modSize;
    }

    @Override
    public long pageIdx2size(int pageIdx) {
        return pageIdx2sizeTab[pageIdx];
    }

    @Override
    public long pageIdx2sizeCompute(int pageIdx) {
        int group = pageIdx >> LOG2_SIZE_CLASS_GROUP;
        int mod = pageIdx & (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        long groupSize = group == 0? 0 :
                1L << pageShifts + LOG2_SIZE_CLASS_GROUP - 1 << group;

        int shift = group == 0? 1 : group;
        int log2Delta = shift + pageShifts - 1;
        int modSize = mod + 1 << log2Delta;

        return groupSize + modSize;
    }

    @Override
    public int size2SizeIdx(int size) {
        if (size == 0) {
            return 0;
        }
        if (size > chunkSize) {
            return nSizes;
        }

        size = alignSizeIfNeeded(size, directMemoryCacheAlignment);

        if (size <= lookupMaxSize) {
            //size-1 / MIN_TINY
            //大小-1 / 最小_微小
            return size2idxTab[size - 1 >> LOG2_QUANTUM];
        }

        int x = log2((size << 1) - 1);
        int shift = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? 0 : x - (LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM);

        int group = shift << LOG2_SIZE_CLASS_GROUP;

        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? LOG2_QUANTUM : x - LOG2_SIZE_CLASS_GROUP - 1;

        int deltaInverseMask = -1 << log2Delta;
        int mod = (size - 1 & deltaInverseMask) >> log2Delta &
                  (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        return group + mod;
    }

    @Override
    public int pages2pageIdx(int pages) {
        return pages2pageIdxCompute(pages, false);
    }

    @Override
    public int pages2pageIdxFloor(int pages) {
        return pages2pageIdxCompute(pages, true);
    }

    private int pages2pageIdxCompute(int pages, boolean floor) {
        int pageSize = pages << pageShifts;
        if (pageSize > chunkSize) {
            return nPSizes;
        }

        int x = log2((pageSize << 1) - 1);

        int shift = x < LOG2_SIZE_CLASS_GROUP + pageShifts
                ? 0 : x - (LOG2_SIZE_CLASS_GROUP + pageShifts);

        int group = shift << LOG2_SIZE_CLASS_GROUP;

        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + pageShifts + 1?
                pageShifts : x - LOG2_SIZE_CLASS_GROUP - 1;

        int deltaInverseMask = -1 << log2Delta;
        int mod = (pageSize - 1 & deltaInverseMask) >> log2Delta &
                  (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        int pageIdx = group + mod;

        if (floor && pageIdx2sizeTab[pageIdx] > pages << pageShifts) {
            pageIdx--;
        }

        return pageIdx;
    }

    // Round size up to the nearest multiple of alignment.

    // 将大小向上舍入到最接近的对齐倍数。
    private static int alignSizeIfNeeded(int size, int directMemoryCacheAlignment) {
        if (directMemoryCacheAlignment <= 0) {
            return size;
        }
        int delta = size & directMemoryCacheAlignment - 1;
        return delta == 0? size : size + directMemoryCacheAlignment - delta;
    }

    @Override
    public int normalizeSize(int size) {
        if (size == 0) {
            return sizeIdx2sizeTab[0];
        }
        size = alignSizeIfNeeded(size, directMemoryCacheAlignment);
        if (size <= lookupMaxSize) {
            int ret = sizeIdx2sizeTab[size2idxTab[size - 1 >> LOG2_QUANTUM]];
            assert ret == normalizeSizeCompute(size);
            return ret;
        }
        return normalizeSizeCompute(size);
    }

    private static int normalizeSizeCompute(int size) {
        int x = log2((size << 1) - 1);
        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? LOG2_QUANTUM : x - LOG2_SIZE_CLASS_GROUP - 1;
        int delta = 1 << log2Delta;
        int delta_mask = delta - 1;
        return size + delta_mask & ~delta_mask;
    }
}
