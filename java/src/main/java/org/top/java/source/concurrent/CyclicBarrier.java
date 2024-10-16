package org.top.java.source.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>CyclicBarrier 是一种同步工具，它允许一组线程在共同的屏障点等待彼此，直到所有线程都到达该屏障点后才继续执行。CyclicBarrier 在需要固定数量的线程偶尔相互等待的程序中非常有用。它被称为“循环的”，因为在所有等待的线程被释放后，可以重新使用该屏障。</p>
 * <p>CyclicBarrier 支持一个可选的 <code>Runnable</code> 命令，该命令在每个屏障点执行一次，在所有线程到达屏障后、任何线程被释放之前执行。这个屏障操作在继续之前可以用于更新共享状态。</p>
 * <h3 id='使用示例'>使用示例</h3>
 * <p>以下是一个在并行分解设计中使用 CyclicBarrier 的示例：</p>
 * <pre><code class='language-java' lang='java'>class Solver {
 *     final int N;
 *     final float[][] data;
 *     final CyclicBarrier barrier;
 *
 *     class Worker implements Runnable {
 *         int myRow;
 *
 *         Worker(int row) {
 *             myRow = row;
 *         }
 *
 *         public void run() {
 *             while (!done()) {
 *                 processRow(myRow);
 *                 try {
 *                     barrier.await();
 *                 } catch (InterruptedException ex) {
 *                     return;
 *                 } catch (BrokenBarrierException ex) {
 *                     return;
 *                 }
 *             }
 *         }
 *     }
 *
 *     public Solver(float[][] matrix) {
 *         data = matrix;
 *         N = matrix.length;
 *         Runnable barrierAction = new Runnable() {
 *             public void run() {
 *                 mergeRows(...);
 *             }
 *         };
 *         barrier = new CyclicBarrier(N, barrierAction);
 *
 *         List&lt;Thread&gt; threads = new ArrayList&lt;Thread&gt;(N);
 *         for (int i = 0; i &lt; N; i++) {
 *             Thread thread = new Thread(new Worker(i));
 *             threads.add(thread);
 *             thread.start();
 *         }
 *
 *         // 等待所有线程完成
 *         for (Thread thread : threads) {
 *             thread.join();
 *         }
 *     }
 * }
 * </code></pre>
 * <p>在这个示例中，每个工作线程处理矩阵的一行，然后在屏障点等待，直到所有行都被处理。当所有行处理完毕时，提供的 <code>Runnable</code> 屏障动作会被执行，并合并这些行。如果合并结果确定已经找到解决方案，<code>done()</code> 方法将返回 <code>true</code>，每个工作线程随即终止。</p>
 * <p>如果屏障操作不依赖于各方在执行时被挂起，那么在释放屏障时，任何线程都可以执行该操作。为此，<code>await</code> 的每次调用都会返回该线程在屏障处的到达索引。您可以选择由哪个线程执行屏障操作，例如：</p>
 * <pre><code class='language-java' lang='java'>if (barrier.await() == 0) {
 *     // 记录该迭代的完成
 * }
 * </code></pre>
 * <h3 id='cyclicbarrier-的故障同步模型'>CyclicBarrier 的故障同步模型</h3>
 * <p>CyclicBarrier 使用一种全或无的故障模型来处理同步失败的情况：如果某个线程由于中断、失败或超时而提前离开屏障点，则所有在该屏障点等待的其他线程也会异常离开，并抛出 <code>BrokenBarrierException</code>（或者在同时中断时抛出 <code>InterruptedException</code>）。</p>
 * <h3 id='内存一致性效果'>内存一致性效果</h3>
 * <p>线程中在调用 <code>await()</code> 之前的操作在屏障操作的一部分动作之前执行，屏障操作的动作则在其他线程成功返回相应的 <code>await()</code> 后的操作之前执行。</p>
 * <h3 id='相关类'>相关类</h3>
 * <ul>
 * <li><code>CountDownLatch</code></li>
 *
 * </ul>
 * <h3 id='作者'>作者</h3>
 * <p>Doug Lea</p>
 */
public class CyclicBarrier {

    /**
     * 每次使用屏障时都会生成一个代（generation）实例。
     * 每当屏障被触发或重置时，代会改变。
     * 由于锁可能会以非确定的方式分配给等待线程，因此可能存在多个代与使用屏障的线程相关，
     * 但在任意时间内只有一个代是活跃的（即与 {@code count} 相关联的代），其余的代要么已损坏，要么已触发。
     * 如果发生中断但没有进行重置，则不需要活跃代。
     */
    private static class Generation {
        boolean broken = false;
    }

    /** 用于保护屏障进入的锁 */
    private final ReentrantLock lock = new ReentrantLock();
    /** 等待屏障被触发的条件变量 */
    private final Condition trip = lock.newCondition();
    /** 参与方的数量 */
    private final int parties;
    /** 屏障触发时要执行的命令 */
    private final Runnable barrierCommand;
    /** 当前的代 */
    private Generation generation = new Generation();

    /**
     * 仍然在等待的参与方数量。在每一代中，从 parties 减少到 0。
     * 在每次新的代开始或屏障损坏时重置为 parties。
     */
    private int count;

    /**
     * 当屏障触发时更新状态并唤醒所有线程。
     * 仅在持有锁时调用。
     */
    private void nextGeneration() {
        // 通知上一次代的完成
        trip.signalAll();
        // 设置下一代
        count = parties;
        generation = new Generation();
    }

    /**
     * 将当前屏障代标记为损坏并唤醒所有线程。
     * 仅在持有锁时调用。
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * 主屏障代码，覆盖了各种策略。
     */
    private int dowait(boolean timed, long nanos)
            throws InterruptedException, BrokenBarrierException,
            TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;

