package org.xiyu.onekeyminer;

/** Resolves the release version from the reproducible JAR manifest. */
public final class RuntimeVersion {
    private RuntimeVersion() {
    }

    public static String resolve(Class<?> anchor, String developmentFallback) {
        String version = anchor.getPackage().getImplementationVersion();
        return version == null || version.isBlank()
                ? developmentFallback
                : version;
    }
}
