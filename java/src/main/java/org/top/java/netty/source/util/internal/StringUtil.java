
package org.top.java.netty.source.util.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.*;

/**
 * String utility class.
 */

/**
 * 字符串工具类。
 */
public final class StringUtil {

    public static final String EMPTY_STRING = "";
    public static final String NEWLINE = SystemPropertyUtil.get("line.separator", "\n");

    public static final char DOUBLE_QUOTE = '\"';
    public static final char COMMA = ',';
    public static final char LINE_FEED = '\n';
    public static final char CARRIAGE_RETURN = '\r';
    public static final char TAB = '\t';
    public static final char SPACE = 0x20;

    private static final String[] BYTE2HEX_PAD = new String[256];
    private static final String[] BYTE2HEX_NOPAD = new String[256];
    private static final byte[] HEX2B;

    /**
     * 2 - Quote character at beginning and end.
     * 5 - Extra allowance for anticipated escape characters that may be added.
     */

    /**
     * 2 - 开头和结尾的引号字符。
     * 5 - 为可能添加的转义字符预留额外空间。
     */
    private static final int CSV_NUMBER_ESCAPE_CHARACTERS = 2 + 5;
    private static final char PACKAGE_SEPARATOR_CHAR = '.';

    static {
        // Generate the lookup table that converts a byte into a 2-digit hexadecimal integer.
        // 生成将字节转换为2位十六进制整数的查找表。
        for (int i = 0; i < BYTE2HEX_PAD.length; i++) {
            String str = Integer.toHexString(i);
            BYTE2HEX_PAD[i] = i > 0xf ? str : ('0' + str);
            BYTE2HEX_NOPAD[i] = str;
        }
        // Generate the lookup table that converts an hex char into its decimal value:
        // 生成将十六进制字符转换为其十进制值的查找表：
        // the size of the table is such that the JVM is capable of save any bounds-check
        // 表的大小使得JVM能够保存任何边界检查
        // if a char type is used as an index.
        // 如果使用 char 类型作为索引。
        HEX2B = new byte[Character.MAX_VALUE + 1];
        Arrays.fill(HEX2B, (byte) -1);
        HEX2B['0'] = (byte) 0;
        HEX2B['1'] = (byte) 1;
        HEX2B['2'] = (byte) 2;
        HEX2B['3'] = (byte) 3;
        HEX2B['4'] = (byte) 4;
        HEX2B['5'] = (byte) 5;
        HEX2B['6'] = (byte) 6;
        HEX2B['7'] = (byte) 7;
        HEX2B['8'] = (byte) 8;
        HEX2B['9'] = (byte) 9;
        HEX2B['A'] = (byte) 10;
        HEX2B['B'] = (byte) 11;
        HEX2B['C'] = (byte) 12;
        HEX2B['D'] = (byte) 13;
        HEX2B['E'] = (byte) 14;
        HEX2B['F'] = (byte) 15;
        HEX2B['a'] = (byte) 10;
        HEX2B['b'] = (byte) 11;
        HEX2B['c'] = (byte) 12;
        HEX2B['d'] = (byte) 13;
        HEX2B['e'] = (byte) 14;
        HEX2B['f'] = (byte) 15;
    }

    private StringUtil() {
        // Unused.
        // 未使用。
    }

    /**
     * Get the item after one char delim if the delim is found (else null).
     * This operation is a simplified and optimized
     * version of {@link String#split(String, int)}.
     */

    /**
     * 如果找到分隔符，则获取分隔符后的一个字符项（否则返回 null）。
     * 此操作是 {@link String#split(String, int)} 的简化优化版本。
     */
    public static String substringAfter(String value, char delim) {
        int pos = value.indexOf(delim);
        if (pos >= 0) {
            return value.substring(pos + 1);
        }
        return null;
    }

    /**
     * Get the item before one char delim if the delim is found (else null).
     * This operation is a simplified and optimized
     * version of {@link String#split(String, int)}.
     */

    /**
     * 如果找到分隔符，则获取分隔符前的一个字符项（否则返回 null）。
     * 此操作是 {@link String#split(String, int)} 的简化且优化版本。
     */
    public static String substringBefore(String value, char delim) {
        int pos = value.indexOf(delim);
        if (pos >= 0) {
            return value.substring(0, pos);
        }
        return null;
    }

