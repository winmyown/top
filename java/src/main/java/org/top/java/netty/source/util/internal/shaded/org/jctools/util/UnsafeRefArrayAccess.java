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
package org.top.java.netty.source.util.internal.shaded.org.jctools.util;

import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.UNSAFE;

@InternalAPI
public final class UnsafeRefArrayAccess
{
    public static final long REF_ARRAY_BASE;
    public static final int REF_ELEMENT_SHIFT;

    static
    {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale)
        {
            REF_ELEMENT_SHIFT = 2;
        }
        else if (8 == scale)
        {
            REF_ELEMENT_SHIFT = 3;
        }
        else
        {
            throw new IllegalStateException("Unknown pointer size: " + scale);
        }
        REF_ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class);
    }

    /**
     * A plain store (no ordering/fences) of an element to a given offset
     *
     * @param buffer this.buffer
     * @param offset computed via {@link UnsafeRefArrayAccess#calcRefElementOffset(long)}
     * @param e      an orderly kitty
     */

    /**
     * 将元素普通存储（无排序/栅栏）到给定偏移量
     *
     * @param buffer this.buffer
     * @param offset 通过 {@link UnsafeRefArrayAccess#calcRefElementOffset(long)} 计算
     * @param e      一只有序的猫咪
     */
    public static <E> void spRefElement(E[] buffer, long offset, E e)
    {
        UNSAFE.putObject(buffer, offset, e);
    }

    /**
     * An ordered store of an element to a given offset
     *
     * @param buffer this.buffer
     * @param offset computed via {@link UnsafeRefArrayAccess#calcCircularRefElementOffset}
     * @param e      an orderly kitty
     */

    /**
     * 将元素有序存储到指定偏移量
     *
     * @param buffer this.buffer
     * @param offset 通过 {@link UnsafeRefArrayAccess#calcCircularRefElementOffset} 计算得出
     * @param e      一只有序的小猫
     */
    public static <E> void soRefElement(E[] buffer, long offset, E e)
    {
        UNSAFE.putOrderedObject(buffer, offset, e);
    }

    /**
     * A plain load (no ordering/fences) of an element from a given offset.
     *
     * @param buffer this.buffer
     * @param offset computed via {@link UnsafeRefArrayAccess#calcRefElementOffset(long)}
     * @return the element at the offset
     */

    /**
     * 从给定偏移量处进行普通加载（无排序/栅栏）。
     *
     * @param buffer this.buffer
     * @param offset 通过 {@link UnsafeRefArrayAccess#calcRefElementOffset(long)} 计算得出
     * @return 偏移量处的元素
     */
    @SuppressWarnings("unchecked")
    public static <E> E lpRefElement(E[] buffer, long offset)
    {
        return (E) UNSAFE.getObject(buffer, offset);
    }

    /**
     * A volatile load of an element from a given offset.
     *
     * @param buffer this.buffer
     * @param offset computed via {@link UnsafeRefArrayAccess#calcRefElementOffset(long)}
     * @return the element at the offset
     */

    /**
     * 从给定偏移量处对元素进行volatile加载。
     *
     * @param buffer this.buffer
     * @param offset 通过 {@link UnsafeRefArrayAccess#calcRefElementOffset(long)} 计算
     * @return 偏移量处的元素
     */
    @SuppressWarnings("unchecked")
    public static <E> E lvRefElement(E[] buffer, long offset)
    {
        return (E) UNSAFE.getObjectVolatile(buffer, offset);
    }

    /**
     * @param index desirable element index
     * @return the offset in bytes within the array for a given index
     */

    /**
     * @param index 所需元素的索引
     * @return 给定索引在数组中的字节偏移量
     */
    public static long calcRefElementOffset(long index)
    {
        return REF_ARRAY_BASE + (index << REF_ELEMENT_SHIFT);
    }

    /**
     * Note: circular arrays are assumed a power of 2 in length and the `mask` is (length - 1).
     *
     * @param index desirable element index
     * @param mask (length - 1)
     * @return the offset in bytes within the circular array for a given index
     */

    /**
     * 注意：循环数组的长度假定为2的幂，`mask`为（长度 - 1）。
     *
     * @param index 期望的元素索引
     * @param mask （长度 - 1）
     * @return 给定索引在循环数组中的字节偏移量
     */
    public static long calcCircularRefElementOffset(long index, long mask)
    {
        return REF_ARRAY_BASE + ((index & mask) << REF_ELEMENT_SHIFT);
    }

    /**
     * This makes for an easier time generating the atomic queues, and removes some warnings.
     */

    /**
     * 这使得生成原子队列更加容易，并消除了一些警告。
     */
    @SuppressWarnings("unchecked")
    public static <E> E[] allocateRefArray(int capacity)
    {
        return (E[]) new Object[capacity];
    }
}
