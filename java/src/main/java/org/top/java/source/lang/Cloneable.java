package org.top.java.source.lang;

/**
 * 一个类实现<code>Cloneable</code>接口，以向{@link Object#clone()}方法表明
 * 该方法可以合法地对该类的实例进行字段对字段的复制。
 * <p>
 * 在未实现<code>Cloneable</code>接口的实例上调用Object的clone方法会导致
 * 抛出<code>CloneNotSupportedException</code>异常。
 * <p>
 * 按照惯例，实现此接口的类应该用公共方法覆盖<tt>Object.clone</tt>（它是受保护的）。
 * 有关覆盖此方法的详细信息，请参阅{@link Object#clone()}。
 * <p>
 * 请注意，此接口<i>不</i>包含<tt>clone</tt>方法。因此，仅仅因为一个对象实现了此接口，
 * 并不意味着可以克隆该对象。即使通过反射调用clone方法，也不能保证它会成功。
 *
 * @author  unascribed
 * @see     CloneNotSupportedException
 * @see     Object#clone()
 * @since   JDK1.0
 */
public interface Cloneable {
}
