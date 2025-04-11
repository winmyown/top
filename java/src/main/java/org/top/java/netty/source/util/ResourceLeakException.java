

package org.top.java.netty.source.util;

import java.util.Arrays;

/**
 * @deprecated This class will be removed in the future version.
 */

/**
 * @deprecated 该类将在未来的版本中移除。
 */
@Deprecated
public class ResourceLeakException extends RuntimeException {

    private static final long serialVersionUID = 7186453858343358280L;

    private final StackTraceElement[] cachedStackTrace;

    public ResourceLeakException() {
        cachedStackTrace = getStackTrace();
    }

    public ResourceLeakException(String message) {
        super(message);
        cachedStackTrace = getStackTrace();
    }

    public ResourceLeakException(String message, Throwable cause) {
        super(message, cause);
        cachedStackTrace = getStackTrace();
    }

    public ResourceLeakException(Throwable cause) {
        super(cause);
        cachedStackTrace = getStackTrace();
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (StackTraceElement e: cachedStackTrace) {
            hashCode = hashCode * 31 + e.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResourceLeakException)) {
            return false;
        }
        if (o == this) {
            return true;
        }

        return Arrays.equals(cachedStackTrace, ((ResourceLeakException) o).cachedStackTrace);
    }
}
