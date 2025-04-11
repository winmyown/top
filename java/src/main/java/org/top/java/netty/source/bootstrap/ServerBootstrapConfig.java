
package org.top.java.netty.source.bootstrap;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;

import java.util.Map;

/**
 * Exposes the configuration of a {@link ServerBootstrapConfig}.
 */

/**
 * 暴露 {@link ServerBootstrapConfig} 的配置。
 */
public final class ServerBootstrapConfig extends AbstractBootstrapConfig<ServerBootstrap, ServerChannel> {

    ServerBootstrapConfig(ServerBootstrap bootstrap) {
        super(bootstrap);
    }

    /**
     * Returns the configured {@link EventLoopGroup} which will be used for the child channels or {@code null}
     * if non is configured yet.
     */

    /**
     * 返回已配置的 {@link EventLoopGroup}，该组将用于子通道，如果尚未配置，则返回 {@code null}。
     */
    @SuppressWarnings("deprecation")
    public EventLoopGroup childGroup() {
        return bootstrap.childGroup();
    }

    /**
     * Returns the configured {@link ChannelHandler} be used for the child channels or {@code null}
     * if non is configured yet.
     */

    /**
     * 返回用于子通道的已配置的 {@link ChannelHandler}，如果尚未配置，则返回 {@code null}。
     */
    public ChannelHandler childHandler() {
        return bootstrap.childHandler();
    }

    /**
     * Returns a copy of the configured options which will be used for the child channels.
     */

    /**
     * 返回配置选项的副本，这些选项将用于子通道。
     */
    public Map<ChannelOption<?>, Object> childOptions() {
        return bootstrap.childOptions();
    }

    /**
     * Returns a copy of the configured attributes which will be used for the child channels.
     */

    /**
     * 返回配置的属性副本，这些属性将用于子通道。
     */
    public Map<AttributeKey<?>, Object> childAttrs() {
        return bootstrap.childAttrs();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        EventLoopGroup childGroup = childGroup();
        if (childGroup != null) {
            buf.append("childGroup: ");
            buf.append(StringUtil.simpleClassName(childGroup));
            buf.append(", ");
        }
        Map<ChannelOption<?>, Object> childOptions = childOptions();
        if (!childOptions.isEmpty()) {
            buf.append("childOptions: ");
            buf.append(childOptions);
            buf.append(", ");
        }
        Map<AttributeKey<?>, Object> childAttrs = childAttrs();
        if (!childAttrs.isEmpty()) {
            buf.append("childAttrs: ");
            buf.append(childAttrs);
            buf.append(", ");
        }
        ChannelHandler childHandler = childHandler();
        if (childHandler != null) {
            buf.append("childHandler: ");
            buf.append(childHandler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }
}
