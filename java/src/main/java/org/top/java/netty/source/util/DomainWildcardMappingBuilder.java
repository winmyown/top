
package org.top.java.netty.source.util;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Builder that allows to build {@link Mapping}s that support
 * <a href="https://tools.ietf.org/search/rfc6125#section-6.4">DNS wildcard</a> matching.
 * @param <V> the type of the value that we map to.
 */

/**
 * 构建器，允许构建支持
 * <a href="https://tools.ietf.org/search/rfc6125#section-6.4">DNS通配符</a>匹配的 {@link Mapping}。
 * @param <V> 我们映射到的值的类型。
 */
public class DomainWildcardMappingBuilder<V> {

    private final V defaultValue;
    private final Map<String, V> map;

    /**
     * Constructor with default initial capacity of the map holding the mappings
     *
     * @param defaultValue the default value for {@link Mapping#map(Object)} )} to return
     *                     when nothing matches the input
     */

    /**
     * 使用默认初始容量的映射构造函数
     *
     * @param defaultValue 当输入没有匹配项时，{@link Mapping#map(Object)} 返回的默认值
     */
    public DomainWildcardMappingBuilder(V defaultValue) {
        this(4, defaultValue);
    }

    /**
     * Constructor with initial capacity of the map holding the mappings
     *
     * @param initialCapacity initial capacity for the internal map
     * @param defaultValue    the default value for {@link Mapping#map(Object)} to return
     *                        when nothing matches the input
     */

    /**
     * 带有映射持有初始容量的构造函数
     *
     * @param initialCapacity 内部映射的初始容量
     * @param defaultValue    当输入不匹配任何内容时，{@link Mapping#map(Object)} 返回的默认值
     */
    public DomainWildcardMappingBuilder(int initialCapacity, V defaultValue) {
        this.defaultValue = checkNotNull(defaultValue, "defaultValue");
        map = new LinkedHashMap<String, V>(initialCapacity);
    }

    /**
     * Adds a mapping that maps the specified (optionally wildcard) host name to the specified output value.
     * {@code null} values are forbidden for both hostnames and values.
     * <p>
     * <a href="https://tools.ietf.org/search/rfc6125#section-6.4">DNS wildcard</a> is supported as hostname. The
     * wildcard will only match one sub-domain deep and only when wildcard is used as the most-left label.
     *
     * For example:
     *
     * <p>
     *  *.netty.io will match xyz.netty.io but NOT abc.xyz.netty.io
     * </p>
     *
     * @param hostname the host name (optionally wildcard)
     * @param output   the output value that will be returned by {@link Mapping#map(Object)}
     *                 when the specified host name matches the specified input host name
     */

    /**
     * 添加一个映射，将指定的（可选通配符）主机名映射到指定的输出值。
     * {@code null} 值对于主机名和值都是禁止的。
     * <p>
     * <a href="https://tools.ietf.org/search/rfc6125#section-6.4">DNS 通配符</a> 作为主机名被支持。通配符
     * 只会匹配一个子域深度，并且仅在通配符用作最左侧标签时有效。
     *
     * 例如：
     *
     * <p>
     *  *.netty.io 会匹配 xyz.netty.io 但不会匹配 abc.xyz.netty.io
     * </p>
     *
     * @param hostname 主机名（可选通配符）
     * @param output   当指定的主机名与指定的输入主机名匹配时，{@link Mapping#map(Object)} 将返回的输出值
     */
    public DomainWildcardMappingBuilder<V> add(String hostname, V output) {
        map.put(normalizeHostName(hostname),
                checkNotNull(output, "output"));
        return this;
    }

    private String normalizeHostName(String hostname) {
        checkNotNull(hostname, "hostname");
        if (hostname.isEmpty() || hostname.charAt(0) == '.') {
            throw new IllegalArgumentException("Hostname '" + hostname + "' not valid");
        }
        hostname = ImmutableDomainWildcardMapping.normalize(checkNotNull(hostname, "hostname"));
        if (hostname.charAt(0) == '*') {
            if (hostname.length() < 3 || hostname.charAt(1) != '.') {
                throw new IllegalArgumentException("Wildcard Hostname '" + hostname + "'not valid");
            }
            return hostname.substring(1);
        }
        return hostname;
    }
    /**
     * Creates a new instance of an immutable {@link Mapping}.
     *
     * @return new {@link Mapping} instance
     */
    /**
     * 创建一个新的不可变 {@link Mapping} 实例。
     *
     * @return 新的 {@link Mapping} 实例
     */
    public Mapping<String, V> build() {
        return new ImmutableDomainWildcardMapping<V>(defaultValue, map);
    }

    private static final class ImmutableDomainWildcardMapping<V> implements Mapping<String, V> {
        private static final String REPR_HEADER = "ImmutableDomainWildcardMapping(default: ";
        private static final String REPR_MAP_OPENING = ", map: ";
        private static final String REPR_MAP_CLOSING = ")";

        private final V defaultValue;
        private final Map<String, V> map;

        ImmutableDomainWildcardMapping(V defaultValue, Map<String, V> map) {
            this.defaultValue = defaultValue;
            this.map = new LinkedHashMap<String, V>(map);
        }

        @Override
        public V map(String hostname) {
            if (hostname != null) {
                hostname = normalize(hostname);

                // Let's try an exact match first

                // 让我们先尝试精确匹配
                V value = map.get(hostname);
                if (value != null) {
                    return value;
                }

                // No exact match, let's try a wildcard match.

                // 没有完全匹配，尝试使用通配符匹配。
                int idx = hostname.indexOf('.');
                if (idx != -1) {
                    value = map.get(hostname.substring(idx));
                    if (value != null) {
                        return value;
                    }
                }
            }

            return defaultValue;
        }

        @SuppressWarnings("deprecation")
        static String normalize(String hostname) {
            return DomainNameMapping.normalizeHostname(hostname);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(REPR_HEADER).append(defaultValue).append(REPR_MAP_OPENING).append('{');

            for (Map.Entry<String, V> entry : map.entrySet()) {
                String hostname = entry.getKey();
                if (hostname.charAt(0) == '.') {
                    hostname = '*' + hostname;
                }
                sb.append(hostname).append('=').append(entry.getValue()).append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.append('}').append(REPR_MAP_CLOSING).toString();
        }
    }
}
