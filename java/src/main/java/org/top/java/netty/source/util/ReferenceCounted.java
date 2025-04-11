
package org.top.java.netty.source.util;

/**
 * A reference-counted object that requires explicit deallocation.
 * <p>
 * When a new {@link ReferenceCounted} is instantiated, it starts with the reference count of {@code 1}.
 * {@link #retain()} increases the reference count, and {@link #release()} decreases the reference count.
 * If the reference count is decreased to {@code 0}, the object will be deallocated explicitly, and accessing
 * the deallocated object will usually result in an access violation.
 * </p>
 * <p>
 * If an object that implements {@link ReferenceCounted} is a container of other objects that implement
 * {@link ReferenceCounted}, the contained objects will also be released via {@link #release()} when the container's
 * reference count becomes 0.
 * </p>
 */

/**
 * 一个需要显式释放的引用计数对象。
 * <p>
 * 当一个新的 {@link ReferenceCounted} 被实例化时，它的引用计数从 {@code 1} 开始。
 * {@link #retain()} 会增加引用计数，而 {@link #release()} 会减少引用计数。
 * 如果引用计数减少到 {@code 0}，对象将被显式释放，访问已释放的对象通常会导致访问违规。
 * </p>
 * <p>
 * 如果一个实现了 {@link ReferenceCounted} 的对象是其他实现了 {@link ReferenceCounted} 的对象的容器，
 * 当容器的引用计数变为 0 时，包含的对象也将通过 {@link #release()} 被释放。
 * </p>
 */
public interface ReferenceCounted {
    /**
     * Returns the reference count of this object.  If {@code 0}, it means this object has been deallocated.
     */
    /**
     * 返回此对象的引用计数。如果为{@code 0}，则表示此对象已被释放。
     */
    int refCnt();

    /**
     * Increases the reference count by {@code 1}.
     */

    /**
     * 将引用计数增加 {@code 1}。
     */
    ReferenceCounted retain();

    /**
     * Increases the reference count by the specified {@code increment}.
     */

    /**
     * 按指定的 {@code increment} 增加引用计数。
     */
    ReferenceCounted retain(int increment);

    /**
     * Records the current access location of this object for debugging purposes.
     * If this object is determined to be leaked, the information recorded by this operation will be provided to you
     * via {@link ResourceLeakDetector}.  This method is a shortcut to {@link #touch(Object) touch(null)}.
     */

    /**
     * 记录此对象的当前访问位置以用于调试目的。
     * 如果确定此对象已泄漏，此操作记录的信息将通过 {@link ResourceLeakDetector} 提供给您。
     * 此方法是 {@link #touch(Object) touch(null)} 的快捷方式。
     */
    ReferenceCounted touch();

    /**
     * Records the current access location of this object with an additional arbitrary information for debugging
     * purposes.  If this object is determined to be leaked, the information recorded by this operation will be
     * provided to you via {@link ResourceLeakDetector}.
     */

    /**
     * 记录此对象的当前访问位置以及额外的任意信息，用于调试目的。如果确定此对象已泄漏，
     * 则通过 {@link ResourceLeakDetector} 提供此操作记录的信息。
     */
    ReferenceCounted touch(Object hint);

    /**
     * Decreases the reference count by {@code 1} and deallocates this object if the reference count reaches at
     * {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */

    /**
     * 将引用计数减少 {@code 1}，如果引用计数达到 {@code 0}，则释放此对象。
     *
     * @return {@code true} 当且仅当引用计数变为 {@code 0} 并且此对象已被释放
     */
    boolean release();

    /**
     * Decreases the reference count by the specified {@code decrement} and deallocates this object if the reference
     * count reaches at {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */

    /**
     * 将引用计数减少指定的 {@code decrement}，如果引用计数达到 {@code 0}，则释放此对象。
     *
     * @return 当且仅当引用计数变为 {@code 0} 并且此对象已被释放时返回 {@code true}
     */
    boolean release(int decrement);
}
