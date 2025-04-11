

package org.top.java.netty.source.buffer;

import io.netty.util.internal.ObjectPool.Handle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract base class for derived {@link ByteBuf} implementations.
 */

/**
 * 派生 {@link ByteBuf} 实现的抽象基类。
 */
abstract class AbstractPooledDerivedByteBuf extends AbstractReferenceCountedByteBuf {

    private final Handle<AbstractPooledDerivedByteBuf> recyclerHandle;
    private AbstractByteBuf rootParent;
    /**
     * Deallocations of a pooled derived buffer should always propagate through the entire chain of derived buffers.
     * This is because each pooled derived buffer maintains its own reference count and we should respect each one.
     * If deallocations cause a release of the "root parent" then then we may prematurely release the underlying
     * content before all the derived buffers have been released.
     */
    /**
     * 池化派生缓冲区的释放应始终在整个派生缓冲区链中传播。
     * 这是因为每个池化派生缓冲区都维护自己的引用计数，我们应该尊重每个引用计数。
     * 如果释放操作导致“根父级”被释放，那么我们可能会在所有派生缓冲区被释放之前过早地释放底层内容。
     */
    private ByteBuf parent;

    @SuppressWarnings("unchecked")
    AbstractPooledDerivedByteBuf(Handle<? extends AbstractPooledDerivedByteBuf> recyclerHandle) {
        super(0);
        this.recyclerHandle = (Handle<AbstractPooledDerivedByteBuf>) recyclerHandle;
    }

    // Called from within SimpleLeakAwareByteBuf and AdvancedLeakAwareByteBuf.

    // 从 SimpleLeakAwareByteBuf 和 AdvancedLeakAwareByteBuf 内部调用。
    final void parent(ByteBuf newParent) {
        assert newParent instanceof SimpleLeakAwareByteBuf;
        parent = newParent;
    }

    @Override
    public final AbstractByteBuf unwrap() {
        return rootParent;
    }

    final <U extends AbstractPooledDerivedByteBuf> U init(
            AbstractByteBuf unwrapped, ByteBuf wrapped, int readerIndex, int writerIndex, int maxCapacity) {
        wrapped.retain(); // Retain up front to ensure the parent is accessible before doing more work.
        parent = wrapped;
        rootParent = unwrapped;

        try {
            maxCapacity(maxCapacity);
            setIndex0(readerIndex, writerIndex); // It is assumed the bounds checking is done by the caller.
            resetRefCnt();

            @SuppressWarnings("unchecked")
            final U castThis = (U) this;
            wrapped = null;
            return castThis;
        } finally {
            if (wrapped != null) {
                parent = rootParent = null;
                wrapped.release();
            }
        }
    }

    @Override
    protected final void deallocate() {
        // We need to first store a reference to the parent before recycle this instance. This is needed as
        // 我们需要在回收此实例之前先存储对父级的引用。这是必需的，因为
        // otherwise it is possible that the same AbstractPooledDerivedByteBuf is again obtained and init(...) is
        // 否则，可能会再次获取到相同的 AbstractPooledDerivedByteBuf 并调用 init(...)
        // called before we actually have a chance to call release(). This leads to call release() on the wrong parent.
        // 在实际调用 release() 之前被调用。这会导致在错误的父对象上调用 release()。
        ByteBuf parent = this.parent;
        recyclerHandle.recycle(this);
        parent.release();
    }

    @Override
    public final ByteBufAllocator alloc() {
        return unwrap().alloc();
    }

    @Override
    @Deprecated
    public final ByteOrder order() {
        return unwrap().order();
    }

    @Override
    public boolean isReadOnly() {
        return unwrap().isReadOnly();
    }

    @Override
    public final boolean isDirect() {
        return unwrap().isDirect();
    }

    @Override
    public boolean hasArray() {
        return unwrap().hasArray();
    }

    @Override
    public byte[] array() {
        return unwrap().array();
    }

    @Override
    public boolean hasMemoryAddress() {
        return unwrap().hasMemoryAddress();
    }

    @Override
    public boolean isContiguous() {
        return unwrap().isContiguous();
    }

    @Override
    public final int nioBufferCount() {
        return unwrap().nioBufferCount();
    }

    @Override
    public final ByteBuffer internalNioBuffer(int index, int length) {
        return nioBuffer(index, length);
    }

    @Override
    public final ByteBuf retainedSlice() {
        final int index = readerIndex();
        return retainedSlice(index, writerIndex() - index);
    }

    @Override
    public ByteBuf slice(int index, int length) {
        ensureAccessible();
        // All reference count methods should be inherited from this object (this is the "parent").
        // 所有引用计数方法都应从该对象继承（这是“父对象”）。
        return new PooledNonRetainedSlicedByteBuf(this, unwrap(), index, length);
    }

