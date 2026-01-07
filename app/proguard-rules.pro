# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep our classes
-keep class com.gtasa.skinmanager.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);  
}

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }