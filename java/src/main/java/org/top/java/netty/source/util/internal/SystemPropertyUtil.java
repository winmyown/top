
package org.top.java.netty.source.util.internal;

import static io.netty.util.internal.ObjectUtil.checkNonEmpty;

import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */

/**
 * 一个实用方法集合，用于检索和解析Java系统属性的值。
 */
public final class SystemPropertyUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SystemPropertyUtil.class);

    /**
     * Returns {@code true} if and only if the system property with the specified {@code key}
     * exists.
     */

    /**
     * 当且仅当具有指定 {@code key} 的系统属性存在时，返回 {@code true}。
     */
    public static boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to {@code null} if the property access fails.
     *
     * @return the property value or {@code null}
     */

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败则回退为 {@code null}。
     *
     * @return 属性值或 {@code null}
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。
     *         如果没有该属性或不允许访问指定属性，则返回 {@code def}。
     */
    public static String get(final String key, String def) {
        checkNonEmpty(key, "key");

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(key);
                    }
                });
            }
        } catch (SecurityException e) {
            logger.warn("Unable to retrieve a system property '{}'; default values will be used.", key, e);
        }

        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。
     *         如果没有该属性或不允许访问指定属性，则返回 {@code def}。
     */
    public static boolean getBoolean(String key, boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return def;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        logger.warn(
                "Unable to parse the boolean system property '{}':{} - using the default value: {}",
                key, value, def
        );

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。
     *         如果没有该属性或不允许访问指定属性，则返回 {@code def}。
     */
    public static int getInt(String key, int def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim();
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            // Ignore
            // 忽略
        }

        logger.warn(
                "Unable to parse the integer system property '{}':{} - using the default value: {}",
                key, value, def
        );

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */

    /**
     * 返回具有指定 {@code key} 的 Java 系统属性的值，如果属性访问失败，则回退到指定的默认值。
     *
     * @return 属性值。
     *         如果没有该属性或不允许访问指定属性，则返回 {@code def}。
     */
    public static long getLong(String key, long def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim();
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            // Ignore
            // 忽略
        }

        logger.warn(
                "Unable to parse the long integer system property '{}':{} - using the default value: {}",
                key, value, def
        );

        return def;
    }

    private SystemPropertyUtil() {
        // Unused
        // 未使用
    }
}
