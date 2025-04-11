
package org.top.java.netty.source.util;

import java.util.concurrent.TimeUnit;

/**
 * A task which is executed after the delay specified with
 * {@link Timer#newTimeout(TimerTask, long, TimeUnit)}.
 */

/**
 * 一个任务，在通过 {@link Timer#newTimeout(TimerTask, long, TimeUnit)} 指定的延迟后执行。
 */
public interface TimerTask {

    /**
     * Executed after the delay specified with
     * {@link Timer#newTimeout(TimerTask, long, TimeUnit)}.
     *
     * @param timeout a handle which is associated with this task
     */

    /**
     * 在通过 {@link Timer#newTimeout(TimerTask, long, TimeUnit)} 指定的延迟后执行。
     *
     * @param timeout 与此任务关联的句柄
     */
    void run(Timeout timeout) throws Exception;
}
