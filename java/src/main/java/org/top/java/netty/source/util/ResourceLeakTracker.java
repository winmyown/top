
package org.top.java.netty.source.util;

public interface ResourceLeakTracker<T>  {

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
     * Close the leak so that {@link ResourceLeakTracker} does not warn about leaked resources.
     * After this method is called a leak associated with this ResourceLeakTracker should not be reported.
     *
     * @return {@code true} if called first time, {@code false} if called already
     */

    /**
     * 关闭泄漏，以便 {@link ResourceLeakTracker} 不会警告有关资源泄漏的问题。
     * 调用此方法后，与此 ResourceLeakTracker 关联的泄漏不应再被报告。
     *
     * @return {@code true} 如果是第一次调用，{@code false} 如果已经调用过
     */
    boolean close(T trackedObject);
}
