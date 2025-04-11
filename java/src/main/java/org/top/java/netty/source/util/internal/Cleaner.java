
package org.top.java.netty.source.util.internal;

import java.nio.ByteBuffer;

/**
 * Allows to free direct {@link ByteBuffer}s.
 */

/**
 * 允许释放直接的 {@link ByteBuffer}。
 */
interface Cleaner {

    /**
     * Free a direct {@link ByteBuffer} if possible
     */

    /**
     * 如果可能，释放一个直接的 {@link ByteBuffer}
     */
    void freeDirectBuffer(ByteBuffer buffer);
}
