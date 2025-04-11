

package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.StringUtil;

import java.net.IDN;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.StringUtil.commonSuffixOfLength;

/**
 * Maps a domain name to its associated value object.
 * <p>
 * DNS wildcard is supported as hostname, so you can use {@code *.netty.io} to match both {@code netty.io}
 * and {@code downloads.netty.io}.
 * </p>
 * @deprecated Use {@link DomainWildcardMappingBuilder}}
 */

/**
 * 将域名映射到其关联的值对象。
 * <p>
 * 支持将DNS通配符作为主机名，因此您可以使用 {@code *.netty.io} 来匹配 {@code netty.io} 和 {@code downloads.netty.io}。
 * </p>
 * @deprecated 使用 {@link DomainWildcardMappingBuilder}}
 */
@Deprecated
public class DomainNameMapping<V> implements Mapping<String, V> {

    final V defaultValue;
    private final Map<String, V> map;
    private final Map<String, V> unmodifiableMap;

    /**
     * Creates a default, order-sensitive mapping. If your hostnames are in conflict, the mapping
     * will choose the one you add first.
     *
     * @param defaultValue the default value for {@link #map(String)} to return when nothing matches the input
     * @deprecated use {@link DomainNameMappingBuilder} to create and fill the mapping instead
     */

    /**
     * 创建一个默认的、顺序敏感的映射。如果您的域名存在冲突，映射将选择您最先添加的那个。
     *
     * @param defaultValue 当输入不匹配任何项时，{@link #map(String)} 返回的默认值
     * @deprecated 请使用 {@link DomainNameMappingBuilder} 来创建和填充映射
     */
    @Deprecated
    public DomainNameMapping(V defaultValue) {
        this(4, defaultValue);
    }

    /**
     * Creates a default, order-sensitive mapping. If your hostnames are in conflict, the mapping
     * will choose the one you add first.
     *
     * @param initialCapacity initial capacity for the internal map
     * @param defaultValue    the default value for {@link #map(String)} to return when nothing matches the input
     * @deprecated use {@link DomainNameMappingBuilder} to create and fill the mapping instead
     */

    /**
     * 创建一个默认的、顺序敏感的映射。如果主机名存在冲突，映射将选择最先添加的那个。
     *
     * @param initialCapacity 内部映射的初始容量
     * @param defaultValue    {@link #map(String)} 在输入不匹配时返回的默认值
     * @deprecated 请使用 {@link DomainNameMappingBuilder} 来创建和填充映射
     */
    @Deprecated
    public DomainNameMapping(int initialCapacity, V defaultValue) {
        this(new LinkedHashMap<String, V>(initialCapacity), defaultValue);
    }

    DomainNameMapping(Map<String, V> map, V defaultValue) {
        this.defaultValue = checkNotNull(defaultValue, "defaultValue");
        this.map = map;
        unmodifiableMap = map != null ? Collections.unmodifiableMap(map)
                                      : null;
    }

    /**
     * Adds a mapping that maps the specified (optionally wildcard) host name to the specified output value.
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a> is supported as hostname.
     * For example, you can use {@code *.netty.io} to match {@code netty.io} and {@code downloads.netty.io}.
     * </p>
     *
     * @param hostname the host name (optionally wildcard)
     * @param output   the output value that will be returned by {@link #map(String)} when the specified host name
     *                 matches the specified input host name
     * @deprecated use {@link DomainNameMappingBuilder} to create and fill the mapping instead
     */

    /**
     * 添加一个映射，将指定的（可选的通配符）主机名映射到指定的输出值。
     * <p>
     * 支持使用<a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS通配符</a>作为主机名。
     * 例如，您可以使用 {@code *.netty.io} 来匹配 {@code netty.io} 和 {@code downloads.netty.io}。
     * </p>
     *
     * @param hostname 主机名（可选的通配符）
     * @param output   当指定的主机名与输入的主机名匹配时，{@link #map(String)} 将返回的输出值
     * @deprecated 请使用 {@link DomainNameMappingBuilder} 来创建和填充映射
     */
    @Deprecated
    public DomainNameMapping<V> add(String hostname, V output) {
        map.put(normalizeHostname(checkNotNull(hostname, "hostname")), checkNotNull(output, "output"));
        return this;
    }

    /**
     * Simple function to match <a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a>.
     */

    /**
     * 用于匹配<a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS通配符</a>的简单函数。
     */
    static boolean matches(String template, String hostName) {
        if (template.startsWith("*.")) {
            return template.regionMatches(2, hostName, 0, hostName.length())
                || commonSuffixOfLength(hostName, template, template.length() - 1);
        }
        return template.equals(hostName);
    }

    /**
     * IDNA ASCII conversion and case normalization
     */

    /**
     * IDNA ASCII 转换和大小写规范化
     */
    static String normalizeHostname(String hostname) {
        if (needsNormalization(hostname)) {
            hostname = IDN.toASCII(hostname, IDN.ALLOW_UNASSIGNED);
        }
        return hostname.toLowerCase(Locale.US);
    }

    private static boolean needsNormalization(String hostname) {
        final int length = hostname.length();
        for (int i = 0; i < length; i++) {
            int c = hostname.charAt(i);
            if (c > 0x7F) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V map(String hostname) {
        if (hostname != null) {
            hostname = normalizeHostname(hostname);

            for (Map.Entry<String, V> entry : map.entrySet()) {
                if (matches(entry.getKey(), hostname)) {
                    return entry.getValue();
                }
            }
        }
        return defaultValue;
    }

    /**
     * Returns a read-only {@link Map} of the domain mapping patterns and their associated value objects.
     */

    /**
     * 返回一个只读的{@link Map}，包含域映射模式及其关联的值对象。
     */
    public Map<String, V> asMap() {
        return unmodifiableMap;
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) + "(default: " + defaultValue + ", map: " + map + ')';
    }
}
