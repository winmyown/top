

package org.top.java.netty.source.util.internal.logging;

import org.top.java.netty.source.util.internal.ObjectUtil;

/**
 * Creates an {@link InternalLogger} or changes the default factory
 * implementation.  This factory allows you to choose what logging framework
 * Netty should use.  The default factory is {@link Slf4JLoggerFactory}.  If SLF4J
 * is not available, {@link Log4JLoggerFactory} is used.  If Log4J is not available,
 * {@link JdkLoggerFactory} is used.  You can change it to your preferred
 * logging framework before other Netty classes are loaded:
 * <pre>
 * {@link InternalLoggerFactory}.setDefaultFactory({@link Log4JLoggerFactory}.INSTANCE);
 * </pre>
 * Please note that the new default factory is effective only for the classes
 * which were loaded after the default factory is changed.  Therefore,
 * {@link #setDefaultFactory(InternalLoggerFactory)} should be called as early
 * as possible and shouldn't be called more than once.
 */

/**
 * 创建一个 {@link InternalLogger} 或更改默认的工厂实现。该工厂允许您选择 Netty 应该使用的日志框架。
 * 默认工厂是 {@link Slf4JLoggerFactory}。如果 SLF4J 不可用，则使用 {@link Log4JLoggerFactory}。
 * 如果 Log4J 不可用，则使用 {@link JdkLoggerFactory}。您可以在加载其他 Netty 类之前将其更改为您首选的日志框架：
 * <pre>
 * {@link InternalLoggerFactory}.setDefaultFactory({@link Log4JLoggerFactory}.INSTANCE);
 * </pre>
 * 请注意，新的默认工厂仅对在更改默认工厂后加载的类生效。因此，
 * {@link #setDefaultFactory(InternalLoggerFactory)} 应尽早调用，并且不应多次调用。
 */
public abstract class InternalLoggerFactory {

    private static volatile InternalLoggerFactory defaultFactory;

    @SuppressWarnings("UnusedCatchParameter")
    private static InternalLoggerFactory newDefaultFactory(String name) {
        InternalLoggerFactory f = useSlf4JLoggerFactory(name);
        if (f != null) {
            return f;
        }

        f = useLog4J2LoggerFactory(name);
        if (f != null) {
            return f;
        }

        f = useLog4JLoggerFactory(name);
        if (f != null) {
            return f;
        }

        return useJdkLoggerFactory(name);
    }

    private static InternalLoggerFactory useSlf4JLoggerFactory(String name) {
        try {
            InternalLoggerFactory f = Slf4JLoggerFactory.getInstanceWithNopCheck();
            f.newInstance(name).debug("Using SLF4J as the default logging framework");
            return f;
        } catch (LinkageError ignore) {
            return null;
        } catch (Exception ignore) {
            // We catch Exception and not ReflectiveOperationException as we still support java 6
            // 我们捕获Exception而不是ReflectiveOperationException，因为我们仍然支持Java 6
            return null;
        }
    }

    private static InternalLoggerFactory useLog4J2LoggerFactory(String name) {
        try {
            InternalLoggerFactory f = Log4J2LoggerFactory.INSTANCE;
            f.newInstance(name).debug("Using Log4J2 as the default logging framework");
            return f;
        } catch (LinkageError ignore) {
            return null;
        } catch (Exception ignore) {
            // We catch Exception and not ReflectiveOperationException as we still support java 6
            // 我们捕获Exception而不是ReflectiveOperationException，因为我们仍然支持Java 6
            return null;
        }
    }

    private static InternalLoggerFactory useLog4JLoggerFactory(String name) {
        try {
            InternalLoggerFactory f = Log4JLoggerFactory.INSTANCE;
            f.newInstance(name).debug("Using Log4J as the default logging framework");
            return f;
        } catch (LinkageError ignore) {
            return null;
        } catch (Exception ignore) {
            // We catch Exception and not ReflectiveOperationException as we still support java 6
            // 我们捕获Exception而不是ReflectiveOperationException，因为我们仍然支持Java 6
            return null;
        }
    }

    private static InternalLoggerFactory useJdkLoggerFactory(String name) {
        InternalLoggerFactory f = JdkLoggerFactory.INSTANCE;
        f.newInstance(name).debug("Using java.util.logging as the default logging framework");
        return f;
    }

    /**
     * Returns the default factory.  The initial default factory is
     * {@link JdkLoggerFactory}.
     */

    /**
     * 返回默认的工厂。初始的默认工厂是
     * {@link JdkLoggerFactory}。
     */
    public static InternalLoggerFactory getDefaultFactory() {
        if (defaultFactory == null) {
            defaultFactory = newDefaultFactory(InternalLoggerFactory.class.getName());
        }
        return defaultFactory;
    }

    /**
     * Changes the default factory.
     */

    /**
     * 更改默认工厂。
     */
    public static void setDefaultFactory(InternalLoggerFactory defaultFactory) {
        InternalLoggerFactory.defaultFactory = ObjectUtil.checkNotNull(defaultFactory, "defaultFactory");
    }

    /**
     * Creates a new logger instance with the name of the specified class.
     */

    /**
     * 使用指定类的名称创建一个新的日志记录器实例。
     */
    public static InternalLogger getInstance(Class<?> clazz) {
        return getInstance(clazz.getName());
    }

    /**
     * Creates a new logger instance with the specified name.
     */

    /**
     * 使用指定名称创建一个新的日志记录器实例。
     */
    public static InternalLogger getInstance(String name) {
        return getDefaultFactory().newInstance(name);
    }

    /**
     * Creates a new logger instance with the specified name.
     */

    /**
     * 使用指定名称创建一个新的日志记录器实例。
     */
    protected abstract InternalLogger newInstance(String name);

}
