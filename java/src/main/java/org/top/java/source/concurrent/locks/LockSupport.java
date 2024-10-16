package org.top.java.source.concurrent.locks;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午2:05
 */

/**
 * <p><code>LockSupport</code> 类提供了基本的线程阻塞原语，用于创建锁和其他同步类。</p>
 * <p>此类与每个使用它的线程关联一个许可（在 <code>Semaphore</code> 类的意义上）。调用 <code>park</code> 方法时，如果许可可用，它会立即返回，同时消耗该许可；否则，可能会阻塞。调用 <code>unpark</code> 方法会使许可可用（如果之前不可用）。（与信号量不同，许可不会累积，最多只能有一个。）</p>
 * <p><code>park</code> 和 <code>unpark</code> 方法提供了一种高效的线程阻塞和解除阻塞的手段，不会遇到已废弃的 <code>Thread.suspend</code> 和 <code>Thread.resume</code> 方法不可用的问题：通过许可，一个线程调用 <code>park</code> 和另一个线程调用 <code>unpark</code> 之间的竞争不会影响线程的活性。此外，<code>park</code> 方法会在调用线程被中断时返回，并支持超时版本。<code>park</code> 方法还可能在“无理由”的情况下返回，因此通常需要在循环中调用，并在返回时重新检查条件。从这个意义上说，<code>park</code> 是对“忙等”的一种优化，它不会浪费太多时间在自旋上，但必须与 <code>unpark</code> 配合使用才能生效。</p>
 * <p>三种形式的 <code>park</code> 方法都支持一个阻塞器对象参数。在线程被阻塞时，这个对象会被记录下来，允许监控和诊断工具识别阻塞线程的原因。（这些工具可以使用 <code>getBlocker(Thread)</code> 方法访问阻塞器。）建议使用带阻塞器参数的形式，而不是原始形式。在锁实现中，通常将 <code>this</code> 作为阻塞器参数传递。</p>
 * <p>这些方法是为创建高级同步工具而设计的，本身并不适用于大多数并发控制应用。<code>park</code> 方法仅设计用于类似以下形式的结构：</p>
 * <pre><code class='language-java' lang='java'>while (!canProceed()) {
 *     // ...
 *     LockSupport.park(this);
 * }
 * </code></pre>
 * <p>其中 <code>canProceed</code> 或在调用 <code>park</code> 之前的其他操作都不涉及锁定或阻塞。由于每个线程仅关联一个许可，因此任何中间的 <code>park</code> 使用可能会干扰其预期效果。</p>
 * <h3 id='示例用法'>示例用法</h3>
 * <p>下面是一个先进先出（FIFO）非重入锁类的示例：</p>
 * <pre><code class='language-java' lang='java'>class FIFOMutex {
 *     private final AtomicBoolean locked = new AtomicBoolean(false);
 *     private final Queue&lt;Thread&gt; waiters = new ConcurrentLinkedQueue&lt;&gt;();
 *
 *     public void lock() {
 *         boolean wasInterrupted = false;
 *         Thread current = Thread.currentThread();
 *         waiters.add(current);
 *
 *         // 阻塞，直到队列中的第一个线程或无法获取锁
 *         while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
 *             LockSupport.park(this);
 *             if (Thread.interrupted()) { // 忽略等待时的中断
 *                 wasInterrupted = true;
 *             }
 *         }
 *
 *         waiters.remove();
 *         if (wasInterrupted) {
 *             current.interrupt(); // 在退出时重新声明中断状态
 *         }
 *     }
 *
 *     public void unlock() {
 *         locked.set(false);
 *         LockSupport.unpark(waiters.peek());
 *     }
 * }
 * </code></pre>
 * <p>以上代码定义了一个 <code>FIFOMutex</code> 类，它是一个基于 FIFO（先进先出）策略的非重入锁。通过 <code>AtomicBoolean</code> 来控制锁的状态，并使用 <code>ConcurrentLinkedQueue</code> 来维护一个线程等待队列。<code>lock</code> 方法将当前线程添加到等待队列中，并通过 <code>LockSupport.park(this)</code> 使线程阻塞，直到满足条件才能继续。<code>unlock</code> 方法则通过调用 <code>LockSupport.unpark</code> 来唤醒等待的线程。</p>
 */
public class LockSupport {
    private LockSupport() {} // 无法实例化。

    private static void setBlocker(Thread t, Object arg) {
        // 即使是 volatile 变量，hotspot 在这里不需要写屏障。
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * 为指定的线程提供许可证（如果尚未提供）。如果线程被阻塞在
     * {@code park} 上，那么它将被解除阻塞。否则，它的下次对
     * {@code park} 的调用将保证不被阻塞。如果指定的线程尚未启动，
     * 则此操作不保证会产生任何效果。
     *
     * @param thread 要解锁的线程，或者 {@code null}，在这种情况下
     *        此操作没有效果
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * 禁用当前线程的线程调度，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到发生以下三种情况之一：
     *
     * <ul>
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断}
     * 当前线程；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态。
     *
     * @param blocker 导致此线程停车的同步对象
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * 禁用当前线程的线程调度，直到指定的时间，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到以下四种情况之一发生：
     *
     * <ul>
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断}
     * 当前线程；或
     *
     * <li>指定的等待时间结束；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态或经过的时间。
     *
     * @param blocker 导致此线程停车的同步对象
     * @param nanos 最长等待的纳秒数
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * 禁用当前线程的线程调度，直到指定的截止时间，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到以下四种情况之一发生：
     *
     * <ul>
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或
     *
     * <li>指定的截止时间到期；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态或当前时间。
     *
     * @param blocker 导致此线程停车的同步对象
     * @param deadline 从纪元（Epoch）开始的毫秒数，表示等待的截止时间
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * 返回提供给最近一次 park 方法调用的 blocker 对象，该调用尚未解除阻塞，
     * 如果未被阻塞则返回 null。返回的值只是一个瞬时快照——
     * 线程可能已经解除阻塞或阻塞在另一个 blocker 对象上。
     *
     * @param t 线程
     * @return blocker 对象
     * @throws NullPointerException 如果参数为 null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * 禁用当前线程的线程调度，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到发生以下三种情况之一：
     *
     * <ul>
     *
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断}
     * 当前线程；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态。
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * 禁用当前线程的线程调度，直到指定的等待时间，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到以下四种情况之一发生：
     *
     * <ul>
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断}
     * 当前线程；或
     *
     * <li>指定的等待时间结束；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态或经过的时间。
     *
     * @param nanos 最长等待的纳秒数
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * 禁用当前线程的线程调度，直到指定的截止时间，除非许可证可用。
     *
     * <p>如果许可证可用，则它将被消耗，调用会立即返回；
     * 否则当前线程将禁用线程调度并保持休眠状态，直到以下四种情况之一发生：
     *
     * <ul>
     * <li>其他线程调用 {@link #unpark unpark} 并指定当前线程为目标；或
     *
     * <li>其他线程 {@linkplain Thread#interrupt 中断}
     * 当前线程；或
     *
     * <li>指定的截止时间到期；或
     *
     * <li>调用无缘无故地（即无理由地）返回。
     * </ul>
     *
     * <p>此方法不会报告导致方法返回的原因。调用者应重新检查导致
     * 线程停车的条件。调用者还可以确定，例如，返回时线程的中断状态或当前时间。
     *
     * @param deadline 从纪元（Epoch）开始的毫秒数，表示等待的截止时间
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * 返回伪随机初始化或更新的次级种子。
     * 从 ThreadLocalRandom 复制，因为包访问受限。
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift 算法
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // 避免零
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot 通过内在 API 实现
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}

