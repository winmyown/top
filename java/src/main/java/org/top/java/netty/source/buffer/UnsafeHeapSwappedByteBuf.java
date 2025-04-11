
package org.top.java.netty.source.buffer;

import io.netty.util.internal.PlatformDependent;

/**
 * Special {@link SwappedByteBuf} for {@link ByteBuf}s that use unsafe to access the byte array.
 */

/**
 * 针对使用unsafe访问字节数组的{@link ByteBuf}的特殊{@link SwappedByteBuf}。
 */
final class UnsafeHeapSwappedByteBuf extends AbstractUnsafeSwappedByteBuf {

    UnsafeHeapSwappedByteBuf(AbstractByteBuf buf) {
        super(buf);
    }

    private static int idx(ByteBuf wrapped, int index) {
        return wrapped.arrayOffset() + index;
    }

    @Override
    protected long _getLong(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getLong(wrapped.array(), idx(wrapped, index));
    }

    @Override
    protected int _getInt(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getInt(wrapped.array(), idx(wrapped, index));
    }

    @Override
    protected short _getShort(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getShort(wrapped.array(), idx(wrapped, index));
    }

    @Override
    protected void _setShort(AbstractByteBuf wrapped, int index, short value) {
        PlatformDependent.putShort(wrapped.array(), idx(wrapped, index), value);
    }

    @Override
    protected void _setInt(AbstractByteBuf wrapped, int index, int value) {
        PlatformDependent.putInt(wrapped.array(), idx(wrapped, index), value);
    }

    @Override
    protected void _setLong(AbstractByteBuf wrapped, int index, long value) {
        PlatformDependent.putLong(wrapped.array(), idx(wrapped, index), value);
    }
}
