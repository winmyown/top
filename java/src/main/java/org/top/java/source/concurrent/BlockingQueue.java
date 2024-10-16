package org.top.java.source.concurrent;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 下午3:19
 */
/**
 * 一个 {@link java.util.Queue} 的扩展版本，支持在获取元素时等待队列变为非空，
 * 以及在存储元素时等待队列中出现可用空间的操作。
 *
 * <p>{@code BlockingQueue} 的方法有四种形式，用不同的方式处理不能立即满足的操作，
 * 但可能在未来某个时间可以满足：
 * 第一种抛出异常，第二种返回一个特殊值（根据操作返回 {@code null} 或 {@code false}），
 * 第三种阻塞当前线程直到操作可以成功，第四种只阻塞给定的最大时间限制，然后放弃。
 * 这些方法在下表中进行了总结：
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>BlockingQueue 方法摘要</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>抛出异常</em></td>
 *    <td ALIGN=CENTER><em>特殊值</em></td>
 *    <td ALIGN=CENTER><em>阻塞</em></td>
 *    <td ALIGN=CENTER><em>超时</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>插入</b></td>
 *    <td>{@link #add add(e)}</td>
 *    <td>{@link #offer offer(e)}</td>
 *    <td>{@link #put put(e)}</td>
 *    <td>{@link #offer(Object, long, TimeUnit) offer(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>移除</b></td>
 *    <td>{@link #remove remove()}</td>
 *    <td>{@link #poll poll()}</td>
 *    <td>{@link #take take()}</td>
 *    <td>{@link #poll(long, TimeUnit) poll(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>检查</b></td>
 *    <td>{@link #element element()}</td>
 *    <td>{@link #peek peek()}</td>
 *    <td><em>不适用</em></td>
 *    <td><em>不适用</em></td>
 *  </tr>
 * </table>
 *
 * <p>{@code BlockingQueue} 不接受 {@code null} 元素。
 * 在尝试 {@code add}、{@code put} 或 {@code offer} 一个 {@code null} 时，
 * 实现将抛出 {@code NullPointerException}。在 {@code poll} 操作失败时，
 * 使用 {@code null} 作为哨兵值。
 *
 * <p>{@code BlockingQueue} 可能是有容量限制的。在任意时刻，它可能有一个
 * {@code remainingCapacity}，超过这个容量限制后，无法再添加元素，
 * 否则操作将被阻塞。没有内在容量限制的 {@code BlockingQueue} 总是报告
 * 剩余容量为 {@code Integer.MAX_VALUE}。
 *
 * <p>{@code BlockingQueue} 实现主要用于生产者-消费者队列，
 * 但也支持 {@link java.util.Collection} 接口。
 * 例如，可以使用 {@code remove(x)} 从队列中删除任意元素。
 * 然而，这类操作通常并不高效，通常仅用于偶发场景，如取消已排队的消息。
 *
 * <p>{@code BlockingQueue} 的实现是线程安全的。所有的队列操作都使用内部锁或其他并发控制
 * 机制以原子方式完成。然而，<em>批量</em>的集合操作（如 {@code addAll}、{@code containsAll}、
 * {@code retainAll} 和 {@code removeAll}）不一定是原子完成的，除非在实现中有特别说明。
 * 因此，例如，调用 {@code addAll(c)} 可能在添加了部分元素后失败并抛出异常。
 *
 * <p>{@code BlockingQueue} 并不内置任何 “关闭” 或 “停止” 操作，
 * 用于指示不再会有新元素加入。对此类功能的需求和使用往往依赖于具体实现。
 * 一个常见策略是生产者插入特殊的 <em>结束标志</em> 或 <em>毒丸</em> 对象，
 * 当消费者取出这些对象时进行相应的解释。
 *
 * <p>
 * 基于典型的生产者-消费者场景的使用示例。
 * 请注意，{@code BlockingQueue} 可以安全地与多个生产者和多个消费者一起使用。
 *  <pre> {@code
 * class Producer implements Runnable {
 *   private final BlockingQueue queue;
 *   Producer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { queue.put(produce()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   Object produce() { ... }
 * }
 *
 * class Consumer implements Runnable {
 *   private final BlockingQueue queue;
 *   Consumer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { consume(queue.take()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   void consume(Object x) { ... }
 * }
 *
 * class Setup {
 *   void main() {
 *     BlockingQueue q = new SomeQueueImplementation();
 *     Producer p = new Producer(q);
 *     Consumer c1 = new Consumer(q);
 *     Consumer c2 = new Consumer(q);
 *     new Thread(p).start();
 *     new Thread(c1).start();
 *     new Thread(c2).start();
 *   }
 * }}</pre>
 *
 * <p>内存一致性效应：与其他并发集合一样，
 * 一个线程在将对象放入 {@code BlockingQueue} 之前的操作
 * <a href="package-summary.html#MemoryVisibility"><i>先行发生</i></a>于
 * 另一个线程从 {@code BlockingQueue} 中访问或移除该元素之后的操作。
 *
 * <p>该接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员之一。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> 此集合中保存的元素类型
 */

public interface BlockingQueue<E> extends Queue<E> {
    /**
     * 如果可以在不违反容量限制的情况下立即将指定的元素插入此队列，则插入成功并返回 {@code true}，
     * 如果当前没有可用空间，则抛出 {@code IllegalStateException}。在使用有容量限制的队列时，
     * 通常建议使用 {@link #offer(Object) offer}。
     *
     * @param e 要添加的元素
     * @return 成功时返回 {@code true}（由 {@link Collection#add} 指定）
     * @throws IllegalStateException 如果由于容量限制而无法添加该元素
     * @throws ClassCastException 如果指定元素的类型不允许将其添加到此队列
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性不允许将其添加到此队列
     */
    boolean add(E e);

    /**
     * 如果可以在不违反容量限制的情况下立即将指定的元素插入此队列，则插入成功并返回 {@code true}，
     * 如果当前没有可用空间，则返回 {@code false}。在使用有容量限制的队列时，此方法通常优于 {@link #add}，
     * 因为后者只能通过抛出异常来表示插入失败。
     *
     * @param e 要添加的元素
     * @return 如果元素已添加到此队列，则返回 {@code true}，否则返回 {@code false}
     * @throws ClassCastException 如果指定元素的类型不允许将其添加到此队列
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性不允许将其添加到此队列
     */
    boolean offer(E e);

    /**
     * 将指定的元素插入此队列，如有必要，等待空间变为可用。
     *
     * @param e 要添加的元素
     * @throws InterruptedException 如果在等待时被中断
     * @throws ClassCastException 如果指定元素的类型不允许将其添加到此队列
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性不允许将其添加到此队列
     */
    void put(E e) throws InterruptedException;

    /**
     * 将指定的元素插入此队列，如有必要，等待指定的等待时间，直到空间变为可用。
     *
     * @param e 要添加的元素
     * @param timeout 等待的最长时间
     * @param unit {@code TimeUnit}，确定如何解释 {@code timeout} 参数
     * @return 如果成功，则返回 {@code true}；如果在等待时间内空间未变为可用，则返回 {@code false}
     * @throws InterruptedException 如果在等待时被中断
     * @throws ClassCastException 如果指定元素的类型不允许将其添加到此队列
     * @throws NullPointerException 如果指定的元素为 null
     * @throws IllegalArgumentException 如果指定元素的某些属性不允许将其添加到此队列
     */
    boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 检索并移除此队列的头部元素，如有必要，等待直到有元素可用。
     *
     * @return 此队列的头部元素
     * @throws InterruptedException 如果在等待时被中断
     */
    E take() throws InterruptedException;

