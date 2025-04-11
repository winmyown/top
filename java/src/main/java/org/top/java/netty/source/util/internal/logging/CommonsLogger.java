
/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/**
 * 版权所有 (c) 2004-2011 QOS.ch
 * 保留所有权利。
 *
 * 特此授予任何人免费的许可，以获取本软件及其相关文档文件（“软件”）的副本，
 * 并无限制地处理软件，包括但不限于使用、复制、修改、合并、出版、分发、再许可
 * 和/或销售软件的副本，并允许获得软件的人员在遵守以下条件的情况下使用软件：
 *
 * 上述版权声明和本许可声明应包含在软件的所有副本或主要部分中。
 *
 * 本软件按“原样”提供，不提供任何形式的明示或暗示的担保，包括但不限于对适销性、
 * 特定用途适用性和非侵权性的担保。在任何情况下，作者或版权持有人均不对任何索赔、
 * 损害或其他责任负责，无论是在合同、侵权或其他行为中产生的，还是与软件或软件的使用
 * 或其他交易相关的。
 *
 */
package org.top.java.netty.source.util.internal.logging;

import org.apache.commons.logging.Log;
import org.top.java.netty.source.util.internal.ObjectUtil;

/**
 * <a href="https://commons.apache.org/logging/">Apache Commons Logging</a>
 * logger.
 *
 * @deprecated Please use {@link Log4J2Logger} or {@link Log4JLogger} or
 * {@link Slf4JLogger}.
 */

/**
 * <a href="https://commons.apache.org/logging/">Apache Commons Logging</a>
 * 日志记录器。
 *
 * @deprecated 请使用 {@link Log4J2Logger} 或 {@link Log4JLogger} 或
 * {@link Slf4JLogger}。
 */
@Deprecated
class CommonsLogger extends AbstractInternalLogger {

    private static final long serialVersionUID = 8647838678388394885L;

    private final transient Log logger;

    CommonsLogger(Log logger, String name) {
        super(name);
        this.logger = ObjectUtil.checkNotNull(logger, "logger");
    }

