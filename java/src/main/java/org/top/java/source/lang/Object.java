package org.top.java.source.lang;

/**
 * 类 {@code Object} 是类层次结构的根。
 * 每个类都将 {@code Object} 作为超类。所有对象，
 * 包括数组，都实现了该类的方法。
 *
 * @author  unascribed
 * @see     Class
 * @since   JDK1.0
 */
public class Object {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
     * 返回此 {@code Object} 的运行时类。返回的 {@code Class} 对象是被表示类的 {@code static synchronized} 方法锁定的对象。
     *
     * <p><b>实际结果类型是 {@code Class<? extends |X|>}，其中 {@code |X|} 是调用 {@code getClass} 的表达式静态类型的擦除。</b> 例如，以下代码片段中不需要强制转换：</p>
     *
     * <p>
     * {@code Number n = 0;                             }<br>
     * {@code Class<? extends Number> c = n.getClass(); }
     * </p>
     *
     * @return 表示此对象运行时类的 {@code Class} 对象。
     * @jls 15.8.2 类字面量
     */
    public final native Class<?> getClass1();

    /**
     * 返回对象的哈希码值。此方法是为了支持哈希表（例如由 {@link java.util.HashMap} 提供的哈希表）而存在的。
     * <p>
     * {@code hashCode} 方法的通用约定是：
     * <ul>
     * <li>在 Java 应用程序的执行过程中，无论何时在同一对象上多次调用此方法，{@code hashCode} 方法必须一致地返回相同的整数，前提是在对象上的 {@code equals} 比较中使用的信息没有被修改。
     *     这个整数不需要在应用程序的一次执行与另一次执行之间保持一致。
     * <li>如果根据 {@code equals(Object)} 方法两个对象相等，那么在这两个对象上调用 {@code hashCode} 方法必须产生相同的整数结果。
     * <li>如果根据 {@link Object#equals(Object)} 方法两个对象不相等，则在这两个对象上调用 {@code hashCode} 方法不要求必须产生不同的整数结果。然而，程序员应该意识到，为不相等的对象生成不同的整数结果可能会提高哈希表的性能。
     * </ul>
     * <p>
     * 在合理可行的范围内，由 {@code Object} 类定义的 hashCode 方法确实会为不同的对象返回不同的整数。（这通常通过将对象的内部地址转换为整数来实现，但 Java&trade; 编程语言并不要求使用这种实现技术。）
     *
     * @return  此对象的哈希码值。
     * @see     Object#equals(Object)
     * @see     java.lang.System#identityHashCode
     */
    public native int hashCode();

