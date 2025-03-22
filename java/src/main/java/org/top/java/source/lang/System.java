package org.top.java.source.lang;

import sun.reflect.CallerSensitive;
import sun.security.util.SecurityConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channel;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.PropertyPermission;
import java.lang.Object;
/**
 * @作者 zack
 * @描述
 * @日期 2024/10/15 上午8:06
 */
public final class System {

    /* 通过静态初始化器注册本地方法。
     *
     * VM 将调用 initializeSystemClass 方法来完成
     * 该类与 clinit 分离的初始化。
     * 注意，要使用 VM 设置的属性，请参阅 initializeSystemClass 方法中
     * 描述的约束条件。
     */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    /** 不允许任何人实例化这个类 */
    private System() {
    }

    /**
     * "标准"输入流。此流已经打开并准备提供输入数据。通常，此流对应于键盘输入或由主机环境或用户指定的其他输入源。
     */
    public final static InputStream in = null;

    /**
     * "标准"输出流。此流已经打开并准备好接受输出数据。通常，此流对应于显示输出或由主机环境或用户指定的其他输出目标。
     * <p>
     * 对于简单的独立Java应用程序，写入一行输出数据的典型方式是：
     * <blockquote><pre>
     *     System.out.println(data)
     * </pre></blockquote>
     * <p>
     * 请参阅<code>PrintStream</code>类中的<code>println</code>方法。
     *
     * @see     java.io.PrintStream#println()
     * @see     java.io.PrintStream#println(boolean)
     * @see     java.io.PrintStream#println(char)
     * @see     java.io.PrintStream#println(char[])
     * @see     java.io.PrintStream#println(double)
     * @see     java.io.PrintStream#println(float)
     * @see     java.io.PrintStream#println(int)
     * @see     java.io.PrintStream#println(long)
     * @see     java.io.PrintStream#println(java.lang.Object)
     * @see     java.io.PrintStream#println(java.lang.String)
     */
    public final static PrintStream out = null;

    /**
     * "标准"错误输出流。此流已经打开并准备好接受输出数据。
     * <p>
     * 通常，此流对应于显示输出或由主机环境或用户指定的其他输出目标。按照惯例，
     * 此输出流用于显示错误消息或其他应立即引起用户注意的信息，即使主输出流（即
     * <code>out</code>变量的值）已被重定向到文件或其他通常不会持续监控的目标。
     */
    public final static PrintStream err = null;

    /* 系统的安全管理器。 */
    private static volatile SecurityManager security = null;

    /**
     * 重新分配“标准”输入流。
     *
     * <p>首先，如果存在安全管理器，则调用其 <code>checkPermission</code>
     * 方法，并传入 <code>RuntimePermission("setIO")</code> 权限，
     * 以检查是否允许重新分配“标准”输入流。
     * <p>
     *
     * @param in 新的标准输入流。
     *
     * @throws SecurityException
     *        如果存在安全管理器且其 <code>checkPermission</code> 方法
     *        不允许重新分配标准输入流。
     *
     * @see java.lang.SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     *
     * @since   JDK1.1
     */
    public static void setIn(InputStream in) {
        checkIO();
        setIn0(in);
    }

    /**
     * 重新分配“标准”输出流。
     *
     * <p>首先，如果存在安全管理器，则调用其 <code>checkPermission</code>
     * 方法，并传入 <code>RuntimePermission("setIO")</code> 权限，
     * 以检查是否可以重新分配“标准”输出流。
     *
     * @param out 新的标准输出流
     *
     * @throws SecurityException
     *        如果存在安全管理器且其 <code>checkPermission</code> 方法
     *        不允许重新分配标准输出流。
     *
     * @see java.lang.SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     *
     * @since   JDK1.1
     */
    public static void setOut(PrintStream out) {
        checkIO();
        setOut0(out);
    }

    /**
     * 重新分配“标准”错误输出流。
     *
     * <p>首先，如果存在安全管理器，则调用其 <code>checkPermission</code>
     * 方法，并传入 <code>RuntimePermission("setIO")</code> 权限，
     * 以检查是否允许重新分配“标准”错误输出流。
     *
     * @param err 新的标准错误输出流。
     *
     * @throws SecurityException
     *        如果存在安全管理器且其 <code>checkPermission</code> 方法
     *        不允许重新分配标准错误输出流。
     *
     * @see java.lang.SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     *
     * @since   JDK1.1
     */
    public static void setErr(PrintStream err) {
        checkIO();
        setErr0(err);
    }

