
package org.top.java.netty.source.buffer;

public interface ByteBufAllocatorMetric {
    /**
     * Returns the number of bytes of heap memory used by a {@link ByteBufAllocator} or {@code -1} if unknown.
     */
    /**
     * 返回由 {@link ByteBufAllocator} 使用的堆内存字节数，如果未知则返回 {@code -1}。
     */
    long usedHeapMemory();

    /**
     * Returns the number of bytes of direct memory used by a {@link ByteBufAllocator} or {@code -1} if unknown.
     */

    /**
     * 返回由 {@link ByteBufAllocator} 使用的直接内存的字节数，如果未知则返回 {@code -1}。
     */
    long usedDirectMemory();
}
