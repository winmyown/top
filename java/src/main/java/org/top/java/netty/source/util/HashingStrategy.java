
package org.top.java.netty.source.util;

/**
 * Abstraction for hash code generation and equality comparison.
 */

/**
 * 用于哈希码生成和相等性比较的抽象。
 */
public interface HashingStrategy<T> {
    /**
     * Generate a hash code for {@code obj}.
     * <p>
     * This method must obey the same relationship that {@link Object#hashCode()} has with
     * {@link Object#equals(Object)}:
     * <ul>
     * <li>Calling this method multiple times with the same {@code obj} should return the same result</li>
     * <li>If {@link #equals(Object, Object)} with parameters {@code a} and {@code b} returns {@code true}
     * then the return value for this method for parameters {@code a} and {@code b} must return the same result</li>
     * <li>If {@link #equals(Object, Object)} with parameters {@code a} and {@code b} returns {@code false}
     * then the return value for this method for parameters {@code a} and {@code b} does <strong>not</strong> have to
     * return different results results. However this property is desirable.</li>
     * <li>if {@code obj} is {@code null} then this method return {@code 0}</li>
     * </ul>
     */
    /**
     * 为 {@code obj} 生成哈希码。
     * <p>
     * 此方法必须遵循与 {@link Object#hashCode()} 和 {@link Object#equals(Object)} 相同的关系：
     * <ul>
     * <li>多次使用相同的 {@code obj} 调用此方法应返回相同的结果</li>
     * <li>如果使用参数 {@code a} 和 {@code b} 调用 {@link #equals(Object, Object)} 返回 {@code true}，
     * 则使用参数 {@code a} 和 {@code b} 调用此方法必须返回相同的结果</li>
     * <li>如果使用参数 {@code a} 和 {@code b} 调用 {@link #equals(Object, Object)} 返回 {@code false}，
     * 则使用参数 {@code a} 和 {@code b} 调用此方法<strong>不</strong>必须返回不同的结果。然而，此属性是可取的。</li>
     * <li>如果 {@code obj} 为 {@code null}，则此方法返回 {@code 0}</li>
     * </ul>
     */
    int hashCode(T obj);

    /**
     * Returns {@code true} if the arguments are equal to each other and {@code false} otherwise.
     * This method has the following restrictions:
     * <ul>
     * <li><i>reflexive</i> - {@code equals(a, a)} should return true</li>
     * <li><i>symmetric</i> - {@code equals(a, b)} returns {@code true} if {@code equals(b, a)} returns
     * {@code true}</li>
     * <li><i>transitive</i> - if {@code equals(a, b)} returns {@code true} and {@code equals(a, c)} returns
     * {@code true} then {@code equals(b, c)} should also return {@code true}</li>
     * <li><i>consistent</i> - {@code equals(a, b)} should return the same result when called multiple times
     * assuming {@code a} and {@code b} remain unchanged relative to the comparison criteria</li>
     * <li>if {@code a} and {@code b} are both {@code null} then this method returns {@code true}</li>
     * <li>if {@code a} is {@code null} and {@code b} is non-{@code null}, or {@code a} is non-{@code null} and
     * {@code b} is {@code null} then this method returns {@code false}</li>
     * </ul>
     */

    /**
     * 如果参数彼此相等，则返回 {@code true}，否则返回 {@code false}。
     * 此方法具有以下限制：
     * <ul>
     * <li><i>自反性</i> - {@code equals(a, a)} 应返回 true</li>
     * <li><i>对称性</i> - {@code equals(a, b)} 返回 {@code true} 如果 {@code equals(b, a)} 返回
     * {@code true}</li>
     * <li><i>传递性</i> - 如果 {@code equals(a, b)} 返回 {@code true} 且 {@code equals(a, c)} 返回
     * {@code true}，则 {@code equals(b, c)} 也应返回 {@code true}</li>
     * <li><i>一致性</i> - {@code equals(a, b)} 应在多次调用时返回相同的结果，
     * 假设 {@code a} 和 {@code b} 相对于比较标准保持不变</li>
     * <li>如果 {@code a} 和 {@code b} 都为 {@code null}，则此方法返回 {@code true}</li>
     * <li>如果 {@code a} 为 {@code null} 且 {@code b} 为非 {@code null}，或 {@code a} 为非 {@code null} 且
     * {@code b} 为 {@code null}，则此方法返回 {@code false}</li>
     * </ul>
     */
    boolean equals(T a, T b);

    /**
     * A {@link HashingStrategy} which delegates to java's {@link Object#hashCode()}
     * and {@link Object#equals(Object)}.
     */

    /**
     * 一个{@link HashingStrategy}，它委托给java的{@link Object#hashCode()}
     * 和{@link Object#equals(Object)}。
     */
    @SuppressWarnings("rawtypes")
    HashingStrategy JAVA_HASHER = new HashingStrategy() {
        @Override
        public int hashCode(Object obj) {
            return obj != null ? obj.hashCode() : 0;
        }

        @Override
        public boolean equals(Object a, Object b) {
            return (a == b) || (a != null && a.equals(b));
        }
    };
}
