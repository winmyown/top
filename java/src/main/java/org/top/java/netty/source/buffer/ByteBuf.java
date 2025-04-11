
package org.top.java.netty.source.buffer;

import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCounted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A random and sequential accessible sequence of zero or more bytes (octets).
 * This interface provides an abstract view for one or more primitive byte
 * arrays ({@code byte[]}) and {@linkplain ByteBuffer NIO buffers}.
 *
 * <h3>Creation of a buffer</h3>
 *
 * It is recommended to create a new buffer using the helper methods in
 * {@link Unpooled} rather than calling an individual implementation's
 * constructor.
 *
 * <h3>Random Access Indexing</h3>
 *
 * Just like an ordinary primitive byte array, {@link ByteBuf} uses
 * <a href="https://en.wikipedia.org/wiki/Zero-based_numbering">zero-based indexing</a>.
 * It means the index of the first byte is always {@code 0} and the index of the last byte is
 * always {@link #capacity() capacity - 1}.  For example, to iterate all bytes of a buffer, you
 * can do the following, regardless of its internal implementation:
 *
 * <pre>
 * {@link ByteBuf} buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++) {
 *     byte b = buffer.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 *
 * <h3>Sequential Access Indexing</h3>
 *
 * {@link ByteBuf} provides two pointer variables to support sequential
 * read and write operations - {@link #readerIndex() readerIndex} for a read
 * operation and {@link #writerIndex() writerIndex} for a write operation
 * respectively.  The following diagram shows how a buffer is segmented into
 * three areas by the two pointers:
 *
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * <h4>Readable bytes (the actual content)</h4>
 *
 * This segment is where the actual data is stored.  Any operation whose name
 * starts with {@code read} or {@code skip} will get or skip the data at the
 * current {@link #readerIndex() readerIndex} and increase it by the number of
 * read bytes.  If the argument of the read operation is also a
 * {@link ByteBuf} and no destination index is specified, the specified
 * buffer's {@link #writerIndex() writerIndex} is increased together.
 * <p>
 * If there's not enough content left, {@link IndexOutOfBoundsException} is
 * raised.  The default value of newly allocated, wrapped or copied buffer's
 * {@link #readerIndex() readerIndex} is {@code 0}.
 *
 * <pre>
 * // Iterates the readable bytes of a buffer.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.isReadable()) {
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 *
 * <h4>Writable bytes</h4>
 *
 * This segment is a undefined space which needs to be filled.  Any operation
 * whose name starts with {@code write} will write the data at the current
 * {@link #writerIndex() writerIndex} and increase it by the number of written
 * bytes.  If the argument of the write operation is also a {@link ByteBuf},
 * and no source index is specified, the specified buffer's
 * {@link #readerIndex() readerIndex} is increased together.
 * <p>
 * If there's not enough writable bytes left, {@link IndexOutOfBoundsException}
 * is raised.  The default value of newly allocated buffer's
 * {@link #writerIndex() writerIndex} is {@code 0}.  The default value of
 * wrapped or copied buffer's {@link #writerIndex() writerIndex} is the
 * {@link #capacity() capacity} of the buffer.
 *
 * <pre>
 * // Fills the writable bytes of a buffer with random integers.
 * {@link ByteBuf} buffer = ...;
 * while (buffer.maxWritableBytes() >= 4) {
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 *
 * <h4>Discardable bytes</h4>
 *
 * This segment contains the bytes which were read already by a read operation.
 * Initially, the size of this segment is {@code 0}, but its size increases up
 * to the {@link #writerIndex() writerIndex} as read operations are executed.
 * The read bytes can be discarded by calling {@link #discardReadBytes()} to
 * reclaim unused area as depicted by the following diagram:
 *
 * <pre>
 *  BEFORE discardReadBytes()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER discardReadBytes()
 *
 *      +------------------+--------------------------------------+
 *      |  readable bytes  |    writable bytes (got more space)   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (decreased)        <=        capacity
 * </pre>
 *
 * Please note that there is no guarantee about the content of writable bytes
 * after calling {@link #discardReadBytes()}.  The writable bytes will not be
 * moved in most cases and could even be filled with completely different data
 * depending on the underlying buffer implementation.
 *
 * <h4>Clearing the buffer indexes</h4>
 *
 * You can set both {@link #readerIndex() readerIndex} and
 * {@link #writerIndex() writerIndex} to {@code 0} by calling {@link #clear()}.
 * It does not clear the buffer content (e.g. filling with {@code 0}) but just
 * clears the two pointers.  Please also note that the semantic of this
 * operation is different from {@link ByteBuffer#clear()}.
 *
 * <pre>
 *  BEFORE clear()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER clear()
 *
 *      +---------------------------------------------------------+
 *      |             writable bytes (got more space)             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 *
 * <h3>Search operations</h3>
 *
 * For simple single-byte searches, use {@link #indexOf(int, int, byte)} and {@link #bytesBefore(int, int, byte)}.
 * {@link #bytesBefore(byte)} is especially useful when you deal with a {@code NUL}-terminated string.
 * For complicated searches, use {@link #forEachByte(int, int, ByteProcessor)} with a {@link ByteProcessor}
 * implementation.
 *
 * <h3>Mark and reset</h3>
 *
 * There are two marker indexes in every buffer. One is for storing
 * {@link #readerIndex() readerIndex} and the other is for storing
 * {@link #writerIndex() writerIndex}.  You can always reposition one of the
 * two indexes by calling a reset method.  It works in a similar fashion to
 * the mark and reset methods in {@link InputStream} except that there's no
 * {@code readlimit}.
 *
 * <h3>Derived buffers</h3>
 *
 * You can create a view of an existing buffer by calling one of the following methods:
 * <ul>
 *   <li>{@link #duplicate()}</li>
 *   <li>{@link #slice()}</li>
 *   <li>{@link #slice(int, int)}</li>
 *   <li>{@link #readSlice(int)}</li>
 *   <li>{@link #retainedDuplicate()}</li>
 *   <li>{@link #retainedSlice()}</li>
 *   <li>{@link #retainedSlice(int, int)}</li>
 *   <li>{@link #readRetainedSlice(int)}</li>
 * </ul>
 * A derived buffer will have an independent {@link #readerIndex() readerIndex},
 * {@link #writerIndex() writerIndex} and marker indexes, while it shares
 * other internal data representation, just like a NIO buffer does.
 * <p>
 * In case a completely fresh copy of an existing buffer is required, please
 * call {@link #copy()} method instead.
 *
 * <h4>Non-retained and retained derived buffers</h4>
 *
 * Note that the {@link #duplicate()}, {@link #slice()}, {@link #slice(int, int)} and {@link #readSlice(int)} does NOT
 * call {@link #retain()} on the returned derived buffer, and thus its reference count will NOT be increased. If you
 * need to create a derived buffer with increased reference count, consider using {@link #retainedDuplicate()},
 * {@link #retainedSlice()}, {@link #retainedSlice(int, int)} and {@link #readRetainedSlice(int)} which may return
 * a buffer implementation that produces less garbage.
 *
 * <h3>Conversion to existing JDK types</h3>
 *
 * <h4>Byte array</h4>
 *
 * If a {@link ByteBuf} is backed by a byte array (i.e. {@code byte[]}),
 * you can access it directly via the {@link #array()} method.  To determine
 * if a buffer is backed by a byte array, {@link #hasArray()} should be used.
 *
 * <h4>NIO Buffers</h4>
 *
 * If a {@link ByteBuf} can be converted into an NIO {@link ByteBuffer} which shares its
 * content (i.e. view buffer), you can get it via the {@link #nioBuffer()} method.  To determine
 * if a buffer can be converted into an NIO buffer, use {@link #nioBufferCount()}.
 *
 * <h4>Strings</h4>
 *
 * Various {@link #toString(Charset)} methods convert a {@link ByteBuf}
 * into a {@link String}.  Please note that {@link #toString()} is not a
 * conversion method.
 *
 * <h4>I/O Streams</h4>
 *
 * Please refer to {@link ByteBufInputStream} and
 * {@link ByteBufOutputStream}.
 */

/**
 * 一个可以随机和顺序访问的零个或多个字节（八位字节）的序列。
 * 该接口为一个或多个原始字节数组（{@code byte[]}）和 {@linkplain ByteBuffer NIO 缓冲区} 提供了抽象视图。
 *
 * <h3>缓冲区的创建</h3>
 *
 * 建议使用 {@link Unpooled} 中的辅助方法来创建新缓冲区，而不是调用单个实现的构造函数。
 *
 * <h3>随机访问索引</h3>
 *
 * 与普通的原始字节数组一样，{@link ByteBuf} 使用
 * <a href="https://en.wikipedia.org/wiki/Zero-based_numbering">零基索引</a>。
 * 这意味着第一个字节的索引始终为 {@code 0}，最后一个字节的索引始终为
 * {@link #capacity() capacity - 1}。例如，要遍历缓冲区的所有字节，可以执行以下操作，无论其内部实现如何：
 *
 * <pre>
 * {@link ByteBuf} buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++) {
 *     byte b = buffer.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 *
 * <h3>顺序访问索引</h3>
 *
 * {@link ByteBuf} 提供了两个指针变量来支持顺序读写操作 -
 * {@link #readerIndex() readerIndex} 用于读操作，{@link #writerIndex() writerIndex} 用于写操作。
 * 以下图表展示了如何通过这两个指针将缓冲区分为三个区域：
 *
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | 可丢弃字节 |  可读字节  |  可写字节  |
 *      |                   |     (内容)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * <h4>可读字节（实际内容）</h4>
 *
 * 此段存储实际数据。任何以 {@code read} 或 {@code skip} 开头的操作都将获取或跳过当前
 * {@link #readerIndex() readerIndex} 处的数据，并将 {@link #readerIndex() readerIndex} 增加读取的字节数。
 * 如果读操作的参数也是一个 {@link ByteBuf} 并且未指定目标索引，则指定缓冲区的
 * {@link #writerIndex() writerIndex} 也会一起增加。
 * <p>
 * 如果剩余内容不足，则会抛出 {@link IndexOutOfBoundsException}。新分配、包装或复制的缓冲区的
 * {@link #readerIndex() readerIndex} 默认值为 {@code 0}。
 *
 * <pre>
 * // 遍历缓冲区的可读字节。
 * {@link ByteBuf} buffer = ...;
 * while (buffer.isReadable()) {
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 *
 * <h4>可写字节</h4>
 *
 * 此段是一个未定义的空间，需要填充。任何以 {@code write} 开头的操作都将在当前
 * {@link #writerIndex() writerIndex} 处写入数据，并将 {@link #writerIndex() writerIndex} 增加写入的字节数。
 * 如果写操作的参数也是一个 {@link ByteBuf}，并且未指定源索引，则指定缓冲区的
 * {@link #readerIndex() readerIndex} 也会一起增加。
 * <p>
 * 如果剩余可写字节不足，则会抛出 {@link IndexOutOfBoundsException}。新分配缓冲区的
 * {@link #writerIndex() writerIndex} 默认值为 {@code 0}。包装或复制的缓冲区的
 * {@link #writerIndex() writerIndex} 默认值为缓冲区的 {@link #capacity() capacity}。
 *
 * <pre>
 * // 用随机整数填充缓冲区的可写字节。
 * {@link ByteBuf} buffer = ...;
 * while (buffer.maxWritableBytes() >= 4) {
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 *
 * <h4>可丢弃字节</h4>
 *
 * 此段包含已通过读操作读取的字节。最初，此段的大小为 {@code 0}，但随着读操作的执行，其大小会增加到
 * {@link #writerIndex() writerIndex}。可以通过调用 {@link #discardReadBytes()} 来丢弃已读字节，以回收未使用的区域，如下所示：
 *
 * <pre>
 *  调用 discardReadBytes() 之前
 *
 *      +-------------------+------------------+------------------+
 *      | 可丢弃字节 |  可读字节  |  可写字节  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  调用 discardReadBytes() 之后
 *
 *      +------------------+--------------------------------------+
 *      |  可读字节  |    可写字节（获得更多空间）   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (减少)        <=        capacity
 * </pre>
 *
 * 请注意，调用 {@link #discardReadBytes()} 后，可写字节的内容无法保证。在大多数情况下，可写字节不会移动，甚至可能根据底层缓冲区实现填充完全不同的数据。
 *
 * <h4>清除缓冲区索引</h4>
 *
 * 可以通过调用 {@link #clear()} 将 {@link #readerIndex() readerIndex} 和
 * {@link #writerIndex() writerIndex} 都设置为 {@code 0}。它不会清除缓冲区内容（例如填充 {@code 0}），而只是清除两个指针。请注意，此操作的语义与 {@link ByteBuffer#clear()} 不同。
 *
 * <pre>
 *  调用 clear() 之前
 *
 *      +-------------------+------------------+------------------+
 *      | 可丢弃字节 |  可读字节  |  可写字节  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  调用 clear() 之后
 *
 *      +---------------------------------------------------------+
 *      |             可写字节（获得更多空间）             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 *
 * <h3>搜索操作</h3>
 *
 * 对于简单的单字节搜索，使用 {@link #indexOf(int, int, byte)} 和 {@link #bytesBefore(int, int, byte)}。
 * {@link #bytesBefore(byte)} 在处理 {@code NUL} 终止字符串时特别有用。对于复杂的搜索，使用 {@link #forEachByte(int, int, ByteProcessor)} 和 {@link ByteProcessor} 实现。
 *
 * <h3>标记和重置</h3>
 *
 * 每个缓冲区中有两个标记索引。一个用于存储 {@link #readerIndex() readerIndex}，另一个用于存储
 * {@link #writerIndex() writerIndex}。可以通过调用重置方法重新定位其中一个索引。它的工作方式类似于
 * {@link InputStream} 中的标记和重置方法，只是没有 {@code readlimit}。
 *
 * <h3>派生缓冲区</h3>
 *
 * 可以通过调用以下方法之一创建现有缓冲区的视图：
 * <ul>
 *   <li>{@link #duplicate()}</li>
 *   <li>{@link #slice()}</li>
 *   <li>{@link #slice(int, int)}</li>
 *   <li>{@link #readSlice(int)}</li>
 *   <li>{@link #retainedDuplicate()}</li>
 *   <li>{@link #retainedSlice()}</li>
 *   <li>{@link #retainedSlice(int, int)}</li>
 *   <li>{@link #readRetainedSlice(int)}</li>
 * </ul>
 * 派生缓冲区将具有独立的 {@link #readerIndex() readerIndex}、
 * {@link #writerIndex() writerIndex} 和标记索引，同时共享其他内部数据表示，就像 NIO 缓冲区一样。
 * <p>
 * 如果需要现有缓冲区的全新副本，请调用 {@link #copy()} 方法。
 *
 * <h4>非保留和保留的派生缓冲区</h4>
 *
 * 请注意，{@link #duplicate()}、{@link #slice()}、{@link #slice(int, int)} 和 {@link #readSlice(int)} 不会在返回的派生缓冲区上调用 {@link #retain()}，因此其引用计数不会增加。如果需要创建引用计数增加的派生缓冲区，请考虑使用 {@link #retainedDuplicate()}、{@link #retainedSlice()}、{@link #retainedSlice(int, int)} 和 {@link #readRetainedSlice(int)}，它们可能会返回产生较少垃圾的缓冲区实现。
 *
 * <h3>转换为现有 JDK 类型</h3>
 *
 * <h4>字节数组</h4>
 *
 * 如果 {@link ByteBuf} 由字节数组（即 {@code byte[]}）支持，可以通过 {@link #array()} 方法直接访问它。要确定缓冲区是否由字节数组支持，应使用 {@link #hasArray()}。
 *
 * <h4>NIO 缓冲区</h4>
 *
 * 如果 {@link ByteBuf} 可以转换为共享其内容的 NIO {@link ByteBuffer}（即视图缓冲区），可以通过 {@link #nioBuffer()} 方法获取它。要确定缓冲区是否可以转换为 NIO 缓冲区，请使用 {@link #nioBufferCount()}。
 *
 * <h4>字符串</h4>
 *
 * 各种 {@link #toString(Charset)} 方法将 {@link ByteBuf} 转换为 {@link String}。请注意，{@link #toString()} 不是转换方法。
 *
 * <h4>I/O 流</h4>
 *
 * 请参考 {@link ByteBufInputStream} 和 {@link ByteBufOutputStream}。
 */
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf>, ByteBufConvertible {

    /**
     * Returns the number of bytes (octets) this buffer can contain.
     */

    /**
     * 返回此缓冲区可以包含的字节数（八位字节）。
     */
    public abstract int capacity();

    /**
     * Adjusts the capacity of this buffer.  If the {@code newCapacity} is less than the current
     * capacity, the content of this buffer is truncated.  If the {@code newCapacity} is greater
     * than the current capacity, the buffer is appended with unspecified data whose length is
     * {@code (newCapacity - currentCapacity)}.
     *
     * @throws IllegalArgumentException if the {@code newCapacity} is greater than {@link #maxCapacity()}
     */

    /**
     * 调整此缓冲区的容量。如果 {@code newCapacity} 小于当前容量，则此缓冲区的内容将被截断。如果 {@code newCapacity} 大于当前容量，则缓冲区将附加长度
     * 为 {@code (newCapacity - currentCapacity)} 的未指定数据。
     *
     * @throws IllegalArgumentException 如果 {@code newCapacity} 大于 {@link #maxCapacity()}
     */
    public abstract ByteBuf capacity(int newCapacity);

    /**
     * Returns the maximum allowed capacity of this buffer. This value provides an upper
     * bound on {@link #capacity()}.
     */

    /**
     * 返回此缓冲区的最大允许容量。该值提供了 {@link #capacity()} 的上限。
     */
    public abstract int maxCapacity();

    /**
     * Returns the {@link ByteBufAllocator} which created this buffer.
     */

    /**
     * 返回创建此缓冲区的 {@link ByteBufAllocator}。
     */
    public abstract ByteBufAllocator alloc();

    /**
     * Returns the <a href="https://en.wikipedia.org/wiki/Endianness">endianness</a>
     * of this buffer.
     *
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     */

    /**
     * 返回此缓冲区的<a href="https://en.wikipedia.org/wiki/Endianness">字节序</a>。
     *
     * @deprecated 使用小端序访问器，例如 {@code getShortLE}, {@code getIntLE}，
     * 而不是创建一个交换了 {@code endianness} 的缓冲区。
     */
    @Deprecated
    public abstract ByteOrder order();

    /**
     * Returns a buffer with the specified {@code endianness} which shares the whole region,
     * indexes, and marks of this buffer.  Modifying the content, the indexes, or the marks of the
     * returned buffer or this buffer affects each other's content, indexes, and marks.  If the
     * specified {@code endianness} is identical to this buffer's byte order, this method can
     * return {@code this}.  This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     */

    /**
     * 返回一个具有指定 {@code endianness} 的缓冲区，该缓冲区共享此缓冲区的整个区域、
     * 索引和标记。修改返回缓冲区或此缓冲区的内容、索引或标记会相互影响。如果指定的
     * {@code endianness} 与此缓冲区的字节顺序相同，则此方法可以返回 {@code this}。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @deprecated 使用小端访问器，例如 {@code getShortLE}、{@code getIntLE}，
     * 而不是创建一个具有交换 {@code endianness} 的缓冲区。
     */
    @Deprecated
    public abstract ByteBuf order(ByteOrder endianness);

    /**
     * Return the underlying buffer instance if this buffer is a wrapper of another buffer.
     *
     * @return {@code null} if this buffer is not a wrapper
     */

    /**
     * 如果此缓冲区是另一个缓冲区的包装器，则返回底层缓冲区实例。
     *
     * @return 如果此缓冲区不是包装器，则返回 {@code null}
     */
    public abstract ByteBuf unwrap();

    /**
     * Returns {@code true} if and only if this buffer is backed by an
     * NIO direct buffer.
     */

    /**
     * 当且仅当此缓冲区由NIO直接缓冲区支持时返回{@code true}。
     */
    public abstract boolean isDirect();

    /**
     * Returns {@code true} if and only if this buffer is read-only.
     */

    /**
     * 当且仅当此缓冲区为只读时返回 {@code true}。
     */
    public abstract boolean isReadOnly();

    /**
     * Returns a read-only version of this buffer.
     */

    /**
     * 返回此缓冲区的只读版本。
     */
    public abstract ByteBuf asReadOnly();

    /**
     * Returns the {@code readerIndex} of this buffer.
     */

    /**
     * 返回此缓冲区的 {@code readerIndex}。
     */
    public abstract int readerIndex();

    /**
     * Sets the {@code readerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is
     *            less than {@code 0} or
     *            greater than {@code this.writerIndex}
     */

    /**
     * 设置此缓冲区的 {@code readerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code readerIndex} 小于 {@code 0} 或大于 {@code this.writerIndex}
     */
    public abstract ByteBuf readerIndex(int readerIndex);

    /**
     * Returns the {@code writerIndex} of this buffer.
     */

    /**
     * 返回此缓冲区的 {@code writerIndex}。
     */
    public abstract int writerIndex();

    /**
     * Sets the {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code writerIndex} is
     *            less than {@code this.readerIndex} or
     *            greater than {@code this.capacity}
     */

    /**
     * 设置此缓冲区的 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code writerIndex} 小于 {@code this.readerIndex} 或
     *         大于 {@code this.capacity}
     */
    public abstract ByteBuf writerIndex(int writerIndex);

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer
     * in one shot.  This method is useful when you have to worry about the
     * invocation order of {@link #readerIndex(int)} and {@link #writerIndex(int)}
     * methods.  For example, the following code will fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 0 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.buffer(8);
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // readerIndex (2) cannot be greater than the current writerIndex (0).
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     *
     * The following code will also fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 8 and 8 respectively.
     * {@link ByteBuf} buf = {@link Unpooled}.wrappedBuffer(new byte[8]);
     *
     * // readerIndex becomes 8.
     * buf.readLong();
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // writerIndex (4) cannot be less than the current readerIndex (8).
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     *
     * By contrast, this method guarantees that it never
     * throws an {@link IndexOutOfBoundsException} as long as the specified
     * indexes meet basic constraints, regardless what the current index
     * values of the buffer are:
     *
     * <pre>
     * // No matter what the current state of the buffer is, the following
     * // call always succeeds as long as the capacity of the buffer is not
     * // less than 4.
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code readerIndex} is less than 0,
     *         if the specified {@code writerIndex} is less than the specified
     *         {@code readerIndex} or if the specified {@code writerIndex} is
     *         greater than {@code this.capacity}
     */

    /**
     * 一次性设置此缓冲区的 {@code readerIndex} 和 {@code writerIndex}。
     * 当您需要担心 {@link #readerIndex(int)} 和 {@link #writerIndex(int)} 方法的调用顺序时，
     * 此方法非常有用。例如，以下代码将失败：
     *
     * <pre>
     * // 创建一个 readerIndex、writerIndex 和容量分别为 0、0 和 8 的缓冲区。
     * {@link ByteBuf} buf = {@link Unpooled}.buffer(8);
     *
     * // 抛出 IndexOutOfBoundsException，因为指定的 readerIndex (2) 不能大于当前的 writerIndex (0)。
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     *
     * 以下代码也将失败：
     *
     * <pre>
     * // 创建一个 readerIndex、writerIndex 和容量分别为 0、8 和 8 的缓冲区。
     * {@link ByteBuf} buf = {@link Unpooled}.wrappedBuffer(new byte[8]);
     *
     * // readerIndex 变为 8。
     * buf.readLong();
     *
     * // 抛出 IndexOutOfBoundsException，因为指定的 writerIndex (4) 不能小于当前的 readerIndex (8)。
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     *
     * 相比之下，此方法保证只要指定的索引满足基本约束，无论缓冲区的当前索引值如何，都不会抛出 {@link IndexOutOfBoundsException}：
     *
     * <pre>
     * // 无论缓冲区的当前状态如何，只要缓冲区的容量不小于 4，以下调用始终成功。
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code readerIndex} 小于 0，
     *         如果指定的 {@code writerIndex} 小于指定的 {@code readerIndex}，
     *         或者如果指定的 {@code writerIndex} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setIndex(int readerIndex, int writerIndex);

    /**
     * Returns the number of readable bytes which is equal to
     * {@code (this.writerIndex - this.readerIndex)}.
     */

    /**
     * 返回可读字节数，等于
     * {@code (this.writerIndex - this.readerIndex)}。
     */
    public abstract int readableBytes();

    /**
     * Returns the number of writable bytes which is equal to
     * {@code (this.capacity - this.writerIndex)}.
     */

    /**
     * 返回可写入的字节数，等于
     * {@code (this.capacity - this.writerIndex)}。
     */
    public abstract int writableBytes();

    /**
     * Returns the maximum possible number of writable bytes, which is equal to
     * {@code (this.maxCapacity - this.writerIndex)}.
     */

    /**
     * 返回可写入的最大字节数，其值等于
     * {@code (this.maxCapacity - this.writerIndex)}。
     */
    public abstract int maxWritableBytes();

    /**
     * Returns the maximum number of bytes which can be written for certain without involving
     * an internal reallocation or data-copy. The returned value will be &ge; {@link #writableBytes()}
     * and &le; {@link #maxWritableBytes()}.
     */

    /**
     * 返回在不涉及内部重新分配或数据复制的情况下可以安全写入的最大字节数。返回的值将大于等于 {@link #writableBytes()} 
     * 且小于等于 {@link #maxWritableBytes()}。
     */
    public int maxFastWritableBytes() {
        return writableBytes();
    }

    /**
     * Returns {@code true}
     * if and only if {@code (this.writerIndex - this.readerIndex)} is greater
     * than {@code 0}.
     */

    /**
     * 当且仅当 {@code (this.writerIndex - this.readerIndex)} 大于 {@code 0} 时，
     * 返回 {@code true}。
     */
    public abstract boolean isReadable();

    /**
     * Returns {@code true} if and only if this buffer contains equal to or more than the specified number of elements.
     */

    /**
     * 当且仅当此缓冲区包含等于或大于指定数量的元素时返回 {@code true}。
     */
    public abstract boolean isReadable(int size);

    /**
     * Returns {@code true}
     * if and only if {@code (this.capacity - this.writerIndex)} is greater
     * than {@code 0}.
     */

    /**
     * 当且仅当 {@code (this.capacity - this.writerIndex)} 大于 {@code 0} 时，
     * 返回 {@code true}。
     */
    public abstract boolean isWritable();

    /**
     * Returns {@code true} if and only if this buffer has enough room to allow writing the specified number of
     * elements.
     */

    /**
     * 当且仅当此缓冲区有足够的空间以允许写入指定数量的元素时，返回 {@code true}。
     */
    public abstract boolean isWritable(int size);

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
     * {@code 0}.
     * This method is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
     * <p>
     * Please note that the behavior of this method is different
     * from that of NIO buffer, which sets the {@code limit} to
     * the {@code capacity} of the buffer.
     */

    /**
     * 将此缓冲区的 {@code readerIndex} 和 {@code writerIndex} 设置为 {@code 0}。
     * 此方法与 {@link #setIndex(int, int) setIndex(0, 0)} 相同。
     * <p>
     * 请注意，此方法的行为与 NIO 缓冲区的行为不同，
     * NIO 缓冲区将 {@code limit} 设置为缓冲区的 {@code capacity}。
     */
    public abstract ByteBuf clear();

    /**
     * Marks the current {@code readerIndex} in this buffer.  You can
     * reposition the current {@code readerIndex} to the marked
     * {@code readerIndex} by calling {@link #resetReaderIndex()}.
     * The initial value of the marked {@code readerIndex} is {@code 0}.
     */

    /**
     * 标记当前缓冲区中的 {@code readerIndex}。你可以通过调用
     * {@link #resetReaderIndex()} 将当前的 {@code readerIndex} 重新定位到
     * 标记的 {@code readerIndex}。标记的 {@code readerIndex} 的初始值为 {@code 0}。
     */
    public abstract ByteBuf markReaderIndex();

    /**
     * Repositions the current {@code readerIndex} to the marked
     * {@code readerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code writerIndex} is less than the marked
     *         {@code readerIndex}
     */

    /**
     * 将当前 {@code readerIndex} 重新定位到此缓冲区中标记的
     * {@code readerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果当前 {@code writerIndex} 小于标记的
     *         {@code readerIndex}
     */
    public abstract ByteBuf resetReaderIndex();

    /**
     * Marks the current {@code writerIndex} in this buffer.  You can
     * reposition the current {@code writerIndex} to the marked
     * {@code writerIndex} by calling {@link #resetWriterIndex()}.
     * The initial value of the marked {@code writerIndex} is {@code 0}.
     */

    /**
     * 标记当前缓冲区中的 {@code writerIndex}。您可以通过调用 {@link #resetWriterIndex()} 
     * 将当前的 {@code writerIndex} 重新定位到标记的 {@code writerIndex}。
     * 标记的 {@code writerIndex} 的初始值为 {@code 0}。
     */
    public abstract ByteBuf markWriterIndex();

    /**
     * Repositions the current {@code writerIndex} to the marked
     * {@code writerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the current {@code readerIndex} is greater than the marked
     *         {@code writerIndex}
     */

    /**
     * 将当前 {@code writerIndex} 重新定位到此缓冲区中标记的
     * {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果当前 {@code readerIndex} 大于标记的
     *         {@code writerIndex}
     */
    public abstract ByteBuf resetWriterIndex();

    /**
     * Discards the bytes between the 0th index and {@code readerIndex}.
     * It moves the bytes between {@code readerIndex} and {@code writerIndex}
     * to the 0th index, and sets {@code readerIndex} and {@code writerIndex}
     * to {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
     * <p>
     * Please refer to the class documentation for more detailed explanation.
     */

    /**
     * 丢弃从第0个索引到{@code readerIndex}之间的字节。
     * 它将{@code readerIndex}和{@code writerIndex}之间的字节移动到第0个索引，
     * 并将{@code readerIndex}和{@code writerIndex}分别设置为{@code 0}和{@code oldWriterIndex - oldReaderIndex}。
     * <p>
     * 更多详细说明，请参考类文档。
     */
    public abstract ByteBuf discardReadBytes();

    /**
     * Similar to {@link ByteBuf#discardReadBytes()} except that this method might discard
     * some, all, or none of read bytes depending on its internal implementation to reduce
     * overall memory bandwidth consumption at the cost of potentially additional memory
     * consumption.
     */

    /**
     * 类似于 {@link ByteBuf#discardReadBytes()}，不同之处在于此方法可能会根据其内部实现丢弃部分、全部或不丢弃已读字节，
     * 以减少整体内存带宽消耗，但可能会增加额外的内存消耗。
     */
    public abstract ByteBuf discardSomeReadBytes();

    /**
     * Expands the buffer {@link #capacity()} to make sure the number of
     * {@linkplain #writableBytes() writable bytes} is equal to or greater than the
     * specified value.  If there are enough writable bytes in this buffer, this method
     * returns with no side effect.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @throws IndexOutOfBoundsException
     *         if {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}.
     * @see #capacity(int)
     */

    /**
     * 扩展缓冲区 {@link #capacity()} 以确保 {@linkplain #writableBytes() 可写字节数} 大于或等于
     * 指定的值。如果缓冲区中有足够的可写字节，此方法将不产生任何副作用。
     *
     * @param minWritableBytes
     *        期望的最小可写字节数
     * @throws IndexOutOfBoundsException
     *         如果 {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}。
     * @see #capacity(int)
     */
    public abstract ByteBuf ensureWritable(int minWritableBytes);

    /**
     * Expands the buffer {@link #capacity()} to make sure the number of
     * {@linkplain #writableBytes() writable bytes} is equal to or greater than the
     * specified value. Unlike {@link #ensureWritable(int)}, this method returns a status code.
     *
     * @param minWritableBytes
     *        the expected minimum number of writable bytes
     * @param force
     *        When {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}:
     *        <ul>
     *        <li>{@code true} - the capacity of the buffer is expanded to {@link #maxCapacity()}</li>
     *        <li>{@code false} - the capacity of the buffer is unchanged</li>
     *        </ul>
     * @return {@code 0} if the buffer has enough writable bytes, and its capacity is unchanged.
     *         {@code 1} if the buffer does not have enough bytes, and its capacity is unchanged.
     *         {@code 2} if the buffer has enough writable bytes, and its capacity has been increased.
     *         {@code 3} if the buffer does not have enough bytes, but its capacity has been
     *                   increased to its maximum.
     */

    /**
     * 扩展缓冲区 {@link #capacity()} 以确保 {@linkplain #writableBytes() 可写字节数} 等于或大于指定值。
     * 与 {@link #ensureWritable(int)} 不同，此方法返回一个状态码。
     *
     * @param minWritableBytes
     *        期望的最小可写字节数
     * @param force
     *        当 {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()} 时：
     *        <ul>
     *        <li>{@code true} - 缓冲区的容量扩展到 {@link #maxCapacity()}</li>
     *        <li>{@code false} - 缓冲区的容量保持不变</li>
     *        </ul>
     * @return {@code 0} 如果缓冲区有足够的可写字节，且其容量未改变。
     *         {@code 1} 如果缓冲区没有足够的字节，且其容量未改变。
     *         {@code 2} 如果缓冲区有足够的可写字节，且其容量已增加。
     *         {@code 3} 如果缓冲区没有足够的字节，但其容量已增加到最大值。
     */
    public abstract int ensureWritable(int minWritableBytes, boolean force);

    /**
     * Gets a boolean at the specified absolute (@code index) in this buffer.
     * This method does not modify the {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */

    /**
     * 从该缓冲区的指定绝对位置 (@code index) 获取一个布尔值。
     * 此方法不会修改该缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 1} 大于 {@code this.capacity}
     */
    public abstract boolean getBoolean(int index);

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的字节。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 1} 大于 {@code this.capacity}
     */
    public abstract byte  getByte(int index);

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的无符号字节。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 1} 大于 {@code this.capacity}
     */
    public abstract short getUnsignedByte(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处获取此缓冲区中的 16 位短整型值。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract short getShort(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 以Little Endian字节顺序获取此缓冲区中指定绝对 {@code index} 处的16位短整数。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract short getShortLE(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处获取一个无符号的16位短整数。此方法不会修改
     * 此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract int getUnsignedShort(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处从该缓冲区中获取一个以 Little Endian 字节顺序表示的
     * 无符号 16 位短整数。此方法不会修改该缓冲区的 {@code readerIndex} 或
     * {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract int getUnsignedShortLE(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处获取此缓冲区中的24位中等整数。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract int   getMedium(int index);

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer in the Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处从该缓冲区中以小端字节序获取一个24位的中等整数。此方法不会修改
     * 该缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract int getMediumLE(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的无符号24位中等整数。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract int   getUnsignedMedium(int index);

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 以Little Endian字节顺序获取此缓冲区中指定绝对 {@code index} 处的无符号24位中等整数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract int   getUnsignedMediumLE(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处获取此缓冲区中的 32 位整数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract int   getInt(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer with Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 从该缓冲区中指定的绝对 {@code index} 处获取一个 32 位整数，使用小端字节序。此方法不会修改该缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract int   getIntLE(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区中指定的绝对 {@code index} 处获取一个无符号的32位整数。
     * 此方法不会修改缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract long  getUnsignedInt(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 以Little Endian字节顺序获取此缓冲区中指定绝对{@code index}处的无符号32位整数。此方法不会
     * 修改此缓冲区的{@code readerIndex}或{@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的{@code index}小于{@code 0}或
     *         {@code index + 4}大于{@code this.capacity}
     */
    public abstract long  getUnsignedIntLE(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的 64 位长整型值。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract long  getLong(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处从该缓冲区中以小端字节序获取一个64位长整型。
     * 此方法不会修改该缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract long  getLongLE(int index);

    /**
     * Gets a 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的 2 字节 UTF-16 字符。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract char  getChar(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的 32 位浮点数。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract float getFloat(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 以小端字节序从指定的绝对 {@code index} 处获取此缓冲区中的 32 位浮点数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public float getFloatLE(int index) {
        return Float.intBitsToFloat(getIntLE(index));
    }

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 获取此缓冲区中指定绝对 {@code index} 处的 64 位浮点数。此方法不会修改
     * 此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract double getDouble(int index);

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 以Little Endian字节顺序获取此缓冲区中指定绝对
     * {@code index}处的64位浮点数。
     * 此方法不会修改此缓冲区的{@code readerIndex}或
     * {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的{@code index}小于{@code 0}或
     *         {@code index + 8}大于{@code this.capacity}
     */
    public double getDoubleLE(int index) {
        return Double.longBitsToDouble(getLongLE(index));
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with
     * {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.writableBytes} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标缓冲区，从指定的绝对 {@code index} 开始，直到目标缓冲区不可写为止。
     * 此方法基本上与 {@link #getBytes(int, ByteBuf, int, int)} 相同，不同之处在于此方法会增加目标缓冲区的
     * {@code writerIndex}，增加的大小为传输的字节数，而 {@link #getBytes(int, ByteBuf, int, int)} 不会。
     * 此方法不会修改源缓冲区（即 {@code this}）的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，或者如果 {@code index + dst.writableBytes} 大于
     *         {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code dst.writableBytes}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标位置，从指定的绝对索引 {@code index} 开始。此方法基本上与
     * {@link #getBytes(int, ByteBuf, int, int)} 相同，区别在于此方法会增加目标的
     * {@code writerIndex}，增加的数量为传输的字节数，而 {@link #getBytes(int, ByteBuf, int, int)} 不会。
     * 此方法不会修改源缓冲区（即 {@code this}）的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果 {@code index + length} 大于 {@code this.capacity}，或
     *         如果 {@code length} 大于 {@code dst.writableBytes}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     */

    /**
     * 将此缓冲区的数据传输到从指定绝对索引 {@code index} 开始的指定目标。
     * 此方法不会修改源（即 {@code this}）和目标缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param dstIndex 目标的第一个索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果指定的 {@code dstIndex} 小于 {@code 0}，
     *         如果 {@code index + length} 大于
     *            {@code this.capacity}，或
     *         如果 {@code dstIndex + length} 大于
     *            {@code dst.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.length} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标位置，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + dst.length} 大于 {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.length}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标位置，从指定的绝对索引 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param dstIndex 目标位置的起始索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果指定的 {@code dstIndex} 小于 {@code 0}，
     *         如果 {@code index + length} 大于
     *            {@code this.capacity}，或
     *         如果 {@code dstIndex + length} 大于
     *            {@code dst.length}
     */
    public abstract ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer while the destination's {@code position} will be increased.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + dst.remaining()} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标，从指定的绝对 {@code index} 开始，
     * 直到目标的位置达到其限制。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}，
     * 而目标的 {@code position} 将会增加。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + dst.remaining()} 大于 {@code this.capacity}
     */
    public abstract ByteBuf getBytes(int index, ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */

    /**
     * 将此缓冲区的数据传输到指定的流，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定的流在 I/O 期间抛出异常
     */
    public abstract ByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified channel starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将此缓冲区的数据传输到指定的通道，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的最大字节数
     *
     * @return 实际写入指定通道的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定的通道在 I/O 期间抛出异常
     */
    public abstract int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    /**
     * Transfers this buffer's data starting at the specified absolute {@code index}
     * to the specified channel starting at the given file position.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than
     *            {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将此缓冲区的数据从指定的绝对 {@code index} 开始传输到指定通道的给定文件位置。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * 此方法不会修改通道的位置。
     *
     * @param position 传输开始的文件位置
     * @param length 要传输的最大字节数
     *
     * @return 实际写入指定通道的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定的通道在 I/O 期间抛出异常
     */
    public abstract int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the given index.
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 获取给定索引处具有给定长度的 {@link CharSequence}。
     *
     * @param length 要读取的长度
     * @param charset 应使用的字符集
     * @return 序列
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract CharSequence getCharSequence(int index, int length, Charset charset);

    /**
     * Sets the specified boolean at the specified absolute {@code index} in this
     * buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区中指定的绝对 {@code index} 处设置指定的布尔值。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 1} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setBoolean(int index, boolean value);

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区的指定绝对 {@code index} 处设置指定的字节。指定的值的高24位将被忽略。
     * 此方法不会修改缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 1} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setByte(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区中指定的绝对索引处设置指定的16位短整型值。指定的值的高16位将被忽略。
     * 此方法不会修改缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setShort(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer with the Little Endian Byte Order.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 使用小端字节序在指定的绝对 {@code index} 处设置指定的16位短整型值。
     * 指定值的高16位将被忽略。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setShortLE(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  Please note that the most significant
     * byte is ignored in the specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 24 位中等整数。请注意，在指定的值中，最高有效字节将被忽略。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或 {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setMedium(int index, int value);

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in the Little Endian Byte Order.
     * Please note that the most significant byte is ignored in the
     * specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 3} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 24 位中等整数，使用小端字节序。
     * 请注意，在指定的值中，最高有效字节将被忽略。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 3} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setMediumLE(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 32 位整数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setInt(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer with Little Endian byte order
     * .
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 使用小端字节序在指定的绝对 {@code index} 处设置指定的 32 位整数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setIntLE(int index, int value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区的指定绝对 {@code index} 处设置指定的 64 位长整型值。
     * 此方法不会修改缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setLong(int index, long value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处将此缓冲区中的 64 位长整型数设置为小端字节序。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setLongLE(int index, long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 2 字节 UTF-16 字符。
     * 指定值的高 16 位将被忽略。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 2} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setChar(int index, int value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 在缓冲区中指定的绝对 {@code index} 处设置指定的 32 位浮点数。
     * 此方法不会修改缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setFloat(int index, float value);

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 32 位浮点数，使用小端字节序。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 4} 大于 {@code this.capacity}
     */
    public ByteBuf setFloatLE(int index, float value) {
        return setIntLE(index, Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 64 位浮点数。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setDouble(int index, double value);

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */

    /**
     * 在指定的绝对 {@code index} 处设置指定的 64 位浮点数，使用小端字节序。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         {@code index + 8} 大于 {@code this.capacity}
     */
    public ByteBuf setDoubleLE(int index, double value) {
        return setLongLE(index, Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with
     * {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer (i.e. {@code this}).
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.readableBytes} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将指定源缓冲区的数据传输到此缓冲区，从指定的绝对 {@code index} 开始，直到源缓冲区变为不可读。此方法基本与
     * {@link #setBytes(int, ByteBuf, int, int)} 相同，不同之处在于此方法会增加源缓冲区的 {@code readerIndex}，
     * 增加量为传输的字节数，而 {@link #setBytes(int, ByteBuf, int, int)} 不会。此方法不会修改此缓冲区（即 {@code this}）
     * 的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + src.readableBytes} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code length} is greater than {@code src.readableBytes}
     */

    /**
     * 将指定源缓冲区的数据转移到此缓冲区，从指定的绝对 {@code index} 开始。此方法基本上与
     * {@link #setBytes(int, ByteBuf, int, int)} 相同，不同之处在于此方法会增加源缓冲区的
     * {@code readerIndex}，增加的大小为传输的字节数，而
     * {@link #setBytes(int, ByteBuf, int, int)} 不会。
     * 此方法不会修改此缓冲区（即 {@code this}）的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果 {@code index + length} 大于 {@code this.capacity}，或者
     *         如果 {@code length} 大于 {@code src.readableBytes}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than
     *            {@code src.capacity}
     */

    /**
     * 将指定源缓冲区的数据传输到此缓冲区，从指定的绝对 {@code index} 开始。
     * 此方法不会修改源（即 {@code this}）和目标缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param srcIndex 源缓冲区的起始索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果指定的 {@code srcIndex} 小于 {@code 0}，
     *         如果 {@code index + length} 大于
     *            {@code this.capacity}，或者
     *         如果 {@code srcIndex + length} 大于
     *            {@code src.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.length} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将指定源数组的数据传输到此缓冲区，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + src.length} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than
     *            {@code this.capacity}, or
     *         if {@code srcIndex + length} is greater than {@code src.length}
     */

    /**
     * 将指定源数组的数据传输到此缓冲区中，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0}，
     *         如果指定的 {@code srcIndex} 小于 {@code 0}，
     *         如果 {@code index + length} 大于 {@code this.capacity}，或
     *         如果 {@code srcIndex + length} 大于 {@code src.length}
     */
    public abstract ByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + src.remaining()} is greater than
     *            {@code this.capacity}
     */

    /**
     * 将指定源缓冲区的数据从指定绝对位置 {@code index} 开始传输到此缓冲区，直到源缓冲区的位置达到其限制。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + src.remaining()} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setBytes(int index, ByteBuffer src);

    /**
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified {@link InputStream} reached EOF.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */

    /**
     * 将指定源流的内容传输到此缓冲区，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         {@code -1} 如果指定的 {@link InputStream} 到达了 EOF。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定的流在 I/O 期间抛出异常
     */
    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed or it reached EOF.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将指定源通道的内容传输到此缓冲区，从指定的绝对 {@code index} 开始。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要传输的最大字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         {@code -1} 如果指定通道已关闭或到达 EOF。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定通道在 I/O 期间抛出异常
     */
    public abstract int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified source channel starting at the given file position
     * to this buffer starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed or it reached EOF.
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将指定源通道的内容从给定的文件位置开始传输到此缓冲区中指定的绝对 {@code index} 处。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。此方法不会修改通道的位置。
     *
     * @param position 传输开始的文件位置
     * @param length 要传输的最大字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         {@code -1} 如果指定的通道已关闭或到达文件末尾。
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     * @throws IOException
     *         如果指定的通道在 I/O 期间抛出异常
     */
    public abstract int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
     * absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code index} is less than {@code 0} or
     *         if {@code index + length} is greater than {@code this.capacity}
     */

    /**
     * 从指定的绝对 {@code index} 开始，用 <tt>NUL (0x00)</tt> 填充此缓冲区。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @param length 要写入缓冲区的 <tt>NUL</tt> 的数量
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code index} 小于 {@code 0} 或
     *         如果 {@code index + length} 大于 {@code this.capacity}
     */
    public abstract ByteBuf setZero(int index, int length);

    /**
     * Writes the specified {@link CharSequence} at the given {@code index}.
     * The {@code writerIndex} is not modified by this method.
     *
     * @param index on which the sequence should be written
     * @param sequence to write
     * @param charset that should be used.
     * @return the written number of bytes.
     * @throws IndexOutOfBoundsException
     *         if the sequence at the given index would be out of bounds of the buffer capacity
     */

    /**
     * 在指定的 {@code index} 处写入给定的 {@link CharSequence}。
     * 该方法不会修改 {@code writerIndex}。
     *
     * @param index 序列应写入的位置
     * @param sequence 要写入的序列
     * @param charset 应使用的字符集
     * @return 写入的字节数
     * @throws IndexOutOfBoundsException
     *         如果序列在给定索引处超出缓冲区容量的范围
     */
    public abstract int setCharSequence(int index, CharSequence sequence, Charset charset);

    /**
     * Gets a boolean at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */

    /**
     * 获取当前 {@code readerIndex} 处的布尔值，并将 {@code readerIndex} 增加 {@code 1}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 1}
     */
    public abstract boolean readBoolean();

    /**
     * Gets a byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */

    /**
     * 获取当前 {@code readerIndex} 处的一个字节，并将
     * 该缓冲区的 {@code readerIndex} 增加 {@code 1}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 1}
     */
    public abstract byte  readByte();

    /**
     * Gets an unsigned byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 1}
     */

    /**
     * 从当前 {@code readerIndex} 处获取一个无符号字节，并将此缓冲区中的 {@code readerIndex} 增加 {@code 1}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 1}
     */
    public abstract short readUnsignedByte();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */

    /**
     * 获取当前 {@code readerIndex} 处的 16 位短整型，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 2}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 2}
     */
    public abstract short readShort();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */

    /**
     * 在当前 {@code readerIndex} 处以小端字节序获取一个16位短整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 2}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 2}
     */
    public abstract short readShortLE();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */

    /**
     * 在当前 {@code readerIndex} 处获取一个无符号的16位短整型，并将 {@code readerIndex} 增加 {@code 2}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 2}
     */
    public abstract int   readUnsignedShort();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */

    /**
     * 以 Little Endian 字节顺序获取当前 {@code readerIndex} 处的无符号 16 位短整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 2}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 2}
     */
    public abstract int   readUnsignedShortLE();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */

    /**
     * 获取当前 {@code readerIndex} 处的 24 位中等整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 3}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 3}
     */
    public abstract int   readMedium();

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the
     * {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */

    /**
     * 获取当前 {@code readerIndex} 处的一个 24 位中等整数，使用小端字节序，
     * 并将此缓冲区的 {@code readerIndex} 增加 {@code 3}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 3}
     */
    public abstract int   readMediumLE();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */

    /**
     * 在当前 {@code readerIndex} 处获取一个无符号的24位中等整数，
     * 并将 {@code readerIndex} 增加 {@code 3}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 3}
     */
    public abstract int   readUnsignedMedium();

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 3}
     */

    /**
     * 从当前 {@code readerIndex} 处获取一个无符号的24位中等整数，按照小端字节序，
     * 并将 {@code readerIndex} 增加 {@code 3}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 3}
     */
    public abstract int   readUnsignedMediumLE();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 从当前的 {@code readerIndex} 处获取一个 32 位整数，
     * 并将 {@code readerIndex} 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public abstract int   readInt();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 以小端字节序获取当前 {@code readerIndex} 处的 32 位整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public abstract int   readIntLE();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 获取当前 {@code readerIndex} 处的无符号 32 位整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public abstract long  readUnsignedInt();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 以小端字节序获取当前 {@code readerIndex} 处的无符号 32 位整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public abstract long  readUnsignedIntLE();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */

    /**
     * 从当前的 {@code readerIndex} 处获取一个 64 位整数，
     * 并将 {@code readerIndex} 增加 {@code 8}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 8}
     */
    public abstract long  readLong();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */

    /**
     * 以小端字节序获取当前 {@code readerIndex} 处的 64 位整数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 8}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 8}
     */
    public abstract long  readLongLE();

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 2}
     */

    /**
     * 获取当前 {@code readerIndex} 处的2字节UTF-16字符，
     * 并将 {@code readerIndex} 增加 {@code 2}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 2}
     */
    public abstract char  readChar();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 从当前 {@code readerIndex} 处获取一个 32 位浮点数，
     * 并将 {@code readerIndex} 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public abstract float readFloat();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 4}
     */

    /**
     * 以小端字节序从当前 {@code readerIndex} 处获取一个 32 位浮点数，并将 {@code readerIndex}
     * 增加 {@code 4}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 4}
     */
    public float readFloatLE() {
        return Float.intBitsToFloat(readIntLE());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */

    /**
     * 从当前 {@code readerIndex} 处获取一个 64 位浮点数，
     * 并将 {@code readerIndex} 增加 {@code 8}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 8}
     */
    public abstract double readDouble();

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * in Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code this.readableBytes} is less than {@code 8}
     */

    /**
     * 以小端字节序获取当前 {@code readerIndex} 处的 64 位浮点数，
     * 并将此缓冲区中的 {@code readerIndex} 增加 {@code 8}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code this.readableBytes} 小于 {@code 8}
     */
    public double readDoubleLE() {
        return Double.longBitsToDouble(readLongLE());
    }

    /**
     * Transfers this buffer's data to a newly created buffer starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * The returned buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and {@code length} respectively.
     *
     * @param length the number of bytes to transfer
     *
     * @return the newly created buffer which contains the transferred bytes
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 将此缓冲区的数据传输到一个新创建的缓冲区，从当前的 {@code readerIndex} 开始，
     * 并将 {@code readerIndex} 增加传输的字节数（即 {@code length}）。
     * 返回的缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和 {@code length}。
     *
     * @param length 要传输的字节数
     *
     * @return 包含传输字节的新创建的缓冲区
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(int length);

    /**
     * Returns a new slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     *
     * @param length the size of the new slice
     *
     * @return the newly created slice
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 返回此缓冲区子区域的新切片，从当前的 {@code readerIndex} 开始，并将 {@code readerIndex} 增加新切片的大小（即 {@code length}）。
     * <p>
     * 请注意，此方法不会调用 {@link #retain()}，因此引用计数不会增加。
     *
     * @param length 新切片的大小
     *
     * @return 新创建的切片
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf readSlice(int length);

    /**
     * Returns a new retained slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #readSlice(int)}.
     * This method behaves similarly to {@code readSlice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     *
     * @param length the size of the new slice
     *
     * @return the newly created slice
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 返回此缓冲区子区域的新保留切片，从当前的 {@code readerIndex} 开始，
     * 并将 {@code readerIndex} 增加新切片的大小（即 {@code length}）。
     * <p>
     * 注意，与 {@link #readSlice(int)} 不同，此方法返回一个 {@linkplain #retain() 保留} 的缓冲区。
     * 此方法的行为类似于 {@code readSlice(...).retain()}，但此方法可能会返回一个产生更少垃圾的缓冲区实现。
     *
     * @param length 新切片的大小
     *
     * @return 新创建的切片
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf readRetainedSlice(int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination becomes
     * non-writable, and increases the {@code readerIndex} by the number of the
     * transferred bytes.  This method is basically same with
     * {@link #readBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code writerIndex} of the destination by the number of
     * the transferred bytes while {@link #readBytes(ByteBuf, int, int)}
     * does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.writableBytes} is greater than
     *            {@code this.readableBytes}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标缓冲区，从当前的 {@code readerIndex} 开始，直到目标缓冲区不可写为止，
     * 并将 {@code readerIndex} 增加传输的字节数。此方法基本上与
     * {@link #readBytes(ByteBuf, int, int)} 相同，不同之处在于此方法会将目标缓冲区的
     * {@code writerIndex} 增加传输的字节数，而 {@link #readBytes(ByteBuf, int, int)}
     * 不会。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code dst.writableBytes} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuf dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #readBytes(ByteBuf, int, int)},
     * except that this method increases the {@code writerIndex} of the
     * destination by the number of the transferred bytes (= {@code length})
     * while {@link #readBytes(ByteBuf, int, int)} does not.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes} or
     *         if {@code length} is greater than {@code dst.writableBytes}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标缓冲区，从当前的 {@code readerIndex} 开始，
     * 并将 {@code readerIndex} 增加传输的字节数（= {@code length}）。此方法
     * 与 {@link #readBytes(ByteBuf, int, int)} 基本相同，
     * 不同之处在于此方法会将目标的 {@code writerIndex} 增加传输的字节数（= {@code length}），
     * 而 {@link #readBytes(ByteBuf, int, int)} 不会。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes} 或
     *         如果 {@code length} 大于 {@code dst.writableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than
     *            {@code dst.capacity}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标，从当前的 {@code readerIndex} 开始，
     * 并增加 {@code readerIndex} 的值，增加的字节数为传输的字节数（即 {@code length}）。
     *
     * @param dstIndex 目标的起始索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code dstIndex} 小于 {@code 0}，
     *         如果 {@code length} 大于 {@code this.readableBytes}，或者
     *         如果 {@code dstIndex + length} 大于
     *            {@code dst.capacity}
     */
    public abstract ByteBuf readBytes(ByteBuf dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code dst.length}).
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.length} is greater than {@code this.readableBytes}
     */

    /**
     * 将此缓冲区的数据从当前的 {@code readerIndex} 开始传输到指定的目标，并增加 {@code readerIndex}
     * 传输的字节数（= {@code dst.length}）。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code dst.length} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes}, or
     *         if {@code dstIndex + length} is greater than {@code dst.length}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标，从当前的 {@code readerIndex} 开始，
     * 并将 {@code readerIndex} 增加传输的字节数（= {@code length}）。
     *
     * @param dstIndex 目标的第一个索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code dstIndex} 小于 {@code 0}，
     *         如果 {@code length} 大于 {@code this.readableBytes}，或者
     *         如果 {@code dstIndex + length} 大于 {@code dst.length}
     */
    public abstract ByteBuf readBytes(byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination's position
     * reaches its limit, and increases the {@code readerIndex} by the
     * number of the transferred bytes.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code dst.remaining()} is greater than
     *            {@code this.readableBytes}
     */

    /**
     * 将此缓冲区的数据传输到指定的目标，从当前的 {@code readerIndex} 开始，
     * 直到目标的位置达到其限制，并将 {@code readerIndex} 增加传输的字节数。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code dst.remaining()} 大于
     *            {@code this.readableBytes}
     */
    public abstract ByteBuf readBytes(ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified stream threw an exception during I/O
     */

    /**
     * 将此缓冲区的数据传输到指定的流，从当前的 {@code readerIndex} 开始。
     *
     * @param length 要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     * @throws IOException
     *         如果指定的流在 I/O 过程中抛出异常
     */
    public abstract ByteBuf readBytes(OutputStream out, int length) throws IOException;

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将此缓冲区的数据传输到指定的流，从当前的 {@code readerIndex} 开始。
     *
     * @param length 要传输的最大字节数
     *
     * @return 实际写入指定通道的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     * @throws IOException
     *         如果指定的通道在 I/O 期间抛出异常
     */
    public abstract int readBytes(GatheringByteChannel out, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the current {@code readerIndex}
     * and increases the {@code readerIndex} by the given length.
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 获取当前 {@code readerIndex} 处指定长度的 {@link CharSequence}，
     * 并将 {@code readerIndex} 增加指定长度。
     *
     * @param length 要读取的长度
     * @param charset 使用的字符集
     * @return 字符序列
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract CharSequence readCharSequence(int length, Charset charset);

    /**
     * Transfers this buffer's data starting at the current {@code readerIndex}
     * to the specified channel starting at the given file position.
     * This method does not modify the channel's position.
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes written out to the specified channel
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将此缓冲区中从当前 {@code readerIndex} 开始的数据传输到指定通道中从给定文件位置开始的位置。
     * 此方法不会修改通道的位置。
     *
     * @param position 传输开始的文件位置
     * @param length 要传输的最大字节数
     *
     * @return 实际写入指定通道的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     * @throws IOException
     *         如果指定通道在 I/O 期间抛出异常
     */
    public abstract int readBytes(FileChannel out, long position, int length) throws IOException;

    /**
     * Increases the current {@code readerIndex} by the specified
     * {@code length} in this buffer.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 将当前 {@code readerIndex} 增加指定的
     * {@code length}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract ByteBuf skipBytes(int length);

    /**
     * Sets the specified boolean at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的布尔值，
     * 并将 {@code writerIndex} 增加 {@code 1}。如果 {@code this.writableBytes} 小于 {@code 1}，
     * 将调用 {@link #ensureWritable(int)} 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeBoolean(boolean value);

    /**
     * Sets the specified byte at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * The 24 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的字节，
     * 并将 {@code writerIndex} 增加 {@code 1}。
     * 指定的值的高 24 位将被忽略。
     * 如果 {@code this.writableBytes} 小于 {@code 1}，
     * 将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeByte(int value);

    /**
     * Sets the specified 16-bit short integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 16 位短整型，并将 {@code writerIndex} 增加 {@code 2}。
     * 指定值的高 16 位将被忽略。
     * 如果 {@code this.writableBytes} 小于 {@code 2}，将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeShort(int value);

    /**
     * Sets the specified 16-bit short integer in the Little Endian Byte
     * Order at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 2} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 16 位短整数，采用小端字节序，
     * 并将 {@code writerIndex} 增加 {@code 2}。
     * 指定的值的高 16 位将被忽略。
     * 如果 {@code this.writableBytes} 小于 {@code 2}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeShortLE(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的24位中等整数，并将 {@code writerIndex} 增加 {@code 3}。
     * 如果 {@code this.writableBytes} 小于 {@code 3}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeMedium(int value);

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 3} in this
     * buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处以小端字节序设置指定的 24 位中等整数，
     * 并将此缓冲区中的 {@code writerIndex} 增加 {@code 3}。
     * 如果 {@code this.writableBytes} 小于 {@code 3}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeMediumLE(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 32 位整数，
     * 并将 {@code writerIndex} 增加 {@code 4}。如果 {@code this.writableBytes} 小于 {@code 4}，
     * 将调用 {@link #ensureWritable(int)} 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeInt(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * in the Little Endian Byte Order and increases the {@code writerIndex}
     * by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 以 Little Endian 字节顺序在当前 {@code writerIndex} 处设置指定的 32 位整数，
     * 并将 {@code writerIndex} 增加 {@code 4}。
     * 如果 {@code this.writableBytes} 小于 {@code 4}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeIntLE(int value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 64 位长整型，并将 {@code writerIndex} 增加 {@code 8}。
     * 如果 {@code this.writableBytes} 小于 {@code 8}，将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳。
     */
    public abstract ByteBuf writeLong(long value);

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处以 Little Endian 字节顺序设置指定的 64 位长整型，
     * 并将 {@code writerIndex} 增加 {@code 8}。
     * 如果 {@code this.writableBytes} 小于 {@code 8}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeLongLE(long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 2 字节 UTF-16 字符，并将 {@code writerIndex} 增加 {@code 2}。
     * 指定的值的高 16 位将被忽略。如果 {@code this.writableBytes} 小于 {@code 2}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来适应。
     */
    public abstract ByteBuf writeChar(int value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 4}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的32位浮点数，并将 {@code writerIndex} 增加 {@code 4}。
     * 如果 {@code this.writableBytes} 小于 {@code 4}，将会调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeFloat(float value);

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处以小端字节序设置指定的 32 位浮点数，
     * 并将 {@code writerIndex} 增加 {@code 4}。如果 {@code this.writableBytes}
     * 小于 {@code 4}，将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳数据。
     */
    public ByteBuf writeFloatLE(float value) {
        return writeIntLE(Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 在当前 {@code writerIndex} 处设置指定的 64 位浮点数，并将 {@code writerIndex} 增加 {@code 8}。
     * 如果 {@code this.writableBytes} 小于 {@code 8}，将调用 {@link #ensureWritable(int)} 以尝试扩展容量来容纳。
     */
    public abstract ByteBuf writeDouble(double value);

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} in Little Endian Byte Order and increases
     * the {@code writerIndex} by {@code 8} in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 以小端字节序在当前 {@code writerIndex} 处设置指定的 64 位浮点数，
     * 并将此缓冲区中的 {@code writerIndex} 增加 {@code 8}。
     * 如果 {@code this.writableBytes} 小于 {@code 8}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳。
     */
    public ByteBuf writeDoubleLE(double value) {
        return writeLongLE(Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(ByteBuf, int, int)}
     * does not.
     * If {@code this.writableBytes} is less than {@code src.readableBytes},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     */

    /**
     * 将指定源缓冲区的数据从当前 {@code writerIndex} 开始传输到此缓冲区，直到源缓冲区变得不可读，
     * 并增加 {@code writerIndex} 传输的字节数。此方法基本与
     * {@link #writeBytes(ByteBuf, int, int)} 相同，不同之处在于此方法会增加源缓冲区的
     * {@code readerIndex} 传输的字节数，而 {@link #writeBytes(ByteBuf, int, int)} 不会。
     * 如果 {@code this.writableBytes} 小于 {@code src.readableBytes}，
     * 将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳数据。
     */
    public abstract ByteBuf writeBytes(ByteBuf src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(ByteBuf, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(ByteBuf, int, int)} does not.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater then {@code src.readableBytes}
     */

    /**
     * 将指定源缓冲区的数据传输到此缓冲区，从当前的 {@code writerIndex} 开始，并将 {@code writerIndex} 增加传输的字节数（= {@code length}）。
     * 此方法与 {@link #writeBytes(ByteBuf, int, int)} 基本相同，区别在于此方法会将源缓冲区的 {@code readerIndex} 增加传输的字节数（= {@code length}），
     * 而 {@link #writeBytes(ByteBuf, int, int)} 不会。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)} 尝试扩展容量以容纳数据。
     *
     * @param length 要传输的字节数
     * @throws IndexOutOfBoundsException 如果 {@code length} 大于 {@code src.readableBytes}
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0}, or
     *         if {@code srcIndex + length} is greater than {@code src.capacity}
     */

    /**
     * 将指定源缓冲区的数据传输到此缓冲区，从当前的 {@code writerIndex} 开始，
     * 并将 {@code writerIndex} 增加传输的字节数（= {@code length}）。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量以容纳数据。
     *
     * @param srcIndex 源的起始索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code srcIndex} 小于 {@code 0}，或者
     *         如果 {@code srcIndex + length} 大于 {@code src.capacity}
     */
    public abstract ByteBuf writeBytes(ByteBuf src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code src.length}).
     * If {@code this.writableBytes} is less than {@code src.length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     */

    /**
     * 将指定源数组的数据传输到此缓冲区，从当前的 {@code writerIndex} 开始，
     * 并将 {@code writerIndex} 增加传输的字节数（= {@code src.length}）。
     * 如果 {@code this.writableBytes} 小于 {@code src.length}，将会调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳数据。
     */
    public abstract ByteBuf writeBytes(byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     *
     * @throws IndexOutOfBoundsException
     *         if the specified {@code srcIndex} is less than {@code 0}, or
     *         if {@code srcIndex + length} is greater than {@code src.length}
     */

    /**
     * 将指定源数组的数据传输到此缓冲区，从当前的 {@code writerIndex} 开始，并将 {@code writerIndex}
     * 增加传输的字节数（即 {@code length}）。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳数据。
     *
     * @param srcIndex 源的起始索引
     * @param length   要传输的字节数
     *
     * @throws IndexOutOfBoundsException
     *         如果指定的 {@code srcIndex} 小于 {@code 0}，或者
     *         如果 {@code srcIndex + length} 大于 {@code src.length}
     */
    public abstract ByteBuf writeBytes(byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code src.remaining()},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     */

    /**
     * 将指定源缓冲区的数据从当前 {@code writerIndex} 开始传输到此缓冲区，直到源缓冲区的位置达到其限制，
     * 并将 {@code writerIndex} 增加传输的字节数。
     * 如果 {@code this.writableBytes} 小于 {@code src.remaining()}，
     * 将调用 {@link #ensureWritable(int)} 以尝试扩展容量来容纳数据。
     */
    public abstract ByteBuf writeBytes(ByteBuffer src);

    /**
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified {@link InputStream} reached EOF.
     *
     * @throws IOException if the specified stream threw an exception during I/O
     */

    /**
     * 将指定流的内容传输到此缓冲区，从当前的 {@code writerIndex} 开始，
     * 并将 {@code writerIndex} 增加传输的字节数。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量来容纳数据。
     *
     * @param length 要传输的字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         {@code -1} 如果指定的 {@link InputStream} 到达了 EOF。
     *
     * @throws IOException 如果指定的流在 I/O 过程中抛出异常
     */
    public abstract int writeBytes(InputStream in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed or it reached EOF.
     *
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将指定通道的内容传输到此缓冲区，从当前的 {@code writerIndex} 开始，
     * 并将 {@code writerIndex} 增加传输的字节数。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)}
     * 以尝试扩展容量以容纳。
     *
     * @param length 要传输的最大字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         {@code -1} 如果指定通道已关闭或到达 EOF。
     *
     * @throws IOException
     *         如果指定通道在 I/O 期间抛出异常
     */
    public abstract int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    /**
     * Transfers the content of the specified channel starting at the given file position
     * to this buffer starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * This method does not modify the channel's position.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param position the file position at which the transfer is to begin
     * @param length the maximum number of bytes to transfer
     *
     * @return the actual number of bytes read in from the specified channel.
     *         {@code -1} if the specified channel is closed or it reached EOF.
     *
     * @throws IOException
     *         if the specified channel threw an exception during I/O
     */

    /**
     * 将指定通道的内容从给定的文件位置开始传输到此缓冲区，从当前的 {@code writerIndex} 开始，
     * 并将 {@code writerIndex} 增加传输的字节数。
     * 此方法不会修改通道的位置。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)} 
     * 以尝试扩展容量来容纳。
     *
     * @param position 传输开始的文件位置
     * @param length 要传输的最大字节数
     *
     * @return 从指定通道读取的实际字节数。
     *         如果指定通道已关闭或到达 EOF，则返回 {@code -1}。
     *
     * @throws IOException
     *         如果指定通道在 I/O 期间抛出异常
     */
    public abstract int writeBytes(FileChannel in, long position, int length) throws IOException;

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     */

    /**
     * 从当前的 {@code writerIndex} 开始，用 <tt>NUL (0x00)</tt> 填充此缓冲区，并将 {@code writerIndex} 增加指定的 {@code length}。
     * 如果 {@code this.writableBytes} 小于 {@code length}，将调用 {@link #ensureWritable(int)} 以尝试扩展容量来容纳。
     *
     * @param length 要写入缓冲区的 <tt>NUL</tt> 的数量
     */
    public abstract ByteBuf writeZero(int length);

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     * in this buffer.
     * If {@code this.writableBytes} is not large enough to write the whole sequence,
     * {@link #ensureWritable(int)} will be called in an attempt to expand capacity to accommodate.
     *
     * @param sequence to write
     * @param charset that should be used
     * @return the written number of bytes
     */

    /**
     * 在当前 {@code writerIndex} 处写入指定的 {@link CharSequence}，并将 {@code writerIndex} 增加写入的字节数。
     * 如果 {@code this.writableBytes} 不足以写入整个序列，将调用 {@link #ensureWritable(int)} 以尝试扩展容量以容纳。
     *
     * @param sequence 要写入的序列
     * @param charset 使用的字符集
     * @return 写入的字节数
     */
    public abstract int writeCharSequence(CharSequence sequence, Charset charset);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer. The search takes place from the specified {@code fromIndex}
     * (inclusive) to the specified {@code toIndex} (exclusive).
     * <p>
     * If {@code fromIndex} is greater than {@code toIndex}, the search is
     * performed in a reversed order from {@code fromIndex} (exclusive)
     * down to {@code toIndex} (inclusive).
     * <p>
     * Note that the lower index is always included and higher always excluded.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the absolute index of the first occurrence if found.
     *         {@code -1} otherwise.
     */

    /**
     * 定位此缓冲区中首次出现的指定 {@code value}。搜索从指定的 {@code fromIndex}
     * （包含）到指定的 {@code toIndex}（不包含）进行。
     * <p>
     * 如果 {@code fromIndex} 大于 {@code toIndex}，则搜索将以相反的顺序从 {@code fromIndex}
     * （不包含）向下到 {@code toIndex}（包含）进行。
     * <p>
     * 请注意，较低的索引始终包含在内，较高的索引始终不包含。
     * <p>
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @return 如果找到，则返回首次出现的绝对索引。
     *         否则返回 {@code -1}。
     */
    public abstract int indexOf(int fromIndex, int toIndex, byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the current {@code readerIndex}
     * (inclusive) to the current {@code writerIndex} (exclusive).
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     */

    /**
     * 定位指定 {@code value} 在此缓冲区中的首次出现位置。搜索从当前 {@code readerIndex}
     *（包含）到当前 {@code writerIndex}（不包含）之间进行。
     * <p>
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @return 当前 {@code readerIndex} 与首次出现位置之间的字节数（如果找到）。否则返回 {@code -1}。
     */
    public abstract int bytesBefore(byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the current {@code readerIndex}
     * (inclusive) and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the current {@code readerIndex}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code length} is greater than {@code this.readableBytes}
     */

    /**
     * 定位此缓冲区中第一次出现指定 {@code value} 的位置。搜索从当前 {@code readerIndex}
     * （包含）开始，持续指定的 {@code length}。
     * <p>
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @return 如果找到，返回当前 {@code readerIndex} 与第一次出现之间的字节数。否则返回 {@code -1}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code length} 大于 {@code this.readableBytes}
     */
    public abstract int bytesBefore(int length, byte value);

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the specified {@code index} (inclusive)
     * and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @return the number of bytes between the specified {@code index}
     *         and the first occurrence if found. {@code -1} otherwise.
     *
     * @throws IndexOutOfBoundsException
     *         if {@code index + length} is greater than {@code this.capacity}
     */

    /**
     * 定位此缓冲区中指定 {@code value} 的首次出现位置。搜索从指定的 {@code index}（包含）开始，持续指定的 {@code length}。
     * <p>
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @return 指定 {@code index} 与首次出现位置之间的字节数。如果未找到，则返回 {@code -1}。
     *
     * @throws IndexOutOfBoundsException
     *         如果 {@code index + length} 大于 {@code this.capacity}
     */
    public abstract int bytesBefore(int index, int length, byte value);

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in ascending order.
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the readable bytes.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以升序遍历此缓冲区的可读字节，并使用指定的 {@code processor} 进行处理。
     *
     * @return 如果处理器迭代到或超过可读字节的末尾，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public abstract int forEachByte(ByteProcessor processor);

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in ascending order.
     * (i.e. {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以升序顺序遍历此缓冲区的指定区域，并使用指定的 {@code processor} 进行处理。
     * (即 {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})
     *
     * @return 如果处理器遍历到或超过指定区域的末尾，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public abstract int forEachByte(int index, int length, ByteProcessor processor);

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in descending order.
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the readable bytes.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以降序方式遍历此缓冲区的可读字节，并使用指定的 {@code processor} 进行处理。
     *
     * @return 如果处理器迭代到或超过可读字节的开头，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public abstract int forEachByteDesc(ByteProcessor processor);

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in descending order.
     * (i.e. {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})
     *
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以降序顺序遍历此缓冲区的指定区域，并使用指定的 {@code processor} 进行处理。
     * (即 {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})
     *
     *
     * @return 如果处理器遍历到或超过指定区域的开始位置，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public abstract int forEachByteDesc(int index, int length, ByteProcessor processor);

    /**
     * Returns a copy of this buffer's readable bytes.  Modifying the content
     * of the returned buffer or this buffer does not affect each other at all.
     * This method is identical to {@code buf.copy(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */

    /**
     * 返回此缓冲区可读字节的副本。修改返回缓冲区或此缓冲区的内容不会相互影响。
     * 此方法等同于 {@code buf.copy(buf.readerIndex(), buf.readableBytes())}。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     */
    public abstract ByteBuf copy();

    /**
     * Returns a copy of this buffer's sub-region.  Modifying the content of
     * the returned buffer or this buffer does not affect each other at all.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */

    /**
     * 返回此缓冲区的子区域的副本。修改返回缓冲区或此缓冲区的内容不会相互影响。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     */
    public abstract ByteBuf copy(int index, int length);

    /**
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     */

    /**
     * 返回此缓冲区的可读字节的切片。修改返回的缓冲区或此缓冲区的内容会相互影响，
     * 尽管它们保持独立的索引和标记。此方法与 {@code buf.slice(buf.readerIndex(), buf.readableBytes())} 相同。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 还需注意，此方法不会调用 {@link #retain()}，因此引用计数不会增加。
     */
    public abstract ByteBuf slice();

    /**
     * Returns a retained slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice()}.
     * This method behaves similarly to {@code slice().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */

    /**
     * 返回此缓冲区的可读字节的一个保留切片。修改返回的缓冲区或此缓冲区的内容会相互影响，
     * 同时它们保持独立的索引和标记。此方法与 {@code buf.slice(buf.readerIndex(), buf.readableBytes())} 相同。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 注意，与 {@link #slice()} 不同，此方法返回一个 {@linkplain #retain() 保留的} 缓冲区。
     * 此方法与 {@code slice().retain()} 的行为类似，但此方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedSlice();

    /**
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     */

    /**
     * 返回此缓冲区的子区域切片。修改返回的缓冲区或此缓冲区的内容会相互影响，而它们保持独立的索引和标记。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 还需注意，此方法不会调用 {@link #retain()}，因此引用计数不会增加。
     */
    public abstract ByteBuf slice(int index, int length);

    /**
     * Returns a retained slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code slice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */

    /**
     * 返回此缓冲区子区域的保留切片。修改返回缓冲区或此缓冲区的内容会相互影响，同时它们保持独立的索引和标记。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 请注意，与 {@link #slice(int, int)} 不同，此方法返回一个 {@linkplain #retain() 保留的} 缓冲区。
     * 此方法的行为类似于 {@code slice(...).retain()}，但此方法可能返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedSlice(int index, int length);

    /**
     * Returns a buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * The reader and writer marks will not be duplicated. Also be aware that this method will
     * NOT call {@link #retain()} and so the reference count will NOT be increased.
     * @return A buffer whose readable content is equivalent to the buffer returned by {@link #slice()}.
     * However this buffer will share the capacity of the underlying buffer, and therefore allows access to all of the
     * underlying content if necessary.
     */

    /**
     * 返回一个与此缓冲区共享整个区域的缓冲区。
     * 修改返回缓冲区或此缓冲区的内容会相互影响，但它们保持独立的索引和标记。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 读取和写入标记不会被复制。请注意，此方法不会调用 {@link #retain()}，因此引用计数不会增加。
     * @return 一个缓冲区，其可读内容等同于 {@link #slice()} 返回的缓冲区。
     * 但是，此缓冲区将共享底层缓冲区的容量，因此允许在必要时访问所有底层内容。
     */
    public abstract ByteBuf duplicate();

    /**
     * Returns a retained buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method is identical to {@code buf.slice(0, buf.capacity())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code duplicate().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */

    /**
     * 返回一个保留的缓冲区，该缓冲区共享此缓冲区的整个区域。
     * 修改返回的缓冲区或此缓冲区的内容会相互影响，同时它们保持独立的索引和标记。
     * 此方法与 {@code buf.slice(0, buf.capacity())} 相同。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * <p>
     * 注意，与 {@link #slice(int, int)} 不同，此方法返回一个 {@linkplain #retain() 保留} 的缓冲区。
     * 此方法与 {@code duplicate().retain()} 的行为类似，但此方法可能会返回一个产生较少垃圾的缓冲区实现。
     */
    public abstract ByteBuf retainedDuplicate();

    /**
     * Returns the maximum number of NIO {@link ByteBuffer}s that consist this buffer.  Note that {@link #nioBuffers()}
     * or {@link #nioBuffers(int, int)} might return a less number of {@link ByteBuffer}s.
     *
     * @return {@code -1} if this buffer has no underlying {@link ByteBuffer}.
     *         the number of the underlying {@link ByteBuffer}s if this buffer has at least one underlying
     *         {@link ByteBuffer}.  Note that this method does not return {@code 0} to avoid confusion.
     *
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */

    /**
     * 返回构成此缓冲区的NIO {@link ByteBuffer}的最大数量。请注意，{@link #nioBuffers()} 或 {@link #nioBuffers(int, int)} 
     * 可能会返回较少的 {@link ByteBuffer}。
     *
     * @return 如果此缓冲区没有底层的 {@link ByteBuffer}，则返回 {@code -1}。
     *         如果此缓冲区至少有一个底层的 {@link ByteBuffer}，则返回底层 {@link ByteBuffer} 的数量。
     *         请注意，此方法不会返回 {@code 0} 以避免混淆。
     *
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract int nioBufferCount();

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method is identical to {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */

    /**
     * 将此缓冲区的可读字节作为NIO {@link ByteBuffer}暴露。返回的缓冲区要么共享，要么包含此缓冲区内容的副本，
     * 而改变返回的NIO缓冲区的位置和限制不会影响此缓冲区的索引和标记。
     * 此方法与 {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())} 相同。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * 请注意，如果此缓冲区是动态缓冲区并且调整了其容量，则返回的NIO缓冲区将不会看到此缓冲区的更改。
     *
     * @throws UnsupportedOperationException
     *         如果此缓冲区无法创建一个与其内容共享的 {@link ByteBuffer}
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer();

    /**
     * Exposes this buffer's sub-region as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */

    /**
     * 将此缓冲区的子区域作为 NIO {@link ByteBuffer} 暴露。返回的缓冲区要么共享要么包含此缓冲区的内容副本，而更改返回的 NIO 缓冲区的位置和限制不会影响此缓冲区的索引和标记。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * 请注意，如果此缓冲区是动态缓冲区并且调整了其容量，则返回的 NIO 缓冲区将不会看到此缓冲区的更改。
     *
     * @throws UnsupportedOperationException
     *         如果此缓冲区无法创建与其内容共享的 {@link ByteBuffer}
     *
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer(int index, int length);

    /**
     * Internal use only: Exposes the internal NIO buffer.
     */

    /**
     * 仅限内部使用：暴露内部的NIO缓冲区。
     */
    public abstract ByteBuffer internalNioBuffer(int index, int length);

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}'s. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */

    /**
     * 将此缓冲区的可读字节作为 NIO {@link ByteBuffer} 暴露。返回的缓冲区要么共享，要么包含此缓冲区内容的副本，
     * 而更改返回的 NIO 缓冲区的位置和限制不会影响此缓冲区的索引和标记。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     * 请注意，如果此缓冲区是动态缓冲区并且调整了其容量，则返回的 NIO 缓冲区将不会看到此缓冲区的更改。
     *
     *
     * @throws UnsupportedOperationException
     *         如果此缓冲区无法创建与其自身内容共享的 {@link ByteBuffer}
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    public abstract ByteBuffer[] nioBuffers();

    /**
     * Exposes this buffer's bytes as an NIO {@link ByteBuffer}'s for the specified index and length
     * The returned buffer either share or contains the copied content of this buffer, while changing
     * the position and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer. Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException
     *         if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */

    /**
     * 将此缓冲区的字节作为NIO {@link ByteBuffer}暴露，用于指定的索引和长度
     * 返回的缓冲区要么共享，要么包含此缓冲区的内容副本，而更改返回的NIO缓冲区的位置和限制不会影响此缓冲区的索引和标记。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。请注意，如果此缓冲区是动态缓冲区并且调整了其容量，则返回的NIO缓冲区将不会看到此缓冲区的更改。
     *
     * @throws UnsupportedOperationException
     *         如果此缓冲区无法创建与其内容共享的 {@link ByteBuffer}
     *
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    public abstract ByteBuffer[] nioBuffers(int index, int length);

    /**
     * Returns {@code true} if and only if this buffer has a backing byte array.
     * If this method returns true, you can safely call {@link #array()} and
     * {@link #arrayOffset()}.
     */

    /**
     * 当且仅当此缓冲区具有后备字节数组时返回 {@code true}。
     * 如果此方法返回 true，你可以安全地调用 {@link #array()} 和
     * {@link #arrayOffset()}。
     */
    public abstract boolean hasArray();

    /**
     * Returns the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */

    /**
     * 返回此缓冲区的底层字节数组。
     *
     * @throws UnsupportedOperationException
     *         如果没有可访问的底层字节数组
     */
    public abstract byte[] array();

    /**
     * Returns the offset of the first byte within the backing byte array of
     * this buffer.
     *
     * @throws UnsupportedOperationException
     *         if there no accessible backing byte array
     */

    /**
     * 返回此缓冲区的后备字节数组中第一个字节的偏移量。
     *
     * @throws UnsupportedOperationException
     *         如果没有可访问的后备字节数组
     */
    public abstract int arrayOffset();

    /**
     * Returns {@code true} if and only if this buffer has a reference to the low-level memory address that points
     * to the backing data.
     */

    /**
     * 当且仅当此缓冲区具有指向底层数据的低级内存地址的引用时，返回 {@code true}。
     */
    public abstract boolean hasMemoryAddress();

    /**
     * Returns the low-level memory address that point to the first byte of ths backing data.
     *
     * @throws UnsupportedOperationException
     *         if this buffer does not support accessing the low-level memory address
     */

    /**
     * 返回指向此后备数据第一个字节的低级内存地址。
     *
     * @throws UnsupportedOperationException
     *         如果此缓冲区不支持访问低级内存地址
     */
    public abstract long memoryAddress();

    /**
     * Returns {@code true} if this {@link ByteBuf} implementation is backed by a single memory region.
     * Composite buffer implementations must return false even if they currently hold &le; 1 components.
     * For buffers that return {@code true}, it's guaranteed that a successful call to {@link #discardReadBytes()}
     * will increase the value of {@link #maxFastWritableBytes()} by the current {@code readerIndex}.
     * <p>
     * This method will return {@code false} by default, and a {@code false} return value does not necessarily
     * mean that the implementation is composite or that it is <i>not</i> backed by a single memory region.
     */

    /**
     * 如果此 {@link ByteBuf} 实现由单个内存区域支持，则返回 {@code true}。
     * 即使当前持有 ≤ 1 个组件，组合缓冲区实现也必须返回 false。
     * 对于返回 {@code true} 的缓冲区，保证成功调用 {@link #discardReadBytes()} 将根据当前 {@code readerIndex} 的值增加 {@link #maxFastWritableBytes()}。
     * <p>
     * 此方法默认返回 {@code false}，并且返回 {@code false} 并不一定意味着该实现是组合的或它<i>不</i>由单个内存区域支持。
     */
    public boolean isContiguous() {
        return false;
    }

    /**
     * A {@code ByteBuf} can turn into itself.
     * @return This {@code ByteBuf} instance.
     */

    /**
     * 一个 {@code ByteBuf} 可以转换为其自身。
     * @return 此 {@code ByteBuf} 实例。
     */
    @Override
    public ByteBuf asByteBuf() {
        return this;
    }

    /**
     * Decodes this buffer's readable bytes into a string with the specified
     * character set name.  This method is identical to
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws UnsupportedCharsetException
     *         if the specified character set name is not supported by the
     *         current VM
     */

    /**
     * 将此缓冲区的可读字节解码为具有指定字符集名称的字符串。此方法与
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)} 相同。
     * 此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     *
     * @throws UnsupportedCharsetException
     *         如果当前虚拟机不支持指定的字符集名称
     */
    public abstract String toString(Charset charset);

    /**
     * Decodes this buffer's sub-region into a string with the specified
     * character set.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     */

    /**
     * 将此缓冲区的子区域解码为具有指定字符集的字符串。此方法不会修改此缓冲区的 {@code readerIndex} 或 {@code writerIndex}。
     */
    public abstract String toString(int index, int length, Charset charset);

    /**
     * Returns a hash code which was calculated from the content of this
     * buffer.  If there's a byte array which is
     * {@linkplain #equals(Object) equal to} this array, both arrays should
     * return the same value.
     */

    /**
     * 返回从此缓冲区内容计算出的哈希码。如果存在一个与此缓冲区
     * {@linkplain #equals(Object) 相等}的字节数组，两个数组应返回相同的值。
     */
    @Override
    public abstract int hashCode();

    /**
     * Determines if the content of the specified buffer is identical to the
     * content of this array.  'Identical' here means:
     * <ul>
     * <li>the size of the contents of the two buffers are same and</li>
     * <li>every single byte of the content of the two buffers are same.</li>
     * </ul>
     * Please note that it does not compare {@link #readerIndex()} nor
     * {@link #writerIndex()}.  This method also returns {@code false} for
     * {@code null} and an object which is not an instance of
     * {@link ByteBuf} type.
     */

    /**
     * 确定指定缓冲区的内容是否与此数组的内容相同。'相同'在这里意味着：
     * <ul>
     * <li>两个缓冲区内容的大小相同，且</li>
     * <li>两个缓冲区内容的每一个字节都相同。</li>
     * </ul>
     * 请注意，它不比较 {@link #readerIndex()} 也不比较 {@link #writerIndex()}。
     * 对于 {@code null} 和非 {@link ByteBuf} 类型的对象，此方法也返回 {@code false}。
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Compares the content of the specified buffer to the content of this
     * buffer. Comparison is performed in the same manner with the string
     * comparison functions of various languages such as {@code strcmp},
     * {@code memcmp} and {@link String#compareTo(String)}.
     */

    /**
     * 将指定缓冲区的内容与此缓冲区的内容进行比较。比较方式与各种语言中的字符串比较函数相同，
     * 例如 {@code strcmp}、{@code memcmp} 和 {@link String#compareTo(String)}。
     */
    @Override
    public abstract int compareTo(ByteBuf buffer);

    /**
     * Returns the string representation of this buffer.  This method does not
     * necessarily return the whole content of the buffer but returns
     * the values of the key properties such as {@link #readerIndex()},
     * {@link #writerIndex()} and {@link #capacity()}.
     */

    /**
     * 返回此缓冲区的字符串表示形式。此方法不一定返回缓冲区的全部内容，
     * 而是返回关键属性的值，例如 {@link #readerIndex()}、
     * {@link #writerIndex()} 和 {@link #capacity()}。
     */
    @Override
    public abstract String toString();

    @Override
    public abstract ByteBuf retain(int increment);

    @Override
    public abstract ByteBuf retain();

    @Override
    public abstract ByteBuf touch();

    @Override
    public abstract ByteBuf touch(Object hint);

    /**
     * Used internally by {@link AbstractByteBuf#ensureAccessible()} to try to guard
     * against using the buffer after it was released (best-effort).
     */

    /**
     * 由 {@link AbstractByteBuf#ensureAccessible()} 内部使用，尝试防止在缓冲区被释放后继续使用（尽力而为）。
     */
    boolean isAccessible() {
        return refCnt() != 0;
    }
}
