package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午2:52
 */

import java.util.concurrent.Executor;

/**
 * 一个 {@link Future}，同时也是一个 {@link Runnable}。成功执行 {@code run} 方法将导致该 {@code Future} 的完成，
 * 并允许访问其结果。
 * @see FutureTask
 * @see Executor
 * @since 1.6
 * @author Doug Lea
 * @param <V> 此 Future 的 {@code get} 方法返回的结果类型
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * 将此 Future 设置为其计算结果，除非它已被取消。
     */
    void run();
}

