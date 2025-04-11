
package org.top.java.netty.source.util.internal;

/**
 * Utility which ensures that classes are loaded by the {@link ClassLoader}.
 */

/**
 * 工具类，确保类由 {@link ClassLoader} 加载。
 */
public final class ClassInitializerUtil {

    private ClassInitializerUtil() { }

    /**
     * Preload the given classes and so ensure the {@link ClassLoader} has these loaded after this method call.
     *
     * @param loadingClass      the {@link Class} that wants to load the classes.
     * @param classes           the classes to load.
     */

    /**
     * 预加载给定的类，确保在调用此方法后 {@link ClassLoader} 已经加载了这些类。
     *
     * @param loadingClass      需要加载这些类的 {@link Class}。
     * @param classes           要加载的类。
     */
    public static void tryLoadClasses(Class<?> loadingClass, Class<?>... classes) {
        ClassLoader loader = PlatformDependent.getClassLoader(loadingClass);
        for (Class<?> clazz: classes) {
            tryLoadClass(loader, clazz.getName());
        }
    }

    private static void tryLoadClass(ClassLoader classLoader, String className) {
        try {
            // Load the class and also ensure we init it which means its linked etc.
            // 加载类并确保我们初始化它，这意味着它已链接等。
            Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException ignore) {
            // Ignore
            // 忽略
        } catch (SecurityException ignore) {
            // Ignore
            // 忽略
        }
    }
}
