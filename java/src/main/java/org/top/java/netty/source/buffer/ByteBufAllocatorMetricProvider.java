
package org.top.java.netty.source.buffer;

public interface ByteBufAllocatorMetricProvider {

    /**
     * Returns a {@link ByteBufAllocatorMetric} for a {@link ByteBufAllocator}.
     */

    /**
     * 返回一个用于 {@link ByteBufAllocator} 的 {@link ByteBufAllocatorMetric}。
     */
    ByteBufAllocatorMetric metric();
}
