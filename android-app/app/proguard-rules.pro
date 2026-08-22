# Proguard rules for NewsPulse AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.newspulse.ai.data.model.** { *; }
-keep class com.newspulse.ai.data.remote.** { *; }
