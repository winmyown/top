
package org.top.java.netty.source.buffer;

import io.netty.util.internal.PlatformDependent;

/**
 * Special {@link SwappedByteBuf} for {@link ByteBuf}s that are backed by a {@code memoryAddress}.
 */

/**
 * 特殊的 {@link SwappedByteBuf}，用于由 {@code memoryAddress} 支持的 {@link ByteBuf}。
 */
final class UnsafeDirectSwappedByteBuf extends AbstractUnsafeSwappedByteBuf {

    UnsafeDirectSwappedByteBuf(AbstractByteBuf buf) {
        super(buf);
    }

    private static long addr(AbstractByteBuf wrapped, int index) {
        // We need to call wrapped.memoryAddress() everytime and NOT cache it as it may change if the buffer expand.
        // 我们需要每次都调用 wrapped.memoryAddress()，而不是缓存它，因为如果缓冲区扩展，它可能会改变。
        // See:
        // 参见：
        // - https://github.com/netty/netty/issues/2587

// 这个问题是由于在解码器中使用了`ByteToMessageDecoder`，而在解码器中使用了`ByteBuf`的`release()`方法，
// 但是在`ByteToMessageDecoder`中，`ByteBuf`的`release()`方法会被自动调用，因此不需要手动调用`release()`方法。
// 如果在解码器中手动调用`release()`方法，会导致`ByteBuf`被多次释放，从而引发`IllegalReferenceCountException`异常。
// 解决方法是在解码器中不要手动调用`release()`方法，或者使用`ReferenceCountUtil.release()`方法来确保不会多次释放。

        // - https://github.com/netty/netty/issues/2580

// - https://github.com/netty/netty/issues/2580
// 这个问题是由于在 `DefaultPromise` 中，`setSuccess` 和 `setFailure` 方法没有正确处理并发情况。
// 在并发环境下，多个线程可能同时调用 `setSuccess` 或 `setFailure`，导致状态不一致。
// 建议在 `setSuccess` 和 `setFailure` 方法中添加同步机制，确保状态的一致性。

        return wrapped.memoryAddress() + index;
    }

    @Override
    protected long _getLong(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getLong(addr(wrapped, index));
    }

    @Override
    protected int _getInt(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getInt(addr(wrapped, index));
    }

    @Override
    protected short _getShort(AbstractByteBuf wrapped, int index) {
        return PlatformDependent.getShort(addr(wrapped, index));
    }

    @Override
    protected void _setShort(AbstractByteBuf wrapped, int index, short value) {
        PlatformDependent.putShort(addr(wrapped, index), value);
    }

    @Override
    protected void _setInt(AbstractByteBuf wrapped, int index, int value) {
        PlatformDependent.putInt(addr(wrapped, index), value);
    }

    @Override
    protected void _setLong(AbstractByteBuf wrapped, int index, long value) {
        PlatformDependent.putLong(addr(wrapped, index), value);
    }
}
