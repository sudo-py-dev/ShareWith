# --------------------------------------------------------------------------------
# ShareWith R8/ProGuard Configuration
# Optimized for Ktor, Netty, and Compose
# --------------------------------------------------------------------------------

# 1. Project Specific
# We keep AppState and data models to prevent serialization issues
-keep class com.share.with.AppState { *; }
-keep class com.share.with.ActiveSession { *; }
-keep class com.share.with.SharedItem { *; }
-keep class com.share.with.PendingConnection { *; }
-keep class com.share.with.SecurityMode { *; }

# Keep Activity and Service (standard Android entry points)
-keep class com.share.with.MainActivity { *; }
-keep class com.share.with.CrashActivity { *; }
-keep class com.share.with.FileSharingService { *; }

# 2. Ktor & Netty
# Ktor uses reflection to find and load engines and modules
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Netty performs intensive reflection for platform-specific optimizations
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# 3. Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 4. Libraries
# QRCode Generator
-keep class qrcode.** { *; }
-dontwarn qrcode.**

# 5. General Android & JVM Suppressions
-dontwarn javax.naming.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.Unsafe
-dontwarn sun.misc.Signal*
-dontwarn sun.security.**
-dontwarn com.sun.security.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn java.lang.invoke.MethodHandles$Lookup

# Optimization settings
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively
