

package org.top.java.netty.source.buffer;

import io.netty.util.ByteProcessor;

/**
 * @deprecated Use {@link ByteProcessor}.
 */

/**
 * @deprecated 使用 {@link ByteProcessor}。
 */
@Deprecated
public interface ByteBufProcessor extends ByteProcessor {

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NUL}.
     */

    /**
     * @deprecated 请使用 {@link ByteProcessor#FIND_NUL}。
     */
    @Deprecated
    ByteBufProcessor FIND_NUL = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != 0;
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NON_NUL}.
     */

    /**
     * @deprecated 请使用 {@link ByteProcessor#FIND_NON_NUL}。
     */
    @Deprecated
    ByteBufProcessor FIND_NON_NUL = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value == 0;
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_CR}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_CR}。
     */
    @Deprecated
    ByteBufProcessor FIND_CR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != '\r';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NON_CR}.
     */

    /**
     * @deprecated 请使用 {@link ByteProcessor#FIND_NON_CR}。
     */
    @Deprecated
    ByteBufProcessor FIND_NON_CR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value == '\r';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_LF}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_LF}。
     */
    @Deprecated
    ByteBufProcessor FIND_LF = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != '\n';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NON_LF}.
     */

    /**
     * @deprecated 请使用 {@link ByteProcessor#FIND_NON_LF}。
     */
    @Deprecated
    ByteBufProcessor FIND_NON_LF = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value == '\n';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_CRLF}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_CRLF}。
     */
    @Deprecated
    ByteBufProcessor FIND_CRLF = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != '\r' && value != '\n';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NON_CRLF}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_NON_CRLF}。
     */
    @Deprecated
    ByteBufProcessor FIND_NON_CRLF = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value == '\r' || value == '\n';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_LINEAR_WHITESPACE}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_LINEAR_WHITESPACE}。
     */
    @Deprecated
    ByteBufProcessor FIND_LINEAR_WHITESPACE = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != ' ' && value != '\t';
        }
    };

    /**
     * @deprecated Use {@link ByteProcessor#FIND_NON_LINEAR_WHITESPACE}.
     */

    /**
     * @deprecated 使用 {@link ByteProcessor#FIND_NON_LINEAR_WHITESPACE}。
     */
    @Deprecated
    ByteBufProcessor FIND_NON_LINEAR_WHITESPACE = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value == ' ' || value == '\t';
        }
    };
}
