package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:12
 */

import org.top.java.source.concurrent.atomic.AtomicReference;
import org.top.java.source.concurrent.locks.LockSupport;

import java.util.concurrent.ForkJoinPool;

/**
 * 一个可重用的同步屏障，其功能类似于 {@link java.util.concurrent.CyclicBarrier CyclicBarrier}
 * 和 {@link java.util.concurrent.CountDownLatch CountDownLatch}，但支持更灵活的用法。
 *
 * <p><b>注册。</b> 与其他屏障不同，参与者的数量可以随时间变化。
 * 可以随时注册任务（使用 {@link #register}、{@link #bulkRegister} 或指定初始参与者数量的构造方法），
 * 也可以在任何到达时选择性取消注册（使用 {@link #arriveAndDeregister}）。
 * 与大多数基本同步结构一样，注册和取消注册仅影响内部计数；
 * 它们不会建立任何进一步的内部记录，因此任务不能查询它们是否已注册。
 * （但是，你可以通过继承这个类引入这样的记录。）
 *
 * <p><b>同步。</b> 像 {@code CyclicBarrier} 一样，{@code Phaser} 可以被反复等待。
 * 方法 {@link #arriveAndAwaitAdvance} 的效果类似于 {@link java.util.concurrent.CyclicBarrier#await CyclicBarrier.await}。
 * Phaser 的每一代都有一个相关的阶段号。阶段号从零开始，当所有参与者到达 Phaser 时前进，
 * 达到 {@code Integer.MAX_VALUE} 后重新回到零。
 * 使用阶段号可以通过两类方法独立控制任务到达 Phaser 和等待其他任务的操作，这些方法可由任何注册的参与者调用：
 *
 * <ul>
 *
 *   <li> <b>到达。</b> 方法 {@link #arrive} 和 {@link #arriveAndDeregister} 记录到达。
 *       这些方法不会阻塞，但会返回一个关联的<em>到达阶段号</em>；
 *       即应用于到达的 Phaser 的阶段号。
 *       当某一阶段的最后一个参与者到达时，会执行一个可选操作并推进阶段。
 *       这些操作由触发阶段推进的参与者执行，并且可以通过重写方法 {@link #onAdvance(int, int)} 进行安排，此外它还控制终止。
 *       重写此方法类似于但比为 {@code CyclicBarrier} 提供屏障操作更灵活。
 *
 *   <li> <b>等待。</b> 方法 {@link #awaitAdvance} 需要一个指示到达阶段号的参数，
 *       并在 Phaser 前进到不同阶段时返回。
 *       与 {@code CyclicBarrier} 类似的结构不同，方法 {@code awaitAdvance} 即使等待的线程被中断也会继续等待。
 *       也可以使用可中断和带超时的版本，但任务等待时遇到的中断或超时异常不会改变 Phaser 的状态。
 *       如有必要，你可以在这些异常的处理程序中执行任何关联的恢复操作，通常在调用 {@code forceTermination} 之后。
 *       Phaser 还可以由执行 {@link ForkJoinPool} 中的任务使用，确保在其他任务阻塞等待阶段前进时有足够的并行性来执行任务。
 *
 * </ul>
 *
 * <p><b>终止。</b> Phaser 可以进入<em>终止</em>状态，可以使用方法 {@link #isTerminated} 进行检查。
 * 终止后，所有同步方法都会立即返回，不再等待前进，返回负值。
 * 同样，终止时尝试注册无效。
 * 终止是由 {@code onAdvance} 的调用返回 {@code true} 时触发的。
 * 默认实现在取消注册导致注册的参与者数量变为零时返回 {@code true}。
 * 如下面所示，当 Phaser 控制具有固定迭代次数的操作时，通常通过重写此方法使阶段号达到阈值时终止。
 * 方法 {@link #forceTermination} 也可以用于突然释放等待线程并允许它们终止。
 *
 * <p><b>分层。</b> Phaser 可以<em>分层</em>（即构建为树结构）以减少竞争。
 * 具有大量参与者的 Phaser 否则会经历高同步竞争成本，但可以设置为使子 Phaser 组共享一个共同的父级。
 * 尽管这样做会增加每次操作的开销，但可能会大大提高吞吐量。
 *
 * <p>在分层 Phaser 树中，子 Phaser 与父级的注册和取消注册是自动管理的。
 * 每当子 Phaser 的注册参与者数量变为非零时（在 {@link #Phaser(Phaser,int)} 构造器中、{@link #register} 或 {@link #bulkRegister} 中确定），子 Phaser 就会注册到父级。
 * 每当通过调用 {@link #arriveAndDeregister} 使注册的参与者数量变为零时，子 Phaser 会从父级中取消注册。
 *
 * <p><b>监控。</b> 尽管同步方法只能由注册的参与者调用，但 Phaser 的当前状态可以由任何调用者监控。
 * 在任何给定时刻，{@link #getRegisteredParties} 会返回总共有多少参与者，
 * 其中 {@link #getArrivedParties} 已到达当前阶段（{@link #getPhase}）。
 * 当剩余的 {@link #getUnarrivedParties} 到达时，阶段将前进。
 * 这些方法返回的值可能反映瞬态状态，因此通常不适合用于同步控制。
 * 方法 {@link #toString} 以便于非正式监控的形式返回这些状态查询的快照。
 *
 * <p><b>示例用法：</b>
 *
 * <p>{@code Phaser} 可以代替 {@code CountDownLatch} 来控制一次性操作，
 * 以服务于可变数量的参与者。通常的惯用方式是先注册，接着启动操作，再取消注册，如下所示：
 *
 *  <pre> {@code
 * void runTasks(List<Runnable> tasks) {
 *   final Phaser phaser = new Phaser(1); // "1" 用于注册自己
 *   // 创建并启动线程
 *   for (final Runnable task : tasks) {
 *     phaser.register();
 *     new Thread() {
 *       public void run() {
 *         phaser.arriveAndAwaitAdvance(); // 等待所有任务创建
 *         task.run();
 *       }
 *     }.start();
 *   }
 *
 *   // 允许线程启动并取消注册自己
 *   phaser.arriveAndDeregister();
 * }}</pre>
 *
 * <p>一种让一组线程重复执行操作指定次数的方法是重写 {@code onAdvance}：
 *
 *  <pre> {@code
 * void startTasks(List<Runnable> tasks, final int iterations) {
 *   final Phaser phaser = new Phaser() {
 *     protected boolean onAdvance(int phase, int registeredParties) {
 *       return phase >= iterations || registeredParties == 0;
 *     }
 *   };
 *   phaser.register();
 *   for (final Runnable task : tasks) {
 *     phaser.register();
 *     new Thread() {
 *       public void run() {
 *         do {
 *           task.run();
 *           phaser.arriveAndAwaitAdvance();
 *         } while (!phaser.isTerminated());
 *       }
 *     }.start();
 *   }
 *   phaser.arriveAndDeregister(); // 取消注册自己，不等待
 * }}</pre>
 *
 * 如果主任务必须稍后等待终止，可以重新注册并执行类似的循环：
 *  <pre> {@code
 *   // ...
 *   phaser.register();
 *   while (!phaser.isTerminated())
 *     phaser.arriveAndAwaitAdvance();}</pre>
 *
 * <p>在你确定阶段永远不会绕过 {@code Integer.MAX_VALUE} 时，
 * 可以使用相关结构在特定阶段号下等待。例如：
 *
 *  <pre> {@code
 * void awaitPhase(Phaser phaser, int phase) {
 *   int p = phaser.register(); // 假设调用者尚未注册
 *   while (p < phase) {
 *     if (phaser.isTerminated())
 *       // 处理意外终止
 *     else
 *       p = phaser.arriveAndAwaitAdvance();
 *   }
 *   phaser.arriveAndDeregister();
 * }}</pre>
 *
 *
 * <p>要使用一个 Phaser 树来创建一组 {@code n} 个任务，可以使用以下形式的代码，
 * 假设 Task 类的构造函数接受一个 {@code Phaser}，并在构造时注册它。
 * 调用 {@code build(new Task[n], 0, n, new Phaser())} 后，可以启动这些任务，例如提交给线程池：
 *
 *  <pre> {@code
 * void build(Task[] tasks, int lo, int hi, Phaser ph) {
 *   if (hi - lo > TASKS_PER_PHASER) {
 *     for (int i = lo; i < hi; i += TASKS_PER_PHASER) {
 *       int j = Math.min(i + TASKS_PER_PHASER, hi);
 *       build(tasks, i, j, new Phaser(ph));
 *     }
 *   } else {
 *     for (int i = lo; i < hi; ++i)
 *       tasks[i] = new Task(ph);
 *       // 假设 new Task(ph) 执行 ph.register()
 *   }
 * }}</pre>
 *
 * {@code TASKS_PER_PHASER} 的最佳值主要取决于预期的同步速率。
 * 对于极小的每阶段任务体（因此速率高），适当的值可能低至四个，而对于极大的任务体可能高达数百个。
 *
 * <p><b>实现注释</b>：此实现将最大参与者数限制为 65535。尝试注册更多参与者会抛出 {@code IllegalStateException}。
 * 然而，你可以并且应该创建分层 Phaser 以容纳任意多的参与者。
 *
 * @since 1.7
 * @author Doug Lea
 */
