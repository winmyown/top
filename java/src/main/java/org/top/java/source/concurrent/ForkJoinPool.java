package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:45
 */

import java.security.AccessControlContext;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.top.java.source.concurrent.atomic.AtomicLong;
import org.top.java.source.lang.Thread.UncaughtExceptionHandler;
import org.top.java.source.lang.Thread;

/**
 * 用于运行 {@link ForkJoinTask} 的 {@link ExecutorService}。
 * 一个 {@code ForkJoinPool} 提供了来自非 {@code ForkJoinTask} 客户端的提交入口，
 * 以及管理和监控操作。
 *
 * <p>一个 {@code ForkJoinPool} 与其他类型的 {@link ExecutorService} 主要的区别在于
 * 它使用了<em>工作窃取（work-stealing）</em>：池中的所有线程都尝试找到并执行提交到池中的任务，
 * 和/或由其他活跃任务创建的子任务（最终在没有任务时会阻塞等待）。这使得处理大多数任务
 * 生成其他子任务（如大多数 {@code ForkJoinTask}）以及许多小任务从外部客户端提交到池中时
 * 的效率更高。特别是当在构造函数中将 <em>asyncMode</em> 设置为 true 时，
 * {@code ForkJoinPool} 也适合用于那些永远不会合并的事件式任务。
 *
 * <p>一个静态的 {@link #commonPool()} 可用于大多数应用。常用池适用于任何未显式提交到
 * 指定池中的 ForkJoinTask。使用常用池通常可以减少资源使用（线程会在不使用期间慢慢回收，
 * 并在随后的使用中恢复）。
 *
 * <p>对于需要单独或自定义池的应用程序，可以构造一个具有指定并行度级别的 {@code ForkJoinPool}；
 * 默认情况下，它等于可用处理器的数量。该池通过动态添加、挂起或恢复内部工作线程来保持足够的活跃（或可用）线程，
 * 即使某些任务在等待合并其他任务时被阻塞。然而，对于被阻塞的 I/O 或其他非托管同步任务，
 * 没有任何保证。嵌套的 {@link ManagedBlocker} 接口使得可以扩展所支持的同步种类。
 *
 * <p>除了执行和生命周期控制方法外，该类还提供状态检查方法（例如 {@link #getStealCount}），
 * 旨在帮助开发、调优和监控 fork/join 应用程序。此外，方法 {@link #toString}
 * 以方便的形式返回池的状态指示，用于非正式的监控。
 *
 * <p>与其他 ExecutorServices 类似，任务执行有三种主要方法，总结如下表所示。
 * 这些方法主要用于当前池中没有参与 fork/join 计算的客户端。主要形式的这些方法接受
 * {@code ForkJoinTask} 的实例，但重载形式也允许执行基于 {@code Runnable} 或 {@code Callable} 的活动。
 * 然而，已经在池中执行的任务通常应该使用表中列出的内部计算形式，除非使用的任务是异步事件式的，
 * 通常不会合并，此时方法的选择几乎没有差别。
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>任务执行方法的总结</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER> <b>从非 fork/join 客户端调用</b></td>
 *    <td ALIGN=CENTER> <b>从 fork/join 计算中调用</b></td>
 *  </tr>
 *  <tr>
 *    <td> <b>安排异步执行</b></td>
 *    <td> {@link #execute(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork}</td>
 *  </tr>
 *  <tr>
 *    <td> <b>等待并获取结果</b></td>
 *    <td> {@link #invoke(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#invoke}</td>
 *  </tr>
 *  <tr>
 *    <td> <b>安排执行并获取 Future</b></td>
 *    <td> {@link #submit(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork} （ForkJoinTasks <em>是</em> Futures）</td>
 *  </tr>
 * </table>
 *
 * <p>常用池默认使用默认参数构造，但这些参数可以通过设置三个 {@linkplain System#getProperty 系统属性} 来控制：
 * <ul>
 * <li>{@code java.util.concurrent.ForkJoinPool.common.parallelism}
 * - 并行度级别，一个非负整数
 * <li>{@code java.util.concurrent.ForkJoinPool.common.threadFactory}
 * - {@link ForkJoinWorkerThreadFactory} 的类名
 * <li>{@code java.util.concurrent.ForkJoinPool.common.exceptionHandler}
 * - {@link UncaughtExceptionHandler} 的类名
 * </ul>
 * 如果存在 {@link SecurityManager} 且未指定工厂，则默认池使用提供没有 {@link Permissions} 权限的线程的工厂。
 * 系统类加载器用于加载这些类。在建立这些设置时发生任何错误时，将使用默认参数。通过将并行度属性设置为零，
 * 和/或使用可能返回 {@code null} 的工厂，可以禁用或限制在常用池中使用线程。
 * 但这样做可能导致未合并的任务永远不会被执行。
 *
 * <p><b>实现说明</b>：该实现将最大运行线程数限制为 32767。尝试创建超出最大数量的池会导致抛出
 * {@code IllegalArgumentException}。
 *
 * <p>该实现仅在池关闭或内部资源耗尽时拒绝提交的任务（即，通过抛出
 * {@link RejectedExecutionException}）。
 *
 * @since 1.7
 * @author Doug Lea
 */
@sun.misc.Contended
public class ForkJoinPool extends AbstractExecutorService {

