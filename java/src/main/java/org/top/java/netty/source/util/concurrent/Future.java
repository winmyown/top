
package org.top.java.netty.source.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous operation.
 */


/**
 * 异步操作的结果。
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     */

    /**
     * 当且仅当 I/O 操作成功完成时返回 {@code true}。
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     */

    /**
     * 当且仅当操作可以通过 {@link #cancel(boolean)} 取消时返回 {@code true}。
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     *
     * @return the cause of the failure.
     *         {@code null} if succeeded or this future is not
     *         completed yet.
     */

    /**
     * 如果 I/O 操作失败，则返回失败的原因。
     *
     * @return 失败的原因。
     *         {@code null} 如果成功或此 future 尚未完成。
     */
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     */

    /**
     * 将指定的监听器添加到该 Future 中。当该 Future
     * {@linkplain #isDone() 完成} 时，指定的监听器会被通知。如果该 Future
     * 已经完成，指定的监听器会立即被通知。
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     */

    /**
     * 将指定的监听器添加到此 future。当此 future
     * {@linkplain #isDone() 完成}时，指定的监听器将被通知。如果此 future 已经
     * 完成，指定的监听器将立即被通知。
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     */

    /**
     * 从此 future 中移除指定监听器的第一次出现。
     * 当此 future {@linkplain #isDone() 完成} 时，指定的监听器不再被通知。
     * 如果指定的监听器与此 future 无关，则此方法不执行任何操作并静默返回。
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     */

    /**
     * 从此 future 中移除每个监听器的第一次出现。
     * 当此 future {@linkplain #isDone() 完成} 时，指定的监听器将不再被通知。
     * 如果指定的监听器与此 future 无关，则此方法不执行任何操作并静默返回。
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */

    /**
     * 等待此 future 完成，如果此 future 失败，则重新抛出失败的原因。
     */
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */

    /**
     * 等待此 future 完成，如果此 future 失败，则重新抛出失败的原因。
     */
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */

    /**
     * 等待此 future 完成。
     *
     * @throws InterruptedException
     *         如果当前线程被中断
     */
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     */

    /**
     * 等待此 future 完成，且不会中断。
     * 此方法捕获 {@link InterruptedException} 并静默丢弃它。
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */

    /**
     * 等待此 future 在指定的时间限制内完成。
     *
     * @return {@code true} 当且仅当 future 在指定的时间限制内完成
     *
     * @throws InterruptedException
     *         如果当前线程被中断
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */

    /**
     * 等待此 future 在指定的时间限制内完成。
     *
     * @return {@code true} 当且仅当 future 在指定的时间限制内完成
     *
     * @throws InterruptedException
     *         如果当前线程被中断
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */

    /**
     * 等待此 future 在指定的时间限制内完成，且不会被中断。此方法会捕获
     * {@link InterruptedException} 并静默丢弃它。
     *
     * @return {@code true} 当且仅当 future 在指定的时间限制内完成
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */

    /**
     * 等待此 future 在指定的时间限制内完成，且不会被中断。此方法会捕获
     * {@link InterruptedException} 并静默丢弃它。
     *
     * @return {@code true} 当且仅当 future 在指定的时间限制内完成
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     */

    /**
     * 立即返回结果而不阻塞。如果 future 尚未完成，则返回 {@code null}。
     *
     * 由于 {@code null} 值可能用于标记 future 成功完成，因此还需要使用 {@link #isDone()} 检查 future 是否真的完成，
     * 而不应依赖返回的 {@code null} 值。
     */
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with a {@link CancellationException}.
     */

    /**
     * {@inheritDoc}
     *
     * 如果取消成功，将会以 {@link CancellationException} 使 future 失败。
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