public class Phaser {
    /*
     * 该类实现了 X10 "时钟" 的扩展。感谢 Vijay Saraswat 提供的想法，以及 Vivek Sarkar
     * 提供的增强，扩展了该功能。
     */

    /**
     * 主状态表示，包含四个位域：
     *
     * unarrived  -- 尚未到达屏障的参与者数量 (位 0-15)
     * parties    -- 需要等待的参与者数量 (位 16-31)
     * phase      -- 屏障的当前代数 (位 32-62)
     * terminated -- 如果屏障已终止则设置 (位 63 / 符号位)
     *
     * 一个没有注册参与者的 phaser 通过唯一的非法状态来区分：
     * 即拥有 0 个参与者和 1 个尚未到达的参与者 (编码为下面的 EMPTY)。
     *
     * 为了高效地维护原子性，这些值被打包成一个单一的（原子）long 值。
     * 良好的性能依赖于保持状态的解码和编码简单，以及缩短竞争窗口。
     *
     * 所有状态更新均通过 CAS 完成，除了首次注册子 phaser（即具有非空父级的 phaser）。
     * 在这种（相对罕见的）情况下，我们使用内置同步来锁定并首次注册到父级。
     *
     * 子 phaser 的阶段允许滞后其祖先的阶段，直到它实际被访问为止 —— 参见方法 reconcileState。
     */
    private volatile long state;

