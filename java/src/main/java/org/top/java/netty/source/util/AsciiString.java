
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.EmptyArrays;
import org.top.java.netty.source.util.internal.InternalThreadLocalMap;
import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.PlatformDependent;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.netty.util.internal.MathUtil.isOutOfBounds;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * A string which has been encoded into a character encoding whose character always takes a single byte, similarly to
 * ASCII. It internally keeps its content in a byte array unlike {@link String}, which uses a character array, for
 * reduced memory footprint and faster data transfer from/to byte-based data structures such as a byte array and
 * {@link ByteBuffer}. It is often used in conjunction with {@code Headers} that require a {@link CharSequence}.
 * <p>
 * This class was designed to provide an immutable array of bytes, and caches some internal state based upon the value
 * of this array. However underlying access to this byte array is provided via not copying the array on construction or
 * {@link #array()}. If any changes are made to the underlying byte array it is the user's responsibility to call
 * {@link #arrayChanged()} so the state of this class can be reset.
 */

/**
 * 一个字符串，它被编码为一种字符编码，其字符始终占用单个字节，类似于ASCII。与使用字符数组的{@link String}不同，它在内部将其内容保存在字节数组中，以减少内存占用并加快与字节数据结构（如字节数组和{@link ByteBuffer}）之间的数据传输。它通常与需要{@link CharSequence}的{@code Headers}一起使用。
 * <p>
 * 该类旨在提供一个不可变的字节数组，并根据该数组的值缓存一些内部状态。然而，通过不复制构造时的数组或通过{@link #array()}提供了对底层字节数组的访问。如果对底层字节数组进行了任何更改，用户有责任调用{@link #arrayChanged()}以重置该类的状态。
 */
public final class AsciiString implements CharSequence, Comparable<CharSequence> {
    public static final AsciiString EMPTY_STRING = cached("");
    private static final char MAX_CHAR_VALUE = 255;

    public static final int INDEX_NOT_FOUND = -1;

    /**
     * If this value is modified outside the constructor then call {@link #arrayChanged()}.
     */

    /**
     * 如果此值在构造函数外部被修改，则调用 {@link #arrayChanged()}。
     */
    private final byte[] value;
    /**
     * Offset into {@link #value} that all operations should use when acting upon {@link #value}.
     */
    /**
     * 操作 {@link #value} 时应使用的偏移量。
     */
    private final int offset;
    /**
     * Length in bytes for {@link #value} that we care about. This is independent from {@code value.length}
     * because we may be looking at a subsection of the array.
     */
    /**
     * {@link #value} 中我们关心的字节长度。这与 {@code value.length} 无关，
     * 因为我们可能只关注数组的一部分。
     */
    private final int length;
    /**
     * The hash code is cached after it is first computed. It can be reset with {@link #arrayChanged()}.
     */
    /**
     * 哈希码在首次计算后被缓存。可以通过 {@link #arrayChanged()} 重置。
     */
    private int hash;
    /**
     * Used to cache the {@link #toString()} value.
     */
    /**
     * 用于缓存 {@link #toString()} 的值。
     */
    private String string;

    /**
     * Initialize this byte string based upon a byte array. A copy will be made.
     */

    /**
     * 根据字节数组初始化此字节字符串。将会创建一个副本。
     */
    public AsciiString(byte[] value) {
        this(value, true);
    }

    /**
     * Initialize this byte string based upon a byte array.
     * {@code copy} determines if a copy is made or the array is shared.
     */

    /**
     * 根据字节数组初始化此字节字符串。
     * {@code copy} 决定是否创建副本或共享数组。
     */
    public AsciiString(byte[] value, boolean copy) {
        this(value, 0, value.length, copy);
    }

    /**
     * Construct a new instance from a {@code byte[]} array.
     * @param copy {@code true} then a copy of the memory will be made. {@code false} the underlying memory
     * will be shared.
     */

    /**
     * 从 {@code byte[]} 数组构造一个新实例。
     * @param copy {@code true} 则会对内存进行复制。{@code false} 则会共享底层内存。
     */
    public AsciiString(byte[] value, int start, int length, boolean copy) {
        if (copy) {
            this.value = Arrays.copyOfRange(value, start, start + length);
            this.offset = 0;
        } else {
            if (isOutOfBounds(start, length, value.length)) {
                throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= start + length(" +
                        length + ") <= " + "value.length(" + value.length + ')');
            }
            this.value = value;
            this.offset = start;
        }
        this.length = length;
    }

    /**
     * Create a copy of the underlying storage from {@code value}.
     * The copy will start at {@link ByteBuffer#position()} and copy {@link ByteBuffer#remaining()} bytes.
     */

    /**
     * 从 {@code value} 的底层存储中创建一个副本。
     * 副本将从 {@link ByteBuffer#position()} 开始，并复制 {@link ByteBuffer#remaining()} 字节。
     */
    public AsciiString(ByteBuffer value) {
        this(value, true);
    }

    /**
     * Initialize an instance based upon the underlying storage from {@code value}.
     * There is a potential to share the underlying array storage if {@link ByteBuffer#hasArray()} is {@code true}.
     * if {@code copy} is {@code true} a copy will be made of the memory.
     * if {@code copy} is {@code false} the underlying storage will be shared, if possible.
     */

    /**
     * 基于 {@code value} 的底层存储初始化一个实例。
     * 如果 {@link ByteBuffer#hasArray()} 为 {@code true}，则有可能共享底层数组存储。
     * 如果 {@code copy} 为 {@code true}，则会对内存进行复制。
     * 如果 {@code copy} 为 {@code false}，则尽可能共享底层存储。
     */
    public AsciiString(ByteBuffer value, boolean copy) {
        this(value, value.position(), value.remaining(), copy);
    }

    /**
     * Initialize an {@link AsciiString} based upon the underlying storage from {@code value}.
     * There is a potential to share the underlying array storage if {@link ByteBuffer#hasArray()} is {@code true}.
     * if {@code copy} is {@code true} a copy will be made of the memory.
     * if {@code copy} is {@code false} the underlying storage will be shared, if possible.
     */

    /**
     * 基于 {@code value} 的底层存储初始化一个 {@link AsciiString}。
     * 如果 {@link ByteBuffer#hasArray()} 为 {@code true}，则有可能共享底层数组存储。
     * 如果 {@code copy} 为 {@code true}，将会对内存进行复制。
     * 如果 {@code copy} 为 {@code false}，将尽可能共享底层存储。
     */
    public AsciiString(ByteBuffer value, int start, int length, boolean copy) {
        if (isOutOfBounds(start, length, value.capacity())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= start + length(" + length
                            + ") <= " + "value.capacity(" + value.capacity() + ')');
        }

        if (value.hasArray()) {
            if (copy) {
                final int bufferOffset = value.arrayOffset() + start;
                this.value = Arrays.copyOfRange(value.array(), bufferOffset, bufferOffset + length);
                offset = 0;
            } else {
                this.value = value.array();
                this.offset = start;
            }
        } else {
            this.value = PlatformDependent.allocateUninitializedArray(length);
            int oldPos = value.position();
            value.get(this.value, 0, length);
            value.position(oldPos);
            this.offset = 0;
        }
        this.length = length;
    }

    /**
     * Create a copy of {@code value} into this instance assuming ASCII encoding.
     */

    /**
     * 使用ASCII编码将{@code value}的副本创建到此实例中。
     */
    public AsciiString(char[] value) {
        this(value, 0, value.length);
    }

    /**
     * Create a copy of {@code value} into this instance assuming ASCII encoding.
     * The copy will start at index {@code start} and copy {@code length} bytes.
     */

    /**
     * 使用ASCII编码将{@code value}的副本复制到此实例中。
     * 复制将从索引{@code start}开始，并复制{@code length}个字节。
     */
    public AsciiString(char[] value, int start, int length) {
        if (isOutOfBounds(start, length, value.length)) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= start + length(" + length
                            + ") <= " + "value.length(" + value.length + ')');
        }

        this.value = PlatformDependent.allocateUninitializedArray(length);
        for (int i = 0, j = start; i < length; i++, j++) {
            this.value[i] = c2b(value[j]);
        }
        this.offset = 0;
        this.length = length;
    }

    /**
     * Create a copy of {@code value} into this instance using the encoding type of {@code charset}.
     */

    /**
     * 使用 {@code charset} 的编码类型将 {@code value} 的副本创建到此实例中。
     */
    public AsciiString(char[] value, Charset charset) {
        this(value, charset, 0, value.length);
    }

    /**
     * Create a copy of {@code value} into a this instance using the encoding type of {@code charset}.
     * The copy will start at index {@code start} and copy {@code length} bytes.
     */

    /**
     * 使用 {@code charset} 的编码类型将 {@code value} 复制到此实例中。
     * 复制将从索引 {@code start} 开始，并复制 {@code length} 个字节。
     */
    public AsciiString(char[] value, Charset charset, int start, int length) {
        CharBuffer cbuf = CharBuffer.wrap(value, start, length);
        CharsetEncoder encoder = CharsetUtil.encoder(charset);
        ByteBuffer nativeBuffer = ByteBuffer.allocate((int) (encoder.maxBytesPerChar() * length));
        encoder.encode(cbuf, nativeBuffer, true);
        final int bufferOffset = nativeBuffer.arrayOffset();
        this.value = Arrays.copyOfRange(nativeBuffer.array(), bufferOffset, bufferOffset + nativeBuffer.position());
        this.offset = 0;
        this.length =  this.value.length;
    }

    /**
     * Create a copy of {@code value} into this instance assuming ASCII encoding.
     */

    /**
     * 使用ASCII编码将{@code value}的副本创建到此实例中。
     */
    public AsciiString(CharSequence value) {
        this(value, 0, value.length());
    }

    /**
     * Create a copy of {@code value} into this instance assuming ASCII encoding.
     * The copy will start at index {@code start} and copy {@code length} bytes.
     */

    /**
     * 使用ASCII编码将{@code value}的副本复制到此实例中。
     * 复制将从索引{@code start}开始，并复制{@code length}个字节。
     */
    public AsciiString(CharSequence value, int start, int length) {
        if (isOutOfBounds(start, length, value.length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= start + length(" + length
                            + ") <= " + "value.length(" + value.length() + ')');
        }

        this.value = PlatformDependent.allocateUninitializedArray(length);
        for (int i = 0, j = start; i < length; i++, j++) {
            this.value[i] = c2b(value.charAt(j));
        }
        this.offset = 0;
        this.length = length;
    }

    /**
     * Create a copy of {@code value} into this instance using the encoding type of {@code charset}.
     */

    /**
     * 使用 {@code charset} 的编码类型将 {@code value} 的副本创建到此实例中。
     */
    public AsciiString(CharSequence value, Charset charset) {
        this(value, charset, 0, value.length());
    }

    /**
     * Create a copy of {@code value} into this instance using the encoding type of {@code charset}.
     * The copy will start at index {@code start} and copy {@code length} bytes.
     */

    /**
     * 使用 {@code charset} 的编码类型将 {@code value} 复制到此实例中。
     * 复制将从索引 {@code start} 开始，并复制 {@code length} 个字节。
     */
    public AsciiString(CharSequence value, Charset charset, int start, int length) {
        CharBuffer cbuf = CharBuffer.wrap(value, start, start + length);
        CharsetEncoder encoder = CharsetUtil.encoder(charset);
        ByteBuffer nativeBuffer = ByteBuffer.allocate((int) (encoder.maxBytesPerChar() * length));
        encoder.encode(cbuf, nativeBuffer, true);
        final int offset = nativeBuffer.arrayOffset();
        this.value = Arrays.copyOfRange(nativeBuffer.array(), offset, offset + nativeBuffer.position());
        this.offset = 0;
        this.length = this.value.length;
    }

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
    public int forEachByte(ByteProcessor visitor) throws Exception {
        return forEachByte0(0, length(), visitor);
    }

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in ascending order.
     * (i.e. {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)}).
     *
     * @return {@code -1} if the processor iterated to or beyond the end of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以升序顺序遍历此缓冲区的指定区域，并使用指定的 {@code processor} 进行处理。
     * (即 {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})。
     *
     * @return 如果处理器遍历到或超过指定区域的末尾，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public int forEachByte(int index, int length, ByteProcessor visitor) throws Exception {
        if (isOutOfBounds(index, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= index(" + index + ") <= start + length(" + length
                    + ") <= " + "length(" + length() + ')');
        }
        return forEachByte0(index, length, visitor);
    }

    private int forEachByte0(int index, int length, ByteProcessor visitor) throws Exception {
        final int len = offset + index + length;
        for (int i = offset + index; i < len; ++i) {
            if (!visitor.process(value[i])) {
                return i - offset;
            }
        }
        return -1;
    }

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in descending order.
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the readable bytes.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以降序遍历此缓冲区的可读字节，并使用指定的 {@code processor} 进行处理。
     *
     * @return 如果处理器迭代到或超过可读字节的开头，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public int forEachByteDesc(ByteProcessor visitor) throws Exception {
        return forEachByteDesc0(0, length(), visitor);
    }

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in descending order.
     * (i.e. {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index}).
     *
     * @return {@code -1} if the processor iterated to or beyond the beginning of the specified area.
     *         The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */

    /**
     * 以降序顺序遍历此缓冲区的指定区域，使用指定的 {@code processor}。
     * (即 {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})。
     *
     * @return 如果处理器迭代到或超过指定区域的开始，则返回 {@code -1}。
     *         如果 {@link ByteProcessor#process(byte)} 返回 {@code false}，则返回最后访问的索引。
     */
    public int forEachByteDesc(int index, int length, ByteProcessor visitor) throws Exception {
        if (isOutOfBounds(index, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= index(" + index + ") <= start + length(" + length
                    + ") <= " + "length(" + length() + ')');
        }
        return forEachByteDesc0(index, length, visitor);
    }

    private int forEachByteDesc0(int index, int length, ByteProcessor visitor) throws Exception {
        final int end = offset + index;
        for (int i = offset + index + length - 1; i >= end; --i) {
            if (!visitor.process(value[i])) {
                return i - offset;
            }
        }
        return -1;
    }

    public byte byteAt(int index) {
        // We must do a range check here to enforce the access does not go outside our sub region of the array.
        // 我们必须在这里进行范围检查，以确保访问不会超出数组的子区域。
        // We rely on the array access itself to pick up the array out of bounds conditions
        // 我们依赖数组访问本身来捕获数组越界条件
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index: " + index + " must be in the range [0," + length + ")");
        }
        // Try to use unsafe to avoid double checking the index bounds
        // 尝试使用unsafe来避免双重检查索引边界
        if (PlatformDependent.hasUnsafe()) {
            return PlatformDependent.getByte(value, index + offset);
        }
        return value[index + offset];
    }

    /**
     * Determine if this instance has 0 length.
     */

    /**
     * 判断此实例的长度是否为0。
     */
    public boolean isEmpty() {
        return length == 0;
    }

    /**
     * The length in bytes of this instance.
     */

    /**
     * 此实例的字节长度。
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * During normal use cases the {@link AsciiString} should be immutable, but if the underlying array is shared,
     * and changes then this needs to be called.
     */

    /**
     * 在正常使用情况下，{@link AsciiString} 应该是不可变的，但如果底层数组被共享，
     * 并且发生了变化，则需要调用此方法。
     */
    public void arrayChanged() {
        string = null;
        hash = 0;
    }

    /**
     * This gives direct access to the underlying storage array.
     * The {@link #toByteArray()} should be preferred over this method.
     * If the return value is changed then {@link #arrayChanged()} must be called.
     * @see #arrayOffset()
     * @see #isEntireArrayUsed()
     */

    /**
     * 这提供了对底层存储数组的直接访问。
     * 应优先使用 {@link #toByteArray()} 方法而不是此方法。
     * 如果返回值被更改，则必须调用 {@link #arrayChanged()}。
     * @see #arrayOffset()
     * @see #isEntireArrayUsed()
     */
    public byte[] array() {
        return value;
    }

    /**
     * The offset into {@link #array()} for which data for this ByteString begins.
     * @see #array()
     * @see #isEntireArrayUsed()
     */

    /**
     * 数据在 {@link #array()} 中的起始偏移量。
     * @see #array()
     * @see #isEntireArrayUsed()
     */
    public int arrayOffset() {
        return offset;
    }

    /**
     * Determine if the storage represented by {@link #array()} is entirely used.
     * @see #array()
     */

    /**
     * 判断由 {@link #array()} 表示的存储是否被完全使用。
     * @see #array()
     */
    public boolean isEntireArrayUsed() {
        return offset == 0 && length == value.length;
    }

    /**
     * Converts this string to a byte array.
     */

    /**
     * 将此字符串转换为字节数组。
     */
    public byte[] toByteArray() {
        return toByteArray(0, length());
    }

    /**
     * Converts a subset of this string to a byte array.
     * The subset is defined by the range [{@code start}, {@code end}).
     */

    /**
     * 将字符串的子集转换为字节数组。
     * 子集由范围 [{@code start}, {@code end}) 定义。
     */
    public byte[] toByteArray(int start, int end) {
        return Arrays.copyOfRange(value, start + offset, end + offset);
    }

    /**
     * Copies the content of this string to a byte array.
     *
     * @param srcIdx the starting offset of characters to copy.
     * @param dst the destination byte array.
     * @param dstIdx the starting offset in the destination byte array.
     * @param length the number of characters to copy.
     */

    /**
     * 将此字符串的内容复制到字节数组中。
     *
     * @param srcIdx 要复制的字符的起始偏移量。
     * @param dst 目标字节数组。
     * @param dstIdx 目标字节数组中的起始偏移量。
     * @param length 要复制的字符数。
     */
    public void copy(int srcIdx, byte[] dst, int dstIdx, int length) {
        if (isOutOfBounds(srcIdx, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= srcIdx(" + srcIdx + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + length() + ')');
        }

        System.arraycopy(value, srcIdx + offset, checkNotNull(dst, "dst"), dstIdx, length);
    }

    @Override
    public char charAt(int index) {
        return b2c(byteAt(index));
    }

    /**
     * Determines if this {@code String} contains the sequence of characters in the {@code CharSequence} passed.
     *
     * @param cs the character sequence to search for.
     * @return {@code true} if the sequence of characters are contained in this string, otherwise {@code false}.
     */

    /**
     * 判断此 {@code String} 是否包含传入的 {@code CharSequence} 中的字符序列。
     *
     * @param cs 要搜索的字符序列。
     * @return 如果此字符串包含该字符序列，则返回 {@code true}，否则返回 {@code false}。
     */
    public boolean contains(CharSequence cs) {
        return indexOf(cs) >= 0;
    }

    /**
     * Compares the specified string to this string using the ASCII values of the characters. Returns 0 if the strings
     * contain the same characters in the same order. Returns a negative integer if the first non-equal character in
     * this string has an ASCII value which is less than the ASCII value of the character at the same position in the
     * specified string, or if this string is a prefix of the specified string. Returns a positive integer if the first
     * non-equal character in this string has a ASCII value which is greater than the ASCII value of the character at
     * the same position in the specified string, or if the specified string is a prefix of this string.
     *
     * @param string the string to compare.
     * @return 0 if the strings are equal, a negative integer if this string is before the specified string, or a
     *         positive integer if this string is after the specified string.
     * @throws NullPointerException if {@code string} is {@code null}.
     */

    /**
     * 使用字符的ASCII值将指定的字符串与此字符串进行比较。如果字符串包含相同顺序的相同字符，则返回0。如果此字符串中第一个不相等字符的ASCII值小于指定字符串中相同位置字符的ASCII值，或者如果此字符串是指定字符串的前缀，则返回负整数。如果此字符串中第一个不相等字符的ASCII值大于指定字符串中相同位置字符的ASCII值，或者如果指定字符串是此字符串的前缀，则返回正整数。
     *
     * @param string 要比较的字符串。
     * @return 如果字符串相等，则返回0；如果此字符串在指定字符串之前，则返回负整数；如果此字符串在指定字符串之后，则返回正整数。
     * @throws NullPointerException 如果{@code string}为{@code null}。
     */
    @Override
    public int compareTo(CharSequence string) {
        if (this == string) {
            return 0;
        }

        int result;
        int length1 = length();
        int length2 = string.length();
        int minLength = Math.min(length1, length2);
        for (int i = 0, j = arrayOffset(); i < minLength; i++, j++) {
            result = b2c(value[j]) - string.charAt(i);
            if (result != 0) {
                return result;
            }
        }

        return length1 - length2;
    }

    /**
     * Concatenates this string and the specified string.
     *
     * @param string the string to concatenate
     * @return a new string which is the concatenation of this string and the specified string.
     */

    /**
     * 将此字符串与指定字符串连接。
     *
     * @param string 要连接的字符串
     * @return 一个新字符串，它是此字符串与指定字符串的连接。
     */
    public AsciiString concat(CharSequence string) {
        int thisLen = length();
        int thatLen = string.length();
        if (thatLen == 0) {
            return this;
        }

        if (string instanceof AsciiString) {
            AsciiString that = (AsciiString) string;
            if (isEmpty()) {
                return that;
            }

            byte[] newValue = PlatformDependent.allocateUninitializedArray(thisLen + thatLen);
            System.arraycopy(value, arrayOffset(), newValue, 0, thisLen);
            System.arraycopy(that.value, that.arrayOffset(), newValue, thisLen, thatLen);
            return new AsciiString(newValue, false);
        }

        if (isEmpty()) {
            return new AsciiString(string);
        }

        byte[] newValue = PlatformDependent.allocateUninitializedArray(thisLen + thatLen);
        System.arraycopy(value, arrayOffset(), newValue, 0, thisLen);
        for (int i = thisLen, j = 0; i < newValue.length; i++, j++) {
            newValue[i] = c2b(string.charAt(j));
        }

        return new AsciiString(newValue, false);
    }

    /**
     * Compares the specified string to this string to determine if the specified string is a suffix.
     *
     * @param suffix the suffix to look for.
     * @return {@code true} if the specified string is a suffix of this string, {@code false} otherwise.
     * @throws NullPointerException if {@code suffix} is {@code null}.
     */

    /**
     * 将指定的字符串与此字符串进行比较，以确定指定的字符串是否是一个后缀。
     *
     * @param suffix 要查找的后缀。
     * @return 如果指定的字符串是此字符串的后缀，则返回 {@code true}，否则返回 {@code false}。
     * @throws NullPointerException 如果 {@code suffix} 为 {@code null}。
     */
    public boolean endsWith(CharSequence suffix) {
        int suffixLen = suffix.length();
        return regionMatches(length() - suffixLen, suffix, 0, suffixLen);
    }

    /**
     * Compares the specified string to this string ignoring the case of the characters and returns true if they are
     * equal.
     *
     * @param string the string to compare.
     * @return {@code true} if the specified string is equal to this string, {@code false} otherwise.
     */

    /**
     * 比较指定的字符串与此字符串，忽略字符的大小写，如果它们相等则返回 true。
     *
     * @param string 要比较的字符串。
     * @return {@code true} 如果指定的字符串与此字符串相等，{@code false} 否则。
     */
    public boolean contentEqualsIgnoreCase(CharSequence string) {
        if (this == string) {
            return true;
        }

        if (string == null || string.length() != length()) {
            return false;
        }

        if (string instanceof AsciiString) {
            AsciiString rhs = (AsciiString) string;
            for (int i = arrayOffset(), j = rhs.arrayOffset(), end = i + length(); i < end; ++i, ++j) {
                if (!equalsIgnoreCase(value[i], rhs.value[j])) {
                    return false;
                }
            }
            return true;
        }

        for (int i = arrayOffset(), j = 0, end = length(); j < end; ++i, ++j) {
            if (!equalsIgnoreCase(b2c(value[i]), string.charAt(j))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies the characters in this string to a character array.
     *
     * @return a character array containing the characters of this string.
     */

    /**
     * 将此字符串中的字符复制到字符数组中。
     *
     * @return 包含此字符串字符的字符数组。
     */
    public char[] toCharArray() {
        return toCharArray(0, length());
    }

    /**
     * Copies the characters in this string to a character array.
     *
     * @return a character array containing the characters of this string.
     */

    /**
     * 将此字符串中的字符复制到字符数组中。
     *
     * @return 包含此字符串字符的字符数组。
     */
    public char[] toCharArray(int start, int end) {
        int length = end - start;
        if (length == 0) {
            return EmptyArrays.EMPTY_CHARS;
        }

        if (isOutOfBounds(start, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + length() + ')');
        }

        final char[] buffer = new char[length];
        for (int i = 0, j = start + arrayOffset(); i < length; i++, j++) {
            buffer[i] = b2c(value[j]);
        }
        return buffer;
    }

    /**
     * Copied the content of this string to a character array.
     *
     * @param srcIdx the starting offset of characters to copy.
     * @param dst the destination character array.
     * @param dstIdx the starting offset in the destination byte array.
     * @param length the number of characters to copy.
     */

    /**
     * 将此字符串的内容复制到字符数组中。
     *
     * @param srcIdx 要复制的字符的起始偏移量。
     * @param dst 目标字符数组。
     * @param dstIdx 目标字节数组中的起始偏移量。
     * @param length 要复制的字符数。
     */
    public void copy(int srcIdx, char[] dst, int dstIdx, int length) {
        ObjectUtil.checkNotNull(dst, "dst");

        if (isOutOfBounds(srcIdx, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= srcIdx(" + srcIdx + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + length() + ')');
        }

        final int dstEnd = dstIdx + length;
        for (int i = dstIdx, j = srcIdx + arrayOffset(); i < dstEnd; i++, j++) {
            dst[i] = b2c(value[j]);
        }
    }

    /**
     * Copies a range of characters into a new string.
     * @param start the offset of the first character (inclusive).
     * @return a new string containing the characters from start to the end of the string.
     * @throws IndexOutOfBoundsException if {@code start < 0} or {@code start > length()}.
     */

    /**
     * 将一段字符复制到一个新字符串中。
     * @param start 第一个字符的偏移量（包含）。
     * @return 一个新字符串，包含从 start 到字符串末尾的字符。
     * @throws IndexOutOfBoundsException 如果 {@code start < 0} 或 {@code start > length()}。
     */
    public AsciiString subSequence(int start) {
        return subSequence(start, length());
    }

    /**
     * Copies a range of characters into a new string.
     * @param start the offset of the first character (inclusive).
     * @param end The index to stop at (exclusive).
     * @return a new string containing the characters from start to the end of the string.
     * @throws IndexOutOfBoundsException if {@code start < 0} or {@code start > length()}.
     */

    /**
     * 将一段字符复制到一个新字符串中。
     * @param start 第一个字符的偏移量（包含）。
     * @param end 停止的索引（不包含）。
     * @return 一个包含从 start 到字符串末尾字符的新字符串。
     * @throws IndexOutOfBoundsException 如果 {@code start < 0} 或 {@code start > length()}。
     */
    @Override
    public AsciiString subSequence(int start, int end) {
       return subSequence(start, end, true);
    }

    /**
     * Either copy or share a subset of underlying sub-sequence of bytes.
     * @param start the offset of the first character (inclusive).
     * @param end The index to stop at (exclusive).
     * @param copy If {@code true} then a copy of the underlying storage will be made.
     * If {@code false} then the underlying storage will be shared.
     * @return a new string containing the characters from start to the end of the string.
     * @throws IndexOutOfBoundsException if {@code start < 0} or {@code start > length()}.
     */

    /**
     * 复制或共享底层字节子序列的一个子集。
     * @param start 第一个字符的偏移量（包含）。
     * @param end 停止的索引（不包含）。
     * @param copy 如果为 {@code true}，则复制底层存储。
     * 如果为 {@code false}，则共享底层存储。
     * @return 包含从 start 到字符串末尾字符的新字符串。
     * @throws IndexOutOfBoundsException 如果 {@code start < 0} 或 {@code start > length()}。
     */
    public AsciiString subSequence(int start, int end, boolean copy) {
        if (isOutOfBounds(start, end - start, length())) {
            throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= end (" + end + ") <= length("
                            + length() + ')');
        }

        if (start == 0 && end == length()) {
            return this;
        }

        if (end == start) {
            return EMPTY_STRING;
        }

        return new AsciiString(value, start + offset, end - start, copy);
    }

    /**
     * Searches in this string for the first index of the specified string. The search for the string starts at the
     * beginning and moves towards the end of this string.
     *
     * @param string the string to find.
     * @return the index of the first character of the specified string in this string, -1 if the specified string is
     *         not a substring.
     * @throws NullPointerException if {@code string} is {@code null}.
     */

    /**
     * 在此字符串中搜索指定字符串的第一个索引。搜索从字符串的开头开始，并向字符串的末尾移动。
     *
     * @param string 要查找的字符串。
     * @return 指定字符串在此字符串中第一个字符的索引，如果指定字符串不是子字符串，则返回 -1。
     * @throws NullPointerException 如果 {@code string} 为 {@code null}。
     */
    public int indexOf(CharSequence string) {
        return indexOf(string, 0);
    }

    /**
     * Searches in this string for the index of the specified string. The search for the string starts at the specified
     * offset and moves towards the end of this string.
     *
     * @param subString the string to find.
     * @param start the starting offset.
     * @return the index of the first character of the specified string in this string, -1 if the specified string is
     *         not a substring.
     * @throws NullPointerException if {@code subString} is {@code null}.
     */

    /**
     * 在此字符串中搜索指定字符串的索引。搜索从指定的偏移量开始，并向字符串的末尾移动。
     *
     * @param subString 要查找的字符串。
     * @param start 起始偏移量。
     * @return 指定字符串在此字符串中第一个字符的索引，如果指定字符串不是子字符串，则返回 -1。
     * @throws NullPointerException 如果 {@code subString} 为 {@code null}。
     */
    public int indexOf(CharSequence subString, int start) {
        final int subCount = subString.length();
        if (start < 0) {
            start = 0;
        }
        if (subCount <= 0) {
            return start < length ? start : length;
        }
        if (subCount > length - start) {
            return INDEX_NOT_FOUND;
        }

        final char firstChar = subString.charAt(0);
        if (firstChar > MAX_CHAR_VALUE) {
            return INDEX_NOT_FOUND;
        }
        final byte firstCharAsByte = c2b0(firstChar);
        final int len = offset + length - subCount;
        for (int i = start + offset; i <= len; ++i) {
            if (value[i] == firstCharAsByte) {
                int o1 = i, o2 = 0;
                while (++o2 < subCount && b2c(value[++o1]) == subString.charAt(o2)) {
                    // Intentionally empty
                    // 有意为空
                }
                if (o2 == subCount) {
                    return i - offset;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * Searches in this string for the index of the specified char {@code ch}.
     * The search for the char starts at the specified offset {@code start} and moves towards the end of this string.
     *
     * @param ch the char to find.
     * @param start the starting offset.
     * @return the index of the first occurrence of the specified char {@code ch} in this string,
     * -1 if found no occurrence.
     */

    /**
     * 在此字符串中搜索指定字符 {@code ch} 的索引。
     * 从指定的偏移量 {@code start} 开始搜索，并向字符串的末尾移动。
     *
     * @param ch 要查找的字符。
     * @param start 起始偏移量。
     * @return 返回指定字符 {@code ch} 在此字符串中第一次出现的索引，
     * 如果未找到则返回 -1。
     */
    public int indexOf(char ch, int start) {
        if (ch > MAX_CHAR_VALUE) {
            return INDEX_NOT_FOUND;
        }

        if (start < 0) {
            start = 0;
        }

        final byte chAsByte = c2b0(ch);
        final int len = offset + length;
        for (int i = start + offset; i < len; ++i) {
            if (value[i] == chAsByte) {
                return i - offset;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * Searches in this string for the last index of the specified string. The search for the string starts at the end
     * and moves towards the beginning of this string.
     *
     * @param string the string to find.
     * @return the index of the first character of the specified string in this string, -1 if the specified string is
     *         not a substring.
     * @throws NullPointerException if {@code string} is {@code null}.
     */

    /**
     * 在该字符串中搜索指定字符串的最后一次出现的位置。搜索从字符串的末尾开始，并向字符串的开头移动。
     *
     * @param string 要查找的字符串。
     * @return 指定字符串在该字符串中第一次出现的字符的索引，如果指定字符串不是子字符串，则返回 -1。
     * @throws NullPointerException 如果 {@code string} 为 {@code null}。
     */
    public int lastIndexOf(CharSequence string) {
        // Use count instead of count - 1 so lastIndexOf("") answers count
        // 使用 count 而不是 count - 1，以便 lastIndexOf("") 返回 count
        return lastIndexOf(string, length);
    }

    /**
     * Searches in this string for the index of the specified string. The search for the string starts at the specified
     * offset and moves towards the beginning of this string.
     *
     * @param subString the string to find.
     * @param start the starting offset.
     * @return the index of the first character of the specified string in this string , -1 if the specified string is
     *         not a substring.
     * @throws NullPointerException if {@code subString} is {@code null}.
     */

    /**
     * 在该字符串中搜索指定字符串的索引。搜索从指定的偏移量开始，并向该字符串的开头移动。
     *
     * @param subString 要查找的字符串。
     * @param start 起始偏移量。
     * @return 指定字符串在该字符串中的第一个字符的索引，如果指定字符串不是子字符串，则返回 -1。
     * @throws NullPointerException 如果 {@code subString} 为 {@code null}。
     */
    public int lastIndexOf(CharSequence subString, int start) {
        final int subCount = subString.length();
        start = Math.min(start, length - subCount);
        if (start < 0) {
            return INDEX_NOT_FOUND;
        }
        if (subCount == 0) {
            return start;
        }

        final char firstChar = subString.charAt(0);
        if (firstChar > MAX_CHAR_VALUE) {
            return INDEX_NOT_FOUND;
        }
        final byte firstCharAsByte = c2b0(firstChar);
        for (int i = offset + start; i >= 0; --i) {
            if (value[i] == firstCharAsByte) {
                int o1 = i, o2 = 0;
                while (++o2 < subCount && b2c(value[++o1]) == subString.charAt(o2)) {
                    // Intentionally empty
                    // 有意为空
                }
                if (o2 == subCount) {
                    return i - offset;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * Compares the specified string to this string and compares the specified range of characters to determine if they
     * are the same.
     *
     * @param thisStart the starting offset in this string.
     * @param string the string to compare.
     * @param start the starting offset in the specified string.
     * @param length the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false} otherwise
     * @throws NullPointerException if {@code string} is {@code null}.
     */

    /**
     * 将指定的字符串与此字符串进行比较，并比较指定范围的字符以确定它们是否相同。
     *
     * @param thisStart 此字符串中的起始偏移量。
     * @param string 要比较的字符串。
     * @param start 指定字符串中的起始偏移量。
     * @param length 要比较的字符数量。
     * @return 如果字符范围相等，则返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 如果 {@code string} 为 {@code null}。
     */
    public boolean regionMatches(int thisStart, CharSequence string, int start, int length) {
        ObjectUtil.checkNotNull(string, "string");

        if (start < 0 || string.length() - start < length) {
            return false;
        }

        final int thisLen = length();
        if (thisStart < 0 || thisLen - thisStart < length) {
            return false;
        }

        if (length <= 0) {
            return true;
        }

        final int thatEnd = start + length;
        for (int i = start, j = thisStart + arrayOffset(); i < thatEnd; i++, j++) {
            if (b2c(value[j]) != string.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares the specified string to this string and compares the specified range of characters to determine if they
     * are the same. When ignoreCase is true, the case of the characters is ignored during the comparison.
     *
     * @param ignoreCase specifies if case should be ignored.
     * @param thisStart the starting offset in this string.
     * @param string the string to compare.
     * @param start the starting offset in the specified string.
     * @param length the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false} otherwise.
     * @throws NullPointerException if {@code string} is {@code null}.
     */

    /**
     * 将指定的字符串与此字符串进行比较，并比较指定范围的字符以确定它们是否相同。当 ignoreCase 为 true 时，比较期间忽略字符的大小写。
     *
     * @param ignoreCase 指定是否应忽略大小写。
     * @param thisStart 此字符串中的起始偏移量。
     * @param string 要比较的字符串。
     * @param start 指定字符串中的起始偏移量。
     * @param length 要比较的字符数。
     * @return 如果字符范围相等，则返回 {@code true}，否则返回 {@code false}。
     * @throws NullPointerException 如果 {@code string} 为 {@code null}。
     */
    public boolean regionMatches(boolean ignoreCase, int thisStart, CharSequence string, int start, int length) {
        if (!ignoreCase) {
            return regionMatches(thisStart, string, start, length);
        }

        ObjectUtil.checkNotNull(string, "string");

        final int thisLen = length();
        if (thisStart < 0 || length > thisLen - thisStart) {
            return false;
        }
        if (start < 0 || length > string.length() - start) {
            return false;
        }

        thisStart += arrayOffset();
        final int thisEnd = thisStart + length;
        while (thisStart < thisEnd) {
            if (!equalsIgnoreCase(b2c(value[thisStart++]), string.charAt(start++))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies this string replacing occurrences of the specified character with another character.
     *
     * @param oldChar the character to replace.
     * @param newChar the replacement character.
     * @return a new string with occurrences of oldChar replaced by newChar.
     */

    /**
     * 复制此字符串，并将指定字符的所有出现替换为另一个字符。
     *
     * @param oldChar 要替换的字符。
     * @param newChar 替换字符。
     * @return 一个新字符串，其中所有oldChar的出现都被newChar替换。
     */
    public AsciiString replace(char oldChar, char newChar) {
        if (oldChar > MAX_CHAR_VALUE) {
            return this;
        }

        final byte oldCharAsByte = c2b0(oldChar);
        final byte newCharAsByte = c2b(newChar);
        final int len = offset + length;
        for (int i = offset; i < len; ++i) {
            if (value[i] == oldCharAsByte) {
                byte[] buffer = PlatformDependent.allocateUninitializedArray(length());
                System.arraycopy(value, offset, buffer, 0, i - offset);
                buffer[i - offset] = newCharAsByte;
                ++i;
                for (; i < len; ++i) {
                    byte oldValue = value[i];
                    buffer[i - offset] = oldValue != oldCharAsByte ? oldValue : newCharAsByte;
                }
                return new AsciiString(buffer, false);
            }
        }
        return this;
    }

    /**
     * Compares the specified string to this string to determine if the specified string is a prefix.
     *
     * @param prefix the string to look for.
     * @return {@code true} if the specified string is a prefix of this string, {@code false} otherwise
     * @throws NullPointerException if {@code prefix} is {@code null}.
     */

    /**
     * 将指定的字符串与此字符串进行比较，以确定指定的字符串是否是一个前缀。
     *
     * @param prefix 要查找的字符串。
     * @return 如果指定的字符串是此字符串的前缀，则返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 如果 {@code prefix} 为 {@code null}。
     */
    public boolean startsWith(CharSequence prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * Compares the specified string to this string, starting at the specified offset, to determine if the specified
     * string is a prefix.
     *
     * @param prefix the string to look for.
     * @param start the starting offset.
     * @return {@code true} if the specified string occurs in this string at the specified offset, {@code false}
     *         otherwise.
     * @throws NullPointerException if {@code prefix} is {@code null}.
     */

    /**
     * 将指定的字符串与当前字符串进行比较，从指定的偏移量开始，以确定指定的字符串是否为前缀。
     *
     * @param prefix 要查找的字符串。
     * @param start 起始偏移量。
     * @return 如果指定的字符串在当前字符串的指定偏移量处出现，则返回 {@code true}，否则返回 {@code false}。
     * @throws NullPointerException 如果 {@code prefix} 为 {@code null}。
     */
    public boolean startsWith(CharSequence prefix, int start) {
        return regionMatches(start, prefix, 0, prefix.length());
    }

    /**
     * Converts the characters in this string to lowercase, using the default Locale.
     *
     * @return a new string containing the lowercase characters equivalent to the characters in this string.
     */

    /**
     * 将此字符串中的字符转换为小写，使用默认的 Locale。
     *
     * @return 一个新字符串，包含与此字符串中的字符等效的小写字符。
     */
    public AsciiString toLowerCase() {
        boolean lowercased = true;
        int i, j;
        final int len = length() + arrayOffset();
        for (i = arrayOffset(); i < len; ++i) {
            byte b = value[i];
            if (b >= 'A' && b <= 'Z') {
                lowercased = false;
                break;
            }
        }

        // Check if this string does not contain any uppercase characters.

        // 检查此字符串是否不包含任何大写字符。
        if (lowercased) {
            return this;
        }

        final byte[] newValue = PlatformDependent.allocateUninitializedArray(length());
        for (i = 0, j = arrayOffset(); i < newValue.length; ++i, ++j) {
            newValue[i] = toLowerCase(value[j]);
        }

        return new AsciiString(newValue, false);
    }

    /**
     * Converts the characters in this string to uppercase, using the default Locale.
     *
     * @return a new string containing the uppercase characters equivalent to the characters in this string.
     */

    /**
     * 将此字符串中的字符转换为大写，使用默认的Locale。
     *
     * @return 一个新的字符串，包含与此字符串中的字符等效的大写字符。
     */
    public AsciiString toUpperCase() {
        boolean uppercased = true;
        int i, j;
        final int len = length() + arrayOffset();
        for (i = arrayOffset(); i < len; ++i) {
            byte b = value[i];
            if (b >= 'a' && b <= 'z') {
                uppercased = false;
                break;
            }
        }

        // Check if this string does not contain any lowercase characters.

        // 检查此字符串是否不包含任何小写字符。
        if (uppercased) {
            return this;
        }

        final byte[] newValue = PlatformDependent.allocateUninitializedArray(length());
        for (i = 0, j = arrayOffset(); i < newValue.length; ++i, ++j) {
            newValue[i] = toUpperCase(value[j]);
        }

        return new AsciiString(newValue, false);
    }

    /**
     * Copies this string removing white space characters from the beginning and end of the string, and tries not to
     * copy if possible.
     *
     * @param c The {@link CharSequence} to trim.
     * @return a new string with characters {@code <= \u0020} removed from the beginning and the end.
     */

    /**
     * 复制此字符串，移除字符串开头和结尾的空白字符，并尽可能避免不必要的复制。
     *
     * @param c 要修剪的 {@link CharSequence}。
     * @return 一个新的字符串，开头和结尾移除了 {@code <= \\u0020} 的字符。
     */
    public static CharSequence trim(CharSequence c) {
        if (c instanceof AsciiString) {
            return ((AsciiString) c).trim();
        }
        if (c instanceof String) {
            return ((String) c).trim();
        }
        int start = 0, last = c.length() - 1;
        int end = last;
        while (start <= end && c.charAt(start) <= ' ') {
            start++;
        }
        while (end >= start && c.charAt(end) <= ' ') {
            end--;
        }
        if (start == 0 && end == last) {
            return c;
        }
        return c.subSequence(start, end);
    }

    /**
     * Duplicates this string removing white space characters from the beginning and end of the
     * string, without copying.
     *
     * @return a new string with characters {@code <= \u0020} removed from the beginning and the end.
     */

    /**
     * 复制此字符串并移除字符串开头和结尾的空白字符，无需复制。
     *
     * @return 一个新的字符串，移除了开头和结尾的字符 {@code <= \\u0020}。
     */
    public AsciiString trim() {
        int start = arrayOffset(), last = arrayOffset() + length() - 1;
        int end = last;
        while (start <= end && value[start] <= ' ') {
            start++;
        }
        while (end >= start && value[end] <= ' ') {
            end--;
        }
        if (start == 0 && end == last) {
            return this;
        }
        return new AsciiString(value, start, end - start + 1, false);
    }

    /**
     * Compares a {@code CharSequence} to this {@code String} to determine if their contents are equal.
     *
     * @param a the character sequence to compare to.
     * @return {@code true} if equal, otherwise {@code false}
     */

    /**
     * 比较一个 {@code CharSequence} 与此 {@code String} 以确定它们的内容是否相等。
     *
     * @param a 要比较的字符序列。
     * @return 如果相等则返回 {@code true}，否则返回 {@code false}
     */
    public boolean contentEquals(CharSequence a) {
        if (this == a) {
            return true;
        }

        if (a == null || a.length() != length()) {
            return false;
        }
        if (a instanceof AsciiString) {
            return equals(a);
        }

        for (int i = arrayOffset(), j = 0; j < a.length(); ++i, ++j) {
            if (b2c(value[i]) != a.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether this string matches a given regular expression.
     *
     * @param expr the regular expression to be matched.
     * @return {@code true} if the expression matches, otherwise {@code false}.
     * @throws PatternSyntaxException if the syntax of the supplied regular expression is not valid.
     * @throws NullPointerException if {@code expr} is {@code null}.
     */

    /**
     * 判断此字符串是否匹配给定的正则表达式。
     *
     * @param expr 要匹配的正则表达式。
     * @return 如果表达式匹配，则返回 {@code true}，否则返回 {@code false}。
     * @throws PatternSyntaxException 如果提供的正则表达式语法无效。
     * @throws NullPointerException 如果 {@code expr} 为 {@code null}。
     */
    public boolean matches(String expr) {
        return Pattern.matches(expr, this);
    }

    /**
     * Splits this string using the supplied regular expression {@code expr}. The parameter {@code max} controls the
     * behavior how many times the pattern is applied to the string.
     *
     * @param expr the regular expression used to divide the string.
     * @param max the number of entries in the resulting array.
     * @return an array of Strings created by separating the string along matches of the regular expression.
     * @throws NullPointerException if {@code expr} is {@code null}.
     * @throws PatternSyntaxException if the syntax of the supplied regular expression is not valid.
     * @see Pattern#split(CharSequence, int)
     */

    /**
     * 使用提供的正则表达式 {@code expr} 分割此字符串。参数 {@code max} 控制模式应用于字符串的次数。
     *
     * @param expr 用于分割字符串的正则表达式。
     * @param max 结果数组中的条目数。
     * @return 通过沿正则表达式的匹配项分隔字符串而创建的字符串数组。
     * @throws NullPointerException 如果 {@code expr} 为 {@code null}。
     * @throws PatternSyntaxException 如果提供的正则表达式的语法无效。
     * @see Pattern#split(CharSequence, int)
     */
    public AsciiString[] split(String expr, int max) {
        return toAsciiStringArray(Pattern.compile(expr).split(this, max));
    }

    /**
     * Splits the specified {@link String} with the specified delimiter..
     */

    /**
     * 使用指定的分隔符拆分指定的 {@link String}。
     */
    public AsciiString[] split(char delim) {
        final List<AsciiString> res = InternalThreadLocalMap.get().arrayList();

        int start = 0;
        final int length = length();
        for (int i = start; i < length; i++) {
            if (charAt(i) == delim) {
                if (start == i) {
                    res.add(EMPTY_STRING);
                } else {
                    res.add(new AsciiString(value, start + arrayOffset(), i - start, false));
                }
                start = i + 1;
            }
        }

        if (start == 0) { // If no delimiter was found in the value
            res.add(this);
        } else {
            if (start != length) {
                // Add the last element if it's not empty.
                // 如果最后一个元素不为空，则添加它。
                res.add(new AsciiString(value, start + arrayOffset(), length - start, false));
            } else {
                // Truncate trailing empty elements.
                // 截断尾部的空元素。
                for (int i = res.size() - 1; i >= 0; i--) {
                    if (res.get(i).isEmpty()) {
                        res.remove(i);
                    } else {
                        break;
                    }
                }
            }
        }

        return res.toArray(new AsciiString[0]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Provides a case-insensitive hash code for Ascii like byte strings.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 提供对Ascii类字节字符串不区分大小写的哈希码。
     */
    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = PlatformDependent.hashCodeAscii(value, offset, length);
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != AsciiString.class) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        AsciiString other = (AsciiString) obj;
        return length() == other.length() &&
               hashCode() == other.hashCode() &&
               PlatformDependent.equals(array(), arrayOffset(), other.array(), other.arrayOffset(), length());
    }

    /**
     * Translates the entire byte string to a {@link String}.
     * @see #toString(int)
     */

    /**
     * 将整个字节字符串转换为一个 {@link String}。
     * @see #toString(int)
     */
    @Override
    public String toString() {
        String cache = string;
        if (cache == null) {
            cache = toString(0);
            string = cache;
        }
        return cache;
    }

    /**
     * Translates the entire byte string to a {@link String} using the {@code charset} encoding.
     * @see #toString(int, int)
     */

    /**
     * 使用 {@code charset} 编码将整个字节字符串转换为 {@link String}。
     * @see #toString(int, int)
     */
    public String toString(int start) {
        return toString(start, length());
    }

    /**
     * Translates the [{@code start}, {@code end}) range of this byte string to a {@link String}.
     */

    /**
     * 将此字节字符串的 [{@code start}, {@code end}) 范围转换为 {@link String}。
     */
    public String toString(int start, int end) {
        int length = end - start;
        if (length == 0) {
            return "";
        }

        if (isOutOfBounds(start, length, length())) {
            throw new IndexOutOfBoundsException("expected: " + "0 <= start(" + start + ") <= srcIdx + length("
                            + length + ") <= srcLen(" + length() + ')');
        }

        @SuppressWarnings("deprecation")
        final String str = new String(value, 0, start + offset, length);
        return str;
    }

    public boolean parseBoolean() {
        return length >= 1 && value[offset] != 0;
    }

    public char parseChar() {
        return parseChar(0);
    }

    public char parseChar(int start) {
        if (start + 1 >= length()) {
            throw new IndexOutOfBoundsException("2 bytes required to convert to character. index " +
                    start + " would go out of bounds.");
        }
        final int startWithOffset = start + offset;
        return (char) ((b2c(value[startWithOffset]) << 8) | b2c(value[startWithOffset + 1]));
    }

    public short parseShort() {
        return parseShort(0, length(), 10);
    }

    public short parseShort(int radix) {
        return parseShort(0, length(), radix);
    }

    public short parseShort(int start, int end) {
        return parseShort(start, end, 10);
    }

    public short parseShort(int start, int end, int radix) {
        int intValue = parseInt(start, end, radix);
        short result = (short) intValue;
        if (result != intValue) {
            throw new NumberFormatException(subSequence(start, end, false).toString());
        }
        return result;
    }

    public int parseInt() {
        return parseInt(0, length(), 10);
    }

    public int parseInt(int radix) {
        return parseInt(0, length(), radix);
    }

    public int parseInt(int start, int end) {
        return parseInt(start, end, 10);
    }

    public int parseInt(int start, int end, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException();
        }

        if (start == end) {
            throw new NumberFormatException();
        }

        int i = start;
        boolean negative = byteAt(i) == '-';
        if (negative && ++i == end) {
            throw new NumberFormatException(subSequence(start, end, false).toString());
        }

        return parseInt(i, end, radix, negative);
    }

    private int parseInt(int start, int end, int radix, boolean negative) {
        int max = Integer.MIN_VALUE / radix;
        int result = 0;
        int currOffset = start;
        while (currOffset < end) {
            int digit = Character.digit((char) (value[currOffset++ + offset] & 0xFF), radix);
            if (digit == -1) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            if (max > result) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            int next = result * radix - digit;
            if (next > result) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
        }
        return result;
    }

    public long parseLong() {
        return parseLong(0, length(), 10);
    }

    public long parseLong(int radix) {
        return parseLong(0, length(), radix);
    }

    public long parseLong(int start, int end) {
        return parseLong(start, end, 10);
    }

    public long parseLong(int start, int end, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException();
        }

        if (start == end) {
            throw new NumberFormatException();
        }

        int i = start;
        boolean negative = byteAt(i) == '-';
        if (negative && ++i == end) {
            throw new NumberFormatException(subSequence(start, end, false).toString());
        }

        return parseLong(i, end, radix, negative);
    }

    private long parseLong(int start, int end, int radix, boolean negative) {
        long max = Long.MIN_VALUE / radix;
        long result = 0;
        int currOffset = start;
        while (currOffset < end) {
            int digit = Character.digit((char) (value[currOffset++ + offset] & 0xFF), radix);
            if (digit == -1) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            if (max > result) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            long next = result * radix - digit;
            if (next > result) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw new NumberFormatException(subSequence(start, end, false).toString());
            }
        }
        return result;
    }

    public float parseFloat() {
        return parseFloat(0, length());
    }

    public float parseFloat(int start, int end) {
        return Float.parseFloat(toString(start, end));
    }

    public double parseDouble() {
        return parseDouble(0, length());
    }

    public double parseDouble(int start, int end) {
        return Double.parseDouble(toString(start, end));
    }

    public static final HashingStrategy<CharSequence> CASE_INSENSITIVE_HASHER =
            new HashingStrategy<CharSequence>() {
        @Override
        public int hashCode(CharSequence o) {
            return AsciiString.hashCode(o);
        }

        @Override
        public boolean equals(CharSequence a, CharSequence b) {
            return AsciiString.contentEqualsIgnoreCase(a, b);
        }
    };

    public static final HashingStrategy<CharSequence> CASE_SENSITIVE_HASHER =
            new HashingStrategy<CharSequence>() {
        @Override
        public int hashCode(CharSequence o) {
            return AsciiString.hashCode(o);
        }

        @Override
        public boolean equals(CharSequence a, CharSequence b) {
            return AsciiString.contentEquals(a, b);
        }
    };

    /**
     * Returns an {@link AsciiString} containing the given character sequence. If the given string is already a
     * {@link AsciiString}, just returns the same instance.
     */

    /**
     * 返回包含给定字符序列的 {@link AsciiString}。如果给定的字符串已经是 {@link AsciiString}，则直接返回相同的实例。
     */
    public static AsciiString of(CharSequence string) {
        return string instanceof AsciiString ? (AsciiString) string : new AsciiString(string);
    }

    /**
     * Returns an {@link AsciiString} containing the given string and retains/caches the input
     * string for later use in {@link #toString()}.
     * Used for the constants (which already stored in the JVM's string table) and in cases
     * where the guaranteed use of the {@link #toString()} method.
     */

    /**
     * 返回一个包含给定字符串的 {@link AsciiString}，并保留/缓存输入字符串以便在 {@link #toString()} 中后续使用。
     * 用于常量（已存储在 JVM 的字符串表中）和确保使用 {@link #toString()} 方法的场景。
     */
    public static AsciiString cached(String string) {
        AsciiString asciiString = new AsciiString(string);
        asciiString.string = string;
        return asciiString;
    }

    /**
     * Returns the case-insensitive hash code of the specified string. Note that this method uses the same hashing
     * algorithm with {@link #hashCode()} so that you can put both {@link AsciiString}s and arbitrary
     * {@link CharSequence}s into the same headers.
     */

    /**
     * 返回指定字符串的大小写不敏感的哈希码。请注意，此方法使用与 {@link #hashCode()} 相同的哈希算法，
     * 以便您可以将 {@link AsciiString}s 和任意 {@link CharSequence}s 放入相同的头中。
     */
    public static int hashCode(CharSequence value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof AsciiString) {
            return value.hashCode();
        }

        return PlatformDependent.hashCodeAscii(value);
    }

    /**
     * Determine if {@code a} contains {@code b} in a case sensitive manner.
     */

    /**
     * 判断 {@code a} 是否以区分大小写的方式包含 {@code b}。
     */
    public static boolean contains(CharSequence a, CharSequence b) {
        return contains(a, b, DefaultCharEqualityComparator.INSTANCE);
    }

    /**
     * Determine if {@code a} contains {@code b} in a case insensitive manner.
     */

    /**
     * 判断 {@code a} 是否以不区分大小写的方式包含 {@code b}。
     */
    public static boolean containsIgnoreCase(CharSequence a, CharSequence b) {
        return contains(a, b, AsciiCaseInsensitiveCharEqualityComparator.INSTANCE);
    }

    /**
     * Returns {@code true} if both {@link CharSequence}'s are equals when ignore the case. This only supports 8-bit
     * ASCII.
     */

    /**
     * 当忽略大小写时，如果两个 {@link CharSequence} 相等，则返回 {@code true}。此方法仅支持 8 位 ASCII。
     */
    public static boolean contentEqualsIgnoreCase(CharSequence a, CharSequence b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a instanceof AsciiString) {
            return ((AsciiString) a).contentEqualsIgnoreCase(b);
        }
        if (b instanceof AsciiString) {
            return ((AsciiString) b).contentEqualsIgnoreCase(a);
        }

        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i < a.length(); ++i) {
            if (!equalsIgnoreCase(a.charAt(i),  b.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if {@code collection} contains {@code value} and using
     * {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} to compare values.
     * @param collection The collection to look for and equivalent element as {@code value}.
     * @param value The value to look for in {@code collection}.
     * @return {@code true} if {@code collection} contains {@code value} according to
     * {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)}. {@code false} otherwise.
     * @see #contentEqualsIgnoreCase(CharSequence, CharSequence)
     */

    /**
     * 判断 {@code collection} 是否包含 {@code value}，并使用
     * {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} 来比较值。
     * @param collection 要在其中查找与 {@code value} 等效的元素的集合。
     * @param value 要在 {@code collection} 中查找的值。
     * @return 如果根据 {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)}，
     * {@code collection} 包含 {@code value}，则返回 {@code true}。否则返回 {@code false}。
     * @see #contentEqualsIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean containsContentEqualsIgnoreCase(Collection<CharSequence> collection, CharSequence value) {
        for (CharSequence v : collection) {
            if (contentEqualsIgnoreCase(value, v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if {@code a} contains all of the values in {@code b} using
     * {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} to compare values.
     * @param a The collection under test.
     * @param b The values to test for.
     * @return {@code true} if {@code a} contains all of the values in {@code b} using
     * {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} to compare values. {@code false} otherwise.
     * @see #contentEqualsIgnoreCase(CharSequence, CharSequence)
     */

    /**
     * 使用 {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} 比较值，确定 {@code a} 是否包含 {@code b} 中的所有值。
     * @param a 被测试的集合。
     * @param b 要测试的值。
     * @return 如果 {@code a} 使用 {@link #contentEqualsIgnoreCase(CharSequence, CharSequence)} 比较值后包含 {@code b} 中的所有值，则返回 {@code true}。否则返回 {@code false}。
     * @see #contentEqualsIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean containsAllContentEqualsIgnoreCase(Collection<CharSequence> a, Collection<CharSequence> b) {
        for (CharSequence v : b) {
            if (!containsContentEqualsIgnoreCase(a, v)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the content of both {@link CharSequence}'s are equals. This only supports 8-bit ASCII.
     */

    /**
     * 如果两个 {@link CharSequence} 的内容相等，则返回 {@code true}。此方法仅支持 8 位 ASCII。
     */
    public static boolean contentEquals(CharSequence a, CharSequence b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a instanceof AsciiString) {
            return ((AsciiString) a).contentEquals(b);
        }

        if (b instanceof AsciiString) {
            return ((AsciiString) b).contentEquals(a);
        }

        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i <  a.length(); ++i) {
            if (a.charAt(i) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static AsciiString[] toAsciiStringArray(String[] jdkResult) {
        AsciiString[] res = new AsciiString[jdkResult.length];
        for (int i = 0; i < jdkResult.length; i++) {
            res[i] = new AsciiString(jdkResult[i]);
        }
        return res;
    }

    private interface CharEqualityComparator {
        boolean equals(char a, char b);
    }

    private static final class DefaultCharEqualityComparator implements CharEqualityComparator {
        static final DefaultCharEqualityComparator INSTANCE = new DefaultCharEqualityComparator();
        private DefaultCharEqualityComparator() { }

        @Override
        public boolean equals(char a, char b) {
            return a == b;
        }
    }

    private static final class AsciiCaseInsensitiveCharEqualityComparator implements CharEqualityComparator {
        static final AsciiCaseInsensitiveCharEqualityComparator
                INSTANCE = new AsciiCaseInsensitiveCharEqualityComparator();
        private AsciiCaseInsensitiveCharEqualityComparator() { }

        @Override
        public boolean equals(char a, char b) {
            return equalsIgnoreCase(a, b);
        }
    }

    private static final class GeneralCaseInsensitiveCharEqualityComparator implements CharEqualityComparator {
        static final GeneralCaseInsensitiveCharEqualityComparator
                INSTANCE = new GeneralCaseInsensitiveCharEqualityComparator();
        private GeneralCaseInsensitiveCharEqualityComparator() { }

        @Override
        public boolean equals(char a, char b) {
            //For motivation, why we need two checks, see comment in String#regionMatches
            //为了了解为什么需要两次检查，请参见String#regionMatches中的注释
            return Character.toUpperCase(a) == Character.toUpperCase(b) ||
                Character.toLowerCase(a) == Character.toLowerCase(b);
        }
    }

    private static boolean contains(CharSequence a, CharSequence b, CharEqualityComparator cmp) {
        if (a == null || b == null || a.length() < b.length()) {
            return false;
        }
        if (b.length() == 0) {
            return true;
        }
        int bStart = 0;
        for (int i = 0; i < a.length(); ++i) {
            if (cmp.equals(b.charAt(bStart), a.charAt(i))) {
                // If b is consumed then true.
                // 如果b被消耗则为true。
                if (++bStart == b.length()) {
                    return true;
                }
            } else if (a.length() - i < b.length()) {
                // If there are not enough characters left in a for b to be contained, then false.
                // 如果a中剩余的字符不足以包含b，则返回false。
                return false;
            } else {
                bStart = 0;
            }
        }
        return false;
    }

    private static boolean regionMatchesCharSequences(final CharSequence cs, final int csStart,
                                         final CharSequence string, final int start, final int length,
                                         CharEqualityComparator charEqualityComparator) {
        //general purpose implementation for CharSequences
        //通用CharSequences实现
        if (csStart < 0 || length > cs.length() - csStart) {
            return false;
        }
        if (start < 0 || length > string.length() - start) {
            return false;
        }

        int csIndex = csStart;
        int csEnd = csIndex + length;
        int stringIndex = start;

        while (csIndex < csEnd) {
            char c1 = cs.charAt(csIndex++);
            char c2 = string.charAt(stringIndex++);

            if (!charEqualityComparator.equals(c1, c2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * This methods make regionMatches operation correctly for any chars in strings
     * @param cs the {@code CharSequence} to be processed
     * @param ignoreCase specifies if case should be ignored.
     * @param csStart the starting offset in the {@code cs} CharSequence
     * @param string the {@code CharSequence} to compare.
     * @param start the starting offset in the specified {@code string}.
     * @param length the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false} otherwise.
     */

    /**
     * 该方法使regionMatches操作对于字符串中的任何字符都能正确执行
     * @param cs 要处理的{@code CharSequence}
     * @param ignoreCase 指定是否忽略大小写
     * @param csStart {@code cs} CharSequence中的起始偏移量
     * @param string 要比较的{@code CharSequence}
     * @param start 指定{@code string}中的起始偏移量
     * @param length 要比较的字符数
     * @return 如果字符范围相等，则返回{@code true}，否则返回{@code false}
     */
    public static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int csStart,
                                        final CharSequence string, final int start, final int length) {
        if (cs == null || string == null) {
            return false;
        }

        if (cs instanceof String && string instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, csStart, (String) string, start, length);
        }

        if (cs instanceof AsciiString) {
            return ((AsciiString) cs).regionMatches(ignoreCase, csStart, string, start, length);
        }

        return regionMatchesCharSequences(cs, csStart, string, start, length,
                                            ignoreCase ? GeneralCaseInsensitiveCharEqualityComparator.INSTANCE :
                                                    DefaultCharEqualityComparator.INSTANCE);
    }

    /**
     * This is optimized version of regionMatches for string with ASCII chars only
     * @param cs the {@code CharSequence} to be processed
     * @param ignoreCase specifies if case should be ignored.
     * @param csStart the starting offset in the {@code cs} CharSequence
     * @param string the {@code CharSequence} to compare.
     * @param start the starting offset in the specified {@code string}.
     * @param length the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false} otherwise.
     */

    /**
     * 这是针对仅包含ASCII字符的字符串进行优化的regionMatches版本
     * @param cs 要处理的{@code CharSequence}
     * @param ignoreCase 指定是否忽略大小写
     * @param csStart {@code cs} CharSequence中的起始偏移量
     * @param string 要比较的{@code CharSequence}
     * @param start 指定{@code string}中的起始偏移量
     * @param length 要比较的字符数
     * @return 如果字符范围相等，则返回{@code true}，否则返回{@code false}
     */
    public static boolean regionMatchesAscii(final CharSequence cs, final boolean ignoreCase, final int csStart,
                                        final CharSequence string, final int start, final int length) {
        if (cs == null || string == null) {
            return false;
        }

        if (!ignoreCase && cs instanceof String && string instanceof String) {
            //we don't call regionMatches from String for ignoreCase==true. It's a general purpose method,
            //我们不从String调用regionMatches来处理ignoreCase==true的情况。它是一个通用方法。
            //which make complex comparison in case of ignoreCase==true, which is useless for ASCII-only strings.
            //在ignoreCase==true的情况下进行复杂的比较，这对于仅包含ASCII字符的字符串是无用的。
            //To avoid applying this complex ignore-case comparison, we will use regionMatchesCharSequences
            //为了避免应用这种复杂的忽略大小写比较，我们将使用regionMatchesCharSequences
            return ((String) cs).regionMatches(false, csStart, (String) string, start, length);
        }

        if (cs instanceof AsciiString) {
            return ((AsciiString) cs).regionMatches(ignoreCase, csStart, string, start, length);
        }

        return regionMatchesCharSequences(cs, csStart, string, start, length,
                                          ignoreCase ? AsciiCaseInsensitiveCharEqualityComparator.INSTANCE :
                                                      DefaultCharEqualityComparator.INSTANCE);
    }

    /**
     * <p>Case in-sensitive find of the first index within a CharSequence
     * from the specified position.</p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.
     * A negative start position is treated as zero.
     * An empty ("") search CharSequence always matches.
     * A start position greater than the string length only matches
     * an empty search CharSequence.</p>
     *
     * <pre>
     * AsciiString.indexOfIgnoreCase(null, *, *)          = -1
     * AsciiString.indexOfIgnoreCase(*, null, *)          = -1
     * AsciiString.indexOfIgnoreCase("", "", 0)           = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     * AsciiString.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str  the CharSequence to check, may be null
     * @param searchStr  the CharSequence to find, may be null
     * @param startPos  the start position, negative treated as zero
     * @return the first index of the search CharSequence (always &ge; startPos),
     *  -1 if no match or {@code null} string input
     */

    /**
     * <p>在 CharSequence 中从指定位置开始查找第一个匹配项，不区分大小写。</p>
     *
     * <p>如果 CharSequence 为 {@code null}，则返回 {@code -1}。
     * 如果起始位置为负数，则视为零。
     * 空字符串 ("") 作为搜索字符串时总是匹配。
     * 如果起始位置大于字符串长度，则仅当搜索字符串为空时匹配。</p>
     *
     * <pre>
     * AsciiString.indexOfIgnoreCase(null, *, *)          = -1
     * AsciiString.indexOfIgnoreCase(*, null, *)          = -1
     * AsciiString.indexOfIgnoreCase("", "", 0)           = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     * AsciiString.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str  要检查的 CharSequence，可能为 null
     * @param searchStr  要查找的 CharSequence，可能为 null
     * @param startPos  起始位置，负数视为零
     * @return 搜索 CharSequence 的第一个索引（始终 &ge; startPos），
     *  如果没有匹配或输入为 {@code null}，则返回 -1
     */
    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr, int startPos) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        if (startPos < 0) {
            startPos = 0;
        }
        int searchStrLen = searchStr.length();
        final int endLimit = str.length() - searchStrLen + 1;
        if (startPos > endLimit) {
            return INDEX_NOT_FOUND;
        }
        if (searchStrLen == 0) {
            return startPos;
        }
        for (int i = startPos; i < endLimit; i++) {
            if (regionMatches(str, true, i, searchStr, 0, searchStrLen)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Case in-sensitive find of the first index within a CharSequence
     * from the specified position. This method optimized and works correctly for ASCII CharSequences only</p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.
     * A negative start position is treated as zero.
     * An empty ("") search CharSequence always matches.
     * A start position greater than the string length only matches
     * an empty search CharSequence.</p>
     *
     * <pre>
     * AsciiString.indexOfIgnoreCase(null, *, *)          = -1
     * AsciiString.indexOfIgnoreCase(*, null, *)          = -1
     * AsciiString.indexOfIgnoreCase("", "", 0)           = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     * AsciiString.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str  the CharSequence to check, may be null
     * @param searchStr  the CharSequence to find, may be null
     * @param startPos  the start position, negative treated as zero
     * @return the first index of the search CharSequence (always &ge; startPos),
     *  -1 if no match or {@code null} string input
     */

    /**
     * <p>在CharSequence中从指定位置开始进行不区分大小写的查找，返回第一个匹配的索引。此方法针对ASCII CharSequences进行了优化，仅适用于ASCII字符。</p>
     *
     * <p>如果CharSequence为{@code null}，则返回{@code -1}。
     * 如果起始位置为负数，则视为零。
     * 如果搜索的CharSequence为空（""），则总是匹配。
     * 如果起始位置大于字符串长度，则仅在搜索的CharSequence为空时匹配。</p>
     *
     * <pre>
     * AsciiString.indexOfIgnoreCase(null, *, *)          = -1
     * AsciiString.indexOfIgnoreCase(*, null, *)          = -1
     * AsciiString.indexOfIgnoreCase("", "", 0)           = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "A", 0)  = 0
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 0)  = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "AB", 0) = 1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 3)  = 5
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", 9)  = -1
     * AsciiString.indexOfIgnoreCase("aabaabaa", "B", -1) = 2
     * AsciiString.indexOfIgnoreCase("aabaabaa", "", 2)   = 2
     * AsciiString.indexOfIgnoreCase("abc", "", 9)        = -1
     * </pre>
     *
     * @param str  要检查的CharSequence，可能为null
     * @param searchStr  要查找的CharSequence，可能为null
     * @param startPos  起始位置，负数视为零
     * @return 搜索的CharSequence的第一个索引（总是 &ge; startPos），
     *  如果没有匹配或输入为{@code null}，则返回-1
     */
    public static int indexOfIgnoreCaseAscii(final CharSequence str, final CharSequence searchStr, int startPos) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        if (startPos < 0) {
            startPos = 0;
        }
        int searchStrLen = searchStr.length();
        final int endLimit = str.length() - searchStrLen + 1;
        if (startPos > endLimit) {
            return INDEX_NOT_FOUND;
        }
        if (searchStrLen == 0) {
            return startPos;
        }
        for (int i = startPos; i < endLimit; i++) {
            if (regionMatchesAscii(str, true, i, searchStr, 0, searchStrLen)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the first index in the {@code CharSequence} that matches the
     * specified character.</p>
     *
     * @param cs  the {@code CharSequence} to be processed, not null
     * @param searchChar the char to be searched for
     * @param start  the start index, negative starts at the string start
     * @return the index where the search char was found,
     * -1 if char {@code searchChar} is not found or {@code cs == null}
     */

    /**
     * <p>在 {@code CharSequence} 中查找与指定字符匹配的第一个索引。</p>
     *
     * @param cs  要处理的 {@code CharSequence}，不能为 null
     * @param searchChar 要搜索的字符
     * @param start  起始索引，负值从字符串开头开始
     * @return 找到搜索字符的索引，
     * 如果未找到字符 {@code searchChar} 或 {@code cs == null}，则返回 -1
     */
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    public static int indexOf(final CharSequence cs, final char searchChar, int start) {
        if (cs instanceof String) {
            return ((String) cs).indexOf(searchChar, start);
        } else if (cs instanceof AsciiString) {
            return ((AsciiString) cs).indexOf(searchChar, start);
        }
        if (cs == null) {
            return INDEX_NOT_FOUND;
        }
        final int sz = cs.length();
        for (int i = start < 0 ? 0 : start; i < sz; i++) {
            if (cs.charAt(i) == searchChar) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    private static boolean equalsIgnoreCase(byte a, byte b) {
        return a == b || toLowerCase(a) == toLowerCase(b);
    }

    private static boolean equalsIgnoreCase(char a, char b) {
        return a == b || toLowerCase(a) == toLowerCase(b);
    }

    private static byte toLowerCase(byte b) {
        return isUpperCase(b) ? (byte) (b + 32) : b;
    }

    /**
     * If the character is uppercase - converts the character to lowercase,
     * otherwise returns the character as it is. Only for ASCII characters.
     *
     * @return lowercase ASCII character equivalent
     */

    /**
     * 如果字符是大写字母 - 将其转换为小写字母，
     * 否则返回原字符。仅适用于ASCII字符。
     *
     * @return 小写ASCII字符等价物
     */
    public static char toLowerCase(char c) {
        return isUpperCase(c) ? (char) (c + 32) : c;
    }

    private static byte toUpperCase(byte b) {
        return isLowerCase(b) ? (byte) (b - 32) : b;
    }

    private static boolean isLowerCase(byte value) {
        return value >= 'a' && value <= 'z';
    }

    public static boolean isUpperCase(byte value) {
        return value >= 'A' && value <= 'Z';
    }

    public static boolean isUpperCase(char value) {
        return value >= 'A' && value <= 'Z';
    }

    public static byte c2b(char c) {
        return (byte) ((c > MAX_CHAR_VALUE) ? '?' : c);
    }

    private static byte c2b0(char c) {
        return (byte) c;
    }

    public static char b2c(byte b) {
        return (char) (b & 0xFF);
    }
}
