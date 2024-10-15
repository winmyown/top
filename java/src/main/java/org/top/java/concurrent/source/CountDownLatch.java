package org.top.java.concurrent.source;

import org.top.java.concurrent.source.locks.AbstractQueuedSynchronizer;

import java.util.concurrent.TimeUnit;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午1:05
 */

/**
 * <p>CountDownLatch 是一种同步辅助工具，允许一个或多个线程等待，直到其他线程中正在执行的一组操作完成。</p>
 * <p>CountDownLatch 通过给定的计数值初始化。<code>await</code> 方法会阻塞，直到当前计数通过调用 <code>countDown</code> 方法减少到零为止，此后所有等待的线程都会被释放，且任何后续的 <code>await</code> 调用都会立即返回。这是一次性的现象 —— 计数无法重置。如果需要重置计数的版本，建议使用 <code>CyclicBarrier</code>。</p>
 * <p>CountDownLatch 是一个多功能的同步工具，可以用于多种目的。一个计数为 1 的 CountDownLatch 可以用作简单的开关或门：所有调用 <code>await</code> 的线程都会在门口等待，直到某个线程调用 <code>countDown</code> 来打开它。一个初始化为 N 的 CountDownLatch 可以用来使一个线程等待，直到 N 个线程完成某些操作，或某个操作已经完成了 N 次。</p>
 * <p>CountDownLatch 的一个有用特性是，调用 <code>countDown</code> 的线程不需要等待计数减到零后才能继续，它只是阻止任何线程通过 <code>await</code> 继续执行，直到所有线程都可以通过。</p>
 * <p>示例用法：以下是两个类的示例，其中一组工作线程使用了两个倒计时锁存器（CountDownLatch）：</p>
 * <p>第一个是一个启动信号，它阻止任何工作线程在驱动线程准备好让它们继续之前执行；</p>
 * <p>第二个是一个完成信号，它允许驱动线程等待所有工作线程完成后再继续。</p>
 * <pre><code class='language-java' lang='java'>class Driver {
 *     // ...
 *     void main() throws InterruptedException {
 *         CountDownLatch startSignal = new CountDownLatch(1);
 *         CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *         for (int i = 0; i &lt; N; ++i) // 创建并启动线程
 *             new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *         doSomethingElse();            // 还不让线程运行
 *         startSignal.countDown();      // 让所有线程继续
 *         doSomethingElse();
 *         doneSignal.await();           // 等待所有线程完成
 *     }
 * }
 *
 * class Worker implements Runnable {
 *     private final CountDownLatch startSignal;
 *     private final CountDownLatch doneSignal;
 *
 *     Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *         this.startSignal = startSignal;
 *         this.doneSignal = doneSignal;
 *     }
 *
 *     public void run() {
 *         try {
 *             startSignal.await();
 *             doWork();
 *             doneSignal.countDown();
 *         } catch (InterruptedException ex) {} // 返回
 *     }
 *
 *     void doWork() { ... }
 * }
 * </code></pre>
 * <p>另一种典型的用法是将一个问题分成 N 个部分，用一个执行该部分的 <code>Runnable</code> 来描述每个部分，并在执行完该部分后对倒计时锁存器进行计数递减（countDown），然后将所有的 <code>Runnable</code> 提交到 <code>Executor</code> 中。当所有子部分完成后，协调线程将能够通过 <code>await</code> 继续执行。（当线程需要以这种方式反复递减计数时，应使用 <code>CyclicBarrier</code> 代替。）</p>
 * <pre><code class='language-java' lang='java'>class Driver2 {
 *     // ...
 *     void main() throws InterruptedException {
 *         CountDownLatch doneSignal = new CountDownLatch(N);
 *         Executor e = ...
 *
 *         for (int i = 0; i &lt; N; ++i) // 创建并启动线程
 *             e.execute(new WorkerRunnable(doneSignal, i));
 *
 *         doneSignal.await();           // 等待所有线程完成
 *     }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *     private final CountDownLatch doneSignal;
 *     private final int i;
 *
 *     WorkerRunnable(CountDownLatch doneSignal, int i) {
 *         this.doneSignal = doneSignal;
 *         this.i = i;
 *     }
 *
 *     public void run() {
 *         try {
 *             doWork(i);
 *             doneSignal.countDown();
 *         } catch (InterruptedException ex) {} // 返回
 *     }
 *
 *     void doWork() { ... }
 * }
 * </code></pre>
 * <p>内存一致性效应：直到计数减到零为止，调用 <code>countDown()</code> 之前的线程操作在另一个线程成功从相应的 <code>await()</code> 返回后发生的操作之前执行。</p>
 * <p>自版本 1.5 起提供。</p>
 * <p>作者：Doug Lea</p>
 */
public class CountDownLatch {
    /**
     * CountDownLatch 的同步控制。
     * 使用 AQS 状态来表示计数。
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // 减少计数；当过渡到零时发出信号
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * 构造一个用给定计数初始化的 {@code CountDownLatch}。
     *
     * @param count 在线程可以通过 {@link #await} 之前，必须调用 {@link #countDown} 的次数
     * @throws IllegalArgumentException 如果 {@code count} 为负数
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * 使当前线程等待，直到锁存器的计数减到零，除非线程被 {@linkplain Thread#interrupt 中断}。
     *
     * <p>如果当前计数为零，则此方法立即返回。
     *
     * <p>如果当前计数大于零，则当前线程会被禁用调度并进入休眠，直到发生以下两种情况之一：
     * <ul>
     * <li>由于调用 {@link #countDown} 方法，计数减到零；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 了当前线程。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在等待时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

/**
 * 使当前线程等待，直到锁存器的计数减到零，除非线程被 {@linkplain Thread#interrupt 中断}，
 * 或者指定的等待时间到期。
 *
 * <p>如果当前计数为零，则此方法立即返回 {@code true}。
 *
 * <p>如果当前计数大于零，则当前线程会被禁用调度并进入休眠，直到发生以下三种情况之一：
 * <ul>
 * <li>由于调用 {@link #countDown} 方法，计数减到零；或者
 * <li>其他线程 {@linkplain Thread#interrupt 中断}
 * <li>指定等待时间耗尽
 * </ul>
 *
 * <p>如果计数减到零，则此方法返回 {@code true}。
 *
 * <p>如果当前线程：
 * <ul>
 * <li>在进入此方法时已设置了中断状态；或者
 * <li>在等待时被 {@linkplain Thread#interrupt 中断}，
 *
 * </ul>
 * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
 *
 * <p>如果指定的等待时间到期，则返回值 {@code false}。如果时间小于或等于零，
 * 此方法不会等待。
 *
 * @param timeout 等待的最长时间
 * @param unit {@code timeout} 参数的时间单位
 * @return {@code true} 如果计数减到零，否则 {@code false}
 *         如果在计数减到零之前等待时间已到
 * @throws InterruptedException 如果当前线程在等待时被中断
 */
public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
}

    /**
     * 减少锁存器的计数，当计数达到零时释放所有等待线程。
     *
     * <p>如果当前计数大于零，则将其递减。
     * 如果新的计数为零，则重新启用所有等待线程的调度。
     *
     * <p>如果当前计数等于零，则什么也不会发生。
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * 返回当前计数。
     *
     * <p>此方法通常用于调试和测试目的。
     *
     * @return 当前计数
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * 返回标识此锁存器及其状态的字符串。
     * 状态包含在方括号中，并包含字符串 {@code "Count ="}，
     * 后跟当前计数。
     *
     * @return 标识此锁存器及其状态的字符串
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}

