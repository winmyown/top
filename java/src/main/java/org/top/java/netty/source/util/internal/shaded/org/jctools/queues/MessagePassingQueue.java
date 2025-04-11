/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * 根据 Apache 许可证 2.0 版本（“许可证”）授权;
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下网址获取许可证的副本：
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则按“原样”分发软件，
 * 没有任何明示或暗示的保证或条件。
 * 请参阅许可证以了解特定语言的权限和限制。
 */
package org.top.java.netty.source.util.internal.shaded.org.jctools.queues;

import java.util.Queue;

/**
 * Message passing queues are intended for concurrent method passing. A subset of {@link Queue} methods are provided
 * with the same semantics, while further functionality which accomodates the concurrent usecase is also on offer.
 * <p>
 * Message passing queues provide happens before semantics to messages passed through, namely that writes made
 * by the producer before offering the message are visible to the consuming thread after the message has been
 * polled out of the queue.
 *
 * @param <T> the event/message type
 */

/**
 * 消息传递队列旨在用于并发方法传递。提供了与{@link Queue}方法相同的语义的子集，同时还提供了适应并发用例的进一步功能。
 * <p>
 * 消息传递队列为通过的消息提供happens before语义，即生产者在线程提供消息之前所做的写入在消息从队列中被消费后对消费线程可见。
 *
 * @param <T> 事件/消息类型
 */
public interface MessagePassingQueue<T>
{
    int UNBOUNDED_CAPACITY = -1;

    interface Supplier<T>
    {
        /**
         * This method will return the next value to be written to the queue. As such the queue
         * implementations are commited to insert the value once the call is made.
         * <p>
         * Users should be aware that underlying queue implementations may upfront claim parts of the queue
         * for batch operations and this will effect the view on the queue from the supplier method. In
         * particular size and any offer methods may take the view that the full batch has already happened.
         *
         * <p><b>WARNING</b>: this method is assumed to never throw. Breaking this assumption can lead to a broken queue.
         * <p><b>WARNING</b>: this method is assumed to never return {@code null}. Breaking this assumption can lead to a broken queue.
         *
         * @return new element, NEVER {@code null}
         */
        /**
         * 此方法将返回要写入队列的下一个值。因此，一旦调用此方法，队列实现将承诺插入该值。
         * <p>
         * 用户应注意，底层的队列实现可能会预先为批量操作分配队列的部分空间，这将影响供应商方法对队列的视图。
         * 特别是，size 和任何 offer 方法可能会认为整个批量操作已经发生。
         *
         * <p><b>警告</b>: 此方法假定永远不会抛出异常。违反此假设可能导致队列损坏。
         * <p><b>警告</b>: 此方法假定永远不会返回 {@code null}。违反此假设可能导致队列损坏。
         *
         * @return 新元素，永远不为 {@code null}
         */
        T get();
    }

    interface Consumer<T>
    {
        /**
         * This method will process an element already removed from the queue. This method is expected to
         * never throw an exception.
         * <p>
         * Users should be aware that underlying queue implementations may upfront claim parts of the queue
         * for batch operations and this will effect the view on the queue from the accept method. In
         * particular size and any poll/peek methods may take the view that the full batch has already
         * happened.
         *
         * <p><b>WARNING</b>: this method is assumed to never throw. Breaking this assumption can lead to a broken queue.
         * @param e not {@code null}
         */
        /**
         * 该方法将处理已从队列中移除的元素。此方法预期永远不会抛出异常。
         * <p>
         * 用户应注意，底层队列实现可能会预先声明队列的一部分用于批量操作，这会影响从accept方法中看到的队列视图。
         * 特别是，size和任何poll/peek方法可能会认为完整的批量操作已经发生。
         *
         * <p><b>警告</b>：假定此方法永远不会抛出异常。打破此假设可能导致队列损坏。
         * @param e 非 {@code null}
         */
        void accept(T e);
    }

