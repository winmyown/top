
package org.top.java.netty.source.util.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a public API that can change at any time (even in minor/bugfix releases).
 *
 * Usage guidelines:
 *
 * <ol>
 *     <li>Is not needed for things located in *.internal.* packages</li>
 *     <li>Only public accessible classes/interfaces must be annotated</li>
 *     <li>If this annotation is not present the API is considered stable and so no backward compatibility can be
 *         broken in a non-major release!</li>
 * </ol>
 */

/**
 * 表示一个公共API，可能在任意时间发生变化（即使在次要/修复版本中）。
 *
 * 使用指南：
 *
 * <ol>
 *     <li>对于位于 *.internal.* 包中的内容不需要使用此注解</li>
 *     <li>只有公共可访问的类/接口必须被注解</li>
 *     <li>如果此注解不存在，API被视为稳定的，因此在非主版本发布中不能破坏向后兼容性！</li>
 * </ol>
 */
@Retention(RetentionPolicy.SOURCE) // TODO Retention policy needs to be CLASS in Netty 5.
@Target({
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PACKAGE,
        ElementType.TYPE
})
@Documented
public @interface UnstableApi {
}
