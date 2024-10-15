package org.top.java.lang.source;

import sun.misc.VM;

import java.io.PrintStream;
import java.util.Arrays; /**
 * @Author zack
 * @Description
 * @Date 2024/10/15 上午7:54
 */
public
class ThreadGroup implements Thread.UncaughtExceptionHandler {
    private final ThreadGroup parent; // 父线程组
    String name; // 线程组名称
    int maxPriority; // 最大优先级
    boolean destroyed; // 是否已销毁
    boolean daemon; // 是否为守护线程组
    boolean vmAllowSuspension; // 虚拟机是否允许挂起

    int nUnstartedThreads = 0; // 未启动线程数
    int nthreads; // 活动线程数
    Thread threads[]; // 线程数组

    int ngroups; // 子线程组数量
    ThreadGroup groups[]; // 子线程组数组

    /**
     * 创建一个不属于任何线程组的空线程组。
     * 此方法用于创建系统线程组。
     */
    private ThreadGroup() {     // 从C代码中调用
        this.name = "system"; // 名称为 "system"
        this.maxPriority = Thread.MAX_PRIORITY; // 最大优先级为线程的最大优先级
        this.parent = null; // 没有父线程组
    }

    /**
     * 构造一个新的线程组。新组的父组是当前运行线程的线程组。
     * <p>
     * 父线程组的 <code>checkAccess</code> 方法会被调用，没有参数；这可能会导致安全异常。
     *
     * @param   name   新线程组的名称。
     * @exception  SecurityException  如果当前线程无法在指定线程组中创建线程。
     * @see     java.lang.ThreadGroup#checkAccess()
     * @since   JDK1.0
     */
    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    /**
     * 创建一个新的线程组。新组的父组是指定的线程组。
     * <p>
     * 父线程组的 <code>checkAccess</code> 方法会被调用，没有参数；这可能会导致安全异常。
     *
     * @param     parent   父线程组。
     * @param     name     新线程组的名称。
     * @exception  NullPointerException  如果线程组参数是 <code>null</code>。
     * @exception  SecurityException  如果当前线程无法在指定线程组中创建线程。
     * @see     SecurityException
     * @see     java.lang.ThreadGroup#checkAccess()
     * @since   JDK1.0
     */
    public ThreadGroup(ThreadGroup parent, String name) {
        this(checkParentAccess(parent), parent, name);
    }

    private ThreadGroup(Void unused, ThreadGroup parent, String name) {
        this.name = name; // 设置线程组名称
        this.maxPriority = parent.maxPriority; // 继承父线程组的最大优先级
        this.daemon = parent.daemon; // 继承父线程组的守护线程状态
        this.vmAllowSuspension = parent.vmAllowSuspension; // 继承父线程组的挂起允许状态
        this.parent = parent; // 设置父线程组
        parent.add(this); // 将自己添加到父线程组
    }

    /*
     * @throws  NullPointerException  如果父线程组是 {@code null}
     * @throws  SecurityException     如果当前线程无法在指定线程组中创建线程。
     */
    private static Void checkParentAccess(ThreadGroup parent) {
        parent.checkAccess();
        return null;
    }

    /**
     * 返回此线程组的名称。
     *
     * @return  线程组的名称。
     * @since   JDK1.0
     */
    public final String getName() {
        return name;
    }

    /**
     * 返回此线程组的父线程组。
     * <p>
     * 首先，如果父线程组不为 <code>null</code>，则调用父线程组的 <code>checkAccess</code> 方法，
     * 这可能会导致安全异常。
     *
     * @return  父线程组。如果是顶级线程组，则返回 <code>null</code>。
     * @exception  SecurityException  如果当前线程无法修改此线程组。
     * @see        java.lang.ThreadGroup#checkAccess()
     * @see        SecurityException
     * @see        RuntimePermission
     * @since   JDK1.0
     */
    public final ThreadGroup getParent() {
        if (parent != null) {
            parent.checkAccess();
        }
        return parent;
    }

    /**
     * 返回此线程组的最大优先级。
     * 该组内的线程不能具有比此最大优先级更高的优先级。
     *
     * @return  此线程组中线程的最大优先级。
     * @see     #setMaxPriority
     * @since   JDK1.0
     */
    public final int getMaxPriority() {
        return maxPriority;
    }

