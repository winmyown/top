
package org.top.java.netty.source.util.concurrent;

/**
 * Expose details for a {@link Thread}.
 */

/**
 * 暴露 {@link Thread} 的详细信息。
 */
public interface ThreadProperties {
    /**
     * @see Thread#getState()
     */
    /**
     * @see Thread#getState()
     */
    Thread.State state();

    /**
     * @see Thread#getPriority()
     */

    /**
     * @see Thread#getPriority()
     */
    int priority();

    /**
     * @see Thread#isInterrupted()
     */

    /**
     * @see Thread#isInterrupted()
     */
    boolean isInterrupted();

    /**
     * @see Thread#isDaemon()
     */

    /**
     * @see Thread#isDaemon()
     */
    boolean isDaemon();

    /**
     * @see Thread#getName()
     */

    /**
     * @see Thread#getName()
     */
    String name();

    /**
     * @see Thread#getId()
     */

    /**
     * @see Thread#getId()
     */
    long id();

    /**
     * @see Thread#getStackTrace()
     */

    /**
     * @see Thread#getStackTrace()
     */
    StackTraceElement[] stackTrace();

    /**
     * @see Thread#isAlive()
     */

    /**
     * @see Thread#isAlive()
     */
    boolean isAlive();
}
