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
package org.top.java.netty.source.util.internal.shaded.org.jctools.queues.atomic;

import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.IndexedQueueSizeUtil.IndexedQueue;
import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.IndexedQueueSizeUtil;
import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.QueueProgressIndicators;
import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.SupportsIterator;
import org.top.java.netty.source.util.internal.shaded.org.jctools.util.Pow2;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.top.java.netty.source.util.internal.shaded.org.jctools.queues.atomic.AtomicQueueUtil.*;

abstract class AtomicReferenceArrayQueue<E> extends AbstractQueue<E> implements IndexedQueue, QueueProgressIndicators, MessagePassingQueue<E>, SupportsIterator
{
    protected final AtomicReferenceArray<E> buffer;
    protected final int mask;

    public AtomicReferenceArrayQueue(int capacity)
    {
        int actualCapacity = Pow2.roundToPowerOfTwo(capacity);
        this.mask = actualCapacity - 1;
        this.buffer = new AtomicReferenceArray<E>(actualCapacity);
    }

    @Override
    public String toString()
    {
        return this.getClass().getName();
    }

    @Override
    public void clear()
    {
        while (poll() != null)
        {
            // toss it away
            // 把它扔掉
        }
    }

    @Override
    public final int capacity()
    {
        return (int) (mask + 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     */

    /**
     * {@inheritDoc}
     * <p>
     */
    @Override
    public final int size()
    {
        return IndexedQueueSizeUtil.size(this);
    }

    @Override
    public final boolean isEmpty()
    {
        return IndexedQueueSizeUtil.isEmpty(this);
    }

    @Override
    public final long currentProducerIndex()
    {
        return lvProducerIndex();
    }

    @Override
    public final long currentConsumerIndex()
    {
        return lvConsumerIndex();
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
    public final Iterator<E> iterator() {
        final long cIndex = lvConsumerIndex();
        final long pIndex = lvProducerIndex();

        return new WeakIterator(cIndex, pIndex, mask, buffer);
    }

    private static class WeakIterator<E> implements Iterator<E> {

        private final long pIndex;
        private final int mask;
        private final AtomicReferenceArray<E> buffer;
        private long nextIndex;
        private E nextElement;

        WeakIterator(long cIndex, long pIndex, int mask, AtomicReferenceArray<E> buffer) {
            this.nextIndex = cIndex;
            this.pIndex = pIndex;
            this.mask = mask;
            this.buffer = buffer;
            nextElement = getNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public E next() {
            final E e = nextElement;
            if (e == null)
                throw new NoSuchElementException();
            nextElement = getNext();
            return e;
        }

        private E getNext() {
            final int mask = this.mask;
            final AtomicReferenceArray<E> buffer = this.buffer;
            while (nextIndex < pIndex) {
                int offset = calcCircularRefElementOffset(nextIndex++, mask);
                E e = lvRefElement(buffer, offset);
                if (e != null) {
                    return e;
                }
            }
            return null;
        }
    }
}
