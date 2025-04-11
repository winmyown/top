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

import org.top.java.netty.source.util.internal.shaded.org.jctools.util.Pow2;
import org.top.java.netty.source.util.internal.shaded.org.jctools.util.RangeUtil;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.top.java.netty.source.util.internal.shaded.org.jctools.queues.LinkedArrayQueueUtil.length;
import static io.netty.util.internal.shaded.org.jctools.util.Pow2.roundToPowerOfTwo;

abstract class MpscChunkedArrayQueueColdProducerFields<E> extends BaseMpscLinkedArrayQueue<E>
{
    protected final long maxQueueCapacity;

    MpscChunkedArrayQueueColdProducerFields(int initialCapacity, int maxCapacity)
    {
        super(initialCapacity);
        RangeUtil.checkGreaterThanOrEqual(maxCapacity, 4, "maxCapacity");
        RangeUtil.checkLessThan(roundToPowerOfTwo(initialCapacity), roundToPowerOfTwo(maxCapacity),
            "initialCapacity");
        maxQueueCapacity = ((long) Pow2.roundToPowerOfTwo(maxCapacity)) << 1;
    }
}

/**
 * An MPSC array queue which starts at <i>initialCapacity</i> and grows to <i>maxCapacity</i> in linked chunks
 * of the initial size. The queue grows only when the current chunk is full and elements are not copied on
 * resize, instead a link to the new chunk is stored in the old chunk for the consumer to follow.
 */

/**
 * 一个MPSC数组队列，从<i>initialCapacity</i>开始，并以初始大小的链接块增长到<i>maxCapacity</i>。
 * 队列仅在当前块满时增长，并且在调整大小时不会复制元素，而是在旧块中存储指向新块的链接以供消费者跟随。
 */
public class MpscChunkedArrayQueue<E> extends MpscChunkedArrayQueueColdProducerFields<E>
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

    public MpscChunkedArrayQueue(int maxCapacity)
    {
        super(max(2, min(1024, roundToPowerOfTwo(maxCapacity / 8))), maxCapacity);
    }

    /**
     * @param initialCapacity the queue initial capacity. If chunk size is fixed this will be the chunk size.
     *                        Must be 2 or more.
     * @param maxCapacity     the maximum capacity will be rounded up to the closest power of 2 and will be the
     *                        upper limit of number of elements in this queue. Must be 4 or more and round up to a larger
     *                        power of 2 than initialCapacity.
     */

    /**
     * @param initialCapacity 队列的初始容量。如果块大小固定，这将是块大小。
     *                        必须为2或更大。
     * @param maxCapacity     最大容量将被舍入到最接近的2的幂，并且将是此队列中元素数量的上限。
     *                        必须为4或更大，并且舍入到比initialCapacity更大的2的幂。
     */
    public MpscChunkedArrayQueue(int initialCapacity, int maxCapacity)
    {
        super(initialCapacity, maxCapacity);
    }

    @Override
    protected long availableInQueue(long pIndex, long cIndex)
    {
        return maxQueueCapacity - (pIndex - cIndex);
    }

    @Override
    public int capacity()
    {
        return (int) (maxQueueCapacity / 2);
    }

    @Override
    protected int getNextBufferSize(E[] buffer)
    {
        return length(buffer);
    }

    @Override
    protected long getCurrentBufferCapacity(long mask)
    {
        return mask;
    }
}
