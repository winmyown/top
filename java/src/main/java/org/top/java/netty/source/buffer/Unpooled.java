
package org.top.java.netty.source.buffer;

import org.top.java.netty.source.buffer.CompositeByteBuf.ByteWrapper;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;


/**
 * Creates a new {@link ByteBuf} by allocating new space or by wrapping
 * or copying existing byte arrays, byte buffers and a string.
 *
 * <h3>Use static import</h3>
 * This classes is intended to be used with Java 5 static import statement:
 *
 * <pre>
 * import static io.netty.buffer.{@link Unpooled}.*;
 *
 * {@link ByteBuf} heapBuffer    = buffer(128);
 * {@link ByteBuf} directBuffer  = directBuffer(256);
 * {@link ByteBuf} wrappedBuffer = wrappedBuffer(new byte[128], new byte[256]);
 * {@link ByteBuf} copiedBuffer  = copiedBuffer({@link ByteBuffer}.allocate(128));
 * </pre>
 *
 * <h3>Allocating a new buffer</h3>
 *
 * Three buffer types are provided out of the box.
 *
 * <ul>
 * <li>{@link #buffer(int)} allocates a new fixed-capacity heap buffer.</li>
 * <li>{@link #directBuffer(int)} allocates a new fixed-capacity direct buffer.</li>
 * </ul>
 *
 * <h3>Creating a wrapped buffer</h3>
 *
 * Wrapped buffer is a buffer which is a view of one or more existing
 * byte arrays and byte buffers.  Any changes in the content of the original
 * array or buffer will be visible in the wrapped buffer.  Various wrapper
 * methods are provided and their name is all {@code wrappedBuffer()}.
 * You might want to take a look at the methods that accept varargs closely if
 * you want to create a buffer which is composed of more than one array to
 * reduce the number of memory copy.
 *
 * <h3>Creating a copied buffer</h3>
 *
 * Copied buffer is a deep copy of one or more existing byte arrays, byte
 * buffers or a string.  Unlike a wrapped buffer, there's no shared data
 * between the original data and the copied buffer.  Various copy methods are
 * provided and their name is all {@code copiedBuffer()}.  It is also convenient
 * to use this operation to merge multiple buffers into one buffer.
 */


/**
 * 通过分配新空间或通过包装或复制现有的字节数组、字节缓冲区和字符串来创建一个新的 {@link ByteBuf}。
 *
 * <h3>使用静态导入</h3>
 * 该类旨在与 Java 5 的静态导入语句一起使用：
 *
 * <pre>
 * import static io.netty.buffer.{@link Unpooled}.*;
 *
 * {@link ByteBuf} heapBuffer    = buffer(128);
 * {@link ByteBuf} directBuffer  = directBuffer(256);
 * {@link ByteBuf} wrappedBuffer = wrappedBuffer(new byte[128], new byte[256]);
 * {@link ByteBuf} copiedBuffer  = copiedBuffer({@link ByteBuffer}.allocate(128));
 * </pre>
 *
 * <h3>分配新缓冲区</h3>
 *
 * 提供了三种缓冲区类型。
 *
 * <ul>
 * <li>{@link #buffer(int)} 分配一个新的固定容量的堆缓冲区。</li>
 * <li>{@link #directBuffer(int)} 分配一个新的固定容量的直接缓冲区。</li>
 * </ul>
 *
 * <h3>创建包装缓冲区</h3>
 *
 * 包装缓冲区是一个或多个现有字节数组和字节缓冲区的视图。原始数组或缓冲区中的任何更改都将在包装缓冲区中可见。提供了各种包装方法，它们的名称都是 {@code wrappedBuffer()}。
 * 如果您想创建一个由多个数组组成的缓冲区以减少内存复制的次数，您可能需要仔细查看接受可变参数的方法。
 *
 * <h3>创建复制缓冲区</h3>
 *
 * 复制缓冲区是一个或多个现有字节数组、字节缓冲区或字符串的深拷贝。与包装缓冲区不同，原始数据和复制缓冲区之间没有共享数据。提供了各种复制方法，它们的名称都是 {@code copiedBuffer()}。
 * 使用此操作将多个缓冲区合并为一个缓冲区也非常方便。
 */
public final class Unpooled {

    private static final ByteBufAllocator ALLOC = UnpooledByteBufAllocator.DEFAULT;

    /**
     * Big endian byte order.
     */

    /**
     * 大端字节序。
     */
    public static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

    /**
     * Little endian byte order.
     */

    /**
     * 小端字节序。
     */
    public static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;

    /**
     * A buffer whose capacity is {@code 0}.
     */

    /**
     * 容量为 {@code 0} 的缓冲区。
     */
    @SuppressWarnings("checkstyle:StaticFinalBuffer")  // EmptyByteBuf is not writeable or readable.
    public static final ByteBuf EMPTY_BUFFER = ALLOC.buffer(0, 0);

