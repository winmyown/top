
package org.top.java.netty.source.channel;

import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.lang.annotation.*;

/**
 * Handles an I/O event or intercepts an I/O operation, and forwards it to its next handler in
 * its {@link ChannelPipeline}.
 *
 * <h3>Sub-types</h3>
 * <p>
 * {@link ChannelHandler} itself does not provide many methods, but you usually have to implement one of its subtypes:
 * <ul>
 * <li>{@link ChannelInboundHandler} to handle inbound I/O events, and</li>
 * <li>{@link ChannelOutboundHandler} to handle outbound I/O operations.</li>
 * </ul>
 * </p>
 * <p>
 * Alternatively, the following adapter classes are provided for your convenience:
 * <ul>
 * <li>{@link ChannelInboundHandlerAdapter} to handle inbound I/O events,</li>
 * <li>{@link ChannelOutboundHandlerAdapter} to handle outbound I/O operations, and</li>
 * <li>{@link ChannelDuplexHandler} to handle both inbound and outbound events</li>
 * </ul>
 * </p>
 * <p>
 * For more information, please refer to the documentation of each subtype.
 * </p>
 *
 * <h3>The context object</h3>
 * <p>
 * A {@link ChannelHandler} is provided with a {@link ChannelHandlerContext}
 * object.  A {@link ChannelHandler} is supposed to interact with the
 * {@link ChannelPipeline} it belongs to via a context object.  Using the
 * context object, the {@link ChannelHandler} can pass events upstream or
 * downstream, modify the pipeline dynamically, or store the information
 * (using {@link AttributeKey}s) which is specific to the handler.
 *
 * <h3>State management</h3>
 *
 * A {@link ChannelHandler} often needs to store some stateful information.
 * The simplest and recommended approach is to use member variables:
 * <pre>
 * public interface Message {
 *     // your methods here
 * }
 *
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *
 *     <b>private boolean loggedIn;</b>
 *
 *     {@code @Override}
 *     public void channelRead0({@link ChannelHandlerContext} ctx, Message message) {
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) message);
 *             <b>loggedIn = true;</b>
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>loggedIn</b>) {
 *                 ctx.writeAndFlush(fetchSecret((GetDataMessage) message));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Because the handler instance has a state variable which is dedicated to
 * one connection, you have to create a new handler instance for each new
 * channel to avoid a race condition where a unauthenticated client can get
 * the confidential information:
 * <pre>
 * // Create a new handler instance per channel.
 * // See {@link ChannelInitializer#initChannel(Channel)}.
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>new DataServerHandler()</b>);
 *     }
 * }
 *
 * </pre>
 *
 * <h4>Using {@link AttributeKey}s</h4>
 *
 * Although it's recommended to use member variables to store the state of a
 * handler, for some reason you might not want to create many handler instances.
 * In such a case, you can use {@link AttributeKey}s which is provided by
 * {@link ChannelHandlerContext}:
 * <pre>
 * public interface Message {
 *     // your methods here
 * }
 *
 * {@code @Sharable}
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *     private final {@link AttributeKey}&lt;{@link Boolean}&gt; auth =
 *           {@link AttributeKey#valueOf(String) AttributeKey.valueOf("auth")};
 *
 *     {@code @Override}
 *     public void channelRead({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Attribute}&lt;{@link Boolean}&gt; attr = ctx.attr(auth);
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) o);
 *             <b>attr.set(true)</b>;
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>Boolean.TRUE.equals(attr.get())</b>) {
 *                 ctx.writeAndFlush(fetchSecret((GetDataMessage) o));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Now that the state of the handler is attached to the {@link ChannelHandlerContext}, you can add the
 * same handler instance to different pipelines:
 * <pre>
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *
 *     private static final DataServerHandler <b>SHARED</b> = new DataServerHandler();
 *
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>SHARED</b>);
 *     }
 * }
 * </pre>
 *
 *
 * <h4>The {@code @Sharable} annotation</h4>
 * <p>
 * In the example above which used an {@link AttributeKey},
 * you might have noticed the {@code @Sharable} annotation.
 * <p>
 * If a {@link ChannelHandler} is annotated with the {@code @Sharable}
 * annotation, it means you can create an instance of the handler just once and
 * add it to one or more {@link ChannelPipeline}s multiple times without
 * a race condition.
 * <p>
 * If this annotation is not specified, you have to create a new handler
 * instance every time you add it to a pipeline because it has unshared state
 * such as member variables.
 * <p>
 * This annotation is provided for documentation purpose, just like
 * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */

