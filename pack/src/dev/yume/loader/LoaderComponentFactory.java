package dev.yume.loader;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;

@RequiresApi(api = Build.VERSION_CODES.P)
public final class LoaderComponentFactory extends AppComponentFactory {
    private volatile AppComponentFactory delegate;
    private volatile ClassLoader payloadLoader;

    private static AppComponentFactory instantiateComponentFactory(Class<?> type)
            throws IllegalAccessException, InstantiationException {
        try {
            return type.asSubclass(AppComponentFactory.class).getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException e) {
            InstantiationException failure = new InstantiationException(type.getName());
            failure.initCause(e);
            throw failure;
        }
    }

    @Override
    public @NonNull Application instantiateApplication(@NonNull ClassLoader classLoader, @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        PayloadInstaller.Installation installation = prepare(classLoader);
        if (installation == null) {
            return super.instantiateApplication(classLoader, className);
        }
        return delegate(installation.metadata()).instantiateApplication(
                installation.classLoader(),
                installation.metadata().originalApplication
        );
    }

    @Override
    public @NonNull Activity instantiateActivity(@NonNull ClassLoader classLoader, @NonNull String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        PayloadInstaller.Installation installation = prepare(classLoader);
        if (installation == null) {
            return super.instantiateActivity(classLoader, className, intent);
        }
        return delegate(installation.metadata()).instantiateActivity(installation.classLoader(), className, intent);
    }

    @Override
    public @NonNull Service instantiateService(@NonNull ClassLoader classLoader, @NonNull String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        PayloadInstaller.Installation installation = prepare(classLoader);
        if (installation == null) {
            return super.instantiateService(classLoader, className, intent);
        }
        return delegate(installation.metadata()).instantiateService(installation.classLoader(), className, intent);
    }

    @Override
    public @NonNull BroadcastReceiver instantiateReceiver(@NonNull ClassLoader classLoader, @NonNull String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        PayloadInstaller.Installation installation = prepare(classLoader);
        if (installation == null) {
            return super.instantiateReceiver(classLoader, className, intent);
        }
        return delegate(installation.metadata()).instantiateReceiver(installation.classLoader(), className, intent);
    }

    @Override
    public @NonNull ContentProvider instantiateProvider(@NonNull ClassLoader classLoader, @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        PayloadInstaller.Installation installation = prepare(classLoader);
        if (installation == null) {
            return super.instantiateProvider(classLoader, className);
        }
        return delegate(installation.metadata()).instantiateProvider(installation.classLoader(), className);
    }

    /** Installs the packed payload, or returns {@code null} outside an application process. */
    private PayloadInstaller.Installation prepare(ClassLoader classLoader) {
        if (!RuntimeBootstrap.hasBoundApplication()) {
            return null;
        }
        ApplicationInfo appInfo = RuntimeBootstrap.currentApplicationInfo();
        PayloadInstaller.Installation installation = PayloadInstaller.install(
                appInfo,
                classLoader,
                RuntimeBootstrap.currentLoadedApk()
        );
        payloadLoader = installation.classLoader();
        return installation;
    }

    private AppComponentFactory delegate(PayloadMetadata metadata) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        AppComponentFactory current = delegate;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (delegate == null) {
                if (metadata.originalComponentFactory.isEmpty()) {
                    delegate = new AppComponentFactory();
                } else {
                    Class<?> type = Class.forName(metadata.originalComponentFactory, true, payloadLoader);
                    delegate = instantiateComponentFactory(type);
                }
            }
            return delegate;
        }
    }
}
