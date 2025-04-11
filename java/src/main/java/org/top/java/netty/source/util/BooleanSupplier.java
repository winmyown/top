
package org.top.java.netty.source.util;

/**
 * Represents a supplier of {@code boolean}-valued results.
 */

/**
 * 表示一个提供{@code boolean}类型结果的供应商。
 */
public interface BooleanSupplier {
    /**
     * Gets a boolean value.
     * @return a boolean value.
     * @throws Exception If an exception occurs.
     */
    /**
     * 获取一个布尔值。
     * @return 一个布尔值。
     * @throws Exception 如果发生异常。
     */
    boolean get() throws Exception;

    /**
     * A supplier which always returns {@code false} and never throws.
     */

    /**
     * 一个始终返回 {@code false} 且永不抛出异常的供应商。
     */
    BooleanSupplier FALSE_SUPPLIER = new BooleanSupplier() {
        @Override
        public boolean get() {
            return false;
        }
    };

    /**
     * A supplier which always returns {@code true} and never throws.
     */

    /**
     * 一个总是返回 {@code true} 且永不抛出异常的供应商。
     */
    BooleanSupplier TRUE_SUPPLIER = new BooleanSupplier() {
        @Override
        public boolean get() {
            return true;
        }
    };
}
