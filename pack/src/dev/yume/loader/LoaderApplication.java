package dev.yume.loader;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

public final class LoaderApplication extends Application {
    private Application original;

    @Override
    protected void attachBaseContext(@NonNull Context base) {
        super.attachBaseContext(base);
        if (!RuntimeBootstrap.hasBoundApplication()) {
            // Not an application process (for example the Shizuku UserService, which Shizuku starts
            // itself). The packed payload cannot be installed here, so this class stays a plain
            // Application shell whose class loader keeps serving the APK's own DEX files.
            return;
        }
        PayloadInstaller.Installation installation = PayloadInstaller.install(
                base.getApplicationInfo(),
                base.getClassLoader(),
                RuntimeBootstrap.currentLoadedApk()
        );
        original = ApplicationBridge.create(
                base,
                installation.classLoader(),
                installation.metadata().originalApplication
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (original == null) {
            return;
        }
        ApplicationBridge.replace(this, original);
        original.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (original != null) {
            original.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (original != null) {
            original.onLowMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (original != null) {
            original.onTrimMemory(level);
        }
    }

    @Override
    public void onTerminate() {
        if (original != null) {
            original.onTerminate();
        }
        super.onTerminate();
    }
}
