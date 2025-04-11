
package org.top.java.netty.source.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeMap;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * A nexus to a network socket or a component which is capable of I/O
 * operations such as read, write, connect, and bind.
 * <p>
 * A channel provides a user:
 * <ul>
 * <li>the current state of the channel (e.g. is it open? is it connected?),</li>
 * <li>the {@linkplain ChannelConfig configuration parameters} of the channel (e.g. receive buffer size),</li>
 * <li>the I/O operations that the channel supports (e.g. read, write, connect, and bind), and</li>
 * <li>the {@link ChannelPipeline} which handles all I/O events and requests
 *     associated with the channel.</li>
 * </ul>
 *
 * <h3>All I/O operations are asynchronous.</h3>
 * <p>
 * All I/O operations in Netty are asynchronous.  It means any I/O calls will
 * return immediately with no guarantee that the requested I/O operation has
 * been completed at the end of the call.  Instead, you will be returned with
 * a {@link ChannelFuture} instance which will notify you when the requested I/O
 * operation has succeeded, failed, or canceled.
 *
 * <h3>Channels are hierarchical</h3>
 * <p>
 * A {@link Channel} can have a {@linkplain #parent() parent} depending on
 * how it was created.  For instance, a {@link SocketChannel}, that was accepted
 * by {@link ServerSocketChannel}, will return the {@link ServerSocketChannel}
 * as its parent on {@link #parent()}.
 * <p>
 * The semantics of the hierarchical structure depends on the transport
 * implementation where the {@link Channel} belongs to.  For example, you could
 * write a new {@link Channel} implementation that creates the sub-channels that
 * share one socket connection, as <a href="http://beepcore.org/">BEEP</a> and
 * <a href="https://en.wikipedia.org/wiki/Secure_Shell">SSH</a> do.
 *
 * <h3>Downcast to access transport-specific operations</h3>
 * <p>
 * Some transports exposes additional operations that is specific to the
 * transport.  Down-cast the {@link Channel} to sub-type to invoke such
 * operations.  For example, with the old I/O datagram transport, multicast
 * join / leave operations are provided by {@link DatagramChannel}.
 *
 * <h3>Release resources</h3>
 * <p>
 * It is important to call {@link #close()} or {@link #close(ChannelPromise)} to release all
 * resources once you are done with the {@link Channel}. This ensures all resources are
 * released in a proper way, i.e. filehandles.
 */


/**
 * 连接到网络套接字或能够执行I/O操作（如读、写、连接和绑定）的组件的枢纽。
 * <p>
 * 通道为用户提供：
 * <ul>
 * <li>通道的当前状态（例如，它是否打开？是否已连接？），</li>
 * <li>通道的{@linkplain ChannelConfig 配置参数}（例如接收缓冲区大小），</li>
 * <li>通道支持的I/O操作（例如读、写、连接和绑定），</li>
 * <li>处理与通道相关的所有I/O事件和请求的{@link ChannelPipeline}。</li>
 * </ul>
 *
 * <h3>所有I/O操作都是异步的。</h3>
 * <p>
 * Netty中的所有I/O操作都是异步的。这意味着任何I/O调用都会立即返回，而不保证请求的I/O操作在调用结束时已完成。相反，您将获得一个{@link ChannelFuture}实例，该实例将在请求的I/O操作成功、失败或取消时通知您。
 *
 * <h3>通道是分层的</h3>
 * <p>
 * 根据创建方式，{@link Channel}可以有一个{@linkplain #parent() 父通道}。例如，由{@link ServerSocketChannel}接受的{@link SocketChannel}将在{@link #parent()}中返回{@link ServerSocketChannel}作为其父通道。
 * <p>
 * 分层结构的语义取决于{@link Channel}所属的传输实现。例如，您可以编写一个新的{@link Channel}实现，创建共享一个套接字连接的子通道，就像<a href="http://beepcore.org/">BEEP</a>和<a href="https://en.wikipedia.org/wiki/Secure_Shell">SSH</a>所做的那样。
 *
 * <h3>向下转型以访问特定于传输的操作</h3>
 * <p>
 * 某些传输公开了特定于传输的附加操作。将{@link Channel}向下转型为子类型以调用此类操作。例如，在旧的I/O数据报传输中，组播加入/离开操作由{@link DatagramChannel}提供。
 *
 * <h3>释放资源</h3>
 * <p>
 * 重要的是在完成{@link Channel}的使用后调用{@link #close()}或{@link #close(ChannelPromise)}以释放所有资源。这确保所有资源都以正确的方式释放，例如文件句柄。
 */
public interface Channel extends AttributeMap, ChannelOutboundInvoker, Comparable<Channel> {

    /**
     * Returns the globally unique identifier of this {@link Channel}.
     */

    /**
     * 返回此 {@link Channel} 的全局唯一标识符。
     */
    ChannelId id();

    /**
     * Return the {@link EventLoop} this {@link Channel} was registered to.
     */

    /**
     * 返回此 {@link Channel} 注册到的 {@link EventLoop}。
     */
    EventLoop eventLoop();

    /**
     * Returns the parent of this channel.
     *
     * @return the parent channel.
     *         {@code null} if this channel does not have a parent channel.
     */

    /**
     * 返回此通道的父通道。
     *
     * @return 父通道。
     *         {@code null} 如果此通道没有父通道。
     */
    Channel parent();

    /**
     * Returns the configuration of this channel.
     */

    /**
     * 返回此通道的配置。
     */
    ChannelConfig config();

    /**
     * Returns {@code true} if the {@link Channel} is open and may get active later
     */

    /**
     * 如果 {@link Channel} 是打开的并且可能稍后变为活动状态，则返回 {@code true}
     */
    boolean isOpen();

    /**
     * Returns {@code true} if the {@link Channel} is registered with an {@link EventLoop}.
     */

    /**
     * 如果 {@link Channel} 已注册到 {@link EventLoop}，则返回 {@code true}。
     */
    boolean isRegistered();

    /**
     * Return {@code true} if the {@link Channel} is active and so connected.
     */

    /**
     * 如果 {@link Channel} 是活动的并且已连接，则返回 {@code true}。
     */
    boolean isActive();

    /**
     * Return the {@link ChannelMetadata} of the {@link Channel} which describe the nature of the {@link Channel}.
     */

    /**
     * 返回描述 {@link Channel} 性质的 {@link ChannelMetadata}。
     */
    ChannelMetadata metadata();

    /**
     * Returns the local address where this channel is bound to.  The returned
     * {@link SocketAddress} is supposed to be down-cast into more concrete
     * type such as {@link InetSocketAddress} to retrieve the detailed
     * information.
     *
     * @return the local address of this channel.
     *         {@code null} if this channel is not bound.
     */

    /**
     * 返回此通道绑定的本地地址。返回的 {@link SocketAddress} 应向下转换为更具体的类型，
     * 例如 {@link InetSocketAddress}，以检索详细信息。
     *
     * @return 此通道的本地地址。
     *         如果此通道未绑定，则返回 {@code null}。
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address where this channel is connected to.  The
     * returned {@link SocketAddress} is supposed to be down-cast into more
     * concrete type such as {@link InetSocketAddress} to retrieve the detailed
     * information.
     *
     * @return the remote address of this channel.
     *         {@code null} if this channel is not connected.
     *         If this channel is not connected but it can receive messages
     *         from arbitrary remote addresses (e.g. {@link DatagramChannel},
     *         use {@link DatagramPacket#recipient()} to determine
     *         the origination of the received message as this method will
     *         return {@code null}.
     */

    /**
     * 返回此通道连接的远程地址。返回的 {@link SocketAddress} 应该被向下转型为更具体的类型，
     * 例如 {@link InetSocketAddress}，以获取详细信息。
     *
     * @return 此通道的远程地址。
     *         如果此通道未连接，则返回 {@code null}。
     *         如果此通道未连接但可以从任意远程地址接收消息（例如 {@link DatagramChannel}），
     *         请使用 {@link DatagramPacket#recipient()} 来确定接收消息的源地址，
     *         因为此方法将返回 {@code null}。
     */
    SocketAddress remoteAddress();

    /**
     * Returns the {@link ChannelFuture} which will be notified when this
     * channel is closed.  This method always returns the same future instance.
     */

    /**
     * 返回将在该通道关闭时被通知的 {@link ChannelFuture}。此方法总是返回相同的 future 实例。
     */
    ChannelFuture closeFuture();

    /**
     * Returns {@code true} if and only if the I/O thread will perform the
     * requested write operation immediately.  Any write requests made when
     * this method returns {@code false} are queued until the I/O thread is
     * ready to process the queued write requests.
     */

    /**
     * 当且仅当I/O线程将立即执行请求的写操作时返回{@code true}。当此方法返回{@code false}时，任何写请求将被排队，直到I/O线程准备好处理排队的写请求。
     */
    boolean isWritable();

    /**
     * Get how many bytes can be written until {@link #isWritable()} returns {@code false}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code false} then 0.
     */

    /**
     * 获取在 {@link #isWritable()} 返回 {@code false} 之前可以写入的字节数。
     * 该值始终为非负数。如果 {@link #isWritable()} 为 {@code false}，则返回 0。
     */
    long bytesBeforeUnwritable();

    /**
     * Get how many bytes must be drained from underlying buffers until {@link #isWritable()} returns {@code true}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code true} then 0.
     */

    /**
     * 获取必须从底层缓冲区中排出的字节数，直到 {@link #isWritable()} 返回 {@code true}。
     * 该值始终为非负数。如果 {@link #isWritable()} 为 {@code true}，则返回 0。
     */
    long bytesBeforeWritable();

    /**
     * Returns an <em>internal-use-only</em> object that provides unsafe operations.
     */

    /**
     * 返回一个<em>仅供内部使用</em>的对象，该对象提供不安全的操作。
     */
    Unsafe unsafe();

    /**
     * Return the assigned {@link ChannelPipeline}.
     */

    /**
     * 返回分配的 {@link ChannelPipeline}。
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     */

    /**
     * 返回分配的 {@link ByteBufAllocator}，它将用于分配 {@link ByteBuf}。
     */
    ByteBufAllocator alloc();

    @Override
    Channel read();

    @Override
    Channel flush();

    /**
     * <em>Unsafe</em> operations that should <em>never</em> be called from user-code. These methods
     * are only provided to implement the actual transport, and must be invoked from an I/O thread except for the
     * following methods:
     * <ul>
     *   <li>{@link #localAddress()}</li>
     *   <li>{@link #remoteAddress()}</li>
     *   <li>{@link #closeForcibly()}</li>
     *   <li>{@link #register(EventLoop, ChannelPromise)}</li>
     *   <li>{@link #deregister(ChannelPromise)}</li>
     *   <li>{@link #voidPromise()}</li>
     * </ul>
     */

    /**
     * <em>不安全</em>的操作，这些方法<em>永远</em>不应该从用户代码中调用。这些方法
     * 仅用于实现实际的传输，并且必须从I/O线程中调用，除了以下方法：
     * <ul>
     *   <li>{@link #localAddress()}</li>
     *   <li>{@link #remoteAddress()}</li>
     *   <li>{@link #closeForcibly()}</li>
     *   <li>{@link #register(EventLoop, ChannelPromise)}</li>
     *   <li>{@link #deregister(ChannelPromise)}</li>
     *   <li>{@link #voidPromise()}</li>
     * </ul>
     */
    interface Unsafe {

        /**
         * Return the assigned {@link RecvByteBufAllocator.Handle} which will be used to allocate {@link ByteBuf}'s when
         * receiving data.
         */

        /**
         * 返回分配的 {@link RecvByteBufAllocator.Handle}，该句柄将在接收数据时用于分配 {@link ByteBuf}。
         */
        RecvByteBufAllocator.Handle recvBufAllocHandle();

        /**
         * Return the {@link SocketAddress} to which is bound local or
         * {@code null} if none.
         */

        /**
         * 返回绑定的本地 {@link SocketAddress}，如果没有绑定则返回 {@code null}。
         */
        SocketAddress localAddress();

        /**
         * Return the {@link SocketAddress} to which is bound remote or
         * {@code null} if none is bound yet.
         */

        /**
         * 返回绑定的远程 {@link SocketAddress}，如果尚未绑定则返回 {@code null}。
         */
        SocketAddress remoteAddress();

        /**
         * Register the {@link Channel} of the {@link ChannelPromise} and notify
         * the {@link ChannelFuture} once the registration was complete.
         */

        /**
         * 注册 {@link ChannelPromise} 的 {@link Channel} 并在注册完成后通知
         * {@link ChannelFuture}。
         */
        void register(EventLoop eventLoop, ChannelPromise promise);

        /**
         * Bind the {@link SocketAddress} to the {@link Channel} of the {@link ChannelPromise} and notify
         * it once its done.
         */

        /**
         * 将 {@link SocketAddress} 绑定到 {@link ChannelPromise} 的 {@link Channel} 并在完成后通知它。
         */
        void bind(SocketAddress localAddress, ChannelPromise promise);

        /**
         * Connect the {@link Channel} of the given {@link ChannelFuture} with the given remote {@link SocketAddress}.
         * If a specific local {@link SocketAddress} should be used it need to be given as argument. Otherwise just
         * pass {@code null} to it.
         *
         * The {@link ChannelPromise} will get notified once the connect operation was complete.
         */

        /**
         * 将给定的 {@link ChannelFuture} 的 {@link Channel} 与给定的远程 {@link SocketAddress} 连接。
         * 如果应使用特定的本地 {@link SocketAddress}，则需要将其作为参数传递。否则，只需传递 {@code null}。
         *
         * 一旦连接操作完成，{@link ChannelPromise} 将会被通知。
         */
        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        /**
         * Disconnect the {@link Channel} of the {@link ChannelFuture} and notify the {@link ChannelPromise} once the
         * operation was complete.
         */

        /**
         * 断开 {@link ChannelFuture} 的 {@link Channel} 并在操作完成后通知 {@link ChannelPromise}。
         */
        void disconnect(ChannelPromise promise);

        /**
         * Close the {@link Channel} of the {@link ChannelPromise} and notify the {@link ChannelPromise} once the
         * operation was complete.
         */

        /**
         * 关闭 {@link ChannelPromise} 的 {@link Channel} 并在操作完成后通知 {@link ChannelPromise}。
         */
        void close(ChannelPromise promise);

        /**
         * Closes the {@link Channel} immediately without firing any events.  Probably only useful
         * when registration attempt failed.
         */

        /**
         * 立即关闭 {@link Channel}，不触发任何事件。可能仅在注册尝试失败时有用。
         */
        void closeForcibly();

        /**
         * Deregister the {@link Channel} of the {@link ChannelPromise} from {@link EventLoop} and notify the
         * {@link ChannelPromise} once the operation was complete.
         */

        /**
         * 从 {@link EventLoop} 中注销 {@link ChannelPromise} 的 {@link Channel}，并在操作完成后通知 {@link ChannelPromise}。
         */
        void deregister(ChannelPromise promise);

        /**
         * Schedules a read operation that fills the inbound buffer of the first {@link ChannelInboundHandler} in the
         * {@link ChannelPipeline}.  If there's already a pending read operation, this method does nothing.
         */

        /**
         * 调度一个读取操作，该操作将填充 {@link ChannelPipeline} 中第一个 {@link ChannelInboundHandler} 的入站缓冲区。
         * 如果已经有一个挂起的读取操作，则此方法不执行任何操作。
         */
        void beginRead();

        /**
         * Schedules a write operation.
         */

        /**
         * 调度一个写操作。
         */
        void write(Object msg, ChannelPromise promise);

        /**
         * Flush out all write operations scheduled via {@link #write(Object, ChannelPromise)}.
         */

        /**
         * 刷新所有通过 {@link #write(Object, ChannelPromise)} 调度的写操作。
         */
        void flush();

        /**
         * Return a special ChannelPromise which can be reused and passed to the operations in {@link Unsafe}.
         * It will never be notified of a success or error and so is only a placeholder for operations
         * that take a {@link ChannelPromise} as argument but for which you not want to get notified.
         */

        /**
         * 返回一个特殊的ChannelPromise，可以重复使用并传递给{@link Unsafe}中的操作。
         * 它永远不会被通知成功或错误，因此仅作为占位符用于那些需要{@link ChannelPromise}作为参数
         * 但你不希望收到通知的操作。
         */
        ChannelPromise voidPromise();

        /**
         * Returns the {@link ChannelOutboundBuffer} of the {@link Channel} where the pending write requests are stored.
         */

        /**
         * 返回{@link Channel}的{@link ChannelOutboundBuffer}，其中存储了待处理的写请求。
         */
        ChannelOutboundBuffer outboundBuffer();
    }
}
