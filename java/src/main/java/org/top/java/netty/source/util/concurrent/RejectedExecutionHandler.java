
package org.top.java.netty.source.util.concurrent;

/**
 * Similar to {@link java.util.concurrent.RejectedExecutionHandler} but specific to {@link SingleThreadEventExecutor}.
 */

/**
 * 类似于 {@link java.util.concurrent.RejectedExecutionHandler}，但特定于 {@link SingleThreadEventExecutor}。
 */
public interface RejectedExecutionHandler {

    /**
     * Called when someone tried to add a task to {@link SingleThreadEventExecutor} but this failed due capacity
     * restrictions.
     */

    /**
     * 当有人尝试向 {@link SingleThreadEventExecutor} 添加任务但由于容量限制失败时调用。
     */
    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
