package org.top.java.source.concurrent.locks;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/14 下午1:50
 */

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * <p>基于能力的锁有三种模式用于控制读/写访问。<code>StampedLock</code> 的状态由一个版本和模式组成。锁获取方法返回一个戳记（stamp），该戳记表示并控制相对于锁状态的访问；这些方法的 &quot;try&quot; 版本可能返回特殊值 0，表示未能获取访问权限。锁释放和转换方法需要戳记作为参数，如果它们与锁的状态不匹配则会失败。这三种模式是：</p>
 * <p><strong>写模式</strong>：<code>writeLock</code> 方法可能会阻塞，等待独占访问，返回一个戳记，可用于 <code>unlockWrite</code> 方法释放锁定。还提供了未计时和计时版本的 <code>tryWriteLock</code> 方法。当锁处于写模式时，无法获取任何读锁，并且所有乐观读验证都会失败。</p>
 * <p><strong>读模式</strong>：<code>readLock</code> 方法可能会阻塞，等待非独占访问，返回一个戳记，可用于 <code>unlockRead</code> 方法释放锁定。还提供了未计时和计时版本的 <code>tryReadLock</code> 方法。</p>
 * <p><strong>乐观读模式</strong>：<code>tryOptimisticRead</code> 方法仅在当前锁未处于写模式时返回非零的戳记。<code>validate</code> 方法在自获取某个戳记后，锁未被写模式获取时返回 <code>true</code>。这种模式可以视为读锁的一种极其弱的版本，写线程可以随时打破它。乐观模式的使用在短小的只读代码段中通常能减少争用并提高吞吐量。然而，它的使用本质上非常脆弱。乐观读区块应仅读取字段，并将它们保存在本地变量中，待验证后再使用。在乐观模式下读取的字段可能会非常不一致，因此仅在你对数据表示足够熟悉以检查一致性时才适用，或者反复调用 <code>validate()</code> 方法。例如，当第一次读取对象或数组引用，然后访问它的某个字段、元素或方法时，通常需要采取这样的步骤。</p>
 * <p>该类还支持在三种模式之间条件转换的方法。例如，<code>tryConvertToWriteLock</code> 方法尝试将模式“升级”，如果满足以下条件则返回一个有效的写戳记： (1) 当前已经处于写模式 (2) 当前处于读模式且没有其他读线程 (3) 当前处于乐观模式且锁可用。这些方法的形式旨在减少重试设计中可能出现的代码膨胀。</p>
 * <p><code>StampedLocks</code> 设计用于在线程安全组件的开发中作为内部工具使用。它们的使用依赖于对被保护的数据、对象和方法的内部属性的了解。它们不是可重入的，因此锁定体不应调用可能尝试重新获取锁的其他未知方法（尽管可以将戳记传递给其他可以使用或转换它的方法）。读锁模式的使用依赖于相关代码区块是无副作用的。未经验证的乐观读区块不能调用那些无法容忍潜在不一致性的方法。戳记使用有限表示，并且不是加密安全的（即，有效的戳记可能是可猜测的）。戳记值可能会在（最早一年的持续操作之后）循环。持有戳记且不使用或验证超过此时间段可能会导致验证失败。<code>StampedLocks</code> 是可序列化的，但在反序列化时始终处于初始未锁定状态，因此它们不适用于远程锁定。</p>
 * <p><code>StampedLock</code> 的调度策略并不总是偏向读者或写者。所有 &quot;try&quot; 方法都是尽力而为，并不一定符合任何调度或公平性策略。从任何 <code>try</code> 方法返回的零值表示未能获取或转换锁定，且不携带锁的状态信息；随后的调用可能会成功。</p>
 * <p>由于它支持跨多种锁定模式的协调使用，因此该类不直接实现 <code>Lock</code> 或 <code>ReadWriteLock</code> 接口。然而，<code>StampedLock</code> 可以在仅需要关联功能的应用中视为 <code>asReadLock()</code>、<code>asWriteLock()</code> 或 <code>asReadWriteLock()</code>。</p>
 * <h4 id='示例用法'>示例用法：</h4>
 * <p>以下代码展示了一个维护简单二维点的类中的一些用法惯用法。尽管此处不严格需要，示例代码展示了某些 <code>try/catch</code> 约定。</p>
 * <pre><code class='language-java' lang='java'>class Point {
 *    private double x, y;
 *    private final StampedLock sl = new StampedLock();
 *
 *    void move(double deltaX, double deltaY) { // 一个排他锁定的方法
 *      long stamp = sl.writeLock();
 *      try {
 *        x += deltaX;
 *        y += deltaY;
 *      } finally {
 *        sl.unlockWrite(stamp);
 *      }
 *    }
 *
 *    double distanceFromOrigin() { // 一个只读方法
 *      long stamp = sl.tryOptimisticRead();
 *      double currentX = x, currentY = y;
 *      if (!sl.validate(stamp)) {
 *         stamp = sl.readLock();
 *         try {
 *           currentX = x;
 *           currentY = y;
 *         } finally {
 *            sl.unlockRead(stamp);
 *         }
 *      }
 *      return Math.sqrt(currentX * currentX + currentY * currentY);
 *    }
 *
 *    void moveIfAtOrigin(double newX, double newY) { // 升级
 *      // 可以改为从乐观模式而非读模式开始
 *      long stamp = sl.readLock();
 *      try {
 *        while (x == 0.0 &amp;&amp; y == 0.0) {
 *          long ws = sl.tryConvertToWriteLock(stamp);
 *          if (ws != 0L) {
 *            stamp = ws;
 *            x = newX;
 *            y = newY;
 *            break;
 *          } else {
 *            sl.unlockRead(stamp);
 *            stamp = sl.writeLock();
 *          }
 *        }
 *      } finally {
 *        sl.unlock(stamp);
 *      }
 *    }
 * }
 * </code></pre>
 * <p>&nbsp;</p>
 * @since 1.8
 * @author Doug Lea
 */
public class StampedLock implements java.io.Serializable {
    /**
     * 算法注释：
     *
     * <p>该设计采用了序列锁的元素（如在 Linux 内核中使用的序列锁，参见 Lameter 的
     * http://www.lameter.com/gelato2005.pdf 以及其他地方的参考文献；
     * 参见 Boehm 的 http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html）、
     * 有序读写锁（参见 Shirako 等人 http://dl.acm.org/citation.cfm?id=2312015）。</p>
     *
     * <p>从概念上讲，锁的主要状态包括一个序列号，该序列号在写锁定时为奇数，否则为偶数。
     * 但这被一个非零的读者计数所抵消，当读锁定时，该计数为非零。在验证“乐观”序列锁读者风格的标记时忽略该计数。
     * 由于我们必须使用有限数量的比特（当前为 7 个）来表示读者，因此当读者数量超过计数字段时，
     * 使用一个补充的读者溢出位来处理。我们通过将最大读者计数值（RBITS）作为保护溢出更新的自旋锁来实现这一点。</p>
     *
     * <p>等待者使用了 CLH 锁的修改形式，该锁在 AbstractQueuedSynchronizer 中使用
     * （参见其内部文档了解更多详细信息），每个节点被标记为读者或写者。
     * 等待的读者通过共同节点（字段 cowait）进行分组，因此在大多数 CLH 机制中作为单个节点运行。
     * 由于队列结构的原因，等待节点实际上不需要携带序列号；我们知道每个节点的序列号都比其前驱节点大。
     * 这简化了调度策略，主要采用 FIFO（先进先出）方案，并结合了 Phase-Fair 锁的元素
     * （参见 Brandenburg 和 Anderson，特别是 http://www.cs.unc.edu/~bbb/diss/）。
     * 特别是，我们使用了阶段公平的防闯入规则：如果读锁在持有时到达，且有排队的写者，
     * 则该到来的读者被排队。（该规则导致了 acquireRead 方法的部分复杂性，但没有它，锁将变得非常不公平。）
     * release 方法不会（有时不能）自行唤醒 cowaiters。该操作由主要线程完成，
     * 但在 acquireRead 和 acquireWrite 方法中，其它无更好任务的线程可以帮助完成。</p>
     *
     * <p>这些规则适用于实际排队的线程。所有 tryLock 形式不顾优先级规则尝试获取锁，因此可能“闯入”。
     * 在获取方法中使用随机自旋来减少（越来越昂贵的）上下文切换，同时避免多线程之间的持续内存争用。
     * 我们将自旋次数限制在队列头。线程自旋等待最多 SPINS 次（每次迭代以 50% 的概率减少自旋计数），然后阻塞。
     * 如果唤醒时未能获取锁，并且仍然（或变为）第一个等待线程（这表明其他线程已闯入并获得锁），
     * 则它会增加自旋次数（最多 MAX_HEAD_SPINS），以减少不断输给闯入线程的可能性。</p>
     *
     * <p>几乎所有这些机制都在 acquireWrite 和 acquireRead 方法中执行，典型地，
     * 此类代码由于行为和重试依赖于一组一致的本地缓存读取，因此非常冗长。</p>
     *
     * <p>正如 Boehm 的论文（见上文）所述，序列验证（主要是 validate() 方法）需要比普通易失性读取（状态）更严格的排序规则。
     * 为了在这些情况中强制执行读取操作的顺序并进行验证，我们使用了 Unsafe.loadFence。</p>
     *
     * <p>内存布局将锁状态和队列指针保持在一起（通常在同一个缓存行上）。这通常适用于主要是读操作的负载。
     * 在大多数其他情况下，自适应自旋 CLH 锁减少了内存争用的自然趋势，减少了进一步分散争用位置的动机，但可能会在未来有所改进。</p>
     */

