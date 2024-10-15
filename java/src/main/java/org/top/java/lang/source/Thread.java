package org.top.java.lang.source;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.security.util.SecurityConstants;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 上午7:17
 */

/**
 * <p>线程是程序中的执行线程。Java虚拟机允许应用程序同时运行多个执行线程。<br/>每个线程都有一个优先级。优先级较高的线程优先于优先级较低的线程执行。每个线程还可以标记为守护线程。当某个线程中的代码创建一个新的Thread对象时，新的线程的优先级初始设置为与创建线程的优先级相等，且仅当创建线程是守护线程时，新线程才是守护线程。  </p>
 * <p>当Java虚拟机启动时，通常有一个非守护线程（通常会调用某个指定类的名为main的方法）。Java虚拟机会继续执行线程，直到以下任一情况发生：</p>
 * <ol start='' >
 * <li>调用了<code>Runtime</code>类的<code>exit</code>方法，并且安全管理器允许退出操作。</li>
 * <li>所有非守护线程都已经终止，要么是从<code>run</code>方法的调用中返回，要么是抛出了超出<code>run</code>方法的异常。</li>
 *
 * </ol>
 * <p>有两种方法可以创建新的执行线程。<br/>一种方法是声明一个类为<code>Thread</code>的子类。这个子类应该重写<code>Thread</code>类的<code>run</code>方法。然后可以分配并启动该子类的一个实例。例如，一个计算大于给定值的质数的线程可以这样编写：</p>
 * <pre><code class='language-java' lang='java'>class PrimeThread extends Thread {
 *     long minPrime;
 *     PrimeThread(long minPrime) {
 *         this.minPrime = minPrime;
 *     }
 *
 *     public void run() {
 *         // 计算大于 minPrime 的质数
 *         . . .
 *     }
 * }
 * </code></pre>
 * <p>然后以下代码将创建一个线程并启动它运行：</p>
 * <pre><code class='language-java' lang='java'>PrimeThread p = new PrimeThread(143);
 * p.start();
 * </code></pre>
 * <p>另一种创建线程的方法是声明一个实现<code>Runnable</code>接口的类。该类实现<code>run</code>方法。然后可以分配该类的一个实例，并在创建<code>Thread</code>时将其实例作为参数传递并启动。相同的示例用这种风格编写如下：</p>
 * <pre><code class='language-java' lang='java'>class PrimeRun implements Runnable {
 *     long minPrime;
 *     PrimeRun(long minPrime) {
 *         this.minPrime = minPrime;
 *     }
 *
 *     public void run() {
 *         // 计算大于 minPrime 的质数
 *         . . .
 *     }
 * }
 * </code></pre>
 * <p>然后以下代码将创建一个线程并启动它运行：</p>
 * <pre><code class='language-java' lang='java'>PrimeRun p = new PrimeRun(143);
 * new Thread(p).start();
 * </code></pre>
 * <p>每个线程都有一个用于标识的名称。多个线程可以拥有相同的名称。如果在创建线程时未指定名称，则会为其生成一个新名称。<br/>除非另有说明，向此类的构造方法或方法传递<code>null</code>参数会导致抛出<code>NullPointerException</code>。</p>
 * <p>自JDK1.0开始<br/>参见：</p>
 * <ul>
 * <li><code>Runnable</code></li>
 * <li><code>Runtime.exit(int)</code></li>
 * <li><code>run()</code></li>
 * <li><code>stop()</code></li>
 *
 * </ul>
 * <p>作者：未具名</p>
 */
public class Thread implements Runnable {
    /* 确保 registerNatives 是 <clinit> 执行的第一件事。 */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    private volatile String name;
    private int            priority;
    private Thread         threadQ;
    private long           eetop;

    /* 该线程是否进行单步操作。 */
    private boolean     single_step;

    /* 该线程是否是守护线程。 */
    private boolean     daemon = false;

    /* JVM 状态 */
    private boolean     stillborn = false;

    /* 要运行的目标。 */
    private Runnable target;

    /* 该线程的组 */
    private ThreadGroup group;

    /* 该线程的上下文类加载器 */
    private ClassLoader contextClassLoader;

    /* 该线程继承的访问控制上下文 */
    private AccessControlContext inheritedAccessControlContext;

    /* 用于自动编号匿名线程。 */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /* 与此线程相关的 ThreadLocal 值。此映射由 ThreadLocal 类维护。 */
    ThreadLocal.ThreadLocalMap threadLocals = null;

    /*
     * 与此线程相关的可继承 ThreadLocal 值。此映射由 InheritableThreadLocal 类维护。
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    /*
     * 此线程的请求堆栈大小，如果创建者未指定堆栈大小，则为 0。
     * 由 VM 自行决定如何处理这个数字；某些 VM 将忽略它。
     */
    private long stackSize;

    /*
     * 原生线程终止后，JVM 私有状态将保留。
     */
    private long nativeParkEventPointer;

    /*
     * 线程 ID
     */
    private long tid;

    /* 用于生成线程 ID */
    private static long threadSeqNumber;

    /* 用于工具的 Java 线程状态，初始化为指示线程“尚未启动”的状态 */

