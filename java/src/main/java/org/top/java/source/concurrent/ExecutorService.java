package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午6:22
 */

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 一个 {@link Executor}，提供管理终止的方法，并提供可以生成 {@link Future}
 * 的方法，用于跟踪一个或多个异步任务的进度。
 *
 * <p>一个 {@code ExecutorService} 可以被关闭，关闭后将拒绝接受新任务。提供了两种方法来关闭
 * 一个 {@code ExecutorService}。{@link #shutdown} 方法将允许先前提交的任务执行完再终止，
 * 而 {@link #shutdownNow} 方法则会阻止等待中的任务启动，并尝试停止当前正在执行的任务。
 * 终止后，执行器不再有任务在执行、没有任务等待执行，并且不能提交新任务。未使用的 {@code ExecutorService}
 * 应该被关闭，以释放其资源。
 *
 * <p>方法 {@code submit} 扩展了基础方法 {@link Executor#execute(Runnable)}，通过创建并返回一个
 * {@link Future} 来取消执行和/或等待完成。方法 {@code invokeAny} 和 {@code invokeAll}
 * 实现了批量执行的最常用形式，执行一个任务集合，然后等待至少一个任务或所有任务完成。
 * （类 {@link ExecutorCompletionService} 可以用来编写这些方法的自定义变体。）
 *
 * <p>类 {@link Executors} 提供了工厂方法，用于生成此包中提供的执行器服务。
 *
 * <h3>使用示例</h3>
 *
 * 下面是一个网络服务的草图，其中线程池中的线程为传入的请求提供服务。它使用了预先配置的
 * {@link Executors#newFixedThreadPool} 工厂方法：
 *
 *  <pre> {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // 运行服务
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // 在 socket 上读取和处理请求
 *   }
 * }}</pre>
 *
 * 以下方法分两个阶段关闭一个 {@code ExecutorService}，首先调用 {@code shutdown}
 * 以拒绝新的任务，然后必要时调用 {@code shutdownNow} 来取消任何剩余的任务：
 *
 *  <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // 禁止提交新任务
 *   try {
 *     // 等待一段时间让现有任务完成
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // 取消当前正在执行的任务
 *       // 再次等待一段时间，等待任务响应取消
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("线程池未能终止");
 *     }
 *   } catch (InterruptedException ie) {
 *     // 如果当前线程也被中断，则重新取消
 *     pool.shutdownNow();
 *     // 保留中断状态
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * <p>内存一致性效果：在线程中提交 {@code Runnable} 或 {@code Callable} 任务到 {@code ExecutorService}
 * 之前的操作
 * <a href="package-summary.html#MemoryVisibility"><i>先行发生</i></a>
 * 于该任务所采取的任何操作，而该任务的操作又 <i>先行发生</i> 于通过 {@code Future.get()} 获取结果。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * 启动有序的关闭操作，在此过程中，先前提交的任务会继续执行，但不会接受新任务。
     * 如果已关闭，则调用没有额外效果。
     *
     * <p>此方法不会等待先前提交的任务完成执行。可以使用 {@link #awaitTermination awaitTermination}
     * 来实现这一点。
     *
     * @throws SecurityException 如果存在安全管理器，并且关闭此 ExecutorService 可能会操作调用者无权修改的线程，
     *         因为它没有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}，
     *         或者安全管理器的 {@code checkAccess} 方法拒绝访问。
     */
    void shutdown();

    /**
     * 尝试停止所有正在执行的任务，停止处理等待中的任务，并返回那些等待执行的任务的列表。
     *
     * <p>此方法不会等待正在执行的任务终止。可以使用 {@link #awaitTermination awaitTermination}
     * 来实现这一点。
     *
     * <p>除了尽最大努力停止处理正在执行的任务外，不提供任何保证。例如，典型的实现将通过
     * {@link Thread#interrupt} 来取消，因此任何无法响应中断的任务可能永远不会终止。
     *
     * @return 未开始执行的任务列表
     * @throws SecurityException 如果存在安全管理器，并且关闭此 ExecutorService 可能会操作调用者无权修改的线程，
     *         因为它没有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}，
     *         或者安全管理器的 {@code checkAccess} 方法拒绝访问。
     */
    List<Runnable> shutdownNow();

    /**
     * 如果此执行器已关闭，则返回 {@code true}。
     *
     * @return 如果此执行器已关闭，则返回 {@code true}
     */
    boolean isShutdown();

    /**
     * 如果所有任务在关闭后都已完成，则返回 {@code true}。
     * 注意，除非首先调用了 {@code shutdown} 或 {@code shutdownNow}，否则 {@code isTerminated} 永远不会为 {@code true}。
     *
     * @return 如果所有任务在关闭后都已完成，则返回 {@code true}
     */
    boolean isTerminated();

    /**
     * 在收到关闭请求后，阻塞直到所有任务完成执行、超时发生或当前线程被中断，以先发生者为准。
     *
     * @param timeout 最长等待时间
     * @param unit 超时时间参数的时间单位
     * @return 如果此执行器终止，则返回 {@code true}；如果在终止之前超时，则返回 {@code false}
     * @throws InterruptedException 如果等待时被中断
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 提交一个返回值的任务以执行，并返回一个表示该任务挂起结果的 Future。
     * Future 的 {@code get} 方法将在任务成功完成后返回结果。
     *
     * <p>
     * 如果你想立即阻塞等待任务的完成，可以使用如下构造：
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>注意：类 {@link Executors} 包含一组方法，这些方法可以将一些其他常见的类似闭包的对象转换为
     * {@link Callable} 形式，以便它们可以被提交。
     *
     * @param task 要提交的任务
     * @param <T> 任务结果的类型
     * @return 一个表示任务挂起完成的 Future
     * @throws RejectedExecutionException 如果任务无法被调度执行
     * @throws NullPointerException 如果任务为 null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交一个 Runnable 任务以执行，并返回一个表示该任务的 Future。
     * Future 的 {@code get} 方法将在任务成功完成后返回给定的结果。
     *
     * @param task 要提交的任务
     * @param result 成功完成时返回的结果
     * @param <T> 结果的类型
     * @return 一个表示任务挂起完成的 Future
     * @throws RejectedExecutionException 如果任务无法被调度执行
     * @throws NullPointerException 如果任务为 null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 提交一个 Runnable 任务以执行，并返回一个表示该任务的 Future。
     * Future 的 {@code get} 方法将在任务成功完成后返回 {@code null}。
     *
     * @param task 要提交的任务
     * @return 一个表示任务挂起完成的 Future
     * @throws RejectedExecutionException 如果任务无法被调度执行
     * @throws NullPointerException 如果任务为 null
     */
    Future<?> submit(Runnable task);

    /**
     * 执行给定的任务，返回包含其状态和结果的 Future 列表，当所有任务完成时返回。
     * {@link Future#isDone} 对于返回列表中的每个元素都为 {@code true}。
     * 注意，<em>完成</em>的任务可能是正常终止的，也可能是由于抛出异常终止的。
     * 如果在此操作进行期间修改了给定的集合，则此方法的结果是不确定的。
     *
     * @param tasks 要执行的任务集合
     * @param <T> 任务返回值的类型
     * @return 一个表示任务的 Future 列表，按照给定任务列表的迭代器生成的顺序排列，
     *         每个任务都已完成
     * @throws InterruptedException 如果等待时被中断，在这种情况下未完成的任务将被取消
     * @throws NullPointerException 如果任务或其任何元素为 {@code null}
     * @throws RejectedExecutionException 如果任何任务无法调度执行
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException;

    /**
     * 执行给定的任务，返回包含其状态和结果的 Future 列表，当所有任务完成或超时发生时返回，
     * 以先发生者为准。{@link Future#isDone} 对于返回列表中的每个元素都为 {@code true}。
     * 返回时，尚未完成的任务将被取消。注意，<em>完成</em>的任务可能是正常终止的，
     * 也可能是由于抛出异常终止的。如果在此操作进行期间修改了给定的集合，则此方法的结果是不确定的。
     *
     * @param tasks 要执行的任务集合
     * @param timeout 最长等待时间
     * @param unit 超时时间参数的时间单位
     * @param <T> 任务返回值的类型
     * @return 一个表示任务的 Future 列表，按照给定任务列表的迭代器生成的顺序排列。
     *         如果操作未超时，则每个任务都将完成。如果超时发生，则有些任务可能未完成。
     * @throws InterruptedException 如果等待时被中断，在这种情况下未完成的任务将被取消
     * @throws NullPointerException 如果任务、其任何元素或单位为 {@code null}
     * @throws RejectedExecutionException 如果任何任务无法调度执行
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 执行给定的任务，返回其中一个成功完成的任务的结果（即未抛出异常的任务），
     * 如果有的话。在正常或异常返回时，未完成的任务将被取消。
     * 如果在此操作进行期间修改了给定的集合，则此方法的结果是不确定的。
     *
     * @param tasks 要执行的任务集合
     * @param <T> 任务返回值的类型
     * @return 其中一个任务返回的结果
     * @throws InterruptedException 如果等待时被中断
     * @throws NullPointerException 如果任务或其任何元素为 {@code null}
     * @throws IllegalArgumentException 如果任务集合为空
     * @throws ExecutionException 如果没有任务成功完成
     * @throws RejectedExecutionException 如果任务无法调度执行
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException;

    /**
     * 执行给定的任务，返回其中一个成功完成的任务的结果（即未抛出异常的任务），
     * 如果有的话，或者在给定的超时发生之前完成。在正常或异常返回时，未完成的任务将被取消。
     * 如果在此操作进行期间修改了给定的集合，则此方法的结果是不确定的。
     *
     * @param tasks 要执行的任务集合
     * @param timeout 最长等待时间
     * @param unit 超时时间参数的时间单位
     * @param <T> 任务返回值的类型
     * @return 其中一个任务返回的结果
     * @throws InterruptedException 如果等待时被中断
     * @throws NullPointerException 如果任务、其任何元素或单位为 {@code null}
     * @throws TimeoutException 如果在任何任务成功完成之前超时发生
     * @throws ExecutionException 如果没有任务成功完成
     * @throws RejectedExecutionException 如果任务无法调度执行
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}


