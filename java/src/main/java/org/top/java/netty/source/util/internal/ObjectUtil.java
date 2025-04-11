
package org.top.java.netty.source.util.internal;

import java.util.Collection;
import java.util.Map;

/**
 * A grab-bag of useful utility methods.
 */

/**
 * 一个包含各种实用工具方法的杂项集合。
 */
public final class ObjectUtil {

    private static final float FLOAT_ZERO = 0.0F;
    private static final double DOUBLE_ZERO = 0.0D;
    private static final long LONG_ZERO = 0L;
    private static final int INT_ZERO = 0;

    private ObjectUtil() {
    }

    /**
     * Checks that the given argument is not null. If it is, throws {@link NullPointerException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否为null。如果是，抛出 {@link NullPointerException}。
     * 否则，返回该参数。
     */
    public static <T> T checkNotNull(T arg, String text) {
        if (arg == null) {
            throw new NullPointerException(text);
        }
        return arg;
    }

    /**
     * Check that the given varargs is not null and does not contain elements
     * null elements.
     *
     * If it is, throws {@link NullPointerException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的可变参数不为null且不包含null元素。
     *
     * 如果为null或包含null元素，则抛出{@link NullPointerException}。
     * 否则，返回该参数。
     */
    public static <T> T[] deepCheckNotNull(String text, T... varargs) {
        if (varargs == null) {
            throw new NullPointerException(text);
        }

        for (T element : varargs) {
            if (element == null) {
                throw new NullPointerException(text);
            }
        }
        return varargs;
    }

    /**
     * Checks that the given argument is not null. If it is, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否不为null。如果为null，抛出{@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static <T> T checkNotNullWithIAE(final T arg, final String paramName) throws IllegalArgumentException {
        if (arg == null) {
            throw new IllegalArgumentException("Param '" + paramName + "' must not be null");
        }
        return arg;
    }

    /**
     * Checks that the given argument is not null. If it is, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     *
     * @param <T> type of the given argument value.
     * @param name of the parameter, belongs to the exception message.
     * @param index of the array, belongs to the exception message.
     * @param value to check.
     * @return the given argument value.
     * @throws IllegalArgumentException if value is null.
     */

    /**
     * 检查给定的参数是否为 null。如果是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     *
     * @param <T> 给定参数值的类型。
     * @param name 参数的名称，属于异常消息的一部分。
     * @param index 数组的索引，属于异常消息的一部分。
     * @param value 要检查的值。
     * @return 给定的参数值。
     * @throws IllegalArgumentException 如果值为 null。
     */
    public static <T> T checkNotNullArrayParam(T value, int index, String name) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Array index " + index + " of parameter '" + name + "' must not be null");
        }
        return value;
    }

    /**
     * Checks that the given argument is strictly positive. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否严格为正数。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static int checkPositive(int i, String name) {
        if (i <= INT_ZERO) {
            throw new IllegalArgumentException(name + " : " + i + " (expected: > 0)");
        }
        return i;
    }

    /**
     * Checks that the given argument is strictly positive. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否严格为正数。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static long checkPositive(long l, String name) {
        if (l <= LONG_ZERO) {
            throw new IllegalArgumentException(name + " : " + l + " (expected: > 0)");
        }
        return l;
    }

    /**
     * Checks that the given argument is strictly positive. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否严格为正数。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static double checkPositive(final double d, final String name) {
        if (d <= DOUBLE_ZERO) {
            throw new IllegalArgumentException(name + " : " + d + " (expected: > 0)");
        }
        return d;
    }

    /**
     * Checks that the given argument is strictly positive. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否严格为正数。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static float checkPositive(final float f, final String name) {
        if (f <= FLOAT_ZERO) {
            throw new IllegalArgumentException(name + " : " + f + " (expected: > 0)");
        }
        return f;
    }

    /**
     * Checks that the given argument is positive or zero. If it is not , throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否为正数或零。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static int checkPositiveOrZero(int i, String name) {
        if (i < INT_ZERO) {
            throw new IllegalArgumentException(name + " : " + i + " (expected: >= 0)");
        }
        return i;
    }

    /**
     * Checks that the given argument is positive or zero. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否为正值或零。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static long checkPositiveOrZero(long l, String name) {
        if (l < LONG_ZERO) {
            throw new IllegalArgumentException(name + " : " + l + " (expected: >= 0)");
        }
        return l;
    }

    /**
     * Checks that the given argument is positive or zero. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否为正值或零。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static double checkPositiveOrZero(final double d, final String name) {
        if (d < DOUBLE_ZERO) {
            throw new IllegalArgumentException(name + " : " + d + " (expected: >= 0)");
        }
        return d;
    }

    /**
     * Checks that the given argument is positive or zero. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否为正值或零。如果不是，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static float checkPositiveOrZero(final float f, final String name) {
        if (f < FLOAT_ZERO) {
            throw new IllegalArgumentException(name + " : " + f + " (expected: >= 0)");
        }
        return f;
    }

    /**
     * Checks that the given argument is in range. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否在范围内。如果不在范围内，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static int checkInRange(int i, int start, int end, String name) {
        if (i < start || i > end) {
            throw new IllegalArgumentException(name + ": " + i + " (expected: " + start + "-" + end + ")");
        }
        return i;
    }

    /**
     * Checks that the given argument is in range. If it is not, throws {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数是否在范围内。如果不在范围内，则抛出 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static long checkInRange(long l, long start, long end, String name) {
        if (l < start || l > end) {
            throw new IllegalArgumentException(name + ": " + l + " (expected: " + start + "-" + end + ")");
        }
        return l;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static <T> T[] checkNonEmpty(T[] array, String name) {
        //No String concatenation for check
        //不进行字符串拼接检查
        if (checkNotNull(array, name).length == 0) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return array;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static byte[] checkNonEmpty(byte[] array, String name) {
        //No String concatenation for check
        //不进行字符串拼接检查
        if (checkNotNull(array, name).length == 0) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return array;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static char[] checkNonEmpty(char[] array, String name) {
        //No String concatenation for check
        //不进行字符串拼接检查
        if (checkNotNull(array, name).length == 0) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return array;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static <T extends Collection<?>> T checkNonEmpty(T collection, String name) {
        //No String concatenation for check
        //不进行字符串拼接检查
        if (checkNotNull(collection, name).isEmpty()) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return collection;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static String checkNonEmpty(final String value, final String name) {
        if (checkNotNull(value, name).isEmpty()) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return value;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static <K, V, T extends Map<K, V>> T checkNonEmpty(T value, String name) {
        if (checkNotNull(value, name).isEmpty()) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return value;
    }

    /**
     * Checks that the given argument is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the argument.
     */

    /**
     * 检查给定的参数既不为 null 也不为空。
     * 如果为 null 或为空，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回该参数。
     */
    public static CharSequence checkNonEmpty(final CharSequence value, final String name) {
        if (checkNotNull(value, name).length() == 0) {
            throw new IllegalArgumentException("Param '" + name + "' must not be empty");
        }
        return value;
    }

    /**
     * Trims the given argument and checks whether it is neither null nor empty.
     * If it is, throws {@link NullPointerException} or {@link IllegalArgumentException}.
     * Otherwise, returns the trimmed argument.
     *
     * @param value to trim and check.
     * @param name of the parameter.
     * @return the trimmed (not the original) value.
     * @throws NullPointerException if value is null.
     * @throws IllegalArgumentException if the trimmed value is empty.
     */

    /**
     * 修剪给定的参数并检查其是否既不为null也不为空。
     * 如果是，则抛出 {@link NullPointerException} 或 {@link IllegalArgumentException}。
     * 否则，返回修剪后的参数。
     *
     * @param value 要修剪和检查的值。
     * @param name 参数的名称。
     * @return 修剪后的（非原始的）值。
     * @throws NullPointerException 如果值为null。
     * @throws IllegalArgumentException 如果修剪后的值为空。
     */
    public static String checkNonEmptyAfterTrim(final String value, final String name) {
        String trimmed = checkNotNull(value, name).trim();
        return checkNonEmpty(trimmed, name);
    }

    /**
     * Resolves a possibly null Integer to a primitive int, using a default value.
     * @param wrapper the wrapper
     * @param defaultValue the default value
     * @return the primitive value
     */

    /**
     * 将一个可能为空的Integer解析为基本类型int，使用默认值。
     * @param wrapper 包装器
     * @param defaultValue 默认值
     * @return 基本类型的值
     */
    public static int intValue(Integer wrapper, int defaultValue) {
        return wrapper != null ? wrapper : defaultValue;
    }

    /**
     * Resolves a possibly null Long to a primitive long, using a default value.
     * @param wrapper the wrapper
     * @param defaultValue the default value
     * @return the primitive value
     */

    /**
     * 将可能为空的 Long 解析为基本类型 long，使用默认值。
     * @param wrapper 包装类
     * @param defaultValue 默认值
     * @return 基本类型值
     */
    public static long longValue(Long wrapper, long defaultValue) {
        return wrapper != null ? wrapper : defaultValue;
    }
}
