package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午5:02
 */

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@code Future} 表示异步计算的结果。提供了一些方法用于检查计算是否完成、等待其完成以及获取计算结果。
 * 只有在计算完成时，才能使用 {@code get} 方法获取结果，如果需要，调用会阻塞直到结果准备好。取消任务是通过
 * {@code cancel} 方法执行的。还提供了一些其他方法来确定任务是否正常完成或被取消。一旦计算完成，就不能再取消计算。
 * 如果你希望使用 {@code Future} 只是为了提供取消功能而不关心结果，可以声明 {@code Future<?>} 类型，并且
 * 返回 {@code null} 作为底层任务的结果。
 *
 * <p>
 * <b>使用示例</b>（注意以下类都是虚构的）
 * <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // 在搜索时执行其他操作
 *     try {
 *       displayText(future.get()); // 使用 future 获取结果
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * {@link FutureTask} 类是 {@code Future} 的一个实现，它还实现了 {@code Runnable}，因此可以由 {@code Executor} 执行。
 * 例如，上述使用 {@code submit} 的代码可以替换为：
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * <p>内存一致性效果：异步计算采取的操作
 * <a href="package-summary.html#MemoryVisibility"> <i>先行发生</i></a>
 * 于另一个线程中相应的 {@code Future.get()} 之后的操作。
 *
 * @see FutureTask
 * @see
 * @since 1.5
 * @author Doug Lea
 * @param <V> 此 Future 的 {@code get} 方法返回的结果类型
 */
public interface Future<V> {

    /**
     * 尝试取消此任务的执行。如果任务已经完成、已经被取消或无法取消，则尝试失败。
     * 如果成功，并且在调用 {@code cancel} 时此任务尚未启动，则此任务将永远不会运行。
     * 如果任务已经启动，则 {@code mayInterruptIfRunning} 参数决定是否应该中断执行此任务的线程，
     * 以尝试停止任务。
     *
     * <p>此方法返回后，后续对 {@link #isDone} 的调用将始终返回 {@code true}。
     * 如果此方法返回 {@code true}，则后续对 {@link #isCancelled} 的调用将始终返回 {@code true}。
     *
     * @param mayInterruptIfRunning {@code true} 表示如果任务正在运行则应中断线程；
     * 否则，允许正在进行的任务完成
     * @return 如果任务无法取消，通常是因为它已经正常完成，则返回 {@code false}；
     * 否则返回 {@code true}
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * 如果此任务在正常完成之前已被取消，则返回 {@code true}。
     *
     * @return 如果此任务在正常完成之前已被取消，则返回 {@code true}
     */
    boolean isCancelled();

    /**
     * 如果此任务已完成，则返回 {@code true}。
     *
     * 完成可能是由于正常终止、异常或取消，在所有这些情况下，此方法将返回 {@code true}。
     *
     * @return 如果此任务已完成，则返回 {@code true}
     */
    boolean isDone();

    /**
     * 如有必要，等待计算完成，然后检索其结果。
     *
     * @return 计算结果
     * @throws CancellationException 如果计算已被取消
     * @throws ExecutionException 如果计算抛出了异常
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * 如有必要，等待最多给定时间让计算完成，然后（如果可用）获取其结果。
     *
     * @param timeout 最大等待时间
     * @param unit 超时参数的时间单位
     * @return 计算结果
     * @throws CancellationException 如果计算已被取消
     * @throws ExecutionException 如果计算抛出了异常
     * @throws InterruptedException 如果当前线程在等待时被中断
     * @throws TimeoutException 如果等待超时
     */
    V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}