    private volatile int threadStatus = 0;

    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }

    /**
     * 传递给当前调用 java.util.concurrent.locks.LockSupport.park 的参数。
     * 由 (私有的) java.util.concurrent.locks.LockSupport.setBlocker 设置。
     * 通过 java.util.concurrent.locks.LockSupport.getBlocker 访问。
     */
    volatile Object parkBlocker;

    /* 此线程在可中断的 I/O 操作中被阻塞的对象（如果有）。在设置该线程的中断状态后，应调用阻塞器的中断方法。 */
    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    /* 设置 blocker 字段；通过 sun.misc.SharedSecrets 从 java.nio 代码调用 */
    void blockedOn(Interruptible b) {
        synchronized (blockerLock) {
            blocker = b;
        }
    }

    /**
     * 线程可以具有的最低优先级。
     */
    public final static int MIN_PRIORITY = 1;

    /**
     * 分配给线程的默认优先级。
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * 线程可以具有的最高优先级。
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * 返回对当前正在执行的线程对象的引用。
     *
     * @return 当前正在执行的线程。
     */
    public static native Thread currentThread();

    /**
     * 向调度程序提示当前线程愿意放弃其当前的处理器使用权。
     * 调度程序可以自由地忽略此提示。
     *
     * <p> yield 是一种启发式尝试，旨在提高本来会过度利用
     * CPU 的线程之间的相对进度。它的使用应与详细的分析和基准测试相结合，
     * 以确保它确实具有预期的效果。
     *
     * <p> 很少适合使用此方法。它可能在调试或测试目的下有用，
     * 可能有助于重现由于竞争条件引起的错误。
     * 它在设计并发控制结构（例如 {@link java.util.concurrent.locks} 包中的结构）
     * 时也可能有用。
     */
    public static native void yield();

    /**
     * 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数，
     * 具体取决于系统定时器和调度程序的精度和准确性。
     * 线程不会失去对任何监视器的所有权。
     *
     * @param  millis
     *         休眠的时间长度，以毫秒为单位
     *
     * @throws  IllegalArgumentException
     *          如果 {@code millis} 为负值
     *
     * @throws  InterruptedException
     *          如果任何线程中断了当前线程。抛出此异常时，当前线程的<i>中断状态</i>将被清除。
     */
    public static native void sleep(long millis) throws InterruptedException;

    /**
     * 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数加上指定的纳秒数，
     * 具体取决于系统定时器和调度程序的精度和准确性。
     * 线程不会失去对任何监视器的所有权。
     *
     * @param  millis
     *         休眠的时间长度，以毫秒为单位
     *
     * @param  nanos
     *         {@code 0-999999} 额外的纳秒数以进行休眠
     *
     * @throws  IllegalArgumentException
     *          如果 {@code millis} 的值为负，或 {@code nanos} 的值不在 {@code 0-999999} 范围内
     *
     * @throws  InterruptedException
     *          如果任何线程中断了当前线程。抛出此异常时，当前线程的<i>中断状态</i>将被清除。
     */
    public static void sleep(long millis, int nanos)
            throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("超时值为负");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "纳秒超时值超出范围");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    /**
     * 使用当前的 AccessControlContext 初始化一个 Thread。
     * @see #init(ThreadGroup,Runnable,String,long,AccessControlContext,boolean)
     */
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        init(g, target, name, stackSize, null, true);
    }

    /**
     * 初始化一个线程。
     *
     * @param g 线程组
     * @param target 将调用其 run() 方法的对象
     * @param name 新线程的名称
     * @param stackSize 为新线程请求的堆栈大小，或者如果创建者未指定堆栈大小，则为 0
     * @param acc 要继承的 AccessControlContext，或者为 null 时为 AccessController.getContext()
     * @param inheritThreadLocals 如果为 true，表示从构造线程继承初始的 ThreadLocal 值
     */
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
        if (name == null) {
            throw new NullPointerException("线程名称不能为空");
        }

        this.name = name;

        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();
        if (g == null) {
            /* 判断它是否是一个小程序 */

            /* 如果有安全管理器，询问安全管理器该怎么做。 */
            if (security != null) {
                g = security.getThreadGroup();
            }

            /* 如果安全管理器没有明确意见，则使用父线程组。 */
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }

        /* 无论是否显式传递 threadGroup，都要检查访问权限。 */
        g.checkAccess();

        /*
         * 我们是否拥有所需的权限？
         */
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        g.addUnstarted();

        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass())) {
            this.contextClassLoader = parent.getContextClassLoader();
        } else {
            this.contextClassLoader = parent.contextClassLoader;
        }
        this.inheritedAccessControlContext =
                acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        if (inheritThreadLocals && parent.inheritableThreadLocals != null) {
            this.inheritableThreadLocals =
                    ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        }
        /* 存储指定的堆栈大小，以防虚拟机关心 */
        this.stackSize = stackSize;

        /* 设置线程 ID */
        tid = nextThreadID();
    }

    /**
     * 抛出 CloneNotSupportedException，因为无法有意义地克隆线程。请改为构造一个新线程。
     *
     * @throws  CloneNotSupportedException
     *          永远抛出
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(null, null, gname)} 构造方法相同，其中 {@code gname} 是新生成的名称。
     * 自动生成的名称形式为 {@code "Thread-"+n}，其中 n 是一个整数。
     */
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(null, target, gname)} 构造方法相同，其中 {@code gname} 是新生成的名称。
     * 自动生成的名称形式为 {@code "Thread-"+n}，其中 n 是一个整数。
     *
     * @param  target
     *         启动线程时将调用其 {@code run} 方法的对象。如果为 {@code null}，则此类的 {@code run} 方法不执行任何操作。
     */
    public Thread(Runnable target) {
        init(null, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * 创建一个新的继承给定 AccessControlContext 的线程。此构造函数不对外开放。
     */
    Thread(Runnable target, AccessControlContext acc) {
        init(null, target, "Thread-" + nextThreadNum(), 0, acc, false);
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(group, target, gname)} 构造方法相同，其中 {@code gname} 是新生成的名称。
     * 自动生成的名称形式为 {@code "Thread-"+n}，其中 n 是一个整数。
     *
     * @param  group
     *         线程组。如果为 {@code null} 且有安全管理器，则由 {@code SecurityManager.getThreadGroup()} 确定线程组；
     *         如果没有安全管理器，或者 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，则设置为当前线程的线程组。
     *
     * @param  target
     *         启动线程时将调用其 {@code run} 方法的对象。如果为 {@code null}，则此类的 {@code run} 方法不执行任何操作。
     *
     * @throws  SecurityException
     *          如果当前线程无法在指定的线程组中创建线程
     */
    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(null, null, name)} 构造方法相同。
     *
     * @param   name
     *          新线程的名称
     */
    public Thread(String name) {
        init(null, null, name, 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(group, null, name)} 构造方法相同。
     *
     * @param  group
     *         线程组。如果为 {@code null} 且有安全管理器，则由 {@code SecurityManager.getThreadGroup()} 确定线程组；
     *         如果没有安全管理器，或者 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，则设置为当前线程的线程组。
     *
     * @param  name
     *         新线程的名称
     *
     * @throws  SecurityException
     *          如果当前线程无法在指定的线程组中创建线程
     */
    public Thread(ThreadGroup group, String name) {
        init(group, null, name, 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象。此构造函数的效果与调用 {@code Thread(null, target, name)} 构造方法相同。
     *
     * @param  target
     *         启动线程时将调用其 {@code run} 方法的对象。如果为 {@code null}，则此类的 {@code run} 方法不执行任何操作。
     *
     * @param  name
     *         新线程的名称
     */
    public Thread(Runnable target, String name) {
        init(null, target, name, 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象，使其具有 {@code target} 作为运行对象，具有指定的 {@code name} 作为名称，并属于由 {@code group} 引用的线程组。
     *
     * <p>如果有安全管理器，将使用 ThreadGroup 作为参数调用其 {@code checkAccess} 方法。
     *
     * <p>此外，当由子类构造函数直接或间接调用时，将使用 {@code RuntimePermission("enableContextClassLoaderOverride")} 权限调用其 {@code checkPermission} 方法，
     * 检查是否可以覆盖 {@code getContextClassLoader} 或 {@code setContextClassLoader} 方法。
     *
     * <p>新创建的线程的优先级设置为创建线程的优先级（即当前运行线程）。可以使用 {@code setPriority} 方法将优先级更改为新值。
     *
     * <p>如果且仅当创建它的线程当前标记为守护线程，新创建的线程将最初被标记为守护线程。可以使用 {@code setDaemon} 方法更改线程是否为守护线程。
     *
     * @param  group
     *         线程组。如果为 {@code null} 且有安全管理器，则由 {@code SecurityManager.getThreadGroup()} 确定线程组；
     *         如果没有安全管理器，或者 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，则设置为当前线程的线程组。
     *
     * @param  target
     *         启动线程时将调用其 {@code run} 方法的对象。如果为 {@code null}，则此类的 {@code run} 方法不执行任何操作。
     *
     * @param  name
     *         新线程的名称
     *
     * @throws  SecurityException
     *          如果当前线程无法在指定的线程组中创建线程，或无法覆盖上下文类加载器方法。
     */
    public Thread(ThreadGroup group, Runnable target, String name) {
        init(group, target, name, 0);
    }

    /**
     * 分配一个新的 {@code Thread} 对象，使其具有 {@code target} 作为其运行对象，
     * 指定的 {@code name} 作为其名称，属于由 {@code group} 引用的线程组，并具有
     * 指定的 <i>栈大小</i>。
     *
     * <p>此构造函数与 {@link #Thread(ThreadGroup,Runnable,String)} 构造函数完全相同，
     * 唯一区别是它允许指定线程的栈大小。栈大小是虚拟机为该线程的栈分配的大致字节数。
     * <b> `stackSize` 参数的影响（如果有的话）在很大程度上依赖于平台。</b>
     *
     * <p>在某些平台上，为 {@code stackSize} 参数指定较大的值可能允许线程在抛出
     * {@link StackOverflowError} 之前达到更大的递归深度。同样，指定较小的值可能允许
     * 更多线程同时存在，而不会抛出 {@link OutOfMemoryError}（或其他内部错误）。
     * <b>在某些平台上，{@code stackSize} 参数的值可能完全没有效果。</b>
     *
     * <p>虚拟机可以将 {@code stackSize} 参数视为建议。如果指定的值对平台来说过低，
     * 虚拟机可能会使用某些平台特定的最小值；如果指定的值过高，虚拟机可能会使用某些
     * 平台特定的最大值。同样，虚拟机可以随意向上或向下舍入指定的值（或完全忽略它）。
     *
     * <p>为 {@code stackSize} 参数指定零值将使此构造函数的行为完全像
     * {@code Thread(ThreadGroup, Runnable, String)} 构造函数。
     *
     * <p><i>由于此构造函数行为的依赖于平台的特性，应格外小心使用它。执行给定计算
     * 所需的线程栈大小可能会因不同的 JRE 实现而有所不同。鉴于这种差异，可能需要对
     * 栈大小参数进行仔细调整，并且每次在应用程序运行的每个 JRE 实现上都可能需要重复调整。</i>
     *
     * <p>实现说明：鼓励 Java 平台实现者记录其实现关于 {@code stackSize} 参数的行为。
     *
     * @param  group
     *         线程组。如果为 {@code null} 且存在安全管理器，则线程组由
     *         {@linkplain SecurityManager#getThreadGroup SecurityManager.getThreadGroup()} 确定。
     *         如果没有安全管理器或 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，
     *         则线程组设置为当前线程的线程组。
     *
     * @param  target
     *         当此线程启动时调用其 {@code run} 方法的对象。如果为 {@code null}，
     *         则调用此线程的 {@code run} 方法。
     *
     * @param  name
     *         新线程的名称。
     *
     * @param  stackSize
     *         新线程的期望栈大小，或零表示忽略该参数。
     *
     * @throws  SecurityException
     *          如果当前线程无法在指定的线程组中创建线程。
     *
     * @since 1.4
     */
    public Thread(ThreadGroup group, Runnable target, String name,
                  long stackSize) {
        init(group, target, name, stackSize);
    }

    /**
     * 使此线程开始执行；Java 虚拟机会调用此线程的 <code>run</code> 方法。
     * <p>
     * 结果是两个线程同时运行：当前线程（它从调用 <code>start</code> 方法返回）和另一个线程（它执行其 <code>run</code> 方法）。
     * <p>
     * 启动线程一次后，再次启动它是非法的。特别地，线程在完成执行后无法重新启动。
     *
     * @exception  IllegalThreadStateException  如果线程已经启动。
     * @see        #run()
     * @see        #stop()
     */
    public synchronized void start() {
        /**
         * 此方法不会为主方法线程或由 VM 创建/设置的“系统”组线程调用。将来为此方法添加的任何新功能可能也需要添加到 VM 中。
         *
         * 0 状态值对应于状态“NEW”。
         */
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

        /* 通知组此线程即将启动，以便将其添加到组的线程列表中，并且该组的未启动计数可以减少。 */
        group.add(this);

        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    group.threadStartFailed(this);
                }
            } catch (Throwable ignore) {
                /* 什么也不做。如果 start0 抛出 Throwable，则会将其传递到调用栈中 */
            }
        }
    }

    private native void start0();

    /**
     * 如果该线程是使用单独的 <code>Runnable</code> 运行对象构造的，则调用该 <code>Runnable</code> 对象的 <code>run</code> 方法；
     * 否则，此方法不执行任何操作并返回。
     * <p>
     * 线程的子类应该重写此方法。
     *
     * @see     #start()
     * @see     #stop()
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * 系统调用此方法以在线程退出之前让线程有机会清理资源。
     */
    private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* 积极地将所有引用字段设置为 null: 见 bug 4006245 */
        target = null;
        /* 加快这些资源的释放 */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }

    /**
     * 强制线程停止执行。
     * <p>
     * 如果安装了安全管理器，将使用 <code>this</code> 作为其参数调用安全管理器的 <code>checkAccess</code> 方法。
     * 这可能会导致抛出 <code>SecurityException</code>（在当前线程中）。
     * <p>
     * 如果此线程不同于当前线程（即当前线程试图停止另一个线程），将使用 <code>RuntimePermission("stopThread")</code> 参数调用安全管理器的
     * <code>checkPermission</code> 方法。再次，这可能导致抛出 <code>SecurityException</code>（在当前线程中）。
     * <p>
     * 由该线程表示的线程被迫异常停止并抛出新创建的 <code>ThreadDeath</code> 对象作为异常。
     * <p>
     * 允许停止尚未启动的线程。如果线程最终启动，它会立即终止。
     * <p>
     * 应用程序通常不应尝试捕获 <code>ThreadDeath</code>，除非需要进行某些非常规的清理操作（请注意，抛出 <code>ThreadDeath</code> 会导致在线程正式死亡之前执行 <code>try</code> 语句的 <code>finally</code> 子句）。
     * 如果 <code>catch</code> 子句捕获了 <code>ThreadDeath</code> 对象，则必须重新抛出该对象，以确保线程实际死亡。
     * <p>
     * 顶层错误处理程序对未捕获异常做出反应时，不会打印消息或以其他方式通知应用程序，除非该未捕获异常是 <code>ThreadDeath</code> 实例。
     *
     * @exception  SecurityException  如果当前线程无法修改此线程。
     * @see        #interrupt()
     * @see        #checkAccess()
     * @see        #run()
     * @see        #start()
     * @see        ThreadDeath
     * @see        ThreadGroup#uncaughtException(Thread,Throwable)
     * @see        SecurityManager#checkAccess(Thread)
     * @see        SecurityManager#checkPermission
     * @deprecated 此方法本质上不安全。使用 Thread.stop 终止线程会导致它解锁所有已锁定的监视器（作为未检查的 <code>ThreadDeath</code> 异常沿栈传播的自然结果）。
     * 如果这些监视器之前保护的对象处于不一致状态，这些受损的对象将对其他线程可见，可能导致任意行为。许多使用 <code>stop</code> 的代码应替换为代码，
     * 该代码仅修改某些变量以指示目标线程应停止运行。目标线程应定期检查此变量，并在变量指示应停止运行时有序地从其 run 方法返回。
     * 如果目标线程等待很长时间（例如在条件变量上），则应使用 <code>interrupt</code> 方法中断等待。
     * 有关更多信息，请参见<a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">为何 Thread.stop、Thread.suspend 和 Thread.resume 被弃用？</a>。
     */
    @Deprecated
    public final void stop() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            checkAccess();
            if (this != Thread.currentThread()) {
                security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
            }
        }
        // 0 状态值对应于“NEW”，由于我们持有锁，它不能更改为非 NEW 状态。
        if (threadStatus != 0) {
            resume(); // 唤醒线程，如果线程被挂起；否则是无操作。
        }

        // VM 可以处理所有线程状态
        stop0(new ThreadDeath());
    }

    /**
     * 抛出 {@code UnsupportedOperationException}。
     *
     * @param obj 被忽略
     *
     * @deprecated 此方法最初设计为强制线程停止并抛出给定的 {@code Throwable} 作为异常。它本质上是不安全的（详见 {@link #stop()} 的说明），
     * 且还可以用于生成目标线程无法处理的异常。
     * 有关更多信息，请参见<a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">为何 Thread.stop、Thread.suspend 和 Thread.resume 被弃用？</a>。
     */
    @Deprecated
    public final synchronized void stop(Throwable obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * 中断此线程。
     *
     * <p>除非当前线程正在自我中断（这是始终允许的），否则将调用此线程的 {@link #checkAccess() checkAccess} 方法，这可能会导致抛出 {@link SecurityException}。
     *
     * <p>如果此线程正在 {@link Object#wait() wait()}、{@link Object#wait(long) wait(long)} 或 {@link Object#wait(long, int) wait(long, int)} 方法中被阻塞，
     * 或者在 {@link #join()}、{@link #join(long)}、{@link #join(long, int)}、{@link #sleep(long)} 或 {@link #sleep(long, int)} 方法中被阻塞，
     * 则中断状态将被清除，并且此线程将收到 {@link InterruptedException}。
     *
     * <p>如果此线程在 {@link java.nio.channels.InterruptibleChannel} 上被阻塞，则该通道将被关闭，中断状态将被设置，并且线程将收到 {@link java.nio.channels.ClosedByInterruptException}。
     *
     * <p>如果此线程在 {@link java.nio.channels.Selector} 中被阻塞，则线程的中断状态将被设置，并且它将立即从选择操作返回，可能返回非零值，正如选择器的 {@link java.nio.channels.Selector#wakeup wakeup} 方法被调用一样。
     *
     * <p>如果上述条件均不成立，则线程的中断状态将被设置。</p>
     *
     * <p>中断尚未存活的线程可能不会产生任何效果。
     *
     * @throws  SecurityException 如果当前线程无法修改此线程
     *
     * @revised 6.0
     * @spec JSR-51
     */
    public void interrupt() {
        if (this != Thread.currentThread())
            checkAccess();

        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();           // 仅设置中断标志
                b.interrupt(this);
                return;
            }
        }
        interrupt0();
    }

    /**
     * 测试当前线程是否已中断。该方法会清除线程的中断状态。换句话说，如果连续两次调用该方法，第二次调用将返回 false（除非在第一次调用清除了中断状态之后，当前线程再次被中断）。
     *
     * <p>由于线程尚未启动时的中断将被忽略，因此该方法返回 false。
     *
     * @return  如果当前线程已被中断，则返回 <code>true</code>；否则返回 <code>false</code>。
     * @see #isInterrupted()
     * @revised 6.0
     */
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    /**
     * 测试该线程是否已被中断。该方法不会影响线程的中断状态。
     *
     * <p>由于线程尚未启动时的中断将被忽略，因此该方法返回 false。
     *
     * @return  如果该线程已被中断，则返回 <code>true</code>；否则返回 <code>false</code>。
     * @see     #interrupted()
     * @revised 6.0
     */
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * 测试某个线程是否被中断。是否重置中断状态取决于传递的 ClearInterrupted 参数。
     */
    private native boolean isInterrupted(boolean ClearInterrupted);

    /**
     * 抛出 {@link NoSuchMethodError}。
     *
     * @deprecated 此方法原本设计用于在不进行任何清理的情况下销毁此线程。它持有的任何监视器将保持锁定。然而，该方法从未实现过。
     * 如果它被实现了，它将在许多情况下与 {@link #suspend} 一样容易导致死锁。如果目标线程持有保护关键系统资源的锁，并且该线程被销毁，
     * 则没有线程能够再次访问该资源。如果另一个线程试图在调用 <code>resume</code> 之前锁定该资源，则会导致死锁。这种死锁通常表现为“冻结”进程。
     * 有关更多信息，请参见<a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">为何 Thread.stop、Thread.suspend 和 Thread.resume 被弃用？</a>。
     * @throws NoSuchMethodError 始终抛出此异常
     */
    @Deprecated
    public void destroy() {
        throw new NoSuchMethodError();
    }

    /**
     * 测试此线程是否存活。如果该线程已启动且尚未终止，则该线程处于存活状态。
     *
     * @return  如果此线程存活，则返回 <code>true</code>；否则返回 <code>false</code>。
     */
    public final native boolean isAlive();

    /**
     * 暂停此线程。
     * <p>
     * 首先，调用该线程的 <code>checkAccess</code> 方法。可能会抛出 <code>SecurityException</code>。
     * <p>
     * 如果线程存活，它将暂停并且无法继续执行，直到被恢复。
     *
     * @exception  SecurityException  如果当前线程无法修改此线程。
     * @see #checkAccess
     * @deprecated 该方法由于其固有的死锁问题而被弃用。如果目标线程持有保护关键系统资源的监视器并且它被暂停，则没有线程可以访问该资源，直到目标线程被恢复。
     *             如果尝试恢复目标线程的线程在调用 <code>resume</code> 之前试图锁定此监视器，则会导致死锁。这种死锁通常表现为“冻结”进程。
     *             有关更多信息，请参见<a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">为何 Thread.stop、Thread.suspend 和 Thread.resume 被弃用？</a>。
     */
    @Deprecated
    public final void suspend() {
        checkAccess();
        suspend0();
    }

    /**
     * 恢复一个暂停的线程。
     * <p>
     * 首先，调用该线程的 <code>checkAccess</code> 方法。可能会抛出 <code>SecurityException</code>。
     * <p>
     * 如果线程存活且已暂停，则它将被恢复并允许继续执行。
     *
     * @exception  SecurityException  如果当前线程无法修改此线程。
     * @see        #checkAccess
     * @see        #suspend()
     * @deprecated 该方法仅用于与 {@link #suspend} 配合使用，后者由于其死锁问题而被弃用。有关更多信息，请参见
     * <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">为何 Thread.stop、Thread.suspend 和 Thread.resume 被弃用？</a>。
     */
    @Deprecated
    public final void resume() {
        checkAccess();
        resume0();
    }

    /**
     * 更改此线程的优先级。
     * <p>
     * 首先，调用该线程的 <code>checkAccess</code> 方法。可能会抛出 <code>SecurityException</code>。
     * <p>
     * 否则，将此线程的优先级设置为指定的 <code>newPriority</code> 和该线程组允许的最大优先级中的较小值。
     *
     * @param newPriority 要设置的优先级
     * @exception  IllegalArgumentException  如果优先级不在 <code>MIN_PRIORITY</code> 到 <code>MAX_PRIORITY</code> 范围内。
     * @exception  SecurityException  如果当前线程无法修改此线程。
     * @see        #getPriority
     * @see        #checkAccess()
     * @see        #getThreadGroup()
     * @see        #MAX_PRIORITY
     * @see        #MIN_PRIORITY
     * @see        ThreadGroup#getMaxPriority()
     */
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        checkAccess();
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        if ((g = getThreadGroup()) != null) {
            if (newPriority > g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            setPriority0(priority = newPriority);
        }
    }

    /**
     * 返回此线程的优先级。
     *
     * @return  此线程的优先级。
     * @see     #setPriority
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * 将此线程的名称更改为等于参数 <code>name</code>。
     * <p>
     * 首先，调用该线程的 <code>checkAccess</code> 方法。可能会抛出 <code>SecurityException</code>。
     *
     * @param      name   此线程的新名称。
     * @exception  SecurityException  如果当前线程无法修改此线程。
     * @see        #getName
     * @see        #checkAccess()
     */
    public final synchronized void setName(String name) {
        checkAccess();
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;
        if (threadStatus != 0) {
            setNativeName(name);
        }
    }

    /**
     * 返回此线程的名称。
     *
     * @return  此线程的名称。
     * @see     #setName(String)
     */
    public final String getName() {
        return name;
    }

    /**
     * 返回此线程所属的线程组。
     * 如果该线程已终止（已停止），则此方法返回 null。
     *
     * @return  此线程的线程组。
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * 返回当前线程所属的线程组及其子组中活动线程的估计数量。
     * 递归遍历当前线程的线程组中的所有子组。
     *
     * <p> 返回的值只是一个估计值，因为线程的数量可能会动态变化，并且可能受某些系统线程的影响。
     * 此方法主要用于调试和监控。
     *
     * @return  当前线程的线程组及其子组中的活动线程的估计数量。
     */
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * 将当前线程所属的线程组及其子组中的每个活动线程复制到指定的数组中。
     * 此方法简单地调用当前线程组的 {@link ThreadGroup#enumerate(Thread[])} 方法。
     *
     * <p> 应用程序可能会使用 {@link #activeCount} 方法来估计数组应该多大，
     * 但是如果数组太短以至于无法容纳所有线程，则会忽略多余的线程。
     * 如果获取当前线程组及其子组中所有活动线程是至关重要的，调用者应验证返回的 int 值严格小于 tarray 的长度。
     *
     * <p> 由于此方法固有的竞态条件，建议仅将其用于调试和监控目的。
     *
     * @param  tarray
     *         用于存放线程列表的数组
     *
     * @return  放入数组中的线程数量
     *
     * @throws  SecurityException
     *          如果 {@link java.lang.ThreadGroup#checkAccess} 确定当前线程无法访问其线程组
     */
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    /**
     * 计算此线程中的堆栈帧数量。线程必须处于暂停状态。
     *
     * @return     此线程中的堆栈帧数量。
     * @exception  IllegalThreadStateException  如果此线程未被暂停。
     * @deprecated 该方法依赖于 {@link #suspend}，已被弃用。此外，该方法的结果从未被明确定义。
     */
    @Deprecated
    public native int countStackFrames();

    /**
     * 最多等待 millis 毫秒，让该线程终止。超时为 0 表示永久等待。
     *
     * <p> 此实现使用基于 <code>this.wait</code> 的循环，条件是 <code>this.isAlive</code>。当线程终止时，将调用 <code>this.notifyAll</code> 方法。
     * 不建议应用程序在 <code>Thread</code> 实例上使用 <code>wait</code>、<code>notify</code> 或 <code>notifyAll</code> 方法。
     *
     * @param  millis
     *         等待时间（以毫秒为单位）
     *
     * @throws  IllegalArgumentException
     *          如果 millis 的值为负数
     *
     * @throws  InterruptedException
     *          如果任何线程中断了当前线程。当抛出此异常时，将清除当前线程的 <i>中断状态</i>。
     */
    public final synchronized void join(long millis)
            throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * 最多等待 millis 毫秒加上 nanos 纳秒，让该线程终止。
     *
     * <p> 此实现使用基于 <code>this.wait</code> 的循环，条件是 <code>this.isAlive</code>。当线程终止时，将调用 <code>this.notifyAll</code> 方法。
     * 不建议应用程序在 <code>Thread</code> 实例上使用 <code>wait</code>、<code>notify</code> 或 <code>notifyAll</code> 方法。
     *
     * @param  millis
     *         等待时间（以毫秒为单位）
     *
     * @param  nanos
     *         额外的纳秒等待时间（0-999999 范围内）
     *
     * @throws  IllegalArgumentException
     *          如果 millis 的值为负数，或 nanos 的值不在 0-999999 范围内
     *
     * @throws  InterruptedException
     *          如果任何线程中断了当前线程。当抛出此异常时，将清除当前线程的 <i>中断状态</i>。
     */
    public final synchronized void join(long millis, int nanos)
            throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }

    /**
     * 等待该线程终止。
     *
     * <p> 调用此方法的效果与调用
     *
     * <blockquote>
     * {@linkplain #join(long) join}{@code (0)}
     * </blockquote>
     * 完全相同。
     *
     * @throws  InterruptedException
     *          如果任何线程中断了当前线程。当抛出此异常时，将清除当前线程的 <i>中断状态</i>。
     */
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * 打印当前线程的堆栈跟踪信息到标准错误流中。此方法仅用于调试。
     *
     * @see     Throwable#printStackTrace()
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * 将此线程标记为守护线程或用户线程。当所有运行的线程都是守护线程时，Java 虚拟机将退出。
     *
     * <p> 必须在线程启动之前调用此方法。
     *
     * @param  on
     *         如果为 true，则将此线程标记为守护线程
     *
     * @throws  IllegalThreadStateException
     *          如果此线程已处于存活状态
     *
     * @throws  SecurityException
     *          如果 {@link #checkAccess} 方法确定当前线程无法修改此线程
     */
    public final void setDaemon(boolean on) {
        checkAccess();
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        daemon = on;
    }

    /**
     * 测试此线程是否为守护线程。
     *
     * @return  如果此线程为守护线程，则返回 <code>true</code>；否则返回 <code>false</code>。
     * @see     #setDaemon(boolean)
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * 确定当前正在运行的线程是否有权限修改此线程。
     * <p>
     * 如果存在安全管理器，则调用其 <code>checkAccess</code> 方法，参数为此线程。这可能会抛出 <code>SecurityException</code>。
     *
     * @exception  SecurityException  如果当前线程无权访问此线程。
     * @see        SecurityManager#checkAccess(Thread)
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    /**
     * 返回此线程的字符串表示形式，包括线程的名称、优先级和线程组。
     *
     * @return  此线程的字符串表示形式。
     */
    public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," +
                    group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + "," +
                    "" + "]";
        }
    }

    /**
     * 返回此线程的上下文类加载器。上下文类加载器由线程创建者提供，
     * 用于线程运行时加载类和资源。如果没有设置上下文类加载器，则默认值是父线程的类加载器。
     * 原始线程的上下文类加载器通常设置为用于加载应用程序的类加载器。
     *
     * <p>如果存在安全管理器，并且调用者的类加载器不是 null，也不是上下文类加载器的祖先，则此方法将调用安全管理器的
     * {@link SecurityManager#checkPermission(java.security.Permission) checkPermission} 方法，
     * 使用 {@link RuntimePermission RuntimePermission}{@code ("getClassLoader")} 权限来验证是否允许检索上下文类加载器。
     *
     * @return  此线程的上下文类加载器，或 null 表示系统类加载器（如果失败，则为引导类加载器）
     *
     * @throws  SecurityException
     *          如果当前线程无法获取上下文类加载器
     *
     * @since 1.2
     */
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        if (contextClassLoader == null)
            return null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader.checkClassLoaderPermission(contextClassLoader,
                    Reflection.getCallerClass());
        }
        return contextClassLoader;
    }

    /**
     * 设置此线程的上下文类加载器。上下文类加载器可以在创建线程时设置，
     * 并允许线程创建者为在线程中运行的代码提供适当的类加载器，用于加载类和资源。
     *
     * <p>如果存在安全管理器，则会调用其 {@link SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * 方法，使用 {@link RuntimePermission RuntimePermission}{@code ("setContextClassLoader")} 权限来验证是否允许设置上下文类加载器。
     *
     * @param  cl
     *         此线程的上下文类加载器，或 null 表示系统类加载器（如果失败，则为引导类加载器）
     *
     * @throws  SecurityException
     *          如果当前线程无法设置上下文类加载器
     *
     * @since 1.2
     */
    public void setContextClassLoader(ClassLoader cl) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
        contextClassLoader = cl;
    }
    /**
     * 返回 true 当且仅当当前线程持有指定对象的监视器锁。
     *
     * <p> 此方法设计为允许程序断言当前线程已持有指定的锁：
     * <pre>
     *     assert Thread.holdsLock(obj);
     * </pre>
     *
     * @param  obj  要测试锁所有权的对象
     * @throws NullPointerException 如果 obj 为 null
     * @return true 如果当前线程持有指定对象的监视器锁
     * @since 1.4
     */
    public static native boolean holdsLock(Object obj);

    private static final StackTraceElement[] EMPTY_STACK_TRACE
            = new StackTraceElement[0];

    /**
     * 返回表示此线程堆栈转储的堆栈帧数组。
     * 如果该线程尚未启动、已经启动但尚未被系统调度运行或已经终止，则此方法将返回一个长度为零的数组。
     * 如果返回的数组长度不为零，则数组的第一个元素表示堆栈的顶部，它是最近的一个方法调用。
     * 数组的最后一个元素表示堆栈的底部，它是最不最近的方法调用。
     *
     * <p> 如果存在安全管理器，并且此线程不是当前线程，则安全管理器的
     * <tt>checkPermission</tt> 方法会被调用，使用
     * <tt>RuntimePermission("getStackTrace")</tt> 权限，以查看是否可以获取堆栈跟踪。
     *
     * <p> 某些虚拟机可能会在某些情况下省略一个或多个堆栈帧。在极端情况下，虚拟机可能没有该线程的堆栈跟踪信息，允许此方法返回一个长度为零的数组。
     *
     * @return 一个表示堆栈帧的 <tt>StackTraceElement</tt> 数组
     *
     * @throws SecurityException
     *          如果存在安全管理器，并且其 <tt>checkPermission</tt> 方法不允许获取该线程的堆栈跟踪
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     * @since 1.5
     */
    public StackTraceElement[] getStackTrace() {
        if (this != Thread.currentThread()) {
            // 检查 getStackTrace 权限
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(
                        SecurityConstants.GET_STACK_TRACE_PERMISSION);
            }
            // 优化：如果线程尚未启动或已终止，则不调用 JVM
            if (!isAlive()) {
                return EMPTY_STACK_TRACE;
            }
            StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
            StackTraceElement[] stackTrace = stackTraceArray[0];
            // 一个在前面的 isAlive 调用时仍然存活的线程可能已终止，因此可能没有堆栈跟踪
            if (stackTrace == null) {
                stackTrace = EMPTY_STACK_TRACE;
            }
            return stackTrace;
        } else {
            // 当前线程不需要 JVM 的帮助
            return (new Exception()).getStackTrace();
        }
    }

    /**
     * 返回所有活动线程的堆栈跟踪映射。映射的键是线程，每个映射值是一个表示该线程堆栈转储的
     * <tt>StackTraceElement</tt> 数组。返回的堆栈跟踪格式与 {@link #getStackTrace getStackTrace} 方法指定的格式相同。
     *
     * <p> 线程在调用此方法时可能正在执行。每个线程的堆栈跟踪只表示一个快照，每个堆栈跟踪可能在不同时间获取。
     * 如果虚拟机没有关于某个线程的堆栈跟踪信息，则在映射值中返回长度为零的数组。
     *
     * <p> 如果存在安全管理器，则安全管理器的 <tt>checkPermission</tt> 方法将被调用，使用
     * <tt>RuntimePermission("getStackTrace")</tt> 和 <tt>RuntimePermission("modifyThreadGroup")</tt> 权限，以查看是否可以获取所有线程的堆栈跟踪。
     *
     * @return 一个从 <tt>Thread</tt> 到 <tt>StackTraceElement</tt> 数组的映射，表示相应线程的堆栈跟踪
     *
     * @throws SecurityException
     *          如果存在安全管理器，并且其 <tt>checkPermission</tt> 方法不允许获取该线程的堆栈跟踪
     * @see #getStackTrace
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     * @since 1.5
     */
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // 检查 getStackTrace 权限
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                    SecurityConstants.GET_STACK_TRACE_PERMISSION);
            security.checkPermission(
                    SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        }

        // 获取所有线程的快照
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for (int i = 0; i < threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if (stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // 否则线程已终止，因此不将其放入映射
        }
        return m;
    }

    private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
            new RuntimePermission("enableContextClassLoaderOverride");

    /** 缓存子类安全审计结果 */
    /* 在未来发布版本中替换为 ConcurrentReferenceHashMap */
    private static class Caches {
        /** 缓存子类安全审计结果 */
        static final ConcurrentMap<WeakClassKey, Boolean> subclassAudits =
                new ConcurrentHashMap<>();

        /** 存储已审计子类的弱引用队列 */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue =
                new ReferenceQueue<>();
    }

    /**
     * 验证这个（可能是子类的）实例是否可以在不违反安全约束的情况下被构造：
     * 子类不能覆盖安全敏感的非 final 方法，否则会检查 "enableContextClassLoaderOverride" 运行时权限。
     */
    private static boolean isCCLOverridden(Class<?> cl) {
        if (cl == Thread.class) {
            return false;
        }

        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if (result == null) {
            result = Boolean.valueOf(auditSubclass(cl));
            Caches.subclassAudits.putIfAbsent(key, result);
        }

        return result.booleanValue();
    }

    /**
     * 对给定的子类执行反射检查，以验证它是否没有覆盖安全敏感的非 final 方法。
     * 如果子类覆盖了任何这些方法，返回 true，否则返回 false。
     */
    private static boolean auditSubclass(final Class<?> subcl) {
        Boolean result = AccessController.doPrivileged(
                new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        for (Class<?> cl = subcl;
                             cl != Thread.class;
                             cl = cl.getSuperclass()) {
                            try {
                                cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                                return Boolean.TRUE;
                            } catch (NoSuchMethodException ex) {
                                // 忽略
                            }
                            try {
                                Class<?>[] params = {ClassLoader.class};
                                cl.getDeclaredMethod("setContextClassLoader", params);
                                return Boolean.TRUE;
                            } catch (NoSuchMethodException ex) {
                                // 忽略
                            }
                        }
                        return Boolean.FALSE;
                    }
                }
        );
        return result.booleanValue();
    }

    private static native StackTraceElement[][] dumpThreads(Thread[] threads);
    private static native Thread[] getThreads();

    /**
     * 返回此线程的标识符。线程 ID 是在线程创建时生成的正数 <tt>long</tt> 值。
     * 线程 ID 是唯一的，并在其生命周期中保持不变。线程终止后，线程 ID 可能会被重用。
     *
     * @return 此线程的 ID。
     * @since 1.5
     */
    public long getId() {
        return tid;
    }

    /**
     * 线程状态。线程可以处于以下状态之一：
     * <ul>
     * <li>{@link #NEW}<br>
     *     线程尚未启动时处于此状态。
     *     </li>
     * <li>{@link #RUNNABLE}<br>
     *     线程正在 Java 虚拟机中执行时处于此状态。
     *     </li>
     * <li>{@link #BLOCKED}<br>
     *     线程阻塞等待监视器锁时处于此状态。
     *     </li>
     * <li>{@link #WAITING}<br>
     *     线程无限期等待另一个线程执行特定操作时处于此状态。
     *     </li>
     * <li>{@link #TIMED_WAITING}<br>
     *     线程等待另一个线程在指定时间内执行某个操作时处于此状态。
     *     </li>
     * <li>{@link #TERMINATED}<br>
     *     线程已退出时处于此状态。
     *     </li>
     * </ul>
     *
     * <p>
     * 线程在任一时刻只能处于一种状态。这些状态是虚拟机状态，并不反映操作系统线程状态。
     *
     * @since   1.5
     * @see #getState
     */
    public enum State {
        /**
         * 线程尚未启动的状态。
         */
        NEW,

        /**
         * 可运行的线程状态。线程在可运行状态下正在 Java 虚拟机中执行，
         * 但可能正在等待操作系统的其他资源，例如处理器。
         */
        RUNNABLE,

        /**
         * 阻塞等待监视器锁的线程状态。线程在阻塞状态下，正在等待获取监视器锁，以进入同步代码块或方法。
         */
        BLOCKED,

        /**
         * 等待线程的状态。由于调用了以下方法之一，线程处于等待状态：
         * <ul>
         *   <li>{@link Object#wait() Object.wait} 无超时</li>
         *   <li>{@link #join() Thread.join} 无超时</li>
         *   <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         *
         * <p>等待状态的线程在等待另一个线程执行某个特定的操作。
         *
         * 例如，调用 <tt>Object.wait()</tt> 的线程正在等待另一个线程调用 <tt>Object.notify()</tt> 或 <tt>Object.notifyAll()</tt>。
         */
        WAITING,

        /**
         * 带有指定等待时间的等待线程状态。由于调用了以下方法之一，线程处于定时等待状态：
         * <ul>
         *   <li>{@link #sleep Thread.sleep}</li>
         *   <li>{@link Object#wait(long) Object.wait} 带超时</li>
         *   <li>{@link #join(long) Thread.join} 带超时</li>
         *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
         *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
         * </ul>
         */
        TIMED_WAITING,

        /**
         * 已终止线程的状态。线程已完成执行。
         */
        TERMINATED;
    }

    /**
     * 返回此线程的状态。
     * 该方法设计用于监视系统状态，而不是用于同步控制。
     *
     * @return 此线程的状态。
     * @since 1.5
     */
    public java.lang.Thread.State getState() {
        // 获取当前线程状态
        return sun.misc.VM.toThreadState(threadStatus);
    }
    // 添加于 JSR-166

    /**
     * 用于在 <tt>Thread</tt> 因未捕获的异常突然终止时调用的处理程序接口。
     * <p>当线程由于未捕获的异常即将终止时，Java 虚拟机会查询该线程的
     * <tt>UncaughtExceptionHandler</tt>，并调用处理程序的 <tt>uncaughtException</tt>
     * 方法，传递线程和异常作为参数。
     * 如果线程没有显式设置 <tt>UncaughtExceptionHandler</tt>，那么其 <tt>ThreadGroup</tt>
     * 对象将作为其 <tt>UncaughtExceptionHandler</tt>。如果 <tt>ThreadGroup</tt> 对象没有
     * 处理异常的特殊需求，它可以将调用转发给 {@linkplain #getDefaultUncaughtExceptionHandler
     * 默认未捕获的异常处理程序}。
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @see #setUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        /**
         * 当给定线程由于给定的未捕获异常终止时调用此方法。
         * <p>该方法抛出的任何异常将被 Java 虚拟机忽略。
         *
         * @param t 发生异常的线程
         * @param e 未捕获的异常
         */
        void uncaughtException(Thread t, Throwable e);
    }

    // 仅当显式设置时有效
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // 仅当显式设置时有效
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * 设置当线程由于未捕获的异常突然终止时调用的默认处理程序，且没有为该线程定义其他处理程序。
     *
     * <p>未捕获的异常处理首先由线程控制，然后由线程的 {@link ThreadGroup} 对象控制，最后由
     * 默认的未捕获异常处理程序控制。如果线程没有显式设置未捕获的异常处理程序，且线程的线程组
     * （包括父线程组）没有专门处理 <tt>uncaughtException</tt> 方法，则会调用默认处理程序的
     * <tt>uncaughtException</tt> 方法。
     *
     * <p>通过设置默认的未捕获异常处理程序，应用程序可以更改未捕获异常的处理方式（例如，将其记录到
     * 特定设备或文件），以替代系统提供的默认行为。
     *
     * <p>注意，默认的未捕获异常处理程序通常不应将调用委托给线程的 <tt>ThreadGroup</tt> 对象，
     * 因为这可能导致无限递归。
     *
     * @param eh 用作默认未捕获异常处理程序的对象。如果 <tt>null</tt>，则没有默认处理程序。
     *
     * @throws SecurityException 如果存在安全管理器，并且它拒绝 <tt>{@link RuntimePermission}
     *         (&quot;setDefaultUncaughtExceptionHandler&quot;)</tt>。
     *
     * @see #setUncaughtExceptionHandler
     * @see #getUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                    new RuntimePermission("setDefaultUncaughtExceptionHandler")
            );
        }
        defaultUncaughtExceptionHandler = eh;
    }

    /**
     * 返回默认的未捕获异常处理程序。如果返回值为 <tt>null</tt>，则没有默认处理程序。
     *
     * @return 用于所有线程的默认未捕获异常处理程序
     * @since 1.5
     * @see #setDefaultUncaughtExceptionHandler
     */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    /**
     * 返回此线程在因未捕获异常突然终止时调用的处理程序。
     * 如果该线程没有显式设置未捕获异常处理程序，则返回其 <tt>ThreadGroup</tt> 对象，
     * 除非该线程已终止，在这种情况下返回 <tt>null</tt>。
     *
     * @return 此线程的未捕获异常处理程序
     * @since 1.5
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ?
                uncaughtExceptionHandler : group;
    }

    /**
     * 设置当此线程由于未捕获的异常突然终止时调用的处理程序。
     * <p>线程可以通过显式设置其未捕获异常处理程序来完全控制如何响应未捕获异常。
     * 如果没有设置此类处理程序，则线程的 <tt>ThreadGroup</tt> 对象充当其处理程序。
     *
     * @param eh 用作此线程未捕获异常处理程序的对象。如果 <tt>null</tt>，则此线程没有显式处理程序。
     *
     * @throws SecurityException 如果当前线程不允许修改此线程。
     * @see #setDefaultUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }

    /**
     * 将未捕获的异常分派给处理程序。此方法仅供 JVM 调用。
     */
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }

    /**
     * 移除指定映射中所有已在指定引用队列中排队的键。
     */
    static void processQueue(ReferenceQueue<Class<?>> queue,
                             ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        Reference<? extends Class<?>> ref;
        while ((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    /**
     * 弱引用的 Class 对象的键。
     */
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * 保存引用对象的身份哈希码，以保持在引用对象被清除后的哈希码一致性。
         */
        private final int hash;

        /**
         * 创建一个新的 WeakClassKey 指向给定对象，并在队列中注册。
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }

        /**
         * 返回原始引用对象的身份哈希码。
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * 如果给定对象与此 WeakClassKey 实例相同，或者如果此对象的引用没有被清除，
         * 且给定对象是具有相同非空引用的另一个 WeakClassKey 实例，则返回 true。
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) &&
                        (referent == ((WeakClassKey) obj).get());
            }
            return false;
        }
    }

    // 以下三个字段由 java.util.concurrent.ThreadLocalRandom 类独占管理
    // 这些字段用于构建并发代码中的高性能伪随机数生成器 (PRNG)，避免意外的虚假共享
    // 因此，字段用 @Contended 隔离

    /** ThreadLocalRandom 的当前种子 */
    @sun.misc.Contended("tlr")
    long threadLocalRandomSeed;

    /** 探针哈希值；如果 threadLocalRandomSeed 已初始化则非零 */
    @sun.misc.Contended("tlr")
    int threadLocalRandomProbe;

    /** 与公共 ThreadLocalRandom 序列隔离的次级种子 */
    @sun.misc.Contended("tlr")
    int threadLocalRandomSecondarySeed;

    /* 一些私有辅助方法 */
    private native void setPriority0(int newPriority);
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
    private native void setNativeName(String name);
}


