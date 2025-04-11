

package org.top.java.netty.source.util;

/**
 * Builder for immutable {@link DomainNameMapping} instances.
 *
 * @param <V> concrete type of value objects
 * @deprecated Use {@link DomainWildcardMappingBuilder} instead.
 */

/**
 * 用于构建不可变 {@link DomainNameMapping} 实例的构建器。
 *
 * @param <V> 值对象的具体类型
 * @deprecated 请使用 {@link DomainWildcardMappingBuilder} 代替。
 */
@Deprecated
public final class DomainMappingBuilder<V> {

    private final DomainNameMappingBuilder<V> builder;

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
    public DomainMappingBuilder(V defaultValue) {
        builder = new DomainNameMappingBuilder<V>(defaultValue);
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
    public DomainMappingBuilder(int initialCapacity, V defaultValue) {
        builder = new DomainNameMappingBuilder<V>(initialCapacity, defaultValue);
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
    public DomainMappingBuilder<V> add(String hostname, V output) {
        builder.add(hostname, output);
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
        return builder.build();
    }
}
