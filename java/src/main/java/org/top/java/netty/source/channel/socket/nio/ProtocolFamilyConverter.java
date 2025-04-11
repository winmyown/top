
package org.top.java.netty.source.channel.socket.nio;

import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.util.internal.SuppressJava6Requirement;

import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;

/**
 * Helper class which convert the {@link InternetProtocolFamily}.
 */

/**
 * 辅助类，用于转换 {@link InternetProtocolFamily}。
 */
final class ProtocolFamilyConverter {

    private ProtocolFamilyConverter() {
        // Utility class
        // 工具类
    }

    /**
     * Convert the {@link InternetProtocolFamily}. This MUST only be called on jdk version >= 7.
     */

    /**
     * 转换 {@link InternetProtocolFamily}。此方法只能在 jdk 版本 >= 7 时调用。
     */
    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    public static ProtocolFamily convert(InternetProtocolFamily family) {
        switch (family) {
        case IPv4:
            return StandardProtocolFamily.INET;
        case IPv6:
            return StandardProtocolFamily.INET6;
        default:
            throw new IllegalArgumentException();
        }
    }
}
