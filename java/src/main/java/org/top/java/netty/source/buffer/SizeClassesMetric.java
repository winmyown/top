package org.top.java.netty.source.buffer;

/**
 * Expose metrics for an SizeClasses.
 */

/**
 * 暴露SizeClasses的指标。
 */
public interface SizeClassesMetric {

    /**
     * Computes size from lookup table according to sizeIdx.
     *
     * @return size
     */

    /**
     * 根据sizeIdx从查找表中计算大小。
     *
     * @return 大小
     */
    int sizeIdx2size(int sizeIdx);

    /**
     * Computes size according to sizeIdx.
     *
     * @return size
     */

    /**
     * 根据sizeIdx计算大小。
     *
     * @return 大小
     */
    int sizeIdx2sizeCompute(int sizeIdx);

    /**
     * Computes size from lookup table according to pageIdx.
     *
     * @return size which is multiples of pageSize.
     */

    /**
     * 根据pageIdx从查找表中计算大小。
     *
     * @return 大小为pageSize的倍数。
     */
    long pageIdx2size(int pageIdx);

    /**
     * Computes size according to pageIdx.
     *
     * @return size which is multiples of pageSize
     */

    /**
     * 根据pageIdx计算大小。
     *
     * @return 大小为pageSize的倍数
     */
    long pageIdx2sizeCompute(int pageIdx);

    /**
     * Normalizes request size up to the nearest size class.
     *
     * @param size request size
     *
     * @return sizeIdx of the size class
     */

    /**
     * 将请求大小归一化到最接近的大小类别。
     *
     * @param size 请求大小
     *
     * @return 大小类别的 sizeIdx
     */
    int size2SizeIdx(int size);

    /**
     * Normalizes request size up to the nearest pageSize class.
     *
     * @param pages multiples of pageSizes
     *
     * @return pageIdx of the pageSize class
     */

    /**
     * 将请求大小向上归一化到最近的 pageSize 类别。
     *
     * @param pages pageSizes 的倍数
     *
     * @return pageSize 类别的 pageIdx
     */
    int pages2pageIdx(int pages);

    /**
     * Normalizes request size down to the nearest pageSize class.
     *
     * @param pages multiples of pageSizes
     *
     * @return pageIdx of the pageSize class
     */

    /**
     * 将请求大小规范化到最接近的 pageSize 类别。
     *
     * @param pages pageSizes 的倍数
     *
     * @return pageSize 类别的 pageIdx
     */
    int pages2pageIdxFloor(int pages);

    /**
     * Normalizes usable size that would result from allocating an object with the
     * specified size and alignment.
     *
     * @param size request size
     *
     * @return normalized size
     */

    /**
     * 规范化分配具有指定大小和对齐方式的对象后得到的可用大小。
     *
     * @param size 请求的大小
     *
     * @return 规范化后的大小
     */
    int normalizeSize(int size);
}
