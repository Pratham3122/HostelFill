# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/android/sdk/tools/proguard/proguard-android.txt

# Keep JavaScript interface methods (used by the WebView bridge)
-keepclassmembers class com.prathamshah.hostelfill.ui.screens.AutofillBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
