

package org.top.java.netty.source.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to suppress forbidden-apis errors inside a whole class, a method, or a field.
 */

/**
 * 用于在整个类、方法或字段中抑制forbidden-apis错误的注解。
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface SuppressForbidden {

    String reason();
}
