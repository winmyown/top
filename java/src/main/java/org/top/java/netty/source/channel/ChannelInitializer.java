
package org.top.java.netty.source.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A special {@link ChannelInboundHandler} which offers an easy way to initialize a {@link io.netty.channel.Channel} once it was
 * registered to its {@link EventLoop}.
 *
 * Implementations are most often used in the context of {@link Bootstrap#handler(io.netty.channel.ChannelHandler)} ,
 * {@link ServerBootstrap#handler(io.netty.channel.ChannelHandler)} and {@link ServerBootstrap#childHandler(ChannelHandler)} to
 * setup the {@link io.netty.channel.ChannelPipeline} of a {@link io.netty.channel.Channel}.
 *
 * <pre>
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer} {
 *     public void initChannel({@link io.netty.channel.Channel} channel) {
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 * Be aware that this class is marked as {@link ChannelHandler.Sharable} and so the implementation must be safe to be re-used.
 *
 * @param <C>   A sub-type of {@link io.netty.channel.Channel}
 */

/**
 * 一个特殊的 {@link ChannelInboundHandler}，它提供了一种简单的方法来初始化 {@link io.netty.channel.Channel}，一旦它被注册到其 {@link EventLoop}。
 *
 * 实现通常用于 {@link Bootstrap#handler(io.netty.channel.ChannelHandler)}、
 * {@link ServerBootstrap#handler(io.netty.channel.ChannelHandler)} 和 {@link ServerBootstrap#childHandler(ChannelHandler)} 的上下文中，
 * 以设置 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline}。
 *
 * <pre>
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer} {
 *     public void initChannel({@link io.netty.channel.Channel} channel) {
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 * 请注意，此类被标记为 {@link ChannelHandler.Sharable}，因此实现必须确保可以安全地重复使用。
 *
 * @param <C>   {@link io.netty.channel.Channel} 的子类型
 */
@Sharable
public abstract class ChannelInitializer<C extends io.netty.channel.Channel> extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);
    // We use a Set as a ChannelInitializer is usually shared between all Channels in a Bootstrap /
    // 我们使用 Set 作为 ChannelInitializer，通常在 Bootstrap 中的所有 Channel 之间共享。
    // ServerBootstrap. This way we can reduce the memory usage compared to use Attributes.
    // ServerBootstrap。这样我们可以减少内存使用，相比于使用Attributes。
    private final Set<io.netty.channel.ChannelHandlerContext> initMap = Collections.newSetFromMap(
            new ConcurrentHashMap<io.netty.channel.ChannelHandlerContext, Boolean>());

    /**
     * This method will be called once the {@link io.netty.channel.Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link io.netty.channel.Channel}.
     *
     * @param ch            the {@link io.netty.channel.Channel} which was registered.
     * @throws Exception    is thrown if an error occurs. In that case it will be handled by
     *                      {@link #exceptionCaught(io.netty.channel.ChannelHandlerContext, Throwable)} which will by default close
     *                      the {@link io.netty.channel.Channel}.
     */

    /**
     * 该方法将在 {@link io.netty.channel.Channel} 注册后被调用。方法返回后，该实例将从 {@link io.netty.channel.Channel} 的 {@link ChannelPipeline} 中移除。
     *
     * @param ch            已注册的 {@link io.netty.channel.Channel}。
     * @throws Exception    如果发生错误，将抛出异常。在这种情况下，异常将由 {@link #exceptionCaught(io.netty.channel.ChannelHandlerContext, Throwable)} 处理，默认情况下将关闭 {@link io.netty.channel.Channel}。
     */
    protected abstract void initChannel(C ch) throws Exception;

    @Override
    @SuppressWarnings("unchecked")
    public final void channelRegistered(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        // Normally this method will never be called as handlerAdded(...) should call initChannel(...) and remove
        // 通常这个方法永远不会被调用，因为 handlerAdded(...) 应该调用 initChannel(...) 并移除
        // the handler.
        // 处理器
        if (initChannel(ctx)) {
            // we called initChannel(...) so we need to call now pipeline.fireChannelRegistered() to ensure we not
            // 我们调用了 initChannel(...)，所以现在需要调用 pipeline.fireChannelRegistered() 以确保我们不
            // miss an event.
            // 错过一个事件。
            ctx.pipeline().fireChannelRegistered();

            // We are done with init the Channel, removing all the state for the Channel now.

            // 我们已经完成了 Channel 的初始化，现在正在移除 Channel 的所有状态。
            removeState(ctx);
        } else {
            // Called initChannel(...) before which is the expected behavior, so just forward the event.
            // 之前已经调用了 initChannel(...)，这是预期的行为，所以只需转发事件。
            ctx.fireChannelRegistered();
        }
    }

    /**
     * Handle the {@link Throwable} by logging and closing the {@link Channel}. Sub-classes may override this.
     */

    /**
     * 通过记录日志并关闭 {@link Channel} 来处理 {@link Throwable}。子类可以重写此方法。
     */
    @Override
    public void exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        }
        ctx.close();
    }

    /**
     * {@inheritDoc} If override this method ensure you call super!
     */

    /**
     * {@inheritDoc} 如果重写此方法，请确保调用super！
     */
    @Override
    public void handlerAdded(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isRegistered()) {
            // This should always be true with our current DefaultChannelPipeline implementation.
            // 根据我们当前的 DefaultChannelPipeline 实现，这应该始终为真。
            // The good thing about calling initChannel(...) in handlerAdded(...) is that there will be no ordering
            // 在 handlerAdded(...) 中调用 initChannel(...) 的好处是不会有顺序问题
            // surprises if a ChannelInitializer will add another ChannelInitializer. This is as all handlers
            // 如果 ChannelInitializer 添加另一个 ChannelInitializer，会让人感到意外。这是因为所有的处理器
            // will be added in the expected order.
            // 将按预期顺序添加。
            if (initChannel(ctx)) {

                // We are done with init the Channel, removing the initializer now.

                // 我们已经完成了 Channel 的初始化，现在移除初始化器。
                removeState(ctx);
            }
        }
    }

    @Override
    public void handlerRemoved(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        initMap.remove(ctx);
    }

    @SuppressWarnings("unchecked")
    private boolean initChannel(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        if (initMap.add(ctx)) { // Guard against re-entrance.
            try {
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                // Explicitly call exceptionCaught(...) as we removed the handler before calling initChannel(...).
                // 显式调用 exceptionCaught(...)，因为我们在调用 initChannel(...) 之前移除了处理程序。
                // We do so to prevent multiple calls to initChannel(...).
                // 我们这样做是为了防止多次调用 initChannel(...)。
                exceptionCaught(ctx, cause);
            } finally {
                if (!ctx.isRemoved()) {
                    ctx.pipeline().remove(this);
                }
            }
            return true;
        }
        return false;
    }

    private void removeState(final ChannelHandlerContext ctx) {
        // The removal may happen in an async fashion if the EventExecutor we use does something funky.
        // 如果我们使用的EventExecutor执行了某些特殊操作，移除操作可能会以异步方式发生。
        if (ctx.isRemoved()) {
            initMap.remove(ctx);
        } else {
            // The context is not removed yet which is most likely the case because a custom EventExecutor is used.
            // 上下文尚未移除，这很可能是因为使用了自定义的 EventExecutor。
            // Let's schedule it on the EventExecutor to give it some more time to be completed in case it is offloaded.
            // 让我们将其安排在 EventExecutor 上，以便在它被卸载时给予更多时间来完成。
            ctx.executor().execute(new Runnable() {
                @Override
                public void run() {
                    initMap.remove(ctx);
                }
            });
        }
    }
}
