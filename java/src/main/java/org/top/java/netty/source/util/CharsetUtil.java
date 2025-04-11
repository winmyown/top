
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.InternalThreadLocalMap;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Map;

/**
 * A utility class that provides various common operations and constants
 * related with {@link Charset} and its relevant classes.
 */

/**
 * 一个实用工具类，提供与 {@link Charset} 及其相关类的各种常见操作和常量。
 */
public final class CharsetUtil {

    /**
     * 16-bit UTF (UCS Transformation Format) whose byte order is identified by
     * an optional byte-order mark
     */

    /**
     * 16位UTF（UCS转换格式），其字节顺序由可选的字节顺序标记标识
     */
    public static final Charset UTF_16 = Charset.forName("UTF-16");

    /**
     * 16-bit UTF (UCS Transformation Format) whose byte order is big-endian
     */

    /**
     * 16位UTF（UCS转换格式），其字节顺序为大端序
     */
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");

    /**
     * 16-bit UTF (UCS Transformation Format) whose byte order is little-endian
     */

    /**
     * 16位UTF（UCS转换格式），其字节顺序为小端序
     */
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

    /**
     * 8-bit UTF (UCS Transformation Format)
     */

    /**
     * 8位UTF（UCS转换格式）
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * ISO Latin Alphabet No. 1, as known as <tt>ISO-LATIN-1</tt>
     */

    /**
     * ISO拉丁字母表第1号，也称为<tt>ISO-LATIN-1</tt>
     */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     * 7-bit ASCII, as known as ISO646-US or the Basic Latin block of the
     * Unicode character set
     */

    /**
     * 7-bit ASCII，也称为ISO646-US或Unicode字符集的基本拉丁块
     */
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final Charset[] CHARSETS = new Charset[]
            { UTF_16, UTF_16BE, UTF_16LE, UTF_8, ISO_8859_1, US_ASCII };

    public static Charset[] values() {
        return CHARSETS;
    }

    /**
     * @deprecated Use {@link #encoder(Charset)}.
     */

    /**
     * @deprecated 使用 {@link #encoder(Charset)}。
     */
    @Deprecated
    public static CharsetEncoder getEncoder(Charset charset) {
        return encoder(charset);
    }

    /**
     * Returns a new {@link CharsetEncoder} for the {@link Charset} with specified error actions.
     *
     * @param charset The specified charset
     * @param malformedInputAction The encoder's action for malformed-input errors
     * @param unmappableCharacterAction The encoder's action for unmappable-character errors
     * @return The encoder for the specified {@code charset}
     */

    /**
     * 返回一个用于指定字符集的新的{@link CharsetEncoder}，并指定错误处理动作。
     *
     * @param charset 指定的字符集
     * @param malformedInputAction 编码器对错误输入的处理动作
     * @param unmappableCharacterAction 编码器对无法映射字符的处理动作
     * @return 指定字符集的编码器
     */
    public static CharsetEncoder encoder(Charset charset, CodingErrorAction malformedInputAction,
                                         CodingErrorAction unmappableCharacterAction) {
        checkNotNull(charset, "charset");
        CharsetEncoder e = charset.newEncoder();
        e.onMalformedInput(malformedInputAction).onUnmappableCharacter(unmappableCharacterAction);
        return e;
    }

    /**
     * Returns a new {@link CharsetEncoder} for the {@link Charset} with the specified error action.
     *
     * @param charset The specified charset
     * @param codingErrorAction The encoder's action for malformed-input and unmappable-character errors
     * @return The encoder for the specified {@code charset}
     */

    /**
     * 返回一个指定字符集的 {@link CharsetEncoder}，并设置错误处理动作。
     *
     * @param charset 指定的字符集
     * @param codingErrorAction 编码器对错误输入和无法映射字符的处理动作
     * @return 指定 {@code charset} 的编码器
     */
    public static CharsetEncoder encoder(Charset charset, CodingErrorAction codingErrorAction) {
        return encoder(charset, codingErrorAction, codingErrorAction);
    }

