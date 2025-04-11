
package org.top.java.netty.source.util;

import static org.top.java.netty.source.util.ByteProcessorUtils.CARRIAGE_RETURN;
import static org.top.java.netty.source.util.ByteProcessorUtils.HTAB;
import static org.top.java.netty.source.util.ByteProcessorUtils.LINE_FEED;
import static org.top.java.netty.source.util.ByteProcessorUtils.SPACE;

/**
 * Provides a mechanism to iterate over a collection of bytes.
 */

/**
 * 提供了一种遍历字节集合的机制。
 */
public interface ByteProcessor {
    /**
     * A {@link ByteProcessor} which finds the first appearance of a specific byte.
     */
    /**
     * 一个 {@link ByteProcessor}，用于查找特定字节的首次出现。
     */
    class IndexOfProcessor implements ByteProcessor {
        private final byte byteToFind;

        public IndexOfProcessor(byte byteToFind) {
            this.byteToFind = byteToFind;
        }

        @Override
        public boolean process(byte value) {
            return value != byteToFind;
        }
    }

    /**
     * A {@link ByteProcessor} which finds the first appearance which is not of a specific byte.
     */

    /**
     * 一个 {@link ByteProcessor}，用于查找第一个不匹配特定字节的位置。
     */
    class IndexNotOfProcessor implements ByteProcessor {
        private final byte byteToNotFind;

        public IndexNotOfProcessor(byte byteToNotFind) {
            this.byteToNotFind = byteToNotFind;
        }

        @Override
        public boolean process(byte value) {
            return value == byteToNotFind;
        }
    }

    /**
     * Aborts on a {@code NUL (0x00)}.
     */

    /**
     * 在遇到 {@code NUL (0x00)} 时中止。
     */
    ByteProcessor FIND_NUL = new IndexOfProcessor((byte) 0);

    /**
     * Aborts on a non-{@code NUL (0x00)}.
     */

    /**
     * 在非{@code NUL (0x00)}时中止。
     */
    ByteProcessor FIND_NON_NUL = new IndexNotOfProcessor((byte) 0);

    /**
     * Aborts on a {@code CR ('r')}.
     */

    /**
     * 在遇到 {@code CR ('\r')} 时中止。
     */
    ByteProcessor FIND_CR = new IndexOfProcessor(CARRIAGE_RETURN);

    /**
     * Aborts on a non-{@code CR ('r')}.
     */

    /**
     * 在非{@code CR ('\r')}时中止。
     */
    ByteProcessor FIND_NON_CR = new IndexNotOfProcessor(CARRIAGE_RETURN);

    /**
     * Aborts on a {@code LF ('n')}.
     */

    /**
     * 在遇到 {@code LF ('\n')} 时中止。
     */
    ByteProcessor FIND_LF = new IndexOfProcessor(LINE_FEED);

    /**
     * Aborts on a non-{@code LF ('n')}.
     */

    /**
     * 在非{@code LF ('\n')}时中止。
     */
    ByteProcessor FIND_NON_LF = new IndexNotOfProcessor(LINE_FEED);

    /**
     * Aborts on a semicolon {@code (';')}.
     */

    /**
     * 在分号 {@code (';')} 处中止。
     */
    ByteProcessor FIND_SEMI_COLON = new IndexOfProcessor((byte) ';');

    /**
     * Aborts on a comma {@code (',')}.
     */

    /**
     * 在逗号 {@code (',')} 处中止。
     */
    ByteProcessor FIND_COMMA = new IndexOfProcessor((byte) ',');

    /**
     * Aborts on a ascii space character ({@code ' '}).
     */

    /**
     * 在 ASCII 空格字符 ({@code ' '}) 上中止。
     */
    ByteProcessor FIND_ASCII_SPACE = new IndexOfProcessor(SPACE);

    /**
     * Aborts on a {@code CR ('r')} or a {@code LF ('n')}.
     */

    /**
     * 在遇到 {@code CR ('\r')} 或 {@code LF ('\n')} 时中止。
     */
    ByteProcessor FIND_CRLF = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value != CARRIAGE_RETURN && value != LINE_FEED;
        }
    };

    /**
     * Aborts on a byte which is neither a {@code CR ('r')} nor a {@code LF ('n')}.
     */

    /**
     * 在遇到既不是 {@code CR ('\r')} 也不是 {@code LF ('\n')} 的字节时中止。
     */
    ByteProcessor FIND_NON_CRLF = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value == CARRIAGE_RETURN || value == LINE_FEED;
        }
    };

    /**
     * Aborts on a linear whitespace (a ({@code ' '} or a {@code 't'}).
     */

    /**
     * 在线性空白处（一个 {@code ' '} 或一个 {@code '\t'}）中止。
     */
    ByteProcessor FIND_LINEAR_WHITESPACE = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value != SPACE && value != HTAB;
        }
    };

    /**
     * Aborts on a byte which is not a linear whitespace (neither {@code ' '} nor {@code 't'}).
     */

    /**
     * 在遇到不是线性空白字符（既不是 {@code ' '} 也不是 {@code '\t'}）的字节时中止。
     */
    ByteProcessor FIND_NON_LINEAR_WHITESPACE = new ByteProcessor() {
        @Override
        public boolean process(byte value) {
            return value == SPACE || value == HTAB;
        }
    };

    /**
     * @return {@code true} if the processor wants to continue the loop and handle the next byte in the buffer.
     *         {@code false} if the processor wants to stop handling bytes and abort the loop.
     */

    /**
     * @return {@code true} 如果处理器希望继续循环并处理缓冲区中的下一个字节。
     *         {@code false} 如果处理器希望停止处理字节并中止循环。
     */
    boolean process(byte value) throws Exception;
}
