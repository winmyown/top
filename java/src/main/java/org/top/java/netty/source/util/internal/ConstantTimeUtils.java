
package org.top.java.netty.source.util.internal;

public final class ConstantTimeUtils {
    private ConstantTimeUtils() { }

    /**
     * Compare two {@code int}s without leaking timing information.
     * <p>
     * The {@code int} return type is intentional and is designed to allow cascading of constant time operations:
     * <pre>
     *     int l1 = 1;
     *     int l2 = 1;
     *     int l3 = 1;
     *     int l4 = 500;
     *     boolean equals = (equalsConstantTime(l1, l2) & equalsConstantTime(l3, l4)) != 0;
     * </pre>
     * @param x the first value.
     * @param y the second value.
     * @return {@code 0} if not equal. {@code 1} if equal.
     */

    /**
     * 比较两个 {@code int} 值而不泄露时间信息。
     * <p>
     * {@code int} 返回类型是故意的，旨在允许常量时间操作的级联：
     * <pre>
     *     int l1 = 1;
     *     int l2 = 1;
     *     int l3 = 1;
     *     int l4 = 500;
     *     boolean equals = (equalsConstantTime(l1, l2) & equalsConstantTime(l3, l4)) != 0;
     * </pre>
     * @param x 第一个值。
     * @param y 第二个值。
     * @return {@code 0} 如果不相等。{@code 1} 如果相等。
     */
    public static int equalsConstantTime(int x, int y) {
        int z = ~(x ^ y);
        z &= z >> 16;
        z &= z >> 8;
        z &= z >> 4;
        z &= z >> 2;
        z &= z >> 1;
        return z & 1;
    }

    /**
     * Compare two {@code longs}s without leaking timing information.
     * <p>
     * The {@code int} return type is intentional and is designed to allow cascading of constant time operations:
     * <pre>
     *     long l1 = 1;
     *     long l2 = 1;
     *     long l3 = 1;
     *     long l4 = 500;
     *     boolean equals = (equalsConstantTime(l1, l2) & equalsConstantTime(l3, l4)) != 0;
     * </pre>
     * @param x the first value.
     * @param y the second value.
     * @return {@code 0} if not equal. {@code 1} if equal.
     */

    /**
     * 比较两个 {@code long} 值而不泄露时间信息。
     * <p>
     * {@code int} 返回类型是故意的，旨在允许常量时间操作的级联：
     * <pre>
     *     long l1 = 1;
     *     long l2 = 1;
     *     long l3 = 1;
     *     long l4 = 500;
     *     boolean equals = (equalsConstantTime(l1, l2) & equalsConstantTime(l3, l4)) != 0;
     * </pre>
     * @param x 第一个值。
     * @param y 第二个值。
     * @return {@code 0} 如果不相等。{@code 1} 如果相等。
     */
    public static int equalsConstantTime(long x, long y) {
        long z = ~(x ^ y);
        z &= z >> 32;
        z &= z >> 16;
        z &= z >> 8;
        z &= z >> 4;
        z &= z >> 2;
        z &= z >> 1;
        return (int) (z & 1);
    }

    /**
     * Compare two {@code byte} arrays for equality without leaking timing information.
     * For performance reasons no bounds checking on the parameters is performed.
     * <p>
     * The {@code int} return type is intentional and is designed to allow cascading of constant time operations:
     * <pre>
     *     byte[] s1 = new {1, 2, 3};
     *     byte[] s2 = new {1, 2, 3};
     *     byte[] s3 = new {1, 2, 3};
     *     byte[] s4 = new {4, 5, 6};
     *     boolean equals = (equalsConstantTime(s1, 0, s2, 0, s1.length) &
     *                       equalsConstantTime(s3, 0, s4, 0, s3.length)) != 0;
     * </pre>
     * @param bytes1 the first byte array.
     * @param startPos1 the position (inclusive) to start comparing in {@code bytes1}.
     * @param bytes2 the second byte array.
     * @param startPos2 the position (inclusive) to start comparing in {@code bytes2}.
     * @param length the amount of bytes to compare. This is assumed to be validated as not going out of bounds
     * by the caller.
     * @return {@code 0} if not equal. {@code 1} if equal.
     */

    /**
     * 比较两个 {@code byte} 数组是否相等，不泄露时间信息。
     * 出于性能考虑，不对参数进行边界检查。
     * <p>
     * 返回类型为 {@code int} 是故意的，旨在允许常量时间操作的级联：
     * <pre>
     *     byte[] s1 = new {1, 2, 3};
     *     byte[] s2 = new {1, 2, 3};
     *     byte[] s3 = new {1, 2, 3};
     *     byte[] s4 = new {4, 5, 6};
     *     boolean equals = (equalsConstantTime(s1, 0, s2, 0, s1.length) &
     *                       equalsConstantTime(s3, 0, s4, 0, s3.length)) != 0;
     * </pre>
     * @param bytes1 第一个字节数组。
     * @param startPos1 在 {@code bytes1} 中开始比较的位置（包含）。
     * @param bytes2 第二个字节数组。
     * @param startPos2 在 {@code bytes2} 中开始比较的位置（包含）。
     * @param length 要比较的字节数。假设调用者已验证不会越界。
     * @return {@code 0} 如果不相等。{@code 1} 如果相等。
     */
    public static int equalsConstantTime(byte[] bytes1, int startPos1,
                                         byte[] bytes2, int startPos2, int length) {
        // Benchmarking demonstrates that using an int to accumulate is faster than other data types.
        // 基准测试表明，使用 int 进行累加比其他数据类型更快。
        int b = 0;
        final int end = startPos1 + length;
        for (; startPos1 < end; ++startPos1, ++startPos2) {
            b |= bytes1[startPos1] ^ bytes2[startPos2];
        }
        return equalsConstantTime(b, 0);
    }

    /**
     * Compare two {@link CharSequence} objects without leaking timing information.
     * <p>
     * The {@code int} return type is intentional and is designed to allow cascading of constant time operations:
     * <pre>
     *     String s1 = "foo";
     *     String s2 = "foo";
     *     String s3 = "foo";
     *     String s4 = "goo";
     *     boolean equals = (equalsConstantTime(s1, s2) & equalsConstantTime(s3, s4)) != 0;
     * </pre>
     * @param s1 the first value.
     * @param s2 the second value.
     * @return {@code 0} if not equal. {@code 1} if equal.
     */

    /**
     * 比较两个 {@link CharSequence} 对象，不泄露时间信息。
     * <p>
     * {@code int} 返回类型是故意的，旨在允许级联的恒定时间操作：
     * <pre>
     *     String s1 = "foo";
     *     String s2 = "foo";
     *     String s3 = "foo";
     *     String s4 = "goo";
     *     boolean equals = (equalsConstantTime(s1, s2) & equalsConstantTime(s3, s4)) != 0;
     * </pre>
     * @param s1 第一个值。
     * @param s2 第二个值。
     * @return {@code 0} 如果不相等。{@code 1} 如果相等。
     */
    public static int equalsConstantTime(CharSequence s1, CharSequence s2) {
        if (s1.length() != s2.length()) {
            return 0;
        }

        // Benchmarking demonstrates that using an int to accumulate is faster than other data types.

        // 基准测试表明，使用 int 进行累加比其他数据类型更快。
        int c = 0;
        for (int i = 0; i < s1.length(); ++i) {
            c |= s1.charAt(i) ^ s2.charAt(i);
        }
        return equalsConstantTime(c, 0);
    }
}
