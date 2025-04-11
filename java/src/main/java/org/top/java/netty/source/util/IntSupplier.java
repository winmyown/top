
package org.top.java.netty.source.util;

/**
 * Represents a supplier of {@code int}-valued results.
 */

/**
 * 表示一个提供 {@code int} 类型结果的供应商。
 */
public interface IntSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     */

    /**
     * 获取一个结果。
     *
     * @return 一个结果
     */
    int get() throws Exception;
}