    interface WaitStrategy
    {
        /**
         * This method can implement static or dynamic backoff. Dynamic backoff will rely on the counter for
         * estimating how long the caller has been idling. The expected usage is:
         * <p>
         * <pre>
         * <code>
         * int ic = 0;
         * while(true) {
         *   if(!isGodotArrived()) {
         *     ic = w.idle(ic);
         *     continue;
         *   }
         *   ic = 0;
         *   // party with Godot until he goes again
         * }
         * </code>
         * </pre>
         *
         * @param idleCounter idle calls counter, managed by the idle method until reset
         * @return new counter value to be used on subsequent idle cycle
         */
        /**
         * 该方法可以实现静态或动态回退。动态回退将依赖于计数器来估计调用者已经空闲了多长时间。预期用法如下：
         * <p>
         * <pre>
         * <code>
         * int ic = 0;
         * while(true) {
         *   if(!isGodotArrived()) {
         *     ic = w.idle(ic);
         *     continue;
         *   }
         *   ic = 0;
         *   // 与Godot一起狂欢，直到他再次离开
         * }
         * </code>
         * </pre>
         *
         * @param idleCounter 空闲调用计数器，由idle方法管理直到重置
         * @return 新的计数器值，用于后续的空闲周期
         */
        int idle(int idleCounter);
    }

    interface ExitCondition
    {

        /**
         * This method should be implemented such that the flag read or determination cannot be hoisted out of
         * a loop which notmally means a volatile load, but with JDK9 VarHandles may mean getOpaque.
         *
         * @return true as long as we should keep running
         */

        /**
         * 此方法的实现应确保标志的读取或确定不能被提升到循环之外，通常这意味着使用volatile加载，
         * 但在JDK9中，使用VarHandles可能意味着getOpaque。
         *
         * @return 只要我们应该继续运行，就返回true
         */
        boolean keepRunning();
    }

    /**
     * Called from a producer thread subject to the restrictions appropriate to the implementation and
     * according to the {@link Queue#offer(Object)} interface.
     *
     * @param e not {@code null}, will throw NPE if it is
     * @return true if element was inserted into the queue, false iff full
     */

