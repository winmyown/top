
package org.top.java.netty.source.util.internal.logging;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import java.security.AccessController;
import java.security.PrivilegedAction;

import static org.top.java.netty.source.util.internal.logging.AbstractInternalLogger.EXCEPTION_MESSAGE;

class Log4J2Logger extends ExtendedLoggerWrapper implements InternalLogger {

    private static final long serialVersionUID = 5485418394879791397L;
    private static final boolean VARARGS_ONLY;

    static {
        // Older Log4J2 versions have only log methods that takes the format + varargs. So we should not use
        // 较旧的 Log4J2 版本只有接受格式 + 可变参数的方法。因此我们不应使用
        // Log4J2 if the version is too old.
        // 如果Log4J2版本过旧。
        // See https://github.com/netty/netty/issues/8217
        // 参见 https://github.com/netty/netty/issues/8217
        VARARGS_ONLY = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    Logger.class.getMethod("debug", String.class, Object.class);
                    return false;
                } catch (NoSuchMethodException ignore) {
                    // Log4J2 version too old.
                    // Log4J2 版本过旧。
                    return true;
                } catch (SecurityException ignore) {
                    // We could not detect the version so we will use Log4J2 if its on the classpath.
                    // 我们无法检测到版本，因此如果Log4J2在类路径上，我们将使用它。
                    return false;
                }
            }
        });
    }

    Log4J2Logger(Logger logger) {
        super((ExtendedLogger) logger, logger.getName(), logger.getMessageFactory());
        if (VARARGS_ONLY) {
            throw new UnsupportedOperationException("Log4J2 version mismatch");
        }
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public void trace(Throwable t) {
        log(Level.TRACE, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void debug(Throwable t) {
        log(Level.DEBUG, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void info(Throwable t) {
        log(Level.INFO, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void warn(Throwable t) {
        log(Level.WARN, EXCEPTION_MESSAGE, t);
    }

    @Override
    public void error(Throwable t) {
        log(Level.ERROR, EXCEPTION_MESSAGE, t);
    }

    @Override
    public boolean isEnabled(InternalLogLevel level) {
        return isEnabled(toLevel(level));
    }

    @Override
    public void log(InternalLogLevel level, String msg) {
        log(toLevel(level), msg);
    }

    @Override
    public void log(InternalLogLevel level, String format, Object arg) {
        log(toLevel(level), format, arg);
    }

    @Override
    public void log(InternalLogLevel level, String format, Object argA, Object argB) {
        log(toLevel(level), format, argA, argB);
    }

    @Override
    public void log(InternalLogLevel level, String format, Object... arguments) {
        log(toLevel(level), format, arguments);
    }

    @Override
    public void log(InternalLogLevel level, String msg, Throwable t) {
        log(toLevel(level), msg, t);
    }

    @Override
    public void log(InternalLogLevel level, Throwable t) {
        log(toLevel(level), EXCEPTION_MESSAGE, t);
    }

    private static Level toLevel(InternalLogLevel level) {
        switch (level) {
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.DEBUG;
            case WARN:
                return Level.WARN;
            case ERROR:
                return Level.ERROR;
            case TRACE:
                return Level.TRACE;
            default:
                throw new Error();
        }
    }
}
