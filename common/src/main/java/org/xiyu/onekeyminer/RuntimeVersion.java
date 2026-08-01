package org.xiyu.onekeyminer;

/** Resolves the packaged mod version without initializing game-facing classes. */
final class RuntimeVersion {
    private RuntimeVersion() {
    }

    static String resolve(String implementationVersion) {
        return implementationVersion == null || implementationVersion.isBlank()
                ? "1.6.7"
                : implementationVersion;
    }
}
