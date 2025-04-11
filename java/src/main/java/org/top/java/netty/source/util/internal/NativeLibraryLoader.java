
package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.CharsetUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Helper class to load JNI resources.
 *
 */

/**
 * 帮助类用于加载JNI资源。
 *
 */
public final class NativeLibraryLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NativeLibraryLoader.class);

    private static final String NATIVE_RESOURCE_HOME = "META-INF/native/";
    private static final File WORKDIR;
    private static final boolean DELETE_NATIVE_LIB_AFTER_LOADING;
    private static final boolean TRY_TO_PATCH_SHADED_ID;
    private static final boolean DETECT_NATIVE_LIBRARY_DUPLICATES;

    // Just use a-Z and numbers as valid ID bytes.

    // 仅使用 a-Z 和数字作为有效的 ID 字节。
    private static final byte[] UNIQUE_ID_BYTES =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CharsetUtil.US_ASCII);

    static {
        String workdir = SystemPropertyUtil.get("io.netty.native.workdir");
        if (workdir != null) {
            File f = new File(workdir);
            f.mkdirs();

            try {
                f = f.getAbsoluteFile();
            } catch (Exception ignored) {
                // Good to have an absolute path, but it's OK.
                // 最好使用绝对路径，但也可以接受。
            }

            WORKDIR = f;
            logger.debug("-Dio.netty.native.workdir: " + WORKDIR);
        } else {
            WORKDIR = PlatformDependent.tmpdir();
            logger.debug("-Dio.netty.native.workdir: " + WORKDIR + " (io.netty.tmpdir)");
        }

        DELETE_NATIVE_LIB_AFTER_LOADING = SystemPropertyUtil.getBoolean(
                "io.netty.native.deleteLibAfterLoading", true);
        logger.debug("-Dio.netty.native.deleteLibAfterLoading: {}", DELETE_NATIVE_LIB_AFTER_LOADING);

        TRY_TO_PATCH_SHADED_ID = SystemPropertyUtil.getBoolean(
                "io.netty.native.tryPatchShadedId", true);
        logger.debug("-Dio.netty.native.tryPatchShadedId: {}", TRY_TO_PATCH_SHADED_ID);

        DETECT_NATIVE_LIBRARY_DUPLICATES = SystemPropertyUtil.getBoolean(
                "io.netty.native.detectNativeLibraryDuplicates", true);
        logger.debug("-Dio.netty.native.detectNativeLibraryDuplicates: {}", DETECT_NATIVE_LIBRARY_DUPLICATES);
    }

    /**
     * Loads the first available library in the collection with the specified
     * {@link ClassLoader}.
     *
     * @throws IllegalArgumentException
     *         if none of the given libraries load successfully.
     */

    /**
     * 使用指定的 {@link ClassLoader} 加载集合中第一个可用的库。
     *
     * @throws IllegalArgumentException
     *         如果所有给定的库都无法成功加载。
     */
    public static void loadFirstAvailable(ClassLoader loader, String... names) {
        List<Throwable> suppressed = new ArrayList<Throwable>();
        for (String name : names) {
            try {
                load(name, loader);
                logger.debug("Loaded library with name '{}'", name);
                return;
            } catch (Throwable t) {
                suppressed.add(t);
            }
        }

        IllegalArgumentException iae =
                new IllegalArgumentException("Failed to load any of the given libraries: " + Arrays.toString(names));
        ThrowableUtil.addSuppressedAndClear(iae, suppressed);
        throw iae;
    }

    /**
     * Calculates the mangled shading prefix added to this class's full name.
     *
     * <p>This method mangles the package name as follows, so we can unmangle it back later:
     * <ul>
     *   <li>{@code _} to {@code _1}</li>
     *   <li>{@code .} to {@code _}</li>
     * </ul>
     *
     * <p>Note that we don't mangle non-ASCII characters here because it's extremely unlikely to have
     * a non-ASCII character in a package name. For more information, see:
     * <ul>
     *   <li><a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/design.html">JNI
     *       specification</a></li>
     *   <li>{@code parsePackagePrefix()} in {@code netty_jni_util.c}.</li>
     * </ul>
     *
     * @throws UnsatisfiedLinkError if the shader used something other than a prefix
     */

    /**
     * 计算添加到该类全名中的混淆前缀。
     *
     * <p>此方法对包名进行如下混淆，以便稍后可以将其还原：
     * <ul>
     *   <li>{@code _} 转换为 {@code _1}</li>
     *   <li>{@code .} 转换为 {@code _}</li>
     * </ul>
     *
     * <p>注意，我们在这里不对非ASCII字符进行混淆，因为包名中出现非ASCII字符的可能性极低。更多信息请参考：
     * <ul>
     *   <li><a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/design.html">JNI
     *       规范</a></li>
     *   <li>{@code netty_jni_util.c} 中的 {@code parsePackagePrefix()}。</li>
     * </ul>
     *
     * @throws UnsatisfiedLinkError 如果混淆器使用了非前缀的内容
     */
    private static String calculateMangledPackagePrefix() {
        String maybeShaded = NativeLibraryLoader.class.getName();
        // Use ! instead of . to avoid shading utilities from modifying the string
        // 使用 ! 代替 . 以避免阴影工具修改字符串
        String expected = "io!netty!util!internal!NativeLibraryLoader".replace('!', '.');
        if (!maybeShaded.endsWith(expected)) {
            throw new UnsatisfiedLinkError(String.format(
                    "Could not find prefix added to %s to get %s. When shading, only adding a "
                    + "package prefix is supported", expected, maybeShaded));
        }
        return maybeShaded.substring(0, maybeShaded.length() - expected.length())
                          .replace("_", "_1")
                          .replace('.', '_');
    }

    /**
     * Load the given library with the specified {@link ClassLoader}
     */

    /**
     * 使用指定的 {@link ClassLoader} 加载给定的库
     */
    public static void load(String originalName, ClassLoader loader) {
        String mangledPackagePrefix = calculateMangledPackagePrefix();
        String name = mangledPackagePrefix + originalName;
        List<Throwable> suppressed = new ArrayList<Throwable>();
        try {
            // first try to load from java.library.path
            // 首先尝试从 java.library.path 加载
            loadLibrary(loader, name, false);
            return;
        } catch (Throwable ex) {
            suppressed.add(ex);
        }

        String libname = System.mapLibraryName(name);
        String path = NATIVE_RESOURCE_HOME + libname;

        InputStream in = null;
        OutputStream out = null;
        File tmpFile = null;
        URL url = getResource(path, loader);
        try {
            if (url == null) {
                if (PlatformDependent.isOsx()) {
                    String fileName = path.endsWith(".jnilib") ? NATIVE_RESOURCE_HOME + "lib" + name + ".dynlib" :
                            NATIVE_RESOURCE_HOME + "lib" + name + ".jnilib";
                    url = getResource(fileName, loader);
                    if (url == null) {
                        FileNotFoundException fnf = new FileNotFoundException(fileName);
                        ThrowableUtil.addSuppressedAndClear(fnf, suppressed);
                        throw fnf;
                    }
                } else {
                    FileNotFoundException fnf = new FileNotFoundException(path);
                    ThrowableUtil.addSuppressedAndClear(fnf, suppressed);
                    throw fnf;
                }
            }

            int index = libname.lastIndexOf('.');
            String prefix = libname.substring(0, index);
            String suffix = libname.substring(index);

            tmpFile = PlatformDependent.createTempFile(prefix, suffix, WORKDIR);
            in = url.openStream();
            out = new FileOutputStream(tmpFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            if (shouldShadedLibraryIdBePatched(mangledPackagePrefix)) {
                // Let's try to patch the id and re-sign it. This is a best-effort and might fail if a
                // 让我们尝试修补 id 并重新签名。这是尽力而为的，如果失败可能会失败。
                // SecurityManager is setup or the right executables are not installed :/
                // SecurityManager 未设置或未安装正确的可执行文件 :/
                tryPatchShadedLibraryIdAndSign(tmpFile, originalName);
            }

            // Close the output stream before loading the unpacked library,

            // 在加载解压库之前关闭输出流
            // because otherwise Windows will refuse to load it when it's in use by other process.
            // 否则，当它被其他进程使用时，Windows 将拒绝加载它。
            closeQuietly(out);
            out = null;

            loadLibrary(loader, tmpFile.getPath(), true);
        } catch (UnsatisfiedLinkError e) {
            try {
                if (tmpFile != null && tmpFile.isFile() && tmpFile.canRead() &&
                    !NoexecVolumeDetector.canExecuteExecutable(tmpFile)) {
                    // Pass "io.netty.native.workdir" as an argument to allow shading tools to see
                    // 传递 "io.netty.native.workdir" 作为参数，以允许着色工具查看
                    // the string. Since this is printed out to users to tell them what to do next,
                    // 该字符串。由于这是打印出来告诉用户下一步该做什么的，
                    // we want the value to be correct even when shading.
                    // 我们希望即使在进行着色时，值也能保持正确。
                    logger.info("{} exists but cannot be executed even when execute permissions set; " +
                                "check volume for \"noexec\" flag; use -D{}=[path] " +
                                "to set native working directory separately.",
                                tmpFile.getPath(), "io.netty.native.workdir");
                }
            } catch (Throwable t) {
                suppressed.add(t);
                logger.debug("Error checking if {} is on a file store mounted with noexec", tmpFile, t);
            }
            // Re-throw to fail the load
            // 重新抛出以导致加载失败
            ThrowableUtil.addSuppressedAndClear(e, suppressed);
            throw e;
        } catch (Exception e) {
            UnsatisfiedLinkError ule = new UnsatisfiedLinkError("could not load a native library: " + name);
            ule.initCause(e);
            ThrowableUtil.addSuppressedAndClear(ule, suppressed);
            throw ule;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            // After we load the library it is safe to delete the file.
            // 加载库后，可以安全地删除该文件。
            // We delete the file immediately to free up resources as soon as possible,
            // 我们立即删除文件以尽快释放资源，
            // and if this fails fallback to deleting on JVM exit.
            // 如果失败，则回退到在 JVM 退出时删除。
            if (tmpFile != null && (!DELETE_NATIVE_LIB_AFTER_LOADING || !tmpFile.delete())) {
                tmpFile.deleteOnExit();
            }
        }
    }

    private static URL getResource(String path, ClassLoader loader) {
        final Enumeration<URL> urls;
        try {
            if (loader == null) {
                urls = ClassLoader.getSystemResources(path);
            } else {
                urls = loader.getResources(path);
            }
        } catch (IOException iox) {
            throw new RuntimeException("An error occurred while getting the resources for " + path, iox);
        }

        List<URL> urlsList = Collections.list(urls);
        int size = urlsList.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return urlsList.get(0);
            default:
                if (DETECT_NATIVE_LIBRARY_DUPLICATES) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        // We found more than 1 resource with the same name. Let's check if the content of the file is

// 我们发现存在多个同名资源。让我们检查文件内容是否一致。

                        // the same as in this case it will not have any bad effect.
                        // 在这种情况下，它不会有任何不良影响。
                        URL url = urlsList.get(0);
                        byte[] digest = digest(md, url);
                        boolean allSame = true;
                        if (digest != null) {
                            for (int i = 1; i < size; i++) {
                                byte[] digest2 = digest(md, urlsList.get(i));
                                if (digest2 == null || !Arrays.equals(digest, digest2)) {
                                    allSame = false;
                                    break;
                                }
                            }
                        } else {
                            allSame = false;
                        }
                        if (allSame) {
                            return url;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        logger.debug("Don't support SHA-256, can't check if resources have same content.", e);
                    }

                    throw new IllegalStateException(
                            "Multiple resources found for '" + path + "' with different content: " + urlsList);
                } else {
                    logger.warn("Multiple resources found for '" + path + "' with different content: " +
                            urlsList + ". Please fix your dependency graph.");
                    return urlsList.get(0);
                }
        }
    }

    private static byte[] digest(MessageDigest digest, URL url) {
        InputStream in = null;
        try {
            in = url.openStream();
            byte[] bytes = new byte[8192];
            int i;
            while ((i = in.read(bytes)) != -1) {
                digest.update(bytes, 0, i);
            }
            return digest.digest();
        } catch (IOException e) {
            logger.debug("Can't read resource.", e);
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    static void tryPatchShadedLibraryIdAndSign(File libraryFile, String originalName) {
        String newId = new String(generateUniqueId(originalName.length()), CharsetUtil.UTF_8);
        if (!tryExec("install_name_tool -id " + newId + " " + libraryFile.getAbsolutePath())) {
            return;
        }

        tryExec("codesign -s - " + libraryFile.getAbsolutePath());
    }

    private static boolean tryExec(String cmd) {
        try {
            int exitValue = Runtime.getRuntime().exec(cmd).waitFor();
            if (exitValue != 0) {
                logger.debug("Execution of '{}' failed: {}", cmd, exitValue);
                return false;
            }
            logger.debug("Execution of '{}' succeed: {}", cmd, exitValue);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.info("Execution of '{}' failed.", cmd, e);
        } catch (SecurityException e) {
            logger.error("Execution of '{}' failed.", cmd, e);
        }
        return false;
    }

    private static boolean shouldShadedLibraryIdBePatched(String packagePrefix) {
        return TRY_TO_PATCH_SHADED_ID && PlatformDependent.isOsx() && !packagePrefix.isEmpty();
    }

    private static byte[] generateUniqueId(int length) {
        byte[] idBytes = new byte[length];
        for (int i = 0; i < idBytes.length; i++) {
            // We should only use bytes as replacement that are in our UNIQUE_ID_BYTES array.
            // 我们应该只使用 UNIQUE_ID_BYTES 数组中的字节作为替换。
            idBytes[i] = UNIQUE_ID_BYTES[PlatformDependent.threadLocalRandom()
                    .nextInt(UNIQUE_ID_BYTES.length)];
        }
        return idBytes;
    }

    /**
     * Loading the native library into the specified {@link ClassLoader}.
     * @param loader - The {@link ClassLoader} where the native library will be loaded into
     * @param name - The native library path or name
     * @param absolute - Whether the native library will be loaded by path or by name
     */

    /**
     * 将本地库加载到指定的 {@link ClassLoader} 中。
     * @param loader - 本地库将被加载到的 {@link ClassLoader}
     * @param name - 本地库的路径或名称
     * @param absolute - 本地库是通过路径还是名称加载
     */
    private static void loadLibrary(final ClassLoader loader, final String name, final boolean absolute) {
        Throwable suppressed = null;
        try {
            try {
                // Make sure the helper belongs to the target ClassLoader.
                // 确保助手属于目标 ClassLoader。
                final Class<?> newHelper = tryToLoadClass(loader, NativeLibraryUtil.class);
                loadLibraryByHelper(newHelper, name, absolute);
                logger.debug("Successfully loaded the library {}", name);
                return;
            } catch (UnsatisfiedLinkError e) { // Should by pass the UnsatisfiedLinkError here!
                suppressed = e;
            } catch (Exception e) {
                suppressed = e;
            }
            NativeLibraryUtil.loadLibrary(name, absolute);  // Fallback to local helper class.
            logger.debug("Successfully loaded the library {}", name);
        } catch (NoSuchMethodError nsme) {
            if (suppressed != null) {
                ThrowableUtil.addSuppressed(nsme, suppressed);
            }
            rethrowWithMoreDetailsIfPossible(name, nsme);
        } catch (UnsatisfiedLinkError ule) {
            if (suppressed != null) {
                ThrowableUtil.addSuppressed(ule, suppressed);
            }
            throw ule;
        }
    }

    @SuppressJava6Requirement(reason = "Guarded by version check")
    private static void rethrowWithMoreDetailsIfPossible(String name, NoSuchMethodError error) {
        if (PlatformDependent.javaVersion() >= 7) {
            throw new LinkageError(
                    "Possible multiple incompatible native libraries on the classpath for '" + name + "'?", error);
        }
        throw error;
    }

    private static void loadLibraryByHelper(final Class<?> helper, final String name, final boolean absolute)
            throws UnsatisfiedLinkError {
        Object ret = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    // Invoke the helper to load the native library, if succeed, then the native
                    // 调用助手加载本地库，如果成功，则本地
                    // library belong to the specified ClassLoader.
                    // 库属于指定的 ClassLoader。
                    Method method = helper.getMethod("loadLibrary", String.class, boolean.class);
                    method.setAccessible(true);
                    return method.invoke(null, name, absolute);
                } catch (Exception e) {
                    return e;
                }
            }
        });
        if (ret instanceof Throwable) {
            Throwable t = (Throwable) ret;
            assert !(t instanceof UnsatisfiedLinkError) : t + " should be a wrapper throwable";
            Throwable cause = t.getCause();
            if (cause instanceof UnsatisfiedLinkError) {
                throw (UnsatisfiedLinkError) cause;
            }
            UnsatisfiedLinkError ule = new UnsatisfiedLinkError(t.getMessage());
            ule.initCause(t);
            throw ule;
        }
    }

    /**
     * Try to load the helper {@link Class} into specified {@link ClassLoader}.
     * @param loader - The {@link ClassLoader} where to load the helper {@link Class}
     * @param helper - The helper {@link Class}
     * @return A new helper Class defined in the specified ClassLoader.
     * @throws ClassNotFoundException Helper class not found or loading failed
     */

    /**
     * 尝试将辅助 {@link Class} 加载到指定的 {@link ClassLoader} 中。
     * @param loader - 要加载辅助 {@link Class} 的 {@link ClassLoader}
     * @param helper - 辅助 {@link Class}
     * @return 在指定 ClassLoader 中定义的新辅助 Class。
     * @throws ClassNotFoundException 辅助类未找到或加载失败
     */
    private static Class<?> tryToLoadClass(final ClassLoader loader, final Class<?> helper)
            throws ClassNotFoundException {
        try {
            return Class.forName(helper.getName(), false, loader);
        } catch (ClassNotFoundException e1) {
            if (loader == null) {
                // cannot defineClass inside bootstrap class loader
                // 无法在引导类加载器中定义类
                throw e1;
            }
            try {
                // The helper class is NOT found in target ClassLoader, we have to define the helper class.
                // 在目标 ClassLoader 中未找到 helper 类，我们需要定义 helper 类。
                final byte[] classBinary = classToByteArray(helper);
                return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                    @Override
                    public Class<?> run() {
                        try {
                            // Define the helper class in the target ClassLoader,
                            // 在目标ClassLoader中定义辅助类
                            //  then we can call the helper to load the native library.
                            // 然后我们可以调用助手来加载本地库。
                            Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class,
                                    byte[].class, int.class, int.class);
                            defineClass.setAccessible(true);
                            return (Class<?>) defineClass.invoke(loader, helper.getName(), classBinary, 0,
                                    classBinary.length);
                        } catch (Exception e) {
                            throw new IllegalStateException("Define class failed!", e);
                        }
                    }
                });
            } catch (ClassNotFoundException e2) {
                ThrowableUtil.addSuppressed(e2, e1);
                throw e2;
            } catch (RuntimeException e2) {
                ThrowableUtil.addSuppressed(e2, e1);
                throw e2;
            } catch (Error e2) {
                ThrowableUtil.addSuppressed(e2, e1);
                throw e2;
            }
        }
    }

    /**
     * Load the helper {@link Class} as a byte array, to be redefined in specified {@link ClassLoader}.
     * @param clazz - The helper {@link Class} provided by this bundle
     * @return The binary content of helper {@link Class}.
     * @throws ClassNotFoundException Helper class not found or loading failed
     */

    /**
     * 将帮助类 {@link Class} 加载为字节数组，以便在指定的 {@link ClassLoader} 中重新定义。
     * @param clazz - 由该 bundle 提供的帮助类 {@link Class}
     * @return 帮助类 {@link Class} 的二进制内容。
     * @throws ClassNotFoundException 帮助类未找到或加载失败
     */
    private static byte[] classToByteArray(Class<?> clazz) throws ClassNotFoundException {
        String fileName = clazz.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            fileName = fileName.substring(lastDot + 1);
        }
        URL classUrl = clazz.getResource(fileName + ".class");
        if (classUrl == null) {
            throw new ClassNotFoundException(clazz.getName());
        }
        byte[] buf = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        InputStream in = null;
        try {
            in = classUrl.openStream();
            for (int r; (r = in.read(buf)) != -1;) {
                out.write(buf, 0, r);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ClassNotFoundException(clazz.getName(), ex);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
                // ignore
                // 忽略
            }
        }
    }

    private NativeLibraryLoader() {
        // Utility
        // 工具类
    }

    private static final class NoexecVolumeDetector {

        @SuppressJava6Requirement(reason = "Usage guarded by java version check")
        private static boolean canExecuteExecutable(File file) throws IOException {
            if (PlatformDependent.javaVersion() < 7) {
                // Pre-JDK7, the Java API did not directly support POSIX permissions; instead of implementing a custom
                // 在JDK7之前，Java API并不直接支持POSIX权限；而是通过实现自定义的
                // work-around, assume true, which disables the check.
                // 变通方法，假设为真，从而禁用检查。
                return true;
            }

            // If we can already execute, there is nothing to do.

            // 如果已经可以执行，则无需做任何操作。
            if (file.canExecute()) {
                return true;
            }

            // On volumes, with noexec set, even files with the executable POSIX permissions will fail to execute.

            // 在设置了 noexec 的卷上，即使文件具有可执行的 POSIX 权限，执行也会失败。
            // The File#canExecute() method honors this behavior, probaby via parsing the noexec flag when initializing
            // File#canExecute() 方法遵循此行为，可能是在初始化时通过解析 noexec 标志来实现的
            // the UnixFileStore, though the flag is not exposed via a public API.  To find out if library is being
            // UnixFileStore，尽管该标志未通过公共API暴露。要查明库是否正在
            // loaded off a volume with noexec, confirm or add executalbe permissions, then check File#canExecute().
            // 从带有noexec选项的卷加载，确认或添加可执行权限，然后检查File#canExecute()。

            // Note: We use FQCN to not break when netty is used in java6

            // 注意：我们使用FQCN以避免在Java6中使用netty时出现兼容性问题
            Set<java.nio.file.attribute.PosixFilePermission> existingFilePermissions =
                    java.nio.file.Files.getPosixFilePermissions(file.toPath());
            Set<java.nio.file.attribute.PosixFilePermission> executePermissions =
                    EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
            if (existingFilePermissions.containsAll(executePermissions)) {
                return false;
            }

            Set<java.nio.file.attribute.PosixFilePermission> newPermissions = EnumSet.copyOf(existingFilePermissions);
            newPermissions.addAll(executePermissions);
            java.nio.file.Files.setPosixFilePermissions(file.toPath(), newPermissions);
            return file.canExecute();
        }

        private NoexecVolumeDetector() {
            // Utility
            // 工具类
        }
    }
}
