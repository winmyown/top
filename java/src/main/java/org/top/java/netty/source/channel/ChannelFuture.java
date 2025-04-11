
package org.top.java.netty.source.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.*;
import io.netty.util.concurrent.BlockingOperationException;
import org.top.java.netty.source.util.concurrent.Future;
import org.top.java.netty.source.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous {@link io.netty.channel.Channel} I/O operation.
 * <p>
 * All I/O operations in Netty are asynchronous.  It means any I/O calls will
 * return immediately with no guarantee that the requested I/O operation has
 * been completed at the end of the call.  Instead, you will be returned with
 * a {@link ChannelFuture} instance which gives you the information about the
 * result or status of the I/O operation.
 * <p>
 * A {@link ChannelFuture} is either <em>uncompleted</em> or <em>completed</em>.
 * When an I/O operation begins, a new future object is created.  The new future
 * is uncompleted initially - it is neither succeeded, failed, nor cancelled
 * because the I/O operation is not finished yet.  If the I/O operation is
 * finished either successfully, with failure, or by cancellation, the future is
 * marked as completed with more specific information, such as the cause of the
 * failure.  Please note that even failure and cancellation belong to the
 * completed state.
 * <pre>
 *                                      +---------------------------+
 *                                      | Completed successfully    |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |       cause() = non-null  |
 * |       cause() = null     |    |    +===========================+
 * +--------------------------+    |    | Completed by cancellation |
 *                                 |    +---------------------------+
 *                                 +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 *
 * Various methods are provided to let you check if the I/O operation has been
 * completed, wait for the completion, and retrieve the result of the I/O
 * operation. It also allows you to add {@link ChannelFutureListener}s so you
 * can get notified when the I/O operation is completed.
 *
 * <h3>Prefer {@link #addListener(GenericFutureListener)} to {@link #await()}</h3>
 *
 * It is recommended to prefer {@link #addListener(GenericFutureListener)} to
 * {@link #await()} wherever possible to get notified when an I/O operation is
 * done and to do any follow-up tasks.
 * <p>
 * {@link #addListener(GenericFutureListener)} is non-blocking.  It simply adds
 * the specified {@link ChannelFutureListener} to the {@link ChannelFuture}, and
 * I/O thread will notify the listeners when the I/O operation associated with
 * the future is done.  {@link ChannelFutureListener} yields the best
 * performance and resource utilization because it does not block at all, but
 * it could be tricky to implement a sequential logic if you are not used to
 * event-driven programming.
 * <p>
 * By contrast, {@link #await()} is a blocking operation.  Once called, the
 * caller thread blocks until the operation is done.  It is easier to implement
 * a sequential logic with {@link #await()}, but the caller thread blocks
 * unnecessarily until the I/O operation is done and there's relatively
 * expensive cost of inter-thread notification.  Moreover, there's a chance of
 * dead lock in a particular circumstance, which is described below.
 *
 * <h3>Do not call {@link #await()} inside {@link io.netty.channel.ChannelHandler}</h3>
 * <p>
 * The event handler methods in {@link ChannelHandler} are usually called by
 * an I/O thread.  If {@link #await()} is called by an event handler
 * method, which is called by the I/O thread, the I/O operation it is waiting
 * for might never complete because {@link #await()} can block the I/O
 * operation it is waiting for, which is a dead lock.
 * <pre>
 * // BAD - NEVER DO THIS
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.awaitUninterruptibly();
 *     // Perform post-closure operation
 *     // ...
 * }
 *
 * // GOOD
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.addListener(new {@link ChannelFutureListener}() {
 *         public void operationComplete({@link ChannelFuture} future) {
 *             // Perform post-closure operation
 *             // ...
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * In spite of the disadvantages mentioned above, there are certainly the cases
 * where it is more convenient to call {@link #await()}. In such a case, please
 * make sure you do not call {@link #await()} in an I/O thread.  Otherwise,
 * {@link BlockingOperationException} will be raised to prevent a dead lock.
 *
 * <h3>Do not confuse I/O timeout and await timeout</h3>
 *
 * The timeout value you specify with {@link #await(long)},
 * {@link #await(long, TimeUnit)}, {@link #awaitUninterruptibly(long)}, or
 * {@link #awaitUninterruptibly(long, TimeUnit)} are not related with I/O
 * timeout at all.  If an I/O operation times out, the future will be marked as
 * 'completed with failure,' as depicted in the diagram above.  For example,
 * connect timeout should be configured via a transport-specific option:
 * <pre>
 * // BAD - NEVER DO THIS
 * {@link Bootstrap} b = ...;
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly(10, TimeUnit.SECONDS);
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     // You might get a NullPointerException here because the future
 *     // might not be completed yet.
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 *
 * // GOOD
 * {@link Bootstrap} b = ...;
 * // Configure the connect timeout option.
 * <b>b.option({@link ChannelOption}.CONNECT_TIMEOUT_MILLIS, 10000);</b>
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly();
 *
 * // Now we are sure the future is completed.
 * assert f.isDone();
 *
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 * </pre>
 */


/**
 * 异步 {@link io.netty.channel.Channel} I/O 操作的结果。
 * <p>
 * Netty 中的所有 I/O 操作都是异步的。这意味着任何 I/O 调用都会立即返回，而无法保证请求的 I/O 操作在调用结束时已完成。相反，您将获得一个 {@link ChannelFuture} 实例，它为您提供有关 I/O 操作结果或状态的信息。
 * <p>
 * {@link ChannelFuture} 要么是<em>未完成</em>的，要么是<em>已完成</em>的。当 I/O 操作开始时，会创建一个新的 future 对象。新的 future 最初是未完成的——它既没有成功，也没有失败，也没有被取消，因为 I/O 操作尚未完成。如果 I/O 操作成功完成、失败或取消，future 将被标记为已完成，并提供更具体的信息，例如失败的原因。请注意，即使是失败和取消也属于已完成状态。
 * <pre>
 *                                      +---------------------------+
 *                                      | 成功完成                  |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------------------------+    |    |   isSuccess() = true      |
 * |        未完成            |    |    +===========================+
 * +--------------------------+    |    | 失败完成                  |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |       cause() = non-null  |
 * |       cause() = null     |    |    +===========================+
 * +--------------------------+    |    | 取消完成                  |
 *                                 |    +---------------------------+
 *                                 +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 *
 * 提供了各种方法来让您检查 I/O 操作是否已完成、等待完成并检索 I/O 操作的结果。它还允许您添加 {@link ChannelFutureListener}，以便在 I/O 操作完成时获得通知。
 *
 * <h3>优先使用 {@link #addListener(GenericFutureListener)} 而不是 {@link #await()}</h3>
 *
 * 建议尽可能优先使用 {@link #addListener(GenericFutureListener)} 而不是 {@link #await()}，以便在 I/O 操作完成时获得通知并执行任何后续任务。
 * <p>
 * {@link #addListener(GenericFutureListener)} 是非阻塞的。它只是将指定的 {@link ChannelFutureListener} 添加到 {@link ChannelFuture} 中，I/O 线程将在与 future 关联的 I/O 操作完成时通知监听器。{@link ChannelFutureListener} 提供了最佳的性能和资源利用率，因为它根本不会阻塞，但如果您不习惯事件驱动编程，实现顺序逻辑可能会有些棘手。
 * <p>
 * 相比之下，{@link #await()} 是一个阻塞操作。一旦调用，调用线程将阻塞，直到操作完成。使用 {@link #await()} 实现顺序逻辑更容易，但调用线程会不必要地阻塞，直到 I/O 操作完成，并且线程间通知的成本相对较高。此外，在某些特定情况下可能会出现死锁，如下所述。
 *
 * <h3>不要在 {@link io.netty.channel.ChannelHandler} 内部调用 {@link #await()}</h3>
 * <p>
 * {@link ChannelHandler} 中的事件处理方法通常由 I/O 线程调用。如果事件处理方法中调用了 {@link #await()}，而该事件处理方法是由 I/O 线程调用的，那么它等待的 I/O 操作可能永远不会完成，因为 {@link #await()} 可能会阻塞它正在等待的 I/O 操作，从而导致死锁。
 * <pre>
 * // 错误 - 永远不要这样做
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.awaitUninterruptibly();
 *     // 执行关闭后操作
 *     // ...
 * }
 *
 * // 正确
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.addListener(new {@link ChannelFutureListener}() {
 *         public void operationComplete({@link ChannelFuture} future) {
 *             // 执行关闭后操作
 *             // ...
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * 尽管有上述缺点，但在某些情况下调用 {@link #await()} 更为方便。在这种情况下，请确保不要在 I/O 线程中调用 {@link #await()}。否则，将引发 {@link BlockingOperationException} 以防止死锁。
 *
 * <h3>不要混淆 I/O 超时和 await 超时</h3>
 *
 * 使用 {@link #await(long)}、{@link #await(long, TimeUnit)}、{@link #awaitUninterruptibly(long)} 或 {@link #awaitUninterruptibly(long, TimeUnit)} 指定的超时值与 I/O 超时完全无关。如果 I/O 操作超时，future 将被标记为“失败完成”，如上图所示。例如，连接超时应通过传输特定的选项进行配置：
 * <pre>
 * // 错误 - 永远不要这样做
 * {@link Bootstrap} b = ...;
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly(10, TimeUnit.SECONDS);
 * if (f.isCancelled()) {
 *     // 连接尝试被用户取消
 * } else if (!f.isSuccess()) {
 *     // 您可能会在这里得到一个 NullPointerException，因为 future 可能尚未完成。
 *     f.cause().printStackTrace();
 * } else {
 *     // 连接成功建立
 * }
 *
 * // 正确
 * {@link Bootstrap} b = ...;
 * // 配置连接超时选项。
 * <b>b.option({@link ChannelOption}.CONNECT_TIMEOUT_MILLIS, 10000);</b>
 * {@link ChannelFuture} f = b.connect(...);
 * f.awaitUninterruptibly();
 *
 * // 现在我们可以确定 future 已完成。
 * assert f.isDone();
 *
 * if (f.isCancelled()) {
 *     // 连接尝试被用户取消
 * } else if (!f.isSuccess()) {
 *     f.cause().printStackTrace();
 * } else {
 *     // 连接成功建立
 * }
 * </pre>
 */
public interface ChannelFuture extends Future<Void> {

    /**
     * Returns a channel where the I/O operation associated with this
     * future takes place.
     */

    /**
     * 返回与此未来关联的I/O操作发生的通道。
     */
    Channel channel();

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture sync() throws InterruptedException;

    @Override
    ChannelFuture syncUninterruptibly();

    @Override
    ChannelFuture await() throws InterruptedException;

    @Override
    ChannelFuture awaitUninterruptibly();

    /**
     * Returns {@code true} if this {@link ChannelFuture} is a void future and so not allow to call any of the
     * following methods:
     * <ul>
     *     <li>{@link #addListener(GenericFutureListener)}</li>
     *     <li>{@link #addListeners(GenericFutureListener[])}</li>
     *     <li>{@link #await()}</li>
     *     <li>{@link #await(long, TimeUnit)} ()}</li>
     *     <li>{@link #await(long)} ()}</li>
     *     <li>{@link #awaitUninterruptibly()}</li>
     *     <li>{@link #sync()}</li>
     *     <li>{@link #syncUninterruptibly()}</li>
     * </ul>
     */

    /**
     * 如果此 {@link ChannelFuture} 是一个 void future，则返回 {@code true}，因此不允许调用以下任何方法：
     * <ul>
     *     <li>{@link #addListener(GenericFutureListener)}</li>
     *     <li>{@link #addListeners(GenericFutureListener[])}</li>
     *     <li>{@link #await()}</li>
     *     <li>{@link #await(long, TimeUnit)} ()}</li>
     *     <li>{@link #await(long)} ()}</li>
     *     <li>{@link #awaitUninterruptibly()}</li>
     *     <li>{@link #sync()}</li>
     *     <li>{@link #syncUninterruptibly()}</li>
     * </ul>
     */
    boolean isVoid();
}
