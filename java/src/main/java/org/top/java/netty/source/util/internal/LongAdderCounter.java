
package org.top.java.netty.source.util.internal;

import java.util.concurrent.atomic.LongAdder;

@SuppressJava6Requirement(reason = "Usage guarded by java version check")
final class LongAdderCounter extends LongAdder implements LongCounter {

    @Override
    public long value() {
        return longValue();
    }
}
