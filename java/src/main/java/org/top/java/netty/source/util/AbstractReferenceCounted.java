
package org.top.java.netty.source.util;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.top.java.netty.source.util.internal.ReferenceCountUpdater;

/**
 * Abstract base class for classes wants to implement {@link ReferenceCounted}.
 */

/**
 * 想要实现 {@link ReferenceCounted} 的类的抽象基类。
 */
public abstract class AbstractReferenceCounted implements ReferenceCounted {
    private static final long REFCNT_FIELD_OFFSET =
            ReferenceCountUpdater.getUnsafeOffset(AbstractReferenceCounted.class, "refCnt");
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCounted> AIF_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCounted.class, "refCnt");

    private static final ReferenceCountUpdater<AbstractReferenceCounted> updater =
            new ReferenceCountUpdater<AbstractReferenceCounted>() {
        @Override
        protected AtomicIntegerFieldUpdater<AbstractReferenceCounted> updater() {
            return AIF_UPDATER;
        }
        @Override
        protected long unsafeOffset() {
            return REFCNT_FIELD_OFFSET;
        }
    };

    // Value might not equal "real" reference count, all access should be via the updater

    // 值可能不等于“真实”引用计数，所有访问应通过更新器进行
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int refCnt = updater.initialValue();

    @Override
    public int refCnt() {
        return updater.refCnt(this);
    }

    /**
     * An unsafe operation intended for use by a subclass that sets the reference count of the buffer directly
     */

    /**
     * 一个不安全的操作，供子类直接设置缓冲区的引用计数
     */
    protected final void setRefCnt(int refCnt) {
        updater.setRefCnt(this, refCnt);
    }

    @Override
    public ReferenceCounted retain() {
        return updater.retain(this);
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return updater.retain(this, increment);
    }

    @Override
    public ReferenceCounted touch() {
        return touch(null);
    }

    @Override
    public boolean release() {
        return handleRelease(updater.release(this));
    }

    @Override
    public boolean release(int decrement) {
        return handleRelease(updater.release(this, decrement));
    }

    private boolean handleRelease(boolean result) {
        if (result) {
            deallocate();
        }
        return result;
    }

    /**
     * Called once {@link #refCnt()} is equals 0.
     */

    /**
     * 当 {@link #refCnt()} 等于 0 时调用。
     */
    protected abstract void deallocate();
}
