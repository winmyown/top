
package org.top.java.netty.source.util;

/**
 * Key which can be used to access {@link Attribute} out of the {@link AttributeMap}. Be aware that it is not be
 * possible to have multiple keys with the same name.
 *
 * @param <T>   the type of the {@link Attribute} which can be accessed via this {@link AttributeKey}.
 */

/**
 * 用于从{@link AttributeMap}中访问{@link Attribute}的键。请注意，不可能存在多个具有相同名称的键。
 *
 * @param <T>   可以通过此{@link AttributeKey}访问的{@link Attribute}的类型。
 */
@SuppressWarnings("UnusedDeclaration") // 'T' is used only at compile time
public final class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {

    private static final ConstantPool<AttributeKey<Object>> pool = new ConstantPool<AttributeKey<Object>>() {
        @Override
        protected AttributeKey<Object> newConstant(int id, String name) {
            return new AttributeKey<Object>(id, name);
        }
    };

    /**
     * Returns the singleton instance of the {@link AttributeKey} which has the specified {@code name}.
     */

    /**
     * 返回具有指定 {@code name} 的 {@link AttributeKey} 的单例实例。
     */
    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> valueOf(String name) {
        return (AttributeKey<T>) pool.valueOf(name);
    }

    /**
     * Returns {@code true} if a {@link AttributeKey} exists for the given {@code name}.
     */

    /**
     * 如果给定的 {@code name} 存在 {@link AttributeKey}，则返回 {@code true}。
     */
    public static boolean exists(String name) {
        return pool.exists(name);
    }

    /**
     * Creates a new {@link AttributeKey} for the given {@code name} or fail with an
     * {@link IllegalArgumentException} if a {@link AttributeKey} for the given {@code name} exists.
     */

    /**
     * 为给定的 {@code name} 创建一个新的 {@link AttributeKey}，如果给定 {@code name} 的 {@link AttributeKey} 已存在，则抛出 {@link IllegalArgumentException}。
     */
    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> newInstance(String name) {
        return (AttributeKey<T>) pool.newInstance(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (AttributeKey<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    private AttributeKey(int id, String name) {
        super(id, name);
    }
}