    /**
     * Checks if two strings have the same suffix of specified length
     *
     * @param s   string
     * @param p   string
     * @param len length of the common suffix
     * @return true if both s and p are not null and both have the same suffix. Otherwise - false
     */

    /**
     * 检查两个字符串是否具有指定长度的相同后缀
     *
     * @param s   字符串
     * @param p   字符串
     * @param len 公共后缀的长度
     * @return 如果 s 和 p 都不为 null 且都具有相同的后缀，则返回 true。否则返回 false
     */
    public static boolean commonSuffixOfLength(String s, String p, int len) {
        return s != null && p != null && len >= 0 && s.regionMatches(s.length() - len, p, p.length() - len, len);
    }

    /**
     * Converts the specified byte value into a 2-digit hexadecimal integer.
     */

    /**
     * 将指定的字节值转换为2位十六进制整数。
     */
    public static String byteToHexStringPadded(int value) {
        return BYTE2HEX_PAD[value & 0xff];
    }

    /**
     * Converts the specified byte value into a 2-digit hexadecimal integer and appends it to the specified buffer.
     */

    /**
     * 将指定的字节值转换为2位十六进制整数并追加到指定的缓冲区。
     */
    public static <T extends Appendable> T byteToHexStringPadded(T buf, int value) {
        try {
            buf.append(byteToHexStringPadded(value));
        } catch (IOException e) {
            PlatformDependent.throwException(e);
        }
        return buf;
    }

    /**
     * Converts the specified byte array into a hexadecimal value.
     */

    /**
     * 将指定的字节数组转换为十六进制值。
     */
    public static String toHexStringPadded(byte[] src) {
        return toHexStringPadded(src, 0, src.length);
    }

    /**
     * Converts the specified byte array into a hexadecimal value.
     */

    /**
     * 将指定的字节数组转换为十六进制值。
     */
    public static String toHexStringPadded(byte[] src, int offset, int length) {
        return toHexStringPadded(new StringBuilder(length << 1), src, offset, length).toString();
    }

    /**
     * Converts the specified byte array into a hexadecimal value and appends it to the specified buffer.
     */

    /**
     * 将指定的字节数组转换为十六进制值并将其附加到指定的缓冲区。
     */
    public static <T extends Appendable> T toHexStringPadded(T dst, byte[] src) {
        return toHexStringPadded(dst, src, 0, src.length);
    }

    /**
     * Converts the specified byte array into a hexadecimal value and appends it to the specified buffer.
     */

    /**
     * 将指定的字节数组转换为十六进制值并将其附加到指定的缓冲区。
     */
    public static <T extends Appendable> T toHexStringPadded(T dst, byte[] src, int offset, int length) {
        final int end = offset + length;
        for (int i = offset; i < end; i++) {
            byteToHexStringPadded(dst, src[i]);
        }
        return dst;
    }

    /**
     * Converts the specified byte value into a hexadecimal integer.
     */

    /**
     * 将指定的字节值转换为十六进制整数。
     */
    public static String byteToHexString(int value) {
        return BYTE2HEX_NOPAD[value & 0xff];
    }

    /**
     * Converts the specified byte value into a hexadecimal integer and appends it to the specified buffer.
     */

    /**
     * 将指定的字节值转换为十六进制整数并追加到指定的缓冲区。
     */
    public static <T extends Appendable> T byteToHexString(T buf, int value) {
        try {
            buf.append(byteToHexString(value));
        } catch (IOException e) {
            PlatformDependent.throwException(e);
        }
        return buf;
    }

    /**
     * Converts the specified byte array into a hexadecimal value.
     */

    /**
     * 将指定的字节数组转换为十六进制值。
     */
    public static String toHexString(byte[] src) {
        return toHexString(src, 0, src.length);
    }

    /**
     * Converts the specified byte array into a hexadecimal value.
     */

    /**
     * 将指定的字节数组转换为十六进制值。
     */
    public static String toHexString(byte[] src, int offset, int length) {
        return toHexString(new StringBuilder(length << 1), src, offset, length).toString();
    }

    /**
     * Converts the specified byte array into a hexadecimal value and appends it to the specified buffer.
     */

    /**
     * 将指定的字节数组转换为十六进制值并将其附加到指定的缓冲区。
     */
    public static <T extends Appendable> T toHexString(T dst, byte[] src) {
        return toHexString(dst, src, 0, src.length);
    }

    /**
     * Converts the specified byte array into a hexadecimal value and appends it to the specified buffer.
     */

    /**
     * 将指定的字节数组转换为十六进制值并将其附加到指定的缓冲区。
     */
    public static <T extends Appendable> T toHexString(T dst, byte[] src, int offset, int length) {
        assert length >= 0;
        if (length == 0) {
            return dst;
        }

        final int end = offset + length;
        final int endMinusOne = end - 1;
        int i;

        // Skip preceding zeroes.

        // 跳过前导零。
        for (i = offset; i < endMinusOne; i++) {
            if (src[i] != 0) {
                break;
            }
        }

        byteToHexString(dst, src[i++]);
        int remaining = end - i;
        toHexStringPadded(dst, src, i, remaining);

        return dst;
    }

    /**
     * Helper to decode half of a hexadecimal number from a string.
     * @param c The ASCII character of the hexadecimal number to decode.
     * Must be in the range {@code [0-9a-fA-F]}.
     * @return The hexadecimal value represented in the ASCII character
     * given, or {@code -1} if the character is invalid.
     */

    /**
     * 辅助方法，用于解码字符串中的半个十六进制数字。
     * @param c 要解码的十六进制数字的ASCII字符。
     * 必须在范围 {@code [0-9a-fA-F]} 内。
     * @return 由给定的ASCII字符表示的十六进制值，如果字符无效则返回 {@code -1}。
     */
    public static int decodeHexNibble(final char c) {
        assert HEX2B.length == (Character.MAX_VALUE + 1);
        // Character.digit() is not used here, as it addresses a larger
        // Character.digit() 在这里没有使用，因为它处理的是更大的
        // set of characters (both ASCII and full-width latin letters).
        // 字符集（包括ASCII和全角拉丁字母）。
        return HEX2B[c];
    }

    /**
     * Decode a 2-digit hex byte from within a string.
     */

