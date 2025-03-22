// package org.top.java.source.lang.invoke;
//
// import org.top.java.source.concurrent.ConcurrentHashMap;
// import org.top.java.source.concurrent.ConcurrentMap;
// import org.top.java.source.sun.misc.Unsafe;
// import jdk.internal.org.objectweb.asm.MethodVisitor;
//
// import java.io.File;
// import java.lang.invoke.*;
// import java.util.Properties;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Objects;
// import java.util.Properties;
// import java.util.function.Function;
// import jdk.internal.org.objectweb.asm.Opcodes;
// import sun.security.action.GetPropertyAction;
// import jdk.internal.org.objectweb.asm.ClassWriter;
// import jdk.internal.org.objectweb.asm.Label;
// import jdk.internal.org.objectweb.asm.MethodVisitor;
// import jdk.internal.org.objectweb.asm.Opcodes;
// import jdk.internal.vm.annotation.ForceInline;
// import sun.security.action.GetPropertyAction;
//
// import static jdk.internal.org.objectweb.asm.Opcodes.*;
//
// /**
//  * <p> 用于简化创建字符串拼接方法的工具方法，
//  * 这些方法可以高效地拼接已知类型和数量的参数，
//  * 并且可能在拼接过程中进行类型适配和部分参数求值。
//  * 这些方法通常作为 {@code invokedynamic} 调用点的<em>引导方法</em>使用，
//  * 以支持 Java 编程语言的<em>字符串拼接</em>功能。
//  *
//  * <p>通过所提供的 {@code MethodHandle} 间接访问指定行为的过程分为两个阶段：
//  *
//  * <ol>
//  *     <li><em>链接</em>在调用此类中的方法时发生。
//  *     它们接收一个描述拼接参数数量和类型的方法类型作为参数，
//  *     还可以选择传入字符串<em>配方</em>和参与字符串拼接的常量。
//  *     接受的配方格式的详细信息将在下文中描述。
//  *     链接可能涉及动态加载一个实现预期拼接行为的新类。
//  *     {@code CallSite} 持有一个指向确切拼接方法的 {@code MethodHandle}。
//  *     拼接方法可以在不同的 {@code CallSite} 之间共享，
//  *     例如当链接方法将它们作为纯函数生成时。</li>
//  *
//  *     <li><em>调用</em>发生在生成的拼接方法以确切的动态参数调用时。
//  *     对于单个拼接方法来说，调用可能会多次发生。
//  *     行为 {@code MethodHandle} 引用的方法会与静态参数以及调用时提供的所有其他动态参数一起被调用，
//  *     类似于 {@link MethodHandle#invoke(Object...)}。</li>
//  * </ol>
//  *
//  * <p>此类提供了两种形式的链接方法：一个简单版本
//  * （{@link #makeConcat(java.lang.invoke.MethodHandles.Lookup, String,
//  * MethodType)}），仅使用动态参数；另一个高级版本
//  * （{@link #makeConcatWithConstants(java.lang.invoke.MethodHandles.Lookup,
//  * String, MethodType, String, Object...)}），使用高级形式捕获常量参数。
//  * 高级策略可以生成略微优化的调用字节码，但代价是增加运行时存在的字符串拼接方法形状数量，
//  * 因为这些形状还将包含常量静态参数。
//  *
//  * @作者 Aleksey Shipilev
//  * @作者 Remi Forax
//  * @作者 Peter Levart
//  *
//  * @apiNote
//  * <p>JVM 存在一个限制（classfile 结构约束）：没有方法可以调用超过 255 个槽位。
//  * 这限制了可以传递给引导方法的静态和动态参数的数量。
//  * 由于可能存在使用 {@code MethodHandle} 组合器的拼接策略，
//  * 我们需要在参数列表中预留一些空槽以捕获临时结果。
//  * 因此，此工厂中的引导方法不接受超过 200 个参数槽。
//  * 需要超过 200 个参数槽进行拼接的用户应将大型拼接表达式拆分为较小的表达式。
//  *
//  * @自 JDK 9 起
//  */
// public final class StringConcatFactory {
//
//     /**
//      * 用于标记普通参数的标签。
//      */
//     private static final char TAG_ARG = '\u0001';
//
//     /**
//      * 用于标记常量的标签。
//      */
//     private static final char TAG_CONST = '\u0002';
//
//     /**
//      * 字符串拼接调用中允许的最大参数槽数。
//      *
//      * 虽然 `indy` 调用最多可以处理 253 个参数槽，
//      * 但我们不使用所有这些槽，以便允许带有 `MethodHandle`
//      * 组合器的策略保留一些参数。
//      */
//     private static final int MAX_INDY_CONCAT_ARG_SLOTS = 200;
//
//     /**
//      * 要使用的拼接策略。有关可能的选项，请参见 {@link Strategy}。
//      * 此选项可以通过 JDK 选项 `-Djava.lang.invoke.stringConcat` 控制。
//      */
//     private static Strategy STRATEGY;
//
//     /**
//      * 默认使用的拼接策略。
//      */
//     private static final Strategy DEFAULT_STRATEGY = Strategy.MH_INLINE_SIZED_EXACT;
//
//     private enum Strategy {
//         /**
//          * 字节码生成器，调用 {@link java.lang.StringBuilder}。
//          */
//         BC_SB,
//
//         /**
//          * 字节码生成器，调用 {@link java.lang.StringBuilder}；
//          * 但尝试估计所需的存储空间。
//          */
//         BC_SB_SIZED,
//
//         /**
//          * 字节码生成器，调用 {@link java.lang.StringBuilder}；
//          * 但准确计算所需的存储空间。
//          */
//         BC_SB_SIZED_EXACT,
//
//         /**
//          * 基于 `MethodHandle` 的生成器，最终调用 {@link java.lang.StringBuilder}。
//          * 此策略还尝试估算所需的存储空间。
//          */
//         MH_SB_SIZED,
//
//         /**
//          * 基于 `MethodHandle` 的生成器，最终调用 {@link java.lang.StringBuilder}。
//          * 此策略还精确估计所需的存储空间。
//          */
//         MH_SB_SIZED_EXACT,
//
//         /**
//          * 基于 `MethodHandle` 的生成器，从参数中构建自己的 `byte[]` 数组。
//          * 它准确计算所需的存储空间。
//          */
//         MH_INLINE_SIZED_EXACT
//     }
//
//     /**
//      * 启用调试：这可能会打印调试消息，执行额外的检查（这对性能有负面影响）等。
//      */
//     private static final boolean DEBUG;
//
//     /**
//      * 启用策略存根缓存。通过重用生成的代码，这可能会改进链接时间，但会增加轮廓污染的风险。
//      */
//     private static final boolean CACHE_ENABLE;
//
//     private static final ConcurrentMap<Key, MethodHandle> CACHE;
//
//     /**
//      * 将生成的类转储到磁盘，以便进行调试。
//      */
//     private static final ProxyClassesDumper DUMPER;
//
//     static {
//         // 在此静态初始化期间，如果我们需要重新访问 StringConcatFactory，
//         // 确保我们有合理的默认值来正确完成静态初始化。
//         // 之后，实际用户将使用我们从属性中读取的适当值。
//         STRATEGY = DEFAULT_STRATEGY;
//         // CACHE_ENABLE = false; // 默认设置
//         // CACHE = null;         // 默认设置
//         // DEBUG = false;        // 默认设置
//         // DUMPER = null;        // 默认设置
//
//         Properties props = GetPropertyAction.privilegedGetProperties();
//         final String strategy = props.getProperty("java.lang.invoke.stringConcat");
//         CACHE_ENABLE = Boolean.parseBoolean(props.getProperty("java.lang.invoke.stringConcat.cache"));
//         DEBUG = Boolean.parseBoolean(props.getProperty("java.lang.invoke.stringConcat.debug"));
//         final String dumpPath = props.getProperty("java.lang.invoke.stringConcat.dumpClasses");
//
//         STRATEGY = (strategy == null) ? DEFAULT_STRATEGY : Strategy.valueOf(strategy);
//         CACHE = CACHE_ENABLE ? new ConcurrentHashMap<>() : null;
//         DUMPER = (dumpPath == null) ? null : ProxyClassesDumper.getInstance(dumpPath);
//     }
//
//     /**
//      * 缓存键由以下组成：
//      *   - 类名，用于区分存根，以避免过度共享
//      *   - 方法类型，描述拼接的动态参数
//      *   - 配方，描述常量和拼接结构
//      */
//     private static final class Key {
//         final String className;
//         final MethodType mt;
//         final Recipe recipe;
//
//         public Key(String className, MethodType mt, Recipe recipe) {
//             this.className = className;
//             this.mt = mt;
//             this.recipe = recipe;
//         }
//
//         @Override
//         public boolean equals(Object o) {
//             if (this == o) return true;
//             if (o == null || getClass() != o.getClass()) return false;
//
//             Key key = (Key) o;
//
//             if (!className.equals(key.className)) return false;
//             if (!mt.equals(key.mt)) return false;
//             if (!recipe.equals(key.recipe)) return false;
//             return true;
//         }
//
//         @Override
//         public int hashCode() {
//             int result = className.hashCode();
//             result = 31 * result + mt.hashCode();
//             result = 31 * result + recipe.hashCode();
//             return result;
//         }
//     }
//
//     /**
//      * 解析配方字符串，并为生成器策略生成 {@link StringConcatFactory.RecipeElement} 的可遍历集合。
//      * 特别是，此类会从配方和其他静态参数中解析出常量。
//      */
//     private static final class Recipe {
//         private final List<RecipeElement> elements;
//
//         public Recipe(String src, Object[] constants) {
//             List<RecipeElement> el = new ArrayList<>();
//
//             int constC = 0;
//             int argC = 0;
//
//             StringBuilder acc = new StringBuilder();
//
//             for (int i = 0; i < src.length(); i++) {
//                 char c = src.charAt(i);
//
//                 if (c == TAG_CONST || c == TAG_ARG) {
//                     // 检测到特殊标签，首先将所有累计的字符作为常量输出：
//                     if (acc.length() > 0) {
//                         el.add(new RecipeElement(acc.toString()));
//                         acc.setLength(0);
//                     }
//                     if (c == TAG_CONST) {
//                         Object cnst = constants[constC++];
//                         el.add(new RecipeElement(cnst));
//                     } else if (c == TAG_ARG) {
//                         el.add(new RecipeElement(argC++));
//                     }
//                 } else {
//                     // 非特殊字符，嵌入在配方中的常量
//                     acc.append(c);
//                 }
//             }
//
//             // 将剩余字符作为常量输出：
//             if (acc.length() > 0) {
//                 el.add(new RecipeElement(acc.toString()));
//             }
//
//             elements = el;
//         }
//
//         public List<RecipeElement> getElements() {
//             return elements;
//         }
//
//         @Override
//         public boolean equals(Object o) {
//             if (this == o) return true;
//             if (o == null || getClass() != o.getClass()) return false;
//
//             Recipe recipe = (Recipe) o;
//             return elements.equals(recipe.elements);
//         }
//
//         @Override
//         public int hashCode() {
//             return elements.hashCode();
//         }
//     }
//
//     private static final class RecipeElement {
//         private final String value;
//         private final int argPos;
//         private final char tag;
//
//         public RecipeElement(Object cnst) {
//             this.value = String.valueOf(Objects.requireNonNull(cnst));
//             this.argPos = -1;
//             this.tag = TAG_CONST;
//         }
//
//         public RecipeElement(int arg) {
//             this.value = null;
//             this.argPos = arg;
//             this.tag = TAG_ARG;
//         }
//
//         public String getValue() {
//             assert (tag == TAG_CONST);
//             return value;
//         }
//
//         public int getArgPos() {
//             assert (tag == TAG_ARG);
//             return argPos;
//         }
//
//         public char getTag() {
//             return tag;
//         }
//
//         @Override
//         public boolean equals(Object o) {
//             if (this == o) return true;
//             if (o == null || getClass() != o.getClass()) return false;
//
//             RecipeElement that = (RecipeElement) o;
//
//             if (this.tag != that.tag) return false;
//             if (this.tag == TAG_CONST && (!value.equals(that.value))) return false;
//             if (this.tag == TAG_ARG && (argPos != that.argPos)) return false;
//             return true;
//         }
//
//         @Override
//         public int hashCode() {
//             return (int) tag;
//         }
//     }
//
//     // StringConcatFactory 引导方法对启动过程敏感，
//     // 在 `java.lang.invokeBootstrapMethodInvoker` 中可能有特例处理，
//     // 确保方法调用类型信息精确，以避免生成运行时检查代码。
//     // 在此处进行的任何更改或添加应适当反映在相关部分。
//
//     // (字符串拼接方法的具体内容此处不再继续翻译)
//     /**
//      * 用于创建优化的字符串拼接方法，这些方法可以有效地拼接已知数量和类型的参数，
//      * 可能在拼接过程中进行类型适配和部分参数求值。
//      * 通常作为 {@code invokedynamic} 调用点的<em>引导方法</em>使用，
//      * 以支持 Java 编程语言中的<em>字符串拼接</em>功能。
//      *
//      * <p>当从此方法返回的 {@code CallSite} 的目标被调用时，
//      * 它将返回字符串拼接的结果，使用所有传递给链接方法的函数参数作为拼接的输入。
//      * 目标签名由 {@code concatType} 给出。
//      * 对于一个接收以下参数的目标：
//      * <ul>
//      *     <li>零个输入，拼接结果为空字符串；</li>
//      *     <li>一个输入，拼接结果为该单个输入按 JLS 5.1.11 "字符串转换" 的规则转换后的值；</li>
//      *     <li>两个或多个输入，输入按 JLS 15.18.1 "字符串拼接操作符 +" 的要求拼接，
//      *         输入按 JLS 5.1.11 "字符串转换" 的规则转换，并从左至右组合。</li>
//      * </ul>
//      *
//      * <p>假设链接参数如下：
//      * <ul>
//      *     <li>{@code concatType}，描述 {@code CallSite} 的签名</li>
//      * </ul>
//      *
//      * <p>那么以下链接不变量必须成立：
//      * <ul>
//      *     <li>{@code concatType} 中的参数槽数小于或等于 200</li>
//      *     <li>{@code concatType} 中的返回类型必须是 {@link java.lang.String} 的可赋值类型</li>
//      * </ul>
//      *
//      * @param lookup   表示具有调用方访问权限的查找上下文。
//      *                 具体来说，查找上下文必须具有
//      *                 <a href="MethodHandles.Lookup.html#privacc">私有访问</a>权限。
//      *                 当与 {@code invokedynamic} 一起使用时，JVM 会自动堆叠此项。
//      * @param name     要实现的方法名称。此名称是任意的，对于此链接方法没有实际意义。
//      *                 当与 {@code invokedynamic} 一起使用时，此名称由 {@code InvokeDynamic} 结构的
//      *                 {@code NameAndType} 提供，并由 JVM 自动堆叠。
//      * @param concatType 期望的 {@code CallSite} 签名。
//      *                   参数类型表示动态拼接参数的类型，返回类型始终可赋值为 {@link java.lang.String}。
//      *                   当与 {@code invokedynamic} 一起使用时，
//      *                   此项由 {@code InvokeDynamic} 结构的 {@code NameAndType} 提供，并由 JVM 自动堆叠。
//      * @return 一个 `CallSite`，其目标可用于执行字符串拼接，具有 {@code concatType} 所描述的动态拼接参数。
//      * @throws StringConcatException 如果违反了此处描述的任何链接不变量，或查找上下文无私有访问权限。
//      * @throws NullPointerException 如果任何传入参数为 null。
//      *                              当使用 invokedynamic 调用引导方法时，这种情况不会发生。
//      *
//      * @jls  5.1.11 String Conversion
//      * @jls 15.18.1 String Concatenation Operator +
//      */
//     public static CallSite makeConcat(MethodHandles.Lookup lookup,
//                                       String name,
//                                       MethodType concatType) throws StringConcatException {
//         if (DEBUG) {
//             System.out.println("StringConcatFactory " + STRATEGY + " is here for " + concatType);
//         }
//
//         return doStringConcat(lookup, name, concatType, true, null);
//     }
//
// /**
//  * 用于创建优化的字符串拼接方法，这些方法可以有效地拼接已知数量和类型的参数，
//  * 可能在拼接过程中进行类型适配和部分参数求值。
//  * 通常作为 {@code invokedynamic} 调用点的<em>引导方法</em>使用，
//  * 以支持 Java 编程语言中的<em>字符串拼接</em>功能。
//  *
//  * <p>当从此方法返回的 {@code CallSite} 的目标被调用时，
//  * 它将返回字符串拼接的结果，使用所有传递给链接方法的函数参数和常量作为拼接的输入。
//  * 目标签名由 {@code concatType} 给出，不包括常量。
//  * 对于一个接收以下参数的目标：
//  * <ul>
//  *     <li>零个输入，拼接结果为空字符串；</li>
//  *     <li>一个输入，拼接结果为该单个输入按 JLS 5.1.11 "字符串转换" 的规则转换后的值；</li>
//  *     <li>两个或多个输入，输入按 JLS 15.18.1 "字符串拼接操作符 +" 的要求拼接，
//  *         输入按 JLS 5.1.11 "字符串转换" 的规则转换，并从左至右组合。</li>
//  * </ul>
//  *
//  * <p>拼接<em>配方</em>是一个字符串描述，用于从参数和常量中构建拼接字符串。
//  * 配方从左到右处理，每个字符代表拼接的一个输入。配方字符含义如下：
//  * <ul>
//  *
//  *   <li><em>\1 (Unicode 0001)</em>：一个普通参数。
//  *       此输入通过动态参数传递，并在调用拼接方法时提供。此输入可以为空。</li>
//  *
//  *   <li><em>\2 (Unicode 0002):</em> 一个常量。
//  *       此输入通过静态引导参数传递。该常量可以是任何常量池中的值。
//  *       如有必要，工厂会调用 {@code toString} 进行一次性字符串转换。</li>
//  *
//  *   <li><em>其他任何字符值：</em> 一个单字符常量。</li>
//  * </ul>
//  *
//  * <p>假设链接参数如下：
//  *
//  * <ul>
//  *   <li>{@code concatType}，描述 {@code CallSite} 签名</li>
//  *   <li>{@code recipe}，描述字符串拼接的配方</li>
//  *   <li>{@code constants}，常量的可变参数数组</li>
//  * </ul>
//  *
//  * <p>那么以下链接不变量必须成立：
//  * <ul>
//  *   <li>{@code concatType} 中的参数槽数小于或等于 200</li>
//  *   <li>{@code concatType} 中的参数计数等于 {@code recipe} 中 \1 标签的数量</li>
//  *   <li>{@code concatType} 中的返回类型必须是 {@link java.lang.String} 的可赋值类型，并匹配返回的 {@link MethodHandle} 的返回类型</li>
//  *   <li>{@code constants} 中的元素数量等于 {@code recipe} 中 \2 标签的数量</li>
//  * </ul>
//  *
//  * @param lookup    表示具有调用方访问权限的查找上下文。
//  *                  具体来说，查找上下文必须具有
//  *                  <a href="MethodHandles.Lookup.html#privacc">私有访问</a>权限。
//  *                  当与 {@code invokedynamic} 一起使用时，JVM 会自动堆叠此项。
//  * @param name      要实现的方法名称。此名称是任意的，对于此链接方法没有实际意义。
//  *                  当与 {@code invokedynamic} 一起使用时，此名称由 {@code InvokeDynamic} 结构的
//  *                  {@code NameAndType} 提供，并由 JVM 自动堆叠。
//  * @param concatType 期望的 {@code CallSite} 签名。参数类型表示动态拼接参数的类型；
//  *                   返回类型始终可赋值为 {@link java.lang.String}。
//  *                   当与 {@code invokedynamic} 一起使用时，此项由 {@code InvokeDynamic} 结构的
//  *                   {@code NameAndType} 提供，并由 JVM 自动堆叠。
//  * @param recipe    描述拼接方式的配方。
//  * @param constants 表示传递给链接方法的常量的可变参数。
//  * @return 一个 `CallSite`，其目标可用于执行字符串拼接，具有 {@code concatType} 所描述的动态拼接参数。
//  * @throws StringConcatException 如果违反了此处描述的任何链接不变量，或查找上下文无私有访问权限。
//  * @throws NullPointerException 如果任何传入参数为 null，或 {@code recipe} 中的任何常量为 null。
//  *                              当使用 invokedynamic 调用引导方法时，这种
//  *                              情况不会发生。
//  *
//  * @apiNote 代码生成器有三种不同的方式来处理字符串拼接表达式中的常量字符串操作数 S。
//  *          第一种方式是将 S 作为引用物化（使用 ldc）并作为普通参数传递（配方为 '\1'）。
//  *          第二种方式是将 S 存储在常量池中并作为常量传递（配方为 '\2'）。
//  *          最后一种方式是，如果 S 不包含配方标签字符（'\1' 或 '\2'），
//  *          则可以将 S 直接插入到配方中，从而将其字符插入到结果中。
//  *
//  * @jls  5.1.11 字符串转换
//  * @jls 15.18.1 字符串拼接操作符 +
//  */
// public static CallSite makeConcatWithConstants(MethodHandles.Lookup lookup,
//                                                String name,
//                                                MethodType concatType,
//                                                String recipe,
//                                                Object... constants) throws StringConcatException {
//     if (DEBUG) {
//         System.out.println("StringConcatFactory " + STRATEGY + " is here for " + concatType + ", {" + recipe + "}, " + Arrays.toString(constants));
//     }
//
//     return doStringConcat(lookup, name, concatType, false, recipe, constants);
// }
//
//     private static CallSite doStringConcat(MethodHandles.Lookup lookup,
//                                            String name,
//                                            MethodType concatType,
//                                            boolean generateRecipe,
//                                            String recipe,
//                                            Object... constants) throws StringConcatException {
//         Objects.requireNonNull(lookup, "Lookup is null");
//         Objects.requireNonNull(name, "Name is null");
//         Objects.requireNonNull(concatType, "Concat type is null");
//         Objects.requireNonNull(constants, "Constants are null");
//
//         for (Object o : constants) {
//             Objects.requireNonNull(o, "Cannot accept null constants");
//         }
//
//         if ((lookup.lookupModes() & MethodHandles.Lookup.PRIVATE) == 0) {
//             throw new StringConcatException("Invalid caller: " + lookup.lookupClass().getName());
//         }
//
//         int cCount = 0;
//         int oCount = 0;
//         if (generateRecipe) {
//             // 使用生成的配方复用拼接生成代码
//             char[] value = new char[concatType.parameterCount()];
//             Arrays.fill(value, TAG_ARG);
//             recipe = new String(value);
//             oCount = concatType.parameterCount();
//         } else {
//             Objects.requireNonNull(recipe, "Recipe is null");
//
//             for (int i = 0; i < recipe.length(); i++) {
//                 char c = recipe.charAt(i);
//                 if (c == TAG_CONST) cCount++;
//                 if (c == TAG_ARG)   oCount++;
//             }
//         }
//
//         if (oCount != concatType.parameterCount()) {
//             throw new StringConcatException(
//                     "拼接参数数量不匹配：配方需要 " + oCount + " 个参数，但签名提供了 " + concatType.parameterCount());
//         }
//
//         if (cCount != constants.length) {
//             throw new StringConcatException(
//                     "拼接常量数量不匹配：配方需要 " + cCount + " 个常量，但仅传递了 " + constants.length);
//         }
//
//         if (!concatType.returnType().isAssignableFrom(String.class)) {
//             throw new StringConcatException(
//                     "返回类型应与 String 兼容，但实际类型为 " + concatType.returnType());
//         }
//
//        // if (concatType.parameterSlotCount() > MAX_INDY_CONCAT_ARG_SLOTS) {
//        //     throw new StringConcatException("拼接参数槽位过多：" +
//        //             concatType.parameterSlotCount() +
//        //             "，仅能接受 " +
//        //             MAX_INDY_CONCAT_ARG_SLOTS);
//        // }
//
//         String className = getClassName(lookup.lookupClass());
//         MethodType mt = adaptType(concatType);
//         Recipe rec = new Recipe(recipe, constants);
//
//         MethodHandle mh;
//         if (CACHE_ENABLE) {
//             Key key = new Key(className, mt, rec);
//             mh = CACHE.get(key);
//             if (mh == null) {
//                 mh = generate(lookup, className, mt, rec);
//                 CACHE.put(key, mh);
//             }
//         } else {
//             mh = generate(lookup, className, mt, rec);
//         }
//         return new ConstantCallSite(mh.asType(concatType));
//     }
//
//     /**
//      * 适配方法类型以匹配我们将要使用的 API。
//      *
//      * 这将从签名中去除具体的类类型，从而防止在缓存拼接存根时发生类泄漏。
//      *
//      * @param args 实际的参数类型
//      * @return 策略将使用的参数类型
//      */
//     private static MethodType adaptType(MethodType args) {
//         Class<?>[] ptypes = null;
//         for (int i = 0; i < args.parameterCount(); i++) {
//             Class<?> ptype = args.parameterType(i);
//             if (!ptype.isPrimitive() && ptype != String.class && ptype != Object.class) { // 截断为 Object
//                 if (ptypes == null) {
//                     ptypes = args.parameterArray();
//                 }
//                 ptypes[i] = Object.class;
//             }
//             // 其他原始类型、String 或 Object（保持不变）
//         }
//         return (ptypes != null)
//                 ? MethodType.methodType(args.returnType(), ptypes)
//                 : args;
//     }
//
//     /**
//      * 获取类名，如果缓存启用，则使用缓存前缀。
//      *
//      * @param hostClass 当前使用的类
//      * @return 缓存的类名前缀或完整类名
//      * @throws StringConcatException 在无法找到类名时抛出异常
//      */
//     private static String getClassName(Class<?> hostClass) throws StringConcatException {
//     /*
//        当启用缓存时，我们希望缓存尽可能多的数据。
//
//        但是存在两个特点：
//
//        a) 生成的类应当保留在与宿主类相同的包内，以允许 Unsafe.defineAnonymousClass 的访问控制正确生效。
//           在访问具有非特权调用者的 VM 匿名类时，JDK 可能会选择抛出 IllegalAccessException，参见 JDK-8058575。
//
//        b) 如果我们将存根标记为某个前缀，比如基于包名生成前缀来实现 (a)，那么我们可以在其他包中使用该存根。
//           但对于无准备的用户和性能分析工具来说，调用栈的追踪将会非常混乱：无论哪个存根胜出，将在所有相似的调用点中被链接。
//
//        因此，我们将类的前缀设置为匹配宿主类的包名，并使用前缀作为缓存键。这仅影响 BC_* 策略，且仅在启用缓存时生效。
//     */
//
//         switch (STRATEGY) {
//             case BC_SB:
//             case BC_SB_SIZED:
//             case BC_SB_SIZED_EXACT: {
//                 if (CACHE_ENABLE) {
//                     String pkgName = hostClass.getPackageName();
//                     return (pkgName != null && !pkgName.isEmpty() ? pkgName.replace('.', '/') + "/" : "") + "Stubs$$StringConcat";
//                 } else {
//                     return hostClass.getName().replace('.', '/') + "$$StringConcat";
//                 }
//             }
//             case MH_SB_SIZED:
//             case MH_SB_SIZED_EXACT:
//             case MH_INLINE_SIZED_EXACT:
//                 // MethodHandle 策略不需要类名。
//                 return "";
//             default:
//                 throw new StringConcatException("未实现的拼接策略：" + STRATEGY);
//         }
//     }
//
//     /**
//      * 根据 `lookup`、`className`、`mt`（方法类型）和 `recipe`（拼接配方）
//      * 生成相应的 `MethodHandle` 。
//      *
//      * @param lookup      当前的 `Lookup` 实例，用于查找方法
//      * @param className   目标类名
//      * @param mt          方法类型
//      * @param recipe      拼接配方
//      * @return 生成的 `MethodHandle`
//      * @throws StringConcatException 如果生成方法失败则抛出异常
//      */
//     private static MethodHandle generate(MethodHandles.Lookup lookup, String className, MethodType mt, Recipe recipe) throws StringConcatException {
//         try {
//             switch (STRATEGY) {
//                 case BC_SB:
//                     return BytecodeStringBuilderStrategy.generate(lookup, className, mt, recipe, Mode.DEFAULT);
//                 case BC_SB_SIZED:
//                     return BytecodeStringBuilderStrategy.generate(lookup, className, mt, recipe, Mode.SIZED);
//                 case BC_SB_SIZED_EXACT:
//                     return BytecodeStringBuilderStrategy.generate(lookup, className, mt, recipe, Mode.SIZED_EXACT);
//                 case MH_SB_SIZED:
//                     return MethodHandleStringBuilderStrategy.generate(mt, recipe, Mode.SIZED);
//                 case MH_SB_SIZED_EXACT:
//                     return MethodHandleStringBuilderStrategy.generate(mt, recipe, Mode.SIZED_EXACT);
//                 case MH_INLINE_SIZED_EXACT:
//                     return MethodHandleInlineCopyStrategy.generate(mt, recipe);
//                 default:
//                     throw new StringConcatException("未实现的拼接策略：" + STRATEGY);
//             }
//         } catch (Error | StringConcatException e) {
//             // 直接传递任何错误或已有的 StringConcatException 异常
//             throw e;
//         } catch (Throwable t) {
//             throw new StringConcatException("生成器失败", t);
//         }
//     }
//
//     /**
//      * 枚举 `Mode` 用于指定拼接模式。
//      *
//      * - DEFAULT：不估算或精确拼接大小。
//      * - SIZED：尝试估算所需的容量。
//      * - SIZED_EXACT：精确计算所需的容量。
//      */
//     private enum Mode {
//         DEFAULT(false, false),
//         SIZED(true, false),
//         SIZED_EXACT(true, true);
//
//         private final boolean sized;
//         private final boolean exact;
//
//         Mode(boolean sized, boolean exact) {
//             this.sized = sized;
//             this.exact = exact;
//         }
//
//         boolean isSized() {
//             return sized;
//         }
//
//         boolean isExact() {
//             return exact;
//         }
//     }
//
//     /**
//      * 字节码 StringBuilder 策略。
//      *
//      * <p>该策略以三种模式运行，由 {@link Mode} 控制。
//      *
//      * <p><b>{@link Strategy#BC_SB}: “字节码 StringBuilder”。</b>
//      *
//      * <p>此策略生成包含与 javac 生成的 `StringBuilder` 链相同的字节码。
//      * 该策略仅使用公共 API，并作为当前 JDK 行为的基线。
//      * 换句话说，该策略将 javac 生成的字节码移动到运行时。
//      * 生成的字节码通过 `Unsafe.defineAnonymousClass` 加载，但调用者类来自 BSM（即 `Bootstrap Method`）——
//      * 换句话说，保护保证继承自 `invokedynamic` 的初始调用方法。
//      * 这意味着，除非是 JDK 内部使用，否则字节码需要被验证。
//      *
//      * <p><b>{@link Strategy#BC_SB_SIZED}: “带有容量的字节码 StringBuilder”。</b>
//      *
//      * <p>此策略与 {@link Strategy#BC_SB} 类似，但它还会尝试估算 `StringBuilder` 的容量，
//      * 以便能够接受所有参数而不进行扩展。
//      * 此策略仅进行预估：它仅对已知类型（例如原始类型和 `String`）进行空间估算，
//      * 但不对其他类型进行转换。
//      * 因此，容量估算可能会出错，并且在实际拼接时 `StringBuilder` 可能需要透明地扩展或修剪。
//      * 虽然这不会构成正确性问题（因为 BC_SB 必须这样做），但这确实会带来潜在的性能问题。
//      *
//      * <p><b>{@link Strategy#BC_SB_SIZED_EXACT}: “带有精确容量的字节码 StringBuilder”。</b>
//      *
//      * <p>此策略在 {@link Strategy#BC_SB_SIZED} 的基础上改进，
//      * 通过先将所有参数转换为 `String` 以获得 `StringBuilder` 的精确容量。
//      * 转换通过公共的 `String.valueOf` 和/或 `Object.toString` 方法完成，不涉及任何私有 `String` API。
//      */
//     private static final class BytecodeStringBuilderStrategy {
//         static final Unsafe UNSAFE = Unsafe.getUnsafe();
//         static final int CLASSFILE_VERSION = 52;
//         static final String METHOD_NAME = "concat";
//
//         private BytecodeStringBuilderStrategy() {
//             // 不允许实例化
//         }
//
//         private static MethodHandle generate(MethodHandles.Lookup lookup, String className, MethodType args, Recipe recipe, Mode mode) throws Exception {
//             ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
//
//             cw.visit(CLASSFILE_VERSION,
//                     ACC_SUPER + ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC,
//                     className,  // Unsafe.defineAnonymousClass 将附加一个唯一的 ID
//                     null,
//                     "java/lang/Object",
//                     null
//             );
//
//             MethodVisitor mv = cw.visitMethod(
//                     ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
//                     METHOD_NAME,
//                     args.toMethodDescriptorString(),
//                     null,
//                     null);
//
//             mv.visitAnnotation("Ljdk/internal/vm/annotation/ForceInline;", true);
//             mv.visitCode();
//
//             Class<?>[] arr = args.parameterArray();
//             boolean[] guaranteedNonNull = new boolean[arr.length];
//
//             if (mode.isExact()) {
//             /*
//                 在精确模式中，需要将所有参数转换为它们的字符串表示，
//                 这样就可以精确计算它们的字符串大小。
//                 在此处不能使用私有方法处理原始类型，因此需要进行转换。
//
//                 我们还记录了转换结果的非空保证。
//                 `String.valueOf` 为我们执行空检查。
//                 唯一需要注意的特殊情况是 `String.valueOf(Object)` 自身返回 `null` 的情况。
//
//                 此外，如果发生任何转换，则传入参数的槽位索引与最终的局部映射不同。
//                 唯一可能中断的情况是将双槽 `long`/`double` 参数转换为单槽 `String`。
//                 因此，我们可以通过跟踪修改偏移量来避免此问题，因为任何转换都不会覆盖后续的参数。
//             */
//
//                 int off = 0;
//                 int modOff = 0;
//                 for (int c = 0; c < arr.length; c++) {
//                     Class<?> cl = arr[c];
//                     if (cl == String.class) {
//                         if (off != modOff) {
//                             mv.visitIntInsn(getLoadOpcode(cl), off);
//                             mv.visitIntInsn(ASTORE, modOff);
//                         }
//                     } else {
//                         mv.visitIntInsn(getLoadOpcode(cl), off);
//                         mv.visitMethodInsn(
//                                 INVOKESTATIC,
//                                 "java/lang/String",
//                                 "valueOf",
//                                 getStringValueOfDesc(cl),
//                                 false
//                         );
//                         mv.visitIntInsn(ASTORE, modOff);
//                         arr[c] = String.class;
//                         guaranteedNonNull[c] = cl.isPrimitive();
//                     }
//                     off += getParameterSize(cl);
//                     modOff += getParameterSize(String.class);
//                 }
//             }
//
//             if (mode.isSized()) {
//             /*
//                 在带有容量的模式下（包括精确模式），
//                 使 StringBuilder 的 `append` 链与 `OptimizeStringConcat` 相似是有意义的。
//                 为此，我们需要提前执行空检查，而不是简化 `append` 链的形状。
//             */
//
//                 int off = 0;
//                 for (RecipeElement el : recipe.getElements()) {
//                     switch (el.getTag()) {
//                         case TAG_CONST:
//                             // 保证非空，不需要空检查。
//                             break;
//                         case TAG_ARG:
//                             // 仅对字符串参数需要空检查，并且在上一个阶段未执行隐式空检查时才需要。
//                             // 如果字符串为空，则立即将其替换为 "null" 常量。
//                             // 注意，此处忽略对象，因为我们不会调用 `.length()`。
//                             int ac = el.getArgPos();
//                             Class<?> cl = arr[ac];
//                             if (cl == String.class && !guaranteedNonNull[ac]) {
//                                 Label l0 = new Label();
//                                 mv.visitIntInsn(ALOAD, off);
//                                 mv.visitJumpInsn(IFNONNULL, l0);
//                                 mv.visitLdcInsn("null");
//                                 mv.visitIntInsn(ASTORE, off);
//                                 mv.visitLabel(l0);
//                             }
//                             off += getParameterSize(cl);
//                             break;
//                         default:
//                             throw new StringConcatException("未处理的标签：" + el.getTag());
//                     }
//                 }
//             }
//
//             // 准备 StringBuilder 实例
//             mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//             mv.visitInsn(DUP);
//
//             if (mode.isSized()) {
//             /*
//                 带有容量的模式要求我们遍历参数并估算最终长度。
//                 在精确模式下，这将仅对字符串进行操作。此代码将累积最终长度到栈中。
//             */
//                 int len = 0;
//                 int off = 0;
//
//                 mv.visitInsn(ICONST_0);
//
//                 for (RecipeElement el : recipe.getElements()) {
//                     switch (el.getTag()) {
//                         case TAG_CONST:
//                             len += el.getValue().length();
//                             break;
//                         case TAG_ARG:
//                         /*
//                             如果参数是字符串，则我们可以调用 `.length()`。在带有容量/精确模式下，参数已转换。
//                             如果参数是原始类型，则可以对其字符串表示大小提供一个估计值。
//                         */
//                             Class<?> cl = arr[el.getArgPos()];
//                             if (cl == String.class) {
//                                 mv.visitIntInsn(ALOAD, off);
//                                 mv.visitMethodInsn(
//                                         INVOKEVIRTUAL,
//                                         "java/lang/String",
//                                         "length",
//                                         "()I",
//                                         false
//                                 );
//                                 mv.visitInsn(IADD);
//                             } else if (cl.isPrimitive()) {
//                                 len += estimateSize(cl);
//                             }
//                             off += getParameterSize(cl);
//                             break;
//                         default:
//                             throw new StringConcatException("未处理的标签：" + el.getTag());
//                     }
//                 }
//
//                 // 非零长度的常量，混入
//                 if (len > 0) {
//                     iconst(mv, len);
//                     mv.visitInsn(IADD);
//                 }
//
//                 mv.visitMethodInsn(
//                         INVOKESPECIAL,
//                         "java/lang/StringBuilder",
//                         "<init>",
//                         "(I)V",
//                         false
//                 );
//             } else {
//                 mv.visitMethodInsn(
//                         INVOKESPECIAL,
//                         "java/lang/StringBuilder",
//                         "<init>",
//                         "()V",
//                         false
//                 );
//             }
//
//             // 此时，我们栈中有一个空白的 StringBuilder，用 .append 方法填充它。
//             {
//                 int off = 0;
//                 int modOff = 0;
//                 for (int c = 0; c < arr.length; c++) {
//                     Class<?> cl = arr[c];
//                     if (cl == String.class) {
//                         if (off != modOff) {
//                             mv.visitIntInsn(getLoadOpcode(cl), off);
//                             mv.visitIntInsn(ASTORE, modOff);
//                         }
//                     } else {
//                         mv.visitIntInsn(getLoadOpcode(cl), off);
//                         mv.visitMethodInsn(
//                                 INVOKESTATIC,
//                                 "java/lang/String",
//                                 "valueOf",
//                                 getStringValueOfDesc(cl),
//                                 false
//                         );
//                         mv.visitIntInsn(ASTORE, modOff);
//                         arr[c] = String.class;
//                         guaranteedNonNull[c] = cl.isPrimitive();
//                     }
//                     off += getParameterSize(cl);
//                     modOff += getParameterSize(String.class);
//                 }
//             }
//
//             if (DEBUG && mode.isExact()) {
//             /*
//                 精确性检查将最终的 `StringBuilder.capacity()` 与结果 `String.length()` 进行比较。
//                 如果这些值不一致，则意味着 `StringBuilder` 进行了存储修剪，这破坏了精确策略的目的。
//             */
//
//             /*
//                该检查的逻辑如下：
//
//                  栈状态：       操作：
//                  (SB)              dup, dup
//                  (SB, SB, SB)      capacity()
//                  (int, SB, SB)     swap
//                  (SB, int, SB)     toString()
//                  (S, int, SB)      length()
//                  (int, int, SB)    if_icmpeq
//                  (SB)              <end>
//
//                注意，它在退出时保持与进入时相同的 StringBuilder。
//             */
//
//                 mv.visitInsn(DUP);
//                 mv.visitInsn(DUP);
//
//                 mv.visitMethodInsn(
//                         INVOKEVIRTUAL,
//                         "java/lang/StringBuilder",
//                         "capacity",
//                         "()I",
//                         false
//                 );
//
//                 mv.visitInsn(SWAP);
//
//                 mv.visitMethodInsn(
//                         INVOKEVIRTUAL,
//                         "java/lang/StringBuilder",
//                         "toString",
//                         "()Ljava/lang/String;",
//                         false
//                 );
//
//                 mv.visitMethodInsn(
//                         INVOKEVIRTUAL,
//                         "java/lang/String",
//                         "length",
//                         "()I",
//                         false
//                 );
//
//                 Label l0 = new Label();
//                 mv.visitJumpInsn(IF_ICMPEQ, l0);
//
//                 mv.visitTypeInsn(NEW, "java/lang/AssertionError");
//                 mv.visitInsn(DUP);
//                 mv.visitLdcInsn("精确性检查失败");
//                 mv.visitMethodInsn(INVOKESPECIAL,
//                         "java/lang/AssertionError",
//                         "<init>",
//                         "(Ljava/lang/Object;)V",
//                         false);
//                 mv.visitInsn(ATHROW);
//
//                 mv.visitLabel(l0);
//             }
//
//             mv.visitMethodInsn(
//                     INVOKEVIRTUAL,
//                     "java/lang/StringBuilder",
//                     "toString",
//                     "()Ljava/lang/String;",
//                     false
//             );
//
//             mv.visitInsn(ARETURN);
//
//             mv.visitMaxs(-1, -1);
//             mv.visitEnd();
//             cw.visitEnd();
//
//             byte[] classBytes = cw.toByteArray();
//             try {
//                 Class<?> hostClass = lookup.lookupClass();
//                 Class<?> innerClass = UNSAFE.defineAnonymousClass(hostClass, classBytes, null);
//                 UNSAFE.ensureClassInitialized(innerClass);
//                 dumpIfEnabled(innerClass.getName(), classBytes);
//                 // return MethodHandles.Lookup.IMPL_LOOKUP.findStatic(innerClass, METHOD_NAME, args);
//                 // 替换 去除报错
//                 return null;
//             } catch (Exception e) {
//                 dumpIfEnabled(className + "$$FAILED", classBytes);
//                 throw new StringConcatException("生成类时发生异常", e);
//             }
//         }
//
//         private static void dumpIfEnabled(String name, byte[] bytes) {
//             if (DUMPER != null) {
//                 DUMPER.dumpClass(name, bytes);
//             }
//         }
//
//         private static String getSBAppendDesc(Class<?> cl) {
//             if (cl.isPrimitive()) {
//                 if (cl == Integer.TYPE || cl == Byte.TYPE || cl == Short.TYPE) {
//                     return "(I)Ljava/lang/StringBuilder;";
//                 } else if (cl == Boolean.TYPE) {
//                     return "(Z)Ljava/lang/StringBuilder;";
//                 } else if (cl == Character.TYPE) {
//                     return "(C)Ljava/lang/StringBuilder;";
//                 } else if (cl == Double.TYPE) {
//                     return "(D)Ljava/lang/StringBuilder;";
//                 } else if (cl == Float.TYPE) {
//                     return "(F)Ljava/lang/StringBuilder;";
//                 } else if (cl == Long.TYPE) {
//                     return "(J)Ljava/lang/StringBuilder;";
//                 } else {
//                     throw new IllegalStateException("未处理的原始类型 StringBuilder.append: " + cl);
//                 }
//             } else if (cl == String.class) {
//                 return "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
//             } else {
//                 return "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
//             }
//         }
//
//         private static String getStringValueOfDesc(Class<?> cl) {
//             if (cl.isPrimitive()) {
//                 if (cl == Integer.TYPE || cl == Byte.TYPE || cl == Short.TYPE) {
//                     return "(I)Ljava/lang/String;";
//                 } else if (cl == Boolean.TYPE) {
//                     return "(Z)Ljava/lang/String;";
//                 } else if (cl == Character.TYPE) {
//                     return "(C)Ljava/lang/String;";
//                 } else if (cl == Double.TYPE) {
//                     return "(D)Ljava/lang/String;";
//                 } else if (cl == Float.TYPE) {
//                     return "(F)Ljava/lang/String;";
//                 } else if (cl == Long.TYPE) {
//                     return "(J)Ljava/lang/String;";
//                 } else {
//                     throw new IllegalStateException("未处理的 String.valueOf: " + cl);
//                 }
//             } else if (cl == String.class) {
//                 return "(Ljava/lang/String;)Ljava/lang/String;";
//             } else {
//                 return "(Ljava/lang/Object;)Ljava/lang/String;";
//             }
//         }
//
//         /**
//          * 以下方法来自 `org.objectweb.asm.commons.InstructionAdapter`，属于 ASM 框架的组件之一：
//          * 一个非常小且快速的 Java 字节码操作框架。
//          * 版权所有 (c) 2000-2005 INRIA, 法国电信，保留所有权利。
//          */
//         private static void iconst(MethodVisitor mv, final int cst) {
//             if (cst >= -1 && cst <= 5) {
//                 mv.visitInsn(Opcodes.ICONST_0 + cst);
//             } else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
//                 mv.visitIntInsn(Opcodes.BIPUSH, cst);
//             } else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
//                 mv.visitIntInsn(Opcodes.SIPUSH, cst);
//             } else {
//                 mv.visitLdcInsn(cst);
//             }
//         }
//
//         private static int getLoadOpcode(Class<?> c) {
//             if (c == Void.TYPE) {
//                 throw new InternalError("意外的 void 类型 load 操作码");
//             }
//             return ILOAD + getOpcodeOffset(c);
//         }
//
//         private static int getOpcodeOffset(Class<?> c) {
//             if (c.isPrimitive()) {
//                 if (c == Long.TYPE) {
//                     return 1;
//                 } else if (c == Float.TYPE) {
//                     return 2;
//                 } else if (c == Double.TYPE) {
//                     return 3;
//                 }
//                 return 0;
//             } else {
//                 return 4;
//             }
//         }
//
//         private static int getParameterSize(Class<?> c) {
//             if (c == Void.TYPE) {
//                 return 0;
//             } else if (c == Long.TYPE || c == Double.TYPE) {
//                 return 2;
//             }
//             return 1;
//         }
//     }
//
//
//     private static final class MethodHandleStringBuilderStrategy {
//
//         private MethodHandleStringBuilderStrategy() {
//             // 禁止实例化
//         }
//
//         private static MethodHandle generate(MethodType mt, Recipe recipe, Mode mode) throws Exception {
//             int pc = mt.parameterCount();
//
//             Class<?>[] ptypes = mt.parameterArray();
//             MethodHandle[] filters = new MethodHandle[ptypes.length];
//             for (int i = 0; i < ptypes.length; i++) {
//                 MethodHandle filter;
//                 switch (mode) {
//                     case SIZED:
//                         // 在有尺寸模式中，我们将所有引用和浮点/双精度数转换为String：
//                         // StringBuilder API中没有不同类的特化，且内部会自动将其转换为String。
//                         filter = Stringifiers.forMost(ptypes[i]);
//                         break;
//                     case SIZED_EXACT:
//                         // 在精确模式下，我们将所有内容转换为String：这有助于准确计算存储空间。
//                         filter = Stringifiers.forAny(ptypes[i]);
//                         break;
//                     default:
//                         throw new StringConcatException("不支持的模式");
//                 }
//                 if (filter != null) {
//                     filters[i] = filter;
//                     ptypes[i] = filter.type().returnType();
//                 }
//             }
//
//             MethodHandle[] lengthers = new MethodHandle[pc];
//
//             // 计算长度：常量的长度可以当场计算。
//             // 所有引用参数在以下组合器中均已被过滤为String，因此可以调用通常的String.length()。
//             // 原始值的字符串大小可被估算。
//             int initial = 0;
//             for (RecipeElement el : recipe.getElements()) {
//                 switch (el.getTag()) {
//                     case TAG_CONST:
//                         initial += el.getValue().length();
//                         break;
//                     case TAG_ARG:
//                         final int i = el.getArgPos();
//                         Class<?> type = ptypes[i];
//                         if (type.isPrimitive()) {
//                             MethodHandle est = MethodHandles.constant(int.class, estimateSize(type));
//                             est = MethodHandles.dropArguments(est, 0, type);
//                             lengthers[i] = est;
//                         } else {
//                             lengthers[i] = STRING_LENGTH;
//                         }
//                         break;
//                     default:
//                         throw new StringConcatException("未处理的标签：" + el.getTag());
//                 }
//             }
//
//             // 创建(StringBuilder, <args>)结构用于附加：
//             MethodHandle builder = MethodHandles.dropArguments(MethodHandles.identity(StringBuilder.class), 1, ptypes);
//
//             // 组合append调用。由于应用顺序是反向的，因此以反向顺序处理。
//             List<RecipeElement> elements = recipe.getElements();
//             for (int i = elements.size() - 1; i >= 0; i--) {
//                 RecipeElement el = elements.get(i);
//                 MethodHandle appender;
//                 switch (el.getTag()) {
//                     case TAG_CONST:
//                         MethodHandle mh = appender(adaptToStringBuilder(String.class));
//                         appender = MethodHandles.insertArguments(mh, 1, el.getValue());
//                         break;
//                     case TAG_ARG:
//                         int ac = el.getArgPos();
//                         appender = appender(ptypes[ac]);
//
//                         // 插入虚拟参数以匹配签名中的前缀。
//                         // 实际的appender参数将是ac位置的参数。
//                         if (ac != 0) {
//                             appender = MethodHandles.dropArguments(appender, 1, Arrays.copyOf(ptypes, ac));
//                         }
//                         break;
//                     default:
//                         throw new StringConcatException("未处理的标签：" + el.getTag());
//                 }
//                 builder = MethodHandles.foldArguments(builder, appender);
//             }
//
//             // 构建子树以累加大小并生成一个StringBuilder对象：
//
//             // a) 从接受所有参数的reducer开始，并为初始值预留一个槽。立即注入初始值。
//             //    生成(<ints>)int结构：
//             MethodHandle sum = getReducerFor(pc + 1);
//             MethodHandle adder = MethodHandles.insertArguments(sum, 0, initial);
//
//             // b) 应用lengthers以将参数转换为长度，生成(<args>)int
//             adder = MethodHandles.filterArguments(adder, 0, lengthers);
//
//             // c) 实例化StringBuilder (<args>)int -> (<args>)StringBuilder
//             MethodHandle newBuilder = MethodHandles.filterReturnValue(adder, NEW_STRING_BUILDER);
//
//             // d) 折叠StringBuilder构造器，这会生成(<args>)StringBuilder
//             MethodHandle mh = MethodHandles.foldArguments(builder, newBuilder);
//
//             // 将非原始参数转换为字符串
//             mh = MethodHandles.filterArguments(mh, 0, filters);
//
//             // 将(<args>)StringBuilder转换为(<args>)String
//             if (DEBUG && mode.isExact()) {
//                 mh = MethodHandles.filterReturnValue(mh, BUILDER_TO_STRING_CHECKED);
//             } else {
//                 mh = MethodHandles.filterReturnValue(mh, BUILDER_TO_STRING);
//             }
//
//             return mh;
//         }
//
//         private static MethodHandle getReducerFor(int cnt) {
//             return SUMMERS.computeIfAbsent(cnt, SUMMER);
//         }
//
//         private static MethodHandle appender(Class<?> appendType) {
//             MethodHandle appender = lookupVirtual(MethodHandles.publicLookup(), StringBuilder.class, "append",
//                     StringBuilder.class, adaptToStringBuilder(appendType));
//
//             // appender应返回void，以确保在折叠期间不会更改目标签名
//             MethodType nt = MethodType.methodType(void.class, StringBuilder.class, appendType);
//             return appender.asType(nt);
//         }
//
//         private static String toStringChecked(StringBuilder sb) {
//             String s = sb.toString();
//             if (s.length() != sb.capacity()) {
//                 throw new AssertionError("精确性检查失败：结果长度 = " + s.length() + ", 缓冲区容量 = " + sb.capacity());
//             }
//             return s;
//         }
//
//         private static int sum(int v1, int v2) {
//             return v1 + v2;
//         }
//
//         private static int sum(int v1, int v2, int v3) {
//             return v1 + v2 + v3;
//         }
//
//         private static int sum(int v1, int v2, int v3, int v4) {
//             return v1 + v2 + v3 + v4;
//         }
//
//         private static int sum(int v1, int v2, int v3, int v4, int v5) {
//             return v1 + v2 + v3 + v4 + v5;
//         }
//
//         private static int sum(int v1, int v2, int v3, int v4, int v5, int v6) {
//             return v1 + v2 + v3 + v4 + v5 + v6;
//         }
//
//         private static int sum(int v1, int v2, int v3, int v4, int v5, int v6, int v7) {
//             return v1 + v2 + v3 + v4 + v5 + v6 + v7;
//         }
//
//         private static int sum(int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
//             return v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8;
//         }
//
//         private static int sum(int initial, int[] vs) {
//             int sum = initial;
//             for (int v : vs) {
//                 sum += v;
//             }
//             return sum;
//         }
//
//         private static final ConcurrentMap<Integer, MethodHandle> SUMMERS;
//
//         // 此处的实现非lambda以优化启动时间
//         private static final Function<Integer, MethodHandle> SUMMER = new Function<Integer, MethodHandle>() {
//             @Override
//             public MethodHandle apply(Integer cnt) {
//                 if (cnt == 1) {
//                     return MethodHandles.identity(int.class);
//                 } else if (cnt <= 8) {
//                     // 对于小计数方法，展开一些初始大小，因为可变参数收集器的效率较低
//                     Class<?>[] cls = new Class<?>[cnt];
//                     Arrays.fill(cls, int.class);
//                     // return lookupStatic(Lookup.IMPL_LOOKUP, MethodHandleStringBuilderStrategy.class, "sum", int.class, cls);
//                     return null;
//                 } else {
//                     // return lookupStatic(Lookup.IMPL_LOOKUP, MethodHandleStringBuilderStrategy.class, "sum", int.class, int.class, int[].class)
//                     //         .asCollector(int[].class, cnt - 1);
//                     return null;
//                 }
//             }
//         };
//
//         private static final MethodHandle NEW_STRING_BUILDER, STRING_LENGTH, BUILDER_TO_STRING, BUILDER_TO_STRING_CHECKED;
//
//         static {
//             SUMMERS = new ConcurrentHashMap<>();
//             MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
//             NEW_STRING_BUILDER = lookupConstructor(publicLookup, StringBuilder.class, int.class);
//             STRING_LENGTH = lookupVirtual(publicLookup, String.class, "length", int.class);
//             BUILDER_TO_STRING = lookupVirtual(publicLookup, StringBuilder.class, "toString", String.class);
//             if (DEBUG) {
//                 // BUILDER_TO_STRING_CHECKED = lookupStatic(MethodHandles.Lookup.IMPL_LOOKUP,
//                 //         MethodHandleStringBuilderStrategy.class, "toStringChecked", String.class, StringBuilder.class);
//                 BUILDER_TO_STRING_CHECKED = null;
//             } else {
//                 BUILDER_TO_STRING_CHECKED = null;
//             }
//         }
//
//     }
//
//     /**
//      * <p><b>{@link Strategy#MH_INLINE_SIZED_EXACT}: "MethodHandles inline,
//      * sized exactly".</b>
//      *
//      * <p>该策略模拟了StringBuilder的行为：它在自身上构建字节数组(byte[])，并将其传递给
//      * String构造函数。此策略需要访问一些JDK中的私有API，尤其是只读的
//      * Integer/Long.stringSize方法，用于测量整数的字符长度，以及接受
//      * byte[]数组的私有String构造函数。在特定实现假定String的情况下，
//      * 这种方式可以构建一个非常优化的字符串连接序列。如果JDK发生私有变化，
//      * 则该策略是唯一可能需要移植的策略。
//      */
//     private static final class MethodHandleInlineCopyStrategy {
//         static final Unsafe UNSAFE = Unsafe.getUnsafe();
//
//         private MethodHandleInlineCopyStrategy() {
//             // 禁止实例化
//         }
//
//         static MethodHandle generate(MethodType mt, Recipe recipe) throws Throwable {
//             // 创建filters并获取过滤后的参数类型列表。filters将用于初期将传入的参数转换为可处理的形式
//             // （如Objects -> Strings）。过滤后的参数类型列表将用于后续的组合器。
//             Class<?>[] ptypes = mt.parameterArray();
//             MethodHandle[] filters = null;
//             for (int i = 0; i < ptypes.length; i++) {
//                 MethodHandle filter = Stringifiers.forMost(ptypes[i]);
//                 if (filter != null) {
//                     if (filters == null) {
//                         filters = new MethodHandle[ptypes.length];
//                     }
//                     filters[i] = filter;
//                     ptypes[i] = filter.type().returnType();
//                 }
//             }
//
//             // 开始构建组合树。组合树从(<parameters>)String的形式“开始”，并以String的(helper参数类型)
//             // 完成。组合器从下往上组合，使代码逻辑更复杂。
//             MethodHandle mh;
//
//             mh = MethodHandles.dropArguments(NEW_STRING, 3, ptypes);
//
//             // 逐步添加前置器。这里是从后往前组装字符串，因此“index”是最终索引。
//             for (RecipeElement el : recipe.getElements()) {
//                 mh = MethodHandles.dropArguments(mh, 2, int.class);
//                 switch (el.getTag()) {
//                     case TAG_CONST: {
//                         MethodHandle prepender = MethodHandles.insertArguments(prepender(String.class), 3, el.getValue());
//                         // mh = MethodHandles.foldArguments(mh, 1, prepender, 2, 0, 3);
//                         break;
//                     }
//                     case TAG_ARG: {
//                         int pos = el.getArgPos();
//                         MethodHandle prepender = prepender(ptypes[pos]);
//                         // mh = MethodHandles.foldArguments(mh, 1, prepender, 2, 0, 3, 4 + pos);
//                         break;
//                     }
//                     default:
//                         throw new StringConcatException("Unhandled tag: " + el.getTag());
//                 }
//             }
//
//             // 在参数0位置合并字节数组实例化
//             // mh = MethodHandles.foldArguments(mh, 0, NEW_ARRAY, 1, 2);
//
//             // 开始组合长度和编码的混合器
//             byte initialCoder = INITIAL_CODER;
//             int initialLen = 0;    // 初始长度，以字符为单位
//             for (RecipeElement el : recipe.getElements()) {
//                 switch (el.getTag()) {
//                     case TAG_CONST:
//                         String constant = el.getValue();
//                         initialCoder = (byte) coderMixer(String.class).invoke(initialCoder, constant);
//                         initialLen += constant.length();
//                         break;
//                     case TAG_ARG:
//                         int ac = el.getArgPos();
//
//                         Class<?> argClass = ptypes[ac];
//                         MethodHandle lm = lengthMixer(argClass);
//                         MethodHandle cm = coderMixer(argClass);
//
//                         // 从下往上读取
//                         mh = MethodHandles.dropArguments(mh, 2, int.class, byte.class);
//
//                         // mh = MethodHandles.foldArguments(mh, 0, lm, 2, 4 + ac);
//                         //
//                         // mh = MethodHandles.foldArguments(mh, 0, cm, 2, 3 + ac);
//
//                         break;
//                     default:
//                         throw new StringConcatException("Unhandled tag: " + el.getTag());
//                 }
//             }
//
//             // 插入初始长度和编码
//             mh = MethodHandles.insertArguments(mh, 0, initialLen, initialCoder);
//
//             // 应用filters以转换参数
//             if (filters != null) {
//                 mh = MethodHandles.filterArguments(mh, 0, filters);
//             }
//
//             return mh;
//         }
//
//         @ForceInline
//         private static byte[] newArray(int length, byte coder) {
//             // return (byte[]) UNSAFE.allocateUninitializedArray(byte.class, length << coder);
//             return null;
//         }
//
//         private static MethodHandle prepender(Class<?> cl) {
//             return PREPENDERS.computeIfAbsent(cl, PREPEND);
//         }
//
//         private static MethodHandle coderMixer(Class<?> cl) {
//             return CODER_MIXERS.computeIfAbsent(cl, CODER_MIX);
//         }
//
//         private static MethodHandle lengthMixer(Class<?> cl) {
//             return LENGTH_MIXERS.computeIfAbsent(cl, LENGTH_MIX);
//         }
//
//         // 此方法专为优化启动时间而非lambda化
//         private static final Function<Class<?>, MethodHandle> PREPEND = new Function<Class<?>, MethodHandle>() {
//             @Override
//             public MethodHandle apply(Class<?> c) {
//                 // return lookupStatic(Lookup.IMPL_LOOKUP, STRING_HELPER, "prepend", int.class, int.class, byte[].class, byte.class,
//                 //         Wrapper.asPrimitiveType(c));
//                 return null;
//             }
//         };
//
//         private static final Function<Class<?>, MethodHandle> CODER_MIX = new Function<Class<?>, MethodHandle>() {
//             @Override
//             public MethodHandle apply(Class<?> c) {
//                 // return lookupStatic(Lookup.IMPL_LOOKUP, STRING_HELPER, "mixCoder", byte.class, byte.class,
//                 //         Wrapper.asPrimitiveType(c));
//                 return null;
//             }
//         };
//
//         private static final Function<Class<?>, MethodHandle> LENGTH_MIX = new Function<Class<?>, MethodHandle>() {
//             @Override
//             public MethodHandle apply(Class<?> c) {
//                 // return lookupStatic(Lookup.IMPL_LOOKUP, STRING_HELPER, "mixLen", int.class, int.class,
//                 //         Wrapper.asPrimitiveType(c));
//                 return null;
//             }
//         };
//
//         private static final MethodHandle NEW_STRING;
//         private static final MethodHandle NEW_ARRAY;
//         private static final ConcurrentMap<Class<?>, MethodHandle> PREPENDERS;
//         private static final ConcurrentMap<Class<?>, MethodHandle> LENGTH_MIXERS;
//         private static final ConcurrentMap<Class<?>, MethodHandle> CODER_MIXERS;
//         private static final byte INITIAL_CODER;
//         static final Class<?> STRING_HELPER;
//
//         static {
//             try {
//                 STRING_HELPER = Class.forName("java.lang.StringConcatHelper");
//                 // MethodHandle initCoder = lookupStatic(MethodHandles.Lookup.IMPL_LOOKUP, STRING_HELPER, "initialCoder", byte.class);
//                 // INITIAL_CODER = (byte) initCoder.invoke();
//                 INITIAL_CODER=0;
//             } catch (Throwable e) {
//                 throw new AssertionError(e);
//             }
//
//             PREPENDERS = new ConcurrentHashMap<>();
//             LENGTH_MIXERS = new ConcurrentHashMap<>();
//             CODER_MIXERS = new ConcurrentHashMap<>();
//
//             // NEW_STRING = lookupStatic(MethodHandles.Lookup.IMPL_LOOKUP, STRING_HELPER, "newString", String.class, byte[].class, int.class, byte.class);
//             // NEW_ARRAY  = lookupStatic(MethodHandles.Lookup.IMPL_LOOKUP, MethodHandleInlineCopyStrategy.class, "newArray", byte[].class, int.class, byte.class);
//             NEW_STRING=null;
//             NEW_ARRAY=null;
//         }
//     }
//
//     /**
//      * 公共接口，用于公有的“stringify”方法。此类方法的格式为`String apply(T obj)`，通常
//      * 根据参数类型代理到{@code String.valueOf}方法。
//      */
//     private static final class Stringifiers {
//         private Stringifiers() {
//             // 禁止实例化
//         }
//
//         private static class StringifierMost extends ClassValue<MethodHandle> {
//             @Override
//             protected MethodHandle computeValue(Class<?> cl) {
//                 if (cl == String.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, Object.class);
//                 } else if (cl == float.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, float.class);
//                 } else if (cl == double.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, double.class);
//                 } else if (!cl.isPrimitive()) {
//                     MethodHandle mhObject = lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, Object.class);
//
//                     // 这里需要额外的转换，因为String.valueOf(Object)可能返回null。
//                     // Java中字符串转换规则要求此时应返回“null”字符串。
//                     // 通过再次应用valueOf方法可以轻松实现此操作。
//                     return MethodHandles.filterReturnValue(mhObject,
//                             mhObject.asType(MethodType.methodType(String.class, String.class)));
//                 }
//
//                 return null;
//             }
//         }
//
//         private static class StringifierAny extends ClassValue<MethodHandle> {
//             @Override
//             protected MethodHandle computeValue(Class<?> cl) {
//                 if (cl == byte.class || cl == short.class || cl == int.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, int.class);
//                 } else if (cl == boolean.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, boolean.class);
//                 } else if (cl == char.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, char.class);
//                 } else if (cl == long.class) {
//                     return lookupStatic(MethodHandles.publicLookup(), String.class, "valueOf", String.class, long.class);
//                 } else {
//                     MethodHandle mh = STRINGIFIERS_MOST.get(cl);
//                     if (mh != null) {
//                         return mh;
//                     } else {
//                         throw new IllegalStateException("未知类：" + cl);
//                     }
//                 }
//             }
//         }
//
//         private static final ClassValue<MethodHandle> STRINGIFIERS_MOST = new StringifierMost();
//         private static final ClassValue<MethodHandle> STRINGIFIERS_ANY = new StringifierAny();
//
//         /**
//          * 返回一个适用于引用类型和浮点类型的字符串化方法句柄。
//          * 对于其他原始类型总是返回null。
//          *
//          * @param t 要字符串化的类
//          * @return 字符串化方法句柄；如果不可用，则为null
//          */
//         static MethodHandle forMost(Class<?> t) {
//             return STRINGIFIERS_MOST.get(t);
//         }
//
//         /**
//          * 返回适用于任何类型的字符串化方法句柄。不会返回null。
//          *
//          * @param t 要字符串化的类
//          * @return 字符串化方法句柄
//          */
//         static MethodHandle forAny(Class<?> t) {
//             return STRINGIFIERS_ANY.get(t);
//         }
//     }
//
//     /* ------------------------------- 公共实用方法 ------------------------------------ */
//
//     static MethodHandle lookupStatic(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> rtype, Class<?>... ptypes) {
//         try {
//             return lookup.findStatic(refc, name, MethodType.methodType(rtype, ptypes));
//         } catch (NoSuchMethodException | IllegalAccessException e) {
//             throw new AssertionError(e);
//         }
//     }
//
//     static MethodHandle lookupVirtual(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> rtype, Class<?>... ptypes) {
//         try {
//             return lookup.findVirtual(refc, name, MethodType.methodType(rtype, ptypes));
//         } catch (NoSuchMethodException | IllegalAccessException e) {
//             throw new AssertionError(e);
//         }
//     }
//
//     static MethodHandle lookupConstructor(MethodHandles.Lookup lookup, Class<?> refc, Class<?> ptypes) {
//         try {
//             return lookup.findConstructor(refc, MethodType.methodType(void.class, ptypes));
//         } catch (NoSuchMethodException | IllegalAccessException e) {
//             throw new AssertionError(e);
//         }
//     }
//
//     static int estimateSize(Class<?> cl) {
//         if (cl == Integer.TYPE) {
//             return 11; // "-2147483648"
//         } else if (cl == Boolean.TYPE) {
//             return 5; // "false"
//         } else if (cl == Byte.TYPE) {
//             return 4; // "-128"
//         } else if (cl == Character.TYPE) {
//             return 1; // duh
//         } else if (cl == Short.TYPE) {
//             return 6; // "-32768"
//         } else if (cl == Double.TYPE) {
//             return 26; // 应不超过此值，参见FloatingDecimal.BinaryToASCIIBuffer.buffer
//         } else if (cl == Float.TYPE) {
//             return 26; // 应不超过此值，参见FloatingDecimal.BinaryToASCIIBuffer.buffer
//         } else if (cl == Long.TYPE) {
//             return 20; // "-9223372036854775808"
//         } else {
//             throw new IllegalArgumentException("无法估计此类型的大小：" + cl);
//         }
//     }
//
//     static Class<?> adaptToStringBuilder(Class<?> c) {
//         if (c.isPrimitive()) {
//             if (c == Byte.TYPE || c == Short.TYPE) {
//                 return int.class;
//             }
//         } else {
//             if (c != String.class) {
//                 return Object.class;
//             }
//         }
//         return c;
//     }
//
//     private StringConcatFactory() {
//         // 禁止实例化
//     }
//
// }
//
