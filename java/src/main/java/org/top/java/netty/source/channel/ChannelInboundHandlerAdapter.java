
package org.top.java.netty.source.channel;

import io.netty.channel.*;

/**
 * Abstract base class for {@link io.netty.channel.ChannelInboundHandler} implementations which provide
 * implementations of all of their methods.
 *
 * <p>
 * This implementation just forward the operation to the next {@link io.netty.channel.ChannelHandler} in the
 * {@link ChannelPipeline}. Sub-classes may override a method implementation to change this.
 * </p>
 * <p>
 * Be aware that messages are not released after the {@link #channelRead(ChannelHandlerContext, Object)}
 * method returns automatically. If you are looking for a {@link io.netty.channel.ChannelInboundHandler} implementation that
 * releases the received messages automatically, please see {@link SimpleChannelInboundHandler}.
 * </p>
 */

/**
 * 为 {@link io.netty.channel.ChannelInboundHandler} 实现提供的抽象基类，该类实现了所有方法。
 *
 * <p>
 * 此实现只是将操作转发给 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelHandler}。子类可以覆盖方法实现以更改此行为。
 * </p>
 * <p>
 * 请注意，在 {@link #channelRead(ChannelHandlerContext, Object)} 方法返回后，消息不会自动释放。如果您正在寻找一个自动释放接收到的消息的 {@link io.netty.channel.ChannelInboundHandler} 实现，请参阅 {@link SimpleChannelInboundHandler}。
 * </p>
 */
public class ChannelInboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelInboundHandler {

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRegistered()} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelRegistered()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelUnregistered()} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelUnregistered()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelActive()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelInactive()} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelInactive()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelRead(Object)} 将消息转发给
     * {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelReadComplete()} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelReadComplete()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireUserEventTriggered(Object)} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireUserEventTriggered(Object)} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelInboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelWritabilityChanged()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireChannelWritabilityChanged()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelInboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} 将异常传递给
     * {@link ChannelPipeline} 中的下一个 {@link ChannelHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    @Override
    @SuppressWarnings("deprecation")
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
