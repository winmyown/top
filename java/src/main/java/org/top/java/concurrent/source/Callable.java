package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午6:46
 */

import java.util.concurrent.Executor;

/**
 * 一个任务，该任务返回一个结果，并可能抛出异常。
 * 实现者定义一个无参数的单一方法，名为 {@code call}。
 *
 * <p>{@code Callable} 接口类似于 {@link java.lang.Runnable}，因为它们都设计用于由其他线程执行其实例的类。
 * 但是，{@code Runnable} 不返回结果，且不能抛出受检异常。
 *
 * <p>{@link Executors} 类包含实用方法，可用于将其他常见形式转换为 {@code Callable} 类。
 *
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> 方法 {@code call} 的返回结果类型
 */
@FunctionalInterface
public interface Callable<V> {
    /**
     * 计算结果，或者如果无法计算，则抛出异常。
     *
     * @return 计算的结果
     * @throws Exception 如果无法计算结果
     */
    V call() throws Exception;
}

