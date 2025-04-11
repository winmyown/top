

package org.top.java.netty.source.util.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to suppress the Java 6 source code requirement checks for a method.
 */

/**
 * 用于抑制对方法Java 6源代码要求的检查的注解。
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface SuppressJava6Requirement {

    String reason();
}