            if (g.broken)
                throw new BrokenBarrierException();

            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count;
            if (index == 0) {  // 屏障被触发
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 循环等待直到屏障被触发、损坏、中断或超时
            for (;;) {
                try {
                    if (!timed)
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // 我们即将结束等待，即使没有中断，这个中断被认为属于后续执行。
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 创建一个新的 {@code CyclicBarrier}，当给定数量的线程（参与方）等待时屏障被触发，并在屏障触发时执行给定的命令。
     * 该命令由最后一个进入屏障的线程执行。
     *
     * @param parties 调用 {@link #await} 以触发屏障的线程数量
     * @param barrierAction 屏障触发时执行的命令，若无操作则为 {@code null}
     * @throws IllegalArgumentException 如果 {@code parties} 小于 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    /**
     * 创建一个新的 {@code CyclicBarrier}，当给定数量的线程（参与方）等待时屏障被触发，
     * 且不执行任何预定义的操作。
     *
     * @param parties 调用 {@link #await} 以触发屏障的线程数量
     * @throws IllegalArgumentException 如果 {@code parties} 小于 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * 返回触发该屏障所需的线程数量。
     *
     * @return 触发屏障所需的线程数量
     */
    public int getParties() {
        return parties;
    }

    /**
     * 等待直到所有参与方调用 {@link #await}，然后触发屏障。
     *
     * <p>如果当前线程不是最后一个到达的，则它将禁用调度，并在以下情况之一发生之前保持休眠：
     * <ul>
     * <li>最后一个线程到达；或者
     * <li>其他某个线程 {@linkplain Thread#interrupt 中断} 当前线程；或者
     * <li>其他某个线程 {@linkplain Thread#interrupt 中断} 其他正在等待的线程；或者
     * <li>某个线程在等待屏障时超时；或者
     * <li>其他某个线程调用 {@link #reset} 重置屏障。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置中断状态；或者
     * <li>在等待时被 {@linkplain Thread#interrupt 中断}
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * <p>如果在任何线程等待期间屏障被重置，或者调用 {@code await} 时屏障已损坏，
     * 或者屏障动作（如果有）由于异常而失败，则抛出 {@link BrokenBarrierException}。
     *
     * <p>如果当前线程是最后一个到达的，并且构造函数提供了一个非空的屏障操作，
     * 则当前线程在允许其他线程继续之前执行该操作。如果在执行屏障操作期间发生异常，
     * 则该异常将在当前线程中传播，并且屏障处于损坏状态。
     *
     * @return 当前线程的到达索引，其中 {@code getParties() - 1} 表示第一个到达，0 表示最后一个到达
     * @throws InterruptedException 如果当前线程在等待时被中断
     * @throws BrokenBarrierException 如果另一个线程在当前线程等待时被中断或超时，或者屏障已重置，或者屏障已损坏
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // 不可能发生
        }
    }

    /**
     * 等待直到所有参与方调用 {@code await}，或者指定的等待时间已过。
     *
     * <p>如果当前线程不是最后一个到达的，则它会禁用调度并保持休眠，直到以下某个情况发生：
     * <ul>
     * <li>最后一个线程到达；或者
     * <li>指定的超时时间已过；或者
     * <li>其他某个线程 {@linkplain Thread#interrupt 中断} 当前线程；或者
     * <li>其他某个线程 {@linkplain Thread#interrupt 中断} 其他等待的线程；或者
     * <li>某个线程在等待时超时；或者
     * <li>其他某个线程调用 {@link #reset} 重置屏障。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时设置了中断状态；或者
     * <li>在等待时被 {@linkplain Thread#interrupt 中断}
     * </ul>
     * 则抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     *
     * <p>如果指定的等待时间已过，则抛出 {@link TimeoutException}。如果时间小于等于零，则方法不会等待。
     *
     * <p>如果在任何线程等待期间屏障被重置，或者调用 {@code await} 时屏障已损坏，
     * 或者屏障操作（如果有）由于异常而失败，则抛出 {@link BrokenBarrierException}。
     *
     * <p>如果当前线程是最后一个到达的，并且构造函数提供了一个非空的屏障操作，
     * 则当前线程在允许其他线程继续之前执行该操作。如果在执行屏障操作期间发生异常，
     * 则该异常将在当前线程中传播，并且屏障处于损坏状态。
     *
     * @param timeout 等待屏障的时间
     * @param unit timeout 参数的时间单位
     * @return 当前线程的到达索引，其中 {@code getParties() - 1} 表示第一个到达，0 表示最后一个到达
     * @throws InterruptedException 如果当前线程在等待时被中断
     * @throws TimeoutException 如果指定的等待时间已过。在这种情况下，屏障将被损坏。
     * @throws BrokenBarrierException 如果另一个线程在当前线程等待时被中断或超时，或者屏障已重置或损坏
     */
    public int await(long timeout, TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * 查询此屏障是否处于损坏状态。
     *
     * @return 如果一个或多个参与方自构造或最后一次重置后由于中断或超时破坏了此屏障，则返回 {@code true}；否则返回 {@code false}。
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将屏障重置为初始状态。如果有线程正在屏障处等待，它们将返回 {@link BrokenBarrierException}。
     * 注意，在其他原因导致的损坏后执行重置可能很复杂；线程需要通过某种方式重新同步，并选择一个线程来执行重置。
     * 更可取的做法是为后续使用创建一个新的屏障。
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // 破坏当前代
            nextGeneration(); // 启动新代
        } finally {
            lock.unlock();
        }
    }

    /**
     * 返回当前在屏障处等待的线程数量。
     * 此方法主要用于调试和断言。
     *
     * @return 当前在 {@link #await} 中阻塞的线程数量
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
