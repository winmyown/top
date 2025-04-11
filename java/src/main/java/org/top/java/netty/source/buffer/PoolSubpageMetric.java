
package org.top.java.netty.source.buffer;

/**
 * Metrics for a sub-page.
 */

/**
 * 子页面的指标。
 */
public interface PoolSubpageMetric {

    /**
     * Return the number of maximal elements that can be allocated out of the sub-page.
     */

    /**
     * 返回可以从子页面中分配的最大元素数量。
     */
    int maxNumElements();

    /**
     * Return the number of available elements to be allocated.
     */

    /**
     * 返回可供分配的元素数量。
     */
    int numAvailable();

    /**
     * Return the size (in bytes) of the elements that will be allocated.
     */

    /**
     * 返回将要分配的元素的字节大小。
     */
    int elementSize();

    /**
     * Return the page size (in bytes) of this page.
     */

    /**
     * 返回此页的大小（以字节为单位）。
     */
    int pageSize();
}

