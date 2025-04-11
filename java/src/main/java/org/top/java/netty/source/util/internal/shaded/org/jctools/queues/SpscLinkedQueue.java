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

/**
 * This is a weakened version of the MPSC algorithm as presented
 * <a href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue"> on
 * 1024 Cores</a> by D. Vyukov. The original has been adapted to Java and it's quirks with regards to memory
 * model and layout:
 * <ol>
 * <li>Use inheritance to ensure no false sharing occurs between producer/consumer node reference fields.
 * <li>As this is an SPSC we have no need for XCHG, an ordered store is enough.
 * </ol>
 * The queue is initialized with a stub node which is set to both the producer and consumer node references.
 * From this point follow the notes on offer/poll.
 *
 * @param <E>
 * @author nitsanw
 */

/**
 * 这是MPSC算法的弱化版本，源自D. Vyukov在<a href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue">1024 Cores</a>上提出的算法。原版已根据Java的内存模型和布局特点进行了适配：
 * <ol>
 * <li>使用继承来确保生产者/消费者节点引用字段之间不会发生伪共享。
 * <li>由于这是一个SPSC队列，因此不需要XCHG操作，有序存储就足够了。
 * </ol>
 * 队列初始化时包含一个存根节点，该节点同时被设置为生产者和消费者节点引用。从这一点开始，遵循offer/poll的说明。
 *
 * @param <E>
 * @author nitsanw
 */
public class SpscLinkedQueue<E> extends BaseLinkedQueue<E>
{

    public SpscLinkedQueue()
    {
        LinkedQueueNode<E> node = newNode();
        spProducerNode(node);
        spConsumerNode(node);
        node.soNext(null); // this ensures correct construction: StoreStore
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Offer is allowed from a SINGLE thread.<br>
     * Offer allocates a new node (holding the offered value) and:
     * <ol>
     * <li>Sets the new node as the producerNode
     * <li>Sets that node as the lastProducerNode.next
     * </ol>
     * From this follows that producerNode.next is always null and for all other nodes node.next is not null.
     *
     * @see MessagePassingQueue#offer(Object)
     * @see java.util.Queue#offer(Object)
     */

    /**
     * {@inheritDoc} <br>
     * <p>
     * 实现说明:<br>
     * 允许从单个线程进行offer操作。<br>
     * Offer操作会分配一个新节点（持有提供的值）并：
     * <ol>
     * <li>将新节点设置为producerNode
     * <li>将该节点设置为lastProducerNode.next
     * </ol>
     * 由此可知，producerNode.next始终为null，而对于所有其他节点，node.next不为null。
     *
     * @see MessagePassingQueue#offer(Object)
     * @see java.util.Queue#offer(Object)
     */
    @Override
    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException();
        }
        final LinkedQueueNode<E> nextNode = newNode(e);
        LinkedQueueNode<E> oldNode = lpProducerNode();
        soProducerNode(nextNode);
        // Should a producer thread get interrupted here the chain WILL be broken until that thread is resumed
        // 如果生产者线程在这里被中断，链将中断，直到该线程恢复
        // and completes the store in prev.next. This is a "bubble".
        // 并在 prev.next 中完成存储。这是一个“气泡”。
        // Inverting the order here will break the `isEmpty` invariant, and will require matching adjustments elsewhere.
        // 在此处颠倒顺序将破坏 `isEmpty` 不变性，并且需要在其他地方进行相应的调整。
        oldNode.soNext(nextNode);
        return true;
    }

    @Override
    public int fill(Supplier<E> s)
    {
        return MessagePassingQueueUtil.fillUnbounded(this, s);
    }

    @Override
    public int fill(Supplier<E> s, int limit)
    {
        if (null == s)
            throw new IllegalArgumentException("supplier is null");
        if (limit < 0)
            throw new IllegalArgumentException("limit is negative:" + limit);
        if (limit == 0)
            return 0;

        LinkedQueueNode<E> tail = newNode(s.get());
        final LinkedQueueNode<E> head = tail;
        for (int i = 1; i < limit; i++)
        {
            final LinkedQueueNode<E> temp = newNode(s.get());
            // spNext : soProducerNode ensures correct construction
            // spNext : soProducerNode 确保正确构建
            tail.spNext(temp);
            tail = temp;
        }
        final LinkedQueueNode<E> oldPNode = lpProducerNode();
        soProducerNode(tail);
        // same bubble as offer, and for the same reasons.
        // 与offer在同一个bubble中，原因相同。
        oldPNode.soNext(head);
        return limit;
    }

    @Override
    public void fill(Supplier<E> s, WaitStrategy wait, ExitCondition exit)
    {
        MessagePassingQueueUtil.fill(this, s, wait, exit);
    }
}
