package org.top.java.concurrent.source.locks;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/13 下午1:03
 */

import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public abstract  class AbstractQueuedSynchronizer
        extends AbstractOwnableSynchronizer
        implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * 创建一个新的AbstractQueuedSynchronizer实例，初始同步状态为0。
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * 等待队列节点类。
     *
     * <p>等待队列是“CLH”（Craig, Landin 和 Hagersten）锁队列的变体。CLH 锁通常用于自旋锁。
     * 我们使用它们来实现阻塞同步器，但使用相同的基本策略，将有关线程的某些控制信息保存在其节点的前继节点中。
     * 每个节点的“状态”字段用于跟踪线程是否应该阻塞。节点在其前继节点释放时被唤醒。队列中的每个节点
     * 都作为一种特定通知风格的监视器，持有一个等待线程。状态字段并不决定线程是否获取锁等资源。
     * 一个线程只有在队列的第一个时才可以尝试获取锁。但即使是队列头的线程，也并不保证一定能够成功；
     * 它仅有权利进行争夺。因此当前被释放的争夺线程可能需要再次等待。
     *
     * <p>要将节点加入CLH锁队列中，您只需以原子方式将其拼接为新的尾节点。要出队，只需设置头节点即可。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>插入CLH队列只需要对“tail”进行一次原子操作，因此从未排队到排队的状态转换只需要一个简单的原子操作。
     * 同样，出队操作仅仅是更新“head”。然而，节点在确定其继任者时需要做一些额外的工作，特别是要处理
     * 由于超时和中断引起的取消。
     *
     * <p>“prev”链接（在原始CLH锁中未使用）主要用于处理取消。如果某个节点被取消，其后继节点（通常）会重新链接到
     * 一个未取消的前继节点。有关类似机制在自旋锁中的解释，请参见Scott和Scherer的论文。
     *
     * <p>我们还使用“next”链接来实现阻塞机制。每个节点的线程ID保存在其自己的节点中，因此前继节点通过遍历“next”
     * 链接来确定唤醒哪个线程。确定后继节点时必须避免与新入队的节点发生竞争，而这些节点正在为其前继节点设置“next”字段。
     * 当一个节点的“next”字段似乎为空时，我们通过从原子更新的“tail”向后检查来解决这一问题。
     * （换句话说，“next”链接是一种优化，因此我们通常不需要进行向后的扫描。）
     *
     * <p>取消引入了一些保守的算法。由于我们必须轮询其他节点是否被取消，因此可能会错过某个被取消的节点是
     * 在我们之前还是之后。我们通过在取消时总是唤醒后继节点来处理这一问题，允许它们在新前继节点上稳定下来，除非我们能够
     * 识别出一个未取消的前继节点来承担这个责任。
     *
     * <p>CLH队列需要一个虚拟的头节点才能启动。但我们不会在构造时创建它，因为如果没有争用，它将是浪费的。
     * 相反，节点是在首次争用时构造的，并且在第一次争用时设置头和尾指针。
     *
     * <p>等待条件的线程使用相同的节点，但会使用一个额外的链接。条件队列只需要简单的（非并发）链式队列，
     * 因为它们只在独占模式下被访问。在等待时，节点被插入条件队列中。在信号触发时，该节点被转移到主队列。
     * 特殊的状态字段值用于标记节点位于哪个队列中。
     *
     * <p>感谢 Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer 和 Michael Scott 以及 JSR-166 专家组成员
     * 对此类设计的有用想法、讨论和批评。
     */
    static final class Node {
        /** 标记一个节点正在共享模式下等待 */
        static final Node SHARED = new Node();
        /** 标记一个节点正在独占模式下等待 */
        static final Node EXCLUSIVE = null;

        /** 表示线程已经取消的等待状态 */
        static final int CANCELLED =  1;
        /** 表示后继节点的线程需要被唤醒 */
        static final int SIGNAL    = -1;
        /** 表示线程正在条件上等待 */
        static final int CONDITION = -2;
        /**
         * 表示下一个acquireShared操作应无条件传播
         */
        static final int PROPAGATE = -3;

        /**
         * 状态字段，可能的值为：
         *   SIGNAL:     当前节点的后继节点被阻塞，因此当前节点必须在释放或取消时唤醒其后继节点。
         *   CANCELLED:  该节点由于超时或中断而被取消。取消的节点永远不会再次阻塞。
         *   CONDITION:  当前节点在条件队列中，直到被转移到同步队列。
         *   PROPAGATE:  表示共享模式的释放应传播到其他节点。
         *   0:          没有上述状态。
         *
         * 值的数值排列简化了使用。非负值表示节点不需要发送信号。因此，大多数代码只需检查符号而不是特定的值。
         *
         * 状态字段的初始值为 0，用于正常的同步节点；对于条件节点，初始值为 CONDITION。该字段通过CAS或
         * 无条件的volatile写操作进行修改。
         */
        volatile int waitStatus;

        /**
         * 链接到前驱节点，当前节点/线程依赖前驱节点的waitStatus进行检查。此字段在入队时分配，并且只在出队时置空
         * （为了垃圾收集）。另外，在取消前驱节点时，我们会短路以找到一个未取消的节点，这将始终存在，因为头节点
         * 永远不会被取消：一个节点只有成功获取资源时才会成为头节点。被取消的线程永远不会成功获取资源，
         * 并且线程只会取消自己，而不会取消其他节点。
         */
        volatile Node prev;

        /**
         * 链接到后继节点，当前节点/线程在释放时会唤醒后继节点。此字段在入队时分配，在跳过取消的前驱节点时进行调整，
         * 并且在出队时置空（为了垃圾收集）。入队操作不会为前驱节点分配next字段，直到完成附加操作，
         * 因此看到空的next字段并不一定意味着该节点在队列末尾。然而，如果next字段为空，我们可以从尾部向前扫描
         * 进行双重检查。取消的节点的next字段被设置为指向节点本身而不是null，以简化isOnSyncQueue方法的操作。
         */
        volatile Node next;

        /**
         * 当前节点的线程。在构造时初始化，使用后置空。
         */
        volatile Thread thread;

        /**
         * 链接到下一个等待条件的节点，或特殊值 SHARED。由于条件队列只在独占模式下访问，
         * 我们只需要一个简单的链表来保存节点，直到它们等待条件时被转移到同步队列中。
         * 并且由于条件只能是独占的，我们通过使用特殊值来表示共享模式来节省一个字段。
         */
        Node nextWaiter;

        /**
         * 返回节点是否在共享模式下等待。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回前驱节点，如果为空则抛出NullPointerException。使用时前驱节点不能为null。
         *
         * @return 当前节点的前驱节点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // 用于创建初始的头节点或共享模式标记节点
        }

        Node(Thread thread, Node mode) {     // 用于addWaiter方法
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // 用于Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 等待队列的头节点，懒初始化。除了初始化，它只通过setHead方法修改。注意：如果头节点存在，
     * 则其waitStatus保证不会是CANCELLED。
     */
    private transient volatile Node head;

    /**
     * 等待队列的尾节点，懒初始化。仅通过enq方法添加新节点时修改。
     */
    private transient volatile Node tail;

    /**
     * 同步状态。
     */
    private volatile int state;

    /**
     * 返回当前的同步状态值。
     * 此操作具有 volatile 读取的内存语义。
     *
     * @return 当前状态值
     */
    protected final int getState() {
        return state;
    }

    /**
     * 设置同步状态的值。
     * 此操作具有 volatile 写入的内存语义。
     *
     * @param newState 新的状态值
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 如果当前状态值等于预期值，则原子性地设置同步状态为给定的更新值。
     * 此操作具有 volatile 读和写的内存语义。
     *
     * @param expect 预期的值
     * @param update 新值
     * @return 如果成功则返回true。返回false表示实际值与预期值不相等。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // 下面是支持此功能的内部机制设置
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // 队列操作的工具方法

    /**
     * 对于短暂超时，轮询操作比使用计时的park操作更快。
     * 粗略估计足以提高短时超时情况下的响应速度。
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 将节点插入队列中，如果有必要，则初始化队列。见上图。
     * @param node 要插入的节点
     * @return 节点的前驱节点
     */
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // 必须初始化
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * 为当前线程和给定模式创建并入队节点。
     *
     * @param mode Node.EXCLUSIVE 表示独占模式，Node.SHARED 表示共享模式
     * @return 新创建的节点
     */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // 尝试快速入队，如果失败则回退到完整的入队操作
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    /**
     * 将队列的头设置为节点，并使其出队。仅由acquire方法调用。
     * 同时置空不再使用的字段，以便进行垃圾回收并抑制不必要的信号和遍历。
     *
     * @param node 要设置为头的节点
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 唤醒指定节点的后继节点（如果存在）。
     *
     * @param node 要唤醒其后继节点的节点
     */
    private void unparkSuccessor(Node node) {
        /*
         * 如果状态是负数（即可能需要发送信号），则尝试清除，以便为发送信号做准备。
         * 如果此操作失败或状态被等待线程更改也没关系。
         */
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * 后继节点存储在next字段中，通常是下一个节点。
         * 但如果该节点已取消或似乎为空，则从尾部向前遍历以找到实际的非取消后继节点。
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * 共享模式的释放操作——唤醒后继节点并确保传播。
     * （注意：独占模式的释放操作只需调用unparkSuccessor即可）
     */
    private void doReleaseShared() {
        /*
         * 确保释放操作能够传播，即使有其他正在进行的获取/释放操作。
         * 这通常通过尝试唤醒头节点的后继节点来进行，如果后继节点需要信号。
         * 如果不需要信号，则将状态设置为PROPAGATE以确保释放时传播继续。
         * 另外，如果在我们执行此操作时有新节点加入队列，也必须继续循环。
         */
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // 重新检查其他情况
                    unparkSuccessor(h);
                } else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // 如果CAS操作失败则继续循环
            }
            if (h == head)                   // 如果头节点改变则继续循环
                break;
        }
    }

    /**
     * 设置队列的头节点，并检查其后继节点是否在共享模式下等待。
     * 如果是这样，根据 propagate 值或 PROPAGATE 状态决定是否传播。
     *
     * @param node 当前节点
     * @param propagate 由 tryAcquireShared 返回的值
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // 记录旧的头节点以便后续检查
        setHead(node);
        /*
         * 尝试唤醒下一个排队的节点，如果：
         *   propagate 被调用者指示，或
         *   通过之前或之后的 setHead 操作记录了 PROPAGATE 状态
         *   （注意：此处使用状态检查，因为 PROPAGATE 状态可能会转换为 SIGNAL。）
         *   并且
         *   下一个节点正在共享模式下等待，或者我们不确定，因为它似乎为空
         *
         * 这两项检查中的保守性可能导致不必要的唤醒，但仅在有多个并发获取/释放时发生。
         * 因此大多数情况需要立即或很快唤醒。
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // 各种获取操作的工具方法

    /**
     * 取消正在进行的获取操作。
     *
     * @param node 正在获取的节点
     */
    private void cancelAcquire(Node node) {
        // 如果节点不存在则忽略
        if (node == null)
            return;

        node.thread = null;

        // 跳过已取消的前驱节点
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext 是显而易见的要从链中解开的节点。
        // 如果失败，则表明我们在与其他取消或唤醒操作竞争，因此无需进一步操作。
        Node predNext = pred.next;

        // 可以使用无条件写入代替 CAS 操作。在此原子步骤之后，其他节点可以跳过我们。
        node.waitStatus = Node.CANCELLED;

        // 如果我们是队列的尾节点，则移除自身。
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // 如果后继节点需要信号，尝试设置 pred 的 next 链接，以便它接收到信号。
            // 否则将其唤醒以继续操作。
            int ws;
            if (pred != head &&
                    ((ws = pred.waitStatus) == Node.SIGNAL ||
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                    pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // 帮助 GC 回收
        }
    }

    /**
     * 检查并更新未能获取的节点的状态。
     * 如果线程应该阻塞，则返回 true。这是所有获取循环中的主要信号控制。
     * 需要 pred == node.prev。
     *
     * @param pred 当前节点的前驱节点
     * @param node 当前节点
     * @return 如果线程应该阻塞，则返回 true
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * 该节点已设置状态，要求前驱节点唤醒它，因此可以安全地阻塞。
             */
            return true;
        if (ws > 0) {
            /*
             * 前驱节点已取消。跳过取消的前驱节点并返回重试标志。
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus 必须为 0 或 PROPAGATE。指示需要信号，但尚未阻塞。
             * 调用方将需要在阻塞前重试。
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * 中断当前线程的便捷方法。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 便捷方法，用于阻塞并检查是否中断。
     *
     * @return 如果被中断则返回 true
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * 各种获取操作的实现，区分独占/共享以及中断模式等。每种方法大同小异，但在异常处理和其他控制
     * 方面有细微差别。为了性能，代码结构进行了简化。
     */

    /**
     * 以不可中断的独占模式获取队列中线程的资源。用于条件等待方法和获取资源的方法。
     *
     * @param node 正在获取资源的节点
     * @param arg 获取资源的参数
     * @return {@code true} 如果等待过程中线程被中断
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // 帮助垃圾回收
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    /**
     * 以可中断的独占模式获取资源。
     * @param arg 获取资源的参数
     */
    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // 帮助垃圾回收
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    /**
     * 以定时的独占模式获取资源。
     *
     * @param arg 获取资源的参数
     * @param nanosTimeout 最大等待时间
     * @return {@code true} 如果获取成功
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // 帮助垃圾回收
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    /**
     * 以不可中断的共享模式获取资源。
     * @param arg 获取资源的参数
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // 帮助垃圾回收
                        if (interrupted) {
                            selfInterrupt();
                        }
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    /**
     * 以可中断的共享模式获取资源。
     * @param arg 获取资源的参数
     */
    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // 帮助垃圾回收
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    /**
     * 以定时的共享模式获取资源。
     *
     * @param arg 获取资源的参数
     * @param nanosTimeout 最大等待时间
     * @return {@code true} 如果获取成功
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // 帮助垃圾回收
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    // 主要的导出方法

    /**
     * 尝试以独占模式获取资源。此方法应查询对象的状态是否允许以独占模式获取资源，如果允许则获取资源。
     *
     * <p>此方法总是由执行获取操作的线程调用。如果此方法报告失败，获取操作可能会将线程排队，直到某个线程释放资源并唤醒它。
     * 这个可以用来实现 {@link Lock#tryLock()} 方法。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}。
     *
     * @param arg 获取资源的参数。此值总是传递给获取方法，或是在进入条件等待时保存的值。此值可以表示任何内容，随你定义。
     * @return {@code true} 如果获取成功。当成功时，该对象已被获取。
     * @throws IllegalMonitorStateException 如果获取操作将此同步器置于非法状态。为了使同步正确运行，必须一致地抛出此异常。
     * @throws UnsupportedOperationException 如果独占模式不支持
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试设置状态以反映独占模式下的释放。
     *
     * <p>此方法总是由执行释放操作的线程调用。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}。
     *
     * @param arg 释放资源的参数。此值总是传递给释放方法，或是在进入条件等待时当前的状态值。此值可以表示任何内容，随你定义。
     * @return {@code true} 如果此对象现在完全释放，等待的线程可以尝试获取资源；否则返回 {@code false}。
     * @throws IllegalMonitorStateException 如果释放操作将同步器置于非法状态。为了使同步正确运行，必须一致地抛出此异常。
     * @throws UnsupportedOperationException 如果独占模式不支持
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试以共享模式获取资源。此方法应查询对象的状态是否允许以共享模式获取资源，如果允许则获取资源。
     *
     * <p>此方法总是由执行获取操作的线程调用。如果此方法报告失败，获取操作可能会将线程排队，直到某个线程释放资源并唤醒它。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}。
     *
     * @param arg 获取资源的参数。此值总是传递给获取方法，或是在进入条件等待时保存的值。此值可以表示任何内容，随你定义。
     * @return 失败时返回负值；如果获取共享模式成功，但不允许后续的共享模式获取，则返回 0；如果获取共享模式成功，并允许后续的共享模式获取，则返回正值。
     * @throws IllegalMonitorStateException 如果获取操作将同步器置于非法状态。为了使同步正确运行，必须一致地抛出此异常。
     * @throws UnsupportedOperationException 如果共享模式不支持
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 尝试设置状态以反映共享模式下的释放。
     *
     * <p>此方法总是由执行释放操作的线程调用。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}。
     *
     * @param arg 释放资源的参数。此值总是传递给释放方法，或是在进入条件
     * 等待时当前的状态值。此值可以表示任何内容，随你定义。
     * @return {@code true} 如果此次共享模式下的释放可能允许等待的获取（共享或独占）成功；否则返回 {@code false}。
     * @throws IllegalMonitorStateException 如果释放操作将同步器置于非法状态。为了使同步正确运行，必须一致地抛出此异常。
     * @throws UnsupportedOperationException 如果共享模式不支持
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 如果当前（调用）线程独占持有同步状态，则返回 {@code true}。此方法在每次调用非等待的 {@link ConditionObject} 方法时被调用。
     * （等待方法改为调用 {@link #release}）。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}。此方法仅在 {@link ConditionObject} 方法内部调用，
     * 所以如果不使用条件，可能不需要定义此方法。
     *
     * @return 如果同步状态是独占持有的，则返回 {@code true}；否则返回 {@code false}
     * @throws UnsupportedOperationException 如果条件不支持
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 以不可中断的独占模式获取资源。通过至少一次调用 {@link #tryAcquire} 来实现，并在成功时返回。否则，线程将排队，
     * 可能反复阻塞和解除阻塞，直到 {@link #tryAcquire} 成功。此方法可以用来实现 {@link Lock#lock}。
     *
     * @param arg 获取资源的参数。此值传递给 {@link #tryAcquire}，但除此之外没有其他解释，可以表示任何内容。
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            selfInterrupt();
        }
    }

    /**
     * 以可中断的独占模式获取资源。首先检查中断状态，然后至少调用一次 {@link #tryAcquire}，在成功时返回。否则，线程将排队，
     * 可能反复阻塞和解除阻塞，直到 {@link #tryAcquire} 成功或线程被中断。此方法可以用来实现 {@link Lock#lockInterruptibly}。
     *
     * @param arg 获取资源的参数。此值传递给 {@link #tryAcquire}，但除此之外没有其他解释，可以表示任何内容。
     * @throws InterruptedException 如果当前线程被中断
     */
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
    }

    /**
     * 尝试以独占模式获取资源，超时并中止。如果成功返回 {@code true}，否则在给定的超时时间内返回失败。
     * 首先检查中断状态，然后至少调用一次 {@link #tryAcquire}，在成功时返回。否则，线程将排队，
     * 可能反复阻塞和解除阻塞，直到 {@link #tryAcquire} 成功、线程被中断或超时到期。
     * 此方法可以用来实现 {@link Lock#tryLock(long, TimeUnit)}。
     *
     * @param arg 获取资源的参数。此值传递给 {@link #tryAcquire}，但除此之外没有其他解释，可以表示任何内容。
     * @param nanosTimeout 最大等待时间，单位为纳秒
     * @return {@code true} 如果获取成功；{@code false} 如果超时
     * @throws InterruptedException 如果当前线程被中断
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 在独占模式下释放资源。通过在 {@link #tryRelease} 返回 {@code true} 时解除阻塞一个或多个线程来实现。
     * 此方法可以用来实现 {@link Lock#unlock}。
     *
     * @param arg 释放资源的参数。此值传递给 {@link #tryRelease}，但除此之外没有其他解释，可以表示任何内容。
     * @return {@link #tryRelease} 返回的值
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0) {
                unparkSuccessor(h);
            }
            return true;
        }
        return false;
    }

    /**
     * 以不可中断的共享模式获取资源。首先调用至少一次 {@link #tryAcquireShared}，在成功时返回。否则，线程将排队，
     * 可能反复阻塞和解除阻塞，直到 {@link #tryAcquireShared} 成功。
     *
     * @param arg 获取资源的参数。此值传递给 {@link #tryAcquireShared}，但除此之外没有其他解释，可以表示任何内容。
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) {
            doAcquireShared(arg);
        }
    }

    /**
     * 以可中断的共享模式获取资源。首先检查中断状态，然后至少调用一次 {@link #tryAcquireShared}，在成功时返回。
     * 否则，线程将排队，可能反复阻塞和解除阻塞，直到 {@link #tryAcquireShared} 成功或线程被中断。
     * @param arg 获取资源的参数。
     * 此值传递给 {@link #tryAcquireShared}，但除此之外没有其他解释，可以表示任何内容。
     * @throws InterruptedException 如果当前线程被中断
     */
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
    }

    /**
     * 尝试以共享模式获取资源，超时并中止。如果成功返回 {@code true}，否则在给定的超时时间内返回失败。
     * 首先检查中断状态，然后至少调用一次 {@link #tryAcquireShared}，在成功时返回。否则，线程将排队，
     * 可能反复阻塞和解除阻塞，直到 {@link #tryAcquireShared} 成功、线程被中断或超时到期。
     *
     * @param arg 获取资源的参数。此值传递给 {@link #tryAcquireShared}，但除此之外没有其他解释，可以表示任何内容。
     * @param nanosTimeout 最大等待时间，单位为纳秒
     * @return {@code true} 如果获取成功；{@code false} 如果超时
     * @throws InterruptedException 如果当前线程被中断
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * 在共享模式下释放资源。通过在 {@link #tryReleaseShared} 返回 {@code true} 时解除阻塞一个或多个线程来实现。
     *
     * @param arg 释放资源的参数。此值传递给 {@link #tryReleaseShared}，但除此之外没有其他解释，可以表示任何内容。
     * @return {@link #tryReleaseShared} 返回的值
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }


    // 队列检查方法

    /**
     * 查询是否有线程正在等待获取。由于中断和超时可能随时发生，
     * `true` 的返回值并不保证其他线程会获取成功。
     *
     * @return 如果可能有其他线程在等待获取，则返回 true
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * 查询是否有线程曾试图获取该同步器（即，获取方法是否曾经阻塞过）。
     *
     * @return 如果曾经有过争用，则返回 true
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * 返回队列中的第一个（等待时间最长的）线程，如果当前没有线程排队，则返回 null。
     *
     * @return 队列中等待时间最长的线程，或 null 如果队列为空
     */
    public final Thread getFirstQueuedThread() {
        // 仅处理快速路径的情况，否则执行完整的查询
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * 当快速路径失败时调用的 `getFirstQueuedThread` 版本。
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * 第一个节点通常是 head.next。尝试获取其线程字段，确保读取一致：
         * 如果线程字段为空或者 s.prev 不再是 head，则说明在某些读取操作之间，
         * 其他线程执行了 setHead。我们尝试两次，然后再进行遍历。
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * head.next 可能尚未设置，或者在设置之后又被重置。
         * 因此，我们必须检查 tail 是否实际是第一个节点。
         * 如果不是，我们继续安全地从 tail 向后遍历到 head，确保找到第一个节点。
         */
        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * 返回 `true`，如果给定线程当前在队列中等待。
     *
     * 此实现遍历队列以确定给定线程的存在。
     *
     * @param thread 要检查的线程
     * @return 如果给定线程在队列中，则返回 true
     * @throws NullPointerException 如果线程为空
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * 如果显然第一个排队的线程是以独占模式等待，则返回 `true`。
     * 如果此方法返回 `true`，并且当前线程尝试以共享模式获取，
     * 则保证当前线程不是第一个排队的线程。
     * 仅在 `ReentrantReadWriteLock` 中作为启发式方法使用。
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
                (s = h.next) != null &&
                !s.isShared() &&
                s.thread != null;
    }

    /**
     * 查询是否有其他线程比当前线程等待获取时间更长。
     *
     * 调用此方法等价于（但可能比以下操作更有效）：
     *
     * `getFirstQueuedThread() != Thread.currentThread() && hasQueuedThreads()`
     *
     * 该方法设计用于公平同步器，以避免插队现象。
     * 如果该方法返回 `true`，则同步器的 `tryAcquire` 方法应返回 `false`，
     * 而 `tryAcquireShared` 方法应返回负值。
     *
     * @return 如果当前有排队线程在当前线程之前，则返回 `true`；
     *         如果当前线程是队列头或队列为空，则返回 `false`
     */
    public final boolean hasQueuedPredecessors() {
        // 此检查的正确性取决于 head 在 tail 之前初始化，以及 head.next 是准确的
        Node t = tail; // 以逆初始化顺序读取字段
        Node h = head;
        Node s;
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }

    // 仪表化和监控方法

    /**
     * 返回正在等待获取的线程数的估计值。这个值只是一个估计，因为线程数在此方法遍历内部数据结构时可能会动态变化。
     * 该方法用于监控系统状态，而不是用于同步控制。
     *
     * @return 等待获取的线程数的估计值
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * 返回一个包含可能在等待获取的线程的集合。由于在构造此结果时线程的实际集合可能会动态变化，因此返回的集合只是一个尽力而为的估计。
     * 返回的集合中的元素没有特定顺序。此方法旨在便于构建提供更广泛监控功能的子类。
     *
     * @return 包含线程的集合
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * 返回一个包含可能在独占模式下等待获取的线程的集合。
     * 它与 {@link #getQueuedThreads} 的属性相同，但仅返回由于独占获取而等待的线程。
     *
     * @return 包含线程的集合
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 返回一个包含可能在共享模式下等待获取的线程的集合。
     * 它与 {@link #getQueuedThreads} 的属性相同，但仅返回由于共享获取而等待的线程。
     *
     * @return 包含线程的集合
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 返回一个标识该同步器及其状态的字符串。状态在方括号中，包括字符串 {@code "State ="} 后跟 {@link #getState} 的当前值，
     * 以及 {@code "nonempty"} 或 {@code "empty"}，具体取决于队列是否为空。
     *
     * @return 标识该同步器及其状态的字符串
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() + "[State = " + s + ", " + q + "empty queue]";
    }


    // 条件的内部支持方法

    /**
     * 如果一个节点最初放置在条件队列中，并且现在正在同步队列上等待重新获取，则返回 true。
     *
     * @param node 节点
     * @return 如果正在重新获取，则返回 true
     */
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // 如果有后继节点，则必须在队列上
            return true;
        /*
         * node.prev 可以是非空的，但由于 CAS 操作可能失败，它还没有进入队列。
         * 因此，我们必须从队尾开始遍历以确保它实际进入了队列。
         * 在此方法的调用中，它将始终接近队尾，除非 CAS 失败（这不太可能发生），它将位于队列中，因此我们几乎不需要遍历太多。
         */
        return findNodeFromTail(node);
    }

    /**
     * 通过从队尾向后搜索，返回节点是否在同步队列中。
     * 仅在 isOnSyncQueue 需要时调用。
     *
     * @param node 节点
     * @return 如果节点存在于同步队列中则返回 true
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * 将一个节点从条件队列转移到同步队列。如果成功，返回 true。
     *
     * @param node 节点
     * @return 如果成功转移（否则在信号发送前节点被取消），则返回 true
     */
    final boolean transferForSignal(Node node) {
        /*
         * 如果无法更改 waitStatus，则该节点已被取消。
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * 拼接到队列中，并尝试将前驱节点的 waitStatus 设置为表明线程可能正在等待。
         * 如果取消或者尝试设置 waitStatus 失败，则唤醒以重新同步（在这种情况下，waitStatus 可能会暂时错误但无害）。
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * 如果有必要，在取消等待后将节点转移到同步队列。 如果在线程被信号唤醒之前取消了，则返回 true。
     *
     * @param node 节点
     * @return 如果在节点收到信号前取消了，则返回 true
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * 如果我们在 signal() 操作中失败了，那么我们在它完成拼接操作之前不能继续。
         * 在未完成转移时进行取消既罕见又短暂，因此只需自旋即可。
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * 调用 release 操作并传入当前状态值；返回保存的状态。
     * 如果失败，则取消节点并抛出异常。
     *
     * @param node 等待此条件的节点
     * @return 之前的同步状态
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }


    // 条件相关的方法

    /**
     * 返回给定的 ConditionObject 是否使用了此同步器作为其锁。
     *
     * @param condition 条件对象
     * @return 如果是该同步器的拥有者则返回 true
     * @throws NullPointerException 如果条件为空
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * 查询是否有任何线程在给定条件上等待。
     * 请注意，由于中断和超时可能随时发生，返回 `true` 并不保证任何线程会被唤醒。
     * 此方法主要用于监控系统状态。
     *
     * @param condition 条件对象
     * @return 如果有任何线程在等待则返回 `true`
     * @throws IllegalMonitorStateException 如果没有独占同步持有
     * @throws IllegalArgumentException 如果给定的条件不与该同步器关联
     * @throws NullPointerException 如果条件为空
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("不是拥有者");
        return condition.hasWaiters();
    }

    /**
     * 返回等待给定条件的线程数的估计值。
     * 请注意，由于中断和超时可能随时发生，此估计值仅是最大值。
     * 此方法主要用于监控系统状态。
     *
     * @param condition 条件对象
     * @return 等待线程的估计数量
     * @throws IllegalMonitorStateException 如果没有独占同步持有
     * @throws IllegalArgumentException 如果给定的条件不与该同步器关联
     * @throws NullPointerException 如果条件为空
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("不是拥有者");
        return condition.getWaitQueueLength();
    }

/**
 * 返回包含等待给定条件的线程的集合。
 * 由于实际线程集可能
 * 动态变化，因此返回的集合只是尽力而为的估计。返回的集合中元素没有特定的顺序。
 *
 * @param condition 条件对象
 * @return 包含等待该条件的线程的集合
 * @throws IllegalMonitorStateException 如果没有独占同步持有
 * @throws IllegalArgumentException 如果给定的条件不与该同步器关联
 * @throws NullPointerException 如果条件为空
 */
public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
    if (!owns(condition))
        throw new IllegalArgumentException("不是拥有者");
    return condition.getWaitingThreads();
}

    // 条件对象的实现

    /**
     * 用于实现锁的 `AbstractQueuedSynchronizer` 基础的条件对象实现。
     *
     * <p>此类的方法文档描述的是机械性实现，而不是从 `Lock` 和 `Condition` 用户的角度描述的行为规范。
     * 导出的版本通常需要伴随文档，描述依赖于 `AbstractQueuedSynchronizer` 相关语义的条件语义。
     *
     * <p>此类是可序列化的，但所有字段都是瞬时的，因此反序列化的条件不再有任何等待者。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** 条件队列中的第一个等待节点。 */
        private transient Node firstWaiter;
        /** 条件队列中的最后一个等待节点。 */
        private transient Node lastWaiter;

        /**
         * 创建一个新的 `ConditionObject` 实例。
         */
        public ConditionObject() { }

        // 内部方法

        /**
         * 将一个新等待者添加到等待队列。
         * @return 它的新的等待节点
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // 如果 lastWaiter 已被取消，则清理它
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * 删除并传递节点，直到遇到一个非取消节点或空节点。
         * 从 `signal` 中拆分出来，部分目的是为了鼓励编译器内联没有等待者的情况。
         * @param first （非空）条件队列中的第一个节点
         */
        private void doSignal(Node first) {
            do {
                if ((firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }

        /**
         * 删除并传递所有节点。
         * @param first （非空）条件队列中的第一个节点
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        /**
         * 从条件队列中取消所有被取消的等待节点。
         * 仅在持有锁时调用。这个方法在等待时发生取消，或者在插入新的等待者时发现 lastWaiter 被取消时调用。
         * 这个方法是为了防止没有信号的情况下的垃圾保留。因此，即使可能需要完全遍历，它也仅在超时或取消发生时起作用。
         * 它遍历所有节点，而不是在特定目标处停止，目的是解除所有无用节点的链接，而不会在取消风暴期间多次遍历。
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                } else {
                    trail = t;
                }
                t = next;
            }
        }

        // 公共方法

        /**
         * 将等待最久的线程（如果存在）从条件队列移到同步队列中。
         *
         * @throws IllegalMonitorStateException 如果 `isHeldExclusively` 返回 `false`
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

        /**
         * 将所有线程从条件队列移到同步队列中。
         *
         * @throws IllegalMonitorStateException 如果 `isHeldExclusively` 返回 `false`
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * 实现不可中断的条件等待。
         * <ol>
         * <li> 保存锁状态，通过 `getState` 返回。
         * <li> 调用 `release`，将保存的状态作为参数，如果失败则抛出 `IllegalMonitorStateException`。
         * <li> 阻塞，直到收到信号。
         * <li> 通过调用带保存状态参数的 `acquire` 方法重新获取锁。
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * 对于可中断的等待，我们需要跟踪在条件阻塞时被中断的情况下是抛出 InterruptedException，
         * 还是在等待重新获取锁时被中断的情况下重新中断当前线程。
         */

        /** 表示在等待退出时重新中断的模式 */
        private static final int REINTERRUPT =  1;
        /** 表示在等待退出时抛出 InterruptedException 的模式 */
        private static final int THROW_IE    = -1;


        /**
         * 检查是否在等待期间被中断，返回 THROW_IE 如果在信号前中断，REINTERRUPT 如果在信号后中断，或 0 如果未中断。
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * 在等待后根据模式抛出 `InterruptedException`，重新中断当前线程或不做任何处理。
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * 实现中断条件等待。
         * <ol>
         * <li> 如果当前线程被中断，则抛出 `InterruptedException`。
         * <li> 保存锁状态，通过 `getState` 返回。
         * <li> 调用 `release`，将保存的状态作为参数，如果失败则抛出 `IllegalMonitorStateException`。
         * <li> 阻塞，直到收到信号或中断。
         * <li> 通过调用带保存状态参数的 `acquire` 方法重新获取锁。
         * <li> 如果在步骤 4 中被中断，则抛出 `InterruptedException`。
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // 如果取消，清理节点
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        // 更多支持中断、超时等待以及条件的相关方法省略...
        /**
         * 实现定时的条件等待。
         * <ol>
         * <li> 如果当前线程被中断，则抛出 `InterruptedException`。
         * <li> 保存锁状态，通过 `getState` 返回。
         * <li> 调用 `release`，将保存的状态作为参数，如果失败则抛出 `IllegalMonitorStateException`。
         * <li> 阻塞，直到收到信号、中断或超时。
         * <li> 通过调用带保存状态参数的 `acquire` 方法重新获取锁。
         * <li> 如果在步骤 4 中被中断，则抛出 `InterruptedException`。
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * 实现绝对时间的条件等待。
         * <ol>
         * <li> 如果当前线程被中断，则抛出 `InterruptedException`。
         * <li> 保存锁状态，通过 `getState` 返回。
         * <li> 调用 `release`，将保存的状态作为参数，如果失败则抛出 `IllegalMonitorStateException`。
         * <li> 阻塞，直到收到信号、中断或超时。
         * <li> 通过调用带保存状态参数的 `acquire` 方法重新获取锁。
         * <li> 如果在步骤 4 中被中断，则抛出 `InterruptedException`。
         * <li> 如果在步骤 4 中超时，则返回 false，否则返回 true。
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * 实现带超时的条件等待。
         * <ol>
         * <li> 如果当前线程被中断，则抛出 `InterruptedException`。
         * <li> 保存锁状态，通过 `getState` 返回。
         * <li> 调用 `release`，将保存的状态作为参数，如果失败则抛出 `IllegalMonitorStateException`。
         * <li> 阻塞，直到收到信号、中断或超时。
         * <li> 通过调用带保存状态参数的 `acquire` 方法重新获取锁。
         * <li> 如果在步骤 4 中被中断，则抛出 `InterruptedException`。
         * <li> 如果在步骤 4 中超时，则返回 false，否则返回 true。
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        // 支持调试和监视

        /**
         * 如果此条件是由给定的同步器创建的，则返回 true。
         *
         * @param sync 同步器
         * @return true 如果属于同步器
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * 查询是否有线程在等待此条件。
         *
         * @return 如果有任何线程在等待则返回 true
         * @throws IllegalMonitorStateException 如果 `isHeldExclusively` 返回 `false`
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * 返回等待此条件的线程数量的估计值。
         *
         * @return 等待线程的估计数量
         * @throws IllegalMonitorStateException 如果 `isHeldExclusively` 返回 `false`
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * 返回一个包含可能在此条件上等待的线程的集合。
         *
         * @return 线程的集合
         * @throws IllegalMonitorStateException 如果 `isHeldExclusively` 返回 `false`
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    // 此处省略不相关的内部实现细节（如 CAS 操作、Unsafe 操作等）

    /**
     * 用于支持 compareAndSet 操作。我们需要在这里本地实现此功能：
     * 为了支持未来的增强，我们不能显式地继承 `AtomicInteger`，尽管那会很高效且有用。
     * 因此，作为折中方案，我们使用 `hotspot` 内部的 API 来本地实现该功能。
     * 同时，我们还对其他可使用 CAS 的字段进行类似的实现（这些操作本可以通过原子字段更新器来完成）。
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS 操作用于修改队列头部，仅在 enq 方法中使用。
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS 操作用于修改队列尾部，仅在 enq 方法中使用。
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS 操作用于修改节点的 waitStatus 字段。
     */
    private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }

    /**
     * CAS 操作用于修改节点的 next 字段。
     */
    private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }

}





