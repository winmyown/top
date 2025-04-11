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

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.UNSAFE;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.fieldOffset;

abstract class BaseLinkedQueuePad0<E> extends AbstractQueue<E> implements MessagePassingQueue<E>
{
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    // byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
    // byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
    //    * drop 8b as object header acts as padding and is >= 8b *
    //    * 删除 8b，因为对象头充当填充且 >= 8b *
}

// $gen:ordered-fields
abstract class BaseLinkedQueueProducerNodeRef<E> extends BaseLinkedQueuePad0<E>
{
    final static long P_NODE_OFFSET = fieldOffset(BaseLinkedQueueProducerNodeRef.class, "producerNode");

    private volatile LinkedQueueNode<E> producerNode;

    final void spProducerNode(LinkedQueueNode<E> newValue)
    {
        UNSAFE.putObject(this, P_NODE_OFFSET, newValue);
    }

    final void soProducerNode(LinkedQueueNode<E> newValue)
    {
        UNSAFE.putOrderedObject(this, P_NODE_OFFSET, newValue);
    }

    final LinkedQueueNode<E> lvProducerNode()
    {
        return producerNode;
    }

    final boolean casProducerNode(LinkedQueueNode<E> expect, LinkedQueueNode<E> newValue)
    {
        return UNSAFE.compareAndSwapObject(this, P_NODE_OFFSET, expect, newValue);
    }

    final LinkedQueueNode<E> lpProducerNode()
    {
        return producerNode;
    }
}

abstract class BaseLinkedQueuePad1<E> extends BaseLinkedQueueProducerNodeRef<E>
{
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
}

//$gen:ordered-fields
abstract class BaseLinkedQueueConsumerNodeRef<E> extends BaseLinkedQueuePad1<E>
{
    private final static long C_NODE_OFFSET = fieldOffset(BaseLinkedQueueConsumerNodeRef.class,"consumerNode");

    private LinkedQueueNode<E> consumerNode;

    final void spConsumerNode(LinkedQueueNode<E> newValue)
    {
        consumerNode = newValue;
    }

    @SuppressWarnings("unchecked")
    final LinkedQueueNode<E> lvConsumerNode()
    {
        return (LinkedQueueNode<E>) UNSAFE.getObjectVolatile(this, C_NODE_OFFSET);
    }

    final LinkedQueueNode<E> lpConsumerNode()
    {
        return consumerNode;
    }
}

abstract class BaseLinkedQueuePad2<E> extends BaseLinkedQueueConsumerNodeRef<E>
{
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
}

/**
 * A base data structure for concurrent linked queues. For convenience also pulled in common single consumer
 * methods since at this time there's no plan to implement MC.
 */

/**
 * 并发链表队列的基础数据结构。为了方便起见，还引入了常见的单消费者方法，因为目前没有实现多消费者（MC）的计划。
 */
abstract class BaseLinkedQueue<E> extends BaseLinkedQueuePad2<E>
{

