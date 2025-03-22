package org.top.java.source.collection;
/**

 这是一个标记接口，由 {@code List} 实现类使用，用于指示它们支持快速（通常是常数时间）的随机访问。此接口的主要目的是允许通用算法根据列表的访问方式调整其行为，从而在应用于随机访问列表或顺序访问列表时提供良好的性能。

 <p>用于操作随机访问列表（例如 {@code ArrayList}）的最佳算法在应用于顺序访问列表（例如 {@code LinkedList}）时可能会导致二次方的时间复杂度。因此，鼓励通用列表算法在应用可能对顺序访问列表性能较差的算法之前，检查给定的列表是否是此接口的实例（即 {@code instanceof}），并在必要时调整其行为，以保证可接受的性能。
 <p>需要注意的是，随机访问和顺序访问之间的区别通常是模糊的。例如，某些 {@code List} 实现在数据量非常大时可能提供渐近线性的访问时间，但在实际应用中仍然是常数时间。这样的 {@code List} 实现通常应该实现此接口。作为一个经验法则，如果对于类的典型实例，以下循环：
 <pre>
 for (int i=0, n=list.size(); i &lt; n; i++)
 list.get(i);
 </pre>
 比以下循环运行得更快：

 <pre>
 for (Iterator i=list.iterator(); i.hasNext(); )
 i.next();
 </pre>
 那么 {@code List} 实现应该实现此接口。

 <p>此接口是
 <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 Java 集合框架</a>的成员。

 @since 1.4
 */
public interface RandomAccess {
}
