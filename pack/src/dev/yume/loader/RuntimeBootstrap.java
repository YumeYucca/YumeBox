package dev.yume.loader;

import android.annotation.SuppressLint;

import android.content.pm.ApplicationInfo;

@SuppressLint("PrivateApi")
final class RuntimeBootstrap {
    private static final int BOUND_APPLICATION_PRESENT = 1;
    private static final int BOUND_APPLICATION_ABSENT = 2;
    private static final int BOUND_APPLICATION_ASSUMED = 3;

    /** 0 while unresolved; otherwise one of the {@code BOUND_APPLICATION_*} states. */
    private static volatile int boundApplicationState;

    private RuntimeBootstrap() {
    }

    /**
     * Whether this process is an application process, i.e. whether {@code ActivityThread}
     * {@code mBoundApplication} is set. A process that only hosts classes for another app (Shizuku
     * starts its UserService with {@code ActivityThread.systemMain()}) has none, and the payload
     * must not be installed there.
     *
     * <p>Resolved once per process. An unreadable state keeps the application assumption, so a
     * reflection failure can never disable payload installation.
     */
    static boolean hasBoundApplication() {
        int resolved = boundApplicationState;
        if (resolved == 0) {
            resolved = classifyBoundApplication();
            if (resolved == 0) {
                // Reflection reported "unknown"; fall back to the application assumption.
                resolved = BOUND_APPLICATION_ASSUMED;
            }
            boundApplicationState = resolved;
        }
        // Only an explicit "absent" disables payload installation.
        return resolved != BOUND_APPLICATION_ABSENT;
    }

    /**
     * @return {@link #BOUND_APPLICATION_PRESENT} or {@link #BOUND_APPLICATION_ABSENT}, or {@code 0}
     *     when the state cannot be read.
     */
    private static int classifyBoundApplication() {
        try {
            ReflectionAccess.exemptHiddenApis();
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread =
                    ReflectionAccess.invokeStatic(
                            activityThread, "currentActivityThread", new Class<?>[0]);
            if (thread == null) {
                return 0;
            }
            return ReflectionAccess.get(thread, "mBoundApplication") != null
                    ? BOUND_APPLICATION_PRESENT
                    : BOUND_APPLICATION_ABSENT;
        } catch (Throwable error) {
            return 0;
        }
    }

    static ApplicationInfo currentApplicationInfo() {
        return (ApplicationInfo) boundField("appInfo");
    }

    static Object currentLoadedApk() {
        return boundField("info");
    }

    static void installClassLoader(Object loadedApk, ClassLoader classLoader)
            throws ReflectiveOperationException {
        ReflectionAccess.set(loadedApk, "mClassLoader", classLoader);
        ReflectionAccess.setIfPresent(loadedApk, "mDefaultClassLoader", classLoader);
    }

    private static Object boundField(String fieldName) {
        ReflectionAccess.exemptHiddenApis();
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = ReflectionAccess.invokeStatic(
                    activityThread,
                    "currentActivityThread",
                    new Class<?>[0]
            );
            Object boundApplication = ReflectionAccess.get(thread, "mBoundApplication");
            return ReflectionAccess.get(boundApplication, fieldName);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to read ActivityThread.AppBindData." + fieldName, error);
        }
    }
}
