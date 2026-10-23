package com.github.yumeyucca.yumebox.loader;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;

final class ApplicationBridge {
    private ApplicationBridge() {
    }

    static Application create(Context base, ClassLoader loader, String className) {
        try {
            Class<?> type = Class.forName(className, true, loader);
            Application application = (Application) type.getDeclaredConstructor().newInstance();
            Method attach = ReflectionAccess.findMethod(Application.class, "attach", Context.class);
            attach.invoke(application, base);
            return application;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to create the original Application: " + className, error);
        }
    }

    @SuppressWarnings("unchecked")
    static void replace(Application shell, Application original) {
        try {
            Object base = ReflectionAccess.get(shell, "mBase");
            Object loadedApk = ReflectionAccess.get(base, "mPackageInfo");
            Object activityThread = ReflectionAccess.get(loadedApk, "mActivityThread");
            ReflectionAccess.set(loadedApk, "mApplication", original);
            ReflectionAccess.set(activityThread, "mInitialApplication", original);
            List<Application> applications = (List<Application>) ReflectionAccess.get(activityThread, "mAllApplications");
            int index = applications.indexOf(shell);
            if (index >= 0) {
                applications.set(index, original);
            }
            ReflectionAccess.set(base, "mOuterContext", original);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to replace the loader Application", error);
        }
    }
}
