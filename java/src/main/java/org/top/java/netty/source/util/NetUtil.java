
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.NetUtilInitializations.NetworkIfaceAndInetAddress;
import org.top.java.netty.source.util.internal.PlatformDependent;
import org.top.java.netty.source.util.internal.StringUtil;
import org.top.java.netty.source.util.internal.SystemPropertyUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import static io.netty.util.AsciiString.indexOf;

/**
 * A class that holds a number of network-related constants.
 * <p/>
 * This class borrowed some of its methods from a  modified fork of the
 * <a href="https://svn.apache.org/repos/asf/harmony/enhanced/java/branches/java6/classlib/modules/luni/
 * src/main/java/org/apache/harmony/luni/util/Inet6Util.java">Inet6Util class</a> which was part of Apache Harmony.
 */

/**
 * 一个包含网络相关常量的类。
 * <p/>
 * 该类从<a href="https://svn.apache.org/repos/asf/harmony/enhanced/java/branches/java6/classlib/modules/luni/
 * src/main/java/org/apache/harmony/luni/util/Inet6Util.java">Inet6Util类</a>的修改分支中借用了一些方法，该类是Apache Harmony的一部分。
 */
public final class NetUtil {

    /**
     * The {@link Inet4Address} that represents the IPv4 loopback address '127.0.0.1'
     */

    /**
     * 表示IPv4回环地址'127.0.0.1'的{@link Inet4Address}
     */
    public static final Inet4Address LOCALHOST4;

    /**
     * The {@link Inet6Address} that represents the IPv6 loopback address '::1'
     */

    /**
     * 表示 IPv6 环回地址 '::1' 的 {@link Inet6Address}
     */
    public static final Inet6Address LOCALHOST6;

    /**
     * The {@link InetAddress} that represents the loopback address. If IPv6 stack is available, it will refer to
     * {@link #LOCALHOST6}.  Otherwise, {@link #LOCALHOST4}.
     */

    /**
     * 表示回环地址的 {@link InetAddress}。如果 IPv6 协议栈可用，它将引用 {@link #LOCALHOST6}。否则，引用 {@link #LOCALHOST4}。
     */
    public static final InetAddress LOCALHOST;

    /**
     * The loopback {@link NetworkInterface} of the current machine
     */

    /**
     * 当前机器的回环 {@link NetworkInterface}
     */
    public static final NetworkInterface LOOPBACK_IF;

    /**
     * The SOMAXCONN value of the current machine.  If failed to get the value,  {@code 200} is used as a
     * default value for Windows and {@code 128} for others.
     */

    /**
     * 当前机器的 SOMAXCONN 值。如果获取值失败，Windows 默认使用 {@code 200}，其他系统默认使用 {@code 128}。
     */
    public static final int SOMAXCONN;

    /**
     * This defines how many words (represented as ints) are needed to represent an IPv6 address
     */

    /**
     * 这定义了表示一个IPv6地址需要多少个单词（用整数表示）
     */
    private static final int IPV6_WORD_COUNT = 8;

    /**
     * The maximum number of characters for an IPV6 string with no scope
     */

    /**
     * 不带作用域的IPv6字符串的最大字符数
     */
    private static final int IPV6_MAX_CHAR_COUNT = 39;

    /**
     * Number of bytes needed to represent an IPV6 value
     */

    /**
     * 表示一个IPV6值所需的字节数
     */
    private static final int IPV6_BYTE_COUNT = 16;

    /**
     * Maximum amount of value adding characters in between IPV6 separators
     */

    /**
     * IPV6分隔符之间添加字符的最大数量
     */
    private static final int IPV6_MAX_CHAR_BETWEEN_SEPARATOR = 4;

    /**
     * Minimum number of separators that must be present in an IPv6 string
     */

    /**
     * IPv6字符串中必须存在的最少分隔符数量
     */
    private static final int IPV6_MIN_SEPARATORS = 2;

    /**
     * Maximum number of separators that must be present in an IPv6 string
     */

    /**
     * IPv6字符串中必须存在的最大分隔符数量
     */
    private static final int IPV6_MAX_SEPARATORS = 8;

    /**
     * Maximum amount of value adding characters in between IPV4 separators
     */

    /**
     * IPV4分隔符之间添加字符的最大值
     */
    private static final int IPV4_MAX_CHAR_BETWEEN_SEPARATOR = 3;

    /**
     * Number of separators that must be present in an IPv4 string
     */

    /**
     * IPv4字符串中必须存在的分隔符数量
     */
    private static final int IPV4_SEPARATORS = 3;

    /**
     * {@code true} if IPv4 should be used even if the system supports both IPv4 and IPv6.
     */

    /**
     * 如果即使系统同时支持IPv4和IPv6，也应使用IPv4，则为{@code true}。
     */
    private static final boolean IPV4_PREFERRED = SystemPropertyUtil.getBoolean("java.net.preferIPv4Stack", false);

    /**
     * {@code true} if an IPv6 address should be preferred when a host has both an IPv4 address and an IPv6 address.
     */

    /**
     * 如果主机同时拥有IPv4地址和IPv6地址时，应优先使用IPv6地址，则为 {@code true}。
     */
    private static final boolean IPV6_ADDRESSES_PREFERRED =
            SystemPropertyUtil.getBoolean("java.net.preferIPv6Addresses", false);

    /**
     * The logger being used by this class
     */

