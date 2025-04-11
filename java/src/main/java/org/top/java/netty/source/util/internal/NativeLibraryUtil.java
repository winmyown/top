
package org.top.java.netty.source.util.internal;

/**
 * A Utility to Call the {@link System#load(String)} or {@link System#loadLibrary(String)}.
 * Because the {@link System#load(String)} and {@link System#loadLibrary(String)} are both
 * CallerSensitive, it will load the native library into its caller's {@link ClassLoader}.
 * In OSGi environment, we need this helper to delegate the calling to {@link System#load(String)}
 * and it should be as simple as possible. It will be injected into the native library's
 * ClassLoader when it is undefined. And therefore, when the defined new helper is invoked,
 * the native library would be loaded into the native library's ClassLoader, not the
 * caller's ClassLoader.
 */

/**
 * 一个用于调用 {@link System#load(String)} 或 {@link System#loadLibrary(String)} 的工具。
 * 由于 {@link System#load(String)} 和 {@link System#loadLibrary(String)} 都是
 * CallerSensitive 的，它们会将本地库加载到调用者的 {@link ClassLoader} 中。
 * 在 OSGi 环境中，我们需要这个帮助类来委托调用 {@link System#load(String)}，
 * 并且它应该尽可能简单。当本地库的 ClassLoader 未定义时，它将被注入到本地库的
 * ClassLoader 中。因此，当定义的新帮助类被调用时，本地库将被加载到本地库的
 * ClassLoader 中，而不是调用者的 ClassLoader。
 */
final class NativeLibraryUtil {
    /**
     * Delegate the calling to {@link System#load(String)} or {@link System#loadLibrary(String)}.
     * @param libName - The native library path or name
     * @param absolute - Whether the native library will be loaded by path or by name
     */
    /**
     * 将调用委托给 {@link System#load(String)} 或 {@link System#loadLibrary(String)}。
     * @param libName - 本地库路径或名称
     * @param absolute - 本地库是通过路径加载还是通过名称加载
     */
    public static void loadLibrary(String libName, boolean absolute) {
        if (absolute) {
            System.load(libName);
        } else {
            System.loadLibrary(libName);
        }
    }

    private NativeLibraryUtil() {
        // Utility
        // 工具类
    }
}
