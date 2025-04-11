
package org.top.java.netty.source.util;

/**
 * A handle associated with a {@link TimerTask} that is returned by a
 * {@link Timer}.
 */

/**
 * 与 {@link TimerTask} 关联的句柄，由 {@link Timer} 返回。
 */
public interface Timeout {

    /**
     * Returns the {@link Timer} that created this handle.
     */

    /**
     * 返回创建此句柄的 {@link Timer}。
     */
    Timer timer();

    /**
     * Returns the {@link TimerTask} which is associated with this handle.
     */

    /**
     * 返回与此句柄关联的 {@link TimerTask}。
     */
    TimerTask task();

    /**
     * Returns {@code true} if and only if the {@link TimerTask} associated
     * with this handle has been expired.
     */

    /**
     * 当且仅当与此句柄关联的 {@link TimerTask} 已过期时返回 {@code true}。
     */
    boolean isExpired();

    /**
     * Returns {@code true} if and only if the {@link TimerTask} associated
     * with this handle has been cancelled.
     */

    /**
     * 当且仅当与此句柄关联的 {@link TimerTask} 已被取消时，返回 {@code true}。
     */
    boolean isCancelled();

    /**
     * Attempts to cancel the {@link TimerTask} associated with this handle.
     * If the task has been executed or cancelled already, it will return with
     * no side effect.
     *
     * @return True if the cancellation completed successfully, otherwise false
     */

    /**
     * 尝试取消与此句柄关联的 {@link TimerTask}。
     * 如果任务已经执行或已被取消，则此操作将无副作用地返回。
     *
     * @return 如果取消成功完成则返回 true，否则返回 false
     */
    boolean cancel();
}
