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

import org.top.java.netty.source.util.internal.shaded.org.jctools.util.InternalAPI;

/**
 * A note to maintainers on index assumptions: in a single threaded world it would seem intuitive to assume:
 * <pre>
 * <code>producerIndex >= consumerIndex</code>
 * </pre>
 * As an invariant, but in a concurrent, long running settings all of the following need to be considered:
 * <ul>
 *     <li> <code>consumerIndex > producerIndex</code> : due to counter overflow (unlikey with longs, but easy to reason)
 *     <li> <code>consumerIndex > producerIndex</code> : due to consumer FastFlow like implementation discovering the
 *     element before the counter is updated.
 *     <li> <code>producerIndex - consumerIndex < 0</code> : due to above.
 *     <li> <code>producerIndex - consumerIndex > Integer.MAX_VALUE</code> : as linked buffers allow constructing queues
 *     with more than <code>Integer.MAX_VALUE</code> elements.
 *
 * </ul>
 */

/**
 * 维护者关于索引假设的说明：在单线程世界中，似乎可以直观地假设：
 * <pre>
 * <code>producerIndex >= consumerIndex</code>
 * </pre>
 * 作为一个不变量，但在并发、长时间运行的设置中，需要考虑以下所有情况：
 * <ul>
 *     <li> <code>consumerIndex > producerIndex</code> : 由于计数器溢出（在 long 类型中不太可能，但易于推理）
 *     <li> <code>consumerIndex > producerIndex</code> : 由于消费者 FastFlow 类似实现在计数器更新之前发现元素。
 *     <li> <code>producerIndex - consumerIndex < 0</code> : 由于上述原因。
 *     <li> <code>producerIndex - consumerIndex > Integer.MAX_VALUE</code> : 因为链接缓冲区允许构建元素数量超过 <code>Integer.MAX_VALUE</code> 的队列。
 * </ul>
 */
@InternalAPI
public final class IndexedQueueSizeUtil
{
    public static int size(IndexedQueue iq)
    {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and
         * consumer indices, therefore protection is required to ensure size is within valid range. In the
         * event of concurrent polls/offers to this method the size is OVER estimated as we read consumer
         * index BEFORE the producer index.
         */
        /*
         * 由于线程可能在读取生产者和消费者索引之间被中断或重新调度，因此需要保护措施以确保大小在有效范围内。在
         * 并发调用poll/offer方法的情况下，大小会被高估，因为我们先读取消费者索引，再读取生产者索引。
         */
        long after = iq.lvConsumerIndex();
        long size;
        while (true)
        {
            final long before = after;
            final long currentProducerIndex = iq.lvProducerIndex();
            after = iq.lvConsumerIndex();
            if (before == after)
            {
                size = (currentProducerIndex - after);
                break;
            }
        }
        // Long overflow is impossible (), so size is always positive. Integer overflow is possible for the unbounded
        // 长整型溢出是不可能的，所以大小总是正数。对于无界的整数，溢出是可能的。
        // indexed queues.
        // 索引队列。
        if (size > Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        // concurrent updates to cIndex and pIndex may lag behind other progress enablers (e.g. FastFlow), so we need
        // 对 cIndex 和 pIndex 的并发更新可能会落后于其他进度推动者（例如 FastFlow），因此我们需要
        // to check bounds
        // 检查边界
        else if (size < 0)
        {
            return 0;
        }
        else if (iq.capacity() != MessagePassingQueue.UNBOUNDED_CAPACITY && size > iq.capacity())
        {
            return iq.capacity();
        }
        else
        {
            return (int) size;
        }
    }

    public static boolean isEmpty(IndexedQueue iq)
    {
        // Order matters!
        // 顺序很重要！
        // Loading consumer before producer allows for producer increments after consumer index is read.
        // 在生产者之前加载消费者允许在消费者索引读取后进行生产者增量。
        // This ensures this method is conservative in it's estimate. Note that as this is an MPMC there is
        // 这确保了该方法在估算时是保守的。请注意，由于这是一个MPMC，
        // nothing we can do to make this an exact method.
        // 我们无法将其变成一个精确的方法。
        return (iq.lvConsumerIndex() >= iq.lvProducerIndex());
    }

    @InternalAPI
    public interface IndexedQueue
    {
        long lvConsumerIndex();

        long lvProducerIndex();

        int capacity();
    }
}
