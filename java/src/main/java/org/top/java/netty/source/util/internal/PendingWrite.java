
package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.internal.ObjectPool.Handle;
import org.top.java.netty.source.util.internal.ObjectPool.ObjectCreator;
import org.top.java.netty.source.util.ReferenceCountUtil;
import org.top.java.netty.source.util.concurrent.Promise;

/**
 * Some pending write which should be picked up later.
 */

/**
 * 一些待处理的写入操作，稍后应被处理。
 */
public final class PendingWrite {
    private static final ObjectPool<PendingWrite> RECYCLER = ObjectPool.newPool(new ObjectCreator<PendingWrite>() {
        @Override
        public PendingWrite newObject(Handle<PendingWrite> handle) {
            return new PendingWrite(handle);
        }
    });

    /**
     * Create a new empty {@link RecyclableArrayList} instance
     */

    /**
     * 创建一个新的空的 {@link RecyclableArrayList} 实例
     */
    public static PendingWrite newInstance(Object msg, Promise<Void> promise) {
        PendingWrite pending = RECYCLER.get();
        pending.msg = msg;
        pending.promise = promise;
        return pending;
    }

    private final Handle<PendingWrite> handle;
    private Object msg;
    private Promise<Void> promise;

    private PendingWrite(Handle<PendingWrite> handle) {
        this.handle = handle;
    }

    /**
     * Clear and recycle this instance.
     */

    /**
     * 清除并回收此实例。
     */
    public boolean recycle() {
        msg = null;
        promise = null;
        handle.recycle(this);
        return true;
    }

    /**
     * Fails the underlying {@link Promise} with the given cause and recycle this instance.
     */

    /**
     * 使用给定的原因使底层的 {@link Promise} 失败，并回收此实例。
     */
    public boolean failAndRecycle(Throwable cause) {
        ReferenceCountUtil.release(msg);
        if (promise != null) {
            promise.setFailure(cause);
        }
        return recycle();
    }

    /**
     * Mark the underlying {@link Promise} successfully and recycle this instance.
     */

    /**
     * 标记底层的 {@link Promise} 为成功并回收此实例。
     */
    public boolean successAndRecycle() {
        if (promise != null) {
            promise.setSuccess(null);
        }
        return recycle();
    }

    public Object msg() {
        return msg;
    }

    public Promise<Void> promise() {
        return promise;
    }

    /**
     * Recycle this instance and return the {@link Promise}.
     */

    /**
     * 回收此实例并返回 {@link Promise}。
     */
    public Promise<Void> recycleAndGet() {
        Promise<Void> promise = this.promise;
        recycle();
        return promise;
    }
}