    @Override
    public final Iterator<E> iterator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        return this.getClass().getName();
    }

    protected final LinkedQueueNode<E> newNode()
    {
        return new LinkedQueueNode<E>();
    }

    protected final LinkedQueueNode<E> newNode(E e)
    {
        return new LinkedQueueNode<E>(e);
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * This is an O(n) operation as we run through all the nodes and count them.<br>
     * The accuracy of the value returned by this method is subject to races with producer/consumer threads. In
     * particular when racing with the consumer thread this method may under estimate the size.<br>
     *
     * @see Queue#size()
     */

    /**
     * {@inheritDoc} <br>
     * <p>
     * 实现说明：<br>
     * 这是一个O(n)操作，因为我们遍历所有节点并计数。<br>
     * 此方法返回值的准确性受生产者/消费者线程竞争的影响。特别是在与消费者线程竞争时，此方法可能会低估大小。<br>
     *
     * @see Queue#size()
     */
    @Override
    public final int size()
    {
        // Read consumer first, this is important because if the producer is node is 'older' than the consumer
        // 先读取消费者，这很重要，因为如果生产者节点比消费者“更旧”
        // the consumer may overtake it (consume past it) invalidating the 'snapshot' notion of size.
        // 消费者可能会超过它（在它之后消费），从而使“快照”大小的概念无效。
        LinkedQueueNode<E> chaserNode = lvConsumerNode();
        LinkedQueueNode<E> producerNode = lvProducerNode();
        int size = 0;
        // must chase the nodes all the way to the producer node, but there's no need to count beyond expected head.
        // 必须一直追踪节点到生产者节点，但无需计算超过预期的头部。
        while (chaserNode != producerNode && // don't go passed producer node
            chaserNode != null && // stop at last node
            size < Integer.MAX_VALUE) // stop at max int
        {
            LinkedQueueNode<E> next;
            next = chaserNode.lvNext();
            // check if this node has been consumed, if so return what we have
            // 检查此节点是否已被消耗，如果是，则返回我们已有的内容
            if (next == chaserNode)
            {
                return size;
            }
            chaserNode = next;
            size++;
        }
        return size;
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Queue is empty when producerNode is the same as consumerNode. An alternative implementation would be to
     * observe the producerNode.value is null, which also means an empty queue because only the
     * consumerNode.value is allowed to be null.
     *
     * @see MessagePassingQueue#isEmpty()
     */

    /**
     * {@inheritDoc} <br>
     * <p>
     * 实现说明:<br>
     * 当 producerNode 与 consumerNode 相同时，队列为空。另一种实现方式是观察 producerNode.value 是否为 null，这也意味着队列为空，因为只有 consumerNode.value 允许为 null。
     *
     * @see MessagePassingQueue#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        LinkedQueueNode<E> consumerNode = lvConsumerNode();
        LinkedQueueNode<E> producerNode = lvProducerNode();
        return consumerNode == producerNode;
    }

    protected E getSingleConsumerNodeValue(LinkedQueueNode<E> currConsumerNode, LinkedQueueNode<E> nextNode)
    {
        // we have to null out the value because we are going to hang on to the node
        // 我们需要将值置空，因为我们要保留这个节点
        final E nextValue = nextNode.getAndNullValue();

        // Fix up the next ref of currConsumerNode to prevent promoted nodes from keeping new ones alive.

        // 修复 currConsumerNode 的 next 引用，防止被提升的节点保持新节点的存活。
        // We use a reference to self instead of null because null is already a meaningful value (the next of
        // 我们使用对自身的引用而不是null，因为null已经是一个有意义的值（下一个
        // producer node is null).
        // 生产者节点为空
        currConsumerNode.soNext(currConsumerNode);
        spConsumerNode(nextNode);
        // currConsumerNode is now no longer referenced and can be collected
        // currConsumerNode 现在不再被引用，可以被回收
        return nextValue;
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Poll is allowed from a SINGLE thread.<br>
     * Poll is potentially blocking here as the {@link Queue#poll()} does not allow returning {@code null} if the queue is not
     * empty. This is very different from the original Vyukov guarantees. See {@link #relaxedPoll()} for the original
     * semantics.<br>
     * Poll reads {@code consumerNode.next} and:
     * <ol>
     * <li>If it is {@code null} AND the queue is empty return {@code null}, <b>if queue is not empty spin wait for
     * value to become visible</b>.
     * <li>If it is not {@code null} set it as the consumer node and return it's now evacuated value.
     * </ol>
     * This means the consumerNode.value is always {@code null}, which is also the starting point for the queue.
     * Because {@code null} values are not allowed to be offered this is the only node with it's value set to
     * {@code null} at any one time.
     *
     * @see MessagePassingQueue#poll()
     * @see Queue#poll()
     */

    /**
     * {@inheritDoc} <br>
     * <p>
     * 实现说明:<br>
     * 只允许从单个线程进行轮询。<br>
     * 轮询在此处可能会阻塞，因为 {@link Queue#poll()} 不允许在队列不为空时返回 {@code null}。这与原始的 Vyukov 保证非常不同。有关原始语义，请参见 {@link #relaxedPoll()}。<br>
     * 轮询读取 {@code consumerNode.next} 并：
     * <ol>
     * <li>如果它为 {@code null} 且队列为空，则返回 {@code null}，<b>如果队列不为空，则自旋等待值变为可见</b>。
     * <li>如果它不为 {@code null}，则将其设置为消费者节点并返回其现在已移除的值。
     * </ol>
     * 这意味着 consumerNode.value 始终为 {@code null}，这也是队列的起始点。因为不允许提供 {@code null} 值，所以在任何时候这是唯一一个值设置为 {@code null} 的节点。
     *
     * @see MessagePassingQueue#poll()
     * @see Queue#poll()
     */
    @Override
    public E poll()
    {
        final LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
        LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
        if (nextNode != null)
        {
            return getSingleConsumerNodeValue(currConsumerNode, nextNode);
        }
        else if (currConsumerNode != lvProducerNode())
        {
            nextNode = spinWaitForNextNode(currConsumerNode);
            // got the next node...
            // 获取下一个节点...
            return getSingleConsumerNodeValue(currConsumerNode, nextNode);
        }
        return null;
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Peek is allowed from a SINGLE thread.<br>
     * Peek is potentially blocking here as the {@link Queue#peek()} does not allow returning {@code null} if the queue is not
     * empty. This is very different from the original Vyukov guarantees. See {@link #relaxedPeek()} for the original
     * semantics.<br>
     * Poll reads the next node from the consumerNode and:
     * <ol>
     * <li>If it is {@code null} AND the queue is empty return {@code null}, <b>if queue is not empty spin wait for
     * value to become visible</b>.
     * <li>If it is not {@code null} return it's value.
     * </ol>
     *
     * @see MessagePassingQueue#peek()
     * @see Queue#peek()
     */

    /**
     * {@inheritDoc} <br>
     * <p>
     * 实现说明:<br>
     * Peek 操作允许在单一线程中进行。<br>
     * Peek 操作在此处可能是阻塞的，因为 {@link Queue#peek()} 不允许在队列不为空时返回 {@code null}。这与原始的 Vyukov 保证非常不同。有关原始语义，请参见 {@link #relaxedPeek()}。<br>
     * Poll 操作从 consumerNode 读取下一个节点并：
     * <ol>
     * <li>如果它为 {@code null} 且队列为空，则返回 {@code null}，<b>如果队列不为空，则自旋等待值变为可见</b>。
     * <li>如果它不为 {@code null}，则返回其值。
     * </ol>
     *
     * @see MessagePassingQueue#peek()
     * @see Queue#peek()
     */
    @Override
    public E peek()
    {
        final LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
        LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
        if (nextNode != null)
        {
            return nextNode.lpValue();
        }
        else if (currConsumerNode != lvProducerNode())
        {
            nextNode = spinWaitForNextNode(currConsumerNode);
            // got the next node...
            // 获取下一个节点...
            return nextNode.lpValue();
        }
        return null;
    }

    LinkedQueueNode<E> spinWaitForNextNode(LinkedQueueNode<E> currNode)
    {
        LinkedQueueNode<E> nextNode;
        while ((nextNode = currNode.lvNext()) == null)
        {
            // spin, we are no longer wait free
            // 自旋，我们不再是无等待的
        }
        return nextNode;
    }

    @Override
    public E relaxedPoll()
    {
        final LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
        final LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
        if (nextNode != null)
        {
            return getSingleConsumerNodeValue(currConsumerNode, nextNode);
        }
        return null;
    }

    @Override
    public E relaxedPeek()
    {
        final LinkedQueueNode<E> nextNode = lpConsumerNode().lvNext();
        if (nextNode != null)
        {
            return nextNode.lpValue();
        }
        return null;
    }

    @Override
    public boolean relaxedOffer(E e)
    {
        return offer(e);
    }

    @Override
    public int drain(Consumer<E> c, int limit)
    {
        if (null == c)
            throw new IllegalArgumentException("c is null");
        if (limit < 0)
            throw new IllegalArgumentException("limit is negative: " + limit);
        if (limit == 0)
            return 0;

        LinkedQueueNode<E> chaserNode = this.lpConsumerNode();
        for (int i = 0; i < limit; i++)
        {
            final LinkedQueueNode<E> nextNode = chaserNode.lvNext();

            if (nextNode == null)
            {
                return i;
            }
            // we have to null out the value because we are going to hang on to the node
            // 我们需要将值置空，因为我们要保留这个节点
            final E nextValue = getSingleConsumerNodeValue(chaserNode, nextNode);
            chaserNode = nextNode;
            c.accept(nextValue);
        }
        return limit;
    }

    @Override
    public int drain(Consumer<E> c)
    {
        return MessagePassingQueueUtil.drain(this, c);
    }

    @Override
    public void drain(Consumer<E> c, WaitStrategy wait, ExitCondition exit)
    {
        MessagePassingQueueUtil.drain(this, c, wait, exit);
    }

    @Override
    public int capacity()
    {
        return UNBOUNDED_CAPACITY;
    }

}
