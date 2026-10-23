package com.github.yumeyucca.yumebox.loader;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.PathClassLoader;

final class PayloadInstaller {
    private static final String CACHE_MARKER = ".complete";
    private static final String LAZY_CORE_LIBRARY = "libmihomocore.so";
    private static volatile Installation installedPayload;
    private PayloadInstaller() {
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            // Best-effort cleanup of temp payload artifacts.
        }
    }

    static Installation install(ApplicationInfo appInfo, ClassLoader parentLoader, Object loadedApk) {
        Installation current = installedPayload;
        if (current != null) {
            return current;
        }
        synchronized (PayloadInstaller.class) {
            if (installedPayload != null) {
                return installedPayload;
            }
            ReflectionAccess.exemptHiddenApis();
            try (ZipFile apk = new ZipFile(appInfo.sourceDir)) {
                PayloadMetadata metadata = readMetadata(apk);
                File root = new File(appInfo.dataDir, "code_cache/loader");
                ensureDirectory(root);
                try (RandomAccessFile lockFile = new RandomAccessFile(new File(root, "install.lock"), "rw");
                     java.nio.channels.FileLock ignored = lockFile.getChannel().lock()) {
                    File payloadDir = safeChild(root, metadata.id, "payload id");
                    File dexDir = new File(payloadDir, "dex");
                    ensureDirectory(dexDir);

                    List<PayloadMetadata.NativeEntry> nativeEntries = selectNativeEntries(metadata.nativeEntries);
                    List<PayloadMetadata.NativeEntry> eagerNativeEntries = new ArrayList<>();
                    for (PayloadMetadata.NativeEntry entry : nativeEntries) {
                        if (!LAZY_CORE_LIBRARY.equals(entry.outputName())) {
                            eagerNativeEntries.add(entry);
                        }
                    }
                    File nativeDir = null;
                    if (!nativeEntries.isEmpty()) {
                        nativeDir = safeChild(new File(payloadDir, "lib"), nativeEntries.get(0).abi(), "ABI");
                        ensureDirectory(nativeDir);
                    }
                    File completeMarker = new File(payloadDir, CACHE_MARKER);
                    boolean cacheComplete = hasCompleteMarker(completeMarker, metadata.id)
                            && hasExpectedPayloadFiles(metadata.dexEntries, dexDir, eagerNativeEntries, nativeDir);
                    if (completeMarker.exists() && !cacheComplete && !completeMarker.delete()) {
                        throw new IOException("Unable to invalidate incomplete payload cache: " + completeMarker);
                    }

                    long archive = cacheComplete ? 0 : NativePayloadExtractor.openArchive(apk.getName());
                    List<File> dexFiles = new ArrayList<>(metadata.dexEntries.size());
                    try {
                        for (PayloadMetadata.DexEntry entry : metadata.dexEntries) {
                            File target = safeChild(dexDir, entry.outputName(), "dex output name");
                            if (!cacheComplete && !isValid(target, entry.size(), entry.sha256())) {
                                extract(archive, entry.assetName(), entry.size(), entry.sha256(), target);
                            }
                            makeReadOnly(target);
                            dexFiles.add(target);
                        }
                        if (!eagerNativeEntries.isEmpty()) {
                            for (PayloadMetadata.NativeEntry entry : eagerNativeEntries) {
                                File target = safeChild(nativeDir, entry.outputName(), "native output name");
                                if (!cacheComplete && !isValid(target, entry.size(), entry.sha256())) {
                                    extract(archive, entry.assetName(), entry.size(), entry.sha256(), target);
                                }
                                makeReadOnly(target);
                            }
                        }
                    } finally {
                        if (archive != 0) {
                            NativePayloadExtractor.closeArchive(archive);
                        }
                    }
                    if (!cacheComplete) {
                        publishCompleteMarker(payloadDir, metadata.id, completeMarker);
                    }
                    ClassLoader payloadLoader = createPayloadLoader(
                            parentLoader,
                            dexFiles,
                            nativeDir
                    );
                    RuntimeBootstrap.installClassLoader(loadedApk, payloadLoader);
                    installedPayload = new Installation(
                            metadata,
                            payloadLoader,
                            nativeDir,
                            appInfo.sourceDir,
                            nativeEntries
                    );
                }
                return installedPayload;
            } catch (IOException | ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to install the application payload", error);
            }
        }
    }

    private static PayloadMetadata readMetadata(ZipFile apk) throws IOException {
        ZipEntry entry = apk.getEntry(PayloadMetadata.METADATA_ENTRY);
        if (entry == null) {
            throw new IOException("Loader payload metadata is missing");
        }
        try (java.io.InputStream input = apk.getInputStream(entry)) {
            return PayloadMetadata.read(input);
        }
    }

    private static ClassLoader createPayloadLoader(
            ClassLoader parentLoader,
            List<File> dexFiles,
            File nativeDir
    ) {
        StringBuilder dexPath = new StringBuilder();
        for (File file : dexFiles) {
            if (dexPath.length() != 0) {
                dexPath.append(File.pathSeparatorChar);
            }
            dexPath.append(file.getAbsolutePath());
        }
        PathClassLoader payloadLoader = new PathClassLoader(
                dexPath.toString(),
                nativeDir == null ? null : nativeDir.getAbsolutePath(),
                parentLoader
        );
        Thread.currentThread().setContextClassLoader(payloadLoader);
        return payloadLoader;
    }

    private static void extract(
            long archive,
            String assetName,
            long expectedSize,
            byte[] expectedSha256,
            File target
    )
            throws IOException {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp-" + android.os.Process.myPid());
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException("Unable to remove stale temporary payload: " + temporary);
        }
        try {
            NativePayloadExtractor.extract(
                    archive,
                    assetName,
                    temporary.getAbsolutePath(),
                    expectedSize,
                    expectedSha256
            );
        } catch (Throwable error) {
            deleteQuietly(temporary);
            throw error;
        }
        if (target.exists() && !target.delete()) {
            deleteQuietly(temporary);
            throw new IOException("Unable to replace stale payload: " + target);
        }
        if (!temporary.renameTo(target)) {
            deleteQuietly(temporary);
            throw new IOException("Unable to publish extracted payload: " + target);
        }
    }

    private static boolean isValid(File file, long expectedSize, byte[] expectedSha256) throws IOException {
        if (!file.isFile() || file.length() != expectedSize) {
            return false;
        }
        MessageDigest digest = sha256Digest();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return Arrays.equals(digest.digest(), expectedSha256);
    }

    private static boolean hasExpectedPayloadFiles(
            List<PayloadMetadata.DexEntry> dexEntries,
            File dexDir,
            List<PayloadMetadata.NativeEntry> nativeEntries,
            File nativeDir
    ) {
        try {
            for (PayloadMetadata.DexEntry entry : dexEntries) {
                if (!hasExpectedSize(safeChild(dexDir, entry.outputName(), "dex output name"), entry.size())) {
                    return false;
                }
            }
            for (PayloadMetadata.NativeEntry entry : nativeEntries) {
                if (!hasExpectedSize(safeChild(nativeDir, entry.outputName(), "native output name"), entry.size())) {
                    return false;
                }
            }
            return true;
        } catch (IOException invalidPayload) {
            return false;
        }
    }

    private static boolean hasExpectedSize(File file, long expectedSize) {
        return file.isFile() && file.length() == expectedSize;
    }

    private static boolean hasCompleteMarker(File marker, String payloadId) {
        byte[] expected = payloadId.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!marker.isFile() || marker.length() != expected.length) {
            return false;
        }
        byte[] actual = new byte[expected.length];
        try (FileInputStream input = new FileInputStream(marker)) {
            int offset = 0;
            while (offset < actual.length) {
                int read = input.read(actual, offset, actual.length - offset);
                if (read < 0) {
                    return false;
                }
                offset += read;
            }
            return input.read() == -1 && Arrays.equals(actual, expected);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static File safeChild(File parent, String name, String field) throws IOException {
        if (name == null || name.isEmpty()) {
            throw new IOException("Payload " + field + " is empty");
        }
        File canonicalParent = parent.getCanonicalFile();
        File candidate = new File(canonicalParent, name).getCanonicalFile();
        if (!canonicalParent.equals(candidate.getParentFile())) {
            throw new IOException("Payload " + field + " escapes its directory: " + name);
        }
        return candidate;
    }

    private static void publishCompleteMarker(File payloadDir, String payloadId, File target)
            throws IOException {
        File temporary = new File(payloadDir, CACHE_MARKER + ".tmp-" + android.os.Process.myPid());
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException("Unable to remove stale cache marker: " + temporary);
        }
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(payloadId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.getFD().sync();
        } catch (Throwable error) {
            deleteQuietly(temporary);
            throw error;
        }
        makeReadOnly(temporary);
        if (target.exists() && !target.delete()) {
            deleteQuietly(temporary);
            throw new IOException("Unable to replace payload cache marker: " + target);
        }
        if (!temporary.renameTo(target)) {
            deleteQuietly(temporary);
            throw new IOException("Unable to publish payload cache marker: " + target);
        }
    }

    private static void makeReadOnly(File file) throws IOException {
        if (file.canWrite() && !file.setReadOnly()) {
            throw new IOException("Unable to make payload read-only: " + file);
        }
        if (file.canWrite()) {
            throw new IOException("Payload remains writable: " + file);
        }
    }

    private static List<PayloadMetadata.NativeEntry> selectNativeEntries(
            List<PayloadMetadata.NativeEntry> entries
    ) throws IOException {
        if (entries.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        for (String abi : Build.SUPPORTED_ABIS) {
            List<PayloadMetadata.NativeEntry> selected = new ArrayList<>();
            for (PayloadMetadata.NativeEntry entry : entries) {
                if (abi.equals(entry.abi())) {
                    selected.add(entry);
                }
            }
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        throw new IOException("Payload has no native libraries for " + Arrays.toString(Build.SUPPORTED_ABIS));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Unable to create directory: " + directory);
        }
    }

    static String findNativeLibrary(String fileName) {
        synchronized (PayloadInstaller.class) {
            Installation installation = installedPayload;
            if (installation == null || installation.nativeDirectory() == null) {
                return null;
            }
            try {
                File library = safeChild(
                        installation.nativeDirectory(),
                        fileName,
                        "native library name"
                );
                PayloadMetadata.NativeEntry metadata = null;
                for (PayloadMetadata.NativeEntry entry : installation.nativeEntries()) {
                    if (fileName.equals(entry.outputName())) {
                        metadata = entry;
                        break;
                    }
                }
                if (metadata == null) {
                    return null;
                }
                if (hasExpectedSize(library, metadata.size())) {
                    return library.getAbsolutePath();
                }
                if (library.exists() && !library.delete()) {
                    return null;
                }
                long archive = NativePayloadExtractor.openArchive(installation.apkPath());
                try {
                    extract(
                            archive,
                            metadata.assetName(),
                            metadata.size(),
                            metadata.sha256(),
                            library
                    );
                } finally {
                    NativePayloadExtractor.closeArchive(archive);
                }
                makeReadOnly(library);
                return library.getAbsolutePath();
            } catch (IOException invalidLibrary) {
                return null;
            }
        }
    }

    record Installation(
            PayloadMetadata metadata,
            ClassLoader classLoader,
            File nativeDirectory,
            String apkPath,
            List<PayloadMetadata.NativeEntry> nativeEntries
    ) {
    }
}