    /**
     * 指示其他某个对象是否“等于”此对象。
     * <p>
     * {@code equals} 方法在非空对象引用上实现了等价关系：
     * <ul>
     * <li>它是<i>自反的</i>：对于任何非空引用值
     *     {@code x}，{@code x.equals(x)} 应返回
     *     {@code true}。
     * <li>它是<i>对称的</i>：对于任何非空引用值
     *     {@code x} 和 {@code y}，{@code x.equals(y)}
     *     应返回 {@code true} 当且仅当
     *     {@code y.equals(x)} 返回 {@code true}。
     * <li>它是<i>传递的</i>：对于任何非空引用值
     *     {@code x}、{@code y} 和 {@code z}，如果
     *     {@code x.equals(y)} 返回 {@code true} 且
     *     {@code y.equals(z)} 返回 {@code true}，则
     *     {@code x.equals(z)} 应返回 {@code true}。
     * <li>它是<i>一致的</i>：对于任何非空引用值
     *     {@code x} 和 {@code y}，多次调用
     *     {@code x.equals(y)} 应一致地返回 {@code true}
     *     或一致地返回 {@code false}，前提是
     *     对象上用于 {@code equals} 比较的信息未被修改。
     * <li>对于任何非空引用值 {@code x}，
     *     {@code x.equals(null)} 应返回 {@code false}。
     * </ul>
     * <p>
     * {@code Object} 类的 {@code equals} 方法实现了
     * 对象上最具区分性的等价关系；即，对于任何非空引用值
     * {@code x} 和 {@code y}，此方法返回 {@code true} 当且仅当
     * {@code x} 和 {@code y} 引用同一个对象
     * （{@code x == y} 的值为 {@code true}）。
     * <p>
     * 请注意，通常需要在重写此方法时重写 {@code hashCode}
     * 方法，以维护 {@code hashCode} 方法的通用约定，该约定规定
     * 相等的对象必须具有相等的哈希码。
     *
     * @param   obj   要与之比较的引用对象。
     * @return  {@code true} 如果此对象与 obj
     *          参数相同；否则返回 {@code false}。
     * @see     #hashCode()
     * @see     java.util.HashMap
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * 创建并返回此对象的副本。关于“副本”的确切含义
     * 可能取决于对象的类。通常的意图是，对于任何对象 {@code x}，表达式：
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * 将为 true，并且表达式：
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * 将为 {@code true}，但这些并不是绝对要求。
     * 虽然通常情况下：
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * 将为 {@code true}，但这并不是绝对要求。
     * <p>
     * 按照惯例，返回的对象应通过调用
     * {@code super.clone} 获得。如果一个类及其所有超类（除了
     * {@code Object}）都遵守此约定，那么
     * {@code x.clone().getClass() == x.getClass()} 将为 true。
     * <p>
     * 按照惯例，此方法返回的对象应独立于
     * 此对象（即被克隆的对象）。为了实现这种独立性，
     * 可能需要在返回 {@code super.clone} 返回的对象之前
     * 修改其一个或多个字段。通常，这意味着
     * 复制构成被克隆对象内部“深层结构”的任何可变对象，
     * 并将对这些对象的引用替换为对副本的引用。如果一个类仅包含
     * 原始字段或对不可变对象的引用，那么通常
     * 不需要修改 {@code super.clone} 返回的对象中的任何字段。
     * <p>
     * 类 {@code Object} 的 {@code clone} 方法执行
     * 特定的克隆操作。首先，如果此对象的类没有
     * 实现 {@code Cloneable} 接口，则会抛出
     * {@code CloneNotSupportedException}。请注意，所有数组
     * 都被视为实现了 {@code Cloneable} 接口，并且
     * 数组类型 {@code T[]} 的 {@code clone} 方法的返回类型
     * 为 {@code T[]}，其中 T 是任何引用或原始类型。
     * 否则，此方法会创建此对象类的新实例，
     * 并使用此对象相应字段的内容初始化其所有字段，就像通过赋值一样；
     * 字段的内容本身不会被克隆。因此，此方法
     * 执行的是此对象的“浅拷贝”，而不是“深拷贝”操作。
     * <p>
     * 类 {@code Object} 本身并未实现 {@code Cloneable} 接口，
     * 因此对类为 {@code Object} 的对象调用 {@code clone} 方法
     * 将在运行时抛出异常。
     *
     * @return     此实例的副本。
     * @throws  CloneNotSupportedException  如果对象的类不支持
     *                {@code Cloneable} 接口。覆盖 {@code clone} 方法的子类
     *                也可以抛出此异常以指示无法克隆实例。
     * @see java.lang.Cloneable
     */
    protected native Object clone() throws CloneNotSupportedException;

