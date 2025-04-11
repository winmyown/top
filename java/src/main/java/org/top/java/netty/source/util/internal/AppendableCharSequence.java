
package org.top.java.netty.source.util.internal;

import static io.netty.util.internal.ObjectUtil.checkPositive;
import static io.netty.util.internal.ObjectUtil.checkNonEmpty;

import java.util.Arrays;

public final class AppendableCharSequence implements CharSequence, Appendable {
    private char[] chars;
    private int pos;

    public AppendableCharSequence(int length) {
        chars = new char[checkPositive(length, "length")];
    }

    private AppendableCharSequence(char[] chars) {
        this.chars = checkNonEmpty(chars, "chars");
        pos = chars.length;
    }

    public void setLength(int length) {
        if (length < 0 || length > pos) {
            throw new IllegalArgumentException("length: " + length + " (length: >= 0, <= " + pos + ')');
        }
        this.pos = length;
    }

    @Override
    public int length() {
        return pos;
    }

    @Override
    public char charAt(int index) {
        if (index > pos) {
            throw new IndexOutOfBoundsException();
        }
        return chars[index];
    }

    /**
     * Access a value in this {@link CharSequence}.
     * This method is considered unsafe as index values are assumed to be legitimate.
     * Only underlying array bounds checking is done.
     * @param index The index to access the underlying array at.
     * @return The value at {@code index}.
     */

    /**
     * 访问此 {@link CharSequence} 中的值。
     * 此方法被认为是不安全的，因为假定索引值是合法的。
     * 仅进行底层数组的边界检查。
     * @param index 访问底层数组的索引。
     * @return {@code index} 处的值。
     */
    public char charAtUnsafe(int index) {
        return chars[index];
    }

    @Override
    public AppendableCharSequence subSequence(int start, int end) {
        if (start == end) {
            // If start and end index is the same we need to return an empty sequence to conform to the interface.
            // 如果起始索引和结束索引相同，我们需要返回一个空序列以符合接口要求。
            // As our expanding logic depends on the fact that we have a char[] with length > 0 we need to construct
            // 由于我们的扩展逻辑依赖于我们有一个长度 > 0 的 char[]，因此我们需要构建
            // an instance for which this is true.
            // 一个实例，对于该实例这是正确的。
            return new AppendableCharSequence(Math.min(16, chars.length));
        }
        return new AppendableCharSequence(Arrays.copyOfRange(chars, start, end));
    }

    @Override
    public AppendableCharSequence append(char c) {
        if (pos == chars.length) {
            char[] old = chars;
            chars = new char[old.length << 1];
            System.arraycopy(old, 0, chars, 0, old.length);
        }
        chars[pos++] = c;
        return this;
    }

    @Override
    public AppendableCharSequence append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    public AppendableCharSequence append(CharSequence csq, int start, int end) {
        if (csq.length() < end) {
            throw new IndexOutOfBoundsException("expected: csq.length() >= ("
                    + end + "),but actual is (" + csq.length() + ")");
        }
        int length = end - start;
        if (length > chars.length - pos) {
            chars = expand(chars, pos + length, pos);
        }
        if (csq instanceof AppendableCharSequence) {
            // Optimize append operations via array copy
            // 通过数组复制优化追加操作
            AppendableCharSequence seq = (AppendableCharSequence) csq;
            char[] src = seq.chars;
            System.arraycopy(src, start, chars, pos, length);
            pos += length;
            return this;
        }
        for (int i = start; i < end; i++) {
            chars[pos++] = csq.charAt(i);
        }

        return this;
    }

    /**
     * Reset the {@link AppendableCharSequence}. Be aware this will only reset the current internal position and not
     * shrink the internal char array.
     */

    /**
     * 重置 {@link AppendableCharSequence}。请注意，这只会重置当前的内部位置，而不会缩小内部字符数组。
     */
    public void reset() {
        pos = 0;
    }

    @Override
    public String toString() {
        return new String(chars, 0, pos);
    }

    /**
     * Create a new {@link String} from the given start to end.
     */

    /**
     * 从给定的开始到结束创建一个新的 {@link String}。
     */
    public String substring(int start, int end) {
        int length = end - start;
        if (start > pos || length > pos) {
            throw new IndexOutOfBoundsException("expected: start and length <= ("
                    + pos + ")");
        }
        return new String(chars, start, length);
    }

    /**
     * Create a new {@link String} from the given start to end.
     * This method is considered unsafe as index values are assumed to be legitimate.
     * Only underlying array bounds checking is done.
     */

    /**
     * 从给定的起始位置到结束位置创建一个新的 {@link String}。
     * 该方法被认为是不安全的，因为假定索引值是合法的。
     * 仅进行底层数组边界检查。
     */
    public String subStringUnsafe(int start, int end) {
        return new String(chars, start, end - start);
    }

    private static char[] expand(char[] array, int neededSpace, int size) {
        int newCapacity = array.length;
        do {
            // double capacity until it is big enough
            // 双倍容量直到足够大
            newCapacity <<= 1;

            if (newCapacity < 0) {
                throw new IllegalStateException();
            }

        } while (neededSpace > newCapacity);

        char[] newArray = new char[newCapacity];
        System.arraycopy(array, 0, newArray, 0, size);

        return newArray;
    }
}
