
package org.top.java.netty.source.channel;

import io.netty.channel.*;

public interface ChannelInboundInvoker {

    /**
     * A {@link io.netty.channel.Channel} was registered to its {@link EventLoop}.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#channelRegistered(io.netty.channel.ChannelHandlerContext)} method
     * called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 被注册到它的 {@link EventLoop}。
     *
     * 这将导致 {@link io.netty.channel.ChannelInboundHandler#channelRegistered(io.netty.channel.ChannelHandlerContext)} 方法
     * 被调用，该方法属于 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}，
     * 该管道属于 {@link io.netty.channel.Channel}。
     */
    ChannelInboundInvoker fireChannelRegistered();

    /**
     * A {@link io.netty.channel.Channel} was unregistered from its {@link EventLoop}.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#channelUnregistered(io.netty.channel.ChannelHandlerContext)} method
     * called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 从其 {@link EventLoop} 中注销。
     *
     * 这将导致在 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler} 的
     * {@link io.netty.channel.ChannelInboundHandler#channelUnregistered(io.netty.channel.ChannelHandlerContext)} 方法被调用。
     */
    ChannelInboundInvoker fireChannelUnregistered();

    /**
     * A {@link io.netty.channel.Channel} is active now, which means it is connected.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#channelActive(io.netty.channel.ChannelHandlerContext)} method
     * called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 现在处于活动状态，这意味着它已连接。
     *
     * 这将导致在 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler} 的
     * {@link io.netty.channel.ChannelInboundHandler#channelActive(io.netty.channel.ChannelHandlerContext)} 方法被调用。
     */
    ChannelInboundInvoker fireChannelActive();

    /**
     * A {@link io.netty.channel.Channel} is inactive now, which means it is closed.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#channelInactive(io.netty.channel.ChannelHandlerContext)} method
     * called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 现在处于非活动状态，这意味着它已关闭。
     *
     * 这将导致在 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler} 的
     * {@link io.netty.channel.ChannelInboundHandler#channelInactive(io.netty.channel.ChannelHandlerContext)} 方法被调用。
     */
    ChannelInboundInvoker fireChannelInactive();

    /**
     * A {@link io.netty.channel.Channel} received an {@link Throwable} in one of its inbound operations.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#exceptionCaught(io.netty.channel.ChannelHandlerContext, Throwable)}
     * method  called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 在其入站操作中接收到了一个 {@link Throwable}。
     *
     * 这将导致 {@link io.netty.channel.ChannelInboundHandler#exceptionCaught(io.netty.channel.ChannelHandlerContext, Throwable)}
     * 方法被调用，该方法属于 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}，
     * 该管道属于 {@link io.netty.channel.Channel}。
     */
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);

    /**
     * A {@link io.netty.channel.Channel} received an user defined event.
     *
     * This will result in having the  {@link io.netty.channel.ChannelInboundHandler#userEventTriggered(io.netty.channel.ChannelHandlerContext, Object)}
     * method  called of the next  {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 接收到了一个用户定义的事件。
     *
     * 这将导致 {@link io.netty.channel.ChannelInboundHandler#userEventTriggered(io.netty.channel.ChannelHandlerContext, Object)}
     * 方法被调用，该方法属于 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}，
     * 该管道属于 {@link io.netty.channel.Channel}。
     */
    ChannelInboundInvoker fireUserEventTriggered(Object event);

    /**
     * A {@link io.netty.channel.Channel} received a message.
     *
     * This will result in having the {@link io.netty.channel.ChannelInboundHandler#channelRead(io.netty.channel.ChannelHandlerContext, Object)}
     * method  called of the next {@link io.netty.channel.ChannelInboundHandler} contained in the  {@link io.netty.channel.ChannelPipeline} of the
     * {@link Channel}.
     */

    /**
     * 一个 {@link io.netty.channel.Channel} 接收到了一条消息。
     *
     * 这将导致 {@link io.netty.channel.ChannelInboundHandler#channelRead(io.netty.channel.ChannelHandlerContext, Object)}
     * 方法被调用，该方法属于 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     */
    ChannelInboundInvoker fireChannelRead(Object msg);

    /**
     * Triggers an {@link io.netty.channel.ChannelInboundHandler#channelReadComplete(io.netty.channel.ChannelHandlerContext)}
     * event to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link io.netty.channel.ChannelPipeline}.
     */

    /**
     * 触发一个 {@link io.netty.channel.ChannelInboundHandler#channelReadComplete(io.netty.channel.ChannelHandlerContext)}
     * 事件到 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     */
    ChannelInboundInvoker fireChannelReadComplete();

    /**
     * Triggers an {@link io.netty.channel.ChannelInboundHandler#channelWritabilityChanged(ChannelHandlerContext)}
     * event to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */

    /**
     * 触发一个 {@link io.netty.channel.ChannelInboundHandler#channelWritabilityChanged(ChannelHandlerContext)}
     * 事件到 {@link ChannelPipeline} 中的下一个 {@link ChannelInboundHandler}。
     */
    ChannelInboundInvoker fireChannelWritabilityChanged();
}
