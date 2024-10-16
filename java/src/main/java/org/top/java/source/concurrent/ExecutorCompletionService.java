package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午2:55
 */

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 一个 {@link CompletionService}，它使用提供的 {@link Executor} 来执行任务。
 * 该类安排已提交的任务在完成时被放置到一个队列中，可以通过 {@code take} 方法访问该队列。
 * 该类足够轻量，适合在处理一组任务时临时使用。
 *
 * <p>
 *
 * <b>使用示例。</b>
 *
 * 假设你有一组求解某个问题的解算器，每个解算器返回某种类型 {@code Result} 的值，
 * 并且你希望并发地运行它们，处理每个返回非空值的结果，在某个方法 {@code use(Result r)} 中使用这些结果。
 * 你可以这样写：
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException, ExecutionException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     for (Callable<Result> s : solvers)
 *         ecs.submit(s);
 *     int n = solvers.size();
 *     for (int i = 0; i < n; ++i) {
 *         Result r = ecs.take().get();
 *         if (r != null)
 *             use(r);
 *     }
 * }}</pre>
 *
 * 假设你希望使用这组任务中第一个非空的结果，忽略遇到异常的任务，
 * 并在第一个任务准备好时取消所有其他任务：
 *
 * <pre> {@code
 * void solve(Executor e,
 *            Collection<Callable<Result>> solvers)
 *     throws InterruptedException {
 *     CompletionService<Result> ecs
 *         = new ExecutorCompletionService<Result>(e);
 *     int n = solvers.size();
 *     List<Future<Result>> futures
 *         = new ArrayList<Future<Result>>(n);
 *     Result result = null;
 *     try {
 *         for (Callable<Result> s : solvers)
 *             futures.add(ecs.submit(s));
 *         for (int i = 0; i < n; ++i) {
 *             try {
 *                 Result r = ecs.take().get();
 *                 if (r != null) {
 *                     result = r;
 *                     break;
 *                 }
 *             } catch (ExecutionException ignore) {}
 *         }
 *     }
 *     finally {
 *         for (Future<Result> f : futures)
 *             f.cancel(true);
 *     }
 *
 *     if (result != null)
 *         use(result);
 * }}</pre>
 */
public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final Executor executor;
    private final AbstractExecutorService aes;
    private final BlockingQueue<Future<V>> completionQueue;

    /**
     * FutureTask 扩展类，用于在完成时将任务加入队列
     */
    private class QueueingFuture extends FutureTask<Void> {
        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);
            this.task = task;
        }
        protected void done() { completionQueue.add(task); }
        private final Future<V> task;
    }

    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if (aes == null)
            return new FutureTask<V>(task);
        else
            return aes.newTaskFor(task);
    }

    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if (aes == null)
            return new FutureTask<V>(task, result);
        else
            return aes.newTaskFor(task, result);
    }

    /**
     * 使用提供的 executor 进行基础任务执行，并使用 {@link LinkedBlockingQueue} 作为完成队列，
     * 创建一个 ExecutorCompletionService。
     *
     * @param executor 要使用的执行器
     * @throws NullPointerException 如果 executor 为 {@code null}
     */
    public ExecutorCompletionService(Executor executor) {
        if (executor == null)
            throw new NullPointerException();
        this.executor = executor;
        this.aes = (executor instanceof AbstractExecutorService) ?
                (AbstractExecutorService) executor : null;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }

    /**
     * 使用提供的执行器进行基础任务执行，并使用提供的队列作为完成队列，
     * 创建一个 ExecutorCompletionService。
     *
     * @param executor 要使用的执行器
     * @param completionQueue 要使用的完成队列，通常专用于此服务的队列。
     *        该队列被视为无界队列——对于已完成任务的 {@code Queue.add} 操作失败将导致它们无法被检索到。
     * @throws NullPointerException 如果 executor 或 completionQueue 为 {@code null}
     */
    public ExecutorCompletionService(Executor executor,
                                     BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null)
            throw new NullPointerException();
        this.executor = executor;
        this.aes = (executor instanceof AbstractExecutorService) ?
                (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;
    }

    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task, result);
        executor.execute(new QueueingFuture(f));
        return f;
    }

    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    public Future<V> poll() {
        return completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }
}