    static {
        assert EMPTY_BUFFER instanceof EmptyByteBuf : "EMPTY_BUFFER must be an EmptyByteBuf.";
    }

    /**
     * Creates a new big-endian Java heap buffer with reasonably small initial capacity, which
     * expands its capacity boundlessly on demand.
     */

    /**
     * 创建一个具有合理较小初始容量的大端Java堆缓冲区，该缓冲区在需要时可以无限扩展其容量。
     */
    public static ByteBuf buffer() {
        return ALLOC.heapBuffer();
    }

    /**
     * Creates a new big-endian direct buffer with reasonably small initial capacity, which
     * expands its capacity boundlessly on demand.
     */

    /**
     * 创建一个具有合理初始容量的新大端直接缓冲区，该缓冲区在需要时可以无限扩展其容量。
     */
    public static ByteBuf directBuffer() {
        return ALLOC.directBuffer();
    }

    /**
     * Creates a new big-endian Java heap buffer with the specified {@code capacity}, which
     * expands its capacity boundlessly on demand.  The new buffer's {@code readerIndex} and
     * {@code writerIndex} are {@code 0}.
     */

    /**
     * 创建一个具有指定 {@code capacity} 的大端 Java 堆缓冲区，该缓冲区根据需要无限扩展其容量。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 均为 {@code 0}。
     */
    public static ByteBuf buffer(int initialCapacity) {
        return ALLOC.heapBuffer(initialCapacity);
    }

    /**
     * Creates a new big-endian direct buffer with the specified {@code capacity}, which
     * expands its capacity boundlessly on demand.  The new buffer's {@code readerIndex} and
     * {@code writerIndex} are {@code 0}.
     */

    /**
     * 创建一个具有指定 {@code capacity} 的新的大端直接缓冲区，该缓冲区根据需要无限扩展其容量。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 均为 {@code 0}。
     */
    public static ByteBuf directBuffer(int initialCapacity) {
        return ALLOC.directBuffer(initialCapacity);
    }

    /**
     * Creates a new big-endian Java heap buffer with the specified
     * {@code initialCapacity}, that may grow up to {@code maxCapacity}
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0}.
     */

    /**
     * 创建一个指定初始容量 {@code initialCapacity} 的大端 Java 堆缓冲区，
     * 该缓冲区可以增长到 {@code maxCapacity}。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 均为 {@code 0}。
     */
    public static ByteBuf buffer(int initialCapacity, int maxCapacity) {
        return ALLOC.heapBuffer(initialCapacity, maxCapacity);
    }

    /**
     * Creates a new big-endian direct buffer with the specified
     * {@code initialCapacity}, that may grow up to {@code maxCapacity}.
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0}.
     */

