
package org.top.java.netty.source.buffer;

import io.netty.util.AsciiString;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.*;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Locale;

import static io.netty.util.internal.MathUtil.isOutOfBounds;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static io.netty.util.internal.StringUtil.NEWLINE;
import static io.netty.util.internal.StringUtil.isSurrogate;

/**
 * A collection of utility methods that is related with handling {@link ByteBuf},
 * such as the generation of hex dump and swapping an integer's byte order.
 */

/**
 * 一个与处理 {@link ByteBuf} 相关的实用方法集合，
 * 例如生成十六进制转储和交换整数的字节顺序。
 */
public final class ByteBufUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ByteBufUtil.class);
    private static final FastThreadLocal<byte[]> BYTE_ARRAYS = new FastThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() throws Exception {
            return PlatformDependent.allocateUninitializedArray(MAX_TL_ARRAY_LEN);
        }
    };

    private static final byte WRITE_UTF_UNKNOWN = (byte) '?';
    private static final int MAX_CHAR_BUFFER_SIZE;
    private static final int THREAD_LOCAL_BUFFER_SIZE;
    private static final int MAX_BYTES_PER_CHAR_UTF8 =
            (int) CharsetUtil.encoder(CharsetUtil.UTF_8).maxBytesPerChar();

    static final int WRITE_CHUNK_SIZE = 8192;
    static final ByteBufAllocator DEFAULT_ALLOCATOR;

    static {
        String allocType = SystemPropertyUtil.get(
                "io.netty.allocator.type", PlatformDependent.isAndroid() ? "unpooled" : "pooled");
        allocType = allocType.toLowerCase(Locale.US).trim();

        ByteBufAllocator alloc;
        if ("unpooled".equals(allocType)) {
            alloc = UnpooledByteBufAllocator.DEFAULT;
            logger.debug("-Dio.netty.allocator.type: {}", allocType);
        } else if ("pooled".equals(allocType)) {
            alloc = PooledByteBufAllocator.DEFAULT;
            logger.debug("-Dio.netty.allocator.type: {}", allocType);
        } else {
            alloc = PooledByteBufAllocator.DEFAULT;
            logger.debug("-Dio.netty.allocator.type: pooled (unknown: {})", allocType);
        }

        DEFAULT_ALLOCATOR = alloc;

        THREAD_LOCAL_BUFFER_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalDirectBufferSize", 0);
        logger.debug("-Dio.netty.threadLocalDirectBufferSize: {}", THREAD_LOCAL_BUFFER_SIZE);

        MAX_CHAR_BUFFER_SIZE = SystemPropertyUtil.getInt("io.netty.maxThreadLocalCharBufferSize", 16 * 1024);
        logger.debug("-Dio.netty.maxThreadLocalCharBufferSize: {}", MAX_CHAR_BUFFER_SIZE);
    }

    static final int MAX_TL_ARRAY_LEN = 1024;

    /**
     * Allocates a new array if minLength > {@link ByteBufUtil#MAX_TL_ARRAY_LEN}
     */

    /**
     * 如果 minLength > {@link ByteBufUtil#MAX_TL_ARRAY_LEN}，则分配一个新数组
     */
    static byte[] threadLocalTempArray(int minLength) {
        return minLength <= MAX_TL_ARRAY_LEN ? BYTE_ARRAYS.get()
            : PlatformDependent.allocateUninitializedArray(minLength);
    }

    /**
     * @return whether the specified buffer has a nonzero ref count
     */

    /**
     * @return 指定的缓冲区是否具有非零引用计数
     */
    public static boolean isAccessible(ByteBuf buffer) {
        return buffer.isAccessible();
    }

    /**
     * @throws IllegalReferenceCountException if the buffer has a zero ref count
     * @return the passed in buffer
     */

    /**
     * @throws IllegalReferenceCountException 如果缓冲区的引用计数为零
     * @return 传入的缓冲区
     */
    public static ByteBuf ensureAccessible(ByteBuf buffer) {
        if (!buffer.isAccessible()) {
            throw new IllegalReferenceCountException(buffer.refCnt());
        }
        return buffer;
    }

    /**
     * Returns a <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     * of the specified buffer's readable bytes.
     */

    /**
     * 返回指定缓冲区的可读字节的<a href="https://en.wikipedia.org/wiki/Hex_dump">十六进制转储</a>。
     */
    public static String hexDump(ByteBuf buffer) {
        return hexDump(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    /**
     * Returns a <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     * of the specified buffer's sub-region.
     */

    /**
     * 返回指定缓冲区子区域的<a href="https://en.wikipedia.org/wiki/Hex_dump">十六进制转储</a>。
     */
    public static String hexDump(ByteBuf buffer, int fromIndex, int length) {
        return HexUtil.hexDump(buffer, fromIndex, length);
    }

    /**
     * Returns a <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     * of the specified byte array.
     */

    /**
     * 返回指定字节数组的<a href="https://en.wikipedia.org/wiki/Hex_dump">十六进制转储</a>。
     */
    public static String hexDump(byte[] array) {
        return hexDump(array, 0, array.length);
    }

    /**
     * Returns a <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     * of the specified byte array's sub-region.
     */

    /**
     * 返回指定字节数组子区域的<a href="https://en.wikipedia.org/wiki/Hex_dump">十六进制转储</a>。
     */
    public static String hexDump(byte[] array, int fromIndex, int length) {
        return HexUtil.hexDump(array, fromIndex, length);
    }

    /**
     * Decode a 2-digit hex byte from within a string.
     */

    /**
     * 从字符串中解码一个2位的十六进制字节。
     */
    public static byte decodeHexByte(CharSequence s, int pos) {
        return StringUtil.decodeHexByte(s, pos);
    }

    /**
     * Decodes a string generated by {@link #hexDump(byte[])}
     */

    /**
     * 解码由 {@link #hexDump(byte[])} 生成的字符串
     */
    public static byte[] decodeHexDump(CharSequence hexDump) {
        return StringUtil.decodeHexDump(hexDump, 0, hexDump.length());
    }

    /**
     * Decodes part of a string generated by {@link #hexDump(byte[])}
     */

    /**
     * 解码由 {@link #hexDump(byte[])} 生成的部分字符串
     */
    public static byte[] decodeHexDump(CharSequence hexDump, int fromIndex, int length) {
        return StringUtil.decodeHexDump(hexDump, fromIndex, length);
    }

    /**
     * Used to determine if the return value of {@link ByteBuf#ensureWritable(int, boolean)} means that there is
     * adequate space and a write operation will succeed.
     * @param ensureWritableResult The return value from {@link ByteBuf#ensureWritable(int, boolean)}.
     * @return {@code true} if {@code ensureWritableResult} means that there is adequate space and a write operation
     * will succeed.
     */

    /**
     * 用于确定 {@link ByteBuf#ensureWritable(int, boolean)} 的返回值是否表示有足够的空间且写操作会成功。
     * @param ensureWritableResult {@link ByteBuf#ensureWritable(int, boolean)} 的返回值。
     * @return 如果 {@code ensureWritableResult} 表示有足够的空间且写操作会成功，则返回 {@code true}。
     */
    public static boolean ensureWritableSuccess(int ensureWritableResult) {
        return ensureWritableResult == 0 || ensureWritableResult == 2;
    }

    /**
     * Calculates the hash code of the specified buffer.  This method is
     * useful when implementing a new buffer type.
     */

    /**
     * 计算指定缓冲区的哈希码。此方法在实现新缓冲区类型时非常有用。
     */
    public static int hashCode(ByteBuf buffer) {
        final int aLen = buffer.readableBytes();
        final int intCount = aLen >>> 2;
        final int byteCount = aLen & 3;

        int hashCode = EmptyByteBuf.EMPTY_BYTE_BUF_HASH_CODE;
        int arrayIndex = buffer.readerIndex();
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            for (int i = intCount; i > 0; i --) {
                hashCode = 31 * hashCode + buffer.getInt(arrayIndex);
                arrayIndex += 4;
            }
        } else {
            for (int i = intCount; i > 0; i --) {
                hashCode = 31 * hashCode + swapInt(buffer.getInt(arrayIndex));
                arrayIndex += 4;
            }
        }

        for (int i = byteCount; i > 0; i --) {
            hashCode = 31 * hashCode + buffer.getByte(arrayIndex ++);
        }

        if (hashCode == 0) {
            hashCode = 1;
        }

        return hashCode;
    }

    /**
     * Returns the reader index of needle in haystack, or -1 if needle is not in haystack.
     * This method uses the <a href="https://en.wikipedia.org/wiki/Two-way_string-matching_algorithm">Two-Way
     * string matching algorithm</a>, which yields O(1) space complexity and excellent performance.
     */

    /**
     * 返回needle在haystack中的读取索引，如果needle不在haystack中则返回-1。
     * 该方法使用<a href="https://en.wikipedia.org/wiki/Two-way_string-matching_algorithm">双向字符串匹配算法</a>，
     * 该算法具有O(1)的空间复杂度和优异的性能。
     */
    public static int indexOf(ByteBuf needle, ByteBuf haystack) {
        if (haystack == null || needle == null) {
            return -1;
        }

        if (needle.readableBytes() > haystack.readableBytes()) {
            return -1;
        }

        int n = haystack.readableBytes();
        int m = needle.readableBytes();
        if (m == 0) {
            return 0;
        }

        // When the needle has only one byte that can be read,

        // 当针只有一个字节可以读取时，
        // the ByteBuf.indexOf() can be used
        // ByteBuf.indexOf() 可以被使用
        if (m == 1) {
            return haystack.indexOf(haystack.readerIndex(), haystack.writerIndex(),
                          needle.getByte(needle.readerIndex()));
        }

        int i;
        int j = 0;
        int aStartIndex = needle.readerIndex();
        int bStartIndex = haystack.readerIndex();
        long suffixes =  maxSuf(needle, m, aStartIndex, true);
        long prefixes = maxSuf(needle, m, aStartIndex, false);
        int ell = Math.max((int) (suffixes >> 32), (int) (prefixes >> 32));
        int per = Math.max((int) suffixes, (int) prefixes);
        int memory;
        int length = Math.min(m - per, ell + 1);

        if (equals(needle, aStartIndex, needle, aStartIndex + per,  length)) {
            memory = -1;
            while (j <= n - m) {
                i = Math.max(ell, memory) + 1;
                while (i < m && needle.getByte(i + aStartIndex) == haystack.getByte(i + j + bStartIndex)) {
                    ++i;
                }
                if (i > n) {
                    return -1;
                }
                if (i >= m) {
                    i = ell;
                    while (i > memory && needle.getByte(i + aStartIndex) == haystack.getByte(i + j + bStartIndex)) {
                        --i;
                    }
                    if (i <= memory) {
                        return j + bStartIndex;
                    }
                    j += per;
                    memory = m - per - 1;
                } else {
                    j += i - ell;
                    memory = -1;
                }
            }
        } else {
            per = Math.max(ell + 1, m - ell - 1) + 1;
            while (j <= n - m) {
                i = ell + 1;
                while (i < m && needle.getByte(i + aStartIndex) == haystack.getByte(i + j + bStartIndex)) {
                    ++i;
                }
                if (i > n) {
                    return -1;
                }
                if (i >= m) {
                    i = ell;
                    while (i >= 0 && needle.getByte(i + aStartIndex) == haystack.getByte(i + j + bStartIndex)) {
                        --i;
                    }
                    if (i < 0) {
                        return j + bStartIndex;
                    }
                    j += per;
                } else {
                    j += i - ell;
                }
            }
        }
        return -1;
    }

    private static long maxSuf(ByteBuf x, int m, int start, boolean isSuffix) {
        int p = 1;
        int ms = -1;
        int j = start;
        int k = 1;
        byte a;
        byte b;
        while (j + k < m) {
            a = x.getByte(j + k);
            b = x.getByte(ms + k);
            boolean suffix = isSuffix ? a < b : a > b;
            if (suffix) {
                j += k;
                k = 1;
                p = j - ms;
            } else if (a == b) {
                if (k != p) {
                    ++k;
                } else {
                    j += p;
                    k = 1;
                }
            } else {
                ms = j;
                j = ms + 1;
                k = p = 1;
            }
        }
        return ((long) ms << 32) + p;
    }

    /**
     * Returns {@code true} if and only if the two specified buffers are
     * identical to each other for {@code length} bytes starting at {@code aStartIndex}
     * index for the {@code a} buffer and {@code bStartIndex} index for the {@code b} buffer.
     * A more compact way to express this is:
     * <p>
     * {@code a[aStartIndex : aStartIndex + length] == b[bStartIndex : bStartIndex + length]}
     */

    /**
     * 当且仅当两个指定的缓冲区在从 {@code a} 缓冲区的 {@code aStartIndex} 索引开始和从 {@code b} 缓冲区的 {@code bStartIndex} 索引开始的 {@code length} 字节范围内完全相同时，返回 {@code true}。
     * 更简洁的表达方式是：
     * <p>
     * {@code a[aStartIndex : aStartIndex + length] == b[bStartIndex : bStartIndex + length]}
     */
    public static boolean equals(ByteBuf a, int aStartIndex, ByteBuf b, int bStartIndex, int length) {
        checkNotNull(a, "a");
        checkNotNull(b, "b");
        // All indexes and lengths must be non-negative
        // 所有索引和长度必须为非负数
        checkPositiveOrZero(aStartIndex, "aStartIndex");
        checkPositiveOrZero(bStartIndex, "bStartIndex");
        checkPositiveOrZero(length, "length");

        if (a.writerIndex() - length < aStartIndex || b.writerIndex() - length < bStartIndex) {
            return false;
        }

        final int longCount = length >>> 3;
        final int byteCount = length & 7;

        if (a.order() == b.order()) {
            for (int i = longCount; i > 0; i --) {
                if (a.getLong(aStartIndex) != b.getLong(bStartIndex)) {
                    return false;
                }
                aStartIndex += 8;
                bStartIndex += 8;
            }
        } else {
            for (int i = longCount; i > 0; i --) {
                if (a.getLong(aStartIndex) != swapLong(b.getLong(bStartIndex))) {
                    return false;
                }
                aStartIndex += 8;
                bStartIndex += 8;
            }
        }

        for (int i = byteCount; i > 0; i --) {
            if (a.getByte(aStartIndex) != b.getByte(bStartIndex)) {
                return false;
            }
            aStartIndex ++;
            bStartIndex ++;
        }

        return true;
    }

    /**
     * Returns {@code true} if and only if the two specified buffers are
     * identical to each other as described in {@link ByteBuf#equals(Object)}.
     * This method is useful when implementing a new buffer type.
     */

    /**
     * 当且仅当两个指定的缓冲区根据 {@link ByteBuf#equals(Object)} 中的描述彼此相同时，返回 {@code true}。
     * 该方法在实现新的缓冲区类型时非常有用。
     */
    public static boolean equals(ByteBuf bufferA, ByteBuf bufferB) {
        if (bufferA == bufferB) {
            return true;
        }
        final int aLen = bufferA.readableBytes();
        if (aLen != bufferB.readableBytes()) {
            return false;
        }
        return equals(bufferA, bufferA.readerIndex(), bufferB, bufferB.readerIndex(), aLen);
    }

    /**
     * Compares the two specified buffers as described in {@link ByteBuf#compareTo(ByteBuf)}.
     * This method is useful when implementing a new buffer type.
     */

    /**
     * 按照 {@link ByteBuf#compareTo(ByteBuf)} 中描述的方式比较两个指定的缓冲区。
     * 在实现新的缓冲区类型时，此方法非常有用。
     */
    public static int compare(ByteBuf bufferA, ByteBuf bufferB) {
        if (bufferA == bufferB) {
            return 0;
        }
        final int aLen = bufferA.readableBytes();
        final int bLen = bufferB.readableBytes();
        final int minLength = Math.min(aLen, bLen);
        final int uintCount = minLength >>> 2;
        final int byteCount = minLength & 3;
        int aIndex = bufferA.readerIndex();
        int bIndex = bufferB.readerIndex();

        if (uintCount > 0) {
            boolean bufferAIsBigEndian = bufferA.order() == ByteOrder.BIG_ENDIAN;
            final long res;
            int uintCountIncrement = uintCount << 2;

            if (bufferA.order() == bufferB.order()) {
                res = bufferAIsBigEndian ? compareUintBigEndian(bufferA, bufferB, aIndex, bIndex, uintCountIncrement) :
                        compareUintLittleEndian(bufferA, bufferB, aIndex, bIndex, uintCountIncrement);
            } else {
                res = bufferAIsBigEndian ? compareUintBigEndianA(bufferA, bufferB, aIndex, bIndex, uintCountIncrement) :
                        compareUintBigEndianB(bufferA, bufferB, aIndex, bIndex, uintCountIncrement);
            }
            if (res != 0) {
                // Ensure we not overflow when cast
                // 确保在转换时不会溢出
                return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, res));
            }
            aIndex += uintCountIncrement;
            bIndex += uintCountIncrement;
        }

        for (int aEnd = aIndex + byteCount; aIndex < aEnd; ++aIndex, ++bIndex) {
            int comp = bufferA.getUnsignedByte(aIndex) - bufferB.getUnsignedByte(bIndex);
            if (comp != 0) {
                return comp;
            }
        }

        return aLen - bLen;
    }

    private static long compareUintBigEndian(
            ByteBuf bufferA, ByteBuf bufferB, int aIndex, int bIndex, int uintCountIncrement) {
        for (int aEnd = aIndex + uintCountIncrement; aIndex < aEnd; aIndex += 4, bIndex += 4) {
            long comp = bufferA.getUnsignedInt(aIndex) - bufferB.getUnsignedInt(bIndex);
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

    private static long compareUintLittleEndian(
            ByteBuf bufferA, ByteBuf bufferB, int aIndex, int bIndex, int uintCountIncrement) {
        for (int aEnd = aIndex + uintCountIncrement; aIndex < aEnd; aIndex += 4, bIndex += 4) {
            long comp = uintFromLE(bufferA.getUnsignedIntLE(aIndex)) - uintFromLE(bufferB.getUnsignedIntLE(bIndex));
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

    private static long compareUintBigEndianA(
            ByteBuf bufferA, ByteBuf bufferB, int aIndex, int bIndex, int uintCountIncrement) {
        for (int aEnd = aIndex + uintCountIncrement; aIndex < aEnd; aIndex += 4, bIndex += 4) {
            long a = bufferA.getUnsignedInt(aIndex);
            long b = uintFromLE(bufferB.getUnsignedIntLE(bIndex));
            long comp =  a - b;
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

    private static long compareUintBigEndianB(
            ByteBuf bufferA, ByteBuf bufferB, int aIndex, int bIndex, int uintCountIncrement) {
        for (int aEnd = aIndex + uintCountIncrement; aIndex < aEnd; aIndex += 4, bIndex += 4) {
            long a = uintFromLE(bufferA.getUnsignedIntLE(aIndex));
            long b = bufferB.getUnsignedInt(bIndex);
            long comp =  a - b;
            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }

    private static long uintFromLE(long value) {
        return Long.reverseBytes(value) >>> Integer.SIZE;
    }

    private static final class SWARByteSearch {

        private static long compilePattern(byte byteToFind) {
            return (byteToFind & 0xFFL) * 0x101010101010101L;
        }

        private static int firstAnyPattern(long word, long pattern, boolean leading) {
            long input = word ^ pattern;
            long tmp = (input & 0x7F7F7F7F7F7F7F7FL) + 0x7F7F7F7F7F7F7F7FL;
            tmp = ~(tmp | input | 0x7F7F7F7F7F7F7F7FL);
            final int binaryPosition = leading? Long.numberOfLeadingZeros(tmp) : Long.numberOfTrailingZeros(tmp);
            return binaryPosition >>> 3;
        }
    }

    private static int unrolledFirstIndexOf(AbstractByteBuf buffer, int fromIndex, int byteCount, byte value) {
        assert byteCount > 0 && byteCount < 8;
        if (buffer._getByte(fromIndex) == value) {
            return fromIndex;
        }
        if (byteCount == 1) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 1) == value) {
            return fromIndex + 1;
        }
        if (byteCount == 2) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 2) == value) {
            return fromIndex + 2;
        }
        if (byteCount == 3) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 3) == value) {
            return fromIndex + 3;
        }
        if (byteCount == 4) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 4) == value) {
            return fromIndex + 4;
        }
        if (byteCount == 5) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 5) == value) {
            return fromIndex + 5;
        }
        if (byteCount == 6) {
            return -1;
        }
        if (buffer._getByte(fromIndex + 6) == value) {
            return fromIndex + 6;
        }
        return -1;
    }

    /**
     * This is using a SWAR (SIMD Within A Register) batch read technique to minimize bound-checks and improve memory
     * usage while searching for {@code value}.
     */

    /**
     * 这里使用了SWAR（寄存器内SIMD）批量读取技术，以减少边界检查并提高在搜索{@code value}时的内存使用效率。
     */
    static int firstIndexOf(AbstractByteBuf buffer, int fromIndex, int toIndex, byte value) {
        fromIndex = Math.max(fromIndex, 0);
        if (fromIndex >= toIndex || buffer.capacity() == 0) {
            return -1;
        }
        final int length = toIndex - fromIndex;
        buffer.checkIndex(fromIndex, length);
        if (!PlatformDependent.isUnaligned()) {
            return linearFirstIndexOf(buffer, fromIndex, toIndex, value);
        }
        assert PlatformDependent.isUnaligned();
        int offset = fromIndex;
        final int byteCount = length & 7;
        if (byteCount > 0) {
            final int index = unrolledFirstIndexOf(buffer, fromIndex, byteCount, value);
            if (index != -1) {
                return index;
            }
            offset += byteCount;
            if (offset == toIndex) {
                return -1;
            }
        }
        final int longCount = length >>> 3;
        final ByteOrder nativeOrder = ByteOrder.nativeOrder();
        final boolean isNative = nativeOrder == buffer.order();
        final boolean useLE = nativeOrder == ByteOrder.LITTLE_ENDIAN;
        final long pattern = SWARByteSearch.compilePattern(value);
        for (int i = 0; i < longCount; i++) {
            // use the faster available getLong
            // 使用更快的可用getLong
            final long word = useLE? buffer._getLongLE(offset) : buffer._getLong(offset);
            int index = SWARByteSearch.firstAnyPattern(word, pattern, isNative);
            if (index < Long.BYTES) {
                return offset + index;
            }
            offset += Long.BYTES;
        }
        return -1;
    }

    private static int linearFirstIndexOf(AbstractByteBuf buffer, int fromIndex, int toIndex, byte value) {
        for (int i = fromIndex; i < toIndex; i++) {
            if (buffer._getByte(i) == value) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The default implementation of {@link ByteBuf#indexOf(int, int, byte)}.
     * This method is useful when implementing a new buffer type.
     */

    /**
     * {@link ByteBuf#indexOf(int, int, byte)} 的默认实现。
     * 该方法在实现新缓冲区类型时非常有用。
     */
    public static int indexOf(ByteBuf buffer, int fromIndex, int toIndex, byte value) {
        return buffer.indexOf(fromIndex, toIndex, value);
    }

    /**
     * Toggles the endianness of the specified 16-bit short integer.
     */

    /**
     * 切换指定16位短整数的字节顺序。
     */
    public static short swapShort(short value) {
        return Short.reverseBytes(value);
    }

    /**
     * Toggles the endianness of the specified 24-bit medium integer.
     */

    /**
     * 切换指定24位中等整数的字节顺序。
     */
    public static int swapMedium(int value) {
        int swapped = value << 16 & 0xff0000 | value & 0xff00 | value >>> 16 & 0xff;
        if ((swapped & 0x800000) != 0) {
            swapped |= 0xff000000;
        }
        return swapped;
    }

    /**
     * Toggles the endianness of the specified 32-bit integer.
     */

    /**
     * 切换指定32位整数的字节序。
     */
    public static int swapInt(int value) {
        return Integer.reverseBytes(value);
    }

    /**
     * Toggles the endianness of the specified 64-bit long integer.
     */

    /**
     * 切换指定64位长整数的字节序。
     */
    public static long swapLong(long value) {
        return Long.reverseBytes(value);
    }

    /**
     * Writes a big-endian 16-bit short integer to the buffer.
     */

    /**
     * 向缓冲区写入一个大端序的16位短整数。
     */
    @SuppressWarnings("deprecation")
    public static ByteBuf writeShortBE(ByteBuf buf, int shortValue) {
        return buf.order() == ByteOrder.BIG_ENDIAN? buf.writeShort(shortValue) :
                buf.writeShort(swapShort((short) shortValue));
    }

    /**
     * Sets a big-endian 16-bit short integer to the buffer.
     */

    /**
     * 将一个以大端序表示的16位短整数设置到缓冲区中。
     */
    @SuppressWarnings("deprecation")
    public static ByteBuf setShortBE(ByteBuf buf, int index, int shortValue) {
        return buf.order() == ByteOrder.BIG_ENDIAN? buf.setShort(index, shortValue) :
                buf.setShort(index, swapShort((short) shortValue));
    }

    /**
     * Writes a big-endian 24-bit medium integer to the buffer.
     */

    /**
     * 将一个大端序的24位中等整数写入缓冲区。
     */
    @SuppressWarnings("deprecation")
    public static ByteBuf writeMediumBE(ByteBuf buf, int mediumValue) {
        return buf.order() == ByteOrder.BIG_ENDIAN? buf.writeMedium(mediumValue) :
                buf.writeMedium(swapMedium(mediumValue));
    }

    /**
     * Read the given amount of bytes into a new {@link ByteBuf} that is allocated from the {@link ByteBufAllocator}.
     */

    /**
     * 从 {@link ByteBufAllocator} 分配一个新的 {@link ByteBuf}，并读取指定数量的字节到其中。
     */
    public static ByteBuf readBytes(ByteBufAllocator alloc, ByteBuf buffer, int length) {
        boolean release = true;
        ByteBuf dst = alloc.buffer(length);
        try {
            buffer.readBytes(dst);
            release = false;
            return dst;
        } finally {
            if (release) {
                dst.release();
            }
        }
    }

    static int lastIndexOf(AbstractByteBuf buffer, int fromIndex, int toIndex, byte value) {
        assert fromIndex > toIndex;
        final int capacity = buffer.capacity();
        fromIndex = Math.min(fromIndex, capacity);
        if (fromIndex < 0 || capacity == 0) {
            return -1;
        }
        buffer.checkIndex(toIndex, fromIndex - toIndex);
        for (int i = fromIndex - 1; i >= toIndex; i--) {
            if (buffer._getByte(i) == value) {
                return i;
            }
        }

        return -1;
    }

    private static CharSequence checkCharSequenceBounds(CharSequence seq, int start, int end) {
        if (MathUtil.isOutOfBounds(start, end - start, seq.length())) {
            throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= end (" + end
                    + ") <= seq.length(" + seq.length() + ')');
        }
        return seq;
    }

    /**
     * Encode a {@link CharSequence} in <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> and write
     * it to a {@link ByteBuf} allocated with {@code alloc}.
     * @param alloc The allocator used to allocate a new {@link ByteBuf}.
     * @param seq The characters to write into a buffer.
     * @return The {@link ByteBuf} which contains the <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> encoded
     * result.
     */

    /**
     * 将 {@link CharSequence} 编码为 <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> 并写入
     * 使用 {@code alloc} 分配的 {@link ByteBuf}。
     * @param alloc 用于分配新 {@link ByteBuf} 的分配器。
     * @param seq 要写入缓冲区的字符。
     * @return 包含 <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> 编码结果的 {@link ByteBuf}。
     */
    public static ByteBuf writeUtf8(ByteBufAllocator alloc, CharSequence seq) {
        // UTF-8 uses max. 3 bytes per char, so calculate the worst case.
        // UTF-8 每个字符最多使用 3 个字节，因此计算最坏情况。
        ByteBuf buf = alloc.buffer(utf8MaxBytes(seq));
        writeUtf8(buf, seq);
        return buf;
    }

    /**
     * Encode a {@link CharSequence} in <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> and write
     * it to a {@link ByteBuf}.
     * <p>
     * It behaves like {@link #reserveAndWriteUtf8(ByteBuf, CharSequence, int)} with {@code reserveBytes}
     * computed by {@link #utf8MaxBytes(CharSequence)}.<br>
     * This method returns the actual number of bytes written.
     */

    /**
     * 将 {@link CharSequence} 编码为 <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> 并写入
     * {@link ByteBuf}。
     * <p>
     * 它的行为类似于 {@link #reserveAndWriteUtf8(ByteBuf, CharSequence, int)}，其中 {@code reserveBytes}
     * 由 {@link #utf8MaxBytes(CharSequence)} 计算。<br>
     * 此方法返回实际写入的字节数。
     */
    public static int writeUtf8(ByteBuf buf, CharSequence seq) {
        int seqLength = seq.length();
        return reserveAndWriteUtf8Seq(buf, seq, 0, seqLength, utf8MaxBytes(seqLength));
    }

    /**
     * Equivalent to <code>{@link #writeUtf8(ByteBuf, CharSequence) writeUtf8(buf, seq.subSequence(start, end))}</code>
     * but avoids subsequence object allocation.
     */

    /**
     * 等同于 <code>{@link #writeUtf8(ByteBuf, CharSequence) writeUtf8(buf, seq.subSequence(start, end))}</code>
     * 但避免了子序列对象的分配。
     */
    public static int writeUtf8(ByteBuf buf, CharSequence seq, int start, int end) {
        checkCharSequenceBounds(seq, start, end);
        return reserveAndWriteUtf8Seq(buf, seq, start, end, utf8MaxBytes(end - start));
    }

    /**
     * Encode a {@link CharSequence} in <a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a> and write
     * it into {@code reserveBytes} of a {@link ByteBuf}.
     * <p>
     * The {@code reserveBytes} must be computed (ie eagerly using {@link #utf8MaxBytes(CharSequence)}
     * or exactly with {@link #utf8Bytes(CharSequence)}) to ensure this method to not fail: for performance reasons
     * the index checks will be performed using just {@code reserveBytes}.<br>
     * This method returns the actual number of bytes written.
     */

    /**
     * 使用<a href="https://en.wikipedia.org/wiki/UTF-8">UTF-8</a>编码对{@link CharSequence}进行编码，并将其写入{@link ByteBuf}的{@code reserveBytes}中。
     * <p>
     * {@code reserveBytes}必须预先计算（例如使用{@link #utf8MaxBytes(CharSequence)}进行预计算或使用{@link #utf8Bytes(CharSequence)}精确计算），以确保此方法不会失败：出于性能原因，索引检查将仅使用{@code reserveBytes}进行。<br>
     * 此方法返回实际写入的字节数。
     */
    public static int reserveAndWriteUtf8(ByteBuf buf, CharSequence seq, int reserveBytes) {
        return reserveAndWriteUtf8Seq(buf, seq, 0, seq.length(), reserveBytes);
    }

    /**
     * Equivalent to <code>{@link #reserveAndWriteUtf8(ByteBuf, CharSequence, int)
     * reserveAndWriteUtf8(buf, seq.subSequence(start, end), reserveBytes)}</code> but avoids
     * subsequence object allocation if possible.
     *
     * @return actual number of bytes written
     */

    /**
     * 等效于 <code>{@link #reserveAndWriteUtf8(ByteBuf, CharSequence, int)
     * reserveAndWriteUtf8(buf, seq.subSequence(start, end), reserveBytes)}</code> 但尽可能避免
     * 子序列对象分配。
     *
     * @return 实际写入的字节数
     */
    public static int reserveAndWriteUtf8(ByteBuf buf, CharSequence seq, int start, int end, int reserveBytes) {
        return reserveAndWriteUtf8Seq(buf, checkCharSequenceBounds(seq, start, end), start, end, reserveBytes);
    }

    private static int reserveAndWriteUtf8Seq(ByteBuf buf, CharSequence seq, int start, int end, int reserveBytes) {
        for (;;) {
            if (buf instanceof WrappedCompositeByteBuf) {
                // WrappedCompositeByteBuf is a sub-class of AbstractByteBuf so it needs special handling.
                // WrappedCompositeByteBuf 是 AbstractByteBuf 的子类，因此需要特殊处理。
                buf = buf.unwrap();
            } else if (buf instanceof AbstractByteBuf) {
                AbstractByteBuf byteBuf = (AbstractByteBuf) buf;
                byteBuf.ensureWritable0(reserveBytes);
                int written = writeUtf8(byteBuf, byteBuf.writerIndex, reserveBytes, seq, start, end);
                byteBuf.writerIndex += written;
                return written;
            } else if (buf instanceof WrappedByteBuf) {
                // Unwrap as the wrapped buffer may be an AbstractByteBuf and so we can use fast-path.
                // 解包，因为包装的缓冲区可能是 AbstractByteBuf，所以我们可以使用快速路径。
                buf = buf.unwrap();
            } else {
                byte[] bytes = seq.subSequence(start, end).toString().getBytes(CharsetUtil.UTF_8);
                buf.writeBytes(bytes);
                return bytes.length;
            }
        }
    }

    static int writeUtf8(AbstractByteBuf buffer, int writerIndex, int reservedBytes, CharSequence seq, int len) {
        return writeUtf8(buffer, writerIndex, reservedBytes, seq, 0, len);
    }

    // Fast-Path implementation

    // 快速路径实现
    static int writeUtf8(AbstractByteBuf buffer, int writerIndex, int reservedBytes,
                         CharSequence seq, int start, int end) {
        if (seq instanceof AsciiString) {
            writeAsciiString(buffer, writerIndex, (AsciiString) seq, start, end);
            return end - start;
        }
        if (PlatformDependent.hasUnsafe()) {
            if (buffer.hasArray()) {
                return unsafeWriteUtf8(buffer.array(), PlatformDependent.byteArrayBaseOffset(),
                                       buffer.arrayOffset() + writerIndex, seq, start, end);
            }
            if (buffer.hasMemoryAddress()) {
                return unsafeWriteUtf8(null, buffer.memoryAddress(), writerIndex, seq, start, end);
            }
        } else {
            if (buffer.hasArray()) {
                return safeArrayWriteUtf8(buffer.array(), buffer.arrayOffset() + writerIndex, seq, start, end);
            }
            if (buffer.isDirect()) {
                assert buffer.nioBufferCount() == 1;
                final ByteBuffer internalDirectBuffer = buffer.internalNioBuffer(writerIndex, reservedBytes);
                final int bufferPosition = internalDirectBuffer.position();
                return safeDirectWriteUtf8(internalDirectBuffer, bufferPosition, seq, start, end);
            }
        }
        return safeWriteUtf8(buffer, writerIndex, seq, start, end);
    }

    // AsciiString Fast-Path implementation - no explicit bound-checks

    // AsciiString 快速路径实现 - 无显式边界检查
    static void writeAsciiString(AbstractByteBuf buffer, int writerIndex, AsciiString seq, int start, int end) {
        final int begin = seq.arrayOffset() + start;
        final int length = end - start;
        if (PlatformDependent.hasUnsafe()) {
            if (buffer.hasArray()) {
                PlatformDependent.copyMemory(seq.array(), begin,
                                             buffer.array(), buffer.arrayOffset() + writerIndex, length);
                return;
            }
            if (buffer.hasMemoryAddress()) {
                PlatformDependent.copyMemory(seq.array(), begin, buffer.memoryAddress() + writerIndex, length);
                return;
            }
        }
        if (buffer.hasArray()) {
            System.arraycopy(seq.array(), begin, buffer.array(), buffer.arrayOffset() + writerIndex, length);
            return;
        }
        buffer.setBytes(writerIndex, seq.array(), begin, length);
    }

    // Safe off-heap Fast-Path implementation

    // 安全的堆外快速路径实现
    private static int safeDirectWriteUtf8(ByteBuffer buffer, int writerIndex, CharSequence seq, int start, int end) {
        assert !(seq instanceof AsciiString);
        int oldWriterIndex = writerIndex;

        // We can use the _set methods as these not need to do any index checks and reference checks.

        // 我们可以使用 _set 方法，因为这些方法不需要进行任何索引检查和引用检查。
        // This is possible as we called ensureWritable(...) before.
        // 这是可能的，因为我们在之前调用了ensureWritable(...)。
        for (int i = start; i < end; i++) {
            char c = seq.charAt(i);
            if (c < 0x80) {
                buffer.put(writerIndex++, (byte) c);
            } else if (c < 0x800) {
                buffer.put(writerIndex++, (byte) (0xc0 | (c >> 6)));
                buffer.put(writerIndex++, (byte) (0x80 | (c & 0x3f)));
            } else if (isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    buffer.put(writerIndex++, WRITE_UTF_UNKNOWN);
                    continue;
                }
                // Surrogate Pair consumes 2 characters.
                // 代理对占用2个字符。
                if (++i == end) {
                    buffer.put(writerIndex++, WRITE_UTF_UNKNOWN);
                    break;
                }
                // Extra method is copied here to NOT allow inlining of writeUtf8
                // 此处复制了额外的方法以防止writeUtf8的内联
                // and increase the chance to inline CharSequence::charAt instead
                // 并增加内联 CharSequence::charAt 的机会
                char c2 = seq.charAt(i);
                if (!Character.isLowSurrogate(c2)) {
                    buffer.put(writerIndex++, WRITE_UTF_UNKNOWN);
                    buffer.put(writerIndex++, Character.isHighSurrogate(c2)? WRITE_UTF_UNKNOWN : (byte) c2);
                } else {
                    int codePoint = Character.toCodePoint(c, c2);
                    // See https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630.
                    // 参见 https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630。
                    buffer.put(writerIndex++, (byte) (0xf0 | (codePoint >> 18)));
                    buffer.put(writerIndex++, (byte) (0x80 | ((codePoint >> 12) & 0x3f)));
                    buffer.put(writerIndex++, (byte) (0x80 | ((codePoint >> 6) & 0x3f)));
                    buffer.put(writerIndex++, (byte) (0x80 | (codePoint & 0x3f)));
                }
            } else {
                buffer.put(writerIndex++, (byte) (0xe0 | (c >> 12)));
                buffer.put(writerIndex++, (byte) (0x80 | ((c >> 6) & 0x3f)));
                buffer.put(writerIndex++, (byte) (0x80 | (c & 0x3f)));
            }
        }
        return writerIndex - oldWriterIndex;
    }

    // Safe off-heap Fast-Path implementation

    // 安全的堆外快速路径实现
    private static int safeWriteUtf8(AbstractByteBuf buffer, int writerIndex, CharSequence seq, int start, int end) {
        assert !(seq instanceof AsciiString);
        int oldWriterIndex = writerIndex;

        // We can use the _set methods as these not need to do any index checks and reference checks.

        // 我们可以使用 _set 方法，因为这些方法不需要进行任何索引检查和引用检查。
        // This is possible as we called ensureWritable(...) before.
        // 这是可能的，因为我们在之前调用了ensureWritable(...)。
        for (int i = start; i < end; i++) {
            char c = seq.charAt(i);
            if (c < 0x80) {
                buffer._setByte(writerIndex++, (byte) c);
            } else if (c < 0x800) {
                buffer._setByte(writerIndex++, (byte) (0xc0 | (c >> 6)));
                buffer._setByte(writerIndex++, (byte) (0x80 | (c & 0x3f)));
            } else if (isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    buffer._setByte(writerIndex++, WRITE_UTF_UNKNOWN);
                    continue;
                }
                // Surrogate Pair consumes 2 characters.
                // 代理对占用2个字符。
                if (++i == end) {
                    buffer._setByte(writerIndex++, WRITE_UTF_UNKNOWN);
                    break;
                }
                // Extra method is copied here to NOT allow inlining of writeUtf8
                // 此处复制了额外的方法以防止writeUtf8的内联
                // and increase the chance to inline CharSequence::charAt instead
                // 并增加内联 CharSequence::charAt 的机会
                char c2 = seq.charAt(i);
                if (!Character.isLowSurrogate(c2)) {
                    buffer._setByte(writerIndex++, WRITE_UTF_UNKNOWN);
                    buffer._setByte(writerIndex++, Character.isHighSurrogate(c2)? WRITE_UTF_UNKNOWN : c2);
                } else {
                    int codePoint = Character.toCodePoint(c, c2);
                    // See https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630.
                    // 参见 https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630。
                    buffer._setByte(writerIndex++, (byte) (0xf0 | (codePoint >> 18)));
                    buffer._setByte(writerIndex++, (byte) (0x80 | ((codePoint >> 12) & 0x3f)));
                    buffer._setByte(writerIndex++, (byte) (0x80 | ((codePoint >> 6) & 0x3f)));
                    buffer._setByte(writerIndex++, (byte) (0x80 | (codePoint & 0x3f)));
                }
            } else {
                buffer._setByte(writerIndex++, (byte) (0xe0 | (c >> 12)));
                buffer._setByte(writerIndex++, (byte) (0x80 | ((c >> 6) & 0x3f)));
                buffer._setByte(writerIndex++, (byte) (0x80 | (c & 0x3f)));
            }
        }
        return writerIndex - oldWriterIndex;
    }

    // safe byte[] Fast-Path implementation

    // 安全字节数组快速路径实现
    private static int safeArrayWriteUtf8(byte[] buffer, int writerIndex, CharSequence seq, int start, int end) {
        int oldWriterIndex = writerIndex;
        for (int i = start; i < end; i++) {
            char c = seq.charAt(i);
            if (c < 0x80) {
                buffer[writerIndex++] = (byte) c;
            } else if (c < 0x800) {
                buffer[writerIndex++] = (byte) (0xc0 | (c >> 6));
                buffer[writerIndex++] = (byte) (0x80 | (c & 0x3f));
            } else if (isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    buffer[writerIndex++] = WRITE_UTF_UNKNOWN;
                    continue;
                }
                // Surrogate Pair consumes 2 characters.
                // 代理对占用2个字符。
                if (++i == end) {
                    buffer[writerIndex++] = WRITE_UTF_UNKNOWN;
                    break;
                }
                char c2 = seq.charAt(i);
                // Extra method is copied here to NOT allow inlining of writeUtf8
                // 此处复制了额外的方法以防止writeUtf8的内联
                // and increase the chance to inline CharSequence::charAt instead
                // 并增加内联 CharSequence::charAt 的机会
                if (!Character.isLowSurrogate(c2)) {
                    buffer[writerIndex++] = WRITE_UTF_UNKNOWN;
                    buffer[writerIndex++] = (byte) (Character.isHighSurrogate(c2)? WRITE_UTF_UNKNOWN : c2);
                } else {
                    int codePoint = Character.toCodePoint(c, c2);
                    // See https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630.
                    // 参见 https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630。
                    buffer[writerIndex++] = (byte) (0xf0 | (codePoint >> 18));
                    buffer[writerIndex++] = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
                    buffer[writerIndex++] = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
                    buffer[writerIndex++] = (byte) (0x80 | (codePoint & 0x3f));
                }
            } else {
                buffer[writerIndex++] = (byte) (0xe0 | (c >> 12));
                buffer[writerIndex++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                buffer[writerIndex++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        return writerIndex - oldWriterIndex;
    }

    // unsafe Fast-Path implementation

    // 不安全的快速路径实现
    private static int unsafeWriteUtf8(byte[] buffer, long memoryOffset, int writerIndex,
                                       CharSequence seq, int start, int end) {
        assert !(seq instanceof AsciiString);
        long writerOffset = memoryOffset + writerIndex;
        final long oldWriterOffset = writerOffset;
        for (int i = start; i < end; i++) {
            char c = seq.charAt(i);
            if (c < 0x80) {
                PlatformDependent.putByte(buffer, writerOffset++, (byte) c);
            } else if (c < 0x800) {
                PlatformDependent.putByte(buffer, writerOffset++, (byte) (0xc0 | (c >> 6)));
                PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | (c & 0x3f)));
            } else if (isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    PlatformDependent.putByte(buffer, writerOffset++, WRITE_UTF_UNKNOWN);
                    continue;
                }
                // Surrogate Pair consumes 2 characters.
                // 代理对占用2个字符。
                if (++i == end) {
                    PlatformDependent.putByte(buffer, writerOffset++, WRITE_UTF_UNKNOWN);
                    break;
                }
                char c2 = seq.charAt(i);
                // Extra method is copied here to NOT allow inlining of writeUtf8
                // 此处复制了额外的方法以防止writeUtf8的内联
                // and increase the chance to inline CharSequence::charAt instead
                // 并增加内联 CharSequence::charAt 的机会
                if (!Character.isLowSurrogate(c2)) {
                    PlatformDependent.putByte(buffer, writerOffset++, WRITE_UTF_UNKNOWN);
                    PlatformDependent.putByte(buffer, writerOffset++,
                                              (byte) (Character.isHighSurrogate(c2)? WRITE_UTF_UNKNOWN : c2));
                } else {
                    int codePoint = Character.toCodePoint(c, c2);
                    // See https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630.
                    // 参见 https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630。
                    PlatformDependent.putByte(buffer, writerOffset++, (byte) (0xf0 | (codePoint >> 18)));
                    PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | ((codePoint >> 12) & 0x3f)));
                    PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | ((codePoint >> 6) & 0x3f)));
                    PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | (codePoint & 0x3f)));
                }
            } else {
                PlatformDependent.putByte(buffer, writerOffset++, (byte) (0xe0 | (c >> 12)));
                PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | ((c >> 6) & 0x3f)));
                PlatformDependent.putByte(buffer, writerOffset++, (byte) (0x80 | (c & 0x3f)));
            }
        }
        return (int) (writerOffset - oldWriterOffset);
    }

    /**
     * Returns max bytes length of UTF8 character sequence of the given length.
     */

    /**
     * 返回给定长度的UTF8字符序列的最大字节长度。
     */
    public static int utf8MaxBytes(final int seqLength) {
        return seqLength * MAX_BYTES_PER_CHAR_UTF8;
    }

    /**
     * Returns max bytes length of UTF8 character sequence.
     * <p>
     * It behaves like {@link #utf8MaxBytes(int)} applied to {@code seq} {@link CharSequence#length()}.
     */

    /**
     * 返回UTF8字符序列的最大字节长度。
     * <p>
     * 其行为类似于将 {@link #utf8MaxBytes(int)} 应用于 {@code seq} 的 {@link CharSequence#length()}。
     */
    public static int utf8MaxBytes(CharSequence seq) {
        return utf8MaxBytes(seq.length());
    }

    /**
     * Returns the exact bytes length of UTF8 character sequence.
     * <p>
     * This method is producing the exact length according to {@link #writeUtf8(ByteBuf, CharSequence)}.
     */

    /**
     * 返回UTF8字符序列的精确字节长度。
     * <p>
     * 此方法根据{@link #writeUtf8(ByteBuf, CharSequence)}生成精确长度。
     */
    public static int utf8Bytes(final CharSequence seq) {
        return utf8ByteCount(seq, 0, seq.length());
    }

    /**
     * Equivalent to <code>{@link #utf8Bytes(CharSequence) utf8Bytes(seq.subSequence(start, end))}</code>
     * but avoids subsequence object allocation.
     * <p>
     * This method is producing the exact length according to {@link #writeUtf8(ByteBuf, CharSequence, int, int)}.
     */

    /**
     * 等价于 <code>{@link #utf8Bytes(CharSequence) utf8Bytes(seq.subSequence(start, end))}</code>
     * 但避免了子序列对象的分配。
     * <p>
     * 此方法根据 {@link #writeUtf8(ByteBuf, CharSequence, int, int)} 生成精确的长度。
     */
    public static int utf8Bytes(final CharSequence seq, int start, int end) {
        return utf8ByteCount(checkCharSequenceBounds(seq, start, end), start, end);
    }

    private static int utf8ByteCount(final CharSequence seq, int start, int end) {
        if (seq instanceof AsciiString) {
            return end - start;
        }
        int i = start;
        // ASCII fast path
        // ASCII 快速路径
        while (i < end && seq.charAt(i) < 0x80) {
            ++i;
        }
        // !ASCII is packed in a separate method to let the ASCII case be smaller
        // !ASCII被打包在一个单独的方法中，以使ASCII情况更小
        return i < end ? (i - start) + utf8BytesNonAscii(seq, i, end) : i - start;
    }

    private static int utf8BytesNonAscii(final CharSequence seq, final int start, final int end) {
        int encodedLength = 0;
        for (int i = start; i < end; i++) {
            final char c = seq.charAt(i);
            // making it 100% branchless isn't rewarding due to the many bit operations necessary!
            // 由于需要进行许多位操作，使其完全无分支并不值得！
            if (c < 0x800) {
                // branchless version of: (c <= 127 ? 0:1) + 1
                // 无分支版本的: (c <= 127 ? 0:1) + 1
                encodedLength += ((0x7f - c) >>> 31) + 1;
            } else if (isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    encodedLength++;
                    // WRITE_UTF_UNKNOWN
                    // WRITE_UTF_UNKNOWN
                    continue;
                }
                // Surrogate Pair consumes 2 characters.
                // 代理对占用2个字符。
                if (++i == end) {
                    encodedLength++;
                    // WRITE_UTF_UNKNOWN
                    // WRITE_UTF_UNKNOWN
                    break;
                }
                if (!Character.isLowSurrogate(seq.charAt(i))) {
                    // WRITE_UTF_UNKNOWN + (Character.isHighSurrogate(c2) ? WRITE_UTF_UNKNOWN : c2)
                    // WRITE_UTF_UNKNOWN + (Character.isHighSurrogate(c2) ? WRITE_UTF_UNKNOWN : c2)
                    encodedLength += 2;
                    continue;
                }
                // See https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630.
                // 参见 https://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630。
                encodedLength += 4;
            } else {
                encodedLength += 3;
            }
        }
        return encodedLength;
    }

    /**
     * Encode a {@link CharSequence} in <a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> and write
     * it to a {@link ByteBuf} allocated with {@code alloc}.
     * @param alloc The allocator used to allocate a new {@link ByteBuf}.
     * @param seq The characters to write into a buffer.
     * @return The {@link ByteBuf} which contains the <a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> encoded
     * result.
     */

    /**
     * 将{@link CharSequence}编码为<a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a>并写入
     * 使用{@code alloc}分配的{@link ByteBuf}。
     * @param alloc 用于分配新{@link ByteBuf}的分配器。
     * @param seq 要写入缓冲区的字符。
     * @return 包含<a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a>编码结果的{@link ByteBuf}。
     */
    public static ByteBuf writeAscii(ByteBufAllocator alloc, CharSequence seq) {
        // ASCII uses 1 byte per char
        // ASCII uses 1 byte per char
        ByteBuf buf = alloc.buffer(seq.length());
        writeAscii(buf, seq);
        return buf;
    }

    /**
     * Encode a {@link CharSequence} in <a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> and write it
     * to a {@link ByteBuf}.
     *
     * This method returns the actual number of bytes written.
     */

    /**
     * 将 {@link CharSequence} 编码为 <a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> 并写入
     * {@link ByteBuf}。
     *
     * 此方法返回实际写入的字节数。
     */
    public static int writeAscii(ByteBuf buf, CharSequence seq) {
        // ASCII uses 1 byte per char
        // ASCII uses 1 byte per char
        for (;;) {
            if (buf instanceof WrappedCompositeByteBuf) {
                // WrappedCompositeByteBuf is a sub-class of AbstractByteBuf so it needs special handling.
                // WrappedCompositeByteBuf 是 AbstractByteBuf 的子类，因此需要特殊处理。
                buf = buf.unwrap();
            } else if (buf instanceof AbstractByteBuf) {
                final int len = seq.length();
                AbstractByteBuf byteBuf = (AbstractByteBuf) buf;
                byteBuf.ensureWritable0(len);
                if (seq instanceof AsciiString) {
                    writeAsciiString(byteBuf, byteBuf.writerIndex, (AsciiString) seq, 0, len);
                } else {
                    final int written = writeAscii(byteBuf, byteBuf.writerIndex, seq, len);
                    assert written == len;
                }
                byteBuf.writerIndex += len;
                return len;
            } else if (buf instanceof WrappedByteBuf) {
                // Unwrap as the wrapped buffer may be an AbstractByteBuf and so we can use fast-path.
                // 解包，因为包装的缓冲区可能是 AbstractByteBuf，所以我们可以使用快速路径。
                buf = buf.unwrap();
            } else {
                byte[] bytes = seq.toString().getBytes(CharsetUtil.US_ASCII);
                buf.writeBytes(bytes);
                return bytes.length;
            }
        }
    }

    // Fast-Path implementation

    // 快速路径实现
    static int writeAscii(AbstractByteBuf buffer, int writerIndex, CharSequence seq, int len) {

        // We can use the _set methods as these not need to do any index checks and reference checks.

        // 我们可以使用 _set 方法，因为这些方法不需要进行任何索引检查和引用检查。
        // This is possible as we called ensureWritable(...) before.
        // 这是可能的，因为我们在之前调用了ensureWritable(...)。
        for (int i = 0; i < len; i++) {
            buffer._setByte(writerIndex++, AsciiString.c2b(seq.charAt(i)));
        }
        return len;
    }

    /**
     * Encode the given {@link CharBuffer} using the given {@link Charset} into a new {@link ByteBuf} which
     * is allocated via the {@link ByteBufAllocator}.
     */

    /**
     * 使用给定的 {@link Charset} 将指定的 {@link CharBuffer} 编码为一个新的 {@link ByteBuf}，该 {@link ByteBuf} 通过 {@link ByteBufAllocator} 分配。
     */
    public static ByteBuf encodeString(ByteBufAllocator alloc, CharBuffer src, Charset charset) {
        return encodeString0(alloc, false, src, charset, 0);
    }

    /**
     * Encode the given {@link CharBuffer} using the given {@link Charset} into a new {@link ByteBuf} which
     * is allocated via the {@link ByteBufAllocator}.
     *
     * @param alloc The {@link ByteBufAllocator} to allocate {@link ByteBuf}.
     * @param src The {@link CharBuffer} to encode.
     * @param charset The specified {@link Charset}.
     * @param extraCapacity the extra capacity to alloc except the space for decoding.
     */

    /**
     * 使用给定的 {@link Charset} 将指定的 {@link CharBuffer} 编码为一个新的 {@link ByteBuf}，
     * 该 {@link ByteBuf} 通过 {@link ByteBufAllocator} 进行分配。
     *
     * @param alloc 用于分配 {@link ByteBuf} 的 {@link ByteBufAllocator}。
     * @param src 要编码的 {@link CharBuffer}。
     * @param charset 指定的 {@link Charset}。
     * @param extraCapacity 除了解码所需空间外额外分配的容量。
     */
    public static ByteBuf encodeString(ByteBufAllocator alloc, CharBuffer src, Charset charset, int extraCapacity) {
        return encodeString0(alloc, false, src, charset, extraCapacity);
    }

    static ByteBuf encodeString0(ByteBufAllocator alloc, boolean enforceHeap, CharBuffer src, Charset charset,
                                 int extraCapacity) {
        final CharsetEncoder encoder = CharsetUtil.encoder(charset);
        int length = (int) ((double) src.remaining() * encoder.maxBytesPerChar()) + extraCapacity;
        boolean release = true;
        final ByteBuf dst;
        if (enforceHeap) {
            dst = alloc.heapBuffer(length);
        } else {
            dst = alloc.buffer(length);
        }
        try {
            final ByteBuffer dstBuf = dst.internalNioBuffer(dst.readerIndex(), length);
            final int pos = dstBuf.position();
            CoderResult cr = encoder.encode(src, dstBuf, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = encoder.flush(dstBuf);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            dst.writerIndex(dst.writerIndex() + dstBuf.position() - pos);
            release = false;
            return dst;
        } catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        } finally {
            if (release) {
                dst.release();
            }
        }
    }

    @SuppressWarnings("deprecation")
    static String decodeString(ByteBuf src, int readerIndex, int len, Charset charset) {
        if (len == 0) {
            return StringUtil.EMPTY_STRING;
        }
        final byte[] array;
        final int offset;

        if (src.hasArray()) {
            array = src.array();
            offset = src.arrayOffset() + readerIndex;
        } else {
            array = threadLocalTempArray(len);
            offset = 0;
            src.getBytes(readerIndex, array, 0, len);
        }
        if (CharsetUtil.US_ASCII.equals(charset)) {
            // Fast-path for US-ASCII which is used frequently.
            // 为频繁使用的 US-ASCII 提供快速路径。
            return new String(array, 0, offset, len);
        }
        return new String(array, offset, len, charset);
    }

    /**
     * Returns a cached thread-local direct buffer, if available.
     *
     * @return a cached thread-local direct buffer, if available.  {@code null} otherwise.
     */

    /**
     * 返回一个缓存的线程本地直接缓冲区，如果可用。
     *
     * @return 一个缓存的线程本地直接缓冲区，如果可用。否则返回 {@code null}。
     */
    public static ByteBuf threadLocalDirectBuffer() {
        if (THREAD_LOCAL_BUFFER_SIZE <= 0) {
            return null;
        }

        if (PlatformDependent.hasUnsafe()) {
            return ThreadLocalUnsafeDirectByteBuf.newInstance();
        } else {
            return ThreadLocalDirectByteBuf.newInstance();
        }
    }

    /**
     * Create a copy of the underlying storage from {@code buf} into a byte array.
     * The copy will start at {@link ByteBuf#readerIndex()} and copy {@link ByteBuf#readableBytes()} bytes.
     */

    /**
     * 将 {@code buf} 中的底层存储复制到一个字节数组中。
     * 复制将从 {@link ByteBuf#readerIndex()} 开始，并复制 {@link ByteBuf#readableBytes()} 个字节。
     */
    public static byte[] getBytes(ByteBuf buf) {
        return getBytes(buf,  buf.readerIndex(), buf.readableBytes());
    }

    /**
     * Create a copy of the underlying storage from {@code buf} into a byte array.
     * The copy will start at {@code start} and copy {@code length} bytes.
     */

    /**
     * 从 {@code buf} 的底层存储中创建一个字节数组的副本。
     * 复制将从 {@code start} 开始，复制 {@code length} 个字节。
     */
    public static byte[] getBytes(ByteBuf buf, int start, int length) {
        return getBytes(buf, start, length, true);
    }

    /**
     * Return an array of the underlying storage from {@code buf} into a byte array.
     * The copy will start at {@code start} and copy {@code length} bytes.
     * If {@code copy} is true a copy will be made of the memory.
     * If {@code copy} is false the underlying storage will be shared, if possible.
     */

    /**
     * 从 {@code buf} 的基础存储中返回一个字节数组。
     * 复制将从 {@code start} 开始，复制 {@code length} 个字节。
     * 如果 {@code copy} 为 true，将创建内存的副本。
     * 如果 {@code copy} 为 false，将尽可能共享基础存储。
     */
    public static byte[] getBytes(ByteBuf buf, int start, int length, boolean copy) {
        int capacity = buf.capacity();
        if (isOutOfBounds(start, length, capacity)) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= start + length(" + length
                    + ") <= " + "buf.capacity(" + capacity + ')');
        }

        if (buf.hasArray()) {
            int baseOffset = buf.arrayOffset() + start;
            byte[] bytes = buf.array();
            if (copy || baseOffset != 0 || length != bytes.length) {
                return Arrays.copyOfRange(bytes, baseOffset, baseOffset + length);
            } else {
                return bytes;
            }
        }

        byte[] bytes = PlatformDependent.allocateUninitializedArray(length);
        buf.getBytes(start, bytes);
        return bytes;
    }

    /**
     * Copies the all content of {@code src} to a {@link ByteBuf} using {@link ByteBuf#writeBytes(byte[], int, int)}.
     *
     * @param src the source string to copy
     * @param dst the destination buffer
     */

    /**
     * 使用 {@link ByteBuf#writeBytes(byte[], int, int)} 将 {@code src} 的所有内容复制到 {@link ByteBuf} 中。
     *
     * @param src 要复制的源字符串
     * @param dst 目标缓冲区
     */
    public static void copy(AsciiString src, ByteBuf dst) {
        copy(src, 0, dst, src.length());
    }

    /**
     * Copies the content of {@code src} to a {@link ByteBuf} using {@link ByteBuf#setBytes(int, byte[], int, int)}.
     * Unlike the {@link #copy(AsciiString, ByteBuf)} and {@link #copy(AsciiString, int, ByteBuf, int)} methods,
     * this method do not increase a {@code writerIndex} of {@code dst} buffer.
     *
     * @param src the source string to copy
     * @param srcIdx the starting offset of characters to copy
     * @param dst the destination buffer
     * @param dstIdx the starting offset in the destination buffer
     * @param length the number of characters to copy
     */

    /**
     * 使用 {@link ByteBuf#setBytes(int, byte[], int, int)} 将 {@code src} 的内容复制到 {@link ByteBuf} 中。
     * 与 {@link #copy(AsciiString, ByteBuf)} 和 {@link #copy(AsciiString, int, ByteBuf, int)} 方法不同，
     * 此方法不会增加 {@code dst} 缓冲区的 {@code writerIndex}。
     *
     * @param src 要复制的源字符串
     * @param srcIdx 要复制的字符的起始偏移量
     * @param dst 目标缓冲区
     * @param dstIdx 目标缓冲区中的起始偏移量
     * @param length 要复制的字符数
     */
    public static void copy(AsciiString src, int srcIdx, ByteBuf dst, int dstIdx, int length) {
        if (isOutOfBounds(srcIdx, length, src.length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= srcIdx(" + srcIdx + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + src.length() + ')');
        }

        checkNotNull(dst, "dst").setBytes(dstIdx, src.array(), srcIdx + src.arrayOffset(), length);
    }

    /**
     * Copies the content of {@code src} to a {@link ByteBuf} using {@link ByteBuf#writeBytes(byte[], int, int)}.
     *
     * @param src the source string to copy
     * @param srcIdx the starting offset of characters to copy
     * @param dst the destination buffer
     * @param length the number of characters to copy
     */

    /**
     * 使用 {@link ByteBuf#writeBytes(byte[], int, int)} 将 {@code src} 的内容复制到 {@link ByteBuf} 中。
     *
     * @param src 要复制的源字符串
     * @param srcIdx 要复制的字符的起始偏移量
     * @param dst 目标缓冲区
     * @param length 要复制的字符数
     */
    public static void copy(AsciiString src, int srcIdx, ByteBuf dst, int length) {
        if (isOutOfBounds(srcIdx, length, src.length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= srcIdx(" + srcIdx + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + src.length() + ')');
        }

        checkNotNull(dst, "dst").writeBytes(src.array(), srcIdx + src.arrayOffset(), length);
    }

    /**
     * Returns a multi-line hexadecimal dump of the specified {@link ByteBuf} that is easy to read by humans.
     */

    /**
     * 返回指定 {@link ByteBuf} 的多行十六进制转储，易于人类阅读。
     */
    public static String prettyHexDump(ByteBuf buffer) {
        return prettyHexDump(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    /**
     * Returns a multi-line hexadecimal dump of the specified {@link ByteBuf} that is easy to read by humans,
     * starting at the given {@code offset} using the given {@code length}.
     */

    /**
     * 返回指定 {@link ByteBuf} 的多行十六进制转储，便于人类阅读，
     * 从给定的 {@code offset} 开始，使用给定的 {@code length}。
     */
    public static String prettyHexDump(ByteBuf buffer, int offset, int length) {
        return HexUtil.prettyHexDump(buffer, offset, length);
    }

    /**
     * Appends the prettified multi-line hexadecimal dump of the specified {@link ByteBuf} to the specified
     * {@link StringBuilder} that is easy to read by humans.
     */

    /**
     * 将指定 {@link ByteBuf} 的格式化多行十六进制转储附加到指定的 {@link StringBuilder} 中，使其易于人类阅读。
     */
    public static void appendPrettyHexDump(StringBuilder dump, ByteBuf buf) {
        appendPrettyHexDump(dump, buf, buf.readerIndex(), buf.readableBytes());
    }

    /**
     * Appends the prettified multi-line hexadecimal dump of the specified {@link ByteBuf} to the specified
     * {@link StringBuilder} that is easy to read by humans, starting at the given {@code offset} using
     * the given {@code length}.
     */

    /**
     * 将指定 {@link ByteBuf} 的格式化多行十六进制转储追加到指定的 {@link StringBuilder} 中，便于人类阅读，
     * 从给定的 {@code offset} 开始，使用给定的 {@code length}。
     */
    public static void appendPrettyHexDump(StringBuilder dump, ByteBuf buf, int offset, int length) {
        HexUtil.appendPrettyHexDump(dump, buf, offset, length);
    }

    /* Separate class so that the expensive static initialization is only done when needed */

    /* 单独的类，以便仅在需要时执行昂贵的静态初始化 */
    private static final class HexUtil {

        private static final char[] BYTE2CHAR = new char[256];
        private static final char[] HEXDUMP_TABLE = new char[256 * 4];
        private static final String[] HEXPADDING = new String[16];
        private static final String[] HEXDUMP_ROWPREFIXES = new String[65536 >>> 4];
        private static final String[] BYTE2HEX = new String[256];
        private static final String[] BYTEPADDING = new String[16];

        static {
            final char[] DIGITS = "0123456789abcdef".toCharArray();
            for (int i = 0; i < 256; i ++) {
                HEXDUMP_TABLE[ i << 1     ] = DIGITS[i >>> 4 & 0x0F];
                HEXDUMP_TABLE[(i << 1) + 1] = DIGITS[i       & 0x0F];
            }

            int i;

            // Generate the lookup table for hex dump paddings

            // 生成十六进制转储填充的查找表
            for (i = 0; i < HEXPADDING.length; i ++) {
                int padding = HEXPADDING.length - i;
                StringBuilder buf = new StringBuilder(padding * 3);
                for (int j = 0; j < padding; j ++) {
                    buf.append("   ");
                }
                HEXPADDING[i] = buf.toString();
            }

            // Generate the lookup table for the start-offset header in each row (up to 64KiB).

            // 为每行的起始偏移量头生成查找表（最多64KiB）。
            for (i = 0; i < HEXDUMP_ROWPREFIXES.length; i ++) {
                StringBuilder buf = new StringBuilder(12);
                buf.append(NEWLINE);
                buf.append(Long.toHexString(i << 4 & 0xFFFFFFFFL | 0x100000000L));
                buf.setCharAt(buf.length() - 9, '|');
                buf.append('|');
                HEXDUMP_ROWPREFIXES[i] = buf.toString();
            }

            // Generate the lookup table for byte-to-hex-dump conversion

            // 生成字节到十六进制转储的查找表
            for (i = 0; i < BYTE2HEX.length; i ++) {
                BYTE2HEX[i] = ' ' + StringUtil.byteToHexStringPadded(i);
            }

            // Generate the lookup table for byte dump paddings

            // 生成字节转储填充的查找表
            for (i = 0; i < BYTEPADDING.length; i ++) {
                int padding = BYTEPADDING.length - i;
                StringBuilder buf = new StringBuilder(padding);
                for (int j = 0; j < padding; j ++) {
                    buf.append(' ');
                }
                BYTEPADDING[i] = buf.toString();
            }

            // Generate the lookup table for byte-to-char conversion

            // 生成字节到字符转换的查找表
            for (i = 0; i < BYTE2CHAR.length; i ++) {
                if (i <= 0x1f || i >= 0x7f) {
                    BYTE2CHAR[i] = '.';
                } else {
                    BYTE2CHAR[i] = (char) i;
                }
            }
        }

        private static String hexDump(ByteBuf buffer, int fromIndex, int length) {
            checkPositiveOrZero(length, "length");
            if (length == 0) {
              return "";
            }

            int endIndex = fromIndex + length;
            char[] buf = new char[length << 1];

            int srcIdx = fromIndex;
            int dstIdx = 0;
            for (; srcIdx < endIndex; srcIdx ++, dstIdx += 2) {
              System.arraycopy(
                  HEXDUMP_TABLE, buffer.getUnsignedByte(srcIdx) << 1,
                  buf, dstIdx, 2);
            }

            return new String(buf);
        }

        private static String hexDump(byte[] array, int fromIndex, int length) {
            checkPositiveOrZero(length, "length");
            if (length == 0) {
                return "";
            }

            int endIndex = fromIndex + length;
            char[] buf = new char[length << 1];

            int srcIdx = fromIndex;
            int dstIdx = 0;
            for (; srcIdx < endIndex; srcIdx ++, dstIdx += 2) {
                System.arraycopy(
                    HEXDUMP_TABLE, (array[srcIdx] & 0xFF) << 1,
                    buf, dstIdx, 2);
            }

            return new String(buf);
        }

        private static String prettyHexDump(ByteBuf buffer, int offset, int length) {
            if (length == 0) {
              return StringUtil.EMPTY_STRING;
            } else {
                int rows = length / 16 + ((length & 15) == 0? 0 : 1) + 4;
                StringBuilder buf = new StringBuilder(rows * 80);
                appendPrettyHexDump(buf, buffer, offset, length);
                return buf.toString();
            }
        }

        private static void appendPrettyHexDump(StringBuilder dump, ByteBuf buf, int offset, int length) {
            if (isOutOfBounds(offset, length, buf.capacity())) {
                throw new IndexOutOfBoundsException(
                        "expected: " + "0 <= offset(" + offset + ") <= offset + length(" + length
                                                    + ") <= " + "buf.capacity(" + buf.capacity() + ')');
            }
            if (length == 0) {
                return;
            }
            dump.append(
                              "         +-------------------------------------------------+" +
                    NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" +
                    NEWLINE + "+--------+-------------------------------------------------+----------------+");

            final int fullRows = length >>> 4;
            final int remainder = length & 0xF;

            // Dump the rows which have 16 bytes.

            // 丢弃具有16字节的行。
            for (int row = 0; row < fullRows; row ++) {
                int rowStartIndex = (row << 4) + offset;

                // Per-row prefix.

                // 每行前缀。
                appendHexDumpRowPrefix(dump, row, rowStartIndex);

                // Hex dump

                // 十六进制转储
                int rowEndIndex = rowStartIndex + 16;
                for (int j = rowStartIndex; j < rowEndIndex; j ++) {
                    dump.append(BYTE2HEX[buf.getUnsignedByte(j)]);
                }
                dump.append(" |");

                // ASCII dump

                // ASCII转储
                for (int j = rowStartIndex; j < rowEndIndex; j ++) {
                    dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
                }
                dump.append('|');
            }

            // Dump the last row which has less than 16 bytes.

            // 丢弃最后一行，如果它少于16字节。
            if (remainder != 0) {
                int rowStartIndex = (fullRows << 4) + offset;
                appendHexDumpRowPrefix(dump, fullRows, rowStartIndex);

                // Hex dump

                // 十六进制转储
                int rowEndIndex = rowStartIndex + remainder;
                for (int j = rowStartIndex; j < rowEndIndex; j ++) {
                    dump.append(BYTE2HEX[buf.getUnsignedByte(j)]);
                }
                dump.append(HEXPADDING[remainder]);
                dump.append(" |");

                // Ascii dump
                for (int j = rowStartIndex; j < rowEndIndex; j ++) {
                    dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
                }
                dump.append(BYTEPADDING[remainder]);
                dump.append('|');
            }

            dump.append(NEWLINE +
                        "+--------+-------------------------------------------------+----------------+");
        }

        private static void appendHexDumpRowPrefix(StringBuilder dump, int row, int rowStartIndex) {
            if (row < HEXDUMP_ROWPREFIXES.length) {
                dump.append(HEXDUMP_ROWPREFIXES[row]);
            } else {
                dump.append(NEWLINE);
                dump.append(Long.toHexString(rowStartIndex & 0xFFFFFFFFL | 0x100000000L));
                dump.setCharAt(dump.length() - 9, '|');
                dump.append('|');
            }
        }
    }

    static final class ThreadLocalUnsafeDirectByteBuf extends UnpooledUnsafeDirectByteBuf {

        private static final ObjectPool<ThreadLocalUnsafeDirectByteBuf> RECYCLER =
                ObjectPool.newPool(new ObjectCreator<ThreadLocalUnsafeDirectByteBuf>() {
                    @Override
                    public ThreadLocalUnsafeDirectByteBuf newObject(Handle<ThreadLocalUnsafeDirectByteBuf> handle) {
                        return new ThreadLocalUnsafeDirectByteBuf(handle);
                    }
                });

        static ThreadLocalUnsafeDirectByteBuf newInstance() {
            ThreadLocalUnsafeDirectByteBuf buf = RECYCLER.get();
            buf.resetRefCnt();
            return buf;
        }

        private final Handle<ThreadLocalUnsafeDirectByteBuf> handle;

        private ThreadLocalUnsafeDirectByteBuf(Handle<ThreadLocalUnsafeDirectByteBuf> handle) {
            super(UnpooledByteBufAllocator.DEFAULT, 256, Integer.MAX_VALUE);
            this.handle = handle;
        }

        @Override
        protected void deallocate() {
            if (capacity() > THREAD_LOCAL_BUFFER_SIZE) {
                super.deallocate();
            } else {
                clear();
                handle.recycle(this);
            }
        }
    }

    static final class ThreadLocalDirectByteBuf extends UnpooledDirectByteBuf {

        private static final ObjectPool<ThreadLocalDirectByteBuf> RECYCLER = ObjectPool.newPool(
                new ObjectCreator<ThreadLocalDirectByteBuf>() {
            @Override
            public ThreadLocalDirectByteBuf newObject(Handle<ThreadLocalDirectByteBuf> handle) {
                return new ThreadLocalDirectByteBuf(handle);
            }
        });

        static ThreadLocalDirectByteBuf newInstance() {
            ThreadLocalDirectByteBuf buf = RECYCLER.get();
            buf.resetRefCnt();
            return buf;
        }

        private final Handle<ThreadLocalDirectByteBuf> handle;

        private ThreadLocalDirectByteBuf(Handle<ThreadLocalDirectByteBuf> handle) {
            super(UnpooledByteBufAllocator.DEFAULT, 256, Integer.MAX_VALUE);
            this.handle = handle;
        }

        @Override
        protected void deallocate() {
            if (capacity() > THREAD_LOCAL_BUFFER_SIZE) {
                super.deallocate();
            } else {
                clear();
                handle.recycle(this);
            }
        }
    }

    /**
     * Returns {@code true} if the given {@link ByteBuf} is valid text using the given {@link Charset},
     * otherwise return {@code false}.
     *
     * @param buf The given {@link ByteBuf}.
     * @param charset The specified {@link Charset}.
     */

    /**
     * 如果给定的 {@link ByteBuf} 使用指定的 {@link Charset} 是有效的文本，则返回 {@code true}，
     * 否则返回 {@code false}。
     *
     * @param buf 给定的 {@link ByteBuf}。
     * @param charset 指定的 {@link Charset}。
     */
    public static boolean isText(ByteBuf buf, Charset charset) {
        return isText(buf, buf.readerIndex(), buf.readableBytes(), charset);
    }

    /**
     * Returns {@code true} if the specified {@link ByteBuf} starting at {@code index} with {@code length} is valid
     * text using the given {@link Charset}, otherwise return {@code false}.
     *
     * @param buf The given {@link ByteBuf}.
     * @param index The start index of the specified buffer.
     * @param length The length of the specified buffer.
     * @param charset The specified {@link Charset}.
     *
     * @throws IndexOutOfBoundsException if {@code index} + {@code length} is greater than {@code buf.readableBytes}
     */

    /**
     * 如果从 {@code index} 开始、长度为 {@code length} 的指定 {@link ByteBuf} 使用给定的 {@link Charset} 是有效的文本，则返回 {@code true}，否则返回 {@code false}。
     *
     * @param buf 给定的 {@link ByteBuf}。
     * @param index 指定缓冲区的起始索引。
     * @param length 指定缓冲区的长度。
     * @param charset 指定的 {@link Charset}。
     *
     * @throws IndexOutOfBoundsException 如果 {@code index} + {@code length} 大于 {@code buf.readableBytes}
     */
    public static boolean isText(ByteBuf buf, int index, int length, Charset charset) {
        checkNotNull(buf, "buf");
        checkNotNull(charset, "charset");
        final int maxIndex = buf.readerIndex() + buf.readableBytes();
        if (index < 0 || length < 0 || index > maxIndex - length) {
            throw new IndexOutOfBoundsException("index: " + index + " length: " + length);
        }
        if (charset.equals(CharsetUtil.UTF_8)) {
            return isUtf8(buf, index, length);
        } else if (charset.equals(CharsetUtil.US_ASCII)) {
            return isAscii(buf, index, length);
        } else {
            CharsetDecoder decoder = CharsetUtil.decoder(charset, CodingErrorAction.REPORT, CodingErrorAction.REPORT);
            try {
                if (buf.nioBufferCount() == 1) {
                    decoder.decode(buf.nioBuffer(index, length));
                } else {
                    ByteBuf heapBuffer = buf.alloc().heapBuffer(length);
                    try {
                        heapBuffer.writeBytes(buf, index, length);
                        decoder.decode(heapBuffer.internalNioBuffer(heapBuffer.readerIndex(), length));
                    } finally {
                        heapBuffer.release();
                    }
                }
                return true;
            } catch (CharacterCodingException ignore) {
                return false;
            }
        }
    }

    /**
     * Aborts on a byte which is not a valid ASCII character.
     */

    /**
     * 在遇到非有效ASCII字符时中止。
     */
    private static final ByteProcessor FIND_NON_ASCII = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value >= 0;
        }
    };

    /**
     * Returns {@code true} if the specified {@link ByteBuf} starting at {@code index} with {@code length} is valid
     * ASCII text, otherwise return {@code false}.
     *
     * @param buf    The given {@link ByteBuf}.
     * @param index  The start index of the specified buffer.
     * @param length The length of the specified buffer.
     */

    /**
     * 如果从 {@code index} 开始且长度为 {@code length} 的指定 {@link ByteBuf} 是有效的
     * ASCII 文本，则返回 {@code true}，否则返回 {@code false}。
     *
     * @param buf    给定的 {@link ByteBuf}。
     * @param index  指定缓冲区的起始索引。
     * @param length 指定缓冲区的长度。
     */
    private static boolean isAscii(ByteBuf buf, int index, int length) {
        return buf.forEachByte(index, length, FIND_NON_ASCII) == -1;
    }

    /**
     * Returns {@code true} if the specified {@link ByteBuf} starting at {@code index} with {@code length} is valid
     * UTF8 text, otherwise return {@code false}.
     *
     * @param buf The given {@link ByteBuf}.
     * @param index The start index of the specified buffer.
     * @param length The length of the specified buffer.
     *
     * @see
     * <a href=https://www.ietf.org/rfc/rfc3629.txt>UTF-8 Definition</a>
     *
     * <pre>
     * 1. Bytes format of UTF-8
     *
     * The table below summarizes the format of these different octet types.
     * The letter x indicates bits available for encoding bits of the character number.
     *
     * Char. number range  |        UTF-8 octet sequence
     *    (hexadecimal)    |              (binary)
     * --------------------+---------------------------------------------
     * 0000 0000-0000 007F | 0xxxxxxx
     * 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     * </pre>
     *
     * <pre>
     * 2. Syntax of UTF-8 Byte Sequences
     *
     * UTF8-octets = *( UTF8-char )
     * UTF8-char   = UTF8-1 / UTF8-2 / UTF8-3 / UTF8-4
     * UTF8-1      = %x00-7F
     * UTF8-2      = %xC2-DF UTF8-tail
     * UTF8-3      = %xE0 %xA0-BF UTF8-tail /
     *               %xE1-EC 2( UTF8-tail ) /
     *               %xED %x80-9F UTF8-tail /
     *               %xEE-EF 2( UTF8-tail )
     * UTF8-4      = %xF0 %x90-BF 2( UTF8-tail ) /
     *               %xF1-F3 3( UTF8-tail ) /
     *               %xF4 %x80-8F 2( UTF8-tail )
     * UTF8-tail   = %x80-BF
     * </pre>
     */

    /**
     * 如果从 {@code index} 开始、长度为 {@code length} 的指定 {@link ByteBuf} 是有效的 UTF8 文本，则返回 {@code true}，否则返回 {@code false}。
     *
     * @param buf 给定的 {@link ByteBuf}。
     * @param index 指定缓冲区的起始索引。
     * @param length 指定缓冲区的长度。
     *
     * @see
     * <a href=https://www.ietf.org/rfc/rfc3629.txt>UTF-8 定义</a>
     *
     * <pre>
     * 1. UTF-8 的字节格式
     *
     * 下表总结了这些不同八位字节类型的格式。
     * 字母 x 表示可用于编码字符编号的位。
     *
     * 字符编号范围  |        UTF-8 八位字节序列
     *    (十六进制)    |              (二进制)
     * --------------------+---------------------------------------------
     * 0000 0000-0000 007F | 0xxxxxxx
     * 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     * </pre>
     *
     * <pre>
     * 2. UTF-8 字节序列的语法
     *
     * UTF8-octets = *( UTF8-char )
     * UTF8-char   = UTF8-1 / UTF8-2 / UTF8-3 / UTF8-4
     * UTF8-1      = %x00-7F
     * UTF8-2      = %xC2-DF UTF8-tail
     * UTF8-3      = %xE0 %xA0-BF UTF8-tail /
     *               %xE1-EC 2( UTF8-tail ) /
     *               %xED %x80-9F UTF8-tail /
     *               %xEE-EF 2( UTF8-tail )
     * UTF8-4      = %xF0 %x90-BF 2( UTF8-tail ) /
     *               %xF1-F3 3( UTF8-tail ) /
     *               %xF4 %x80-8F 2( UTF8-tail )
     * UTF8-tail   = %x80-BF
     * </pre>
     */
    private static boolean isUtf8(ByteBuf buf, int index, int length) {
        final int endIndex = index + length;
        while (index < endIndex) {
            byte b1 = buf.getByte(index++);
            byte b2, b3, b4;
            if ((b1 & 0x80) == 0) {
                // 1 byte
                // 1 byte
                continue;
            }
            if ((b1 & 0xE0) == 0xC0) {
                // 2 bytes
                // 2 bytes
                //
                // Bit/Byte pattern
                // 位/字节模式
                // 110xxxxx    10xxxxxx
                // 110xxxxx    10xxxxxx
                // C2..DF      80..BF
                // C2..DF      80..BF
                if (index >= endIndex) { // no enough bytes
                    return false;
                }
                b2 = buf.getByte(index++);
                if ((b2 & 0xC0) != 0x80) { // 2nd byte not starts with 10
                    return false;
                }
                if ((b1 & 0xFF) < 0xC2) { // out of lower bound
                    return false;
                }
            } else if ((b1 & 0xF0) == 0xE0) {
                // 3 bytes
                // 3字节
                //
                // Bit/Byte pattern
                // 位/字节模式
                // 1110xxxx    10xxxxxx    10xxxxxx
                // 1110xxxx    10xxxxxx    10xxxxxx
                // E0          A0..BF      80..BF
                // E0          A0..BF      80..BF
                // E1..EC      80..BF      80..BF
                // E1..EC      80..BF      80..BF
                // ED          80..9F      80..BF
                // ED          80..9F      80..BF
                // E1..EF      80..BF      80..BF
                // E1..EF      80..BF      80..BF
                if (index > endIndex - 2) { // no enough bytes
                    return false;
                }
                b2 = buf.getByte(index++);
                b3 = buf.getByte(index++);
                if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) { // 2nd or 3rd bytes not start with 10
                    return false;
                }
                if ((b1 & 0x0F) == 0x00 && (b2 & 0xFF) < 0xA0) { // out of lower bound
                    return false;
                }
                if ((b1 & 0x0F) == 0x0D && (b2 & 0xFF) > 0x9F) { // out of upper bound
                    return false;
                }
            } else if ((b1 & 0xF8) == 0xF0) {
                // 4 bytes
                // 4字节
                //
                // Bit/Byte pattern
                // 位/字节模式
                // 11110xxx    10xxxxxx    10xxxxxx    10xxxxxx
                // 11110xxx    10xxxxxx    10xxxxxx    10xxxxxx
                // F0          90..BF      80..BF      80..BF
                // F0          90..BF      80..BF      80..BF
                // F1..F3      80..BF      80..BF      80..BF
                // F1..F3      80..BF      80..BF      80..BF
                // F4          80..8F      80..BF      80..BF
                // F4          80..8F      80..BF      80..BF
                if (index > endIndex - 3) { // no enough bytes
                    return false;
                }
                b2 = buf.getByte(index++);
                b3 = buf.getByte(index++);
                b4 = buf.getByte(index++);
                if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80) {
                    // 2nd, 3rd or 4th bytes not start with 10
                    // 第2、3或4字节不以10开头
                    return false;
                }
                if ((b1 & 0xFF) > 0xF4 // b1 invalid
                        || (b1 & 0xFF) == 0xF0 && (b2 & 0xFF) < 0x90    // b2 out of lower bound
                        || (b1 & 0xFF) == 0xF4 && (b2 & 0xFF) > 0x8F) { // b2 out of upper bound
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Read bytes from the given {@link ByteBuffer} into the given {@link OutputStream} using the {@code position} and
     * {@code length}. The position and limit of the given {@link ByteBuffer} may be adjusted.
     */

    /**
     * 从给定的 {@link ByteBuffer} 读取字节到给定的 {@link OutputStream}，使用 {@code position} 和
     * {@code length}。给定的 {@link ByteBuffer} 的 position 和 limit 可能会被调整。
     */
    static void readBytes(ByteBufAllocator allocator, ByteBuffer buffer, int position, int length, OutputStream out)
            throws IOException {
        if (buffer.hasArray()) {
            out.write(buffer.array(), position + buffer.arrayOffset(), length);
        } else {
            int chunkLen = Math.min(length, WRITE_CHUNK_SIZE);
            buffer.clear().position(position);

            if (length <= MAX_TL_ARRAY_LEN || !allocator.isDirectBufferPooled()) {
                getBytes(buffer, threadLocalTempArray(chunkLen), 0, chunkLen, out, length);
            } else {
                // if direct buffers are pooled chances are good that heap buffers are pooled as well.
                // 如果直接缓冲区被池化，那么堆缓冲区很可能也被池化。
                ByteBuf tmpBuf = allocator.heapBuffer(chunkLen);
                try {
                    byte[] tmp = tmpBuf.array();
                    int offset = tmpBuf.arrayOffset();
                    getBytes(buffer, tmp, offset, chunkLen, out, length);
                } finally {
                    tmpBuf.release();
                }
            }
        }
    }

    private static void getBytes(ByteBuffer inBuffer, byte[] in, int inOffset, int inLen, OutputStream out, int outLen)
            throws IOException {
        do {
            int len = Math.min(inLen, outLen);
            inBuffer.get(in, inOffset, len);
            out.write(in, inOffset, len);
            outLen -= len;
        } while (outLen > 0);
    }

    private ByteBufUtil() { }
}
