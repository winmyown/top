/*
 * 版权所有 (c) 1996, 2013, Oracle 和/或其附属公司。保留所有权利。
 * ORACLE 专有/机密。使用受许可条款约束。
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.top.java.source.io;

import java.io.*;

/**
 * 类的可序列化性通过类实现 java.io.Serializable 接口来启用。未实现此接口的类将不会对其状态进行序列化或反序列化。所有可序列化类的子类型本身也是可序列化的。序列化接口没有方法或字段，仅用于标识可序列化的语义。<p>
 *
 * 为了允许非序列化类的子类型被序列化，子类型可以承担保存和恢复超类型的公共、受保护和（如果可访问）包字段状态的责任。子类型只有在扩展的类具有可访问的无参构造函数以初始化类的状态时，才能承担此责任。如果情况并非如此，则声明类为 Serializable 是错误的。该错误将在运行时被检测到。<p>
 *
 * 在反序列化过程中，非序列化类的字段将使用类的公共或受保护的无参构造函数进行初始化。无参构造函数必须对可序列化的子类可访问。可序列化子类的字段将从流中恢复。<p>
 *
 * 在遍历图时，可能会遇到不支持 Serializable 接口的对象。在这种情况下，将抛出 NotSerializableException 并标识非序列化对象的类。<p>
 *
 * 在序列化和反序列化过程中需要特殊处理的类必须实现具有以下确切签名的特殊方法：
 *
 * <PRE>
 * private void writeObject(java.io.ObjectOutputStream out)
 *     throws IOException
 * private void readObject(java.io.ObjectInputStream in)
 *     throws IOException, ClassNotFoundException;
 * private void readObjectNoData()
 *     throws ObjectStreamException;
 * </PRE>
 *
 * <p>writeObject 方法负责为其特定类写入对象的状态，以便相应的 readObject 方法可以恢复它。可以通过调用 out.defaultWriteObject 来调用保存对象字段的默认机制。该方法不需要关心其超类或子类的状态。通过使用 writeObject 方法将各个字段写入 ObjectOutputStream，或使用 DataOutput 支持的原始数据类型方法来保存状态。
 *
 * <p>readObject 方法负责从流中读取并恢复类的字段。它可以调用 in.defaultReadObject 来调用恢复对象的非静态和非瞬态字段的默认机制。defaultReadObject 方法使用流中的信息将流中保存的对象的字段分配给当前对象中相应命名的字段。这处理了类已演变为添加新字段的情况。该方法不需要关心其超类或子类的状态。通过使用 writeObject 方法将各个字段写入 ObjectOutputStream，或使用 DataOutput 支持的原始数据类型方法来保存状态。
 *
 * <p>readObjectNoData 方法负责在序列化流未将给定类列为正在反序列化的对象的超类时，为其特定类初始化对象的状态。这可能发生在接收方使用与发送方不同版本的已反序列化实例的类，并且接收方的版本扩展了发送方版本未扩展的类的情况下。如果序列化流被篡改，也可能发生这种情况；因此，readObjectNoData 对于正确初始化反序列化对象非常有用，尽管流是“敌对”的或不完整的。
 *
 * <p>需要在将对象写入流时指定替代对象的可序列化类应实现具有以下确切签名的特殊方法：
 *
 * <PRE>
 * ANY-ACCESS-MODIFIER Object writeReplace() throws ObjectStreamException;
 * </PRE><p>
 *
 * 如果存在此 writeReplace 方法并且可以从正在序列化的对象类中定义的方法访问它，则序列化将调用此方法。因此，该方法可以具有 private、protected 和包私有访问权限。子类对此方法的访问遵循 Java 可访问性规则。<p>
 *
 * 需要在从流中读取其实例时指定替换的类应实现具有以下确切签名的特殊方法。
 *
 * <PRE>
 * ANY-ACCESS-MODIFIER Object readResolve() throws ObjectStreamException;
 * </PRE><p>
 *
 * 此 readResolve 方法遵循与 writeReplace 相同的调用规则和可访问性规则。<p>
 *
 * 序列化运行时与每个可序列化类关联一个版本号，称为 serialVersionUID，用于在反序列化期间验证序列化对象的发送方和接收方是否加载了与该对象兼容的类。如果接收方加载的对象的类具有与相应发送方类不同的 serialVersionUID，则反序列化将导致 {@link InvalidClassException}。可序列化类可以通过声明一个名为 <code>"serialVersionUID"</code> 的字段来显式声明其 serialVersionUID，该字段必须是 static、final 且类型为 <code>long</code>：
 *
 * <PRE>
 * ANY-ACCESS-MODIFIER static final long serialVersionUID = 42L;
 * </PRE>
 *
 * 如果可序列化类未显式声明 serialVersionUID，则序列化运行时将根据类的各个方面计算默认的 serialVersionUID 值，如 Java(TM) 对象序列化规范中所述。然而，强烈建议所有可序列化类显式声明 serialVersionUID 值，因为默认的 serialVersionUID 计算对类细节高度敏感，这些细节可能因编译器实现而异，因此可能导致反序列化期间出现意外的 <code>InvalidClassException</code>。因此，为了在不同 Java 编译器实现中保证一致的 serialVersionUID 值，可序列化类必须声明显式的 serialVersionUID 值。还强烈建议显式 serialVersionUID 声明尽可能使用 <code>private</code> 修饰符，因为此类声明仅适用于直接声明的类——serialVersionUID 字段作为继承成员没有用处。数组类不能声明显式的 serialVersionUID，因此它们始终具有默认计算值，但对数组类放弃了匹配 serialVersionUID 值的要求。
 *
 * @author  unascribed
 * @see ObjectOutputStream
 * @see ObjectInputStream
 * @see ObjectOutput
 * @see ObjectInput
 * @see Externalizable
 * @since   JDK1.1
 */
public interface Serializable {
}
