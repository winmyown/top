
package org.top.java.netty.source.util;

/**
 * A singleton which is safe to compare via the {@code ==} operator. Created and managed by {@link ConstantPool}.
 */

/**
 * 一个可以通过 {@code ==} 操作符安全比较的单例。由 {@link ConstantPool} 创建和管理。
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * Returns the unique number assigned to this {@link Constant}.
     */

    /**
     * 返回分配给此 {@link Constant} 的唯一编号。
     */
    int id();

    /**
     * Returns the name of this {@link Constant}.
     */

    /**
     * 返回此 {@link Constant} 的名称。
     */
    String name();
}
