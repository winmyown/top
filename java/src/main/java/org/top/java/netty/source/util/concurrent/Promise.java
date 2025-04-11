
package org.top.java.netty.source.util.concurrent;

/**
 * Special {@link Future} which is writable.
 */

/**
 * 可写的特殊 {@link Future}。
 */
public interface Promise<V> extends Future<V> {

    /**
     * Marks this future as a success and notifies all
     * listeners.
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     */

    /**
     * 将此 future 标记为成功并通知所有
     * 监听器。
     *
     * 如果它已经成功或失败，将抛出 {@link IllegalStateException}。
     */
    Promise<V> setSuccess(V result);

    /**
     * Marks this future as a success and notifies all
     * listeners.
     *
     * @return {@code true} if and only if successfully marked this future as
     *         a success. Otherwise {@code false} because this future is
     *         already marked as either a success or a failure.
     */

    /**
     * 将此future标记为成功并通知所有监听器。
     *
     * @return {@code true} 当且仅当成功将此future标记为成功。否则返回 {@code false}，
     *         因为此future已被标记为成功或失败。
     */
    boolean trySuccess(V result);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     *
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     */

    /**
     * 将此未来标记为失败并通知所有
     * 监听器。
     *
     * 如果它已经成功或失败，将抛出 {@link IllegalStateException}。
     */
    Promise<V> setFailure(Throwable cause);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     *
     * @return {@code true} if and only if successfully marked this future as
     *         a failure. Otherwise {@code false} because this future is
     *         already marked as either a success or a failure.
     */

    /**
     * 将此未来标记为失败并通知所有监听器。
     *
     * @return 如果成功将此未来标记为失败，则返回 {@code true}。
     *         否则返回 {@code false}，因为此未来已经被标记为成功或失败。
     */
    boolean tryFailure(Throwable cause);

    /**
     * Make this future impossible to cancel.
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done
     *         without being cancelled.  {@code false} if this future has been cancelled already.
     */

    /**
     * 使此 future 无法被取消。
     *
     * @return {@code true} 当且仅当成功将此 future 标记为不可取消或它已经完成且未被取消。
     *         {@code false} 如果此 future 已经被取消。
     */
    boolean setUncancellable();

    @Override
    Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
