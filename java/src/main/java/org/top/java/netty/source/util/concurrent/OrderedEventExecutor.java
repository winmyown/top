
package org.top.java.netty.source.util.concurrent;

/**
 * Marker interface for {@link EventExecutor}s that will process all submitted tasks in an ordered / serial fashion.
 */

/**
 * 用于标记将按顺序/串行方式处理所有提交任务的 {@link EventExecutor} 的接口。
 */
public interface OrderedEventExecutor extends EventExecutor {
}
