
package org.top.java.netty.source.channel;


import java.net.SocketAddress;

/**
 * {@link ChannelHandler} implementation which represents a combination out of a {@link ChannelInboundHandler} and
 * the {@link ChannelOutboundHandler}.
 *
 * It is a good starting point if your {@link ChannelHandler} implementation needs to intercept operations and also
 * state updates.
 */

/**
 * {@link ChannelHandler} 实现，它结合了 {@link ChannelInboundHandler} 和 {@link ChannelOutboundHandler}。
 *
 * 如果你的 {@link ChannelHandler} 实现需要拦截操作和状态更新，这是一个很好的起点。
 */
public class ChannelDuplexHandler extends ChannelInboundHandlerAdapter implements ChannelOutboundHandler {

    /**
     * Calls {@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)} 将操作转发给
     * {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#disconnect(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#disconnect(ChannelPromise)} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#close(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#close(ChannelPromise)} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#deregister(ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#deregister(ChannelPromise)} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#read()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#read()} 以转发
     * 到 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以更改行为。
     */
    
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    /**
     * Calls {@link ChannelHandlerContext#write(Object, ChannelPromise)} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#write(Object, ChannelPromise)} 将消息转发给
     * {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    /**
     * Calls {@link ChannelHandlerContext#flush()} to forward
     * to the next {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */

    /**
     * 调用 {@link ChannelHandlerContext#flush()} 以将操作转发
     * 给 {@link ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     *
     * 子类可以重写此方法以改变行为。
     */
    
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
