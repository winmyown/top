package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:28
 */

import org.top.java.source.concurrent.locks.LockSupport;
import org.top.java.source.concurrent.locks.ReentrantLock;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;

/**
 * 一种 {@linkplain BlockingQueue 阻塞队列}，在其中每次插入操作都必须等待另一个线程的对应移除操作，反之亦然。
 * 同步队列没有任何内部容量，甚至连一个容量都没有。你不能在同步队列中使用 {@code peek}，因为只有在尝试移除时元素才会存在；
 * 你不能插入一个元素（使用任何方法），除非另一个线程正在尝试移除它；你不能迭代队列，因为没有任何元素可供迭代。
 * 队列的<em>头部</em>是第一个排队的插入线程试图添加到队列中的元素；如果没有这样的排队线程，那么没有元素可供移除，{@code poll()} 将返回 {@code null}。
 * 对于其他 {@code Collection} 方法（例如 {@code contains}）而言，{@code SynchronousQueue} 表现为一个空集合。
 * 此队列不允许 {@code null} 元素。
 *
 * <p>同步队列类似于 CSP 和 Ada 中使用的交会通道（rendezvous channels）。它们非常适合于交接设计，
 * 即一个线程中运行的对象必须与另一个线程中的对象同步以传递某些信息、事件或任务。
 *
 * <p>此类支持可选的公平性策略来为等待的生产者和消费者线程排序。默认情况下，不保证这种排序。
 * 但是，构造时设置为 {@code true} 的公平队列以 FIFO 顺序授予线程访问权限。
 *
 * <p>此类及其迭代器实现了 {@link Collection} 和 {@link Iterator} 接口的所有<em>可选</em>方法。
 *
 * <p>此类是<a href="{@docRoot}/../technotes/guides/collections/index.html">Java Collections Framework</a>的成员。
 *
 * @since 1.5
 * 作者 Doug Lea, Bill Scherer 和 Michael Scott
 * @param <E> 此集合中持有的元素类型
 */
public class SynchronousQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

