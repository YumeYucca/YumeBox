/*
 * Minimal JNI shim for the P0 spike.
 *
 * Why this exists: Android's Java ProcessBuilder spawns with CLOEXEC_DEFAULT, so an arbitrary fd
 * (the VpnService tun) cannot be inherited by a child process without YumeBox's own fork/exec
 * bridge. Running the core in-process sidesteps that entirely — the fd stays valid — and it is how
 * libbox works on real clients. The trade-off is that the core shares the app's lifetime, which is
 * fine for a probe.
 *
 * The core itself is built as a c-shared library; this shim only loads it and calls SingboxMain on
 * the calling (Java) thread. It never calls back into the JVM, so no AttachCurrentThread is needed.
 */
#include <dlfcn.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>

typedef int (*singbox_entry)(int, char **, char *);

static char **split_arguments(const char *line, int *out_count) {
    char *copy = strdup(line);
    int capacity = 8;
    int count = 0;
    char **arguments = malloc(sizeof(char *) * capacity);
    char *cursor = copy;
    while (cursor != NULL && *cursor != '\0') {
        if (count + 1 >= capacity) {
            capacity *= 2;
            arguments = realloc(arguments, sizeof(char *) * capacity);
        }
        arguments[count++] = cursor;
        char *next = strchr(cursor, '\n');
        if (next == NULL) {
            break;
        }
        *next = '\0';
        cursor = next + 1;
    }
    arguments[count] = NULL;
    *out_count = count;
    return arguments;
}

JNIEXPORT jint JNICALL
Java_com_yumebox_spike_CoreBridge_run(JNIEnv *env, jclass clazz, jstring library_path, jstring arguments) {
    const char *path = (*env)->GetStringUTFChars(env, library_path, NULL);
    if (path == NULL) {
        return -10;
    }
    void *handle = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
    (*env)->ReleaseStringUTFChars(env, library_path, path);
    if (handle == NULL) {
        return -11;
    }
    singbox_entry entry = (singbox_entry)dlsym(handle, "SingboxMain");
    if (entry == NULL) {
        return -12;
    }
    const char *line = (*env)->GetStringUTFChars(env, arguments, NULL);
    if (line == NULL) {
        return -13;
    }
    int count = 0;
    char **argv = split_arguments(line, &count);
    (*env)->ReleaseStringUTFChars(env, arguments, line);
    if (count == 0) {
        return -14;
    }
    int result = entry(count, argv, "");
    free(argv);
    return result;
}
