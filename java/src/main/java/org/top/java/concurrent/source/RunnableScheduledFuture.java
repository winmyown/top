package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:53
 */

import java.util.concurrent.Executor;

/**
 * 一个既是 {@link Runnable} 又是 {@link ScheduledFuture} 的接口。成功执行 {@code run} 方法将导致 {@code Future} 的完成，并允许访问其结果。
 * @see FutureTask
 * @see Executor
 * @since 1.6
 * @author Doug Lea
 * @param <V> 此 Future 的 {@code get} 方法返回的结果类型
 */
public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

    /**
     * 如果此任务是定期任务，则返回 {@code true}。定期任务可以根据某些计划重新运行。非定期任务只能运行一次。
     *
     * @return {@code true} 如果此任务是定期任务
     */
    boolean isPeriodic();
}

