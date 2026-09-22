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
     * Whether this process is an application process started by the system, i.e. whether
     * {@code ActivityThread.mBoundApplication} is set.
     *
     * <p>Processes that host classes for another app (for example the Shizuku UserService, which
     * Shizuku starts with {@code ActivityThread.systemMain()}) have no bound application. In such a
     * process the packed payload cannot be installed, so the loader stays out of the way and the
     * application is instantiated from the APK's own DEX files.</p>
     *
     * <p>When the state cannot be determined the application assumption is kept, so a reflection
     * failure can never disable payload installation for a real application process.</p>
     *
     * <p>Resolved once per process: {@link LoaderComponentFactory} asks on every component
     * instantiation, and the answer cannot change while the process lives.</p>
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
        return resolved == BOUND_APPLICATION_PRESENT;
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
