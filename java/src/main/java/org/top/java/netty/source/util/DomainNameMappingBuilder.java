

package org.top.java.netty.source.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Builder for immutable {@link DomainNameMapping} instances.
 *
 * @param <V> concrete type of value objects
 * @deprecated Use {@link DomainWildcardMappingBuilder}
 */

/**
 * 用于构建不可变的 {@link DomainNameMapping} 实例的构建器。
 *
 * @param <V> 值对象的具体类型
 * @deprecated 请使用 {@link DomainWildcardMappingBuilder}
 */
@Deprecated
public final class DomainNameMappingBuilder<V> {

    private final V defaultValue;
    private final Map<String, V> map;

    /**
     * Constructor with default initial capacity of the map holding the mappings
     *
     * @param defaultValue the default value for {@link DomainNameMapping#map(String)} to return
     *                     when nothing matches the input
     */

    /**
     * 使用默认初始容量的映射构造函数
     *
     * @param defaultValue 当输入不匹配任何内容时，{@link DomainNameMapping#map(String)} 返回的默认值
     */
    public DomainNameMappingBuilder(V defaultValue) {
        this(4, defaultValue);
    }

    /**
     * Constructor with initial capacity of the map holding the mappings
     *
     * @param initialCapacity initial capacity for the internal map
     * @param defaultValue    the default value for {@link DomainNameMapping#map(String)} to return
     *                        when nothing matches the input
     */

    /**
     * 使用初始容量构造映射的构造函数
     *
     * @param initialCapacity 内部映射的初始容量
     * @param defaultValue    当输入不匹配任何内容时，{@link DomainNameMapping#map(String)} 返回的默认值
     */
    public DomainNameMappingBuilder(int initialCapacity, V defaultValue) {
        this.defaultValue = checkNotNull(defaultValue, "defaultValue");
        map = new LinkedHashMap<String, V>(initialCapacity);
    }

    /**
     * Adds a mapping that maps the specified (optionally wildcard) host name to the specified output value.
     * Null values are forbidden for both hostnames and values.
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a> is supported as hostname.
     * For example, you can use {@code *.netty.io} to match {@code netty.io} and {@code downloads.netty.io}.
     * </p>
     *
     * @param hostname the host name (optionally wildcard)
     * @param output   the output value that will be returned by {@link DomainNameMapping#map(String)}
     *                 when the specified host name matches the specified input host name
     */

    /**
     * 添加一个映射，将指定的（可选通配符）主机名映射到指定的输出值。
     * 主机名和值都不允许为 null。
     * <p>
     * 支持使用 <a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS 通配符</a> 作为主机名。
     * 例如，可以使用 {@code *.netty.io} 来匹配 {@code netty.io} 和 {@code downloads.netty.io}。
     * </p>
     *
     * @param hostname 主机名（可选通配符）
     * @param output   当指定的主机名与输入的主机名匹配时，{@link DomainNameMapping#map(String)} 将返回的输出值
     */
    public DomainNameMappingBuilder<V> add(String hostname, V output) {
        map.put(checkNotNull(hostname, "hostname"), checkNotNull(output, "output"));
        return this;
    }

    /**
     * Creates a new instance of immutable {@link DomainNameMapping}
     * Attempts to add new mappings to the result object will cause {@link UnsupportedOperationException} to be thrown
     *
     * @return new {@link DomainNameMapping} instance
     */

    /**
     * 创建一个新的不可变 {@link DomainNameMapping} 实例
     * 尝试向结果对象添加新映射将导致抛出 {@link UnsupportedOperationException}
     *
     * @return 新的 {@link DomainNameMapping} 实例
     */
    public DomainNameMapping<V> build() {
        return new ImmutableDomainNameMapping<V>(defaultValue, map);
    }

    /**
     * Immutable mapping from domain name pattern to its associated value object.
     * Mapping is represented by two arrays: keys and values. Key domainNamePatterns[i] is associated with values[i].
     *
     * @param <V> concrete type of value objects
     */

    /**
     * 从域名模式到其关联值对象的不可变映射。
     * 映射由两个数组表示：keys 和 values。键 domainNamePatterns[i] 与 values[i] 相关联。
     *
     * @param <V> 值对象的具体类型
     */
    private static final class ImmutableDomainNameMapping<V> extends DomainNameMapping<V> {
        private static final String REPR_HEADER = "ImmutableDomainNameMapping(default: ";
        private static final String REPR_MAP_OPENING = ", map: {";
        private static final String REPR_MAP_CLOSING = "})";
        private static final int REPR_CONST_PART_LENGTH =
            REPR_HEADER.length() + REPR_MAP_OPENING.length() + REPR_MAP_CLOSING.length();

