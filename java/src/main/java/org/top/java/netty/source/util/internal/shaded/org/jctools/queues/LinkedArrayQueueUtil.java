package org.top.java.netty.source.util.internal.shaded.org.jctools.queues;

import org.top.java.netty.source.util.internal.shaded.org.jctools.util.InternalAPI;

import static io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess.REF_ARRAY_BASE;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess.REF_ELEMENT_SHIFT;

/**
 * This is used for method substitution in the LinkedArray classes code generation.
 */

/**
 * 这用于在LinkedArray类的代码生成中进行方法替换。
 */
@InternalAPI
final class LinkedArrayQueueUtil
{
    static int length(Object[] buf)
    {
        return buf.length;
    }

    /**
     * This method assumes index is actually (index << 1) because lower bit is
     * used for resize. This is compensated for by reducing the element shift.
     * The computation is constant folded, so there's no cost.
     */

    /**
     * 该方法假设索引实际上是 (index << 1)，因为最低位用于扩容。
     * 通过减少元素位移来补偿这一点。计算是常量折叠的，因此没有额外开销。
     */
    static long modifiedCalcCircularRefElementOffset(long index, long mask)
    {
        return REF_ARRAY_BASE + ((index & mask) << (REF_ELEMENT_SHIFT - 1));
    }

    static long nextArrayOffset(Object[] curr)
    {
        return REF_ARRAY_BASE + ((long) (length(curr) - 1) << REF_ELEMENT_SHIFT);
    }
}