    /**
     * 从字符串中解码一个2位的十六进制字节。
     */
    public static byte decodeHexByte(CharSequence s, int pos) {
        int hi = decodeHexNibble(s.charAt(pos));
        int lo = decodeHexNibble(s.charAt(pos + 1));
        if (hi == -1 || lo == -1) {
            throw new IllegalArgumentException(String.format(
                    "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
        }
        return (byte) ((hi << 4) + lo);
    }

    /**
     * Decodes part of a string with <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     *
     * @param hexDump a {@link CharSequence} which contains the hex dump
     * @param fromIndex start of hex dump in {@code hexDump}
     * @param length hex string length
     */

    /**
     * 解码部分包含<a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>的字符串
     *
     * @param hexDump 包含hex dump的{@link CharSequence}
     * @param fromIndex hex dump在{@code hexDump}中的起始位置
     * @param length hex字符串长度
     */
    public static byte[] decodeHexDump(CharSequence hexDump, int fromIndex, int length) {
        if (length < 0 || (length & 1) != 0) {
            throw new IllegalArgumentException("length: " + length);
        }
        if (length == 0) {
            return EmptyArrays.EMPTY_BYTES;
        }
        byte[] bytes = new byte[length >>> 1];
        for (int i = 0; i < length; i += 2) {
            bytes[i >>> 1] = decodeHexByte(hexDump, fromIndex + i);
        }
        return bytes;
    }

    /**
     * Decodes a <a href="https://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     */

    /**
     * 解码一个<a href="https://en.wikipedia.org/wiki/Hex_dump">十六进制转储</a>
     */
    public static byte[] decodeHexDump(CharSequence hexDump) {
        return decodeHexDump(hexDump, 0, hexDump.length());
    }

    /**
     * The shortcut to {@link #simpleClassName(Class) simpleClassName(o.getClass())}.
     */

    /**
     * {@link #simpleClassName(Class) simpleClassName(o.getClass())} 的快捷方式。
     */
    public static String simpleClassName(Object o) {
        if (o == null) {
            return "null_object";
        } else {
            return simpleClassName(o.getClass());
        }
    }

    /**
     * Generates a simplified name from a {@link Class}.  Similar to {@link Class#getSimpleName()}, but it works fine
     * with anonymous classes.
     */

    /**
     * 从 {@link Class} 生成一个简化的名称。类似于 {@link Class#getSimpleName()}，但它可以很好地处理匿名类。
     */
    public static String simpleClassName(Class<?> clazz) {
        String className = checkNotNull(clazz, "clazz").getName();
        final int lastDotIdx = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);
        if (lastDotIdx > -1) {
            return className.substring(lastDotIdx + 1);
        }
        return className;
    }

    /**
     * Escapes the specified value, if necessary according to
     * <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>.
     *
     * @param value The value which will be escaped according to
     *              <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>
     * @return {@link CharSequence} the escaped value if necessary, or the value unchanged
     */

    /**
     * 根据<a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>，对指定值进行转义（如有必要）。
     *
     * @param value 根据<a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>进行转义的值
     * @return {@link CharSequence} 如有必要则返回转义后的值，否则返回原值
     */
    public static CharSequence escapeCsv(CharSequence value) {
        return escapeCsv(value, false);
    }

    /**
     * Escapes the specified value, if necessary according to
     * <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>.
     *
     * @param value          The value which will be escaped according to
     *                       <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>
     * @param trimWhiteSpace The value will first be trimmed of its optional white-space characters,
     *                       according to <a href="https://tools.ietf.org/html/rfc7230#section-7">RFC-7230</a>
     * @return {@link CharSequence} the escaped value if necessary, or the value unchanged
     */

    /**
     * 根据 <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a> 的规定，
     * 如有必要，对指定的值进行转义。
     *
     * @param value          将根据 <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>
     *                       进行转义的值
     * @param trimWhiteSpace 该值将首先根据 <a href="https://tools.ietf.org/html/rfc7230#section-7">RFC-7230</a>
     *                       的规定，去除其可选的空白字符
     * @return {@link CharSequence} 如有必要，返回转义后的值，否则返回未更改的值
     */
    public static CharSequence escapeCsv(CharSequence value, boolean trimWhiteSpace) {
        int length = checkNotNull(value, "value").length();
        int start;
        int last;
        if (trimWhiteSpace) {
            start = indexOfFirstNonOwsChar(value, length);
            last = indexOfLastNonOwsChar(value, start, length);
        } else {
            start = 0;
            last = length - 1;
        }
        if (start > last) {
            return EMPTY_STRING;
        }

        int firstUnescapedSpecial = -1;
        boolean quoted = false;
        if (isDoubleQuote(value.charAt(start))) {
            quoted = isDoubleQuote(value.charAt(last)) && last > start;
            if (quoted) {
                start++;
                last--;
            } else {
                firstUnescapedSpecial = start;
            }
        }

        if (firstUnescapedSpecial < 0) {
            if (quoted) {
                for (int i = start; i <= last; i++) {
                    if (isDoubleQuote(value.charAt(i))) {
                        if (i == last || !isDoubleQuote(value.charAt(i + 1))) {
                            firstUnescapedSpecial = i;
                            break;
                        }
                        i++;
                    }
                }
            } else {
                for (int i = start; i <= last; i++) {
                    char c = value.charAt(i);
                    if (c == LINE_FEED || c == CARRIAGE_RETURN || c == COMMA) {
                        firstUnescapedSpecial = i;
                        break;
                    }
                    if (isDoubleQuote(c)) {
                        if (i == last || !isDoubleQuote(value.charAt(i + 1))) {
                            firstUnescapedSpecial = i;
                            break;
                        }
                        i++;
                    }
                }
            }

            if (firstUnescapedSpecial < 0) {
                // Special characters is not found or all of them already escaped.
                // 未找到特殊字符或所有字符均已转义。
                // In the most cases returns a same string. New string will be instantiated (via StringBuilder)
                // 在大多数情况下返回相同的字符串。新字符串将通过 StringBuilder 实例化。
                // only if it really needed. It's important to prevent GC extra load.
                // 仅在真正需要时使用。防止GC额外负载很重要。
                return quoted? value.subSequence(start - 1, last + 2) : value.subSequence(start, last + 1);
            }
        }

        StringBuilder result = new StringBuilder(last - start + 1 + CSV_NUMBER_ESCAPE_CHARACTERS);
        result.append(DOUBLE_QUOTE).append(value, start, firstUnescapedSpecial);
        for (int i = firstUnescapedSpecial; i <= last; i++) {
            char c = value.charAt(i);
            if (isDoubleQuote(c)) {
                result.append(DOUBLE_QUOTE);
                if (i < last && isDoubleQuote(value.charAt(i + 1))) {
                    i++;
                }
            }
            result.append(c);
        }
        return result.append(DOUBLE_QUOTE);
    }

    /**
     * Unescapes the specified escaped CSV field, if necessary according to
     * <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>.
     *
     * @param value The escaped CSV field which will be unescaped according to
     *              <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>
     * @return {@link CharSequence} the unescaped value if necessary, or the value unchanged
     */

    /**
     * 根据<a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>，如有必要，对指定的已转义CSV字段进行反转义。
     *
     * @param value 已转义的CSV字段，将根据<a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>进行反转义
     * @return {@link CharSequence} 如有必要则返回反转义后的值，否则返回原值
     */
    public static CharSequence unescapeCsv(CharSequence value) {
        int length = checkNotNull(value, "value").length();
        if (length == 0) {
            return value;
        }
        int last = length - 1;
        boolean quoted = isDoubleQuote(value.charAt(0)) && isDoubleQuote(value.charAt(last)) && length != 1;
        if (!quoted) {
            validateCsvFormat(value);
            return value;
        }
        StringBuilder unescaped = InternalThreadLocalMap.get().stringBuilder();
        for (int i = 1; i < last; i++) {
            char current = value.charAt(i);
            if (current == DOUBLE_QUOTE) {
                if (isDoubleQuote(value.charAt(i + 1)) && (i + 1) != last) {
                    // Followed by a double-quote but not the last character
                    // 后跟双引号但不是最后一个字符
                    // Just skip the next double-quote
                    // 只需跳过下一个双引号
                    i++;
                } else {
                    // Not followed by a double-quote or the following double-quote is the last character
                    // 不跟双引号或后面的双引号是最后一个字符
                    throw newInvalidEscapedCsvFieldException(value, i);
                }
            }
            unescaped.append(current);
        }
        return unescaped.toString();
    }

    /**
     * Unescapes the specified escaped CSV fields according to
     * <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>.
     *
     * @param value A string with multiple CSV escaped fields which will be unescaped according to
     *              <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a>
     * @return {@link List} the list of unescaped fields
     */

    /**
     * 根据 <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a> 对指定的转义 CSV 字段进行反转义。
     *
     * @param value 一个包含多个 CSV 转义字段的字符串，将根据
     *              <a href="https://tools.ietf.org/html/rfc4180#section-2">RFC-4180</a> 进行反转义
     * @return {@link List} 反转义后的字段列表
     */
    public static List<CharSequence> unescapeCsvFields(CharSequence value) {
        List<CharSequence> unescaped = new ArrayList<CharSequence>(2);
        StringBuilder current = InternalThreadLocalMap.get().stringBuilder();
        boolean quoted = false;
        int last = value.length() - 1;
        for (int i = 0; i <= last; i++) {
            char c = value.charAt(i);
            if (quoted) {
                switch (c) {
                    case DOUBLE_QUOTE:
                        if (i == last) {
                            // Add the last field and return
                            // 添加最后一个字段并返回
                            unescaped.add(current.toString());
                            return unescaped;
                        }
                        char next = value.charAt(++i);
                        if (next == DOUBLE_QUOTE) {
                            // 2 double-quotes should be unescaped to one
                            // 2 个双引号应被转义为一个
                            current.append(DOUBLE_QUOTE);
                            break;
                        }
                        if (next == COMMA) {
                            // This is the end of a field. Let's start to parse the next field.
                            // 这是字段的结尾。让我们开始解析下一个字段。
                            quoted = false;
                            unescaped.add(current.toString());
                            current.setLength(0);
                            break;
                        }
                        // double-quote followed by other character is invalid
                        // 双引号后跟其他字符是无效的
                        throw newInvalidEscapedCsvFieldException(value, i - 1);
                    default:
                        current.append(c);
                }
            } else {
                switch (c) {
                    case COMMA:
                        // Start to parse the next field
                        // 开始解析下一个字段
                        unescaped.add(current.toString());
                        current.setLength(0);
                        break;
                    case DOUBLE_QUOTE:
                        if (current.length() == 0) {
                            quoted = true;
                            break;
                        }
                        // double-quote appears without being enclosed with double-quotes
                        // 双引号未用双引号括起来出现
                        // fall through
                        // 穿透
                    case LINE_FEED:
                        // fall through
                        // 穿透
                    case CARRIAGE_RETURN:
                        // special characters appears without being enclosed with double-quotes
                        // 特殊字符出现时未用双引号括起来
                        throw newInvalidEscapedCsvFieldException(value, i);
                    default:
                        current.append(c);
                }
            }
        }
        if (quoted) {
            throw newInvalidEscapedCsvFieldException(value, last);
        }
        unescaped.add(current.toString());
        return unescaped;
    }

    /**
     * Validate if {@code value} is a valid csv field without double-quotes.
     *
     * @throws IllegalArgumentException if {@code value} needs to be encoded with double-quotes.
     */

    /**
     * 验证 {@code value} 是否是不带双引号的有效 csv 字段。
     *
     * @throws IllegalArgumentException 如果 {@code value} 需要用双引号编码。
     */
    private static void validateCsvFormat(CharSequence value) {
        int length = value.length();
        for (int i = 0; i < length; i++) {
            switch (value.charAt(i)) {
                case DOUBLE_QUOTE:
                case LINE_FEED:
                case CARRIAGE_RETURN:
                case COMMA:
                    // If value contains any special character, it should be enclosed with double-quotes
                    // 如果值包含任何特殊字符，应使用双引号将其括起来
                    throw newInvalidEscapedCsvFieldException(value, i);
                default:
            }
        }
    }

    private static IllegalArgumentException newInvalidEscapedCsvFieldException(CharSequence value, int index) {
        return new IllegalArgumentException("invalid escaped CSV field: " + value + " index: " + index);
    }

    /**
     * Get the length of a string, {@code null} input is considered {@code 0} length.
     */

    /**
     * 获取字符串的长度，{@code null} 输入被视为长度为 {@code 0}。
     */
    public static int length(String s) {
        return s == null ? 0 : s.length();
    }

    /**
     * Determine if a string is {@code null} or {@link String#isEmpty()} returns {@code true}.
     */

    /**
     * 判断字符串是否为 {@code null} 或 {@link String#isEmpty()} 返回 {@code true}。
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Find the index of the first non-white space character in {@code s} starting at {@code offset}.
     *
     * @param seq    The string to search.
     * @param offset The offset to start searching at.
     * @return the index of the first non-white space character or &lt;{@code -1} if none was found.
     */

    /**
     * 在 {@code seq} 中从 {@code offset} 开始查找第一个非空白字符的索引。
     *
     * @param seq    要搜索的字符串。
     * @param offset 开始搜索的偏移量。
     * @return 第一个非空白字符的索引，如果未找到则返回 {@code -1}。
     */
    public static int indexOfNonWhiteSpace(CharSequence seq, int offset) {
        for (; offset < seq.length(); ++offset) {
            if (!Character.isWhitespace(seq.charAt(offset))) {
                return offset;
            }
        }
        return -1;
    }

    /**
     * Find the index of the first white space character in {@code s} starting at {@code offset}.
     *
     * @param seq    The string to search.
     * @param offset The offset to start searching at.
     * @return the index of the first white space character or &lt;{@code -1} if none was found.
     */

    /**
     * 在 {@code seq} 中从 {@code offset} 开始查找第一个空白字符的索引。
     *
     * @param seq    要搜索的字符串。
     * @param offset 开始搜索的偏移量。
     * @return 第一个空白字符的索引，如果未找到则返回 &lt;{@code -1}。
     */
    public static int indexOfWhiteSpace(CharSequence seq, int offset) {
        for (; offset < seq.length(); ++offset) {
            if (Character.isWhitespace(seq.charAt(offset))) {
                return offset;
            }
        }
        return -1;
    }

    /**
     * Determine if {@code c} lies within the range of values defined for
     * <a href="https://unicode.org/glossary/#surrogate_code_point">Surrogate Code Point</a>.
     *
     * @param c the character to check.
     * @return {@code true} if {@code c} lies within the range of values defined for
     * <a href="https://unicode.org/glossary/#surrogate_code_point">Surrogate Code Point</a>. {@code false} otherwise.
     */

    /**
     * 判断 {@code c} 是否位于 <a href="https://unicode.org/glossary/#surrogate_code_point">代理码点</a> 定义的值范围内。
     *
     * @param c 要检查的字符。
     * @return 如果 {@code c} 位于 <a href="https://unicode.org/glossary/#surrogate_code_point">代理码点</a> 定义的值范围内，则返回 {@code true}。否则返回 {@code false}。
     */
    public static boolean isSurrogate(char c) {
        return c >= '\uD800' && c <= '\uDFFF';
    }

    private static boolean isDoubleQuote(char c) {
        return c == DOUBLE_QUOTE;
    }

    /**
     * Determine if the string {@code s} ends with the char {@code c}.
     *
     * @param s the string to test
     * @param c the tested char
     * @return true if {@code s} ends with the char {@code c}
     */

    /**
     * 判断字符串 {@code s} 是否以字符 {@code c} 结尾。
     *
     * @param s 要测试的字符串
     * @param c 测试的字符
     * @return 如果 {@code s} 以字符 {@code c} 结尾则返回 true
     */
    public static boolean endsWith(CharSequence s, char c) {
        int len = s.length();
        return len > 0 && s.charAt(len - 1) == c;
    }

    /**
     * Trim optional white-space characters from the specified value,
     * according to <a href="https://tools.ietf.org/html/rfc7230#section-7">RFC-7230</a>.
     *
     * @param value the value to trim
     * @return {@link CharSequence} the trimmed value if necessary, or the value unchanged
     */

    /**
     * 根据 <a href="https://tools.ietf.org/html/rfc7230#section-7">RFC-7230</a> 的规定，
     * 从指定的值中去除可选的空白字符。
     *
     * @param value 需要去除空白字符的值
     * @return {@link CharSequence} 必要时返回去除空白字符后的值，否则返回原值
     */
    public static CharSequence trimOws(CharSequence value) {
        final int length = value.length();
        if (length == 0) {
            return value;
        }
        int start = indexOfFirstNonOwsChar(value, length);
        int end = indexOfLastNonOwsChar(value, start, length);
        return start == 0 && end == length - 1 ? value : value.subSequence(start, end + 1);
    }

    /**
     * Returns a char sequence that contains all {@code elements} joined by a given separator.
     *
     * @param separator for each element
     * @param elements to join together
     *
     * @return a char sequence joined by a given separator.
     */

    /**
     * 返回一个包含所有 {@code elements} 并通过给定分隔符连接的字符序列。
     *
     * @param separator 每个元素之间的分隔符
     * @param elements 要连接的元素
     *
     * @return 通过给定分隔符连接的字符序列。
     */
    public static CharSequence join(CharSequence separator, Iterable<? extends CharSequence> elements) {
        ObjectUtil.checkNotNull(separator, "separator");
        ObjectUtil.checkNotNull(elements, "elements");

        Iterator<? extends CharSequence> iterator = elements.iterator();
        if (!iterator.hasNext()) {
            return EMPTY_STRING;
        }

        CharSequence firstElement = iterator.next();
        if (!iterator.hasNext()) {
            return firstElement;
        }

        StringBuilder builder = new StringBuilder(firstElement);
        do {
            builder.append(separator).append(iterator.next());
        } while (iterator.hasNext());

        return builder;
    }

    /**
     * @return {@code length} if no OWS is found.
     */

    /**
     * @return {@code length} 如果没有找到OWS。
     */
    private static int indexOfFirstNonOwsChar(CharSequence value, int length) {
        int i = 0;
        while (i < length && isOws(value.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * @return {@code start} if no OWS is found.
     */

    /**
     * @return {@code start} 如果未找到OWS。
     */
    private static int indexOfLastNonOwsChar(CharSequence value, int start, int length) {
        int i = length - 1;
        while (i > start && isOws(value.charAt(i))) {
            i--;
        }
        return i;
    }

    private static boolean isOws(char c) {
        return c == SPACE || c == TAB;
    }

}