    /**
     * 返回对象的字符串表示形式。通常，{@code toString} 方法返回一个
     * "文本化表示" 此对象的字符串。结果应该是一个简洁但信息丰富的表示形式，
     * 便于人们阅读。建议所有子类重写此方法。
     * <p>
     * {@code Object} 类的 {@code toString} 方法返回一个字符串，
     * 该字符串由对象实例的类名、at符号 `{@code @}` 以及对象哈希码的无符号十六进制表示组成。
     * 换句话说，此方法返回的字符串等于以下值：
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return  对象的字符串表示形式。
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * 唤醒在此对象监视器上等待的单个线程。如果有任何线程正在等待此对象，则选择其中一个线程被唤醒。选择是任意的，由实现自行决定。线程通过在对象上调用 {@code wait} 方法之一来等待对象的监视器。
     * <p>
     * 被唤醒的线程将无法继续执行，直到当前线程释放此对象上的锁。被唤醒的线程将以通常的方式与任何其他可能正在积极竞争以同步此对象的线程竞争；例如，被唤醒的线程在成为下一个锁定此对象的线程时没有可靠的特权或劣势。
     * <p>
     * 此方法只能由拥有此对象监视器的线程调用。线程通过以下三种方式之一成为对象监视器的拥有者：
     * <ul>
     * <li>通过执行该对象的同步实例方法。
     * <li>通过执行同步语句的主体，该语句在该对象上同步。
     * <li>对于类型为 {@code Class} 的对象，通过执行该类的同步静态方法。
     * </ul>
     * <p>
     * 一次只有一个线程可以拥有对象的监视器。
     *
     * @throws  IllegalMonitorStateException  如果当前线程不是此对象监视器的拥有者。
     * @see        Object#notifyAll()
     * @see        Object#wait()
     */
    public final native void notify1();

    /**
     * 唤醒所有在此对象监视器上等待的线程。线程通过调用 {@code wait} 方法之一来等待对象的监视器。
     * <p>
     * 被唤醒的线程在当前线程释放此对象的锁之前无法继续执行。被唤醒的线程将以通常的方式与任何其他可能正在竞争同步此对象的线程进行竞争；例如，被唤醒的线程在成为下一个锁定此对象的线程时，既没有可靠的特权，也没有劣势。
     * <p>
     * 此方法应仅由拥有此对象监视器的线程调用。有关线程如何成为监视器所有者的描述，请参阅 {@code notify} 方法。
     *
     * @throws  IllegalMonitorStateException  如果当前线程不是此对象监视器的所有者。
     * @see        Object#notify()
     * @see        Object#wait()
     */
    public final native void notifyAll1();

    /**
     * 使当前线程等待，直到另一个线程调用该对象的
     * {@link Object#notify()} 方法或
     * {@link Object#notifyAll()} 方法，或者指定的时间已过。
     * <p>
     * 当前线程必须拥有该对象的监视器。
     * <p>
     * 此方法使当前线程（称为 <var>T</var>）将自己放入该对象的等待集中，然后放弃对该对象的所有同步声明。线程 <var>T</var>
     * 将被禁用进行线程调度，并处于休眠状态，直到发生以下四种情况之一：
     * <ul>
     * <li>其他线程调用了该对象的 {@code notify} 方法，并且线程 <var>T</var> 恰好被任意选择为被唤醒的线程。
     * <li>其他线程调用了该对象的 {@code notifyAll} 方法。
     * <li>其他线程 {@linkplain java.lang.Thread#interrupt() 中断} 了线程 <var>T</var>。
     * <li>指定的实际时间已过，或多或少。如果 {@code timeout} 为零，则不考虑实际时间，线程将一直等待直到被通知。
     * </ul>
     * 然后，线程 <var>T</var> 从该对象的等待集中移除，并重新启用进行线程调度。然后它以通常的方式与其他线程竞争对该对象的同步权；一旦它获得了对对象的控制，所有对该对象的同步声明将恢复到调用 {@code wait} 方法时的状态。线程 <var>T</var> 然后从 {@code wait} 方法的调用中返回。因此，从 {@code wait} 方法返回时，对象和线程 {@code T} 的同步状态与调用 {@code wait} 方法时完全相同。
     * <p>
     * 线程也可能在没有被通知、中断或超时的情况下被唤醒，这被称为 <i>虚假唤醒</i>。虽然这种情况在实践中很少发生，但应用程序必须通过测试应该导致线程被唤醒的条件来防止这种情况，如果条件不满足，则继续等待。换句话说，等待应该始终发生在循环中，如下所示：
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;条件不满足&gt;)
     *             obj.wait(timeout);
     *         ... // 执行与条件相符的操作
     *     }
     * </pre>
     * （有关此主题的更多信息，请参见 Doug Lea 的《Java 并发编程（第二版）》（Addison-Wesley, 2000）中的第 3.2.3 节，或 Joshua Bloch 的《Effective Java 编程语言指南》（Addison-Wesley, 2001）中的第 50 条。
     *
     * <p>如果当前线程在等待之前或等待期间被任何线程 {@linkplain java.lang.Thread#interrupt() 中断}，则会抛出 {@code InterruptedException}。在恢复该对象的锁状态之前，不会抛出此异常。
     *
     * <p>
     * 请注意，{@code wait} 方法将当前线程放入该对象的等待集中时，仅解锁该对象；当前线程可能同步的任何其他对象在线程等待期间仍保持锁定状态。
     * <p>
     * 此方法只能由拥有该对象监视器的线程调用。有关线程如何成为监视器所有者的描述，请参见 {@code notify} 方法。
     *
     * @param      timeout   等待的最长时间，以毫秒为单位。
     * @throws  IllegalArgumentException      如果 timeout 的值为负数。
     * @throws  IllegalMonitorStateException  如果当前线程不是该对象监视器的所有者。
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或期间中断了当前线程。抛出此异常时，当前线程的 <i>中断状态</i> 将被清除。
     * @see        Object#notify()
     * @see        Object#notifyAll()
     */
    public final native void wait1(long timeout) throws InterruptedException;

