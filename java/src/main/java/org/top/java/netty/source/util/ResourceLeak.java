

package org.top.java.netty.source.util;

/**
 * @deprecated please use {@link ResourceLeakTracker} as it may lead to false-positives.
 */

/**
 * @deprecated 请使用 {@link ResourceLeakTracker}，因为它可能导致误报。
 */
@Deprecated
public interface ResourceLeak {
    /**
     * Records the caller's current stack trace so that the {@link ResourceLeakDetector} can tell where the leaked
     * resource was accessed lastly. This method is a shortcut to {@link #record(Object) record(null)}.
     */
    /**
     * 记录调用者当前的堆栈跟踪，以便 {@link ResourceLeakDetector} 可以知道泄漏的资源最后一次是在哪里被访问的。此方法是 {@link #record(Object) record(null)} 的快捷方式。
     */
    void record();

    /**
     * Records the caller's current stack trace and the specified additional arbitrary information
     * so that the {@link ResourceLeakDetector} can tell where the leaked resource was accessed lastly.
     */

    /**
     * 记录调用者当前的堆栈跟踪和指定的附加任意信息，
     * 以便 {@link ResourceLeakDetector} 可以告知泄漏资源最后被访问的位置。
     */
    void record(Object hint);

    /**
     * Close the leak so that {@link ResourceLeakDetector} does not warn about leaked resources.
     *
     * @return {@code true} if called first time, {@code false} if called already
     */

    /**
     * 关闭泄漏，以便 {@link ResourceLeakDetector} 不会警告资源泄漏。
     *
     * @return {@code true} 如果第一次调用，{@code false} 如果已经调用过
     */
    boolean close();
}
