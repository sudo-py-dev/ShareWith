# --------------------------------------------------------------------------------
# ShareWith R8/ProGuard Configuration
# Optimized for Ktor (CIO Engine) and Compose with aggressive shrinking
# --------------------------------------------------------------------------------

# 1. Attributes & System Metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

# 2. Project Specific
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

# 3. Ktor (Netty Engine)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Netty specific ignores (optional dependencies)
-dontwarn io.netty.**
-keep class io.netty.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn jdk.jfr.**

# 4. Kotlin Coroutines
# Bundled rules inside the libraries automatically manage coroutines keeps.
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.**

# 5. Libraries
# QRCode Generator
-keep class qrcode.** { *; }
-dontwarn qrcode.**

# 6. General Android & JVM Suppressions
-dontwarn javax.naming.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.Unsafe
-dontwarn sun.misc.Signal*
-dontwarn sun.security.**
-dontwarn com.sun.security.**
-dontwarn java.lang.invoke.**
-dontwarn reactor.blockhound.**

# Optimization settings
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively

