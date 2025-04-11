
package org.top.java.netty.source.buffer;

/**
 * Implementations are responsible to allocate buffers. Implementations of this interface are expected to be
 * thread-safe.
 */

/**
 * 实现类负责分配缓冲区。此接口的实现类应保证线程安全。
 */
public interface ByteBufAllocator {

    ByteBufAllocator DEFAULT = ByteBufUtil.DEFAULT_ALLOCATOR;

    /**
     * Allocate a {@link ByteBuf}. If it is a direct or heap buffer
     * depends on the actual implementation.
     */

    /**
     * 分配一个 {@link ByteBuf}。它是直接缓冲区还是堆缓冲区
     * 取决于实际实现。
     */
    ByteBuf buffer();

    /**
     * Allocate a {@link ByteBuf} with the given initial capacity.
     * If it is a direct or heap buffer depends on the actual implementation.
     */

    /**
     * 分配一个具有给定初始容量的 {@link ByteBuf}。
     * 它是直接缓冲区还是堆缓冲区取决于具体实现。
     */
    ByteBuf buffer(int initialCapacity);

    /**
     * Allocate a {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity. If it is a direct or heap buffer depends on the actual
     * implementation.
     */

    /**
     * 分配一个具有给定初始容量和最大容量的 {@link ByteBuf}。它是直接缓冲区还是堆缓冲区取决于实际实现。
     */
    ByteBuf buffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */

    /**
     * 分配一个{@link ByteBuf}，最好是适合I/O的直接缓冲区。
     */
    ByteBuf ioBuffer();

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */

    /**
     * 分配一个{@link ByteBuf}，最好是适合I/O的直接缓冲区。
     */
    ByteBuf ioBuffer(int initialCapacity);

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */

    /**
     * 分配一个{@link ByteBuf}，最好是适合I/O的直接缓冲区。
     */
    ByteBuf ioBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a heap {@link ByteBuf}.
     */

    /**
     * 分配一个堆 {@link ByteBuf}。
     */
    ByteBuf heapBuffer();

    /**
     * Allocate a heap {@link ByteBuf} with the given initial capacity.
     */

    /**
     * 分配一个具有给定初始容量的堆 {@link ByteBuf}。
     */
    ByteBuf heapBuffer(int initialCapacity);

    /**
     * Allocate a heap {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity.
     */

    /**
     * 分配一个具有给定初始容量和最大容量的堆 {@link ByteBuf}。
     */
    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a direct {@link ByteBuf}.
     */

    /**
     * 分配一个直接的 {@link ByteBuf}。
     */
    ByteBuf directBuffer();

    /**
     * Allocate a direct {@link ByteBuf} with the given initial capacity.
     */

    /**
     * 分配一个具有给定初始容量的直接 {@link ByteBuf}。
     */
    ByteBuf directBuffer(int initialCapacity);

    /**
     * Allocate a direct {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity.
     */

    /**
     * 分配一个具有给定初始容量和最大容量的直接 {@link ByteBuf}。
     */
    ByteBuf directBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link CompositeByteBuf}.
     * If it is a direct or heap buffer depends on the actual implementation.
     */

    /**
     * 分配一个 {@link CompositeByteBuf}。
     * 它是直接缓冲区还是堆缓冲区取决于实际实现。
     */
    CompositeByteBuf compositeBuffer();

    /**
     * Allocate a {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     * If it is a direct or heap buffer depends on the actual implementation.
     */

    /**
     * 分配一个具有给定最大组件数的 {@link CompositeByteBuf}。
     * 它是直接缓冲区还是堆缓冲区取决于实际实现。
     */
    CompositeByteBuf compositeBuffer(int maxNumComponents);

    /**
     * Allocate a heap {@link CompositeByteBuf}.
     */

    /**
     * 分配一个堆 {@link CompositeByteBuf}。
     */
    CompositeByteBuf compositeHeapBuffer();

    /**
     * Allocate a heap {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */

    /**
     * 分配一个堆内存的 {@link CompositeByteBuf}，并指定其可以存储的最大组件数量。
     */
    CompositeByteBuf compositeHeapBuffer(int maxNumComponents);

    /**
     * Allocate a direct {@link CompositeByteBuf}.
     */

    /**
     * 分配一个直接的 {@link CompositeByteBuf}。
     */
    CompositeByteBuf compositeDirectBuffer();

    /**
     * Allocate a direct {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */

    /**
     * 分配一个直接的 {@link CompositeByteBuf}，并指定其可以存储的最大组件数量。
     */
    CompositeByteBuf compositeDirectBuffer(int maxNumComponents);

    /**
     * Returns {@code true} if direct {@link ByteBuf}'s are pooled
     */

    /**
     * 如果直接 {@link ByteBuf} 是池化的，则返回 {@code true}
     */
    boolean isDirectBufferPooled();

    /**
     * Calculate the new capacity of a {@link ByteBuf} that is used when a {@link ByteBuf} needs to expand by the
     * {@code minNewCapacity} with {@code maxCapacity} as upper-bound.
     */

    /**
     * 计算当 {@link ByteBuf} 需要根据 {@code minNewCapacity} 进行扩展时，使用的新容量，并以 {@code maxCapacity} 作为上限。
     */
    int calculateNewCapacity(int minNewCapacity, int maxCapacity);
 }