    /**
     * 由生产者线程调用，受限于实现中的适当限制，并根据 {@link Queue#offer(Object)} 接口。
     *
     * @param e 不能为 {@code null}，如果为 null 将抛出 NPE
     * @return 如果元素成功插入队列返回 true，如果队列已满返回 false
     */
    boolean offer(T e);

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and
     * according to the {@link Queue#poll()} interface.
     *
     * @return a message from the queue if one is available, {@code null} iff empty
     */

    /**
     * 由消费者线程调用，受限于实现中适用的限制，并符合 {@link Queue#poll()} 接口的规范。
     *
     * @return 如果队列中有消息则返回一条消息，如果队列为空则返回 {@code null}
     */
    T poll();

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation and
     * according to the {@link Queue#peek()} interface.
     *
     * @return a message from the queue if one is available, {@code null} iff empty
     */

    /**
     * 由消费者线程调用，需根据实现和 {@link Queue#peek()} 接口的适当限制进行调用。
     *
     * @return 如果队列中有消息则返回消息，如果队列为空则返回 {@code null}
     */
    T peek();

    /**
     * This method's accuracy is subject to concurrent modifications happening as the size is estimated and as
     * such is a best effort rather than absolute value. For some implementations this method may be O(n)
     * rather than O(1).
     *
     * @return number of messages in the queue, between 0 and {@link Integer#MAX_VALUE} but less or equals to
     * capacity (if bounded).
     */

    /**
     * 该方法的准确性受并发修改的影响，因为大小是在估计时计算的，因此是一个最佳估计值而不是绝对值。对于某些实现，该方法可能是 O(n)
     * 而不是 O(1)。
     *
     * @return 队列中的消息数量，介于 0 和 {@link Integer#MAX_VALUE} 之间，但小于或等于容量（如果有界）。
     */
    int size();

    /**
     * Removes all items from the queue. Called from the consumer thread subject to the restrictions
     * appropriate to the implementation and according to the {@link Queue#clear()} interface.
     */

    /**
     * 从队列中移除所有项。由消费者线程调用，需遵守实现相关的限制，
     * 并符合 {@link Queue#clear()} 接口的规定。
     */
    void clear();

    /**
     * This method's accuracy is subject to concurrent modifications happening as the observation is carried
     * out.
     *
     * @return true if empty, false otherwise
     */

    /**
     * 该方法的准确性受并发修改的影响，这些修改可能在观察过程中发生。
     *
     * @return 如果为空则返回 true，否则返回 false
     */
    boolean isEmpty();

    /**
     * @return the capacity of this queue or {@link MessagePassingQueue#UNBOUNDED_CAPACITY} if not bounded
     */

    /**
     * @return 该队列的容量，如果无界则返回 {@link MessagePassingQueue#UNBOUNDED_CAPACITY}
     */
    int capacity();

    /**
     * Called from a producer thread subject to the restrictions appropriate to the implementation. As opposed
     * to {@link Queue#offer(Object)} this method may return false without the queue being full.
     *
     * @param e not {@code null}, will throw NPE if it is
     * @return true if element was inserted into the queue, false if unable to offer
     */

    /**
     * 由生产者线程调用，受限于实现所适用的限制。与 {@link Queue#offer(Object)} 不同，此方法可能会在队列未满时返回 false。
     *
     * @param e 不能为 {@code null}，如果为 null 将抛出 NPE
     * @return 如果元素成功插入队列则返回 true，否则返回 false
     */
    boolean relaxedOffer(T e);

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation. As
     * opposed to {@link Queue#poll()} this method may return {@code null} without the queue being empty.
     *
     * @return a message from the queue if one is available, {@code null} if unable to poll
     */

    /**
     * 由消费者线程调用，受限于实现中适当的限制。与 {@link Queue#poll()} 不同，此方法可能会返回 {@code null}，即使队列不为空。
     *
     * @return 如果队列中有可用消息，则返回消息；如果无法轮询，则返回 {@code null}
     */
    T relaxedPoll();

    /**
     * Called from the consumer thread subject to the restrictions appropriate to the implementation. As
     * opposed to {@link Queue#peek()} this method may return {@code null} without the queue being empty.
     *
     * @return a message from the queue if one is available, {@code null} if unable to peek
     */

    /**
     * 由消费者线程调用，受限于实现所适用的限制。与 {@link Queue#peek()} 不同，此方法可能会返回 {@code null}，即使队列不为空。
     *
     * @return 如果队列中有可用消息则返回该消息，如果无法窥视则返回 {@code null}
     */
    T relaxedPeek();

    /**
     * Remove up to <i>limit</i> elements from the queue and hand to consume. This should be semantically
     * similar to:
     * <p>
     * <pre>{@code
     *   M m;
     *   int i = 0;
     *   for(;i < limit && (m = relaxedPoll()) != null; i++){
     *     c.accept(m);
     *   }
     *   return i;
     * }</pre>
     * <p>
     * There's no strong commitment to the queue being empty at the end of a drain. Called from a consumer
     * thread subject to the restrictions appropriate to the implementation.
     * <p>
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Consumer#accept} make sure you have read
     * and understood these before using this method.
     *
     * @return the number of polled elements
     * @throws IllegalArgumentException c is {@code null}
     * @throws IllegalArgumentException if limit is negative
     */

    /**
     * 从队列中移除最多 <i>limit</i> 个元素并交给消费者处理。此操作在语义上应类似于：
     * <p>
     * <pre>{@code
     *   M m;
     *   int i = 0;
     *   for(;i < limit && (m = relaxedPoll()) != null; i++){
     *     c.accept(m);
     *   }
     *   return i;
     * }</pre>
     * <p>
     * 此操作不保证在 drain 操作结束后队列为空。此方法由消费者线程调用，需遵守实现中的相关限制。
     * <p>
     * <b>警告</b>：此方法对 {@link Consumer#accept} 有明确的假设，请在使用前确保已阅读并理解这些假设。
     *
     * @return 移除的元素数量
     * @throws IllegalArgumentException 如果 c 为 {@code null}
     * @throws IllegalArgumentException 如果 limit 为负数
     */
    int drain(Consumer<T> c, int limit);

    /**
     * Stuff the queue with up to <i>limit</i> elements from the supplier. Semantically similar to:
     * <p>
     * <pre>{@code
     *   for(int i=0; i < limit && relaxedOffer(s.get()); i++);
     * }</pre>
     * <p>
     * There's no strong commitment to the queue being full at the end of a fill. Called from a producer
     * thread subject to the restrictions appropriate to the implementation.
     *
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Supplier#get} make sure you have read
     * and understood these before using this method.
     *
     * @return the number of offered elements
     * @throws IllegalArgumentException s is {@code null}
     * @throws IllegalArgumentException if limit is negative
     */

    /**
     * 使用来自供应商的至多 <i>limit</i> 个元素填充队列。语义上类似于：
     * <p>
     * <pre>{@code
     *   for(int i=0; i < limit && relaxedOffer(s.get()); i++);
     * }</pre>
     * <p>
     * 不保证队列在填充结束时是满的。由生产者线程调用，需遵守实现相关的限制。
     *
     * <b>警告</b>：对 {@link Supplier#get} 有明确的假设，使用此方法前请确保已阅读并理解这些假设。
     *
     * @return 提供的元素数量
     * @throws IllegalArgumentException 如果 s 为 {@code null}
     * @throws IllegalArgumentException 如果 limit 为负数
     */
    int fill(Supplier<T> s, int limit);

    /**
     * Remove all available item from the queue and hand to consume. This should be semantically similar to:
     * <pre>
     * M m;
     * while((m = relaxedPoll()) != null){
     * c.accept(m);
     * }
     * </pre>
     * There's no strong commitment to the queue being empty at the end of a drain. Called from a
     * consumer thread subject to the restrictions appropriate to the implementation.
     * <p>
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Consumer#accept} make sure you have read
     * and understood these before using this method.
     *
     * @return the number of polled elements
     * @throws IllegalArgumentException c is {@code null}
     */

    /**
     * 从队列中移除所有可用项并交给消费者处理。这应类似于以下语义：
     * <pre>
     * M m;
     * while((m = relaxedPoll()) != null){
     * c.accept(m);
     * }
     * </pre>
     * 该方法不保证在结束时队列为空。从消费者线程调用，需遵循实现中的限制条件。
     * <p>
     * <b>警告</b>: 对 {@link Consumer#accept} 有明确的假设，请在使用此方法前确保已阅读并理解这些假设。
     *
     * @return 被轮询的元素数量
     * @throws IllegalArgumentException 如果 c 为 {@code null}
     */
    int drain(Consumer<T> c);

    /**
     * Stuff the queue with elements from the supplier. Semantically similar to:
     * <pre>
     * while(relaxedOffer(s.get());
     * </pre>
     * There's no strong commitment to the queue being full at the end of a fill. Called from a
     * producer thread subject to the restrictions appropriate to the implementation.
     * <p>
     * Unbounded queues will fill up the queue with a fixed amount rather than fill up to oblivion.
     *
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Supplier#get} make sure you have read
     * and understood these before using this method.
     *
     * @return the number of offered elements
     * @throws IllegalArgumentException s is {@code null}
     */

    /**
     * 使用供应商提供的元素填充队列。语义上类似于：
     * <pre>
     * while(relaxedOffer(s.get());
     * </pre>
     * 并不保证在填充结束时队列已满。由生产者线程调用，需遵循实现中的限制。
     * <p>
     * 无界队列将填充固定数量的元素，而不是无限填充。
     *
     * <b>警告</b>：对 {@link Supplier#get} 做出了明确的假设，请在使用此方法前确保已阅读并理解这些假设。
     *
     * @return 提供的元素数量
     * @throws IllegalArgumentException 如果 s 为 {@code null}
     */
    int fill(Supplier<T> s);

    /**
     * Remove elements from the queue and hand to consume forever. Semantically similar to:
     * <p>
     * <pre>
     *  int idleCounter = 0;
     *  while (exit.keepRunning()) {
     *      E e = relaxedPoll();
     *      if(e==null){
     *          idleCounter = wait.idle(idleCounter);
     *          continue;
     *      }
     *      idleCounter = 0;
     *      c.accept(e);
     *  }
     * </pre>
     * <p>
     * Called from a consumer thread subject to the restrictions appropriate to the implementation.
     * <p>
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Consumer#accept} make sure you have read
     * and understood these before using this method.
     *
     * @throws IllegalArgumentException c OR wait OR exit are {@code null}
     */

    /**
     * 从队列中移除元素并交给消费者处理，直到永远。语义上类似于：
     * <p>
     * <pre>
     *  int idleCounter = 0;
     *  while (exit.keepRunning()) {
     *      E e = relaxedPoll();
     *      if(e==null){
     *          idleCounter = wait.idle(idleCounter);
     *          continue;
     *      }
     *      idleCounter = 0;
     *      c.accept(e);
     *  }
     * </pre>
     * <p>
     * 从消费者线程调用，受限于实现中适用的限制。
     * <p>
     * <b>警告</b>：对 {@link Consumer#accept} 做了明确的假设，请在使用此方法前确保已阅读并理解这些假设。
     *
     * @throws IllegalArgumentException 如果 c 或 wait 或 exit 为 {@code null}
     */
    void drain(Consumer<T> c, WaitStrategy wait, ExitCondition exit);

    /**
     * Stuff the queue with elements from the supplier forever. Semantically similar to:
     * <p>
     * <pre>
     * <code>
     *  int idleCounter = 0;
     *  while (exit.keepRunning()) {
     *      E e = s.get();
     *      while (!relaxedOffer(e)) {
     *          idleCounter = wait.idle(idleCounter);
     *          continue;
     *      }
     *      idleCounter = 0;
     *  }
     * </code>
     * </pre>
     * <p>
     * Called from a producer thread subject to the restrictions appropriate to the implementation. The main difference
     * being that implementors MUST assure room in the queue is available BEFORE calling {@link Supplier#get}.
     *
     * <b>WARNING</b>: Explicit assumptions are made with regards to {@link Supplier#get} make sure you have read
     * and understood these before using this method.
     *
     * @throws IllegalArgumentException s OR wait OR exit are {@code null}
     */

    /**
     * 永远使用供应商提供的元素填充队列。语义上类似于：
     * <p>
     * <pre>
     * <code>
     *  int idleCounter = 0;
     *  while (exit.keepRunning()) {
     *      E e = s.get();
     *      while (!relaxedOffer(e)) {
     *          idleCounter = wait.idle(idleCounter);
     *          continue;
     *      }
     *      idleCounter = 0;
     *  }
     * </code>
     * </pre>
     * <p>
     * 从生产者线程调用，受限于实现中适用的限制。主要区别在于，实现者必须在调用 {@link Supplier#get} 之前确保队列中有空间可用。
     *
     * <b>警告</b>：对 {@link Supplier#get} 有明确的假设，请在使用此方法之前确保已阅读并理解这些假设。
     *
     * @throws IllegalArgumentException 如果 s 或 wait 或 exit 为 {@code null}
     */
    void fill(Supplier<T> s, WaitStrategy wait, ExitCondition exit);
}
