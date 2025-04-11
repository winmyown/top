
package org.top.java.netty.source.util;

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Schedules {@link TimerTask}s for one-time future execution in a background
 * thread.
 */

/**
 * 在后台线程中调度{@link TimerTask}以进行一次性未来执行。
 */
public interface Timer {

    /**
     * Schedules the specified {@link TimerTask} for one-time execution after
     * the specified delay.
     *
     * @return a handle which is associated with the specified task
     *
     * @throws IllegalStateException       if this timer has been {@linkplain #stop() stopped} already
     * @throws RejectedExecutionException if the pending timeouts are too many and creating new timeout
     *                                    can cause instability in the system.
     */

    /**
     * 安排指定的 {@link TimerTask} 在指定的延迟后一次性执行。
     *
     * @return 与指定任务关联的句柄
     *
     * @throws IllegalStateException       如果此计时器已被 {@linkplain #stop() 停止}
     * @throws RejectedExecutionException 如果挂起的超时过多，创建新的超时可能导致系统不稳定。
     */
    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

    /**
     * Releases all resources acquired by this {@link Timer} and cancels all
     * tasks which were scheduled but not executed yet.
     *
     * @return the handles associated with the tasks which were canceled by
     *         this method
     */

    /**
     * 释放此{@link Timer}获取的所有资源，并取消所有已调度但尚未执行的任务。
     *
     * @return 由此方法取消的任务关联的句柄
     */
    Set<Timeout> stop();
}
