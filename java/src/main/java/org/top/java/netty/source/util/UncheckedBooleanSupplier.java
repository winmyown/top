
package org.top.java.netty.source.util;

/**
 * Represents a supplier of {@code boolean}-valued results which doesn't throw any checked exceptions.
 */

/**
 * 表示一个不抛出任何受检异常的 {@code boolean} 值结果供应商。
 */
public interface UncheckedBooleanSupplier extends BooleanSupplier {
    /**
     * Gets a boolean value.
     * @return a boolean value.
     */
    /**
     * 获取一个布尔值。
     * @return 一个布尔值。
     */
    @Override
    boolean get();

    /**
     * A supplier which always returns {@code false} and never throws.
     */

    /**
     * 一个始终返回 {@code false} 且永不抛出异常的供应商。
     */
    UncheckedBooleanSupplier FALSE_SUPPLIER = new UncheckedBooleanSupplier() {
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
    UncheckedBooleanSupplier TRUE_SUPPLIER = new UncheckedBooleanSupplier() {
        @Override
        public boolean get() {
            return true;
        }
    };
}