    final ByteBuf duplicate0() {
        ensureAccessible();
        // All reference count methods should be inherited from this object (this is the "parent").
        // 所有引用计数方法都应从该对象继承（这是“父对象”）。
        return new PooledNonRetainedDuplicateByteBuf(this, unwrap());
    }

    private static final class PooledNonRetainedDuplicateByteBuf extends UnpooledDuplicatedByteBuf {
        private final ByteBuf referenceCountDelegate;

        PooledNonRetainedDuplicateByteBuf(ByteBuf referenceCountDelegate, AbstractByteBuf buffer) {
            super(buffer);
            this.referenceCountDelegate = referenceCountDelegate;
        }

        @Override
        boolean isAccessible0() {
            return referenceCountDelegate.isAccessible();
        }

        @Override
        int refCnt0() {
            return referenceCountDelegate.refCnt();
        }

        @Override
        ByteBuf retain0() {
            referenceCountDelegate.retain();
            return this;
        }

        @Override
        ByteBuf retain0(int increment) {
            referenceCountDelegate.retain(increment);
            return this;
        }

        @Override
        ByteBuf touch0() {
            referenceCountDelegate.touch();
            return this;
        }

        @Override
        ByteBuf touch0(Object hint) {
            referenceCountDelegate.touch(hint);
            return this;
        }

        @Override
        boolean release0() {
            return referenceCountDelegate.release();
        }

        @Override
        boolean release0(int decrement) {
            return referenceCountDelegate.release(decrement);
        }

        @Override
        public ByteBuf duplicate() {
            ensureAccessible();
            return new PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, this);
        }

        @Override
        public ByteBuf retainedDuplicate() {
            return PooledDuplicatedByteBuf.newInstance(unwrap(), this, readerIndex(), writerIndex());
        }

        @Override
        public ByteBuf slice(int index, int length) {
            checkIndex(index, length);
            return new PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), index, length);
        }

        @Override
        public ByteBuf retainedSlice() {
            // Capacity is not allowed to change for a sliced ByteBuf, so length == capacity()
            // 对于切片后的 ByteBuf，容量不允许改变，因此 length == capacity()
            return retainedSlice(readerIndex(), capacity());
        }

        @Override
        public ByteBuf retainedSlice(int index, int length) {
            return PooledSlicedByteBuf.newInstance(unwrap(), this, index, length);
        }
    }

    private static final class PooledNonRetainedSlicedByteBuf extends UnpooledSlicedByteBuf {
        private final ByteBuf referenceCountDelegate;

        PooledNonRetainedSlicedByteBuf(ByteBuf referenceCountDelegate,
                                       AbstractByteBuf buffer, int index, int length) {
            super(buffer, index, length);
            this.referenceCountDelegate = referenceCountDelegate;
        }

        @Override
        boolean isAccessible0() {
            return referenceCountDelegate.isAccessible();
        }

        @Override
        int refCnt0() {
            return referenceCountDelegate.refCnt();
        }

        @Override
        ByteBuf retain0() {
            referenceCountDelegate.retain();
            return this;
        }

        @Override
        ByteBuf retain0(int increment) {
            referenceCountDelegate.retain(increment);
            return this;
        }

        @Override
        ByteBuf touch0() {
            referenceCountDelegate.touch();
            return this;
        }

        @Override
        ByteBuf touch0(Object hint) {
            referenceCountDelegate.touch(hint);
            return this;
        }

        @Override
        boolean release0() {
            return referenceCountDelegate.release();
        }

        @Override
        boolean release0(int decrement) {
            return referenceCountDelegate.release(decrement);
        }

        @Override
        public ByteBuf duplicate() {
            ensureAccessible();
            return new PooledNonRetainedDuplicateByteBuf(referenceCountDelegate, unwrap())
                    .setIndex(idx(readerIndex()), idx(writerIndex()));
        }

        @Override
        public ByteBuf retainedDuplicate() {
            return PooledDuplicatedByteBuf.newInstance(unwrap(), this, idx(readerIndex()), idx(writerIndex()));
        }

        @Override
        public ByteBuf slice(int index, int length) {
            checkIndex(index, length);
            return new PooledNonRetainedSlicedByteBuf(referenceCountDelegate, unwrap(), idx(index), length);
        }

        @Override
        public ByteBuf retainedSlice() {
            // Capacity is not allowed to change for a sliced ByteBuf, so length == capacity()
            // 对于切片后的 ByteBuf，容量不允许改变，因此 length == capacity()
            return retainedSlice(0, capacity());
        }

        @Override
        public ByteBuf retainedSlice(int index, int length) {
            return PooledSlicedByteBuf.newInstance(unwrap(), this, idx(index), length);
        }
    }
}
