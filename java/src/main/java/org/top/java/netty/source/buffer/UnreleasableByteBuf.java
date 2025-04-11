
package org.top.java.netty.source.buffer;

import io.netty.util.internal.ObjectUtil;

import java.nio.ByteOrder;

/**
 * A {@link ByteBuf} implementation that wraps another buffer to prevent a user from increasing or decreasing the
 * wrapped buffer's reference count.
 */

/**
 * 一个 {@link ByteBuf} 实现，它包装另一个缓冲区，以防止用户增加或减少被包装缓冲区的引用计数。
 */
final class UnreleasableByteBuf extends WrappedByteBuf {

    private SwappedByteBuf swappedBuf;

    UnreleasableByteBuf(ByteBuf buf) {
        super(buf instanceof UnreleasableByteBuf ? buf.unwrap() : buf);
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (ObjectUtil.checkNotNull(endianness, "endianness") == order()) {
            return this;
        }

        SwappedByteBuf swappedBuf = this.swappedBuf;
        if (swappedBuf == null) {
            this.swappedBuf = swappedBuf = new SwappedByteBuf(this);
        }
        return swappedBuf;
    }

    @Override
    public ByteBuf asReadOnly() {
        return buf.isReadOnly() ? this : new UnreleasableByteBuf(buf.asReadOnly());
    }

    @Override
    public ByteBuf readSlice(int length) {
        return new UnreleasableByteBuf(buf.readSlice(length));
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        // We could call buf.readSlice(..), and then call buf.release(). However this creates a leak in unit tests
        // 我们可以调用 buf.readSlice(..)，然后调用 buf.release()。然而，这会在单元测试中造成泄漏。
        // because the release method on UnreleasableByteBuf will never allow the leak record to be cleaned up.
        // 因为 UnreleasableByteBuf 上的 release 方法永远不会允许泄漏记录被清理。
        // So we just use readSlice(..) because the end result should be logically equivalent.
        // 所以我们只需使用 readSlice(..)，因为最终结果在逻辑上应该是等价的。
        return readSlice(length);
    }

    @Override
    public ByteBuf slice() {
        return new UnreleasableByteBuf(buf.slice());
    }

    @Override
    public ByteBuf retainedSlice() {
        // We could call buf.retainedSlice(), and then call buf.release(). However this creates a leak in unit tests
        // 我们可以调用 buf.retainedSlice()，然后调用 buf.release()。然而，这会在单元测试中造成内存泄漏。
        // because the release method on UnreleasableByteBuf will never allow the leak record to be cleaned up.
        // 因为 UnreleasableByteBuf 上的 release 方法永远不会允许泄漏记录被清理。
        // So we just use slice() because the end result should be logically equivalent.
        // 所以我们只是使用slice()，因为最终结果应该是逻辑等价的。
        return slice();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return new UnreleasableByteBuf(buf.slice(index, length));
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        // We could call buf.retainedSlice(..), and then call buf.release(). However this creates a leak in unit tests
        // 我们可以调用 buf.retainedSlice(..)，然后调用 buf.release()。然而，这会在单元测试中造成内存泄漏。
        // because the release method on UnreleasableByteBuf will never allow the leak record to be cleaned up.
        // 因为 UnreleasableByteBuf 上的 release 方法永远不会允许泄漏记录被清理。
        // So we just use slice(..) because the end result should be logically equivalent.
        // 所以我们只使用 slice(..) 因为最终结果应该是逻辑等价的。
        return slice(index, length);
    }

    @Override
    public ByteBuf duplicate() {
        return new UnreleasableByteBuf(buf.duplicate());
    }

    @Override
    public ByteBuf retainedDuplicate() {
        // We could call buf.retainedDuplicate(), and then call buf.release(). However this creates a leak in unit tests
        // 我们可以调用 buf.retainedDuplicate()，然后调用 buf.release()。然而，这会在单元测试中造成内存泄漏
        // because the release method on UnreleasableByteBuf will never allow the leak record to be cleaned up.
        // 因为 UnreleasableByteBuf 上的 release 方法永远不会允许泄漏记录被清理。
        // So we just use duplicate() because the end result should be logically equivalent.
        // 所以我们只需使用 duplicate()，因为最终结果应该是逻辑等价的。
        return duplicate();
    }

    @Override
    public ByteBuf retain(int increment) {
        return this;
    }

    @Override
    public ByteBuf retain() {
        return this;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
