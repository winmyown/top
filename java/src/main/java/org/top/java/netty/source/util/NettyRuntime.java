

package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.SystemPropertyUtil;

import java.util.Locale;

/**
 * A utility class for wrapping calls to {@link Runtime}.
 */

/**
 * 一个用于封装对 {@link Runtime} 调用的工具类。
 */
public final class NettyRuntime {

    /**
     * Holder class for available processors to enable testing.
     */

    /**
     * 用于可用处理器的持有者类，以便进行测试。
     */
    static class AvailableProcessorsHolder {

        private int availableProcessors;

        /**
         * Set the number of available processors.
         *
         * @param availableProcessors the number of available processors
         * @throws IllegalArgumentException if the specified number of available processors is non-positive
         * @throws IllegalStateException    if the number of available processors is already configured
         */

        /**
         * 设置可用处理器的数量。
         *
         * @param availableProcessors 可用处理器的数量
         * @throws IllegalArgumentException 如果指定的可用处理器数量为非正数
         * @throws IllegalStateException    如果可用处理器的数量已经配置
         */
        synchronized void setAvailableProcessors(final int availableProcessors) {
            ObjectUtil.checkPositive(availableProcessors, "availableProcessors");
            if (this.availableProcessors != 0) {
                final String message = String.format(
                        Locale.ROOT,
                        "availableProcessors is already set to [%d], rejecting [%d]",
                        this.availableProcessors,
                        availableProcessors);
                throw new IllegalStateException(message);
            }
            this.availableProcessors = availableProcessors;
        }

        /**
         * Get the configured number of available processors. The default is {@link Runtime#availableProcessors()}.
         * This can be overridden by setting the system property "io.netty.availableProcessors" or by invoking
         * {@link #setAvailableProcessors(int)} before any calls to this method.
         *
         * @return the configured number of available processors
         */

        /**
         * 获取配置的可用处理器数量。默认值为 {@link Runtime#availableProcessors()}。
         * 可以通过设置系统属性 "io.netty.availableProcessors" 或在调用此方法之前调用
         * {@link #setAvailableProcessors(int)} 来覆盖此值。
         *
         * @return 配置的可用处理器数量
         */
        @SuppressForbidden(reason = "to obtain default number of available processors")
        synchronized int availableProcessors() {
            if (this.availableProcessors == 0) {
                final int availableProcessors =
                        SystemPropertyUtil.getInt(
                                "io.netty.availableProcessors",
                                Runtime.getRuntime().availableProcessors());
                setAvailableProcessors(availableProcessors);
            }
            return this.availableProcessors;
        }
    }

    private static final AvailableProcessorsHolder holder = new AvailableProcessorsHolder();

    /**
     * Set the number of available processors.
     *
     * @param availableProcessors the number of available processors
     * @throws IllegalArgumentException if the specified number of available processors is non-positive
     * @throws IllegalStateException    if the number of available processors is already configured
     */

    /**
     * 设置可用处理器的数量。
     *
     * @param availableProcessors 可用处理器的数量
     * @throws IllegalArgumentException 如果指定的可用处理器数量为非正数
     * @throws IllegalStateException    如果可用处理器的数量已经配置
     */
    @SuppressWarnings("unused,WeakerAccess") // this method is part of the public API
    public static void setAvailableProcessors(final int availableProcessors) {
        holder.setAvailableProcessors(availableProcessors);
    }

    /**
     * Get the configured number of available processors. The default is {@link Runtime#availableProcessors()}. This
     * can be overridden by setting the system property "io.netty.availableProcessors" or by invoking
     * {@link #setAvailableProcessors(int)} before any calls to this method.
     *
     * @return the configured number of available processors
     */

    /**
     * 获取配置的可用处理器数量。默认值为 {@link Runtime#availableProcessors()}。可以通过设置系统属性 "io.netty.availableProcessors" 或在调用此方法之前调用
     * {@link #setAvailableProcessors(int)} 来覆盖此值。
     *
     * @return 配置的可用处理器数量
     */
    public static int availableProcessors() {
        return holder.availableProcessors();
    }

    /**
     * No public constructor to prevent instances from being created.
     */

    /**
     * 没有公共构造函数以防止创建实例。
     */
    private NettyRuntime() {
    }
}
