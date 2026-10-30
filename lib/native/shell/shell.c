/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * YumeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

#include <dlfcn.h>
#include <stdio.h>
#include <string.h>

#define CORE_LIBRARY_OPTION "--core-library"
#define CORE_ENTRY_SYMBOL "MihomoMain"
#define CORE_ENTRY_SYMBOL_ALT "SingboxMain"
#define CHANNEL_ENV_PREFIX "CHANNEL="

extern char **environ;

typedef int (*mihomo_main_fn)(int argc, char **argv, char *channel);

static char *find_channel(void) {
    char *channel = NULL;
    const size_t prefix_length = sizeof(CHANNEL_ENV_PREFIX) - 1U;
    for (char **entry = environ; entry != NULL && *entry != NULL; ++entry) {
        if (strncmp(*entry, CHANNEL_ENV_PREFIX, prefix_length) == 0) {
            channel = *entry + prefix_length;
        }
    }
    return channel;
}

static int fail(const char *message, const char *detail) {
    if (detail == NULL || detail[0] == '\0') {
        fprintf(stderr, "mihomo shell: %s\n", message);
    } else {
        fprintf(stderr, "mihomo shell: %s: %s\n", message, detail);
    }
    return 127;
}

int main(int argc, char **argv) {
    int option_index = -1;
    for (int index = 1; index < argc; ++index) {
        if (strcmp(argv[index], CORE_LIBRARY_OPTION) == 0) {
            option_index = index;
            break;
        }
    }
    if (option_index < 0 || option_index + 1 >= argc) {
        return fail("missing " CORE_LIBRARY_OPTION, NULL);
    }

    const char *library_path = argv[option_index + 1];
    for (int index = option_index; index + 2 <= argc; ++index) {
        argv[index] = argv[index + 2];
    }
    argc -= 2;
    argv[argc] = NULL;

    void *library = dlopen(library_path, RTLD_NOW | RTLD_LOCAL);
    if (library == NULL) {
        return fail("load core library", dlerror());
    }

    dlerror();
    void *symbol = dlsym(library, CORE_ENTRY_SYMBOL);
    const char *symbol_error = dlerror();
    if (symbol_error != NULL || symbol == NULL) {
        /* The sing-box core exports its own entry point; one loader serves both engines, which is
         * what lets a device keep a mihomo kernel installed and fall back to it. */
        symbol = dlsym(library, CORE_ENTRY_SYMBOL_ALT);
        symbol_error = dlerror();
    }
    if (symbol_error != NULL || symbol == NULL) {
        return fail("resolve " CORE_ENTRY_SYMBOL " or " CORE_ENTRY_SYMBOL_ALT, symbol_error);
    }

    mihomo_main_fn entry;
    memcpy(&entry, &symbol, sizeof(entry));
    return entry(argc, argv, find_channel());
}
