

package org.top.java.netty.source.channel;

import io.netty.util.internal.InternalThreadLocalMap;

import java.util.Map;

/**
 * Skeleton implementation of a {@link io.netty.channel.ChannelHandler}.
 */

/**
 * {@link io.netty.channel.ChannelHandler} 的骨架实现。
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {

    // Not using volatile because it's used only for a sanity check.

    // 不使用 volatile，因为它仅用于健全性检查。
    boolean added;

    /**
     * Throws {@link IllegalStateException} if {@link ChannelHandlerAdapter#isSharable()} returns {@code true}
     */

    /**
     * 如果 {@link ChannelHandlerAdapter#isSharable()} 返回 {@code true}，则抛出 {@link IllegalStateException}
     */
    protected void ensureNotSharable() {
        if (isSharable()) {
            throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
        }
    }

    /**
     * Return {@code true} if the implementation is {@link Sharable} and so can be added
     * to different {@link ChannelPipeline}s.
     */

    /**
     * 如果实现是 {@link Sharable} 并且可以添加到不同的 {@link ChannelPipeline}s 中，则返回 {@code true}。
     */
    public boolean isSharable() {
        /**
         * Cache the result of {@link Sharable} annotation detection to workaround a condition. We use a
         * {@link ThreadLocal} and {@link WeakHashMap} to eliminate the volatile write/reads. Using different
         * {@link WeakHashMap} instances per {@link Thread} is good enough for us and the number of
         * {@link Thread}s are quite limited anyway.
         *
         * See <a href="https://github.com/netty/netty/issues/2289">#2289</a>.
         */
        /**
         * 缓存 {@link Sharable} 注解的检测结果以解决某个条件问题。我们使用
         * {@link ThreadLocal} 和 {@link WeakHashMap} 来消除 volatile 写/读操作。每个
         * {@link Thread} 使用不同的 {@link WeakHashMap} 实例对我们来说已经足够，而且
         * {@link Thread} 的数量也相当有限。
         *
         * 参见 <a href="https://github.com/netty/netty/issues/2289">#2289</a>。
         */
        Class<?> clazz = getClass();
        Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
        Boolean sharable = cache.get(clazz);
        if (sharable == null) {
            sharable = clazz.isAnnotationPresent(Sharable.class);
            cache.put(clazz, sharable);
        }
        return sharable;
    }

    /**
     * Do nothing by default, sub-classes may override this method.
     */

    /**
     * 默认情况下什么也不做，子类可以重写此方法。
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // NOOP
        // NOOP
    }

    /**
     * Do nothing by default, sub-classes may override this method.
     */

    /**
     * 默认情况下什么也不做，子类可以重写此方法。
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
        // NOOP
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     *
     * @deprecated is part of {@link ChannelInboundHandler}
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} 将异常转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelHandler}。
     *
     * 子类可以重写此方法以更改行为。
     *
     * @deprecated 属于 {@link ChannelInboundHandler} 的一部分
     */

    @Override
    @Deprecated
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
