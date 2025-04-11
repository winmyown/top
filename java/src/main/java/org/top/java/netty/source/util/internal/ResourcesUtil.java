
package org.top.java.netty.source.util.internal;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * A utility class that provides various common operations and constants
 * related to loading resources
 */

/**
 * 一个实用类，提供与加载资源相关的各种常见操作和常量
 */
public final class ResourcesUtil {

    /**
     * Returns a {@link File} named {@code fileName} associated with {@link Class} {@code resourceClass} .
     *
     * @param resourceClass The associated class
     * @param fileName The file name
     * @return The file named {@code fileName} associated with {@link Class} {@code resourceClass} .
     */

    /**
     * 返回与 {@link Class} {@code resourceClass} 关联的名为 {@code fileName} 的 {@link File}。
     *
     * @param resourceClass 关联的类
     * @param fileName 文件名
     * @return 与 {@link Class} {@code resourceClass} 关联的名为 {@code fileName} 的文件。
     */
    public static File getFile(Class resourceClass, String fileName) {
        try {
            return new File(URLDecoder.decode(resourceClass.getResource(fileName).getFile(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return new File(resourceClass.getResource(fileName).getFile());
        }
    }

    private ResourcesUtil() { }
}
