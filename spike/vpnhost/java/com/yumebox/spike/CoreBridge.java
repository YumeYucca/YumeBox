package com.yumebox.spike;

/** Loads the sing-box core and runs it on the calling thread. */
final class CoreBridge {
    static {
        System.loadLibrary("corebridge");
    }

    private CoreBridge() {
    }

    /**
     * @param libraryPath absolute path of the c-shared core
     * @param arguments   newline separated argv, program name included
     * @return the core's exit code, or a negative value when loading failed
     */
    static native int run(String libraryPath, String arguments);
}