    /**
     * 该类使用的日志记录器
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NetUtil.class);

    static {
        logger.debug("-Djava.net.preferIPv4Stack: {}", IPV4_PREFERRED);
        logger.debug("-Djava.net.preferIPv6Addresses: {}", IPV6_ADDRESSES_PREFERRED);

        // Create IPv4 loopback address.

        // 创建IPv4回环地址。
        LOCALHOST4 = NetUtilInitializations.createLocalhost4();

        // Create IPv6 loopback address.

        // 创建IPv6环回地址。
        LOCALHOST6 = NetUtilInitializations.createLocalhost6();

        NetworkIfaceAndInetAddress loopback = NetUtilInitializations.determineLoopback(LOCALHOST4, LOCALHOST6);
        LOOPBACK_IF = loopback.iface();
        LOCALHOST = loopback.address();

        // As a SecurityManager may prevent reading the somaxconn file we wrap this in a privileged block.

        // 由于SecurityManager可能会阻止读取somaxconn文件，我们将其包装在一个特权块中。
        //
        // See https://github.com/netty/netty/issues/3680
        // 参见 https://github.com/netty/netty/issues/3680
        SOMAXCONN = AccessController.doPrivileged(new SoMaxConnAction());
    }

    private static final class SoMaxConnAction implements PrivilegedAction<Integer> {
        @Override
        public Integer run() {
            // Determine the default somaxconn (server socket backlog) value of the platform.
            // 确定平台的默认 somaxconn（服务器套接字 backlog）值。
            // The known defaults:
            // 已知的默认值：
            // - Windows NT Server 4.0+: 200
            // - Windows NT Server 4.0+: 200
            // - Linux and Mac OS X: 128
            // - Linux 和 Mac OS X: 128
            int somaxconn = PlatformDependent.isWindows() ? 200 : 128;
            File file = new File("/proc/sys/net/core/somaxconn");
            BufferedReader in = null;
            try {
                // file.exists() may throw a SecurityException if a SecurityManager is used, so execute it in the
                // file.exists() 可能会在使用 SecurityManager 时抛出 SecurityException，因此请将其放在
                // try / catch block.
                // try / catch 块。
                // See https://github.com/netty/netty/issues/4936
                // 参见 https://github.com/netty/netty/issues/4936
                if (file.exists()) {
                    in = new BufferedReader(new FileReader(file));
                    somaxconn = Integer.parseInt(in.readLine());
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}: {}", file, somaxconn);
                    }
                } else {
                    // Try to get from sysctl
                    // 尝试从sysctl获取
                    Integer tmp = null;
                    if (SystemPropertyUtil.getBoolean("io.netty.net.somaxconn.trySysctl", false)) {
                        tmp = sysctlGetInt("kern.ipc.somaxconn");
                        if (tmp == null) {
                            tmp = sysctlGetInt("kern.ipc.soacceptqueue");
                            if (tmp != null) {
                                somaxconn = tmp;
                            }
                        } else {
                            somaxconn = tmp;
                        }
                    }

                    if (tmp == null) {
                        logger.debug("Failed to get SOMAXCONN from sysctl and file {}. Default: {}", file,
                                somaxconn);
                    }
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to get SOMAXCONN from sysctl and file {}. Default: {}",
                            file, somaxconn, e);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        // Ignored.
                        // 忽略。
                    }
                }
            }
            return somaxconn;
        }
    }
    /**
     * This will execute <a href ="https://www.freebsd.org/cgi/man.cgi?sysctl(8)">sysctl</a> with the {@code sysctlKey}
     * which is expected to return the numeric value for for {@code sysctlKey}.
     * @param sysctlKey The key which the return value corresponds to.
     * @return The <a href ="https://www.freebsd.org/cgi/man.cgi?sysctl(8)">sysctl</a> value for {@code sysctlKey}.
     */
    /**
     * 这将执行 <a href ="https://www.freebsd.org/cgi/man.cgi?sysctl(8)">sysctl</a> 并传入 {@code sysctlKey}，
     * 预期返回与 {@code sysctlKey} 对应的数值。
     * @param sysctlKey 返回值对应的键。
     * @return {@code sysctlKey} 对应的 <a href ="https://www.freebsd.org/cgi/man.cgi?sysctl(8)">sysctl</a> 值。
     */
    private static Integer sysctlGetInt(String sysctlKey) throws IOException {
        Process process = new ProcessBuilder("sysctl", sysctlKey).start();
        try {
            // Suppress warnings about resource leaks since the buffered reader is closed below
            // 由于下面的缓冲读取器被关闭，因此抑制有关资源泄漏的警告
            InputStream is = process.getInputStream();  // lgtm[java/input-resource-leak
            InputStreamReader isr = new InputStreamReader(is);  // lgtm[java/input-resource-leak
            BufferedReader br = new BufferedReader(isr);
            try {
                String line = br.readLine();
                if (line != null && line.startsWith(sysctlKey)) {
                    for (int i = line.length() - 1; i > sysctlKey.length(); --i) {
                        if (!Character.isDigit(line.charAt(i))) {
                            return Integer.valueOf(line.substring(i + 1));
                        }
                    }
                }
                return null;
            } finally {
                br.close();
            }
        } finally {
            // No need of 'null' check because we're initializing
            // 不需要进行 'null' 检查，因为我们正在初始化
            // the Process instance in first line. Any exception
            // 第一行中的Process实例。任何异常
            // raised will directly lead to throwable.
            // raised 将直接导致 throwable。
            process.destroy();
        }
    }

    /**
     * Returns {@code true} if IPv4 should be used even if the system supports both IPv4 and IPv6. Setting this
     * property to {@code true} will disable IPv6 support. The default value of this property is {@code false}.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html">Java SE
     *      networking properties</a>
     */

    /**
     * 返回 {@code true} 表示即使系统同时支持 IPv4 和 IPv6，也应使用 IPv4。将此属性设置为 {@code true} 将禁用 IPv6 支持。此属性的默认值为 {@code false}。
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html">Java SE
     *      网络属性</a>
     */
    public static boolean isIpV4StackPreferred() {
        return IPV4_PREFERRED;
    }

    /**
     * Returns {@code true} if an IPv6 address should be preferred when a host has both an IPv4 address and an IPv6
     * address. The default value of this property is {@code false}.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html">Java SE
     *      networking properties</a>
     */

    /**
     * 当主机同时拥有IPv4地址和IPv6地址时，如果应优先选择IPv6地址，则返回{@code true}。此属性的默认值为{@code false}。
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html">Java SE
     *      网络属性</a>
     */
    public static boolean isIpV6AddressesPreferred() {
        return IPV6_ADDRESSES_PREFERRED;
    }

    /**
     * Creates an byte[] based on an ipAddressString. No error handling is performed here.
     */

    /**
     * 根据ipAddressString创建一个byte[]。此处不进行错误处理。
     */
    public static byte[] createByteArrayFromIpAddressString(String ipAddressString) {

        if (isValidIpV4Address(ipAddressString)) {
            return validIpV4ToBytes(ipAddressString);
        }

        if (isValidIpV6Address(ipAddressString)) {
            if (ipAddressString.charAt(0) == '[') {
                ipAddressString = ipAddressString.substring(1, ipAddressString.length() - 1);
            }

            int percentPos = ipAddressString.indexOf('%');
            if (percentPos >= 0) {
                ipAddressString = ipAddressString.substring(0, percentPos);
            }

            return getIPv6ByName(ipAddressString, true);
        }
        return null;
    }

    /**
     * Creates an {@link InetAddress} based on an ipAddressString or might return null if it can't be parsed.
     * No error handling is performed here.
     */

