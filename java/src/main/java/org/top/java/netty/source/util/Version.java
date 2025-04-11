

package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.PlatformDependent;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Retrieves the version information of available Netty artifacts.
 * <p>
 * This class retrieves the version information from {@code META-INF/io.netty.versions.properties}, which is
 * generated in build time.  Note that it may not be possible to retrieve the information completely, depending on
 * your environment, such as the specified {@link ClassLoader}, the current {@link SecurityManager}.
 * </p>
 */

/**
 * 检索可用的Netty工件的版本信息。
 * <p>
 * 该类从 {@code META-INF/io.netty.versions.properties} 中检索版本信息，该文件在构建时生成。请注意，根据您的环境（例如指定的 {@link ClassLoader}、当前的 {@link SecurityManager}），可能无法完全检索到信息。
 * </p>
 */
public final class Version {

    private static final String PROP_VERSION = ".version";
    private static final String PROP_BUILD_DATE = ".buildDate";
    private static final String PROP_COMMIT_DATE = ".commitDate";
    private static final String PROP_SHORT_COMMIT_HASH = ".shortCommitHash";
    private static final String PROP_LONG_COMMIT_HASH = ".longCommitHash";
    private static final String PROP_REPO_STATUS = ".repoStatus";

    /**
     * Retrieves the version information of Netty artifacts using the current
     * {@linkplain Thread#getContextClassLoader() context class loader}.
     *
     * @return A {@link Map} whose keys are Maven artifact IDs and whose values are {@link Version}s
     */

    /**
     * 使用当前的 {@linkplain Thread#getContextClassLoader() 上下文类加载器} 获取 Netty 工件的版本信息。
     *
     * @return 一个 {@link Map}，其键是 Maven 工件 ID，值是对应的 {@link Version}
     */
    public static Map<String, Version> identify() {
        return identify(null);
    }

    /**
     * Retrieves the version information of Netty artifacts using the specified {@link ClassLoader}.
     *
     * @return A {@link Map} whose keys are Maven artifact IDs and whose values are {@link Version}s
     */

    /**
     * 使用指定的 {@link ClassLoader} 检索 Netty 工件的版本信息。
     *
     * @return 一个 {@link Map}，其键是 Maven 工件 ID，值是 {@link Version}
     */
    public static Map<String, Version> identify(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = PlatformDependent.getContextClassLoader();
        }

        // Collect all properties.

        // 收集所有属性。
        Properties props = new Properties();
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/io.netty.versions.properties");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                InputStream in = url.openStream();
                try {
                    props.load(in);
                } finally {
                    try {
                        in.close();
                    } catch (Exception ignore) {
                        // Ignore.
                        // 忽略。
                    }
                }
            }
        } catch (Exception ignore) {
            // Not critical. Just ignore.
            // 非关键。忽略即可。
        }

        // Collect all artifactIds.

        // 收集所有 artifactIds。
        Set<String> artifactIds = new HashSet<String>();
        for (Object o: props.keySet()) {
            String k = (String) o;

            int dotIndex = k.indexOf('.');
            if (dotIndex <= 0) {
                continue;
            }

            String artifactId = k.substring(0, dotIndex);

            // Skip the entries without required information.

            // 跳过没有所需信息的条目。
            if (!props.containsKey(artifactId + PROP_VERSION) ||
                !props.containsKey(artifactId + PROP_BUILD_DATE) ||
                !props.containsKey(artifactId + PROP_COMMIT_DATE) ||
                !props.containsKey(artifactId + PROP_SHORT_COMMIT_HASH) ||
                !props.containsKey(artifactId + PROP_LONG_COMMIT_HASH) ||
                !props.containsKey(artifactId + PROP_REPO_STATUS)) {
                continue;
            }

            artifactIds.add(artifactId);
        }

        Map<String, Version> versions = new TreeMap<String, Version>();
        for (String artifactId: artifactIds) {
            versions.put(
                    artifactId,
                    new Version(
                            artifactId,
                            props.getProperty(artifactId + PROP_VERSION),
                            parseIso8601(props.getProperty(artifactId + PROP_BUILD_DATE)),
                            parseIso8601(props.getProperty(artifactId + PROP_COMMIT_DATE)),
                            props.getProperty(artifactId + PROP_SHORT_COMMIT_HASH),
                            props.getProperty(artifactId + PROP_LONG_COMMIT_HASH),
                            props.getProperty(artifactId + PROP_REPO_STATUS)));
        }

        return versions;
    }

    private static long parseIso8601(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(value).getTime();
        } catch (ParseException ignored) {
            return 0;
        }
    }

    /**
     * Prints the version information to {@link System#err}.
     */

    /**
     * 将版本信息打印到 {@link System#err}。
     */
    public static void main(String[] args) {
        for (Version v: identify().values()) {
            System.err.println(v);
        }
    }

    private final String artifactId;
    private final String artifactVersion;
    private final long buildTimeMillis;
    private final long commitTimeMillis;
    private final String shortCommitHash;
    private final String longCommitHash;
    private final String repositoryStatus;

    private Version(
            String artifactId, String artifactVersion,
            long buildTimeMillis, long commitTimeMillis,
            String shortCommitHash, String longCommitHash, String repositoryStatus) {
        this.artifactId = artifactId;
        this.artifactVersion = artifactVersion;
        this.buildTimeMillis = buildTimeMillis;
        this.commitTimeMillis = commitTimeMillis;
        this.shortCommitHash = shortCommitHash;
        this.longCommitHash = longCommitHash;
        this.repositoryStatus = repositoryStatus;
    }

    public String artifactId() {
        return artifactId;
    }

    public String artifactVersion() {
        return artifactVersion;
    }

    public long buildTimeMillis() {
        return buildTimeMillis;
    }

    public long commitTimeMillis() {
        return commitTimeMillis;
    }

    public String shortCommitHash() {
        return shortCommitHash;
    }

    public String longCommitHash() {
        return longCommitHash;
    }

    public String repositoryStatus() {
        return repositoryStatus;
    }

    @Override
    public String toString() {
        return artifactId + '-' + artifactVersion + '.' + shortCommitHash +
               ("clean".equals(repositoryStatus)? "" : " (repository: " + repositoryStatus + ')');
    }
}