    /**
     * Returns a cached thread-local {@link CharsetEncoder} for the specified {@link Charset}.
     *
     * @param charset The specified charset
     * @return The encoder for the specified {@code charset}
     */

    /**
     * 返回指定 {@link Charset} 的线程本地缓存的 {@link CharsetEncoder}。
     *
     * @param charset 指定的字符集
     * @return 指定 {@code charset} 的编码器
     */
    public static CharsetEncoder encoder(Charset charset) {
        checkNotNull(charset, "charset");

        Map<Charset, CharsetEncoder> map = InternalThreadLocalMap.get().charsetEncoderCache();
        CharsetEncoder e = map.get(charset);
        if (e != null) {
            e.reset().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            return e;
        }

        e = encoder(charset, CodingErrorAction.REPLACE, CodingErrorAction.REPLACE);
        map.put(charset, e);
        return e;
    }

    /**
     * @deprecated Use {@link #decoder(Charset)}.
     */

    /**
     * @deprecated 请使用 {@link #decoder(Charset)}。
     */
    @Deprecated
    public static CharsetDecoder getDecoder(Charset charset) {
        return decoder(charset);
    }

    /**
     * Returns a new {@link CharsetDecoder} for the {@link Charset} with specified error actions.
     *
     * @param charset The specified charset
     * @param malformedInputAction The decoder's action for malformed-input errors
     * @param unmappableCharacterAction The decoder's action for unmappable-character errors
     * @return The decoder for the specified {@code charset}
     */

    /**
     * 返回一个用于指定字符集的新的{@link CharsetDecoder}，并指定错误处理动作。
     *
     * @param charset 指定的字符集
     * @param malformedInputAction 解码器对错误输入的处理动作
     * @param unmappableCharacterAction 解码器对无法映射字符的处理动作
     * @return 指定{@code charset}的解码器
     */
    public static CharsetDecoder decoder(Charset charset, CodingErrorAction malformedInputAction,
                                         CodingErrorAction unmappableCharacterAction) {
        checkNotNull(charset, "charset");
        CharsetDecoder d = charset.newDecoder();
        d.onMalformedInput(malformedInputAction).onUnmappableCharacter(unmappableCharacterAction);
        return d;
    }

    /**
     * Returns a new {@link CharsetDecoder} for the {@link Charset} with the specified error action.
     *
     * @param charset The specified charset
     * @param codingErrorAction The decoder's action for malformed-input and unmappable-character errors
     * @return The decoder for the specified {@code charset}
     */

    /**
     * 返回一个用于指定字符集的新的 {@link CharsetDecoder}，并指定错误处理行为。
     *
     * @param charset 指定的字符集
     * @param codingErrorAction 解码器对畸形输入和不可映射字符错误的处理行为
     * @return 指定 {@code charset} 的解码器
     */
    public static CharsetDecoder decoder(Charset charset, CodingErrorAction codingErrorAction) {
        return decoder(charset, codingErrorAction, codingErrorAction);
    }

    /**
     * Returns a cached thread-local {@link CharsetDecoder} for the specified {@link Charset}.
     *
     * @param charset The specified charset
     * @return The decoder for the specified {@code charset}
     */

    /**
     * 返回指定 {@link Charset} 的缓存线程局部 {@link CharsetDecoder}。
     *
     * @param charset 指定的字符集
     * @return 指定 {@code charset} 的解码器
     */
    public static CharsetDecoder decoder(Charset charset) {
        checkNotNull(charset, "charset");

        Map<Charset, CharsetDecoder> map = InternalThreadLocalMap.get().charsetDecoderCache();
        CharsetDecoder d = map.get(charset);
        if (d != null) {
            d.reset().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            return d;
        }

        d = decoder(charset, CodingErrorAction.REPLACE, CodingErrorAction.REPLACE);
        map.put(charset, d);
        return d;
    }

    private CharsetUtil() { }
}
