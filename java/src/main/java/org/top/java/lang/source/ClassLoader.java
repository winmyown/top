package org.top.java.lang.source;

import sun.reflect.Reflection;
import sun.security.util.SecurityConstants;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/15 上午8:17
 */
public abstract class ClassLoader {
    private static ClassLoader scl;
    // Returns the class's class loader, or null if none.
    static java.lang.ClassLoader getClassLoader(Class<?> caller) {

        return java.lang.ClassLoader.getSystemClassLoader();
    }

    static void checkClassLoaderPermission(ClassLoader cl, Class<?> caller) {
        //仅用于 排除thread 的报错
    }

    public static ClassLoader getSystemClassLoader() {
        if (scl == null) {
            return null;
        }

        return scl;
    }
}