    private static final long serialVersionUID = -6001602636862214147L;

    /** 处理器数量，用于控制自旋 */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** 在入队前获取锁的最大重试次数 */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;

    /** 在队列头阻塞之前的最大重试次数 */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 0;

    /** 再次阻塞前的最大重试次数 */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 0;

    /** 等待溢出自旋锁时的让步周期 */
    private static final int OVERFLOW_YIELD_RATE = 7; // 必须是 2 的幂减 1

    /** 溢出前用于读者计数的位数 */
    private static final int LG_READERS = 7;

    // 锁状态和标记操作的值
    private static final long RUNIT = 1L;
    private static final long WBIT  = 1L << LG_READERS;
    private static final long RBITS = WBIT - 1L;
    private static final long RFULL = RBITS - 1L;
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS; // 注意与 ABITS 重叠

    // 锁状态的初始值；避免失败值 0
    private static final long ORIGIN = WBIT << 1;

    // 从取消的获取方法中返回的特殊值，供调用者抛出 IE 异常
    private static final long INTERRUPTED = 1L;

    // 节点状态的值，顺序很重要
    private static final int WAITING   = -1;
    private static final int CANCELLED =  1;

    // 节点模式（int 类型而非 boolean，以便进行算术操作）
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /** 等待节点 */
    static final class WNode {
        volatile WNode prev;
        volatile WNode next;
        volatile WNode cowait;    // 链接的读者列表
        volatile Thread thread;   // 在可能停车时为非空
        volatile int status;      // 0，WAITING 或 CANCELLED
        final int mode;           // RMODE 或 WMODE
        WNode(int m, WNode p) { mode = m; prev = p; }
    }

    /** CLH 队列头 */
    private transient volatile WNode whead;
    /** CLH 队列尾（最后一个） */
    private transient volatile WNode wtail;

    // 视图
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    /** 锁序列/状态 */
    private transient volatile long state;
    /** 当状态读计数达到饱和时的额外读者计数 */
    private transient int readerOverflow;

    /**
     * 创建一个新的锁，初始状态为未锁定。
     */
    public StampedLock() {
        state = ORIGIN;
    }

