# Rename everything aggressively
-dontskipnonpubliclibraryclasses
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

# Keep only what Android needs to run
-keep public class com.echo.assistant.SplashActivity
-keep public class com.echo.assistant.LoginActivity
-keep public class com.echo.assistant.MainActivity
-keep public class com.echo.assistant.SettingsActivity
-keep public class com.echo.assistant.VoiceActivity
-keep public class com.echo.assistant.EchoAccessibility
-keep public class com.echo.assistant.EchoWakeService
-keep public class com.echo.assistant.BootReceiver
-keep public class com.echo.assistant.ReminderReceiver

# Keep Android lifecycle methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public *;
}
-keepclassmembers class * extends android.app.Service {
    public *;
}
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public *;
}

# Obfuscate everything else — API keys logic, action handlers, etc.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove all logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
