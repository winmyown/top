
package org.top.java.netty.source.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.netty.util.internal.shaded.io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
final class UnsafeRefArrayAccessSubstitution {
    private UnsafeRefArrayAccessSubstitution() {
    }

    @Alias
    @RecomputeFieldValue(
        kind = RecomputeFieldValue.Kind.ArrayIndexShift,
        declClass = Object[].class)
    public static int REF_ELEMENT_SHIFT;
}