    /**
     * 检索并移除此队列的头部元素，如有必要，等待指定的时间，直到有元素可用。
     *
     * @param timeout 等待的最长时间
     * @param unit {@code TimeUnit}，确定如何解释 {@code timeout} 参数
     * @return 此队列的头部元素，如果在等待时间内没有可用的元素，则返回 {@code null}
     * @throws InterruptedException 如果在等待时被中断
     */
    E poll(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * 返回此队列可以接受的额外元素数量（在没有内存或资源约束的情况下），
     * 如果没有固有的限制，则返回 {@code Integer.MAX_VALUE}。
     *
     * <p>请注意，通过检查 {@code remainingCapacity}，你无法总是确定是否可以插入元素，
     * 因为可能有其他线程即将插入或移除元素。
     *
     * @return 剩余容量
     */
    int remainingCapacity();

    /**
     * 如果此队列中存在指定的元素，则移除此队列中的该元素的一个实例。
     * 更正式地说，移除一个元素 {@code e}，使得 {@code o.equals(e)}，如果此队列包含一个或多个这样的元素。
     * 如果此队列包含指定的元素，则返回 {@code true}（等价于此队列因调用而更改）。
     *
     * @param o 要从此队列中移除的元素
     * @return 如果此队列因调用而发生更改，则返回 {@code true}
     * @throws ClassCastException 如果指定元素的类型与此队列不兼容
     * @throws NullPointerException 如果指定的元素为 null
     */
    boolean remove(Object o);

    /**
     * 如果此队列包含指定的元素，则返回 {@code true}。
     * 更正式地说，如果且仅当此队列包含至少一个元素 {@code e} 使得 {@code o.equals(e)}，则返回 {@code true}。
     *
     * @param o 要检查是否包含在此队列中的对象
     * @return 如果此队列包含指定的元素，则返回 {@code true}
     * @throws ClassCastException 如果指定元素的类型与此队列不兼容
     * @throws NullPointerException 如果指定的元素为 null
     */
    public boolean contains(Object o);

    /**
     * 移除此队列中的所有可用元素并将其添加到给定集合中。此操作可能比重复轮询此队列更高效。
     * 尝试将元素添加到集合 {@code c} 时遇到的失败可能导致抛出异常时元素既不在队列中也不在集合中。
     * 试图将队列排空到自身会导致 {@code IllegalArgumentException}。此外，如果在操作进行期间修改了指定的集合，则此操作的行为是未定义的。
     *
     * @param c 要将元素转移到的集合
     * @return 转移的元素数
     * @throws UnsupportedOperationException 如果指定集合不支持添加元素
     * @throws ClassCastException 如果此队列中某个元素的类型不允许将其添加到指定集合
     * @throws NullPointerException 如果指定的集合为 null
     * @throws IllegalArgumentException 如果指定的集合是此队列，或此队列中的某个元素的某些属性不允许将其添加到指定集合
     */
    int drainTo(Collection<? super E> c);

    /**
     * 从此队列中最多移除指定数量的可用元素并将其添加到给定集合中。
     * 尝试将元素添加到集合 {@code c} 时遇到的失败可能导致抛出异常时元素既不在队列中也不在集合中。
     * 试图将队列排空到自身会导致 {@code IllegalArgumentException}。此外，如果在操作进行期间修改了指定的集合，则此操作的行为是未定义的。
     *
     * @param c 要将元素转移到的集合
     * @param maxElements 转移的最大元素数
     * @return 转移的元素数
     * @throws UnsupportedOperationException 如果指定集合不支持添加元素
     * @throws ClassCastException 如果此队列中某个元素的类型不允许将其添加到指定集合
     * @throws NullPointerException 如果指定的集合为 null
     * @throws IllegalArgumentException 如果指定的集合是此队列，或此队列中的某个元素的某些属性不允许将其添加到指定集合
     */
    int drainTo(Collection<? super E> c, int maxElements);
}

