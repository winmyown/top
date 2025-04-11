
package org.top.java.netty.source.bootstrap;

import io.netty.channel.Channel;
import io.netty.resolver.AddressResolverGroup;

import java.net.SocketAddress;

/**
 * Exposes the configuration of a {@link Bootstrap}.
 */

/**
 * 暴露一个{@link Bootstrap}的配置。
 */
public final class BootstrapConfig extends AbstractBootstrapConfig<Bootstrap, Channel> {

    BootstrapConfig(Bootstrap bootstrap) {
        super(bootstrap);
    }

    /**
     * Returns the configured remote address or {@code null} if non is configured yet.
     */

    /**
     * 返回配置的远程地址，如果尚未配置则返回 {@code null}。
     */
    public SocketAddress remoteAddress() {
        return bootstrap.remoteAddress();
    }

    /**
     * Returns the configured {@link AddressResolverGroup} or the default if non is configured yet.
     */

    /**
     * 返回已配置的 {@link AddressResolverGroup}，如果尚未配置，则返回默认值。
     */
    public AddressResolverGroup<?> resolver() {
        return bootstrap.resolver();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", resolver: ").append(resolver());
        SocketAddress remoteAddress = remoteAddress();
        if (remoteAddress != null) {
            buf.append(", remoteAddress: ")
                    .append(remoteAddress);
        }
        return buf.append(')').toString();
    }
}
