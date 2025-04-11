

package org.top.java.netty.source.util.internal;

public final class NoOpTypeParameterMatcher extends TypeParameterMatcher {
    @Override
    public boolean match(Object msg) {
        return true;
    }
}
