

package org.top.java.netty.source.util;

/**
 * A hint object that provides human-readable message for easier resource leak tracking.
 */

/**
 * 一个提示对象，提供人类可读的消息，以便更轻松地跟踪资源泄漏。
 */
public interface ResourceLeakHint {
    /**
     * Returns a human-readable message that potentially enables easier resource leak tracking.
     */
    /**
     * 返回一个人类可读的消息，可能有助于更轻松地追踪资源泄漏。
     */
    String toHintString();
}
