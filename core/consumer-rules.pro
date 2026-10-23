# Included by app/proguard-rules.pro during Toolchain release builds.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Both native libraries resolve these by name at runtime: libcompat.so declares its entry points
# as Java_com_github_yumeyucca_yumebox_core_bridge_{NativeProcess,Channel,UnixSocket}_*, and
# liboverride.so as ..._bridge_Compiler_*. Renaming the package or the classes breaks the lookup.
-keep class com.github.yumeyucca.yumebox.core.bridge.** { *; }

# Orphaned: the comment here used to claim libcompat.c resolved kotlin.Unit, but neither native
# library does — libcompat.c only FindClass'es java/io/IOException, and liboverride.so touches no
# Kotlin type at all. Left in place because dropping a keep rule changes R8 output and was not
# worth verifying against a release build; delete it once someone can run a minified build.
-keep class kotlin.Unit {
    public static final kotlin.Unit INSTANCE;
}
# Keep the whole interface: member signatures vary across coroutines versions (R8 unmatched-member noise).
-keep,allowoptimization interface kotlinx.coroutines.CompletableDeferred { *; }
