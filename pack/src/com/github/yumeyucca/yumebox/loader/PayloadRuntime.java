/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.loader;

/** Runtime bridge exposed by the bootstrap DEX to code loaded from the compressed payload. */
public final class PayloadRuntime {
    private PayloadRuntime() {
    }

    public static String findNativeLibrary(String fileName) {
        return PayloadInstaller.findNativeLibrary(fileName);
    }
}
