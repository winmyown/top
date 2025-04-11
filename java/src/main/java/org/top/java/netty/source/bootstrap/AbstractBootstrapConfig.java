
package org.top.java.netty.source.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Exposes the configuration of an {@link AbstractBootstrap}.
 */

/**
 * 暴露 {@link AbstractBootstrap} 的配置。
 */
public abstract class AbstractBootstrapConfig<B extends AbstractBootstrap<B, C>, C extends Channel> {

    protected final B bootstrap;

    protected AbstractBootstrapConfig(B bootstrap) {
        this.bootstrap = ObjectUtil.checkNotNull(bootstrap, "bootstrap");
    }

    /**
     * Returns the configured local address or {@code null} if non is configured yet.
     */

    /**
     * 返回配置的本地地址，如果尚未配置则返回 {@code null}。
     */
    public final SocketAddress localAddress() {
        return bootstrap.localAddress();
    }

    /**
     * Returns the configured {@link ChannelFactory} or {@code null} if non is configured yet.
     */

    /**
     * 返回已配置的 {@link ChannelFactory}，如果尚未配置，则返回 {@code null}。
     */
    @SuppressWarnings("deprecation")
    public final ChannelFactory<? extends C> channelFactory() {
        return bootstrap.channelFactory();
    }

    /**
     * Returns the configured {@link ChannelHandler} or {@code null} if non is configured yet.
     */

    /**
     * 返回配置的 {@link ChannelHandler}，如果尚未配置则返回 {@code null}。
     */
    public final ChannelHandler handler() {
        return bootstrap.handler();
    }

    /**
     * Returns a copy of the configured options.
     */

    /**
     * 返回配置选项的副本。
     */
    public final Map<ChannelOption<?>, Object> options() {
        return bootstrap.options();
    }

    /**
     * Returns a copy of the configured attributes.
     */

    /**
     * 返回配置属性的副本。
     */
    public final Map<AttributeKey<?>, Object> attrs() {
        return bootstrap.attrs();
    }

    /**
     * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
     */

    /**
     * 返回已配置的 {@link EventLoopGroup}，如果尚未配置，则返回 {@code null}。
     */
    @SuppressWarnings("deprecation")
    public final EventLoopGroup group() {
        return bootstrap.group();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
                .append(StringUtil.simpleClassName(this))
                .append('(');
        EventLoopGroup group = group();
        if (group != null) {
            buf.append("group: ")
                    .append(StringUtil.simpleClassName(group))
                    .append(", ");
        }
        @SuppressWarnings("deprecation")
        ChannelFactory<? extends C> factory = channelFactory();
        if (factory != null) {
            buf.append("channelFactory: ")
                    .append(factory)
                    .append(", ");
        }
        SocketAddress localAddress = localAddress();
        if (localAddress != null) {
            buf.append("localAddress: ")
                    .append(localAddress)
                    .append(", ");
        }

        Map<ChannelOption<?>, Object> options = options();
        if (!options.isEmpty()) {
            buf.append("options: ")
                    .append(options)
                    .append(", ");
        }
        Map<AttributeKey<?>, Object> attrs = attrs();
        if (!attrs.isEmpty()) {
            buf.append("attrs: ")
                    .append(attrs)
                    .append(", ");
        }
        ChannelHandler handler = handler();
        if (handler != null) {
            buf.append("handler: ")
                    .append(handler)
                    .append(", ");
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
