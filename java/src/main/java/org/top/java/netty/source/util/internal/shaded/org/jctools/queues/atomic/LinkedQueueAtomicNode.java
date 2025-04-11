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

import java.util.concurrent.atomic.AtomicReference;

public final class LinkedQueueAtomicNode<E> extends AtomicReference<LinkedQueueAtomicNode<E>>
{
    /** */
    /** */
    private static final long serialVersionUID = 2404266111789071508L;
    private E value;

    LinkedQueueAtomicNode()
    {
    }

    LinkedQueueAtomicNode(E val)
    {
        spValue(val);
    }

    /**
     * Gets the current value and nulls out the reference to it from this node.
     *
     * @return value
     */

    /**
     * 获取当前值并将此节点对它的引用置为空。
     *
     * @return 值
     */
    public E getAndNullValue()
    {
        E temp = lpValue();
        spValue(null);
        return temp;
    }

    public E lpValue()
    {
        return value;
    }

    public void spValue(E newValue)
    {
        value = newValue;
    }

    public void soNext(LinkedQueueAtomicNode<E> n)
    {
        lazySet(n);
    }

    public void spNext(LinkedQueueAtomicNode<E> n)
    {
        lazySet(n);
    }

    public LinkedQueueAtomicNode<E> lvNext()
    {
        return get();
    }
}