    /**
     * 创建一个具有指定 {@code initialCapacity} 的大端直接缓冲区，
     * 该缓冲区可以增长到 {@code maxCapacity}。新缓冲区的
     * {@code readerIndex} 和 {@code writerIndex} 均为 {@code 0}。
     */
    public static ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
        return ALLOC.directBuffer(initialCapacity, maxCapacity);
    }

    /**
     * Creates a new big-endian buffer which wraps the specified {@code array}.
     * A modification on the specified array's content will be visible to the
     * returned buffer.
     */

    /**
     * 创建一个新的大端序缓冲区，该缓冲区包装指定的 {@code array}。
     * 对指定数组内容的修改将在返回的缓冲区中可见。
     */
    public static ByteBuf wrappedBuffer(byte[] array) {
        if (array.length == 0) {
            return EMPTY_BUFFER;
        }
        return new UnpooledHeapByteBuf(ALLOC, array, array.length);
    }

    /**
     * Creates a new big-endian buffer which wraps the sub-region of the
     * specified {@code array}.  A modification on the specified array's
     * content will be visible to the returned buffer.
     */

    /**
     * 创建一个新的大端序缓冲区，该缓冲区包装指定 {@code array} 的子区域。
     * 对指定数组内容的修改将在返回的缓冲区中可见。
     */
    public static ByteBuf wrappedBuffer(byte[] array, int offset, int length) {
        if (length == 0) {
            return EMPTY_BUFFER;
        }

        if (offset == 0 && length == array.length) {
            return wrappedBuffer(array);
        }

        return wrappedBuffer(array).slice(offset, length);
    }

    /**
     * Creates a new buffer which wraps the specified NIO buffer's current
     * slice.  A modification on the specified buffer's content will be
     * visible to the returned buffer.
     */

    /**
     * 创建一个新的缓冲区，该缓冲区包装指定NIO缓冲区的当前切片。
     * 对指定缓冲区内容的修改将在返回的缓冲区中可见。
     */
    public static ByteBuf wrappedBuffer(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return EMPTY_BUFFER;
        }
        if (!buffer.isDirect() && buffer.hasArray()) {
            return wrappedBuffer(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining()).order(buffer.order());
        } else if (PlatformDependent.hasUnsafe()) {
            if (buffer.isReadOnly()) {
                if (buffer.isDirect()) {
                    return new ReadOnlyUnsafeDirectByteBuf(ALLOC, buffer);
                } else {
                    return new ReadOnlyByteBufferBuf(ALLOC, buffer);
                }
            } else {
                return new UnpooledUnsafeDirectByteBuf(ALLOC, buffer, buffer.remaining());
            }
        } else {
            if (buffer.isReadOnly()) {
                return new ReadOnlyByteBufferBuf(ALLOC, buffer);
            }  else {
                return new UnpooledDirectByteBuf(ALLOC, buffer, buffer.remaining());
            }
        }
    }

    /**
     * Creates a new buffer which wraps the specified memory address. If {@code doFree} is true the
     * memoryAddress will automatically be freed once the reference count of the {@link ByteBuf} reaches {@code 0}.
     */

    /**
     * 创建一个新的缓冲区，该缓冲区包装指定的内存地址。如果 {@code doFree} 为 true，则当 {@link ByteBuf} 的引用计数达到 {@code 0} 时，内存地址将自动释放。
     */
    public static ByteBuf wrappedBuffer(long memoryAddress, int size, boolean doFree) {
        return new WrappedUnpooledUnsafeDirectByteBuf(ALLOC, memoryAddress, size, doFree);
    }

    /**
     * Creates a new buffer which wraps the specified buffer's readable bytes.
     * A modification on the specified buffer's content will be visible to the
     * returned buffer.
     * @param buffer The buffer to wrap. Reference count ownership of this variable is transferred to this method.
     * @return The readable portion of the {@code buffer}, or an empty buffer if there is no readable portion.
     * The caller is responsible for releasing this buffer.
     */

    /**
     * 创建一个新的缓冲区，该缓冲区包装指定缓冲区的可读字节。
     * 对指定缓冲区内容的修改将在返回的缓冲区中可见。
     * @param buffer 要包装的缓冲区。此变量的引用计数所有权转移给此方法。
     * @return {@code buffer} 的可读部分，如果没有可读部分，则返回空缓冲区。
     * 调用者负责释放此缓冲区。
     */
    public static ByteBuf wrappedBuffer(ByteBuf buffer) {
        if (buffer.isReadable()) {
            return buffer.slice();
        } else {
            buffer.release();
            return EMPTY_BUFFER;
        }
    }

    /**
     * Creates a new big-endian composite buffer which wraps the specified
     * arrays without copying them.  A modification on the specified arrays'
     * content will be visible to the returned buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定的数组而不复制它们。
     * 对指定数组内容的修改将在返回的缓冲区中可见。
     */
    public static ByteBuf wrappedBuffer(byte[]... arrays) {
        return wrappedBuffer(arrays.length, arrays);
    }

    /**
     * Creates a new big-endian composite buffer which wraps the readable bytes of the
     * specified buffers without copying them.  A modification on the content
     * of the specified buffers will be visible to the returned buffer.
     * @param buffers The buffers to wrap. Reference count ownership of all variables is transferred to this method.
     * @return The readable portion of the {@code buffers}. The caller is responsible for releasing this buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定缓冲区的可读字节而不复制它们。对指定缓冲区内容的修改将在返回的缓冲区中可见。
     * @param buffers 要包装的缓冲区。所有变量的引用计数所有权将转移给此方法。
     * @return {@code buffers} 的可读部分。调用者负责释放此缓冲区。
     */
    public static ByteBuf wrappedBuffer(ByteBuf... buffers) {
        return wrappedBuffer(buffers.length, buffers);
    }

    /**
     * Creates a new big-endian composite buffer which wraps the slices of the specified
     * NIO buffers without copying them.  A modification on the content of the
     * specified buffers will be visible to the returned buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定NIO缓冲区的切片而不复制它们。
     * 对指定缓冲区内容的修改将对返回的缓冲区可见。
     */
    public static ByteBuf wrappedBuffer(ByteBuffer... buffers) {
        return wrappedBuffer(buffers.length, buffers);
    }

    static <T> ByteBuf wrappedBuffer(int maxNumComponents, ByteWrapper<T> wrapper, T[] array) {
        switch (array.length) {
        case 0:
            break;
        case 1:
            if (!wrapper.isEmpty(array[0])) {
                return wrapper.wrap(array[0]);
            }
            break;
        default:
            for (int i = 0, len = array.length; i < len; i++) {
                T bytes = array[i];
                if (bytes == null) {
                    return EMPTY_BUFFER;
                }
                if (!wrapper.isEmpty(bytes)) {
                    return new CompositeByteBuf(ALLOC, false, maxNumComponents, wrapper, array, i);
                }
            }
        }

        return EMPTY_BUFFER;
    }

    /**
     * Creates a new big-endian composite buffer which wraps the specified
     * arrays without copying them.  A modification on the specified arrays'
     * content will be visible to the returned buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定的数组而不复制它们。
     * 对指定数组内容的修改将在返回的缓冲区中可见。
     */
    public static ByteBuf wrappedBuffer(int maxNumComponents, byte[]... arrays) {
        return wrappedBuffer(maxNumComponents, CompositeByteBuf.BYTE_ARRAY_WRAPPER, arrays);
    }

    /**
     * Creates a new big-endian composite buffer which wraps the readable bytes of the
     * specified buffers without copying them.  A modification on the content
     * of the specified buffers will be visible to the returned buffer.
     * @param maxNumComponents Advisement as to how many independent buffers are allowed to exist before
     * consolidation occurs.
     * @param buffers The buffers to wrap. Reference count ownership of all variables is transferred to this method.
     * @return The readable portion of the {@code buffers}. The caller is responsible for releasing this buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定缓冲区的可读字节而不复制它们。
     * 对指定缓冲区内容的修改将对返回的缓冲区可见。
     * @param maxNumComponents 建议在合并发生之前允许存在的独立缓冲区的数量。
     * @param buffers 要包装的缓冲区。所有变量的引用计数所有权将转移给此方法。
     * @return {@code buffers} 的可读部分。调用者负责释放此缓冲区。
     */
    public static ByteBuf wrappedBuffer(int maxNumComponents, ByteBuf... buffers) {
        switch (buffers.length) {
        case 0:
            break;
        case 1:
            ByteBuf buffer = buffers[0];
            if (buffer.isReadable()) {
                return wrappedBuffer(buffer.order(BIG_ENDIAN));
            } else {
                buffer.release();
            }
            break;
        default:
            for (int i = 0; i < buffers.length; i++) {
                ByteBuf buf = buffers[i];
                if (buf.isReadable()) {
                    return new CompositeByteBuf(ALLOC, false, maxNumComponents, buffers, i);
                }
                buf.release();
            }
            break;
        }
        return EMPTY_BUFFER;
    }

    /**
     * Creates a new big-endian composite buffer which wraps the slices of the specified
     * NIO buffers without copying them.  A modification on the content of the
     * specified buffers will be visible to the returned buffer.
     */

    /**
     * 创建一个新的大端序复合缓冲区，该缓冲区包装指定NIO缓冲区的切片而不复制它们。
     * 对指定缓冲区内容的修改将对返回的缓冲区可见。
     */
    public static ByteBuf wrappedBuffer(int maxNumComponents, ByteBuffer... buffers) {
        return wrappedBuffer(maxNumComponents, CompositeByteBuf.BYTE_BUFFER_WRAPPER, buffers);
    }

    /**
     * Returns a new big-endian composite buffer with no components.
     */

    /**
     * 返回一个没有组件的大端复合缓冲区。
     */
    public static CompositeByteBuf compositeBuffer() {
        return compositeBuffer(AbstractByteBufAllocator.DEFAULT_MAX_COMPONENTS);
    }

    /**
     * Returns a new big-endian composite buffer with no components.
     */

    /**
     * 返回一个没有组件的大端复合缓冲区。
     */
    public static CompositeByteBuf compositeBuffer(int maxNumComponents) {
        return new CompositeByteBuf(ALLOC, false, maxNumComponents);
    }

    /**
     * Creates a new big-endian buffer whose content is a copy of the
     * specified {@code array}.  The new buffer's {@code readerIndex} and
     * {@code writerIndex} are {@code 0} and {@code array.length} respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定 {@code array} 的副本。新缓冲区的 {@code readerIndex} 和
     * {@code writerIndex} 分别为 {@code 0} 和 {@code array.length}。
     */
    public static ByteBuf copiedBuffer(byte[] array) {
        if (array.length == 0) {
            return EMPTY_BUFFER;
        }
        return wrappedBuffer(array.clone());
    }

    /**
     * Creates a new big-endian buffer whose content is a copy of the
     * specified {@code array}'s sub-region.  The new buffer's
     * {@code readerIndex} and {@code writerIndex} are {@code 0} and
     * the specified {@code length} respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定 {@code array} 的子区域的副本。新缓冲区的
     * {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和指定的 {@code length}。
     */
    public static ByteBuf copiedBuffer(byte[] array, int offset, int length) {
        if (length == 0) {
            return EMPTY_BUFFER;
        }
        byte[] copy = PlatformDependent.allocateUninitializedArray(length);
        System.arraycopy(array, offset, copy, 0, length);
        return wrappedBuffer(copy);
    }

    /**
     * Creates a new buffer whose content is a copy of the specified
     * {@code buffer}'s current slice.  The new buffer's {@code readerIndex}
     * and {@code writerIndex} are {@code 0} and {@code buffer.remaining}
     * respectively.
     */

    /**
     * 创建一个新缓冲区，其内容是指定 {@code buffer} 当前切片的一个副本。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和 {@code buffer.remaining}。
     */
    public static ByteBuf copiedBuffer(ByteBuffer buffer) {
        int length = buffer.remaining();
        if (length == 0) {
            return EMPTY_BUFFER;
        }
        byte[] copy = PlatformDependent.allocateUninitializedArray(length);
        // Duplicate the buffer so we not adjust the position during our get operation.
        // 复制缓冲区，以便在获取操作期间不调整位置。
        // See https://github.com/netty/netty/issues/3896

// 参见 https://github.com/netty/netty/issues/3896

        ByteBuffer duplicate = buffer.duplicate();
        duplicate.get(copy);
        return wrappedBuffer(copy).order(duplicate.order());
    }

    /**
     * Creates a new buffer whose content is a copy of the specified
     * {@code buffer}'s readable bytes.  The new buffer's {@code readerIndex}
     * and {@code writerIndex} are {@code 0} and {@code buffer.readableBytes}
     * respectively.
     */

    /**
     * 创建一个新缓冲区，其内容是指定 {@code buffer} 的可读字节的副本。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和 {@code buffer.readableBytes}。
     */
    public static ByteBuf copiedBuffer(ByteBuf buffer) {
        int readable = buffer.readableBytes();
        if (readable > 0) {
            ByteBuf copy = buffer(readable);
            copy.writeBytes(buffer, buffer.readerIndex(), readable);
            return copy;
        } else {
            return EMPTY_BUFFER;
        }
    }

    /**
     * Creates a new big-endian buffer whose content is a merged copy of
     * the specified {@code arrays}.  The new buffer's {@code readerIndex}
     * and {@code writerIndex} are {@code 0} and the sum of all arrays'
     * {@code length} respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定 {@code arrays} 的合并副本。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和所有数组 {@code length} 的总和。
     */
    public static ByteBuf copiedBuffer(byte[]... arrays) {
        switch (arrays.length) {
        case 0:
            return EMPTY_BUFFER;
        case 1:
            if (arrays[0].length == 0) {
                return EMPTY_BUFFER;
            } else {
                return copiedBuffer(arrays[0]);
            }
        }

        // Merge the specified arrays into one array.

        // 将指定的数组合并为一个数组。
        int length = 0;
        for (byte[] a: arrays) {
            if (Integer.MAX_VALUE - length < a.length) {
                throw new IllegalArgumentException(
                        "The total length of the specified arrays is too big.");
            }
            length += a.length;
        }

        if (length == 0) {
            return EMPTY_BUFFER;
        }

        byte[] mergedArray = PlatformDependent.allocateUninitializedArray(length);
        for (int i = 0, j = 0; i < arrays.length; i ++) {
            byte[] a = arrays[i];
            System.arraycopy(a, 0, mergedArray, j, a.length);
            j += a.length;
        }

        return wrappedBuffer(mergedArray);
    }

    /**
     * Creates a new buffer whose content is a merged copy of the specified
     * {@code buffers}' readable bytes.  The new buffer's {@code readerIndex}
     * and {@code writerIndex} are {@code 0} and the sum of all buffers'
     * {@code readableBytes} respectively.
     *
     * @throws IllegalArgumentException
     *         if the specified buffers' endianness are different from each
     *         other
     */

    /**
     * 创建一个新的缓冲区，其内容是指定 {@code buffers} 的可读字节的合并副本。新缓冲区的
     * {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和所有缓冲区的
     * {@code readableBytes} 的总和。
     *
     * @throws IllegalArgumentException
     *         如果指定的缓冲区的字节序彼此不同
     */
    public static ByteBuf copiedBuffer(ByteBuf... buffers) {
        switch (buffers.length) {
        case 0:
            return EMPTY_BUFFER;
        case 1:
            return copiedBuffer(buffers[0]);
        }

        // Merge the specified buffers into one buffer.

        // 将指定的缓冲区合并为一个缓冲区。
        ByteOrder order = null;
        int length = 0;
        for (ByteBuf b: buffers) {
            int bLen = b.readableBytes();
            if (bLen <= 0) {
                continue;
            }
            if (Integer.MAX_VALUE - length < bLen) {
                throw new IllegalArgumentException(
                        "The total length of the specified buffers is too big.");
            }
            length += bLen;
            if (order != null) {
                if (!order.equals(b.order())) {
                    throw new IllegalArgumentException("inconsistent byte order");
                }
            } else {
                order = b.order();
            }
        }

        if (length == 0) {
            return EMPTY_BUFFER;
        }

        byte[] mergedArray = PlatformDependent.allocateUninitializedArray(length);
        for (int i = 0, j = 0; i < buffers.length; i ++) {
            ByteBuf b = buffers[i];
            int bLen = b.readableBytes();
            b.getBytes(b.readerIndex(), mergedArray, j, bLen);
            j += bLen;
        }

        return wrappedBuffer(mergedArray).order(order);
    }

    /**
     * Creates a new buffer whose content is a merged copy of the specified
     * {@code buffers}' slices.  The new buffer's {@code readerIndex} and
     * {@code writerIndex} are {@code 0} and the sum of all buffers'
     * {@code remaining} respectively.
     *
     * @throws IllegalArgumentException
     *         if the specified buffers' endianness are different from each
     *         other
     */

    /**
     * 创建一个新的缓冲区，其内容是指定 {@code buffers} 的切片的合并副本。新缓冲区的
     * {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和所有缓冲区
     * {@code remaining} 的总和。
     *
     * @throws IllegalArgumentException
     *         如果指定的缓冲区的字节顺序彼此不同
     */
    public static ByteBuf copiedBuffer(ByteBuffer... buffers) {
        switch (buffers.length) {
        case 0:
            return EMPTY_BUFFER;
        case 1:
            return copiedBuffer(buffers[0]);
        }

        // Merge the specified buffers into one buffer.

        // 将指定的缓冲区合并为一个缓冲区。
        ByteOrder order = null;
        int length = 0;
        for (ByteBuffer b: buffers) {
            int bLen = b.remaining();
            if (bLen <= 0) {
                continue;
            }
            if (Integer.MAX_VALUE - length < bLen) {
                throw new IllegalArgumentException(
                        "The total length of the specified buffers is too big.");
            }
            length += bLen;
            if (order != null) {
                if (!order.equals(b.order())) {
                    throw new IllegalArgumentException("inconsistent byte order");
                }
            } else {
                order = b.order();
            }
        }

        if (length == 0) {
            return EMPTY_BUFFER;
        }

        byte[] mergedArray = PlatformDependent.allocateUninitializedArray(length);
        for (int i = 0, j = 0; i < buffers.length; i ++) {
            // Duplicate the buffer so we not adjust the position during our get operation.
            // 复制缓冲区，以便在获取操作期间不调整位置。
            // See https://github.com/netty/netty/issues/3896

// 参见 https://github.com/netty/netty/issues/3896

            ByteBuffer b = buffers[i].duplicate();
            int bLen = b.remaining();
            b.get(mergedArray, j, bLen);
            j += bLen;
        }

        return wrappedBuffer(mergedArray).order(order);
    }

    /**
     * Creates a new big-endian buffer whose content is the specified
     * {@code string} encoded in the specified {@code charset}.
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and the length of the encoded string respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定的 {@code string} 使用指定的 {@code charset} 编码。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和编码字符串的长度。
     */
    public static ByteBuf copiedBuffer(CharSequence string, Charset charset) {
        ObjectUtil.checkNotNull(string, "string");
        if (CharsetUtil.UTF_8.equals(charset)) {
            return copiedBufferUtf8(string);
        }
        if (CharsetUtil.US_ASCII.equals(charset)) {
            return copiedBufferAscii(string);
        }
        if (string instanceof CharBuffer) {
            return copiedBuffer((CharBuffer) string, charset);
        }

        return copiedBuffer(CharBuffer.wrap(string), charset);
    }

    private static ByteBuf copiedBufferUtf8(CharSequence string) {
        boolean release = true;
        // Mimic the same behavior as other copiedBuffer implementations.
        // 模仿其他 copiedBuffer 实现的相同行为。
        ByteBuf buffer = ALLOC.heapBuffer(ByteBufUtil.utf8Bytes(string));
        try {
            ByteBufUtil.writeUtf8(buffer, string);
            release = false;
            return buffer;
        } finally {
            if (release) {
                buffer.release();
            }
        }
    }

    private static ByteBuf copiedBufferAscii(CharSequence string) {
        boolean release = true;
        // Mimic the same behavior as other copiedBuffer implementations.
        // 模仿其他 copiedBuffer 实现的相同行为。
        ByteBuf buffer = ALLOC.heapBuffer(string.length());
        try {
            ByteBufUtil.writeAscii(buffer, string);
            release = false;
            return buffer;
        } finally {
            if (release) {
                buffer.release();
            }
        }
    }

    /**
     * Creates a new big-endian buffer whose content is a subregion of
     * the specified {@code string} encoded in the specified {@code charset}.
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and the length of the encoded string respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定 {@code string} 在指定 {@code charset} 中编码的子区域。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和编码字符串的长度。
     */
    public static ByteBuf copiedBuffer(
            CharSequence string, int offset, int length, Charset charset) {
        ObjectUtil.checkNotNull(string, "string");
        if (length == 0) {
            return EMPTY_BUFFER;
        }

        if (string instanceof CharBuffer) {
            CharBuffer buf = (CharBuffer) string;
            if (buf.hasArray()) {
                return copiedBuffer(
                        buf.array(),
                        buf.arrayOffset() + buf.position() + offset,
                        length, charset);
            }

            buf = buf.slice();
            buf.limit(length);
            buf.position(offset);
            return copiedBuffer(buf, charset);
        }

        return copiedBuffer(CharBuffer.wrap(string, offset, offset + length), charset);
    }

    /**
     * Creates a new big-endian buffer whose content is the specified
     * {@code array} encoded in the specified {@code charset}.
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and the length of the encoded string respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容为指定的 {@code array} 使用指定的 {@code charset} 编码。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和编码字符串的长度。
     */
    public static ByteBuf copiedBuffer(char[] array, Charset charset) {
        ObjectUtil.checkNotNull(array, "array");
        return copiedBuffer(array, 0, array.length, charset);
    }

    /**
     * Creates a new big-endian buffer whose content is a subregion of
     * the specified {@code array} encoded in the specified {@code charset}.
     * The new buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and the length of the encoded string respectively.
     */

    /**
     * 创建一个新的大端序缓冲区，其内容是指定 {@code array} 的子区域，使用指定的 {@code charset} 编码。
     * 新缓冲区的 {@code readerIndex} 和 {@code writerIndex} 分别为 {@code 0} 和编码字符串的长度。
     */
    public static ByteBuf copiedBuffer(char[] array, int offset, int length, Charset charset) {
        ObjectUtil.checkNotNull(array, "array");
        if (length == 0) {
            return EMPTY_BUFFER;
        }
        return copiedBuffer(CharBuffer.wrap(array, offset, length), charset);
    }

    private static ByteBuf copiedBuffer(CharBuffer buffer, Charset charset) {
        return ByteBufUtil.encodeString0(ALLOC, true, buffer, charset, 0);
    }

    /**
     * Creates a read-only buffer which disallows any modification operations
     * on the specified {@code buffer}.  The new buffer has the same
     * {@code readerIndex} and {@code writerIndex} with the specified
     * {@code buffer}.
     *
     * @deprecated Use {@link ByteBuf#asReadOnly()}.
     */

    /**
     * 创建一个只读缓冲区，禁止对指定 {@code buffer} 进行任何修改操作。
     * 新缓冲区具有与指定 {@code buffer} 相同的 {@code readerIndex} 和 {@code writerIndex}。
     *
     * @deprecated 使用 {@link ByteBuf#asReadOnly()}。
     */
    @Deprecated
    public static ByteBuf unmodifiableBuffer(ByteBuf buffer) {
        ByteOrder endianness = buffer.order();
        if (endianness == BIG_ENDIAN) {
            return new ReadOnlyByteBuf(buffer);
        }

        return new ReadOnlyByteBuf(buffer.order(BIG_ENDIAN)).order(LITTLE_ENDIAN);
    }

    /**
     * Creates a new 4-byte big-endian buffer that holds the specified 32-bit integer.
     */

    /**
     * 创建一个新的4字节大端序缓冲区，该缓冲区持有指定的32位整数。
     */
    public static ByteBuf copyInt(int value) {
        ByteBuf buf = buffer(4);
        buf.writeInt(value);
        return buf;
    }

    /**
     * Create a big-endian buffer that holds a sequence of the specified 32-bit integers.
     */

    /**
     * 创建一个大端序的缓冲区，用于存储指定的32位整数序列。
     */
    public static ByteBuf copyInt(int... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 4);
        for (int v: values) {
            buffer.writeInt(v);
        }
        return buffer;
    }

    /**
     * Creates a new 2-byte big-endian buffer that holds the specified 16-bit integer.
     */

    /**
     * 创建一个新的2字节大端序缓冲区，用于存储指定的16位整数。
     */
    public static ByteBuf copyShort(int value) {
        ByteBuf buf = buffer(2);
        buf.writeShort(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 16-bit integers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的16位整数序列。
     */
    public static ByteBuf copyShort(short... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 2);
        for (int v: values) {
            buffer.writeShort(v);
        }
        return buffer;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 16-bit integers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的16位整数序列。
     */
    public static ByteBuf copyShort(int... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 2);
        for (int v: values) {
            buffer.writeShort(v);
        }
        return buffer;
    }

    /**
     * Creates a new 3-byte big-endian buffer that holds the specified 24-bit integer.
     */

    /**
     * 创建一个新的3字节大端序缓冲区，用于保存指定的24位整数。
     */
    public static ByteBuf copyMedium(int value) {
        ByteBuf buf = buffer(3);
        buf.writeMedium(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 24-bit integers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的24位整数序列。
     */
    public static ByteBuf copyMedium(int... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 3);
        for (int v: values) {
            buffer.writeMedium(v);
        }
        return buffer;
    }

    /**
     * Creates a new 8-byte big-endian buffer that holds the specified 64-bit integer.
     */

    /**
     * 创建一个新的8字节大端序缓冲区，用于保存指定的64位整数。
     */
    public static ByteBuf copyLong(long value) {
        ByteBuf buf = buffer(8);
        buf.writeLong(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 64-bit integers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的64位整数序列。
     */
    public static ByteBuf copyLong(long... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 8);
        for (long v: values) {
            buffer.writeLong(v);
        }
        return buffer;
    }

    /**
     * Creates a new single-byte big-endian buffer that holds the specified boolean value.
     */

    /**
     * 创建一个新的单字节大端缓冲区，用于保存指定的布尔值。
     */
    public static ByteBuf copyBoolean(boolean value) {
        ByteBuf buf = buffer(1);
        buf.writeBoolean(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified boolean values.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的布尔值序列。
     */
    public static ByteBuf copyBoolean(boolean... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length);
        for (boolean v: values) {
            buffer.writeBoolean(v);
        }
        return buffer;
    }

    /**
     * Creates a new 4-byte big-endian buffer that holds the specified 32-bit floating point number.
     */

    /**
     * 创建一个新的4字节大端序缓冲区，用于保存指定的32位浮点数。
     */
    public static ByteBuf copyFloat(float value) {
        ByteBuf buf = buffer(4);
        buf.writeFloat(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 32-bit floating point numbers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的32位浮点数序列。
     */
    public static ByteBuf copyFloat(float... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 4);
        for (float v: values) {
            buffer.writeFloat(v);
        }
        return buffer;
    }

    /**
     * Creates a new 8-byte big-endian buffer that holds the specified 64-bit floating point number.
     */

    /**
     * 创建一个新的8字节大端序缓冲区，用于存储指定的64位浮点数。
     */
    public static ByteBuf copyDouble(double value) {
        ByteBuf buf = buffer(8);
        buf.writeDouble(value);
        return buf;
    }

    /**
     * Create a new big-endian buffer that holds a sequence of the specified 64-bit floating point numbers.
     */

    /**
     * 创建一个新的大端序缓冲区，用于保存指定的64位浮点数序列。
     */
    public static ByteBuf copyDouble(double... values) {
        if (values == null || values.length == 0) {
            return EMPTY_BUFFER;
        }
        ByteBuf buffer = buffer(values.length * 8);
        for (double v: values) {
            buffer.writeDouble(v);
        }
        return buffer;
    }

    /**
     * Return a unreleasable view on the given {@link ByteBuf} which will just ignore release and retain calls.
     */

    /**
     * 返回给定 {@link ByteBuf} 的不可释放视图，该视图将忽略 release 和 retain 调用。
     */
    public static ByteBuf unreleasableBuffer(ByteBuf buf) {
        return new UnreleasableByteBuf(buf);
    }

    /**
     * Wrap the given {@link ByteBuf}s in an unmodifiable {@link ByteBuf}. Be aware the returned {@link ByteBuf} will
     * not try to slice the given {@link ByteBuf}s to reduce GC-Pressure.
     *
     * @deprecated Use {@link #wrappedUnmodifiableBuffer(ByteBuf...)}.
     */

    /**
     * 将给定的 {@link ByteBuf}s 包装在一个不可修改的 {@link ByteBuf} 中。请注意，返回的 {@link ByteBuf} 不会尝试对给定的 {@link ByteBuf}s 进行切片以减少 GC 压力。
     *
     * @deprecated 使用 {@link #wrappedUnmodifiableBuffer(ByteBuf...)}。
     */
    @Deprecated
    public static ByteBuf unmodifiableBuffer(ByteBuf... buffers) {
        return wrappedUnmodifiableBuffer(true, buffers);
    }

    /**
     * Wrap the given {@link ByteBuf}s in an unmodifiable {@link ByteBuf}. Be aware the returned {@link ByteBuf} will
     * not try to slice the given {@link ByteBuf}s to reduce GC-Pressure.
     *
     * The returned {@link ByteBuf} may wrap the provided array directly, and so should not be subsequently modified.
     */

    /**
     * 将给定的 {@link ByteBuf}s 包装在一个不可修改的 {@link ByteBuf} 中。请注意，返回的 {@link ByteBuf} 不会尝试对给定的 {@link ByteBuf}s 进行切片以减少 GC 压力。
     *
     * 返回的 {@link ByteBuf} 可能会直接包装提供的数组，因此不应随后修改。
     */
    public static ByteBuf wrappedUnmodifiableBuffer(ByteBuf... buffers) {
        return wrappedUnmodifiableBuffer(false, buffers);
    }

    private static ByteBuf wrappedUnmodifiableBuffer(boolean copy, ByteBuf... buffers) {
        switch (buffers.length) {
        case 0:
            return EMPTY_BUFFER;
        case 1:
            return buffers[0].asReadOnly();
        default:
            if (copy) {
                buffers = Arrays.copyOf(buffers, buffers.length, ByteBuf[].class);
            }
            return new FixedCompositeByteBuf(ALLOC, buffers);
        }
    }

    private Unpooled() {
        // Unused
        // 未使用
    }
}
