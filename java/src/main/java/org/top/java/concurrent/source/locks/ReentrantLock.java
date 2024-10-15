package org.top.java.concurrent.source.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午1:31
 */

/**
 * <p>ReentrantLock 是一种可重入的互斥锁，具有与通过 <code>synchronized</code> 方法和语句访问的隐式监视器锁相同的基本行为和语义，但它具有扩展的功能。</p>
 * <p>ReentrantLock 由最后成功加锁但尚未解锁的线程持有。当锁未被其他线程持有时，调用 <code>lock</code> 的线程将成功获取锁并返回。如果当前线程已经持有锁，该方法将立即返回。这可以通过方法 <code>isHeldByCurrentThread</code> 和 <code>getHoldCount</code> 来检查。</p>
 * <p>该类的构造函数接受一个可选的公平性参数。当设置为 <code>true</code> 时，在发生竞争时，锁将优先授予等待时间最长的线程访问权限。否则，此锁不保证任何特定的访问顺序。由多个线程访问的公平锁的程序可能显示出较低的总体吞吐量（即速度较慢；通常慢得多），但它们在获取锁的时间上波动较小，并且保证不会发生线程饥饿。然而请注意，锁的公平性并不保证线程调度的公平性。因此，使用公平锁的多个线程中的某一个可能会连续多次获取锁，而其他活动线程并未进展且当前未持有锁。还要注意，未定时的 <code>tryLock()</code> 方法不遵循公平性设置。如果锁可用，它将成功获取锁，即使其他线程正在等待。</p>
 * <p>推荐的做法是在调用 <code>lock</code> 后立即使用 <code>try</code> 代码块，通常在类似于以下的前/后结构中：</p>
 * <pre><code class='language-java' lang='java'>class X {
 *     private final ReentrantLock lock = new ReentrantLock();
 *     // ...
 *     public void m() {
 *         lock.lock();  // 阻塞，直到条件满足
 *         try {
 *             // ... 方法主体
 *         } finally {
 *             lock.unlock();
 *         }
 *     }
 * }
 * </code></pre>
 * <p>除了实现 <code>Lock</code> 接口外，此类还定义了许多用于检查锁状态的公共和受保护方法。这些方法中的一些仅用于检测和监控。</p>
 * <p>该类的序列化行为与内置锁的行为相同：反序列化后的锁处于未加锁状态，无论它在序列化时的状态如何。</p>
 * <p>此锁支持同一线程递归锁定最多 2147483647 次。尝试超过此限制将导致锁定方法抛出 <code>Error</code>。</p>
 * <p>自版本 1.5 起提供。</p>
 * <p>作者：Doug Lea</p>
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** 提供所有实现机制的同步器 */
    private final Sync sync;

    /**
     * 此锁的同步控制基类。下面的子类分为公平和非公平版本。
     * 使用 AQS 状态来表示锁的持有次数。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * 执行 {@link Lock#lock}。子类化的主要原因是为了允许非公平版本的快速路径。
         */
        abstract void lock();

        /**
         * 执行非公平的 tryLock。tryAcquire 在子类中实现，但两者都需要用于 tryLock 方法的非公平尝试。
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // 溢出
                    throw new Error("锁持有次数超过最大值");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // 虽然我们通常必须先读取状态再读取所有者，
            // 但我们不需要这样做来检查当前线程是否为所有者
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 从外部类转发的方法

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * 从流中重构实例（即反序列化它）。
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // 重置为未加锁状态
        }
    }

    /**
     * 非公平锁的同步对象
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * 执行锁定。尝试立即抢占，失败时回退到正常获取。
         */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁的同步对象
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        /**
         * 公平版本的 tryAcquire。除非是递归调用、没有等待线程或者是第一个线程，否则不授予访问权限。
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                        compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("锁持有次数超过最大值");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * 创建一个 {@code ReentrantLock} 实例。
     * 这等价于使用 {@code ReentrantLock(false)}。
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 创建具有给定公平性策略的 {@code ReentrantLock} 实例。
     *
     * @param fair {@code true} 表示此锁应使用公平排序策略
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获取锁。
     *
     * <p>如果锁未被其他线程持有，则获取锁并立即返回，将锁持有计数设置为 1。
     *
     * <p>如果当前线程已经持有锁，则持有计数加 1，并立即返回。
     *
     * <p>如果锁已被其他线程持有，则当前线程将被禁用调度并进入休眠，直到获取到锁为止，
     * 此时锁持有计数将设置为 1。
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 获取锁，除非当前线程被 {@linkplain Thread#interrupt 中断}。
     *
     * <p>如果锁未被其他线程持有，则获取锁并立即返回，将锁持有计数设置为 1。
     *
     * <p>如果当前线程已经持有锁，则持有计数加 1，并立即返回。
     *
     * <p>如果锁已被其他线程持有，则当前线程将被禁用调度并进入休眠，直到发生以下两种情况之一：
     *
     * <ul>
     * <li>当前线程获取到锁；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程。
     * </ul>
     *
     * <p>如果当前线程获取到锁，则锁持有计数将设置为 1。
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在获取锁时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * <p>在此实现中，由于此方法是一个明确的中断点，因此优先响应中断，而不是正常或重入获取锁。
     *
     * @throws InterruptedException 如果当前线程被中断
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 仅在调用时锁未被其他线程持有时获取锁。
     *
     * <p>如果锁未被其他线程持有，则获取锁并立即返回 {@code true}，将锁持有计数设置为 1。
     * 即使此锁已被设置为使用公平排序策略，调用 {@code tryLock()} 时也会立即获取锁（如果可用），
     * 无论是否有其他线程正在等待获取锁。这种“抢占”行为在某些情况下很有用，尽管它破坏了公平性。
     * 如果您希望遵守此锁的公平性设置，则使用
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS)}，
     * 它几乎等价（同时它也检测中断）。
     *
     * <p>如果当前线程已经持有此锁，则持有计数加 1，并返回 {@code true}。
     *
     * <p>如果锁已被其他线程持有，则此方法将立即返回 {@code false}。
     *
     * @return {@code true} 如果锁空闲且被当前线程获取，或者锁已被当前线程持有；
     *         否则返回 {@code false}
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 如果锁未被其他线程持有，并且当前线程在给定的等待时间内未被
     * {@linkplain Thread#interrupt 中断}，则获取锁。
     *
     * <p>如果锁未被其他线程持有，则获取锁并立即返回 {@code true}，将锁持有计数设置为 1。
     * 如果此锁已被设置为使用公平排序策略，则如果有其他线程正在等待锁，则不会获取锁，
     * 这与 {@link #tryLock()} 方法不同。如果您希望在公平锁上允许抢占的定时 {@code tryLock}，
     * 则将定时和非定时形式结合起来：
     *
     * <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * <p>如果当前线程已持有此锁，则持有计数加 1，并返回 {@code true}。
     *
     * <p>如果锁已被其他线程持有，则当前线程将被禁用调度并进入休眠，直到发生以下三种情况之一：
     * <ul>
     * <li>当前线程获取到锁；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或者
     * <li>指定的等待时间已过。
     * </ul>
     *
     * <p>如果获取到锁，则返回值为 {@code true}，并将锁持有计数设置为 1。
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在获取锁时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * <p>如果指定的等待时间已过，则返回值为 {@code false}。
     * 如果时间小于或等于零，则此方法不会等待。
     *
     * <p>在此实现中，由于此方法是一个明确的中断点，因此优先响应中断，
     * 而不是正常或重入获取锁，也优先于报告等待时间的到期。
     *
     * @param timeout 等待锁的时间
     * @param unit 超时时间参数的时间单位
     * @return {@code true} 如果锁空闲且被当前线程获取，或者锁已被当前线程持有；
     *         否则返回 {@code false} 如果等待时间已过，锁仍未获取
     * @throws InterruptedException 如果当前线程被中断
     * @throws NullPointerException 如果时间单位为 null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 尝试释放此锁。
     *
     * <p>如果当前线程是此锁的持有者，则持有计数减少。
     * 如果持有计数现在为零，则释放锁。
     * 如果当前线程不是此锁的持有者，则抛出 {@link IllegalMonitorStateException}。
     *
     * @throws IllegalMonitorStateException 如果当前线程没有持有此锁
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 返回与此 {@link Lock} 实例一起使用的 {@link Condition} 实例。
     *
     * <p>返回的 {@link Condition} 实例支持与 {@link Object} 监视器方法
     * （{@link Object#wait() wait}、{@link Object#notify notify} 和
     * {@link Object#notifyAll notifyAll}）相同的用法，
     * 当与内置的监视器锁一起使用时。
     *
     * <ul>
     * <li>如果在调用任何 {@link Condition} {@linkplain Condition#await() 等待} 或
     * {@linkplain Condition#signal 信号} 方法时未持有此锁，则抛出 {@link IllegalMonitorStateException}。
     *
     * <li>当调用条件的 {@linkplain Condition#await() 等待} 方法时，锁被释放，
     * 并且在它们返回之前，锁将被重新获取，锁持有计数将恢复到调用方法时的状态。
     *
     * <li>如果线程在等待时被 {@linkplain Thread#interrupt 中断}，等待将终止，
     * 抛出 {@link InterruptedException}，并清除线程的中断状态。
     *
     * <li>等待线程按 FIFO 顺序发出信号。
     *
     * <li>从等待方法返回的线程的锁重新获取顺序与最初获取锁的线程相同，
     * 对于 <em>公平</em> 锁，优先考虑那些等待时间最长的线程。
     * </ul>
     *
     * @return Condition 对象
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程对该锁的持有次数。
     *
     * <p>对于每个锁定操作（未与解锁操作匹配的），线程对锁有一个持有记录。
     *
     * <p>持有计数信息通常仅用于测试和调试目的。例如，如果某个代码段不应在锁已经持有的情况下进入，
     * 我们可以断言这一事实：
     *
     * <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... 方法主体
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return 当前线程对该锁的持有次数，如果该锁未被当前线程持有，则为 0
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询当前线程是否持有该锁。
     *
     * <p>类似于内置监视器锁的 {@link Thread#holdsLock(Object)} 方法，
     * 此方法通常用于调试和测试。例如，某个方法只能在锁持有时调用，可以断言这一事实：
     *
     * <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... 方法主体
     *   }
     * }}</pre>
     *
     * <p>它还可用于确保重入锁以非重入方式使用，例如：
     *
     * <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... 方法主体
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return 如果当前线程持有该锁，则返回 {@code true}，否则返回 {@code false}
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询该锁是否被任何线程持有。此方法用于监视系统状态，
     * 而不是用于同步控制。
     *
     * @return 如果有线程持有该锁，则返回 {@code true}，否则返回 {@code false}
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 如果该锁设置为公平锁，则返回 {@code true}。
     *
     * @return 如果该锁为公平锁，则返回 {@code true}
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前持有该锁的线程，如果没有线程持有该锁，则返回 {@code null}。
     * 当此方法由非所有者线程调用时，返回值反映当前锁状态的尽力近似值。
     * 例如，尽管有线程试图获取锁但尚未获取，所有者可能暂时为 {@code null}。
     * 此方法旨在促进构建提供更广泛锁监控功能的子类。
     *
     * @return 持有锁的线程，如果没有线程持有锁，则返回 {@code null}
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询是否有任何线程在等待获取该锁。请注意，由于取消可能随时发生，
     * 返回 {@code true} 并不保证任何其他线程将会获取该锁。
     * 此方法主要用于监视系统状态。
     *
     * @return 如果可能有其他线程在等待获取该锁，则返回 {@code true}
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给定线程是否在等待获取该锁。请注意，由于取消可能随时发生，
     * 返回 {@code true} 并不保证此线程将会获取该锁。
     * 此方法主要用于监视系统状态。
     *
     * @param thread 要查询的线程
     * @return 如果给定线程在队列中等待获取该锁，则返回 {@code true}
     * @throws NullPointerException 如果线程为 null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取该锁的线程数量的估计值。由于在此方法遍历内部数据结构时，
     * 线程数量可能会动态变化，因此该值仅为估计值。
     * 此方法用于监视系统状态，而不是用于同步控制。
     *
     * @return 等待获取该锁的线程的估计数量
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个集合，包含可能在等待获取该锁的线程。
     * 由于在构建此结果时，线程的实际集合可能会动态变化，
     * 因此返回的集合只是尽力估计。返回集合中的元素没有特定顺序。
     * 此方法旨在促进构建提供更广泛监控功能的子类。
     *
     * @return 线程集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有线程在等待与此锁相关的给定条件。
     * 请注意，由于超时和中断可能随时发生，返回 {@code true} 并不保证将来会有线程被唤醒。
     * 此方法主要用于监视系统状态。
     *
     * @param condition 条件对象
     * @return 如果有线程在等待，则返回 {@code true}
     * @throws IllegalMonitorStateException 如果该锁未被持有
     * @throws IllegalArgumentException 如果给定条件与该锁无关
     * @throws NullPointerException 如果条件为 null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回等待与此锁相关的给定条件的线程数量的估计值。
     * 请注意，由于超时和中断可能随时发生，此估计值仅为实际等待线程数量的上限。
     * 此方法用于监视系统状态，而不是用于同步控制。
     *
     * @param condition 条件对象
     * @return 等待线程的估计数量
     * @throws IllegalMonitorStateException 如果该锁未被持有
     * @throws IllegalArgumentException 如果给定条件与该锁无关
     * @throws NullPointerException 如果条件为 null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回一个集合，包含可能在等待与此锁相关的给定条件的线程。
     * 由于在构建此结果时，线程的实际集合可能会动态变化，
     * 因此返回的集合只是尽力估计。返回集合中的元素没有特定顺序。
     * 此方法旨在促进构建提供更多监控功能的子类。
     *
     * @param condition 条件对象
     * @return 线程集合
     * @throws IllegalMonitorStateException 如果该锁未被持有
     * @throws IllegalArgumentException 如果给定条件与该锁无关
     * @throws NullPointerException 如果条件为 null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回标识此锁及其锁状态的字符串。
     * 状态包含在方括号中，并包括字符串 {@code "Unlocked"} 或字符串 {@code "Locked by"}，
     * 后跟持有线程的 {@linkplain Thread#getName 名称}。
     *
     * @return 标识此锁及其锁状态的字符串
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}



