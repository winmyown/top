package org.top.java.source.sun.misc;

import sun.misc.OSEnvironment;
import sun.misc.VMNotification;

import java.util.Properties;
import org.top.java.source.lang.Thread.State;
/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:56
 */
public class VM {
    // 表示当前线程是否被挂起的状态
    private static boolean suspended = false;

    /**
     * @deprecated 表示已弃用的状态，保持向后兼容
     */
    @Deprecated
    public static final int STATE_GREEN = 1;

    /**
     * @deprecated 表示已弃用的状态，保持向后兼容
     */
    @Deprecated
    public static final int STATE_YELLOW = 2;

    /**
     * @deprecated 表示已弃用的状态，保持向后兼容
     */
    @Deprecated
    public static final int STATE_RED = 3;

    // 标识虚拟机是否已启动
    private static volatile boolean booted = false;

    // 线程同步锁对象
    private static final Object lock = new Object();

    // 最大直接内存大小，默认值为64MB
    private static long directMemory = 67108864L;

    // 标识直接内存是否需要页对齐
    private static boolean pageAlignDirectMemory;

    // 标识是否允许数组语法的默认值
    private static boolean defaultAllowArraySyntax = false;

    // 标识是否允许数组语法
    private static boolean allowArraySyntax;

    // 保存系统属性的对象
    private static final Properties savedProps;

    // 用于跟踪 final 引用计数
    private static volatile int finalRefCount;

    // 跟踪 final 引用计数峰值
    private static volatile int peakFinalRefCount;

    // JVMTI 线程状态常量
    private static final int JVMTI_THREAD_STATE_ALIVE = 1;
    private static final int JVMTI_THREAD_STATE_TERMINATED = 2;
    private static final int JVMTI_THREAD_STATE_RUNNABLE = 4;
    private static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 1024;
    private static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 16;
    private static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 32;

    // 构造方法
    public VM() {
    }

    /**
     * @deprecated 判断线程是否被挂起，已弃用方法
     */
    @Deprecated
    public static boolean threadsSuspended() {
        return suspended;
    }

    // 允许线程组的线程挂起
    public static boolean allowThreadSuspension(ThreadGroup var0, boolean var1) {
        return var0.allowThreadSuspension(var1);
    }

    /**
     * @deprecated 挂起线程，已弃用方法
     */
    @Deprecated
    public static boolean suspendThreads() {
        suspended = true;
        return true;
    }

    /**
     * @deprecated 恢复挂起的线程，已弃用方法
     */
    @Deprecated
    public static void unsuspendThreads() {
        suspended = false;
    }

    /**
     * @deprecated 恢复部分挂起的线程，已弃用方法
     */
    @Deprecated
    public static void unsuspendSomeThreads() {
    }

    /**
     * @deprecated 获取当前状态，已弃用方法
     */
    @Deprecated
    public static final int getState() {
        return 1;
    }

    /**
     * @deprecated 注册虚拟机通知，已弃用方法
     */
    @Deprecated
    public static void registerVMNotification(VMNotification var0) {
    }

    /**
     * @deprecated 改变虚拟机的状态，已弃用方法
     */
    @Deprecated
    public static void asChange(int var0, int var1) {
    }

    /**
     * @deprecated 改变虚拟机状态，处理其他线程，已弃用方法
     */
    @Deprecated
    public static void asChange_otherthread(int var0, int var1) {
    }

    // 通知虚拟机已启动
    public static void booted() {
        synchronized(lock) {
            booted = true;
            lock.notifyAll();
        }
    }

    // 检查虚拟机是否已启动
    public static boolean isBooted() {
        return booted;
    }

    // 等待虚拟机启动完成
    public static void awaitBooted() throws InterruptedException {
        synchronized(lock) {
            while(!booted) {
                lock.wait();
            }
        }
    }

    // 获取最大直接内存大小
    public static long maxDirectMemory() {
        return directMemory;
    }

    // 判断直接内存是否需要页对齐
    public static boolean isDirectMemoryPageAligned() {
        return pageAlignDirectMemory;
    }

    // 是否允许数组语法
    public static boolean allowArraySyntax() {
        return allowArraySyntax;
    }

    // 检查是否为系统域加载器
    public static boolean isSystemDomainLoader(ClassLoader var0) {
        return var0 == null;
    }

    // 获取保存的属性
    public static String getSavedProperty(String var0) {
        if (savedProps.isEmpty()) {
            throw new IllegalStateException("应在初始化后非空");
        } else {
            return savedProps.getProperty(var0);
        }
    }

    // 保存和移除属性
    public static void saveAndRemoveProperties(Properties var0) {
        if (booted) {
            throw new IllegalStateException("系统初始化已完成");
        } else {
            savedProps.putAll(var0);
            String var1 = (String)var0.remove("sun.nio.MaxDirectMemorySize");
            if (var1 != null) {
                if (var1.equals("-1")) {
                    directMemory = Runtime.getRuntime().maxMemory();
                } else {
                    long var2 = Long.parseLong(var1);
                    if (var2 > -1L) {
                        directMemory = var2;
                    }
                }
            }

            var1 = (String)var0.remove("sun.nio.PageAlignDirectMemory");
            if ("true".equals(var1)) {
                pageAlignDirectMemory = true;
            }

            var1 = var0.getProperty("sun.lang.ClassLoader.allowArraySyntax");
            allowArraySyntax = var1 == null ? defaultAllowArraySyntax : Boolean.parseBoolean(var1);
            var0.remove("java.lang.Integer.IntegerCache.high");
            var0.remove("sun.zip.disableMemoryMapping");
            var0.remove("sun.java.launcher.diag");
            var0.remove("sun.cds.enableSharedLookupCache");
        }
    }

    // 初始化操作系统环境
    public static void initializeOSEnvironment() {
        if (!booted) {
            OSEnvironment.initialize();
        }
    }

    // 获取 final 引用计数
    public static int getFinalRefCount() {
        return finalRefCount;
    }

    // 获取峰值 final 引用计数
    public static int getPeakFinalRefCount() {
        return peakFinalRefCount;
    }

    // 增加 final 引用计数
    public static void addFinalRefCount(int var0) {
        finalRefCount += var0;
        if (finalRefCount > peakFinalRefCount) {
            peakFinalRefCount = finalRefCount;
        }
    }

    // 将 JVMTI 线程状态转换为线程状态
    public static State toThreadState(int var0) {
        if ((var0 & 4) != 0) {
            return State.RUNNABLE;
        } else if ((var0 & 1024) != 0) {
            return State.BLOCKED;
        } else if ((var0 & 16) != 0) {
            return State.WAITING;
        } else if ((var0 & 32) != 0) {
            return State.TIMED_WAITING;
        } else if ((var0 & 2) != 0) {
            return State.TERMINATED;
        } else {
            return (var0 & 1) == 0 ? State.NEW : State.RUNNABLE;
        }
    }

    // 原生方法：获取最新的用户自定义类加载器
    public static native ClassLoader latestUserDefinedLoader();

    // 原生方法：初始化
    private static native void initialize();

    // 静态初始化块
    static {
        allowArraySyntax = defaultAllowArraySyntax;
        savedProps = new Properties();
        finalRefCount = 0;
        peakFinalRefCount = 0;
        initialize();
    }
}