    /**
     * 使当前线程等待，直到另一个线程调用该对象的
     * {@link Object#notify()} 方法或
     * {@link Object#notifyAll()} 方法，或者
     * 其他线程中断了当前线程，或者
     * 一定的实际时间已经过去。
     * <p>
     * 该方法与单参数的 {@code wait} 方法类似，但它允许更精细地控制在放弃之前等待通知的时间。实际时间的量，以纳秒为单位，由以下公式给出：
     * <blockquote>
     * <pre>
     * 1000000*timeout+nanos</pre></blockquote>
     * <p>
     * 在其他所有方面，该方法与单参数的 {@link #wait(long)} 方法执行相同的操作。特别是，
     * {@code wait(0, 0)} 与 {@code wait(0)} 含义相同。
     * <p>
     * 当前线程必须拥有该对象的监视器。线程释放对该监视器的所有权，并等待直到以下任一条件发生：
     * <ul>
     * <li>另一个线程通过调用 {@code notify} 方法或 {@code notifyAll} 方法通知等待该对象监视器的线程唤醒。
     * <li>由 {@code timeout} 毫秒加上 {@code nanos} 纳秒参数指定的超时时间已经过去。
     * </ul>
     * <p>
     * 然后，线程等待直到它可以重新获得监视器的所有权并恢复执行。
     * <p>
     * 与单参数版本一样，中断和虚假唤醒是可能的，因此该方法应始终在循环中使用：
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;条件不满足&gt;)
     *             obj.wait(timeout, nanos);
     *         ... // 执行与条件相适应的操作
     *     }
     * </pre>
     * 该方法只能由拥有该对象监视器的线程调用。有关线程如何成为监视器所有者的描述，请参阅 {@code notify} 方法。
     *
     * @param      timeout   等待的最长时间，以毫秒为单位。
     * @param      nanos     额外的时间，以纳秒为单位，范围为
     *                       0-999999。
     * @throws  IllegalArgumentException      如果 timeout 的值为负数，或者 nanos 的值不在 0-999999 范围内。
     * @throws  IllegalMonitorStateException  如果当前线程不是该对象监视器的所有者。
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或期间中断了当前线程。抛出此异常时，当前线程的<i>中断状态</i>将被清除。
     */
    public final void wait1(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }

    /**
     * 使当前线程等待，直到另一个线程调用该对象的
     * {@link Object#notify()} 方法或
     * {@link Object#notifyAll()} 方法。
     * 换句话说，此方法的行为与调用 {@code wait(0)} 完全相同。
     * <p>
     * 当前线程必须拥有该对象的监视器。线程将释放对该监视器的所有权，
     * 并等待直到另一个线程通过调用 {@code notify} 方法或
     * {@code notifyAll} 方法唤醒等待该对象监视器的线程。
     * 然后线程将等待直到重新获得监视器的所有权并恢复执行。
     * <p>
     * 与单参数版本一样，中断和虚假唤醒是可能的，
     * 因此此方法应始终在循环中使用：
     * <pre>
     *     synchronized (obj) {
     *         while (&lt;条件不成立&gt;)
     *             obj.wait();
     *         ... // 执行与条件相符的操作
     *     }
     * </pre>
     * 此方法只能由拥有该对象监视器的线程调用。
     * 有关线程如何成为监视器所有者的描述，请参阅 {@code notify} 方法。
     *
     * @throws  IllegalMonitorStateException  如果当前线程不是该对象监视器的所有者。
     * @throws  InterruptedException 如果任何线程在当前线程等待通知之前或期间中断了当前线程。
     *             抛出此异常时，当前线程的<i>中断状态</i>将被清除。
     * @see        Object#notify()
     * @see        Object#notifyAll()
     */
    public final void wait1() throws InterruptedException {
        wait(0);
    }

    /**
     * 当垃圾收集器确定不再有对该对象的引用时，在对象上调用此方法。
     * 子类覆盖 {@code finalize} 方法以释放系统资源或执行其他清理操作。
     * <p>
     * {@code finalize} 方法的一般约定是，当 Java&trade; 虚拟机
     * 确定不再有任何方式让任何尚未终止的线程访问此对象时，
     * 除了由于其他对象或类的终结操作所导致的结果之外，
     * 此方法将被调用。{@code finalize} 方法可以执行任何操作，
     * 包括使此对象再次可供其他线程使用；然而，{@code finalize} 的
     * 通常目的是在对象被不可撤销地丢弃之前执行清理操作。
     * 例如，表示输入/输出连接的对象的 finalize 方法可能会执行
     * 显式的 I/O 事务以在对象被永久丢弃之前断开连接。
     * <p>
     * 类 {@code Object} 的 {@code finalize} 方法不执行任何
     * 特殊操作；它只是正常返回。{@code Object} 的子类可以
     * 覆盖此定义。
     * <p>
     * Java 编程语言不保证哪个线程将为任何给定对象调用
     * {@code finalize} 方法。然而，保证调用 finalize 的线程
     * 在调用 finalize 时不会持有任何用户可见的同步锁。
     * 如果 finalize 方法抛出未捕获的异常，则该异常将被忽略，
     * 并且该对象的终结操作将终止。
     * <p>
     * 在为对象调用 {@code finalize} 方法之后，直到 Java 虚拟机
     * 再次确定不再有任何方式让任何尚未终止的线程访问此对象之前，
     * 不会采取进一步的操作，包括可能由其他对象或类执行的操作，
     * 此时该对象可能会被丢弃。
     * <p>
     * Java 虚拟机不会为任何给定对象多次调用 {@code finalize} 方法。
     * <p>
     * {@code finalize} 方法抛出的任何异常都会导致
     * 该对象的终结操作被终止，但除此之外将被忽略。
     *
     * @throws Throwable 此方法抛出的 {@code Exception}
     * @see java.lang.ref.WeakReference
     * @see java.lang.ref.PhantomReference
     * @jls 12.6 Finalization of Class Instances
     */
    protected void finalize() throws Throwable { }
}
