package org.top.java.concurrent.source.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午2:17
 */

/**
 * <p>这是一个支持与 <code>ReentrantLock</code> 类似语义的 <code>ReadWriteLock</code> 实现。该类具有以下属性：</p>
 * <h4 id='获取顺序'>获取顺序</h4>
 * <p>此类不强制为锁访问指定读者或写者的优先顺序。但是，它支持一个可选的公平策略。</p>
 * <ul>
 * <li><p><strong>非公平模式（默认）</strong>
 * 当构造为非公平模式（默认）时，读锁和写锁的进入顺序未指定，受重入性约束的影响。一个被持续竞争的非公平锁可能无限期推迟一个或多个读者或写者线程，但通常会比公平锁具有更高的吞吐量。</p>
 * </li>
 * <li><p><strong>公平模式</strong>
 * 当构造为公平模式时，线程会按大致到达顺序竞争进入。当当前持有的锁被释放时，等待时间最长的单个写线程将被分配写锁，或者如果有一组等待时间比所有写线程更长的读线程组，则该组读线程将被分配读锁。</p>
 * <p>尝试获取公平读锁（非重入地）的线程如果写锁已被持有或有等待的写线程，将被阻塞。该线程在等待时间最长的写线程获取并释放写锁之后才会获得读锁。当然，如果一个等待的写线程放弃了等待，导致一个或多个读线程成为队列中等待时间最长的线程且写锁为空闲状态，那么这些读线程将被分配读锁。</p>
 * <p>尝试获取公平写锁（非重入地）的线程将被阻塞，除非读锁和写锁都为空闲状态（这意味着没有等待的线程）。(注意：非阻塞的 <code>ReentrantReadWriteLock.ReadLock.tryLock()</code> 和 <code>ReentrantReadWriteLock.WriteLock.tryLock()</code> 方法不遵循此公平设置，并且如果可能将立即获取锁，而不考虑等待的线程。)</p>
 * </li>
 *
 * </ul>
 * <h4 id='重入性'>重入性</h4>
 * <p>此锁允许读者和写者以 <code>ReentrantLock</code> 的方式重新获取读锁或写锁。在写线程持有的所有写锁被释放之前，非重入的读线程不被允许。
 * 此外，写者可以获取读锁，但反之不行。在其他应用中，当写锁持有期间调用或回调执行基于读锁的读取操作时，重入性可能很有用。如果读者尝试获取写锁，它将永远不会成功。</p>
 * <h4 id='锁降级'>锁降级</h4>
 * <p>重入性还允许从写锁降级为读锁，即先获取写锁，然后获取读锁，最后释放写锁。但是，不可能从读锁升级为写锁。</p>
 * <h4 id='锁获取的中断支持'>锁获取的中断支持</h4>
 * <p>读锁和写锁都支持在获取锁时被中断。</p>
 * <h4 id='条件支持'>条件支持</h4>
 * <p>写锁提供了一个 <code>Condition</code> 实现，该实现与 <code>ReentrantLock.newCondition()</code> 提供的 <code>Condition</code> 实现的行为相同。当然，这个 <code>Condition</code> 只能与写锁一起使用。
 * 读锁不支持 <code>Condition</code>，调用 <code>readLock().newCondition()</code> 会抛出 <code>UnsupportedOperationException</code>。</p>
 * <h4 id='仪表支持'>仪表支持</h4>
 * <p>该类支持方法来确定锁是否被持有或竞争。这些方法旨在用于监控系统状态，而不是用于同步控制。</p>
 * <h4 id='序列化'>序列化</h4>
 * <p>该类的序列化行为与内置锁相同：无论序列化时锁的状态如何，反序列化后的锁都处于未锁定状态。</p>
 * <h4 id='使用示例'>使用示例</h4>
 * <p>以下是一个代码示例，展示了如何在更新缓存后执行锁降级（在非嵌套方式处理多个锁时，异常处理尤其棘手）：</p>
 * <pre><code class='language-java' lang='java'>class CachedData {
 *     Object data;
 *     volatile boolean cacheValid;
 *     final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *     void processCachedData() {
 *         rwl.readLock().lock();
 *         if (!cacheValid) {
 *             // 必须在获取写锁之前释放读锁
 *             rwl.readLock().unlock();
 *             rwl.writeLock().lock();
 *             try {
 *                 // 重新检查状态，因为其他线程可能在我们之前获取了写锁并改变了状态。
 *                 if (!cacheValid) {
 *                     data = ...;
 *                     cacheValid = true;
 *                 }
 *                 // 降级：在释放写锁之前获取读锁
 *                 rwl.readLock().lock();
 *             } finally {
 *                 rwl.writeLock().unlock(); // 解锁写锁，仍持有读锁
 *             }
 *         }
 *
 *         try {
 *             use(data);
 *         } finally {
 *             rwl.readLock().unlock();
 *         }
 *     }
 * }
 * </code></pre>
 * <p><code>ReentrantReadWriteLocks</code> 可以用于改进某些类型的 <code>Collections</code> 的并发性。只有在集合预计较大、由更多读线程而非写线程访问，并且涉及的操作的开销大于同步开销时，这才通常值得使用。例如，以下是一个使用 <code>TreeMap</code> 的类，预计其较大且被并发访问：</p>
 * <pre><code class='language-java' lang='java'>class RWDictionary {
 *     private final Map&lt;String, Data&gt; m = new TreeMap&lt;String, Data&gt;();
 *     private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *     private final Lock r = rwl.readLock();
 *     private final Lock w = rwl.writeLock();
 *
 *     public Data get(String key) {
 *         r.lock();
 *         try { return m.get(key); }
 *         finally { r.unlock(); }
 *     }
 *
 *     public String[] allKeys() {
 *         r.lock();
 *         try { return m.keySet().toArray(); }
 *         finally { r.unlock(); }
 *     }
 *
 *     public Data put(String key, Data value) {
 *         w.lock();
 *         try { return m.put(key, value); }
 *         finally { w.unlock(); }
 *     }
 *
 *     public void clear() {
 *         w.lock();
 *         try { m.clear(); }
 *         finally { w.unlock(); }
 *     }
 * }
 * </code></pre>
 * <h4 id='实现说明'>实现说明</h4>
 * <p>该锁支持最多 65535 次递归写锁和 65535 次读锁。尝试超过这些限制会导致锁定方法抛出 <code>Error</code>。</p>
 * <p><strong>自 1.5 起</strong></p>
 * <p><strong>作者</strong>：Doug Lea</p>
 * <hr />
 * <p>&nbsp;</p>
 */
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** 内部类提供读锁 */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** 内部类提供写锁 */
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** 执行所有同步机制 */
    final Sync sync;

    /**
     * 创建一个新的 {@code ReentrantReadWriteLock}，具有默认的（非公平）排序属性。
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 创建一个具有给定公平策略的新的 {@code ReentrantReadWriteLock}。
     *
     * @param fair 如果该锁应该使用公平排序策略，则传入 {@code true}
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    /**
     * ReentrantReadWriteLock 的同步实现。
     * 分为公平和非公平版本。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        /*
         * 读写计数提取常量和函数。
         * 锁状态逻辑上分为两个无符号的 short：
         * 下半部分表示独占（写锁）持有计数，
         * 上半部分表示共享（读锁）持有计数。
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** 返回 count 中表示的共享持有数量 */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** 返回 count 中表示的独占持有数量 */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        /**
         * 每个线程的读持有计数器。
         * 通过 ThreadLocal 维护；缓存到 cachedHoldCounter 中。
         */
        static final class HoldCounter {
            int count = 0;
            // 使用 id 而不是引用，以避免垃圾回收问题
            final long tid = getThreadId(Thread.currentThread());
        }

        /**
         * ThreadLocal 子类。为了反序列化机制，最好显式定义它。
         */
        static final class ThreadLocalHoldCounter
                extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * 当前线程持有的可重入读锁数量。
         * 仅在构造函数和 readObject 中初始化。
         * 当线程的读持有计数降至 0 时移除。
         */
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 最后一个成功获取读锁的线程的持有计数。
         * 在下一次释放时避免 ThreadLocal 查找的常见情况。
         * 由于仅用于启发式，因此是非易失的，并且对线程进行缓存是有益的。
         *
         * <p>它可以比缓存它的线程存在得更长，但通过不保留对线程的引用来避免垃圾回收。
         *
         * <p>通过良性的竞争访问；依赖于内存模型的最终字段和空中生成引用的保证。
         */
        private transient HoldCounter cachedHoldCounter;

        /**
         * firstReader 是第一个获取读锁的线程。
         * firstReaderHoldCount 是 firstReader 的持有计数。
         *
         * <p>更准确地说，firstReader 是最后一个将共享计数从 0 变为 1 且自此未释放读锁的唯一线程；如果没有此类线程，则为 null。
         *
         * <p>除非线程在没有释放其读锁的情况下终止，否则它不会导致垃圾回收问题，因为 tryReleaseShared 会将其设置为 null。
         *
         * <p>通过良性的竞争访问；依赖于内存模型的引用空中生成保证。
         *
         * <p>这允许对无争用的读锁进行非常便宜的跟踪。
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // 确保 readHolds 的可见性
        }

        /*
         * 公平和非公平锁在获取和释放时使用相同的代码，
         * 但在队列非空时允许争夺的方式有所不同。
         */

        /**
         * 如果当前线程尝试获取读锁，并且在其他条件下有资格，但由于政策阻止了超越其他等待线程，
         * 则返回 true。
         */
        abstract boolean readerShouldBlock();

        /**
         * 如果当前线程尝试获取写锁，并且在其他条件下有资格，但由于政策阻止了超越其他等待线程，
         * 则返回 true。
         */
        abstract boolean writerShouldBlock();

        /*
         * 请注意，tryRelease 和 tryAcquire 可以由 Conditions 调用。
         * 因此，它们的参数可能包含在条件等待期间释放的所有读写持有，并在 tryAcquire 中重新建立。
         */

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free)
                setExclusiveOwnerThread(null);
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * 步骤：
             * 1. 如果读计数非零或写计数非零且拥有者是不同的线程，则失败。
             * 2. 如果计数达到最大值，则失败。（这只能发生在计数已经非零的情况下。）
             * 3. 否则，如果这是重入获取，或者队列策略允许它，则该线程有资格获取锁。
             *    如果是，更新状态并设置拥有者。
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // （注意：如果 c != 0 且 w == 0，则共享计数不为 0）
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("超过了最大锁计数");
                // 重入获取
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() ||
                    !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // 释放读锁不会影响读者，
                    // 但它可能允许等待的写线程继续，如果读写锁都已释放。
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                    "尝试解锁读锁，但当前线程未持有该锁");
        }

        protected final int tryAcquireShared(int unused) {
            /*
             * 步骤：
             * 1. 如果写锁由其他线程持有，则失败。
             * 2. 否则，此线程有资格获取锁状态，因此询问它是否应该由于队列策略而阻塞。
             *    如果不应该，尝试通过 CAS 更新状态和计数来授予。
             *    注意，此步骤不会检查重入获取，以避免在典型的非重入情况下必须检查持有计数。
             * 3. 如果步骤 2 失败，原因可能是线程不符合条件，或 CAS 失败，或计数已饱和，
             *    则进入包含完整重试循环的版本。
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                return -1;
            int r = sharedCount(c);
            if (!readerShouldBlock() &&
                    r < MAX_COUNT &&
                    compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * 读获取的完整版本，处理 CAS 失败和重入读取的情况，
         * 这些情况在 tryAcquireShared 中没有处理。
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * 该代码部分与 tryAcquireShared 中的代码有些冗余，
             * 但通过不在 tryAcquireShared 中添加重试和延迟读取持有计数的交互，
             * 整体上更加简单。
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                    // 否则我们持有独占锁；阻塞在此会导致死锁。
                } else if (readerShouldBlock()) {
                    // 确保我们不是重入地获取读锁
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("超过了最大锁计数");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // 缓存以便释放时使用
                    }
                    return 1;
                }
            }
        }
        /**
         * 执行写锁的 tryLock，允许两种模式下的争抢。
         * 该方法与 tryAcquire 的效果相同，但不调用 writerShouldBlock。
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("超过了最大锁计数");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * 执行读锁的 tryLock，允许两种模式下的争抢。
         * 该方法与 tryAcquireShared 的效果相同，但不调用 readerShouldBlock。
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                        getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("超过了最大锁计数");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            // 我们通常必须先读取状态再读取 owner，
            // 但我们无需这样做即可检查当前线程是否为 owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 传递给外部类的方法

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            // 必须在 owner 之前读取状态以确保内存一致性
            return ((exclusiveCount(getState()) == 0) ?
                    null :
                    getExclusiveOwnerThread());
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * 从流中重新构造实例（即反序列化它）。
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // 重置为未锁定状态
        }

        final int getCount() { return getState(); }
    }

    /**
     * 非公平版的 Sync
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        final boolean writerShouldBlock() {
            return false; // 写线程可以随时争抢
        }
        final boolean readerShouldBlock() {
            /* 为了避免无限制的写线程饥饿，
             * 如果此刻排在队列头部的线程（如果存在）是等待的写线程，则阻塞。
             * 这只是一个概率效应，因为如果在其他未排出队列的已启用读线程后面有一个等待的写线程，
             * 则新读线程不会阻塞。
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * 公平版的 Sync
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 由 {@link ReentrantReadWriteLock#readLock} 方法返回的锁。
     */
    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        /**
         * 子类使用的构造函数
         *
         * @param lock 外部锁对象
         * @throws NullPointerException 如果锁为 null
         */
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取读锁。
         *
         * <p>如果写锁未被其他线程持有，则获取读锁并立即返回。
         *
         * <p>如果写锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到获取读锁。
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 获取读锁，除非当前线程被
         * {@linkplain Thread#interrupt 中断}。
         *
         * <p>如果写锁未被其他线程持有，则获取读锁并立即返回。
         *
         * <p>如果写锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到以下两种情况之一发生：
         *
         * <ul>
         *
         * <li>当前线程获取了读锁；或
         *
         * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程。
         *
         * </ul>
         *
         * <p>如果当前线程：
         *
         * <ul>
         *
         * <li>在进入此方法时已设置了中断状态；或
         *
         * <li>在获取读锁时被 {@linkplain Thread#interrupt 中断}，
         *
         * </ul>
         *
         * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
         *
         * <p>在此实现中，由于此方法是一个显式的中断点，
         * 因此优先响应中断，而非正常或重入获取锁。
         *
         * @throws InterruptedException 如果当前线程被中断
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 仅当写锁未被其他线程持有时，获取读锁。
         *
         * <p>如果写锁未被其他线程持有，则获取读锁并立即返回 {@code true}。
         * 即使此锁已被设置为使用公平排序策略，调用 {@code tryLock()}
         * <em>仍将</em>立即获取读锁（如果可用），无论是否有其他线程正在等待读锁。
         * 这种 "争抢" 行为在某些情况下可能是有用的，尽管它打破了公平性。
         * 如果你想遵守该锁的公平性设置，请使用 {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS)}，这几乎等效（它也会检测中断）。
         *
         * <p>如果写锁由其他线程持有，则此方法会立即返回 {@code false}。
         *
         * @return 如果获取了读锁，则返回 {@code true}
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 如果写锁在给定的等待时间内未被其他线程持有，
         * 并且当前线程未被 {@linkplain Thread#interrupt 中断}，
         * 则获取读锁。
         *
         * <p>如果写锁未被其他线程持有，则获取读锁并立即返回 {@code true}。
         * 如果此锁已被设置为使用公平排序策略，则可用的锁
         * <em>不会</em>在任何其他线程正在等待该锁时获取。这与 {@link #tryLock()}
         * 方法形成对比。如果你想要一个允许在公平锁上争抢的计时 {@code tryLock}，
         * 请结合计时和非计时的形式：
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>如果写锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到以下三种情况之一发生：
         *
         * <ul>
         *
         * <li>当前线程获取了读锁；或
         *
         * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或
         *
         * <li>指定的等待时间已过。
         *
         * </ul>
         *
         * <p>如果获取了读锁，则返回值为 {@code true}。
         *
         * <p>如果当前线程：
         *
         * <ul>
         *
         * <li>在进入此方法时已设置了中断状态；或
         *
         * <li>在获取读锁时被 {@linkplain Thread#interrupt 中断}，
         *
         * </ul> 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
         *
         * <p>如果指定的等待时间已过，则返回值为 {@code false}。
         * 如果时间小于或等于零，则方法不会等待。
         *
         * <p>在此实现中，由于此方法是一个显式的中断点，
         * 因此优先响应中断，而非正常或重入获取锁，
         * 也优先于报告等待时间的经过。
         *
         * @param timeout 等待读锁的时间
         * @param unit 等待时间的时间单位
         * @return 如果获取了读锁，则返回 {@code true}
         * @throws InterruptedException 如果当前线程被中断
         * @throws NullPointerException 如果时间单位为 null
         */
        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 尝试释放此锁。
         *
         * <p>如果读者数量现在为零，则使该锁可供写锁尝试获取。
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * 抛出 {@code UnsupportedOperationException}，因为
         * {@code ReadLocks} 不支持条件。
         *
         * @throws UnsupportedOperationException 始终抛出
         */
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * 返回标识此锁及其状态的字符串。
         * 状态包括字符串 {@code "Read locks ="} 后跟持有的读锁的数量。
         *
         * @return 标识此锁及其状态的字符串
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                    "[Read locks = " + r + "]";
        }
    }

    /**
     * 由 {@link ReentrantReadWriteLock#writeLock} 方法返回的锁。
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        /**
         * 子类使用的构造函数
         *
         * @param lock 外部锁对象
         * @throws NullPointerException 如果锁为 null
         */
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 获取写锁。
         *
         * <p>如果读锁和写锁都未被其他线程持有，则获取写锁并立即返回，
         * 将写锁持有计数设置为 1。
         *
         * <p>如果当前线程已持有写锁，则持有计数递增 1 并立即返回。
         *
         * <p>如果锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到获取写锁，
         * 此时写锁持有计数设置为 1。
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 获取写锁，除非当前线程被
         * {@linkplain Thread#interrupt 中断}。
         *
         * <p>如果读锁和写锁都未被其他线程持有，则获取写锁并立即返回，
         * 将写锁持有计数设置为 1。
         *
         * <p>如果当前线程已持有该锁，则持有计数递增 1 并立即返回。
         *
         * <p>如果锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到以下两种情况之一发生：
         *
         * <ul>
         *
         * <li>当前线程获取了写锁；或
         *
         * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程。
         *
         * </ul>
         *
         * <p>如果写锁由当前线程获取，则持有计数设置为 1。
         *
         * <p>如果当前线程：
         *
         * <ul>
         *
         * <li>在进入此方法时已设置了中断状态；或
         *
         * <li>在获取写锁时被 {@linkplain Thread#interrupt 中断}，
         *
         * </ul>
         *
         * 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
         *
         * <p>在此实现中，由于此方法是一个显式的中断点，
         * 因此优先响应中断，而非正常或重入获取锁。
         *
         * @throws InterruptedException 如果当前线程被中断
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 仅当其他线程未持有写锁时，获取写锁。
         *
         * <p>如果读锁和写锁都未被其他线程持有，则获取写锁并立即返回 {@code true}，并将写锁持有计数设置为 1。
         * 即使此锁已被设置为使用公平排序策略，调用 {@code tryLock()} 仍将立即获取锁（如果可用），无论其他线程是否正在等待写锁。
         * 这种“争抢”行为在某些情况下可能有用，尽管它打破了公平性。如果你想遵守该锁的公平性设置，请使用
         * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS)}，它几乎等效（它也检测中断）。
         *
         * <p>如果当前线程已持有该锁，则持有计数递增 1 并返回 {@code true}。
         *
         * <p>如果锁由其他线程持有，则此方法将立即返回 {@code false}。
         *
         * @return 如果该锁是空闲的并且被当前线程获取，或写锁已被当前线程持有，则返回 {@code true}；否则返回 {@code false}。
         */
        public boolean tryLock() {
            return sync.tryWriteLock();
        }

        /**
         * 如果写锁在给定的等待时间内未被其他线程持有，并且当前线程未被 {@linkplain Thread#interrupt 中断}，则获取写锁。
         *
         * <p>如果读锁和写锁都未被其他线程持有，则获取写锁并立即返回 {@code true}，并将写锁持有计数设置为 1。
         * 如果此锁已被设置为使用公平排序策略，则可用的锁 <em>不会</em> 在任何其他线程正在等待锁时获取。这与 {@link #tryLock()} 方法形成对比。
         * 如果你想要一个允许在公平锁上争抢的计时 {@code tryLock}，请结合计时和非计时的形式：
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * <p>如果当前线程已持有该锁，则持有计数递增 1 并返回 {@code true}。
         *
         * <p>如果锁由其他线程持有，则当前线程将因线程调度而被禁用并休眠，直到以下三种情况之一发生：
         *
         * <ul>
         *
         * <li>当前线程获取了写锁；或
         *
         * <li>其他线程 {@linkplain Thread#interrupt 中断} 当前线程；或
         *
         * <li>指定的等待时间已过。
         *
         * </ul>
         *
         * <p>如果写锁由当前线程获取，则写锁持有计数设置为 1，并返回值 {@code true}。
         *
         * <p>如果当前线程：
         *
         * <ul>
         *
         * <li>在进入此方法时已设置了中断状态；或
         *
         * <li>在获取写锁时被 {@linkplain Thread#interrupt 中断}，
         *
         * </ul> 则抛出 {@link InterruptedException}，并清除当前线程的中断状态。
         *
         * <p>如果指定的等待时间已过，则返回值为 {@code false}。如果时间小于或等于零，则该方法不会等待。
         *
         * <p>在此实现中，由于此方法是一个显式的中断点，因此优先响应中断，而非正常或重入获取锁，并且优先于报告等待时间的经过。
         *
         * @param timeout 等待写锁的时间
         * @param unit 等待时间的时间单位
         * @return 如果获取了写锁，则返回 {@code true}；如果等待时间过去且未获取锁，则返回 {@code false}
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
         * <p>如果当前线程是此锁的持有者，则持有计数递减。如果持有计数现在为零，则释放该锁。
         * 如果当前线程不是此锁的持有者，则抛出 {@link IllegalMonitorStateException}。
         *
         * @throws IllegalMonitorStateException 如果当前线程未持有此锁
         */
        public void unlock() {
            sync.release(1);
        }

        /**
         * 返回此 {@link Lock} 实例的 {@link Condition} 实例。
         * <p>返回的 {@link Condition} 实例支持与 {@link Object} 监视器方法（{@link Object#wait() wait}、{@link Object#notify notify} 和 {@link Object#notifyAll notifyAll}）
         * 相同的用法，当与内置的监视器锁一起使用时。
         *
         * <ul>
         *
         * <li>如果在调用任何 {@link Condition} 方法时未持有写锁，则抛出 {@link IllegalMonitorStateException}。
         * （读锁独立于写锁持有，因此不会被检查或影响。然而，当前线程在获取了读锁的情况下调用条件等待方法本质上总是一个错误，
         * 因为可能会解除阻塞它的其他线程将无法获取写锁。）
         *
         * <li>当调用条件的 {@linkplain Condition#await() 等待} 方法时，写锁被释放，并且在它们返回之前，写锁被重新获取，锁持有计数恢复到调用方法时的状态。
         *
         * <li>如果在等待期间线程被 {@linkplain Thread#interrupt 中断}，则等待将终止，抛出 {@link InterruptedException}，并清除线程的中断状态。
         *
         * <li>等待的线程按 FIFO 顺序被唤醒。
         *
         * <li>返回等待方法的线程的锁重新获取顺序与初始获取锁的线程相同，默认情况下没有指定，但对于公平锁来说，优先考虑等待时间最长的线程。
         *
         * </ul>
         *
         * @return Condition 对象
         */
        public Condition newCondition() {
            return sync.newCondition();
        }

        /**
         * 返回标识此锁及其状态的字符串。
         * 状态，包括字符串 {@code "Write locks ="} 后跟可重入持有的写锁数量，和字符串 {@code "Read locks ="} 后跟持有的读锁数量。
         *
         * @return 标识此锁及其状态的字符串
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                    "[未锁定]" :
                    "[锁定线程为 " + o.getName() + "]");
        }

        /**
         * 查询当前线程是否持有此写锁。
         * 与 {@link ReentrantReadWriteLock#isWriteLockedByCurrentThread} 的效果相同。
         *
         * @return 如果当前线程持有此锁，则返回 {@code true}；否则返回 {@code false}
         * @since 1.6
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * 查询当前线程对该写锁的持有计数。
         * 线程每执行一次加锁操作（与一次解锁操作不匹配时），就会对锁进行一次持有。
         * 与 {@link ReentrantReadWriteLock#getWriteHoldCount} 的效果相同。
         *
         * @return 当前线程持有此锁的次数；如果当前线程未持有此锁，则返回 0
         * @since 1.6
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // 监控和状态查询

    /**
     * 如果此锁的公平性设置为 true，则返回 {@code true}。
     *
     * @return 如果此锁的公平性设置为 true，则返回 {@code true}
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前持有写锁的线程，或 {@code null}（如果未持有）。
     * 当线程调用此方法而不是锁的持有者时，返回值反映当前锁状态的最佳近似。
     * 例如，即使有线程试图获取锁但尚未成功，所有者也可能暂时为 {@code null}。
     * 该方法旨在便于构建提供更广泛锁监控功能的子类。
     *
     * @return 持有写锁的线程，或 {@code null} 如果未被持有
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询当前持有的读锁数量。此方法设计用于监控系统状态，而非用于同步控制。
     * @return 持有的读锁数量
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 查询是否有线程持有写锁。此方法设计用于监控系统状态，而非用于同步控制。
     *
     * @return 如果有线程持有写锁，则返回 {@code true}；否则返回 {@code false}
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * 查询当前线程是否持有写锁。
     *
     * @return 如果当前线程持有写锁，则返回 {@code true}；否则返回 {@code false}
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前线程对该写锁的可重入持有次数。每次加锁操作都会对锁进行一次持有，除非与一次解锁操作匹配。
     *
     * @return 当前线程对该写锁的持有次数；如果当前线程未持有写锁，则返回 0
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * 查询当前线程对该读锁的可重入持有次数。每次加锁操作都会对锁进行一次持有，除非与一次解锁操作匹配。
     *
     * @return 当前线程对该读锁的持有次数；如果当前线程未持有读锁，则返回 0
     * @since 1.6
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * 返回包含可能正在等待获取写锁的线程的集合。由于在构建此结果时线程集可能动态变化，因此返回的集合仅是一个最佳估计。
     * 返回集合中的元素没有特定顺序。此方法设计用于构建提供更广泛锁监控功能的子类。
     *
     * @return 包含线程的集合
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * 返回包含可能正在等待获取读锁的线程的集合。由于在构建此结果时线程集可能动态变化，因此返回的集合仅是一个最佳估计。
     * 返回集合中的元素没有特定顺序。此方法设计用于构建提供更广泛锁监控功能的子类。
     *
     * @return 包含线程的集合
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * 查询是否有线程在等待获取读锁或写锁。请注意，由于取消操作可能随时发生，因此 {@code true} 返回值并不保证有其他线程会成功获取锁。
     * 此方法主要设计用于系统状态的监控。
     *
     * @return 如果可能有其他线程在等待获取锁，则返回 {@code true}
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给定的线程是否在等待获取读锁或写锁。请注意，由于取消操作可能随时发生，因此 {@code true} 返回值并不保证此线程会成功获取锁。
     * 此方法主要设计用于系统状态的监控。
     *
     * @param thread 线程
     * @return 如果给定线程排队等待该锁，则返回 {@code true}
     * @throws NullPointerException 如果线程为 null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回等待获取读锁或写锁的线程数量的估计值。由于线程数量可能在此方法遍历内部数据结构时动态变化，因此该值仅是一个估计值。
     * 此方法设计用于系统状态的监控，而非用于同步控制。
     *
     * @return 等待此锁的线程的估计数量
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回包含可能在等待获取读锁或写锁的线程的集合。由于在构建此结果时线程集可能动态变化，因此返回的集合仅是一个最佳估计。
     * 返回集合中的元素没有特定顺序。此方法设计用于构建提供更广泛监控功能的子类。
     *
     * @return 包含线程的集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有任何线程在给定的与写锁相关联的条件上等待。请注意，由于超时和中断可能随时发生，因此 {@code true} 返回值并不保证将来的 {@code signal} 会唤醒任何线程。
     * 此方法主要设计用于系统状态的监控。
     *
     * @param condition 条件
     * @return 如果有任何线程在等待，则返回 {@code true}
     * @throws IllegalMonitorStateException 如果未持有该锁
     * @throws IllegalArgumentException 如果给定的条件与此锁无关
     * @throws NullPointerException 如果条件为 null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("不是所有者");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回等待给定的与写锁相关联的条件的线程数量的估计值。请注意，由于超时和中断可能随时发生，因此该估计值仅作为等待线程的上限。
     * 此方法主要设计用于系统状态的监控，而非用于同步控制。
     *
     * @param condition 条件
     * @return 等待线程的估计数量
     * @throws IllegalMonitorStateException 如果未持有该锁
     * @throws IllegalArgumentException 如果给定的条件与此锁无关
     * @throws NullPointerException 如果条件为 null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("不是所有者");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回包含可能在给定的与写锁相关联的条件上等待的线程的集合。由于在构建此结果时线程集可能动态变化，因此返回的集合仅是一个最佳估计。
     * 返回集合中的元素没有特定顺序。此方法设计用于构建提供更广泛的条件监控功能的子类。
     *
     * @param condition 条件
     * @return 包含线程的集合
     * @throws IllegalMonitorStateException 如果未持有该锁
     * @throws IllegalArgumentException 如果给定的条件与此锁无关
     * @throws NullPointerException 如果条件为 null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("不是所有者");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    /**
     * 返回标识此锁及其状态的字符串。
     * 状态包括字符串 {@code "Write locks ="}，后跟可重入持有的写锁数量，以及字符串 {@code "Read locks ="}，后跟持有的读锁数量。
     *
     * @return 标识此锁及其状态的字符串
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
                "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * 返回给定线程的线程 ID。我们必须直接访问它，而不是通过 Thread.getId() 方法，
     * 因为 getId() 不是 final 的，已知在某些情况下会被覆盖，从而无法保持唯一映射。
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe 机制
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}

