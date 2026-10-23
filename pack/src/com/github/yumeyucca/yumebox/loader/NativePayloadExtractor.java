package com.github.yumeyucca.yumebox.loader;

import java.io.IOException;

final class NativePayloadExtractor {
    static {
        System.loadLibrary("loader");
    }

    private NativePayloadExtractor() {
    }

    static native long openArchive(String apkPath) throws IOException;

    static native void extract(
            long archive,
            String entryName,
            String targetPath,
            long expectedSize,
            byte[] expectedSha256
    ) throws IOException;

    static native void closeArchive(long archive);
}