    /**
     * 实现概述
     *
     * 该类及其嵌套类提供了一组工作线程的主要功能和控制：
     * 来自非 FJ 线程的提交进入提交队列。工作线程会获取这些任务，通常将其分解为子任务，
     * 子任务可能会被其他工作线程窃取。优先级规则优先处理它们自己队列中的任务（LIFO 或 FIFO，取决于模式），
     * 然后随机从其他队列窃取 FIFO 任务。这个框架最初作为支持使用工作窃取的树形并行处理的工具。
     * 随着时间的推移，其可扩展性优势导致了扩展和更改，以更好地支持更多样化的使用场景。
     * 由于大多数内部方法和嵌套类是相互关联的，它们的主要原理和描述都呈现在这里；
     * 个别方法和嵌套类中仅包含简短的详细注释。
     *
     * WorkQueues
     * ==========
     *
     * 大多数操作发生在工作窃取队列（嵌套类 WorkQueue 中）内。这些是 Deques 的特殊形式，
     * 仅支持四种可能的端操作中的三种——push、pop 和 poll（即偷取），
     * 并且进一步限制为 push 和 pop 只能由拥有线程调用（或在此扩展的情况下，在锁下），
     * 而 poll 可以由其他线程调用。（如果您不熟悉它们，您可能需要阅读 Herlihy 和 Shavit 的书
     * "Multiprocessor 编程的艺术" 第 16 章，在继续之前更详细地描述了这些内容。）
     * 主要的工作窃取队列设计大致类似于 Chase 和 Lev 在 SPAA 2005 中发表的
     * “动态圆形工作窃取队列”（Dynamic Circular Work-Stealing Deque）论文
     * (http://research.sun.com/scalable/pubs/index.html)
     * 和 Michael、Saraswat 以及 Vechev 在 PPoPP 2009 中发表的
     * “幂等工作窃取”（Idempotent work stealing）论文
     * (http://portal.acm.org/citation.cfm?id=1504186)。
     * 主要区别最终源于 GC 要求我们尽快将获取到的槽位设置为空，以在生成大量任务的程序中保持尽可能小的占用。
     * 为实现这一点，我们将仲裁 pop 与 poll（偷取）的 CAS 操作从索引（“base” 和 “top”）移到了槽位本身。
     *
     * 添加任务的形式是经典的数组 push(task)：
     *    q.array[q.top] = task; ++q.top;
     *
     * （实际代码需要空检查和大小检查数组，正确的访问屏障，并可能通知等待的工作线程开始扫描——见下文。）
     * 成功的 pop 和 poll 主要涉及从非空到空的槽位 CAS 操作。
     *
     * pop 操作（始终由拥有者执行）是：
     *   if ((base != top) 并且
     *        (top 槽中的任务不为空) 并且
     *        (CAS 槽位为空))
     *           递减 top 并返回任务；
     *
     * 而 poll 操作（通常由窃取者执行）是：
     *    if ((base != top) 并且
     *        (base 槽中的任务不为空) 并且
     *        (base 没有改变) 并且
     *        (CAS 槽位为空))
     *           递增 base 并返回任务；
     *
     * 因为我们依赖引用的 CAS 操作，所以不需要在 base 或 top 上使用标记位。
     * 它们是简单的 int，和任何基于圆形数组的队列一样（参见例如 ArrayDeque）。
     * 索引更新保证 top == base 表示队列为空，但否则在 push、pop 或 poll 未完全提交时，
     * 队列可能会被错误地视为空。（方法 isEmpty() 会检查删除最后一个元素时部分完成的情况。）
     * 因此，单个偷取操作可能不是无等待的。一名窃取者在另一名正在进行偷取的窃取者（或，如果先前为空，
     * 则 push 操作）完成之前无法成功继续。然而，总体上，我们至少确保了概率性的非阻塞性。
     * 如果窃取尝试失败，窃取者总是选择下一个随机的受害者目标继续尝试。
     * 因此，只要有一个正在进行的偷取或新的 push 操作完成，其他窃取者就可以继续。
     * (这就是为什么我们通常使用方法 pollAt 及其变体，只在 apparent base 索引尝试一次，
     * 否则考虑其他操作，而不是使用反复尝试的 poll 方法的原因。)
     *
     * 这种方法还支持用户模式，在该模式下本地任务处理按 FIFO 顺序而不是 LIFO 顺序进行，
     * 只需使用 poll 而不是 pop 即可。这在任务永远不会合并的消息传递框架中可能很有用。
     * 然而，两种模式都不考虑亲和性、负载、缓存局部性等，因此很少能在给定的机器上提供最佳性能，
     * 但便携地通过这些因素的平均值提供良好的吞吐量。
     * 此外，即使我们尝试使用此类信息，我们通常也没有基础来利用它。
     * 例如，某些任务集合受益于缓存亲和性，但其他任务会因缓存污染效应而受到损害。
     * 另外，尽管需要扫描，长期的吞吐量通常最好使用随机选择，而不是定向选择策略，
     * 因此在适用的情况下使用廉价的随机化（通常使用 Marsaglia XorShifts）是合理的。
     *
     * WorkQueues 也以类似的方式用于提交到池中的任务。我们不能将这些任务与工作线程使用的队列混合在一起。
     * 相反，我们使用一种散列形式随机地将提交队列与提交线程关联。ThreadLocalRandom 探针值作为选择现有队列的哈希码，
     * 并且在与其他提交者发生争用时可能会随机重新定位。
     * 实质上，提交者的行为类似于工作线程，只是它们受限于执行它们提交的本地任务
     * （或者在 CountedCompleters 的情况下，其他具有相同根任务的任务）。
     * 在共享模式下插入任务需要锁定（主要用于保护在调整大小时），但我们只使用简单的自旋锁
     * （使用字段 qlock），因为遇到繁忙队列的提交者会继续尝试或创建其他队列——它们仅在创建和注册新队列时阻塞。
     * 此外，在关闭时 "qlock" 会饱和到不可解锁的值（-1）。在成功的情况下，仍可以通过便宜的有序写入来解锁 "qlock"，
     * 但在不成功的情况下使用 CAS。
     *
     * 管理
     * ==========
     *
     * 工作窃取的主要吞吐量优势来自去中心化控制——工作线程大多从自己或彼此那里获取任务，
     * 速度可以超过每秒数十亿。池本身创建、激活（允许扫描和运行任务）、停用、阻塞和终止线程，
     * 所有这些都使用最少的中心信息。
     * 我们只有少数属性可以全局跟踪或维护，因此我们将它们打包到少量变量中，通常通过不阻塞或锁定来维护原子性。
     * 几乎所有本质上原子的控制状态都保存在两个易失变量中，这些变量在状态和一致性检查时远远大多数情况下是读取（而不是写入）的。
     * （此外，字段 "config" 保存不变的配置状态。）
     *
     * 字段 "ctl" 包含 64 位信息，用于原子性决定添加、停用、入队（在事件队列上）、出队和/或重新激活工作线程。
     * 为了实现这种打包，我们将最大并行度限制为 (1<<15)-1（远远超出正常的运行范围），
     * 以允许 id、计数及其反值（用于阈值判断）适合 16 位子字段。
     *
     * 字段 "runState" 保存可锁定的状态位（STARTED, STOP 等），还保护对 workQueues 数组的更新。
     * 当用作锁时，它通常只持有几个指令的时间（唯一的例外是一次性数组初始化和不常见的调整大小。），因此几乎总是可以在短暂的自旋后获得锁定。
     * 但为了更谨慎，如果初始 CAS 操作失败，方法 `awaitRunStateLock`（仅在非常罕见的需要时被调用）会使用 `wait/notify` 机制，
     * 在内置监视器上阻塞。这在高度竞争的锁中可能是个糟糕的主意，但大多数池在超过自旋限制后几乎不需要竞争锁，
     * 因此这种保守的替代方案运作良好。因为我们没有其他内部对象可以作为监视器，"stealCounter"（一个 `AtomicLong`）
     * 被用于这种情况（它也必须被懒加载；请参见 `externalSubmit`）。
     *
     * 使用 "runState" 和 "ctl" 的相互作用仅在一个场景中发生：
     * 决定是否添加工作线程（见 `tryAddWorker`），此时 `ctl` CAS 操作是在持有锁时执行的。
     *
     * 记录 WorkQueues：`WorkQueues` 被记录在 `workQueues` 数组中。数组在第一次使用时被创建（参见 `externalSubmit`），
     * 并在必要时扩展。更新数组以记录新的工作线程和注销已终止的工作线程的操作受到 `runState` 锁的保护，
     * 但该数组可以被并发读取并直接访问。我们还确保数组引用本身的读取不会变得太陈旧。
     * 为了简化基于索引的操作，数组大小始终为 2 的幂，所有读者必须容忍空槽。
     * 工作线程队列存储在奇数索引处。共享（提交）队列存储在偶数索引处，最多支持 64 个插槽，
     * 以限制即使在需要扩展数组以添加更多工作线程时的增长。以这种方式将它们分组在一起简化
     * 并加速了任务扫描。
     *
     * 所有的工作线程都是按需创建的，由任务提交、替换已终止的工作线程和/或补偿被阻塞的工作线程触发。
     * 然而，所有其他支持代码都设置为支持其他策略。为了确保我们不会保留阻止垃圾回收的工作线程引用，
     * 所有对 `workQueues` 的访问都是通过 `workQueues` 数组的索引（这是这里一些代码结构复杂的原因之一）。
     * 本质上，`workQueues` 数组充当一种弱引用机制。例如，`ctl` 的栈顶子字段存储的是索引，而不是引用。
     *
     * 排队空闲的工作线程：与 HPC 工作窃取框架不同，当无法立即找到任务时，我们不能让工作线程无限期自旋扫描任务，
     * 也不能在似乎有可用任务时启动或恢复工作线程。另一方面，当提交或生成新任务时，我们必须迅速激活它们。
     * 在许多用例中，激活工作线程的加速是影响整体性能的主要因素，尤其是在程序启动时，JIT 编译和分配会加重这种情况。
     * 因此，我们尽可能地简化这一过程。
     *
     * 字段 "ctl" 原子性地维护了活跃和总的工作线程计数，以及一个队列，用于存放等待的线程，
     * 以便在需要时对它们进行信号处理。活跃计数也充当了静止指示器的角色，
     * 因此在工作线程认为没有更多任务可执行时会递减。"队列" 实际上是一种 Treiber 栈。
     * 栈是按最近使用顺序激活线程的理想选择。这提高了性能和局部性，
     * 超过了它容易产生竞争和无法释放除非栈顶的工作线程的缺点。
     * 当它们找不到工作时，我们将工作线程推到空闲的工作线程栈中（由 `ctl` 的低 32 位子字段表示）
     * 并使其进入阻塞状态。栈顶状态保存了工作线程的 "scanState" 字段的值：其索引和状态，
     * 加上一个版本计数器，除了作为版本戳的计数子字段之外，这些还提供了对 Treiber 栈的 ABA 效应的保护。
     *
     * 字段 `scanState` 被工作线程和池共同使用，用于管理和跟踪工作线程是处于非活跃状态（可能正在等待信号）还是正在扫描任务（当两者都没有时，它正在运行任务）。
     * 当一个工作线程被停用时，它的 `scanState` 字段被设置，并且即使它必须扫描一次以避免队列竞争，
     * 它也无法执行任务。请注意，`scanState` 更新会滞后于队列 CAS 释放，因此在使用时需要小心。
     * 当排队时，`scanState` 的低 16 位必须保存它的池索引。因此，在初始化时（参见 `registerWorker`）
     * 我们将索引放在那里，并在必要时保持或恢复它。
     *
     * 内存排序：请参阅 Le、Pop、Cohen 和 Nardelli 在 PPoPP 2013 发表的文章《弱内存模型的正确和高效工作窃取算法》（"Correct and Efficient Work-Stealing for Weak Memory Models"），
     * (http://www.di.ens.fr/~zappa/readings/ppopp13.pdf) 来分析类似于这里使用的工作窃取算法中的内存排序需求。我们通常需要比最小排序更强的排序，因为有时必须信号通知工作线程以避免丢失信号。安排足够的排序而不会导致昂贵的过度屏障需要在支持表达访问限制的各种方式之间进行权衡。最核心的操作是从队列中取出任务和更新 `ctl` 状态，这些操作需要全屏障 CAS。数组槽位的读取使用了 Unsafe 提供的模拟易失性（volatile）的机制。对于其他线程访问 `WorkQueue` 的 `base`、`top` 和数组，需要在读取其中任意一个之前执行易失性加载。我们使用的约定是将 "base" 索引声明为 `volatile`，并且始终在读取其他字段之前读取它。拥有线程必须确保有序的更新，因此写操作使用有序的内部实现，除非它们可以与其他写操作结合使用。类似的约定和原理适用于 `WorkQueue` 的其他字段（如 "currentSteal"），这些字段只由拥有者写入但由其他线程观察。
     *
     * ### 创建工作线程：
     * 要创建一个工作线程，我们首先递增总计数（作为保留），并尝试通过其工厂构造一个 `ForkJoinWorkerThread`。在构造过程中，新线程会调用 `registerWorker`，其中它会构造一个 `WorkQueue`，并在 `workQueues` 数组中分配一个索引（如有必要，扩展数组）。然后线程启动。如果在这些步骤中发生任何异常，或者工厂返回 `null`，`deregisterWorker` 会相应地调整计数并记录状态。如果工厂返回 `null`，池会继续运行，但线程数量低于目标数量。如果发生异常，则异常会被传播，通常传递给某些外部调用者。工作线程索引的分配避免了扫描偏差，该偏差会在工作线程按顺序排列在 `workQueues` 数组的前面时发生。我们将该数组视为一个简单的幂次散列表，必要时扩展数组。`seedIndex` 递增确保在需要调整大小或注销并替换工作线程之前不会发生冲突，并在之后保持低概率的冲突。我们不能在这里使用 `ThreadLocalRandom.getProbe()` 执行类似的功能，因为线程尚未启动，但会用于为现有外部线程创建提交队列。
     *
     * ### 停用和等待：
     * 排队遇到了几种内在的竞争条件；最主要的是，生产任务的线程可能错过看到（和发送信号给）已停止查找工作的线程，但该线程尚未进入等待队列。当工作线程无法找到可窃取的任务时，它会停用并入队。由于 GC 或操作系统调度，任务的缺乏通常是暂时的。为了减少误报停用，扫描器在扫描期间计算队列状态的校验和。（这里使用的稳定性检查以及其他地方的检查是概率性快照技术的变种——请参见 Herlihy & Shavit 的著作。）工作线程只有在扫描期间的和保持稳定之后才会放弃并尝试停用。此外，为了避免错过信号，它们在成功入队后会再次扫描，直到再次稳定。在这种状态下，工作线程无法执行它看到的任务，直到它从队列中释放出来，因此它最终会尝试释放自己或任何继任者（请参见 `tryRelease`）。否则，在空扫描后，停用的工作线程会使用自适应的本地自旋构造（请参见 `awaitWork`），然后阻塞（通过 `park`）。请注意，围绕停车和其他阻塞的 `Thread.interrupt` 处理的约定很不寻常：由于中断仅用于提醒线程检查终止（无论如何在阻塞时都会检查），我们在调用 `park` 之前清除状态（使用 `Thread.interrupted`），以防止因为状态已在用户代码中的其他不相关中断调用中被设置而导致 `park` 立即返回。
     *
     * ### 信号和激活：
     * 只有当有至少一个任务它们可能能够找到并执行时，工作线程才会被创建或激活。在将任务推送到一个先前可能为空的队列时（无论是由工作线程还是外部提交执行的），如果工作线程处于空闲状态，则会发送信号，如果现有的工作线程数量少于指定的并行度级别，则会创建新的工作线程。当其他线程从队列中移除任务并发现队列中还有其他任务时，也会发送这些辅助信号。在大多数平台上，发送信号（`unpark`）的开销时间相当长，并且在发送信号的线程开始真正取得进展之前的时间可能非常长，因此尽可能将这些延迟从关键路径中分离出来也是值得的。此外，由于停用的工作线程通常会重新扫描或自旋而不是阻塞，因此我们设置和清除了 `WorkQueues` 的 "parker" 字段以减少不必要的 `unpark` 调用。（这需要进行二次检查以避免丢失信号。）
     *
     * ### 修剪工作线程：
     * 为了在缺乏使用的时期释放资源，当池处于静止状态时，一个开始等待的工作线程如果池在空闲期间保持静止，将会超时并终止（请参见 `awaitWork`），并且随着线程数量的减少，静止的时间段会增加，最终删除所有工作线程。此外，当存在超过两个的备用线程时，多余的线程将在下一个静止点立即终止。（通过增加两个线程可以避免产生“滞后效应”。）
     *
     * ### 关闭和终止：
     * 调用 `shutdownNow` 会调用 `tryTerminate`，以原子方式设置一个 `runState` 位。调用线程以及随后终止的每个工作线程都会帮助终止其他工作线程，通过设置它们的（`qlock`）状态、取消它们未处理的任务，并唤醒它们，直到状态稳定（但循环的上限是工作线程的数量）。对非突然关闭的 `shutdown()` 调用在此之前检查是否应开始终止。这主要依赖于 `ctl` 的活跃计数位来维持共识——每当工作线程静止时都会调用 `tryTerminate`。然而，外部提交者并不参与此共识。因此，`tryTerminate` 会遍历队列（直到状态稳定）以确保没有正在进行的提交和即将处理它们的工作线程，然后触发终止的 "STOP" 阶段。（注意：如果在启用关闭时调用 `helpQuiescePool` 会产生内在冲突。两者都等待静止状态，但 `tryTerminate` 偏向于在 `helpQuiescePool` 完成之前不触发。）
     *
     * ### 加入任务
     * ============
     *
     * 当一个工作线程等待合并一个被其他线程偷走（或始终持有）的任务时，可以采取几种操作。由于我们将许多任务多路复用到一个工作线程池中，我们不能像 `Thread.join` 那样让它们阻塞。我们也不能简单地重新分配合并线程的运行时堆栈并稍后再替换它，这将是某种形式的“延续”（continuation），即使可能，也未必是个好主意，因为我们可能同时需要未阻塞的任务和它的延续来继续执行。因此，我们结合了两种策略：
     *
     * 1. **帮助 (Helping)**：安排合并线程执行它本来会运行的某些任务，假设没有发生窃取。
     *
     * 2. **补偿 (Compensating)**：除非已经有足够的活动线程，否则 `tryCompensate()` 方法可能会创建或重新激活一个备用线程，以在合并线程阻塞期间补偿它，直到它被解除阻塞。
     *
     * 第三种形式（在 `tryRemoveAndExec` 中实现）相当于帮助一个假设的补偿线程：如果我们可以很容易地确定补偿线程的一个可能动作是偷取并执行要合并的任务，那么合并线程可以直接执行它，而不需要补偿线程（虽然以牺牲更大的运行时堆栈为代价，但这种权衡通常是值得的）。
     *
     * 扩展 API `ManagedBlocker` 不能使用帮助机制，因此仅在方法 `awaitBlocker` 中依赖补偿。
     *
     * `helpStealer` 算法采用了一种“线性帮助”形式。每个工作线程都会在 `currentSteal` 字段中记录其最近从其他工作线程（或提交）偷走的任务。它还在 `currentJoin` 字段中记录当前正在合并的任务。方法 `helpStealer` 使用这些标记来尝试找到一个可以帮助的工作线程（即，从它偷回一个任务并执行它），以加快正在合并的任务的完成。因此，合并线程执行的任务将是如果没有发生被合并的任务被偷走的情况下，它在自己的本地双端队列中执行的任务。这是 Wagner 和 Calder 在 1993 年 SIGPLAN Notices 中的文章 "Leapfrogging: a portable technique for implementing efficient futures" 中描述的方法的保守变体。它的不同之处在于：
     * 1. 我们仅在任务被偷走时维护工作线程之间的依赖链接，而不是为每个任务进行记账。这有时需要对 `workQueues` 数组进行线性扫描以定位偷取者，但通常不需要，因为偷取者会留下可能变得陈旧/错误的线索，指示可以在哪里找到它们。这仅仅是一个提示，因为工作线程可能进行了多次窃取，提示只记录其中之一（通常是最近的一次）。提示将成本限制在需要时，而不是增加每个任务的开销。
     * 2. 它是“浅层的”，忽略嵌套和可能的循环互相偷取。
     * 3. 它有意地带有竞争性：`currentJoin` 字段仅在活动合并时更新，这意味着我们在长时间任务期间错过了链条中的某些链接，GC 停顿等（这是可以的，因为在这种情况下通常阻塞是一个好主意）。
     * 4. 我们将尝试寻找工作的次数设置了上限，使用校验和，并回退到挂起工作线程，如果有必要，再用另一个线程替换它。
     *
     * 对于 `CountedCompleters` 的帮助操作不需要跟踪 `currentJoins`：方法 `helpComplete` 获取并执行具有与等待中的任务相同根的任何任务（优先从本地弹出，而不是非本地偷取）。然而，这仍然需要遍历完成者链，因此效率不如使用不显式合并的 `CountedCompleters`。
     *
     * 补偿机制并不旨在随时保持恰好目标并行度数量的未阻塞线程运行。该类的某些先前版本为任何阻塞合并立即提供补偿。然而，实际上，绝大多数阻塞是 GC 和其他 JVM 或操作系统活动的暂时副作用，由于替换会加剧这种情况。因此，当前只有在验证所有自称活动的线程都在处理任务时才尝试补偿，通过检查字段 `WorkQueue.scanState` 消除大多数误报。此外，在最常见的情况下，当一个队列为空的工作线程（因此没有后续任务）阻塞在合并上时，并且仍然有足够的线程来确保存活时，补偿将被绕过（容忍较少的线程）。
     *
     * 补偿机制可能会受到限制。对于公共池（参见 `commonMaxSpares`），此限制更好地让 JVM 应对编程错误和滥用问题，以防耗尽资源。在其他情况下，用户可以提供限制线程创建的工厂。此池中的边界（如同所有其他池一样）是不精确的。线程总数是在工作线程注销时递减的，而不是它们退出并由 JVM 和操作系统回收资源时。因此，同时存在的活动线程数可能会暂时超出限制。
     *
     * ### 公共池
     * ===========
     *
     * 公共池在静态初始化之后始终存在。由于它（或任何其他创建的池）不需要被使用，因此我们将初始构造开销和内存占用最小化到大约十几个字段的设置，而没有嵌套分配。大多数的引导过程发生在首次提交给池时的 `externalSubmit` 方法中。
     *
     * 当外部线程提交到公共池时，它们可以在合并任务时执行子任务处理（参见 `externalHelpComplete` 及相关方法）。这种调用者帮助策略使得将公共池并行度设置为比可用内核总数少一或更多，甚至将并行度设置为零（即纯粹的调用者运行）变得合理。我们不需要记录外部提交是否针对公共池——如果不是，外部帮助方法会快速返回。这些提交者将会被阻塞等待任务完成，因此额外的努力（通过广泛的任务状态检查）在不适用的情况下相当于一种在 `ForkJoinTask.join` 中阻塞前的有限自旋等待。
     *
     * 作为受管理环境中更合适的默认选择，除非通过系统属性覆盖，在存在 `SecurityManager` 时，我们使用 `InnocuousForkJoinWorkerThread` 子类的工作线程。这些工作线程没有设置权限，不属于任何用户定义的线程组，并且在执行完任何顶层任务后会清除所有 `ThreadLocal`（参见 `WorkQueue.runTask`）。关联的机制（主要在 `ForkJoinWorkerThread` 中）可能是依赖于 JVM 的，必须访问特定的 `Thread` 类字段才能实现此效果。
     *
     * ### 风格说明
     * ===========
     *
     * 内存排序主要依赖于 Unsafe 内部实现，后者进一步负责显式执行通常由 JVM 隐式处理的空和边界检查。这可能会很尴尬且丑陋，但也反映了在非常竞争激烈且几乎没有不变量的代码中控制结果的需求。因此这些显式检查无论如何都会以某种形式存在。所有字段都在使用前被读取到本地变量中，并且如果它们是引用，则进行空检查。这通常以 "C" 风格的方式进行，在方法或代码块的头部列出声明，并在第一次遇到时使用内联赋值。数组边界检查通常通过使用 `array.length - 1` 进行掩码操作，依赖于这些数组是以正长度创建的，这本身经过偏执的检查。几乎所有的显式检查都导致跳过或返回，而不是抛出异常，因为它们可能由于关闭期间的取消/撤销而合法出现。
     *
     * 在 `ForkJoinPool`、`ForkJoinWorkerThread` 和 `ForkJoinTask` 类之间存在大量的表示级耦合。`WorkQueue` 的字段维护由 `ForkJoinPool` 管理的数据结构，因此可以直接访问。试图减少这种耦合几乎没有意义，因为任何未来在表示上的变更都需要伴随算法的更改。一些方法本质上比较复杂，因为它们必须累积一组一致的字段读取，存储在局部变量中。还有一些代码怪癖（包括一些看似不必要的提升的空检查），这有助于某些方法在解释执行（而不是编译执行）时表现良好。
     *
     * 类中声明的顺序是（有少数例外）：
     * (1) 静态工具函数
     * (2) 嵌套（静态）类
     * (3) 静态字段
     * (4) 字段，以及用于解包其中一些字段的常量
     * (5) 内部控制方法
     * (6) 回调以及对 `ForkJoinTask` 方法的支持
     * (7) 导出方法
     * (8) 静态块初始化静态变量，按最小依赖顺序初始化
     */

