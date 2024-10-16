package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午2:07
 */

import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import org.top.java.source.lang.Thread;
import org.top.java.source.lang.ThreadGroup;
import org.top.java.source.lang.ClassLoader;

/**
 * 由 {@link ForkJoinPool} 管理的线程，执行 {@link ForkJoinTask}。
 * 此类只能为添加功能而进行子类化——没有与调度或执行相关的可重写方法。
 * 但是，你可以重写围绕主任务处理循环的初始化和终止方法。
 * 如果你创建了这样的子类，还需要提供一个自定义的 {@link ForkJoinPool.ForkJoinWorkerThreadFactory}，
 * 通过 {@linkplain ForkJoinPool 使用它} 在 {@code ForkJoinPool} 中。
 *
 * @since 1.7
 * @author Doug Lea
 */
public class ForkJoinWorkerThread extends Thread {
    /*
     * ForkJoinWorkerThreads 由 ForkJoinPools 管理并执行 ForkJoinTasks。
     * 解释见 ForkJoinPool 类的内部文档。
     *
     * 这个类只维护到其池和 WorkQueue 的链接。pool 字段在构造时立即设置，
     * 但 workQueue 字段在调用 registerWorker 完成之前不会设置。这导致了一个可见性竞争，
     * 通过要求 workQueue 字段只能由所属线程访问来容忍。
     *
     * 对非公开子类 InnocuousForkJoinWorkerThread 的支持需要在这里和子类中
     * 打破大量的封装（通过 Unsafe）以访问和设置线程字段。
     */

    final ForkJoinPool pool;                // 该线程工作的池
    final ForkJoinPool.WorkQueue workQueue; // 工作窃取机制

    /**
     * 创建一个在给定池中运行的 ForkJoinWorkerThread。
     *
     * @param pool 此线程所在的池
     * @throws NullPointerException 如果池为 null
     */
    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        // 使用占位符，直到可以在 registerWorker 中设置有用的名称
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    /**
     * InnocuousForkJoinWorkerThread 的版本
     */
    ForkJoinWorkerThread(ForkJoinPool pool, ThreadGroup threadGroup,
                         AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, acc);
        eraseThreadLocals(); // 在注册前清除
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    /**
     * 返回承载此线程的池。
     *
     * @return 池
     */
    public ForkJoinPool getPool() {
        return pool;
    }

    /**
     * 返回此线程在池中的唯一索引号。
     * 返回的值范围从零到池中可能存在的最大线程数（减去一），并且在线程的生命周期内不会改变。
     * 此方法对于按工作线程跟踪状态或收集结果的应用程序可能很有用，而不是按任务。
     *
     * @return 索引号
     */
    public int getPoolIndex() {
        return workQueue.getPoolIndex();
    }

    /**
     * 在构造之后但在处理任何任务之前初始化内部状态。如果重写此方法，必须在方法开头调用 {@code super.onStart()}。
     * 初始化需要谨慎：大多数字段必须具有合法的默认值，以确保在此线程开始处理任务之前，
     * 其他线程的尝试访问能够正确工作。
     */
    protected void onStart() {
    }

    /**
     * 执行与此工作线程终止相关的清理。如果重写此方法，必须在重写方法的末尾调用 {@code super.onTermination}。
     *
     * @param exception 导致此线程由于不可恢复的错误中止的异常，或者 {@code null} 表示正常完成
     */
    protected void onTermination(Throwable exception) {
    }

    /**
     * 此方法需要是公共的，但不应被显式调用。它执行主循环以执行 {@link ForkJoinTask}。
     */
    public void run() {
        if (workQueue.array == null) { // 仅运行一次
            Throwable exception = null;
            try {
                onStart();
                pool.runWorker(workQueue);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                try {
                    onTermination(exception);
                } catch (Throwable ex) {
                    if (exception == null)
                        exception = ex;
                } finally {
                    pool.deregisterWorker(this, exception);
                }
            }
        }
    }

    /**
     * 通过将 Thread 映射清空来清除 ThreadLocals。
     */
    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, null);
        U.putObject(this, INHERITABLETHREADLOCALS, null);
    }

    /**
     * InnocuousForkJoinWorkerThread 的非公共钩子方法。
     */
    void afterTopLevelExec() {
    }

    // 设置以允许在构造函数中设置线程字段
    private static final sun.misc.Unsafe U;
    private static final long THREADLOCALS;
    private static final long INHERITABLETHREADLOCALS;
    private static final long INHERITEDACCESSCONTROLCONTEXT;
    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            THREADLOCALS = U.objectFieldOffset
                    (tk.getDeclaredField("threadLocals"));
            INHERITABLETHREADLOCALS = U.objectFieldOffset
                    (tk.getDeclaredField("inheritableThreadLocals"));
            INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset
                    (tk.getDeclaredField("inheritedAccessControlContext"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * 一个工作线程，它没有权限，不属于任何用户定义的 ThreadGroup，并且在运行每个顶级任务后清除所有 ThreadLocals。
     */
    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {
        /** 所有 InnocuousForkJoinWorkerThreads 的 ThreadGroup */
        private static final ThreadGroup innocuousThreadGroup =
                createThreadGroup();

        /** 支持无权限的 AccessControlContext */
        private static final AccessControlContext INNOCUOUS_ACC =
                new AccessControlContext(
                        new ProtectionDomain[] {
                                new ProtectionDomain(null, null)
                        });

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, innocuousThreadGroup, INNOCUOUS_ACC);
        }

        @Override // 用于清除 ThreadLocals
        void afterTopLevelExec() {
            eraseThreadLocals();
        }

        @Override // 始终报告系统加载器
        public ClassLoader getContextClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        @Override // 静默失败
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) { }

        @Override // 超安全
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }

        /**
         * 返回一个新的线程组，其父级是系统 ThreadGroup（最高级、无父级的组）。
         * 使用 Unsafe 遍历 Thread.group 和 ThreadGroup.parent 字段。
         */
        private static ThreadGroup createThreadGroup() {
            try {
                sun.misc.Unsafe u = sun.misc.Unsafe.getUnsafe();
                Class<?> tk = Thread.class;
                Class<?> gk = ThreadGroup.class;
                long tg = u.objectFieldOffset(tk.getDeclaredField("group"));
                long gp = u.objectFieldOffset(gk.getDeclaredField("parent"));
                ThreadGroup group = (ThreadGroup)
                        u.getObject(Thread.currentThread(), tg);
                while (group != null) {
                    ThreadGroup parent = (ThreadGroup)u.getObject(group, gp);
                    if (parent == null)
                        return new ThreadGroup(group,
                                "InnocuousForkJoinWorkerThreadGroup");
                    group = parent;
                }
            } catch (Exception e) {
                throw new Error(e);
            }
            // 若为空，作为不可发生的防护措施
            throw new Error("无法创建 ThreadGroup");
        }
    }

}

