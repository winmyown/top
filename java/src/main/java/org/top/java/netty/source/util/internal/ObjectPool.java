
package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.Recycler;

/**
 * Light-weight object pool.
 *
 * @param <T> the type of the pooled object
 */

/**
 * 轻量级对象池。
 *
 * @param <T> 池化对象的类型
 */
public abstract class ObjectPool<T> {

    ObjectPool() { }

    /**
     * Get a {@link Object} from the {@link ObjectPool}. The returned {@link Object} may be created via
     * {@link ObjectCreator#newObject(Handle)} if no pooled {@link Object} is ready to be reused.
     */

    /**
     * 从 {@link ObjectPool} 中获取一个 {@link Object}。返回的 {@link Object} 可能是通过
     * {@link ObjectCreator#newObject(Handle)} 创建的，如果没有可重用的池化 {@link Object}。
     */
    public abstract T get();

    /**
     * Handle for an pooled {@link Object} that will be used to notify the {@link ObjectPool} once it can
     * reuse the pooled {@link Object} again.
     * @param <T>
     */

    /**
     * 用于池化 {@link Object} 的句柄，当池化 {@link Object} 可以再次被 {@link ObjectPool} 重用时，将通知 {@link ObjectPool}。
     * @param <T>
     */
    public interface Handle<T> {
        /**
         * Recycle the {@link Object} if possible and so make it ready to be reused.
         */
        /**
         * 如果可能，回收 {@link Object} 以便可以重新使用。
         */
        void recycle(T self);
    }

    /**
     * Creates a new Object which references the given {@link Handle} and calls {@link Handle#recycle(Object)} once
     * it can be re-used.
     *
     * @param <T> the type of the pooled object
     */

    /**
     * 创建一个新的对象，该对象引用给定的 {@link Handle}，并在可以重用时调用 {@link Handle#recycle(Object)}。
     *
     * @param <T> 池化对象的类型
     */
    public interface ObjectCreator<T> {

        /**
         * Creates an returns a new {@link Object} that can be used and later recycled via
         * {@link Handle#recycle(Object)}.
         */

        /**
         * 创建并返回一个新的 {@link Object}，该对象可以使用并在稍后通过
         * {@link Handle#recycle(Object)} 进行回收。
         */
        T newObject(Handle<T> handle);
    }

    /**
     * Creates a new {@link ObjectPool} which will use the given {@link ObjectCreator} to create the {@link Object}
     * that should be pooled.
     */

    /**
     * 创建一个新的 {@link ObjectPool}，它将使用给定的 {@link ObjectCreator} 来创建应被池化的 {@link Object}。
     */
    public static <T> ObjectPool<T> newPool(final ObjectCreator<T> creator) {
        return new RecyclerObjectPool<T>(ObjectUtil.checkNotNull(creator, "creator"));
    }

    private static final class RecyclerObjectPool<T> extends ObjectPool<T> {
        private final Recycler<T> recycler;

        RecyclerObjectPool(final ObjectCreator<T> creator) {
             recycler = new Recycler<T>() {
                @Override
                protected T newObject(Handle<T> handle) {
                    return creator.newObject(handle);
                }
            };
        }

        @Override
        public T get() {
            return recycler.get();
        }
    }
}
