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

import io.netty.util.internal.shaded.org.jctools.queues.IndexedQueueSizeUtil.IndexedQueue;
import org.top.java.netty.source.util.internal.shaded.org.jctools.util.PortableJvmInfo;
import org.top.java.netty.source.util.internal.shaded.org.jctools.util.Pow2;
import org.top.java.netty.source.util.internal.shaded.org.jctools.util.RangeUtil;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.top.java.netty.source.util.internal.shaded.org.jctools.queues.LinkedArrayQueueUtil.length;
import static org.top.java.netty.source.util.internal.shaded.org.jctools.queues.LinkedArrayQueueUtil.modifiedCalcCircularRefElementOffset;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.UNSAFE;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.fieldOffset;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess.*;


abstract class BaseMpscLinkedArrayQueuePad1<E> extends AbstractQueue<E> implements IndexedQueue
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

// $gen:ordered-fields
abstract class BaseMpscLinkedArrayQueueProducerFields<E> extends BaseMpscLinkedArrayQueuePad1<E>
{
    private final static long P_INDEX_OFFSET = fieldOffset(BaseMpscLinkedArrayQueueProducerFields.class, "producerIndex");

    private volatile long producerIndex;

    @Override
    public final long lvProducerIndex()
    {
        return producerIndex;
    }

    final void soProducerIndex(long newValue)
    {
        UNSAFE.putOrderedLong(this, P_INDEX_OFFSET, newValue);
    }

    final boolean casProducerIndex(long expect, long newValue)
    {
        return UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, expect, newValue);
    }
}

abstract class BaseMpscLinkedArrayQueuePad2<E> extends BaseMpscLinkedArrayQueueProducerFields<E>
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

// $gen:ordered-fields
abstract class BaseMpscLinkedArrayQueueConsumerFields<E> extends BaseMpscLinkedArrayQueuePad2<E>
{
    private final static long C_INDEX_OFFSET = fieldOffset(BaseMpscLinkedArrayQueueConsumerFields.class,"consumerIndex");

    private volatile long consumerIndex;
    protected long consumerMask;
    protected E[] consumerBuffer;

    @Override
    public final long lvConsumerIndex()
    {
        return consumerIndex;
    }

    final long lpConsumerIndex()
    {
        return UNSAFE.getLong(this, C_INDEX_OFFSET);
    }

    final void soConsumerIndex(long newValue)
    {
        UNSAFE.putOrderedLong(this, C_INDEX_OFFSET, newValue);
    }
}

abstract class BaseMpscLinkedArrayQueuePad3<E> extends BaseMpscLinkedArrayQueueConsumerFields<E>
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

// $gen:ordered-fields
abstract class BaseMpscLinkedArrayQueueColdProducerFields<E> extends BaseMpscLinkedArrayQueuePad3<E>
{
    private final static long P_LIMIT_OFFSET = fieldOffset(BaseMpscLinkedArrayQueueColdProducerFields.class,"producerLimit");

    private volatile long producerLimit;
    protected long producerMask;
    protected E[] producerBuffer;

    final long lvProducerLimit()
    {
        return producerLimit;
    }

    final boolean casProducerLimit(long expect, long newValue)
    {
        return UNSAFE.compareAndSwapLong(this, P_LIMIT_OFFSET, expect, newValue);
    }

    final void soProducerLimit(long newValue)
    {
        UNSAFE.putOrderedLong(this, P_LIMIT_OFFSET, newValue);
    }
}


/**
 * An MPSC array queue which starts at <i>initialCapacity</i> and grows to <i>maxCapacity</i> in linked chunks
 * of the initial size. The queue grows only when the current buffer is full and elements are not copied on
 * resize, instead a link to the new buffer is stored in the old buffer for the consumer to follow.
 */


/**
 * 一个MPSC数组队列，从<i>initialCapacity</i>开始，并以初始大小的链接块增长到<i>maxCapacity</i>。
 * 队列仅在当前缓冲区满时增长，并且在调整大小时不会复制元素，而是在旧缓冲区中存储指向新缓冲区的链接以供消费者跟随。
 */
