package org.top.java.source.concurrent;

import org.top.java.source.concurrent.locks.AbstractQueuedSynchronizer;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午12:31
 */

/**
 * <p>计数信号量.从概念上讲，信号量维护了一组许可（permit）。每次调用 <code>acquire()</code> 方法时，如果有必要，线程将被阻塞，直到有可用的许可，然后获取许可。每次调用 <code>release()</code> 方法时，增加一个许可，可能会释放一个阻塞的线程。然而，实际上并没有真正的许可对象被使用；信号量只是维护了一个可用许可的计数，并相应地进行操作。</p>
 * <p>信号量通常用于限制访问某些（物理或逻辑）资源的线程数量。例如，下面是一个使用信号量控制对资源池访问的类：</p>
 * <pre><code class='language-java' lang='java'>class Pool {
 *     private static final int MAX_AVAILABLE = 100;
 *     private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *     public Object getItem() throws InterruptedException {
 *         available.acquire();
 *         return getNextAvailableItem();
 *     }
 *
 *     public void putItem(Object x) {
 *         if (markAsUnused(x))
 *             available.release();
 *     }
 *
 *     // 非常简单的数据结构，仅用于演示
 *     protected Object[] items = ... // 管理的各种对象
 *     protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *     protected synchronized Object getNextAvailableItem() {
 *         for (int i = 0; i &lt; MAX_AVAILABLE; ++i) {
 *             if (!used[i]) {
 *                 used[i] = true;
 *                 return items[i];
 *             }
 *         }
 *         return null; // 理论上不会到达这里
 *     }
 *
 *     protected synchronized boolean markAsUnused(Object item) {
 *         for (int i = 0; i &lt; MAX_AVAILABLE; ++i) {
 *             if (item == items[i]) {
 *                 if (used[i]) {
 *                     used[i] = false;
 *                     return true;
 *                 } else
 *                     return false;
 *             }
 *         }
 *         return false;
 *     }
 * }
 * </code></pre>
 * <p>在获取某个资源之前，每个线程必须从信号量中获取一个许可，保证有资源可供使用。当线程完成对资源的使用后，资源被返回到池中，并且一个许可被归还给信号量，允许另一个线程获取该资源。请注意，当调用 <code>acquire()</code> 时，没有持有任何同步锁，因为那样会阻止其他线程将资源归还到池中。信号量封装了对资源池访问的同步控制，与维护资源池内部一致性所需的同步机制分离。</p>
 * <h3 id='二进制信号量binary-semaphore）'>二进制信号量（Binary Semaphore）</h3>
 * <p>将信号量初始化为 1，且只允许最多有一个许可可用时，它可以作为一种互斥锁使用。这通常被称为<strong>二进制信号量</strong>，因为它只有两种状态：一个许可可用或没有许可可用。以这种方式使用时，二进制信号量具有与许多 <code>java.util.concurrent</code> 包中的锁实现不同的特性，即“锁”可以由非所有者线程释放（因为信号量没有所有权的概念）。在某些特定的上下文中，这种特性是有用的，例如死锁恢复。</p>
 * <h3 id='公平性设置'>公平性设置</h3>
 * <p>该类的构造方法可以选择性地接受一个<strong>公平性参数</strong>。如果设置为 <code>false</code>，则该类不保证线程获取许可的顺序。特别是，允许“插队”：一个新调用 <code>acquire()</code> 的线程可能会在已经等待的线程之前获得许可——逻辑上，新线程将自己放在等待线程队列的前面。</p>
 * <p>如果公平性设置为 <code>true</code>，信号量保证调用任何 <code>acquire()</code> 方法的线程按照其调用顺序获取许可（即<strong>先进先出</strong>，FIFO）。需要注意的是，FIFO 顺序适用于这些方法内部执行的特定点。因此，一个线程可能在另一个线程之前调用 <code>acquire()</code>，但在到达顺序点时晚于其他线程。同样，在方法返回时也可能会发生类似情况。</p>
 * <p>还要注意，非定时的 <code>tryAcquire()</code> 方法不遵循公平性设置，而是会直接获取任何可用的许可。</p>
 * <p>通常，用于控制资源访问的信号量应该初始化为公平模式，以确保没有线程会被饿死而无法访问资源。在使用信号量进行其他类型的同步控制时，非公平顺序的吞吐量优势通常会超过公平性的考虑。</p>
 * <h3 id='多许可获取和释放'>多许可获取和释放</h3>
 * <p>该类还提供了便捷方法来一次获取和释放多个许可。需要注意的是，如果这些方法在未设置公平性的情况下使用，可能会增加无限推迟的风险（即某些线程可能永远不会获取许可）。</p>
 * <h3 id='内存一致性效果'>内存一致性效果</h3>
 * <p>在一个线程中调用 <code>release()</code> 方法（如 <code>release()</code>）之前的操作，<strong>先行发生</strong>于另一个线程成功调用 <code>acquire()</code> 方法（如 <code>acquire()</code>）后的操作。</p>
 * <h3 id='版本信息'>版本信息：</h3>
 * <p>自 1.5 版本起</p>
 * <p><strong>作者</strong>：Doug Lea</p>
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** 所有机制通过 AbstractQueuedSynchronizer 子类完成 */
    private final Sync sync;

    /**
     * 信号量的同步实现。使用 AQS 状态来表示许可。分为公平和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // 溢出
                    throw new Error("超出最大许可数量");
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // 下溢
                    throw new Error("许可数量下溢");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * 非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * 公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * 使用给定的许可数量和非公平设置创建一个 {@code Semaphore}。
     *
     * @param permits 可用的初始许可数量。
     *        这个值可以是负数，在这种情况下必须先释放许可，然后才能授予任何请求。
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * 使用给定的许可数量和给定的公平性设置创建一个 {@code Semaphore}。
     *
     * @param permits 可用的初始许可数量。
     *        这个值可以是负数，在这种情况下必须先释放许可，然后才能授予任何请求。
     * @param fair {@code true} 如果该信号量在竞争时保证按先进先出的顺序授予许可，否则为 {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * 从此信号量中获取一个许可，直到有可用的许可或线程被
     * {@linkplain Thread#interrupt 中断} 为止。
     *
     * <p>获取一个许可，如果有可用的许可，立即返回，并减少一个许可数量。
     *
     * <p>如果没有可用的许可，则当前线程会被禁用调度，并进入休眠，直到发生以下两种情况之一：
     * <ul>
     * <li>其他线程调用 {@link #release} 方法为此信号量释放一个许可，并且当前线程是下一个被分配许可的线程；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入该方法时已设置了中断状态；或者
     * <li>在等待许可时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * @throws InterruptedException 如果当前线程被中断
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 从此信号量获取一个许可，直到有可用的许可为止。
     *
     * <p>获取一个许可，如果有可用的许可，立即返回，并减少一个许可数量。
     *
     * <p>如果没有可用的许可，则当前线程会被禁用调度并进入休眠，直到其他线程调用
     * {@link #release} 方法为此信号量释放一个许可，并且当前线程是下一个被分配许可的线程。
     *
     * <p>如果当前线程在等待许可期间被 {@linkplain Thread#interrupt 中断}，它将继续等待，
     * 但线程被分配许可的时间可能会发生变化。
     * 当线程从此方法返回时，它的中断状态将被设置。
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 仅当调用时有可用许可时，从此信号量获取一个许可。
     *
     * <p>获取一个许可，如果有可用的许可，立即返回，并减少一个许可数量。
     *
     * <p>如果没有可用的许可，则此方法立即返回 {@code false}。
     *
     * <p>即使此信号量设置为使用公平排序策略，调用 {@code tryAcquire()}
     * 仍然会立即获取许可（如果有可用的许可），而不管其他线程是否正在等待。
     * 这种 "抢占" 行为在某些情况下很有用，即使它破坏了公平性。
     * 如果你想遵守公平性设置，则使用
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }，
     * 该方法几乎等价（它还检测中断）。
     *
     * @return {@code true} 如果成功获取许可，否则返回 {@code false}
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 如果在指定的等待时间内许可变得可用且当前线程未被
     * {@linkplain Thread#interrupt 中断}，则从此信号量获取一个许可。
     *
     * <p>获取一个许可，如果有可用的许可，立即返回 {@code true}，
     * 并减少一个许可数量。
     *
     * <p>如果没有可用的许可，则当前线程会被禁用调度并进入休眠，直到以下三种情况之一发生：
     * <ul>
     * <li>其他线程调用 {@link #release} 方法为此信号量释放一个许可，当前线程是下一个被分配许可的线程；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或者
     * <li>指定的等待时间已过。
     * </ul>
     *
     * <p>如果获取到许可，则返回值 {@code true}。
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在等待许可时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     *
     * <p>如果指定的等待时间已过，则返回值 {@code false}。如果时间小于或等于零，此方法不会等待。
     *
     * @param timeout 等待许可的最长时间
     * @param unit {@code timeout} 参数的时间单位
     * @return {@code true} 如果获取到许可，否则 {@code false}
     * @throws InterruptedException 如果当前线程被中断
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放一个许可，将其返回到信号量中。
     *
     * <p>释放一个许可，增加可用的许可数量。如果有线程正在尝试获取许可，
     * 则选择一个线程并给予刚刚释放的许可。该线程将（重新）启用线程调度。
     *
     * <p>释放许可的线程不必是通过调用 {@link #acquire} 获取到许可的线程。
     * 正确使用信号量是通过编程约定在应用程序中建立的。
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 从此信号量获取给定数量的许可，直到所有许可都可用，或者线程被
     * {@linkplain Thread#interrupt 中断} 为止。
     *
     * <p>获取给定数量的许可，如果它们可用，立即返回，并减少许可数量。
     *
     * <p>如果可用的许可不足，则当前线程会被禁用调度并进入休眠，直到以下两种情况之一发生：
     * <ul>
     * <li>其他线程调用 {@link #release()} 为此信号量释放许可，当前线程是下一个被分配许可的线程，
     * 并且可用的许可数量满足该请求；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在等待许可时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     * 此线程要分配的许可将被分配给其他尝试获取许可的线程，
     * 就像通过调用 {@link #release()} 方法使许可可用一样。
     *
     * @param permits 要获取的许可数量
     * @throws InterruptedException 如果当前线程被中断
     * @throws IllegalArgumentException 如果 {@code permits} 为负数
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * 从此信号量获取给定数量的许可，直到所有许可都可用为止。
     *
     * <p>获取给定数量的许可，如果它们可用，立即返回，并减少许可数量。
     *
     * <p>如果可用的许可不足，则当前线程会被禁用调度并进入休眠，直到其他线程
     * 调用 {@link #release()} 为此信号量释放许可，当前线程是下一个被分配许可的线程，
     * 并且可用的许可数量满足该请求。
     *
     * <p>如果当前线程在等待许可期间被 {@linkplain Thread#interrupt 中断}，
     * 它将继续等待，并且它在队列中的位置不会受到影响。
     * 当线程从此方法返回时，它的中断状态将被设置。
     *
     * @param permits 要获取的许可数量
     * @throws IllegalArgumentException 如果 {@code permits} 为负数
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * 仅当调用时有足够的许可时，从此信号量获取给定数量的许可。
     *
     * <p>获取给定数量的许可，如果有可用的许可，立即返回 {@code true}，
     * 并减少许可数量。
     *
     * <p>如果可用的许可不足，则此方法立即返回 {@code false}，并且许可数量不变。
     *
     * <p>即使此信号量设置为使用公平排序策略，调用 {@code tryAcquire()}
     * 仍然会立即获取许可（如果有可用的许可），而不管其他线程是否正在等待。
     * 这种 "抢占" 行为在某些情况下很有用，即使它破坏了公平性。如果你想遵守公平性设置，
     * 则使用 {@link #tryAcquire(int, long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS)}，
     * 该方法几乎等价（它还检测中断）。
     *
     * @param permits 要获取的许可数量
     * @return {@code true} 如果成功获取许可，否则返回 {@code false}
     * @throws IllegalArgumentException 如果 {@code permits} 为负数
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 如果在指定的等待时间内所有许可变得可用且当前线程未被
     * {@linkplain Thread#interrupt 中断}，则从此信号量获取给定数量的许可。
     *
     * <p>获取给定数量的许可，如果它们可用，立即返回 {@code true}，
     * 并减少许可数量。
     *
     * <p>如果可用的许可不足，则当前线程会被禁用调度并进入休眠，直到以下三种情况之一发生：
     * <ul>
     * <li>其他线程调用 {@link #release()} 为此信号量释放许可，当前线程是下一个被分配许可的线程，
     * 并且可用的许可数量满足该请求；或者
     * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或者
     * <li>指定的等待时间已过。
     * </ul>
     *
     * <p>如果获取到许可，则返回值 {@code true}。
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置了中断状态；或者
     * <li>在等待许可时被 {@linkplain Thread#interrupt 中断}，
     * </ul>
     * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
     * 要分配给此线程的许可将被分配给其他尝试获取许可的线程，
     * 就像通过调用 {@link #release()} 方法使许可可用一样。
     *
     * <p>如果指定的等待时间已过，则返回值 {@code false}。如果时间小于或等于零，
     * 则此方法不会等待。要分配给此线程的许可将被分配给其他尝试获取许可的线程，
     * 就像通过调用 {@link #release()} 方法使许可可用一样。
     *
     * @param permits 要获取的许可数量
     * @param timeout 等待许可的最长时间
     * @param unit {@code timeout} 参数的时间单位
     * @return {@code true} 如果成功获取许可，否则返回 {@code false}
     * @throws InterruptedException 如果当前线程被中断
     * @throws IllegalArgumentException 如果 {@code permits} 为负数
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 释放给定数量的许可，并将它们返回到信号量中。
     *
     * <p>释放给定数量的许可，增加可用的许可数量。
     * 如果有线程正在尝试获取许可，则选择一个线程并给予刚刚释放的许可。
     * 如果可用的许可数量满足该线程的请求，则该线程将（重新）启用线程调度；
     * 否则该线程将继续等待，直到有足够的许可可用。
     * 如果在满足该线程请求之后仍有可用的许可，则这些许可将依次分配给其他尝试获取许可的线程。
     *
     * <p>释放许可的线程不必是通过调用 {@link java.util.concurrent.Semaphore#acquire acquire} 获取到许可的线程。
     * 正确使用信号量是通过编程约定在应用程序中建立的。
     *
     * @param permits 要释放的许可数量
     * @throws IllegalArgumentException 如果 {@code permits} 为负数
     */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * 返回此信号量中当前可用的许可数量。
     *
     * <p>此方法通常用于调试和测试目的。
     *
     * @return 此信号量中可用的许可数量
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * 获取并返回所有立即可用的许可。
     *
     * @return 获取到的许可数量
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * 缩减可用许可的数量。
     * 该方法在使用信号量跟踪不可用资源的子类中很有用。
     * 此方法与 {@code acquire} 的不同之处在于它不会阻塞等待许可变得可用。
     *
     * @param reduction 要减少的许可数量
     * @throws IllegalArgumentException 如果 {@code reduction} 为负数
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * 如果此信号量设置为公平，则返回 {@code true}。
     *
     * @return 如果此信号量设置为公平，则返回 {@code true}
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 查询是否有任何线程正在等待获取许可。请注意，由于随时可能发生取消，
     * {@code true} 的返回值并不保证有其他线程会获取许可。
     * 此方法主要用于监视系统状态。
     *
     * @return {@code true} 如果可能有其他线程在等待获取许可
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 返回等待获取许可的线程数量的估计值。该值仅为估计值，因为线程数量可能在此方法遍历内部数据结构时动态变化。
     * 此方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @return 正在等待此锁的线程数量的估计值
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回一个集合，包含可能正在等待获取许可的线程。
     * 由于在构建此结果时实际的线程集合可能会动态变化，因此返回的集合只是尽力而为的估计值。
     * 返回集合中的元素没有特定的顺序。
     * 此方法设计用于帮助构建提供更广泛监控功能的子类。
     *
     * @return 线程集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 返回标识此信号量及其状态的字符串。状态包含在方括号内，
     * 包括字符串 {@code "Permits ="}，后跟许可数量。
     *
     * @return 标识此信号量及其状态的字符串
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}



