package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:01
 */

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 一个服务，能够将新异步任务的生产与已完成任务结果的消费解耦开来。
 * 生产者使用 {@code submit} 提交任务进行执行。消费者通过 {@code take} 获取已完成的任务并按其完成的顺序处理结果。
 * 例如，{@code CompletionService} 可以用于管理异步 I/O，其中执行读取操作的任务在程序或系统的一部分被提交，
 * 然后在读取完成后在程序的另一部分进行处理，这些任务可能与请求的顺序不同。
 *
 * <p>通常，{@code CompletionService} 依赖于一个独立的 {@link Executor} 实际执行任务，
 * 而 {@code CompletionService} 仅管理一个内部的完成队列。{@link ExecutorCompletionService} 类提供了这种方法的实现。
 *
 * <p>内存一致性效应：在线程中，提交任务到 {@code CompletionService} 之前的操作
 * <a href="package-summary.html#MemoryVisibility"><i>先于</i></a>该任务执行的操作，
 * 而这些操作又<i>先于</i>从相应的 {@code take()} 成功返回后的操作。
 */
public interface CompletionService<V> {
    /**
     * 提交一个返回值的任务以执行，并返回表示该任务的待定结果的 Future。
     * 任务完成后，可以通过 take 或 poll 方法获取该任务。
     *
     * @param task 要提交的任务
     * @return 表示任务待定完成的 Future
     * @throws RejectedExecutionException 如果任务无法被安排执行
     * @throws NullPointerException 如果任务为空
     */
    Future<V> submit(Callable<V> task);

    /**
     * 提交一个 Runnable 任务以执行，并返回表示该任务的 Future。
     * 任务完成后，可以通过 take 或 poll 方法获取该任务。
     *
     * @param task 要提交的任务
     * @param result 成功完成时要返回的结果
     * @return 表示任务待定完成的 Future，其 {@code get()} 方法将在任务完成时返回给定的结果值
     * @throws RejectedExecutionException 如果任务无法被安排执行
     * @throws NullPointerException 如果任务为空
     */
    Future<V> submit(Runnable task, V result);

    /**
     * 检索并移除表示下一个已完成任务的 Future，如果当前没有任务完成则会阻塞等待。
     *
     * @return 表示下一个已完成任务的 Future
     * @throws InterruptedException 如果在等待时被中断
     */
    Future<V> take() throws InterruptedException;

    /**
     * 检索并移除表示下一个已完成任务的 Future，如果当前没有任务完成则返回 {@code null}。
     *
     * @return 表示下一个已完成任务的 Future，如果没有任务完成则返回 {@code null}
     */
    Future<V> poll();

    /**
     * 检索并移除表示下一个已完成任务的 Future，如果当前没有任务完成则会阻塞等待指定的时间。
     * 如果在指定的等待时间内没有任务完成，则返回 {@code null}。
     *
     * @param timeout 在放弃之前等待的时间
     * @param unit 确定如何解释 {@code timeout} 参数的 {@code TimeUnit}
     * @return 表示下一个已完成任务的 Future，如果在指定的等待时间内没有任务完成则返回 {@code null}
     * @throws InterruptedException 如果在等待时被中断
     */
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}

