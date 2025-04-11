
package org.top.java.netty.source.channel;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FutureListener;

import java.net.ConnectException;
import java.net.SocketAddress;

public interface ChannelOutboundInvoker {

    /**
     * Request to bind to the given {@link SocketAddress} and notify the {@link io.netty.channel.ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} method
     * called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求绑定到给定的 {@link SocketAddress}，并在操作完成时通知 {@link io.netty.channel.ChannelFuture}，无论操作成功还是失败。
     * <p>
     * 这将导致调用下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} 方法，
     * 该方法包含在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中。
     */
    ChannelFuture bind(SocketAddress localAddress);

    /**
     * Request to connect to the given {@link SocketAddress} and notify the {@link io.netty.channel.ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * If the connection fails because of a connection timeout, the {@link io.netty.channel.ChannelFuture} will get failed with
     * a {@link ConnectTimeoutException}. If it fails because of connection refused a {@link ConnectException}
     * will be used.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求连接到给定的 {@link SocketAddress}，并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，无论是操作成功还是因为错误。
     * <p>
     * 如果连接因连接超时而失败，{@link io.netty.channel.ChannelFuture} 将以 {@link ConnectTimeoutException} 失败。如果因连接被拒绝而失败，则将使用 {@link ConnectException}。
     * <p>
     * 这将导致在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中包含的下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)} 方法被调用。
     */
    ChannelFuture connect(SocketAddress remoteAddress);

    /**
     * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify the
     * {@link io.netty.channel.ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求连接到给定的 {@link SocketAddress}，同时绑定到本地地址，并在操作完成时通知
     * {@link io.netty.channel.ChannelFuture}，无论操作成功还是因为错误。
     * <p>
     * 这将导致调用下一个包含在 {@link io.netty.channel.ChannelPipeline} 中的
     * {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * 方法。
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * Request to disconnect from the remote peer and notify the {@link io.netty.channel.ChannelFuture} once the operation completes,
     * either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求与远程对等方断开连接，并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，
     * 无论操作成功还是因为发生错误。
     * <p>
     * 这将导致调用下一个包含在 {@link io.netty.channel.ChannelPipeline} 中的
     * {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * 方法。
     */
    ChannelFuture disconnect();

    /**
     * Request to close the {@link io.netty.channel.Channel} and notify the {@link io.netty.channel.ChannelFuture} once the operation completes,
     * either because the operation was successful or because of
     * an error.
     *
     * After it is closed it is not possible to reuse it again.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求关闭 {@link io.netty.channel.Channel} 并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，
     * 无论操作成功还是因为发生错误。
     *
     * 关闭后无法再次使用。
     * <p>
     * 这将导致调用下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * 方法，该处理程序包含在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中。
     */
    ChannelFuture close();

    /**
     * Request to deregister from the previous assigned {@link EventExecutor} and notify the
     * {@link io.netty.channel.ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     *
     */

    /**
     * 请求从之前分配的 {@link EventExecutor} 中注销，并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，无论操作成功还是因为错误。
     * <p>
     * 这将导致调用下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)}
     * 方法，该处理器包含在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中。
     *
     */
    ChannelFuture deregister();

