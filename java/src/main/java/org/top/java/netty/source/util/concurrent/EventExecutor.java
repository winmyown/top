
package org.top.java.netty.source.util.concurrent;

/**
 * The {@link EventExecutor} is a special {@link EventExecutorGroup} which comes
 * with some handy methods to see if a {@link Thread} is executed in a event loop.
 * Besides this, it also extends the {@link EventExecutorGroup} to allow for a generic
 * way to access methods.
 *
 */

/**
 * {@link EventExecutor} 是一个特殊的 {@link EventExecutorGroup}，它提供了一些方便的方法来检查某个 {@link Thread} 是否在事件循环中执行。
 * 除此之外，它还扩展了 {@link EventExecutorGroup}，以允许以通用的方式访问方法。
 *
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     */

    /**
     * 返回自身的引用。
     */
    @Override
    EventExecutor next();

    /**
     * Return the {@link EventExecutorGroup} which is the parent of this {@link EventExecutor},
     */

    /**
     * 返回作为此 {@link EventExecutor} 父级的 {@link EventExecutorGroup}。
     */
    EventExecutorGroup parent();

    /**
     * Calls {@link #inEventLoop(Thread)} with {@link Thread#currentThread()} as argument
     */

    /**
     * 使用 {@link Thread#currentThread()} 作为参数调用 {@link #inEventLoop(Thread)}
     */
    boolean inEventLoop();

    /**
     * Return {@code true} if the given {@link Thread} is executed in the event loop,
     * {@code false} otherwise.
     */

    /**
     * 如果给定的 {@link Thread} 在事件循环中执行，则返回 {@code true}，
     * 否则返回 {@code false}。
     */
    boolean inEventLoop(Thread thread);

    /**
     * Return a new {@link Promise}.
     */

    /**
     * 返回一个新的 {@link Promise}。
     */
    <V> Promise<V> newPromise();

    /**
     * Create a new {@link ProgressivePromise}.
     */

    /**
     * 创建一个新的 {@link ProgressivePromise}。
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * Create a new {@link Future} which is marked as succeeded already. So {@link Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */

    /**
     * 创建一个已经标记为成功的新 {@link Future}。因此 {@link Future#isSuccess()}
     * 将返回 {@code true}。所有添加到它的 {@link FutureListener} 将立即被通知。同时，
     * 所有阻塞方法的调用都将直接返回而不会阻塞。
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * Create a new {@link Future} which is marked as failed already. So {@link Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */

    /**
     * 创建一个新的 {@link Future}，该 Future 已被标记为失败。因此 {@link Future#isSuccess()}
     * 将返回 {@code false}。所有添加到它的 {@link FutureListener} 将立即被通知。此外，
     * 所有阻塞方法的调用都将直接返回而不会阻塞。
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