        private final String[] domainNamePatterns;
        private final V[] values;
        private final Map<String, V> map;

        @SuppressWarnings("unchecked")
        private ImmutableDomainNameMapping(V defaultValue, Map<String, V> map) {
            super(null, defaultValue);

            Set<Map.Entry<String, V>> mappings = map.entrySet();
            int numberOfMappings = mappings.size();
            domainNamePatterns = new String[numberOfMappings];
            values = (V[]) new Object[numberOfMappings];

            final Map<String, V> mapCopy = new LinkedHashMap<String, V>(map.size());
            int index = 0;
            for (Map.Entry<String, V> mapping : mappings) {
                final String hostname = normalizeHostname(mapping.getKey());
                final V value = mapping.getValue();
                domainNamePatterns[index] = hostname;
                values[index] = value;
                mapCopy.put(hostname, value);
                ++index;
            }

            this.map = Collections.unmodifiableMap(mapCopy);
        }

        @Override
        @Deprecated
        public DomainNameMapping<V> add(String hostname, V output) {
            throw new UnsupportedOperationException(
                "Immutable DomainNameMapping does not support modification after initial creation");
        }

        @Override
        public V map(String hostname) {
            if (hostname != null) {
                hostname = normalizeHostname(hostname);

                int length = domainNamePatterns.length;
                for (int index = 0; index < length; ++index) {
                    if (matches(domainNamePatterns[index], hostname)) {
                        return values[index];
                    }
                }
            }

            return defaultValue;
        }

        @Override
        public Map<String, V> asMap() {
            return map;
        }

        @Override
        public String toString() {
            String defaultValueStr = defaultValue.toString();

            int numberOfMappings = domainNamePatterns.length;
            if (numberOfMappings == 0) {
                return REPR_HEADER + defaultValueStr + REPR_MAP_OPENING + REPR_MAP_CLOSING;
            }

            String pattern0 = domainNamePatterns[0];
            String value0 = values[0].toString();
            int oneMappingLength = pattern0.length() + value0.length() + 3; // 2 for separator ", " and 1 for '='
            int estimatedBufferSize = estimateBufferSize(defaultValueStr.length(), numberOfMappings, oneMappingLength);

            StringBuilder sb = new StringBuilder(estimatedBufferSize)
                .append(REPR_HEADER).append(defaultValueStr).append(REPR_MAP_OPENING);

            appendMapping(sb, pattern0, value0);
            for (int index = 1; index < numberOfMappings; ++index) {
                sb.append(", ");
                appendMapping(sb, index);
            }

            return sb.append(REPR_MAP_CLOSING).toString();
        }

        /**
         * Estimates the length of string representation of the given instance:
         * est = lengthOfConstantComponents + defaultValueLength + (estimatedMappingLength * numOfMappings) * 1.10
         *
         * @param defaultValueLength     length of string representation of {@link #defaultValue}
         * @param numberOfMappings       number of mappings the given instance holds,
         *                               e.g. {@link #domainNamePatterns#length}
         * @param estimatedMappingLength estimated size taken by one mapping
         * @return estimated length of string returned by {@link #toString()}
         */

        /**
         * 估计给定实例的字符串表示的长度：
         * est = lengthOfConstantComponents + defaultValueLength + (estimatedMappingLength * numOfMappings) * 1.10
         *
         * @param defaultValueLength     {@link #defaultValue} 的字符串表示的长度
         * @param numberOfMappings       给定实例持有的映射数量，
         *                               例如 {@link #domainNamePatterns#length}
         * @param estimatedMappingLength 估计一个映射占用的长度
         * @return 估计的 {@link #toString()} 返回的字符串长度
         */
        private static int estimateBufferSize(int defaultValueLength,
                                              int numberOfMappings,
                                              int estimatedMappingLength) {
            return REPR_CONST_PART_LENGTH + defaultValueLength
                + (int) (estimatedMappingLength * numberOfMappings * 1.10);
        }

        private StringBuilder appendMapping(StringBuilder sb, int mappingIndex) {
            return appendMapping(sb, domainNamePatterns[mappingIndex], values[mappingIndex].toString());
        }

        private static StringBuilder appendMapping(StringBuilder sb, String domainNamePattern, String value) {
            return sb.append(domainNamePattern).append('=').append(value);
        }
    }
}
