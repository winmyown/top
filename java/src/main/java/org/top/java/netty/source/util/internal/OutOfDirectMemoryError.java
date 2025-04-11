
package org.top.java.netty.source.util.internal;

import java.nio.ByteBuffer;

/**
 * {@link OutOfMemoryError} that is throws if {@link PlatformDependent#allocateDirectNoCleaner(int)} can not allocate
 * a new {@link ByteBuffer} due memory restrictions.
 */

/**
 * {@link OutOfMemoryError} 当 {@link PlatformDependent#allocateDirectNoCleaner(int)} 由于内存限制无法分配
 * 新的 {@link ByteBuffer} 时抛出。
 */
public final class OutOfDirectMemoryError extends OutOfMemoryError {
    private static final long serialVersionUID = 4228264016184011555L;

    OutOfDirectMemoryError(String s) {
        super(s);
    }
}
