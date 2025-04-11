
package org.top.java.netty.source.channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * {@link io.netty.channel.ChannelInboundHandlerAdapter} which allows to explicit only handle a specific type of messages.
 *
 * For example here is an implementation which only handle {@link String} messages.
 *
 * <pre>
 *     public class StringHandler extends
 *             {@link SimpleChannelInboundHandler}&lt;{@link String}&gt; {
 *
 *         {@code @Override}
 *         protected void channelRead0({@link ChannelHandlerContext} ctx, {@link String} message)
 *                 throws {@link Exception} {
 *             System.out.println(message);
 *         }
 *     }
 * </pre>
 *
 * Be aware that depending of the constructor parameters it will release all handled messages by passing them to
 * {@link ReferenceCountUtil#release(Object)}. In this case you may need to use
 * {@link ReferenceCountUtil#retain(Object)} if you pass the object to the next handler in the {@link ChannelPipeline}.
 */

/**
 * {@link io.netty.channel.ChannelInboundHandlerAdapter} 允许显式处理特定类型的消息。
 *
 * 例如，以下是一个仅处理 {@link String} 消息的实现。
 *
 * <pre>
 *     public class StringHandler extends
 *             {@link SimpleChannelInboundHandler}&lt;{@link String}&gt; {
 *
 *         {@code @Override}
 *         protected void channelRead0({@link ChannelHandlerContext} ctx, {@link String} message)
 *                 throws {@link Exception} {
 *             System.out.println(message);
 *         }
 *     }
 * </pre>
 *
 * 请注意，根据构造函数参数，它将通过将处理的消息传递给 {@link ReferenceCountUtil#release(Object)} 来释放所有处理的消息。在这种情况下，如果您将对象传递给 {@link ChannelPipeline} 中的下一个处理程序，则可能需要使用 {@link ReferenceCountUtil#retain(Object)}。
 */
public abstract class SimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {

    private final TypeParameterMatcher matcher;
    private final boolean autoRelease;

    /**
     * see {@link #SimpleChannelInboundHandler(boolean)} with {@code true} as boolean parameter.
     */

    /**
     * 参见 {@link #SimpleChannelInboundHandler(boolean)}，其中 {@code true} 作为布尔参数。
     */
    protected SimpleChannelInboundHandler() {
        this(true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param autoRelease   {@code true} if handled messages should be released automatically by passing them to
     *                      {@link ReferenceCountUtil#release(Object)}.
     */

    /**
     * 创建一个新实例，该实例将尝试从类的类型参数中检测要匹配的类型。
     *
     * @param autoRelease   {@code true} 如果处理的消息应通过将其传递给 {@link ReferenceCountUtil#release(Object)} 自动释放。
     */
    protected SimpleChannelInboundHandler(boolean autoRelease) {
        matcher = TypeParameterMatcher.find(this, SimpleChannelInboundHandler.class, "I");
        this.autoRelease = autoRelease;
    }

    /**
     * see {@link #SimpleChannelInboundHandler(Class, boolean)} with {@code true} as boolean value.
     */

    /**
     * 参见 {@link #SimpleChannelInboundHandler(Class, boolean)}，其中布尔值为 {@code true}。
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType) {
        this(inboundMessageType, true);
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType    The type of messages to match
     * @param autoRelease           {@code true} if handled messages should be released automatically by passing them to
     *                              {@link ReferenceCountUtil#release(Object)}.
     */

    /**
     * 创建一个新实例
     *
     * @param inboundMessageType    要匹配的消息类型
     * @param autoRelease           {@code true} 如果处理后的消息应通过传递给 {@link ReferenceCountUtil#release(Object)} 自动释放。
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
        this.autoRelease = autoRelease;
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */

    /**
     * 如果给定的消息应该被处理，则返回 {@code true}。如果返回 {@code false}，它将被传递给 {@link ChannelPipeline} 中的下一个
     * {@link ChannelInboundHandler}。
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean release = true;
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I imsg = (I) msg;
                channelRead0(ctx, imsg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (autoRelease && release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    /**
     * Is called for each message of type {@link I}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */

    /**
     * 为每个类型为 {@link I} 的消息调用。
     *
     * @param ctx           此 {@link SimpleChannelInboundHandler} 所属的 {@link ChannelHandlerContext}
     * @param msg           要处理的消息
     * @throws Exception    如果发生错误，则抛出异常
     */
    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