abstract class BaseMpscLinkedArrayQueue<E> extends BaseMpscLinkedArrayQueueColdProducerFields<E>
    implements MessagePassingQueue<E>, QueueProgressIndicators
{
    // No post padding here, subclasses must add
    // 此处无后置填充，子类必须添加
    private static final Object JUMP = new Object();
    private static final Object BUFFER_CONSUMED = new Object();
    private static final int CONTINUE_TO_P_INDEX_CAS = 0;
    private static final int RETRY = 1;
    private static final int QUEUE_FULL = 2;
    private static final int QUEUE_RESIZE = 3;


    /**
     * @param initialCapacity the queue initial capacity. If chunk size is fixed this will be the chunk size.
     *                        Must be 2 or more.
     */


    /**
     * @param initialCapacity 队列的初始容量。如果块大小固定，这将是块的大小。
     *                        必须为2或更大。
     */
    public BaseMpscLinkedArrayQueue(final int initialCapacity)
    {
        RangeUtil.checkGreaterThanOrEqual(initialCapacity, 2, "initialCapacity");

        int p2capacity = Pow2.roundToPowerOfTwo(initialCapacity);
        // leave lower bit of mask clear
        // 保留掩码的低位
        long mask = (p2capacity - 1) << 1;
        // need extra element to point at next array
        // 需要额外的元素指向下一个数组
        E[] buffer = allocateRefArray(p2capacity + 1);
        producerBuffer = buffer;
        producerMask = mask;
        consumerBuffer = buffer;
        consumerMask = mask;
        soProducerLimit(mask); // we know it's all empty to start with
    }

    @Override
    public int size()
    {
        // NOTE: because indices are on even numbers we cannot use the size util.
        // 注意：因为索引在偶数上，我们不能使用 size 工具。

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
        long after = lvConsumerIndex();
        long size;
        while (true)
        {
            final long before = after;
            final long currentProducerIndex = lvProducerIndex();
            after = lvConsumerIndex();
            if (before == after)
            {
                size = ((currentProducerIndex - after) >> 1);
                break;
            }
        }
        // Long overflow is impossible, so size is always positive. Integer overflow is possible for the unbounded
        // 长整型溢出是不可能的，所以大小总是正数。对于无界的情况，整型溢出是可能的。
        // indexed queues.
        // 索引队列。
        if (size > Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        else
        {
            return (int) size;
        }
    }

    @Override
    public boolean isEmpty()
    {
        // Order matters!
        // 顺序很重要！
        // Loading consumer before producer allows for producer increments after consumer index is read.
        // 在生产者之前加载消费者允许在消费者索引读取后进行生产者增量。
        // This ensures this method is conservative in it's estimate. Note that as this is an MPMC there is
        // 这确保了该方法在估算时是保守的。请注意，由于这是一个MPMC，
        // nothing we can do to make this an exact method.
        // 我们无法将其变成一个精确的方法。
        return (this.lvConsumerIndex() == this.lvProducerIndex());
    }

    @Override
    public String toString()
    {
        return this.getClass().getName();
    }

    @Override
    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException();
        }

        long mask;
        E[] buffer;
        long pIndex;

        while (true)
        {
            long producerLimit = lvProducerLimit();
            pIndex = lvProducerIndex();
            // lower bit is indicative of resize, if we see it we spin until it's cleared
            // 低位表示调整大小，如果看到它，我们旋转直到它被清除
            if ((pIndex & 1) == 1)
            {
                continue;
            }
            // pIndex is even (lower bit is 0) -> actual index is (pIndex >> 1)
            // pIndex 是偶数（最低位为 0） -> 实际索引为 (pIndex >> 1)

            // mask/buffer may get changed by resizing -> only use for array access after successful CAS.

            // mask/buffer 可能会因调整大小而改变 -> 仅在 CAS 成功后用于数组访问。
            mask = this.producerMask;
            buffer = this.producerBuffer;
            // a successful CAS ties the ordering, lv(pIndex) - [mask/buffer] -> cas(pIndex)
            // 一个成功的CAS将绑定顺序，lv(pIndex) - [掩码/缓冲区] -> cas(pIndex)

            // assumption behind this optimization is that queue is almost always empty or near empty

            // 该优化的假设是队列几乎总是为空或接近为空
            if (producerLimit <= pIndex)
            {
                int result = offerSlowPath(mask, pIndex, producerLimit);
                switch (result)
                {
                    case CONTINUE_TO_P_INDEX_CAS:
                        break;
                    case RETRY:
                        continue;
                    case QUEUE_FULL:
                        return false;
                    case QUEUE_RESIZE:
                        resize(mask, buffer, pIndex, e, null);
                        return true;
                }
            }

            if (casProducerIndex(pIndex, pIndex + 2))
            {
                break;
            }
        }
        // INDEX visible before ELEMENT
        // 索引在元素之前可见
        final long offset = modifiedCalcCircularRefElementOffset(pIndex, mask);
        soRefElement(buffer, offset, e); // release element e
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 此实现仅适用于单消费者线程使用。
     */
    @SuppressWarnings("unchecked")
    @Override
    public E poll()
    {
        final E[] buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final long mask = consumerMask;

        final long offset = modifiedCalcCircularRefElementOffset(index, mask);
        Object e = lvRefElement(buffer, offset);
        if (e == null)
        {
            if (index != lvProducerIndex())
            {
                // poll() == null iff queue is empty, null element is not strong enough indicator, so we must
                // poll() == null 当且仅当队列为空，null 元素不足以作为强指示器，因此我们必须
                // check the producer index. If the queue is indeed not empty we spin until element is
                // 检查生产者索引。如果队列确实不为空，我们自旋直到元素被处理
                // visible.
                // 可见的。
                do
                {
                    e = lvRefElement(buffer, offset);
                }
                while (e == null);
            }
            else
            {
                return null;
            }
        }

        if (e == JUMP)
        {
            final E[] nextBuffer = nextBuffer(buffer, mask);
            return newBufferPoll(nextBuffer, index);
        }

        soRefElement(buffer, offset, null); // release element null
        soConsumerIndex(index + 2); // release cIndex
        return (E) e;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 此实现仅适用于单消费者线程使用。
     */
    @SuppressWarnings("unchecked")
    @Override
    public E peek()
    {
        final E[] buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final long mask = consumerMask;

        final long offset = modifiedCalcCircularRefElementOffset(index, mask);
        Object e = lvRefElement(buffer, offset);
        if (e == null && index != lvProducerIndex())
        {
            // peek() == null iff queue is empty, null element is not strong enough indicator, so we must
            // peek() == null 当且仅当队列为空，null 元素不足以作为强指示器，因此我们必须
            // check the producer index. If the queue is indeed not empty we spin until element is visible.
            // 检查生产者索引。如果队列确实不为空，我们自旋直到元素可见。
            do
            {
                e = lvRefElement(buffer, offset);
            }
            while (e == null);
        }
        if (e == JUMP)
        {
            return newBufferPeek(nextBuffer(buffer, mask), index);
        }
        return (E) e;
    }

    /**
     * We do not inline resize into this method because we do not resize on fill.
     */

    /**
     * 我们不将resize内联到此方法中，因为我们不在填充时进行resize。
     */
    private int offerSlowPath(long mask, long pIndex, long producerLimit)
    {
        final long cIndex = lvConsumerIndex();
        long bufferCapacity = getCurrentBufferCapacity(mask);

        if (cIndex + bufferCapacity > pIndex)
        {
            if (!casProducerLimit(producerLimit, cIndex + bufferCapacity))
            {
                // retry from top
                // 重试从顶部开始
                return RETRY;
            }
            else
            {
                // continue to pIndex CAS
                // 继续到 pIndex CAS
                return CONTINUE_TO_P_INDEX_CAS;
            }
        }
        // full and cannot grow
        // 已满且无法增长
        else if (availableInQueue(pIndex, cIndex) <= 0)
        {
            // offer should return false;
            // offer 应该返回 false;
            return QUEUE_FULL;
        }
        // grab index for resize -> set lower bit
        // 获取调整大小的索引 -> 设置低位
        else if (casProducerIndex(pIndex, pIndex + 1))
        {
            // trigger a resize
            // 触发调整大小
            return QUEUE_RESIZE;
        }
        else
        {
            // failed resize attempt, retry from top
            // 调整大小尝试失败，从顶部重试
            return RETRY;
        }
    }

    /**
     * @return available elements in queue * 2
     */

    /**
     * @return 队列中可用元素的数量 * 2
     */
    protected abstract long availableInQueue(long pIndex, long cIndex);

    @SuppressWarnings("unchecked")
    private E[] nextBuffer(final E[] buffer, final long mask)
    {
        final long offset = nextArrayOffset(mask);
        final E[] nextBuffer = (E[]) lvRefElement(buffer, offset);
        consumerBuffer = nextBuffer;
        consumerMask = (length(nextBuffer) - 2) << 1;
        soRefElement(buffer, offset, BUFFER_CONSUMED);
        return nextBuffer;
    }

    private static long nextArrayOffset(long mask)
    {
        return modifiedCalcCircularRefElementOffset(mask + 2, Long.MAX_VALUE);
    }

    private E newBufferPoll(E[] nextBuffer, long index)
    {
        final long offset = modifiedCalcCircularRefElementOffset(index, consumerMask);
        final E n = lvRefElement(nextBuffer, offset);
        if (n == null)
        {
            throw new IllegalStateException("new buffer must have at least one element");
        }
        soRefElement(nextBuffer, offset, null);
        soConsumerIndex(index + 2);
        return n;
    }

    private E newBufferPeek(E[] nextBuffer, long index)
    {
        final long offset = modifiedCalcCircularRefElementOffset(index, consumerMask);
        final E n = lvRefElement(nextBuffer, offset);
        if (null == n)
        {
            throw new IllegalStateException("new buffer must have at least one element");
        }
        return n;
    }

    @Override
    public long currentProducerIndex()
    {
        return lvProducerIndex() / 2;
    }

    @Override
    public long currentConsumerIndex()
    {
        return lvConsumerIndex() / 2;
    }

    @Override
    public abstract int capacity();

    @Override
    public boolean relaxedOffer(E e)
    {
        return offer(e);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E relaxedPoll()
    {
        final E[] buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final long mask = consumerMask;

        final long offset = modifiedCalcCircularRefElementOffset(index, mask);
        Object e = lvRefElement(buffer, offset);
        if (e == null)
        {
            return null;
        }
        if (e == JUMP)
        {
            final E[] nextBuffer = nextBuffer(buffer, mask);
            return newBufferPoll(nextBuffer, index);
        }
        soRefElement(buffer, offset, null);
        soConsumerIndex(index + 2);
        return (E) e;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E relaxedPeek()
    {
        final E[] buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final long mask = consumerMask;

        final long offset = modifiedCalcCircularRefElementOffset(index, mask);
        Object e = lvRefElement(buffer, offset);
        if (e == JUMP)
        {
            return newBufferPeek(nextBuffer(buffer, mask), index);
        }
        return (E) e;
    }

    @Override
    public int fill(Supplier<E> s)
    {
        long result = 0;// result is a long because we want to have a safepoint check at regular intervals
        final int capacity = capacity();
        do
        {
            final int filled = fill(s, PortableJvmInfo.RECOMENDED_OFFER_BATCH);
            if (filled == 0)
            {
                return (int) result;
            }
            result += filled;
        }
        while (result <= capacity);
        return (int) result;
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

        long mask;
        E[] buffer;
        long pIndex;
        int claimedSlots;
        while (true)
        {
            long producerLimit = lvProducerLimit();
            pIndex = lvProducerIndex();
            // lower bit is indicative of resize, if we see it we spin until it's cleared
            // 低位表示调整大小，如果看到它，我们旋转直到它被清除
            if ((pIndex & 1) == 1)
            {
                continue;
            }
            // pIndex is even (lower bit is 0) -> actual index is (pIndex >> 1)
            // pIndex 是偶数（最低位为 0） -> 实际索引为 (pIndex >> 1)

            // NOTE: mask/buffer may get changed by resizing -> only use for array access after successful CAS.

            // 注意：mask/buffer 可能会因为调整大小而改变 -> 仅在成功 CAS 后用于数组访问。
            // Only by virtue offloading them between the lvProducerIndex and a successful casProducerIndex are they
            // 只有通过将它们从 lvProducerIndex 卸载并成功执行 casProducerIndex，它们才
            // safe to use.
            // 安全使用。
            mask = this.producerMask;
            buffer = this.producerBuffer;
            // a successful CAS ties the ordering, lv(pIndex) -> [mask/buffer] -> cas(pIndex)
            // 一个成功的CAS将顺序绑定，lv(pIndex) -> [mask/buffer] -> cas(pIndex)

            // we want 'limit' slots, but will settle for whatever is visible to 'producerLimit'

            // 我们希望有 'limit' 个槽位，但会接受 'producerLimit' 可见的任何数量
            long batchIndex = Math.min(producerLimit, pIndex + 2l * limit); //  -> producerLimit >= batchIndex

            if (pIndex >= producerLimit)
            {
                int result = offerSlowPath(mask, pIndex, producerLimit);
                switch (result)
                {
                    case CONTINUE_TO_P_INDEX_CAS:
                        // offer slow path verifies only one slot ahead, we cannot rely on indication here
                        // 提供慢路径仅验证一个槽位，我们不能依赖此处的指示
                    case RETRY:
                        continue;
                    case QUEUE_FULL:
                        return 0;
                    case QUEUE_RESIZE:
                        resize(mask, buffer, pIndex, null, s);
                        return 1;
                }
            }

            // claim limit slots at once

            // 一次声明限制槽位
            if (casProducerIndex(pIndex, batchIndex))
            {
                claimedSlots = (int) ((batchIndex - pIndex) / 2);
                break;
            }
        }

        for (int i = 0; i < claimedSlots; i++)
        {
            final long offset = modifiedCalcCircularRefElementOffset(pIndex + 2l * i, mask);
            soRefElement(buffer, offset, s.get());
        }
        return claimedSlots;
    }

    @Override
    public void fill(Supplier<E> s, WaitStrategy wait, ExitCondition exit)
    {
        MessagePassingQueueUtil.fill(this, s, wait, exit);
    }
    @Override
    public int drain(Consumer<E> c)
    {
        return drain(c, capacity());
    }

    @Override
    public int drain(Consumer<E> c, int limit)
    {
        return MessagePassingQueueUtil.drain(this, c, limit);
    }

    @Override
    public void drain(Consumer<E> c, WaitStrategy wait, ExitCondition exit)
    {
        MessagePassingQueueUtil.drain(this, c, wait, exit);
    }

    /**
     * Get an iterator for this queue. This method is thread safe.
     * <p>
     * The iterator provides a best-effort snapshot of the elements in the queue.
     * The returned iterator is not guaranteed to return elements in queue order,
     * and races with the consumer thread may cause gaps in the sequence of returned elements.
     * Like {link #relaxedPoll}, the iterator may not immediately return newly inserted elements.
     *
     * @return The iterator.
     */

    /**
     * 获取此队列的迭代器。此方法是线程安全的。
     * <p>
     * 迭代器提供队列中元素的最佳努力快照。
     * 返回的迭代器不保证按队列顺序返回元素，
     * 并且与消费者线程的竞争可能导致返回元素序列中的间隙。
     * 与 {link #relaxedPoll} 类似，迭代器可能不会立即返回新插入的元素。
     *
     * @return 迭代器。
     */
    @Override
    public Iterator<E> iterator() {
        return new WeakIterator(consumerBuffer, lvConsumerIndex(), lvProducerIndex());
    }

    private static class WeakIterator<E> implements Iterator<E>
    {
        private final long pIndex;
        private long nextIndex;
        private E nextElement;
        private E[] currentBuffer;
        private int mask;

        WeakIterator(E[] currentBuffer, long cIndex, long pIndex)
        {
            this.pIndex = pIndex >> 1;
            this.nextIndex = cIndex >> 1;
            setBuffer(currentBuffer);
            nextElement = getNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public boolean hasNext()
        {
            return nextElement != null;
        }

        @Override
        public E next()
        {
            final E e = nextElement;
            if (e == null)
            {
                throw new NoSuchElementException();
            }
            nextElement = getNext();
            return e;
        }

        private void setBuffer(E[] buffer)
        {
            this.currentBuffer = buffer;
            this.mask = length(buffer) - 2;
        }

        private E getNext()
        {
            while (nextIndex < pIndex)
            {
                long index = nextIndex++;
                E e = lvRefElement(currentBuffer, calcCircularRefElementOffset(index, mask));
                // skip removed/not yet visible elements
                // 跳过已移除/尚未可见的元素
                if (e == null)
                {
                    continue;
                }

                // not null && not JUMP -> found next element

                // 非空 && 非跳转 -> 找到下一个元素
                if (e != JUMP)
                {
                    return e;
                }

                // need to jump to the next buffer

                // 需要跳转到下一个缓冲区
                int nextBufferIndex = mask + 1;
                Object nextBuffer = lvRefElement(currentBuffer,
                                              calcRefElementOffset(nextBufferIndex));

                if (nextBuffer == BUFFER_CONSUMED || nextBuffer == null)
                {
                    // Consumer may have passed us, or the next buffer is not visible yet: drop out early
                    // 消费者可能已经处理过，或者下一个缓冲区还未可见：提前退出
                    return null;
                }

                setBuffer((E[]) nextBuffer);
                // now with the new array retry the load, it can't be a JUMP, but we need to repeat same index
                // 现在使用新的数组重试加载，它不能是跳转，但我们需要重复相同的索引
                e = lvRefElement(currentBuffer, calcCircularRefElementOffset(index, mask));
                // skip removed/not yet visible elements
                // 跳过已移除/尚未可见的元素
                if (e == null)
                {
                    continue;
                }
                else
                {
                    return e;
                }

            }
            return null;
        }
    }

    private void resize(long oldMask, E[] oldBuffer, long pIndex, E e, Supplier<E> s)
    {
        assert (e != null && s == null) || (e == null || s != null);
        int newBufferLength = getNextBufferSize(oldBuffer);
        final E[] newBuffer;
        try
        {
            newBuffer = allocateRefArray(newBufferLength);
        }
        catch (OutOfMemoryError oom)
        {
            assert lvProducerIndex() == pIndex + 1;
            soProducerIndex(pIndex);
            throw oom;
        }

        producerBuffer = newBuffer;
        final int newMask = (newBufferLength - 2) << 1;
        producerMask = newMask;

        final long offsetInOld = modifiedCalcCircularRefElementOffset(pIndex, oldMask);
        final long offsetInNew = modifiedCalcCircularRefElementOffset(pIndex, newMask);

        soRefElement(newBuffer, offsetInNew, e == null ? s.get() : e);// element in new array
        soRefElement(oldBuffer, nextArrayOffset(oldMask), newBuffer);// buffer linked

        // ASSERT code

        // 断言代码
        final long cIndex = lvConsumerIndex();
        final long availableInQueue = availableInQueue(pIndex, cIndex);
        RangeUtil.checkPositive(availableInQueue, "availableInQueue");

        // Invalidate racing CASs

        // 使竞态CAS失效
        // We never set the limit beyond the bounds of a buffer
        // 我们从未将限制设置为超出缓冲区的边界
        soProducerLimit(pIndex + Math.min(newMask, availableInQueue));

        // make resize visible to the other producers

        // 使调整大小对其他生产者可见
        soProducerIndex(pIndex + 2);

        // INDEX visible before ELEMENT, consistent with consumer expectation

        // INDEX 在 ELEMENT 之前可见，与消费者预期一致

        // make resize visible to consumer

        // 使调整大小对消费者可见
        soRefElement(oldBuffer, offsetInOld, JUMP);
    }

    /**
     * @return next buffer size(inclusive of next array pointer)
     */

    /**
     * @return 下一个缓冲区大小（包含下一个数组指针）
     */
    protected abstract int getNextBufferSize(E[] buffer);

    /**
     * @return current buffer capacity for elements (excluding next pointer and jump entry) * 2
     */

    /**
     * @return 当前缓冲区元素容量（不包括下一个指针和跳转条目） * 2
     */
    protected abstract long getCurrentBufferCapacity(long mask);
}