    /**
     * Delegates to the {@link Log#isTraceEnabled} method of the underlying
     * {@link Log} instance.
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#isTraceEnabled} 方法。
     */
    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Delegates to the {@link Log#trace(Object)} method of the underlying
     * {@link Log} instance.
     *
     * @param msg - the message object to be logged
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#trace(Object)} 方法。
     *
     * @param msg - 要记录的消息对象
     */
    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    /**
     * Delegates to the {@link Log#trace(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#trace(Object)} 方法。
     *
     * <p>
     * 然而，当日志记录器对 TRACE 级别禁用时，此形式避免了多余的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void trace(String format, Object arg) {
        if (logger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.trace(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#trace(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 委托给底层的 {@link Log#trace(Object)} 方法。
     *
     * <p>
     * 但是，当日志级别为 TRACE 禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void trace(String format, Object argA, Object argB) {
        if (logger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.trace(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#trace(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 委托给底层的 {@link Log#trace(Object)} 方法。
     *
     * <p>
     * 然而，当日志级别为 TRACE 时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format 格式字符串
     * @param arguments 3个或更多参数的列表
     */
    @Override
    public void trace(String format, Object... arguments) {
        if (logger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.trace(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#trace(Object, Throwable)} method of
     * the underlying {@link Log} instance.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#trace(Object, Throwable)} 方法。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（可抛出对象）
     */
    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    /**
     * Delegates to the {@link Log#isDebugEnabled} method of the underlying
     * {@link Log} instance.
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#isDebugEnabled} 方法。
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    //

    /**
     * Delegates to the {@link Log#debug(Object)} method of the underlying
     * {@link Log} instance.
     *
     * @param msg - the message object to be logged
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#debug(Object)} 方法。
     *
     * @param msg - 要记录的消息对象
     */
    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    /**
     * Delegates to the {@link Log#debug(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#debug(Object)} 方法。
     *
     * <p>
     * 然而，当日志级别为 DEBUG 时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param arg
     *          参数
     */
    @Override
    public void debug(String format, Object arg) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#debug(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 委托给底层的 {@link Log#debug(Object)} 方法。
     *
     * <p>
     * 然而，当记录器对 DEBUG 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void debug(String format, Object argA, Object argB) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#debug(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#debug(Object)} 方法。
     *
     * <p>
     * 然而，当日志记录器对 DEBUG 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format 格式字符串
     * @param arguments 3个或更多参数的列表
     */
    @Override
    public void debug(String format, Object... arguments) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#debug(Object, Throwable)} method of
     * the underlying {@link Log} instance.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#debug(Object, Throwable)} 方法。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（可抛出对象）
     */
    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    /**
     * Delegates to the {@link Log#isInfoEnabled} method of the underlying
     * {@link Log} instance.
     */

    /**
     * 委托给底层的 {@link Log#isInfoEnabled} 方法。
     */
    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Delegates to the {@link Log#debug(Object)} method of the underlying
     * {@link Log} instance.
     *
     * @param msg - the message object to be logged
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#debug(Object)} 方法。
     *
     * @param msg - 要记录的消息对象
     */
    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    /**
     * Delegates to the {@link Log#info(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level INFO.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#info(Object)} 方法。
     *
     * <p>
     * 但是，当记录器对 INFO 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param arg
     *          参数
     */

    @Override
    public void info(String format, Object arg) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.info(ft.getMessage(), ft.getThrowable());
        }
    }
    /**
     * Delegates to the {@link Log#info(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level INFO.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */
    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#info(Object)} 方法。
     *
     * <p>
     * 然而，当日志记录器对 INFO 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void info(String format, Object argA, Object argB) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#info(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level INFO.
     * </p>
     *
     * @param format the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 委托给底层的 {@link Log#info(Object)} 方法。
     *
     * <p>
     * 然而，当日志器对 INFO 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format 格式字符串
     * @param arguments 3个或更多参数的列表
     */
    @Override
    public void info(String format, Object... arguments) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#info(Object, Throwable)} method of
     * the underlying {@link Log} instance.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#info(Object, Throwable)} 方法。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    /**
     * Delegates to the {@link Log#isWarnEnabled} method of the underlying
     * {@link Log} instance.
     */

    /**
     * 委托给底层的 {@link Log#isWarnEnabled} 方法。
     */
    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Delegates to the {@link Log#warn(Object)} method of the underlying
     * {@link Log} instance.
     *
     * @param msg - the message object to be logged
     */

    /**
     * 委托给底层的 {@link Log#warn(Object)} 方法。
     *
     * @param msg - 要记录的消息对象
     */
    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    /**
     * Delegates to the {@link Log#warn(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level WARN.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 委托给底层的 {@link Log#warn(Object)} 方法。
     *
     * <p>
     * 然而，当日志级别为 WARN 时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void warn(String format, Object arg) {
        if (logger.isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#warn(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level WARN.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 委托给底层的 {@link Log#warn(Object)} 方法。
     *
     * <p>
     * 然而，当记录器在 WARN 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式化字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void warn(String format, Object argA, Object argB) {
        if (logger.isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#warn(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level WARN.
     * </p>
     *
     * @param format the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#warn(Object)} 方法。
     *
     * <p>
     * 然而，当日志记录器在 WARN 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format 格式字符串
     * @param arguments 3个或更多参数的列表
     */
    @Override
    public void warn(String format, Object... arguments) {
        if (logger.isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#warn(Object, Throwable)} method of
     * the underlying {@link Log} instance.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 委托给底层的 {@link Log} 实例的 {@link Log#warn(Object, Throwable)} 方法。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（可抛出对象）
     */

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    /**
     * Delegates to the {@link Log#isErrorEnabled} method of the underlying
     * {@link Log} instance.
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#isErrorEnabled} 方法。
     */
    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * Delegates to the {@link Log#error(Object)} method of the underlying
     * {@link Log} instance.
     *
     * @param msg - the message object to be logged
     */

    /**
     * 委托给底层的 {@link Log#error(Object)} 方法。
     *
     * @param msg - 要记录的消息对象
     */
    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    /**
     * Delegates to the {@link Log#error(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level ERROR.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#error(Object)} 方法。
     *
     * <p>
     * 然而，当记录器对 ERROR 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void error(String format, Object arg) {
        if (logger.isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#error(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level ERROR.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#error(Object)} 方法。
     *
     * <p>
     * 然而，当日志级别为 ERROR 时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void error(String format, Object argA, Object argB) {
        if (logger.isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#error(Object)} method of the underlying
     * {@link Log} instance.
     *
     * <p>
     * However, this form avoids superfluous object creation when the logger is disabled
     * for level ERROR.
     * </p>
     *
     * @param format the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 委托给底层的 {@link Log#error(Object)} 方法。
     *
     * <p>
     * 但是，当日志器对 ERROR 级别禁用时，此形式避免了多余的对象创建。
     * </p>
     *
     * @param format 格式字符串
     * @param arguments 3个或更多参数的列表
     */
    @Override
    public void error(String format, Object... arguments) {
        if (logger.isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Delegates to the {@link Log#error(Object, Throwable)} method of
     * the underlying {@link Log} instance.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 委托给底层 {@link Log} 实例的 {@link Log#error(Object, Throwable)} 方法。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（可抛出对象）
     */
    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
}
