-repackageclasses ''
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5
-mergeinterfacesaggressively

# مبهم‌سازی قوی‌تر کلاس‌های اصلی
-keep class io.nekohasekai.sagernet.** { *;}
-keep class moe.matsuri.nb4a.** { *;}

# حذف پروتکل‌های غیرضروری برای کاهش حجم
-keep,allowshrinking,allowoptimization class moe.matsuri.nb4a.proxy.ssh.** { !<methods>; }
-keep,allowshrinking,allowoptimization class moe.matsuri.nb4a.proxy.anytls.** { !<methods>; }
-keep,allowshrinking,allowoptimization class moe.matsuri.nb4a.proxy.shadowtls.** { !<methods>; }
-keep,allowshrinking,allowoptimization class moe.matsuri.nb4a.proxy.naive.** { !<methods>; }

# Clean Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNull(java.lang.Object);
    static void checkNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}

# حذف لاگ‌های debug
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ini4j
-keep public class org.ini4j.spi.** { <init>(); }

# SnakeYaml
-keep class org.yaml.snakeyaml.** { *; }

-printmapping build/outputs/mapping/mapping.txt
-keepattributes SourceFile
-keepattributes LineNumberTable

-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.beans.Transient
-dontwarn java.beans.VetoableChangeListener
-dontwarn java.beans.VetoableChangeSupport
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.bouncycastle.jce.provider.BouncyCastleProvider
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn java.beans.PropertyVetoException

# حذف پروتکل‌های غیرضروری
-dontwarn moe.matsuri.nb4a.proxy.ssh.**
-dontwarn moe.matsuri.nb4a.proxy.anytls.**
-dontwarn moe.matsuri.nb4a.proxy.shadowtls.**
-dontwarn moe.matsuri.nb4a.proxy.naive.**
