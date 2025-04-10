
package org.top.java.netty.source.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.netty.util.internal.CleanerJava6")
final class CleanerJava6Substitution {
    private CleanerJava6Substitution() {
    }

    @Alias
    @RecomputeFieldValue(
        kind = RecomputeFieldValue.Kind.FieldOffset,
        declClassName = "java.nio.DirectByteBuffer",
        name = "cleaner")
    private static long CLEANER_FIELD_OFFSET;
}