    private static volatile Console cons = null;
    /**
     * 返回与当前Java虚拟机关联的唯一 {@link java.io.Console Console} 对象（如果有的话）。
     *
     * @return  系统控制台（如果有），否则返回 <tt>null</tt>。
     *
     * @since   1.6
     */
    public static Console console() {
        if (cons == null) {
            synchronized (java.lang.System.class) {
                cons = sun.misc.SharedSecrets.getJavaIOAccess().console();
            }
        }
        return cons;
    }

    /**
     * 返回从创建此 Java 虚拟机实体继承的通道。
     *
     * <p> 此方法通过调用系统范围内默认的
     * {@link java.nio.channels.spi.SelectorProvider} 对象的
     * {@link java.nio.channels.spi.SelectorProvider#inheritedChannel
     * inheritedChannel} 方法来获取通道。 </p>
     *
     * <p> 除了 {@link java.nio.channels.spi.SelectorProvider#inheritedChannel
     * inheritedChannel} 中描述的网络相关通道外，此方法未来可能返回其他类型的通道。
     *
     * @return  继承的通道，如果有的话，否则返回 <tt>null</tt>。
     *
     * @throws IOException
     *          如果发生 I/O 错误
     *
     * @throws  SecurityException
     *          如果存在安全管理器且它不允许访问该通道。
     *
     * @since 1.5
     */
    public static Channel inheritedChannel() throws IOException {
        return SelectorProvider.provider().inheritedChannel();
    }

