
package org.top.java.netty.source.util.internal.svm;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.netty.util.internal.PlatformDependent")
final class PlatformDependentSubstitution {
    private PlatformDependentSubstitution() {
    }

    /**
     * The class PlatformDependent caches the byte array base offset by reading the
     * field from PlatformDependent0. The automatic recomputation of Substrate VM
     * correctly recomputes the field in PlatformDependent0, but since the caching
     * in PlatformDependent happens during image building, the non-recomputed value
     * is cached.
     */

    /**
     * PlatformDependent 类通过从 PlatformDependent0 读取字段来缓存字节数组的基础偏移量。Substrate VM 的自动重新计算
     * 正确地重新计算了 PlatformDependent0 中的字段，但由于 PlatformDependent 中的缓存在镜像构建期间发生，因此缓存的是未重新计算的值。
     */
    @Alias
    @RecomputeFieldValue(
        kind = RecomputeFieldValue.Kind.ArrayBaseOffset,
        declClass = byte[].class)
    private static long BYTE_ARRAY_BASE_OFFSET;
}
