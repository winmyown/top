
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.ObjectUtil;

import java.util.concurrent.Executor;

/**
 * {@link Executor} which execute tasks in the callers thread.
 */

/**
 * {@link Executor} 在调用者线程中执行任务。
 */
public final class ImmediateExecutor implements Executor {
    public static final ImmediateExecutor INSTANCE = new ImmediateExecutor();

    private ImmediateExecutor() {
        // use static instance
        // 使用静态实例
    }

    @Override
    public void execute(Runnable command) {
        ObjectUtil.checkNotNull(command, "command").run();
    }
}