    private static void checkIO() {
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setIO"));
        }
    }

    private static native void setIn0(InputStream in);
    private static native void setOut0(PrintStream out);
    private static native void setErr0(PrintStream err);

    /**
     * 设置系统安全。
     *
     * <p> 如果已经安装了安全管理器，此方法首先调用安全管理器的 <code>checkPermission</code> 方法，
     * 使用 <code>RuntimePermission("setSecurityManager")</code> 权限来确保可以替换现有的
     * 安全管理器。这可能会导致抛出 <code>SecurityException</code>。
     *
     * <p> 否则，参数将被设置为当前的安全管理器。如果参数为 <code>null</code> 并且尚未
     * 建立安全管理器，则不执行任何操作，方法直接返回。
     *
     * @param      s   安全管理器。
     * @exception  SecurityException  如果安全管理器已经设置并且其 <code>checkPermission</code> 方法
     *             不允许替换。
     * @see #getSecurityManager
     * @see java.lang.SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     */
    public static
    void setSecurityManager(final SecurityManager s) {
        try {
            s.checkPackageAccess("java.lang");
        } catch (Exception e) {
            // 无操作
        }
        setSecurityManager0(s);
    }

    private static synchronized
    void setSecurityManager0(final SecurityManager s) {
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            // 询问当前安装的安全管理器是否可以
            // 可以替换它。
            sm.checkPermission(new RuntimePermission
                    ("setSecurityManager"));
        }

        if ((s != null) && (s.getClass().getClassLoader() != null)) {
            // 新的安全管理器类不在引导类路径上。
            // 确保在安装新策略之前先初始化策略
            // 安全管理器，为了防止无限循环
            // 尝试初始化策略（通常涉及
            // 访问一些安全和/或系统属性，进而
            // 调用已安装的安全管理器的checkPermission方法
            // 如果存在非系统类，将会无限循环
            // （在这种情况下：新的安全管理器类）在堆栈上）。
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    s.getClass().getProtectionDomain().implies
                            (SecurityConstants.ALL_PERMISSION);
                    return null;
                }
            });
        }

        security = s;
    }

    /**
     * 获取系统安全接口。
     *
     * @return  如果当前应用程序已经建立了安全管理器，则返回该安全管理器；
     *          否则，返回 <code>null</code>。
     * @see     #setSecurityManager
     */
    public static SecurityManager getSecurityManager() {
        return security;
    }

    /**
     * 返回当前时间的毫秒数。请注意，
     * 虽然返回值的单位是毫秒，
     * 但值的粒度取决于底层
     * 操作系统，可能更大。例如，许多
     * 操作系统以几十毫秒为单位测量时间。
     *
     * <p> 有关“计算机时间”与协调世界时（UTC）之间
     * 可能出现的微小差异的讨论，请参见
     * <code>Date</code> 类的描述。
     *
     * @return  当前时间与1970年1月1日午夜（UTC）之间
     *          的差值，以毫秒为单位。
     * @see     java.util.Date
     */
    public static native long currentTimeMillis();

    /**
     * 返回当前运行的Java虚拟机的高分辨率时间源的当前值，以纳秒为单位。
     *
     * <p>此方法仅用于测量经过的时间，与系统或挂钟时间的任何其他概念无关。
     * 返回的值表示自某个固定但任意的<i>起始</i>时间（可能在将来，因此值可能为负）以来的纳秒数。
     * 同一Java虚拟机实例中的所有调用都使用相同的起始时间；其他虚拟机实例可能使用不同的起始时间。
     *
     * <p>此方法提供纳秒精度，但不一定提供纳秒分辨率（即值变化的频率）
     * - 不提供任何保证，除了分辨率至少与{@link #currentTimeMillis()}一样好。
     *
     * <p>跨越超过大约292年（2<sup>63</sup>纳秒）的连续调用之间的差异将由于数值溢出而无法正确计算经过的时间。
     *
     * <p>只有在同一Java虚拟机实例中获取的两个此类值之间的差异计算时，此方法返回的值才有意义。
     *
     * <p>例如，要测量某些代码执行所需的时间：
     *  <pre> {@code
     * long startTime = System.nanoTime();
     * // ... 被测量的代码 ...
     * long estimatedTime = System.nanoTime() - startTime;}</pre>
     *
     * <p>要比较两个nanoTime值
     *  <pre> {@code
     * long t0 = System.nanoTime();
     * ...
     * long t1 = System.nanoTime();}</pre>
     *
     * 应使用 {@code t1 - t0 < 0}，而不是 {@code t1 < t0}，
     * 因为可能存在数值溢出的情况。
     *
     * @return 当前运行的Java虚拟机的高分辨率时间源的当前值，以纳秒为单位
     * @since 1.5
     */
    public static native long nanoTime();

    /**
     * 从指定的源数组复制一个子数组，从指定位置开始，到目标数组的指定位置。
     * 从源数组<code>src</code>引用的数组中复制一个子序列到目标数组<code>dest</code>引用的数组中。
     * 复制的元素数量等于<code>length</code>参数。
     * 源数组中从位置<code>srcPos</code>到<code>srcPos+length-1</code>的元素被复制到目标数组中从位置<code>destPos</code>到<code>destPos+length-1</code>的位置。
     * <p>
     * 如果<code>src</code>和<code>dest</code>参数引用同一个数组对象，则复制过程将首先将位置<code>srcPos</code>到<code>srcPos+length-1</code>的元素复制到一个具有<code>length</code>个元素的临时数组中，然后将临时数组的内容复制到目标数组中从位置<code>destPos</code>到<code>destPos+length-1</code>的位置。
     * <p>
     * 如果<code>dest</code>为<code>null</code>，则抛出<code>NullPointerException</code>。
     * <p>
     * 如果<code>src</code>为<code>null</code>，则抛出<code>NullPointerException</code>，并且目标数组不会被修改。
     * <p>
     * 否则，如果以下任一条件为真，则抛出<code>ArrayStoreException</code>，并且目标数组不会被修改：
     * <ul>
     * <li><code>src</code>参数引用的对象不是数组。
     * <li><code>dest</code>参数引用的对象不是数组。
     * <li><code>src</code>参数和<code>dest</code>参数引用的数组的组件类型是不同的基本类型。
     * <li><code>src</code>参数引用的数组具有基本组件类型，而<code>dest</code>参数引用的数组具有引用组件类型。
     * <li><code>src</code>参数引用的数组具有引用组件类型，而<code>dest</code>参数引用的数组具有基本组件类型。
     * </ul>
     * <p>
     * 否则，如果以下任一条件为真，则抛出<code>IndexOutOfBoundsException</code>，并且目标数组不会被修改：
     * <ul>
     * <li><code>srcPos</code>参数为负数。
     * <li><code>destPos</code>参数为负数。
     * <li><code>length</code>参数为负数。
     * <li><code>srcPos+length</code>大于<code>src.length</code>，即源数组的长度。
     * <li><code>destPos+length</code>大于<code>dest.length</code>，即目标数组的长度。
     * </ul>
     * <p>
     * 否则，如果源数组中从位置<code>srcPos</code>到<code>srcPos+length-1</code>的任何实际元素无法通过赋值转换转换为目标数组的组件类型，则抛出<code>ArrayStoreException</code>。
     * 在这种情况下，设<b><i>k</i></b>为小于length的最小非负整数，使得<code>src[srcPos+</code><i>k</i><code>]</code>无法转换为目标数组的组件类型；
     * 当抛出异常时，源数组中从位置<code>srcPos</code>到<code>srcPos+</code><i>k</i><code>-1</code>的元素已经被复制到目标数组中从位置<code>destPos</code>到<code>destPos+</code><i>k</i><code>-1</code>的位置，并且目标数组的其他位置未被修改。
     * （由于已经列出的限制，本段实际上仅适用于两个数组的组件类型均为引用类型的情况。）
     *
     * @param      src      源数组。
     * @param      srcPos   源数组中的起始位置。
     * @param      dest     目标数组。
     * @param      destPos  目标数组中的起始位置。
     * @param      length   要复制的数组元素数量。
     * @exception  IndexOutOfBoundsException  如果复制会导致访问数组边界之外的数据。
     * @exception  ArrayStoreException  如果<code>src</code>数组中的某个元素由于类型不匹配而无法存储到<code>dest</code>数组中。
     * @exception  NullPointerException  如果<code>src</code>或<code>dest</code>为<code>null</code>。
     */
    public static native void arraycopy(Object src,  int  srcPos,
                                        Object dest, int destPos,
                                        int length);

    /**
     * 返回给定对象的哈希码，与默认方法 hashCode() 返回的哈希码相同，
     * 无论给定对象的类是否重写了 hashCode()。
     * null 引用的哈希码为零。
     *
     * @param x 要计算哈希码的对象
     * @return  哈希码
     * @since   JDK1.1
     */
    public static native int identityHashCode(Object x);

    /**
     * 系统属性。以下属性保证已定义：
     * <dl>
     * <dt>java.version         <dd>Java 版本号
     * <dt>java.vendor          <dd>Java 供应商特定字符串
     * <dt>java.vendor.url      <dd>Java 供应商 URL
     * <dt>java.home            <dd>Java 安装目录
     * <dt>java.class.version   <dd>Java 类版本号
     * <dt>java.class.path      <dd>Java 类路径
     * <dt>os.name              <dd>操作系统名称
     * <dt>os.arch              <dd>操作系统架构
     * <dt>os.version           <dd>操作系统版本
     * <dt>file.separator       <dd>文件分隔符（Unix 上为 "/"）
     * <dt>path.separator       <dd>路径分隔符（Unix 上为 ":"）
     * <dt>line.separator       <dd>行分隔符（Unix 上为 "n"）
     * <dt>user.name            <dd>用户账户名
     * <dt>user.home            <dd>用户主目录
     * <dt>user.dir             <dd>用户当前工作目录
     * </dl>
     */

    private static Properties props;
    private static native Properties initProperties(Properties props);

    /**
     * 确定当前系统属性。
     * <p>
     * 首先，如果存在安全管理器，则调用其
     * <code>checkPropertiesAccess</code> 方法，不带任何
     * 参数。这可能会导致安全异常。
     * <p>
     * 返回当前系统属性集，供
     * {@link #getProperty(String)} 方法使用，作为
     * <code>Properties</code> 对象。如果当前没有系统属性集，
     * 则首先创建并初始化一组系统属性。该组系统属性始终包括以下键的值：
     * <table summary="显示属性键及其关联值">
     * <tr><th>键</th>
     *     <th>关联值的描述</th></tr>
     * <tr><td><code>java.version</code></td>
     *     <td>Java 运行时环境版本</td></tr>
     * <tr><td><code>java.vendor</code></td>
     *     <td>Java 运行时环境供应商</td></tr>
     * <tr><td><code>java.vendor.url</code></td>
     *     <td>Java 供应商 URL</td></tr>
     * <tr><td><code>java.home</code></td>
     *     <td>Java 安装目录</td></tr>
     * <tr><td><code>java.vm.specification.version</code></td>
     *     <td>Java 虚拟机规范版本</td></tr>
     * <tr><td><code>java.vm.specification.vendor</code></td>
     *     <td>Java 虚拟机规范供应商</td></tr>
     * <tr><td><code>java.vm.specification.name</code></td>
     *     <td>Java 虚拟机规范名称</td></tr>
     * <tr><td><code>java.vm.version</code></td>
     *     <td>Java 虚拟机实现版本</td></tr>
     * <tr><td><code>java.vm.vendor</code></td>
     *     <td>Java 虚拟机实现供应商</td></tr>
     * <tr><td><code>java.vm.name</code></td>
     *     <td>Java 虚拟机实现名称</td></tr>
     * <tr><td><code>java.specification.version</code></td>
     *     <td>Java 运行时环境规范版本</td></tr>
     * <tr><td><code>java.specification.vendor</code></td>
     *     <td>Java 运行时环境规范供应商</td></tr>
     * <tr><td><code>java.specification.name</code></td>
     *     <td>Java 运行时环境规范名称</td></tr>
     * <tr><td><code>java.class.version</code></td>
     *     <td>Java 类格式版本号</td></tr>
     * <tr><td><code>java.class.path</code></td>
     *     <td>Java 类路径</td></tr>
     * <tr><td><code>java.library.path</code></td>
     *     <td>加载库时搜索的路径列表</td></tr>
     * <tr><td><code>java.io.tmpdir</code></td>
     *     <td>默认临时文件路径</td></tr>
     * <tr><td><code>java.compiler</code></td>
     *     <td>要使用的 JIT 编译器名称</td></tr>
     * <tr><td><code>java.ext.dirs</code></td>
     *     <td>扩展目录路径
     *         <b>已弃用。</b> <i>此属性及其实现机制
     *            可能在未来的版本中移除。</i> </td></tr>
     * <tr><td><code>os.name</code></td>
     *     <td>操作系统名称</td></tr>
     * <tr><td><code>os.arch</code></td>
     *     <td>操作系统架构</td></tr>
     * <tr><td><code>os.version</code></td>
     *     <td>操作系统版本</td></tr>
     * <tr><td><code>file.separator</code></td>
     *     <td>文件分隔符（UNIX 上为 "/"）</td></tr>
     * <tr><td><code>path.separator</code></td>
     *     <td>路径分隔符（UNIX 上为 ":"）</td></tr>
     * <tr><td><code>line.separator</code></td>
     *     <td>行分隔符（UNIX 上为 "n"）</td></tr>
     * <tr><td><code>user.name</code></td>
     *     <td>用户账户名称</td></tr>
     * <tr><td><code>user.home</code></td>
     *     <td>用户主目录</td></tr>
     * <tr><td><code>user.dir</code></td>
     *     <td>用户当前工作目录</td></tr>
     * </table>
     * <p>
     * 系统属性值中的多个路径由平台的路径分隔符字符分隔。
     * <p>
     * 请注意，即使安全管理器不允许
     * <code>getProperties</code> 操作，它也可能允许
     * {@link #getProperty(String)} 操作。
     *
     * @return     系统属性
     * @exception  SecurityException  如果存在安全管理器且其
     *             <code>checkPropertiesAccess</code> 方法不允许访问
     *             系统属性。
     * @see        #setProperties
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkPropertiesAccess()
     * @see        java.util.Properties
     */
    public static Properties getProperties() {
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPropertiesAccess();
        }

        return props;
    }

    /**
     * 返回系统相关的行分隔符字符串。它总是返回相同的值 - {@linkplain
     * #getProperty(String) 系统属性} {@code line.separator} 的初始值。
     *
     * <p>在 UNIX 系统上，它返回 {@code "n"}；在 Microsoft
     * Windows 系统上，它返回 {@code "rn"}。
     *
     * @return 系统相关的行分隔符字符串
     * @since 1.7
     */
    public static String lineSeparator() {
        return lineSeparator;
    }

    private static String lineSeparator;

    /**
     * 将系统属性设置为<code>Properties</code>参数。
     * <p>
     * 首先，如果存在安全管理器，则调用其无参的
     * <code>checkPropertiesAccess</code>方法。这可能会导致安全异常。
     * <p>
     * 该参数将成为由{@link #getProperty(String)}方法使用的当前系统属性集。
     * 如果参数为<code>null</code>，则当前系统属性集将被遗忘。
     *
     * @param      props   新的系统属性。
     * @exception  SecurityException  如果存在安全管理器且其
     *             <code>checkPropertiesAccess</code>方法不允许访问系统属性。
     * @see        #getProperties
     * @see        java.util.Properties
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkPropertiesAccess()
     */
    public static void setProperties(Properties props) {
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPropertiesAccess();
        }
        if (props == null) {
            props = new Properties();
            initProperties(props);
        }
        props = props;
    }

    /**
     * 获取由指定键指示的系统属性。
     * <p>
     * 首先，如果存在安全管理器，则调用其
     * <code>checkPropertyAccess</code> 方法，并将键作为其参数。
     * 这可能会导致 SecurityException。
     * <p>
     * 如果当前没有系统属性集，则首先创建并初始化系统属性集，
     * 初始化方式与 <code>getProperties</code> 方法相同。
     *
     * @param      key   系统属性的名称。
     * @return     系统属性的字符串值，
     *             如果不存在该键的属性，则返回 <code>null</code>。
     *
     * @exception  SecurityException  如果存在安全管理器，并且其
     *             <code>checkPropertyAccess</code> 方法不允许访问指定的系统属性。
     * @exception  NullPointerException 如果 <code>key</code> 为
     *             <code>null</code>。
     * @exception  IllegalArgumentException 如果 <code>key</code> 为空。
     * @see        #setProperty
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see        java.lang.System#getProperties()
     */
    public static String getProperty(String key) {
        checkKey(key);
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess(key);
        }

        return props.getProperty(key);
    }

    /**
     * 获取由指定键指示的系统属性。
     * <p>
     * 首先，如果存在安全管理器，则调用其
     * <code>checkPropertyAccess</code> 方法，并将
     * <code>key</code> 作为参数传递。
     * <p>
     * 如果没有当前的系统属性集，则首先创建并初始化一个系统属性集，
     * 其方式与 <code>getProperties</code> 方法相同。
     *
     * @param      key   系统属性的名称。
     * @param      def   默认值。
     * @return     系统属性的字符串值，
     *             如果没有该键对应的属性，则返回默认值。
     *
     * @exception  SecurityException  如果存在安全管理器且其
     *             <code>checkPropertyAccess</code> 方法不允许
     *             访问指定的系统属性。
     * @exception  NullPointerException 如果 <code>key</code> 为
     *             <code>null</code>。
     * @exception  IllegalArgumentException 如果 <code>key</code> 为空。
     * @see        #setProperty
     * @see        java.lang.SecurityManager#checkPropertyAccess(java.lang.String)
     * @see        java.lang.System#getProperties()
     */
    public static String getProperty(String key, String def) {
        checkKey(key);
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess(key);
        }

        return props.getProperty(key, def);
    }

    /**
     * 设置由指定键指示的系统属性。
     * <p>
     * 首先，如果存在安全管理器，则调用其
     * <code>SecurityManager.checkPermission</code> 方法，
     * 并传递一个 <code>PropertyPermission(key, "write")</code>
     * 权限。这可能会导致抛出 SecurityException。
     * 如果没有抛出异常，则将指定的属性设置为给定的值。
     * <p>
     *
     * @param      key   系统属性的名称。
     * @param      value 系统属性的值。
     * @return     系统属性的先前值，
     *             如果没有先前值，则返回 <code>null</code>。
     *
     * @exception  SecurityException 如果存在安全管理器且其
     *             <code>checkPermission</code> 方法不允许
     *             设置指定的属性。
     * @exception  NullPointerException 如果 <code>key</code> 或
     *             <code>value</code> 为 <code>null</code>。
     * @exception  IllegalArgumentException 如果 <code>key</code> 为空。
     * @see        #getProperty
     * @see        java.lang.System#getProperty(java.lang.String)
     * @see        java.lang.System#getProperty(java.lang.String, java.lang.String)
     * @see        java.util.PropertyPermission
     * @see        java.lang.SecurityManager#checkPermission
     * @since      1.2
     */
    public static String setProperty(String key, String value) {
        checkKey(key);
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new PropertyPermission(key,
                    SecurityConstants.PROPERTY_WRITE_ACTION));
        }

        return (String) props.setProperty(key, value);
    }

    /**
     * 移除指定键指示的系统属性。
     * <p>
     * 首先，如果存在安全管理器，则调用其
     * <code>SecurityManager.checkPermission</code> 方法，
     * 并传入 <code>PropertyPermission(key, "write")</code> 权限。
     * 这可能会导致抛出 SecurityException。
     * 如果没有抛出异常，则移除指定的属性。
     * <p>
     *
     * @param      key   要移除的系统属性的名称。
     * @return     系统属性的前一个字符串值，
     *             如果没有该键的属性，则返回 <code>null</code>。
     *
     * @exception  SecurityException  如果存在安全管理器且其
     *             <code>checkPropertyAccess</code> 方法不允许
     *             访问指定的系统属性。
     * @exception  NullPointerException 如果 <code>key</code> 为
     *             <code>null</code>。
     * @exception  IllegalArgumentException 如果 <code>key</code> 为空。
     * @see        #getProperty
     * @see        #setProperty
     * @see        java.util.Properties
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkPropertiesAccess()
     * @since 1.5
     */
    public static String clearProperty(String key) {
        checkKey(key);
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new PropertyPermission(key, "write"));
        }

        return (String) props.remove(key);
    }

    private static void checkKey(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }
    }


    /**
     * 终止当前运行的Java虚拟机。参数作为状态码；按照惯例，非零状态码表示异常终止。
     * <p>
     * 此方法调用类<code>Runtime</code>中的<code>exit</code>方法。此方法永远不会正常返回。
     * <p>
     * 调用<code>System.exit(n)</code>实际上等同于调用：
     * <blockquote><pre>
     * Runtime.getRuntime().exit(n)
     * </pre></blockquote>
     *
     * @param      status   退出状态。
     * @throws  SecurityException
     *        如果存在安全管理器且其<code>checkExit</code>方法不允许以指定状态退出。
     * @see        java.lang.Runtime#exit(int)
     */
    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    /**
     * 运行垃圾回收器。
     * <p>
     * 调用 <code>gc</code> 方法建议 Java 虚拟机尽力回收未使用的对象，以便它们当前占用的内存可以快速重用。
     * 当从方法调用返回时，Java 虚拟机已经尽力从所有丢弃的对象中回收空间。
     * <p>
     * 调用 <code>System.gc()</code> 实际上等同于调用：
     * <blockquote><pre>
     * Runtime.getRuntime().gc()
     * </pre></blockquote>
     *
     * @see     java.lang.Runtime#gc()
     */
    public static void gc() {
        Runtime.getRuntime().gc();
    }

    /**
     * 运行所有待终结对象的终结方法。
     * <p>
     * 调用此方法建议 Java 虚拟机尽力运行已丢弃但尚未运行其 <code>finalize</code> 方法的对象的 <code>finalize</code> 方法。当从方法调用返回时，Java 虚拟机已尽最大努力完成所有未完成的终结操作。
     * <p>
     * 调用 <code>System.runFinalization()</code> 实际上等同于调用：
     * <blockquote><pre>
     * Runtime.getRuntime().runFinalization()
     * </pre></blockquote>
     *
     * @see     java.lang.Runtime#runFinalization()
     */
    public static void runFinalization() {
        Runtime.getRuntime().runFinalization();
    }

    /**
     * 启用或禁用退出时的终结操作；这样做指定在Java运行时退出之前，所有具有终结器且尚未自动调用的对象的终结器都将被运行。
     * 默认情况下，退出时的终结操作是禁用的。
     *
     * <p>如果存在安全管理器，
     * 则首先调用其<code>checkExit</code>方法，
     * 以0作为参数，以确保允许退出。
     * 这可能会导致SecurityException。
     *
     * @deprecated  此方法本质上是不安全的。它可能导致在活对象上调用终结器，而其他线程同时操作这些对象，从而导致异常行为或死锁。
     * @param value 指示启用或禁用终结操作
     * @throws  SecurityException
     *        如果存在安全管理器且其<code>checkExit</code>方法不允许退出。
     *
     * @see     java.lang.Runtime#exit(int)
     * @see     java.lang.Runtime#gc()
     * @see     java.lang.SecurityManager#checkExit(int)
     * @since   JDK1.1
     */
    @Deprecated
    public static void runFinalizersOnExit(boolean value) {
        Runtime.runFinalizersOnExit(value);
    }

    /**
     * 加载由文件名参数指定的本地库。文件名参数必须是绝对路径。
     *
     * 如果文件名参数在去除任何平台特定的库前缀、路径和文件扩展名后，指示的库名为，
     * 例如 L，并且一个名为 L 的本地库已静态链接到虚拟机，则调用该库导出的
     * JNI_OnLoad_L 函数，而不是尝试加载动态库。文件名参数匹配的文件不必在文件系统中存在。
     * 更多细节请参见 JNI 规范。
     *
     * 否则，文件名参数将以实现相关的方式映射到本地库映像。
     *
     * <p>
     * 调用 <code>System.load(name)</code> 实际上等同于调用：
     * <blockquote><pre>
     * Runtime.getRuntime().load(name)
     * </pre></blockquote>
     *
     * @param      filename   要加载的文件。
     * @exception  SecurityException  如果存在安全管理器并且其
     *             <code>checkLink</code> 方法不允许加载指定的动态库。
     * @exception  UnsatisfiedLinkError  如果文件名不是绝对路径名，或者本地库未静态链接到虚拟机，
     *             或者主机系统无法将库映射到本地库映像。
     * @exception  NullPointerException  如果 <code>filename</code> 为
     *             <code>null</code>。
     * @see        java.lang.Runtime#load(java.lang.String)
     * @see        java.lang.SecurityManager#checkLink(java.lang.String)
     */
    @CallerSensitive
    public static void load(String filename) {
        //Runtime.getRuntime().load0(Reflection.getCallerClass(), filename);
    }


    /**
     * 将库名称映射为表示本地库的平台特定字符串。
     *
     * @param      libname 库的名称。
     * @return     一个依赖于平台的本地库名称。
     * @exception  NullPointerException 如果 <code>libname</code> 为
     *             <code>null</code>
     * @see        java.lang.System#loadLibrary(java.lang.String)
     * @see
     * @since      1.2
     */
    public static native String mapLibraryName(String libname);

    /**
     * 根据编码创建标准输出/错误的PrintStream。
     */
    private static PrintStream newPrintStream(FileOutputStream fos, String enc) {
        if (enc != null) {
            try {
                return new PrintStream(new BufferedOutputStream(fos, 128), true, enc);
            } catch (UnsupportedEncodingException uee) {}
        }
        return new PrintStream(new BufferedOutputStream(fos, 128), true);
    }


    /**
     * 初始化系统类。在线程初始化之后调用。
     */
    private static void initializeSystemClass() {

        // VM 可能会调用 JNU_NewStringPlatform() 来设置这些编码
        // 敏感属性（user.home, user.name, boot.class.path, 等）
        // 在“props”初始化期间，可能需要通过以下方式访问
        // System.getProperty(), 获取相关的系统编码属性
        // 已在早期阶段初始化（放入 "props" 中）
        // 初始化。确保“props”在
        // 初始化的最开始和所有的系统属性
        // 直接放入其中。
        props = new Properties();
        initProperties(props);  // 由虚拟机初始化

        // 某些系统配置可能由
        // VM 选项，例如最大直接内存量
        // 用于支持对象标识语义的整数缓存大小
        // 关于自动装箱。通常，库会获取这些值
        // 从虚拟机设置的属性中获取。如果属性是用于
        // 内部实现专用，这些属性应
        // 已从系统属性中移除。
        // 这是一个示例类
// 用于演示注释翻译
/**
 * 这是一个示例方法
 * 用于演示多行注释翻译
 * @param 参数1 第一个参数
 * @param 参数2 第二个参数
 * @return 返回结果
 */
        // 参见 java.lang.Integer.IntegerCache 以及
        /**
 * 保存并移除系统属性。
 * 该方法用于保存当前系统属性的快照，并从系统中移除这些属性。
 * 通常在需要临时修改系统属性时使用，以确保在操作完成后可以恢复原始状态。
 *
 * @return 保存的系统属性快照
 * @throws SecurityException 如果没有权限访问或修改系统属性
 */
        // 这是一个示例类
// 用于演示注释翻译
/**
 * 这是一个示例方法
 * 用于演示多行注释翻译
 * @param 参数1 第一个参数
 * @param 参数2 第二个参数
 * @return 返回结果
 */
        // 保存系统属性对象的私有副本
        // 只能由内部实现访问。移除
        // 某些系统属性不打算公开访问。
        sun.misc.VM.saveAndRemoveProperties(props);


        lineSeparator = props.getProperty("line.separator");
        sun.misc.Version.init();

        FileInputStream fdIn = new FileInputStream(FileDescriptor.in);
        FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
        FileOutputStream fdErr = new FileOutputStream(FileDescriptor.err);
        setIn0(new BufferedInputStream(fdIn));
        setOut0(newPrintStream(fdOut, props.getProperty("sun.stdout.encoding")));
        setErr0(newPrintStream(fdErr, props.getProperty("sun.stderr.encoding")));

        // 现在加载 zip 库以保留 java.util.zip.ZipFile
        // 防止后续尝试使用自身来加载此库。
        //加载库("zip");

        // 为 HUP、TERM 和 INT（在可用的情况下）设置 Java 信号处理程序。
        //Terminator.setup();

        // 初始化任何需要设置的其他操作系统设置
        // 为类库设置。目前这除了...之外在所有地方都是无操作的
        // 对于Windows，在java.io之前设置进程范围的错误模式
        // 使用了类。
        sun.misc.VM.initializeOSEnvironment();

        // 主线程不会以相同的方式添加到其线程组中
        // 像其他线程一样；我们必须在这里自己完成。
        Thread current = Thread.currentThread();
        current.getThreadGroup().add(current);

        // 注册共享密钥
        //setJavaLangAccess();

        // 在初始化期间调用的子系统可以调用
        // sun.misc.VM.isBooted() 用于避免执行那些应该
        // 等待应用程序类加载器设置完成。
        // 重要：确保这始终是最后的初始化操作！
        sun.misc.VM.booted();
    }
}
