
package org.top.java.netty.source.util.internal;

/**
 * Math utility methods.
 */

/**
 * 数学工具方法。
 */
public final class MathUtil {

    private MathUtil() {
    }

    /**
     * Fast method of finding the next power of 2 greater than or equal to the supplied value.
     *
     * <p>If the value is {@code <= 0} then 1 will be returned.
     * This method is not suitable for {@link Integer#MIN_VALUE} or numbers greater than 2^30.
     *
     * @param value from which to search for next power of 2
     * @return The next power of 2 or the value itself if it is a power of 2
     */

    /**
     * 快速找到大于或等于给定值的下一个2的幂。
     *
     * <p>如果值 {@code <= 0}，则返回1。
     * 此方法不适用于 {@link Integer#MIN_VALUE} 或大于 2^30 的数字。
     *
     * @param value 从此值开始搜索下一个2的幂
     * @return 下一个2的幂，如果该值本身是2的幂，则返回该值
     */
    public static int findNextPositivePowerOfTwo(final int value) {
        assert value > Integer.MIN_VALUE && value < 0x40000000;
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Fast method of finding the next power of 2 greater than or equal to the supplied value.
     * <p>This method will do runtime bounds checking and call {@link #findNextPositivePowerOfTwo(int)} if within a
     * valid range.
     * @param value from which to search for next power of 2
     * @return The next power of 2 or the value itself if it is a power of 2.
     * <p>Special cases for return values are as follows:
     * <ul>
     *     <li>{@code <= 0} -> 1</li>
     *     <li>{@code >= 2^30} -> 2^30</li>
     * </ul>
     */

    /**
     * 快速找到大于或等于给定值的下一个2的幂次方的方法。
     * <p>此方法将在运行时进行边界检查，并在有效范围内调用 {@link #findNextPositivePowerOfTwo(int)}。
     * @param value 从该值开始搜索下一个2的幂次方
     * @return 下一个2的幂次方，或者如果该值本身是2的幂次方，则返回该值。
     * <p>返回值的特殊情况如下：
     * <ul>
     *     <li>{@code <= 0} -> 1</li>
     *     <li>{@code >= 2^30} -> 2^30</li>
     * </ul>
     */
    public static int safeFindNextPositivePowerOfTwo(final int value) {
        return value <= 0 ? 1 : value >= 0x40000000 ? 0x40000000 : findNextPositivePowerOfTwo(value);
    }

    /**
     * Determine if the requested {@code index} and {@code length} will fit within {@code capacity}.
     * @param index The starting index.
     * @param length The length which will be utilized (starting from {@code index}).
     * @param capacity The capacity that {@code index + length} is allowed to be within.
     * @return {@code false} if the requested {@code index} and {@code length} will fit within {@code capacity}.
     * {@code true} if this would result in an index out of bounds exception.
     */

    /**
     * 判断请求的 {@code index} 和 {@code length} 是否在 {@code capacity} 范围内。
     * @param index 起始索引。
     * @param length 从 {@code index} 开始使用的长度。
     * @param capacity {@code index + length} 允许的最大容量。
     * @return 如果请求的 {@code index} 和 {@code length} 在 {@code capacity} 范围内，返回 {@code false}。
     * 如果会导致索引越界异常，返回 {@code true}。
     */
    public static boolean isOutOfBounds(int index, int length, int capacity) {
        return (index | length | capacity | (index + length) | (capacity - (index + length))) < 0;
    }

    /**
     * Compares two {@code int} values.
     *
     * @param  x the first {@code int} to compare
     * @param  y the second {@code int} to compare
     * @return the value {@code 0} if {@code x == y};
     *         {@code -1} if {@code x < y}; and
     *         {@code 1} if {@code x > y}
     */

    /**
     * 比较两个 {@code int} 值。
     *
     * @param  x 要比较的第一个 {@code int}
     * @param  y 要比较的第二个 {@code int}
     * @return 如果 {@code x == y}，返回 {@code 0}；
     *         如果 {@code x < y}，返回 {@code -1}；
     *         如果 {@code x > y}，返回 {@code 1}
     */
    public static int compare(final int x, final int y) {
        // do not subtract for comparison, it could overflow
        // 不要使用减法进行比较，可能会导致溢出
        return x < y ? -1 : (x > y ? 1 : 0);
    }

    /**
     * Compare two {@code long} values.
     * @param x the first {@code long} to compare.
     * @param y the second {@code long} to compare.
     * @return
     * <ul>
     * <li>0 if {@code x == y}</li>
     * <li>{@code > 0} if {@code x > y}</li>
     * <li>{@code < 0} if {@code x < y}</li>
     * </ul>
     */

    /**
     * 比较两个 {@code long} 值。
     * @param x 要比较的第一个 {@code long}。
     * @param y 要比较的第二个 {@code long}。
     * @return
     * <ul>
     * <li>0 如果 {@code x == y}</li>
     * <li>{@code > 0} 如果 {@code x > y}</li>
     * <li>{@code < 0} 如果 {@code x < y}</li>
     * </ul>
     */
    public static int compare(long x, long y) {
        return (x < y) ? -1 : (x > y) ? 1 : 0;
    }
}