    /**
     * 基于ipAddressString创建一个{@link InetAddress}，如果无法解析则可能返回null。
     * 此处不进行错误处理。
     */
    public static InetAddress createInetAddressFromIpAddressString(String ipAddressString) {
        if (isValidIpV4Address(ipAddressString)) {
            byte[] bytes = validIpV4ToBytes(ipAddressString);
            try {
                return InetAddress.getByAddress(bytes);
            } catch (UnknownHostException e) {
                // Should never happen!
                // 不应该发生！
                throw new IllegalStateException(e);
            }
        }

        if (isValidIpV6Address(ipAddressString)) {
            if (ipAddressString.charAt(0) == '[') {
                ipAddressString = ipAddressString.substring(1, ipAddressString.length() - 1);
            }

            int percentPos = ipAddressString.indexOf('%');
            if (percentPos >= 0) {
                try {
                    int scopeId = Integer.parseInt(ipAddressString.substring(percentPos + 1));
                    ipAddressString = ipAddressString.substring(0, percentPos);
                    byte[] bytes = getIPv6ByName(ipAddressString, true);
                    if (bytes == null) {
                        return null;
                    }
                    try {
                        return Inet6Address.getByAddress(null, bytes, scopeId);
                    } catch (UnknownHostException e) {
                        // Should never happen!
                        // 不应该发生！
                        throw new IllegalStateException(e);
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            byte[] bytes = getIPv6ByName(ipAddressString, true);
            if (bytes == null) {
                return null;
            }
            try {
                return InetAddress.getByAddress(bytes);
            } catch (UnknownHostException e) {
                // Should never happen!
                // 不应该发生！
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    private static int decimalDigit(String str, int pos) {
        return str.charAt(pos) - '0';
    }

    private static byte ipv4WordToByte(String ip, int from, int toExclusive) {
        int ret = decimalDigit(ip, from);
        from++;
        if (from == toExclusive) {
            return (byte) ret;
        }
        ret = ret * 10 + decimalDigit(ip, from);
        from++;
        if (from == toExclusive) {
            return (byte) ret;
        }
        return (byte) (ret * 10 + decimalDigit(ip, from));
    }

    // visible for tests

    // 对测试可见
    static byte[] validIpV4ToBytes(String ip) {
        int i;
        return new byte[] {
                ipv4WordToByte(ip, 0, i = ip.indexOf('.', 1)),
                ipv4WordToByte(ip, i + 1, i = ip.indexOf('.', i + 2)),
                ipv4WordToByte(ip, i + 1, i = ip.indexOf('.', i + 2)),
                ipv4WordToByte(ip, i + 1, ip.length())
        };
    }

    /**
     * Convert {@link Inet4Address} into {@code int}
     */

    /**
     * 将 {@link Inet4Address} 转换为 {@code int}
     */
    public static int ipv4AddressToInt(Inet4Address ipAddress) {
        byte[] octets = ipAddress.getAddress();

        return  (octets[0] & 0xff) << 24 |
                (octets[1] & 0xff) << 16 |
                (octets[2] & 0xff) << 8 |
                 octets[3] & 0xff;
    }

    /**
     * Converts a 32-bit integer into an IPv4 address.
     */

    /**
     * 将32位整数转换为IPv4地址。
     */
    public static String intToIpAddress(int i) {
        StringBuilder buf = new StringBuilder(15);
        buf.append(i >> 24 & 0xff);
        buf.append('.');
        buf.append(i >> 16 & 0xff);
        buf.append('.');
        buf.append(i >> 8 & 0xff);
        buf.append('.');
        buf.append(i & 0xff);
        return buf.toString();
    }

    /**
     * Converts 4-byte or 16-byte data into an IPv4 or IPv6 string respectively.
     *
     * @throws IllegalArgumentException
     *         if {@code length} is not {@code 4} nor {@code 16}
     */

    /**
     * 将4字节或16字节的数据转换为IPv4或IPv6字符串。
     *
     * @throws IllegalArgumentException
     *         如果 {@code length} 不是 {@code 4} 或 {@code 16}
     */
    public static String bytesToIpAddress(byte[] bytes) {
        return bytesToIpAddress(bytes, 0, bytes.length);
    }

    /**
     * Converts 4-byte or 16-byte data into an IPv4 or IPv6 string respectively.
     *
     * @throws IllegalArgumentException
     *         if {@code length} is not {@code 4} nor {@code 16}
     */

    /**
     * 将4字节或16字节的数据转换为IPv4或IPv6字符串。
     *
     * @throws IllegalArgumentException
     *         如果 {@code length} 不是 {@code 4} 或 {@code 16}
     */
    public static String bytesToIpAddress(byte[] bytes, int offset, int length) {
        switch (length) {
            case 4: {
                return new StringBuilder(15)
                        .append(bytes[offset] & 0xff)
                        .append('.')
                        .append(bytes[offset + 1] & 0xff)
                        .append('.')
                        .append(bytes[offset + 2] & 0xff)
                        .append('.')
                        .append(bytes[offset + 3] & 0xff).toString();
            }
            case 16:
                return toAddressString(bytes, offset, false);
            default:
                throw new IllegalArgumentException("length: " + length + " (expected: 4 or 16)");
        }
    }

    public static boolean isValidIpV6Address(String ip) {
        return isValidIpV6Address((CharSequence) ip);
    }

    public static boolean isValidIpV6Address(CharSequence ip) {
        int end = ip.length();
        if (end < 2) {
            return false;
        }

        // strip "[]"

        // 去除 "[]"
        int start;
        char c = ip.charAt(0);
        if (c == '[') {
            end--;
            if (ip.charAt(end) != ']') {
                // must have a close ]
                // 必须有一个关闭的 ]
                return false;
            }
            start = 1;
            c = ip.charAt(1);
        } else {
            start = 0;
        }

        int colons;
        int compressBegin;
        if (c == ':') {
            // an IPv6 address can start with "::" or with a number
            // 一个IPv6地址可以以"::"或数字开头
            if (ip.charAt(start + 1) != ':') {
                return false;
            }
            colons = 2;
            compressBegin = start;
            start += 2;
        } else {
            colons = 0;
            compressBegin = -1;
        }

        int wordLen = 0;
        loop:
        for (int i = start; i < end; i++) {
            c = ip.charAt(i);
            if (isValidHexChar(c)) {
                if (wordLen < 4) {
                    wordLen++;
                    continue;
                }
                return false;
            }

            switch (c) {
            case ':':
                if (colons > 7) {
                    return false;
                }
                if (ip.charAt(i - 1) == ':') {
                    if (compressBegin >= 0) {
                        return false;
                    }
                    compressBegin = i - 1;
                } else {
                    wordLen = 0;
                }
                colons++;
                break;
            case '.':
                // case for the last 32-bits represented as IPv4 x:x:x:x:x:x:d.d.d.d
                // 最后32位表示为IPv4的情况 x:x:x:x:x:x:d.d.d.d

                // check a normal case (6 single colons)

                // 检查正常情况（6个单冒号）
                if (compressBegin < 0 && colons != 6 ||
                    // a special case ::1:2:3:4:5:d.d.d.d allows 7 colons with an
                    // 特殊情况 ::1:2:3:4:5:d.d.d.d 允许有7个冒号
                    // IPv4 ending, otherwise 7 :'s is bad
                    // IPv4 结尾，否则 7 个 : 是坏的
                    (colons == 7 && compressBegin >= start || colons > 7)) {
                    return false;
                }

                // Verify this address is of the correct structure to contain an IPv4 address.

                // 验证此地址是否具有包含IPv4地址的正确结构。
                // It must be IPv4-Mapped or IPv4-Compatible
                // 必须是IPv4映射或IPv4兼容
                // (see https://tools.ietf.org/html/rfc4291#section-2.5.5).
                // (参见 https://tools.ietf.org/html/rfc4291#section-2.5.5)。
                int ipv4Start = i - wordLen;
                int j = ipv4Start - 2; // index of character before the previous ':'.
                if (isValidIPv4MappedChar(ip.charAt(j))) {
                    if (!isValidIPv4MappedChar(ip.charAt(j - 1)) ||
                        !isValidIPv4MappedChar(ip.charAt(j - 2)) ||
                        !isValidIPv4MappedChar(ip.charAt(j - 3))) {
                        return false;
                    }
                    j -= 5;
                }

                for (; j >= start; --j) {
                    char tmpChar = ip.charAt(j);
                    if (tmpChar != '0' && tmpChar != ':') {
                        return false;
                    }
                }

                // 7 - is minimum IPv4 address length

                // 7 - 是最小IPv4地址长度
                int ipv4End = indexOf(ip, '%', ipv4Start + 7);
                if (ipv4End < 0) {
                    ipv4End = end;
                }
                return isValidIpV4Address(ip, ipv4Start, ipv4End);
            case '%':
                // strip the interface name/index after the percent sign
                // 去除百分号后的接口名称/索引
                end = i;
                break loop;
            default:
                return false;
            }
        }

        // normal case without compression

        // 正常情况，未进行压缩
        if (compressBegin < 0) {
            return colons == 7 && wordLen > 0;
        }

        return compressBegin + 2 == end ||
               // 8 colons is valid only if compression in start or end
               // 8 个冒号仅在开头或结尾压缩时有效
               wordLen > 0 && (colons < 8 || compressBegin <= start);
    }

    private static boolean isValidIpV4Word(CharSequence word, int from, int toExclusive) {
        int len = toExclusive - from;
        char c0, c1, c2;
        if (len < 1 || len > 3 || (c0 = word.charAt(from)) < '0') {
            return false;
        }
        if (len == 3) {
            return (c1 = word.charAt(from + 1)) >= '0' &&
                   (c2 = word.charAt(from + 2)) >= '0' &&
                   (c0 <= '1' && c1 <= '9' && c2 <= '9' ||
                    c0 == '2' && c1 <= '5' && (c2 <= '5' || c1 < '5' && c2 <= '9'));
        }
        return c0 <= '9' && (len == 1 || isValidNumericChar(word.charAt(from + 1)));
    }

    private static boolean isValidHexChar(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
    }

    private static boolean isValidNumericChar(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isValidIPv4MappedChar(char c) {
        return c == 'f' || c == 'F';
    }

    private static boolean isValidIPv4MappedSeparators(byte b0, byte b1, boolean mustBeZero) {
        // We allow IPv4 Mapped (https://tools.ietf.org/html/rfc4291#section-2.5.5.1)
        // 我们允许IPv4映射 (https://tools.ietf.org/html/rfc4291#section-2.5.5.1)
        // and IPv4 compatible (https://tools.ietf.org/html/rfc4291#section-2.5.5.1).
        // 并且与IPv4兼容 (https://tools.ietf.org/html/rfc4291#section-2.5.5.1)。
        // The IPv4 compatible is deprecated, but it allows parsing of plain IPv4 addressed into IPv6-Mapped addresses.
        // IPv4 兼容模式已弃用，但它允许将纯 IPv4 地址解析为 IPv6 映射地址。
        return b0 == b1 && (b0 == 0 || !mustBeZero && b1 == -1);
    }

    private static boolean isValidIPv4Mapped(byte[] bytes, int currentIndex, int compressBegin, int compressLength) {
        final boolean mustBeZero = compressBegin + compressLength >= 14;
        return currentIndex <= 12 && currentIndex >= 2 && (!mustBeZero || compressBegin < 12) &&
                isValidIPv4MappedSeparators(bytes[currentIndex - 1], bytes[currentIndex - 2], mustBeZero) &&
                PlatformDependent.isZero(bytes, 0, currentIndex - 3);
    }

    /**
     * Takes a {@link CharSequence} and parses it to see if it is a valid IPV4 address.
     *
     * @return true, if the string represents an IPV4 address in dotted
     *         notation, false otherwise
     */

    /**
     * 接受一个 {@link CharSequence} 并解析它以判断它是否是一个有效的 IPV4 地址。
     *
     * @return 如果字符串表示一个以点分十进制表示的 IPV4 地址，则返回 true，否则返回 false
     */
    public static boolean isValidIpV4Address(CharSequence ip) {
        return isValidIpV4Address(ip, 0, ip.length());
    }

    /**
     * Takes a {@link String} and parses it to see if it is a valid IPV4 address.
     *
     * @return true, if the string represents an IPV4 address in dotted
     *         notation, false otherwise
     */

    /**
     * 接收一个 {@link String} 并解析它以判断它是否是一个有效的 IPV4 地址。
     *
     * @return 如果字符串表示一个以点分十进制表示的 IPV4 地址，则返回 true，否则返回 false
     */
    public static boolean isValidIpV4Address(String ip) {
        return isValidIpV4Address(ip, 0, ip.length());
    }

    private static boolean isValidIpV4Address(CharSequence ip, int from, int toExcluded) {
        return ip instanceof String ? isValidIpV4Address((String) ip, from, toExcluded) :
                ip instanceof AsciiString ? isValidIpV4Address((AsciiString) ip, from, toExcluded) :
                        isValidIpV4Address0(ip, from, toExcluded);
    }

    @SuppressWarnings("DuplicateBooleanBranch")
    private static boolean isValidIpV4Address(String ip, int from, int toExcluded) {
        int len = toExcluded - from;
        int i;
        return len <= 15 && len >= 7 &&
                (i = ip.indexOf('.', from + 1)) > 0 && isValidIpV4Word(ip, from, i) &&
                (i =  ip.indexOf('.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                (i =  ip.indexOf('.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                isValidIpV4Word(ip, i + 1, toExcluded);
    }

    @SuppressWarnings("DuplicateBooleanBranch")
    private static boolean isValidIpV4Address(AsciiString ip, int from, int toExcluded) {
        int len = toExcluded - from;
        int i;
        return len <= 15 && len >= 7 &&
                (i = ip.indexOf('.', from + 1)) > 0 && isValidIpV4Word(ip, from, i) &&
                (i =  ip.indexOf('.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                (i =  ip.indexOf('.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                isValidIpV4Word(ip, i + 1, toExcluded);
    }

    @SuppressWarnings("DuplicateBooleanBranch")
    private static boolean isValidIpV4Address0(CharSequence ip, int from, int toExcluded) {
        int len = toExcluded - from;
        int i;
        return len <= 15 && len >= 7 &&
                (i = indexOf(ip, '.', from + 1)) > 0 && isValidIpV4Word(ip, from, i) &&
                (i =  indexOf(ip, '.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                (i =  indexOf(ip, '.', from = i + 2)) > 0 && isValidIpV4Word(ip, from - 1, i) &&
                isValidIpV4Word(ip, i + 1, toExcluded);
    }

    /**
     * Returns the {@link Inet6Address} representation of a {@link CharSequence} IP address.
     * <p>
     * This method will treat all IPv4 type addresses as "IPv4 mapped" (see {@link #getByName(CharSequence, boolean)})
     * @param ip {@link CharSequence} IP address to be converted to a {@link Inet6Address}
     * @return {@link Inet6Address} representation of the {@code ip} or {@code null} if not a valid IP address.
     */

    /**
     * 返回 {@link CharSequence} IP 地址的 {@link Inet6Address} 表示。
     * <p>
     * 该方法会将所有 IPv4 类型的地址视为“IPv4 映射”（参见 {@link #getByName(CharSequence, boolean)}）。
     * @param ip 要转换为 {@link Inet6Address} 的 {@link CharSequence} IP 地址
     * @return {@code ip} 的 {@link Inet6Address} 表示，如果无效则返回 {@code null}。
     */
    public static Inet6Address getByName(CharSequence ip) {
        return getByName(ip, true);
    }

    /**
     * Returns the {@link Inet6Address} representation of a {@link CharSequence} IP address.
     * <p>
     * The {@code ipv4Mapped} parameter specifies how IPv4 addresses should be treated.
     * "IPv4 mapped" format as
     * defined in <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> is supported.
     * @param ip {@link CharSequence} IP address to be converted to a {@link Inet6Address}
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} To allow IPv4 mapped inputs to be translated into {@link Inet6Address}</li>
     * <li>{@code false} Consider IPv4 mapped addresses as invalid.</li>
     * </ul>
     * @return {@link Inet6Address} representation of the {@code ip} or {@code null} if not a valid IP address.
     */

    /**
     * 返回 {@link CharSequence} IP 地址的 {@link Inet6Address} 表示。
     * <p>
     * {@code ipv4Mapped} 参数指定了 IPv4 地址应如何处理。
     * 支持按照 <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 第 2 节</a> 定义的 "IPv4 映射" 格式。
     * @param ip 要转换为 {@link Inet6Address} 的 {@link CharSequence} IP 地址
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} 允许将 IPv4 映射的输入转换为 {@link Inet6Address}</li>
     * <li>{@code false} 将 IPv4 映射地址视为无效。</li>
     * </ul>
     * @return {@code ip} 的 {@link Inet6Address} 表示，如果 IP 地址无效则返回 {@code null}。
     */
    public static Inet6Address getByName(CharSequence ip, boolean ipv4Mapped) {
        byte[] bytes = getIPv6ByName(ip, ipv4Mapped);
        if (bytes == null) {
            return null;
        }
        try {
            return Inet6Address.getByAddress(null, bytes, -1);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e); // Should never happen
        }
    }

    /**
     * Returns the byte array representation of a {@link CharSequence} IP address.
     * <p>
     * The {@code ipv4Mapped} parameter specifies how IPv4 addresses should be treated.
     * "IPv4 mapped" format as
     * defined in <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> is supported.
     * @param ip {@link CharSequence} IP address to be converted to a {@link Inet6Address}
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} To allow IPv4 mapped inputs to be translated into {@link Inet6Address}</li>
     * <li>{@code false} Consider IPv4 mapped addresses as invalid.</li>
     * </ul>
     * @return byte array representation of the {@code ip} or {@code null} if not a valid IP address.
     */

    /**
     * 返回 {@link CharSequence} 表示的 IP 地址的字节数组形式。
     * <p>
     * {@code ipv4Mapped} 参数指定了 IPv4 地址应如何处理。
     * 支持以 <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 第 2 节</a> 定义的 "IPv4 映射" 格式。
     * @param ip 要转换为 {@link Inet6Address} 的 {@link CharSequence} IP 地址
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} 允许将 IPv4 映射的输入转换为 {@link Inet6Address}</li>
     * <li>{@code false} 将 IPv4 映射地址视为无效。</li>
     * </ul>
     * @return {@code ip} 的字节数组表示，如果 IP 地址无效则返回 {@code null}。
     */
     // visible for test
     // 可见用于测试
    static byte[] getIPv6ByName(CharSequence ip, boolean ipv4Mapped) {
        final byte[] bytes = new byte[IPV6_BYTE_COUNT];
        final int ipLength = ip.length();
        int compressBegin = 0;
        int compressLength = 0;
        int currentIndex = 0;
        int value = 0;
        int begin = -1;
        int i = 0;
        int ipv6Separators = 0;
        int ipv4Separators = 0;
        int tmp;
        for (; i < ipLength; ++i) {
            final char c = ip.charAt(i);
            switch (c) {
            case ':':
                ++ipv6Separators;
                if (i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR ||
                        ipv4Separators > 0 || ipv6Separators > IPV6_MAX_SEPARATORS ||
                        currentIndex + 1 >= bytes.length) {
                    return null;
                }
                value <<= (IPV6_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin)) << 2;

                if (compressLength > 0) {
                    compressLength -= 2;
                }

                // The value integer holds at most 4 bytes from right (most significant) to left (least significant).

                // 整数值最多从右（最高有效位）到左（最低有效位）保留4个字节。
                // The following bit shifting is used to extract and re-order the individual bytes to achieve a
                // 以下位移操作用于提取并重新排序各个字节，以实现
                // left (most significant) to right (least significant) ordering.
                // 从左（最高有效位）到右（最低有效位）排序。
                bytes[currentIndex++] = (byte) (((value & 0xf) << 4) | ((value >> 4) & 0xf));
                bytes[currentIndex++] = (byte) ((((value >> 8) & 0xf) << 4) | ((value >> 12) & 0xf));
                tmp = i + 1;
                if (tmp < ipLength && ip.charAt(tmp) == ':') {
                    ++tmp;
                    if (compressBegin != 0 || (tmp < ipLength && ip.charAt(tmp) == ':')) {
                        return null;
                    }
                    ++ipv6Separators;
                    compressBegin = currentIndex;
                    compressLength = bytes.length - compressBegin - 2;
                    ++i;
                }
                value = 0;
                begin = -1;
                break;
            case '.':
                ++ipv4Separators;
                tmp = i - begin; // tmp is the length of the current segment.
                if (tmp > IPV4_MAX_CHAR_BETWEEN_SEPARATOR
                        || begin < 0
                        || ipv4Separators > IPV4_SEPARATORS
                        || (ipv6Separators > 0 && (currentIndex + compressLength < 12))
                        || i + 1 >= ipLength
                        || currentIndex >= bytes.length
                        || ipv4Separators == 1 &&
                            // We also parse pure IPv4 addresses as IPv4-Mapped for ease of use.
                            // 为了便于使用，我们也将纯IPv4地址解析为IPv4-Mapped。
                            ((!ipv4Mapped || currentIndex != 0 && !isValidIPv4Mapped(bytes, currentIndex,
                                                                                     compressBegin, compressLength)) ||
                                (tmp == 3 && (!isValidNumericChar(ip.charAt(i - 1)) ||
                                              !isValidNumericChar(ip.charAt(i - 2)) ||
                                              !isValidNumericChar(ip.charAt(i - 3))) ||
                                 tmp == 2 && (!isValidNumericChar(ip.charAt(i - 1)) ||
                                              !isValidNumericChar(ip.charAt(i - 2))) ||
                                 tmp == 1 && !isValidNumericChar(ip.charAt(i - 1))))) {
                    return null;
                }
                value <<= (IPV4_MAX_CHAR_BETWEEN_SEPARATOR - tmp) << 2;

                // The value integer holds at most 3 bytes from right (most significant) to left (least significant).

                // 该整数值最多从右（最高有效位）到左（最低有效位）保留3个字节。
                // The following bit shifting is to restructure the bytes to be left (most significant) to
                // 以下位移操作是为了将字节重组为从左（最高有效位）到右
                // right (least significant) while also accounting for each IPv4 digit is base 10.
                // 右侧（最低有效位）同时考虑到每个IPv4数字是基数为10的。
                begin = (value & 0xf) * 100 + ((value >> 4) & 0xf) * 10 + ((value >> 8) & 0xf);
                if (begin > 255) {
                    return null;
                }
                bytes[currentIndex++] = (byte) begin;
                value = 0;
                begin = -1;
                break;
            default:
                if (!isValidHexChar(c) || (ipv4Separators > 0 && !isValidNumericChar(c))) {
                    return null;
                }
                if (begin < 0) {
                    begin = i;
                } else if (i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR) {
                    return null;
                }
                // The value is treated as a sort of array of numbers because we are dealing with
                // 该值被视为一种数字数组，因为我们正在处理
                // at most 4 consecutive bytes we can use bit shifting to accomplish this.
                // 最多4个连续的字节，我们可以使用位移操作来实现这一点。
                // The most significant byte will be encountered first, and reside in the right most
                // 最高有效字节将首先遇到，并位于最右侧
                // position of the following integer
                // 以下整数的位置
                value += StringUtil.decodeHexNibble(c) << ((i - begin) << 2);
                break;
            }
        }

        final boolean isCompressed = compressBegin > 0;
        // Finish up last set of data that was accumulated in the loop (or before the loop)
        // 完成循环中（或循环前）累积的最后一批数据
        if (ipv4Separators > 0) {
            if (begin > 0 && i - begin > IPV4_MAX_CHAR_BETWEEN_SEPARATOR ||
                    ipv4Separators != IPV4_SEPARATORS ||
                    currentIndex >= bytes.length) {
                return null;
            }
            if (!(ipv6Separators == 0 || ipv6Separators >= IPV6_MIN_SEPARATORS &&
                           (!isCompressed && (ipv6Separators == 6 && ip.charAt(0) != ':') ||
                            isCompressed && (ipv6Separators < IPV6_MAX_SEPARATORS &&
                                             (ip.charAt(0) != ':' || compressBegin <= 2))))) {
                return null;
            }
            value <<= (IPV4_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin)) << 2;

            // The value integer holds at most 3 bytes from right (most significant) to left (least significant).

            // 该整数值最多从右（最高有效位）到左（最低有效位）保留3个字节。
            // The following bit shifting is to restructure the bytes to be left (most significant) to
            // 以下位移操作是为了将字节重组为从左（最高有效位）到右
            // right (least significant) while also accounting for each IPv4 digit is base 10.
            // 右侧（最低有效位）同时考虑到每个IPv4数字是基数为10的。
            begin = (value & 0xf) * 100 + ((value >> 4) & 0xf) * 10 + ((value >> 8) & 0xf);
            if (begin > 255) {
                return null;
            }
            bytes[currentIndex++] = (byte) begin;
        } else {
            tmp = ipLength - 1;
            if (begin > 0 && i - begin > IPV6_MAX_CHAR_BETWEEN_SEPARATOR ||
                    ipv6Separators < IPV6_MIN_SEPARATORS ||
                    !isCompressed && (ipv6Separators + 1 != IPV6_MAX_SEPARATORS  ||
                                      ip.charAt(0) == ':' || ip.charAt(tmp) == ':') ||
                    isCompressed && (ipv6Separators > IPV6_MAX_SEPARATORS ||
                        (ipv6Separators == IPV6_MAX_SEPARATORS &&
                          (compressBegin <= 2 && ip.charAt(0) != ':' ||
                           compressBegin >= 14 && ip.charAt(tmp) != ':'))) ||
                    currentIndex + 1 >= bytes.length ||
                    begin < 0 && ip.charAt(tmp - 1) != ':' ||
                    compressBegin > 2 && ip.charAt(0) == ':') {
                return null;
            }
            if (begin >= 0 && i - begin <= IPV6_MAX_CHAR_BETWEEN_SEPARATOR) {
                value <<= (IPV6_MAX_CHAR_BETWEEN_SEPARATOR - (i - begin)) << 2;
            }
            // The value integer holds at most 4 bytes from right (most significant) to left (least significant).
            // 整数值最多从右（最高有效位）到左（最低有效位）保留4个字节。
            // The following bit shifting is used to extract and re-order the individual bytes to achieve a
            // 以下位移操作用于提取并重新排序各个字节，以实现
            // left (most significant) to right (least significant) ordering.
            // 从左（最高有效位）到右（最低有效位）排序。
            bytes[currentIndex++] = (byte) (((value & 0xf) << 4) | ((value >> 4) & 0xf));
            bytes[currentIndex++] = (byte) ((((value >> 8) & 0xf) << 4) | ((value >> 12) & 0xf));
        }

        if (currentIndex < bytes.length) {
            int toBeCopiedLength = currentIndex - compressBegin;
            int targetIndex = bytes.length - toBeCopiedLength;
            System.arraycopy(bytes, compressBegin, bytes, targetIndex, toBeCopiedLength);
            // targetIndex is also the `toIndex` to fill 0
            // targetIndex 也是要填充 0 的 `toIndex`
            Arrays.fill(bytes, compressBegin, targetIndex, (byte) 0);
        }

        if (ipv4Separators > 0) {
            // We only support IPv4-Mapped addresses [1] because IPv4-Compatible addresses are deprecated [2].
            // 我们仅支持IPv4映射地址[1]，因为IPv4兼容地址已被弃用[2]。
            // [1] https://tools.ietf.org/html/rfc4291#section-2.5.5.2
            // [1] https://tools.ietf.org/html/rfc4291#section-2.5.5.2
            // [2] https://tools.ietf.org/html/rfc4291#section-2.5.5.1
            // [2] https://tools.ietf.org/html/rfc4291#section-2.5.5.1
            bytes[10] = bytes[11] = (byte) 0xff;
        }

        return bytes;
    }

    /**
     * Returns the {@link String} representation of an {@link InetSocketAddress}.
     * <p>
     * The output does not include Scope ID.
     * @param addr {@link InetSocketAddress} to be converted to an address string
     * @return {@code String} containing the text-formatted IP address
     */

    /**
     * 返回 {@link InetSocketAddress} 的 {@link String} 表示。
     * <p>
     * 输出不包含 Scope ID。
     * @param addr 要转换为地址字符串的 {@link InetSocketAddress}
     * @return 包含文本格式 IP 地址的 {@code String}
     */
    public static String toSocketAddressString(InetSocketAddress addr) {
        String port = String.valueOf(addr.getPort());
        final StringBuilder sb;

        if (addr.isUnresolved()) {
            String hostname = getHostname(addr);
            sb = newSocketAddressStringBuilder(hostname, port, !isValidIpV6Address(hostname));
        } else {
            InetAddress address = addr.getAddress();
            String hostString = toAddressString(address);
            sb = newSocketAddressStringBuilder(hostString, port, address instanceof Inet4Address);
        }
        return sb.append(':').append(port).toString();
    }

    /**
     * Returns the {@link String} representation of a host port combo.
     */

    /**
     * 返回主机端口组合的 {@link String} 表示。
     */
    public static String toSocketAddressString(String host, int port) {
        String portStr = String.valueOf(port);
        return newSocketAddressStringBuilder(
                host, portStr, !isValidIpV6Address(host)).append(':').append(portStr).toString();
    }

    private static StringBuilder newSocketAddressStringBuilder(String host, String port, boolean ipv4) {
        int hostLen = host.length();
        if (ipv4) {
            // Need to include enough space for hostString:port.
            // 需要为 hostString:port 预留足够的空间。
            return new StringBuilder(hostLen + 1 + port.length()).append(host);
        }
        // Need to include enough space for [hostString]:port.
        // 需要为 [hostString]:port 留出足够的空间。
        StringBuilder stringBuilder = new StringBuilder(hostLen + 3 + port.length());
        if (hostLen > 1 && host.charAt(0) == '[' && host.charAt(hostLen - 1) == ']') {
            return stringBuilder.append(host);
        }
        return stringBuilder.append('[').append(host).append(']');
    }

    /**
     * Returns the {@link String} representation of an {@link InetAddress}.
     * <ul>
     * <li>Inet4Address results are identical to {@link InetAddress#getHostAddress()}</li>
     * <li>Inet6Address results adhere to
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 section 4</a></li>
     * </ul>
     * <p>
     * The output does not include Scope ID.
     * @param ip {@link InetAddress} to be converted to an address string
     * @return {@code String} containing the text-formatted IP address
     */

    /**
     * 返回 {@link InetAddress} 的 {@link String} 表示形式。
     * <ul>
     * <li>Inet4Address 的结果与 {@link InetAddress#getHostAddress()} 相同</li>
     * <li>Inet6Address 的结果遵循
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 第 4 节</a></li>
     * </ul>
     * <p>
     * 输出不包含 Scope ID。
     * @param ip 要转换为地址字符串的 {@link InetAddress}
     * @return 包含文本格式 IP 地址的 {@code String}
     */
    public static String toAddressString(InetAddress ip) {
        return toAddressString(ip, false);
    }

    /**
     * Returns the {@link String} representation of an {@link InetAddress}.
     * <ul>
     * <li>Inet4Address results are identical to {@link InetAddress#getHostAddress()}</li>
     * <li>Inet6Address results adhere to
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 section 4</a> if
     * {@code ipv4Mapped} is false.  If {@code ipv4Mapped} is true then "IPv4 mapped" format
     * from <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> will be supported.
     * The compressed result will always obey the compression rules defined in
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 section 4</a></li>
     * </ul>
     * <p>
     * The output does not include Scope ID.
     * @param ip {@link InetAddress} to be converted to an address string
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} to stray from strict rfc 5952 and support the "IPv4 mapped" format
     * defined in <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 section 2</a> while still
     * following the updated guidelines in
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 section 4</a></li>
     * <li>{@code false} to strictly follow rfc 5952</li>
     * </ul>
     * @return {@code String} containing the text-formatted IP address
     */

    /**
     * 返回 {@link InetAddress} 的 {@link String} 表示形式。
     * <ul>
     * <li>Inet4Address 的结果与 {@link InetAddress#getHostAddress()} 相同</li>
     * <li>如果 {@code ipv4Mapped} 为 false，Inet6Address 的结果遵循
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 第 4 节</a>。如果 {@code ipv4Mapped} 为 true，则支持
     * <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 第 2 节</a> 中定义的 "IPv4 mapped" 格式。
     * 压缩结果将始终遵循
     * <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 第 4 节</a> 中定义的压缩规则</li>
     * </ul>
     * <p>
     * 输出不包括 Scope ID。
     * @param ip 要转换为地址字符串的 {@link InetAddress}
     * @param ipv4Mapped
     * <ul>
     * <li>{@code true} 表示偏离严格的 rfc 5952，支持
     * <a href="https://tools.ietf.org/html/rfc4291#section-2.5.5">rfc 4291 第 2 节</a> 中定义的 "IPv4 mapped" 格式，同时仍然
     * 遵循 <a href="https://tools.ietf.org/html/rfc5952#section-4">rfc 5952 第 4 节</a> 中的更新指南</li>
     * <li>{@code false} 表示严格遵循 rfc 5952</li>
     * </ul>
     * @return 包含文本格式 IP 地址的 {@code String}
     */
    public static String toAddressString(InetAddress ip, boolean ipv4Mapped) {
        if (ip instanceof Inet4Address) {
            return ip.getHostAddress();
        }
        if (!(ip instanceof Inet6Address)) {
            throw new IllegalArgumentException("Unhandled type: " + ip);
        }

        return toAddressString(ip.getAddress(), 0, ipv4Mapped);
    }

    private static String toAddressString(byte[] bytes, int offset, boolean ipv4Mapped) {
        final int[] words = new int[IPV6_WORD_COUNT];
        int i;
        final int end = offset + words.length;
        for (i = offset; i < end; ++i) {
            words[i] = ((bytes[i << 1] & 0xff) << 8) | (bytes[(i << 1) + 1] & 0xff);
        }

        // Find longest run of 0s, tie goes to first found instance

        // 找到最长的0的连续串，如果长度相同则选择第一个找到的实例
        int currentStart = -1;
        int currentLength;
        int shortestStart = -1;
        int shortestLength = 0;
        for (i = 0; i < words.length; ++i) {
            if (words[i] == 0) {
                if (currentStart < 0) {
                    currentStart = i;
                }
            } else if (currentStart >= 0) {
                currentLength = i - currentStart;
                if (currentLength > shortestLength) {
                    shortestStart = currentStart;
                    shortestLength = currentLength;
                }
                currentStart = -1;
            }
        }
        // If the array ends on a streak of zeros, make sure we account for it
        // 如果数组以一连串的零结尾，请确保我们对此进行了处理
        if (currentStart >= 0) {
            currentLength = i - currentStart;
            if (currentLength > shortestLength) {
                shortestStart = currentStart;
                shortestLength = currentLength;
            }
        }
        // Ignore the longest streak if it is only 1 long
        // 如果最长连胜仅为1，则忽略
        if (shortestLength == 1) {
            shortestLength = 0;
            shortestStart = -1;
        }

        // Translate to string taking into account longest consecutive 0s

        // 翻译为字符串，考虑最长的连续0
        final int shortestEnd = shortestStart + shortestLength;
        final StringBuilder b = new StringBuilder(IPV6_MAX_CHAR_COUNT);
        if (shortestEnd < 0) { // Optimization when there is no compressing needed
            b.append(Integer.toHexString(words[0]));
            for (i = 1; i < words.length; ++i) {
                b.append(':');
                b.append(Integer.toHexString(words[i]));
            }
        } else { // General case that can handle compressing (and not compressing)
            // Loop unroll the first index (so we don't constantly check i==0 cases in loop)
            // 展开第一个索引的循环（这样我们就不必在循环中不断检查i==0的情况）
            final boolean isIpv4Mapped;
            if (inRangeEndExclusive(0, shortestStart, shortestEnd)) {
                b.append("::");
                isIpv4Mapped = ipv4Mapped && (shortestEnd == 5 && words[5] == 0xffff);
            } else {
                b.append(Integer.toHexString(words[0]));
                isIpv4Mapped = false;
            }
            for (i = 1; i < words.length; ++i) {
                if (!inRangeEndExclusive(i, shortestStart, shortestEnd)) {
                    if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd)) {
                        // If the last index was not part of the shortened sequence
                        // 如果最后一个索引不是缩短序列的一部分
                        if (!isIpv4Mapped || i == 6) {
                            b.append(':');
                        } else {
                            b.append('.');
                        }
                    }
                    if (isIpv4Mapped && i > 5) {
                        b.append(words[i] >> 8);
                        b.append('.');
                        b.append(words[i] & 0xff);
                    } else {
                        b.append(Integer.toHexString(words[i]));
                    }
                } else if (!inRangeEndExclusive(i - 1, shortestStart, shortestEnd)) {
                    // If we are in the shortened sequence and the last index was not
                    // 如果我们在缩短的序列中并且最后一个索引不是
                    b.append("::");
                }
            }
        }

        return b.toString();
    }

    /**
     * Returns {@link InetSocketAddress#getHostString()} if Java >= 7,
     * or {@link InetSocketAddress#getHostName()} otherwise.
     * @param addr The address
     * @return the host string
     */

    /**
     * 如果 Java >= 7，返回 {@link InetSocketAddress#getHostString()}，
     * 否则返回 {@link InetSocketAddress#getHostName()}。
     * @param addr 地址
     * @return 主机字符串
     */
    public static String getHostname(InetSocketAddress addr) {
        return PlatformDependent.javaVersion() >= 7 ? addr.getHostString() : addr.getHostName();
    }

    /**
     * Does a range check on {@code value} if is within {@code start} (inclusive) and {@code end} (exclusive).
     * @param value The value to checked if is within {@code start} (inclusive) and {@code end} (exclusive)
     * @param start The start of the range (inclusive)
     * @param end The end of the range (exclusive)
     * @return
     * <ul>
     * <li>{@code true} if {@code value} if is within {@code start} (inclusive) and {@code end} (exclusive)</li>
     * <li>{@code false} otherwise</li>
     * </ul>
     */

    /**
     * 检查 {@code value} 是否在 {@code start}（包含）和 {@code end}（不包含）之间。
     * @param value 要检查的值，判断是否在 {@code start}（包含）和 {@code end}（不包含）之间
     * @param start 范围的起始值（包含）
     * @param end 范围的结束值（不包含）
     * @return
     * <ul>
     * <li>{@code true} 如果 {@code value} 在 {@code start}（包含）和 {@code end}（不包含）之间</li>
     * <li>{@code false} 否则</li>
     * </ul>
     */
    private static boolean inRangeEndExclusive(int value, int start, int end) {
        return value >= start && value < end;
    }

    /**
     * A constructor to stop this class being constructed.
     */

    /**
     * 一个构造函数，用于阻止此类被实例化。
     */
    private NetUtil() {
        // Unused
        // 未使用
    }
}
