package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午9:33
 */

import org.top.java.concurrent.source.locks.LockSupport;
import org.top.java.concurrent.source.locks.ReentrantLock;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

/**
 * 一个支持完全并发检索和高预期并发更新的哈希表。
 * 该类遵守与 {@link java.util.Hashtable} 相同的功能规范，并包括与 {@code Hashtable} 的每个方法对应的方法版本。
 * 但是，尽管所有操作都是线程安全的，检索操作<em>不</em>需要加锁，且没有支持以某种方式锁定整个表来阻止所有访问的机制。
 * 该类完全与 {@code Hashtable} 在依赖其线程安全性但不依赖其同步细节的程序中互操作。
 *
 * <p>检索操作（包括 {@code get}）通常不会阻塞，因此可能会与更新操作（包括 {@code put} 和 {@code remove}）重叠。
 * 检索操作会反映在其启动时最近<em>完成</em>的更新操作的结果。（更正式地说，给定键的更新操作与报告该键更新值的任何（非空）检索之间存在 <em>happens-before</em> 关系。）
 * 对于诸如 {@code putAll} 和 {@code clear} 之类的聚合操作，并发检索可能只反映插入或删除的一部分条目。
 * 类似地，迭代器、Spliterator 和枚举返回的元素反映了哈希表在创建迭代器或枚举时或之后的某个时间点的状态。
 * 它们<em>不会</em>抛出 {@link java.util.ConcurrentModificationException ConcurrentModificationException}。
 * 但是，迭代器仅设计为一次由一个线程使用。
 * 请记住，聚合状态方法的结果（包括 {@code size}、{@code isEmpty} 和 {@code containsValue}）通常仅在地图没有被其他线程进行并发更新时才有用。
 * 否则，这些方法的结果仅反映瞬态状态，可能足以用于监控或估算目的，但不适用于程序控制。
 *
 * <p>当发生过多冲突时（即具有不同哈希码的键被分配到表大小的相同槽中），该表将动态扩展，预计的平均效果是保持每个映射大约两个桶（对应于 0.75 的负载因子阈值以进行调整）。添加和删除映射时，平均值可能会有很大的变化，但总体上，这保持了哈希表的常见时间/空间折衷。
 * 然而，调整此类或任何其他类型的哈希表可能是一个相对较慢的操作。可能的话，最好提供一个大小估计值作为可选的 {@code initialCapacity} 构造函数参数。
 * 另一个可选的 {@code loadFactor} 构造函数参数提供了一种通过指定用于计算为给定数量元素分配的空间量的表密度来进一步定制初始表容量的方法。
 * 另外，为了兼容此类的以前版本，构造函数还可以可选地指定预期的 {@code concurrencyLevel}，作为内部大小调整的另一个提示。
 * 请注意，使用具有完全相同 {@code hashCode()} 的许多键肯定会降低任何哈希表的性能。
 * 为了减轻这种影响，当键是 {@link Comparable} 时，该类可能使用键之间的比较顺序来帮助打破平局。
 *
 * <p>可以创建（使用 {@link #newKeySet()} 或 {@link #newKeySet(int)}）或查看（使用 {@link #keySet(Object)}）ConcurrentHashMap 的 {@link Set} 投影，仅当只对键感兴趣且映射的值（可能是暂时的）未使用或全部具有相同的映射值时。
 *
 * <p>ConcurrentHashMap 可以通过使用 {@link java.util.concurrent.atomic.LongAdder} 值并通过 {@link #computeIfAbsent computeIfAbsent} 初始化作为可扩展的频率映射（直方图或多集的一种形式）使用。
 * 例如，要向 {@code ConcurrentHashMap<String,LongAdder> freqs} 添加计数，可以使用 {@code freqs.computeIfAbsent(k -> new LongAdder()).increment();}。
 *
 * <p>此类及其视图和迭代器实现 {@link Map} 和 {@link Iterator} 接口的所有<em>可选</em>方法。
 *
 * <p>像 {@link Hashtable} 一样，但与 {@link HashMap} 不同，该类<em>不</em>允许 {@code null} 作为键或值使用。
 *
 * <p>ConcurrentHashMaps 支持一组顺序和并行批量操作，与大多数 {@link Stream} 方法不同，这些操作设计为即使在地图被其他线程并发更新时也能安全地并且通常合理地应用；例如，在共享注册表中计算值的快照摘要时。
 * 有三种操作，每种操作都有四种形式，接受带有键、值、条目和（键，值）参数和/或返回值的函数。由于 ConcurrentHashMap 的元素没有以任何特定方式排序，并且在不同的并行执行中可能以不同的顺序处理，所提供函数的正确性不应依赖于任何排序，或在计算进行时可能暂时更改的任何其他对象或值；除了 forEach 操作外，理想情况下应该没有副作用。对 {@link java.util.Map.Entry} 对象的批量操作不支持方法 {@code setValue}。
 *
 * <ul>
 * <li> forEach：对每个元素执行给定操作。变体形式在执行操作之前应用给定的转换。</li>
 *
 * <li> search：返回应用给定函数到每个元素的第一个可用非空结果；找到结果后跳过进一步搜索。</li>
 *
 * <li> reduce：累积每个元素。提供的归约函数不能依赖于排序（更正式地说，它应该是既结合性又交换性的）。有五种变体：
 *
 * <ul>
 *
 * <li>普通归约。（没有这种方法的形式适用于（键，值）函数参数，因为没有相应的返回类型。）</li>
 *
 * <li>映射归约，累积应用于每个元素的给定函数的结果。</li>
 *
 * <li>使用给定基值的标量双精度、长整型和整型归约。</li>
 *
 * </ul>
 * </li>
 * </ul>
 *
 * <p>这些批量操作接受一个 {@code parallelismThreshold} 参数。如果当前映射大小估计小于给定阈值，则方法顺序执行。使用 {@code Long.MAX_VALUE} 值可以抑制所有并行性。使用值 {@code 1} 通过将任务划分为足够多的子任务以充分利用 {@link ForkJoinPool#commonPool()}，可最大程度地并行化所有计算。通常，您会首先选择这些极端值之一，然后测量使用介于两者之间的值的性能，这些值在开销和吞吐量之间进行权衡。
 *
 * <p>批量操作的并发性属性遵循 ConcurrentHashMap 的属性：从 {@code get(key)} 和相关访问方法返回的任何非空结果都与相关的插入或更新存在 happens-before 关系。
 * 任何批量操作的结果反映了这些每个元素关系的组合（但不一定与地图整体原子化，除非它以某种方式被认为是静止的）。
 * 反过来，由于地图中的键和值永远不会为空，因此 null 是当前缺乏任何结果的可靠原子指示器。为了保持这一特性，null 是所有非标量归约操作的隐含基础。对于 double、long 和 int 版本，基础应该是与任何其他值组合时返回该其他值的值（更正式地说，它应该是归约的标识元素）。大多数常见的归约具有这些属性；例如，计算基值为 0 的和或基值为 MAX_VALUE 的最小值。
 *
 * <p>作为参数提供的搜索和转换函数应同样返回 null 以指示没有任何结果（在这种情况下不使用）。对于映射归约，这也允许转换作为过滤器使用，如果元素不应被组合，则返回 null（或对于基本特化，返回标识基础）。您可以通过在搜索或归约操作之前在此“null 表示现在没有内容”的规则下自行组合转换和过滤。
 *
 * <p>接受和/或返回条目参数的方法维护键值关联。例如，当查找最大值的键时，它们可能很有用。请注意，使用 {@code new AbstractMap.SimpleEntry(k,v)} 可以提供“普通”条目参数。
 *
 * <p>批量操作可能会突然完成，抛出在提供函数应用中遇到的异常。处理这些异常时请记住，其他并发执行的函数也可能抛出异常，或者如果第一个异常未发生，则会抛出异常。
 * <p>与顺序形式相比，并行形式的加速通常很常见，但不保证。 如果涉及简短函数的小地图上的并行操作比顺序形式执行得更慢，可能是因为并行化计算的底层工作比计算本身更昂贵。
 * 同样，如果所有处理器都忙于执行无关任务，可能也不会导致太多实际并行。
 *
 * <p>所有任务方法的参数必须为非空。
 *
 * <p>该类是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a>的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> 此映射维护的键的类型
 * @param <V> 此映射维护的映射值的类型
 */
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
        implements ConcurrentMap<K,V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    /**
     * 概述：
     *
     * 此哈希表的主要设计目标是保持并发可读性（通常是方法 get()，但也包括迭代器和相关方法）同时尽量减少更新争用。
     * 次要目标是保持与 java.util.HashMap 相同或更少的空间消耗，并支持由多个线程在空表上执行的高初始插入速率。
     *
     * 此映射通常作为分箱（桶）的哈希表运行。每个键-值映射保存在一个 Node 中。大多数节点是具有哈希、键、值和下一个字段的基本 Node 类实例。
     * 然而，存在各种子类：TreeNode 是在平衡树中排列的，而不是列表。TreeBin 保存 TreeNode 集合的根。ForwardingNode 在调整大小期间放置在箱的头部。ReservationNode 在 computeIfAbsent 和相关方法中建立值时用作占位符。
     * TreeBin、ForwardingNode 和 ReservationNode 类型不保存正常的用户键、值或哈希，并且在搜索等过程中可以轻松区分，因为它们具有负的哈希字段和空的键和值字段。（这些特殊节点要么是不常见的，要么是暂时的，因此携带一些未使用字段的影响是微不足道的。）
     *
     * 表在第一次插入时会延迟初始化为 2 的幂大小。表中的每个桶通常包含一个节点列表（大多数情况下，列表只有零或一个节点）。
     * 表访问需要使用 volatile/atomic 读、写和 CAS 操作。由于没有其他方法可以安排这一点而不增加进一步的间接寻址，因此我们使用内部操作（sun.misc.Unsafe）。
     *
     * 我们使用节点哈希字段的最高（符号）位进行控制用途——由于地址约束，它是可用的。哈希字段为负的节点在映射方法中得到特殊处理或被忽略。
     *
     * 在空桶中插入第一个节点（通过 put 或其变体）是通过将其 CAS 到桶中执行的。这在大多数键/哈希分布下是 put 操作最常见的情况。
     * 其他更新操作（插入、删除和替换）需要锁定。我们不希望浪费为每个桶关联一个独立锁对象所需的空间，因此改用桶列表的第一个节点本身作为锁。这些锁的锁定支持依赖于内建的 "synchronized" 监视器。
     *
     * 使用列表的第一个节点作为锁本身并不足够：当一个节点被锁定时，任何更新都必须首先验证锁定后它仍然是第一个节点，如果不是则重试。由于新节点总是附加到列表中，因此一旦节点成为桶中的第一个节点，它将保持第一个，直到被删除或桶失效（调整大小时）。
     *
     * 每个桶锁的主要缺点是由相同锁保护的桶列表中的其他节点的更新操作可能会停止，例如，当用户 equals() 或映射函数耗时较长时。
     * 但是，统计上来说，在随机哈希码下，这不是一个常见问题。理想情况下，桶中节点的频率遵循泊松分布（http://en.wikipedia.org/wiki/Poisson_distribution），其参数平均约为 0.5，给定 0.75 的调整阈值，尽管由于调整大小的粒度而具有较大方差。
     * 忽略方差，列表大小 k 的预期出现率为（exp(-0.5) * pow(0.5, k) / 阶乘(k)）。前几个值是：
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * 更多：低于千万分之一
     *
     * 在随机哈希下，两线程访问不同元素的锁争用概率约为 1 / (8 * #elements)。
     *
     * 实际哈希码分布在实践中有时会显著偏离均匀随机性。这包括当 N > (1<<30) 时，某些键必须发生冲突。
     * 同样，对于使用多个键的愚蠢或恶意用法，设计这些键具有相同的哈希码或仅在屏蔽的高位上有所不同。为了应对这种情况，当桶中节点数超过某个阈值时，我们使用次级策略。这些 TreeBin 使用平衡树来保存节点（一种专门形式的红黑树），将搜索时间限制为 O(log N)。
     * TreeBin 中的每个搜索步骤至少比常规列表慢两倍，但由于 N 不能超过 (1<<64)（在耗尽地址空间之前），因此这些搜索步骤、锁保持时间等被限制为合理的常数（最坏情况下每次操作检查大约 100 个节点），只要键是 Comparable（这很常见——String，Long 等等）。TreeBin 节点（TreeNode）还维护与常规节点相同的“下一个”遍历指针，因此可以在迭代器中以相同方式遍历它们。
     *
     * 当占用率超过一定百分比阈值时（名义上为 0.75，但见下文），表将调整大小。任何线程注意到桶过满时可能会协助调整大小，发起线程分配并设置替换数组后。
     * 然而，这些其他线程可能会继续插入等操作，而不会暂停。TreeBin 的使用屏蔽了最坏情况下调整大小期间过度填充的影响。调整大小通过将桶一个一个地从旧表传输到新表进行。但是，线程在执行之前通过字段 transferIndex 声明要传输的小块索引，从而减少争用。字段 sizeCtl 中的生成戳确保调整大小不会重叠。
     * 由于我们使用 2 的幂扩展，因此每个桶中的元素要么保持相同索引，要么移动到 2 的幂偏移位置。通过捕捉可以重用旧节点的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。平均而言，表翻倍时大约只有六分之一的节点需要克隆。它们替换的节点将在不再被可能同时遍历表的任何读取线程引用时可以被垃圾回收。
     * 传输完成后，旧表桶只包含一个特殊的转发节点（哈希字段为 "MOVED"），其中包含下一个表作为其键。遇到转发节点时，访问和更新操作重新开始，使用新表。
     *
     * 每个桶传输需要其桶锁，这可能会在调整大小期间等待锁定时停止。然而，由于其他线程可以加入并帮助调整大小而不是争夺锁，平均总等待时间随着调整大小的进展而变短。传输操作还必须确保旧表和新表中的所有可访问桶都可以被任何遍历使用。这部分通过从最后一个桶（table.length - 1）向上移动到第一个来安排。看到转发节点时，遍历（参见 Traverser 类）安排移动到新表而不重新访问节点。
     * 为了确保即使节点按顺序移出也不会跳过任何中间节点，第一次在遍历过程中遇到转发节点时，会创建一个堆栈（参见 TableStack 类），以便稍后处理当前表时维护其位置。
     * 这些保存/恢复机制的需要相对较少，但当遇到一个转发节点时，通常会有更多。所以 Traversers 使用一个简单的缓存方案来避免创建太多新的 TableStack 节点。（感谢 Peter Levart 提议在此处使用堆栈。）
     *
     * 遍历方案还适用于部分遍历桶范围（通过备用 Traverser 构造函数）以支持分区聚合操作。此外，如果被转发到空表，只读操作将放弃，这提供了对关闭风格清理的支持，当前尚未实现。
     *
     * 延迟表初始化最小化了第一次使用前的内存占用，并且还避免了当第一个操作来自 putAll、构造函数带有映射参数或反序列化时的调整大小。这些情况会尝试覆盖初始容量设置，但在竞争的情况下不会生效。
     *
     * 元素计数使用 LongAdder 的特化版本维护。我们需要合并一个特化版本而不是直接使用 LongAdder，以便访问隐含的争用感知机制，从而导致创建多个 CounterCell。计数器机制避免了更新时的争用，但在并发访问期间如果读取过于频繁，可能会遇到缓存抖动。为了避免频繁读取，只有在向已经包含两个或更多节点的桶添加时，才会在争用情况下尝试调整大小。根据均匀哈希分布，这种情况在阈值下发生的概率约为 13%，这意味着大约 8 次放入中只有 1 次检查阈值（并且在调整大小之后，检查的次数更少）。
     *
     * TreeBin 使用一种特殊的比较形式用于搜索和相关操作（这也是我们无法使用现有集合如 TreeMap 的主要原因）。
     * TreeBin 包含可比较的元素，但也可能包含其他元素，以及那些可比较但不一定对于相同类型 T 进行比较的元素，因此我们无法在它们之间调用 compareTo。
     * 为了解决这个问题，树主要按哈希值排序，如果适用，则按 Comparable.compareTo 顺序排序。
     * 在节点查找时，如果元素不可比较或比较结果为 0，则在哈希值相同的情况下可能需要搜索左右子节点。（这对应于在所有元素不可比较并且哈希值相同的情况下进行的完整列表搜索。）
     * 在插入时，为了保持重新平衡时的全局排序（或在此处所需的最接近排序），我们会比较类和 identityHashCode 作为平局的决胜条件。
     * 红黑树平衡代码从 pre-jdk-collections（http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java）更新而来，转而基于 Cormen、Leiserson 和 Rivest 的《算法导论》。
     *
     * TreeBin 还需要额外的锁定机制。虽然即使在更新期间，列表遍历对读取者来说总是可行的，但树遍历则不行，主要是因为树旋转可能会更改根节点和/或其链接。
     * TreeBin 包含一种寄生在主要桶同步策略上的简单读写锁机制：与插入或删除相关的结构调整已经是桶锁定的（因此不会与其他写入者冲突），但必须等待正在进行的读取者完成。
     * 由于最多只能有一个这样的等待者，我们使用了一个简单的方案，使用单个“等待者”字段来阻止写入者。
     * 然而，读取者永远不需要阻塞。如果持有根锁，他们将沿着慢速遍历路径（通过 next 指针）继续前进，直到锁变得可用或列表耗尽，以先到者为准。这些情况速度不快，但最大限度地提高了总体预期吞吐量。
     *
     * 保持与此类先前版本的 API 和序列化兼容性引入了几个怪异之处。主要是：我们保留了未触动但未使用的构造函数参数 concurrencyLevel。我们接受一个 loadFactor 构造函数参数，但仅将其应用于初始表容量（这是我们唯一能够保证遵守它的时间）。我们还声明了一个未使用的“Segment”类，它仅在序列化时以最小形式实例化。
     *
     * 同样，仅出于与此类先前版本兼容的目的，它扩展了 AbstractMap，即使其所有方法都被重写，因此它只是无用的负担。
     *
     * 该文件的组织使得阅读时比其他情况下更容易跟随：首先是主要的静态声明和实用程序，然后是字段，然后是主要的公共方法（有一些将多个公共方法分解为内部方法的情况），然后是大小调整方法、树、遍历器和批量操作。

     */

    /* ---------------- 常量 -------------- */

    /**
     * 最大可能的表容量。此值必须精确为 1<<30，以保持在 Java 数组分配和索引边界内的 2 的幂大小表，并且还要求因为 32 位哈希字段的前两位用于控制用途。
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认的初始表容量。必须为 2 的幂（即至少为 1）并且不超过 MAXIMUM_CAPACITY。
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 最大可能的（非 2 的幂）数组大小。
     * toArray 和相关方法所需。
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 此表的默认并发级别。未使用但为与此类先前版本兼容而定义。
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 此表的负载因子。此值在构造函数中的覆盖仅影响初始表容量。
     * 实际的浮点值通常不使用——更简单的是使用类似 {@code n - (n >>> 2)} 的表达式作为相关的调整阈值。
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 用于将桶列表转换为树的桶节点数阈值。当向具有至少此数量节点的桶中添加元素时，桶将转换为树。该值必须大于 2，并且应至少为 8，以与树删除中的缩减回普通桶的转换假设相匹配。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 在调整大小操作期间去树化（拆分）桶的节点数阈值。应小于 TREEIFY_THRESHOLD，并且至多为 6，以与移除时的缩减检测相匹配。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 可将桶树化的最小表容量。（否则如果桶中有过多节点则调整表的大小。）该值应至少为 4 * TREEIFY_THRESHOLD，以避免调整大小与树化阈值之间的冲突。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 每次传输步骤的最小重新分配数量。范围被细分以允许多个调整大小的线程。
     * 该值作为下限以避免调整大小线程遇到过度的内存争用。该值应至少为 DEFAULT_CAPACITY。
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * sizeCtl 中用于生成戳的位数。
     * 对于 32 位数组，必须至少为 6。
     */
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * 可以帮助调整大小的最大线程数。
     * 必须适合 32 - RESIZE_STAMP_BITS 位。
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * 用于在 sizeCtl 中记录大小戳的位移。
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /*
     * 节点哈希字段的编码。参见上文说明。
     */
    static final int MOVED     = -1; // 转发节点的哈希值
    static final int TREEBIN   = -2; // 树根节点的哈希值
    static final int RESERVED  = -3; // 临时预留节点的哈希值
    static final int HASH_BITS = 0x7fffffff; // 常规节点哈希值的可用位

    /** CPU 数量，用于在某些大小调整中设置界限 */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** 为了序列化兼容性。 */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("segments", Segment[].class),
            new ObjectStreamField("segmentMask", Integer.TYPE),
            new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /* ---------------- 节点 -------------- */

    /**
     * 键值条目。此类不会作为支持 setValue 的用户可变 Map.Entry（参见 MapEntry）导出，但可用于批量任务中的只读遍历。
     * 具有负哈希字段的 Node 子类是特殊的，包含空键和值（但从不导出）。否则，键和值从不为空。
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u;
            Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * 用于 map.get() 的虚拟支持；在子类中重写。
         */
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- 静态工具方法 -------------- */

    /**
     * 将哈希值的高位扩展 (XOR) 到低位，并且将最高位强制设为0。由于表使用的是2的幂次掩码，哈希值中仅在当前掩码之上的位数不同的集合会始终发生碰撞。（已知的例子是一些Float键的集合，这些键在较小的表中持有连续的整数值。）因此，我们应用一种变换，将高位的影响向下扩展。这里存在速度、实用性和位扩展质量之间的权衡。因为许多常见的哈希值集合已经相对合理地分布（因此不需要扩展），并且我们使用树来处理桶中的大量碰撞集合，所以我们仅通过最便宜的方式 XOR 一些移位后的位数，以减少系统性损耗，并同时包含那些因为表界限而永远不会被用于索引计算的最高位的影响。
     */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * 返回给定期望容量的2的幂次的表大小。参考《Hackers Delight》第3.2节
     */
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * 如果x的形式为“class C implements Comparable<C>”，则返回x的类，否则返回null。
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // 跳过检查
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType)t).getRawType() == Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // 类型参数是c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * 如果x匹配kc（k的筛选后的可比较类），则返回k.compareTo(x)，否则返回0。
     */
    @SuppressWarnings({"rawtypes","unchecked"}) // 用于强制转换为Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 : ((Comparable)k).compareTo(x));
    }

    /* ---------------- 表元素访问方法 -------------- */

    /*
     * 使用volatile访问方法访问表元素以及正在调整大小时的下一个表的元素。调用者必须检查tab参数是否为null。所有调用者还必须谨慎地预先检查tab的长度是否不为零（或等效的检查），从而确保以哈希值为基础的索引与(length - 1)进行与操作时产生的索引是有效的。注意，为了与用户可能引起的并发错误保持一致，这些检查必须在局部变量上操作，这也解释了下面一些看似奇怪的内联赋值语句。此外，调用setTabAt方法始终在锁定的区域内进行，原则上只需要释放排序，而不需要完整的volatile语义，但目前已被编码为volatile写入以保持保守性。
     */

    @SuppressWarnings("unchecked")
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }

    /* ---------------- 字段 -------------- */

    /**
     * 桶数组。在第一次插入时懒惰地初始化。大小总是2的幂。直接由迭代器访问。
     */
    transient volatile Node<K,V>[] table;

    /**
     * 要使用的下一个表；仅在调整大小时非null。
     */
    private transient volatile Node<K,V>[] nextTable;

    /**
     * 基本计数器值，主要用于在没有竞争时，但也作为表初始化竞争期间的后备。通过CAS更新。
     */
    private transient volatile long baseCount;

    /**
     * 表初始化和调整大小控制。当为负数时，表正在初始化或调整大小：-1表示初始化，否则表示-(1 + 活动调整大小线程的数量)。另外，当表为空时，保存创建时要使用的初始表大小，或者为0表示默认值。初始化后，保存下一个用于调整表大小的元素计数值。
     */
    private transient volatile int sizeCtl;

    /**
     * 调整大小时要拆分的下一个表索引（加1）。
     */
    private transient volatile int transferIndex;

    /**
     * 在调整大小和/或创建CounterCells时使用的自旋锁（通过CAS锁定）。
     */
    private transient volatile int cellsBusy;

    /**
     * 计数器单元表。当非null时，大小为2的幂次。
     */
    private transient volatile CounterCell[] counterCells;

    // 视图
    private transient KeySetView<K,V> keySet;
    private transient ValuesView<K,V> values;
    private transient EntrySetView<K,V> entrySet;

    /* ---------------- 公共操作 -------------- */

    /**
     * 创建一个新的、空的映射，具有默认的初始表大小（16）。
     */
    public ConcurrentHashMap() {
    }

    /**
     * 创建一个新的、空的映射，具有一个初始表大小，足以容纳指定数量的元素，而无需动态调整大小。
     *
     * @param initialCapacity 实现执行内部调整，以容纳这么多元素。
     * @throws IllegalArgumentException 如果初始容量为负数
     */
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    /**
     * 创建一个与给定映射具有相同映射的新映射。
     *
     * @param m 要复制的映射
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    /**
     * 创建一个新的、空的映射，具有基于指定元素数量({@code initialCapacity})和初始表密度({@code loadFactor})的初始表大小。
     *
     * @param initialCapacity 初始容量。实现执行内部调整，以容纳这么多元素，给定指定的装载因子。
     * @param loadFactor 用于确定初始表大小的装载因子（表密度）
     * @throws IllegalArgumentException 如果初始容量为负数或装载因子为非正数
     *
     * @since 1.6
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * 创建一个新的、空的映射，具有基于指定元素数量({@code initialCapacity})、表密度({@code loadFactor})和并发更新线程数({@code concurrencyLevel})的初始表大小。
     *
     * @param initialCapacity 初始容量。实现执行内部调整，以容纳这么多元素，给定指定的装载因子。
     * @param loadFactor 用于确定初始表大小的装载因子（表密度）
     * @param concurrencyLevel 估计的并发更新线程数。实现可能会使用此值作为大小调整的提示。
     * @throws IllegalArgumentException 如果初始容量为负数、装载因子或并发级别为非正数
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // 使用至少与估计的线程数一样多的桶
            initialCapacity = concurrencyLevel;
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }

    // 原始 (自JDK1.2起) Map方法

    /**
     * {@inheritDoc}
     */
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int)n);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return sumCount() <= 0L; // 忽略瞬时的负值
    }

    /**
     * 返回指定键映射到的值，如果此映射不包含该键的映射，则返回{@code null}。
     *
     * <p>更正式地说，如果此映射包含从键{@code k}到值{@code v}的映射，使得{@code key.equals(k)}，则此方法返回{@code v}；否则，它返回{@code null}。 (最多可以有一个这样的映射。)
     *
     * @throws NullPointerException 如果指定的键为null
     */
    public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    /**
     * 测试指定的对象是否是此表中的键。
     *
     * @param  key 可能的键
     * @return {@code true} 当且仅当指定的对象是此表中的键时，结果通过{@code equals}方法确定；否则返回{@code false}
     * @throws NullPointerException 如果指定的键为null
     */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * 如果此映射将一个或多个键映射到指定的值，则返回{@code true}。注意：此方法可能需要完整遍历映射，速度远低于{@code containsKey}方法。
     *
     * @param value 测试此映射中是否存在的值
     * @return {@code true} 如果此映射将一个或多个键映射到指定的值
     * @throws NullPointerException 如果指定的值为null
     */
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }

    /**
     * 将指定的键映射到此表中的指定值。键和值都不能为null。
     *
     * <p>可以通过调用{@code get}方法并提供与原始键相等的键来检索该值。
     *
     * @param key 键，与其关联的值将与之关联
     * @param value 将与指定键关联的值
     * @return 先前与{@code key}关联的值；如果没有该键的映射，则返回{@code null}
     * @throws NullPointerException 如果指定的键或值为null
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /** put 和 putIfAbsent 的实现 */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                        new Node<K,V>(hash, key, value, null)))
                    break;                   // 没有锁定时，添加到空桶
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key, value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key, value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * 将指定映射中的所有映射复制到此映射中。这些映射将替换此映射中当前存在的任何相同键的映射。
     *
     * @param m 要存储在此映射中的映射
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    /**
     * 从此映射中移除键及其对应的值。如果此映射中没有该键，则此方法不执行任何操作。
     *
     * @param key 要移除的键
     * @return 先前与{@code key}关联的值，如果没有该键的映射，则返回{@code null}
     * @throws NullPointerException 如果指定的键为null
     */
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * 实现四个公共remove/replace方法的实现：将节点的值替换为v，并在匹配到cv时有条件地进行。如果结果值为null，则删除该节点。
     */
    final V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0 ||
                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev ||
                                            (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null)
                                            e.val = value;
                                        else if (pred != null)
                                            pred.next = e.next;
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                        (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 从此映射中移除所有映射。
     */
    public void clear() {
        long delta = 0L; // 删除的负数数量
        int i = 0;
        Node<K,V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            Node<K,V> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // 重新开始
            }
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K,V> p = (fh >= 0 ? f :
                                (f instanceof TreeBin) ?
                                        ((TreeBin<K,V>)f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * 返回此映射中包含的键的{@link Set}视图。该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。集合支持元素的移除，这将通过{@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll}和{@code clear}操作移除映射中的对应映射。它不支持{@code add}或{@code addAll}操作。
     *
     * <p>视图的迭代器和分割迭代器是<a href="package-summary.html#Weakly"><i>弱一致性</i></a>。
     *
     * <p>视图的{@code spliterator}报告{@link Spliterator#CONCURRENT}、{@link Spliterator#DISTINCT}和{@link Spliterator#NONNULL}。
     *
     * @return 键的集合视图
     */
    public KeySetView<K,V> keySet() {
        KeySetView<K,V> ks;
        return (ks = keySet) != null ? ks : (keySet = new KeySetView<K,V>(this, null));
    }

    /**
     * 返回此映射中包含的值的{@link Collection}视图。该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。集合支持元素的移除，这将通过{@code Iterator.remove}、{@code Collection.remove}、{@code removeAll}、{@code retainAll}和{@code clear}操作移除映射中的对应映射。它不支持{@code add}或{@code addAll}操作。
     *
     * <p>视图的迭代器和分割迭代器是<a href="package-summary.html#Weakly"><i>弱一致性</i></a>。
     *
     * <p>视图的{@code spliterator}报告{@link Spliterator#CONCURRENT}和{@link Spliterator#NONNULL}。
     *
     * @return 值的集合视图
     */
    public Collection<V> values() {
        ValuesView<K,V> vs;
        return (vs = values) != null ? vs : (values = new ValuesView<K,V>(this));
    }

    /**
     * 返回此映射中包含的映射关系的{@link Set}视图。该集合由映射支持，因此对映射的更改会反映在集合中，反之亦然。集合支持元素的移除，这将通过{@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll}和{@code clear}操作移除映射中的对应映射。
     *
     * <p>视图的迭代器和分割迭代器是<a href="package-summary.html#Weakly"><i>弱一致性</i></a>。
     *
     * <p>视图的{@code spliterator}报告{@link Spliterator#CONCURRENT}、{@link Spliterator#DISTINCT}和{@link Spliterator#NONNULL}。
     *
     * @return 映射关系的集合视图
     */
    public Set<Entry<K,V>> entrySet() {
        EntrySetView<K,V> es;
        return (es = entrySet) != null ? es : (entrySet = new EntrySetView<K,V>(this));
    }

    /**
     * 返回此{@link Map}的哈希码值，即对于映射中的每个键值对，返回{@code key.hashCode() ^ value.hashCode()}的和。
     *
     * @return 此映射的哈希码值
     */
    public int hashCode() {
        int h = 0;
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }

    /**
     * 返回此映射的字符串表示形式。字符串表示形式包含在括号“{@code {}}”内的一组键值映射。（没有特定顺序）。相邻映射之间用字符“{@code , }”分隔。每个键值映射以键、等号“{@code =}”和相关联的值表示。
     *
     * @return 此映射的字符串表示形式
     */
    public String toString() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K,V> p;
        if ((p = it.advance()) != null) {
            for (;;) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * 比较指定的对象与此映射是否相等。如果给定对象是一个与此映射具有相同映射关系的映射，则返回{@code true}。如果在此方法执行期间，任何映射被并发修改，此操作可能返回误导性结果。
     *
     * @param o 要与此映射比较的对象
     * @return 如果指定对象等于此映射，则返回{@code true}
     */
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?,?> m = (Map<?,?>) o;
            Node<K,V>[] t;
            int f = (t = table) == null ? 0 : t.length;
            Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val)))
                    return false;
            }
            for (Map.Entry<?,?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * 这是用于以前版本中的辅助类的简化版本，声明它是为了保持序列化兼容性。
     */
    static class Segment<K,V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;
        Segment(float lf) { this.loadFactor = lf; }
    }

    /**
     * 将{@code ConcurrentHashMap}实例的状态保存到流中（即，将其序列化）。
     * @param s 流
     * @throws java.io.IOException 如果发生I/O错误
     * @serialData
     *  对于每个键值映射，依次保存键（Object）和值（Object），然后是一个空对。键值映射无特定顺序输出。
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // 为了保持序列化兼容性
        // 模拟此类以前版本中的段计算
        int sshift = 0;
        int ssize = 1;
        while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
            ++sshift;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        @SuppressWarnings("unchecked")
        Segment<K,V>[] segments = (Segment<K,V>[])
                new Segment<?,?>[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < segments.length; ++i)
            segments[i] = new Segment<K,V>(LOAD_FACTOR);
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
        s.writeFields();

        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
        segments = null; // 丢弃
    }

    /**
     * 从流中重建实例（即反序列化它）。
     * @param s 流
     * @throws ClassNotFoundException 如果找不到序列化对象的类
     * @throws java.io.IOException 如果发生I/O错误
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        /*
         * 为了提高典型情况下的性能，我们在读取时创建节点，
         * 然后在已知大小后将它们放入表中。
         * 然而，我们必须在此过程中验证唯一性，并处理过度填充的桶，
         * 这需要特殊的putVal机制版本。
         */
        sizeCtl = -1; // 强制排除表构造
        s.defaultReadObject();
        long size = 0L;
        Node<K,V> p = null;
        for (;;) {
            @SuppressWarnings("unchecked")
            K k = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V v = (V) s.readObject();
            if (k != null && v != null) {
                p = new Node<K,V>(spread(k.hashCode()), k, v, p);
                ++size;
            } else
                break;
        }
        if (size == 0L)
            sizeCtl = 0;
        else {
            int n;
            if (size >= (long)(MAXIMUM_CAPACITY >>> 1))
                n = MAXIMUM_CAPACITY;
            else {
                int sz = (int)size;
                n = tableSizeFor(sz + (sz >>> 1) + 1);
            }
            @SuppressWarnings("unchecked")
            Node<K,V>[] tab = (Node<K,V>[])new Node<?,?>[n];
            int mask = n - 1;
            long added = 0L;
            while (p != null) {
                boolean insertAtFront;
                Node<K,V> next = p.next, first;
                int h = p.hash, j = h & mask;
                if ((first = tabAt(tab, j)) == null)
                    insertAtFront = true;
                else {
                    K k = p.key;
                    if (first.hash < 0) {
                        TreeBin<K,V> t = (TreeBin<K,V>)first;
                        if (t.putTreeVal(h, k, p.val) == null)
                            ++added;
                        insertAtFront = false;
                    } else {
                        int binCount = 0;
                        insertAtFront = true;
                        Node<K,V> q; K qk;
                        for (q = first; q != null; q = q.next) {
                            if (q.hash == h &&
                                    ((qk = q.key) == k ||
                                            (qk != null && k.equals(qk)))) {
                                insertAtFront = false;
                                break;
                            }
                            ++binCount;
                        }
                        if (insertAtFront && binCount >= TREEIFY_THRESHOLD) {
                            insertAtFront = false;
                            ++added;
                            p.next = first;
                            TreeNode<K,V> hd = null, tl = null;
                            for (q = p; q != null; q = q.next) {
                                TreeNode<K,V> t = new TreeNode<K,V>
                                        (q.hash, q.key, q.val, null, null);
                                if ((t.prev = tl) == null)
                                    hd = t;
                                else
                                    tl.next = t;
                                tl = t;
                            }
                            setTabAt(tab, j, new TreeBin<K,V>(hd));
                        }
                    }
                }
                if (insertAtFront) {
                    ++added;
                    p.next = first;
                    setTabAt(tab, j, p);
                }
                p = next;
            }
            table = tab;
            sizeCtl = n - (n >>> 2);
            baseCount = added;
        }
    }

    // ConcurrentMap 方法

    /**
     * {@inheritDoc}
     *
     * @return 与指定键关联的先前值，
     *         如果该键没有映射则返回 {@code null}
     * @throws NullPointerException 如果指定的键或值为 null
     */
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException 如果指定的键为 null
     */
    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException 如果任意一个参数为 null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return 与指定键关联的先前值，
     *         如果该键没有映射则返回 {@code null}
     * @throws NullPointerException 如果指定的键或值为 null
     */
    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