    /**
     * Request to bind to the given {@link SocketAddress} and notify the {@link io.netty.channel.ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} method
     * called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求绑定到给定的 {@link SocketAddress}，并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，
     * 无论操作成功还是因错误而失败。
     *
     * 给定的 {@link ChannelPromise} 将被通知。
     * <p>
     * 这将导致在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中的
     * 下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, ChannelPromise)} 方法被调用。
     */
    ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise);

    /**
     * Request to connect to the given {@link SocketAddress} and notify the {@link io.netty.channel.ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     *
     * The given {@link io.netty.channel.ChannelFuture} will be notified.
     *
     * <p>
     * If the connection fails because of a connection timeout, the {@link io.netty.channel.ChannelFuture} will get failed with
     * a {@link ConnectTimeoutException}. If it fails because of connection refused a {@link ConnectException}
     * will be used.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求连接到给定的 {@link SocketAddress}，并在操作完成时通知 {@link io.netty.channel.ChannelFuture}，无论操作成功还是因错误而失败。
     *
     * 给定的 {@link io.netty.channel.ChannelFuture} 将被通知。
     *
     * <p>
     * 如果连接因连接超时而失败，{@link io.netty.channel.ChannelFuture} 将以 {@link ConnectTimeoutException} 失败。如果因连接被拒绝而失败，则使用 {@link ConnectException}。
     * <p>
     * 这将导致调用下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * 方法，该处理器包含在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中。
     */
    ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise);

    /**
     * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify the
     * {@link io.netty.channel.ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     *
     * The given {@link ChannelPromise} will be notified and also returned.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求连接到给定的 {@link SocketAddress}，同时绑定到本地地址，并在操作完成时通知 {@link io.netty.channel.ChannelFuture}，无论操作成功还是发生错误。
     *
     * 给定的 {@link ChannelPromise} 将被通知并返回。
     * <p>
     * 这将导致调用下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, ChannelPromise)}
     * 方法，该处理程序包含在 {@link io.netty.channel.Channel} 的 {@link io.netty.channel.ChannelPipeline} 中。
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

    /**
     * Request to disconnect from the remote peer and notify the {@link io.netty.channel.ChannelFuture} once the operation completes,
     * either because the operation was successful or because of an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求与远程对等端断开连接，并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，
     * 无论操作成功还是发生错误。
     *
     * 给定的 {@link ChannelPromise} 将被通知。
     * <p>
     * 这将导致在 {@link io.netty.channel.ChannelPipeline} 中的下一个
     * {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#disconnect(ChannelHandlerContext, ChannelPromise)}
     * 方法被调用。
     */
    ChannelFuture disconnect(ChannelPromise promise);

    /**
     * Request to close the {@link io.netty.channel.Channel} and notify the {@link io.netty.channel.ChannelFuture} once the operation completes,
     * either because the operation was successful or because of
     * an error.
     *
     * After it is closed it is not possible to reuse it again.
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求关闭 {@link io.netty.channel.Channel} 并在操作完成后通知 {@link io.netty.channel.ChannelFuture}，
     * 无论操作成功还是因为错误。
     *
     * 关闭后，无法再次使用它。
     * 给定的 {@link ChannelPromise} 将被通知。
     * <p>
     * 这将导致在 {@link io.netty.channel.ChannelPipeline} 中的下一个
     * {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#close(ChannelHandlerContext, ChannelPromise)}
     * 方法被调用。
     */
    ChannelFuture close(ChannelPromise promise);

    /**
     * Request to deregister from the previous assigned {@link EventExecutor} and notify the
     * {@link io.netty.channel.ChannelFuture} once the operation completes, either because the operation was successful or because of
     * an error.
     *
     * The given {@link ChannelPromise} will be notified.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)}
     * method called of the next {@link io.netty.channel.ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link io.netty.channel.Channel}.
     */

    /**
     * 请求从先前分配的 {@link EventExecutor} 中注销，并在操作完成时通知 {@link io.netty.channel.ChannelFuture}，无论操作成功还是因错误而完成。
     *
     * 给定的 {@link ChannelPromise} 将被通知。
     * <p>
     * 这将导致调用 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link io.netty.channel.ChannelOutboundHandler} 的
     * {@link io.netty.channel.ChannelOutboundHandler#deregister(ChannelHandlerContext, ChannelPromise)} 方法。
     */
    ChannelFuture deregister(ChannelPromise promise);

    /**
     * Request to Read data from the {@link io.netty.channel.Channel} into the first inbound buffer, triggers an
     * {@link io.netty.channel.ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)} event if data was
     * read, and triggers a
     * {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext) channelReadComplete} event so the
     * handler can decide to continue reading.  If there's a pending read operation already, this method does nothing.
     * <p>
     * This will result in having the
     * {@link io.netty.channel.ChannelOutboundHandler#read(ChannelHandlerContext)}
     * method called of the next {@link ChannelOutboundHandler} contained in the {@link io.netty.channel.ChannelPipeline} of the
     * {@link Channel}.
     */

    /**
     * 请求从 {@link io.netty.channel.Channel} 读取数据到第一个入站缓冲区，如果读取到数据，则触发
     * {@link io.netty.channel.ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)} 事件，并触发
     * {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext) channelReadComplete} 事件，以便处理器可以决定是否继续读取。如果已经有一个挂起的读取操作，则此方法不执行任何操作。
     * <p>
     * 这将导致调用 {@link io.netty.channel.ChannelOutboundHandler#read(ChannelHandlerContext)}
     * 方法，该方法属于 {@link io.netty.channel.ChannelPipeline} 中的下一个 {@link ChannelOutboundHandler}。
     */
    ChannelOutboundInvoker read();

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link io.netty.channel.ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     */

    /**
     * 通过这个 {@link ChannelHandlerContext} 请求写入消息，消息将通过 {@link io.netty.channel.ChannelPipeline} 传递。
     * 此方法不会请求实际刷新，因此请确保在需要将所有挂起的数据刷新到实际传输时调用 {@link #flush()}。
     */
    ChannelFuture write(Object msg);

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link io.netty.channel.ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     */

    /**
     * 通过这个 {@link ChannelHandlerContext} 请求写入消息，消息将通过 {@link io.netty.channel.ChannelPipeline} 传递。
     * 此方法不会请求实际刷新，因此请确保在需要将所有挂起的数据刷新到实际传输时调用 {@link #flush()}。
     */
    ChannelFuture write(Object msg, ChannelPromise promise);

    /**
     * Request to flush all pending messages via this ChannelOutboundInvoker.
     */

    /**
     * 请求通过此 ChannelOutboundInvoker 刷新所有挂起的消息。
     */
    ChannelOutboundInvoker flush();

    /**
     * Shortcut for call {@link #write(Object, ChannelPromise)} and {@link #flush()}.
     */

    /**
     * 调用 {@link #write(Object, ChannelPromise)} 和 {@link #flush()} 的快捷方式。
     */
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);

    /**
     * Shortcut for call {@link #write(Object)} and {@link #flush()}.
     */

    /**
     * 调用 {@link #write(Object)} 和 {@link #flush()} 的快捷方式。
     */
    ChannelFuture writeAndFlush(Object msg);

    /**
     * Return a new {@link ChannelPromise}.
     */

    /**
     * 返回一个新的 {@link ChannelPromise}。
     */
    ChannelPromise newPromise();

    /**
     * Return an new {@link ChannelProgressivePromise}
     */

    /**
     * 返回一个新的 {@link ChannelProgressivePromise}
     */
    ChannelProgressivePromise newProgressivePromise();

    /**
     * Create a new {@link io.netty.channel.ChannelFuture} which is marked as succeeded already. So {@link io.netty.channel.ChannelFuture#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */

    /**
     * 创建一个新的 {@link io.netty.channel.ChannelFuture}，该对象已被标记为成功。因此 {@link io.netty.channel.ChannelFuture#isSuccess()}
     * 将返回 {@code true}。所有添加到它的 {@link FutureListener} 将立即被通知。此外，
     * 所有阻塞方法的调用都将直接返回而不会阻塞。
     */
    ChannelFuture newSucceededFuture();

    /**
     * Create a new {@link io.netty.channel.ChannelFuture} which is marked as failed already. So {@link io.netty.channel.ChannelFuture#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */

    /**
     * 创建一个已经标记为失败的 {@link io.netty.channel.ChannelFuture}。因此 {@link io.netty.channel.ChannelFuture#isSuccess()}
     * 将返回 {@code false}。所有添加到它的 {@link FutureListener} 将立即被通知。此外，
     * 所有阻塞方法的调用将直接返回而不会阻塞。
     */
    ChannelFuture newFailedFuture(Throwable cause);

    /**
     * Return a special ChannelPromise which can be reused for different operations.
     * <p>
     * It's only supported to use
     * it for {@link ChannelOutboundInvoker#write(Object, ChannelPromise)}.
     * </p>
     * <p>
     * Be aware that the returned {@link ChannelPromise} will not support most operations and should only be used
     * if you want to save an object allocation for every write operation. You will not be able to detect if the
     * operation  was complete, only if it failed as the implementation will call
     * {@link ChannelPipeline#fireExceptionCaught(Throwable)} in this case.
     * </p>
     * <strong>Be aware this is an expert feature and should be used with care!</strong>
     */

    /**
     * 返回一个可以重复用于不同操作的特殊ChannelPromise。
     * <p>
     * 它仅支持用于 {@link ChannelOutboundInvoker#write(Object, ChannelPromise)}。
     * </p>
     * <p>
     * 请注意，返回的 {@link ChannelPromise} 不支持大多数操作，应仅在您希望为每个写操作节省对象分配时使用。您将无法检测操作是否完成，只能检测是否失败，因为在这种情况下，实现将调用 {@link ChannelPipeline#fireExceptionCaught(Throwable)}。
     * </p>
     * <strong>请注意，这是一个专家功能，应谨慎使用！</strong>
     */
    ChannelPromise voidPromise();
}
