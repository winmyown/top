package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:40
 */

import java.util.concurrent.Delayed;

/**
 * 一个可取消的带有延迟结果的动作。
 * 通常，计划的未来任务是通过 {@link ScheduledExecutorService} 调度任务的结果。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> 由此 Future 返回的结果类型
 */
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}

