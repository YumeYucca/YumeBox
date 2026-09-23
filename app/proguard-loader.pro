-keep class com.github.yumeyucca.yumebox.App { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }

# language-textmate uses the native Oniguruma backend; Joni is an optional fallback.
-dontwarn org.joni.**

# Shizuku / Sui: the API uses hidden-API reflection and the UserService is instantiated by name.
-keep class rikka.shizuku.** { *; }
-keep class rikka.sui.** { *; }
-keep class com.github.yumeyucca.yumebox.runtime.service.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn rikka.sui.**