    private static final int  MAX_PARTIES     = 0xffff;
    private static final int  MAX_PHASE       = Integer.MAX_VALUE;
    private static final int  PARTIES_SHIFT   = 16;
    private static final int  PHASE_SHIFT     = 32;
    private static final int  UNARRIVED_MASK  = 0xffff;      // 用于掩码 int
    private static final long PARTIES_MASK    = 0xffff0000L; // 用于掩码 long
    private static final long COUNTS_MASK     = 0xffffffffL;
    private static final long TERMINATION_BIT = 1L << 63;

    // 一些特殊值
    private static final int  ONE_ARRIVAL     = 1;
    private static final int  ONE_PARTY       = 1 << PARTIES_SHIFT;
    private static final int  ONE_DEREGISTER  = ONE_ARRIVAL | ONE_PARTY;
    private static final int  EMPTY           = 1;

    // 下面的解包方法通常手动内联

    private static int unarrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
    }

    private static int partiesOf(long s) {
        return (int)s >>> PARTIES_SHIFT;
    }

    private static int phaseOf(long s) {
        return (int)(s >>> PHASE_SHIFT);
    }

    private static int arrivedOf(long s) {
        int counts = (int)s;
        return (counts == EMPTY) ? 0 : (counts >>> PARTIES_SHIFT) - (counts & UNARRIVED_MASK);
    }

    /**
     * 此 phaser 的父级，如果没有则为 null。
     */
    private final Phaser parent;

    /**
     * phaser 树的根。如果不在树中，则等于此。
     */
    private final Phaser root;

    /**
     * Treiber 栈的头部，用于等待线程。为了在释放一些线程时消除竞争，我们
     * 使用两个队列，在偶数和奇数阶段之间交替。子 phaser 与根共享队列以加速释放。
     */
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;

    private AtomicReference<QNode> queueFor(int phase) {
        return ((phase & 1) == 0) ? evenQ : oddQ;
    }

    /**
     * 返回到达时边界异常的消息字符串。
     */
    private String badArrive(long s) {
        return "尝试到达未注册的参与者 " + stateToString(s);
    }

    /**
     * 返回注册时边界异常的消息字符串。
     */
    private String badRegister(long s) {
        return "尝试注册超过 " + MAX_PARTIES + " 个参与者 " + stateToString(s);
    }

    /**
     * 方法 arrive 和 arriveAndDeregister 的主实现。
     * 经过手动调整以加速并最小化常见情况下未到达字段递减时的竞争窗口。
     *
     * @param adjust 要从状态中减去的值；
     *               对于 arrive 是 ONE_ARRIVAL，
     *               对于 arriveAndDeregister 是 ONE_DEREGISTER
     */
    private int doArrive(int adjust) {
        final Phaser root = this.root;
        while (true) {
            long s = (root == this) ? state : reconcileState();
            int phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                return phase;
            int counts = (int)s;
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if (unarrived <= 0)
                throw new IllegalStateException(badArrive(s));
            if (UNSAFE.compareAndSwapLong(this, stateOffset, s, s -= adjust)) {
                if (unarrived == 1) {
                    long n = s & PARTIES_MASK;  // 下一个状态的基数
                    int nextUnarrived = (int)n >>> PARTIES_SHIFT;
                    if (root == this) {
                        if (onAdvance(phase, nextUnarrived))
                            n |= TERMINATION_BIT;
                        else if (nextUnarrived == 0)
                            n |= EMPTY;
                        else
                            n |= nextUnarrived;
                        int nextPhase = (phase + 1) & MAX_PHASE;
                        n |= (long)nextPhase << PHASE_SHIFT;
                        UNSAFE.compareAndSwapLong(this, stateOffset, s, n);
                        releaseWaiters(phase);
                    } else if (nextUnarrived == 0) { // 传播取消注册
                        phase = parent.doArrive(ONE_DEREGISTER);
                        UNSAFE.compareAndSwapLong(this, stateOffset, s, s | EMPTY);
                    } else
                        phase = parent.doArrive(ONE_ARRIVAL);
                }
                return phase;
            }
        }
    }
    /**
     * 方法 register、bulkRegister 的实现
     *
     * @param registrations 要添加到参与者和未到达字段的数量。必须大于零。
     */
    private int doRegister(int registrations) {
        // 调整状态
        long adjust = ((long)registrations << PARTIES_SHIFT) | registrations;
        final Phaser parent = this.parent;
        int phase;
        while (true) {
            long s = (parent == null) ? state : reconcileState();
            int counts = (int)s;
            int parties = counts >>> PARTIES_SHIFT;
            int unarrived = counts & UNARRIVED_MASK;
            if (registrations > MAX_PARTIES - parties)
                throw new IllegalStateException(badRegister(s));
            phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                break;
            if (counts != EMPTY) {                  // 不是第一次注册
                if (parent == null || reconcileState() == s) {
                    if (unarrived == 0)             // 等待阶段前进
                        root.internalAwaitAdvance(phase, null);
                    else if (UNSAFE.compareAndSwapLong(this, stateOffset,
                            s, s + adjust))
                        break;
                }
            }
            else if (parent == null) {              // 第一次根注册
                long next = ((long)phase << PHASE_SHIFT) | adjust;
                if (UNSAFE.compareAndSwapLong(this, stateOffset, s, next))
                    break;
            }
            else {
                synchronized (this) {               // 第一次子注册
                    if (state == s) {               // 在锁下重新检查
                        phase = parent.doRegister(1);
                        if (phase < 0)
                            break;
                        // 当父级注册成功时，无论是否与终止发生竞争，完成注册，
                        // 因为这些属于同一个“事务”。
                        while (!UNSAFE.compareAndSwapLong(this, stateOffset,
                                s, ((long)phase << PHASE_SHIFT) | adjust)) {
                            s = state;
                            phase = (int)(root.state >>> PHASE_SHIFT);
                        }
                        break;
                    }
                }
            }
        }
        return phase;
    }

    /**
     * 如果有必要，从根部解析滞后的阶段传播。
     * 调和通常发生在根前进但子 phaser 尚未前进的情况下，在这种情况下，它们必须通过设置未到达字段
     * 为参与者来完成它们自己的前进（或如果参与者为零，则重置为未注册的 EMPTY 状态）。
     *
     * @return 调和后的状态
     */
    private long reconcileState() {
        final Phaser root = this.root;
        long s = state;
        if (root != this) {
            int phase, p;
            // 使用当前参与者与根阶段 CAS 以触发未到达
            while ((phase = (int)(root.state >>> PHASE_SHIFT)) !=
                    (int)(s >>> PHASE_SHIFT) &&
                    !UNSAFE.compareAndSwapLong(this, stateOffset, s,
                            s = (((long)phase << PHASE_SHIFT) |
                                    ((phase < 0) ? (s & COUNTS_MASK) :
                                            (((p = (int)s >>> PARTIES_SHIFT) == 0) ? EMPTY :
                                                    ((s & PARTIES_MASK) | p)))))) {
                s = state;
            }
        }
        return s;
    }

    /**
     * 创建一个新的 phaser，没有最初注册的参与者，没有父级，初始阶段号为 0。
     * 任何使用此 phaser 的线程首先需要注册。
     */
    public Phaser() {
        this(null, 0);
    }

    /**
     * 创建一个新的 phaser，具有给定数量的注册未到达参与者，没有父级，初始阶段号为 0。
     *
     * @param parties 需要前进到下一阶段的参与者数量
     * @throws IllegalArgumentException 如果参与者数量小于零或大于支持的最大数量
     */
    public Phaser(int parties) {
        this(null, parties);
    }

    /**
     * 等同于 {@link #Phaser(Phaser, int) Phaser(parent, 0)}。
     *
     * @param parent 父 phaser
     */
    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    /**
     * 创建一个新的 phaser，具有给定的父级和注册未到达参与者数量。
     * 当给定的父级不为空且参与者数量大于零时，此子 phaser 会注册到父级。
     *
     * @param parent 父 phaser
     * @param parties 需要前进到下一阶段的参与者数量
     * @throws IllegalArgumentException 如果参与者数量小于零或大于支持的最大数量
     */
    public Phaser(Phaser parent, int parties) {
        if (parties >>> PARTIES_SHIFT != 0)
            throw new IllegalArgumentException("非法的参与者数量");
        int phase = 0;
        this.parent = parent;
        if (parent != null) {
            final Phaser root = parent.root;
            this.root = root;
            this.evenQ = root.evenQ;
            this.oddQ = root.oddQ;
            if (parties != 0)
                phase = parent.doRegister(1);
        }
        else {
            this.root = this;
            this.evenQ = new AtomicReference<QNode>();
            this.oddQ = new AtomicReference<QNode>();
        }
        this.state = (parties == 0) ? (long)EMPTY :
                ((long)phase << PHASE_SHIFT) |
                        ((long)parties << PARTIES_SHIFT) |
                        ((long)parties);
    }
    /**
     * 为此 phaser 添加一个新的未到达参与者。
     * 如果正在调用 {@link #onAdvance}，则此方法可能会在其完成后再返回。
     * 如果此 phaser 有一个父级，并且之前没有注册参与者，则该子 phaser 也会注册到其父级。
     * 如果此 phaser 已经终止，尝试注册无效，并返回负值。
     *
     * @return 应用于此注册的到达阶段号。如果该值为负数，则表示此 phaser 已终止，此时注册无效。
     * @throws IllegalStateException 如果尝试注册超过支持的最大参与者数量
     */
    public int register() {
        return doRegister(1);
    }

    /**
     * 为此 phaser 添加给定数量的新未到达参与者。
     * 如果正在调用 {@link #onAdvance}，则此方法可能会在其完成后再返回。
     * 如果此 phaser 有一个父级，并且给定数量的参与者大于零，并且此 phaser 之前没有注册参与者，
     * 则此子 phaser 也会注册到其父级。
     * 如果此 phaser 已终止，尝试注册无效，并返回负值。
     *
     * @param parties 要添加的参与者数量
     * @return 应用于此注册的到达阶段号。如果该值为负数，则表示此 phaser 已终止，此时注册无效。
     * @throws IllegalStateException 如果尝试注册超过支持的最大参与者数量
     * @throws IllegalArgumentException 如果 {@code parties < 0}
     */
    public int bulkRegister(int parties) {
        if (parties < 0)
            throw new IllegalArgumentException();
        if (parties == 0)
            return getPhase();
        return doRegister(parties);
    }

    /**
     * 到达此 phaser，而不等待其他参与者到达。
     *
     * <p>如果未注册的参与者调用此方法，将出现使用错误。
     * 然而，这种错误可能仅在对此 phaser 的某些后续操作中（如果有）导致 {@code IllegalStateException}。
     *
     * @return 到达的阶段号，如果已终止则返回负值
     * @throws IllegalStateException 如果未终止但未到达的参与者数量变为负数
     */
    public int arrive() {
        return doArrive(ONE_ARRIVAL);
    }

    /**
     * 到达此 phaser 并取消注册，而不等待其他参与者到达。
     * 取消注册减少了将来各阶段需要前进的参与者数量。
     * 如果此 phaser 有一个父级，并且取消注册使得此 phaser 没有参与者，则此 phaser 也会从其父级取消注册。
     *
     * <p>如果未注册的参与者调用此方法，将出现使用错误。
     * 然而，这种错误可能仅在对此 phaser 的某些后续操作中（如果有）导致 {@code IllegalStateException}。
     *
     * @return 到达的阶段号，如果已终止则返回负值
     * @throws IllegalStateException 如果未终止且注册或未到达的参与者数量变为负数
     */
    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }

    /**
     * 到达此 phaser 并等待其他参与者到达。
     * 效果等同于 {@code awaitAdvance(arrive())}。
     * 如果你需要带中断或超时的等待，可以使用 {@code awaitAdvance} 方法的其他形式。
     * 如果你需要在到达后取消注册，则可以使用 {@code awaitAdvance(arriveAndDeregister())}。
     *
     * <p>如果未注册的参与者调用此方法，将出现使用错误。
     * 然而，这种错误可能仅在对此 phaser 的某些后续操作中（如果有）导致 {@code IllegalStateException}。
     *
     * @return 到达的阶段号，或（负数）表示已终止的 {@linkplain #getPhase() 当前阶段}
     * @throws IllegalStateException 如果未终止且未到达的参与者数量变为负数
     */
    public int arriveAndAwaitAdvance() {
        // 专用于 doArrive+awaitAdvance，消除一些读取/路径
        final Phaser root = this.root;
        while (true) {
            long s = (root == this) ? state : reconcileState();
            int phase = (int)(s >>> PHASE_SHIFT);
            if (phase < 0)
                return phase;
            int counts = (int)s;
            int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
            if (unarrived <= 0)
                throw new IllegalStateException(badArrive(s));
            if (UNSAFE.compareAndSwapLong(this, stateOffset, s,
                    s -= ONE_ARRIVAL)) {
                if (unarrived > 1)
                    return root.internalAwaitAdvance(phase, null);
                if (root != this)
                    return parent.arriveAndAwaitAdvance();
                long n = s & PARTIES_MASK;  // 下一个状态的基数
                int nextUnarrived = (int)n >>> PARTIES_SHIFT;
                if (onAdvance(phase, nextUnarrived))
                    n |= TERMINATION_BIT;
                else if (nextUnarrived == 0)
                    n |= EMPTY;
                else
                    n |= nextUnarrived;
                int nextPhase = (phase + 1) & MAX_PHASE;
                n |= (long)nextPhase << PHASE_SHIFT;
                if (!UNSAFE.compareAndSwapLong(this, stateOffset, s, n))
                    return (int)(state >>> PHASE_SHIFT); // 已终止
                releaseWaiters(phase);
                return nextPhase;
            }
        }
    }

    /**
     * 等待此 phaser 的阶段从给定的阶段值前进，如果当前阶段不等于给定阶段值或者此 phaser 已终止，则立即返回。
     *
     * @param phase 到达的阶段号，或负数表示已终止；
     * 此参数通常是先前调用 {@code arrive} 或 {@code arriveAndDeregister} 返回的值。
     * @return 下一个到达的阶段号，或该参数（如果为负），或（负数）表示已终止的 {@linkplain #getPhase() 当前阶段}
     */
    public int awaitAdvance(int phase) {
        final Phaser root = this.root;
        long s = (root == this) ? state : reconcileState();
        int p = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        if (p == phase)
            return root.internalAwaitAdvance(phase, null);
        return p;
    }

    /**
     * 中断等待时抛出 {@code InterruptedException} 或阶段前进时等待超时，或立即返回如果当前阶段不等于给定阶段值或此 phaser 已终止。
     *
     * @param phase 到达的阶段号，或负数表示已终止；
     * 此参数通常是先前调用 {@code arrive} 或 {@code arriveAndDeregister} 返回的值。
     * @return 下一个到达的阶段号，或该参数（如果为负），或（负数）表示已终止的 {@linkplain #getPhase() 当前阶段}
     * @throws InterruptedException 如果等待时线程被中断
     */
    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        final Phaser root = this.root;
        long s = (root == this) ? state : reconcileState();
        int p = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        if (p == phase) {
            QNode node = new QNode(this, phase, true, false, 0L);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted)
                throw new InterruptedException();
        }
        return p;
    }

    /**
     * 强制此 phaser 进入终止状态。
     * 注册的参与者计数不受影响。如果此 phaser 是一组分层 phaser 的成员，则该集合中的所有 phaser 都会终止。
     * 如果此 phaser 已经终止，则此方法无效。
     * 此方法在多个任务遇到意外异常后协调恢复时非常有用。
     */
    public void forceTermination() {
        // 只需要更改根状态
        final Phaser root = this.root;
        long s;
        while ((s = root.state) >= 0) {
            if (UNSAFE.compareAndSwapLong(root, stateOffset,
                    s, s | TERMINATION_BIT)) {
                // 通知所有线程
                releaseWaiters(0); // 偶数队列中的等待者
                releaseWaiters(1); // 奇数队列中的等待者
                return;
            }
        }
    }

    /**
     * 返回当前阶段号。最大阶段号为 {@code Integer.MAX_VALUE}，之后重新从零开始。
     * 终止后，阶段号为负数，在这种情况下，可以通过 {@code getPhase() + Integer.MIN_VALUE} 获取终止前的阶段。
     *
     * @return 阶段号，或终止时为负数
     */
    public final int getPhase() {
        return (int)(root.state >>> PHASE_SHIFT);
    }

    /**
     * 返回此 phaser 注册的参与者数量。
     *
     * @return 参与者数量
     */
    public int getRegisteredParties() {
        return partiesOf(state);
    }

    /**
     * 返回此 phaser 当前阶段已到达的注册参与者数量。
     * 如果此 phaser 已终止，则返回的值是无意义的。
     *
     * @return 已到达的参与者数量
     */
    public int getArrivedParties() {
        return arrivedOf(reconcileState());
    }

    /**
     * 返回此 phaser 当前阶段尚未到达的注册参与者数量。
     * 如果此 phaser 已终止，则返回的值是无意义的。
     *
     * @return 尚未到达的参与者数量
     */
    public int getUnarrivedParties() {
        return unarrivedOf(reconcileState());
    }

    /**
     * 返回此 phaser 的父级，如果没有则为 {@code null}。
     *
     * @return 父 phaser，如果没有则为 {@code null}
     */
    public Phaser getParent() {
        return parent;
    }

    /**
     * 返回此 phaser 的根祖先，如果没有父级则为自身。
     *
     * @return 根祖先 phaser
     */
    public Phaser getRoot() {
        return root;
    }

    /**
     * 如果此 phaser 已终止，返回 {@code true}。
     *
     * @return 如果已终止返回 {@code true}
     */
    public boolean isTerminated() {
        return root.state < 0L;
    }

    /**
     * 可重写的方法，用于在阶段即将前进时执行操作，并控制终止。
     * 此方法由推进此 phaser 的参与者调用（当所有其他等待的参与者都处于休眠状态时）。
     * 如果此方法返回 {@code true}，则此 phaser 在前进时将设置为最终的终止状态，
     * 并且后续对 {@link #isTerminated} 的调用将返回 true。
     * 任何由此方法抛出的（未检查的）异常或错误都会传播到尝试推进此 phaser 的参与者，
     * 此时不再前进。
     *
     * <p>此方法的参数提供了当前转换时 phaser 的状态。
     * 在 {@code onAdvance} 中调用到达、注册和等待方法的效果是未指定的，不应依赖它们。
     *
     * <p>如果此 phaser 是分层 phaser 集的一部分，则 {@code onAdvance} 仅在每次前进时为其根 phaser 调用。
     *
     * <p>为了支持最常见的用例，当参与者通过调用 {@code arriveAndDeregister} 取消注册导致参与者数量变为零时，
     * 此方法的默认实现返回 {@code true}。
     * 可以通过重写此方法以始终返回 {@code false} 来禁用此行为，从而启用在未来的注册上继续：
     *
     * <pre> {@code
     * Phaser phaser = new Phaser() {
     *   protected boolean onAdvance(int phase, int parties) { return false; }
     * }}</pre>
     *
     * @param phase 进入此方法时的当前阶段号，在 phaser 前进之前
     * @param registeredParties 当前的注册参与者数量
     * @return 如果此 phaser 应终止则返回 {@code true}
     */
    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    /**
     * 返回标识此 phaser 及其状态的字符串。
     * 状态包含在括号中，包括字符串 {@code "phase = "} 后跟阶段号，
     * {@code "parties = "} 后跟注册的参与者数量，以及 {@code "arrived = "} 后跟已到达的参与者数量。
     *
     * @return 标识此 phaser 及其状态的字符串
     */
    public String toString() {
        return stateToString(reconcileState());
    }

    /**
     * 实现 toString 和基于字符串的错误消息
     */
    private String stateToString(long s) {
        return super.toString() +
                "[phase = " + phaseOf(s) +
                " parties = " + partiesOf(s) +
                " arrived = " + arrivedOf(s) + "]";
    }

    // 等待机制

    /**
     * 从队列中移除线程并通知它们完成了当前阶段。
     */
    private void releaseWaiters(int phase) {
        QNode q;   // 队列中的第一个元素
        Thread t;  // 队列中的线程
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        // 只要队列头部不为空，并且队列中线程所属的阶段不是当前阶段
        while ((q = head.get()) != null &&
                q.phase != (int)(root.state >>> PHASE_SHIFT)) {
            // 如果队列的头部线程与队列更新保持一致，并且线程不为空
            if (head.compareAndSet(q, q.next) &&
                    (t = q.thread) != null) {
                q.thread = null;  // 将线程引用设置为 null
                LockSupport.unpark(t);  // 唤醒被阻塞的线程
            }
        }
    }

    /**
     * releaseWaiters 的变体，尝试移除由于超时或中断不再等待前进的节点。
     * 当前情况下，只有当节点位于队列头部时才会被移除，这已经足够在大多数情况下减少内存占用。
     *
     * @return 退出时的当前阶段
     */
    private int abortWait(int phase) {
        AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
        while (true) {
            Thread t;
            QNode q = head.get();
            int p = (int)(root.state >>> PHASE_SHIFT);
            if (q == null || ((t = q.thread) != null && q.phase == p)) {
                return p;
            }
            if (head.compareAndSet(q, q.next) && t != null) {
                q.thread = null;  // 取消等待
                LockSupport.unpark(t);  // 唤醒线程
            }
        }
    }

    /** CPU 数量，用于自旋控制 */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * 等待阶段前进时自旋的次数。在多处理器系统中，完全阻塞和唤醒大量线程的过程通常非常慢，
     * 因此我们使用可充电自旋来避免这种情况：当线程在 internalAwaitAdvance 中注意到
     * 另一个线程到达时，且系统中有足够的 CPU 可用，它将继续自旋 SPINS_PER_ARRIVAL 次，
     * 而不是直接阻塞。该值在“好市民行为”和“避免不必要的延迟”之间取得了权衡。
     */
    static final int SPINS_PER_ARRIVAL = (NCPU < 2) ? 1 : 1 << 8;

    /**
     * 阻塞并等待阶段前进，除非被中止。仅在根 phaser 上调用。
     *
     * @param phase 当前阶段
     * @param node 如果非 null，则为等待节点，用于跟踪中断和超时；如果为 null，表示不可中断的等待
     * @return 当前阶段
     */
    private int internalAwaitAdvance(int phase, QNode node) {
        // 确保之前的队列被清理
        releaseWaiters(phase - 1);
        boolean queued = false;  // true 表示节点已入队
        int lastUnarrived = 0;   // 用于在变更时增加自旋次数
        int spins = SPINS_PER_ARRIVAL;
        long s;
        int p;
        // 阶段保持不变时继续循环
        while ((p = (int)((s = state) >>> PHASE_SHIFT)) == phase) {
            if (node == null) {  // 在不可中断模式下自旋
                int unarrived = (int)s & UNARRIVED_MASK;
                if (unarrived != lastUnarrived &&
                        (lastUnarrived = unarrived) < NCPU) {
                    spins += SPINS_PER_ARRIVAL;
                }
                boolean interrupted = Thread.interrupted();
                if (interrupted || --spins < 0) {  // 需要节点来记录中断
                    node = new QNode(this, phase, false, false, 0L);
                    node.wasInterrupted = interrupted;
                }
            } else if (node.isReleasable()) {  // 完成或被中止
                break;
            } else if (!queued) {  // 将节点推入队列
                AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
                QNode q = node.next = head.get();
                if ((q == null || q.phase == phase) &&
                        (int)(state >>> PHASE_SHIFT) == phase) {  // 避免过期的入队
                    queued = head.compareAndSet(q, node);
                }
            } else {
                try {
                    ForkJoinPool.managedBlock(node);
                } catch (InterruptedException ie) {
                    node.wasInterrupted = true;
                }
            }
        }

        if (node != null) {
            if (node.thread != null) {
                node.thread = null;  // 避免需要调用 unpark()
            }
            if (node.wasInterrupted && !node.interruptible) {
                Thread.currentThread().interrupt();
            }
            if (p == phase && (p = (int)(state >>> PHASE_SHIFT)) == phase) {
                return abortWait(phase);  // 在中止时可能清理
            }
        }
        releaseWaiters(phase);
        return p;
    }

    /**
     * 表示等待队列的 Treiber 栈的等待节点。
     */
    static final class QNode implements ForkJoinPool.ManagedBlocker {
        final Phaser phaser;
        final int phase;
        final boolean interruptible;
        final boolean timed;
        boolean wasInterrupted;
        long nanos;
        final long deadline;
        volatile Thread thread; // 如果等待被取消则为 null
        QNode next;

        QNode(Phaser phaser, int phase, boolean interruptible,
              boolean timed, long nanos) {
            this.phaser = phaser;
            this.phase = phase;
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.timed = timed;
            this.deadline = timed ? System.nanoTime() + nanos : 0L;
            thread = Thread.currentThread();
        }

        public boolean isReleasable() {
            if (thread == null)
                return true;
            if (phaser.getPhase() != phase) {
                thread = null;
                return true;
            }
            if (Thread.interrupted())
                wasInterrupted = true;
            if (wasInterrupted && interruptible) {
                thread = null;
                return true;
            }
            if (timed) {
                if (nanos > 0L) {
                    nanos = deadline - System.nanoTime();
                }
                if (nanos <= 0L) {
                    thread = null;
                    return true;
                }
            }
            return false;
        }

        public boolean block() {
            if (isReleasable())
                return true;
            else if (!timed)
                LockSupport.park(this);
            else if (nanos > 0L)
                LockSupport.parkNanos(this, nanos);
            return isReleasable();
        }
    }

    // Unsafe 机制

    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = Phaser.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}


