
package org.top.java.netty.source.bootstrap;

import io.netty.channel.Channel;

/**
 * @deprecated Use {@link io.netty.channel.ChannelFactory} instead.
 */

/**
 * @deprecated 请使用 {@link io.netty.channel.ChannelFactory} 代替。
 */
@Deprecated
public interface ChannelFactory<T extends Channel> {
    /**
     * Creates a new channel.
     */
    /**
     * 创建一个新的频道。
     */
    T newChannel();
}