    /**
     * 独占获取锁，如有必要会阻塞直到可用。
     *
     * @return 一个标记，可以用于解锁或转换模式
     */
    public long writeLock() {
        long s, next;  // 仅在完全未锁定的情况下绕过 acquireWrite
        return ((((s = state) & ABITS) == 0L &&
                U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : acquireWrite(false, 0L));
    }

    /**
     * 如果锁立即可用，独占获取它。
     *
     * @return 一个标记，可以用于解锁或转换模式，
     * 如果锁不可用，则返回 0
     */
    public long tryWriteLock() {
        long s, next;
        return ((((s = state) & ABITS) == 0L &&
                U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : 0L);
    }

    /**
     * 如果在给定时间内锁可用且当前线程未被中断，则独占获取锁。
     * 超时和中断下的行为符合 {@link Lock#tryLock(long,TimeUnit)} 指定的行为。
     *
     * @param time 等待锁的最长时间
     * @param unit {@code time} 参数的时间单位
     * @return 一个标记，可以用于解锁或转换模式，
     * 如果锁不可用则返回 0
     * @throws InterruptedException 如果当前线程在获取锁之前被中断
     */
    public long tryWriteLock(long time, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next, deadline;
            if ((next = tryWriteLock()) != 0L)
                return next;
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireWrite(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * 独占获取锁，如有必要会阻塞直到可用或当前线程被中断。
     * 在中断下的行为与 {@link Lock#lockInterruptibly()} 指定的行为一致。
     *
     * @return 一个标记，可以用于解锁或转换模式
     * @throws InterruptedException 如果当前线程在获取锁之前被中断
     */
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
                (next = acquireWrite(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * 非独占获取锁，如有必要会阻塞直到可用。
     *
     * @return 一个标记，可以用于解锁或转换模式
     */
    public long readLock() {
        long s = state, next;  // 在常见的无争用情况下绕过 acquireRead
        return ((whead == wtail && (s & ABITS) < RFULL &&
                U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
                next : acquireRead(false, 0L));
    }

    /**
     * 如果锁立即可用，非独占获取它。
     *
     * @return 一个标记，可以用于解锁或转换模式，
     * 如果锁不可用，则返回 0
     */
    public long tryReadLock() {
        for (;;) {
            long s, m, next;
            if ((m = (s = state) & ABITS) == WBIT)
                return 0L;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next;
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L)
                return next;
        }
    }

    /**
     * 如果在给定时间内锁可用且当前线程未被中断，则非独占获取锁。
     * 超时和中断下的行为符合 {@link Lock#tryLock(long,TimeUnit)} 指定的行为。
     *
     * @param time 等待锁的最长时间
     * @param unit {@code time} 参数的时间单位
     * @return 一个标记，可以用于解锁或转换模式，
     * 如果锁不可用则返回 0
     * @throws InterruptedException 如果当前线程在获取锁之前被中断
     */
    public long tryReadLock(long time, TimeUnit unit)
            throws InterruptedException {
        long s, m, next, deadline;
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            if ((m = (s = state) & ABITS) != WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * 非独占获取锁，如有必要会阻塞直到可用或当前线程被中断。
     * 在中断下的行为与 {@link Lock#lockInterruptibly()} 指定的行为一致。
     *
     * @return 一个标记，可以用于解锁或转换模式
     * @throws InterruptedException 如果当前线程在获取锁之前被中断
     */
    public long readLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
                (next = acquireRead(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * 返回一个可以稍后验证的标记，如果已独占锁定则返回 0。
     *
     * @return 一个标记，如果已独占锁定则返回 0
     */
    public long tryOptimisticRead() {
        long s;
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * 如果自给定标记发出后没有独占获取锁，则返回 true。如果标记为 0，则始终返回 false。
     * 如果标记表示当前持有的锁，则始终返回 true。
     * 使用从 {@link #tryOptimisticRead} 或该锁的锁定方法未获得的值调用此方法没有定义的效果或结果。
     *
     * @param stamp 一个标记
     * @return {@code true} 如果自标记发出后没有独占获取锁；否则为 false
     */
    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (state & SBITS);
    }

    /**
     * 如果锁状态与给定标记匹配，则释放独占锁。
     *
     * @param stamp 由写锁操作返回的标记
     * @throws IllegalMonitorStateException 如果标记与当前锁状态不匹配
     */
    public void unlockWrite(long stamp) {
        WNode h;
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    /**
     * 如果锁状态与给定标记匹配，则释放非独占锁。
     *
     * @param stamp 由读锁操作返回的标记
     * @throws IllegalMonitorStateException 如果标记与当前锁状态不匹配
     */
    public void unlockRead(long stamp) {
        long s, m; WNode h;
        for (;;) {
            if (((s = state) & SBITS) != (stamp & SBITS) ||
                    (stamp & ABITS) == 0L || (m = s & ABITS) == 0L || m == WBIT)
                throw new IllegalMonitorStateException();
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    /**
     * 如果锁状态与给定标记匹配，则释放相应模式的锁。
     *
     * @param stamp 由锁操作返回的标记
     * @throws IllegalMonitorStateException 如果标记与当前锁状态不匹配
     */
    public void unlock(long stamp) {
        long a = stamp & ABITS, m, s; WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L)
                break;
            else if (m == WBIT) {
                if (a != m)
                    break;
                state = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return;
            }
            else if (a == 0L || a >= WBIT)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        throw new IllegalMonitorStateException();
    }

    /**
     * 如果锁状态与给定标记匹配，则执行以下操作之一。
     * 如果标记表示持有写锁，则返回它。或者，如果为读锁且写锁可用，
     * 则释放读锁并返回写标记。或者，如果为乐观读，则仅在写锁立即可用时返回写标记。
     * 在所有其他情况下，此方法返回 0。
     *
     * @param stamp 一个标记
     * @return 有效的写标记，失败时返回 0
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    return next;
            }
            else if (m == WBIT) {
                if (a != m)
                    break;
                return stamp;
            }
            else if (m == RUNIT && a != 0L) {
                if (U.compareAndSwapLong(this, STATE, s,
                        next = s - RUNIT + WBIT))
                    return next;
            }
            else
                break;
        }
        return 0L;
    }

    /**
     * 如果锁状态与给定标记匹配，则执行以下操作之一。
     * 如果标记表示持有写锁，则释放它并获取读锁。
     * 或者，如果为读锁，则返回它。
     * 或者，如果为乐观读，则获取读锁并仅在立即可用时返回读标记。
     * 在所有其他情况下，此方法返回 0。
     *
     * @param stamp 一个标记
     * @return 有效的读标记，失败时返回 0
     */
    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                else if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            else if (m == WBIT) {
                if (a != m)
                    break;
                state = next = s + (WBIT + RUNIT);
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            }
            else if (a != 0L && a < WBIT)
                return stamp;
            else
                break;
        }
        return 0L;
    }

    /**
     * 如果锁状态与给定标记匹配，则执行以下操作之一。
     * 如果标记表示持有锁，则释放它并返回观察标记。
     * 或者，如果为乐观读，则在验证后返回该标记。
     * 在所有其他情况下，此方法返回 0，因此它可用作 "tryUnlock" 的一种形式。
     *
     * @param stamp 一个标记
     * @return 有效的乐观读标记，失败时返回 0
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        U.loadFence();
        for (;;) {
            if (((s = state) & SBITS) != (stamp & SBITS))
                break;
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                return s;
            }
            else if (m == WBIT) {
                if (a != m)
                    break;
                state = next = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            }
            else if (a == 0L || a >= WBIT)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return next & SBITS;
                }
            }
            else if ((next = tryDecReaderOverflow(s)) != 0L)
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * 如果持有写锁，则释放它，而不需要标记值。
     * 此方法对于处理错误后的恢复可能有用。
     *
     * @return {@code true} 如果锁已持有，否则为 false
     */
    public boolean tryUnlockWrite() {
        long s; WNode h;
        if (((s = state) & WBIT) != 0L) {
            state = (s += WBIT) == 0L ? ORIGIN : s;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return true;
        }
        return false;
    }

    /**
     * 如果持有读锁，则释放其中一个持有的读锁，而不需要标记值。
     * 此方法对于处理错误后的恢复可能有用。
     *
     * @return {@code true} 如果读锁已持有，否则为 false
     */
    public boolean tryUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    // 状态监控方法

    /**
     * 返回给定状态 s 的总读锁数量和溢出读计数。
     */
    private int getReadLockCount(long s) {
        long readers;
        if ((readers = s & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }

    /**
     * 返回 {@code true} 如果锁当前被独占持有。
     *
     * @return {@code true} 如果锁当前被独占持有
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * 返回 {@code true} 如果锁当前被非独占持有。
     *
     * @return {@code true} 如果锁当前被非独占持有
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    /**
     * 查询该锁的读锁持有数量。此方法设计用于监控系统状态，而非用于同步控制。
     * @return 持有的读锁数量
     */
    public int getReadLockCount() {
        return getReadLockCount(state);
    }

    /**
     * 返回标识此锁以及其锁状态的字符串。
     * 状态在括号内包含字符串 {@code "Unlocked"} 或字符串 {@code "Write-locked"}，
     * 或字符串 {@code "Read-locks:"} 后跟当前持有的读锁数量。
     *
     * @return 标识此锁以及其锁状态的字符串
     */
    public String toString() {
        long s = state;
        return super.toString() +
                ((s & ABITS) == 0L ? "[Unlocked]" :
                        (s & WBIT) != 0L ? "[Write-locked]" :
                                "[Read-locks:" + getReadLockCount(s) + "]");
    }

    // 视图

    /**
     * 返回此 StampedLock 的普通 {@link Lock} 视图，
     * 其中 {@link Lock#lock} 方法映射到 {@link #readLock}，
     * 其他方法类似。返回的锁不支持 {@link Condition}；
     * 方法 {@link Lock#newCondition()} 抛出 {@code UnsupportedOperationException}。
     *
     * @return 锁
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    /**
     * 返回此 StampedLock 的普通 {@link Lock} 视图，
     * 其中 {@link Lock#lock} 方法映射到 {@link #writeLock}，
     * 其他方法类似。返回的锁不支持 {@link Condition}；
     * 方法 {@link Lock#newCondition()} 抛出 {@code UnsupportedOperationException}。
     *
     * @return 锁
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new WriteLockView()));
    }

    /**
     * 返回此 StampedLock 的 {@link ReadWriteLock} 视图，
     * 其中 {@link ReadWriteLock#readLock()} 方法映射到 {@link #asReadLock()}，
     * 而 {@link ReadWriteLock#writeLock()} 映射到 {@link #asWriteLock()}。
     *
     * @return 锁
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new ReadWriteLockView()));
    }

    // 视图类

    final class ReadLockView implements Lock {
        public void lock() { readLock(); }
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        public boolean tryLock() { return tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockRead(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        public void lock() { writeLock(); }
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        public boolean tryLock() { return tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockWrite(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() { return asReadLock(); }
        public Lock writeLock() { return asWriteLock(); }
    }

    // 无标记参数检查的解锁方法，适用于视图类。
    // 需要这些方法是因为视图类的锁方法会丢弃标记。

    final void unstampedUnlockWrite() {
        WNode h; long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (s += WBIT) == 0L ? ORIGIN : s;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    final void unstampedUnlockRead() {
        for (;;) {
            long s, m; WNode h;
            if ((m = (s = state) & ABITS) == 0L || m >= WBIT)
                throw new IllegalMonitorStateException();
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        state = ORIGIN; // 重置为未加锁状态
    }

    // 内部机制

    /**
     * 尝试通过首先将状态访问位设置为 RBITS 表示持有自旋锁，
     * 然后更新并释放，来增加 readerOverflow。
     *
     * @param s 一个 reader 溢出标记：(s & ABITS) >= RFULL
     * @return 成功时返回新标记，否则返回 0
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                ++readerOverflow;
                state = s;
                return s;
            }
        }
        else if ((LockSupport.nextSecondarySeed() &
                OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * 尝试减少 readerOverflow。
     *
     * @param s 一个 reader 溢出标记：(s & ABITS) >= RFULL
     * @return 成功时返回新标记，否则返回 0
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                int r; long next;
                if ((r = readerOverflow) > 0) {
                    readerOverflow = r - 1;
                    next = s;
                }
                else
                    next = s - RUNIT;
                state = next;
                return next;
            }
        }
        else if ((LockSupport.nextSecondarySeed() &
                OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * 唤醒 h 的后继者（通常是 whead）。这通常只是 h.next，
     * 但在 next 指针滞后时可能需要从 wtail 进行遍历。
     * 当一个或多个线程被取消时，这可能无法唤醒获取锁的线程，
     * 但取消方法本身提供了额外的保障以确保活跃性。
     */
    private void release(WNode h) {
        if (h != null) {
            WNode q; Thread w;
            U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (q != null && (w = q.thread) != null)
                U.unpark(w);
        }
    }

    /**
     * 获取写锁。
     *
     * @param interruptible 如果应检查中断，则为 true
     * 并在中断时返回 INTERRUPTED
     * @param deadline 如果非零，则为超时时间的 System.nanoTime 值（返回 0）
     * @return 下一个状态，或 INTERRUPTED
     */
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) { // 在排队时自旋
            long m, s, ns;
            if ((m = (s = state) & ABITS) == 0L) {
                if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                    return ns;
            }
            else if (spins < 0)
                spins = (m == WBIT && wtail == whead) ? SPINS : 0;
            else if (spins > 0) {
                if (LockSupport.nextSecondarySeed() >= 0)
                    --spins;
            }
            else if ((p = wtail) == null) { // 初始化队列
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd))
                    wtail = hd;
            }
            else if (node == null)
                node = new WNode(WMODE, p);
            else if (node.prev != p)
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                p.next = node;
                break;
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
                for (int k = spins;;) { // 在头部自旋
                    long s, ns;
                    if (((s = state) & ABITS) == 0L) {
                        if (U.compareAndSwapLong(this, STATE, s,
                                ns = s + WBIT)) {
                            whead = node;
                            node.prev = null;
                            return ns;
                        }
                    }
                    else if (LockSupport.nextSecondarySeed() >= 0 &&
                            --k <= 0)
                        break;
                }
            }
            else if (h != null) { // 帮助释放陈旧的等待者
                WNode c; Thread w;
                while ((c = h.cowait) != null) {
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                            (w = c.thread) != null)
                        U.unpark(w);
                }
            }
            if (whead == h) {
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // 失效
                }
                else if ((ps = p.status) == 0)
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                else if (ps == CANCELLED) {
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time; // 0 表示不超时
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 && (p != h || (state & ABITS) != 0L) &&
                            whead == h && node.prev == p)
                        U.park(false, time);  // 模拟 LockSupport.park
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    if (interruptible && Thread.interrupted())
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }
    /**
     * 获取读锁。
     *
     * @param interruptible 如果应检查中断，则为 true 并在中断时返回 INTERRUPTED
     * @param deadline 如果非零，则为超时时间的 System.nanoTime 值（返回 0）
     * @return 下一个状态，或 INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) {
            WNode h;
            if ((h = whead) == (p = wtail)) {
                for (long m, s, ns;;) {
                    if ((m = (s = state) & ABITS) < RFULL ?
                            U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                            (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))
                        return ns;
                    else if (m >= WBIT) {
                        if (spins > 0) {
                            if (LockSupport.nextSecondarySeed() >= 0)
                                --spins;
                        } else {
                            if (spins == 0) {
                                WNode nh = whead, np = wtail;
                                if ((nh == h && np == p) || (h = nh) != (p = np))
                                    break;
                            }
                            spins = SPINS;
                        }
                    }
                }
            }
            if (p == null) { // 初始化队列
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd))
                    wtail = hd;
            } else if (node == null) {
                node = new WNode(RMODE, p);
            } else if (h == p || p.mode != RMODE) {
                if (node.prev != p)
                    node.prev = p;
                else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    break;
                }
            } else if (!U.compareAndSwapObject(p, WCOWAIT,
                    node.cowait = p.cowait, node))
                node.cowait = null;
            else {
                for (;;) {
                    WNode pp, c; Thread w;
                    if ((h = whead) != null && (c = h.cowait) != null &&
                            U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                            (w = c.thread) != null) // 帮助释放
                        U.unpark(w);
                    if (h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        do {
                            if ((m = (s = state) & ABITS) < RFULL ?
                                    U.compareAndSwapLong(this, STATE, s,
                                            ns = s + RUNIT) :
                                    (m < WBIT &&
                                            (ns = tryIncReaderOverflow(s)) != 0L))
                                return ns;
                        } while (m < WBIT);
                    }
                    if (whead == h && p.prev == pp) {
                        long time;
                        if (pp == null || h == p || p.status > 0) {
                            node = null; // 丢弃
                            break;
                        }
                        if (deadline == 0L)
                            time = 0L;
                        else if ((time = deadline - System.nanoTime()) <= 0L)
                            return cancelWaiter(node, p, false);
                        Thread wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        if ((h != pp || (state & ABITS) == WBIT) &&
                                whead == h && p.prev == pp)
                            U.park(false, time);
                        node.thread = null;
                        U.putObject(wt, PARKBLOCKER, null);
                        if (interruptible && Thread.interrupted())
                            return cancelWaiter(node, p, true);
                    }
                }
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    long m, s, ns;
                    if ((m = (s = state) & ABITS) < RFULL ?
                            U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                            (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                        WNode c; Thread w;
                        whead = node;
                        node.prev = null;
                        while ((c = node.cowait) != null) {
                            if (U.compareAndSwapObject(node, WCOWAIT,
                                    c, c.cowait) &&
                                    (w = c.thread) != null)
                                U.unpark(w);
                        }
                        return ns;
                    }
                    else if (m >= WBIT &&
                            LockSupport.nextSecondarySeed() >= 0 && --k <= 0)
                        break;
                }
            }
            else if (h != null) {
                WNode c; Thread w;
                while ((c = h.cowait) != null) {
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                            (w = c.thread) != null)
                        U.unpark(w);
                }
            }
            if (whead == h) {
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0)
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                else if (ps == CANCELLED) {
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time;
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 &&
                            (p != h || (state & ABITS) == WBIT) &&
                            whead == h && node.prev == p)
                        U.park(false, time);
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    if (interruptible && Thread.interrupted())
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }

    /**
     * 如果非空，则强制取消状态，并在可能的情况下将其从队列中移除，
     * 唤醒任何正在等待的 cowaiters（根据适用情况）。
     * 同时，帮助释放锁的当前第一个等待者。
     * （调用 null 参数时，作为条件释放的形式，目前不需要此功能，
     * 但可能在未来的取消策略中需要）。
     * 这是 AbstractQueuedSynchronizer 中取消方法的变体
     * （详细说明请参见 AQS 的内部文档）。
     *
     * @param node 如果非空，表示等待线程
     * @param group 节点或正在一起等待的 group 节点
     * @param interrupted 是否已经被中断
     * @return INTERRUPTED 如果被中断或 Thread.interrupted 为 true，则返回 INTERRUPTED；否则返回 0
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        if (node != null && group != null) {
            Thread w;
            node.status = CANCELLED;
            // 从 group 中取消未完成的节点
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group; // 重新开始
                } else {
                    p = q;
                }
            }
            if (group == node) {
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    if ((w = r.thread) != null)
                        U.unpark(w); // 唤醒未取消的 cowaiters
                }
                for (WNode pred = node.prev; pred != null;) { // 解除链表中的无效节点
                    WNode succ, pp;        // 找到有效的后继节点
                    while ((succ = node.next) == null || succ.status == CANCELLED) {
                        WNode q = null;    // 找到后继节点
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // 如果后继取消，则不要链接
                        if (succ == q ||   // 确保准确的后继
                                U.compareAndSwapObject(node, WNEXT,
                                        succ, succ = q)) {
                            if (succ == null && node == wtail)
                                U.compareAndSwapObject(this, WTAIL, node, pred);
                            break;
                        }
                    }
                    if (pred.next == node) // 解开 pred 链
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    if (succ != null && (w = succ.thread) != null) {
                        succ.thread = null;
                        U.unpark(w);       // 唤醒后继以观察新的前继节点
                    }
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // 如果新前继节点错误/取消，重复操作
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // 可能释放第一个等待者
        while ((h = whead) != null) {
            long s; WNode q; // 类似 release()，但检查资格
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                if (q != null && h.status == 0 &&
                        ((s = state) & ABITS) != WBIT && // 等待者有资格
                        (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }

    // Unsafe 机制
    private static final sun.misc.Unsafe U;
    private static final long STATE;
    private static final long WHEAD;
    private static final long WTAIL;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WCOWAIT;
    private static final long PARKBLOCKER;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = StampedLock.class;
            Class<?> wk = WNode.class;
            STATE = U.objectFieldOffset
                    (k.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset
                    (k.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset
                    (k.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset
                    (wk.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset
                    (wk.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset
                    (wk.getDeclaredField("cowait"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset
                    (tk.getDeclaredField("parkBlocker"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}