// JDK8+ Map 扩展方法的重写

    /**
     * 返回与指定键关联的值，如果该映射中没有该键的映射，则返回
     * 给定的默认值。
     *
     * @param key 与其关联值一起返回的键
     * @param defaultValue 如果映射中没有该键，则返回的值
     * @return 键的映射值（如果存在）；否则为默认值
     * @throws NullPointerException 如果指定的键为 null
     */
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key;;) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null)
                        throw new NullPointerException();
                    if (replaceNode(key, newValue, oldValue) != null ||
                            (oldValue = get(key)) == null)
                        break;
                }
            }
        }
    }

    /**
     * 如果指定的键尚未与值关联，
     * 则尝试使用给定的映射函数计算其值，并将其插入到此映射中，除非为 {@code null}。
     * 整个方法调用是原子性的，因此每个键的函数最多应用一次。
     * 其他线程对该映射的某些更新操作可能会被阻塞，因此计算应简单、快速，
     * 并且不得尝试更新此映射的任何其他映射。
     *
     * @param key 要与指定值关联的键
     * @param mappingFunction 计算值的函数
     * @return 当前与指定键关联的值（已存在或已计算），
     *         如果计算的值为 null 则返回 null
     * @throws NullPointerException 如果指定的键或映射函数为 null
     * @throws IllegalStateException 如果计算过程中检测到递归更新此映射的行为，
     *         该行为可能永远不会完成
     * @throws RuntimeException 或 Error 如果映射函数抛出异常，
     *         在这种情况下映射不会建立
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new Node<K,V>(h, key, val, null);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek; V ev;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }

    /**
     * 如果指定键存在，则尝试使用给定的函数计算新的映射。
     * 整个方法调用是原子性的，其他线程可能会被阻塞，
     * 所以计算应尽可能简单且快速。
     *
     * @param key 可能与某个值关联的键
     * @param remappingFunction 用于计算值的函数
     * @return 与指定键关联的新值，如果没有则为 null
     * @throws NullPointerException 如果指定的键或函数为 null
     * @throws IllegalStateException 如果检测到递归更新的行为
     * @throws RuntimeException 或 Error 如果函数抛出异常
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * 尝试计算指定键和当前值（如果有）的映射。
     * 整个方法调用是原子性的，阻止其他线程进行更新。
     *
     * @param key 与值关联的键
     * @param remappingFunction 计算新值的函数
     * @return 与指定键关联的新值，如果没有则为 null
     * @throws NullPointerException 如果键或函数为 null
     * @throws IllegalStateException 如果检测到递归更新
     * @throws RuntimeException 或 Error 如果函数抛出异常
     */
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K,V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next = new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            V pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * 如果指定键没有关联值，则将其与给定值关联。
     * 否则，使用给定的重新映射函数替换值，或者如果新值为 null 则移除映射。
     * 整个方法调用是原子性的，可能会阻止其他线程的更新。
     *
     * @param key 与指定值关联的键
     * @param value 如果缺少值时使用的默认值
     * @param remappingFunction 用于重新计算值的函数
     * @return 与指定键关联的新值，如果没有则为 null
     * @throws NullPointerException 如果指定的键、值或函数为 null
     * @throws RuntimeException 或 Error 如果函数抛出异常，映射将保持不变
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next = new Node<K,V>(h, key, val, null);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r = t.root;
                            TreeNode<K,V> p = (r == null) ? null :
                                    r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                    remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    // Hashtable 的遗留方法

    /**
     * 测试某个键是否映射到此表中的指定值。
     * 此方法的功能与 {@link #containsValue(Object)} 相同，
     * 仅为了确保与 {@link java.util.Hashtable} 类的完全兼容，
     * 该类在引入 Java Collections 框架之前支持此方法。
     *
     * @param value 要搜索的值
     * @return {@code true} 如果并且只有当某个键映射到此表中的 {@code value} 参数
     *         时（通过 {@code equals} 方法判断）；否则为 {@code false}
     * @throws NullPointerException 如果指定的值为 null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * 返回此表中键的枚举。
     *
     * @return 此表中键的枚举
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new KeyIterator<K,V>(t, f, 0, f, this);
    }

    /**
     * 返回此表中值的枚举。
     *
     * @return 此表中值的枚举
     * @see #values()
     */
    public Enumeration<V> elements() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new ValueIterator<K,V>(t, f, 0, f, this);
    }

    // 仅限 ConcurrentHashMap 的方法

    /**
     * 返回映射的数量。该方法应优先于 {@link #size} 使用，因为
     * {@code ConcurrentHashMap} 中可能包含的映射数量超出了 int 的表示范围。
     * 返回的值是一个估计值；如果有并发插入或删除操作，实际计数可能会有所不同。
     *
     * @return 映射的数量
     * @since 1.8
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // 忽略暂时的负值
    }

    /**
     * 从给定类型到 {@code Boolean.TRUE} 创建一个由 ConcurrentHashMap 支持的新 {@link Set}。
     *
     * @param <K> 返回集合的元素类型
     * @return 新的集合
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet() {
        return new KeySetView<K,Boolean>(new ConcurrentHashMap<K,Boolean>(), Boolean.TRUE);
    }

    /**
     * 从给定类型到 {@code Boolean.TRUE} 创建一个由 ConcurrentHashMap 支持的新 {@link Set}。
     *
     * @param initialCapacity 实现会执行内部大小调整，以容纳指定数量的元素。
     * @param <K> 返回集合的元素类型
     * @return 新的集合
     * @throws IllegalArgumentException 如果元素的初始容量为负
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<K,Boolean>(new ConcurrentHashMap<K,Boolean>(initialCapacity), Boolean.TRUE);
    }

    /**
     * 返回此映射中键的 {@link Set} 视图，使用给定的公共映射值进行任何添加操作
     * （即 {@link Collection#add} 和 {@link Collection#addAll(Collection)}）。
     * 如果接受为此视图中的所有添加操作使用相同的值，则此方法才是合适的。
     *
     * @param mappedValue 用于任何添加操作的映射值
     * @return 键的集合视图
     * @throws NullPointerException 如果 {@code mappedValue} 为 null
     */
    public KeySetView<K,V> keySet(V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new KeySetView<K,V>(this, mappedValue);
    }

    /* ---------------- 特殊节点 -------------- */

    /**
     * 在转移操作期间插入到桶中的头节点。
     */
    static final class ForwardingNode<K,V> extends Node<K,V> {
        final Node<K,V>[] nextTable; // 保存下一个表的引用

        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null); // 调用父类构造函数，表明节点已移动
            this.nextTable = tab; // 初始化 nextTable
        }

        Node<K,V> find(int h, Object k) {
            // 通过循环避免对转发节点进行深度递归
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    if ((eh = e.hash) == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable; // 转到下一个表
                            continue outer;
                        }
                        else
                            return e.find(h, k); // 调用父类的查找方法
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * 用于 computeIfAbsent 和 compute 方法中的占位符节点
     */
    static final class ReservationNode<K,V> extends Node<K,V> {
        ReservationNode() {
            super(RESERVED, null, null, null); // 调用父类构造函数，节点标识为保留状态
        }

        Node<K,V> find(int h, Object k) {
            return null; // 保留节点不做实际查找
        }
    }

    /* ---------------- 表初始化和调整大小 -------------- */

    /**
     * 返回用于调整大小的标识位，表的大小为 n。
     * 左移 RESIZE_STAMP_SHIFT 位后必须为负。
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * 初始化表，使用 sizeCtl 中记录的大小。
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // 失去初始化竞争；仅进行忙等待
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY; // 根据 sc 确定容量
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n]; // 创建新的表
                        table = tab = nt;
                        sc = n - (n >>> 2); // 更新 sizeCtl 的值为 0.75 倍的容量
                    }
                } finally {
                    sizeCtl = sc; // 释放控制权
                }
                break;
            }
        }
        return tab;
    }

    /**
     * 增加计数，如果表太小且未调整大小，则启动转移。如果已经在调整大小，则帮助执行转移。
     * 转移后重新检查占用情况，以查看是否需要再次调整大小，因为调整大小可能落后于添加操作。
     *
     * @param x 要增加的计数
     * @param check 如果 < 0，则不检查调整大小；如果 <= 1，则仅在没有竞争时检查
     */
    private final void addCount(long x, int check) {
        CounterCell[] as; long b, s;
        if ((as = counterCells) != null ||
                !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell a; long v; int m;
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                    (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                    !(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, uncontended); // 执行完整的增加操作
                return;
            }
            if (check <= 1)
                return;
            s = sumCount(); // 重新计算总和
        }
        if (check >= 0) {
            Node<K,V>[] tab, nt; int n, sc;
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                    (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt); // 执行转移
                } else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2)) {
                    transfer(tab, null); // 开始新的转移
                }
                s = sumCount(); // 重新计算总和
            }
        }
    }

    /**
     * 如果调整大小正在进行，则帮助转移。
     */
    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        Node<K,V>[] nextTab; int sc;
        if (tab != null && (f instanceof ForwardingNode) &&
                (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && table == tab &&
                    (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab); // 帮助转移操作
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

    /**
     * 尝试调整表的大小以容纳指定数量的元素。
     *
     * @param size 元素数量（不需要精确）
     */
    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K,V>[] tab = table; int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    Node<K,V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                } else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }

    /**
     * 移动并/或复制每个桶中的节点到新的表。
     */
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // 分配较小的传输范围
        if (nextTab == null) {            // 开始转移
            try {
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1]; // 新表的大小是旧表的两倍
                nextTab = nt;
            } catch (Throwable ex) {      // 处理 OOME 错误
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab); // 创建转发节点
        boolean advance = true;
        boolean finishing = false; // 确保在提交 nextTab 之前完成转移
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                } else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex,
                        nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // 在提交之前重新检查
                }
            } else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            else if ((fh = f.hash) == MOVED)
                advance = true; // 已经处理过
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K,V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof TreeBin) {
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                        (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }

    /* ---------------- 计数器支持 -------------- */

    /**
     * 用于分配计数的填充单元。源自 LongAdder 和 Striped64。参见它们的内部文档以了解更多信息。
     */
    @sun.misc.Contended static final class CounterCell {
        volatile long value; // 保存当前单元的值

        CounterCell(long x) { value = x; } // 构造函数初始化为给定值
    }

    /**
     * 计算所有计数单元的和，包括 baseCount 和 counterCells。
     */
    final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount; // 从 baseCount 开始累加
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) // 如果计数单元存在，则将其值加到 sum 中
                    sum += a.value;
            }
        }
        return sum; // 返回总和
    }

    /**
     * 参见 LongAdder 版本的解释，完成完整的计数添加操作。
     */
    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // 强制初始化探针
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // 是否出现碰撞，即最后的槽位非空
        for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // 尝试附加新的计数单元
                        CounterCell r = new CounterCell(x); // 乐观创建新单元
                        if (cellsBusy == 0 &&
                                U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // 在锁下重新检查
                                CounterCell[] rs; int m, j;
                                if ((rs = counterCells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // 插槽现在非空，继续循环
                        }
                    }
                    collide = false;
                }
                else if (!wasUncontended)       // 如果 CAS 已经失败
                    wasUncontended = true;      // 在重新计算后继续
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    break;
                else if (counterCells != as || n >= NCPU)
                    collide = false;            // 达到最大容量或单元表已过期
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == as) { // 扩展表，除非表已过期
                            CounterCell[] rs = new CounterCell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            counterCells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // 用扩展表重新尝试
                }
                h = ThreadLocalRandom.advanceProbe(h); // 探针前进
            }
            else if (cellsBusy == 0 && counterCells == as &&
                    U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {                           // 初始化单元表
                    if (counterCells == as) {
                        CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            }
            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                break;                          // 回退到使用 baseCount 计数
        }
    }

    /* ---------------- 树形节点转换 -------------- */

    /**
     * 替换给定索引的所有链表节点，除非表太小，在这种情况下调整表大小。
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY) // 小于最小树化容量
                tryPresize(n << 1); // 调整表大小
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                    new TreeNode<K,V>(e.hash, e.key, e.val, null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K,V>(hd)); // 替换为树形节点
                    }
                }
            }
        }
    }

    /**
     * 返回替换给定链表中节点的非树节点列表。
     */
    static <K,V> Node<K,V> untreeify(Node<K,V> b) {
        Node<K,V> hd = null, tl = null;
        for (Node<K,V> q = b; q != null; q = q.next) {
            Node<K,V> p = new Node<K,V>(q.hash, q.key, q.val, null);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }
        return hd; // 返回链表形式的节点
    }

    /* ---------------- 树节点 -------------- */

    /**
     * 用于 TreeBin 的节点
     */
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // 红黑树的链接
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // 在删除时取消链接 next
        boolean red;           // 红黑树的颜色

        TreeNode(int hash, K key, V val, Node<K,V> next, TreeNode<K,V> parent) {
            super(hash, key, val, next); // 调用父类构造函数
            this.parent = parent; // 设置父节点
        }

        Node<K,V> find(int h, Object k) {
            return findTreeNode(h, k, null); // 查找树形节点
        }

        /**
         * 从给定根节点开始，返回与给定键匹配的 TreeNode（如果未找到，则返回 null）。
         */
        final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K,V> p = this;
                do  {
                    int ph, dir; K pk; TreeNode<K,V> q;
                    TreeNode<K,V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl; // 查找左子树
                    else if (ph < h)
                        p = pr; // 查找右子树
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p; // 找到匹配节点
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                            (kc = comparableClassFor(k)) != null) &&
                            (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null; // 如果没找到，返回 null
        }
    }

    /* ---------------- 树形结构容器 -------------- */

    /**
     * 用于桶的树形节点（TreeNode）的容器。TreeBins 不保存用户键或值，而是指向 TreeNode 列表及其根节点。
     * 它们还维护一个读写锁，强制写操作（持有桶锁的线程）等待读操作（不持有桶锁的线程）完成，然后再进行树重构操作。
     */
    static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;   // 树的根节点
        volatile TreeNode<K,V> first; // 第一个节点（头节点）
        volatile Thread waiter; // 当前等待锁的线程
        volatile int lockState; // 锁的状态
        // 锁状态的值
        static final int WRITER = 1; // 表示持有写锁
        static final int WAITER = 2; // 表示有线程在等待写锁
        static final int READER = 4; // 递增值，表示读锁的持有者数量

        /**
         * 用于排序插入的辅助方法，当哈希码相等且无法比较时使用。
         * 我们不需要总顺序，只需要一致的插入规则以保持在重新平衡期间的等效性。进一步的排序有助于简化测试。
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
            return d;
        }

        /**
         * 用给定的节点列表创建一个桶。
         */
        TreeBin(TreeNode<K,V> b) {
            super(TREEBIN, null, null, null); // 调用父类构造函数，标识为树形桶
            this.first = b;
            TreeNode<K,V> r = null;
            for (TreeNode<K,V> x = b, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = r;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);
                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root); // 检查红黑树的不变量
        }

        /**
         * 获取树结构的写锁，以进行树重构。
         */
        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                contendedLock(); // 如果获取失败，则调用 contendedLock 方法处理
        }

        /**
         * 释放树结构的写锁。
         */
        private final void unlockRoot() {
            lockState = 0; // 直接将锁状态置为 0，表示锁已释放
        }

        /**
         * 可能会阻塞，等待获取树结构的根锁。
         */
        private final void contendedLock() {
            boolean waiting = false;
            for (int s;;) {
                if (((s = lockState) & ~WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                } else if (waiting)
                    LockSupport.park(this); // 挂起当前线程，等待锁的释放
            }
        }

        /**
         * 返回与给定哈希码和键匹配的节点，或者返回 null（如果找不到）。
         * 尝试使用树结构比较进行查找，但在锁不可用时继续进行线性查找。
         */
        final Node<K,V> find(int h, Object k) {
            if (k != null) {
                for (Node<K,V> e = first; e != null; ) {
                    int s; K ek;
                    if (((s = lockState) & (WAITER|WRITER)) != 0) {
                        if (e.hash == h &&
                                ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, s, s + READER)) {
                        TreeNode<K,V> r, p;
                        try {
                            p = ((r = root) == null ? null : r.findTreeNode(h, k, null));
                        } finally {
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                                    (READER|WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }

        /**
         * 插入或查找树形节点。
         * @return 如果插入新节点则返回 null；如果找到相同键的节点则返回该节点
         */
        final TreeNode<K,V> putTreeVal(int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if (p == null) {
                    first = root = new TreeNode<K,V>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.findTreeNode(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<K,V> x, f = first;
                    first = x = new TreeNode<K,V>(h, k, v, f, xp);
                    if (f != null)
                        f.prev = x;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    if (!xp.red)
                        x.red = true;
                    else {
                        lockRoot(); // 获取写锁进行树结构平衡
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot(); // 释放锁
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root); // 检查红黑树的不变量
            return null;
        }

        /**
         * 删除给定的节点，必须在调用此方法之前确保节点已存在。
         * 这个删除过程比通常的红黑树删除代码更复杂，因为我们不能直接交换内部节点的内容与叶节点的内容。
         * @return 如果节点树太小而需要取消树化，则返回 true
         */
        final boolean removeTreeNode(TreeNode<K,V> p) {
            TreeNode<K,V> next = (TreeNode<K,V>)p.next;
            TreeNode<K,V> pred = p.prev;  // 取消链接遍历指针
            TreeNode<K,V> r, rl;
            if (pred == null)
                first = next;
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null || // 树太小
                    (rl = r.left) == null || rl.left == null)
                return true;
            lockRoot(); // 获取写锁，进行树节点删除
            try {
                TreeNode<K,V> replacement;
                TreeNode<K,V> pl = p.left;
                TreeNode<K,V> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<K,V> s = pr, sl;
                    while ((sl = s.left) != null) // 查找后继节点
                        s = sl;
                    boolean c = s.red; s.red = p.red; p.red = c; // 交换颜色
                    TreeNode<K,V> sr = s.right;
                    TreeNode<K,V> pp = p.parent;
                    if (s == pr) { // 如果 p 是 s 的直接父节点
                        p.parent = s;
                        s.right = p;
                    } else {
                        TreeNode<K,V> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left)
                                sp.left = p;
                            else
                                sp.right = p;
                        }
                        if ((s.right = pr) != null)
                            pr.parent = s;
                    }
                    p.left = null;
                    if ((p.right = sr) != null)
                        sr.parent = p;
                    if ((s.left = pl) != null)
                        pl.parent = s;
                    if ((s.parent = pp) == null)
                        r = s;
                    else if (p == pp.left)
                        pp.left = s;
                    else
                        pp.right = s;
                    if (sr != null)
                        replacement = sr;
                    else
                        replacement = p;
                } else if (pl != null)
                    replacement = pl;
                else if (pr != null)
                    replacement = pr;
                else
                    replacement = p;
                if (replacement != p) {
                    TreeNode<K,V> pp = replacement.parent = p.parent;
                    if (pp == null)
                        r = replacement;
                    else if (p == pp.left)
                        pp.left = replacement;
                    else
                        pp.right = replacement;
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {  // 断开指针链接
                    TreeNode<K,V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left)
                            pp.left = null;
                        else if (p == pp.right)
                            pp.right = null;
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot(); // 删除完成后释放锁
            }
            assert checkInvariants(root); // 检查红黑树的不变量
            return false;
        }

        /* ------------------------------------------------------------ */
        // 红黑树相关方法，所有的代码改编自《算法导论》中的红黑树实现。

        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root, TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root, TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root, TreeNode<K,V> x) {
            x.red = true;
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root, TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // 对称操作
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * 递归地检查红黑树的不变量
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;
        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TreeBin.class;
                LOCKSTATE = U.objectFieldOffset(k.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- 表遍历支持 -------------- */

    /**
     * 用于记录表的长度及当前遍历索引，用于必须在处理当前表之前处理转发表的遍历器。
     */
    static final class TableStack<K,V> {
        int length;             // 表的长度
        int index;              // 当前遍历的索引
        Node<K,V>[] tab;        // 当前表的引用
        TableStack<K,V> next;   // 下一个 TableStack 节点
    }

    /**
     * 封装遍历操作，供例如 containsValue 方法使用；也作为其他迭代器和分割迭代器的基类。
     *
     * 方法 advance 一次性访问在构造迭代器时可访问的每个仍然有效的节点。它可能会错过在访问一个桶之后添加到该桶中的某些节点，这对于一致性保证来说是可以接受的。
     * 在存在可能的表调整大小的情况下维护这一特性需要大量的状态记录操作，以应对 volatile 访问难以优化的情况。即使如此，遍历的吞吐量仍能保持在合理范围内。
     *
     * 通常，迭代逐个桶遍历列表。然而，如果表已调整大小，则所有未来的步骤都必须遍历当前索引的桶以及 (index + baseSize) 的桶；在进一步的调整大小操作中也是如此。
     * 为了在迭代器可能跨线程共享的情况下保持一致性，迭代在边界检查失败时终止。
     */
    static class Traverser<K,V> {
        Node<K,V>[] tab;        // 当前表；如果调整大小则更新
        Node<K,V> next;         // 下一个要使用的条目
        TableStack<K,V> stack, spare; // 用于在遇到 ForwardingNode 时保存/恢复状态
        int index;              // 下一个要使用的桶索引
        int baseIndex;          // 初始表的当前索引
        int baseLimit;          // 初始表的索引边界
        final int baseSize;     // 初始表的大小

        Traverser(Node<K,V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        /**
         * 如果可能的话，向前推进，返回下一个有效节点，否则返回 null。
         */
        final Node<K,V> advance() {
            Node<K,V> e;
            if ((e = next) != null)
                e = e.next;
            for (;;) {
                Node<K,V>[] t; int i, n;  // 必须使用局部变量进行检查
                if (e != null)
                    return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0)
                    return next = null;
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    } else if (e instanceof TreeBin)
                        e = ((TreeBin<K,V>)e).first;
                    else
                        e = null;
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex; // 如果存在，则访问上层插槽
            }
        }

        /**
         * 遇到 ForwardingNode 时保存遍历状态。
         */
        private void pushState(Node<K,V>[] t, int i, int n) {
            TableStack<K,V> s = spare;  // 尽可能复用
            if (s != null)
                spare = s.next;
            else
                s = new TableStack<K,V>();
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        /**
         * 可能弹出遍历状态。
         *
         * @param n 当前表的长度
         */
        private void recoverState(int n) {
            TableStack<K,V> s; int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K,V> next = s.next;
                s.next = spare; // 保存以便复用
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }

    /**
     * 键、值和条目迭代器的基类。添加了字段到 Traverser 以支持 iterator.remove 方法。
     */
    static class BaseIterator<K,V> extends Traverser<K,V> {
        final ConcurrentHashMap<K,V> map;
        Node<K,V> lastReturned;

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param size 哈希表的大小
         * @param index 起始索引
         * @param limit 结束索引的限制
         * @param map 关联的 ConcurrentHashMap
         */
        BaseIterator(Node<K,V>[] tab, int size, int index, int limit,
                     ConcurrentHashMap<K,V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();  // 初始化时推进到下一个元素
        }

        /**
         * 检查是否有下一个元素
         *
         * @return 如果有下一个元素，返回 true
         */
        public final boolean hasNext() { return next != null; }

        /**
         * 检查是否有更多元素，等同于 hasNext
         *
         * @return 如果有更多元素，返回 true
         */
        public final boolean hasMoreElements() { return next != null; }

        /**
         * 移除当前返回的节点
         *
         * @throws IllegalStateException 如果没有上一个返回的元素
         */
        public final void remove() {
            Node<K,V> p;
            if ((p = lastReturned) == null)
                throw new IllegalStateException();  // 如果没有上一个返回的元素，抛出异常
            lastReturned = null;
            map.replaceNode(p.key, null, null);  // 替换节点（逻辑上的移除操作）
        }
    }

    /**
     * 键迭代器类，继承自 BaseIterator，实现了 Iterator 和 Enumeration 接口
     */
    static final class KeyIterator<K,V> extends BaseIterator<K,V>
            implements Iterator<K>, Enumeration<K> {

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param index 起始索引
         * @param size 哈希表的大小
         * @param limit 结束索引的限制
         * @param map 关联的 ConcurrentHashMap
         */
        KeyIterator(Node<K,V>[] tab, int index, int size, int limit,
                    ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        /**
         * 返回下一个键
         *
         * @return 下一个键
         * @throws NoSuchElementException 如果没有更多元素
         */
        public final K next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();  // 如果没有更多元素，抛出异常
            K k = p.key;
            lastReturned = p;
            advance();  // 推进到下一个元素
            return k;
        }

        /**
         * 返回下一个元素，等同于 next 方法
         *
         * @return 下一个键
         */
        public final K nextElement() { return next(); }
    }

    /**
     * 值迭代器类，继承自 BaseIterator，实现了 Iterator 和 Enumeration 接口
     */
    static final class ValueIterator<K,V> extends BaseIterator<K,V>
            implements Iterator<V>, Enumeration<V> {

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param index 起始索引
         * @param size 哈希表的大小
         * @param limit 结束索引的限制
         * @param map 关联的 ConcurrentHashMap
         */
        ValueIterator(Node<K,V>[] tab, int index, int size, int limit,
                      ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        /**
         * 返回下一个值
         *
         * @return 下一个值
         * @throws NoSuchElementException 如果没有更多元素
         */
        public final V next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();  // 如果没有更多元素，抛出异常
            V v = p.val;
            lastReturned = p;
            advance();  // 推进到下一个元素
            return v;
        }

        /**
         * 返回下一个元素，等同于 next 方法
         *
         * @return 下一个值
         */
        public final V nextElement() { return next(); }
    }

    /**
     * 条目迭代器类，继承自 BaseIterator，实现了 Iterator 接口
     */
    static final class EntryIterator<K,V> extends BaseIterator<K,V>
            implements Iterator<Map.Entry<K,V>> {

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param index 起始索引
         * @param size 哈希表的大小
         * @param limit 结束索引的限制
         * @param map 关联的 ConcurrentHashMap
         */
        EntryIterator(Node<K,V>[] tab, int index, int size, int limit,
                      ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        /**
         * 返回下一个条目
         *
         * @return 下一个 Map.Entry 对象
         * @throws NoSuchElementException 如果没有更多元素
         */
        public final Map.Entry<K,V> next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();  // 如果没有更多元素，抛出异常
            K k = p.key;
            V v = p.val;
            lastReturned = p;
            advance();  // 推进到下一个元素
            return new MapEntry<K,V>(k, v, map);
        }
    }

    /**
     * MapEntry 类：导出 EntryIterator 的条目
     */
    static final class MapEntry<K,V> implements Map.Entry<K,V> {
        final K key; // 非空键
        V val;       // 非空值
        final ConcurrentHashMap<K,V> map;

        /**
         * 构造函数
         *
         * @param key 键
         * @param val 值
         * @param map 关联的 ConcurrentHashMap
         */
        MapEntry(K key, V val, ConcurrentHashMap<K,V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        /**
         * 返回键
         *
         * @return 键
         */
        public K getKey() { return key; }

        /**
         * 返回值
         *
         * @return 值
         */
        public V getValue() { return val; }

        /**
         * 返回哈希码
         *
         * @return 键和值的异或哈希码
         */
        public int hashCode() { return key.hashCode() ^ val.hashCode(); }

        /**
         * 返回键值对的字符串表示
         *
         * @return 键值对的字符串
         */
        public String toString() { return key + "=" + val; }

        /**
         * 比较两个条目是否相等
         *
         * @param o 待比较的对象
         * @return 如果相等则返回 true
         */
        public boolean equals(Object o) {
            Object k, v; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == val || v.equals(val)));
        }

        /**
         * 设置条目的值并将其写入到 map 中
         * 返回值具有一定的随意性。由于我们不一定追踪异步的变化，
         * 所以最 “最近”的先前值可能与我们返回的不同（甚至可能已被删除，
         * 在这种情况下 put 将重新建立该值）。我们不能保证更多。
         *
         * @param value 新值
         * @return 先前的值
         * @throws NullPointerException 如果新值为 null
         */
        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = val;
            val = value;
            map.put(key, value);
            return v;
        }
    }

    /**
     * KeySpliterator 类，继承自 Traverser，实现了 Spliterator 接口
     */
    static final class KeySpliterator<K,V> extends Traverser<K,V>
            implements Spliterator<K> {
        long est;  // 大小估计值

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param size 哈希表的大小
         * @param index 起始索引
         * @param limit 结束索引的限制
         * @param est 初始大小估计值
         */
        KeySpliterator(Node<K,V>[] tab, int size, int index, int limit,
                       long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        /**
         * 尝试拆分此 Spliterator，返回一个新的 Spliterator
         *
         * @return 如果无法拆分，则返回 null，否则返回新的 Spliterator
         */
        public Spliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new KeySpliterator<K,V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        /**
         * 对每个剩余的键执行给定的动作
         *
         * @param action 要对每个键执行的动作
         * @throws NullPointerException 如果 action 为 null
         */
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null;)
                action.accept(p.key);
        }

        /**
         * 如果存在下一元素，执行给定的动作并返回 true，否则返回 false
         *
         * @param action 要对键执行的动作
         * @return 如果有下一元素，返回 true
         * @throws NullPointerException 如果 action 为 null
         */
        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.key);
            return true;
        }

        /**
         * 返回估计的大小
         *
         * @return 大小估计值
         */
        public long estimateSize() { return est; }

        /**
         * 返回此 Spliterator 的特性值
         *
         * @return Spliterator 的特性值
         */
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }

    /**
     * ValueSpliterator 类，继承自 Traverser，实现了 Spliterator 接口
     */
    static final class ValueSpliterator<K,V> extends Traverser<K,V>
            implements Spliterator<V> {
        long est;  // 大小估计值

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param size 哈希表的大小
         * @param index 起始索引
         * @param limit 结束索引的限制
         * @param est 初始大小估计值
         */
        ValueSpliterator(Node<K,V>[] tab, int size, int index, int limit,
                         long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        /**
         * 尝试拆分此 Spliterator，返回一个新的 Spliterator
         *
         * @return 如果无法拆分，则返回 null，否则返回新的 Spliterator
         */
        public Spliterator<V> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new ValueSpliterator<K,V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        /**
         * 对每个剩余的值执行给定的动作
         *
         * @param action 要对每个值执行的动作
         * @throws NullPointerException 如果 action 为 null
         */
        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null;)
                action.accept(p.val);
        }

        /**
         * 如果存在下一元素，执行给定的动作并返回 true，否则返回 false
         *
         * @param action 要对值执行的动作
         * @return 如果有下一元素，返回 true
         * @throws NullPointerException 如果 action 为 null
         */
        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.val);
            return true;
        }

        /**
         * 返回估计的大小
         *
         * @return 大小估计值
         */
        public long estimateSize() { return est; }

        /**
         * 返回此 Spliterator 的特性值
         *
         * @return Spliterator 的特性值
         */
        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL;
        }
    }

    /**
     * EntrySpliterator 类，继承自 Traverser，实现了 Spliterator 接口
     */
    static final class EntrySpliterator<K,V> extends Traverser<K,V>
            implements Spliterator<Map.Entry<K,V>> {
        final ConcurrentHashMap<K,V> map;  // 用于导出 MapEntry
        long est;  // 大小估计值

        /**
         * 构造函数
         *
         * @param tab 哈希表数组
         * @param size 哈希表的大小
         * @param index 起始索引
         * @param limit 结束索引的限制
         * @param est 初始大小估计值
         * @param map 关联的 ConcurrentHashMap
         */
        EntrySpliterator(Node<K,V>[] tab, int size, int index, int limit,
                         long est, ConcurrentHashMap<K,V> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        /**
         * 尝试拆分此 Spliterator，返回一个新的 Spliterator
         *
         * @return 如果无法拆分，则返回 null，否则返回新的 Spliterator
         */
        public Spliterator<Map.Entry<K,V>> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new EntrySpliterator<K,V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1, map);
        }

        /**
         * 对每个剩余的条目执行给定的动作
         *
         * @param action 要对每个条目执行的动作
         * @throws NullPointerException 如果 action 为 null
         */
        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null; )
                action.accept(new MapEntry<K,V>(p.key, p.val, map));
        }

        /**
         * 如果存在下一元素，执行给定的动作并返回 true，否则返回 false
         *
         * @param action 要对条目执行的动作
         * @return 如果有下一元素，返回 true
         * @throws NullPointerException 如果 action 为 null
         */
        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(new MapEntry<K,V>(p.key, p.val, map));
            return true;
        }

        /**
         * 返回估计的大小
         *
         * @return 大小估计值
         */
        public long estimateSize() { return est; }

        /**
         * 返回此 Spliterator 的特性值
         *
         * @return Spliterator 的特性值
         */
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }

    // 并行批量操作

    /**
     * 计算用于批量任务的初始批量值。返回的值大约是
     * exp2（任务分成两部分的次数减去 1）。这个值比深度更快计算，
     * 并且在划分时更方便用作分割的指导，因为它本质上已经在做二分操作。
     *
     * @param b 批量大小的上限
     * @return 计算后的批量大小
     */
    final int batchFor(long b) {
        long n;
        if (b == Long.MAX_VALUE || (n = sumCount()) <= 1L || n < b)
            return 0;
        int sp = ForkJoinPool.getCommonPoolParallelism() << 2; // slack 为 4
        return (b <= 0L || (n /= b) >= sp) ? sp : (int)n;
    }

    /**
     * 对每个 (key, value) 对执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param action 要执行的操作
     * @since 1.8
     */
    public void forEach(long parallelismThreshold,
                        BiConsumer<? super K,? super V> action) {
        if (action == null) throw new NullPointerException();
        new ForEachMappingTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }

    /**
     * 对每个 (key, value) 对的非空转换结果执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回元素的转换结果，或者 null（如果没有转换）
     * @param action 要执行的操作
     * @param <U> 转换结果的类型
     * @since 1.8
     */
    public <U> void forEach(long parallelismThreshold,
                            BiFunction<? super K, ? super V, ? extends U> transformer,
                            Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedMappingTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }

    /**
     * 使用给定的搜索函数返回非空结果（如果存在），或者返回 null（如果没有找到）。
     * 一旦找到成功结果，后续的元素处理将被忽略。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param searchFunction 一个函数，返回成功时的非空结果，否则返回 null
     * @param <U> 搜索函数的返回类型
     * @return 成功时搜索函数返回的非空结果，或者 null（如果没有找到）
     * @since 1.8
     */
    public <U> U search(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchMappingsTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * 使用给定的转换器对所有 (key, value) 对进行累积操作，并使用给定的规约函数
     * 组合结果，返回累积后的值，或者返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回元素的转换结果，或者 null（如果没有转换）
     * @param reducer 一个交换律和结合律的规约函数
     * @param <U> 转换结果的类型
     * @return 累积后的结果
     * @since 1.8
     */
    public <U> U reduce(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> transformer,
                        BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有 (key, value) 对进行累积操作，使用给定的规约函数
     * 组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回元素的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public double reduceToDouble(long parallelismThreshold,
                                 ToDoubleBiFunction<? super K, ? super V> transformer,
                                 double basis,
                                 DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToDoubleTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有 (key, value) 对进行累积操作，使用给定的规约函数
     * 组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回元素的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public long reduceToLong(long parallelismThreshold,
                             ToLongBiFunction<? super K, ? super V> transformer,
                             long basis,
                             LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToLongTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有 (key, value) 对进行累积操作，使用给定的规约函数
     * 组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回元素的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public int reduceToInt(long parallelismThreshold,
                           ToIntBiFunction<? super K, ? super V> transformer,
                           int basis,
                           IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToIntTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 对每个键执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param action 要执行的操作
     * @since 1.8
     */
    public void forEachKey(long parallelismThreshold,
                           Consumer<? super K> action) {
        if (action == null) throw new NullPointerException();
        new ForEachKeyTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }

    /**
     * 对每个键的非空转换结果执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回键的转换结果，或者 null（如果没有转换）
     * @param action 要执行的操作
     * @param <U> 转换结果的类型
     * @since 1.8
     */
    public <U> void forEachKey(long parallelismThreshold,
                               Function<? super K, ? extends U> transformer,
                               Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedKeyTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }

    /**
     * 使用给定的搜索函数对每个键进行搜索，返回非空结果，或返回 null（如果没有找到）。
     * 一旦找到成功结果，后续的元素处理将被忽略。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param searchFunction 一个函数，返回成功时的非空结果，否则返回 null
     * @param <U> 搜索函数的返回类型
     * @return 成功时搜索函数返回的非空结果，或者 null（如果没有找到）
     * @since 1.8
     */
    public <U> U searchKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchKeysTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * 使用给定的规约函数对所有键进行累积，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public K reduceKeys(long parallelismThreshold,
                        BiFunction<? super K, ? super K, ? extends K> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceKeysTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有键进行累积，使用给定的规约函数组合结果，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回键的转换结果，或者 null（如果没有转换）
     * @param reducer 一个交换律和结合律的规约函数
     * @param <U> 转换结果的类型
     * @return 累积后的结果
     * @since 1.8
     */
    public <U> U reduceKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> transformer,
                            BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有键进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回键的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public double reduceKeysToDouble(long parallelismThreshold,
                                     ToDoubleFunction<? super K> transformer,
                                     double basis,
                                     DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToDoubleTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有键进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回键的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public long reduceKeysToLong(long parallelismThreshold,
                                 ToLongFunction<? super K> transformer,
                                 long basis,
                                 LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToLongTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有键进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回键的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public int reduceKeysToInt(long parallelismThreshold,
                               ToIntFunction<? super K> transformer,
                               int basis,
                               IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToIntTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 对每个值执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param action 要执行的操作
     * @since 1.8
     */
    public void forEachValue(long parallelismThreshold,
                             Consumer<? super V> action) {
        if (action == null)
            throw new NullPointerException();
        new ForEachValueTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }

    /**
     * 对每个值的非空转换结果执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回值的转换结果，或者 null（如果没有转换）
     * @param action 要执行的操作
     * @param <U> 转换结果的类型
     * @since 1.8
     */
    public <U> void forEachValue(long parallelismThreshold,
                                 Function<? super V, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedValueTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }

    /**
     * 使用给定的搜索函数对每个值进行搜索，返回非空结果，或返回 null（如果没有找到）。
     * 一旦找到成功结果，后续的元素处理将被忽略。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param searchFunction 一个函数，返回成功时的非空结果，否则返回 null
     * @param <U> 搜索函数的返回类型
     * @return 成功时搜索函数返回的非空结果，或者 null（如果没有找到）
     * @since 1.8
     */
    public <U> U searchValues(long parallelismThreshold,
                              Function<? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchValuesTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * 使用给定的规约函数对所有值进行累积，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public V reduceValues(long parallelismThreshold,
                          BiFunction<? super V, ? super V, ? extends V> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceValuesTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有值进行累积，使用给定的规约函数组合结果，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回值的转换结果，或者 null（如果没有转换）
     * @param reducer 一个交换律和结合律的规约函数
     * @param <U> 转换结果的类型
     * @return 累积后的结果
     * @since 1.8
     */
    public <U> U reduceValues(long parallelismThreshold,
                              Function<? super V, ? extends U> transformer,
                              BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有值进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回值的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public double reduceValuesToDouble(long parallelismThreshold,
                                       ToDoubleFunction<? super V> transformer,
                                       double basis,
                                       DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToDoubleTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有值进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回值的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public long reduceValuesToLong(long parallelismThreshold,
                                   ToLongFunction<? super V> transformer,
                                   long basis,
                                   LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToLongTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有值进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回值的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public int reduceValuesToInt(long parallelismThreshold,
                                 ToIntFunction<? super V> transformer,
                                 int basis,
                                 IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToIntTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 对每个条目执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param action 要执行的操作
     * @since 1.8
     */
    public void forEachEntry(long parallelismThreshold,
                             Consumer<? super Map.Entry<K,V>> action) {
        if (action == null) throw new NullPointerException();
        new ForEachEntryTask<K,V>(null, batchFor(parallelismThreshold), 0, 0, table,
                action).invoke();
    }

    /**
     * 对每个条目的非空转换结果执行给定的操作。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回条目的转换结果，或者 null（如果没有转换）
     * @param action 要执行的操作
     * @param <U> 转换结果的类型
     * @since 1.8
     */
    public <U> void forEachEntry(long parallelismThreshold,
                                 Function<Map.Entry<K,V>, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedEntryTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }

    /**
     * 使用给定的搜索函数对每个条目进行搜索，返回非空结果，或返回 null（如果没有找到）。
     * 一旦找到成功结果，后续的元素处理将被忽略。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param searchFunction 一个函数，返回成功时的非空结果，否则返回 null
     * @param <U> 搜索函数的返回类型
     * @return 成功时搜索函数返回的非空结果，或者 null（如果没有找到）
     * @since 1.8
     */
    public <U> U searchEntries(long parallelismThreshold,
                               Function<Map.Entry<K,V>, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchEntriesTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * 使用给定的规约函数对所有条目进行累积，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public Map.Entry<K,V> reduceEntries(long parallelismThreshold,
                                        BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceEntriesTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有条目进行累积，使用给定的规约函数组合结果，返回累积后的结果，或返回 null（如果没有累积结果）。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回条目的转换结果，或者 null（如果没有转换）
     * @param reducer 一个交换律和结合律的规约函数
     * @param <U> 转换结果的类型
     * @return 累积后的结果
     * @since 1.8
     */
    public <U> U reduceEntries(long parallelismThreshold,
                               Function<Map.Entry<K,V>, ? extends U> transformer,
                               BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesTask<K,V,U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有条目进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回条目的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public double reduceEntriesToDouble(long parallelismThreshold,
                                        ToDoubleFunction<Map.Entry<K,V>> transformer,
                                        double basis,
                                        DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToDoubleTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有条目进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回条目的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public long reduceEntriesToLong(long parallelismThreshold,
                                    ToLongFunction<Map.Entry<K,V>> transformer,
                                    long basis,
                                    LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToLongTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /**
     * 使用给定的转换器对所有条目进行累积，使用给定的规约函数组合结果，并将给定的基础作为身份值。
     *
     * @param parallelismThreshold 需要并行执行的元素数量阈值
     * @param transformer 一个函数，返回条目的转换结果
     * @param basis 累积的初始值
     * @param reducer 一个交换律和结合律的规约函数
     * @return 累积后的结果
     * @since 1.8
     */
    public int reduceEntriesToInt(long parallelismThreshold,
                                  ToIntFunction<Map.Entry<K,V>> transformer,
                                  int basis,
                                  IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToIntTask<K,V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }

    /* ----------------视图---------------- */

    /**
     * 视图的基类。
     */
    abstract static class CollectionView<K,V,E>
            implements Collection<E>, java.io.Serializable {

        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMap<K,V> map;

        CollectionView(ConcurrentHashMap<K,V> map)  {
            this.map = map;
        }

        /**
         * 返回该视图支持的映射表。
         *
         * @return 支持该视图的映射表
         */
        public ConcurrentHashMap<K,V> getMap() {
            return map;
        }

        /**
         * 通过删除该视图支持的映射表中的所有映射，移除所有元素。
         */
        public final void clear() {
            map.clear();
        }

        public final int size() {
            return map.size();
        }

        public final boolean isEmpty() {
            return map.isEmpty();
        }

        // 下面的实现依赖具体类提供的这些抽象方法

        /**
         * 返回该集合中元素的迭代器。
         *
         * <p>返回的迭代器是
         * <a href="package-summary.html#Weakly"><i>弱一致的</i></a>。
         *
         * @return 该集合中元素的迭代器
         */
        public abstract Iterator<E> iterator();
        public abstract boolean contains(Object o);
        public abstract boolean remove(Object o);

        private static final String oomeMsg = "所需的数组大小过大";

        public final Object[] toArray() {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int n = (int)sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int m = (int)sz;
            T[] r = (a.length >= m) ? a :
                    (T[])java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T)e;
            }
            if (a == r && i < n) {
                r[i] = null; // 用null终止
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        /**
         * 返回该集合的字符串表示形式。
         * 字符串表示形式由集合元素的字符串表示组成，元素按迭代器的顺序返回，
         * 并且包含在方括号 ({@code "[]"}) 中。相邻元素由字符{@code ", "}
         * (逗号和空格)分隔。元素会通过{@link String#valueOf(Object)}转为字符串。
         *
         * @return 该集合的字符串表示形式
         */
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (;;) {
                    Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext())
                        break;
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e))
                        return false;
                }
            }
            return true;
        }

        public final boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public final boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    /**
     * 以{@link Set}的形式查看ConcurrentHashMap的键，
     * 在这种视图中，可以选择通过映射到公共值来启用添加。
     * 该类不能直接实例化。
     * 参见{@link #keySet() keySet()},
     * {@link #keySet(Object) keySet(V)},
     * {@link #newKeySet() newKeySet()},
     * {@link #newKeySet(int) newKeySet(int)}。
     *
     * @since 1.8
     */
    public static class KeySetView<K,V> extends CollectionView<K,V,K>
            implements Set<K>, java.io.Serializable {

        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        KeySetView(ConcurrentHashMap<K,V> map, V value) {  // 非公共
            super(map);
            this.value = value;
        }

        /**
         * 返回添加时映射的默认值，若不支持添加则返回{@code null}。
         *
         * @return 添加时映射的默认值，若不支持添加则返回{@code null}
         */
        public V getMappedValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         * @throws NullPointerException 如果指定的键为null
         */
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        /**
         * 从此映射视图中移除键，通过从支持的映射表中移除该键（及其对应的值）。
         * 如果键不在映射表中，此方法不执行任何操作。
         *
         * @param  o 要从支持的映射表中移除的键
         * @return {@code true} 如果支持的映射表包含指定的键
         * @throws NullPointerException 如果指定的键为null
         */
        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        /**
         * @return 支持映射表的键的迭代器
         */
        public Iterator<K> iterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeyIterator<K,V>(t, f, 0, f, m);
        }

        /**
         * 通过将键映射到支持映射表中的默认映射值来将指定的键添加到此集合视图中（如果定义了该值）。
         *
         * @param e 要添加的键
         * @return {@code true} 如果此集合因调用而改变
         * @throws NullPointerException 如果指定的键为null
         * @throws UnsupportedOperationException 如果没有为添加提供默认映射值
         */
        public boolean add(K e) {
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        /**
         * 将指定集合中的所有元素添加到此集合中，
         * 就像通过调用{@link #add}来添加每个元素一样。
         *
         * @param c 要插入到此集合中的元素
         * @return {@code true} 如果此集合因调用而改变
         * @throws NullPointerException 如果集合或其中的任何元素为{@code null}
         * @throws UnsupportedOperationException 如果没有为添加提供默认映射值
         */
        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            for (K e : c) {
                if (map.putVal(e, v, true) == null)
                    added = true;
            }
            return added;
        }

        public int hashCode() {
            int h = 0;
            for (K e : this)
                h += e.hashCode();
            return h;
        }

        public boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<K> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.key);
            }
        }
    }

    /**
     * 以{@link Collection}的形式查看ConcurrentHashMap的值，其中禁止添加。
     * 该类不能直接实例化。参见{@link #values()}。
     */
    static final class ValuesView<K,V> extends CollectionView<K,V,V>
            implements Collection<V>, java.io.Serializable {

        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        public final boolean contains(Object o) {
            return map.containsValue(o);
        }

        public final boolean remove(Object o) {
            if (o != null) {
                for (Iterator<V> it = iterator(); it.hasNext();) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public final Iterator<V> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueIterator<K,V>(t, f, 0, f, m);
        }

        public final boolean add(V e) {
            throw new UnsupportedOperationException();
        }

        public final boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        public Spliterator<V> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueSpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.val);
            }
        }
    }

    /**
     * 以{@link Set}形式查看ConcurrentHashMap中的（键，值）条目。
     * 该类不能直接实例化。参见{@link #entrySet()}。
     */
    static final class EntrySetView<K,V> extends CollectionView<K,V,Map.Entry<K,V>>
            implements Set<Map.Entry<K,V>>, java.io.Serializable {

        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        public boolean contains(Object o) {
            Object k, v, r;
            Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (r = map.get(k)) != null &&
                    (v = e.getValue()) != null &&
                    (v == r || v.equals(r)));
        }

        public boolean remove(Object o) {
            Object k, v;
            Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    map.remove(k, v));
        }

        /**
         * @return 支持映射表中条目的迭代器
         */
        public Iterator<Map.Entry<K,V>> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntryIterator<K,V>(t, f, 0, f, m);
        }

        public boolean add(Entry<K,V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(Collection<? extends Entry<K,V>> c) {
            boolean added = false;
            for (Entry<K,V> e : c) {
                if (add(e))
                    added = true;
            }
            return added;
        }

        public final int hashCode() {
            int h = 0;
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        public final boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<Map.Entry<K,V>> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntrySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n, m);
        }

        public void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(new MapEntry<K,V>(p.key, p.val, map));
            }
        }
    }

    // -------------------------------------------------------

    /**
     * 批量任务的基类。重复了某些字段和代码
     * 来自类 Traverser，因为我们需要继承 CountedCompleter。
     */
    @SuppressWarnings("serial")
    abstract static class BulkTask<K,V,R> extends CountedCompleter<R> {
        Node<K,V>[] tab;        // 与 Traverser 相同
        Node<K,V> next;
        TableStack<K,V> stack, spare;
        int index;
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int batch;              // 分割控制

        BulkTask(BulkTask<K,V,?> par, int b, int i, int f, Node<K,V>[] t) {
            super(par);
            this.batch = b;
            this.index = this.baseIndex = i;
            if ((this.tab = t) == null)
                this.baseSize = this.baseLimit = 0;
            else if (par == null)
                this.baseSize = this.baseLimit = t.length;
            else {
                this.baseLimit = f;
                this.baseSize = par.baseSize;
            }
        }

        /**
         * 与 Traverser 版本相同
         */
        final Node<K,V> advance() {
            Node<K,V> e;
            if ((e = next) != null)
                e = e.next;
            for (;;) {
                Node<K,V>[] t; int i, n;
                if (e != null)
                    return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0)
                    return next = null;
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    }
                    else if (e instanceof TreeBin)
                        e = ((TreeBin<K,V>)e).first;
                    else
                        e = null;
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex;
            }
        }

        private void pushState(Node<K,V>[] t, int i, int n) {
            TableStack<K,V> s = spare;
            if (s != null)
                spare = s.next;
            else
                s = new TableStack<K,V>();
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        private void recoverState(int n) {
            TableStack<K,V> s; int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K,V> next = s.next;
                s.next = spare; // 保存以供重用
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }

    /*
     * 任务类。以一种常规但丑陋的格式/样式编写，
     * 以简化检查每个变体与其他变体的正确不同之处。
     * 空检查的存在是因为编译器无法知道我们已经对任务参数进行了空检查，
     * 所以我们强制最简单的提升以避免复杂的陷阱。
     */
    @SuppressWarnings("serial")
    static final class ForEachKeyTask<K,V>
            extends BulkTask<K,V,Void> {
        final Consumer<? super K> action;
        ForEachKeyTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Consumer<? super K> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super K> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachKeyTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null;)
                    action.accept(p.key);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachValueTask<K,V>
            extends BulkTask<K,V,Void> {
        final Consumer<? super V> action;
        ForEachValueTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Consumer<? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachValueTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null;)
                    action.accept(p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachEntryTask<K,V>
            extends BulkTask<K,V,Void> {
        final Consumer<? super Entry<K,V>> action;
        ForEachEntryTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Consumer<? super Entry<K,V>> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super Entry<K,V>> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachEntryTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    action.accept(p);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachMappingTask<K,V>
            extends BulkTask<K,V,Void> {
        final BiConsumer<? super K, ? super V> action;
        ForEachMappingTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 BiConsumer<? super K,? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final BiConsumer<? super K, ? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachMappingTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    action.accept(p.key, p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedKeyTask<K,V,U>
            extends BulkTask<K,V,Void> {
        final Function<? super K, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedKeyTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedKeyTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedValueTask<K,V,U>
            extends BulkTask<K,V,Void> {
        final Function<? super V, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedValueTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedValueTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedEntryTask<K,V,U>
            extends BulkTask<K,V,Void> {
        final Function<Map.Entry<K,V>, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedEntryTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<Map.Entry<K,V>, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<Map.Entry<K,V>, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedEntryTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedMappingTask<K,V,U>
            extends BulkTask<K,V,Void> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedMappingTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 BiFunction<? super K, ? super V, ? extends U> transformer,
                 Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedMappingTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }
    @SuppressWarnings("serial")
    static final class SearchKeysTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<? super K, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchKeysTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<? super K, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<? super K, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchKeysTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchValuesTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchValuesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<? super V, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchValuesTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchEntriesTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<Entry<K,V>, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchEntriesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 Function<Entry<K,V>, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<Entry<K,V>, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchEntriesTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchMappingsTask<K,V,U>
            extends BulkTask<K,V,U> {
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchMappingsTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 BiFunction<? super K, ? super V, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchMappingsTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key, p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceKeysTask<K,V>
            extends BulkTask<K,V,K> {
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        ReduceKeysTask<K,V> rights, nextRight;
        ReduceKeysTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 ReduceKeysTask<K,V> nextRight,
                 BiFunction<? super K, ? super K, ? extends K> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final K getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super K, ? super K, ? extends K> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceKeysTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                K r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    K u = p.key;
                    r = (r == null) ? u : u == null ? r : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceKeysTask<K,V>
                            t = (ReduceKeysTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        K tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceValuesTask<K,V>
            extends BulkTask<K,V,V> {
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        ReduceValuesTask<K,V> rights, nextRight;
        ReduceValuesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 ReduceValuesTask<K,V> nextRight,
                 BiFunction<? super V, ? super V, ? extends V> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final V getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super V, ? super V, ? extends V> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceValuesTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                V r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    V v = p.val;
                    r = (r == null) ? v : reducer.apply(r, v);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceValuesTask<K,V>
                            t = (ReduceValuesTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        V tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceEntriesTask<K,V>
            extends BulkTask<K,V,Map.Entry<K,V>> {
        final BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer;
        Map.Entry<K,V> result;
        ReduceEntriesTask<K,V> rights, nextRight;
        ReduceEntriesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 ReduceEntriesTask<K,V> nextRight,
                 BiFunction<Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final Map.Entry<K,V> getRawResult() { return result; }
        public final void compute() {
            final BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceEntriesTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                Map.Entry<K,V> r = null;
                for (Node<K,V> p; (p = advance()) != null; )
                    r = (r == null) ? p : reducer.apply(r, p);
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceEntriesTask<K,V>
                            t = (ReduceEntriesTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        Map.Entry<K,V> tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }
    @SuppressWarnings("serial")
    static final class MapReduceKeysTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<? super K, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceKeysTask<K,V,U> rights, nextRight;
        MapReduceKeysTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceKeysTask<K,V,U> nextRight,
                 Function<? super K, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysTask<K,V,U>
                            t = (MapReduceKeysTask<K,V,U>)c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceValuesTask<K,V,U> rights, nextRight;
        MapReduceValuesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceValuesTask<K,V,U> nextRight,
                 Function<? super V, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesTask<K,V,U>
                            t = (MapReduceValuesTask<K,V,U>)c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesTask<K,V,U>
            extends BulkTask<K,V,U> {
        final Function<Map.Entry<K,V>, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceEntriesTask<K,V,U> rights, nextRight;
        MapReduceEntriesTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceEntriesTask<K,V,U> nextRight,
                 Function<Map.Entry<K,V>, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<Map.Entry<K,V>, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesTask<K,V,U>
                            t = (MapReduceEntriesTask<K,V,U>)c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsTask<K,V,U>
            extends BulkTask<K,V,U> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceMappingsTask<K,V,U> rights, nextRight;
        MapReduceMappingsTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceMappingsTask<K,V,U> nextRight,
                 BiFunction<? super K, ? super V, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsTask<K,V,U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsTask<K,V,U>
                            t = (MapReduceMappingsTask<K,V,U>)c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToDoubleTask<K,V>
            extends BulkTask<K,V,Double> {
        final ToDoubleFunction<? super K> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceKeysToDoubleTask<K,V> rights, nextRight;
        MapReduceKeysToDoubleTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceKeysToDoubleTask<K,V> nextRight,
                 ToDoubleFunction<? super K> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<? super K> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToDoubleTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToDoubleTask<K,V>
                            t = (MapReduceKeysToDoubleTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }
    @SuppressWarnings("serial")
    static final class MapReduceValuesToDoubleTask<K,V>
            extends BulkTask<K,V,Double> {
        final ToDoubleFunction<? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceValuesToDoubleTask<K,V> rights, nextRight;
        MapReduceValuesToDoubleTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceValuesToDoubleTask<K,V> nextRight,
                 ToDoubleFunction<? super V> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToDoubleTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToDoubleTask<K,V>
                            t = (MapReduceValuesToDoubleTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToDoubleTask<K,V>
            extends BulkTask<K,V,Double> {
        final ToDoubleFunction<Map.Entry<K,V>> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceEntriesToDoubleTask<K,V> rights, nextRight;
        MapReduceEntriesToDoubleTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceEntriesToDoubleTask<K,V> nextRight,
                 ToDoubleFunction<Map.Entry<K,V>> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<Map.Entry<K,V>> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToDoubleTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToDoubleTask<K,V>
                            t = (MapReduceEntriesToDoubleTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToDoubleTask<K,V>
            extends BulkTask<K,V,Double> {
        final ToDoubleBiFunction<? super K, ? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceMappingsToDoubleTask<K,V> rights, nextRight;
        MapReduceMappingsToDoubleTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceMappingsToDoubleTask<K,V> nextRight,
                 ToDoubleBiFunction<? super K, ? super V> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleBiFunction<? super K, ? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToDoubleTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToDoubleTask<K,V>
                            t = (MapReduceMappingsToDoubleTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToLongTask<K,V>
            extends BulkTask<K,V,Long> {
        final ToLongFunction<? super K> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceKeysToLongTask<K,V> rights, nextRight;
        MapReduceKeysToLongTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceKeysToLongTask<K,V> nextRight,
                 ToLongFunction<? super K> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<? super K> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToLongTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToLongTask<K,V>
                            t = (MapReduceKeysToLongTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToLongTask<K,V>
            extends BulkTask<K,V,Long> {
        final ToLongFunction<? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceValuesToLongTask<K,V> rights, nextRight;
        MapReduceValuesToLongTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceValuesToLongTask<K,V> nextRight,
                 ToLongFunction<? super V> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToLongTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToLongTask<K,V>
                            t = (MapReduceValuesToLongTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToLongTask<K,V>
            extends BulkTask<K,V,Long> {
        final ToLongFunction<Map.Entry<K,V>> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceEntriesToLongTask<K,V> rights, nextRight;
        MapReduceEntriesToLongTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceEntriesToLongTask<K,V> nextRight,
                 ToLongFunction<Map.Entry<K,V>> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<Map.Entry<K,V>> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToLongTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToLongTask<K,V>
                            t = (MapReduceEntriesToLongTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToLongTask<K,V>
            extends BulkTask<K,V,Long> {
        final ToLongBiFunction<? super K, ? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceMappingsToLongTask<K,V> rights, nextRight;
        MapReduceMappingsToLongTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceMappingsToLongTask<K,V> nextRight,
                 ToLongBiFunction<? super K, ? super V> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongBiFunction<? super K, ? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToLongTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToLongTask<K,V>
                            t = (MapReduceMappingsToLongTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }
    @SuppressWarnings("serial")
    static final class MapReduceKeysToIntTask<K,V>
            extends BulkTask<K,V,Integer> {
        final ToIntFunction<? super K> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceKeysToIntTask<K,V> rights, nextRight;
        MapReduceKeysToIntTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceKeysToIntTask<K,V> nextRight,
                 ToIntFunction<? super K> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<? super K> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToIntTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToIntTask<K,V>
                            t = (MapReduceKeysToIntTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToIntTask<K,V>
            extends BulkTask<K,V,Integer> {
        final ToIntFunction<? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceValuesToIntTask<K,V> rights, nextRight;
        MapReduceValuesToIntTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceValuesToIntTask<K,V> nextRight,
                 ToIntFunction<? super V> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToIntTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToIntTask<K,V>
                            t = (MapReduceValuesToIntTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToIntTask<K,V>
            extends BulkTask<K,V,Integer> {
        final ToIntFunction<Map.Entry<K,V>> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceEntriesToIntTask<K,V> rights, nextRight;
        MapReduceEntriesToIntTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceEntriesToIntTask<K,V> nextRight,
                 ToIntFunction<Map.Entry<K,V>> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<Map.Entry<K,V>> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToIntTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToIntTask<K,V>
                            t = (MapReduceEntriesToIntTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToIntTask<K,V>
            extends BulkTask<K,V,Integer> {
        final ToIntBiFunction<? super K, ? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceMappingsToIntTask<K,V> rights, nextRight;
        MapReduceMappingsToIntTask
                (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
                 MapReduceMappingsToIntTask<K,V> nextRight,
                 ToIntBiFunction<? super K, ? super V> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer; this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntBiFunction<? super K, ? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToIntTask<K,V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToIntTask<K,V>
                            t = (MapReduceMappingsToIntTask<K,V>)c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    // 不安全的机制
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            // 获取Unsafe实例
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;

            // 获取ConcurrentHashMap类中“sizeCtl”字段的内存偏移量
            SIZECTL = U.objectFieldOffset
                    (k.getDeclaredField("sizeCtl"));

            // 获取ConcurrentHashMap类中“transferIndex”字段的内存偏移量
            TRANSFERINDEX = U.objectFieldOffset
                    (k.getDeclaredField("transferIndex"));

            // 获取ConcurrentHashMap类中“baseCount”字段的内存偏移量
            BASECOUNT = U.objectFieldOffset
                    (k.getDeclaredField("baseCount"));

            // 获取ConcurrentHashMap类中“cellsBusy”字段的内存偏移量
            CELLSBUSY = U.objectFieldOffset
                    (k.getDeclaredField("cellsBusy"));

            // 获取CounterCell类中“value”字段的内存偏移量
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                    (ck.getDeclaredField("value"));

            // 获取Node[]类中数组基地址的偏移量
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);

            // 获取数组索引缩放因子
            int scale = U.arrayIndexScale(ak);

            // 如果缩放因子不是2的幂，则抛出错误
            if ((scale & (scale - 1)) != 0)
                throw new Error("数据类型缩放因子不是2的幂");

            // 计算用于数组索引的移位值
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}

















