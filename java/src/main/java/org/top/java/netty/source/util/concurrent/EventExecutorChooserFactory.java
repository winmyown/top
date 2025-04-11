
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.UnstableApi;

/**
 * Factory that creates new {@link EventExecutorChooser}s.
 */

/**
 * 创建新 {@link EventExecutorChooser} 的工厂。
 */
@UnstableApi
public interface EventExecutorChooserFactory {

    /**
     * Returns a new {@link EventExecutorChooser}.
     */

    /**
     * 返回一个新的 {@link EventExecutorChooser}。
     */
    EventExecutorChooser newChooser(EventExecutor[] executors);

    /**
     * Chooses the next {@link EventExecutor} to use.
     */

    /**
     * 选择下一个要使用的 {@link EventExecutor}。
     */
    @UnstableApi
    interface EventExecutorChooser {

        /**
         * Returns the new {@link EventExecutor} to use.
         */

        /**
         * 返回要使用的新 {@link EventExecutor}。
         */
        EventExecutor next();
    }
}
