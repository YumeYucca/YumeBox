package com.github.yumeyucca.yumebox.loader;

import android.os.Build;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({"ConstantConditions", "DataFlowIssue", "NullableProblems"})
final class ReflectionAccess {
    private static volatile boolean exempted;

    private ReflectionAccess() {
    }

    static void exemptHiddenApis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || exempted) {
            return;
        }
        synchronized (ReflectionAccess.class) {
            if (!exempted) {
                if (!HiddenApiBypass.addHiddenApiExemptions("L")) {
                    throw new IllegalStateException("Unable to disable hidden API enforcement");
                }
                exempted = true;
            }
        }
    }

    static Object get(Object target, String name) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    static void setIfPresent(Object target, String name, Object value) {
        try {
            set(target, name, value);
        } catch (ReflectiveOperationException ignored) {
            // Field is not present on every supported Android version.
        }
    }

    static Object invokeStatic(Class<?> type, String name, Class<?>[] parameters, Object... args)
            throws ReflectiveOperationException {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    static Method findMethod(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException((type == null ? "null" : type.getName()) + "." + name);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException((type == null ? "null" : type.getName()) + "." + name);
    }
}