    /**
     * 测试此线程组是否是守护线程组。
     * 当最后一个线程停止或最后一个子线程组被销毁时，守护线程组会自动销毁。
     *
     * @return  如果是守护线程组则返回 <code>true</code>，否则返回 <code>false</code>。
     * @since   JDK1.0
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * 测试此线程组是否已被销毁。
     *
     * @return  如果此对象已销毁则返回 true。
     * @since   JDK1.1
     */
    public synchronized boolean isDestroyed() {
        return destroyed;
    }

    /**
     * 更改此线程组的守护状态。
     * <p>
     * 首先调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 当最后一个线程停止或最后一个子线程组被销毁时，守护线程组会自动销毁。
     *
     * @param      daemon   如果 <code>true</code>，则将此线程组标记为守护线程组；
     *                      否则标记为普通线程组。
     * @exception  SecurityException  如果当前线程无法修改此线程组。
     * @see        SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void setDaemon(boolean daemon) {
        checkAccess();
        this.daemon = daemon;
    }

    /**
     * 设置线程组的最大优先级。此线程组中的线程如果已经有更高的优先级，则不受影响。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 如果 <code>pri</code> 参数小于 {@link Thread#MIN_PRIORITY} 或大于
     * {@link Thread#MAX_PRIORITY}，则线程组的最大优先级保持不变。
     * <p>
     * 否则，此线程组的最大优先级将设置为指定的 <code>pri</code> 和父线程组允许的最大优先级
     * 中较小的一个。如果此线程组是系统线程组（没有父线程组），则最大优先级直接设置为 <code>pri</code>。
     * 然后此方法递归地为属于此线程组的每个子线程组调用，传递 <code>pri</code> 作为参数。
     *
     * @param      pri   线程组的新优先级。
     * @exception  SecurityException  如果当前线程无法修改此线程组。
     * @see        #getMaxPriority
     * @see        SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void setMaxPriority(int pri) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (pri < Thread.MIN_PRIORITY || pri > Thread.MAX_PRIORITY) {
                return;
            }
            maxPriority = (parent != null) ? Math.min(pri, parent.maxPriority) : pri;
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].setMaxPriority(pri);
        }
    }

    /**
     * 测试此线程组是否是指定线程组或其祖先线程组之一。
     *
     * @param   g   一个线程组。
     * @return  <code>true</code> 如果此线程组是指定线程组或其祖先线程组之一；
     *          否则返回 <code>false</code>。
     * @since   JDK1.0
     */
    public final boolean parentOf(ThreadGroup g) {
        for (; g != null; g = g.parent) {
            if (g == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确定当前运行的线程是否有权限修改此线程组。
     * <p>
     * 如果有安全管理器，它的 <code>checkAccess</code> 方法将使用此线程组作为参数被调用。
     * 这可能会导致抛出 <code>SecurityException</code>。
     *
     * @exception  SecurityException  如果当前线程不允许访问此线程组。
     * @see        java.lang.SecurityManager#checkAccess(java.lang.ThreadGroup)
     * @since      JDK1.0
     */
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    /**
     * 返回此线程组及其子线程组中活动线程的估计数量。递归遍历此线程组中的所有子线程组。
     *
     * <p> 返回的值只是一个估计值，因为线程数可能在此方法遍历内部数据结构时动态变化，
     * 并且可能受到某些系统线程的影响。此方法主要用于调试和监控目的。
     *
     * @return  此线程组及其所有子线程组中活动线程的估计数量。
     * @since   JDK1.0
     */
    public int activeCount() {
        int result;
        // 快照子组数据，以便在子线程组计算时不持有此锁
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            result = nthreads;
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            result += groupsSnapshot[i].activeCount();
        }
        return result;
    }

    /**
     * 将此线程组及其子线程组中的每个活动线程复制到指定数组中。
     *
     * <p> 此方法的调用行为与以下调用相同：
     *
     * <blockquote>
     * {@linkplain #enumerate(Thread[], boolean) enumerate}{@code (list, true)}
     * </blockquote>
     *
     * @param  list
     *         将线程列表放入的数组
     *
     * @return  放入数组中的线程数
     *
     * @throws  SecurityException
     *          如果 {@linkplain #checkAccess checkAccess} 确定当前线程无法访问此线程组
     *
     * @since   JDK1.0
     */
    public int enumerate(Thread list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * 将此线程组中的每个活动线程复制到指定的数组中。如果 {@code recurse} 为 {@code true}，
     * 则此方法会递归遍历此线程组的所有子线程组，并将这些子线程组中的每个活动线程引用也包含在内。
     * 如果数组太短，无法容纳所有线程，则多余的线程会被静默忽略。
     *
     * <p> 应用程序可能会使用 {@linkplain #activeCount activeCount} 方法来估计数组的大小，
     * 但如果数组太短，无法容纳所有线程，<i>多余的线程将被静默忽略。</i>
     * 如果获取此线程组中的每个活动线程对调用者至关重要，则调用者应验证返回的整数值是否严格小于 {@code list} 的长度。
     *
     * <p> 由于此方法中固有的竞态条件，建议仅将该方法用于调试和监控目的。
     *
     * @param  list
     *         要放置线程列表的数组
     *
     * @param  recurse
     *         如果为 {@code true}，递归遍历此线程组的所有子线程组
     *
     * @return  放入数组中的线程数
     *
     * @throws  SecurityException
     *          如果 {@linkplain #checkAccess checkAccess} 确定当前线程无法访问此线程组
     *
     * @since   JDK1.0
     */
    public int enumerate(Thread list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(Thread list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            int nt = nthreads;
            if (nt > list.length - n) {
                nt = list.length - n;
            }
            for (int i = 0; i < nt; i++) {
                if (threads[i].isAlive()) {
                    list[n++] = threads[i];
                }
            }
            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                }
            }
        }
        if (recurse) {
            for (int i = 0; i < ngroupsSnapshot; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    /**
     * 返回此线程组及其子线程组中活动线程组的估计数量。递归遍历此线程组中的所有子线程组。
     *
     * <p> 返回的值只是一个估计值，因为线程组数量可能在此方法遍历内部数据结构时动态变化。
     * 此方法主要用于调试和监控目的。
     *
     * @return  此线程组及其子线程组中活动线程组的估计数量
     *
     * @since   JDK1.0
     */
    public int activeGroupCount() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        int n = ngroupsSnapshot;
        for (int i = 0; i < ngroupsSnapshot; i++) {
            n += groupsSnapshot[i].activeGroupCount();
        }
        return n;
    }

    /**
     * 将此线程组及其子线程组中的每个活动子线程组的引用复制到指定数组中。
     * 此方法的调用行为与以下调用相同：
     *
     * <blockquote>
     * {@linkplain #enumerate(ThreadGroup[], boolean) enumerate}{@code (list, true)}
     * </blockquote>
     *
     * @param  list
     *         要放置线程组列表的数组
     *
     * @return  放入数组中的线程组数
     *
     * @throws  SecurityException
     *          如果 {@linkplain #checkAccess checkAccess} 确定当前线程无法访问此线程组
     *
     * @since   JDK1.0
     */
    public int enumerate(ThreadGroup list[]) {
        checkAccess();
        return enumerate(list, 0, true);
    }

    /**
     * 将此线程组中的每个活动子线程组复制到指定数组中。如果 {@code recurse} 为 {@code true}，
     * 则此方法会递归遍历此线程组的所有子线程组，并将这些子线程组中的每个活动子线程组引用也包含在内。
     *
     * <p> 应用程序可能会使用 {@linkplain #activeGroupCount activeGroupCount} 方法来估计数组的大小，
     * 但如果数组太短，无法容纳所有线程组，<i>多余的线程组将被静默忽略。</i>
     * 如果获取此线程组中的每个活动子线程组对调用者至关重要，则调用者应验证返回的整数值是否严格小于 {@code list} 的长度。
     *
     * <p> 由于此方法中固有的竞态条件，建议仅将该方法用于调试和监控目的。
     *
     * @param  list
     *         要放置线程组列表的数组
     *
     * @param  recurse
     *         如果为 {@code true}，递归遍历此线程组的所有子线程组
     *
     * @return  放入数组中的线程组数
     *
     * @throws  SecurityException
     *          如果 {@linkplain #checkAccess checkAccess} 确定当前线程无法访问此线程组
     *
     * @since   JDK1.0
     */
    public int enumerate(ThreadGroup list[], boolean recurse) {
        checkAccess();
        return enumerate(list, 0, recurse);
    }

    private int enumerate(ThreadGroup list[], int n, boolean recurse) {
        int ngroupsSnapshot = 0;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            if (destroyed) {
                return 0;
            }
            int ng = ngroups;
            if (ng > list.length - n) {
                ng = list.length - n;
            }
            if (ng > 0) {
                System.arraycopy(groups, 0, list, n, ng);
                n += ng;
            }
            if (recurse) {
                ngroupsSnapshot = ngroups;
                if (groups != null) {
                    groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
                }
            }
        }
        if (recurse) {
            for (int i = 0; i < ngroupsSnapshot; i++) {
                n = groupsSnapshot[i].enumerate(list, n, true);
            }
        }
        return n;
    }

    /**
     * 停止此线程组中的所有线程。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 然后，此方法调用此线程组及其所有子线程组中的所有线程的 <code>stop</code> 方法。
     *
     * @exception  SecurityException  如果当前线程无法访问此线程组或线程组中的任何线程。
     * @see        SecurityException
     * @see        java.lang.Thread#stop()
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated    此方法本质上是不安全的。有关详细信息，请参见 {@link Thread#stop}。
     */
    @Deprecated
    public final void stop() {
        if (stopOrSuspend(false))
            Thread.currentThread().stop();
    }

    /**
     * 中断此线程组中的所有线程。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 然后，此方法调用此线程组及其所有子线程组中的所有线程的 <code>interrupt</code> 方法。
     *
     * @exception  SecurityException  如果当前线程无法访问此线程组或线程组中的任何线程。
     * @see        java.lang.Thread#interrupt()
     * @see        SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      1.2
     */
    public final void interrupt() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            checkAccess();
            for (int i = 0; i < nthreads; i++) {
                threads[i].interrupt();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].interrupt();
        }
    }

    /**
     * 挂起此线程组中的所有线程。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 然后，此方法调用此线程组及其所有子线程组中的所有线程的 <code>suspend</code> 方法。
     *
     * @exception  SecurityException  如果当前线程无法访问此线程组或线程组中的任何线程。
     * @see        java.lang.Thread#suspend()
     * @see        SecurityException
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated 此方法本质上容易导致死锁。有关详细信息，请参见 {@link Thread#suspend}。
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void suspend() {
        if (stopOrSuspend(true))
            Thread.currentThread().suspend();
    }

    /**
     * 辅助方法：递归停止或挂起（由布尔参数决定）此线程组及其子线程组中的所有线程，除了当前线程。
     * 如果（且仅当）在此线程组或其子线程组中找到当前线程时，此方法返回 true。
     */
    @SuppressWarnings("deprecation")
    private boolean stopOrSuspend(boolean suspend) {
        boolean suicide = false;
        Thread us = Thread.currentThread();
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot = null;
        synchronized (this) {
            checkAccess();
            for (int i = 0; i < nthreads; i++) {
                if (threads[i] == us)
                    suicide = true;
                else if (suspend)
                    threads[i].suspend();
                else
                    threads[i].stop();
            }

            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++)
            suicide = groupsSnapshot[i].stopOrSuspend(suspend) || suicide;

        return suicide;
    }

    /**
     * 恢复此线程组中的所有线程。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     * <p>
     * 然后，此方法调用此线程组及其所有子线程组中的所有线程的 <code>resume</code> 方法。
     *
     * @exception  SecurityException  如果当前线程无法访问此线程组或线程组中的任何线程。
     * @see        java.lang.Thread#resume()
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     * @deprecated 此方法仅与 <tt>Thread.suspend</tt> 和 <tt>ThreadGroup.suspend</tt> 一起使用，
     *              它们都已被弃用，因为它们本质上容易导致死锁。有关详细信息，请参见 {@link Thread#suspend}。
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public final void resume() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            for (int i = 0; i < nthreads; i++) {
                threads[i].resume();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].resume();
        }
    }

    /**
     * 销毁此线程组及其所有子线程组。此线程组必须为空，表示该线程组中的所有线程都已停止。
     * <p>
     * 首先，调用此线程组的 <code>checkAccess</code> 方法，没有参数；这可能会导致安全异常。
     *
     * @exception  IllegalThreadStateException  如果线程组未空或线程组已被销毁。
     * @exception  SecurityException  如果当前线程无法修改此线程组。
     * @see        java.lang.ThreadGroup#checkAccess()
     * @since      JDK1.0
     */
    public final void destroy() {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            checkAccess();
            if (destroyed || (nthreads > 0)) {
                throw new IllegalThreadStateException();
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
            if (parent != null) {
                destroyed = true;
                ngroups = 0;
                groups = null;
                nthreads = 0;
                threads = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].destroy();
        }
        if (parent != null) {
            parent.remove(this);
        }
    }

    /**
     * 将指定的线程组添加到此组中。
     * @param g 要添加的指定线程组
     * @exception IllegalThreadStateException 如果线程组已被销毁。
     */
    private final void add(ThreadGroup g) {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            if (groups == null) {
                groups = new ThreadGroup[4];
            } else if (ngroups == groups.length) {
                groups = Arrays.copyOf(groups, ngroups * 2);
            }
            groups[ngroups] = g;
            // 最后执行此操作，以防线程被终止
            ngroups++;
        }
    }

    /**
     * 从此组中删除指定的线程组。
     * @param g 要删除的线程组
     * @return 如果此线程已被销毁，则为 true。
     */
    private void remove(ThreadGroup g) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            for (int i = 0; i < ngroups; i++) {
                if (groups[i] == g) {
                    ngroups -= 1;
                    System.arraycopy(groups, i + 1, groups, i, ngroups - i);
                    // 清除对已销毁线程组的引用，以便垃圾收集器收集它
                    groups[ngroups] = null;
                    break;
                }
            }
            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                    (nUnstartedThreads == 0) && (ngroups == 0)) {
                destroy();
            }
        }
    }

    /**
     * 增加线程组中的未启动线程计数。
     * 未启动的线程不会被添加到线程组中，因此如果它们从未启动，它们可以被垃圾收集。
     * 但是它们必须被计算在内，以防止包含未启动线程的守护线程组被销毁。
     */
    void addUnstarted() {
        synchronized(this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            nUnstartedThreads++;
        }
    }

    /**
     * 将指定的线程添加到此线程组中。
     *
     * <p> 注意：此方法从库代码和虚拟机（VM）两者中调用。它从虚拟机中调用，以将某些系统线程添加到系统线程组。
     *
     * @param  t
     *         要添加的线程
     *
     * @throws  IllegalThreadStateException
     *          如果线程组已被销毁
     */
    void add(Thread t) {
        synchronized (this) {
            if (destroyed) {
                throw new IllegalThreadStateException();
            }
            if (threads == null) {
                threads = new Thread[4];
            } else if (nthreads == threads.length) {
                threads = Arrays.copyOf(threads, nthreads * 2);
            }
            threads[nthreads] = t;

            // 最后执行此操作，以防线程被终止
            nthreads++;

            // 线程现在是组中的完全成员，即使它可能已经启动或未启动。它将防止组被销毁，因此未启动的线程计数递减。
            nUnstartedThreads--;
        }
    }

    /**
     * 通知组线程 {@code t} 启动失败。
     *
     * <p> 此线程组的状态将回滚，就像从未尝试启动线程一样。该线程再次被视为线程组的未启动成员，随后可再次尝试启动该线程。
     *
     * @param  t
     *         调用了其 start 方法的线程
     */
    void threadStartFailed(Thread t) {
        synchronized (this) {
            remove(t);
            nUnstartedThreads++;
        }
    }

    /**
     * 通知组线程 {@code t} 已终止。
     *
     * <p> 如果以下所有条件都成立，则销毁该组：这是一个守护线程组；该组中没有更多的存活线程或未启动的线程；该线程组中没有子组。
     *
     * @param  t
     *         已终止的线程
     */
    void threadTerminated(Thread t) {
        synchronized (this) {
            remove(t);

            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                    (nUnstartedThreads == 0) && (ngroups == 0)) {
                destroy();
            }
        }
    }

    /**
     * 从此线程组中移除指定的线程。对已销毁的线程组调用此方法无效。
     *
     * @param  t
     *         要移除的线程
     */
    private void remove(Thread t) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            for (int i = 0; i < nthreads; i++) {
                if (threads[i] == t) {
                    System.arraycopy(threads, i + 1, threads, i, --nthreads - i);
                    // 清除对已终止线程的引用，以便垃圾收集器能够收集它。
                    threads[nthreads] = null;
                    break;
                }
            }
        }
    }

    /**
     * 将此线程组的相关信息打印到标准输出。此方法仅用于调试。
     *
     * @since   JDK1.0
     */
    public void list() {
        list(System.out, 0);
    }

    void list(PrintStream out, int indent) {
        int ngroupsSnapshot;
        ThreadGroup[] groupsSnapshot;
        synchronized (this) {
            for (int j = 0; j < indent; j++) {
                out.print(" ");
            }
            out.println(this);
            indent += 4;
            for (int i = 0; i < nthreads; i++) {
                for (int j = 0; j < indent; j++) {
                    out.print(" ");
                }
                out.println(threads[i]);
            }
            ngroupsSnapshot = ngroups;
            if (groups != null) {
                groupsSnapshot = Arrays.copyOf(groups, ngroupsSnapshot);
            } else {
                groupsSnapshot = null;
            }
        }
        for (int i = 0; i < ngroupsSnapshot; i++) {
            groupsSnapshot[i].list(out, indent);
        }
    }

    /**
     * 当线程组中的线程由于未捕获的异常停止运行，并且该线程未安装特定的 {@link Thread.UncaughtExceptionHandler} 时，Java 虚拟机会调用此方法。
     * <p>
     * <code>uncaughtException</code> 方法执行以下操作：
     * <ul>
     * <li>如果此线程组有父线程组，则会使用相同的两个参数调用该父线程组的 <code>uncaughtException</code> 方法。
     * <li>否则，此方法会检查是否已安装 {@linkplain Thread#getDefaultUncaughtExceptionHandler 默认未捕获异常处理程序}，如果安装了，
     *     则会使用相同的两个参数调用其 <code>uncaughtException</code> 方法。
     * <li>否则，此方法将确定 <code>Throwable</code> 参数是否是 {@link ThreadDeath} 的实例。如果是，则不会执行任何特殊操作。
     *     否则，使用线程的 {@link Thread#getName getName} 方法返回的线程名称和使用 <code>Throwable</code> 的 {@link Throwable#printStackTrace printStackTrace} 方法打印的堆栈回溯，
     *     将消息打印到 {@linkplain System#err 标准错误输出流} 中。
     * </ul>
     * <p>
     * 应用程序可以在 <code>ThreadGroup</code> 的子类中重写此方法，以提供对未捕获异常的替代处理方式。
     *
     * @param   t   即将退出的线程。
     * @param   e   未捕获的异常。
     * @since   JDK1.0
     */
    public void uncaughtException(Thread t, Throwable e) {
        if (parent != null) {
            parent.uncaughtException(t, e);
        } else {
            Thread.UncaughtExceptionHandler ueh =
                    Thread.getDefaultUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            } else if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception in thread \""
                        + t.getName() + "\" ");
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * 由 VM 使用，用于控制低内存隐式挂起。
     *
     * @param b 允许或不允许挂起的布尔值
     * @return 成功时返回 true
     * @since   JDK1.1
     * @deprecated 此调用的定义取决于 {@link #suspend}，它已被弃用。此外，此调用的行为从未被指定。
     */
    @Deprecated
    public boolean allowThreadSuspension(boolean b) {
        this.vmAllowSuspension = b;
        if (!b) {
            VM.unsuspendSomeThreads();
        }
        return true;
    }

    /**
     * 返回此线程组的字符串表示形式。
     *
     * @return 线程组的字符串表示形式。
     * @since   JDK1.0
     */
    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}