/**
 * 处理I/O事件或拦截I/O操作，并将其转发到{@link ChannelPipeline}中的下一个处理器。
 *
 * <h3>子类型</h3>
 * <p>
 * {@link ChannelHandler}本身不提供许多方法，但通常需要实现其子类型之一：
 * <ul>
 * <li>{@link ChannelInboundHandler} 用于处理入站I/O事件，</li>
 * <li>{@link ChannelOutboundHandler} 用于处理出站I/O操作。</li>
 * </ul>
 * </p>
 * <p>
 * 或者，提供了以下适配器类以方便使用：
 * <ul>
 * <li>{@link ChannelInboundHandlerAdapter} 用于处理入站I/O事件，</li>
 * <li>{@link ChannelOutboundHandlerAdapter} 用于处理出站I/O操作，</li>
 * <li>{@link ChannelDuplexHandler} 用于处理入站和出站事件。</li>
 * </ul>
 * </p>
 * <p>
 * 有关更多信息，请参阅每个子类型的文档。
 * </p>
 *
 * <h3>上下文对象</h3>
 * <p>
 * {@link ChannelHandler}提供了一个{@link ChannelHandlerContext}对象。{@link ChannelHandler}应通过上下文对象与其所属的{@link ChannelPipeline}进行交互。使用上下文对象，{@link ChannelHandler}可以向上游或下游传递事件，动态修改管道，或存储特定于处理器的信息（使用{@link AttributeKey}）。
 *
 * <h3>状态管理</h3>
 *
 * {@link ChannelHandler}通常需要存储一些有状态的信息。最简单且推荐的方法是使用成员变量：
 * <pre>
 * public interface Message {
 *     // 你的方法在这里
 * }
 *
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *
 *     <b>private boolean loggedIn;</b>
 *
 *     {@code @Override}
 *     public void channelRead0({@link ChannelHandlerContext} ctx, Message message) {
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) message);
 *             <b>loggedIn = true;</b>
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>loggedIn</b>) {
 *                 ctx.writeAndFlush(fetchSecret((GetDataMessage) message));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * 由于处理器实例具有专用于一个连接的状态变量，因此必须为每个新通道创建一个新的处理器实例，以避免未经身份验证的客户端获取机密信息的竞争条件：
 * <pre>
 * // 为每个通道创建一个新的处理器实例。
 * // 参见{@link ChannelInitializer#initChannel(Channel)}。
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>new DataServerHandler()</b>);
 *     }
 * }
 *
 * </pre>
 *
 * <h4>使用{@link AttributeKey}</h4>
 *
 * 虽然建议使用成员变量来存储处理器的状态，但在某些情况下，您可能不希望创建许多处理器实例。在这种情况下，您可以使用{@link ChannelHandlerContext}提供的{@link AttributeKey}：
 * <pre>
 * public interface Message {
 *     // 你的方法在这里
 * }
 *
 * {@code @Sharable}
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *     private final {@link AttributeKey}&lt;{@link Boolean}&gt; auth =
 *           {@link AttributeKey#valueOf(String) AttributeKey.valueOf("auth")};
 *
 *     {@code @Override}
 *     public void channelRead({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Attribute}&lt;{@link Boolean}&gt; attr = ctx.attr(auth);
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) o);
 *             <b>attr.set(true)</b>;
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>Boolean.TRUE.equals(attr.get())</b>) {
 *                 ctx.writeAndFlush(fetchSecret((GetDataMessage) o));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * 现在，处理器的状态附加到{@link ChannelHandlerContext}，您可以将相同的处理器实例添加到不同的管道中：
 * <pre>
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *
 *     private static final DataServerHandler <b>SHARED</b> = new DataServerHandler();
 *
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>SHARED</b>);
 *     }
 * }
 * </pre>
 *
 *
 * <h4>{@code @Sharable}注解</h4>
 * <p>
 * 在上面使用{@link AttributeKey}的示例中，您可能已经注意到{@code @Sharable}注解。
 * <p>
 * 如果{@link ChannelHandler}被{@code @Sharable}注解标记，意味着您可以创建一个处理器实例，并将其多次添加到一个或多个{@link ChannelPipeline}中，而不会出现竞争条件。
 * <p>
 * 如果未指定此注解，则每次将其添加到管道时都必须创建一个新的处理器实例，因为它具有未共享的状态，例如成员变量。
 * <p>
 * 此注解用于文档目的，类似于<a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">JCIP注解</a>。
 *
 * <h3>值得阅读的额外资源</h3>
 * <p>
 * 请参阅{@link ChannelHandler}和{@link ChannelPipeline}，以了解更多关于入站和出站操作的信息，它们之间的根本区别，它们如何在管道中流动，以及如何在应用程序中处理这些操作。
 */
public interface ChannelHandler {

    /**
     * Gets called after the {@link ChannelHandler} was added to the actual context and it's ready to handle events.
     */

    /**
     * 在 {@link ChannelHandler} 被添加到实际上下文并准备好处理事件后被调用。
     */
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called after the {@link ChannelHandler} was removed from the actual context and it doesn't handle events
     * anymore.
     */

    /**
     * 在{@link ChannelHandler}从实际上下文中移除后调用，此时它不再处理事件。
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     *
     * @deprecated if you want to handle this event you should implement {@link ChannelInboundHandler} and
     * implement the method there.
     */

    /**
     * 在抛出 {@link Throwable} 时被调用。
     *
     * @deprecated 如果你想处理此事件，应该实现 {@link ChannelInboundHandler} 并在其中实现该方法。
     */
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * Indicates that the same instance of the annotated {@link ChannelHandler}
     * can be added to one or more {@link ChannelPipeline}s multiple times
     * without a race condition.
     * <p>
     * If this annotation is not specified, you have to create a new handler
     * instance every time you add it to a pipeline because it has unshared
     * state such as member variables.
     * <p>
     * This annotation is provided for documentation purpose, just like
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
     */

    /**
     * 表示被注解的 {@link ChannelHandler} 的同一实例可以多次添加到一个或多个 {@link ChannelPipeline} 中，
     * 而不会发生竞态条件。
     * <p>
     * 如果未指定此注解，则每次将处理器添加到管道时都必须创建一个新的处理器实例，因为它具有未共享的状态，例如成员变量。
     * <p>
     * 此注解仅用于文档目的，类似于
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">JCIP 注解</a>。
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
        // 无值
    }
}
