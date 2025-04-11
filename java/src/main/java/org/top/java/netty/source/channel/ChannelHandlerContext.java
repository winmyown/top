
package org.top.java.netty.source.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import org.top.java.netty.source.util.concurrent.EventExecutor;

/**
 * Enables a {@link io.netty.channel.ChannelHandler} to interact with its {@link io.netty.channel.ChannelPipeline}
 * and other handlers. Among other things a handler can notify the next {@link io.netty.channel.ChannelHandler} in the
 * {@link io.netty.channel.ChannelPipeline} as well as modify the {@link io.netty.channel.ChannelPipeline} it belongs to dynamically.
 *
 * <h3>Notify</h3>
 *
 * You can notify the closest handler in the same {@link io.netty.channel.ChannelPipeline} by calling one of the various methods
 * provided here.
 *
 * Please refer to {@link io.netty.channel.ChannelPipeline} to understand how an event flows.
 *
 * <h3>Modifying a pipeline</h3>
 *
 * You can get the {@link io.netty.channel.ChannelPipeline} your handler belongs to by calling
 * {@link #pipeline()}.  A non-trivial application could insert, remove, or
 * replace handlers in the pipeline dynamically at runtime.
 *
 * <h3>Retrieving for later use</h3>
 *
 * You can keep the {@link ChannelHandlerContext} for later use, such as
 * triggering an event outside the handler methods, even from a different thread.
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 * </pre>
 *
 * <h3>Storing stateful information</h3>
 *
 * {@link #attr(AttributeKey)} allow you to
 * store and access stateful information that is related with a {@link io.netty.channel.ChannelHandler} / {@link io.netty.channel.Channel} and its
 * context. Please refer to {@link io.netty.channel.ChannelHandler} to learn various recommended
 * ways to manage stateful information.
 *
 * <h3>A handler can have more than one {@link ChannelHandlerContext}</h3>
 *
 * Please note that a {@link io.netty.channel.ChannelHandler} instance can be added to more than
 * one {@link io.netty.channel.ChannelPipeline}.  It means a single {@link io.netty.channel.ChannelHandler}
 * instance can have more than one {@link ChannelHandlerContext} and therefore
 * the single instance can be invoked with different
 * {@link ChannelHandlerContext}s if it is added to one or more {@link io.netty.channel.ChannelPipeline}s more than once.
 * Also note that a {@link io.netty.channel.ChannelHandler} that is supposed to be added to multiple {@link io.netty.channel.ChannelPipeline}s should
 * be marked as {@link io.netty.channel.ChannelHandler.Sharable}.
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link io.netty.channel.ChannelHandler}, and
 * {@link io.netty.channel.ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */

/**
 * 使 {@link io.netty.channel.ChannelHandler} 能够与其 {@link io.netty.channel.ChannelPipeline} 和其他处理器进行交互。除此之外，处理器可以通知 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelHandler}，也可以动态修改其所属的 {@link io.netty.channel.ChannelPipeline}。
 *
 * <h3>通知</h3>
 *
 * 您可以通过调用此处提供的各种方法之一来通知同一 {@link io.netty.channel.ChannelPipeline} 中最接近的处理器。
 *
 * 请参考 {@link io.netty.channel.ChannelPipeline} 以了解事件如何流动。
 *
 * <h3>修改管道</h3>
 *
 * 您可以通过调用 {@link #pipeline()} 获取您的处理器所属的 {@link io.netty.channel.ChannelPipeline}。一个非平凡的应用程序可以在运行时动态地插入、移除或替换管道中的处理器。
 *
 * <h3>检索以供后续使用</h3>
 *
 * 您可以保留 {@link ChannelHandlerContext} 以供后续使用，例如在处理器方法之外触发事件，甚至可以从不同的线程触发。
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 * </pre>
 *
 * <h3>存储状态信息</h3>
 *
 * {@link #attr(AttributeKey)} 允许您存储和访问与 {@link io.netty.channel.ChannelHandler} / {@link io.netty.channel.Channel} 及其上下文相关的状态信息。请参考 {@link io.netty.channel.ChannelHandler} 以了解管理状态信息的各种推荐方式。
 *
 * <h3>一个处理器可以有多个 {@link ChannelHandlerContext}</h3>
 *
 * 请注意，一个 {@link io.netty.channel.ChannelHandler} 实例可以被添加到多个 {@link io.netty.channel.ChannelPipeline} 中。这意味着单个 {@link io.netty.channel.ChannelHandler} 实例可以有多个 {@link ChannelHandlerContext}，因此如果该实例被多次添加到一个或多个 {@link io.netty.channel.ChannelPipeline} 中，则可以使用不同的 {@link ChannelHandlerContext} 调用该实例。还请注意，应该被添加到多个 {@link io.netty.channel.ChannelPipeline} 中的 {@link io.netty.channel.ChannelHandler} 应标记为 {@link io.netty.channel.ChannelHandler.Sharable}。
 *
 * <h3>值得阅读的额外资源</h3>
 * <p>
 * 请参考 {@link io.netty.channel.ChannelHandler} 和 {@link io.netty.channel.ChannelPipeline} 以了解更多关于入站和出站操作的信息，它们之间的根本区别，它们如何在管道中流动，以及如何在您的应用程序中处理这些操作。
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    /**
     * Return the {@link io.netty.channel.Channel} which is bound to the {@link ChannelHandlerContext}.
     */

    /**
     * 返回绑定到 {@link ChannelHandlerContext} 的 {@link io.netty.channel.Channel}。
     */
    Channel channel();

    /**
     * Returns the {@link EventExecutor} which is used to execute an arbitrary task.
     */

    /**
     * 返回用于执行任意任务的 {@link EventExecutor}。
     */
    EventExecutor executor();

    /**
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link io.netty.channel.ChannelHandler}
     * was added to the {@link io.netty.channel.ChannelPipeline}. This name can also be used to access the registered
     * {@link io.netty.channel.ChannelHandler} from the {@link io.netty.channel.ChannelPipeline}.
     */

    /**
     * {@link ChannelHandlerContext} 的唯一名称。该名称在将 {@link io.netty.channel.ChannelHandler} 添加到 {@link io.netty.channel.ChannelPipeline} 时使用。此名称也可用于从 {@link io.netty.channel.ChannelPipeline} 中访问已注册的 {@link io.netty.channel.ChannelHandler}。
     */
    String name();

    /**
     * The {@link io.netty.channel.ChannelHandler} that is bound this {@link ChannelHandlerContext}.
     */

    /**
     * 绑定到此 {@link ChannelHandlerContext} 的 {@link io.netty.channel.ChannelHandler}。
     */
    ChannelHandler handler();

    /**
     * Return {@code true} if the {@link ChannelHandler} which belongs to this context was removed
     * from the {@link io.netty.channel.ChannelPipeline}. Note that this method is only meant to be called from with in the
     * {@link EventLoop}.
     */

    /**
     * 如果属于此上下文的 {@link ChannelHandler} 已从 {@link io.netty.channel.ChannelPipeline} 中移除，则返回 {@code true}。请注意，此方法仅应在
     * {@link EventLoop} 内部调用。
     */
    boolean isRemoved();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    /**
     * Return the assigned {@link io.netty.channel.ChannelPipeline}
     */

    /**
     * 返回已分配的 {@link io.netty.channel.ChannelPipeline}
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     */

    /**
     * 返回分配的 {@link ByteBufAllocator}，它将用于分配 {@link ByteBuf}。
     */
    ByteBufAllocator alloc();

    /**
     * @deprecated Use {@link io.netty.channel.Channel#attr(AttributeKey)}
     */

    /**
     * @deprecated 使用 {@link io.netty.channel.Channel#attr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * @deprecated Use {@link Channel#hasAttr(AttributeKey)}
     */

    /**
     * @deprecated 使用 {@link Channel#hasAttr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
