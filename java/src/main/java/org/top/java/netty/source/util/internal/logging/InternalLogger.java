
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

/**
 * <em>Internal-use-only</em> logger used by Netty.  <strong>DO NOT</strong>
 * access this class outside of Netty.
 */

/**
 * <em>仅供内部使用</em>的日志记录器，由Netty使用。<strong>请勿</strong>
 * 在Netty之外访问此类。
 */
public interface InternalLogger {

    /**
     * Return the name of this {@link InternalLogger} instance.
     *
     * @return name of this logger instance
     */

    /**
     * 返回此 {@link InternalLogger} 实例的名称。
     *
     * @return 此日志记录器实例的名称
     */
    String name();

    /**
     * Is the logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for the TRACE level,
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了 TRACE 级别？
     *
     * @return 如果此 Logger 启用了 TRACE 级别，则返回 true，
     *         否则返回 false。
     */
    boolean isTraceEnabled();

    /**
     * Log a message at the TRACE level.
     *
     * @param msg the message string to be logged
     */

    /**
     * 在 TRACE 级别记录消息。
     *
     * @param msg 要记录的消息字符串
     */
    void trace(String msg);

    /**
     * Log a message at the TRACE level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the TRACE level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数记录TRACE级别的日志消息。
     * <p/>
     * <p>当日志器在TRACE级别被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void trace(String format, Object arg);

    /**
     * Log a message at the TRACE level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the TRACE level. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数记录 TRACE 级别的消息。
     * <p/>
     * <p>当记录器对 TRACE 级别禁用时，此形式避免了多余的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void trace(String format, Object argA, Object argB);

    /**
     * Log a message at the TRACE level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the TRACE level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for TRACE. The variants taking {@link #trace(String, Object) one} and
     * {@link #trace(String, Object, Object) two} arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数记录一条TRACE级别的消息。
     * <p/>
     * <p>当记录器对TRACE级别禁用时，此形式避免了不必要的字符串连接。然而，即使此记录器对TRACE级别禁用，此变体在调用方法之前也会产生创建{@code Object[]}的隐藏（相对较小）成本。存在接受{@link #trace(String, Object) 一个}和
     * {@link #trace(String, Object, Object) 两个}参数的变体，仅仅是为了避免此隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 包含3个或更多参数的列表
     */
    void trace(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the TRACE level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 以TRACE级别记录异常（throwable）及附带的消息。
     *
     * @param msg 随异常附带的消息
     * @param t   要记录的异常（throwable）
     */
    void trace(String msg, Throwable t);

    /**
     * Log an exception (throwable) at the TRACE level.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 在TRACE级别记录一个异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void trace(Throwable t);

    /**
     * Is the logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for the DEBUG level,
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了DEBUG级别？
     *
     * @return 如果此Logger启用了DEBUG级别，则返回true，
     *         否则返回false。
     */
    boolean isDebugEnabled();

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */

    /**
     * 在DEBUG级别记录消息。
     *
     * @param msg 要记录的消息字符串
     */
    void debug(String msg);

    /**
     * Log a message at the DEBUG level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the DEBUG level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数记录DEBUG级别的消息。
     * <p/>
     * <p>当日志器在DEBUG级别被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void debug(String format, Object arg);

    /**
     * Log a message at the DEBUG level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the DEBUG level. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数记录DEBUG级别的消息。
     * <p/>
     * <p>当日志记录器在DEBUG级别被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void debug(String format, Object argA, Object argB);

    /**
     * Log a message at the DEBUG level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the DEBUG level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for DEBUG. The variants taking
     * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数在DEBUG级别记录消息。
     * <p/>
     * <p>当记录器在DEBUG级别被禁用时，此形式避免了不必要的字符串连接。然而，即使此记录器在DEBUG级别被禁用，此变体在调用方法之前仍会产生创建{@code Object[]}的隐藏（且相对较小）成本。存在接受
     * {@link #debug(String, Object) 一个}和{@link #debug(String, Object, Object) 两个}
     * 参数的变体，仅是为了避免此隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 3个或更多参数的列表
     */
    void debug(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the DEBUG level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 以DEBUG级别记录异常（Throwable）并附带消息。
     *
     * @param msg 伴随异常的消息
     * @param t   要记录的异常（Throwable）
     */
    void debug(String msg, Throwable t);

    /**
     * Log an exception (throwable) at the DEBUG level.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 以DEBUG级别记录异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void debug(Throwable t);

    /**
     * Is the logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level,
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了 INFO 级别？
     *
     * @return 如果此 Logger 启用了 INFO 级别，则返回 true，
     *         否则返回 false。
     */
    boolean isInfoEnabled();

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */

    /**
     * 在INFO级别记录一条消息。
     *
     * @param msg 要记录的消息字符串
     */
    void info(String msg);

    /**
     * Log a message at the INFO level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数记录一条INFO级别的消息。
     * <p/>
     * <p>当记录器对INFO级别禁用时，此形式避免了多余的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void info(String format, Object arg);

    /**
     * Log a message at the INFO level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数记录INFO级别的消息。
     * <p/>
     * <p>当INFO级别的日志被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void info(String format, Object argA, Object argB);

    /**
     * Log a message at the INFO level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the INFO level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for INFO. The variants taking
     * {@link #info(String, Object) one} and {@link #info(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数，在INFO级别记录消息。
     * <p/>
     * <p>当记录器在INFO级别被禁用时，此形式避免了不必要的字符串连接。然而，即使此记录器在INFO级别被禁用，此变体在调用方法之前也会产生创建{@code Object[]}的隐藏（相对较小的）成本。提供
     * {@link #info(String, Object) 一个}和{@link #info(String, Object, Object) 两个}
     * 参数的变体仅是为了避免此隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 3个或更多参数的列表
     */
    void info(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the INFO level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 以INFO级别记录一个异常（throwable）及附带的消息。
     *
     * @param msg 伴随异常的消息
     * @param t   要记录的异常（throwable）
     */
    void info(String msg, Throwable t);

    /**
     * Log an exception (throwable) at the INFO level.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 以INFO级别记录异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void info(Throwable t);

    /**
     * Is the logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level,
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了WARN级别？
     *
     * @return 如果此Logger启用了WARN级别，则返回true，
     *         否则返回false。
     */
    boolean isWarnEnabled();

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */

    /**
     * 以WARN级别记录一条消息。
     *
     * @param msg 要记录的消息字符串
     */
    void warn(String msg);

    /**
     * Log a message at the WARN level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数记录一条WARN级别的日志消息。
     * <p/>
     * <p>当WARN级别的日志被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void warn(String format, Object arg);

    /**
     * Log a message at the WARN level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the WARN level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for WARN. The variants taking
     * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数记录一条WARN级别的日志消息。
     * <p/>
     * <p>当记录器对WARN级别禁用时，此形式避免了不必要的字符串连接。然而，即使此记录器对WARN禁用，此变体在调用方法之前也会产生创建{@code Object[]}的隐藏（相对较小）成本。存在{@link #warn(String, Object) 一个}和{@link #warn(String, Object, Object) 两个}参数的变体，仅是为了避免这种隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 3个或更多参数的列表
     */
    void warn(String format, Object... arguments);

    /**
     * Log a message at the WARN level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数记录一条WARN级别的日志消息。
     * <p/>
     * <p>当日志器对WARN级别禁用时，此形式避免了多余的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void warn(String format, Object argA, Object argB);

    /**
     * Log an exception (throwable) at the WARN level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 以WARN级别记录异常（throwable）并附带消息。
     *
     * @param msg 伴随异常的消息
     * @param t   要记录的异常（throwable）
     */
    void warn(String msg, Throwable t);

    /**
     * Log an exception (throwable) at the WARN level.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 以 WARN 级别记录异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void warn(Throwable t);

    /**
     * Is the logger instance enabled for the ERROR level?
     *
     * @return True if this Logger is enabled for the ERROR level,
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了 ERROR 级别？
     *
     * @return 如果此 Logger 启用了 ERROR 级别，
     *         则返回 true，否则返回 false。
     */
    boolean isErrorEnabled();

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */

    /**
     * 在ERROR级别记录一条消息。
     *
     * @param msg 要记录的消息字符串
     */
    void error(String msg);

    /**
     * Log a message at the ERROR level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数记录ERROR级别的消息。
     * <p/>
     * <p>当日志记录器对ERROR级别禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void error(String format, Object arg);

    /**
     * Log a message at the ERROR level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数记录ERROR级别的日志消息。
     * <p/>
     * <p>当日志记录器在ERROR级别被禁用时，此形式避免了多余的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void error(String format, Object argA, Object argB);

    /**
     * Log a message at the ERROR level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the ERROR level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for ERROR. The variants taking
     * {@link #error(String, Object) one} and {@link #error(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数记录一个 ERROR 级别的日志消息。
     * <p/>
     * <p>当日志器对 ERROR 级别禁用时，此形式避免了不必要的字符串连接。然而，即使此日志器对 ERROR 禁用，此变体在调用方法之前仍会产生创建 {@code Object[]} 的隐藏（相对较小）成本。
     * 提供 {@link #error(String, Object) 一个} 和 {@link #error(String, Object, Object) 两个} 参数的变体仅是为了避免这种隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 3 个或更多参数的列表
     */
    void error(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the ERROR level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 以 ERROR 级别记录异常（throwable）并附带消息。
     *
     * @param msg 伴随异常的消息
     * @param t   要记录的异常（throwable）
     */
    void error(String msg, Throwable t);

    /**
     * Log an exception (throwable) at the ERROR level.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 以 ERROR 级别记录异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void error(Throwable t);

    /**
     * Is the logger instance enabled for the specified {@code level}?
     *
     * @return True if this Logger is enabled for the specified {@code level},
     *         false otherwise.
     */

    /**
     * 日志记录器实例是否启用了指定的 {@code level}？
     *
     * @return 如果此 Logger 启用了指定的 {@code level}，则返回 true，
     *         否则返回 false。
     */
    boolean isEnabled(InternalLogLevel level);

    /**
     * Log a message at the specified {@code level}.
     *
     * @param msg the message string to be logged
     */

    /**
     * 在指定的 {@code level} 记录一条消息。
     *
     * @param msg 要记录的消息字符串
     */
    void log(InternalLogLevel level, String msg);

    /**
     * Log a message at the specified {@code level} according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the specified {@code level}. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */

    /**
     * 根据指定的格式和参数，在指定的 {@code level} 记录日志消息。
     * <p/>
     * <p>当日志器在指定的 {@code level} 被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param arg    参数
     */
    void log(InternalLogLevel level, String format, Object arg);

    /**
     * Log a message at the specified {@code level} according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the specified {@code level}. </p>
     *
     * @param format the format string
     * @param argA   the first argument
     * @param argB   the second argument
     */

    /**
     * 根据指定的格式和参数，在指定的 {@code level} 记录日志消息。
     * <p/>
     * <p>当指定 {@code level} 的日志记录器被禁用时，此形式避免了不必要的对象创建。</p>
     *
     * @param format 格式字符串
     * @param argA   第一个参数
     * @param argB   第二个参数
     */
    void log(InternalLogLevel level, String format, Object argA, Object argB);

    /**
     * Log a message at the specified {@code level} according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the specified {@code level}. However, this variant incurs the hidden
     * (and relatively small) cost of creating an {@code Object[]} before invoking the method,
     * even if this logger is disabled for the specified {@code level}. The variants taking
     * {@link #log(InternalLogLevel, String, Object) one} and
     * {@link #log(InternalLogLevel, String, Object, Object) two} arguments exist solely
     * in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */

    /**
     * 根据指定的格式和参数，在指定的 {@code level} 记录消息。
     * <p/>
     * <p>当指定 {@code level} 的日志记录被禁用时，此形式避免了不必要的字符串连接。然而，即使此日志记录器在指定 {@code level} 被禁用，此变体仍会产生创建 {@code Object[]} 的隐藏（相对较小）成本。提供 {@link #log(InternalLogLevel, String, Object) 一个} 和
     * {@link #log(InternalLogLevel, String, Object, Object) 两个} 参数的变体，仅是为了避免此隐藏成本。</p>
     *
     * @param format    格式字符串
     * @param arguments 3个或更多参数的列表
     */
    void log(InternalLogLevel level, String format, Object... arguments);

    /**
     * Log an exception (throwable) at the specified {@code level} with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */

    /**
     * 在指定的 {@code level} 记录一个异常（throwable）并附带一条消息。
     *
     * @param msg 伴随异常的消息
     * @param t   要记录的异常（throwable）
     */
    void log(InternalLogLevel level, String msg, Throwable t);

    /**
     * Log an exception (throwable) at the specified {@code level}.
     *
     * @param t   the exception (throwable) to log
     */

    /**
     * 在指定的 {@code level} 记录一个异常（throwable）。
     *
     * @param t   要记录的异常（throwable）
     */
    void log(InternalLogLevel level, Throwable t);
}
