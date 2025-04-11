
package org.top.java.netty.source.util.internal;

import java.lang.reflect.AccessibleObject;

public final class ReflectionUtil {

    private ReflectionUtil() { }

    /**
     * Try to call {@link AccessibleObject#setAccessible(boolean)} but will catch any {@link SecurityException} and
     * {@link java.lang.reflect.InaccessibleObjectException} and return it.
     * The caller must check if it returns {@code null} and if not handle the returned exception.
     */

    /**
     * 尝试调用 {@link AccessibleObject#setAccessible(boolean)}，但会捕获任何 {@link SecurityException} 和
     * {@link java.lang.reflect.InaccessibleObjectException} 并返回它。
     * 调用者必须检查返回是否为 {@code null}，如果不是，则处理返回的异常。
     */
    public static Throwable trySetAccessible(AccessibleObject object, boolean checkAccessible) {
        if (checkAccessible && !PlatformDependent0.isExplicitTryReflectionSetAccessible()) {
            return new UnsupportedOperationException("Reflective setAccessible(true) disabled");
        }
        try {
            object.setAccessible(true);
            return null;
        } catch (SecurityException e) {
            return e;
        } catch (RuntimeException e) {
            return handleInaccessibleObjectException(e);
        }
    }

    private static RuntimeException handleInaccessibleObjectException(RuntimeException e) {
        // JDK 9 can throw an inaccessible object exception here; since Netty compiles
        // JDK 9 在这里可能会抛出不可访问对象异常；由于 Netty 编译
        // against JDK 7 and this exception was only added in JDK 9, we have to weakly
        // 针对JDK 7，此异常仅在JDK 9中添加，我们必须弱化
        // check the type
        // 检查类型
        if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) {
            return e;
        }
        throw e;
    }
}