    // 静态工具类

    /**
     * 如果存在安全管理器，确保调用者有修改线程的权限。
     */
    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(modifyThreadPermission);
    }

    // 内部类

    /**
     * 创建新 {@link ForkJoinWorkerThread} 的工厂。
     * 对于扩展了基础功能或使用不同上下文初始化线程的
     * {@code ForkJoinWorkerThread} 子类，必须定义并使用
     * {@code ForkJoinWorkerThreadFactory}。
     */
    public static interface ForkJoinWorkerThreadFactory {
        /**
         * 返回在给定池中运行的新工作线程。
         *
         * @param pool 该线程工作的线程池
         * @return 新的工作线程
         * @throws NullPointerException 如果线程池为 null
         */
        public ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    /**
     * 默认的 ForkJoinWorkerThreadFactory 实现；创建一个新的 ForkJoinWorkerThread。
     */
    static final class DefaultForkJoinWorkerThreadFactory
            implements ForkJoinWorkerThreadFactory {
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    /**
     * 用于替换 WorkQueue.tryRemoveAndExec 中移除的目标任务的人工任务类。
     * 我们不需要代理执行任何操作，只需具有唯一身份即可。
     */
    static final class EmptyTask extends ForkJoinTask<Void> {
        private static final long serialVersionUID = -7721805057305804111L;
        EmptyTask() { status = NORMAL; } // 强制标记完成
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void x) {}
        public final boolean exec() { return true; }
    }

    // 在 ForkJoinPool 和 WorkQueue 之间共享的常量

    // 边界
    static final int SMASK        = 0xffff;        // short 位 == 最大索引
    static final int MAX_CAP      = 0x7fff;        // 最大工作线程数量 - 1
    static final int EVENMASK     = 0xfffe;        // 偶数 short 位
    static final int SQMASK       = 0x007e;        // 最多 64 (偶数) 槽位

    // 用于 WorkQueue.scanState 和 ctl sp 子字段的掩码和单元
    static final int SCANNING     = 1;             // 运行任务时为 false
    static final int INACTIVE     = 1 << 31;       // 必须为负数
    static final int SS_SEQ       = 1 << 16;       // 版本计数

    // ForkJoinPool.config 和 WorkQueue.config 的模式位
    static final int MODE_MASK    = 0xffff << 16;  // int 的高半部分
    static final int LIFO_QUEUE   = 0;
    static final int FIFO_QUEUE   = 1 << 16;
    static final int SHARED_QUEUE = 1 << 31;       // 必须为负数

    /**
     * 支持工作窃取以及外部任务提交的队列。
     * 性能在大多数平台上对 WorkQueues 实例及其数组的位置非常敏感 —— 我们绝对不希望
     * 多个 WorkQueue 实例或多个队列数组共享缓存行。
     * @Contended 注解会提醒 JVM 尽量将实例分开存放。
     */
    @sun.misc.Contended
    static final class WorkQueue {

        /**
         * 初始化时工作窃取队列数组的容量。必须为 2 的幂；
         * 至少为 4，但应更大，以减少或消除队列间的缓存行共享。
         * 目前，它更大，作为 JVM 通常在共享 GC 记账的位置
         * （特别是 cardmarks）放置数组的部分解决方案，
         * 因此每次写入访问都会遇到严重的内存争用问题。
         */
        static final int INITIAL_QUEUE_CAPACITY = 1 << 13;

        /**
         * 队列数组的最大大小。必须是小于或等于 1 << (31 - 数组项宽度) 的 2 的幂，
         * 以确保索引计算不会溢出，但定义为略小的值，以帮助用户在系统饱和之前
         * 捕获失控的程序。
         */
        static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26; // 64M

        // 实例字段
        volatile int scanState;    // 版本化，<0: 非活动；奇数：扫描中
        int stackPred;             // 池堆栈 (ctl) 的前驱
        int nsteals;               // 窃取的数量
        int hint;                  // 随机化和窃取者索引提示
        int config;                // 池索引和模式
        volatile int qlock;        // 1: 锁定，< 0: 终止；否则为 0
        volatile int base;         // 下一个槽位的索引
        int top;                   // 推送的下一个槽位的索引
        ForkJoinTask<?>[] array;   // 元素（最初未分配）
        final ForkJoinPool pool;   // 所在的池（可能为 null）
        final ForkJoinWorkerThread owner; // 所有者线程或 null（如果共享）
        volatile Thread parker;    // 在调用 park 时为 owner；否则为 null
        volatile ForkJoinTask<?> currentJoin;  // 正在 awaitJoin 中加入的任务
        volatile ForkJoinTask<?> currentSteal; // 主要用于 helpStealer

        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
            this.pool = pool;
            this.owner = owner;
            // 将索引放置在数组中心（尚未分配）
            base = top = INITIAL_QUEUE_CAPACITY >>> 1;
        }

        /**
         * 返回可导出的索引（由 ForkJoinWorkerThread 使用）。
         */
        final int getPoolIndex() {
            return (config & 0xffff) >>> 1; // 忽略奇数/偶数标签位
        }

        /**
         * 返回队列中的任务数量。
         */
        final int queueSize() {
            int n = base - top;       // 非所有者调用者必须先读取 base
            return (n >= 0) ? 0 : -n; // 忽略瞬时负值
        }

        /**
         * 通过检查一个接近空的队列是否至少有一个未被声明的任务，
         * 提供比 queueSize 更准确的队列中是否有任务的估计。
         */
        final boolean isEmpty() {
            ForkJoinTask<?>[] a; int n, m, s;
            return ((n = base - (s = top)) >= 0 ||
                    (n == -1 &&           // 可能有一个任务
                            ((a = array) == null || (m = a.length - 1) < 0 ||
                                    U.getObject
                                            (a, (long)((m & (s - 1)) << ASHIFT) + ABASE) == null)));
        }

        /**
         * 推送任务。仅由所有者在非共享队列中调用。
         *
         * @param task 任务。调用者必须确保非空。
         * @throws RejectedExecutionException 如果数组无法调整大小
         */
        final void push(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a; ForkJoinPool p;
            int b = base, s = top, n;
            if ((a = array) != null) {    // 如果队列被移除则忽略
                int m = a.length - 1;     // 栅栏写入以确保任务可见性
                U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
                U.putOrderedInt(this, QTOP, s + 1);
                if ((n = s - b) <= 1) {
                    if ((p = pool) != null)
                        p.signalWork(p.workQueues, this);
                }
                else if (n >= m)
                    growArray();
            }
        }

        /**
         * 初始化或加倍数组的容量。由所有者或在持有锁的情况下调用 ——
         * 在调整大小时 base 可以移动，但 top 不能。
         */
        final ForkJoinTask<?>[] growArray() {
            ForkJoinTask<?>[] oldA = array;
            int size = oldA != null ? oldA.length << 1 : INITIAL_QUEUE_CAPACITY;
            if (size > MAXIMUM_QUEUE_CAPACITY)
                throw new RejectedExecutionException("队列容量超出限制");
            int oldMask, t, b;
            ForkJoinTask<?>[] a = array = new ForkJoinTask<?>[size];
            if (oldA != null && (oldMask = oldA.length - 1) >= 0 &&
                    (t = top) - (b = base) > 0) {
                int mask = size - 1;
                do { // 模拟从旧数组轮询，推送到新数组
                    ForkJoinTask<?> x;
                    int oldj = ((b & oldMask) << ASHIFT) + ABASE;
                    int j    = ((b &    mask) << ASHIFT) + ABASE;
                    x = (ForkJoinTask<?>)U.getObjectVolatile(oldA, oldj);
                    if (x != null &&
                            U.compareAndSwapObject(oldA, oldj, x, null))
                        U.putObjectVolatile(a, j, x);
                } while (++b != t);
            }
            return a;
        }
        /**
         * 以 LIFO 顺序获取下一个任务（如果存在）。仅由所有者在非共享队列中调用。
         */
        final ForkJoinTask<?> pop() {
            ForkJoinTask<?>[] a; ForkJoinTask<?> t; int m;
            if ((a = array) != null && (m = a.length - 1) >= 0) {
                for (int s; (s = top - 1) - base >= 0;) {
                    long j = ((m & s) << ASHIFT) + ABASE;
                    if ((t = (ForkJoinTask<?>)U.getObject(a, j)) == null)
                        break;
                    if (U.compareAndSwapObject(a, j, t, null)) {
                        U.putOrderedInt(this, QTOP, s);
                        return t;
                    }
                }
            }
            return null;
        }

        /**
         * 如果 b 是队列的 base 且任务可以在没有竞争的情况下被领取，则以 FIFO 顺序获取任务。
         * 专用版本出现在 ForkJoinPool 的 scan 和 helpStealer 方法中。
         */
        final ForkJoinTask<?> pollAt(int b) {
            ForkJoinTask<?> t; ForkJoinTask<?>[] a;
            if ((a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((t = (ForkJoinTask<?>)U.getObjectVolatile(a, j)) != null &&
                        base == b && U.compareAndSwapObject(a, j, t, null)) {
                    base = b + 1;
                    return t;
                }
            }
            return null;
        }

        /**
         * 以 FIFO 顺序获取下一个任务（如果存在）。
         */
        final ForkJoinTask<?> poll() {
            ForkJoinTask<?>[] a; int b; ForkJoinTask<?> t;
            while ((b = base) - top < 0 && (a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                t = (ForkJoinTask<?>)U.getObjectVolatile(a, j);
                if (base == b) {
                    if (t != null) {
                        if (U.compareAndSwapObject(a, j, t, null)) {
                            base = b + 1;
                            return t;
                        }
                    } else if (b + 1 == top) // 现在为空
                        break;
                }
            }
            return null;
        }

        /**
         * 按模式指定的顺序获取下一个本地任务（如果存在）。
         */
        final ForkJoinTask<?> nextLocalTask() {
            return (config & FIFO_QUEUE) == 0 ? pop() : poll();
        }

        /**
         * 按模式指定的顺序返回下一个任务（如果存在）。
         */
        final ForkJoinTask<?> peek() {
            ForkJoinTask<?>[] a = array; int m;
            if (a == null || (m = a.length - 1) < 0)
                return null;
            int i = (config & FIFO_QUEUE) == 0 ? top - 1 : base;
            int j = ((i & m) << ASHIFT) + ABASE;
            return (ForkJoinTask<?>)U.getObjectVolatile(a, j);
        }

        /**
         * 仅当给定任务位于当前 top 时，弹出该任务。
         * （共享版本仅通过 FJP.tryExternalUnpush 提供）
         */
        final boolean tryUnpush(ForkJoinTask<?> t) {
            ForkJoinTask<?>[] a; int s;
            if ((a = array) != null && (s = top) != base &&
                    U.compareAndSwapObject
                            (a, (((a.length - 1) & --s) << ASHIFT) + ABASE, t, null)) {
                U.putOrderedInt(this, QTOP, s);
                return true;
            }
            return false;
        }

        /**
         * 移除并取消所有已知的任务，忽略任何异常。
         */
        final void cancelAll() {
            ForkJoinTask<?> t;
            if ((t = currentJoin) != null) {
                currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            if ((t = currentSteal) != null) {
                currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            while ((t = poll()) != null)
                ForkJoinTask.cancelIgnoringExceptions(t);
        }

        // 专用执行方法

        /**
         * 轮询并运行所有任务直到队列为空。
         */
        final void pollAndExecAll() {
            for (ForkJoinTask<?> t; (t = poll()) != null;)
                t.doExec();
        }

        /**
         * 移除并执行所有本地任务。如果为 LIFO，调用 pollAndExecAll。
         * 否则实现一个专用的 pop 循环，直到队列为空为止执行任务。
         */
        final void execLocalTasks() {
            int b = base, m, s;
            ForkJoinTask<?>[] a = array;
            if (b - (s = top - 1) <= 0 && a != null &&
                    (m = a.length - 1) >= 0) {
                if ((config & FIFO_QUEUE) == 0) {
                    for (ForkJoinTask<?> t;;) {
                        if ((t = (ForkJoinTask<?>)U.getAndSetObject
                                (a, ((m & s) << ASHIFT) + ABASE, null)) == null)
                            break;
                        U.putOrderedInt(this, QTOP, s);
                        t.doExec();
                        if (base - (s = top - 1) > 0)
                            break;
                    }
                }
                else
                    pollAndExecAll();
            }
        }

        /**
         * 执行给定任务并执行剩余的本地任务。
         */
        final void runTask(ForkJoinTask<?> task) {
            if (task != null) {
                scanState &= ~SCANNING; // 标记为忙碌
                (currentSteal = task).doExec();
                U.putOrderedObject(this, QCURRENTSTEAL, null); // 释放以供 GC
                execLocalTasks();
                ForkJoinWorkerThread thread = owner;
                if (++nsteals < 0)      // 收集溢出时
                    transferStealCount(pool);
                scanState |= SCANNING;
                if (thread != null)
                    thread.afterTopLevelExec();
            }
        }

        /**
         * 将窃取计数添加到池的 stealCounter（如果存在），并重置计数。
         */
        final void transferStealCount(ForkJoinPool p) {
            AtomicLong sc;
            if (p != null && (sc = p.stealCounter) != null) {
                int s = nsteals;
                nsteals = 0;            // 如果为负数，修正为溢出
                sc.getAndAdd((long)(s < 0 ? Integer.MAX_VALUE : s));
            }
        }

        /**
         * 如果任务存在，则将其从队列中移除并执行，或执行任何其他已取消的任务。
         * 仅由 awaitJoin 使用。
         *
         * @return 如果队列为空且任务未被视为已完成，则返回 true
         */
        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a; int m, s, b, n;
            if ((a = array) != null && (m = a.length - 1) >= 0 &&
                    task != null) {
                while ((n = (s = top) - (b = base)) > 0) {
                    for (ForkJoinTask<?> t;;) {      // 从 s 到 b 依次遍历
                        long j = ((--s & m) << ASHIFT) + ABASE;
                        if ((t = (ForkJoinTask<?>)U.getObject(a, j)) == null)
                            return s + 1 == top;     // 比预期更短
                        else if (t == task) {
                            boolean removed = false;
                            if (s + 1 == top) {      // 弹出
                                if (U.compareAndSwapObject(a, j, task, null)) {
                                    U.putOrderedInt(this, QTOP, s);
                                    removed = true;
                                }
                            }
                            else if (base == b)      // 用代理替换
                                removed = U.compareAndSwapObject(
                                        a, j, task, new EmptyTask());
                            if (removed)
                                task.doExec();
                            break;
                        }
                        else if (t.status < 0 && s + 1 == top) {
                            if (U.compareAndSwapObject(a, j, t, null))
                                U.putOrderedInt(this, QTOP, s);
                            break;                  // 被取消
                        }
                        if (--n == 0)
                            return false;
                    }
                    if (task.status < 0)
                        return false;
                }
            }
            return true;
        }

        /**
         * 如果与给定任务处于同一 CC 计算中，则弹出任务，
         * 无论是在共享模式还是拥有模式下。仅由 helpComplete 使用。
         */
        final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
            int s; ForkJoinTask<?>[] a; Object o;
            if (base - (s = top) < 0 && (a = array) != null) {
                long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
                if ((o = U.getObjectVolatile(a, j)) != null &&
                        (o instanceof CountedCompleter)) {
                    CountedCompleter<?> t = (CountedCompleter<?>)o;
                    for (CountedCompleter<?> r = t;;) {
                        if (r == task) {
                            if (mode < 0) { // 必须加锁
                                if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                                    if (top == s && array == a &&
                                            U.compareAndSwapObject(a, j, t, null)) {
                                        U.putOrderedInt(this, QTOP, s - 1);
                                        U.putOrderedInt(this, QLOCK, 0);
                                        return t;
                                    }
                                    U.compareAndSwapInt(this, QLOCK, 1, 0);
                                }
                            }
                            else if (U.compareAndSwapObject(a, j, t, null)) {
                                U.putOrderedInt(this, QTOP, s - 1);
                                return t;
                            }
                            break;
                        }
                        else if ((r = r.completer) == null) // 尝试父任务
                            break;
                    }
                }
            }
            return null;
        }

        /**
         * 如果存在与给定任务处于同一 CC 计算中的任务，并且可以无争用地获取任务，则窃取并运行该任务。
         * 否则返回用于 helpComplete 方法的校验和/控制值。
         *
         * @return 如果成功则返回 1，如果可重试（被另一个窃取者抢走）则返回 2，
         * 如果非空但未找到匹配任务则返回 -1，否则返回 base 索引，并强制为负值。
         */
        final int pollAndExecCC(CountedCompleter<?> task) {
            int b, h; ForkJoinTask<?>[] a; Object o;
            if ((b = base) - top >= 0 || (a = array) == null)
                h = b | Integer.MIN_VALUE;  // 用于感知重新轮询时的移动
            else {
                long j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((o = U.getObjectVolatile(a, j)) == null)
                    h = 2;                  // 可重试
                else if (!(o instanceof CountedCompleter))
                    h = -1;                 // 不匹配
                else {
                    CountedCompleter<?> t = (CountedCompleter<?>)o;
                    for (CountedCompleter<?> r = t;;) {
                        if (r == task) {
                            if (base == b &&
                                    U.compareAndSwapObject(a, j, t, null)) {
                                base = b + 1;
                                t.doExec();
                                h = 1;      // 成功
                            }
                            else
                                h = 2;      // CAS 失败
                            break;
                        }
                        else if ((r = r.completer) == null) {
                            h = -1;         // 不匹配
                            break;
                        }
                    }
                }
            }
            return h;
        }

        /**
         * 如果拥有任务且未知为阻塞，则返回 true。
         */
        final boolean isApparentlyUnblocked() {
            Thread wt; Thread.State s;
            return (scanState >= 0 &&
                    (wt = owner) != null &&
                    (s = wt.getState()) != Thread.State.BLOCKED &&
                    s != Thread.State.WAITING &&
                    s != Thread.State.TIMED_WAITING);
        }


        // Unsafe机制
        private static final sun.misc.Unsafe U;
        private static final int  ABASE;
        private static final int  ASHIFT;
        private static final long QTOP;
        private static final long QLOCK;
        private static final long QCURRENTSTEAL;
        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> wk = WorkQueue.class;
                Class<?> ak = ForkJoinTask[].class;
                QTOP = U.objectFieldOffset
                        (wk.getDeclaredField("top"));
                QLOCK = U.objectFieldOffset
                        (wk.getDeclaredField("qlock"));
                QCURRENTSTEAL = U.objectFieldOffset
                        (wk.getDeclaredField("currentSteal"));
                ABASE = U.arrayBaseOffset(ak);
                int scale = U.arrayIndexScale(ak);
                if ((scale & (scale - 1)) != 0)
                    throw new Error("数据类型比例不是 2 的幂");
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    // 静态字段 (在下面的静态初始化块中初始化)

    /**
     * 创建一个新的 ForkJoinWorkerThread。除非在 ForkJoinPool 构造函数中被覆盖，
     * 否则使用该工厂。
     */
    public static final ForkJoinWorkerThreadFactory
            defaultForkJoinWorkerThreadFactory;

    /**
     * 调用可能启动或终止线程的方法所需的权限。
     */
    private static final RuntimePermission modifyThreadPermission;

    /**
     * 公共（静态）池。除非静态构造时发生异常，否则该池用于公共用途。
     * 但内部使用时会进行空检查，以偏执地避免可能的初始化循环，同时简化生成的代码。
     */
    static final ForkJoinPool common;

    /**
     * 公共池的并行度。当公共池线程被禁用时，允许底层的 common.parallelism 字段为零，
     * 但在这种情况下仍报告并行度为 1，以反映调用方执行的机制。
     */
    static final int commonParallelism;

    /**
     * tryCompensate 中备用线程构建的限制。
     */
    private static int commonMaxSpares;

    /**
     * 用于创建 workerNamePrefix 的序列号。
     */
    private static int poolNumberSequence;

    /**
     * 返回下一个序列号。我们不期望它会争用，因此使用简单的内置同步机制。
     */
    private static final synchronized int nextPoolId() {
        return ++poolNumberSequence;
    }

    // 静态配置常量

    /**
     * 触发静止的线程停车等待新工作的初始超时值（以纳秒为单位）。
     * 超时时间足够大，避免大多数短暂的停滞（如长时间 GC 等）时过于激进地缩减工作线程。
     */
    private static final long IDLE_TIMEOUT = 2000L * 1000L * 1000L; // 2秒

    /**
     * 空闲超时的容差，处理定时器的误差。
     */
    private static final long TIMEOUT_SLOP = 20L * 1000L * 1000L;  // 20毫秒

    /**
     * 静态初始化期间 commonMaxSpares 的初始值。该值远远超出了正常需求，
     * 但仍远低于 MAX_CAP 和典型的操作系统线程限制，因此允许 JVM 在资源耗尽前捕获滥用情况。
     */
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;

    /**
     * 在阻塞之前等待的自旋次数。当前自旋使用随机化的自旋。
     * 目前设为零以减少 CPU 使用。
     *
     * 如果大于零，SPINS 的值必须是 2 的幂，至少为 4。
     * 2048 的值会导致自旋持续短时间的上下文切换。
     */
    private static final int SPINS  = 0;

    /**
     * 用于生成种子的增量。参见 ThreadLocal 类的说明。
     */
    private static final int SEED_INCREMENT = 0x9e3779b9;

    /*
     * ctl 字段的位和掩码，包含 4 个 16 位子字段：
     * AC: 活动运行工作线程数减去目标并行度
     * TC: 总工作线程数减去目标并行度
     * SS: 顶层等待线程的版本计数和状态
     * ID: 堆栈顶层等待线程的 poolIndex
     *
     * 当方便时，可以提取较低的 32 位堆栈顶位（包括版本位），即 sp=(int)ctl。
     * 通过使用目标并行度进行计数偏移和字段定位，使得可以通过字段的符号测试
     * 执行最常见的检查：当 ac 为负数时，没有足够的活动工作线程；
     * 当 tc 为负数时，没有足够的工作线程；当 sp 非零时，有等待的工作线程。
     *
     * 由于占据了最高位，因此在从阻塞的 join 返回时，可以使用 AC_UNIT 的 getAndAddLong
     * 而不是 CAS 来增加一个活动计数。其他更新涉及多个子字段和掩码，需要使用 CAS。
     */

    // 下位字和上位字的掩码
    private static final long SP_MASK    = 0xffffffffL;
    private static final long UC_MASK    = ~SP_MASK;

    // 活动计数
    private static final int  AC_SHIFT   = 48;
    private static final long AC_UNIT    = 0x0001L << AC_SHIFT;
    private static final long AC_MASK    = 0xffffL << AC_SHIFT;

    // 总计数
    private static final int  TC_SHIFT   = 32;
    private static final long TC_UNIT    = 0x0001L << TC_SHIFT;
    private static final long TC_MASK    = 0xffffL << TC_SHIFT;
    private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15); // 符号位

    // runState 位：SHUTDOWN 必须为负数，其他任意为 2 的幂
    private static final int  RSLOCK     = 1;
    private static final int  RSIGNAL    = 1 << 1;
    private static final int  STARTED    = 1 << 2;
    private static final int  STOP       = 1 << 29;
    private static final int  TERMINATED = 1 << 30;
    private static final int  SHUTDOWN   = 1 << 31;

    // 实例字段
    volatile long ctl;                   // 主池控制
    volatile int runState;               // 可锁定状态
    final int config;                    // 并行度，模式
    int indexSeed;                       // 用于生成工作线程索引
    volatile WorkQueue[] workQueues;     // 主注册表
    final ForkJoinWorkerThreadFactory factory;
    final UncaughtExceptionHandler ueh;  // 每个工作线程的未捕获异常处理器
    final String workerNamePrefix;       // 用于创建工作线程名称的前缀
    volatile AtomicLong stealCounter;    // 也用作同步监视器

    /**
     * 获取 runState 锁；返回当前（已锁定的）runState。
     */
    private int lockRunState() {
        int rs;
        return ((((rs = runState) & RSLOCK) != 0 ||
                !U.compareAndSwapInt(this, RUNSTATE, rs, rs |= RSLOCK)) ?
                awaitRunStateLock() : rs);
    }

    /**
     * 自旋和/或阻塞，直到 runState 锁可用。参见上文解释。
     */
    private int awaitRunStateLock() {
        Object lock;
        boolean wasInterrupted = false;
        for (int spins = SPINS, r = 0, rs, ns;;) {
            if (((rs = runState) & RSLOCK) == 0) {
                if (U.compareAndSwapInt(this, RUNSTATE, rs, ns = rs | RSLOCK)) {
                    if (wasInterrupted) {
                        try {
                            Thread.currentThread().interrupt();
                        } catch (SecurityException ignore) {
                        }
                    }
                    return ns;
                }
            }
            else if (r == 0)
                r = ThreadLocalRandom.nextSecondarySeed();
            else if (spins > 0) {
                r ^= r << 6; r ^= r >>> 21; r ^= r << 7; // xorshift
                if (r >= 0)
                    --spins;
            }
            else if ((rs & STARTED) == 0 || (lock = stealCounter) == null)
                Thread.yield();   // 初始化竞争
            else if (U.compareAndSwapInt(this, RUNSTATE, rs, rs | RSIGNAL)) {
                synchronized (lock) {
                    if ((runState & RSIGNAL) != 0) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ie) {
                            if (!(Thread.currentThread() instanceof
                                    ForkJoinWorkerThread))
                                wasInterrupted = true;
                        }
                    }
                    else
                        lock.notifyAll();
                }
            }
        }
    }

    /**
     * 解锁并将 runState 设置为 newRunState。
     *
     * @param oldRunState 从 lockRunState 返回的值
     * @param newRunState 下一个值（必须清除锁位）。
     */
    private void unlockRunState(int oldRunState, int newRunState) {
        if (!U.compareAndSwapInt(this, RUNSTATE, oldRunState, newRunState)) {
            Object lock = stealCounter;
            runState = newRunState;              // 清除 RSIGNAL 位
            if (lock != null)
                synchronized (lock) { lock.notifyAll(); }
        }
    }

    // 创建、注册和注销工作线程

    /**
     * 尝试构建并启动一个工作线程。假设总计数已经作为保留值递增。
     * 在失败时调用 deregisterWorker。
     *
     * @return 如果成功则返回 true
     */
    private boolean createWorker() {
        ForkJoinWorkerThreadFactory fac = factory;
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try {
            if (fac != null && (wt = fac.newThread(this)) != null) {
                wt.start();
                return true;
            }
        } catch (Throwable rex) {
            ex = rex;
        }
        deregisterWorker(wt, ex);
        return false;
    }

    /**
     * 尝试增加一个工作线程，在此之前递增 ctl 计数，依赖 createWorker 在失败时回滚。
     *
     * @param c 传入的 ctl 值，总计数为负且没有空闲工作线程。
     *          在 CAS 失败时，如果条件满足，将刷新并重试 c 值。
     */
    private void tryAddWorker(long c) {
        boolean add = false;
        do {
            long nc = ((AC_MASK & (c + AC_UNIT)) |
                    (TC_MASK & (c + TC_UNIT)));
            if (ctl == c) {
                int rs, stop;                 // 检查是否终止
                if ((stop = (rs = lockRunState()) & STOP) == 0)
                    add = U.compareAndSwapLong(this, CTL, c, nc);
                unlockRunState(rs, rs & ~RSLOCK);
                if (stop != 0)
                    break;
                if (add) {
                    createWorker();
                    break;
                }
            }
        } while (((c = ctl) & ADD_WORKER) != 0L && (int)c == 0);
    }

    /**
     * 从 ForkJoinWorkerThread 构造函数的回调，用于建立并记录其 WorkQueue。
     *
     * @param wt 工作线程
     * @return 工作线程的队列
     */
    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        UncaughtExceptionHandler handler;
        wt.setDaemon(true);                           // 配置线程
        if ((handler = ueh) != null)
            wt.setUncaughtExceptionHandler(handler);
        WorkQueue w = new WorkQueue(this, wt);
        int i = 0;                                    // 分配池索引
        int mode = config & MODE_MASK;
        int rs = lockRunState();
        try {
            WorkQueue[] ws; int n;                    // 如果没有数组则跳过
            if ((ws = workQueues) != null && (n = ws.length) > 0) {
                int s = indexSeed += SEED_INCREMENT;  // 不太可能发生碰撞
                int m = n - 1;
                i = ((s << 1) | 1) & m;               // 奇数编号索引
                if (ws[i] != null) {                  // 碰撞
                    int probes = 0;                   // 步进约为 n 的一半
                    int step = (n <= 4) ? 2 : ((n >>> 1) & EVENMASK) + 2;
                    while (ws[i = (i + step) & m] != null) {
                        if (++probes >= n) {
                            workQueues = ws = Arrays.copyOf(ws, n <<= 1);
                            m = n - 1;
                            probes = 0;
                        }
                    }
                }
                w.hint = s;                           // 用作随机种子
                w.config = i | mode;
                w.scanState = i;                      // 发布栅栏
                ws[i] = w;
            }
        } finally {
            unlockRunState(rs, rs & ~RSLOCK);
        }
        wt.setName(workerNamePrefix.concat(Integer.toString(i >>> 1)));
        return w;
    }

    /**
     * 终止的工作线程的最终回调，或者在构建或启动工作线程失败时调用。
     * 从数组中移除工作线程的记录并调整计数。如果线程池正在关闭，尝试完成终止。
     *
     * @param wt 工作线程，如果构建失败则为 null
     * @param ex 导致失败的异常，如果没有则为 null
     */
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        WorkQueue w = null;
        if (wt != null && (w = wt.workQueue) != null) {
            WorkQueue[] ws;                           // 从数组中移除索引
            int idx = w.config & SMASK;
            int rs = lockRunState();
            if ((ws = workQueues) != null && ws.length > idx && ws[idx] == w)
                ws[idx] = null;
            unlockRunState(rs, rs & ~RSLOCK);
        }
        long c;                                       // 减少计数
        do {} while (!U.compareAndSwapLong
                (this, CTL, c = ctl, ((AC_MASK & (c - AC_UNIT)) |
                        (TC_MASK & (c - TC_UNIT)) |
                        (SP_MASK & c))));
        if (w != null) {
            w.qlock = -1;                             // 确保设置
            w.transferStealCount(this);
            w.cancelAll();                            // 取消剩余任务
        }
        for (;;) {                                    // 可能替换
            WorkQueue[] ws; int m, sp;
            if (tryTerminate(false, false) || w == null || w.array == null ||
                    (runState & STOP) != 0 || (ws = workQueues) == null ||
                    (m = ws.length - 1) < 0)              // 已经终止
                break;
            if ((sp = (int)(c = ctl)) != 0) {         // 唤醒替换线程
                if (tryRelease(c, ws[sp & m], AC_UNIT))
                    break;
            }
            else if (ex != null && (c & ADD_WORKER) != 0L) {
                tryAddWorker(c);                      // 创建替换
                break;
            }
            else                                      // 不需要替换
                break;
        }
        if (ex == null)                               // 在退出时帮助清理
            ForkJoinTask.helpExpungeStaleExceptions();
        else                                          // 抛出异常
            ForkJoinTask.rethrow(ex);
    }

    // 信号机制

    /**
     * 尝试创建或激活一个工作线程，如果活动的线程数量太少。
     *
     * @param ws 用于查找被唤醒线程的工作线程数组
     * @param q 如果非空，表示现在为空时不重试的工作队列
     */
    final void signalWork(WorkQueue[] ws, WorkQueue q) {
        long c; int sp, i; WorkQueue v; Thread p;
        while ((c = ctl) < 0L) {                       // 活动的线程太少
            if ((sp = (int)c) == 0) {                  // 没有空闲的工作线程
                if ((c & ADD_WORKER) != 0L)            // 工作线程数量太少
                    tryAddWorker(c);
                break;
            }
            if (ws == null)                            // 未启动/已终止
                break;
            if (ws.length <= (i = sp & SMASK))         // 已终止
                break;
            if ((v = ws[i]) == null)                   // 正在终止
                break;
            int vs = (sp + SS_SEQ) & ~INACTIVE;        // 下一个 scanState
            int d = sp - v.scanState;                  // 屏蔽 CAS
            long nc = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & v.stackPred);
            if (d == 0 && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = vs;                      // 激活 v
                if ((p = v.parker) != null)
                    U.unpark(p);
                break;
            }
            if (q != null && q.base == q.top)          // 没有更多的工作
                break;
        }
    }

    /**
     * 如果 v 是空闲工作线程栈的顶部，则唤醒并释放工作线程 v。
     * 这个方法执行 signalWork 的一次性版本，仅在（显然）至少有一个空闲线程时。
     *
     * @param c 传入的 ctl 值
     * @param v 如果非空，表示一个工作线程
     * @param inc 增加的活动线程数（补偿时为零）
     * @return 如果成功，返回 true
     */
    private boolean tryRelease(long c, WorkQueue v, long inc) {
        int sp = (int)c, vs = (sp + SS_SEQ) & ~INACTIVE; Thread p;
        if (v != null && v.scanState == sp) {          // v 是栈顶
            long nc = (UC_MASK & (c + inc)) | (SP_MASK & v.stackPred);
            if (U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = vs;
                if ((p = v.parker) != null)
                    U.unpark(p);
                return true;
            }
        }
        return false;
    }

    // 任务扫描

    /**
     * 工作线程的顶层运行循环，由 ForkJoinWorkerThread.run 调用。
     */
    final void runWorker(WorkQueue w) {
        w.growArray();                   // 分配队列
        int seed = w.hint;               // 初始包含随机化提示
        int r = (seed == 0) ? 1 : seed;  // 避免 0 作为 xorShift
        for (ForkJoinTask<?> t;;) {
            if ((t = scan(w, r)) != null)
                w.runTask(t);
            else if (!awaitWork(w, r))
                break;
            r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // xorshift
        }
    }

    /**
     * 扫描并尝试窃取顶层任务。扫描从随机位置开始，随机移动以应对明显的竞争，
     * 否则线性继续，直到两次连续遍历所有队列都为空，并且校验和相同（每个队列的基索引之和，在每次窃取时发生变化），
     * 此时工作线程尝试使自己失活并重新扫描，尝试在找到任务时重新激活（自己或其他工作线程）；否则返回 null 以等待工作。
     * 扫描尽量少接触内存，以减少对其他扫描线程的干扰。
     *
     * @param w 工作线程（通过其 WorkQueue）
     * @param r 随机种子
     * @return 任务，如果未找到则返回 null
     */
    private ForkJoinTask<?> scan(WorkQueue w, int r) {
        WorkQueue[] ws; int m;
        if ((ws = workQueues) != null && (m = ws.length - 1) > 0 && w != null) {
            int ss = w.scanState;                     // 初始为非负数
            for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0;;) {
                WorkQueue q; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
                int b, n; long c;
                if ((q = ws[k]) != null) {
                    if ((n = (b = q.base) - q.top) < 0 &&
                            (a = q.array) != null) {      // 非空
                        long i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                        if ((t = ((ForkJoinTask<?>)
                                U.getObjectVolatile(a, i))) != null &&
                                q.base == b) {
                            if (ss >= 0) {
                                if (U.compareAndSwapObject(a, i, t, null)) {
                                    q.base = b + 1;
                                    if (n < -1)       // 向其他线程发信号
                                        signalWork(ws, q);
                                    return t;
                                }
                            }
                            else if (oldSum == 0 &&   // 尝试激活
                                    w.scanState < 0)
                                tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);
                        }
                        if (ss < 0)                   // 刷新
                            ss = w.scanState;
                        r ^= r << 1; r ^= r >>> 3; r ^= r << 10;
                        origin = k = r & m;           // 移动并重新扫描
                        oldSum = checkSum = 0;
                        continue;
                    }
                    checkSum += b;
                }
                if ((k = (k + 1) & m) == origin) {    // 继续直到稳定
                    if ((ss >= 0 || (ss == (ss = w.scanState))) &&
                            oldSum == (oldSum = checkSum)) {
                        if (ss < 0 || w.qlock < 0)    // 已失活
                            break;
                        int ns = ss | INACTIVE;       // 尝试失活
                        long nc = ((SP_MASK & ns) |
                                (UC_MASK & ((c = ctl) - AC_UNIT)));
                        w.stackPred = (int)c;         // 保持前栈顶
                        U.putInt(w, QSCANSTATE, ns);
                        if (U.compareAndSwapLong(this, CTL, c, nc))
                            ss = ns;
                        else
                            w.scanState = ss;         // 回退
                    }
                    checkSum = 0;
                }
            }
        }
        return null;
    }

    /**
     * 可能会阻塞工作线程 w 以等待可窃取的任务，或者如果工作线程应该终止则返回 false。
     * 如果使 w 失活导致线程池进入静止状态，检查池的终止状态，并在不是唯一工作线程的情况下，最多等待给定的持续时间。
     * 超时时，如果 ctl 未更改，终止工作线程，该线程将唤醒另一个工作线程可能重复此过程。
     *
     * @param w 调用的工作线程
     * @param r 随机种子（用于自旋）
     * @return 如果工作线程应终止则返回 false
     */
    private boolean awaitWork(WorkQueue w, int r) {
        if (w == null || w.qlock < 0)                 // w 正在终止
            return false;
        for (int pred = w.stackPred, spins = SPINS, ss;;) {
            if ((ss = w.scanState) >= 0)
                break;
            else if (spins > 0) {
                r ^= r << 6; r ^= r >>> 21; r ^= r << 7;
                if (r >= 0 && --spins == 0) {         // 随机化自旋
                    WorkQueue v; WorkQueue[] ws; int s, j; AtomicLong sc;
                    if (pred != 0 && (ws = workQueues) != null &&
                            (j = pred & SMASK) < ws.length &&
                            (v = ws[j]) != null &&        // 检查 pred 是否在停车
                            (v.parker == null || v.scanState >= 0))
                        spins = SPINS;                // 继续自旋
                }
            }
            else if (w.qlock < 0)                     // 自旋后重新检查
                return false;
            else if (!Thread.interrupted()) {
                long c, prevctl, parkTime, deadline;
                int ac = (int)((c = ctl) >> AC_SHIFT) + (config & SMASK);
                if ((ac <= 0 && tryTerminate(false, false)) ||
                        (runState & STOP) != 0)           // 池正在终止
                    return false;
                if (ac <= 0 && ss == (int)c) {        // 是最后一个等待者
                    prevctl = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & pred);
                    int t = (short)(c >>> TC_SHIFT);  // 收缩多余的备用线程
                    if (t > 2 && U.compareAndSwapLong(this, CTL, c, prevctl))
                        return false;                 // 否则使用定时等待
                    parkTime = IDLE_TIMEOUT * ((t >= 0) ? 1 : 1 - t);
                    deadline = System.nanoTime() + parkTime - TIMEOUT_SLOP;
                }
                else
                    prevctl = parkTime = deadline = 0L;
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);   // 模拟 LockSupport
                w.parker = wt;
                if (w.scanState < 0 && ctl == c)      // 停车前重新检查
                    U.park(false, parkTime);
                U.putOrderedObject(w, QPARKER, null);
                U.putObject(wt, PARKBLOCKER, null);
                if (w.scanState >= 0)
                    break;
                if (parkTime != 0L && ctl == c &&
                        deadline - System.nanoTime() <= 0L &&
                        U.compareAndSwapLong(this, CTL, c, prevctl))
                    return false;                     // 收缩池
            }
        }
        return true;
    }

    // 加入任务

    /**
     * 尝试窃取并运行目标的计算内的任务。使用顶层算法的变体，限制为拥有给定任务作为祖先的任务：
     * 优先从工作线程自己的队列中弹出并运行符合条件的任务（通过 popCC）。否则，它扫描其他任务，
     * 在竞争或执行时随机移动，并根据校验和决定是否放弃（通过 pollAndExecCC 的返回码调整）。
     * maxTasks 参数支持外部使用；内部调用使用零，允许无限制步骤（外部调用会捕获非正数值）。
     *
     * @param w 调用者
     * @param maxTasks 如果非零，表示要运行的其他任务的最大数量
     * @return 任务状态
     */
    final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
        WorkQueue[] ws; int s = 0, m;
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 &&
                task != null && w != null) {
            int mode = w.config;                 // 用于 popCC
            int r = w.hint ^ w.top;              // 任意种子用于起始位置
            int origin = r & m;                  // 要扫描的第一个队列
            int h = 1;                           // 1: 已运行，>1: 竞争，<0: 哈希
            for (int k = origin, oldSum = 0, checkSum = 0;;) {
                CountedCompleter<?> p; WorkQueue q;
                if ((s = task.status) < 0)
                    break;
                if (h == 1 && (p = w.popCC(task, mode)) != null) {
                    p.doExec();                  // 运行本地任务
                    if (maxTasks != 0 && --maxTasks == 0)
                        break;
                    origin = k;                  // 重置
                    oldSum = checkSum = 0;
                }
                else {                           // 扫描其他队列
                    if ((q = ws[k]) == null)
                        h = 0;
                    else if ((h = q.pollAndExecCC(task)) < 0)
                        checkSum += h;
                    if (h > 0) {
                        if (h == 1 && maxTasks != 0 && --maxTasks == 0)
                            break;
                        r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // xorshift
                        origin = k = r & m;      // 移动并重启
                        oldSum = checkSum = 0;
                    }
                    else if ((k = (k + 1) & m) == origin) {
                        if (oldSum == (oldSum = checkSum))
                            break;
                        checkSum = 0;
                    }
                }
            }
        }
        return s;
    }

    /**
     * 尝试找到并执行任务的窃取者的任务，或者它的窃取者之一。
     * 通过当前的 currentSteal -> currentJoin 链，查找正在处理给定任务的后代并且有非空队列的线程，
     * 从中窃取并执行任务。此方法的第一次调用通常需要扫描/搜索（这没问题，因为加入者没什么其他可做的），
     * 但此方法在工作线程中留下提示以加快后续调用。
     *
     * @param w 调用者
     * @param task 要加入的任务
     */
    private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
        WorkQueue[] ws = workQueues;
        int oldSum = 0, checkSum, m;
        if (ws != null && (m = ws.length - 1) >= 0 && w != null &&
                task != null) {
            do {                                       // 重启点
                checkSum = 0;                          // 用于稳定性检查
                ForkJoinTask<?> subtask;
                WorkQueue j = w, v;                    // v 是子任务的窃取者
                descent: for (subtask = task; subtask.status >= 0; ) {
                    for (int h = j.hint | 1, k = 0, i; ; k += 2) {
                        if (k > m)                     // 找不到窃取者
                            break descent;
                        if ((v = ws[i = (h + k) & m]) != null) {
                            if (v.currentSteal == subtask) {
                                j.hint = i;
                                break;
                            }
                            checkSum += v.base;
                        }
                    }
                    for (;;) {                         // 帮助 v 或继续深入
                        ForkJoinTask<?>[] a; int b;
                        checkSum += (b = v.base);
                        ForkJoinTask<?> next = v.currentJoin;
                        if (subtask.status < 0 || j.currentJoin != subtask ||
                                v.currentSteal != subtask) // 过时
                            break descent;
                        if (b - v.top >= 0 || (a = v.array) == null) {
                            if ((subtask = next) == null)
                                break descent;
                            j = v;
                            break;
                        }
                        int i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                        ForkJoinTask<?> t = ((ForkJoinTask<?>)
                                U.getObjectVolatile(a, i));
                        if (v.base == b) {
                            if (t == null)             // 过时
                                break descent;
                            if (U.compareAndSwapObject(a, i, t, null)) {
                                v.base = b + 1;
                                ForkJoinTask<?> ps = w.currentSteal;
                                int top = w.top;
                                do {
                                    U.putOrderedObject(w, QCURRENTSTEAL, t);
                                    t.doExec();        // 也清理本地任务
                                } while (task.status >= 0 &&
                                        w.top != top &&
                                        (t = w.pop()) != null);
                                U.putOrderedObject(w, QCURRENTSTEAL, ps);
                                if (w.base != w.top)
                                    return;            // 无法进一步帮助
                            }
                        }
                    }
                }
            } while (task.status >= 0 && oldSum != (oldSum = checkSum));
        }
    }

    /**
     * 尝试减少活动计数（有时隐式地），并可能在阻塞前释放或创建一个补偿线程。
     * 如果由于竞争、检测到的过时、不稳定或终止而失败，返回 false（调用方可以重试）。
     *
     * @param w 调用者
     */
    private boolean tryCompensate(WorkQueue w) {
        boolean canBlock;
        WorkQueue[] ws; long c; int m, pc, sp;
        if (w == null || w.qlock < 0 ||           // 调用者正在终止
                (ws = workQueues) == null || (m = ws.length - 1) <= 0 ||
                (pc = config & SMASK) == 0)           // 并行度被禁用
            canBlock = false;
        else if ((sp = (int)(c = ctl)) != 0)      // 释放空闲线程
            canBlock = tryRelease(c, ws[sp & m], 0L);
        else {
            int ac = (int)(c >> AC_SHIFT) + pc;
            int tc = (short)(c >> TC_SHIFT) + pc;
            int nbusy = 0;                        // 验证饱和度
            for (int i = 0; i <= m; ++i) {        // 偶数索引两次循环
                WorkQueue v;
                if ((v = ws[((i << 1) | 1) & m]) != null) {
                    if ((v.scanState & SCANNING) != 0)
                        break;
                    ++nbusy;
                }
            }
            if (nbusy != (tc << 1) || ctl != c)
                canBlock = false;                 // 不稳定或过时
            else if (tc >= pc && ac > 1 && w.isEmpty()) {
                long nc = ((AC_MASK & (c - AC_UNIT)) |
                        (~AC_MASK & c));       // 未补偿
                canBlock = U.compareAndSwapLong(this, CTL, c, nc);
            }
            else if (tc >= MAX_CAP ||
                    (this == common && tc >= pc + commonMaxSpares))
                throw new RejectedExecutionException(
                        "替换阻塞工作线程的线程数量超过限制");
            else {                                // 类似于 tryAddWorker
                boolean add = false; int rs;      // 在锁内 CAS
                long nc = ((AC_MASK & c) |
                        (TC_MASK & (c + TC_UNIT)));
                if (((rs = lockRunState()) & STOP) == 0)
                    add = U.compareAndSwapLong(this, CTL, c, nc);
                unlockRunState(rs, rs & ~RSLOCK);
                canBlock = add && createWorker(); // 异常时抛出异常
            }
        }
        return canBlock;
    }

    /**
     * 帮助并/或阻塞直到给定任务完成或超时。
     *
     * @param w 调用者
     * @param task 任务
     * @param deadline 用于定时等待，如果非零
     * @return 任务的退出状态
     */
    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        int s = 0;
        if (task != null && w != null) {
            ForkJoinTask<?> prevJoin = w.currentJoin;
            U.putOrderedObject(w, QCURRENTJOIN, task);
            CountedCompleter<?> cc = (task instanceof CountedCompleter) ?
                    (CountedCompleter<?>)task : null;
            for (;;) {
                if ((s = task.status) < 0)
                    break;
                if (cc != null)
                    helpComplete(w, cc, 0);
                else if (w.base == w.top || w.tryRemoveAndExec(task))
                    helpStealer(w, task);
                if ((s = task.status) < 0)
                    break;
                long ms, ns;
                if (deadline == 0L)
                    ms = 0L;
                else if ((ns = deadline - System.nanoTime()) <= 0L)
                    break;
                else if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) <= 0L)
                    ms = 1L;
                if (tryCompensate(w)) {
                    task.internalWait(ms);
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
            }
            U.putOrderedObject(w, QCURRENTJOIN, prevJoin);
        }
        return s;
    }

    // 专门扫描

    /**
     * 返回一个（可能）非空的窃取队列，如果在扫描过程中找到，否则返回 null。
     * 调用者必须在尝试使用队列时重试，因为到时它可能已经为空。
     */
    private WorkQueue findNonEmptyStealQueue() {
        WorkQueue[] ws; int m;  // scan 循环的一次性版本
        int r = ThreadLocalRandom.nextSecondarySeed();
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0) {
            for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0;;) {
                WorkQueue q; int b;
                if ((q = ws[k]) != null) {
                    if ((b = q.base) - q.top < 0)
                        return q;
                    checkSum += b;
                }
                if ((k = (k + 1) & m) == origin) {
                    if (oldSum == (oldSum = checkSum))
                        break;
                    checkSum = 0;
                }
            }
        }
        return null;
    }

    /**
     * 运行任务直到 {@code isQuiescent()}。我们依靠活动计数 ctl 的维护，
     * 但与其在找不到任务时阻塞，我们会重新扫描，直到其他线程也无法找到任务为止。
     */
    final void helpQuiescePool(WorkQueue w) {
        ForkJoinTask<?> ps = w.currentSteal; // 保存上下文
        for (boolean active = true;;) {
            long c; WorkQueue q; ForkJoinTask<?> t; int b;
            w.execLocalTasks();     // 在每次扫描之前运行本地任务
            if ((q = findNonEmptyStealQueue()) != null) {
                if (!active) {      // 重新建立活动计数
                    active = true;
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
                if ((b = q.base) - q.top < 0 && (t = q.pollAt(b)) != null) {
                    U.putOrderedObject(w, QCURRENTSTEAL, t);
                    t.doExec();
                    if (++w.nsteals < 0)
                        w.transferStealCount(this);
                }
            }
            else if (active) {      // 在不排队的情况下减少活动计数
                long nc = (AC_MASK & ((c = ctl) - AC_UNIT)) | (~AC_MASK & c);
                if ((int)(nc >> AC_SHIFT) + (config & SMASK) <= 0)
                    break;          // 绕过减量后再增量
                if (U.compareAndSwapLong(this, CTL, c, nc))
                    active = false;
            }
            else if ((int)((c = ctl) >> AC_SHIFT) + (config & SMASK) <= 0 &&
                    U.compareAndSwapLong(this, CTL, c, c + AC_UNIT))
                break;
        }
        U.putOrderedObject(w, QCURRENTSTEAL, ps);
    }

    /**
     * 为给定的工作队列获取并移除一个本地或被窃取的任务。
     *
     * @return 可用的任务
     */
    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        for (ForkJoinTask<?> t;;) {
            WorkQueue q; int b;
            if ((t = w.nextLocalTask()) != null)
                return t;
            if ((q = findNonEmptyStealQueue()) == null)
                return null;
            if ((b = q.base) - q.top < 0 && (t = q.pollAt(b)) != null)
                return t;
        }
    }

    /**
     * 返回任务划分的一个廉价启发式指导，用于当程序员、框架、工具或语言对任务粒度几乎没有或没有了解时。
     * 实质上，通过提供此方法，我们只要求用户在开销与预期吞吐量及其方差之间进行权衡，而不是如何细分任务。
     *
     * 在一个稳定状态的严格（树状结构的）计算中，每个线程为其他线程保持足够的任务可供窃取以保持活动状态。
     * 归纳地说，如果所有线程都遵循相同的规则，每个线程应该只生成一个常量数量的任务。
     *
     * 最小的有用常量只是 1。但使用值 1 会要求每次窃取后立即补充任务，以保持足够的任务量，这是不可行的。
     * 此外，提供的任务的划分/粒度应尽量减少窃取率，这通常意味着位于计算树顶部的线程应生成比位于底部的线程更多的任务。
     * 在完美的稳定状态下，每个线程大约处于相同的计算树级别。然而，生成额外任务可以摊薄进度的不确定性和扩散假设。
     *
     * 因此，用户将希望使用比 1 大（但不大得多）的值，以同时平滑暂时性短缺并对不均匀的进度进行对冲；
     * 同时需要权衡额外任务开销的成本。我们让用户选择一个阈值与此方法的结果进行比较以指导决策，
     * 但建议使用诸如 3 这样的值。
     *
     * 当所有线程都处于活动状态时，通常可以通过本地估计来严格估算盈余。在稳定状态下，如果一个线程保持例如 2 个盈余任务，
     * 那么其他线程也是如此。因此我们可以仅使用估计的队列长度。
     * 然而，这种策略单独在某些非稳定状态条件下会导致严重的错误估算（如爬升、降速、其他停顿）。
     * 我们可以通过进一步考虑“空闲”线程的数量来检测这些条件中的许多，因为我们知道它们没有排队任务，
     * 因此通过（#空闲线程/#活动线程）的比例进行补偿。
     */
    static int getSurplusQueuedTaskCount() {
        Thread t; ForkJoinWorkerThread wt; ForkJoinPool pool; WorkQueue q;
        if (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)) {
            int p = (pool = (wt = (ForkJoinWorkerThread)t).pool).
                    config & SMASK;
            int n = (q = wt.workQueue).top - q.base;
            int a = (int)(pool.ctl >> AC_SHIFT) + p;
            return n - (a > (p >>>= 1) ? 0 :
                    a > (p >>>= 1) ? 1 :
                            a > (p >>>= 1) ? 2 :
                                    a > (p >>>= 1) ? 4 :
                                            8);
        }
        return 0;
    }

    // 终止

    /**
     * 可能启动和/或完成终止。
     *
     * @param now 如果为 true，则无条件终止，否则仅当没有任务且没有活动的工作线程时终止
     * @param enable 如果为 true，则在下次可能时启用关闭
     * @return 如果现在正在终止或已终止，则返回 true
     */
    private boolean tryTerminate(boolean now, boolean enable) {
        int rs;
        if (this == common)                       // 无法关闭
            return false;
        if ((rs = runState) >= 0) {
            if (!enable)
                return false;
            rs = lockRunState();                  // 进入 SHUTDOWN 阶段
            unlockRunState(rs, (rs & ~RSLOCK) | SHUTDOWN);
        }

        if ((rs & STOP) == 0) {
            if (!now) {                           // 检查是否处于空闲状态
                for (long oldSum = 0L;;) {        // 重复直到稳定
                    WorkQueue[] ws; WorkQueue w; int m, b; long c;
                    long checkSum = ctl;
                    if ((int)(checkSum >> AC_SHIFT) + (config & SMASK) > 0)
                        return false;             // 仍有活动的工作线程
                    if ((ws = workQueues) == null || (m = ws.length - 1) <= 0)
                        break;                    // 检查队列
                    for (int i = 0; i <= m; ++i) {
                        if ((w = ws[i]) != null) {
                            if ((b = w.base) != w.top || w.scanState >= 0 ||
                                    w.currentSteal != null) {
                                tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);
                                return false;     // 安排重新检查
                            }
                            checkSum += b;
                            if ((i & 1) == 0)
                                w.qlock = -1;     // 尝试禁用外部
                        }
                    }
                    if (oldSum == (oldSum = checkSum))
                        break;
                }
            }
            if ((runState & STOP) == 0) {
                rs = lockRunState();              // 进入 STOP 阶段
                unlockRunState(rs, (rs & ~RSLOCK) | STOP);
            }
        }

        int pass = 0;                             // 3 次遍历以帮助终止
        for (long oldSum = 0L;;) {                // 或直到完成或稳定
            WorkQueue[] ws; WorkQueue w; ForkJoinWorkerThread wt; int m;
            long checkSum = ctl;
            if ((short)(checkSum >>> TC_SHIFT) + (config & SMASK) <= 0 ||
                    (ws = workQueues) == null || (m = ws.length - 1) <= 0) {
                if ((runState & TERMINATED) == 0) {
                    rs = lockRunState();          // 完成
                    unlockRunState(rs, (rs & ~RSLOCK) | TERMINATED);
                    synchronized (this) { notifyAll(); } // 用于 awaitTermination
                }
                break;
            }
            for (int i = 0; i <= m; ++i) {
                if ((w = ws[i]) != null) {
                    checkSum += w.base;
                    w.qlock = -1;                 // 尝试禁用
                    if (pass > 0) {
                        w.cancelAll();            // 清空队列
                        if (pass > 1 && (wt = w.owner) != null) {
                            if (!wt.isInterrupted()) {
                                try {             // 解锁 join
                                    wt.interrupt();
                                } catch (Throwable ignore) {
                                }
                            }
                            if (w.scanState < 0)
                                U.unpark(wt);     // 唤醒
                        }
                    }
                }
            }
            if (checkSum != oldSum) {             // 不稳定
                oldSum = checkSum;
                pass = 0;
            }
            else if (pass > 3 && pass > m)        // 无法进一步帮助
                break;
            else if (++pass > 1) {                // 尝试取消排队
                long c; int j = 0, sp;            // 限制尝试次数
                while (j++ <= m && (sp = (int)(c = ctl)) != 0)
                    tryRelease(c, ws[sp & m], AC_UNIT);
            }
        }
        return true;
    }

    // 外部操作

    /**
     * externalPush 的完整版本，处理不常见的情况，并在首次任务提交到池时执行二次初始化。它还检测由外部线程的首次提交，并在索引处的队列为空或被竞争时创建一个新的共享队列。
     *
     * @param task 任务。调用者必须确保非空。
     */
    private void externalSubmit(ForkJoinTask<?> task) {
        int r;                                    // 初始化调用者的探测
        if ((r = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();
            r = ThreadLocalRandom.getProbe();
        }
        for (;;) {
            WorkQueue[] ws; WorkQueue q; int rs, m, k;
            boolean move = false;
            if ((rs = runState) < 0) {
                tryTerminate(false, false);     // 帮助终止
                throw new RejectedExecutionException();
            }
            else if ((rs & STARTED) == 0 ||     // 初始化
                    ((ws = workQueues) == null || (m = ws.length - 1) < 0)) {
                int ns = 0;
                rs = lockRunState();
                try {
                    if ((rs & STARTED) == 0) {
                        U.compareAndSwapObject(this, STEALCOUNTER, null,
                                new AtomicLong());
                        // 创建大小为 2 的幂的 workQueues 数组
                        int p = config & SMASK; // 确保至少有 2 个插槽
                        int n = (p > 1) ? p - 1 : 1;
                        n |= n >>> 1; n |= n >>> 2;  n |= n >>> 4;
                        n |= n >>> 8; n |= n >>> 16; n = (n + 1) << 1;
                        workQueues = new WorkQueue[n];
                        ns = STARTED;
                    }
                } finally {
                    unlockRunState(rs, (rs & ~RSLOCK) | ns);
                }
            }
            else if ((q = ws[k = r & m & SQMASK]) != null) {
                if (q.qlock == 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                    ForkJoinTask<?>[] a = q.array;
                    int s = q.top;
                    boolean submitted = false; // 初次提交或调整大小
                    try {                      // push 的加锁版本
                        if ((a != null && a.length > s + 1 - q.base) ||
                                (a = q.growArray()) != null) {
                            int j = (((a.length - 1) & s) << ASHIFT) + ABASE;
                            U.putOrderedObject(a, j, task);
                            U.putOrderedInt(q, QTOP, s + 1);
                            submitted = true;
                        }
                    } finally {
                        U.compareAndSwapInt(q, QLOCK, 1, 0);
                    }
                    if (submitted) {
                        signalWork(ws, q);
                        return;
                    }
                }
                move = true;                   // 失败时移动
            }
            else if (((rs = runState) & RSLOCK) == 0) { // 创建新队列
                q = new WorkQueue(this, null);
                q.hint = r;
                q.config = k | SHARED_QUEUE;
                q.scanState = INACTIVE;
                rs = lockRunState();           // 发布索引
                if (rs > 0 && (ws = workQueues) != null &&
                        k < ws.length && ws[k] == null)
                    ws[k] = q;                 // 否则已终止
                unlockRunState(rs, rs & ~RSLOCK);
            }
            else
                move = true;                   // 如果繁忙则移动
            if (move)
                r = ThreadLocalRandom.advanceProbe(r);
        }
    }

    /**
     * 尝试将给定任务添加到提交者当前队列的提交队列中。此方法仅处理 (极大地) 最常见的路径，同时筛选是否需要 externalSubmit。
     *
     * @param task 任务。调用者必须确保非空。
     */
    final void externalPush(ForkJoinTask<?> task) {
        WorkQueue[] ws; WorkQueue q; int m;
        int r = ThreadLocalRandom.getProbe();
        int rs = runState;
        if ((ws = workQueues) != null && (m = (ws.length - 1)) >= 0 &&
                (q = ws[m & r & SQMASK]) != null && r != 0 && rs > 0 &&
                U.compareAndSwapInt(q, QLOCK, 0, 1)) {
            ForkJoinTask<?>[] a; int am, n, s;
            if ((a = q.array) != null &&
                    (am = a.length - 1) > (n = (s = q.top) - q.base)) {
                int j = ((am & s) << ASHIFT) + ABASE;
                U.putOrderedObject(a, j, task);
                U.putOrderedInt(q, QTOP, s + 1);
                U.putIntVolatile(q, QLOCK, 0);
                if (n <= 1)
                    signalWork(ws, q);
                return;
            }
            U.compareAndSwapInt(q, QLOCK, 1, 0);
        }
        externalSubmit(task);
    }

    /**
     * 返回外部线程的公共池队列。
     */
    static WorkQueue commonSubmitterQueue() {
        ForkJoinPool p = common;
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws; int m;
        return (p != null && (ws = p.workQueues) != null &&
                (m = ws.length - 1) >= 0) ?
                ws[m & r & SQMASK] : null;
    }

    /**
     * 为外部提交者执行 tryUnpush：查找队列，如果队列非空则加锁，在加锁时验证，并调整 top。每个检查都可能失败，但很少会失败。
     */
    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        WorkQueue[] ws; WorkQueue w; ForkJoinTask<?>[] a; int m, s;
        int r = ThreadLocalRandom.getProbe();
        if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 &&
                (w = ws[m & r & SQMASK]) != null &&
                (a = w.array) != null && (s = w.top) != w.base) {
            long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
            if (U.compareAndSwapInt(w, QLOCK, 0, 1)) {
                if (w.top == s && w.array == a &&
                        U.getObject(a, j) == task &&
                        U.compareAndSwapObject(a, j, task, null)) {
                    U.putOrderedInt(w, QTOP, s - 1);
                    U.putOrderedInt(w, QLOCK, 0);
                    return true;
                }
                U.compareAndSwapInt(w, QLOCK, 1, 0);
            }
        }
        return false;
    }

    /**
     * 为外部提交者执行 helpComplete。
     */
    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        WorkQueue[] ws; int n;
        int r = ThreadLocalRandom.getProbe();
        return ((ws = workQueues) == null || (n = ws.length) == 0) ? 0 :
                helpComplete(ws[(n - 1) & r & SQMASK], task, maxTasks);
    }

    // 导出的方法

    // 构造函数

    /**
     * 创建一个 {@code ForkJoinPool}，其并行度等于 {@link java.lang.Runtime#availableProcessors}，使用 {@linkplain #defaultForkJoinWorkerThreadFactory 默认线程工厂}，没有 UncaughtExceptionHandler，并使用非异步的 LIFO 处理模式。
     *
     * @throws SecurityException 如果存在安全管理器且调用者没有修改线程的权限
     * 因为它没有持有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()),
                defaultForkJoinWorkerThreadFactory, null, false);
    }

    /**
     * 创建一个具有指定并行度级别的 {@code ForkJoinPool}，使用 {@linkplain #defaultForkJoinWorkerThreadFactory 默认线程工厂}，没有 UncaughtExceptionHandler，并使用非异步的 LIFO 处理模式。
     *
     * @param parallelism 并行度级别
     * @throws IllegalArgumentException 如果并行度小于或等于零，或超过实现的限制
     * @throws SecurityException 如果存在安全管理器且调用者没有修改线程的权限
     * 因为它没有持有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    /**
     * 使用给定的参数创建一个 {@code ForkJoinPool}。
     *
     * @param parallelism 并行度级别。对于默认值，使用 {@link java.lang.Runtime#availableProcessors}。
     * @param factory 用于创建新线程的工厂。对于默认值，使用 {@link #defaultForkJoinWorkerThreadFactory}。
     * @param handler 用于内部工作线程在遇到无法恢复的错误时终止时的处理程序。对于默认值，使用 {@code null}。
     * @param asyncMode 如果为 true，则建立局部先进先出 (FIFO) 调度模式用于从不连接的 forked 任务。在仅处理事件样式异步任务的应用程序中，此模式可能比默认的局部栈模式更合适。对于默认值，使用 {@code false}。
     * @throws IllegalArgumentException 如果并行度小于或等于零，或超过实现限制
     * @throws NullPointerException 如果工厂为 null
     * @throws SecurityException 如果存在安全管理器且调用者没有修改线程的权限
     * 因为它没有持有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}
     */
    public ForkJoinPool(int parallelism,
                        ForkJoinWorkerThreadFactory factory,
                        UncaughtExceptionHandler handler,
                        boolean asyncMode) {
        this(checkParallelism(parallelism),
                checkFactory(factory),
                handler,
                asyncMode ? FIFO_QUEUE : LIFO_QUEUE,
                "ForkJoinPool-" + nextPoolId() + "-worker-");
        checkPermission();
    }

    private static int checkParallelism(int parallelism) {
        if (parallelism <= 0 || parallelism > MAX_CAP)
            throw new IllegalArgumentException();
        return parallelism;
    }

    private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory factory) {
        if (factory == null)
            throw new NullPointerException();
        return factory;
    }

    /**
     * 使用给定的参数创建一个 {@code ForkJoinPool}，不进行任何安全检查或参数验证。直接由 makeCommonPool 调用。
     */
    private ForkJoinPool(int parallelism,
                         ForkJoinWorkerThreadFactory factory,
                         UncaughtExceptionHandler handler,
                         int mode,
                         String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
        this.factory = factory;
        this.ueh = handler;
        this.config = (parallelism & SMASK) | mode;
        long np = (long)(-parallelism); // 偏移 ctl 计数
        this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << TC_SHIFT) & TC_MASK);
    }

    /**
     * 返回公共池实例。此池是静态构造的；其运行状态不受 {@link #shutdown} 或 {@link #shutdownNow} 的尝试影响。然而，此池和任何正在进行的处理将在程序 {@link System#exit} 时自动终止。依赖于异步任务处理完成的程序应在退出之前调用 {@code commonPool().}{@link #awaitQuiescence awaitQuiescence}。
     *
     * @return 公共池实例
     * @since 1.8
     */
    public static ForkJoinPool commonPool() {
        // 确保 common 不为空："静态初始化错误"
        return common;
    }

    // 执行方法

    /**
     * 执行给定的任务，完成后返回其结果。如果计算遇到未经检查的异常或错误，它将作为此调用的结果重新抛出。重新抛出的异常的行为与常规异常相同，但在可能的情况下，包含当前线程以及实际遇到异常的线程的堆栈跟踪（例如，使用 {@code ex.printStackTrace()} 显示）。至少包含后者。
     *
     * @param task 任务
     * @param <T> 任务结果的类型
     * @return 任务的结果
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null)
            throw new NullPointerException();
        externalPush(task);
        return task.join();
    }

    /**
     * 安排给定任务的（异步）执行。
     *
     * @param task 任务
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public void execute(ForkJoinTask<?> task) {
        if (task == null)
            throw new NullPointerException();
        externalPush(task);
    }

    // AbstractExecutorService 方法

    /**
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public void execute(Runnable task) {
        if (task == null)
            throw new NullPointerException();
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask<?>) // 避免重新包装
            job = (ForkJoinTask<?>) task;
        else
            job = new ForkJoinTask.RunnableExecuteAction(task);
        externalPush(job);
    }

    /**
     * 提交一个 ForkJoinTask 以执行。
     *
     * @param task 要提交的任务
     * @param <T> 任务结果的类型
     * @return 任务
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        if (task == null)
            throw new NullPointerException();
        externalPush(task);
        return task;
    }

    /**
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable<T>(task);
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedRunnable<T>(task, result);
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException 如果任务为 null
     * @throws RejectedExecutionException 如果任务无法安排执行
     */
    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null)
            throw new NullPointerException();
        ForkJoinTask<?> job;
        if (task instanceof ForkJoinTask<?>) // 避免重新包装
            job = (ForkJoinTask<?>) task;
        else
            job = new ForkJoinTask.AdaptedRunnableAction(task);
        externalPush(job);
        return job;
    }

    /**
     * @throws NullPointerException       {@inheritDoc}
     * @throws RejectedExecutionException {@inheritDoc}
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        // 在此类的先前版本中，此方法构造了一个任务以运行 ForkJoinTask.invokeAll，但现在外部调用多个任务的效率至少相同。
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());

        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = new ForkJoinTask.AdaptedCallable<T>(t);
                futures.add(f);
                externalPush(f);
            }
            for (int i = 0, size = futures.size(); i < size; i++)
                ((ForkJoinTask<?>)futures.get(i)).quietlyJoin();
            done = true;
            return futures;
        } finally {
            if (!done)
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(false);
        }
    }

    /**
     * 返回用于构造新工作线程的工厂。
     *
     * @return 用于构造新工作线程的工厂
     */
    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }

    /**
     * 返回由于在执行任务时遇到不可恢复的错误而终止的内部工作线程的处理程序。
     *
     * @return 处理程序，或 {@code null} 如果没有
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ueh;
    }

    /**
     * 返回此池的目标并行度级别。
     *
     * @return 此池的目标并行度级别
     */
    public int getParallelism() {
        int par;
        return ((par = config & SMASK) > 0) ? par : 1;
    }

    /**
     * 返回公共池的目标并行度级别。
     *
     * @return 公共池的目标并行度级别
     * @since 1.8
     */
    public static int getCommonPoolParallelism() {
        return commonParallelism;
    }

    /**
     * 返回已经启动但尚未终止的工作线程的数量。此方法返回的结果可能与 {@link #getParallelism} 不同，当线程在其他线程阻塞时被创建以维持并行度。
     *
     * @return 工作线程的数量
     */
    public int getPoolSize() {
        return (config & SMASK) + (short)(ctl >>> TC_SHIFT);
    }

    /**
     * 如果此池使用本地先进先出的调度模式来处理从未连接的 forked 任务，则返回 {@code true}。
     *
     * @return 如果此池使用异步模式，则为 {@code true}
     */
    public boolean getAsyncMode() {
        return (config & FIFO_QUEUE) != 0;
    }

    /**
     * 返回一个估计的、当前未阻塞等待连接任务或其他托管同步的工作线程数。此方法可能会高估运行中的线程数。
     *
     * @return 工作线程的数量
     */
    public int getRunningThreadCount() {
        int rc = 0;
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && w.isApparentlyUnblocked())
                    ++rc;
            }
        }
        return rc;
    }

    /**
     * 返回一个估计的、当前正在窃取或执行任务的线程数量。此方法可能会高估活跃线程的数量。
     *
     * @return 活跃线程的数量
     */
    public int getActiveThreadCount() {
        int r = (config & SMASK) + (int)(ctl >> AC_SHIFT);
        return (r <= 0) ? 0 : r; // 抑制暂时的负值
    }

    /**
     * 如果所有工作线程当前都处于空闲状态，则返回 {@code true}。空闲工作线程是指由于没有可供其他线程窃取的任务，无法获得任务执行的线程，并且没有提交到池的待处理任务。此方法是保守的；它可能不会在所有线程刚刚变为空闲时立即返回 {@code true}，但如果线程保持不活动，则最终会变为 true。
     *
     * @return 如果所有线程当前都空闲，则返回 {@code true}
     */
    public boolean isQuiescent() {
        return (config & SMASK) + (int)(ctl >> AC_SHIFT) <= 0;
    }

    /**
     * 返回一个估计的、从一个线程的工作队列中被另一个线程窃取的任务总数。当池不处于空闲状态时，报告的值会低估实际的总窃取数。此值可能对监控和调整 fork/join 程序有用：通常，窃取计数应该足够高以使线程保持忙碌，但又低到足以避免线程之间的开销和竞争。
     *
     * @return 窃取的任务数量
     */
    public long getStealCount() {
        AtomicLong sc = stealCounter;
        long count = (sc == null) ? 0L : sc.get();
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null)
                    count += w.nsteals;
            }
        }
        return count;
    }

    /**
     * 返回一个估计的、工作线程队列中当前持有的任务总数（不包括提交到池中但尚未开始执行的任务）。此值只是一个近似值，通过遍历池中的所有线程来获得。此方法可能对调整任务粒度有用。
     *
     * @return 排队任务的数量
     */
    public long getQueuedTaskCount() {
        long count = 0;
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 1; i < ws.length; i += 2) {
                if ((w = ws[i]) != null)
                    count += w.queueSize();
            }
        }
        return count;
    }

    /**
     * 返回一个估计的、提交到此池但尚未开始执行的任务数量。此方法可能需要根据提交的任务数量花费相应的时间。
     *
     * @return 排队提交的任务数量
     */
    public int getQueuedSubmissionCount() {
        int count = 0;
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null)
                    count += w.queueSize();
            }
        }
        return count;
    }

    /**
     * 如果有任何提交到此池的任务尚未开始执行，则返回 {@code true}。
     *
     * @return 如果有任何排队提交的任务，则返回 {@code true}
     */
    public boolean hasQueuedSubmissions() {
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && !w.isEmpty())
                    return true;
            }
        }
        return false;
    }

    /**
     * 如果有可用的未执行提交，则删除并返回下一个提交的任务。此方法在扩展此类以在多个池中重新分配工作时可能有用。
     *
     * @return 下一个提交的任务，如果没有则返回 {@code null}
     */
    protected ForkJoinTask<?> pollSubmission() {
        WorkQueue[] ws; WorkQueue w; ForkJoinTask<?> t;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; i += 2) {
                if ((w = ws[i]) != null && (t = w.poll()) != null)
                    return t;
            }
        }
        return null;
    }

    /**
     * 删除所有可用的未执行的提交和 forked 任务，并将它们添加到给定的集合中，而不更改它们的执行状态。这些可能包括人为生成或包装的任务。此方法设计为仅在池处于空闲状态时调用。在其他时间调用可能不会删除所有任务。尝试将元素添加到集合 {@code c} 时遇到的故障可能导致在抛出相关异常时元素既不在集合中，也可能在其中，或者两者都有。此操作的行为在指定的集合在操作过程中被修改时未定义。
     *
     * @param c 将元素转移到的集合
     * @return 转移的元素数量
     */
    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int count = 0;
        WorkQueue[] ws; WorkQueue w; ForkJoinTask<?> t;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; ++i) {
                if ((w = ws[i]) != null) {
                    while ((t = w.poll()) != null) {
                        c.add(t);
                        ++count;
                    }
                }
            }
        }
        return count;
    }

    /**
     * 返回标识此池的字符串以及其状态，包括运行状态、并行度级别、工作线程数和任务数的指示。
     *
     * @return 标识此池及其状态的字符串
     */
    public String toString() {
        // 使用单次遍历 workQueues 来收集计数
        long qt = 0L, qs = 0L; int rc = 0;
        AtomicLong sc = stealCounter;
        long st = (sc == null) ? 0L : sc.get();
        long c = ctl;
        WorkQueue[] ws; WorkQueue w;
        if ((ws = workQueues) != null) {
            for (int i = 0; i < ws.length; ++i) {
                if ((w = ws[i]) != null) {
                    int size = w.queueSize();
                    if ((i & 1) == 0)
                        qs += size;
                    else {
                        qt += size;
                        st += w.nsteals;
                        if (w.isApparentlyUnblocked())
                            ++rc;
                    }
                }
            }
        }
        int pc = (config & SMASK);
        int tc = pc + (short)(c >>> TC_SHIFT);
        int ac = pc + (int)(c >> AC_SHIFT);
        if (ac < 0) // 忽略瞬间负值
            ac = 0;
        int rs = runState;
        String level = ((rs & TERMINATED) != 0 ? "已终止" :
                (rs & STOP)       != 0 ? "正在终止" :
                        (rs & SHUTDOWN)   != 0 ? "正在关闭" :
                                "运行中");
        return super.toString() +
                "[" + level +
                ", 并行度 = " + pc +
                ", 大小 = " + tc +
                ", 活跃 = " + ac +
                ", 运行中 = " + rc +
                ", 窃取 = " + st +
                ", 任务 = " + qt +
                ", 提交 = " + qs +
                "]";
    }

    /**
     * 可能会启动一个有序的关闭过程，在此过程中，先前提交的任务会执行，但不会接受新任务。
     * 如果这是 {@link #commonPool()}，则调用对执行状态没有影响；如果已经关闭，也不会有任何附加效果。
     * 在该方法执行期间，同时正在提交的任务可能会被拒绝，也可能不会被拒绝。
     *
     * @throws SecurityException 如果存在安全管理器，并且
     *         调用者没有权限修改线程，因为它没有 {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")} 的权限
     */
    public void shutdown() {
        checkPermission();
        tryTerminate(false, true);
    }

    /**
     * 可能尝试取消和/或停止所有任务，并拒绝随后提交的所有任务。
     * 如果这是 {@link #commonPool()}，则调用对执行状态没有影响；如果已经关闭，也不会有任何附加效果。
     * 否则，在该方法执行期间，同时正在提交或执行的任务可能会被拒绝，也可能不会被拒绝。
     * 该方法会取消现有和未执行的任务，以允许在存在任务依赖的情况下进行终止。
     * 因此，该方法总是返回一个空列表（与某些其他执行器的情况不同）。
     *
     * @return 一个空列表
     * @throws SecurityException 如果存在安全管理器，并且
     *         调用者没有权限修改线程，因为它没有 {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")} 的权限
     */
    public List<Runnable> shutdownNow() {
        checkPermission();
        tryTerminate(true, true);
        return Collections.emptyList();
    }

    /**
     * 返回 {@code true}，如果所有任务在关闭后已完成。
     *
     * @return 如果所有任务在关闭后已完成，则返回 {@code true}
     */
    public boolean isTerminated() {
        return (runState & TERMINATED) != 0;
    }

    /**
     * 返回 {@code true}，如果终止过程已经开始但尚未完成。
     * 该方法可能对调试有用。在关闭后经过足够的时间后返回 {@code true}，
     * 可能表示已提交的任务忽略或抑制了中断，或正在等待 I/O，导致该执行器无法正确终止。
     * （请参阅类 {@link ForkJoinTask} 的建议说明，指出任务通常不应涉及阻塞操作。
     * 但如果确实如此，它们必须在中断时终止它们。）
     *
     * @return 如果正在终止但尚未终止，则返回 {@code true}
     */
    public boolean isTerminating() {
        int rs = runState;
        return (rs & STOP) != 0 && (rs & TERMINATED) == 0;
    }

    /**
     * 返回 {@code true}，如果该池已关闭。
     *
     * @return 如果该池已关闭，则返回 {@code true}
     */
    public boolean isShutdown() {
        return (runState & SHUTDOWN) != 0;
    }

    /**
     * 阻塞直到所有任务在关闭请求后完成执行，或超时发生，或当前线程被中断，以最先发生者为准。
     * 因为 {@link #commonPool()} 在程序关闭之前永远不会终止，因此当应用于公共池时，
     * 此方法等效于 {@link #awaitQuiescence(long, TimeUnit)}，但总是返回 {@code false}。
     *
     * @param timeout 等待的最长时间
     * @param unit 超时参数的时间单位
     * @return 如果此执行器终止，则返回 {@code true}，如果在终止之前超时，则返回 {@code false}
     * @throws InterruptedException 如果在等待时被中断
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (this == common) {
            awaitQuiescence(timeout, unit);
            return false;
        }
        long nanos = unit.toNanos(timeout);
        if (isTerminated())
            return true;
        if (nanos <= 0L)
            return false;
        long deadline = System.nanoTime() + nanos;
        synchronized (this) {
            for (;;) {
                if (isTerminated())
                    return true;
                if (nanos <= 0L)
                    return false;
                long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                wait(millis > 0L ? millis : 1L);
                nanos = deadline - System.nanoTime();
            }
        }
    }

    /**
     * 如果由在此池中运行的 ForkJoinTask 调用，效果等同于 {@link ForkJoinTask#helpQuiesce}。
     * 否则，等待并/或尝试协助执行任务，直到此池 {@link #isQuiescent} 或指定的超时时间过去。
     *
     * @param timeout 等待的最长时间
     * @param unit 超时参数的时间单位
     * @return 如果池已处于静止状态则返回 {@code true}；如果超时则返回 {@code false}。
     */
    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        long nanos = unit.toNanos(timeout);
        ForkJoinWorkerThread wt;
        Thread thread = Thread.currentThread();
        if ((thread instanceof ForkJoinWorkerThread) &&
                (wt = (ForkJoinWorkerThread)thread).pool == this) {
            helpQuiescePool(wt.workQueue);
            return true;
        }
        long startTime = System.nanoTime();
        WorkQueue[] ws;
        int r = 0, m;
        boolean found = true;
        while (!isQuiescent() && (ws = workQueues) != null &&
                (m = ws.length - 1) >= 0) {
            if (!found) {
                if ((System.nanoTime() - startTime) > nanos)
                    return false;
                Thread.yield(); // 无法阻塞
            }
            found = false;
            for (int j = (m + 1) << 2; j >= 0; --j) {
                ForkJoinTask<?> t; WorkQueue q; int b, k;
                if ((k = r++ & m) <= m && k >= 0 && (q = ws[k]) != null &&
                        (b = q.base) - q.top < 0) {
                    found = true;
                    if ((t = q.pollAt(b)) != null)
                        t.doExec();
                    break;
                }
            }
        }
        return true;
    }

    /**
     * 等待并/或尝试协助执行任务，直到 {@link #commonPool()} {@link #isQuiescent}。
     */
    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    /**
     * 接口用于扩展在 {@link ForkJoinPool} 中运行的任务的并行性管理。
     *
     * <p>{@code ManagedBlocker} 提供两个方法。方法 {@link #isReleasable} 必须返回 {@code true}，如果不需要阻塞。
     * 方法 {@link #block} 在必要时阻塞当前线程（可能在实际阻塞前内部调用 {@code isReleasable}）。
     * 这些操作由任何调用 {@link ForkJoinPool#managedBlock(ManagedBlocker)} 的线程执行。
     * 此 API 中的非同寻常方法适用于可能但通常不会长时间阻塞的同步器。
     * 同样，它们允许在内部更有效地处理在某些情况下可能需要额外工作者以确保足够的并行性，
     * 但通常不需要。为了实现这一点，方法 {@code isReleasable} 的实现必须适合重复调用。
     *
     * <p>例如，这里是一个基于 ReentrantLock 的 ManagedBlocker：
     *  <pre> {@code
     * class ManagedLocker implements ManagedBlocker {
     *   final ReentrantLock lock;
     *   boolean hasLock = false;
     *   ManagedLocker(ReentrantLock lock) { this.lock = lock; }
     *   public boolean block() {
     *     if (!hasLock)
     *       lock.lock();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return hasLock || (hasLock = lock.tryLock());
     *   }
     * }}</pre>
     *
     * <p>这里是一个可能阻塞等待队列中项的类：
     *  <pre> {@code
     * class QueueTaker<E> implements ManagedBlocker {
     *   final BlockingQueue<E> queue;
     *   volatile E item = null;
     *   QueueTaker(BlockingQueue<E> q) { this.queue = q; }
     *   public boolean block() throws InterruptedException {
     *     if (item == null)
     *       item = queue.take();
     *     return true;
     *   }
     *   public boolean isReleasable() {
     *     return item != null || (item = queue.poll()) != null;
     *   }
     *   public E getItem() { // 在 pool.managedBlock 完成后调用
     *     return item;
     *   }
     * }}</pre>
     */
    public static interface ManagedBlocker {
        /**
         * 可能阻塞当前线程，例如等待锁或条件。
         *
         * @return {@code true} 如果不再需要阻塞（即，如果 isReleasable 返回 true）
         * @throws InterruptedException 如果在等待时被中断（方法不要求这样做，但允许）
         */
        boolean block() throws InterruptedException;

        /**
         * 返回 {@code true}，如果阻塞是不必要的。
         * @return {@code true} 如果阻塞是不必要的
         */
        boolean isReleasable();
    }

    /**
     * 运行给定的可能阻塞的任务。当在 {@linkplain ForkJoinTask#inForkJoinPool() ForkJoinPool 中运行}时，
     * 此方法可能会安排激活备用线程（如果需要），以确保在当前线程被 {@link ManagedBlocker#block blocker.block()} 阻塞期间有足够的并行性。
     *
     * <p>此方法重复调用 {@code blocker.isReleasable()} 和 {@code blocker.block()}，直到其中一个方法返回 {@code true}。
     * 每次调用 {@code blocker.block()} 之前都会调用 {@code blocker.isReleasable()}，其返回 {@code false}。
     *
     * <p>如果未在 ForkJoinPool 中运行，则此方法的行为等同于：
     *  <pre> {@code
     * while (!blocker.isReleasable())
     *   if (blocker.block())
     *     break;}</pre>
     *
     * 如果在 ForkJoinPool 中运行，池可能会首先扩展，以确保在调用 {@code blocker.block()} 期间有足够的并行性。
     *
     * @param blocker 阻塞器任务
     * @throws InterruptedException 如果 {@code blocker.block()} 这样做
     */
    public static void managedBlock(ManagedBlocker blocker)
            throws InterruptedException {
        ForkJoinPool p;
        ForkJoinWorkerThread wt;
        Thread t = Thread.currentThread();
        if ((t instanceof ForkJoinWorkerThread) &&
                (p = (wt = (ForkJoinWorkerThread)t).pool) != null) {
            WorkQueue w = wt.workQueue;
            while (!blocker.isReleasable()) {
                if (p.tryCompensate(w)) {
                    try {
                        do {} while (!blocker.isReleasable() &&
                                !blocker.block());
                    } finally {
                        U.getAndAddLong(p, CTL, AC_UNIT);
                    }
                    break;
                }
            }
        }
        else {
            do {} while (!blocker.isReleasable() &&
                    !blocker.block());
        }
    }
    // AbstractExecutorService 重写。这些依赖于 ForkJoinTask.adapt 返回实现 RunnableFuture 的 ForkJoinTask。

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ForkJoinTask.AdaptedRunnable<T>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable<T>(callable);
    }

    // Unsafe机制
    private static final sun.misc.Unsafe U;
    private static final int ABASE;
    private static final int ASHIFT;
    private static final long CTL;
    private static final long RUNSTATE;
    private static final long STEALCOUNTER;
    private static final long PARKBLOCKER;
    private static final long QTOP;
    private static final long QLOCK;
    private static final long QSCANSTATE;
    private static final long QPARKER;
    private static final long QCURRENTSTEAL;
    private static final long QCURRENTJOIN;

    static {
        // 初始化字段偏移，用于CAS等操作
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ForkJoinPool.class;
            CTL = U.objectFieldOffset(k.getDeclaredField("ctl"));
            RUNSTATE = U.objectFieldOffset(k.getDeclaredField("runState"));
            STEALCOUNTER = U.objectFieldOffset(k.getDeclaredField("stealCounter"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
            Class<?> wk = WorkQueue.class;
            QTOP = U.objectFieldOffset(wk.getDeclaredField("top"));
            QLOCK = U.objectFieldOffset(wk.getDeclaredField("qlock"));
            QSCANSTATE = U.objectFieldOffset(wk.getDeclaredField("scanState"));
            QPARKER = U.objectFieldOffset(wk.getDeclaredField("parker"));
            QCURRENTSTEAL = U.objectFieldOffset(wk.getDeclaredField("currentSteal"));
            QCURRENTJOIN = U.objectFieldOffset(wk.getDeclaredField("currentJoin"));
            Class<?> ak = ForkJoinTask[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0)
                throw new Error("数据类型比例不是2的幂");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }

        commonMaxSpares = DEFAULT_COMMON_MAX_SPARES;
        defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
        modifyThreadPermission = new RuntimePermission("modifyThread");

        common = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<ForkJoinPool>() {
            public ForkJoinPool run() {
                return makeCommonPool();
            }
        });
        int par = common.config & SMASK; // 即使线程被禁用也报告1
        commonParallelism = par > 0 ? par : 1;
    }

    /**
     * 创建并返回公共池，尊重用户通过系统属性指定的设置。
     */
    private static ForkJoinPool makeCommonPool() {
        int parallelism = -1;
        ForkJoinWorkerThreadFactory factory = null;
        UncaughtExceptionHandler handler = null;
        try {
            // 忽略在访问/解析属性时的异常
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (pp != null)
                parallelism = Integer.parseInt(pp);
            if (fp != null)
                factory = ((ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance());
            if (hp != null)
                handler = ((UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance());
        } catch (Exception ignore) {
        }
        if (factory == null) {
            if (System.getSecurityManager() == null)
                factory = defaultForkJoinWorkerThreadFactory;
            else
                factory = new InnocuousForkJoinWorkerThreadFactory();
        }
        if (parallelism < 0 && // 默认并行度为核数减1
                (parallelism = Runtime.getRuntime().availableProcessors() - 1) <= 0)
            parallelism = 1;
        if (parallelism > MAX_CAP)
            parallelism = MAX_CAP;
        return new ForkJoinPool(parallelism, factory, handler, LIFO_QUEUE, "ForkJoinPool.commonPool-worker-");
    }

    /**
     * 工厂类用于创建无害的工作线程。
     */
    static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

        /**
         * 限制工厂自身权限的 ACC。构造的工作线程没有设置任何权限。
         */
        private static final AccessControlContext innocuousAcc;
        static {
            Permissions innocuousPerms = new Permissions();
            innocuousPerms.add(modifyThreadPermission);
            innocuousPerms.add(new RuntimePermission("enableContextClassLoaderOverride"));
            innocuousPerms.add(new RuntimePermission("modifyThreadGroup"));
            innocuousAcc = new AccessControlContext(new ProtectionDomain[] {
                    new ProtectionDomain(null, innocuousPerms)
            });
        }

        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return (ForkJoinWorkerThread.InnocuousForkJoinWorkerThread) java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedAction<ForkJoinWorkerThread>() {
                        public ForkJoinWorkerThread run() {
                            return new ForkJoinWorkerThread.InnocuousForkJoinWorkerThread(pool);
                        }
                    }, innocuousAcc);
        }
    }
}


