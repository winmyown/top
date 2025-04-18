// package org.top.java.source.lang.invoke;
//
// import sun.util.logging.PlatformLogger;
//
// import java.io.FilePermission;
// import java.nio.file.Files;
// import java.nio.file.InvalidPathException;
// import java.nio.file.Path;
// import java.security.AccessController;
// import java.security.PrivilegedAction;
// import java.util.Objects;
//
// /**
//  * Helper class used by InnerClassLambdaMetafactory to log generated classes
//  *
//  * @implNote
//  * <p> Because this class is called by LambdaMetafactory, make use
//  * of lambda lead to recursive calls cause stack overflow.
//  */
// final class ProxyClassesDumper {
//     private static final char[] HEX = {
//             '0', '1', '2', '3', '4', '5', '6', '7',
//             '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
//     };
//     private static final char[] BAD_CHARS = {
//             '\\', ':', '*', '?', '"', '<', '>', '|'
//     };
//     private static final String[] REPLACEMENT = {
//             "%5C", "%3A", "%2A", "%3F", "%22", "%3C", "%3E", "%7C"
//     };
//
//     private final Path dumpDir;
//
//     public static ProxyClassesDumper getInstance(String path) {
//         if (null == path) {
//             return null;
//         }
//         try {
//             path = path.trim();
//             final Path dir = Path.of(path.length() == 0 ? "." : path);
//             AccessController.doPrivileged(new PrivilegedAction<>() {
//                 @Override
//                 public Void run() {
//                     validateDumpDir(dir);
//                     return null;
//                 }
//             }, null, new FilePermission("<<ALL FILES>>", "read, write"));
//             return new ProxyClassesDumper(dir);
//         } catch (InvalidPathException ex) {
//             PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
//                     .warning("Path " + path + " is not valid - dumping disabled", ex);
//         } catch (IllegalArgumentException iae) {
//             PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
//                     .warning(iae.getMessage() + " - dumping disabled");
//         }
//         return null;
//     }
//
//     private ProxyClassesDumper(Path path) {
//         dumpDir = Objects.requireNonNull(path);
//     }
//
//     private static void validateDumpDir(Path path) {
//         if (!Files.exists(path)) {
//             throw new IllegalArgumentException("Directory " + path + " does not exist");
//         } else if (!Files.isDirectory(path)) {
//             throw new IllegalArgumentException("Path " + path + " is not a directory");
//         } else if (!Files.isWritable(path)) {
//             throw new IllegalArgumentException("Directory " + path + " is not writable");
//         }
//     }
//
//     public static String encodeForFilename(String className) {
//         final int len = className.length();
//         StringBuilder sb = new StringBuilder(len);
//
//         for (int i = 0; i < len; i++) {
//             char c = className.charAt(i);
//             // control characters
//             if (c <= 31) {
//                 sb.append('%');
//                 sb.append(HEX[c >> 4 & 0x0F]);
//                 sb.append(HEX[c & 0x0F]);
//             } else {
//                 int j = 0;
//                 for (; j < BAD_CHARS.length; j++) {
//                     if (c == BAD_CHARS[j]) {
//                         sb.append(REPLACEMENT[j]);
//                         break;
//                     }
//                 }
//                 if (j >= BAD_CHARS.length) {
//                     sb.append(c);
//                 }
//             }
//         }
//
//         return sb.toString();
//     }
//
//     public void dumpClass(String className, final byte[] classBytes) {
//         Path file;
//         try {
//             file = dumpDir.resolve(encodeForFilename(className) + ".class");
//         } catch (InvalidPathException ex) {
//             PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
//                     .warning("Invalid path for class " + className);
//             return;
//         }
//
//         try {
//             Path dir = file.getParent();
//             Files.createDirectories(dir);
//             Files.write(file, classBytes);
//         } catch (Exception ignore) {
//             PlatformLogger.getLogger(ProxyClassesDumper.class.getName())
//                     .warning("Exception writing to path at " + file.toString());
//             // simply don't care if this operation failed
//         }
//     }
// }