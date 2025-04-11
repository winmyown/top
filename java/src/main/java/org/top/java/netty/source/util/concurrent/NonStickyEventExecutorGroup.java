
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.PlatformDependent;
import org.top.java.netty.source.util.internal.UnstableApi;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link EventExecutorGroup} which will preserve {@link Runnable} execution order but makes no guarantees about what
 * {@link EventExecutor} (and therefore {@link Thread}) will be used to execute the {@link Runnable}s.
 *
 * <p>The {@link EventExecutorGroup#next()} for the wrapped {@link EventExecutorGroup} must <strong>NOT</strong> return
 * executors of type {@link OrderedEventExecutor}.
 */

/**
 * {@link EventExecutorGroup}，它将保留 {@link Runnable} 的执行顺序，但不保证使用哪个 {@link EventExecutor}（以及因此的 {@link Thread}）来执行 {@link Runnable}。
 *
 * <p>被包装的 {@link EventExecutorGroup} 的 {@link EventExecutorGroup#next()} 方法必须<strong>不能</strong>返回类型为 {@link OrderedEventExecutor} 的执行器。
 */
@UnstableApi
public final class NonStickyEventExecutorGroup implements EventExecutorGroup {
    private final EventExecutorGroup group;
    private final int maxTaskExecutePerRun;

    /**
     * Creates a new instance. Be aware that the given {@link EventExecutorGroup} <strong>MUST NOT</strong> contain
     * any {@link OrderedEventExecutor}s.
     */

    /**
     * 创建新实例。请注意，给定的 {@link EventExecutorGroup} <strong>不能</strong> 包含
     * 任何 {@link OrderedEventExecutor}。
     */
    public NonStickyEventExecutorGroup(EventExecutorGroup group) {
        this(group, 1024);
    }

    /**
     * Creates a new instance. Be aware that the given {@link EventExecutorGroup} <strong>MUST NOT</strong> contain
     * any {@link OrderedEventExecutor}s.
     */

    /**
     * 创建新实例。请注意，给定的 {@link EventExecutorGroup} <strong>不能</strong> 包含
     * 任何 {@link OrderedEventExecutor}。
     */
    public NonStickyEventExecutorGroup(EventExecutorGroup group, int maxTaskExecutePerRun) {
        this.group = verify(group);
        this.maxTaskExecutePerRun = ObjectUtil.checkPositive(maxTaskExecutePerRun, "maxTaskExecutePerRun");
    }

    private static EventExecutorGroup verify(EventExecutorGroup group) {
        Iterator<EventExecutor> executors = ObjectUtil.checkNotNull(group, "group").iterator();
        while (executors.hasNext()) {
            EventExecutor executor = executors.next();
            if (executor instanceof OrderedEventExecutor) {
                throw new IllegalArgumentException("EventExecutorGroup " + group
                        + " contains OrderedEventExecutors: " + executor);
            }
        }
        return group;
    }

    private NonStickyOrderedEventExecutor newExecutor(EventExecutor executor) {
        return new NonStickyOrderedEventExecutor(executor, maxTaskExecutePerRun);
    }

    @Override
    public boolean isShuttingDown() {
        return group.isShuttingDown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return group.shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return group.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    public Future<?> terminationFuture() {
        return group.terminationFuture();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void shutdown() {
        group.shutdown();
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<Runnable> shutdownNow() {
        return group.shutdownNow();
    }

    @Override
    public EventExecutor next() {
        return newExecutor(group.next());
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        final Iterator<EventExecutor> itr = group.iterator();
        return new Iterator<EventExecutor>() {
            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public EventExecutor next() {
                return newExecutor(itr.next());
            }

            @Override
            public void remove() {
                itr.remove();
            }
        };
    }

    @Override
    public Future<?> submit(Runnable task) {
        return group.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return group.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return group.submit(task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return group.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return group.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return group.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return group.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShutdown() {
        return group.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return group.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return group.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return group.invokeAll(tasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return group.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return group.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return group.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        group.execute(command);
    }

    private static final class NonStickyOrderedEventExecutor extends AbstractEventExecutor
            implements Runnable, OrderedEventExecutor {
        private final EventExecutor executor;
        private final Queue<Runnable> tasks = PlatformDependent.newMpscQueue();

        private static final int NONE = 0;
        private static final int SUBMITTED = 1;
        private static final int RUNNING = 2;

        private final AtomicInteger state = new AtomicInteger();
        private final int maxTaskExecutePerRun;

        NonStickyOrderedEventExecutor(EventExecutor executor, int maxTaskExecutePerRun) {
            super(executor);
            this.executor = executor;
            this.maxTaskExecutePerRun = maxTaskExecutePerRun;
        }

        @Override
        public void run() {
            if (!state.compareAndSet(SUBMITTED, RUNNING)) {
                return;
            }
            for (;;) {
                int i = 0;
                try {
                    for (; i < maxTaskExecutePerRun; i++) {
                        Runnable task = tasks.poll();
                        if (task == null) {
                            break;
                        }
                        safeExecute(task);
                    }
                } finally {
                    if (i == maxTaskExecutePerRun) {
                        try {
                            state.set(SUBMITTED);
                            executor.execute(this);
                            return; // done
                        } catch (Throwable ignore) {
                            // Reset the state back to running as we will keep on executing tasks.
                            // 将状态重置回运行中，因为我们将继续执行任务。
                            state.set(RUNNING);
                            // if an error happened we should just ignore it and let the loop run again as there is not
                            // 如果发生错误，我们应该忽略它并让循环再次运行，因为没有
                            // much else we can do. Most likely this was triggered by a full task queue. In this case
                            // 我们还能做很多事情。很可能是由于任务队列已满触发了这种情况。
                            // we just will run more tasks and try again later.
                            // 我们稍后会运行更多任务并再次尝试。
                        }
                    } else {
                        state.set(NONE);
                        // After setting the state to NONE, look at the tasks queue one more time.
                        // 将状态设置为 NONE 后，再次查看任务队列。
                        // If it is empty, then we can return from this method.
                        // 如果为空，则可以从该方法返回。
                        // Otherwise, it means the producer thread has called execute(Runnable)
                        // 否则，意味着生产者线程调用了 execute(Runnable)
                        // and enqueued a task in between the tasks.poll() above and the state.set(NONE) here.
                        // 并且在上述的 tasks.poll() 和此处的 state.set(NONE) 之间入队了一个任务。
                        // There are two possible scenarios when this happen
                        // 当这种情况发生时，有两种可能的场景
                        //
                        // 1. The producer thread sees state == NONE, hence the compareAndSet(NONE, SUBMITTED)
                        // 1. 生产者线程看到 state == NONE，因此执行 compareAndSet(NONE, SUBMITTED)
                        //    is successfully setting the state to SUBMITTED. This mean the producer
                        //    成功将状态设置为SUBMITTED。这意味着生产者
                        //    will call / has called executor.execute(this). In this case, we can just return.
                        //    将会调用 / 已经调用了 executor.execute(this)。在这种情况下，我们可以直接返回。
                        // 2. The producer thread don't see the state change, hence the compareAndSet(NONE, SUBMITTED)
                        // 2. 生产者线程没有看到状态变化，因此 compareAndSet(NONE, SUBMITTED)
                        //    returns false. In this case, the producer thread won't call executor.execute.
                        //    返回 false。在这种情况下，生产者线程不会调用 executor.execute。
                        //    In this case, we need to change the state to RUNNING and keeps running.
                        //    在这种情况下，我们需要将状态更改为RUNNING并保持运行。
                        //
                        // The above cases can be distinguished by performing a
                        // 可以通过执行以下操作来区分上述情况
                        // compareAndSet(NONE, RUNNING). If it returns "false", it is case 1; otherwise it is case 2.
                        // compareAndSet(NONE, RUNNING)。如果返回“false”，则为情况1；否则为情况2。
                        if (tasks.isEmpty() || !state.compareAndSet(NONE, RUNNING)) {
                            return; // done
                        }
                    }
                }
            }
        }

        @Override
        public boolean inEventLoop(Thread thread) {
            return false;
        }

        @Override
        public boolean inEventLoop() {
            return false;
        }

        @Override
        public boolean isShuttingDown() {
            return executor.isShutdown();
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            return executor.shutdownGracefully(quietPeriod, timeout, unit);
        }

        @Override
        public Future<?> terminationFuture() {
            return executor.terminationFuture();
        }

        @Override
        public void shutdown() {
            executor.shutdown();
        }

        @Override
        public boolean isShutdown() {
            return executor.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return executor.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            if (!tasks.offer(command)) {
                throw new RejectedExecutionException();
            }
            if (state.compareAndSet(NONE, SUBMITTED)) {
                // Actually it could happen that the runnable was picked up in between but we not care to much and just
                // 实际上，有可能在中间阶段Runnable被拾取，但我们并不太在意，只是
                // execute ourself. At worst this will be a NOOP when run() is called.
                // 执行自身。在最坏的情况下，当调用 run() 时，这将是一个空操作。
                executor.execute(this);
            }
        }
    }
}
