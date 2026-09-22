-keep class com.github.yumeyucca.yumebox.App { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }

# language-textmate uses the native Oniguruma backend; Joni is an optional fallback.
-dontwarn org.joni.**

# Shizuku / Sui. The API uses hidden-API reflection, and the UserService class is instantiated by
# its binary name from another process, so neither the names nor the implicitly-used no-argument
# constructors may be shrunk away.
-keep class rikka.shizuku.** { *; }
-keep class rikka.sui.** { *; }
-keep class com.github.yumeyucca.yumebox.runtime.service.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn rikka.sui.**