/*
 * 此类实现了 "Nonblocking Concurrent Objects with Condition Synchronization" 一文中描述的双栈和双队列算法的扩展，
 * 该文由 W. N. Scherer III 和 M. L. Scott 撰写，发表于第 18 届分布式计算年会，2004 年 10 月
 * (另见 http://www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html)。
 * （Lifo）栈用于非公平模式，(Fifo) 队列用于公平模式。两者的性能通常类似。在争用情况下，Fifo 通常支持更高的吞吐量，
 * 而 Lifo 在常见应用中维护更高的线程局部性。
 *
 * 双队列（和类似的双栈）在任何给定时间点要么持有 "数据"——通过 put 操作提供的项目，要么持有 "请求"——表示 take 操作的槽，或者为空。
 * "满足" 的调用（即，调用请求从队列中获取数据或相反）会出列一个互补的节点。这些队列最有趣的特点是任何操作都可以判断队列处于哪种模式，
 * 并相应地采取行动而不需要锁。
 *
 * 队列和栈都继承了定义单一方法 transfer 的抽象类 Transferer，该方法执行 put 或 take 操作。这些操作被统一为单个方法，
 * 因为在双数据结构中，put 和 take 操作是对称的，因此几乎所有代码都可以合并。结果是 transfer 方法的代码较长，
 * 但如果将其拆分为几乎重复的部分，则代码会更难以理解。
 *
 * 队列和栈数据结构共享许多概念上的相似之处，但几乎没有具体的细节。为了简化，它们被保留为独立的，
 * 以便它们可以在以后单独演化。
 *
 * 这里的算法与上述论文中的版本有所不同，扩展了它们以用于同步队列，并处理取消操作。主要区别包括：
 *
 *  1. 原始算法使用带有标记位的指针，而这里的算法使用节点中的模式位，导致了许多进一步的调整。
 *  2. SynchronousQueues 必须阻塞等待被满足的线程。
 *  3. 支持通过超时和中断来取消操作，包括从列表中清理取消的节点/线程，以避免垃圾保留和内存耗尽。
 *
 * 阻塞主要通过 LockSupport 的 park/unpark 实现，除非节点看起来是下一个要被满足的节点时会首先进行自旋（仅在多处理器上）。
 * 在非常繁忙的同步队列中，自旋可以显著提高吞吐量。而在不那么繁忙的队列中，自旋的数量小到不会被注意到。
 *
 * 在队列和栈中，清理操作的方式有所不同。对于队列，我们几乎总是可以在 O(1) 时间内立即移除一个取消的节点
 * （在进行一致性检查的重试之外），但如果它可能被固定为当前尾部节点，则必须等待某些后续取消操作。
 * 对于栈，我们需要可能的 O(n) 遍历才能确保移除节点，但这可以与访问栈的其他线程并发运行。
 *
 * 尽管垃圾回收机制解决了大多数节点回收问题，这些问题在非阻塞算法中通常会造成复杂性，
 * 但我们仍然需要小心地 "忘记" 可能被阻塞线程长时间持有的对数据、其他节点和线程的引用。
 * 在某些情况下，如果将引用设置为 null 会与主算法发生冲突，则通过将节点的链接更改为指向节点本身来解决。
 * 对于 Stack 节点，这种情况不多见（因为阻塞线程不会保留旧的头指针），但必须积极忘记 Queue 节点中的引用，
 * 以避免由于每个节点自到达以来引用的所有对象的可达性问题。
 */
    /**
     * 共享的内部 API，用于双栈和双队列。
     */
    abstract static class Transferer<E> {
        /**
         * 执行 put 或 take 操作。
         *
         * @param e 如果非 null，表示要交给消费者的元素；如果为 null，表示请求 transfer 方法返回由生产者提供的元素。
         * @param timed 此操作是否应超时
         * @param nanos 超时时间（以纳秒为单位）
         * @return 如果非 null，则为提供或接收到的元素；如果为 null，则操作由于超时或中断失败 —— 调用方可以通过检查 Thread.interrupted 来区分发生了哪种情况。
         */
        abstract E transfer(E e, boolean timed, long nanos);
    }

    /** CPU 的数量，用于控制自旋 */
    static final int NCPUS = Runtime.getRuntime().availableProcessors();

    /**
     * 在定时等待中，在阻塞之前自旋的次数。
     * 该值是通过经验得出的 —— 它在各种处理器和操作系统中表现良好。根据经验，最佳值似乎不会随 CPU 数量（超过 2）而变化，因此是一个常量。
     */
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    /**
     * 在非定时等待中，在阻塞之前自旋的次数。
     * 由于非定时等待自旋速度更快，因为它们不需要在每次自旋时检查时间，因此该值比定时值大。
     */
    static final int maxUntimedSpins = maxTimedSpins * 16;

    /**
     * 在使用定时 park 之前自旋更快的纳秒数。粗略估计即可。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /** 双栈 */
    static final class TransferStack<E> extends Transferer<E> {
        /*
         * 该实现扩展了 Scherer-Scott 双栈算法，主要区别包括使用 "掩盖" 节点而不是标记位的指针：满足操作会在堆栈上推入标记节点
         * （在 mode 中设置 FULFILLING 位）以保留一个与等待节点匹配的位置。
         */

        /* SNode 节点的模式，在节点字段中按位 OR */
        /** 节点表示未被满足的消费者 */
        static final int REQUEST    = 0;
        /** 节点表示未被满足的生产者 */
        static final int DATA       = 1;
        /** 节点正在满足另一个未被满足的 DATA 或 REQUEST */
        static final int FULFILLING = 2;

        /** 如果 m 设置了 FULFILLING 位，则返回 true。 */
        static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }

        /** TransferStack 的节点类。 */
        static final class SNode {
            volatile SNode next;        // 栈中的下一个节点
            volatile SNode match;       // 与该节点匹配的节点
            volatile Thread waiter;     // 用于控制 park/unpark
            Object item;                // 数据；对于 REQUESTs，值为 null
            int mode;
            // 注意：item 和 mode 字段不需要是 volatile，因为它们始终在其他 volatile/原子操作之前写入，之后读取。

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                return cmp == next &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            /**
             * 尝试将节点 s 与当前节点匹配，若匹配则唤醒线程。
             * 满足者调用 tryMatch 来识别其等待者。
             * 等待者阻塞，直到它们被匹配。
             *
             * @param s 要匹配的节点
             * @return 如果成功匹配到 s，返回 true
             */
            boolean tryMatch(SNode s) {
                if (match == null &&
                        UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // 等待者最多需要一个 unpark 操作
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                return match == s;
            }

            /**
             * 尝试通过将节点与自身匹配来取消等待。
             */
            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            boolean isCancelled() {
                return match == this;
            }

            // Unsafe 机制
            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** 堆栈的头节点 */
        volatile SNode head;

        boolean casHead(SNode h, SNode nh) {
            return h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }

        /**
         * 创建或重置节点的字段。仅在 transfer 方法中调用，该方法懒惰地创建节点，并在可能的情况下重用节点，
         * 以帮助减少读取 head 和 CAS 操作之间的间隔，并避免由于 CAS 操作在争用情况下推送节点失败时导致垃圾激增。
         */
        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) s = new SNode(e);
            s.mode = mode;
            s.next = next;
            return s;
        }

        /**
         * 放置或获取一个项目。
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /*
             * 基本算法是循环尝试以下三种操作之一：
             *
             * 1. 如果队列为空或已包含相同模式的节点，尝试将节点推入堆栈并等待匹配，返回它，或者如果取消则返回 null。
             *
             * 2. 如果队列中已包含互补模式的节点，尝试推入一个满足节点到堆栈中，与对应的等待节点匹配，
             *    从堆栈中弹出两个节点，并返回匹配的项目。由于其他线程执行的操作 3，有时匹配或解除链操作可能是不必要的。
             *
             * 3. 如果堆栈顶部已经持有另一个满足节点，帮助它完成匹配和/或弹出操作，然后继续。帮助的代码本质上与满足的代码相同，
             *    除了它不返回项目。
             */
            SNode s = null; // 根据需要构造/重用
            int mode = (e == null) ? REQUEST : DATA;

            for (;;) {
                SNode h = head;
                if (h == null || h.mode == mode) {  // 为空或相同模式
                    if (timed && nanos <= 0) {      // 无法等待
                        if (h != null && h.isCancelled())
                            casHead(h, h.next);     // 弹出已取消的节点
                        else
                            return null;
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        SNode m = awaitFulfill(s, timed, nanos);
                        if (m == s) {               // 等待已取消
                            clean(s);
                            return null;
                        }
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);     // 帮助 s 的满足节点
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) { // 尝试满足
                    if (h.isCancelled())            // 已取消
                        casHead(h, h.next);         // 弹出并重试
                    else if (casHead(h, s = snode(s, e, h, FULFILLING | mode))) {
                        for (;;) { // 循环直到匹配或等待者消失
                            SNode m = s.next;       // m 是 s 的匹配节点
                            if (m == null) {        // 所有等待者都消失了
                                casHead(s, null);   // 弹出满足节点
                                s = null;           // 下次使用新节点
                                break;              // 重新开始主循环
                            }
                            SNode mn = m.next;
                            if (m.tryMatch(s)) {
                                casHead(s, mn);     // 弹出 s 和 m
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // 匹配失败
                                s.casNext(m, mn);   // 帮助取消链接
                        }
                    }
                } else {                            // 帮助一个满足者
                    SNode m = h.next;               // m 是 h 的匹配节点
                    if (m == null)                  // 等待者消失了
                        casHead(h, null);           // 弹出满足节点
                    else {
                        SNode mn = m.next;
                        if (m.tryMatch(h))          // 帮助匹配
                            casHead(h, mn);         // 弹出 h 和 m
                        else                        // 匹配失败
                            h.casNext(m, mn);       // 帮助取消链接
                    }
                }
            }
        }

        /**
         * 自旋/阻塞，直到节点 s 被满足操作匹配。
         *
         * @param s 等待的节点
         * @param timed 如果为 true 则为定时等待
         * @param nanos 超时时间
         * @return 匹配的节点，或如果取消则为 s
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            /*
             * 当一个节点/线程即将阻塞时，它设置其 waiter 字段，然后再至少一次重新检查状态，
             * 在实际 park 之前，这样可以覆盖满足者注意到 waiter 非 null 的情况，
             * 以便唤醒等待者。
             *
             * 当被调用的节点在调用点看起来位于堆栈顶部时，自旋会在 park 之前进行，以避免在生产者和消费者到达时间非常接近时阻塞。
             * 这只会在多处理器上发生得足够多，以至于造成困扰。
             *
             * 主循环返回检查顺序反映了中断优先于正常返回，而正常返回又优先于超时的事实。
             * （因此，在超时情况下，在放弃之前会进行最后一次匹配检查。）除了来自未定时的 SynchronousQueue.{poll/offer} 的调用，
             * 它们不检查中断，也不等待，因此在 transfer 方法中被捕获，而不是调用 awaitFulfill。
             */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = (shouldSpin(s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted())
                    s.tryCancel();
                SNode m = s.match;
                if (m != null)
                    return m;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0)
                    spins = shouldSpin(s) ? (spins - 1) : 0;
                else if (s.waiter == null)
                    s.waiter = w; // 确保 waiter 已设置，下一次迭代时可以 park
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 返回 true 如果节点 s 位于堆栈顶部或存在一个活动的满足者。
         */
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }

        /**
         * 将 s 从堆栈中取消链接。
         */
        void clean(SNode s) {
            s.item = null;   // 忘记元素
            s.waiter = null; // 忘记线程

            /*
             * 在最坏的情况下，我们可能需要遍历整个堆栈以取消链接 s。如果有多个并发调用 clean，
             * 我们可能看不到 s 如果其他线程已经将其删除。但我们可以在看到任何已知跟随 s 的节点时停止。
             * 我们使用 s.next，除非它也已取消，在这种情况下我们尝试跳过一个节点。
             * 我们不进一步检查，因为我们不想为了找到哨兵节点而重复遍历。
             */

            SNode past = s.next;
            if (past != null && past.isCancelled())
                past = past.next;

            // 吸收取消的头节点
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled())
                casHead(p, p.next);

            // 取消嵌入的节点链接
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled())
                    p.casNext(n, n.next);
                else
                    p = n;
            }
        }

        // Unsafe 机制
        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
    /** 双队列 */
    static final class TransferQueue<E> extends Transferer<E> {
        /*
         * 这个类继承了 Scherer-Scott 双队列算法，不同之处在于使用节点内部的模式而不是标记指针。
         * 这个算法比堆栈的实现更简单，因为满足者不需要显式的节点，匹配通过 CAS 操作在 QNode.item 字段上完成。
         */

        /** TransferQueue 的节点类 */
        static final class QNode {
            volatile QNode next;          // 队列中的下一个节点
            volatile Object item;         // 通过 CAS 置为 null 或非 null
            volatile Thread waiter;       // 用于控制 park/unpark
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                        UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * 尝试通过将 item 的引用 CAS 为自身来取消。
             */
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            boolean isCancelled() {
                return item == this;
            }

            /**
             * 如果节点已被出列，因为它的 next 指针由于 advanceHead 操作而被遗忘，则返回 true。
             */
            boolean isOffList() {
                return next == this;
            }

            // Unsafe 机制
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** 队列的头节点 */
        transient volatile QNode head;
        /** 队列的尾节点 */
        transient volatile QNode tail;
        /**
         * 引用一个可能尚未从队列中取消链接的取消节点，因为它是被取消时插入的最后一个节点。
         */
        transient volatile QNode cleanMe;

        TransferQueue() {
            QNode h = new QNode(null, false); // 初始化为虚拟节点
            head = h;
            tail = h;
        }

        /**
         * 尝试将 nh 设置为新的头节点；如果成功，取消旧头节点的 next 引用以避免内存泄漏。
         */
        void advanceHead(QNode h, QNode nh) {
            if (h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh))
                h.next = h; // 忘记旧的 next
        }

        /**
         * 尝试将 nt 设置为新的尾节点。
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
        }

        /**
         * 尝试 CAS 设置 cleanMe 槽位。
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp &&
                    UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        /**
         * 插入或获取一个元素。
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {
            /* 基本算法是循环执行以下两个动作之一：
             *
             * 1. 如果队列明显为空或包含相同模式的节点，尝试将节点加入等待队列，等待被满足（或取消），然后返回匹配的元素。
             *
             * 2. 如果队列明显包含等待的元素，并且调用是互补模式，尝试通过 CAS 操作满足等待节点的 item 字段，然后返回匹配的元素。
             *
             * 在每个情况下，沿途检查并帮助推进队列头尾，以帮助其他阻塞/缓慢的线程。
             */
            QNode s = null; // 根据需要构造/重用
            boolean isData = (e != null);

            for (;;) {
                QNode t = tail;
                QNode h = head;
                if (t == null || h == null)         // 检测未初始化的值
                    continue;                       // 自旋

                if (h == t || t.isData == isData) { // 空或相同模式
                    QNode tn = t.next;
                    if (t != tail)                  // 读不一致
                        continue;
                    if (tn != null) {               // 尾节点滞后
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)        // 无法等待
                        return null;
                    if (s == null)
                        s = new QNode(e, isData);
                    if (!t.casNext(null, s))        // 链接失败
                        continue;

                    advanceTail(t, s);              // 推进尾节点并等待
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) {                   // 等待被取消
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {           // 未取消链接
                        advanceHead(t, s);          // 如果是头节点则取消链接
                        if (x != null)              // 忘记字段
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;

                } else {                            // 互补模式
                    QNode m = h.next;               // 满足节点
                    if (t != tail || m == null || h != head)
                        continue;                   // 读不一致

                    Object x = m.item;
                    if (isData == (x != null) ||    // 节点已被满足
                            x == m ||                   // 节点已取消
                            !m.casItem(x, e)) {         // CAS 失败
                        advanceHead(h, m);          // 出队并重试
                        continue;
                    }

                    advanceHead(h, m);              // 成功满足
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;
                }
            }
        }

        /**
         * 自旋/阻塞直到节点 s 被满足。
         *
         * @param s 等待的节点
         * @param e 用于检查匹配的比较值
         * @param timed 如果为 true 则定时等待
         * @param nanos 超时时间
         * @return 匹配的元素，或取消时返回 s
         */
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            /* 同样的思想如 TransferStack.awaitFulfill */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted())
                    s.tryCancel(e);
                Object x = s.item;
                if (x != e)
                    return x;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0)
                    --spins;
                else if (s.waiter == null)
                    s.waiter = w;
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 删除已取消的节点 s，并使用初始前驱 pred。
         */
        void clean(QNode pred, QNode s) {
            s.waiter = null; // 忘记线程
            /*
             * 在任何给定时刻，只有一个节点无法被删除——即最后插入的节点。为此，
             * 如果无法删除 s，则保存其前驱为 "cleanMe"，并首先删除先前保存的节点。
             * s 或先前保存的节点中至少一个总能被删除，因此这总是能终止。
             */
            while (pred.next == s) { // 如果已经取消链接，则提前返回
                QNode h = head;
                QNode hn = h.next;   // 吸收已取消的第一个节点作为头节点
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // 确保尾节点的一致读取
                if (t == h)
                    return;
                QNode tn = t.next;
                if (t != tail)
                    continue;
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {        // 如果不是尾节点，尝试取消链接
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                QNode dp = cleanMe;
                if (dp != null) {    // 尝试取消链接之前取消的节点
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d 不见了
                            d == dp ||                 // d 离队列了
                            !d.isCancelled() ||        // d 未取消
                            (d != t &&                 // d 不是尾节点
                                    (dn = d.next) != null &&  // 有后继节点
                                    dn != d &&                // 后继在队列中
                                    dp.casNext(d, dn)))       // d 取消链接
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s 已经是保存的节点
                } else if (casCleanMe(null, pred))
                    return;          // 延迟清理 s
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * transferer 是双栈或双队列，取决于构造时的公平性设置。
     * 该字段在构造函数中设置，但不能声明为 final，避免复杂化序列化过程。
     * 由于每个公共方法最多只能访问一次它，使用 volatile 而非 final 并不会显著影响性能。
     */
    private transient volatile Transferer<E> transferer;

    /**
     * 创建一个具有非公平访问策略的 {@code SynchronousQueue}。
     */
    public SynchronousQueue() {
        this(false);
    }

    /**
     * 创建一个具有指定公平性策略的 {@code SynchronousQueue}。
     *
     * @param fair 如果为 true，则等待的线程按 FIFO 顺序争用访问权限；否则顺序不确定。
     */
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

    /**
     * 将指定的元素添加到此队列中，如果有必要，将等待另一个线程来接收它。
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    /**
     * 在指定的等待时间内，将指定的元素插入此队列，如果有必要，将等待另一个线程来接收它。
     *
     * @return {@code true} 如果成功，{@code false} 如果在等待时间内没有出现消费者
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null)
            return true;
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    /**
     * 如果有线程正在等待接收此队列中的元素，则插入指定的元素。
     *
     * @param e 要添加的元素
     * @return {@code true} 如果元素已添加到此队列，{@code false} 否则
     * @throws NullPointerException 如果指定的元素为 {@code null}
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        return transferer.transfer(e, true, 0) != null;
    }

    /**
     * 检索并移除此队列的头部，如果有必要，将等待另一个线程插入它。
     *
     * @return 此队列的头部
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    /**
     * 检索并移除此队列的头部，如果有必要，将等待最多指定的等待时间，直到另一个线程插入它。
     *
     * @return 此队列的头部，或者 {@code null} 如果在等待时间之前没有元素出现
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    /**
     * 如果有线程正在插入元素，则检索并移除此队列的头部。
     *
     * @return 此队列的头部，或者 {@code null} 如果没有可用的元素
     */
    public E poll() {
        return transferer.transfer(null, true, 0);
    }

    /**
     * 始终返回 {@code true}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @return {@code true}
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * 始终返回 0。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @return 0
     */
    public int size() {
        return 0;
    }

    /**
     * 始终返回 0。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @return 0
     */
    public int remainingCapacity() {
        return 0;
    }

    /**
     * 什么也不做。
     * {@code SynchronousQueue} 没有内部容量。
     */
    public void clear() {
    }

    /**
     * 始终返回 {@code false}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @param o 元素
     * @return {@code false}
     */
    public boolean contains(Object o) {
        return false;
    }

    /**
     * 始终返回 {@code false}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @param o 要移除的元素
     * @return {@code false}
     */
    public boolean remove(Object o) {
        return false;
    }

    /**
     * 如果给定集合为空则返回 {@code false}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @param c 集合
     * @return {@code false} 除非给定集合为空
     */
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    /**
     * 始终返回 {@code false}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @param c 集合
     * @return {@code false}
     */
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    /**
     * 始终返回 {@code false}。
     * {@code SynchronousQueue} 没有内部容量。
     *
     * @param c 集合
     * @return {@code false}
     */
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    /**
     * 始终返回 {@code null}。
     * {@code SynchronousQueue} 不会返回元素，除非主动等待。
     *
     * @return {@code null}
     */
    public E peek() {
        return null;
    }

    /**
     * 返回一个空迭代器，其中 {@code hasNext} 始终返回 {@code false}。
     *
     * @return 一个空迭代器
     */
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    /**
     * 返回一个空的 spliterator，其中对 {@link java.util.Spliterator#trySplit()} 的调用总是返回 {@code null}。
     *
     * @return 一个空的 spliterator
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    /**
     * 返回一个长度为 0 的数组。
     * @return 长度为 0 的数组
     */
    public Object[] toArray() {
        return new Object[0];
    }

    /**
     * 将指定数组的第 0 个元素设置为 {@code null}（如果数组长度大于 0）并返回它。
     *
     * @param a 数组
     * @return 指定的数组
     * @throws NullPointerException 如果指定的数组为 null
     */
    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /*
     * 为了应对 SynchronousQueue 在 1.5 版本中的序列化策略，我们声明了一些未使用的类和字段，
     * 这些类和字段仅用于支持跨版本的可序列化性。由于这些字段从未被使用，因此只有在此对象
     * 被序列化或反序列化时才会初始化。
     */

    @SuppressWarnings("serial")
    static class WaitQueue implements java.io.Serializable { }
    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }
    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }
    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;

    /**
     * 将此队列保存到流中（即，序列化它）。
     * @param s 流
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        boolean fair = transferer instanceof TransferQueue;
        if (fair) {
            qlock = new ReentrantLock(true);
            waitingProducers = new FifoWaitQueue();
            waitingConsumers = new FifoWaitQueue();
        } else {
            qlock = new ReentrantLock();
            waitingProducers = new LifoWaitQueue();
            waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
    }

    /**
     * 从流中重构此队列（即，反序列化它）。
     * @param s 流
     * @throws ClassNotFoundException 如果找不到序列化对象的类
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (waitingProducers instanceof FifoWaitQueue)
            transferer = new TransferQueue<E>();
        else
            transferer = new TransferStack<E>();
    }

    // Unsafe mechanics
    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // 将异常转换为相应的错误
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }

}




