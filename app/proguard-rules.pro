-keep class com.github.yumeyucca.yumebox.App { *; }
-include ../core/consumer-rules.pro
-keep class androidx.core.app.CoreComponentFactory { *; }

# language-textmate uses the native Oniguruma backend; Joni is an optional fallback.
-dontwarn org.joni.**

# Shizuku / Sui: the API uses hidden-API reflection and the UserService is instantiated by name.
# The moe.shizuku.server AIDL interfaces must keep their names: the Binder descriptor is the
# interface name and the Shizuku server rejects a renamed one.
-keep class moe.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep class rikka.sui.** { *; }
-keep class com.github.yumeyucca.yumebox.runtime.service.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn rikka.sui.**
